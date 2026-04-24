import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class HRRNScheduler implements Scheduler {

    private final List<PCB> readyQueue;

    public HRRNScheduler() {
        readyQueue = new ArrayList<>();
    }

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

        // update waiting times 
        for (PCB pcb : readyQueue) {
            pcb.accumulateWaiting(currentTime);
        }

        // find the process with the highest response ratio
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

   
    @Override
    public boolean tick(PCB running, int currentTime) {
        
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

    // prints the ready queue 
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

    public List<PCB> getReadyQueue() {
        return readyQueue;
    }

    public boolean isEmpty() {
        return readyQueue.isEmpty();
    }
}
