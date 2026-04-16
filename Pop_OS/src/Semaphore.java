package com.osproject;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Represents a mutex semaphore for mutual exclusion on a resource.
 * Each semaphore has a blocked queue for processes waiting on it.
 */
public class Semaphore {
    private final String resourceName;      // "userInput", "userOutput", "file"
    private int value;                      // 1 = available, 0 = locked
    private final Queue<Process> blockedQueue; // Processes blocked on this semaphore

    public Semaphore(String name) {
        this.resourceName = name;
        this.value = 1;  // Initially free
        this.blockedQueue = new LinkedList<>();
    }

    /**
     * Called when a process executes "semWait resource".
     * If the resource is free (value == 1), acquire it and continue.
     * If not free, block the process and add to this semaphore's blocked queue.
     * Also add to the global blocked queue (maintained by Scheduler).
     *
     * @param p The process requesting the resource.
     * @return true if the process acquired the lock and can continue; false if blocked.
     */
    public boolean semWait(Process p) {
        if (value == 1) {
            value = 0; // Lock acquired
            return true; // Process continues execution
        } else {
            // Resource is busy: block the process
            p.setState(ProcessState.BLOCKED);
            blockedQueue.add(p);
            // Notify the scheduler to move this process to global blocked queue
            OSSimulator.addToGlobalBlockedQueue(p);
            return false; // Process blocked, stop execution
        }
    }

    /**
     * Called when a process executes "semSignal resource".
     * If no processes are waiting, release the lock (value = 1).
     * If processes are waiting, dequeue one and move it to Ready state.
     *
     * @param p The process releasing the resource.
     */
    public void semSignal(Process p) {
        if (blockedQueue.isEmpty()) {
            value = 1; // Release lock
        } else {
            // Wake up the first waiting process
            Process nextProcess = blockedQueue.poll();
            nextProcess.setState(ProcessState.READY);
            OSSimulator.addToReadyQueue(nextProcess);
            // The lock remains held by the woken process (value stays 0)
        }
    }

    // Getters for GUI display
    public String getResourceName() { return resourceName; }
    public Queue<Process> getBlockedQueue() { return blockedQueue; }
    public boolean isLocked() { return value == 0; }
}
