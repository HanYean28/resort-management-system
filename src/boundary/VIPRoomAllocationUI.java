package boundary;

import control.VIPRoomAllocation;
import entity.Guest;
import entity.Room;
import java.util.Scanner;

/**
 * Boundary class for the VIP Room Allocation module.
 *
 * Purpose:
 * Handles all user input and output through the CLI.
 *
 * The Boundary does NOT decide VIP priority.
 * The Controller and Priority Queue handle that.
 */
public class VIPRoomAllocationUI {

    private Scanner scanner;
    private VIPRoomAllocation controller;


    /**
     * Purpose:
     * Creates the VIP Room Allocation UI and
     * initializes the Scanner and Controller.
     */
    public VIPRoomAllocationUI() {

        scanner = new Scanner(System.in);

        controller = new VIPRoomAllocation();
    }


    /**
     * Purpose:
     * Starts the VIP Room Allocation menu
     * and handles the user's menu selection.
     */
    public void start() {

        int choice;

        do {

            displayMenu();

            choice = readInt("Enter your choice: ");

            switch (choice) {

                case 1:
                    addVIPGuest();
                    break;

                case 2:
                    allocateRoom();
                    break;

                case 3:
                    findGuest();
                    break;

                case 4:
                    removeGuest();
                    break;

                case 5:
                    displayWaitingList();
                    break;

                case 6:
                    viewNextVIPGuest();
                    break;

                case 7:
                    displayAvailableRooms();
                    break;

                case 8:
                    System.out.println(
                            "Exiting VIP Room Allocation System."
                    );
                    break;

                default:
                    System.out.println(
                            "Invalid choice. Please try again."
                    );
            }

        } while (choice != 8);
    }


    /**
     * Purpose:
     * Displays the main menu of the VIP Room Allocation module.
     */
    private void displayMenu() {

        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "       VIP ROOM ALLOCATION SYSTEM"
        );
        System.out.println(
                "=========================================="
        );

        System.out.println(
                "[1] Add VIP Guest"
        );

        System.out.println(
                "[2] Allocate Room to VIP Guest"
        );

        System.out.println(
                "[3] Find Guest by Confirmation Number"
        );

        System.out.println(
                "[4] Remove Guest from Waiting List"
        );

        System.out.println(
                "[5] Display Waiting List"
        );

        System.out.println(
                "[6] View Next VIP Guest"
        );

        System.out.println(
                "[7] View Available Rooms"
        );

        System.out.println(
                "[8] Exit"
        );

