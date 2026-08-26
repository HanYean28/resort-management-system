package adt;

import java.util.Comparator;

/**
 * A generic max-heap (complete binary tree) stored in a resizable array.
 *
 * The element at index 0 is the root (highest priority).
 * For any node at index i:
 *   - left  child  → 2i + 1
 *   - right child  → 2i + 2
 *   - parent       → (i - 1) / 2
 *
 * Priority is determined entirely by the Comparator supplied at
 * construction time. A positive result means the first argument
 * has higher priority and should sit closer to the root.
 *
 * @param <T> the element type stored in the heap
 *
 * @author Kaizen Soh
 */
public class BinaryHeap<T> {

    // -------------------------------------------------------
    // Constants
    // -------------------------------------------------------

    private static final int DEFAULT_CAPACITY = 16;

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------

    private Object[]   heap;        // backing array (max-heap)
    private int        size;        // number of elements
    private Comparator<T> comparator;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Creates an empty BinaryHeap using the given comparator.
     *
     * comparator.compare(a, b) > 0  →  a has higher priority than b
     *
     * @param comparator defines the priority ordering
     */
    public BinaryHeap(Comparator<T> comparator) {
        this.comparator  = comparator;
        this.heap        = new Object[DEFAULT_CAPACITY];
        this.size        = 0;
    }

    // -------------------------------------------------------
    // Core operations
    // -------------------------------------------------------

    /**
     * Inserts an element into the heap.
     *
     * Places the element at the next leaf position (end of array),
     * then bubbles it UP the tree until heap order is restored.
     *
     * Time complexity: O(log n)
     *
     * @param element the element to insert
     * @throws IllegalArgumentException if element is null
     */
    public void insert(T element) {

        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        ensureCapacity();

        heap[size] = element;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the element with the highest priority (the root).
     *
     * Swaps root with the last leaf, reduces size, then bubbles
     * the new root DOWN until heap order is restored.
     *
     * Time complexity: O(log n)
     *
     * @return the highest-priority element, or null if the heap is empty
     */
    public T removeMax() {

        if (size == 0) {
            return null;
        }

        T max = elementAt(0);

        heap[0] = heap[size - 1];
        heap[size - 1] = null;
        size--;

        if (size > 0) {
            siftDown(0);
        }

        return max;
    }

    /**
     * Returns the highest-priority element without removing it.
     *
     * Time complexity: O(1)
     *
     * @return the root element, or null if the heap is empty
     */
    public T peekMax() {

        if (size == 0) {
            return null;
        }

        return elementAt(0);
    }

    /**
     * Removes a specific element from anywhere in the heap.
     *
     * Uses equals() to locate the element, swaps it with the last
     * leaf, then re-heapifies (sift-up or sift-down as needed)
     * to restore heap order.
     *
     * Time complexity: O(n) to find + O(log n) to reheapify
     *
     * @param element the element to remove
     * @return true if the element was found and removed; false otherwise
     */
    public boolean remove(T element) {

        if (element == null) {
            return false;
        }

        int index = indexOf(element);

        if (index == -1) {
            return false;
        }

        heap[index] = heap[size - 1];
        heap[size - 1] = null;
        size--;

        if (index < size) {
            // try both directions; only one will actually move the node
            siftUp(index);
            siftDown(index);
        }

        return true;
    }

    /**
     * Returns all elements in descending priority order without
     * modifying this heap.
     *
     * Operates on a shallow copy of the backing array so the
     * real heap is never disturbed.
     *
     * Time complexity: O(n log n)
     *
     * @param result a pre-allocated array to fill (must have length >= size)
     * @return the filled result array
     */
    @SuppressWarnings("unchecked")
    public T[] toSortedArray(T[] result) {

        // work on a copy
        Object[] copy  = new Object[size];
        System.arraycopy(heap, 0, copy, 0, size);
        int copySize = size;

        BinaryHeap<T> temp = new BinaryHeap<>(comparator);
        temp.heap = copy;
        temp.size = copySize;

        int i = 0;
        while (!temp.isEmpty()) {
            result[i++] = temp.removeMax();
        }

        return result;
    }

    // -------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------

    /** Returns the number of elements currently in the heap. */
    public int size() {
        return size;
    }

    /** Returns true if the heap contains no elements. */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Removes all elements from the heap. */
    public void clear() {
        for (int i = 0; i < size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    /**
     * Moves the element at the given index UP the tree
     * until the heap-order property is restored.
     *
     * Called after insertion (new leaf may have higher priority
     * than its parent).
     */
    private void siftUp(int index) {

        while (index > 0) {

            int parent = (index - 1) / 2;

            if (compare(index, parent) > 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Moves the element at the given index DOWN the tree
     * until the heap-order property is restored.
     *
     * Called after removeMax() (last leaf placed at root
     * may have lower priority than its children).
     */
    private void siftDown(int index) {

        while (true) {

            int largest = index;
            int left    = 2 * index + 1;
            int right   = 2 * index + 2;

            if (left < size && compare(left, largest) > 0) {
                largest = left;
            }

            if (right < size && compare(right, largest) > 0) {
                largest = right;
            }

            if (largest == index) {
                break;
            }

            swap(index, largest);
            index = largest;
        }
    }

    /**
     * Compares the elements at the two given indices using the
     * comparator. Returns a positive value when index a has
     * higher priority than index b.
     */
    private int compare(int a, int b) {
        return comparator.compare(elementAt(a), elementAt(b));
    }

    /** Swaps the elements at the two given indices. */
    private void swap(int a, int b) {
        Object tmp = heap[a];
        heap[a]    = heap[b];
        heap[b]    = tmp;
    }

    /**
     * Returns the index of the first element that equals the
     * given target, or -1 if not found.
     */
    private int indexOf(T target) {
        for (int i = 0; i < size; i++) {
            if (target.equals(heap[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Doubles the backing array capacity when the heap is full.
     */
    private void ensureCapacity() {
        if (size == heap.length) {
            Object[] larger = new Object[heap.length * 2];
            System.arraycopy(heap, 0, larger, 0, size);
            heap = larger;
        }
    }

    /**
     * Convenience cast — eliminates repeated unchecked-cast
     * warnings at every array read.
     */
    @SuppressWarnings("unchecked")
    private T elementAt(int index) {
        return (T) heap[index];
    }
}