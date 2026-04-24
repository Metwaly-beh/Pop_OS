import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Memory class - simulates a flat array of 40 words
// each process gets a fixed-size block with a PCB section, variable slots, and instruction space
public class Memory {

    public static int TOTAL_SIZE = 40;

    // offsets within the PCB section for each field
    public static int PCB_PROCESS_ID     = 0;
    public static int PCB_STATE          = 1;
    public static int PCB_PROGRAM_COUNTER= 2;
    public static int PCB_LOWER_BOUND    = 3;
    public static int PCB_UPPER_BOUND    = 4;

    // PCB takes 5 words, then 3 variables * 2 words each = 6 words
    public static int PCB_SIZE           = 5;
    public static int MAX_VARIABLES      = 3;
    public static int VARIABLE_SPACE     = MAX_VARIABLES * 2;
    public static int FIXED_OVERHEAD     = PCB_SIZE + VARIABLE_SPACE; // = 11

    // the actual memory array
    private final String[] memory;
    // maps processID -> base address in memory
    private final Map<Integer, Integer> processBaseMap;

    public Memory() {
        memory = new String[TOTAL_SIZE];
        processBaseMap = new HashMap<>();
    }

    // allocates a contiguous block for a process and fills in its PCB + instructions
    public int allocate(int processID, List<String> instructions) {
        int required = FIXED_OVERHEAD + instructions.size();
        int base = findFreeBlock(required);

        if (base == -1) {
            System.out.println("[Memory] Not enough space for Process " + processID
                    + " (needs " + required + " words). Consider swapping a process out.");
            return -1;
        }

        processBaseMap.put(processID, base);

        // fill in PCB fields
        memory[base + PCB_PROCESS_ID]      = "PID:"   + processID;
        memory[base + PCB_STATE]           = "STATE:READY";
        memory[base + PCB_PROGRAM_COUNTER] = "PC:0";
        memory[base + PCB_LOWER_BOUND]     = "LB:"    + base;
        memory[base + PCB_UPPER_BOUND]     = "UB:"    + (base + required - 1);

        // initialize variable slots to null
        for (int i = 0; i < MAX_VARIABLES; i++) {
            memory[base + PCB_SIZE + i * 2]     = "VAR" + (i + 1) + "_KEY:null";
            memory[base + PCB_SIZE + i * 2 + 1] = "VAR" + (i + 1) + "_VAL:null";
        }

        // store the instructions right after the overhead section
        int instrBase = base + FIXED_OVERHEAD;
        for (int i = 0; i < instructions.size(); i++) {
            memory[instrBase + i] = "INSTR:" + instructions.get(i);
        }

        System.out.println("[Memory] Allocated " + required + " words for Process "
                + processID + " at addresses [" + base + " - " + (base + required - 1) + "]");
        return base;
    }

    // frees all memory words used by a process
    public void deallocate(int processID) {
        int base = getBase(processID);
        if (base == -1) return;

        int upper = Integer.parseInt(readField(base + PCB_UPPER_BOUND, "UB:"));
        for (int i = base; i <= upper; i++) {
            memory[i] = null;
        }
        processBaseMap.remove(processID);
        System.out.println("[Memory] Freed memory for Process " + processID);
    }

    public void setState(int processID, String state) {
        write(processID, PCB_STATE, "STATE:" + state);
    }

    public String getState(int processID) {
        return readField(getBase(processID) + PCB_STATE, "STATE:");
    }

    public void setProgramCounter(int processID, int pc) {
        write(processID, PCB_PROGRAM_COUNTER, "PC:" + pc);
    }

    public int getProgramCounter(int processID) {
        return Integer.parseInt(readField(getBase(processID) + PCB_PROGRAM_COUNTER, "PC:"));
    }

    public int getLowerBound(int processID) {
        return Integer.parseInt(readField(getBase(processID) + PCB_LOWER_BOUND, "LB:"));
    }

    public int getUpperBound(int processID) {
        return Integer.parseInt(readField(getBase(processID) + PCB_UPPER_BOUND, "UB:"));
    }

    // stores a variable by key in one of the 3 variable slots
    // updates the value if the key already exists, otherwise finds an empty slot
    public void setVariable(int processID, String key, String value) {
        int base = getBase(processID);
        int varBase = base + PCB_SIZE;

        // check if variable already exists, update it
        for (int i = 0; i < MAX_VARIABLES; i++) {
            String storedKey = readField(varBase + i * 2, "VAR" + (i + 1) + "_KEY:");
            if (storedKey.equals(key)) {
                guardBounds(processID, varBase + i * 2 + 1);
                memory[varBase + i * 2 + 1] = "VAR" + (i + 1) + "_VAL:" + value;
                return;
            }
        }

        // find a free slot
        for (int i = 0; i < MAX_VARIABLES; i++) {
            String storedKey = readField(varBase + i * 2, "VAR" + (i + 1) + "_KEY:");
            if (storedKey.equals("null")) {
                guardBounds(processID, varBase + i * 2);
                guardBounds(processID, varBase + i * 2 + 1);
                memory[varBase + i * 2]     = "VAR" + (i + 1) + "_KEY:" + key;
                memory[varBase + i * 2 + 1] = "VAR" + (i + 1) + "_VAL:" + value;
                return;
            }
        }

        // all 3 slots are taken
        throw new IllegalArgumentException(
                "[Memory] Process " + processID + " has no free variable slots.");
    }

