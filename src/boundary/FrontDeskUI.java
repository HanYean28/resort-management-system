package boundary;

import adt.ListInterface;
import control.FrontDeskServiceController;
import entity.BillingRecord;
import entity.Guest;
import entity.Room;
import java.util.Scanner;
import utility.UIUtils;

/**
 * @author Lim How Voon
 */
public class FrontDeskUI {
    private FrontDeskServiceController service;
    private Scanner scanner;

    public FrontDeskUI(Scanner scanner) {
        service = new FrontDeskServiceController();
        this.scanner = scanner;
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("FRONT-DESK SERVICE MENU");
            System.out.println(" [1] View All Guests (sorted by Confirmation No.)");
            System.out.println(" [2] Search Guest by Confirmation Number");
            System.out.println(" [3] Remove Guest Record");
            System.out.println(" [4] Generate Report 1: Guest Directory Report");
            System.out.println(" [5] Generate Report 2: Guest Billing History Report");
            System.out.println(" [6] Room Availability");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();

            choice = readIntOption("Please enter choice (0-6): ", 0, 6);

            switch (choice) {
                case 1:
                    displayAllGuests();
                    break;
                case 2:
                    handleSearchGuest();
                    break;
                case 3:
                    handleRemoveGuest();
                    break;
                case 4:
                    handleGuestDirectoryReport();
                    break;
                case 5:
                    handleBillingReport();
                    break;
                case 6:
                    displayAvailableRooms();
                    break;
                case 0:
                    UIUtils.clearScreen();
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    UIUtils.printError("Invalid choice. Try again.");
            }

            if (choice != 0) {
                UIUtils.pressEnterToContinue(scanner);
            }
        }
    }

    private void displayAllGuests() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ALL GUESTS OVERVIEW");

        ListInterface<Guest> guests = service.getAllGuestsSorted();
        printGuestTable(guests);
    }

    private void handleSearchGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("SEARCH GUEST BY CONFIRMATION NUMBER");

        String confirmationNo = readConfirmationNo("Enter 8-digit Confirmation No: ");

        Guest result = service.searchByConfirmationNumber(confirmationNo);
        if (result == null) {
            UIUtils.printError("No guest record found for confirmation number " + confirmationNo + ".");
        } else {
            System.out.println("\n[GUEST FOUND]");
            printGuestDetail(result);
            printGuestBillingHistory(result.getConfirmationNo());
        }
    }

    private void handleRemoveGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REMOVE GUEST RECORD");

        String confirmationNo = readConfirmationNo("Enter Confirmation No to remove: ");

        System.out.print("Are you sure you want to remove guest " + confirmationNo + "? [Y/N]: ");
        if (!readYesNo()) {
            System.out.println("Cancelled. No record was removed.");
            return;
        }

        Guest removed = service.removeGuest(confirmationNo);
        if (removed == null) {
            UIUtils.printError("No guest record found for confirmation number " + confirmationNo + ".");
        } else {
            System.out.println("\nRemoved record for: " + removed.getName());
        }
    }

    /**
     * Reads a confirmation number from the user, re-prompting until it is
     * exactly 8 digits. Confirmation numbers are free-form IDs (not a fixed
     * set of options), so typing is kept, but the format is now enforced
     * instead of just warned about.
     */
    private String readConfirmationNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.matches("\\d{8}")) {
                return input;
            }
            UIUtils.printError("Invalid format. Confirmation No must be exactly 8 digits (e.g. 80000001).");
        }
    }

    /** Reads a Y/N confirmation, re-prompting on anything else. */
    private boolean readYesNo() {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("Y")) {
                return true;
            }
            if (input.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.print("Please enter Y or N: ");
        }
    }

    private void handleGuestDirectoryReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("GUEST DIRECTORY REPORT");

        System.out.println("Show:");
        System.out.println(" [1] Loyalty Members only");
        System.out.println(" [2] Non-Members only");
        int filterChoice = readIntOption("Choice: ", 1, 2);
        boolean membersOnly = (filterChoice == 1);

        String roomTypeFilter = readRoomTypeFilterMenu();

        ListInterface<Guest> report = service.generateGuestDirectoryReport(membersOnly, roomTypeFilter);
        System.out.println();
        String title = (membersOnly ? "LOYALTY MEMBERS" : "NON-MEMBERS")
                + (roomTypeFilter.equalsIgnoreCase("ALL") ? "" : " - Room Type: " + roomTypeFilter)
                + " (sorted by Name)";
        UIUtils.printHeader(title);
        printGuestTable(report);
    }

    private void displayAvailableRooms() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ROOM AVAILABILITY");

        String roomTypeFilter = readRoomTypeFilterMenu();

        ListInterface<Room> rooms = service.generateRoomAvailabilityReport(roomTypeFilter);
        System.out.println();
        UIUtils.printHeader("ROOM AVAILABILITY");

        if (rooms.isEmpty()) {
            System.out.println("No rooms found for this filter.");
            return;
        }

        System.out.printf("%-8s %-12s %-14s %-14s %-20s%n",
                "Room No", "Type", "Price/Night", "Availability", "Status");

        int availableCount = 0;
        int unavailableCount = 0;
        int occupiedCount = 0;

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            String availability = service.getRoomAvailabilityLabel(r);
            String status = service.getRoomStatusLabel(r);
            double price = service.getRoomTypePrice(r.getRoomType());

            System.out.printf("%-8s %-12s RM%-12.2f %-14s %-20s%n",
                    r.getRoomNumber(), r.getRoomType(), price, availability, status);

            if (availability.equals("Available")) {
                availableCount++;
            } else if (availability.equals("Occupied")) {
                occupiedCount++;
            } else {
                unavailableCount++;
            }
        }

        UIUtils.printSectionLine();
        System.out.println("Rooms shown: " + rooms.getNumberOfEntries()
                + " | Available: " + availableCount
                + " | Unavailable: " + unavailableCount
                + " | Occupied: " + occupiedCount);
    }

    private void handleBillingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("GUEST BILLING HISTORY REPORT");

        double minAmount = readNonNegativeDouble("Minimum billing amount (RM): ");

        String roomTypeFilter = readRoomTypeFilterMenu();

        ListInterface<BillingRecord> report = service.generateGuestBillingReport(minAmount, roomTypeFilter);
        System.out.println();
        String title = String.format("BILLING >= RM %.2f", minAmount)
                + (roomTypeFilter.equalsIgnoreCase("ALL") ? "" : " - Room Type: " + roomTypeFilter)
                + " (highest first)";
        UIUtils.printHeader(title);
        printBillingTable(report);
    }

    private void printGuestTable(ListInterface<Guest> guests) {
        if (guests.isEmpty()) {
            System.out.println("No matching guest records found.");
            return;
        }

        System.out.printf("%-14s | %-18s | %-8s | %-16s | %-10s | %-12s%n",
                "Confirmation No", "Name", "Tier", "Room No", "Room Type", "Status");
        UIUtils.printSectionLine();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            Guest g = guests.getEntry(i);
            String rawRoomNo = service.getGuestCurrentRoom(g.getConfirmationNo());
            String roomLabel = service.getGuestRoomStatusLabel(g.getConfirmationNo());
            String bookingStatus = service.getGuestBookingStatus(g.getConfirmationNo());
            System.out.printf("%-14s | %-18s | %-8s | %-16s | %-10s | %-12s%n",
                    g.getConfirmationNo(), g.getName(), g.getLoyaltyTier(), roomLabel,
                    service.getRoomType(rawRoomNo), bookingStatus);
        }
        UIUtils.printSectionLine();
        System.out.println("Total Records: " + guests.getNumberOfEntries());
    }

    private void printGuestDetail(Guest g) {
        System.out.println("Confirmation No : " + g.getConfirmationNo());
        System.out.println("Guest Name      : " + g.getName());
        System.out.println("Phone Number    : " + g.getPhone());
        System.out.println("Loyalty Tier    : " + g.getLoyaltyTier());
        System.out.println("Room Number     : " + service.getGuestRoomStatusLabel(g.getConfirmationNo()));
        System.out.println("Room Type       : " + service.getGuestCurrentRoomType(g.getConfirmationNo()));
        System.out.println("Booking Status  : " + service.getGuestBookingStatus(g.getConfirmationNo()));
        UIUtils.printSectionLine();
    }

    /**
     * Shows the guest's full billing history (all stays, most recent
     * first), flags any unpaid bill inline, and prints a total outstanding
     * balance warning if applicable. Used right after Search Guest so
     * front desk sees payment issues immediately, not just current stay.
     */
    private void printGuestBillingHistory(String confirmationNo) {
        ListInterface<BillingRecord> bills = service.getBillingHistoryByConfirmationNo(confirmationNo);

        System.out.println();
        UIUtils.printHeader("BILLING HISTORY");

        if (bills.isEmpty()) {
            System.out.println("No billing records found for this guest.");
            return;
        }

        System.out.printf("%-7s | %-7s | %-6s | %-10s | %-10s | %6s | %10s | %-10s%n",
                "Bill", "Booking", "Room", "Check-In", "Check-Out", "Nights", "Amount", "Status");
        UIUtils.printSectionLine();

        double outstanding = 0.0;
        double totalPaid = 0.0;
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            BillingRecord bill = bills.getEntry(i);
            boolean unpaid = !bill.getPaymentStatus().equalsIgnoreCase("Paid");
            String flag = unpaid ? "  <-- UNPAID" : "";

            System.out.printf("%-7s | %-7s | %-6s | %-10s | %-10s | %6d | %10.2f | %-10s%s%n",
                    bill.getBillId(), bill.getBookingId(), bill.getRoomNumber(),
                    bill.getCheckInDate(), bill.getCheckOutDate(), bill.getNights(),
                    bill.getAmount(), bill.getPaymentStatus(), flag);

            if (unpaid) {
                outstanding += bill.getAmount();
            } else {
                totalPaid += bill.getAmount();
            }
        }

        UIUtils.printSectionLine();
        System.out.println("Total Billing Records: " + bills.getNumberOfEntries());
        System.out.printf("Total Paid to Date : RM %,.2f%n", totalPaid);

        if (outstanding > 0) {
            UIUtils.printError(String.format(
                    "Outstanding balance: RM %.2f — please settle before check-out.", outstanding));
        } else {
            System.out.println("No outstanding balance.");
        }
    }

    private void printBillingTable(ListInterface<BillingRecord> bills) {
        if (bills.isEmpty()) {
            System.out.println("No matching billing records found.");
            return;
        }

        System.out.printf("%-7s | %-7s | %-14s | %-16s | %-6s | %-10s | %-6s | %10s | %-6s%n",
                "Bill", "Booking", "Confirmation", "Guest", "Room", "Room Type", "Nights", "Amount", "Status");
        UIUtils.printSectionLine();

        double totalRevenue = 0.0;
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            BillingRecord bill = bills.getEntry(i);
            System.out.printf("%-7s | %-7s | %-14s | %-16s | %-6s | %-10s | %-6d | %10.2f | %-6s%n",
                    bill.getBillId(),
                    bill.getBookingId(),
                    bill.getConfirmationNo(),
                    service.getGuestName(bill.getConfirmationNo()),
                    bill.getRoomNumber(),
                    bill.getRoomType(),
                    bill.getNights(),
                    bill.getAmount(),
                    bill.getPaymentStatus());

            totalRevenue += bill.getAmount();
        }

        UIUtils.printSectionLine();
        System.out.println("Total Bookings : " + bills.getNumberOfEntries());
        System.out.printf("Total Revenue  : RM %,.2f%n", totalRevenue);
    }

    /**
     * Shows the fixed set of room type filter options as a numbered menu
     * and returns "ALL", "Standard", "Deluxe" or "Suite". Since this is a
     * closed set of options (not free-form text), the user picks a number
     * instead of typing the word — no more risk of typos like "Delux"
     * silently falling through to "ALL".
     */
    private String readRoomTypeFilterMenu() {
        System.out.println("Filter by Room Type:");
        System.out.println(" [1] All");
        System.out.println(" [2] Standard");
        System.out.println(" [3] Deluxe");
        System.out.println(" [4] Suite");
        int choice = readIntOption("Choice: ", 1, 4);

        switch (choice) {
            case 2: return "Standard";
            case 3: return "Deluxe";
            case 4: return "Suite";
            default: return "ALL";
        }
    }

    /**
     * Reads an integer choice within [min, max], re-prompting on invalid
     * input (non-numeric or out of range) instead of silently defaulting
     * to the first option.
     */
    private int readIntOption(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                UIUtils.printError("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                UIUtils.printError("Invalid input! Please enter a number.");
            }
        }
    }

    /**
     * Reads a non-negative decimal amount, re-prompting on invalid input
     * (non-numeric or negative) instead of silently defaulting to 0.0.
     */
    private double readNonNegativeDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value >= 0) {
                    return value;
                }
                UIUtils.printError("Amount cannot be negative. Please try again.");
            } catch (NumberFormatException e) {
                UIUtils.printError("Invalid input! Please enter a valid amount (e.g. 150 or 150.50).");
            }
        }
    }
}