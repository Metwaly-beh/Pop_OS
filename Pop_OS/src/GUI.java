package com.osproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

public class GUI extends JFrame {
    // Display components
    private JLabel runningProcessLabel;
    private JLabel currentInstructionLabel;
    private JTextArea readyQueueArea;
    private JTextArea blockedQueuesArea;  // Will show all blocked queues (per resource)
    private JTextArea memoryArea;
    private JTextArea diskLogArea;

    // Control buttons
    private JButton startButton;
    private JButton pauseButton;
    private JButton stepButton;

    // Reference to simulator (for control)
    private OSSimulator simulator;

    public GUI(OSSimulator sim) {
        this.simulator = sim;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("CSEN 602 OS Simulator - Spring 2026");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Running Process Info ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runningProcessLabel = new JLabel("Running: None");
        runningProcessLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        currentInstructionLabel = new JLabel("Instruction: -");
        currentInstructionLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        topPanel.add(runningProcessLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(currentInstructionLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Queues ---
        JPanel queuesPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Ready Queue
        JPanel readyPanel = new JPanel(new BorderLayout());
        readyPanel.setBorder(BorderFactory.createTitledBorder("Ready Queue"));
        readyQueueArea = new JTextArea();
        readyQueueArea.setEditable(false);
        readyQueueArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane readyScroll = new JScrollPane(readyQueueArea);
        readyPanel.add(readyScroll, BorderLayout.CENTER);
        queuesPanel.add(readyPanel);

        // Blocked Queues (all resources + general)
        JPanel blockedPanel = new JPanel(new BorderLayout());
        blockedPanel.setBorder(BorderFactory.createTitledBorder("Blocked Queues (UserInput | UserOutput | File | General)"));
        blockedQueuesArea = new JTextArea();
        blockedQueuesArea.setEditable(false);
        blockedQueuesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane blockedScroll = new JScrollPane(blockedQueuesArea);
        blockedPanel.add(blockedScroll, BorderLayout.CENTER);
        queuesPanel.add(blockedPanel);

        add(queuesPanel, BorderLayout.CENTER);

        // --- Bottom Panel: Memory and Disk Log ---
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Memory display
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory (40 words)"));
        memoryArea = new JTextArea();
        memoryArea.setEditable(false);
        memoryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane memoryScroll = new JScrollPane(memoryArea);
        memoryPanel.add(memoryScroll, BorderLayout.CENTER);
        bottomPanel.add(memoryPanel);

        // Disk log
        JPanel diskPanel = new JPanel(new BorderLayout());
        diskPanel.setBorder(BorderFactory.createTitledBorder("Disk Swap Log"));
        diskLogArea = new JTextArea();
        diskLogArea.setEditable(false);
        diskLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane diskScroll = new JScrollPane(diskLogArea);
        diskPanel.add(diskScroll, BorderLayout.CENTER);
        bottomPanel.add(diskPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // --- Control Buttons ---
        JPanel controlPanel = new JPanel();
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        stepButton = new JButton("Step");
        pauseButton.setEnabled(false); // Initially disabled

        startButton.addActionListener(e -> {
            simulator.startSimulation();
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
        });

        pauseButton.addActionListener(e -> {
            simulator.pauseSimulation();
            startButton.setEnabled(true);
            pauseButton.setEnabled(false);
        });

        stepButton.addActionListener(e -> {
            simulator.stepExecution();
        });

        controlPanel.add(startButton);
        controlPanel.add(pauseButton);
        controlPanel.add(stepButton);
        add(controlPanel, BorderLayout.PAGE_END);

        setVisible(true);
    }

    // ========== Update Methods (called by simulator every clock cycle) ==========

    /**
     * Update the running process and current instruction display.
     */
    public void updateRunningProcess(int pid, String instruction) {
        SwingUtilities.invokeLater(() -> {
            runningProcessLabel.setText("Running: P" + (pid == -1 ? "None" : pid));
            currentInstructionLabel.setText("Instruction: " + instruction);
        });
    }

    /**
     * Update the ready queue display.
     * @param queueStr A formatted string of PIDs in ready queue (e.g., "P1 P2 P3")
     */
    public void updateReadyQueue(String queueStr) {
        SwingUtilities.invokeLater(() -> readyQueueArea.setText(queueStr));
    }

    /**
     * Update blocked queues display.
     * @param blockedInfo A formatted string showing each semaphore's blocked queue
     *                    and the general blocked queue.
     */
    public void updateBlockedQueues(String blockedInfo) {
        SwingUtilities.invokeLater(() -> blockedQueuesArea.setText(blockedInfo));
    }

    /**
     * Update memory visualization.
     * @param memoryDump A string representing the 40 memory words (e.g., array index and content).
     */
    public void updateMemory(String memoryDump) {
        SwingUtilities.invokeLater(() -> memoryArea.setText(memoryDump));
    }

    /**
     * Append a message to the disk swap log.
     */
    public void appendDiskLog(String message) {
        SwingUtilities.invokeLater(() -> {
            diskLogArea.append(message + "\n");
            // Auto-scroll to bottom
            diskLogArea.setCaretPosition(diskLogArea.getDocument().getLength());
        });
    }
}
