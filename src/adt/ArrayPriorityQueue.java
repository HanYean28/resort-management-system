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
@SuppressWarnings("unchecked")
public class ArrayPriorityQueue<T extends Comparable<? super T>>
    implements PriorityQueueInterface<T>, Serializable {

  private T[] array;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayPriorityQueue() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayPriorityQueue(int initialCapacity) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("Initial capacity must be greater than 0");
    }
    array = (T[]) new Comparable[initialCapacity];
    numberOfEntries = 0;
  }

  @Override
  public void add(T newEntry) {
    if (newEntry == null) {
      throw new IllegalArgumentException("Cannot add null elements to Priority Queue.");
    }

    if (isFull()) {
      doubleArray();
    }

    // Sorted in ascending order; highest priority sits at end of array.
    int insertIndex = numberOfEntries - 1;
    while (insertIndex >= 0 && newEntry.compareTo(array[insertIndex]) <= 0) {
      array[insertIndex + 1] = array[insertIndex]; // shift right
      insertIndex--;
    }

    array[insertIndex + 1] = newEntry;
    numberOfEntries++;
  }

  @Override
  public T remove() {
    T highestPriority = null;

    if (!isEmpty()) {
      highestPriority = array[numberOfEntries - 1];
      array[numberOfEntries - 1] = null; // clean reference
      numberOfEntries--;
    }

    return highestPriority;
  }

  @Override
  public T peek() {
    T highestPriority = null;

    if (!isEmpty()) {
      highestPriority = array[numberOfEntries - 1];
    }

    return highestPriority;
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public int size() {
    return numberOfEntries;
  }

  @Override
  public void clear() {
    if (!isEmpty()) { // deallocates only the used portion
      for (int index = 0; index < numberOfEntries; index++) {
        array[index] = null;
      }
      numberOfEntries = 0;
    }
  }

  @Override
  public boolean isFull() {
    return numberOfEntries == array.length;
  }

  private void doubleArray() {
    T[] oldArray = array;
    array = (T[]) new Comparable[oldArray.length * 2];
    System.arraycopy(oldArray, 0, array, 0, numberOfEntries);
  }

}
