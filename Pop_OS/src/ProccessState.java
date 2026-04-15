/**
 * All possible states a process can be in during its lifetime.
 */
public enum ProccessState {
    NEW,      // just created, not yet in a queue
    READY,    // in the ready queue, waiting for CPU
    RUNNING,  // currently executing on CPU
    BLOCKED,  // waiting for a mutex/resource
    FINISHED  // completed execution
}
 