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
 *
 * @author Lim How Voon
 */
public class ArrayPriorityQueue {

    private BinaryHeap<Guest> heap;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Creates an empty VIP priority queue backed by a BinaryHeap.
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
    // Public methods
    // -------------------------------------------------------

    /**
     * Adds a guest into the priority queue.
     *
     * The BinaryHeap's insert() places the guest at the next leaf,
     * then bubbles it UP the tree until correctly positioned.
     *
     * O(log n)
     *
     * @param guest the VIP guest to enqueue
     * @throws IllegalArgumentException if guest is null
     */
    public void add(Guest guest) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null");
        }

        heap.insert(guest);
    }

    /**
     * Removes and returns the highest-priority guest from the queue.
     *
     * BinaryHeap.removeMax() takes the root, moves the last leaf
     * to the root, then bubbles it DOWN to restore heap order.
     *
     * O(log n)
     *
     * @return the highest-tier guest waiting, or null if the queue is empty
     */
    public Guest remove() {

        return heap.removeMax();
    }

    /**
     * Returns the highest-priority guest without removing them.
     *
     * O(1) — direct root access.
     *
     * @return the highest-tier guest, or null if the queue is empty
     */
    public Guest peek() {

        return heap.peekMax();
    }

    /**
     * Removes a guest from the queue by their confirmation number.
     *
     * Scans the heap to find the matching guest, then delegates
     * removal to BinaryHeap.remove() which restores heap order.
     *
     * O(n) to locate + O(log n) to reheapify
     *
     * @param confirmationNo the 8-digit confirmation number to search for
     * @return true if the guest was found and removed; false otherwise
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
     * Searches for a guest using their confirmation number.
     *
     * Uses getAll() to retrieve guests in priority order,
     * then performs a linear scan for the matching number.
     *
     * O(n)
     *
     * @param confirmationNo the confirmation number to search for
     * @return the matching Guest, or null if not found
     */
    public Guest find(String confirmationNo) {

        if (confirmationNo == null) {
            return null;
        }

        Guest[] all = getAll();

        for (Guest guest : all) {
            if (guest != null
                    && confirmationNo.equals(guest.getConfirmationNo())) {
                return guest;
            }
        }

        return null;
    }

    /**
     * Returns all guests in descending priority order without
     * removing them from the queue.
     *
     * Delegates to BinaryHeap.toSortedArray() which works on a
     * temporary copy — the real queue is untouched.
     *
     * O(n log n)
     *
     * @return array of Guest objects ordered highest-tier first
     */
    public Guest[] getAll() {

        Guest[] result = new Guest[heap.size()];
        return heap.toSortedArray(result);
    }

    /**
     * Returns the number of guests currently waiting in the VIP queue.
     *
     * @return current queue size
     */
    public int size() {

        return heap.size();
    }

    /**
     * Returns true if no guests are waiting in the VIP queue.
     *
     * @return true if empty
     */
    public boolean isEmpty() {

        return heap.isEmpty();
    }

    /**
     * Removes all guests from the VIP queue.
     */
    public void clear() {

        heap.clear();
    }

    // -------------------------------------------------------
    // Private helper
    // -------------------------------------------------------

    /**
     * Converts a loyalty tier string into a numeric priority.
     *
     * Higher number = higher priority = closer to the heap root.
     *
     * Diamond  → 5  (highest)
     * Elite    → 4
     * Platinum → 3
     * Gold     → 2
     * Silver   → 1
     * None     → 0  (lowest)
     *
     * @param loyaltyTier the tier string from Guest.getLoyaltyTier()
     * @return numeric priority (0–5)
     */
    private int getPriority(String loyaltyTier) {

        if (loyaltyTier == null) {
            return 0;
        }

        switch (loyaltyTier.toUpperCase()) {

            case "DIAMOND":  return 5;
            case "ELITE":    return 4;
            case "PLATINUM": return 3;
            case "GOLD":     return 2;
            case "SILVER":   return 1;
            case "NONE":     return 0;
            default:         return 0;
        }
    }
}