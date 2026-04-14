public class Main {
    static Memory memory = new Memory();
    static DiskManager disk = new DiskManager();
    static SwapManager swapManager = new SwapManager(memory, disk);
    static List<PCB> allProcesses = new ArrayList<>();

    public static void main(String[] args) {
         }
}