    // looks up a variable by key, returns null if not found
    public String getVariable(int processID, String key) {
        int base = getBase(processID);
        int varBase = base + PCB_SIZE;

        for (int i = 0; i < MAX_VARIABLES; i++) {
            String storedKey = readField(varBase + i * 2, "VAR" + (i + 1) + "_KEY:");
            if (storedKey.equals(key)) {
                return readField(varBase + i * 2 + 1, "VAR" + (i + 1) + "_VAL:");
            }
        }
        return null;
    }

    // returns the instruction at the given index (0-based) for the process
    public String getInstruction(int processID, int index) {
        int base = getBase(processID);
        int instrAddr = base + FIXED_OVERHEAD + index;
        guardBounds(processID, instrAddr);
        String word = memory[instrAddr];
        if (word == null || !word.startsWith("INSTR:")) return null;
        return word.substring(6);
    }

    // counts how many instructions are stored for the process
    public int getInstructionCount(int processID) {
        int base  = getBase(processID);
        int upper = getUpperBound(processID);
        int count = 0;
        for (int addr = base + FIXED_OVERHEAD; addr <= upper; addr++) {
            if (memory[addr] != null && memory[addr].startsWith("INSTR:")) count++;
        }
        return count;
    }

    // low-level write with bounds check
    public void write(int processID, int offset, String value) {
        int addr = getBase(processID) + offset;
        guardBounds(processID, addr);
        memory[addr] = value;
    }

    // low-level read with bounds check
    public String read(int processID, int offset) {
        int addr = getBase(processID) + offset;
        guardBounds(processID, addr);
        return memory[addr];
    }

    // takes a snapshot of the process's memory and removes it (for swapping out)
    public List<String> swapOut(int processID) {
        int base  = getBase(processID);
        int upper = getUpperBound(processID);

        List<String> snapshot = new ArrayList<>();
        for (int addr = base; addr <= upper; addr++) {
            snapshot.add(addr + "|" + memory[addr]);
        }

        deallocate(processID);
        System.out.println("[Memory] Process " + processID + " swapped OUT to disk.");
        return snapshot;
    }

    // restores a process from a snapshot (for swapping in)
    // may place it at a different base address if memory layout changed
    public int swapIn(int processID, List<String> snapshot) {
        int size = snapshot.size();
        int base = findFreeBlock(size);

        if (base == -1) {
            System.out.println("[Memory] Cannot swap in Process " + processID
                    + ": not enough free space (" + size + " words needed).");
            return -1;
        }

        int originalBase = Integer.parseInt(snapshot.get(0).split("\\|")[0]);
        int offset = base - originalBase;

        for (String line : snapshot) {
            String[] parts = line.split("\\|", 2);
            int oldAddr  = Integer.parseInt(parts[0]);
            String value = parts[1].equals("null") ? null : parts[1];

            int newAddr = oldAddr + offset;
            memory[newAddr] = value;
        }

        processBaseMap.put(processID, base);
        // update LB and UB to reflect new location
        memory[base + PCB_LOWER_BOUND] = "LB:" + base;
        memory[base + PCB_UPPER_BOUND] = "UB:" + (base + size - 1);

        System.out.println("[Memory] Process " + processID + " swapped IN at ["
                + base + " - " + (base + size - 1) + "]");
        return base;
    }

    // returns the base address for a process, throws if not loaded
    private int getBase(int processID) {
        Integer base = processBaseMap.get(processID);
        if (base == null)
            throw new IllegalArgumentException(
                    "[Memory] Process " + processID + " is not loaded in memory.");
        return base;
    }

    // finds the first contiguous free block of the required size
    private int findFreeBlock(int size) {
        int count = 0;
        int start = -1;
        for (int i = 0; i < TOTAL_SIZE; i++) {
            if (memory[i] == null) {
                if (count == 0) start = i;
                count++;
                if (count == size) return start;
            } else {
                count = 0;
                start = -1;
            }
        }
        return -1;
    }

    // strips the prefix from a memory word (e.g. "PC:5" -> "5")
    private String readField(int addr, String prefix) {
        String word = memory[addr];
        if (word == null) return "null";
        return word.startsWith(prefix) ? word.substring(prefix.length()) : word;
    }

    // throws SecurityException if the address is outside the process's allocated range
    private void guardBounds(int processID, int addr) {
        int base  = getBase(processID);
        int upper = Integer.parseInt(readField(base + PCB_UPPER_BOUND, "UB:"));
        if (addr < base || addr > upper) {
            throw new SecurityException(
                    "[Memory] Process " + processID + " attempted out-of-bounds access at address "
                            + addr + " (allowed: [" + base + " - " + upper + "])");
        }
    }

    // returns true if the process is currently in memory
    public boolean isLoaded(int processID) {
        return processBaseMap.containsKey(processID);
    }

    // counts how many words are currently free
    public int getFreeWords() {
        int count = 0;
        for (String word : memory) if (word == null) count++;
        return count;
    }
}
