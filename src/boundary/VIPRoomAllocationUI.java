package boundary;

import control.VIPRoomAllocation;
import entity.Guest;
import entity.Room;
import java.util.Scanner;

import static boundary.UIUtils.clearScreen;
import static boundary.UIUtils.pressEnterToContinue;

public class VIPRoomAllocationUI {

    private Scanner scanner;
    private VIPRoomAllocation controller;

    public VIPRoomAllocationUI(Scanner scanner) {
        controller = new VIPRoomAllocation();
        this.scanner = scanner;
    }

    /**
     * Purpose:
     * Starts the VIP Room Allocation menu and processes
     * the user's selected operation.
     */
    public void start() {

        int choice;

        do {
            clearScreen();
            displayMenu();

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {

                case 1:
                    addVIPBooking();
                    break;

                case 2:
                    viewNextVIPGuest();
                    break;

                case 3:
                    displayWaitingList();
                    break;

                case 4:
                    allocateRoom();
                    break;

                case 5:
                    findGuest();
                    break;

                case 6:
                    removeGuest();
                    break;

                case 7:
                    System.out.println(
                            "Exiting VIP Room Allocation System."
                    );
                    break;

                default:
                    System.out.println(
                            "Invalid choice. Please try again."
                    );
            }

        } while (choice != 7);
    }

    /**
     * Purpose:
     * Displays the available functions of the VIP
     * Room Allocation module.
     */
    private void displayMenu() {

        System.out.println("\n======================================");
        System.out.println("      VIP ROOM ALLOCATION SYSTEM");
        System.out.println("======================================");
        System.out.println("[1] Add VIP Booking");
        System.out.println("[2] View Next VIP Guest");
        System.out.println("[3] Display Waiting List");
        System.out.println("[4] Allocate Room");
        System.out.println("[5] Find Guest by Confirmation Number");
        System.out.println("[6] Remove Guest from Waiting List");
        System.out.println("[7] Exit");
        System.out.print("Enter your choice: ");
    }

