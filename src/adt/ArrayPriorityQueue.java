package adt;

import java.io.Serializable;

/**
 * A sorted array-based implementation of PriorityQueueInterface.
 * 
 * @param <T> The type of elements held in this priority queue, must be
 *            Comparable.
 */
public class ArrayPriorityQueue<T extends Comparable<? super T>>
        implements PriorityQueueInterface<T>, Serializable {

    private T[] queue;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 50;

    public ArrayPriorityQueue() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public ArrayPriorityQueue(int initialCapacity) {
        queue = (T[]) new Comparable[initialCapacity];
        numberOfEntries = 0;
    }

    @Override
    public void add(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("Cannot add null elements to Priority Queue.");
        }
        ensureCapacity();

        // Find the correct position to insert while maintaining sorted order.
        // Array is sorted in ascending order, so the highest priority is at the end
        // (index numberOfEntries - 1).
        int insertIndex = numberOfEntries;
        while (insertIndex > 0 && newEntry.compareTo(queue[insertIndex - 1]) < 0) {
            queue[insertIndex] = queue[insertIndex - 1]; // Shift right
            insertIndex--;
        }

        queue[insertIndex] = newEntry;
        numberOfEntries++;
    }

    @Override
    public T remove() {
        if (isEmpty()) {
            return null;
        }
        // Remove from the end (highest priority, O(1))
        T highestPriority = queue[numberOfEntries - 1];
        queue[numberOfEntries - 1] = null; // Clean reference
        numberOfEntries--;
        return highestPriority;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return queue[numberOfEntries - 1];
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public int getSize() {
        return numberOfEntries;
    }

    @Override
    public void clear() {
        for (int i = 0; i < numberOfEntries; i++) {
            queue[i] = null;
        }
        numberOfEntries = 0;
    }

    @SuppressWarnings("unchecked")
    private void ensureCapacity() {
        if (numberOfEntries == queue.length) {
            T[] oldQueue = queue;
            int newCapacity = oldQueue.length * 2;
            queue = (T[]) new Comparable[newCapacity];
            System.arraycopy(oldQueue, 0, queue, 0, numberOfEntries);
        }
    }
}
