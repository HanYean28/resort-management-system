package boundary;

import control.VIPRoomAllocation;
import control.VIPRoomAllocation.AllocationResult;
import entity.Guest;
import entity.Room;

import java.util.List;
import java.util.Scanner;

/**
 * Boundary class for Module 2 — VIP & Loyalty Tier Priority Room Allocation.
 *
 * Handles all console I/O for the VIP room allocation module.
 * Delegates all business logic to VIPRoomAllocationController.
 *
 * Menu structure:
 *   1. Add VIP Guest to Queue
 *   2. View Queue (priority order)
 *   3. View Available Rooms
 *   4. Allocate Room to Next VIP Guest
 *   5. Allocate Rooms to All Waiting Guests
 *   6. Remove Guest from Queue
 *   7. View Allocation Log
 *   8. Demo — Load Sample Data
 *   0. Exit
 *
 * @author Lim How Voon
 */
public class VIPRoomAllocationUI {

    // -------------------------------------------------------
    // Constants — valid loyalty tiers
    // -------------------------------------------------------

    private static final String[] VALID_TIERS = {
        "DIAMOND", "ELITE", "PLATINUM", "GOLD", "SILVER", "NONE"
    };

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------

    private VIPRoomAllocation controller;
    private Scanner scanner;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Creates the boundary with a fresh controller.
     * Call start() to begin the interactive session.
     */
    public VIPRoomAllocationUI(Scanner scanner) {
        controller  = new VIPRoomAllocation();
        this.scanner = scanner;
    }

    // -------------------------------------------------------
    // Entry point
    // -------------------------------------------------------

    /**
     * Starts the interactive console menu loop.
     * Returns when the user selects option 0 (Exit).
     */
    public void start() {

        printBanner();

        boolean running = true;

        while (running) {

            UIUtils.clearScreen();
            printMenu();
            int choice = readInt("Enter choice: ");

            switch (choice) {

                case 1:
                    handleAddGuest();
                    break;

                case 2:
                    handleViewQueue();
                    break;

                case 3:
                    handleViewRooms();
                    break;

                case 4:
                    handleAllocateNext();
                    break;

                case 5:
                    handleAllocateAll();
                    break;

                case 6:
                    handleRemoveGuest();
                    break;

                case 7:
                    handleViewLog();
                    break;

                case 8:
                    handleLoadSampleData();
                    break;

                case 0:
                    running = false;
                    print("\nExiting VIP Room Allocation module. Goodbye!");
                    break;

                default:
                    printError("Invalid option. Please enter a number from the menu.");
            }
        }
    }

    // -------------------------------------------------------
    // Menu handlers
    // -------------------------------------------------------

