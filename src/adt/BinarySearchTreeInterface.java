package adt;

import java.util.Iterator;

/**
 * An interface for the ADT Binary Search Tree.
 *
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T> The type of elements held in this tree; must be Comparable.
 */
public interface BinarySearchTreeInterface<T extends Comparable<? super T>> {

    /**
     * Searches for a specific entry in the tree.
     *
     * @param entry an object to be found
     * @return true if the object was found in the tree
     */
    boolean contains(T entry);

    /**
     * Retrieves a specific entry in the tree.
     *
     * @param entry an object to be found
     * @return either the object that was found in the tree or null if no such
     *         object exists
     */
    T getEntry(T entry);

    /**
     * Adds a new entry to the tree. If the entry matches an object that exists in
     * the tree already, replaces the object with the new entry.
     *
     * @param newEntry an object to be added to the tree
     * @return either null if newEntry was not in the tree already, or an existing
     *         entry that matched the parameter newEntry and has been replaced in
     *         the tree
     */
    T add(T newEntry);

    /**
     * Removes a specific entry from the tree.
     *
     * @param entry an object to be removed
     * @return either the object that was removed from the tree or null if no such
     *         object exists
     */
    T remove(T entry);

    /**
     * Detects whether this tree is empty.
     *
     * @return true if the tree is empty, or false otherwise
     */
    boolean isEmpty();

    /**
     * Removes all entries from the tree.
     */
    void clear();

    /**
     * Gets an iterator that traverses entries in preorder.
     *
     * @return a preorder iterator
     */
    Iterator<T> getPreorderIterator();

    /**
     * Gets an iterator that traverses entries in postorder.
     *
     * @return a postorder iterator
     */
    Iterator<T> getPostorderIterator();

    /**
     * Gets an iterator that traverses entries in inorder.
     *
     * @return an inorder iterator
     */
    Iterator<T> getInorderIterator();
}
