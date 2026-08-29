package boundary;

import control.VIPRoomAllocationController;
import entity.Guest;
import utility.DateUtils;
import utility.UIUtils;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * Boundary for VIP & Loyalty Tier Priority Room Allocation.
 *
 * This class only handles user input/output. VIP business rules, file access,
 * filtering and sorting are delegated to VIPRoomAllocationController.
 *
 * @author Kaizen Soh
 */
public class VIPRoomAllocationUI {

    private final VIPRoomAllocationController ctrl;
    private final Scanner scanner;

    public VIPRoomAllocationUI(Scanner scanner) {
        this.ctrl = new VIPRoomAllocationController();
        this.scanner = scanner;
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION");
            System.out.println(" [1] Add VIP Guest");
            System.out.println(" [2] Remove VIP Guest");
            System.out.println(" [3] Create Booking");
            System.out.println(" [4] Cancel Booking");
            System.out.println(" [5] View Priority Queue");
            System.out.println(" [6] Generate Report");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-6): ");

            Integer selected = readIntOption(0, 6);
            if (selected == null) {
                UIUtils.printError("Invalid input! Please enter a number between 0 and 6.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            choice = selected;

            switch (choice) {
                case 1: handleAddGuest(); break;
                case 2: handleRemoveGuest(); break;
                case 3: handleCreateBooking(); break;
                case 4: handleCancelBooking(); break;
                case 5: handleViewQueue(); break;
                case 6: handleReport(); break;
                case 0:
                    UIUtils.clearScreen();
                    System.out.println("Returning to Main Menu...");
                    break;
                default: break;
            }

            if (choice != 0) UIUtils.pressEnterToContinue(scanner);
        }
    }

    // ------------------------------------------------------------------
    // 1. Add VIP Guest
    // ------------------------------------------------------------------

    private void handleAddGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD VIP GUEST");

        String name;
        while (true) {
            System.out.print("Enter Guest Name (0 to return): ");
            name = scanner.nextLine().trim();
            if (name.equals("0")) return;
            if (name.isEmpty() || !name.matches("[A-Za-z ]+")) {
                UIUtils.printError("Guest name must contain alphabetic characters only.");
                continue;
            }
            break;
        }

        String phone;
        while (true) {
            System.out.print("Enter Phone Number (0 to return): ");
            phone = scanner.nextLine().trim();
            if (phone.equals("0")) return;
            if (!phone.matches("\\d{10}")) {
                UIUtils.printError("Phone number must contain exactly 10 digits.");
                continue;
            }
            break;
        }

        System.out.println("\nSelect Loyalty Tier:");
        System.out.println(" [1] Diamond");
        System.out.println(" [2] Elite");
        System.out.println(" [3] Platinum");
        System.out.println(" [4] Gold");
        System.out.println(" [5] Silver");
        System.out.println(" [0] Return");
        UIUtils.printSectionLine();
        String tier = promptTier();
        if (tier == null) return;

        UIUtils.printSectionLine();
        System.out.println("Name         : " + name);
        System.out.println("Phone        : " + phone);
        System.out.println("Loyalty Tier : " + tier);
        UIUtils.printSectionLine();
        System.out.print("Confirm add guest? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("Guest registration cancelled.");
            return;
        }

