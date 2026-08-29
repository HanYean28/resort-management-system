package adt;

/**
 * Generic Priority Queue ADT.
 *
 * The ADT stores entries by priority. The implementation decides how
 * priorities are compared and how entries are physically stored.
 *
 * @author Kaizen Soh
 * @param <T> element type stored in the priority queue
 */
public interface PriorityQueueInterface<T> {

    /** Adds a new entry according to its priority. */
    void add(T newEntry);

    /** Removes and returns the highest-priority entry, or null if empty. */
    T remove();

    /** Returns the highest-priority entry without removing it, or null if empty. */
    T peek();

    /** Removes a specified entry while preserving priority-queue order. */
    boolean remove(T entry);

    /**
     * Copies all entries into the supplied array in descending priority order
     * without modifying the real priority queue.
     */
    T[] toSortedArray(T[] result);

    /** Returns true when the priority queue contains no entries. */
    boolean isEmpty();

    /** Returns the current number of entries. */
    int size();

    /** Removes all entries. */
    void clear();

    /** Returns true only if no more entries can be inserted. */
    boolean isFull();
}
