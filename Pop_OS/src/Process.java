public class Process {
    private final PCB pcb;

    public Process(PCB pcb) {
        this.pcb = pcb;
    }

    public PCB getPCB() { return pcb; }
    public int getProcessID() { return pcb.getProcessID(); }
    public State getState() { return pcb.getState(); }
    public void setState(State state) { pcb.setState(state); }
}