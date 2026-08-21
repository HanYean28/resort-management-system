package control;

import adt.ArrayPriorityQueue;
import entity.Guest;
import entity.Room;

/**
 * Controller class for the VIP Room Allocation module.
 *
 * Purpose:
 * Handles the business logic for VIP room allocation.
 *
 * The Controller:
 * - Stores VIP guests in the Priority Queue
 * - Assigns arrival order
 * - Finds guests
 * - Finds available rooms
 * - Allocates rooms to the highest-priority guest
 * - Removes guests from the waiting list
 */
public class VIPRoomAllocation {

    private ArrayPriorityQueue<Guest> vipQueue;
    private int arrivalOrder;
    private Room[] rooms;


    /**
     * Purpose:
     * Creates the VIP Priority Queue and initializes
     * the room list used by the system.
     */
    public VIPRoomAllocation() {

        vipQueue = new ArrayPriorityQueue<>();

        arrivalOrder = 0;

        /*
         * CHANGE:
         * Previously you had:
         *
         * this.rooms = rooms;
         *
         * This does not initialize the room array.
         *
         * We now create the rooms here.
         */
        rooms = new Room[] {

            new Room(
                    "101",
                    "Deluxe",
                    "Ready",
                    "Today"
            ),

            new Room(
                    "102",
                    "Deluxe",
                    "Ready",
                    "Today"
            ),

            new Room(
                    "201",
                    "Executive",
                    "Occupied",
                    "Today"
            ),

            new Room(
                    "202",
                    "Executive",
                    "Ready",
                    "Today"
            ),

            new Room(
                    "301",
                    "Suite",
                    "Cleaning In Progress",
                    "Today"
            ),

            new Room(
                    "302",
                    "Suite",
                    "Ready",
                    "Today"
            )
        };
    }


    /**
     * Purpose:
     * Adds a VIP guest into the Priority Queue.
     *
     * The arrival order is assigned automatically.
     * The Guest's compareTo() determines the actual
     * loyalty-tier priority.
     */
    public void addVIPGuest(Guest guest) {

        if (guest == null) {
            return;
        }

        /*
         * Assign an arrival number to the guest.
         *
         * Smaller arrivalOrder means the guest arrived earlier.
         */
        guest.setArrivalOrder(arrivalOrder++);

        /*
         * Add the guest into the Priority Queue.
         *
         * The Priority Queue automatically places the
         * guest according to Guest.compareTo().
         */
        vipQueue.add(guest);
    }


    /**
     * Purpose:
     * Returns the VIP guest with the highest priority
     * without removing the guest from the queue.
     */
    public Guest getNextVIPGuest() {

        return vipQueue.peek();
    }


    /**
     * Purpose:
     * Checks whether there are no VIP guests
     * waiting for room allocation.
     */
    public boolean isQueueEmpty() {

        return vipQueue.isEmpty();
    }


    /**
     * Purpose:
     * Returns the number of VIP guests currently
     * waiting for room allocation.
     */
    public int getWaitingGuestCount() {

        return vipQueue.size();
    }


    /**
     * Purpose:
     * Returns all rooms that are currently Ready.
     *
     * Only rooms with cleanliness status "Ready"
     * can be selected for VIP room allocation.
     */
    public Room[] getAvailableRooms() {

        int count = 0;

        /*
         * First count the number of available rooms.
         */
        for (Room room : rooms) {

            /*
             * CHANGE:
             * Check room != null BEFORE accessing
             * getCleanlinessStatus().
             */
            if (room != null
                    && "Ready".equals(
                            room.getCleanlinessStatus())) {

                count++;
            }
        }


        /*
         * Create an array with exactly enough
         * space for the available rooms.
         */
        Room[] availableRooms =
                new Room[count];


        int index = 0;


        /*
         * Store all Ready rooms in the new array.
         */
        for (Room room : rooms) {

            if (room != null
                    && "Ready".equals(
                            room.getCleanlinessStatus())) {

                /*
                 * CHANGE:
                 * Previously you used index++
                 * without declaring index.
                 */
                availableRooms[index++] = room;
            }
        }


        return availableRooms;
    }


