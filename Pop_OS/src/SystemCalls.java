import java.io.*;

public class SystemCalls {

    private final Memory memory;

    public SystemCalls(Memory memory) {
        this.memory = memory;
    }

    public void print(int processID, String value) {
        System.out.println("[Process " + processID + " OUTPUT]: " + value);
    }

    public void assign(int processID, String varName, String valueOrKeyword) {
        String actualValue;
        if (valueOrKeyword.equalsIgnoreCase("input")) {
            System.out.println("[Process " + processID + "]: Please enter a value for '" + varName + "':");
            actualValue = GUI.promptUserInput(
                    "Process " + processID + " requests input for variable '" + varName + "':");
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
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            sb.append(i);
            if (i < to) sb.append(", ");
        }
        System.out.println("[Process " + processID + " OUTPUT]: " + sb);
    }
}