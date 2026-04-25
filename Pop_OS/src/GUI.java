import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class GUI extends JFrame {

    private JLabel runningProcessLabel;
    private JLabel currentInstructionLabel;
    private JLabel clockLabel;
    private JTextArea readyQueueArea;
    private JTextArea blockedQueuesArea;
    private JTextArea memoryArea;
    private JTextArea diskLogArea;
    private JTextArea terminalArea;
    private JTextArea processOutputArea;
    private JButton startButton;
    private JButton pauseButton;
    private JButton stepButton;

    private Runnable onStart;
    private Runnable onPause;
    private Runnable onStep;

    public GUI() {
        initializeUI();
        redirectSystemStreams();
    }

    private void initializeUI() {
        setTitle("CSEN 602 OS Simulator - Spring 2026");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        clockLabel = new JLabel("Clock: 0");
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        runningProcessLabel = new JLabel("Running: None");
        runningProcessLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        currentInstructionLabel = new JLabel("Instruction: -");
        currentInstructionLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        topPanel.add(clockLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(runningProcessLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(currentInstructionLabel);
        add(topPanel, BorderLayout.NORTH);

        // ── Centre: ready queue | blocked queues | process output ─────────────
        JPanel queuesPanel = new JPanel(new GridLayout(1, 3, 6, 0));

        JPanel readyPanel = new JPanel(new BorderLayout());
        readyPanel.setBorder(BorderFactory.createTitledBorder("Ready Queue"));
        readyQueueArea = new JTextArea();
        readyQueueArea.setEditable(false);
        readyQueueArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        readyPanel.add(new JScrollPane(readyQueueArea), BorderLayout.CENTER);
        queuesPanel.add(readyPanel);

        JPanel blockedPanel = new JPanel(new BorderLayout());
        blockedPanel.setBorder(BorderFactory.createTitledBorder("Blocked Queues"));
        blockedQueuesArea = new JTextArea();
        blockedQueuesArea.setEditable(false);
        blockedQueuesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        blockedPanel.add(new JScrollPane(blockedQueuesArea), BorderLayout.CENTER);
        queuesPanel.add(blockedPanel);

        // dedicated panel — only OUTPUT lines and input prompts appear here
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Process Output"));
        processOutputArea = new JTextArea();
        processOutputArea.setEditable(false);
        processOutputArea.setFont(new Font("Monospaced", Font.BOLD, 13));
        processOutputArea.setBackground(new Color(0, 30, 50));
        processOutputArea.setForeground(new Color(100, 220, 255));
        outputPanel.add(new JScrollPane(processOutputArea), BorderLayout.CENTER);
        queuesPanel.add(outputPanel);

        add(queuesPanel, BorderLayout.CENTER);

        // ── Bottom: memory | disk log | terminal log ──────────────────────────
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 6, 0));

        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory (40 words)"));
        memoryArea = new JTextArea();
        memoryArea.setEditable(false);
        memoryArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        memoryPanel.add(new JScrollPane(memoryArea), BorderLayout.CENTER);
        bottomPanel.add(memoryPanel);

        JPanel diskPanel = new JPanel(new BorderLayout());
        diskPanel.setBorder(BorderFactory.createTitledBorder("Disk Swap Log"));
        diskLogArea = new JTextArea();
        diskLogArea.setEditable(false);
        diskLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        diskPanel.add(new JScrollPane(diskLogArea), BorderLayout.CENTER);
        bottomPanel.add(diskPanel);

        JPanel terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setBorder(BorderFactory.createTitledBorder("Terminal Log"));
        terminalArea = new JTextArea();
        terminalArea.setEditable(false);
        terminalArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        terminalArea.setBackground(new Color(20, 20, 20));
        terminalArea.setForeground(new Color(180, 255, 180));
        terminalArea.setCaretColor(Color.GREEN);
        terminalPanel.add(new JScrollPane(terminalArea), BorderLayout.CENTER);
        bottomPanel.add(terminalPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // ── Control buttons ───────────────────────────────────────────────────
        JPanel controlPanel = new JPanel();
        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        stepButton  = new JButton("⏭ Step");

        pauseButton.setEnabled(false);
        stepButton.setEnabled(true);

        startButton.addActionListener(e -> {
            if (onStart != null) onStart.run();
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stepButton.setEnabled(false);
        });

        pauseButton.addActionListener(e -> {
            if (onPause != null) onPause.run();
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stepButton.setEnabled(true);
        });

        stepButton.addActionListener(e -> {
            if (onStep != null) onStep.run();
        });

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stepButton);
        add(controlPanel, BorderLayout.PAGE_END);

        setVisible(true);
    }

    // ── Stream redirect ───────────────────────────────────────────────────────
    // Every System.out/err line goes to the terminal log.
    // Lines containing "OUTPUT]:" or "Please enter" also go to the output panel.
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                buffer.append((char) b);
                if ((char) b == '\n') flush();
            }

            @Override
            public void write(byte[] b, int off, int len) {
                buffer.append(new String(b, off, len, StandardCharsets.UTF_8));
                if (buffer.indexOf("\n") >= 0) flush();
            }

            @Override
            public void flush() {
                final String text = buffer.toString();
                buffer.setLength(0);
                if (text.isEmpty()) return;
                SwingUtilities.invokeLater(() -> {
                    terminalArea.append(text);
                    terminalArea.setCaretPosition(terminalArea.getDocument().getLength());

                    if (text.contains("OUTPUT]:") || text.contains("Please enter")) {
                        processOutputArea.append(text);
                        processOutputArea.setCaretPosition(
                                processOutputArea.getDocument().getLength());
                    }
                });
            }
        };

        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    // ── Input dialog (replaces Scanner in SystemCalls) ────────────────────────
    public static String promptUserInput(String prompt) {
        final String[] result = {""};
        try {
            SwingUtilities.invokeAndWait(() -> {
                String input = JOptionPane.showInputDialog(
                        null, prompt,
                        "Process Input Required",
                        JOptionPane.QUESTION_MESSAGE);
                result[0] = (input != null) ? input.trim() : "";
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }

    // ── Public update methods called from Main ────────────────────────────────

    public void setOnStart(Runnable r) { this.onStart = r; }
    public void setOnPause(Runnable r) { this.onPause = r; }
    public void setOnStep(Runnable r)  { this.onStep  = r; }

    public void updateClock(int clock) {
        SwingUtilities.invokeLater(() -> clockLabel.setText("Clock: " + clock));
    }

    public void updateRunningProcess(int pid, String instruction) {
        SwingUtilities.invokeLater(() -> {
            runningProcessLabel.setText("Running: " + (pid == -1 ? "None" : "P" + pid));
            currentInstructionLabel.setText("Instruction: " + instruction);
        });
    }

    public void updateReadyQueue(String queueStr) {
        SwingUtilities.invokeLater(() -> readyQueueArea.setText(queueStr));
    }

    public void updateBlockedQueues(String blockedInfo) {
        SwingUtilities.invokeLater(() -> blockedQueuesArea.setText(blockedInfo));
    }

    public void updateMemory(String memoryDump) {
        SwingUtilities.invokeLater(() -> memoryArea.setText(memoryDump));
    }

    public void appendDiskLog(String message) {
        SwingUtilities.invokeLater(() -> diskLogArea.append(message + "\n"));
    }

    public void onSimulationComplete() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stepButton.setEnabled(false);
            runningProcessLabel.setText("Running: None");
            currentInstructionLabel.setText("Instruction: Simulation Complete ✓");
            processOutputArea.append("\n── Simulation Complete ──\n");
        });
    }
}