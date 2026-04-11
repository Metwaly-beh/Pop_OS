import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates the main memory of the OS.
 *
 * Layout per process (10 words total):
 *   [base+0]  PCB: processID
 *   [base+1]  PCB: processState
 *   [base+2]  PCB: programCounter
 *   [base+3]  PCB: memoryLowerBound
 *   [base+4]  PCB: memoryUpperBound
 *   [base+5]  Variable slot 1  (key)
 *   [base+6]  Variable slot 1  (value)
 *   [base+7]  Variable slot 2  (key)
 *   [base+8]  Variable slot 2  (value)
 *   [base+9]  Variable slot 3  (key)
 *   [base+10] Variable slot 3  (value)
 *   [base+11 .. base+N] Instruction lines
 *
 * Total memory = 40 words.
 * Each process is allocated exactly enough words for:
 *   PCB (5 words) + 3 variables (6 words) + instruction lines.
 */
public class Memory {

    public static  int TOTAL_SIZE = 40;

    // PCB field offsets from a process base address
    public static  int PCB_PROCESS_ID     = 0;
    public static  int PCB_STATE          = 1;
    public static  int PCB_PROGRAM_COUNTER= 2;
    public static  int PCB_LOWER_BOUND    = 3;
    public static  int PCB_UPPER_BOUND    = 4;

    // Each process needs 5 PCB words + 6 variable words = 11 fixed words
    public static  int PCB_SIZE           = 5;
    public static  int MAX_VARIABLES      = 3;
    public static  int VARIABLE_SPACE     = MAX_VARIABLES * 2; // key + value per var
    public static  int FIXED_OVERHEAD     = PCB_SIZE + VARIABLE_SPACE; // 11 words

    private final String[] memory;

    // Maps processID -> base address in memory
    private final Map<Integer, Integer> processBaseMap;

