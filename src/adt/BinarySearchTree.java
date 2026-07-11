package adt;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * Adapted from: Frank M. Carrano, Data Structures and Algorithms in Java.
 * 
 * @author Frank M. Carrano
 * @version 2.0
 * @param <T> The type of elements held in this tree; must be Comparable.
 */
public class BinarySearchTree<T extends Comparable<? super T>>
        implements BinarySearchTreeInterface<T>, Serializable {

    private Node root;

    public BinarySearchTree() {
        root = null;
    }

    public BinarySearchTree(T rootData) {
        root = new Node(rootData);
    }

    @Override
    public boolean contains(T entry) {
        return getEntry(entry) != null;
    }

    @Override
    public T getEntry(T entry) {
        return findEntry(root, entry);
    }

    private T findEntry(Node rootNode, T entry) {
        T result = null;

        if (rootNode != null) {
            T rootEntry = rootNode.data;

            if (entry.equals(rootEntry)) {
                result = rootEntry;
            } else if (entry.compareTo(rootEntry) < 0) {
                result = findEntry(rootNode.left, entry);
            } else {
                result = findEntry(rootNode.right, entry);
            }
        }

        return result;
    }

    @Override
    public T add(T newEntry) {
        T result = null;

        if (isEmpty()) {
            root = new Node(newEntry);
        } else {
            result = addEntry(root, newEntry);
        }

        return result;
    }

    /**
     * Adds newEntry to the nonempty subtree rooted at rootNode.
     */
    private T addEntry(Node rootNode, T newEntry) {
        T result = null;
        int comparison = newEntry.compareTo(rootNode.data);

        if (comparison == 0) {
            result = rootNode.data;
            rootNode.data = newEntry;
        } else if (comparison < 0) {
            if (rootNode.left != null) {
                result = addEntry(rootNode.left, newEntry);
            } else {
                rootNode.left = new Node(newEntry);
            }
        } else {
            if (rootNode.right != null) {
                result = addEntry(rootNode.right, newEntry);
            } else {
                rootNode.right = new Node(newEntry);
            }
        }

        return result;
    }

    @Override
    public T remove(T entry) {
        ReturnObject oldEntry = new ReturnObject(null);
        Node newRoot = removeEntry(root, entry, oldEntry);
        root = newRoot;
        return oldEntry.get();
    }

    /**
     * Removes an entry from the tree rooted at a given node.
     */
    private Node removeEntry(Node rootNode, T entry, ReturnObject oldEntry) {
        if (rootNode != null) {
            T rootData = rootNode.data;
            int comparison = entry.compareTo(rootData);

            if (comparison == 0) {
                oldEntry.set(rootData);
                rootNode = removeFromRoot(rootNode);
            } else if (comparison < 0) {
                Node leftChild = rootNode.left;
                Node subtreeRoot = removeEntry(leftChild, entry, oldEntry);
                rootNode.left = subtreeRoot;
            } else {
                Node rightChild = rootNode.right;
                rootNode.right = removeEntry(rightChild, entry, oldEntry);
            }
        }

        return rootNode;
    }

    /**
     * Removes the entry in a given root node of a subtree.
     */
    private Node removeFromRoot(Node rootNode) {
        if (rootNode.left != null && rootNode.right != null) {
            Node leftSubtreeRoot = rootNode.left;
            Node largestNode = findLargest(leftSubtreeRoot);

            rootNode.data = largestNode.data;
            rootNode.left = removeLargest(leftSubtreeRoot);
        } else if (rootNode.right != null) {
            rootNode = rootNode.right;
        } else {
            rootNode = rootNode.left;
        }

        return rootNode;
    }

    private Node findLargest(Node rootNode) {
        if (rootNode.right != null) {
            rootNode = findLargest(rootNode.right);
        }

        return rootNode;
    }

    private Node removeLargest(Node rootNode) {
        if (rootNode.right != null) {
            Node rightChild = rootNode.right;
            Node subtreeRoot = removeLargest(rightChild);
            rootNode.right = subtreeRoot;
        } else {
            rootNode = rootNode.left;
        }

        return rootNode;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        root = null;
    }

    @Override
    public Iterator<T> getInorderIterator() {
        return new TreeIterator("inorder");
    }

    @Override
    public Iterator<T> getPreorderIterator() {
        return new TreeIterator("preorder");
    }

    @Override
    public Iterator<T> getPostorderIterator() {
        return new TreeIterator("postorder");
    }

    private class ReturnObject {

        private T item;

        private ReturnObject(T entry) {
            item = entry;
        }

        public T get() {
            return item;
        }

        public void set(T entry) {
            item = entry;
        }
    }

    private class Node {

        private T data;
        private Node left;
        private Node right;

        private Node() {
            this(null);
        }

        private Node(T dataPortion) {
            this(dataPortion, null, null);
        }

        private Node(T data, Node left, Node right) {
            this.data = data;
            this.left = left;
            this.right = right;
        }
    }

    private class TreeIterator implements Iterator<T> {

        private final QueueInterface<T> queue = new ArrayQueue<>();

        private TreeIterator(String traversalType) {
            switch (traversalType) {
                case "preorder":
                    preorder(root);
                    break;
                case "postorder":
                    postorder(root);
                    break;
                default:
                    inorder(root);
                    break;
            }
        }

        private void inorder(Node treeNode) {
            if (treeNode != null) {
                inorder(treeNode.left);
                queue.enqueue(treeNode.data);
                inorder(treeNode.right);
            }
        }

        private void preorder(Node treeNode) {
            if (treeNode != null) {
                queue.enqueue(treeNode.data);
                preorder(treeNode.left);
                preorder(treeNode.right);
            }
        }

        private void postorder(Node treeNode) {
            if (treeNode != null) {
                postorder(treeNode.left);
                postorder(treeNode.right);
                queue.enqueue(treeNode.data);
            }
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more entries in tree iterator.");
            }
            return queue.dequeue();
        }
    }
}
