package com.osproject;

import java.util.LinkedList;
import java.util.Queue;


public class Semaphore {
    private final String resourceName;     
    private int value;                      
    private final Queue<Process> blockedQueue; 

    public Semaphore(String name) {
        this.resourceName = name;
        this.value = 1; 
        this.blockedQueue = new LinkedList<>();
    }

 
    public boolean semWait(Process p) {
        if (value == 1) {
            value = 0; 
            return true; 
        } else {
           
            p.setState(ProcessState.BLOCKED);
            blockedQueue.add(p);
            
            OSSimulator.addToGlobalBlockedQueue(p);
            return false; 
        }
    }


    public void semSignal(Process p) {
        if (blockedQueue.isEmpty()) {
            value = 1; 
        } else {
            
            Process nextProcess = blockedQueue.poll();
            nextProcess.setState(ProcessState.READY);
            OSSimulator.addToReadyQueue(nextProcess);
           
        }
    }

    // Getters for GUI display
    public String getResourceName() { return resourceName; }
    public Queue<Process> getBlockedQueue() { return blockedQueue; }
    public boolean isLocked() { return value == 0; }
}
