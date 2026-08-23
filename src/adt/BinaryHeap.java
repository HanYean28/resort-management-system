package adt;

/**
 * BinaryHeap
 *
 * A generic max-heap implemented using a plain array.
 * This is a non-linear ADT — elements are organised as a
 * complete binary tree where every parent is greater than
 * or equal to both its children.
 *
 * Tree structure stored in array (1-indexed):
 *
 *            [1]
 *           /    \
 *         [2]    [3]
 *        /   \   /  \
 *       [4] [5] [6] [7]
 *
 * For any node at index i:
 *   Parent      → i / 2
 *   Left child  → i * 2
 *   Right child → i * 2 + 1
 *
 * This makes it non-linear: each element has a parent-child
 * relationship, NOT a simple predecessor-successor one.
 *
 * The heap requires a Comparator to determine priority so
 * it works with any object type, including Guest.
 */
public class BinaryHeap<T> {

    // -------------------------------------------------------
    // Constants
    // -------------------------------------------------------

    private static final int DEFAULT_CAPACITY = 64;

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------

    private Object[] heap;     // 1-indexed internal array
    private int      size;     // number of elements currently stored
    private java.util.Comparator<T> comparator;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Purpose:
     * Creates an empty max-heap with the given comparator.
     *
     * The comparator determines what "higher priority" means.
     * A positive result from compare(a, b) means a has higher
     * priority than b and should sit closer to the root.
     */
    public BinaryHeap(java.util.Comparator<T> comparator) {
        this.comparator = comparator;
        this.heap       = new Object[DEFAULT_CAPACITY + 1]; // index 0 unused
        this.size       = 0;
    }

    // -------------------------------------------------------
    // Core heap operations
    // -------------------------------------------------------

    /**
     * Purpose:
     * Inserts a new element into the heap.
     *
     * The element is placed at the next available leaf
     * position (end of the array), then bubbled UP the
     * tree until the heap property is restored.
     *
     * Time complexity: O(log n)
     */
    public void insert(T item) {

        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }

        ensureCapacity();

        // Place at the next leaf position.
        size++;
        heap[size] = item;

        // Bubble up to restore heap order.
        bubbleUp(size);
    }

    /**
     * Purpose:
     * Removes and returns the highest-priority element
     * (the root of the heap).
     *
     * The last element is moved to the root, then bubbled
     * DOWN until the heap property is restored.
     *
     * Time complexity: O(log n)
     */
    public T removeMax() {

        if (isEmpty()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        T max = (T) heap[1];

        // Move the last element to the root.
        heap[1] = heap[size];
        heap[size] = null;
        size--;

        // Bubble down to restore heap order.
        if (!isEmpty()) {
            bubbleDown(1);
        }

        return max;
    }

    /**
     * Purpose:
     * Returns the highest-priority element without
     * removing it from the heap.
     *
     * Time complexity: O(1)
     */
    public T peekMax() {

        if (isEmpty()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        T max = (T) heap[1];

        return max;
    }

    /**
     * Purpose:
     * Removes a specific element from the heap by
     * scanning for it, then restoring heap order.
     *
     * Time complexity: O(n)
     */
    public boolean remove(T item) {

        if (item == null) {
            return false;
        }

        // Find the element's position.
        int index = -1;

        for (int i = 1; i <= size; i++) {

            @SuppressWarnings("unchecked")
            T current = (T) heap[i];

            if (current != null && current.equals(item)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return false;
        }

        // Replace with the last element.
        heap[index] = heap[size];
        heap[size]  = null;
        size--;

        // Restore heap order from that position.
        if (index <= size) {
            bubbleUp(index);
            bubbleDown(index);
        }

        return true;
    }

    /**
     * Purpose:
     * Returns all elements in the heap as an array.
     *
     * Note: the array is NOT in sorted order — it reflects
     * the heap's internal tree structure.
     *
     * To get elements in priority order, use toSortedArray().
     */
    @SuppressWarnings("unchecked")
    public T[] toArray(T[] result) {

        for (int i = 0; i < size; i++) {
            result[i] = (T) heap[i + 1];
        }

        return result;
    }

    /**
     * Purpose:
     * Returns all elements in strict priority order
     * (highest priority first) without modifying the heap.
     *
     * Achieved by repeatedly extracting the max from a
     * temporary copy of the heap.
     *
     * Time complexity: O(n log n)
     */
    @SuppressWarnings("unchecked")
    public T[] toSortedArray(T[] result) {

        // Work on a copy so the real heap is not disturbed.
        BinaryHeap<T> copy = new BinaryHeap<>(comparator);

        for (int i = 1; i <= size; i++) {
            copy.insert((T) heap[i]);
        }

        for (int i = 0; i < result.length && !copy.isEmpty(); i++) {
            result[i] = copy.removeMax();
        }

        return result;
    }

    // -------------------------------------------------------
    // State queries
    // -------------------------------------------------------

    /**
     * Purpose:
     * Returns the number of elements currently in the heap.
     */
    public int size() {
        return size;
    }

    /**
     * Purpose:
     * Returns true if the heap contains no elements.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Purpose:
     * Removes all elements from the heap.
     */
    public void clear() {
        for (int i = 1; i <= size; i++) {
            heap[i] = null;
        }
        size = 0;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    /**
     * Purpose:
     * Moves the element at the given index UP the tree
     * until it is in the correct position.
     *
     * Used after insertion (element placed at bottom).
     */
    private void bubbleUp(int index) {

        while (index > 1) {

            int parent = index / 2;

            if (compare(index, parent) > 0) {
                swap(index, parent);
                index = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Purpose:
     * Moves the element at the given index DOWN the tree
     * until it is in the correct position.
     *
     * Used after removal (last element placed at root).
     */
    private void bubbleDown(int index) {

        while (true) {

            int left     = index * 2;
            int right    = index * 2 + 1;
            int largest  = index;

            if (left <= size && compare(left, largest) > 0) {
                largest = left;
            }

            if (right <= size && compare(right, largest) > 0) {
                largest = right;
            }

            if (largest != index) {
                swap(index, largest);
                index = largest;
            } else {
                break;
            }
        }
    }

    /**
     * Purpose:
     * Compares two elements by their heap array positions
     * using the comparator.
     */
    @SuppressWarnings("unchecked")
    private int compare(int i, int j) {
        return comparator.compare((T) heap[i], (T) heap[j]);
    }

    /**
     * Purpose:
     * Swaps two elements in the heap array.
     */
    private void swap(int i, int j) {
        Object temp = heap[i];
        heap[i]     = heap[j];
        heap[j]     = temp;
    }

    /**
     * Purpose:
     * Doubles the internal array capacity when full.
     */
    private void ensureCapacity() {

        if (size >= heap.length - 1) {

            Object[] larger = new Object[heap.length * 2];

            for (int i = 1; i <= size; i++) {
                larger[i] = heap[i];
            }

            heap = larger;
        }
    }
}