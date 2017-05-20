package mmj.util;

/**
 * An interface designed to monitor the progress of the file loading or proof verifying
 */
public interface Progress {
    // TODO handle interruptions, e.g. by throwing an exception in "worked"

    /**
     * Notifies that some new task has been identified, 
     * which adds up to the total number of work unit to be done.
     * 
     * @param work the number of work units to be added to the total work to be done.
     */
    public void addTask(long work);

     /**
      * Notifies that a given number of work unit has been completed. 
      * Note that this amount represents an
      * installment, as opposed to a cumulative amount of work done
      * to date.
      *
      * @param work a non-negative number of work units just completed
      */
     public void worked(long work);
 }
