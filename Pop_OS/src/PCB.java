public class PCB {

    public int    processID;
    public State  state;
    public int    programCounter;
    public int    lowerBound;
    public int    upperBound;
    public int    arrivalTime;
    public int    burstTime;    
    public int    waitingTime;  

    
    public PCB(int processID, int arrivalTime, int burstTime) {
        this.processID     = processID;
        this.state         = State.READY;
        this.programCounter= 0;
        this.lowerBound    = -1;
        this.upperBound    = -1;
        this.arrivalTime   = arrivalTime;
        this.burstTime     = burstTime;
        this.waitingTime   = 0;
    }

    
    public void syncFromMemory(Memory memory) {
        this.state          = State.valueOf(memory.getState(processID));
        this.programCounter = memory.getProgramCounter(processID);
        this.lowerBound     = memory.getLowerBound(processID);
        this.upperBound     = memory.getUpperBound(processID);
    }

    
    public void syncToMemory(Memory memory) {
        memory.setState(processID, state.name());
        memory.setProgramCounter(processID, programCounter);
    }

    
    public double getResponseRatio() {
        if (burstTime == 0) return Double.MAX_VALUE;
        return (double)(waitingTime + burstTime) / burstTime;
    }

    @Override
    public String toString() {
        return String.format(
            "PCB[PID=%d | State=%-8s | PC=%d | Bounds=[%d-%d] | Arrival=%d | Burst=%d | Waiting=%d]",
            processID, state, programCounter, lowerBound, upperBound,
            arrivalTime, burstTime, waitingTime
        );
    }
}