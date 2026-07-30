package adt;

import java.io.Serializable;

/**
 * Adapted from: Frank M. Carrano, Data Structures and Algorithms in Java.
 *
 * @author Frank M. Carrano
 * @modified by: Chang Han Yean
 * @version 2.0
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class ArrayList<T> implements ListInterface<T>, Serializable {

    private T[] array;
    private int numberOfEntries;
    private static final int DEFAULT_CAPACITY = 50;

    // default constructor with default initial capacity
    public ArrayList() {
        this(DEFAULT_CAPACITY);
    }

    // constructor with custom initial capacity
    public ArrayList(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity must be greater than 0");
        }
        numberOfEntries = 0;
        array = (T[]) new Object[initialCapacity];
    }

    private void doubleArray() {
        T[] oldArray = array;
        array = (T[]) new Object[oldArray.length * 2];
        for (int i = 0; i < oldArray.length; i++) {
            array[i] = oldArray[i];
        }
    }

    // add a new entry to the end of the list
    @Override
    public boolean add(T newEntry) {
        if (newEntry == null) {
            throw new IllegalArgumentException("Cannot add null elements to ArrayList.");
        }
        if(isFull()) {
            doubleArray();
        }
        
        array[numberOfEntries] = newEntry;
        numberOfEntries++;
        return true;
    }

    // add a new entry at a specified position within the list
    // position is 1-based, so 1 is the first position and in array is at index 0
    @Override
    public boolean add(int newPosition, T newEntry) {
        if (newPosition >=1 && newPosition <= numberOfEntries + 1){
            if(isFull()) {
                doubleArray();
            }
            for (int index = numberOfEntries; index >= newPosition; index--){
                array[index] = array[index - 1];
            }
            array[newPosition - 1] = newEntry;
            numberOfEntries++;
            return true;

        }
        return false;
    }

    @Override
    public T remove(int givenPosition) {
        if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
            T result = array[givenPosition - 1];

            for (int index = givenPosition - 1; index < numberOfEntries - 1; index++) {
                array[index] = array[index + 1];
            }

            numberOfEntries--;
            array[numberOfEntries] = null;
            return result;
        }
        return null;
    }

    @Override
    public void clear() {
        numberOfEntries = 0;
    }

    @Override
    public boolean replace(int givenPosition, T newEntry) {

        if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
            array[givenPosition - 1] = newEntry;
            return true;
        }
        return false;
    }

    @Override
    public T getEntry(int givenPosition) {

        if ((givenPosition >= 1) && (givenPosition <= numberOfEntries)) {
            return array[givenPosition - 1];
        }

        return null;
    }

    @Override
    public boolean contains(T anEntry) {
        if (anEntry == null) {
            return false;
        }

        boolean found = false;
        for (int index = 0; !found && (index < numberOfEntries); index++) {
            if (anEntry.equals(array[index])) {
                found = true;
            }
        }
        return found;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public boolean isFull() {
        return numberOfEntries == array.length;
    }


}
