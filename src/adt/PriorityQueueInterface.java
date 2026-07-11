package adt;

/**
 * An interface for a custom Priority Queue ADT.
 * 
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T> The type of elements held in this priority queue, must be
 *            Comparable.
 */
public interface PriorityQueueInterface<T extends Comparable<? super T>> {

    /**
     * Adds a new entry to this priority queue.
     * 
     * @param newEntry An object to be added.
     */
    void add(T newEntry);

    /**
     * Removes and returns the entry with the highest priority.
     * 
     * @return Either the object with the highest priority or, if the priority queue
     *         was empty, null.
     */
    T remove();

    /**
     * Retrieves the entry with the highest priority.
     * 
     * @return Either the object with the highest priority or, if the priority queue
     *         was empty, null.
     */
    T peek();

    /**
     * Detects whether this priority queue is empty.
     * 
     * @return True if the priority queue is empty, or false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the size of this priority queue.
     * 
     * @return The number of elements currently in the queue.
     */
    int getSize();

    /**
     * Removes all entries from this priority queue.
     */
    void clear();
}
