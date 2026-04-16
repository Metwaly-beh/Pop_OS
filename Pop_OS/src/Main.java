import java.util.ArrayList;
import java.util.List;

public class Main {
    static Memory memory = new Memory();
    static Disk disk = new Disk();
    static Swap swap = new Swap(memory, disk);
    static List<PCB> allProcesses = new ArrayList<>();
    public static Semaphore userInputSem = new Semaphore("userInput");
public static Semaphore userOutputSem = new Semaphore("userOutput");
public static Semaphore fileSem = new Semaphore("file");
public static GUI gui;


public static Semaphore getSemaphore(String resourceName) {
    switch (resourceName) {
        case "userInput": return userInputSem;
        case "userOutput": return userOutputSem;
        case "file": return fileSem;
        default: return null;
    }
}

public static void addToGlobalBlockedQueue(Process p) {
    SchedulerChooser.getCurrentScheduler().addToBlockedQueue(p);
}

public static void addToReadyQueue(Process p) {
    SchedulerChooser.getCurrentScheduler().addToReadyQueue(p);
}

public static String getBlockedQueuesString() {
    StringBuilder sb = new StringBuilder();
    sb.append("UserInput: ").append(userInputSem.getBlockedQueue()).append("\n");
    sb.append("UserOutput: ").append(userOutputSem.getBlockedQueue()).append("\n");
    sb.append("File: ").append(fileSem.getBlockedQueue()).append("\n");
    sb.append("General: ").append(SchedulerChooser.getCurrentScheduler().getGeneralBlockedQueue());
    return sb.toString();
}
    
    public static void main(String[] args) {
        gui = new GUI();
         }


    
}
