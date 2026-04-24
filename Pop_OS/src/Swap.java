import java.util.List;


public class Swap {

    private final Memory memory;
    private final Disk disk;

    public Swap(Memory memory, Disk disk) {
        this.memory = memory;
        this.disk = disk;
    }

    
    public boolean freeSpaceFor(int requiredWords, List<PCB> allPCBs) {
        PCB vic = pickVic(allPCBs);

        if (vic == null) {
            System.out.println("[Swap] No eligible victim found to evict.");
            return false;
        }

        System.out.println("[Swap] Evicting Process " + vic.getProcessID()
                + " to free " + requiredWords + " words.");

        List<String> snapshot = memory.swapOut(vic.getProcessID());
        disk.writeToDisk(vic.getProcessID(), snapshot);

        // mark bounds as invalid since process is no longer in memory
        vic.setLowerBound(-1);
        vic.setUpperBound(-1);

        System.out.println("[Swap] Process " + vic.getProcessID() + " swapped OUT.");
        disk.displayDiskContents(vic.getProcessID());

        return memory.getFreeWords() >= requiredWords;
    }

    
    public boolean ensureLoaded(PCB pcb, List<PCB> allPCBs) {
        if (memory.isLoaded(pcb.getProcessID())) {
            return true;  
        }

        if (!disk.isOnDisk(pcb.getProcessID())) {
            System.err.println("[Swap] ERROR: Process " + pcb.getProcessID()
                    + " is neither in memory nor on disk.");
            return false;
        }

        System.out.println("[Swap] Process " + pcb.getProcessID()
                + " needs to be swapped IN from disk.");

        List<String> snapshot = disk.readFromDisk(pcb.getProcessID());
        int required = snapshot.size();

        
        while (memory.getFreeWords() < required) {
            boolean freed = freeSpaceFor(required, allPCBs);
            if (!freed) {
                System.out.println("[Swap] Could not free enough space to swap in Process "
                        + pcb.getProcessID());
                return false;
            }
        }

        int newBase = memory.swapIn(pcb.getProcessID(), snapshot);
        if (newBase == -1) {
            System.out.println("[Swap] swapIn failed for Process " + pcb.getProcessID());
            return false;
        }

        
        pcb.setLowerBound(memory.getLowerBound(pcb.getProcessID()));
        pcb.setUpperBound(memory.getUpperBound(pcb.getProcessID()));

       
        disk.deleteFromDisk(pcb.getProcessID());

        System.out.println("[Swap] Process " + pcb.getProcessID() + " swapped IN successfully.");
        return true;
    }

    // picks the victim process to evict
    private PCB pickVic(List<PCB> allPCBs) {
        PCB vic = null;
        int largestSize = -1;

        for (PCB pcb : allPCBs) {
            if (pcb.getState() != State.READY && pcb.getState() != State.BLOCKED) continue;
            if (!memory.isLoaded(pcb.getProcessID())) continue;

            int size = pcb.getUpperBound() - pcb.getLowerBound() + 1;
            if (size > largestSize) {
                largestSize = size;
                vic = pcb;
            }
        }

        return vic;
    }
}
