import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    private JLabel runningProcessLabel;
    private JLabel currentInstructionLabel;
    private JTextArea readyQueueArea;
    private JTextArea blockedQueuesArea;
    private JTextArea memoryArea;
    private JTextArea diskLogArea;
    private JButton startButton;
    private JButton pauseButton;
    private JButton stepButton;

    public GUI() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("CSEN 602 OS Simulator - Spring 2026");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runningProcessLabel = new JLabel("Running: None");
        runningProcessLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        currentInstructionLabel = new JLabel("Instruction: -");
        currentInstructionLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        topPanel.add(runningProcessLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(currentInstructionLabel);
        add(topPanel, BorderLayout.NORTH);

        JPanel queuesPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel readyPanel = new JPanel(new BorderLayout());
        readyPanel.setBorder(BorderFactory.createTitledBorder("Ready Queue"));
        readyQueueArea = new JTextArea();
        readyQueueArea.setEditable(false);
        readyPanel.add(new JScrollPane(readyQueueArea), BorderLayout.CENTER);
        queuesPanel.add(readyPanel);

        JPanel blockedPanel = new JPanel(new BorderLayout());
        blockedPanel.setBorder(BorderFactory.createTitledBorder("Blocked Queues"));
        blockedQueuesArea = new JTextArea();
        blockedQueuesArea.setEditable(false);
        blockedPanel.add(new JScrollPane(blockedQueuesArea), BorderLayout.CENTER);
        queuesPanel.add(blockedPanel);
        add(queuesPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory (40 words)"));
        memoryArea = new JTextArea();
        memoryArea.setEditable(false);
        memoryPanel.add(new JScrollPane(memoryArea), BorderLayout.CENTER);
        bottomPanel.add(memoryPanel);

        JPanel diskPanel = new JPanel(new BorderLayout());
        diskPanel.setBorder(BorderFactory.createTitledBorder("Disk Swap Log"));
        diskLogArea = new JTextArea();
        diskLogArea.setEditable(false);
        diskPanel.add(new JScrollPane(diskLogArea), BorderLayout.CENTER);
        bottomPanel.add(diskPanel);
        add(bottomPanel, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        stepButton = new JButton("Step");
        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stepButton);
        add(controlPanel, BorderLayout.PAGE_END);

        setVisible(true);
    }

    public void updateRunningProcess(int pid, String instruction) {
        SwingUtilities.invokeLater(() -> {
            runningProcessLabel.setText("Running: P" + (pid == -1 ? "None" : pid));
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
}