    public Memory() {
        memory = new String[TOTAL_SIZE];
        processBaseMap = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Allocation / Deallocation
    // -------------------------------------------------------------------------

    /**
     * Allocates memory for a new process.
     *
     * @param processID     Unique process identifier.
     * @param instructions  List of instruction strings from the program file.
     * @return              The base address allocated, or -1 if not enough space.
     */
    public int allocate(int processID, List<String> instructions) {
        int required = FIXED_OVERHEAD + instructions.size();
        int base = findFreeBlock(required);

        if (base == -1) {
            System.out.println("[Memory] Not enough space for Process " + processID
                    + " (needs " + required + " words). Consider swapping a process out.");
            return -1;
        }

        processBaseMap.put(processID, base);

        // Write PCB
        memory[base + PCB_PROCESS_ID]      = "PID:"   + processID;
        memory[base + PCB_STATE]           = "STATE:READY";
        memory[base + PCB_PROGRAM_COUNTER] = "PC:0";
        memory[base + PCB_LOWER_BOUND]     = "LB:"    + base;
        memory[base + PCB_UPPER_BOUND]     = "UB:"    + (base + required - 1);

        // Clear variable slots
        for (int i = 0; i < MAX_VARIABLES; i++) {
            memory[base + PCB_SIZE + i * 2]     = "VAR" + (i + 1) + "_KEY:null";
            memory[base + PCB_SIZE + i * 2 + 1] = "VAR" + (i + 1) + "_VAL:null";
        }

        // Write instructions
        int instrBase = base + FIXED_OVERHEAD;
        for (int i = 0; i < instructions.size(); i++) {
            memory[instrBase + i] = "INSTR:" + instructions.get(i);
        }

        System.out.println("[Memory] Allocated " + required + " words for Process "
                + processID + " at addresses [" + base + " - " + (base + required - 1) + "]");
        return base;
    }

    /**
     * Frees all memory words belonging to a process.
     */
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

    // -------------------------------------------------------------------------
    // PCB Read / Write
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Variable Read / Write
    // -------------------------------------------------------------------------

    /**
     * Stores a variable in one of the 3 variable slots for the given process.
     * If the variable already exists, its value is updated.
     *
     * @throws IllegalArgumentException if all 3 slots are full and the variable is new.
     * @throws SecurityException        if the target address is outside process bounds.
     */
    public void setVariable(int processID, String key, String value) {
        int base = getBase(processID);
        int varBase = base + PCB_SIZE;

        // Check if variable already exists
        for (int i = 0; i < MAX_VARIABLES; i++) {
            String storedKey = readField(varBase + i * 2, "VAR" + (i + 1) + "_KEY:");
            if (storedKey.equals(key)) {
                // Update existing variable
                guardBounds(processID, varBase + i * 2 + 1);
                memory[varBase + i * 2 + 1] = "VAR" + (i + 1) + "_VAL:" + value;
                return;
            }
        }

        // Find an empty slot
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

        throw new IllegalArgumentException(
                "[Memory] Process " + processID + " has no free variable slots.");
    }

    /**
     * Retrieves the value of a variable for the given process.
     *
     * @return The variable's value, or null if not found.
     */
    public String getVariable(int processID, String key) {
        int base = getBase(processID);
        int varBase = base + PCB_SIZE;

        for (int i = 0; i < MAX_VARIABLES; i++) {
            String storedKey = readField(varBase + i * 2, "VAR" + (i + 1) + "_KEY:");
            if (storedKey.equals(key)) {
                return readField(varBase + i * 2 + 1, "VAR" + (i + 1) + "_VAL:");
            }
        }
        return null; // variable not found
    }

    // -------------------------------------------------------------------------
    // Instruction Access
    // -------------------------------------------------------------------------

    /**
     * Returns the instruction at the given index (0-based) for the process.
     *
     * @return The instruction string, or null if index is out of range.
     */
    public String getInstruction(int processID, int index) {
        int base = getBase(processID);
        int instrAddr = base + FIXED_OVERHEAD + index;
        guardBounds(processID, instrAddr);
        String word = memory[instrAddr];
        if (word == null || !word.startsWith("INSTR:")) return null;
        return word.substring(6);
    }

    /**
     * Returns the total number of instructions stored for the process.
     */
    public int getInstructionCount(int processID) {
        int base  = getBase(processID);
        int upper = getUpperBound(processID);
        int count = 0;
        for (int addr = base + FIXED_OVERHEAD; addr <= upper; addr++) {
            if (memory[addr] != null && memory[addr].startsWith("INSTR:")) count++;
        }
        return count;
    }

    // -------------------------------------------------------------------------
    // Low-level Read / Write (with bounds checking)
    // -------------------------------------------------------------------------

    /**
     * Writes a raw value to an offset within a process's memory block.
     */
    public void write(int processID, int offset, String value) {
        int addr = getBase(processID) + offset;
        guardBounds(processID, addr);
        memory[addr] = value;
    }

    /**
     * Reads a raw value from an offset within a process's memory block.
     */
    public String read(int processID, int offset) {
        int addr = getBase(processID) + offset;
        guardBounds(processID, addr);
        return memory[addr];
    }

    // -------------------------------------------------------------------------
    // Disk Swap Support
    // -------------------------------------------------------------------------

    /**
     * Dumps the entire memory block of a process into a list of strings
     * (to be written to disk). The process's memory is then freed.
     *
     * @return List of "<address>|<value>" lines representing the process snapshot.
     */
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

    /**
     * Restores a process's memory from a disk snapshot.
     * Finds a free block large enough and loads the data back.
     *
     * @param snapshot  Lines produced by swapOut().
     * @return          New base address, or -1 if not enough space.
     */
    public int swapIn(int processID, List<String> snapshot) {
        int size = snapshot.size();
        int base = findFreeBlock(size);

        if (base == -1) {
            System.out.println("[Memory] Cannot swap in Process " + processID
                    + ": not enough free space (" + size + " words needed).");
            return -1;
        }

        // Rebase the snapshot to the new base address
        int originalBase = Integer.parseInt(snapshot.get(0).split("\\|")[0]);
        int offset = base - originalBase;

        for (String line : snapshot) {
            String[] parts = line.split("\\|", 2);
            int oldAddr  = Integer.parseInt(parts[0]);
            String value = parts[1].equals("null") ? null : parts[1];

            int newAddr = oldAddr + offset;
            memory[newAddr] = value;
        }

        // Update PCB bounds
        processBaseMap.put(processID, base);
        memory[base + PCB_LOWER_BOUND] = "LB:" + base;
        memory[base + PCB_UPPER_BOUND] = "UB:" + (base + size - 1);

        System.out.println("[Memory] Process " + processID + " swapped IN at ["
                + base + " - " + (base + size - 1) + "]");
        return base;
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    /**
     * Prints the full memory contents in a human-readable table.
     */
//    public void display() {
//        System.out.println("\n╔══════════════════════════════════════════════════════╗");
//        System.out.println("║                  MEMORY STATE                       ║");
//        System.out.println("╠════════╦═════════════════════════════════════════════╣");
//        System.out.printf( "║ %-6s ║ %-43s ║%n", "Addr", "Content");
//        System.out.println("╠════════╬═════════════════════════════════════════════╣");
//        for (int i = 0; i < TOTAL_SIZE; i++) {
//            String content = (memory[i] == null) ? "--- free ---" : memory[i];
//            System.out.printf("║ %-6d ║ %-43s ║%n", i, content);
//        }
//        System.out.println("╚════════╩═════════════════════════════════════════════╝\n");
//    }

    /**
     * Prints only the memory block belonging to a specific process.
     */
//    public void displayProcess(int processID) {
//        int base  = getBase(processID);
//        int upper = getUpperBound(processID);
//        System.out.println("\n[Memory] Process " + processID
//                + " block [" + base + " - " + upper + "]:");
//        for (int i = base; i <= upper; i++) {
//            System.out.printf("  [%2d] %s%n", i, memory[i]);
//        }
//    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int getBase(int processID) {
        Integer base = processBaseMap.get(processID);
        if (base == null)
            throw new IllegalArgumentException(
                    "[Memory] Process " + processID + " is not loaded in memory.");
        return base;
    }

    /**
     * Returns the start of the first contiguous free block of at least `size` words,
     * or -1 if none exists.
     */
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

    /**
     * Extracts the value portion after a known prefix from a memory word.
     */
    private String readField(int addr, String prefix) {
        String word = memory[addr];
        if (word == null) return "null";
        return word.startsWith(prefix) ? word.substring(prefix.length()) : word;
    }

    /**
     * Throws SecurityException if addr is outside the process's allocated bounds.
     */
    private void guardBounds(int processID, int addr) {
        int base  = getBase(processID);
        int upper = Integer.parseInt(readField(base + PCB_UPPER_BOUND, "UB:"));
        if (addr < base || addr > upper) {
            throw new SecurityException(
                    "[Memory] Process " + processID + " attempted out-of-bounds access at address "
                            + addr + " (allowed: [" + base + " - " + upper + "])");
        }
    }

    public boolean isLoaded(int processID) {
        return processBaseMap.containsKey(processID);
    }

    public int getFreeWords() {
        int count = 0;
        for (String word : memory) if (word == null) count++;
        return count;
    }
}