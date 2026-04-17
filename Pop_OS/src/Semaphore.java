import java.util.LinkedList;
import java.util.Queue;

public class Semaphore {
    private final String resourceName;
    private int value;
    private final Queue<Process> blockedQueue;
    private final Scheduler scheduler;

    public Semaphore(String name, Scheduler scheduler) {
        this.resourceName = name;
        this.value        = 1;
        this.blockedQueue = new LinkedList<>();
        this.scheduler    = scheduler;
    }

    public boolean semWait(Process p, int currentTime) {
        if (value == 1) {
            value = 0;
            return true;
        } else {
            blockedQueue.add(p);
            scheduler.onBlock(p.getPCB(), currentTime);
            return false;
        }
    }

    public void semSignal(Process p, int currentTime) {
        if (blockedQueue.isEmpty()) {
            value = 1;
        } else {
            Process next = blockedQueue.poll();
            scheduler.addProcess(next.getPCB(), currentTime);
        }
    }

    public String getResourceName()          { return resourceName; }
    public Queue<Process> getBlockedQueue()  { return blockedQueue; }
    public boolean isLocked()                { return value == 0; }
}