    /**
     * Purpose:
     * Collects information for a new VIP guest booking
     * and adds the guest into the VIP priority queue.
     */
    private void addVIPBooking() {

        clearScreen();
        System.out.println("\n===== ADD VIP BOOKING =====");

        System.out.print("Enter Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Phone: ");
        String phone = scanner.nextLine();

        System.out.print(
                "Enter Loyalty Tier (Diamond, Elite, Platinum, Gold, Silver): "
        );
        String loyaltyTier = scanner.nextLine();

        System.out.print("Enter Requested Room Type (Standard, Deluxe, Suite): ");
        String requestedRoomType = scanner.nextLine();

        System.out.print("Enter Check-In Date (yyyy-MM-dd): ");
        String checkInDate = scanner.nextLine();

        System.out.print("Enter Check-Out Date (yyyy-MM-dd): ");
        String checkOutDate = scanner.nextLine();

        /*
         * addVIPGuest now delegates guest creation and booking
         * persistence entirely to BookingController.
         * Returns null on success, or an error message on failure.
         */
        String error = controller.addVIPGuest(
                name,
                phone,
                loyaltyTier,
                requestedRoomType,
                checkInDate,
                checkOutDate
        );

        if (error != null) {
            System.out.println("\nFailed to add VIP booking: " + error);
        } else {
            System.out.println(
                    "\nVIP booking added successfully."
            );
        }

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Displays the guest currently at the front of
     * the VIP priority queue.
     */
    private void viewNextVIPGuest() {

        clearScreen();
        Guest guest = controller.getNextVIPGuest();

        if (guest == null) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

        } else {

            System.out.println(
                    "\n===== NEXT VIP GUEST ====="
            );

            displayGuestDetails(guest);
        }

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Displays all VIP guests currently waiting for
     * room allocation in priority order.
     */
    private void displayWaitingList() {

        clearScreen();
        Guest[] guests = controller.getWaitingList();

        if (guests.length == 0) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

            return;
        }

        System.out.println(
                "\n===== VIP WAITING LIST ====="
        );

        for (int i = 0; i < guests.length; i++) {

            System.out.println(
                    "\nPriority " + (i + 1)
            );

            displayGuestDetails(guests[i]);
        }

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Displays available rooms and allows the
     * highest-priority VIP guest to select a room.
     *
     * This combines:
     * 1. View available rooms
     * 2. Select a room
     * 3. Allocate the room
     */
    private void allocateRoom() {

        clearScreen();

        /*
         * The guest at the front of the priority queue
         * is the guest who receives the next allocation.
         */
        Guest nextGuest = controller.getNextVIPGuest();

        if (nextGuest == null) {

            System.out.println(
                    "No VIP guests in the waiting list."
            );

            return;
        }

        /*
         * Get rooms whose status is currently "Ready".
         */
        Room[] availableRooms =
                controller.getAvailableRooms();

        if (availableRooms.length == 0) {

            System.out.println(
                    "No available rooms."
            );

            return;
        }

        /*
         * Display the VIP guest who has the highest
         * allocation priority.
         */
        System.out.println(
                "\n===== GUEST RECEIVING PRIORITY ====="
        );

        displayGuestDetails(nextGuest);

        /*
         * Display all available rooms.
         */
        System.out.println(
                "\n===== AVAILABLE ROOMS ====="
        );

        for (int i = 0; i < availableRooms.length; i++) {

            System.out.println(
                    "[" + (i + 1) + "] "
                    + availableRooms[i]
            );
        }

        /*
         * Ask the user to select a room.
         */
        System.out.print(
                "Select room: "
        );

        int selection = scanner.nextInt();
        scanner.nextLine();

        /*
         * Validate the user's room selection.
         */
        if (selection < 1 ||
                selection > availableRooms.length) {

            System.out.println(
                    "Invalid room selection."
            );

            return;
        }

        /*
         * Obtain the room number selected by the user.
         */
        String selectedRoom =
                availableRooms[selection - 1]
                        .getRoomNumber();

        /*
         * Controller performs the actual allocation.
         *
         * The controller removes the highest-priority
         * guest from the queue and assigns the selected room.
         */
        Guest allocatedGuest =
                controller.allocateRoom(selectedRoom);

        if (allocatedGuest == null) {

            System.out.println(
                    "Room allocation failed."
            );

            return;
        }

        /*
         * Display the result of the successful allocation.
         */
        System.out.println();
        System.out.println(
                "===== ROOM ALLOCATED SUCCESSFULLY ====="
        );

        displayGuestDetails(allocatedGuest);

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Searches for a VIP guest using the guest's
     * confirmation number.
     */
    private void findGuest() {

        clearScreen();
        System.out.print(
                "Enter Confirmation Number: "
        );

        String confirmationNo =
                scanner.nextLine();

        Guest guest =
                controller.findGuest(confirmationNo);

        if (guest == null) {

            System.out.println(
                    "Guest not found in VIP waiting list."
            );

        } else {

            System.out.println(
                    "\n===== GUEST FOUND ====="
            );

            displayGuestDetails(guest);
        }

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Removes a VIP guest from the waiting queue
     * using the guest's confirmation number.
     */
    private void removeGuest() {

        clearScreen();
        System.out.print(
                "Enter Confirmation Number: "
        );

        String confirmationNo =
                scanner.nextLine();

        boolean removed =
                controller.removeGuest(confirmationNo);

        if (removed) {

            System.out.println(
                    "Guest removed successfully."
            );

        } else {

            System.out.println(
                    "Guest not found."
            );
        }

        pressEnterToContinue(scanner);
    }

    /**
     * Purpose:
     * Displays the complete information of a guest.
     */
    private void displayGuestDetails(Guest guest) {

        if (guest == null) {
            return;
        }

        System.out.println(
                "Confirmation No : "
                + guest.getConfirmationNo()
        );

        System.out.println(
                "Name            : "
                + guest.getName()
        );

        System.out.println(
                "Phone           : "
                + guest.getPhone()
        );

        System.out.println(
                "Loyalty Tier    : "
                + guest.getLoyaltyTier()
        );

        System.out.println(
                "Billing Amount  : RM "
                + guest.getBillingAmount()
        );

        System.out.println(
                "Room No         : "
                + guest.getRoomNo()
        );

        // Show booking date from bookings.txt for transparency.
        String createdAt =
                controller.getBookingCreatedAt(guest.getConfirmationNo());

        System.out.println(
                "Booking Date    : "
                + (createdAt != null ? createdAt : "N/A")
        );
    }
}
