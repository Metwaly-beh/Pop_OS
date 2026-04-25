import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Main {

    static Memory memory = new Memory();
    static Disk disk = new Disk();
    static Swap swap = new Swap(memory, disk);
    static List<PCB> allPCBs = new ArrayList<>();
    static List<Process> allProcesses = new ArrayList<>();
    static Scheduler scheduler;
    static Map<String, Semaphore> semaphores = new HashMap<>();
    static GUI gui;
    static Interpreter interpreter;

    static int clock = 0;
    static PCB running = null;
    static boolean paused = true;
    static boolean simDone = false;

    static final int[] ARRIVAL_TIMES    = {0, 1, 4};
    static final String[] PROGRAM_FILES = {"Program1.txt", "Program2.txt", "Program3.txt"};
    static final int MAX_CYCLES = 200;

    public static void main(String[] args) {
        gui = new GUI();

        scheduler = SchedulerChooser.create("RR",2);

        semaphores.put("userInput",  new Semaphore("userInput",  scheduler));
        semaphores.put("userOutput", new Semaphore("userOutput", scheduler));
        semaphores.put("file",       new Semaphore("file",       scheduler));

        SystemCalls systemCalls = new SystemCalls(memory);
        interpreter = new Interpreter(memory, systemCalls, semaphores);

        gui.setOnStart(() -> {
            paused = false;
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

    static void stepOnce() {
        if (simDone || clock >= MAX_CYCLES) {
            simDone = true;
            gui.onSimulationComplete();
            return;
        }

        System.out.println("\n========== CLOCK = " + clock + " ==========");
        gui.updateClock(clock);

        // check if for processes after ticks
        for (int i = 0; i < ARRIVAL_TIMES.length; i++) {
            if (clock == ARRIVAL_TIMES[i]) {
                try { createProcess(i + 1, PROGRAM_FILES[i]); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }

        // pick a new process to run
        if (running == null || running.getState() == State.FINISHED
                || running.getState() == State.BLOCKED) {
            PCB next = scheduler.schedule(clock);
            if (next != null) {
                boolean loaded = swap.ensureLoaded(next, allPCBs);
                if (!loaded) {
                    System.out.println("[OS] Could not load Process " + next.getProcessID());
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

        // make sure the process is in memory
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

        boolean ok = interpreter.executeNextInstruction(runningProcess, clock);

        // sync from memory to PCB
        if (memory.isLoaded(running.getProcessID())) {
            running.syncFromMemory(memory);
        }
        
        gui.updateMemory(getMemoryString());
        gui.updateReadyQueue(getReadyQueueString());
        gui.updateBlockedQueues(getBlockedQueuesString());

        if (!ok) {
            if (running.getState() == State.FINISHED) {
                scheduler.onComplete(running, clock);
                if (memory.isLoaded(running.getProcessID())) {
                    memory.deallocate(running.getProcessID());
                }
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " FINISHED.");
                running = null;
            } else if (running.getState() == State.BLOCKED) {
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " BLOCKED.");
                running = null;
            }
        } else {
            // check if the time quantum expired
            boolean preempted = scheduler.tick(running, clock);
            if (preempted) {
                gui.appendDiskLog("[t=" + clock + "] Process " + running.getProcessID() + " preempted.");
                running = null;
            }
        }

        if (allDone()) {
            simDone = true;
            gui.onSimulationComplete();
            System.out.println("\n[OS] All processes finished at clock = " + clock);
        }

        clock++;
    }

    static void createProcess(int pid, String programFile) throws Exception {
        List<String> instructions = loadInstructions(programFile);
        if (instructions.isEmpty()) {
            System.out.println("[OS] Could not load " + programFile);
            return;
        }

        int burstTime = instructions.size();
        PCB pcb = new PCB(pid, clock, burstTime, burstTime);

        int required = Memory.FIXED_OVERHEAD + instructions.size();
        int base = memory.allocate(pid, instructions);
        int attempts = 0;
        while (base == -1 && attempts < allPCBs.size()) {
            boolean freed = swap.freeSpaceFor(required, allPCBs);
            if (!freed) break;
            base = memory.allocate(pid, instructions);
            attempts++;
        }

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

    // load instructions from file
    static List<String> loadInstructions(String fileName) {
        List<String> lines = new ArrayList<>();

        File f = new File(fileName);
        System.out.println("[OS] Looking for file: " + f.getAbsolutePath());
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

        // reads from zip
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

    static Process getProcess(int pid) {
        for (Process p : allProcesses) {
            if (p.getProcessID() == pid) return p;
        }
        return null;
    }

    static boolean allDone() {
        if (clock <= ARRIVAL_TIMES[ARRIVAL_TIMES.length - 1]) return false;
        if (allPCBs.isEmpty()) return false;
        for (PCB pcb : allPCBs) {
            if (pcb.getState() != State.FINISHED) return false;
        }
        return true;
    }

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

    // everything in memory
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
