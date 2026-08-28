package boundary;

import control.VIPRoomAllocation;
import entity.Guest;
import utility.UIUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Boundary class for Module 2 — VIP & Loyalty Tier Priority Room Allocation.
 *
 * Menu:
 *   1. Add VIP Guest
 *   2. Remove VIP Guest
 *   3. Create Booking
 *   4. Cancel Booking
 *   5. View Priority Queue
 *   6. Generate Report
 *   0. Back
 *
 * NOTE: Allocate Room, Check In, and Check Out have been removed.
 *
 * @author Kaizen Soh
 */
public class VIPRoomAllocationUI {

    private static final String BOOKINGS_FILE = "bookings.txt";

    // Status constants — defined locally since VIPRoomAllocation does not expose them publicly.
    private static final String STATUS_PENDING     = "Pending";
    private static final String STATUS_ASSIGNED    = "Assigned";
    private static final String STATUS_CHECKED_IN  = "Checked In";
    private static final String STATUS_CHECKED_OUT = "Checked Out";
    private static final String STATUS_CANCELLED   = "Cancelled";

    // Tier rates — mirrors VIPRoomAllocation.TIER_RATES (private there, duplicated here for preview).
    private static final java.util.Map<String, Double> TIER_RATES = new java.util.HashMap<>();
    static {
        TIER_RATES.put("DIAMOND",  1099.00);
        TIER_RATES.put("ELITE",     899.00);
        TIER_RATES.put("PLATINUM",  699.00);
        TIER_RATES.put("GOLD",      599.00);
        TIER_RATES.put("SILVER",    399.00);
    }

    private final VIPRoomAllocation ctrl;
    private final Scanner           scanner;

    public VIPRoomAllocationUI(Scanner scanner) {
        this.ctrl    = new VIPRoomAllocation();
        this.scanner = scanner;
    }

    // -------------------------------------------------------
    // Entry point
    // -------------------------------------------------------

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
                case 1: handleAddGuest();       break;
                case 2: handleRemoveGuest();    break;
                case 3: handleCreateBooking();  break;
                case 4: handleCancelBooking();  break;
                case 5: handleViewQueue();      break;
                case 6: handleReport();         break;
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

    // -------------------------------------------------------
    // 1. ADD GUEST
    // -------------------------------------------------------

    private void handleAddGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD VIP GUEST");

        String name;
        while (true) {
            System.out.print("Enter Guest Name (0 to return): ");
            name = scanner.nextLine().trim();

            if (name.equals("0")) {
                System.out.println("\nReturning to VIP menu...");
                return;
            }

            if (name.isEmpty()) {
                UIUtils.printError("Guest name cannot be empty.");
                continue;
            }

            if (!name.matches("[A-Za-z ]+")) {
                UIUtils.printError("Guest name must contain alphabetic characters only.");
                continue;
            }

            break;
        }

        String phone;
        while (true) {
            System.out.print("Enter Phone Number (0 to return): ");
            phone = scanner.nextLine().trim();

            if (phone.equals("0")) {
                System.out.println("\nReturning to VIP menu...");
                return;
            }

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
        if (tier == null) {
            System.out.println("\nGuest registration cancelled.");
            return;
        }

        System.out.println();
        UIUtils.printSectionLine();
        System.out.println("Guest Details");
        UIUtils.printSectionLine();
        System.out.println("Name         : " + name);
        System.out.println("Phone        : " + phone);
        System.out.println("Loyalty Tier : " + tier);
        UIUtils.printSectionLine();

        System.out.print("Confirm add guest? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\nGuest registration cancelled.");
            return;
        }

