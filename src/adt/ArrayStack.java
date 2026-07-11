package adt;

import java.io.Serializable;

/**
 * ArrayStack.java A class that implements the ADT Stack using an array.
 *
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T>
 */
public class ArrayStack<T> implements StackInterface<T>, Serializable {

  private T[] array;
  private int topIndex; // index of top entry
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayStack() {
    this(DEFAULT_CAPACITY);
  }

  @SuppressWarnings("unchecked")
  public ArrayStack(int initialCapacity) {
    array = (T[]) new Object[initialCapacity];
    topIndex = -1;
  }

  @Override
  public void push(T newEntry) {
    if (isFull()) {
      throw new IllegalStateException("Stack is full.");
    }

    topIndex++;
    array[topIndex] = newEntry;
  }

  @Override
  public T peek() {
    T top = null;

    if (!isEmpty()) {
      top = array[topIndex];
    }

    return top;
  }

  @Override
  public T pop() {
    T top = null;
    if (!isEmpty()) {
      top = array[topIndex];
      array[topIndex] = null;
      topIndex--;
    } // end if

    return top;
  }

  @Override
  public boolean isEmpty() {
    return topIndex < 0;
  }

  @Override
  public void clear() {
    while (!isEmpty()) {
      array[topIndex] = null;
      topIndex--;
    }
  }

  private boolean isFull() {
    return topIndex == array.length - 1;
  }
}
