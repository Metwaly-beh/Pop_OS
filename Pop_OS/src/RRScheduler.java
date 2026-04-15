import java.util.LinkedList;
import java.util.Queue;

/**
 * Round Robin (RR) Scheduler — preemptive.
 *
 * Each process receives a fixed quantum of instructions (default = 2, but
 * configurable as required by the spec: "Time slice is subject to change").
 *
 * When a process uses its full quantum it is moved to the back of the ready
 * queue and the next process is scheduled.
 */
public class RRScheduler implements Scheduler {

    private final Queue<ProcessControlBlock> readyQueue;
    private int quantum;               // instructions per time slice
    private int instructionsExecuted;  // instructions run in the current slice

    public RRScheduler(int quantum) {
        this.readyQueue           = new LinkedList<>();
        this.quantum              = quantum;
        this.instructionsExecuted = 0;
    }

    /** Convenience constructor using the spec default of 2. */
    public RRScheduler() {
        this(2);
    }

    // ── Scheduler interface ──────────────────────────────────────────────────

    @Override
    public void addProcess(ProcessControlBlock pcb, int currentTime) {
        pcb.setState(ProcessState.READY);
        pcb.setLastReadyTime(currentTime);
        readyQueue.add(pcb);
        System.out.printf("[RR][t=%d] Process %d added to Ready Queue.%n",
                currentTime, pcb.getProcessID());
    }

    @Override
    public ProcessControlBlock schedule(int currentTime) {
        if (readyQueue.isEmpty()) return null;

        instructionsExecuted = 0;     // reset slice counter for the new process
        ProcessControlBlock chosen = readyQueue.poll();
        chosen.setState(ProcessState.RUNNING);
        System.out.printf("[RR][t=%d] Scheduled Process %d  (quantum=%d)%n",
                currentTime, chosen.getProcessID(), quantum);
        printQueues();
        return chosen;
    }

    /**
     * Called after every instruction executed by the running process.
     *
     * @return true if the quantum has expired and the process should be
     *         preempted (put back at the end of the ready queue).
     */
    @Override
    public boolean tick(ProcessControlBlock running, int currentTime) {
        if (running == null) return false;

        instructionsExecuted++;

        if (instructionsExecuted >= quantum) {
            // Quantum expired — preempt
            System.out.printf("[RR][t=%d] Process %d quantum expired (%d instructions).%n",
                    currentTime, running.getProcessID(), quantum);
            preempt(running, currentTime);
            return true;
        }
        return false;
    }

    @Override
    public void onBlock(ProcessControlBlock pcb, int currentTime) {
        pcb.setState(ProcessState.BLOCKED);
        instructionsExecuted = 0;
        System.out.printf("[RR][t=%d] Process %d BLOCKED.%n",
                currentTime, pcb.getProcessID());
        printQueues();
    }

    @Override
    public void onComplete(ProcessControlBlock pcb, int currentTime) {
        pcb.setState(ProcessState.FINISHED);
        instructionsExecuted = 0;
        System.out.printf("[RR][t=%d] Process %d FINISHED.%n",
                currentTime, pcb.getProcessID());
        printQueues();
    }

    @Override
    public void printQueues() {
        System.out.println("  ┌─ RR Ready Queue (quantum=" + quantum
                + ", " + readyQueue.size() + " process(es)) ──────────");
        if (readyQueue.isEmpty()) {
            System.out.println("  │  (empty)");
        } else {
            for (ProcessControlBlock pcb : readyQueue) {
                System.out.printf("  │  PID=%-2d  State=%-8s  PC=%d%n",
                        pcb.getProcessID(), pcb.getState(), pcb.getProgramCounter());
            }
        }
        System.out.println("  └────────────────────────────────────────────────");
    }

    @Override
    public String getName() { return "RR"; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Move the running process to the back of the ready queue. */
    private void preempt(ProcessControlBlock pcb, int currentTime) {
        pcb.setState(ProcessState.READY);
        pcb.setLastReadyTime(currentTime);
        readyQueue.add(pcb);
        instructionsExecuted = 0;
        printQueues();
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int  getQuantum()               { return quantum; }
    public void setQuantum(int quantum)    { this.quantum = quantum; }

    public Queue<ProcessControlBlock> getReadyQueue() { return readyQueue; }

    public boolean isEmpty() { return readyQueue.isEmpty(); }
}