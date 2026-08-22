package adt;

import entity.Guest;

/**
 * Priority Queue specifically used for VIP room allocation.
 *
 * Guests are stored according to their loyalty tier.
 * Higher loyalty tier = higher priority.
 *
 * The queue uses the global ArrayList ADT.
 */
public class ArrayPriorityQueue {

    private ArrayList<Guest> list;

    /**
     * Purpose:
     * Creates an empty VIP priority queue.
     */
    public ArrayPriorityQueue() {
        list = new ArrayList<>();
    }

    /**
     * Purpose:
     * Adds a guest into the priority queue according
     * to the guest's loyalty tier.
     *
     * Higher priority guests are placed closer to
     * the front of the queue.
     */
    public void add(Guest guest) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null");
        }

        int newPriority = getPriority(guest.getLoyaltyTier());

        int position = 1;

        // Find the correct position for the new guest.
        while (position <= list.getNumberOfEntries()) {

            Guest currentGuest = list.getEntry(position);

            int currentPriority =
                    getPriority(currentGuest.getLoyaltyTier());

            /*
             * Higher priority guest should be placed
             * before lower priority guest.
             */
            if (newPriority > currentPriority) {
                break;
            }

            /*
             * If the priority is equal, continue forward.
             * This keeps the existing guest before the new guest.
             */
            position++;
        }

        // IMPORTANT:
        // Your original code calculated 'position' but never
        // actually inserted the guest.
        list.add(position, guest);
    }

    /**
     * Purpose:
     * Removes and returns the highest-priority guest
     * from the front of the queue.
     */
    public Guest remove() {

        if (list.isEmpty()) {
            return null;
        }

        return list.remove(1);
    }

    /**
     * Purpose:
     * Returns the highest-priority guest without
     * removing the guest from the queue.
     */
    public Guest peek() {

        if (list.isEmpty()) {
            return null;
        }

        return list.getEntry(1);
    }

    /**
     * Purpose:
     * Removes a guest from the queue using
     * the guest's confirmation number.
     */
    public boolean removeByConfirmationNo(String confirmationNo) {

        if (confirmationNo == null) {
            return false;
        }

        for (int i = 1; i <= list.getNumberOfEntries(); i++) {

            Guest guest = list.getEntry(i);

            if (guest != null
                    && confirmationNo.equals(guest.getConfirmationNo())) {

                list.remove(i);
                return true;
            }
        }

        return false;
    }

    /**
     * Purpose:
     * Searches for a guest using their
     * confirmation number.
     */
    public Guest find(String confirmationNo) {

        if (confirmationNo == null) {
            return null;
        }

        for (int i = 1; i <= list.getNumberOfEntries(); i++) {

            Guest guest = list.getEntry(i);

            /*
             * FIX:
             * Your original code compared:
             *
             * guest.getConfirmationNo()
             * ==
             * guest.getConfirmationNo()
             *
             * which is always true.
             *
             * We need to compare against the parameter.
             */
            if (guest != null
                    && confirmationNo.equals(guest.getConfirmationNo())) {

                return guest;
            }
        }

        return null;
    }

    /**
     * Purpose:
     * Returns all guests currently in the queue
     * without removing them.
     */
    public Guest[] getAll() {

        Guest[] guests =
                new Guest[list.getNumberOfEntries()];

        for (int i = 1; i <= list.getNumberOfEntries(); i++) {

            guests[i - 1] = list.getEntry(i);
        }

        return guests;
    }

    /**
     * Purpose:
     * Returns the number of guests currently
     * waiting in the VIP queue.
     */
    public int size() {

        return list.getNumberOfEntries();
    }

    /**
     * Purpose:
     * Checks whether the VIP queue is empty.
     */
    public boolean isEmpty() {

        return list.isEmpty();
    }

    /**
     * Purpose:
     * Removes all guests from the VIP queue.
     */
    public void clear() {

        list.clear();
    }

    /**
     * Purpose:
     * Converts a loyalty tier into a numerical
     * priority used internally by the ADT.
     *
     * Higher number = higher priority.
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