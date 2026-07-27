package adt;

/**
 * ArrayStack.java A class that implements the ADT Stack using an array.
 *
 * @author Frank M. Carrano
 * @modified by: Chang Han Yean
 * @version 2.0
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class ArrayStack<T> implements StackInterface<T> {

  private T[] array;
  private int topIndex; // index of top entry
  private static final int DEFAULT_CAPACITY = 50;

  public ArrayStack() {
    this(DEFAULT_CAPACITY);
  }
  public ArrayStack(int initialCapacity) {
    if (initialCapacity < 1) {
      throw new IllegalArgumentException("Initial capacity must be greater than 0");
  }

    array = (T[]) new Object[initialCapacity];
    topIndex = -1;
  }
  @Override
  public boolean isEmpty(){
    return topIndex == -1;
  }

  private void doubleArray() {
    T[] oldArray = array;
    array = (T[]) new Object[oldArray.length * 2];
    for (int i = 0; i < oldArray.length; i++) {
      array[i] = oldArray[i];
    }
  }

  @Override
  public void push(T newEntry) {
    if (topIndex == array.length - 1) doubleArray();
    
    topIndex++;
    array[topIndex] = newEntry;
  }

  @Override
  public T peek() {
    if (isEmpty()) return null;
    
    T top = array[topIndex];

    return top;
  } 
  @Override
  public T pop() {
    if (isEmpty()) return null;
    
    T top = array[topIndex];
    array[topIndex] = null;
    topIndex--;
    return top;
  } 

  @Override
  public void clear() {
    while (!isEmpty()) {
      pop();
    }
  } 
  
} 