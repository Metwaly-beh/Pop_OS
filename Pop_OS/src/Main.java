import java.util.ArrayList;
import java.util.List;

public class Main {
    static Memory memory = new Memory();
    static Disk disk = new Disk();
    static Swap swap = new Swap(memory, disk);
    static List<PCB> allProcesses = new ArrayList<>();

    public static void main(String[] args) {
         }
}
