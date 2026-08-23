package adt;

import entity.Guest;

/**
 * Priority Queue specifically used for VIP room allocation.
 *
 * Guests are stored according to their loyalty tier.
 * Higher loyalty tier = higher priority.
 *
 * Previously backed by ArrayList (linear ADT).
 * Now backed by BinaryHeap (non-linear ADT).
 *
 * The BinaryHeap organises guests as a complete binary tree:
 *
 *              [Diamond]
 *             /          \
 *         [Elite]      [Platinum]
 *        /      \
 *     [Gold]  [Silver]
 *
 * Every public method signature is unchanged — the rest of
 * the codebase (VIPRoomAllocation, UI, Simulation) does not
 * need any modification.
 */
public class ArrayPriorityQueue {

    private BinaryHeap<Guest> heap;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Purpose:
     * Creates an empty VIP priority queue backed by a
     * BinaryHeap.
     *
     * The comparator passed to BinaryHeap defines priority:
     * a guest with a higher tier value is placed closer
     * to the root (max-heap behaviour).
     */
    public ArrayPriorityQueue() {

        heap = new BinaryHeap<>((a, b) ->
                getPriority(a.getLoyaltyTier())
                - getPriority(b.getLoyaltyTier())
        );
    }

    // -------------------------------------------------------
    // Public methods (signatures unchanged)
    // -------------------------------------------------------

    /**
     * Purpose:
     * Adds a guest into the priority queue.
     *
     * The BinaryHeap's insert() places the guest at the
     * next leaf, then bubbles it UP the tree until its
     * priority is correctly positioned.
     *
     * This replaces the old linear scan-and-insert into
     * ArrayList — insertion is now O(log n) instead of O(n).
     */
    public void add(Guest guest) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null");
        }

        heap.insert(guest);
    }

    /**
     * Purpose:
     * Removes and returns the highest-priority guest
     * from the front of the queue.
     *
     * The BinaryHeap's removeMax() takes the root (highest
     * priority), moves the last leaf to the root, then
     * bubbles it DOWN until heap order is restored.
     */
    public Guest remove() {

        return heap.removeMax();
    }

    /**
     * Purpose:
     * Returns the highest-priority guest without
     * removing the guest from the queue.
     *
     * Direct O(1) root access in the heap.
     */
    public Guest peek() {

        return heap.peekMax();
    }

    /**
     * Purpose:
     * Removes a guest from the queue using
     * the guest's confirmation number.
     *
     * Scans the heap to find the matching guest,
     * then delegates removal to BinaryHeap.remove()
     * which restores heap order after deletion.
     */
    public boolean removeByConfirmationNo(String confirmationNo) {

        if (confirmationNo == null) {
            return false;
        }

        Guest target = find(confirmationNo);

        if (target == null) {
            return false;
        }

        return heap.remove(target);
    }

    /**
     * Purpose:
     * Searches for a guest using their confirmation number.
     *
     * Uses toSortedArray() to get all guests, then scans
     * for a matching confirmation number.
     */
    public Guest find(String confirmationNo) {

        if (confirmationNo == null) {
            return null;
        }

        Guest[] all = getAll();

        for (Guest guest : all) {

            if (guest != null
                    && confirmationNo.equals(
                            guest.getConfirmationNo())) {
                return guest;
            }
        }

        return null;
    }

    /**
     * Purpose:
     * Returns all guests currently in the queue
     * in priority order (highest first) without
     * removing them.
     *
     * Delegates to BinaryHeap.toSortedArray() which
     * extracts from a temporary copy of the heap so
     * the real queue is not disturbed.
     */
    public Guest[] getAll() {

        Guest[] result = new Guest[heap.size()];

        return heap.toSortedArray(result);
    }

    /**
     * Purpose:
     * Returns the number of guests currently
     * waiting in the VIP queue.
     */
    public int size() {

        return heap.size();
    }

    /**
     * Purpose:
     * Checks whether the VIP queue is empty.
     */
    public boolean isEmpty() {

        return heap.isEmpty();
    }

    /**
     * Purpose:
     * Removes all guests from the VIP queue.
     */
    public void clear() {

        heap.clear();
    }

    // -------------------------------------------------------
    // Private helper
    // -------------------------------------------------------

    /**
     * Purpose:
     * Converts a loyalty tier into a numerical priority
     * used by the BinaryHeap comparator.
     *
     * Higher number = higher priority = closer to root.
     */
    private int getPriority(String loyaltyTier) {

        if (loyaltyTier == null) {
            return 0;
        }

        switch (loyaltyTier.toUpperCase()) {

            case "DIAMOND":
                return 5;

            case "ELITE":
                return 4;

            case "PLATINUM":
                return 3;

            case "GOLD":
                return 2;

            case "SILVER":
                return 1;

            case "NONE":
                return 0;

            default:
                return 0;
        }
    }
}