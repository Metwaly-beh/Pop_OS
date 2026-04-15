import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Highest Response Ratio Next (HRRN) Scheduler — non-preemptive.
 *
 * At every scheduling decision the process with the highest response ratio
 * is selected:
 *      RR = (Waiting Time + Burst Time) / Burst Time
 *
 * Because HRRN is non-preemptive, tick() always returns false (no forced
 * preemption). A new scheduling decision only happens when the running
 * process finishes or blocks.
 */
public class HRRNScheduler implements Scheduler {

    private final List<PCB> readyQueue;

    public HRRNScheduler() {
        readyQueue = new ArrayList<>();
    }

    // ── Scheduler interface ──────────────────────────────────────────────────

    @Override
    public void addProcess(PCB pcb, int currentTime) {
        pcb.setState(State.READY);
        pcb.setLastReadyTime(currentTime);
        readyQueue.add(pcb);
        System.out.printf("[HRRN][t=%d] Process %d added to Ready Queue.%n",
                currentTime, pcb.getProcessID());
    }

    @Override
    public PCB schedule(int currentTime) {
        if (readyQueue.isEmpty()) return null;

        // Update waiting times for all ready processes before comparing ratios
        for (PCB pcb : readyQueue) {
            pcb.accumulateWaiting(currentTime);
        }

        // Pick the process with the highest response ratio
        PCB chosen = readyQueue.stream()
                .max(Comparator.comparingDouble(PCB::getResponseRatio))
                .orElse(null);

        if (chosen != null) {
            readyQueue.remove(chosen);
            chosen.setState(State.RUNNING);
            System.out.printf("[HRRN][t=%d] Scheduled Process %d  (RR=%.2f, Wait=%d, Burst=%d)%n",
                    currentTime, chosen.getProcessID(),
                    chosen.getResponseRatio(), chosen.getWaitingTime(), chosen.getBurstTime());
            printQueues();
        }
        return chosen;
    }

    /** HRRN is non-preemptive — never forces a preemption. */
    @Override
    public boolean tick(PCB running, int currentTime) {
        // Still accumulate waiting time for queued processes every tick
        for (PCB pcb : readyQueue) {
            // We accumulate lazily in schedule(); nothing extra needed here.
        }
        return false;
    }

    @Override
    public void onBlock(PCB pcb, int currentTime) {
        pcb.setState(State.BLOCKED);
        System.out.printf("[HRRN][t=%d] Process %d BLOCKED.%n",
                currentTime, pcb.getProcessID());
        printQueues();
    }

    @Override
    public void onComplete(PCB pcb, int currentTime) {
        pcb.setState(State.FINISHED);
        System.out.printf("[HRRN][t=%d] Process %d FINISHED.%n",
                currentTime, pcb.getProcessID());
        printQueues();
    }

    @Override
    public void printQueues() {
        System.out.println("  ┌─ HRRN Ready Queue (" + readyQueue.size() + " process(es)) ─────────────");
        if (readyQueue.isEmpty()) {
            System.out.println("  │  (empty)");
        } else {
            for (PCB pcb : readyQueue) {
                System.out.printf("  │  PID=%-2d  Wait=%-3d  Burst=%-3d  RR=%.2f%n",
                        pcb.getProcessID(), pcb.getWaitingTime(),
                        pcb.getBurstTime(), pcb.getResponseRatio());
            }
        }
        System.out.println("  └────────────────────────────────────────────────");
    }

    @Override
    public String getName() { return "HRRN"; }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public List<PCB> getReadyQueue() {
        return readyQueue;
    }

    public boolean isEmpty() {
        return readyQueue.isEmpty();
    }
}