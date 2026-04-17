import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class Disk {

    private static final String SWAP_DIR    = "./swap/";
    private static final String SWAP_SUFFIX = ".swap";

    public Disk() {
        new File(SWAP_DIR).mkdirs();
    }

   
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