    /**
     * Prompts the user to enter a new VIP guest and adds them
     * to the priority queue.
     */
    private void handleAddGuest() {

        UIUtils.clearScreen();
        printHeader("Add VIP Guest to Queue");

        // Auto-generate confirmation number — no manual input needed.
        String confirmationNo = controller.generateConfirmationNo();
        print("  Confirmation No : " + confirmationNo + " (auto-generated)");
        print("");

        String name  = readNonEmpty("Guest name        : ");
        String phone = readNonEmpty("Phone number      : ");
        String tier  = readTier("Loyalty tier      : ");
        String requestedRoomType = readRoomType("Requested room type: ");

        Guest guest = new Guest(confirmationNo, name, phone, tier);
        controller.addGuest(guest, requestedRoomType);

        printSuccess("Guest added to the VIP queue.");
        printGuestCard(guest);
        print("  Requested Room  : " + requestedRoomType);
        printQueueStatus();

        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Displays all guests currently in the queue, highest-tier first.
     */
    private void handleViewQueue() {

        UIUtils.clearScreen();
        printHeader("VIP Queue (Priority Order)");

        if (controller.isQueueEmpty()) {
            print("  The VIP queue is empty.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print(String.format("  %-4s %-12s %-24s %-14s %s",
              "#", "Conf #", "Name", "Tier", "Phone"));
        printDivider(72);

        Guest[] guests = controller.getAllWaitingGuests();
        int rank = 1;

        for (Guest g : guests) {
            if (g != null) {
                print(String.format("  %-4d %-12s %-24s %-14s %s",
                      rank++,
                      g.getConfirmationNo(),
                      g.getName(),
                      g.getLoyaltyTier(),
                      g.getPhone()));
            }
        }

        printDivider(72);
        print("  Total waiting: " + controller.getQueueSize());

        Guest next = controller.peekNextGuest();
        if (next != null) {
            print("  Next to be allocated: " + next.getName()
                  + " [" + next.getLoyaltyTier() + "]");
        }

        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Displays all rooms that are currently available for assignment.
     */
    private void handleViewRooms() {

        UIUtils.clearScreen();
        printHeader("Available Rooms");

        List<Room> rooms = controller.getAvailableRooms();

        if (rooms.isEmpty()) {
            print("  No rooms are currently available for assignment.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print(String.format("  %-4s %-8s %-14s %-22s %s",
              "#", "Room", "Type", "Status", "Last Update"));
        printDivider(72);

        int i = 1;
        for (Room r : rooms) {
            print(String.format("  %-4d %-8s %-14s %-22s %s",
                  i++,
                  r.getRoomNumber(),
                  r.getRoomType(),
                  r.getCleanlinessStatus(),
                  r.getLastUpdate()));
        }

        printDivider(72);
        print("  Total available: " + rooms.size());

        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Allocates the next available room to the highest-tier waiting guest.
     */
    private void handleAllocateNext() {

        UIUtils.clearScreen();
        printHeader("Allocate Room — Next VIP Guest");

        if (controller.isQueueEmpty()) {
            printError("No VIP guests are currently waiting.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        Guest next = controller.peekNextGuest();
        String requestedType = controller.getRequestedRoomType(next.getConfirmationNo());

        print("  Next guest      : " + next.getName() + " [" + next.getLoyaltyTier() + "]");
        print("  Requested type  : " + (requestedType != null ? requestedType : "No preference"));
        print("  Available rooms : " + controller.getAvailableRoomCount());
        print("");

        // Attempt auto-match by requested room type.
        AllocationResult result = controller.allocateNextRoom();

        if (result.isSuccess()) {
            printSuccess("Room auto-matched and allocated successfully!");
            print("");
            printAllocationSummary(result);
            printQueueStatus();
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        // Auto-match failed — check if there are still rooms available.
        List<Room> available = controller.getAvailableRooms();

        if (available.isEmpty()) {
            printError(result.getMessage());
            printQueueStatus();
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        // Show reason for fallback and prompt manual selection.
        printError(result.getMessage());
        print("");
        print("  Please select a room manually:");
        print("");
        printRoomList(available);

        int selection = readInt("  Select room number [1-" + available.size() + "]: ");

        if (selection < 1 || selection > available.size()) {
            printError("Invalid selection. Allocation cancelled.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        Room chosen = available.get(selection - 1);
        AllocationResult manual = controller.allocateRoom(next, chosen.getRoomNumber());

        if (manual.isSuccess()) {
            printSuccess("Room manually allocated successfully!");
            print("");
            printAllocationSummary(manual);
        } else {
            printError(manual.getMessage());
        }

        printQueueStatus();
        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Allocates rooms to all waiting guests in tier priority order
     * until the queue or room pool is exhausted.
     */
    private void handleAllocateAll() {

        UIUtils.clearScreen();
        printHeader("Allocate Rooms — All Waiting Guests");

        if (controller.isQueueEmpty()) {
            printError("No guests are waiting in the VIP queue.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print("  Guests waiting : " + controller.getQueueSize());
        print("  Rooms available: " + controller.getAvailableRoomCount());
        print("");

        String confirm = readNonEmpty("Proceed with batch allocation? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            print("  Batch allocation cancelled.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print("");
        printDivider(72);
        print("  ALLOCATION RESULTS");
        printDivider(72);

        int succeeded = 0;
        int failed    = 0;

        // Process guests one by one until queue or rooms exhausted.
        while (!controller.isQueueEmpty()) {

            List<Room> available = controller.getAvailableRooms();

            if (available.isEmpty()) {
                // No more rooms — report remaining guests as unallocated.
                Guest[] remaining = controller.getAllWaitingGuests();
                for (Guest g : remaining) {
                    if (g != null) {
                        printError("No room available for "
                                + g.getName() + " [" + g.getLoyaltyTier() + "]");
                        failed++;
                    }
                }
                break;
            }

            Guest next = controller.peekNextGuest();
            String requestedType =
                    controller.getRequestedRoomType(next.getConfirmationNo());

            // Try auto-match.
            AllocationResult result = controller.allocateNextRoom();

            if (result.isSuccess()) {
                printSuccess(result.getMessage());
                succeeded++;
            } else {
                // Auto-match failed — prompt manual selection for this guest.
                print("");
                printError("No '" + requestedType + "' room for "
                        + next.getName() + " — manual selection required.");
                print("");
                printRoomList(available);

                int selection = readInt("  Select room [1-" + available.size()
                        + "] for " + next.getName() + ": ");

                if (selection < 1 || selection > available.size()) {
                    printError("Invalid selection. Skipping " + next.getName() + ".");
                    failed++;
                    // Remove from queue so we move to the next guest.
                    controller.removeGuestFromQueue(next.getConfirmationNo());
                    continue;
                }

                Room chosen = available.get(selection - 1);
                AllocationResult manual =
                        controller.allocateRoom(next, chosen.getRoomNumber());

                if (manual.isSuccess()) {
                    printSuccess(manual.getMessage());
                    succeeded++;
                } else {
                    printError(manual.getMessage());
                    failed++;
                }
            }
        }

        printDivider(72);
        print("  Allocated: " + succeeded + "   |   Not allocated: " + failed);
        printQueueStatus();

        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Removes a guest from the queue using their confirmation number
     * (e.g. cancellation scenario).
     */
    private void handleRemoveGuest() {

        UIUtils.clearScreen();
        printHeader("Remove Guest from Queue");

        // Show the current queue so the user knows who to remove.
        if (controller.isQueueEmpty()) {
            print("  The VIP queue is empty. No guests to remove.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print(String.format("  %-4s %-12s %-24s %s",
              "#", "Conf #", "Name", "Tier"));
        printDivider(60);

        Guest[] guests = controller.getAllWaitingGuests();
        int rank = 1;

        for (Guest g : guests) {
            if (g != null) {
                print(String.format("  %-4d %-12s %-24s %s",
                      rank++,
                      g.getConfirmationNo(),
                      g.getName(),
                      g.getLoyaltyTier()));
            }
        }

        printDivider(60);
        print("");

        // Now ask which confirmation number to remove.
        String confirmationNo = readNonEmpty("Enter Confirmation No to remove: ").trim();

        Guest found = controller.findGuestInQueue(confirmationNo);

        if (found == null) {
            printError("No guest with confirmation number "
                       + confirmationNo + " found in the queue.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        print("  Found: " + found.getName() + " [" + found.getLoyaltyTier() + "]");

        String confirm = readNonEmpty("Remove this guest from the queue? (yes/no): ");
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            print("  Removal cancelled.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        boolean removed = controller.removeGuestFromQueue(confirmationNo);

        if (removed) {
            printSuccess("Guest " + found.getName() + " removed from the VIP queue.");
        } else {
            printError("Removal failed unexpectedly. Please try again.");
        }

        printQueueStatus();
        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Displays the complete allocation log.
     */
    private void handleViewLog() {

        UIUtils.clearScreen();
        printHeader("Allocation Log");

        List<String> log = controller.getAllocationLog();

        if (log.isEmpty()) {
            print("  No allocations have been made yet.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        int i = 1;
        for (String entry : log) {
            print("  " + i++ + ". " + entry);
        }

        printDivider(72);
        print("  Total allocations: " + log.size());

        UIUtils.pressEnterToContinue(scanner);
    }

    /**
     * Loads a set of sample guests and rooms so the user can
     * explore the module without entering data manually.
     */
    private void handleLoadSampleData() {

        UIUtils.clearScreen();
        printHeader("Load Sample Data");

        print("  This will add sample VIP guests and rooms.");
        String confirm = readNonEmpty("Proceed? (yes/no): ");

        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            print("  Sample data load cancelled.");
            UIUtils.pressEnterToContinue(scanner);
            return;
        }

        // Sample guests — mix of tiers
        controller.addGuest(new Guest("12345678", "Alice Tan",    "0123456789", "Diamond"));
        controller.addGuest(new Guest("23456789", "Bob Lim",      "0112233445", "Platinum"));
        controller.addGuest(new Guest("34567890", "Carol Ng",     "0198765432", "Elite"));
        controller.addGuest(new Guest("45678901", "David Wong",   "0134567890", "Gold"));
        controller.addGuest(new Guest("56789012", "Eve Chong",    "0156789012", "Diamond"));
        controller.addGuest(new Guest("67890123", "Frank Yap",    "0167890123", "Silver"));
        controller.addGuest(new Guest("78901234", "Grace Ooi",    "0178901234", "Elite"));

        // Sample rooms — all vacant and ready
        controller.addAvailableRoom(
            new Room("101", "Deluxe",      "Ready", "Vacant", "08:00", "06:00", "120"));
        controller.addAvailableRoom(
            new Room("205", "Suite",       "Ready", "Vacant", "08:30", "06:30", "110"));
        controller.addAvailableRoom(
            new Room("310", "Standard",    "Ready", "Vacant", "09:00", "07:00", "95"));
        controller.addAvailableRoom(
            new Room("412", "Junior Suite","Ready", "Vacant", "09:15", "07:15", "105"));

        printSuccess("Sample data loaded.");
        print("  Guests in queue : " + controller.getQueueSize());
        print("  Rooms available : " + controller.getAvailableRoomCount());
        print("  Tip: Try option 2 to see the priority order, then option 5 to allocate all.");

        UIUtils.pressEnterToContinue(scanner);
    }

    // -------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------

    /**
     * Prompts for a room type and validates against Standard, Deluxe, Suite.
     */
    private String readRoomType(String prompt) {

        print("  Valid types: Standard, Deluxe, Suite");

        while (true) {
            String input = readNonEmpty(prompt).trim();
            if (input.equalsIgnoreCase("Standard")
                    || input.equalsIgnoreCase("Deluxe")
                    || input.equalsIgnoreCase("Suite")) {
                // Normalise to title case.
                return input.substring(0, 1).toUpperCase()
                     + input.substring(1).toLowerCase();
            }
            printError("Invalid type. Choose from: Standard, Deluxe, Suite");
        }
    }

    /**
     * Prints a numbered list of available rooms for manual selection.
     */
    private void printRoomList(List<Room> rooms) {
        print(String.format("  %-4s %-8s %-14s %s",
              "#", "Room", "Type", "Status"));
        printDivider(50);
        int i = 1;
        for (Room r : rooms) {
            print(String.format("  %-4d %-8s %-14s %s",
                  i++,
                  r.getRoomNumber(),
                  r.getRoomType(),
                  r.getCleanlinessStatus()));
        }
        printDivider(50);
        print("");
    }

    /**
     * Prompts for a loyalty tier and validates against known values.
     * Shows the valid tiers if the input is unrecognised.
     */
    private String readTier(String prompt) {

        print("  Valid tiers: Diamond, Elite, Platinum, Gold, Silver, None");

        while (true) {
            String input = readNonEmpty(prompt).trim().toUpperCase();
            for (String t : VALID_TIERS) {
                if (t.equals(input)) {
                    return t.substring(0, 1).toUpperCase()
                         + t.substring(1).toLowerCase();    // normalise case
                }
            }
            printError("Unknown tier. Choose from: Diamond, Elite, Platinum, Gold, Silver, None");
        }
    }

    /**
     * Reads a non-empty string from the console.
     * Loops until the user enters something.
     */
    private String readNonEmpty(String prompt) {

        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            printError("Input cannot be empty.");
        }
    }

    /**
     * Reads an integer from the console.
     * Returns -1 if the input is not a valid integer.
     */
    private int readInt(String prompt) {

        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // -------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------

    private void printBanner() {
        print("");
        print("╔══════════════════════════════════════════════════════════╗");
        print("║        VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION       ║");
        print("║                       Module 2                           ║");
        print("╚══════════════════════════════════════════════════════════╝");
        print("");
    }

    private void printMenu() {
        print("");
        print("┌─────────────────────────────────────────┐");
        print("│             MAIN MENU                   │");
        print("├─────────────────────────────────────────┤");
        print("│  1. Add VIP Guest to Queue              │");
        print("│  2. View Queue (priority order)         │");
        print("│  3. View Available Rooms                │");
        print("│  4. Allocate Room — Next Guest          │");
        print("│  5. Allocate Rooms — All Guests         │");
        print("│  6. Remove Guest from Queue             │");
        print("│  7. View Allocation Log                 │");
        print("│  8. Demo — Load Sample Data             │");
        print("│  0. Exit                                │");
        print("└─────────────────────────────────────────┘");
    }

    private void printHeader(String title) {
        print("");
        print("══ " + title + " " + "═".repeat(Math.max(0, 56 - title.length())));
        print("");
    }

    private void printDivider(int width) {
        print("  " + "─".repeat(width));
    }

    private void printGuestCard(Guest g) {
        print("  ┌─ Guest Details ──────────────────────┐");
        print("  │  Name    : " + padRight(g.getName(),     26) + "│");
        print("  │  Conf #  : " + padRight(g.getConfirmationNo(), 26) + "│");
        print("  │  Tier    : " + padRight(g.getLoyaltyTier(),    26) + "│");
        print("  │  Phone   : " + padRight(g.getPhone(),    26) + "│");
        print("  └─────────────────────────────────────┘");
    }

    private void printAllocationSummary(AllocationResult r) {
        Guest g = r.getGuest();
        Room  rm = r.getRoom();
        print("  ┌─ Allocation Summary ──────────────────┐");
        print("  │  Guest   : " + padRight(g.getName(),        26) + "│");
        print("  │  Tier    : " + padRight(g.getLoyaltyTier(), 26) + "│");
        print("  │  Conf #  : " + padRight(g.getConfirmationNo(), 26) + "│");
        print("  │  Room    : " + padRight(rm.getRoomNumber(),  26) + "│");
        print("  │  Type    : " + padRight(rm.getRoomType(),    26) + "│");
        print("  └──────────────────────────────────────┘");
    }

    private void printQueueStatus() {
        print("");
        print("  Queue: " + controller.getQueueSize() + " guest(s) waiting"
            + "   |   Rooms available: " + controller.getAvailableRoomCount());
    }

    private void printSuccess(String msg) {
        print("  [OK]  " + msg);
    }

    private void printError(String msg) {
        print("  [!]   " + msg);
    }

    private void print(String msg) {
        System.out.println(msg);
    }

    private String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }
}