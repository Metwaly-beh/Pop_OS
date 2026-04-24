import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Disk {

    //store the swap files
    private static final String SWAP_DIR    = "./swap/";
    private static final String SWAP_SUFFIX = ".swap";

    public Disk() {
        // make the swap folder
        new File(SWAP_DIR).mkdirs();
    }

    // saves the process snapshot to a file
    public void writeToDisk(int processID, List<String> snapshot) {
        String path = getPath(processID);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (String line : snapshot) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("[Disk] Process " + processID
                    + " written to disk at: " + path);
        } catch (IOException e) {
            System.err.println("[Disk] ERROR writing Process " + processID
                    + " to disk: " + e.getMessage());
        }
    }

    // reads back the process snapshot
    public List<String> readFromDisk(int processID) {
        String path = getPath(processID);
        List<String> snapshot = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
               
                if (!line.trim().isEmpty()) {
                    snapshot.add(line);
                }
            }
            System.out.println("[Disk] Process " + processID
                    + " read back from disk: " + path);
        } catch (FileNotFoundException e) {
            System.err.println("[Disk] ERROR: Swap file not found for Process "
                    + processID + " at: " + path);
        } catch (IOException e) {
            System.err.println("[Disk] ERROR reading Process " + processID
                    + " from disk: " + e.getMessage());
        }
        return snapshot;
    }

    
    public void deleteFromDisk(int processID) {
        File file = new File(getPath(processID));
        if (file.exists()) {
            file.delete();
            System.out.println("[Disk] Swap file deleted for Process " + processID);
        }
    }

    // checks if a process has been swapped 
    public boolean isOnDisk(int processID) {
        return new File(getPath(processID)).exists();
    }

    
    public void displayDiskContents(int processID) {
        if (!isOnDisk(processID)) {
            System.out.println("[Disk] No swap file found for Process " + processID);
            return;
        }
        System.out.println("\n[Disk] Contents of swap file for Process " + processID + ":");
        System.out.println("-----------------------------------------------------");
        List<String> snapshot = readFromDisk(processID);
        for (String line : snapshot) {
            String[] parts = line.split("\\|", 2);
            String addr  = parts.length > 0 ? parts[0] : "?";
            String value = parts.length > 1 ? parts[1] : "null";
            System.out.printf("  [%s] %s%n", addr, value);
        }
        System.out.println("-----------------------------------------------------\n");
    }

    
    private String getPath(int processID) {
        return SWAP_DIR + "process_" + processID + SWAP_SUFFIX;
    }
}
