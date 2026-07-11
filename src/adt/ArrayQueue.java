package adt;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Adapted from: Frank M. Carrano, Data Structures and Algorithms in Java.
 * 
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T>
 */
public class ArrayQueue<T> implements QueueInterface<T>, Serializable {

  private T[] array;
  private final static int frontIndex = 0;
  private int backIndex;
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayQueue() {
    this(DEFAULT_CAPACITY);
  }

  @SuppressWarnings("unchecked")
  public ArrayQueue(int initialCapacity) {
    array = (T[]) new Object[initialCapacity];
    backIndex = -1;
  }

  @Override
  public void enqueue(T newEntry) {
    if (isArrayFull()) {
      throw new IllegalStateException("Queue is full.");
    }

    backIndex++;
    array[backIndex] = newEntry;
  }

  @Override
  public T getFront() {
    T front = null;
    if (!isEmpty()) {
      front = array[frontIndex];
    }
    return front;
  }

  @Override
  public T dequeue() {
    T front = null;
    if (!isEmpty()) {
      front = array[frontIndex]; // shift remaining array items forward one position
      for (int i = frontIndex; i < backIndex; ++i) {
        array[i] = array[i + 1];
      }
      array[backIndex] = null; // Clean reference
      backIndex--;
    }
    return front;
  }

  @Override
  public boolean isEmpty() {
    return frontIndex > backIndex;
  }

  @Override
  public void clear() {
    if (!isEmpty()) { // deallocates only the used portion
      for (int index = frontIndex; index <= backIndex; index++) {
        array[index] = null;
      }
      backIndex = -1;
    }
  }

  private boolean isArrayFull() {
    return backIndex == array.length - 1;
  }

  @Override
  public Iterator<T> getIterator() {
    return new ArrayQueueIterator();
  }

  private class ArrayQueueIterator implements Iterator<T> {
    private int nextIndex;

    private ArrayQueueIterator() {
      nextIndex = 0;
    }

    @Override
    public boolean hasNext() {
      return nextIndex <= backIndex;
    }

    @Override
    public T next() {
      if (hasNext()) {
        T nextEntry = array[nextIndex];
        nextIndex++; // advance iterator
        return nextEntry;
      } else {
        return null;
      }
    }
  }
}
