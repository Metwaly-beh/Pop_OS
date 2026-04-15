/**
 * Factory that creates a Scheduler instance by name.
 *
 * Usage:
 *     Scheduler s = SchedulerFactory.create("RR");       // quantum = 2 (default)
 *     Scheduler s = SchedulerFactory.create("RR", 4);    // quantum = 4
 *     Scheduler s = SchedulerFactory.create("HRRN");
 *     Scheduler s = SchedulerFactory.create("MLFQ");
 */
public class SchedulerChooser {

    private SchedulerChooser () {} // utility class — no instances

    /**
     * Create a scheduler by name with the default quantum (2 for RR).
     *
     * @param name  "HRRN", "RR", or "MLFQ" (case-insensitive)
     * @return the requested Scheduler implementation
     */
    public static Scheduler create(String name) {
        return create(name, 2);
    }

    /**
     * Create a scheduler by name with a specific quantum.
     *
     * @param name    "HRRN", "RR", or "MLFQ" (case-insensitive)
     * @param quantum time-slice length in instructions (used by RR; ignored by HRRN)
     * @return the requested Scheduler implementation
     */
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