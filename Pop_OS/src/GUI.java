import javax.swing.*;
import java.awt.*;

// the main GUI window for our OS simulator
// shows running process, queues, memory, disk log, and control buttons
public class GUI extends JFrame {

    private JLabel runningProcessLabel;
    private JLabel currentInstructionLabel;
    private JLabel clockLabel;
    private JTextArea readyQueueArea;
    private JTextArea blockedQueuesArea;
    private JTextArea memoryArea;
    private JTextArea diskLogArea;
    private JButton startButton;
    private JButton pauseButton;
    private JButton stepButton;

    // callbacks for the buttons, set from Main
    private Runnable onStart;
    private Runnable onPause;
    private Runnable onStep;

    public GUI() {
        initializeUI();
    }

    // builds all the panels and wires up the buttons
    private void initializeUI() {
        setTitle("CSEN 602 OS Simulator - Spring 2026");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // top bar shows clock, running process, and current instruction
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

        // middle section: ready queue on left, blocked queues on right
        JPanel queuesPanel = new JPanel(new GridLayout(1, 2, 10, 0));

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

        add(queuesPanel, BorderLayout.CENTER);

        // bottom section: memory dump on left, disk swap log on right
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));

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

        add(bottomPanel, BorderLayout.SOUTH);

        // control buttons at the bottom
        JPanel controlPanel = new JPanel();
        startButton = new JButton("▶ Start");
        pauseButton = new JButton("⏸ Pause");
        stepButton  = new JButton("⏭ Step");

        // pause is disabled at start, step is enabled so we can go one tick at a time
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

    // these are set by Main to connect the buttons to the simulation logic
    public void setOnStart(Runnable r) { this.onStart = r; }
    public void setOnPause(Runnable r) { this.onPause = r; }
    public void setOnStep(Runnable r)  { this.onStep  = r; }

    // update the clock display
    public void updateClock(int clock) {
        SwingUtilities.invokeLater(() -> clockLabel.setText("Clock: " + clock));
    }

    // update which process is running and what instruction it's on
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

    // append a message to the disk log, doesnt clear previous messages
    public void appendDiskLog(String message) {
        SwingUtilities.invokeLater(() -> diskLogArea.append(message + "\n"));
    }

    // called when simulation finishes, disables all buttons
    public void onSimulationComplete() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stepButton.setEnabled(false);
            runningProcessLabel.setText("Running: None");
            currentInstructionLabel.setText("Instruction: Simulation Complete");
        });
    }
}
