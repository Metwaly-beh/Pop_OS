import java.util.LinkedList;
import java.util.Queue;


public class Semaphore {
    private final String resourceName;
    private int value;
    private final Queue<Process> blockedQueue;  // processes waiting for this resource
    private final Scheduler scheduler;

    public Semaphore(String name, Scheduler scheduler) {
        this.resourceName = name;
        this.value        = 1;   
        this.blockedQueue = new LinkedList<>();
        this.scheduler    = scheduler;
    }

    // tries to acquire the semaphore
    public boolean semWait(Process p, int currentTime) {
        if (value == 1) {
            value = 0;   //take the process
            return true;
        } else {
            // resource is taken, block the process
            blockedQueue.add(p);
            scheduler.onBlock(p.getPCB(), currentTime);
            return false;
        }
    }

    // releases the semaphore
    public void semSignal(Process p, int currentTime) {
        if (blockedQueue.isEmpty()) {
            value = 1;   // no one waiting, just unlock
        } else {
           
            Process next = blockedQueue.poll();
            scheduler.addProcess(next.getPCB(), currentTime);
        }
    }

    public String getResourceName()          { return resourceName; }
    public Queue<Process> getBlockedQueue()  { return blockedQueue; }
    public boolean isLocked()                { return value == 0; }
}