        System.out.println(
                "=========================================="
        );
    }


    /**
     * Purpose:
     * Collects guest information from the user
     * and adds the guest to the VIP Priority Queue.
     */
    private void addVIPGuest() {

        System.out.println();
        System.out.println(
                "===== ADD NEW VIP GUEST ====="
        );


        System.out.print(
                "Enter Confirmation Number: "
        );

        String confirmationNo =
                scanner.nextLine();


        System.out.print(
                "Enter Name: "
        );

        String name =
                scanner.nextLine();


        System.out.print(
                "Enter Phone: "
        );

        String phone =
                scanner.nextLine();


        System.out.print(
                "Enter Loyalty Tier "
                + "(Silver, Gold, Platinum, Elite, Diamond): "
        );

        String loyaltyTier =
                scanner.nextLine();


        double billingAmount =
                readDouble("Enter Billing Amount: ");


        /*
         * Room is initially null because
         * the guest has not received a room yet.
         */
        Guest guest =
                new Guest(
                        confirmationNo,
                        name,
                        phone,
                        loyaltyTier,
                        billingAmount,
                        null
                );


        /*
         * Send the guest to the Controller.
         *
         * The Controller will assign the arrival order
         * and add the guest to the Priority Queue.
         */
        controller.addVIPGuest(guest);


        System.out.println();
        System.out.println(
                "VIP Guest added successfully."
        );
    }


    /**
     * Purpose:
     * Displays the VIP guest who currently has
     * the highest priority.
     *
     * This demonstrates that the Priority Queue
     * automatically places the highest-priority
     * guest at the front.
     */
    private void viewNextVIPGuest() {

        System.out.println();
        System.out.println(
                "===== NEXT VIP GUEST ====="
        );


        Guest guest =
                controller.getNextVIPGuest();


        if (guest == null) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

            return;
        }


        System.out.println(
                "Highest Priority Guest:"
        );

        displayGuestDetails(guest);
    }


    /**
     * Purpose:
     * Displays all VIP guests according to
     * their priority.
     */
    private void displayWaitingList() {

        System.out.println();
        System.out.println(
                "===== VIP WAITING LIST ====="
        );


        Guest[] guests =
                controller.getWaitingList();


        if (guests == null || guests.length == 0) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

            return;
        }


        /*
         * The Priority Queue stores the lowest priority
         * at index 0 and highest priority at the end.
         *
         * Therefore, display the array backwards so
         * the highest-priority guest appears first.
         */
        int priorityNumber = 1;


        for (int i = guests.length - 1;
                i >= 0;
                i--) {

            System.out.println();
            System.out.println(
                    "Priority " + priorityNumber
            );

            displayGuestDetails(
                    guests[i]
            );

            priorityNumber++;
        }
    }


    /**
     * Purpose:
     * Displays complete information about one guest.
     */
    private void displayGuestDetails(Guest guest) {

        if (guest == null) {
            return;
        }


        System.out.println(
                "Confirmation No: "
                + guest.getConfirmationNo()
        );

        System.out.println(
                "Name: "
                + guest.getName()
        );

        System.out.println(
                "Phone: "
                + guest.getPhone()
        );

        System.out.println(
                "Loyalty Tier: "
                + guest.getLoyaltyTier()
        );

        System.out.println(
                "Billing Amount: RM "
                + guest.getBillingAmount()
        );

        System.out.println(
                "Room No: "
                + (
                    guest.getRoomNo() == null
                    ? "Not Assigned"
                    : guest.getRoomNo()
                )
        );
    }


    /**
     * Purpose:
     * Displays all rooms that are currently
     * available for VIP allocation.
     */
    private void displayAvailableRooms() {

        System.out.println();
        System.out.println(
                "===== AVAILABLE ROOMS ====="
        );


        Room[] availableRooms =
                controller.getAvailableRooms();


        if (availableRooms == null
                || availableRooms.length == 0) {

            System.out.println(
                    "No available rooms for allocation."
            );

            return;
        }


        /*
         * Display each available room clearly.
         */
        for (int i = 0;
                i < availableRooms.length;
                i++) {

            Room room =
                    availableRooms[i];


            System.out.println(
                    "[" + (i + 1) + "] "
                    + "Room Number: "
                    + room.getRoomNumber()
                    + " | Type: "
                    + room.getRoomType()
                    + " | Status: "
                    + room.getCleanlinessStatus()
            );
        }
    }


    /**
     * Purpose:
     * Allows the user to allocate an available room
     * to the highest-priority VIP guest.
     *
     * The system determines WHICH guest gets the room.
     * The user chooses WHICH available room is assigned.
     */
    private void allocateRoom() {

        System.out.println();
        System.out.println(
                "===== ALLOCATE ROOM ====="
        );


        /*
         * Check whether there is a VIP guest waiting.
         */
        if (controller.isQueueEmpty()) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

            return;
        }


        /*
         * Get the guest with the highest priority.
         */
        Guest nextGuest =
                controller.getNextVIPGuest();


        System.out.println();
        System.out.println(
                "Guest with highest priority:"
        );

        System.out.println(
                "Name: "
                + nextGuest.getName()
        );

        System.out.println(
                "Loyalty Tier: "
                + nextGuest.getLoyaltyTier()
        );

        System.out.println(
                "Confirmation No: "
                + nextGuest.getConfirmationNo()
        );


        /*
         * Get all available rooms.
         */
        Room[] availableRooms =
                controller.getAvailableRooms();


        if (availableRooms == null
                || availableRooms.length == 0) {

            System.out.println();
            System.out.println(
                    "No available rooms for allocation."
            );

            return;
        }


        /*
         * Display the available rooms.
         */
        System.out.println();
        System.out.println(
                "Available Rooms:"
        );


        for (int i = 0;
                i < availableRooms.length;
                i++) {

            Room room =
                    availableRooms[i];


            System.out.println(
                    "[" + (i + 1) + "] "
                    + "Room "
                    + room.getRoomNumber()
                    + " | Type: "
                    + room.getRoomType()
                    + " | Status: "
                    + room.getCleanlinessStatus()
            );
        }


        /*
         * Ask the user to choose a room.
         */
        int roomChoice =
                readInt(
                        "Select a room: "
                );


        /*
         * FIX:
         *
         * Your previous code used:
         *
         * selection
         *
         * but the variable was actually named:
         *
         * roomChoice
         */
        if (roomChoice < 1
                || roomChoice > availableRooms.length) {

            System.out.println(
                    "Invalid room selection."
            );

            return;
        }


        /*
         * Get the room number from the selected
         * room in the array.
         */
        String selectedRoomNumber =
                availableRooms[
                        roomChoice - 1
                ].getRoomNumber();


        /*
         * Ask the Controller to allocate the selected
         * room to the highest-priority VIP guest.
         */
        Guest guest =
                controller.allocateRoom(
                        selectedRoomNumber
                );


        if (guest == null) {

            System.out.println(
                    "Room allocation failed. "
                    + "Please try again."
            );

            return;
        }


        /*
         * Display successful allocation details.
         */
        System.out.println();
        System.out.println(
                "=========================================="
        );

        System.out.println(
                "       ROOM ALLOCATED SUCCESSFULLY"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Guest: "
                + guest.getName()
        );

        System.out.println(
                "Confirmation No: "
                + guest.getConfirmationNo()
        );

        System.out.println(
                "Loyalty Tier: "
                + guest.getLoyaltyTier()
        );

        System.out.println(
                "Room No: "
                + guest.getRoomNo()
        );
    }


    /**
     * Purpose:
     * Searches for a VIP guest using
     * their confirmation number.
     */
    private void findGuest() {

        System.out.println();
        System.out.println(
                "===== FIND VIP GUEST ====="
        );


        System.out.print(
                "Enter Confirmation Number: "
        );


        String confirmationNo =
                scanner.nextLine();


        Guest guest =
                controller.findGuest(
                        confirmationNo
                );


        if (guest == null) {

            System.out.println(
                    "Guest not found."
            );

        } else {

            System.out.println();
            System.out.println(
                    "Guest Found:"
            );

            displayGuestDetails(guest);
        }
    }


    /**
     * Purpose:
     * Removes a VIP guest from the waiting list
     * using their confirmation number.
     */
    private void removeGuest() {

        System.out.println();
        System.out.println(
                "===== REMOVE GUEST ====="
        );


        System.out.print(
                "Enter Confirmation Number: "
        );


        String confirmationNo =
                scanner.nextLine();


        /*
         * Ask the Controller to remove the guest.
         */
        boolean success =
                controller.removeGuest(
                        confirmationNo
                );


        if (success) {

            System.out.println(
                    "Guest removed successfully."
            );

        } else {

            System.out.println(
                    "Guest not found."
            );
        }
    }


    /**
     * Purpose:
     * Safely reads an integer from the user.
     *
     * This avoids input problems caused by mixing
     * nextInt() and nextLine().
     */
    private int readInt(String message) {

        while (true) {

            try {

                System.out.print(message);

                return Integer.parseInt(
                        scanner.nextLine()
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid number."
                );
            }
        }
    }


    /**
     * Purpose:
     * Safely reads a double value from the user.
     */
    private double readDouble(String message) {

        while (true) {

            try {

                System.out.print(message);

                return Double.parseDouble(
                        scanner.nextLine()
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter a valid amount."
                );
            }
        }
    }
}