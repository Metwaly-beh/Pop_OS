import java.io.*;
import java.util.*;
import java.util.zip.*;

// Main class - this is where the whole simulation runs
// handles process creation, scheduling, and the step-by-step clock loop
public class Main {

    // shared system components
    static Memory memory = new Memory();
    static Disk disk = new Disk();
    static Swap swap = new Swap(memory, disk);
    static List<PCB> allPCBs = new ArrayList<>();
    static List<Process> allProcesses = new ArrayList<>();
    static Scheduler scheduler;
    static Map<String, Semaphore> semaphores = new HashMap<>();
    static GUI gui;
    static Interpreter interpreter;

    // simulation state
    static int clock = 0;
    static PCB running = null;
    static boolean paused = true;
    static boolean simDone = false;

    // process arrival times and their corresponding program files
    static final int[] ARRIVAL_TIMES    = {0, 1, 4};
    static final String[] PROGRAM_FILES = {"Program1.txt", "Program2.txt", "Program3.txt"};
    static final int MAX_CYCLES = 200;

    public static void main(String[] args) {
        gui = new GUI();

        // choose the scheduling algorithm here (RR, HRRN, or MLFQ)
        scheduler = SchedulerChooser.create("RR");

        // create semaphores for the three shared resources
        semaphores.put("userInput",  new Semaphore("userInput",  scheduler));
        semaphores.put("userOutput", new Semaphore("userOutput", scheduler));
        semaphores.put("file",       new Semaphore("file",       scheduler));

        SystemCalls systemCalls = new SystemCalls(memory);
        interpreter = new Interpreter(memory, systemCalls, semaphores);

        // wire up the GUI buttons to the simulation logic
        gui.setOnStart(() -> {
            paused = false;
            // run the simulation automatically in a background thread
            new Thread(() -> {
                while (!paused && !simDone) {
                    stepOnce();
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            }).start();
        });

        gui.setOnPause(() -> paused = true);
        gui.setOnStep(() -> { if (!simDone) stepOnce(); });
    }

    // runs one clock cycle of the simulation
    static void stepOnce() {
        if (simDone || clock >= MAX_CYCLES) {
            simDone = true;
            gui.onSimulationComplete();
            return;
        }

        System.out.println("\n========== CLOCK = " + clock + " ==========");
        gui.updateClock(clock);

        // check if any new process arrives at this clock tick
        for (int i = 0; i < ARRIVAL_TIMES.length; i++) {
            if (clock == ARRIVAL_TIMES[i]) {
                try { createProcess(i + 1, PROGRAM_FILES[i]); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }

        // pick a new process to run if needed
        if (running == null || running.getState() == State.FINISHED
                || running.getState() == State.BLOCKED) {
            PCB next = scheduler.schedule(clock);
            if (next != null) {
                boolean loaded = swap.ensureLoaded(next, allPCBs);
                if (!loaded) {
                    System.out.println("[OS] Could not load Process " + next.getProcessID());
                    // put it back in the ready queue and try again next tick
                    next.setState(State.READY);
                    scheduler.addProcess(next, clock);
                    clock++;
                    return;
                }
                running = next;
                running.setState(State.RUNNING);
                memory.setState(running.getProcessID(), "RUNNING");
            }
        }

        // nothing to run this tick
        if (running == null) {
            System.out.println("[Clock " + clock + "] No process to run.");
            gui.updateRunningProcess(-1, "-");
            gui.updateReadyQueue(getReadyQueueString());
            gui.updateBlockedQueues(getBlockedQueuesString());
            gui.updateMemory(getMemoryString());
            if (allDone()) {
                simDone = true;
                gui.onSimulationComplete();
                System.out.println("\n[OS] All processes finished at clock = " + clock);
            }
            clock++;
            return;
        }

        // make sure the running process is in memory (might have been swapped out)
        if (!memory.isLoaded(running.getProcessID())) {
            boolean loaded = swap.ensureLoaded(running, allPCBs);
            if (!loaded) {
                System.out.println("[OS] Could not swap in Process " + running.getProcessID());
                running.setState(State.READY);
                scheduler.addProcess(running, clock);
                running = null;
                clock++;
                return;
            }
        }

        Process runningProcess = getProcess(running.getProcessID());
        String currentInstr;
        try {
            currentInstr = memory.getInstruction(running.getProcessID(), running.getProgramCounter());
        } catch (SecurityException e) {
            currentInstr = null;
        }
        gui.updateRunningProcess(running.getProcessID(),
                currentInstr != null ? currentInstr : "done");

        // execute one instruction
        boolean ok = interpreter.executeNextInstruction(runningProcess, clock);

        // sync state from memory back to the PCB (if still loaded)
        if (memory.isLoaded(running.getProcessID())) {
            running.syncFromMemory(memory);
        }

        // update the GUI
        gui.updateMemory(getMemoryString());
        gui.updateReadyQueue(getReadyQueueString());
        gui.updateBlockedQueues(getBlockedQueuesString());

        if (!ok) {
            if (running.getState() == State.FINISHED) {
                scheduler.onComplete(running, clock);
                // free memory right away so other processes can use it
                if (memory.isLoaded(running.getProcessID())) {
                    memory.deallocate(running.getProcessID());
                }
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " FINISHED.");
                running = null;
            } else if (running.getState() == State.BLOCKED) {
                // semWait already called scheduler.onBlock(), dont call again
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " BLOCKED.");
                running = null;
            }
        } else {
            // check if the time quantum expired (for preemptive schedulers)
            boolean preempted = scheduler.tick(running, clock);
            if (preempted) {
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " preempted.");
                running = null;
            }
        }

        // check if everything is done
        if (allDone()) {
            simDone = true;
            gui.onSimulationComplete();
            System.out.println("\n[OS] All processes finished at clock = " + clock);
        }

        clock++;
    }

