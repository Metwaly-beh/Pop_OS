
public class SchedulerChooser {

    private SchedulerChooser () {}

    // creates a default scheduler 
    public static Scheduler create(String name) {
        return create(name, 2);
    }

    // creates a scheduler with a specific quantum
   
    public static Scheduler create(String name, int quantum) {
        switch (name.toUpperCase()) {
            case "HRRN": return new HRRNScheduler();
            case "RR":   return new RRScheduler(quantum);
            case "MLFQ": return new MLFQScheduler();
            default:
                throw new IllegalArgumentException(
                        "[SchedulerFactory] Unknown algorithm: '" + name
                                + "'. Valid options: HRRN, RR, MLFQ");
        }
    }
}
