import java.util.LinkedList;
import java.util.Queue;


public class RRScheduler implements Scheduler {

    private final Queue<PCB> readyQueue;
    private int quantum;              
    private int instructionsExecuted;  
    public RRScheduler(int quantum) {
        this.readyQueue           = new LinkedList<>();
        this.quantum              = quantum;
        this.instructionsExecuted = 0;
    }

    //  quantum=2
    public RRScheduler() {
        this(2);
    }

    @Override
    public void addProcess(PCB pcb, int currentTime) {
        pcb.setState(State.READY);
        pcb.setLastReadyTime(currentTime);
        readyQueue.add(pcb);
        System.out.printf("[RR][t=%d] Process %d added to Ready Queue.%n",
                currentTime, pcb.getProcessID());
    }

    @Override
    public PCB schedule(int currentTime) {
        if (readyQueue.isEmpty()) return null;

        // reset the instruction counter for the new process
        instructionsExecuted = 0;
        PCB chosen = readyQueue.poll();
        chosen.setState(State.RUNNING);
        System.out.printf("[RR][t=%d] Scheduled Process %d  (quantum=%d)%n",
                currentTime, chosen.getProcessID(), quantum);
        printQueues();
        return chosen;
    }

    //checks if the quantum is expired
    @Override
    public boolean tick(PCB running, int currentTime) {
        if (running == null) return false;

        instructionsExecuted++;

        if (instructionsExecuted >= quantum) {
            System.out.printf("[RR][t=%d] Process %d quantum expired (%d instructions).%n",
                    currentTime, running.getProcessID(), quantum);
            preempt(running, currentTime);
            return true;
        }
        return false;
    }

    @Override
    public void onBlock(PCB pcb, int currentTime) {
        pcb.setState(State.BLOCKED);
        instructionsExecuted = 0;
        System.out.printf("[RR][t=%d] Process %d BLOCKED.%n",
                currentTime, pcb.getProcessID());
        printQueues();
    }

    @Override
    public void onComplete(PCB pcb, int currentTime) {
        pcb.setState(State.FINISHED);
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
            for (PCB pcb : readyQueue) {
                System.out.printf("  │  PID=%-2d  State=%-8s  PC=%d%n",
                        pcb.getProcessID(), pcb.getState(), pcb.getProgramCounter());
            }
        }
        System.out.println("  └────────────────────────────────────────────────");
    }

    @Override
    public String getName() { return "RR"; }

    // puts the process at the back of the queue
    private void preempt(PCB pcb, int currentTime) {
        pcb.setState(State.READY);
        pcb.setLastReadyTime(currentTime);
        readyQueue.add(pcb);
        instructionsExecuted = 0;
        printQueues();
    }

    public int  getQuantum()               { return quantum; }
    public void setQuantum(int quantum)    { this.quantum = quantum; }

    public Queue<PCB> getReadyQueue() { return readyQueue; }

    public boolean isEmpty() { return readyQueue.isEmpty(); }
}