    // creates a new process, loads its instructions into memory, and adds it to the scheduler
    static void createProcess(int pid, String programFile) throws Exception {
        List<String> instructions = loadInstructions(programFile);
        if (instructions.isEmpty()) {
            System.out.println("[OS] Could not load " + programFile);
            return;
        }

        int burstTime = instructions.size();
        PCB pcb = new PCB(pid, clock, burstTime, burstTime);

        int required = Memory.FIXED_OVERHEAD + instructions.size();

        // try to free memory if there isnt enough
        int attempts = 0;
        while (memory.getFreeWords() < required && attempts < 5) {
            boolean freed = swap.freeSpaceFor(required, allPCBs);
            if (!freed) break;
            attempts++;
        }

        int base = memory.allocate(pid, instructions);
        if (base == -1) {
            System.out.println("[OS] Cannot create Process " + pid + ": not enough memory.");
            return;
        }

        pcb.setLowerBound(memory.getLowerBound(pid));
        pcb.setUpperBound(memory.getUpperBound(pid));

        allPCBs.add(pcb);
        allProcesses.add(new Process(pcb));
        scheduler.addProcess(pcb, clock);

        System.out.println("[OS] Process " + pid + " created from " + programFile);
        gui.appendDiskLog("[t=" + clock + "] Process " + pid + " created.");
    }

    // tries to load instructions from a .txt file, falls back to a .zip if needed
    static List<String> loadInstructions(String fileName) {
        List<String> lines = new ArrayList<>();

        File f = new File(fileName);
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) lines.add(line);
                }
                return lines;
            } catch (IOException ignored) {}
        }

        // try reading from a zip file with the same base name
        String zipName = fileName.replace(".txt", ".zip");
        try (ZipFile zip = new ZipFile(zipName)) {
            ZipEntry entry = zip.getEntry(fileName);
            if (entry == null) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();
                    if (entry.getName().endsWith(".txt")) break;
                }
            }
            if (entry != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(zip.getInputStream(entry)))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) lines.add(line);
                    }
                }
            }
        } catch (IOException ignored) {}

        return lines;
    }

    // finds a Process object by its PID
    static Process getProcess(int pid) {
        for (Process p : allProcesses) {
            if (p.getProcessID() == pid) return p;
        }
        return null;
    }

    // returns true only if every process has finished
    static boolean allDone() {
        if (allPCBs.isEmpty()) return false;
        for (PCB pcb : allPCBs) {
            if (pcb.getState() != State.FINISHED) return false;
        }
        return true;
    }

    // builds a string showing all READY processes for the GUI
    static String getReadyQueueString() {
        StringBuilder sb = new StringBuilder();
        for (PCB pcb : allPCBs) {
            if (pcb.getState() == State.READY) {
                sb.append("PID=").append(pcb.getProcessID())
                  .append(" | PC=").append(pcb.getProgramCounter())
                  .append(" | Burst=").append(pcb.getBurstTime()).append("\n");
            }
        }
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }

    // builds a string showing which processes are blocked on each semaphore
    static String getBlockedQueuesString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Semaphore> entry : semaphores.entrySet()) {
            sb.append(entry.getKey()).append(": [");
            Queue<Process> q = entry.getValue().getBlockedQueue();
            boolean first = true;
            for (Process p : q) {
                if (!first) sb.append(", ");
                sb.append("P").append(p.getProcessID());
                first = false;
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    // builds a string dump of everything currently in memory for the GUI
    static String getMemoryString() {
        StringBuilder sb = new StringBuilder();
        for (PCB pcb : allPCBs) {
            if (memory.isLoaded(pcb.getProcessID())) {
                int lb = memory.getLowerBound(pcb.getProcessID());
                int ub = memory.getUpperBound(pcb.getProcessID());
                sb.append("── P").append(pcb.getProcessID())
                  .append(" [").append(lb).append("-").append(ub).append("] ──\n");
                for (int i = lb; i <= ub; i++) {
                    sb.append("  [").append(String.format("%02d", i)).append("] ")
                      .append(memory.read(pcb.getProcessID(), i - lb)).append("\n");
                }
            }
        }
        return sb.length() == 0 ? "(empty)" : sb.toString();
    }
}
