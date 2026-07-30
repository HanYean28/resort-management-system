package adt;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Adapted from: Frank M. Carrano, Data Structures and Algorithms in Java.
 * 
 * @author Frank M. Carrano
 * @modified by: Chang Han Yean
 * @version 2.0
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class ArrayQueue<T> implements QueueInterface<T>, Serializable {

  private T[] array;
  private int numberOfEntries;
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayQueue() {
    this(DEFAULT_CAPACITY);
  }

  public ArrayQueue(int initialCapacity) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("Initial capacity must be greater than 0");
    }
    array = (T[]) new Object[initialCapacity];
    numberOfEntries = 0;
  }

  @Override
  public void enqueue(T newEntry) {
    if (newEntry == null) {
      throw new IllegalArgumentException("Cannot add null elements to Queue.");
    }
    
    if (isFull()) {
      doubleArray();
    }

    array[numberOfEntries] = newEntry;
    numberOfEntries++;
  }

  @Override
  public T dequeue() {
    if (isEmpty()) {
      return null;
    }
      T front = array[0];
      for (int i = 0; i < numberOfEntries - 1; ++i) {
        array[i] = array[i + 1];
      }
      // Clean reference
      array[numberOfEntries - 1] = null;
      numberOfEntries--;
      return front;
  }

  @Override
  public T getFront() {
    if (isEmpty()) {
      return null;
    }
    return array[0];
  }

  @Override
  public boolean isEmpty() {
    return numberOfEntries == 0;
  }

  @Override
  public void clear() {
    if (isEmpty()) {
      return;
    }
    for (int i = 0; i < numberOfEntries; i++) {
      array[i] = null;
    }
    numberOfEntries = 0;
  }
  @Override
  public boolean isFull() {
    return numberOfEntries == array.length;
  }

  @Override
  public int size() {
    return numberOfEntries;
  }

  private void doubleArray() {
    T[] oldArray = array;
    array = (T[]) new Object[oldArray.length * 2];
    for (int i = 0; i < oldArray.length; i++) {
      array[i] = oldArray[i];
    }
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
      return nextIndex < numberOfEntries;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more entries in queue iterator.");
      }
      T nextEntry = array[nextIndex];
      nextIndex++;
      return nextEntry;
    }
  }


}