        try {
            String confirmationNo = ctrl.generateConfirmationNo();
            Guest guest = new Guest(confirmationNo, name, phone, tier);
            ctrl.addGuest(guest);
            System.out.println("\nGuest added successfully.");
            System.out.println("Generated Confirmation No : " + confirmationNo);
            System.out.println("Name                      : " + name);
            System.out.println("Phone                     : " + phone);
            System.out.println("Loyalty Tier              : " + tier);
            UIUtils.printSectionLine();
            System.out.println("VIP Queue Size : " + ctrl.getQueueSize());
        } catch (IllegalArgumentException | IllegalStateException e) {
            UIUtils.printError(e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2. Remove VIP Guest
    // ------------------------------------------------------------------

    private void handleRemoveGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REMOVE VIP GUEST");
        Guest[] guests = ctrl.getAllWaitingGuests();
        if (guests.length == 0) {
            System.out.println("No VIP guests available to remove.");
            return;
        }
        printVipGuestDetailsTable(guests);

        System.out.print("Enter Guest Confirmation No to remove (0 to return): ");
        String confirmationNo = scanner.nextLine().trim();
        if (confirmationNo.equals("0")) return;

        Guest guest = ctrl.findGuestInQueue(confirmationNo);
        if (guest == null) {
            UIUtils.printError("VIP guest not found.");
            return;
        }

        UIUtils.printSectionLine();
        System.out.println("Confirmation No : " + guest.getConfirmationNo());
        System.out.println("Name            : " + guest.getName());
        System.out.println("Phone           : " + guest.getPhone());
        System.out.println("Loyalty Tier    : " + guest.getLoyaltyTier());
        UIUtils.printSectionLine();
        System.out.print("Confirm removal? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) return;

        if (ctrl.removeGuestFromQueue(confirmationNo)) {
            System.out.println("\nVIP guest removed successfully.");
        } else {
            UIUtils.printError("Guest cannot be removed. Check whether an active booking exists.");
        }
    }

    // ------------------------------------------------------------------
    // 3. Create VIP Booking
    // ------------------------------------------------------------------

    private void handleCreateBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CREATE VIP BOOKING");
        if (ctrl.isQueueEmpty()) {
            UIUtils.printError("No VIP guests in queue. Add a guest first.");
            return;
        }

        printGuestTable(ctrl.getAllWaitingGuests());
        System.out.print("Enter Guest Confirmation No (0 to return): ");
        String confirmationNo = scanner.nextLine().trim();
        if (confirmationNo.equals("0")) return;

        Guest guest = ctrl.findGuestInQueue(confirmationNo);
        if (guest == null) {
            UIUtils.printError("VIP guest not found.");
            return;
        }

        String roomType = promptRoomType();
        if (roomType == null) return;
        String checkIn = promptCheckInDate("Enter Check-In Date (YYYY-MM-DD, 0 to return): ");
        if (checkIn == null) return;
        String checkOut = promptCheckOutDate(DateUtils.parseDate(checkIn),
                "Enter Check-Out Date (YYYY-MM-DD, 0 to return): ");
        if (checkOut == null) return;

        double preview = ctrl.computeBookingPreview(guest.getLoyaltyTier(), checkIn, checkOut);
        UIUtils.printSectionLine();
        System.out.println("Guest      : " + guest.getName());
        System.out.println("Tier       : " + guest.getLoyaltyTier());
        System.out.println("Room Type  : " + roomType);
        System.out.println("Check-In   : " + checkIn);
        System.out.println("Check-Out  : " + checkOut);
        System.out.printf("Est. Total : RM %.2f%n", preview);
        UIUtils.printSectionLine();
        System.out.print("Confirm booking? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) return;

        String error = ctrl.createPendingVipBooking(guest, roomType, checkIn, checkOut);
        if (error == null) {
            System.out.println("\nBooking created successfully. Status: Pending.");
            System.out.println("Rooms Available : " + ctrl.getAvailableRoomCount());
        } else {
            UIUtils.printError(error);
        }
    }

    // ------------------------------------------------------------------
    // 4. Cancel VIP Booking
    // ------------------------------------------------------------------

    private void handleCancelBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CANCEL VIP BOOKING");
        String[][] cancellable = ctrl.getVipBookingsByStatus(VIPRoomAllocationController.STATUS_PENDING);
        if (cancellable.length == 0) {
            System.out.println("No pending VIP bookings available for cancellation.");
            return;
        }

        displayBookingTable(cancellable);
        System.out.print("Enter Booking ID to cancel (0 to return): ");
        String bookingId = scanner.nextLine().trim();
        if (bookingId.equals("0")) return;

        String[] target = findBookingRow(cancellable, bookingId);
        if (target == null) {
            UIUtils.printError("Booking ID not found or booking is no longer Pending.");
            return;
        }

        System.out.println("\nConf #    : " + target[1]);
        System.out.println("Room Type : " + target[3]);
        System.out.println("Check-In  : " + target[4] + "  |  Check-Out : " + target[5]);
        System.out.print("\nConfirm cancellation? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) return;

        if (ctrl.cancelPendingVipBooking(bookingId)) {
            System.out.println("\nBooking cancelled successfully.");
        } else {
            UIUtils.printError("Booking could not be cancelled.");
        }
    }

    // ------------------------------------------------------------------
    // 5. View Priority Queue
    // ------------------------------------------------------------------

    private void handleViewQueue() {
        UIUtils.clearScreen();
        UIUtils.printHeader("VIP BOOKING PRIORITY QUEUE");

        // Tier only; the controller uses stable insertion sort so same-tier
        // bookings keep their existing booking/file order.
        String[][] pending = ctrl.generateVipBookingReport(
                VIPRoomAllocationController.STATUS_PENDING, "ALL", "TIER_DESC");
        if (pending.length == 0) {
            System.out.println("No pending VIP bookings in the priority queue.");
            return;
        }

        System.out.println("Priority: Diamond > Elite > Platinum > Gold > Silver");
        System.out.println("Same-tier bookings keep their existing booking order.\n");
        System.out.printf("%-7s | %-12s | %-20s | %-10s | %-10s | %-10s | %-11s | %s%n",
                "Booking", "Conf#", "Guest Name", "Tier", "Room Type",
                "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();

        for (String[] row : pending) {
            String name = ctrl.getNameForConfirmation(row[1]);
            if (name.length() > 20) name = name.substring(0, 17) + "...";
            System.out.printf("%-7s | %-12s | %-20s | %-10s | %-10s | %-10s | %-11s | %s%n",
                    row[0], row[1], name, ctrl.getTierForConfirmation(row[1]),
                    row[3], row[4], row[5], row[6]);
        }
        UIUtils.printSectionLine();
        System.out.println("Total Pending VIP Bookings: " + pending.length);
        String[] next = pending[0];
        System.out.println("Next to Allocate          : "
                + ctrl.getNameForConfirmation(next[1]) + " ["
                + ctrl.getTierForConfirmation(next[1]) + "] (Booking "
                + next[0] + ", Conf# " + next[1] + ")");
    }

    // ------------------------------------------------------------------
    // 6. Reports
    // ------------------------------------------------------------------

    private void handleReport() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("VIP BOOKING REPORTS");
            System.out.println(" [1] Report 1: VIP Booking Report");
            System.out.println(" [2] Report 2: VIP Revenue Summary Report");
            System.out.println(" [0] Back to VIP Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-2): ");

            Integer selected = readIntOption(0, 2);
            if (selected == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 2.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            choice = selected;
            if (choice == 1) {
                handleVipBookingReport();
                UIUtils.pressEnterToContinue(scanner);
            } else if (choice == 2) {
                handleVipRevenueSummaryReport();
                UIUtils.pressEnterToContinue(scanner);
            }
        }
    }

    private void handleVipBookingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: VIP BOOKING REPORT");

        String statusFilter = promptStatusFilter();
        if (statusFilter == null) return;
        String roomFilter = promptRoomTypeFilter();
        if (roomFilter == null) return;
        String sortOrder = promptSortOrder();
        if (sortOrder == null) return;

        String[][] rows = ctrl.generateVipBookingReport(statusFilter, roomFilter, sortOrder);
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: VIP BOOKING REPORT");
        System.out.println("Status Filter    : " + statusFilter);
        System.out.println("Room Type Filter : " + roomFilter);
        System.out.println("Sort             : " + ("TIER_ASC".equals(sortOrder)
                ? "Tier Lowest -> Highest" : "Tier Highest -> Lowest"));
        UIUtils.printSectionLine();
        displayBookingTableWithTier(rows);

        System.out.println("Pending     : " + countByStatus(rows, VIPRoomAllocationController.STATUS_PENDING));
        System.out.println("Assigned    : " + countByStatus(rows, VIPRoomAllocationController.STATUS_ASSIGNED));
        System.out.println("Checked In  : " + countByStatus(rows, VIPRoomAllocationController.STATUS_CHECKED_IN));
        System.out.println("Checked Out : " + countByStatus(rows, VIPRoomAllocationController.STATUS_CHECKED_OUT));
        System.out.println("Cancelled   : " + countByStatus(rows, VIPRoomAllocationController.STATUS_CANCELLED));
    }

    private void handleVipRevenueSummaryReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 2: VIP REVENUE SUMMARY REPORT");

        String tierFilter = promptRevenueTierFilter();
        if (tierFilter == null) return;
        String roomFilter = promptRoomTypeFilter();
        if (roomFilter == null) return;
        String sortOrder = promptRevenueSortOrder();
        if (sortOrder == null) return;

        VIPRoomAllocationController.VipRevenueRow[] rows =
                ctrl.generateVipRevenueSummary(tierFilter, roomFilter, sortOrder);

        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 2: VIP REVENUE SUMMARY REPORT");
        System.out.println("Loyalty Tier : " + tierFilter);
        System.out.println("Room Type    : " + roomFilter);
        System.out.println("Payment      : Paid");
        System.out.println("Sort         : " + revenueSortLabel(sortOrder));
        UIUtils.printSectionLine();
        System.out.printf("%-12s | %12s | %18s%n", "Tier", "Bookings", "Revenue (RM)");
        UIUtils.printSectionLine();

        int totalBookings = 0;
        double totalRevenue = 0.0;
        for (VIPRoomAllocationController.VipRevenueRow row : rows) {
            System.out.printf("%-12s | %12d | %,18.2f%n",
                    row.getTier(), row.getBookingCount(), row.getRevenue());
            totalBookings += row.getBookingCount();
            totalRevenue += row.getRevenue();
        }

        UIUtils.printSectionLine();
        System.out.printf("%-12s | %12d | %,18.2f%n", "TOTAL", totalBookings, totalRevenue);
        UIUtils.printSectionLine();
        if (totalBookings == 0) {
            System.out.println("No paid VIP billing records match the selected filters.");
        } else {
            System.out.println("Only paid VIP bookings are included in this report.");
        }
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------

    private void printGuestTable(Guest[] guests) {
        System.out.printf("%-14s | %-20s | %-10s%n", "Confirmation", "Name", "Tier");
        UIUtils.printSectionLine();
        for (Guest guest : guests) {
            if (guest != null) {
                System.out.printf("%-14s | %-20s | %-10s%n",
                        guest.getConfirmationNo(), guest.getName(), guest.getLoyaltyTier());
            }
        }
        UIUtils.printSectionLine();
    }

    private void printVipGuestDetailsTable(Guest[] guests) {
        System.out.printf("%-14s | %-20s | %-15s | %-10s%n",
                "Confirmation", "Name", "Phone", "Tier");
        UIUtils.printSectionLine();
        int count = 0;
        for (Guest guest : guests) {
            if (guest != null) {
                count++;
                System.out.printf("%-14s | %-20s | %-15s | %-10s%n",
                        guest.getConfirmationNo(), guest.getName(), guest.getPhone(),
                        guest.getLoyaltyTier());
            }
        }
        UIUtils.printSectionLine();
        System.out.println("Total VIP Guests: " + count);
    }

    private void displayBookingTable(String[][] rows) {
        System.out.printf("%-7s | %-12s | %-10s | %-10s | %-10s | %s%n",
                "ID", "Conf#", "Room Type", "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();
        for (String[] row : rows) {
            System.out.printf("%-7s | %-12s | %-10s | %-10s | %-10s | %s%n",
                    row[0], row[1], row[3], row[4], row[5], row[6]);
        }
        UIUtils.printSectionLine();
        System.out.println("Total Bookings: " + rows.length);
    }

    private void displayBookingTableWithTier(String[][] rows) {
        if (rows.length == 0) {
            System.out.println("No records match the selected filters.");
            return;
        }
        System.out.printf("%-7s | %-12s | %-18s | %-10s | %-10s | %-10s | %-10s | %s%n",
                "ID", "Conf#", "Guest", "Tier", "Room Type", "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();
        for (String[] row : rows) {
            String name = ctrl.getNameForConfirmation(row[1]);
            if (name.length() > 18) name = name.substring(0, 15) + "...";
            System.out.printf("%-7s | %-12s | %-18s | %-10s | %-10s | %-10s | %-10s | %s%n",
                    row[0], row[1], name, ctrl.getTierForConfirmation(row[1]),
                    row[3], row[4], row[5], row[6]);
        }
        UIUtils.printSectionLine();
        System.out.println("Total Records: " + rows.length);
    }

    private String[] findBookingRow(String[][] rows, String bookingId) {
        for (String[] row : rows) {
            if (row[0].equalsIgnoreCase(bookingId)) return row;
        }
        return null;
    }

    private int countByStatus(String[][] rows, String status) {
        int count = 0;
        for (String[] row : rows) {
            if (row[6].equalsIgnoreCase(status)) count++;
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Prompts
    // ------------------------------------------------------------------

    private String promptTier() {
        while (true) {
            System.out.print("Please enter choice (0-5): ");
            Integer choice = readIntOption(0, 5);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 5.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return "Diamond";
                case 2: return "Elite";
                case 3: return "Platinum";
                case 4: return "Gold";
                case 5: return "Silver";
                default: break;
            }
        }
    }

    private String promptRoomType() {
        while (true) {
            System.out.println("\nSelect Room Type:");
            System.out.println(" [1] Standard");
            System.out.println(" [2] Deluxe");
            System.out.println(" [3] Suite");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-3): ");
            Integer choice = readIntOption(0, 3);
            if (choice == null) continue;
            switch (choice) {
                case 0: return null;
                case 1: return "Standard";
                case 2: return "Deluxe";
                case 3: return "Suite";
                default: break;
            }
        }
    }

    private String promptStatusFilter() {
        while (true) {
            System.out.println("Filter by booking status:");
            System.out.println(" [1] All  [2] Pending  [3] Assigned  [4] Checked In");
            System.out.println(" [5] Checked Out  [6] Cancelled  [0] Cancel");
            System.out.print("Please enter choice (0-6): ");
            Integer choice = readIntOption(0, 6);
            if (choice == null) continue;
            switch (choice) {
                case 0: return null;
                case 1: return "ALL";
                case 2: return VIPRoomAllocationController.STATUS_PENDING;
                case 3: return VIPRoomAllocationController.STATUS_ASSIGNED;
                case 4: return VIPRoomAllocationController.STATUS_CHECKED_IN;
                case 5: return VIPRoomAllocationController.STATUS_CHECKED_OUT;
                case 6: return VIPRoomAllocationController.STATUS_CANCELLED;
                default: break;
            }
        }
    }

    private String promptRoomTypeFilter() {
        while (true) {
            System.out.println("\nFilter by room type:");
            System.out.println(" [1] All  [2] Standard  [3] Deluxe  [4] Suite  [0] Cancel");
            System.out.print("Please enter choice (0-4): ");
            Integer choice = readIntOption(0, 4);
            if (choice == null) continue;
            switch (choice) {
                case 0: return null;
                case 1: return "ALL";
                case 2: return "Standard";
                case 3: return "Deluxe";
                case 4: return "Suite";
                default: break;
            }
        }
    }

    private String promptSortOrder() {
        while (true) {
            System.out.println("\nSort by loyalty tier:");
            System.out.println(" [1] Highest to Lowest (Diamond -> Silver)");
            System.out.println(" [2] Lowest to Highest (Silver -> Diamond)");
            System.out.println(" [0] Cancel");
            System.out.print("Please enter choice (0-2): ");
            Integer choice = readIntOption(0, 2);
            if (choice == null) continue;
            if (choice == 0) return null;
            if (choice == 1) return "TIER_DESC";
            return "TIER_ASC";
        }
    }

    private String promptRevenueTierFilter() {
        while (true) {
            System.out.println("Filter by loyalty tier:");
            System.out.println(" [1] All  [2] Diamond  [3] Elite  [4] Platinum");
            System.out.println(" [5] Gold  [6] Silver  [0] Cancel");
            System.out.print("Please enter choice (0-6): ");
            Integer choice = readIntOption(0, 6);
            if (choice == null) continue;
            switch (choice) {
                case 0: return null;
                case 1: return "ALL";
                case 2: return "Diamond";
                case 3: return "Elite";
                case 4: return "Platinum";
                case 5: return "Gold";
                case 6: return "Silver";
                default: break;
            }
        }
    }

    private String promptRevenueSortOrder() {
        while (true) {
            System.out.println("\nSort revenue summary:");
            System.out.println(" [1] Revenue Highest to Lowest");
            System.out.println(" [2] Revenue Lowest to Highest");
            System.out.println(" [3] Loyalty Tier Highest to Lowest");
            System.out.println(" [0] Cancel");
            System.out.print("Please enter choice (0-3): ");
            Integer choice = readIntOption(0, 3);
            if (choice == null) continue;
            switch (choice) {
                case 0: return null;
                case 1: return "REVENUE_DESC";
                case 2: return "REVENUE_ASC";
                case 3: return "TIER_DESC";
                default: break;
            }
        }
    }

    private String revenueSortLabel(String sortOrder) {
        if ("REVENUE_ASC".equals(sortOrder)) return "Revenue Lowest -> Highest";
        if ("TIER_DESC".equals(sortOrder)) return "Tier Highest -> Lowest";
        return "Revenue Highest -> Lowest";
    }

    private String promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;
            try {
                DateUtils.parseDate(input);
                return input;
            } catch (Exception e) {
                UIUtils.printError("Date format must be YYYY-MM-DD.");
            }
        }
    }

    private String promptCheckInDate(String prompt) {
        while (true) {
            String input = promptDate(prompt);
            if (input == null) return null;
            if (!DateUtils.isBeforeToday(input)) return input;
            UIUtils.printError("Check-in date cannot be before today.");
        }
    }

    private String promptCheckOutDate(LocalDate checkInDate, String prompt) {
        while (true) {
            String input = promptDate(prompt);
            if (input == null) return null;
            if (DateUtils.parseDate(input).isAfter(checkInDate)) return input;
            UIUtils.printError("Check-out date must be after check-in date.");
        }
    }

    private Integer readIntOption(int min, int max) {
        if (scanner.hasNextInt()) {
            int value = scanner.nextInt();
            scanner.nextLine();
            return value >= min && value <= max ? value : null;
        }
        scanner.nextLine();
        return null;
    }
}
