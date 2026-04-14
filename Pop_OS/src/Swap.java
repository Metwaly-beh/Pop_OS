import java.util.List;


public class Swap {

    private final Memory memory;
    private final Disk   disk;

    public Swap(Memory memory, Disk disk) {
        this.memory = memory;
        this.disk   = disk;
    }

    
    public boolean freeSpaceFor(int requiredWords, List<PCB> allPCBs) {
        PCB vic = pickVic(allPCBs);

        if (vic == null) {
            System.out.println("[Swap] No eligible victim found to evict.");
            return false;
        }

        System.out.println("[Swap] Evicting Process " + vic.processID
                + " to free " + requiredWords + " words.");

        
        List<String> snapshot = memory.swapOut(vic.processID);
        disk.writeToDisk(vic.processID, snapshot);

        vic.lowerBound = -1;
        vic.upperBound = -1;

        System.out.println("[Swap] Process " + vic.processID + " swapped OUT.");
        disk.displayDiskContents(vic.processID);

        return memory.getFreeWords() >= requiredWords;
    }

    
    public boolean ensureLoaded(PCB pcb, List<PCB> allPCBs) {
        if (memory.isLoaded(pcb.processID)) {
            return true;
        }

        if (!disk.isOnDisk(pcb.processID)) {
            System.err.println("[Swap] ERROR: Process " + pcb.processID
                    + " is neither in memory nor on disk.");
            return false;
        }

        System.out.println("[Swap] Process " + pcb.processID
                + " needs to be swapped IN from disk.");

        List<String> snapshot = disk.readFromDisk(pcb.processID);
        int required = snapshot.size();

        while (memory.getFreeWords() < required) {
            boolean freed = freeSpaceFor(required, allPCBs);
            if (!freed) {
                System.out.println("[Swap] Could not free enough space to swap in Process "
                        + pcb.processID);
                return false;
            }
        }

        int newBase = memory.swapIn(pcb.processID, snapshot);
        if (newBase == -1) {
            System.out.println("[Swap] swapIn failed for Process " + pcb.processID);
            return false;
        }

        pcb.lowerBound = memory.getLowerBound(pcb.processID);
        pcb.upperBound = memory.getUpperBound(pcb.processID);

        disk.deleteFromDisk(pcb.processID);

        System.out.println("[Swap] Process " + pcb.processID + " swapped IN successfully.");
        return true;
    }

 
    private PCB pickVic(List<PCB> allPCBs) {
        PCB vic = null;
        int largestSize = -1;

        for (PCB pcb : allPCBs) {
            // Only evict READY processes that are currently in memory
            if (pcb.state != State.READY) continue;
            if (!memory.isLoaded(pcb.processID)) continue;

            int size = pcb.upperBound - pcb.lowerBound + 1;
            if (size > largestSize) {
                largestSize = size;
                vic = pcb;
            }
        }

        return vic;
    }
}