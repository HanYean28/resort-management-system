package adt;

/**
 * An interface for a custom List ADT based on the textbook.
 * 
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T> The type of elements held in this list.
 */
public interface ListInterface<T> {

    /**
     * Adds a new entry to the end of this list.
     * @param newEntry The object to be added as a new entry.
     * @return True if the addition is successful, or false if not.
     */
    boolean add(T newEntry);

    /**
     * Adds a new entry at a specified position within this list.
     * @param newPosition An integer that specifies the desired position of the new entry.
     * @param newEntry The object to be added as a new entry.
     * @return True if the addition is successful, or false if not.
     */
    boolean add(int newPosition, T newEntry);

    /**
     * Removes the entry at a given position from this list.
     * @param givenPosition An integer that specifies the position of the entry to be removed.
     * @return The object that was removed.
     */
    T remove(int givenPosition);

    /**
     * Removes all entries from this list.
     */
    void clear();

    /**
     * Replaces the entry at a given position in this list with a new entry.
     * @param givenPosition An integer that specifies the position of the entry to be replaced.
     * @param newEntry The object that will replace the entry at givenPosition.
     * @return True if the replacement is successful, or false if not.
     */
    boolean replace(int givenPosition, T newEntry);

    /**
     * Retrieves the entry at a given position in this list.
     * @param givenPosition An integer that specifies the position of the desired entry.
     * @return The object at givenPosition.
     */
    T getEntry(int givenPosition);

    /**
     * Sees whether this list contains a given entry.
     * @param anEntry The object that is the desired entry.
     * @return True if the list contains anEntry, or false if not.
     */
    boolean contains(T anEntry);

    /**
     * Gets the number of entries currently in this list.
     * @return The integer number of entries.
     */
    int getNumberOfEntries();

    /**
     * Sees whether this list is empty.
     * @return True if the list is empty, or false if not.
     */
    boolean isEmpty();

    /**
     * Sees whether this list is full.
     * @return True if the list is full, or false if not.
     */
    boolean isFull();
}
