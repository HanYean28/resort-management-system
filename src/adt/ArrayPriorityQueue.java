package adt;

import java.util.Comparator;

/**
 * Generic priority queue implemented with a custom BinaryHeap.
 *
 * This class contains no application-specific Guest/VIP logic. Priority is
 * supplied by the client through a Comparator, which keeps the ADT reusable.
 *
 * Although the heap is stored in an array internally, its logical structure is
 * a complete binary tree, so this is a non-linear implementation.
 *
 * @author Kaizen Soh
 * @param <T> element type stored in the priority queue
 */
public class ArrayPriorityQueue<T> implements PriorityQueueInterface<T> {

    private final BinaryHeap<T> heap;

    public ArrayPriorityQueue(Comparator<T> comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("Comparator cannot be null");
        }
        heap = new BinaryHeap<>(comparator);
    }

    @Override
    public void add(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("Entry cannot be null");
        }
        heap.insert(newEntry);
    }

    @Override
    public T remove() {
        return heap.removeMax();
    }

    @Override
    public T peek() {
        return heap.peekMax();
    }

    @Override
    public boolean remove(T entry) {
        return heap.remove(entry);
    }

    @Override
    public T[] toSortedArray(T[] result) {
        if (result == null || result.length < heap.size()) {
            throw new IllegalArgumentException("Result array is too small");
        }
        return heap.toSortedArray(result);
    }

    @Override
    public boolean isEmpty() {
        return heap.isEmpty();
    }

    @Override
    public int size() {
        return heap.size();
    }

    @Override
    public void clear() {
        heap.clear();
    }

    /** BinaryHeap expands dynamically, so this implementation is never full. */
    @Override
    public boolean isFull() {
        return false;
    }
}
