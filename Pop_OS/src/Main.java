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

    public static void main(String[] args) {
        GUI gui = new GUI(this); 
         }

    public static Semaphore getSemaphore(String resourceName) {
    switch (resourceName) {
        case "userInput": return userInputSem;
        case "userOutput": return userOutputSem;
        case "file": return fileSem;
        default: return null;
    }
}
    
}
