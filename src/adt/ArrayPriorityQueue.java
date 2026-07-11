package adt;

import java.io.Serializable;

/**
 * Array-based priority queue. Structure follows ArrayQueue, but entries are
 * stored in priority order (highest priority at backIndex) instead of FIFO.
 *
 * Referenced from: adt.ArrayQueue
 *
 * @author Chang Han Yean
 * @version 1.0
 * @param <T> The type of elements held in this priority queue, must be Comparable.
 */
public class ArrayPriorityQueue<T extends Comparable<? super T>>
    implements PriorityQueueInterface<T>, Serializable {

  private T[] array;
  private int backIndex;
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayPriorityQueue() {
    this(DEFAULT_CAPACITY);
  }

  @SuppressWarnings("unchecked")
  public ArrayPriorityQueue(int initialCapacity) {
    array = (T[]) new Comparable[initialCapacity];
    backIndex = -1;
  }

  @Override
  public void add(T newEntry) {
    if (newEntry == null) {
      throw new IllegalArgumentException("Cannot add null elements to Priority Queue.");
    }

    ensureCapacity();

    // Sorted in ascending order; highest priority sits at backIndex.
    int insertIndex = backIndex;
    while (insertIndex >= 0 && newEntry.compareTo(array[insertIndex]) < 0) {
      array[insertIndex + 1] = array[insertIndex]; // shift right
      insertIndex--;
    }

    array[insertIndex + 1] = newEntry;
    backIndex++;
  }

  @Override
  public T remove() {
    T highestPriority = null;

    if (!isEmpty()) {
      highestPriority = array[backIndex];
      array[backIndex] = null; // clean reference
      backIndex--;
    }

    return highestPriority;
  }

  @Override
  public T peek() {
    T highestPriority = null;

    if (!isEmpty()) {
      highestPriority = array[backIndex];
    }

    return highestPriority;
  }

  @Override
  public boolean isEmpty() {
    return backIndex < 0;
  }

  @Override
  public int getSize() {
    return backIndex + 1;
  }

  @Override
  public void clear() {
    if (!isEmpty()) { // deallocates only the used portion
      for (int index = 0; index <= backIndex; index++) {
        array[index] = null;
      }
      backIndex = -1;
    }
  }

  private boolean isArrayFull() {
    return backIndex == array.length - 1;
  }

  @SuppressWarnings("unchecked")
  private void ensureCapacity() {
    if (isArrayFull()) {
      T[] oldArray = array;
      int newCapacity = oldArray.length * 2;
      array = (T[]) new Comparable[newCapacity];
      System.arraycopy(oldArray, 0, array, 0, backIndex + 1);
    }
  }
}
