import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    static Memory memory = new Memory();
    static Disk disk = new Disk();
    static Swap swap = new Swap(memory, disk);
    static List<PCB> allProcesses = new ArrayList<>();
    static Scheduler scheduler;
    static Map<String, Semaphore> semaphores = new HashMap<>();
    static GUI gui;

    public static void main(String[] args) {
        gui = new GUI();

        scheduler = SchedulerChooser.create("RR");

        semaphores.put("userInput",  new Semaphore("userInput",  scheduler));
        semaphores.put("userOutput", new Semaphore("userOutput", scheduler));
        semaphores.put("file",       new Semaphore("file",       scheduler));

        SystemCalls systemCalls = new SystemCalls(memory);
        Interpreter interpreter = new Interpreter(memory, systemCalls, semaphores);
    }

    public static String getBlockedQueuesString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Semaphore> entry : semaphores.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue().getBlockedQueue()).append("\n");
        }
        return sb.toString();
    }
}