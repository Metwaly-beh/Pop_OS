import java.io.*;
import java.util.Scanner;

public class SystemCalls {

    private final Memory  memory;
    private final Scanner userInputScanner;

    public SystemCalls(Memory memory) {
        this.memory           = memory;
        this.userInputScanner = new Scanner(System.in);
    }

    public void print(int processID, String value) {
        System.out.println("[Process " + processID + " OUTPUT]: " + value);
    }

    public void assign(int processID, String varName, String valueOrKeyword) {
        String actualValue;
        if (valueOrKeyword.equalsIgnoreCase("input")) {
            System.out.print("[Process " + processID + "]: Please enter a value: ");
            actualValue = userInputScanner.nextLine().trim();
        } else {
            actualValue = valueOrKeyword;
        }
        memory.setVariable(processID, varName, actualValue);
        System.out.println("[Process " + processID + "]: assign " + varName + " = " + actualValue);
    }

    public void writeFile(int processID, String fileName, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            writer.write(data);
            System.out.println("[Process " + processID + "]: writeFile -> '" + fileName + "'");
        } catch (IOException e) {
            System.err.println("[Process " + processID + "] ERROR writeFile '" + fileName + "': " + e.getMessage());
        }
    }

    public String readFile(int processID, String fileName) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            System.out.println("[Process " + processID + "]: readFile <- '" + fileName + "'");
        } catch (IOException e) {
            System.err.println("[Process " + processID + "] ERROR readFile '" + fileName + "': " + e.getMessage());
            return "";
        }
        return content.toString().trim();
    }

    public void printFromTo(int processID, int from, int to) {
        System.out.print("[Process " + processID + " OUTPUT]: ");
        for (int i = from; i <= to; i++) {
            System.out.print(i);
            if (i < to) System.out.print(", ");
        }
        System.out.println();
    }
}