package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {
	private volatile T result; // The result of the computation.
    private volatile boolean isResolved; // Whether the result is resolved.
	/**
	 * This should be the the only public constructor in this class.
	 */
	public Future() {
        this.result = null;       // ערך התוצאה הוא null בהתחלה
        this.isResolved = false;  // התוצאה לא הוסדרה עדיין
    }
	
	/**
     * retrieves the result the Future object holds if it has been resolved.
     * This is a blocking method! It waits for the computation in case it has
     * not been completed.
     * <p>
     * @return return the result of type T if it is available, if not wait until it is available.
     * 	       
     */
	public T get() {
        synchronized (this) {
            while (!isDone()) {
                try {
                    wait(); // Block the thread until the result is available.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle interruption.
                }
            }
            return result; // Return the resolved result.
        }
    }
	
	/**
     * Resolves the result of this Future object.
     */
	public void resolve(T result) {
        synchronized (this) {
            while (!isDone()) {
                this.result = result;
                isResolved = true;
                notifyAll(); // Notify all threads waiting for the result.
            }
        }
    }
	
	/**
     * @return true if this object has been resolved, false otherwise
     */
	public boolean isDone() {
		return isResolved;
	}
	
	/**
     * retrieves the result the Future object holds if it has been resolved,
     * This method is non-blocking, it has a limited amount of time determined
     * by {@code timeout}
     * <p>
     * @param timout 	the maximal amount of time units to wait for the result.
     * @param unit		the {@link TimeUnit} time units to wait.
     * @return return the result of type T if it is available, if not, 
     * 	       wait for {@code timeout} TimeUnits {@code unit}. If time has
     *         elapsed, return null.
     */
	public T get(long timeout, TimeUnit unit) {
        synchronized (this) {
            long millisTimeout = unit.toMillis(timeout); // Convert timeout to milliseconds.
            long startTime = System.currentTimeMillis(); // Record the start time.
            
            while (!isDone()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long remainingTime = millisTimeout - elapsedTime;
                
                if (remainingTime <= 0) {
                    return null; // Timeout expired, return null.
                }
                
                try {
                    wait(remainingTime); // Wait for the result or the timeout.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Handle interruption.
                }
            }
            return result; // Return the resolved result.
        }
    }
    
    public void setResult(T result) {
        this.result = result;
    }

    // עדכון מצב ה-"הושלם"
    public void setIsResolved(boolean isResolved) {
        this.isResolved = isResolved;
    }

}
