/**
 * Common interface for all scheduling algorithms.
 *
 * The OS kernel (or a simulation driver) calls these methods:
 *   1. addProcess()   – when a new process arrives or unblocks
 *   2. schedule()     – to pick the next process to run
 *   3. tick()         – once per clock cycle, to update waiting times / quanta
 *   4. onBlock()      – when a running process gets blocked on a mutex
 *   5. onComplete()   – when a running process finishes
 *   6. printQueues()  – after every scheduling event (required by spec)
 */
public interface Scheduler {

    /** Enqueue a process into the ready queue. */
    void addProcess(PCB pcb, int currentTime);

    /**
     * Choose the next process to run.
     *
     * @param currentTime current clock tick
     * @return the selected PCB, or null if the ready queue is empty
     */
    PCB schedule(int currentTime);

    /**
     * Called every clock tick.
     * Preemptive schedulers use this to check if the time-slice expired.
     *
     * @param running     the currently running PCB (may be null)
     * @param currentTime current clock tick
     * @return true if the current process should be preempted now
     */
    boolean tick(PCB running, int currentTime);

    /** Called when the running process gets blocked on a resource. */
    void onBlock(PCB pcb, int currentTime);

    /** Called when the running process has finished all its instructions. */
    void onComplete(PCB pcb, int currentTime);

    /** Print ready queue (and any sub-queues) in human-readable form. */
    void printQueues();

    /** Human-readable algorithm name, used in output headers. */
    String getName();
}
 