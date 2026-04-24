import java.util.LinkedList;
import java.util.Queue;


public class MLFQScheduler implements Scheduler {

    public static final int NUM_LEVELS = 4;

    
    @SuppressWarnings("unchecked")
    private final Queue<PCB>[] queues = new Queue[NUM_LEVELS];

    
    private final int[] quanta = new int[NUM_LEVELS];

    
    private int instructionsExecuted;

    
    private int currentLevel;

    public MLFQScheduler() {
        for (int i = 0; i < NUM_LEVELS; i++) {
            queues[i] = new LinkedList<>();
            quanta[i] = (int) Math.pow(2, i);
        }
        instructionsExecuted = 0;
        currentLevel = 0;
    }

    @Override
    public void addProcess(PCB pcb, int currentTime) {
        // new processes go to level 0, unblocked processes go back to their level
        int level = (pcb.getState() == State.NEW) ? 0 : pcb.getMlfqLevel();
        pcb.setMlfqLevel(level);
        pcb.setState(State.READY);
        pcb.setLastReadyTime(currentTime);
        queues[level].add(pcb);
        System.out.printf("[MLFQ][t=%d] Process %d added to Queue[%d] (quantum=%d).%n",
                currentTime, pcb.getProcessID(), level, quanta[level]);
    }

    @Override
    public PCB schedule(int currentTime) {
        // pick from the highest priority 
        for (int level = 0; level < NUM_LEVELS; level++) {
            if (!queues[level].isEmpty()) {
                instructionsExecuted = 0;
                currentLevel = level;
                PCB chosen = queues[level].poll();
                chosen.setState(State.RUNNING);
                System.out.printf(
                        "[MLFQ][t=%d] Scheduled Process %d from Queue[%d] (quantum=%d).%n",
                        currentTime, chosen.getProcessID(), level, quanta[level]);
                printQueues();
                return chosen;
            }
        }
        return null;
    }

    // called every instruction
    @Override
    public boolean tick(PCB running, int currentTime) {
        if (running == null) return false;

        instructionsExecuted++;
        int quantum = quanta[currentLevel];

        if (instructionsExecuted >= quantum) {
            System.out.printf("[MLFQ][t=%d] Process %d quantum expired at Queue[%d].%n",
                    currentTime, running.getProcessID(), currentLevel);
            demoteAndEnqueue(running, currentTime);
            return true;
        }
        return false;
    }

    @Override
    public void onBlock(PCB pcb, int currentTime) {
        pcb.setState(State.BLOCKED);
        instructionsExecuted = 0;
        System.out.printf("[MLFQ][t=%d] Process %d BLOCKED (was at Queue[%d]).%n",
                currentTime, pcb.getProcessID(), pcb.getMlfqLevel());
        printQueues();
    }

    @Override
    public void onComplete(PCB pcb, int currentTime) {
        pcb.setState(State.FINISHED);
        instructionsExecuted = 0;
        System.out.printf("[MLFQ][t=%d] Process %d FINISHED (was at Queue[%d]).%n",
                currentTime, pcb.getProcessID(), pcb.getMlfqLevel());
        printQueues();
    }

    // prints all 4 queues with their process info
    @Override
    public void printQueues() {
        System.out.println("  ┌─ MLFQ Queues ──────────────────────────────────────");
        for (int i = 0; i < NUM_LEVELS; i++) {
            String rrTag = (i == NUM_LEVELS - 1) ? " [RR]" : "";
            System.out.printf("  │  Queue[%d] quantum=%-2d%s  (%d process(es)):%n",
                    i, quanta[i], rrTag, queues[i].size());
            if (queues[i].isEmpty()) {
                System.out.println("  │      (empty)");
            } else {
                for (PCB pcb : queues[i]) {
                    System.out.printf("  │      PID=%-2d  PC=%d/%d%n",
                            pcb.getProcessID(),
                            pcb.getProgramCounter(),
                            pcb.getTotalInstructions());
                }
            }
        }
        System.out.println("  └────────────────────────────────────────────────────");
    }

    @Override
    public String getName() { return "MLFQ"; }

    // moves the process down one level and puts it at the back of that queue
    
    private void demoteAndEnqueue(PCB pcb, int currentTime) {
        int nextLevel = Math.min(currentLevel + 1, NUM_LEVELS - 1);
        pcb.setMlfqLevel(nextLevel);
        pcb.setState(State.READY);
        pcb.setLastReadyTime(currentTime);
        queues[nextLevel].add(pcb);
        instructionsExecuted = 0;
        System.out.printf("[MLFQ][t=%d] Process %d demoted to Queue[%d].%n",
                currentTime, pcb.getProcessID(), nextLevel);
        printQueues();
    }

    public Queue<PCB> getQueue(int level) {
        if (level < 0 || level >= NUM_LEVELS)
            throw new IllegalArgumentException("Invalid MLFQ level: " + level);
        return queues[level];
    }

    public int getQuantum(int level) { return quanta[level]; }

    public boolean isEmpty() {
        for (Queue<PCB> q : queues)
            if (!q.isEmpty()) return false;
        return true;
    }
}
