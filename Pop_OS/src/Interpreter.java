public class Interpreter {

    private final Memory       memory;
    private final SystemCalls  systemCalls;
    private final MutexManager mutexManager;

    public Interpreter(Memory memory, SystemCalls systemCalls, MutexManager mutexManager) {
        this.memory       = memory;
        this.systemCalls  = systemCalls;
        this.mutexManager = mutexManager;
    }

    public boolean executeNextInstruction(int processID) {

        int pc = memory.getProgramCounter(processID);
        String rawLine = memory.getInstruction(processID, pc);

        if (rawLine == null) {
            memory.setState(processID, "FINISHED");
            System.out.println("[Process " + processID + "] FINISHED");
            return false;
        }

        rawLine = rawLine.trim();

        if (rawLine.isEmpty()) {
            memory.setProgramCounter(processID, pc + 1);
            return true;
        }

        System.out.println("[Process " + processID + " | PC=" + pc + "] Executing: " + rawLine);

        String[] tokens = rawLine.split("\\s+", 4);
        String   opcode = tokens[0].toLowerCase();

        switch (opcode) {

            case "print": {
                requireTokens(tokens, 2, rawLine);
                String value = resolveValue(processID, tokens[1]);
                systemCalls.print(processID, value);
                break;
            }

            case "assign": {
                requireTokens(tokens, 3, rawLine);
                String varName = tokens[1];
                String second  = tokens[2];

                if (second.equalsIgnoreCase("readFile")) {
                    requireTokens(tokens, 4, rawLine);
                    String fileName    = resolveValue(processID, tokens[3]);
                    String fileContent = systemCalls.readFile(processID, fileName);
                    memory.setVariable(processID, varName, fileContent);
                } else {
                    systemCalls.assign(processID, varName, second);
                }
                break;
            }

            case "writefile": {
                requireTokens(tokens, 3, rawLine);
                String fileName = resolveValue(processID, tokens[1]);
                String data     = resolveValue(processID, tokens[2]);
                systemCalls.writeFile(processID, fileName, data);
                break;
            }

            case "readfile": {
                requireTokens(tokens, 2, rawLine);
                String fileName    = resolveValue(processID, tokens[1]);
                String fileContent = systemCalls.readFile(processID, fileName);
                systemCalls.print(processID, fileContent);
                break;
            }

            case "printfromto": {
                requireTokens(tokens, 3, rawLine);
                int from = parseIntValue(processID, tokens[1], rawLine);
                int to   = parseIntValue(processID, tokens[2], rawLine);
                systemCalls.printFromTo(processID, from, to);
                break;
            }

            case "semwait": {
                requireTokens(tokens, 2, rawLine);
                String resource  = tokens[1];
                boolean acquired = mutexManager.semWait(resource, processID);
                if (!acquired) {
                    memory.setState(processID, "BLOCKED");
                    System.out.println("[Process " + processID + "] BLOCKED waiting for: " + resource);
                    return false;
                }
                break;
            }

            case "semsignal": {
                requireTokens(tokens, 2, rawLine);
                mutexManager.semSignal(tokens[1], processID);
                break;
            }

            default:
                System.err.println("[Interpreter] Unknown instruction: '" + rawLine + "'");
                break;
        }

        memory.setProgramCounter(processID, pc + 1);
        return true;
    }

    private String resolveValue(int processID, String token) {
        String memValue = memory.getVariable(processID, token);
        return (memValue != null) ? memValue : token;
    }

    private int parseIntValue(int processID, String token, String rawLine) {
        String resolved = resolveValue(processID, token);
        try {
            return Integer.parseInt(resolved.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("[Interpreter] Expected integer for '" + token
                + "' in: " + rawLine + " (resolved to '" + resolved + "')");
        }
    }

    private void requireTokens(String[] tokens, int count, String rawLine) {
        if (tokens.length < count) {
            throw new RuntimeException("[Interpreter] Malformed instruction (need "
                + count + " tokens): '" + rawLine + "'");
        }
    }
}