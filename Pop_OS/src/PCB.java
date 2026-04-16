public class PCB {

    private int processID;
    private State state;
    private int programCounter;
    private int lowerBound;
    private int upperBound;
    private int arrivalTime;
    private int burstTime;
    private int waitingTime;

    public PCB(int processID, int arrivalTime, int burstTime) {
        this.processID = processID;
        this.state = State.READY;
        this.programCounter = 0;
        this.lowerBound = -1;
        this.upperBound = -1;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.waitingTime = 0;
    }

    public void syncFromMemory(Memory memory) {
        this.state = State.valueOf(memory.getState(getProcessID()));
        this.programCounter = memory.getProgramCounter(getProcessID());
        this.lowerBound = memory.getLowerBound(getProcessID());
        this.upperBound = memory.getUpperBound(getProcessID());
    }

    public void syncToMemory(Memory memory) {
        memory.setState(getProcessID(), getState().name());
        memory.setProgramCounter(getProcessID(), getProgramCounter());
    }

    public double getResponseRatio() {
        if (getBurstTime() == 0) return Double.MAX_VALUE;
        return (double)(getWaitingTime() + getBurstTime()) / getBurstTime();
    }

    public int getProcessID() {
        return processID;
    }

    public void setProcessID(int processID) {
        this.processID = processID;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    @Override
    public String toString() {
        return String.format(
            "PCB[PID=%d | State=%-8s | PC=%d | Bounds=[%d-%d] | Arrival=%d | Burst=%d | Waiting=%d]",
            getProcessID(), getState(), getProgramCounter(), getLowerBound(), getUpperBound(),
            getArrivalTime(), getBurstTime(), getWaitingTime()
        );
    }
}