        try {
            String confirmationNo = ctrl.generateConfirmationNo();
            Guest g = new Guest(confirmationNo, name, phone, tier);
            ctrl.addGuest(g);

            System.out.println("\nGuest added successfully.");
            System.out.println("Generated Confirmation No : " + g.getConfirmationNo());
            System.out.println("Name                      : " + g.getName());
            System.out.println("Phone                     : " + g.getPhone());
            System.out.println("Loyalty Tier              : " + g.getLoyaltyTier());
            UIUtils.printSectionLine();
            System.out.println("VIP Queue Size : " + ctrl.getQueueSize());
        } catch (IllegalArgumentException e) {
            UIUtils.printError(e.getMessage());
        }
    }

    // -------------------------------------------------------
    // 2. REMOVE VIP GUEST
    // -------------------------------------------------------

    private void handleRemoveGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REMOVE VIP GUEST");

        Guest[] guests = ctrl.getAllWaitingGuests();

        if (guests == null || guests.length == 0) {
            System.out.println("No VIP guests available to remove.");
            return;
        }

        printVipGuestDetailsTable(guests);

        System.out.print("Enter Guest Confirmation No to remove (0 to return): ");
        String confirmationNo = scanner.nextLine().trim();

        if (confirmationNo.equals("0")) {
            System.out.println("\nReturning to VIP menu...");
            return;
        }

        Guest guest = ctrl.findGuestInQueue(confirmationNo);

        if (guest == null) {
            UIUtils.printError("VIP guest not found.");
            return;
        }

        System.out.println();
        UIUtils.printSectionLine();
        System.out.println("Confirmation No : " + guest.getConfirmationNo());
        System.out.println("Name            : " + guest.getName());
        System.out.println("Phone           : " + guest.getPhone());
        System.out.println("Loyalty Tier    : " + guest.getLoyaltyTier());
        UIUtils.printSectionLine();

        System.out.print("Confirm removal? (yes/no): ");
        String confirm = scanner.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\nGuest removal cancelled.");
            return;
        }

        boolean removed = ctrl.removeGuestFromQueue(confirmationNo);

        if (removed) {
            System.out.println("\nVIP guest removed successfully.");
        } else {
            UIUtils.printError("Failed to remove VIP guest.");
        }
    }

    // -------------------------------------------------------
    // 3. CREATE BOOKING
    // -------------------------------------------------------

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

        if (confirmationNo.equals("0")) {
            System.out.println("\nReturning to VIP menu...");
            return;
        }

        Guest g = ctrl.findGuestInQueue(confirmationNo);
        if (g == null) {
            UIUtils.printError("Guest not found in VIP queue.");
            return;
        }

        System.out.println("\nGuest : " + g.getName() + " [" + g.getLoyaltyTier() + "]");

        String roomType = promptRoomType();
        if (roomType == null) {
            System.out.println("\nBooking cancelled.");
            return;
        }

        String checkIn = promptCheckInDate("Enter Check-In Date (YYYY-MM-DD, or 0 to cancel): ");
        if (checkIn == null) {
            System.out.println("\nBooking cancelled.");
            return;
        }

        String checkOut = promptCheckOutDate(LocalDate.parse(checkIn),
                "Enter Check-Out Date (YYYY-MM-DD, or 0 to cancel): ");
        if (checkOut == null) {
            System.out.println("\nBooking cancelled.");
            return;
        }

        // Billing preview — mirrors VIPRoomAllocation.saveBillingToFile logic.
        double preview = computePreview(g.getLoyaltyTier(), checkIn, checkOut);
        UIUtils.printSectionLine();
        System.out.println("Room Type  : " + roomType);
        System.out.println("Check-In   : " + checkIn);
        System.out.println("Check-Out  : " + checkOut);
        System.out.printf ("Est. Total : RM %.2f%n", preview);
        UIUtils.printSectionLine();

        System.out.print("Confirm booking? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\nBooking cancelled.");
            return;
        }

        // Create an actual Pending VIP booking in bookings.txt.
        // The guest is already in the VIP queue, so do NOT add the guest again.
        String error = ctrl.createPendingVipBooking(g, roomType, checkIn, checkOut);

        if (error == null) {
            System.out.println("\nBooking created successfully. Status: Pending.");
            System.out.println("Rooms Available : " + ctrl.getAvailableRoomCount());
        } else {
            UIUtils.printError(error);
        }
    }

    // -------------------------------------------------------
    // 4. CANCEL BOOKING
    // -------------------------------------------------------

    private void handleCancelBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CANCEL VIP BOOKING");

        // Only Pending VIP bookings can be cancelled.
        List<String[]> cancellable = readVipBookingsByStatus(STATUS_PENDING);

        if (cancellable.isEmpty()) {
            System.out.println("No pending VIP bookings available for cancellation.");
            return;
        }

        displayBookingTable(cancellable);

        System.out.print("Enter Booking ID to cancel (0 to return): ");
        String bookingId = scanner.nextLine().trim();

        if (bookingId.equals("0")) {
            System.out.println("\nReturning to VIP menu...");
            return;
        }

        String[] target = null;
        for (String[] row : cancellable) {
            if (row[0].equalsIgnoreCase(bookingId)) { target = row; break; }
        }

        if (target == null) {
            UIUtils.printError("Booking ID not found or booking is no longer Pending.");
            return;
        }

        // row: bookingId|confirmationNo|bookingType|requestedRoomType|checkIn|checkOut|status|roomNo|createdAt
        System.out.println("\nConf #    : " + target[1]);
        System.out.println("Room Type : " + target[3]);
        System.out.println("Check-In  : " + target[4] + "  |  Check-Out : " + target[5]);

        System.out.print("\nConfirm cancellation? (yes/no): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes") && !confirm.equalsIgnoreCase("y")) {
            System.out.println("\nCancellation aborted.");
            return;
        }

        boolean ok = updateBookingStatus(bookingId, STATUS_CANCELLED);
        if (ok) {
            System.out.println("\nBooking cancelled successfully.");
        } else {
            UIUtils.printError("Failed to update bookings.txt. Please check the file.");
        }
    }

    // -------------------------------------------------------
    // 5. VIEW PRIORITY QUEUE
    // -------------------------------------------------------

    private void handleViewQueue() {
        UIUtils.clearScreen();
        UIUtils.printHeader("VIP BOOKING PRIORITY QUEUE");

        // Only actual Pending VIP bookings belong in this queue view.
        List<String[]> pendingBookings = readVipBookingsByStatus(STATUS_PENDING);

        if (pendingBookings.isEmpty()) {
            System.out.println("No pending VIP bookings in the priority queue.");
            return;
        }

        /*
         * Priority rule:
         * 1. Higher loyalty tier first: Diamond -> Elite -> Platinum -> Gold -> Silver
         * 2. Within the same tier, earlier check-in date first
         * 3. If check-in dates are the same, earlier creation time first
         */
        pendingBookings.sort((a, b) -> {
            int tierCompare = Integer.compare(
                    tierRank(getTierForConf(a[1])),
                    tierRank(getTierForConf(b[1])));

            if (tierCompare != 0) {
                return tierCompare;
            }

            try {
                LocalDate dateA = LocalDate.parse(a[4]);
                LocalDate dateB = LocalDate.parse(b[4]);
                int dateCompare = dateA.compareTo(dateB);

                if (dateCompare != 0) {
                    return dateCompare;
                }
            } catch (Exception ignored) {
                // Fall through to creation time if a stored date is invalid.
            }

            return a[8].compareToIgnoreCase(b[8]);
        });

        System.out.println("Bookings are sorted by loyalty tier, then earliest check-in date.");
        System.out.println();
        System.out.printf("%-7s | %-12s | %-20s | %-10s | %-10s | %-10s | %-11s | %s%n",
                "Booking", "Conf#", "Guest Name", "Tier",
                "Room Type", "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();

        int count = 0;
        Guest nextGuest = null;
        String nextBookingId = null;

        for (String[] booking : pendingBookings) {
            Guest guest = ctrl.findGuestInQueue(booking[1]);

            // Skip invalid/orphaned booking records that no longer have a VIP guest.
            if (guest == null) {
                continue;
            }

            String name = guest.getName();
            if (name.length() > 20) {
                name = name.substring(0, 17) + "...";
            }

            System.out.printf("%-7s | %-12s | %-20s | %-10s | %-10s | %-10s | %-11s | %s%n",
                    booking[0], booking[1], name, guest.getLoyaltyTier(),
                    booking[3], booking[4], booking[5], booking[6]);

            count++;

            // First displayed booking is the highest-priority booking.
            if (nextGuest == null) {
                nextGuest = guest;
                nextBookingId = booking[0];
            }
        }

        UIUtils.printSectionLine();
        System.out.println("Total Pending VIP Bookings: " + count);

        if (nextGuest != null) {
            System.out.println("Next to Allocate          : "
                    + nextGuest.getName()
                    + " [" + nextGuest.getLoyaltyTier() + "]"
                    + " (Booking " + nextBookingId
                    + ", Conf# " + nextGuest.getConfirmationNo() + ")");
        }
    }

    // -------------------------------------------------------
    // 6. GENERATE REPORT
    // -------------------------------------------------------

    private void handleReport() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("VIP BOOKING REPORTS");
            System.out.println(" [1] Report 1: VIP Booking Report");
            System.out.println(" [0] Back to VIP Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-1): ");

            Integer selected = readIntOption(0, 1);
            if (selected == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 1.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            choice = selected;

            switch (choice) {
                case 1:
                    handleVipBookingReport();
                    UIUtils.pressEnterToContinue(scanner);
                    break;
                case 0:
                    break;
                default:
                    UIUtils.printError("Invalid choice. Try again.");
                    UIUtils.pressEnterToContinue(scanner);
            }
        }
    }

    private void handleVipBookingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: VIP BOOKING REPORT");

        // Sort order
        String sortOrder = promptSortOrder();
        if (sortOrder == null) { System.out.println("\nReport cancelled."); return; }

        // Status filter
        String statusFilter = promptStatusFilter();
        if (statusFilter == null) { System.out.println("\nReport cancelled."); return; }

        // Room type filter
        String roomFilter = promptRoomTypeFilter();
        if (roomFilter == null) { System.out.println("\nReport cancelled."); return; }

        // Read, filter, sort.
        List<String[]> all = readAllVipBookings();
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : all) {
            if (!"ALL".equals(statusFilter) && !row[6].equalsIgnoreCase(statusFilter)) continue;
            if (!"ALL".equals(roomFilter)   && !row[3].equalsIgnoreCase(roomFilter))   continue;
            filtered.add(row);
        }

        boolean tierDesc = "TIER_DESC".equals(sortOrder);
        filtered.sort((a, b) -> {
            int ra = tierRank(getTierForConf(a[1]));
            int rb = tierRank(getTierForConf(b[1]));
            return tierDesc ? Integer.compare(ra, rb) : Integer.compare(rb, ra);
        });

        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: VIP BOOKING REPORT");
        System.out.println("Sort Order  : " + (tierDesc ? "Highest to Lowest Tier" : "Lowest to Highest Tier"));
        System.out.println("Status      : " + statusFilter);
        System.out.println("Room Type   : " + roomFilter);
        UIUtils.printSectionLine();

        if ("ALL".equals(statusFilter)) {
            System.out.println("Pending      : " + countByStatus(filtered, STATUS_PENDING));
            System.out.println("Assigned     : " + countByStatus(filtered, STATUS_ASSIGNED));
            System.out.println("Checked In   : " + countByStatus(filtered, STATUS_CHECKED_IN));
            System.out.println("Checked Out  : " + countByStatus(filtered, STATUS_CHECKED_OUT));
            System.out.println("Cancelled    : " + countByStatus(filtered, STATUS_CANCELLED));
            System.out.println("Total Matched: " + filtered.size());
            UIUtils.printSectionLine();
        } else {
            System.out.println("Matched Bookings: " + filtered.size());
            UIUtils.printSectionLine();
        }

        displayBookingTableWithTier(filtered);
    }

    // -------------------------------------------------------
    // bookings.txt helpers
    // -------------------------------------------------------

    /** Reads all rows from bookings.txt where bookingType == "VIP". */
    private List<String[]> readAllVipBookings() {
        List<String[]> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 7) continue;
                if ("VIP".equalsIgnoreCase(parts[2])) {
                    String[] row = new String[9];
                    for (int i = 0; i < 9; i++) row[i] = i < parts.length ? parts[i].trim() : "";
                    result.add(row);
                }
            }
        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + BOOKINGS_FILE + ": " + e.getMessage());
        }
        return result;
    }

    /** Reads VIP bookings from bookings.txt that match any of the given statuses. */
    private List<String[]> readVipBookingsByStatus(String... statuses) {
        List<String[]> result = new ArrayList<>();
        for (String[] row : readAllVipBookings()) {
            for (String s : statuses) {
                if (row[6].equalsIgnoreCase(s)) { result.add(row); break; }
            }
        }
        return result;
    }

    /**
     * Rewrites bookings.txt, changing the status field of the given booking ID
     * to newStatus. Returns true on success.
     */
    private boolean updateBookingStatus(String bookingId, String newStatus) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    String[] parts = trimmed.split("\\|");
                    if (parts.length >= 7 && parts[0].trim().equalsIgnoreCase(bookingId)) {
                        parts[6] = newStatus;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < parts.length; i++) {
                            if (i > 0) sb.append("|");
                            sb.append(parts[i]);
                        }
                        line = sb.toString();
                        found = true;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }
        if (!found) return false;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE, false))) {
            for (String l : lines) { bw.write(l); bw.newLine(); }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Computes a billing preview: nights × tier rate.
     * Mirrors VIPRoomAllocation.saveBillingToFile logic exactly.
     */
    private double computePreview(String loyaltyTier, String checkIn, String checkOut) {
        long nights = 1;
        try {
            LocalDate in  = LocalDate.parse(checkIn);
            LocalDate out = LocalDate.parse(checkOut);
            long computed = ChronoUnit.DAYS.between(in, out);
            if (computed > 0) nights = computed;
        } catch (Exception ignored) {}
        double rate = TIER_RATES.getOrDefault(loyaltyTier.toUpperCase(), 399.00);
        return nights * rate;
    }

    // -------------------------------------------------------
    // Report helpers
    // -------------------------------------------------------

    private int countByStatus(List<String[]> rows, String status) {
        int n = 0;
        for (String[] r : rows) if (r[6].equalsIgnoreCase(status)) n++;
        return n;
    }

    private int tierRank(String tier) {
        if (tier == null) return 99;
        switch (tier.toUpperCase()) {
            case "DIAMOND":  return 1;
            case "ELITE":    return 2;
            case "PLATINUM": return 3;
            case "GOLD":     return 4;
            case "SILVER":   return 5;
            default:         return 99;
        }
    }

    /** Looks up loyalty tier for a confirmation number from the live VIP queue. */
    private String getTierForConf(String confirmationNo) {
        Guest g = ctrl.findGuestInQueue(confirmationNo);
        return g != null ? g.getLoyaltyTier() : "—";
    }

    /** Looks up guest name for a confirmation number from the live VIP queue. */
    private String getNameForConf(String confirmationNo) {
        Guest g = ctrl.findGuestInQueue(confirmationNo);
        return g != null ? g.getName() : confirmationNo;
    }

    // -------------------------------------------------------
    // Table display helpers
    // -------------------------------------------------------

    /** Prints the VIP queue as a guest table (used in Create Booking). */
    private void printGuestTable(Guest[] guests) {
        if (guests == null || guests.length == 0) {
            System.out.println("No guests in VIP queue.");
            return;
        }
        System.out.printf("%-14s | %-20s | %-10s%n", "Confirmation", "Name", "Tier");
        UIUtils.printSectionLine();
        for (Guest g : guests) {
            if (g == null) continue;
            System.out.printf("%-14s | %-20s | %-10s%n",
                    g.getConfirmationNo(), g.getName(), g.getLoyaltyTier());
        }
        UIUtils.printSectionLine();
    }

    /** Prints full VIP guest details (used in Remove VIP Guest). */
    private void printVipGuestDetailsTable(Guest[] guests) {
        if (guests == null || guests.length == 0) {
            System.out.println("No VIP guests found.");
            return;
        }

        System.out.printf("%-14s | %-20s | %-15s | %-10s%n",
                "Confirmation", "Name", "Phone", "Tier");
        UIUtils.printSectionLine();

        int count = 0;
        for (Guest g : guests) {
            if (g == null) continue;
            count++;
            System.out.printf("%-14s | %-20s | %-15s | %-10s%n",
                    g.getConfirmationNo(),
                    g.getName(),
                    g.getPhone(),
                    g.getLoyaltyTier());
        }

        UIUtils.printSectionLine();
        System.out.println("Total VIP Guests: " + count);
    }

    /** Displays a table of raw booking rows (String[9]) for cancel/view. */
    private void displayBookingTable(List<String[]> list) {
        if (list.isEmpty()) {
            System.out.println("No booking records found.");
            return;
        }
        System.out.printf("%-7s | %-12s | %-10s | %-10s | %-10s | %s%n",
                "ID", "Conf#", "Room Type", "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();
        for (String[] row : list) {
            System.out.printf("%-7s | %-12s | %-10s | %-10s | %-10s | %s%n",
                    row[0], row[1], row[3], row[4], row[5], row[6]);
        }
        UIUtils.printSectionLine();
        System.out.println("Total Bookings: " + list.size());
    }

    /** Displays the report table with guest name and tier columns. */
    private void displayBookingTableWithTier(List<String[]> list) {
        if (list.isEmpty()) {
            System.out.println("No records match the selected filters.");
            return;
        }
        System.out.printf("%-7s | %-12s | %-18s | %-10s | %-10s | %-10s | %-10s | %s%n",
                "ID", "Conf#", "Guest", "Tier", "Room Type", "Check-In", "Check-Out", "Status");
        UIUtils.printSectionLine();
        for (String[] row : list) {
            String name = getNameForConf(row[1]);
            if (name.length() > 18) name = name.substring(0, 15) + "...";
            String tier = getTierForConf(row[1]);
            System.out.printf("%-7s | %-12s | %-18s | %-10s | %-10s | %-10s | %-10s | %s%n",
                    row[0], row[1], name, tier, row[3], row[4], row[5], row[6]);
        }
        UIUtils.printSectionLine();
    }

    // -------------------------------------------------------
    // Prompt helpers
    // -------------------------------------------------------

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
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 3.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return "Standard";
                case 2: return "Deluxe";
                case 3: return "Suite";
            }
        }
    }

    private String promptSortOrder() {
        while (true) {
            System.out.println("Sort by loyalty tier:");
            System.out.println(" [1] Highest to Lowest  (Diamond → Silver)");
            System.out.println(" [2] Lowest to Highest  (Silver → Diamond)");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-2): ");
            Integer choice = readIntOption(0, 2);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 2.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return "TIER_DESC";
                case 2: return "TIER_ASC";
            }
        }
    }

    private String promptStatusFilter() {
        while (true) {
            System.out.println("\nFilter by booking status:");
            System.out.println(" [1] All  [2] Pending  [3] Assigned  [4] Checked In");
            System.out.println(" [5] Checked Out  [6] Cancelled  [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-6): ");
            Integer choice = readIntOption(0, 6);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 6.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return "ALL";
                case 2: return STATUS_PENDING;
                case 3: return STATUS_ASSIGNED;
                case 4: return STATUS_CHECKED_IN;
                case 5: return STATUS_CHECKED_OUT;
                case 6: return STATUS_CANCELLED;
            }
        }
    }

    private String promptRoomTypeFilter() {
        while (true) {
            System.out.println("\nFilter by room type:");
            System.out.println(" [1] All  [2] Standard  [3] Deluxe  [4] Suite  [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");
            Integer choice = readIntOption(0, 4);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 4.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return "ALL";
                case 2: return "Standard";
                case 3: return "Deluxe";
                case 4: return "Suite";
            }
        }
    }

    private String promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) return null;
            try {
                LocalDate.parse(input);
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
            if (!LocalDate.parse(input).isBefore(LocalDate.now())) return input;
            UIUtils.printError("Check-in date cannot be before today.");
        }
    }

    private String promptCheckOutDate(LocalDate checkInDate, String prompt) {
        while (true) {
            String input = promptDate(prompt);
            if (input == null) return null;
            if (LocalDate.parse(input).isAfter(checkInDate)) return input;
            UIUtils.printError("Check-out date must be after check-in date.");
        }
    }

    // -------------------------------------------------------
    // Input helpers
    // -------------------------------------------------------

    private Integer readIntOption(int min, int max) {
        if (scanner.hasNextInt()) {
            int value = scanner.nextInt();
            scanner.nextLine();
            if (value >= min && value <= max) return value;
            return null;
        }
        scanner.nextLine();
        return null;
    }
}