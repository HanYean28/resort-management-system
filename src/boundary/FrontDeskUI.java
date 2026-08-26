package boundary;

import adt.ListInterface;
import control.FrontDeskService;
import entity.BillingRecord;
import entity.Guest;
import entity.Room;
import java.util.Scanner;
import utility.UIUtils;

/**
 * @author Lim How Voon
 */
public class FrontDeskUI {
    private FrontDeskService service;
    private Scanner scanner;

    public FrontDeskUI(Scanner scanner) {
        service = new FrontDeskService();
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
            System.out.println(" [5] Generate Report 2: Guest Billing History");
            System.out.println(" [6] View Rooms Available Today");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-6): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                UIUtils.printError("Invalid input! Please enter a number.");
                scanner.nextLine();
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }

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

        System.out.print("Enter Confirmation No: ");
        String confirmationNo = scanner.nextLine().trim();
        warnIfNotEightDigits(confirmationNo);

        Guest result = service.searchByConfirmationNumber(confirmationNo);
        if (result == null) {
            UIUtils.printError("No guest record found for confirmation number " + confirmationNo + ".");
        } else {
            System.out.println("\n[GUEST FOUND]");
            printGuestDetail(result);
        }
    }

    private void handleRemoveGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REMOVE GUEST RECORD");

        System.out.print("Enter Confirmation No to remove: ");
        String confirmationNo = scanner.nextLine().trim();
        warnIfNotEightDigits(confirmationNo);

        Guest removed = service.removeGuest(confirmationNo);
        if (removed == null) {
            UIUtils.printError("No guest record found for confirmation number " + confirmationNo + ".");
        } else {
            System.out.println("\nRemoved record for: " + removed.getName());
        }
    }

    /**
     * Soft (non-blocking) format check: confirmation numbers are 8 digits
     * per spec. Prints a heads-up if the input doesn't match, but still lets
     * the search/remove proceed normally either way.
     */
    private void warnIfNotEightDigits(String confirmationNo) {
        if (!confirmationNo.matches("\\d{8}")) {
            System.out.println("[Notice] Confirmation numbers are usually 8 digits — "
                    + "double-check the number if you don't get a match.");
        }
    }

    private void handleGuestDirectoryReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("GUEST DIRECTORY REPORT");

        System.out.print("Show (1) Loyalty Members only, or (2) Non-Members only? [1/2]: ");
        int filterChoice = readIntOption(1, 2);
        boolean membersOnly = (filterChoice == 1);

        System.out.print("Filter by Room Type (Standard/Deluxe/Suite), or press Enter for ALL: ");
        String roomTypeFilter = scanner.nextLine().trim();
        if (roomTypeFilter.isEmpty()) roomTypeFilter = "ALL";

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
        UIUtils.printHeader("ROOMS AVAILABLE TODAY");

        ListInterface<Room> rooms = service.getAvailableRooms();
        if (rooms.isEmpty()) {
            System.out.println("No rooms are available today.");
            return;
        }

        System.out.printf("%-14s | %-15s | %-22s | %-10s%n",
                "Room Number", "Room Type", "Clean Status", "Occupancy");
        UIUtils.printSectionLine();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            System.out.printf("%-14s | %-15s | %-22s | %-10s%n",
                    r.getRoomNumber(), r.getRoomType(), r.getCleanlinessStatus(), r.getOccupancyStatus());
        }
        UIUtils.printSectionLine();
        System.out.println("Total Available: " + rooms.getNumberOfEntries());
    }

    private void handleBillingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("GUEST BILLING REPORT");

        System.out.print("Minimum billing amount (RM): ");
        double minAmount = readDouble();

        System.out.print("Filter by Room Type (Standard/Deluxe/Suite), or press Enter for ALL: ");
        String roomTypeFilter = scanner.nextLine().trim();
        if (roomTypeFilter.isEmpty()) roomTypeFilter = "ALL";

        ListInterface<BillingRecord> report = service.generateGuestBillingReport(minAmount, roomTypeFilter);
        System.out.println();
        String title = "BILLING >= RM " + minAmount
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

        System.out.printf("%-14s | %-18s | %-8s | %-10s | %-10s%n",
                "Confirmation No", "Name", "Tier", "Room No", "Room Type");
        UIUtils.printSectionLine();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            Guest g = guests.getEntry(i);
            String roomNo = service.getGuestCurrentRoom(g.getConfirmationNo());
            System.out.printf("%-14s | %-18s | %-8s | %-10s | %-10s%n",
                    g.getConfirmationNo(), g.getName(), g.getLoyaltyTier(), roomNo,
                    service.getRoomType(roomNo));
        }
        UIUtils.printSectionLine();
        System.out.println("Total Records: " + guests.getNumberOfEntries());
    }

    private void printGuestDetail(Guest g) {
        System.out.println("Confirmation No : " + g.getConfirmationNo());
        System.out.println("Guest Name      : " + g.getName());
        System.out.println("Phone Number    : " + g.getPhone());
        System.out.println("Loyalty Tier    : " + g.getLoyaltyTier());
        System.out.println("Room Number     : " + service.getGuestCurrentRoom(g.getConfirmationNo()));
        System.out.println("Room Type       : " + service.getGuestCurrentRoomType(g.getConfirmationNo()));
        UIUtils.printSectionLine();
    }

    private void printBillingTable(ListInterface<BillingRecord> bills) {
        if (bills.isEmpty()) {
            System.out.println("No matching billing records found.");
            return;
        }

        System.out.printf("%-7s | %-7s | %-14s | %-16s | %-6s | %-10s | %-6s | %10s | %-6s%n",
                "Bill", "Booking", "Confirmation", "Guest", "Room", "Room Type", "Nights", "Amount", "Status");
        UIUtils.printSectionLine();
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
        }
        UIUtils.printSectionLine();
        System.out.println("Total Billing Records: " + bills.getNumberOfEntries());
    }

    private Integer readIntOption(int min, int max) {
        if (scanner.hasNextInt()) {
            int value = scanner.nextInt();
            scanner.nextLine();
            if (value >= min && value <= max) {
                return value;
            }
        } else {
            scanner.nextLine();
        }
        return min; // fall back to first option on bad input
    }

    private double readDouble() {
        if (scanner.hasNextDouble()) {
            double value = scanner.nextDouble();
            scanner.nextLine();
            return value;
        }
        scanner.nextLine();
        return 0.0;
    }
}
