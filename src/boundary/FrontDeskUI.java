package boundary;

import adt.ListInterface;
import control.FrontDeskService;
import entity.Guest;

import java.util.Scanner;

/**
 * @author Lim How Voon
 */
public class FrontDeskUI {
    private FrontDeskService service;
    private Scanner scanner;

    public FrontDeskUI() {
        service = new FrontDeskService();
        scanner = new Scanner(System.in);
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("FRONT-DESK SERVICE MENU");
            System.out.println(" [1] View All Guests (sorted by Confirmation No.)");
            System.out.println(" [2] Register New Guest Record");
            System.out.println(" [3] Search Guest by Confirmation Number");
            System.out.println(" [4] Remove Guest Record");
            System.out.println(" [5] Generate Report 1: Guest Directory Report");
            System.out.println(" [6] Generate Report 2: Outstanding Billing Report");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-6): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                System.out.println("Invalid input! Please enter a number.");
                scanner.nextLine();
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }

            switch (choice) {
                case 1:
                    displayAllGuests();
                    break;
                case 2:
                    handleRegisterGuest();
                    break;
                case 3:
                    handleSearchGuest();
                    break;
                case 4:
                    handleRemoveGuest();
                    break;
                case 5:
                    handleGuestDirectoryReport();
                    break;
                case 6:
                    handleBillingReport();
                    break;
                case 0:
                    UIUtils.clearScreen();
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
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

    private void handleRegisterGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REGISTER NEW GUEST RECORD");

        System.out.print("Confirmation No (8-digit): ");
        String confirmationNo = scanner.nextLine().trim();

        if (service.confirmationNumberExists(confirmationNo)) {
            System.out.println("\nA record with this confirmation number already exists.");
            return;
        }

        System.out.print("Guest Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Phone Number: ");
        String phone = scanner.nextLine().trim();
        System.out.print("Loyalty Tier (Platinum/Diamond/Elite/NONE): ");
        String tier = scanner.nextLine().trim();
        if (tier.isEmpty()) tier = "NONE";
        System.out.print("Billing Amount (RM): ");
        double billing = readDouble();
        System.out.print("Room Number: ");
        String roomNo = scanner.nextLine().trim();

        service.addGuest(new Guest(confirmationNo, name, phone, tier, billing, roomNo));
        System.out.println("\nGuest record registered successfully.");
    }

    private void handleSearchGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("SEARCH GUEST BY CONFIRMATION NUMBER");

        System.out.print("Enter Confirmation No: ");
        String confirmationNo = scanner.nextLine().trim();

        Guest result = service.searchByConfirmationNumber(confirmationNo);
        if (result == null) {
            System.out.println("\nNo guest record found for confirmation number " + confirmationNo + ".");
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

        Guest removed = service.removeGuest(confirmationNo);
        if (removed == null) {
            System.out.println("\nNo guest record found for confirmation number " + confirmationNo + ".");
        } else {
            System.out.println("\nRemoved record for: " + removed.getName());
        }
    }

    private void handleGuestDirectoryReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("GUEST DIRECTORY REPORT");

        System.out.print("Show (1) Loyalty Members only, or (2) Non-Members only? [1/2]: ");
        int filterChoice = readIntOption(1, 2);
        boolean membersOnly = (filterChoice == 1);

        ListInterface<Guest> report = service.generateGuestDirectoryReport(membersOnly);
        System.out.println();
        UIUtils.printHeader(membersOnly ? "LOYALTY MEMBERS (sorted by Name)" : "NON-MEMBERS (sorted by Name)");
        printGuestTable(report);
    }

    private void handleBillingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("OUTSTANDING BILLING REPORT");

        System.out.print("Minimum outstanding balance (RM): ");
        double minBalance = readDouble();

        ListInterface<Guest> report = service.generateOutstandingBillingReport(minBalance);
        System.out.println();
        UIUtils.printHeader("BILLING >= RM " + minBalance + " (highest first)");
        printGuestTable(report);
    }

    private void printGuestTable(ListInterface<Guest> guests) {
        if (guests.isEmpty()) {
            System.out.println("No matching guest records found.");
            return;
        }

        System.out.printf("%-14s | %-18s | %-8s | %-10s | %10s%n",
                "Confirmation No", "Name", "Tier", "Room No", "Billing (RM)");
        UIUtils.printSectionLine();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            Guest g = guests.getEntry(i);
            System.out.printf("%-14s | %-18s | %-8s | %-10s | %10.2f%n",
                    g.getConfirmationNo(), g.getName(), g.getLoyaltyTier(), g.getRoomNo(), g.getBillingAmount());
        }
        UIUtils.printSectionLine();
        System.out.println("Total Records: " + guests.getNumberOfEntries());
    }

    private void printGuestDetail(Guest g) {
        System.out.println("Confirmation No : " + g.getConfirmationNo());
        System.out.println("Guest Name      : " + g.getName());
        System.out.println("Phone Number    : " + g.getPhone());
        System.out.println("Loyalty Tier    : " + g.getLoyaltyTier());
        System.out.println("Room Number     : " + g.getRoomNo());
        System.out.printf("Billing Amount  : RM %.2f%n", g.getBillingAmount());
        UIUtils.printSectionLine();
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