    /**
     * Purpose:
     * Allocates a selected available room to the
     * highest-priority VIP guest.
     *
     * The user chooses the room number.
     * The Priority Queue determines which VIP guest
     * receives the room.
     */
    public Guest allocateRoom(String roomNumber) {

        /*
         * Get the highest-priority VIP guest
         * without removing them yet.
         */
        Guest guest =
                vipQueue.peek();


        /*
         * No guest waiting.
         */
        if (guest == null) {
            return null;
        }


        /*
         * Find the room selected by the user.
         */
        Room selectedRoom =
                findAvailableRoom(roomNumber);


        /*
         * The selected room does not exist
         * or is not Ready.
         */
        if (selectedRoom == null) {
            return null;
        }


        /*
         * Only remove the VIP guest AFTER
         * confirming that a valid room is available.
         */
        guest =
                vipQueue.remove();


        /*
         * Assign the selected room to the guest.
         */
        guest.setRoomNo(
                selectedRoom.getRoomNumber()
        );


        /*
         * Change the room status because it
         * is no longer available.
         */
        selectedRoom.setCleanlinessStatus(
                "Occupied"
        );


        return guest;
    }


    /**
     * Purpose:
     * Finds a VIP guest using their unique
     * confirmation number.
     */
    public Guest findGuest(String confirmationNo) {

        /*
         * CHANGE:
         * "string" was incorrect Java syntax.
         * It must be "String".
         */
        if (confirmationNo == null) {
            return null;
        }


        Guest[] guests =
                getWaitingList();


        /*
         * Search through every guest in
         * the waiting list.
         */
        for (Guest guest : guests) {

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
     * Finds a specific room using its room number.
     *
     * The room must also have a "Ready" status.
     */
    private Room findAvailableRoom(
            String roomNumber) {

        if (roomNumber == null
                || roomNumber.isEmpty()) {

            return null;
        }


        for (Room room : rooms) {

            if (room != null
                    && roomNumber.equals(
                            room.getRoomNumber())
                    && "Ready".equals(
                            room.getCleanlinessStatus())) {

                return room;
            }
        }


        return null;
    }


    /**
     * Purpose:
     * Removes a VIP guest from the waiting list
     * using their confirmation number.
     */
    public boolean removeGuest(
            String confirmationNo) {

        if (confirmationNo == null) {
            return false;
        }


        /*
         * Get all current guests.
         */
        Guest[] guests =
                getWaitingList();


        boolean found = false;


        /*
         * Create a new Priority Queue
         * without the guest being removed.
         */
        ArrayPriorityQueue<Guest> newQueue =
                new ArrayPriorityQueue<>();


        for (Guest guest : guests) {

            if (guest != null
                    && confirmationNo.equals(
                            guest.getConfirmationNo())) {

                found = true;

            } else {

                newQueue.add(guest);
            }
        }


        /*
         * Replace the old queue only if
         * the guest was actually found.
         */
        if (found) {

            vipQueue = newQueue;
        }


        return found;
    }


    /**
     * Purpose:
     * Returns all guests currently in the
     * Priority Queue without permanently
     * removing them.
     */
    public Guest[] getWaitingList() {

        Guest[] guests =
                new Guest[vipQueue.size()];


        /*
         * Temporary queue is used to preserve
         * the original Priority Queue.
         */
        ArrayPriorityQueue<Guest> tempQueue =
                new ArrayPriorityQueue<>();


        int index = 0;


        /*
         * Remove each guest temporarily.
         */
        while (!vipQueue.isEmpty()) {

            Guest guest =
                    vipQueue.remove();


            guests[index++] =
                    guest;


            /*
             * Put the guest into the temporary queue.
             */
            tempQueue.add(guest);
        }


        /*
         * Restore the original queue.
         */
        vipQueue =
                tempQueue;


        return guests;
    }


    /**
     * Purpose:
     * Returns the current number of guests
     * waiting for room allocation.
     */
    public int getWaitingListSize() {

        return vipQueue.size();
    }


    /**
     * Purpose:
     * Checks whether the VIP waiting list is empty.
     */
    public boolean isWaitingListEmpty() {

        return vipQueue.isEmpty();
    }
}