
public interface Scheduler {

   
    void addProcess(PCB pcb, int currentTime);

    
    PCB schedule(int currentTime);

    //check if the running process is preempted
    boolean tick(PCB running, int currentTime);

    
    void onBlock(PCB pcb, int currentTime);

    
    void onComplete(PCB pcb, int currentTime);

    
    void printQueues();

    
    String getName();
}
