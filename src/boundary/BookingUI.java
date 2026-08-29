package boundary;

import adt.ListInterface;
import control.BookingController;
import control.BookingController.AssignResult;
import control.BookingController.WalkInResult;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;
import utility.DateUtils;
import utility.UIUtils;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * @author Elwin Goh Yao Zu
 */
public class BookingUI {
    private BookingController controller;
    private Scanner scanner;

    public BookingUI(Scanner scanner) {
        controller = new BookingController();
        this.scanner = scanner;
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("WALK-IN AND STANDARD BOOKING MENU");
            System.out.println(" [1] Add Guest");
            System.out.println(" [2] Add Walk-In Booking");
            System.out.println(" [3] Add Standard Booking");
            System.out.println(" [4] View Current Bookings");
            System.out.println(" [5] View Pending Standard Queue");
            System.out.println(" [6] Auto Assign Room");
            System.out.println(" [7] Check In Booking");
            System.out.println(" [8] Check Out Booking");
            System.out.println(" [9] Cancel Booking");
            System.out.println(" [10] Generate Reports");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-10): ");

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
                case 1: handleAddGuest();           break;
                case 2: handleAddWalkIn();          break;
                case 3: handleAddStandardBooking(); break;
                case 4: handleViewBookings();       break;
                case 5: handleViewPendingQueue();   break;
                case 6: handleAutoAssign();         break;
                case 7: handleCheckInBooking();     break;
                case 8: handleCheckOutBooking();    break;
                case 9: handleCancelBooking();      break;
                case 10: handleReports();           break;
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

    private void handleAddGuest() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD GUEST");

        System.out.print("Enter Guest Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine().trim();

        Guest guest = controller.addGuest(name, phone);
        if (guest != null) {
            System.out.println("\nGuest added successfully.");
            System.out.println("Generated Confirmation No: " + guest.getConfirmationNo());
        } else {
            UIUtils.printError("Guest name cannot be empty and phone number must contain 10 or 11 digits.");
        }
    }

    /**
     * Handles walk-in booking using the new WalkInResult pattern.
     *
     * Flow:
     *   1. Call controller.addWalkInRegistration()
     *   2. If success     → show confirmation
     *   3. If manualNeeded → show available room list, prompt selection,
     *                        call controller.completeWalkInManual()
     *   4. If error       → show error message
     */
    private void handleAddWalkIn() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD WALK-IN BOOKING");
        printGuestTable(controller.getAllGuests());

        System.out.print("Enter Guest Confirmation No: ");
        String confirmationNo = scanner.nextLine().trim();
        if (controller.getGuest(confirmationNo) == null) {
            UIUtils.printError("Guest not found. Add guest first.");
            return;
        }

        String roomType = promptRoomType();
        if (roomType == null) {
            System.out.println("\nWalk-in booking cancelled.");
            return;
        }

        String checkOutDate = promptCheckOutDate(LocalDate.now(),
                "Enter Check-Out Date (YYYY-MM-DD, or 0 to cancel): ");
        if (checkOutDate == null) {
            System.out.println("\nWalk-in booking cancelled.");
            return;
        }

        WalkInResult result =
                controller.addWalkInRegistration(confirmationNo, roomType, checkOutDate);

        if (result.isSuccess()) {
            // Auto-assigned successfully
            BookingRequest booking = result.getBooking();
            System.out.println();
            UIUtils.printSectionLine();
            printBookingDetail(booking);
            System.out.println("Walk-in booking added and room assigned successfully.");

        } else if (result.isManualNeeded()) {
            // No room of requested type — prompt manual selection
            System.out.println();
            UIUtils.printError(result.getMessage());
            System.out.println();

            ListInterface<Room> available = result.getAvailableRooms();
            printRoomTable(available);

            System.out.print("Select room [1-" + available.getNumberOfEntries()
                    + "] (0 to cancel): ");
            int sel = readIntRaw();

            if (sel < 1 || sel > available.getNumberOfEntries()) {
                System.out.println("Walk-in booking cancelled.");
                return;
            }

            Room chosen = available.getEntry(sel);
            String error = controller.completeWalkInManual(
                    result.getDraftBooking(), chosen.getRoomNumber());

            if (error == null) {
                BookingRequest booking = controller.getLatestBooking();
                System.out.println();
                UIUtils.printSectionLine();
                if (booking != null) printBookingDetail(booking);
                System.out.println("Walk-in booking added with manually selected room.");
            } else {
                UIUtils.printError(error);
            }

        } else {
            // Error
            UIUtils.printError(result.getErrorMessage());
        }
    }

    private void handleAddStandardBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD STANDARD BOOKING");
        printGuestTable(controller.getAllGuests());

        System.out.print("Enter Guest Confirmation No: ");
        String confirmationNo = scanner.nextLine().trim();
        if (controller.getGuest(confirmationNo) == null) {
            UIUtils.printError("Guest not found. Add guest first.");
            return;
        }

        String roomType = promptRoomType();
        if (roomType == null) {
            System.out.println("\nStandard booking cancelled.");
            return;
        }
        String checkInDate = promptCheckInDate(
                "Enter Check-In Date (YYYY-MM-DD, or 0 to cancel): ");
        if (checkInDate == null) {
            System.out.println("\nStandard booking cancelled.");
            return;
        }
        String checkOutDate = promptCheckOutDate(LocalDate.parse(checkInDate),
                "Enter Check-Out Date (YYYY-MM-DD, or 0 to cancel): ");
        if (checkOutDate == null) {
            System.out.println("\nStandard booking cancelled.");
            return;
        }

        String error = controller.addStandardBooking(
                confirmationNo, roomType, checkInDate, checkOutDate);
        if (error == null) {
            BookingRequest booking = controller.getLatestBooking();
            System.out.println();
            if (booking != null) {
                UIUtils.printSectionLine();
                printBookingDetail(booking);
            }
            System.out.println("Standard booking added to pending queue.");
            System.out.println("Pending Queue Size: " + controller.getPendingQueueSize());
        } else {
            UIUtils.printError(error);
        }
    }

    private void handleViewBookings() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CURRENT BOOKING RECORDS");
        ListInterface<BookingRequest> bookings = controller.getCurrentBookings();
        displayBookingTable(bookings);
    }

    private void handleViewPendingQueue() {
        UIUtils.clearScreen();
        UIUtils.printHeader("PENDING STANDARD QUEUE");
        ListInterface<BookingRequest> queue = controller.getPendingStandardQueue();
        displayPendingQueueTable(queue);
    }

    /**
     * Handles auto-assign using the new AssignResult pattern.
     *
     * Flow:
     *   1. Call controller.autoAssignNextStandardBooking()
     *   2. If success      → show assigned booking (+ optional VIP result)
     *   3. If manualNeeded → show available room list, prompt selection,
     *                        call controller.assignRoomToBookingForGuest()
     *   4. If emptyQueue   → "No pending bookings"
     *   5. If noRooms      → "No rooms available"
     */
    private void handleAutoAssign() {
        UIUtils.clearScreen();
        UIUtils.printHeader("AUTO ASSIGN ROOM");

        AssignResult result = controller.autoAssignNextStandardBooking();

        // Exactly ONE guest is allocated per click.
        // VIP gets the click first whenever a Pending VIP booking exists.
        if (result.isVIPAllocated()) {
            control.VIPRoomAllocationController.AllocationResult vip = result.getVIPResult();
            Guest guest = vip.getGuest();
            Room room = vip.getRoom();

            System.out.println("VIP booking allocated first:");
            UIUtils.printSectionLine();
            System.out.println("Guest Name : " + guest.getName());
            System.out.println("Tier       : " + guest.getLoyaltyTier());
            System.out.println("Room       : " + room.getRoomNumber()
                    + " (" + room.getRoomType() + ")");
            UIUtils.printSectionLine();
            System.out.println("One VIP booking assigned successfully.");
            System.out.println("Press Auto Assign again to allocate the next guest.");
            return;
        }

        // Requested VIP room type is unavailable, but other date-safe rooms exist.
        // Keep serving this SAME highest-priority VIP instead of skipping the queue.
        if (result.isVIPManualNeeded()) {
            control.VIPRoomAllocationController.AllocationResult vip = result.getVIPResult();
            Guest guest = vip.getGuest();

            System.out.println();
            UIUtils.printError(result.getMessage());
            System.out.println("Guest Name         : " + guest.getName());
            System.out.println("Loyalty Tier       : " + guest.getLoyaltyTier());
            System.out.println("Requested Room Type: "
                    + controller.getVIPRequestedRoomType(guest.getConfirmationNo()));
            System.out.println();
            System.out.println("Other available rooms for this guest's booking dates:");
            UIUtils.printSectionLine();

            ListInterface<Room> alternatives = result.getAvailableRooms();
            printRoomTable(alternatives);

            System.out.print("Select room [1-" + alternatives.getNumberOfEntries()
                    + "] (0 to keep booking Pending): ");
            int sel = readIntRaw();

            if (sel == 0) {
                System.out.println("Booking remains Pending. No Standard guest was allocated.");
                return;
            }

            if (sel < 1 || sel > alternatives.getNumberOfEntries()) {
                UIUtils.printError("Invalid selection. Booking remains Pending.");
                return;
            }

            Room chosen = alternatives.getEntry(sel);
            control.VIPRoomAllocationController.AllocationResult manual =
                    controller.assignAlternativeRoomToVip(
                            guest.getConfirmationNo(), chosen.getRoomNumber());

            if (manual != null && manual.isSuccess()) {
                System.out.println();
                System.out.println("VIP alternative room assigned successfully:");
                UIUtils.printSectionLine();
                System.out.println("Guest Name : " + guest.getName());
                System.out.println("Tier       : " + guest.getLoyaltyTier());
                System.out.println("Room       : " + chosen.getRoomNumber()
                        + " (" + chosen.getRoomType() + ")");
                UIUtils.printSectionLine();
                System.out.println("One VIP booking assigned successfully.");
            } else {
                UIUtils.printError(manual == null
                        ? "VIP room assignment could not be completed."
                        : manual.getMessage());
            }
            return;
        }

        // A Pending VIP exists but cannot currently be allocated.
        // Do not skip it and allocate a Standard booking in the same click.
        if (result.isVIPBlocked()) {
            UIUtils.printError(result.getMessage());
            System.out.println("Standard booking was not allocated because VIP bookings have priority.");
            return;
        }

        // No Pending VIP remains, so this click may allocate ONE Standard booking.
        if (result.isSuccess()) {
            printBookingDetail(result.getBooking());
            System.out.println("Booking assigned successfully.");

        } else if (result.isManualNeeded()) {
            System.out.println();
            UIUtils.printError(result.getMessage());
            System.out.println();

            ListInterface<Room> available = result.getAvailableRooms();
            printRoomTable(available);

            System.out.print("Select room [1-" + available.getNumberOfEntries()
                    + "] (0 to cancel): ");
            int sel = readIntRaw();

            if (sel < 1 || sel > available.getNumberOfEntries()) {
                System.out.println("Assignment cancelled.");
                return;
            }

            Room chosen = available.getEntry(sel);
            String error = controller.assignRoomToBookingForGuest(
                    result.getBooking().getConfirmationNo(),
                    chosen.getRoomNumber());

            if (error == null) {
                System.out.println("Room " + chosen.getRoomNumber()
                        + " manually assigned successfully.");
            } else {
                UIUtils.printError(error);
            }

        } else if (result.isEmptyQueue()) {
            System.out.println("No pending VIP or standard booking found.");

        } else if (result.isNoRooms()) {
            UIUtils.printError(result.getMessage());
        }
    }

    private void handleReports() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("BOOKING REPORTS");
            System.out.println(" [1] Report 1: Booking Report");
            System.out.println(" [2] Report 2: Room Type Demand Report");
            System.out.println(" [0] Back to Booking Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-2): ");

            Integer selected = readIntOption(0, 2);
            if (selected == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 2.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            choice = selected;

            switch (choice) {
                case 1:
                    handleBookingReport();
                    UIUtils.pressEnterToContinue(scanner);
                    break;
                case 2:
                    handleRoomTypeDemandReport();
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

    private void handleBookingReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: BOOKING REPORT");

        String bookingTypeFilter = promptBookingTypeFilter();
        if (bookingTypeFilter == null) { System.out.println("\nReport cancelled."); return; }
        String roomTypeFilter = promptRoomTypeFilter();
        if (roomTypeFilter == null) { System.out.println("\nReport cancelled."); return; }
        String statusFilter = promptStatusFilter();
        if (statusFilter == null) { System.out.println("\nReport cancelled."); return; }

        ListInterface<BookingRequest> reportBookings =
                controller.generateBookingReport(bookingTypeFilter, roomTypeFilter, statusFilter);

        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: BOOKING REPORT");
        System.out.println("Booking Type Filter : " + formatFilter(bookingTypeFilter));
        System.out.println("Room Type Filter    : " + formatFilter(roomTypeFilter));
        System.out.println("Status Filter       : " + formatFilter(statusFilter));
        UIUtils.printSectionLine();
        if (statusFilter.equals(BookingController.FILTER_ALL)) {
            System.out.println("Pending      : " + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_PENDING));
            System.out.println("Assigned     : " + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_ASSIGNED));
            System.out.println("Checked In   : " + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CHECKED_IN));
            System.out.println("Checked Out  : " + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CHECKED_OUT));
            System.out.println("Cancelled    : " + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CANCELLED));
            System.out.println("Total Matched: " + reportBookings.getNumberOfEntries());
            UIUtils.printSectionLine();
        } else {
            System.out.println("Matched Bookings: " + reportBookings.getNumberOfEntries());
            UIUtils.printSectionLine();
        }
        displayBookingTable(reportBookings, false);
    }

    private void handleRoomTypeDemandReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 2: ROOM TYPE DEMAND REPORT");

        String bookingTypeFilter = promptBookingTypeFilter();
        if (bookingTypeFilter == null) { System.out.println("\nReport cancelled."); return; }

        ListInterface<BookingController.RoomTypeDemandRow> rows =
                controller.generateRoomTypeDemandReport(bookingTypeFilter);
        int totalRequests = controller.getTotalDemandRequests(rows);

        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 2: ROOM TYPE DEMAND REPORT");
        System.out.println("Booking Type Filter : " + formatFilter(bookingTypeFilter));
        UIUtils.printSectionLine();
        if (totalRequests == 0) {
            System.out.println("No booking requests found.");
            return;
        }

        System.out.printf("%-10s | %-8s | %-10s%n", "Room Type", "Requests", "Percentage");
        UIUtils.printSectionLine();
        for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
            BookingController.RoomTypeDemandRow row = rows.getEntry(i);
            double percentage = (row.getRequests() * 100.0) / totalRequests;
            System.out.printf("%-10s | %-8d | %9.2f%%%n",
                    row.getRoomType(), row.getRequests(), percentage);
        }
        UIUtils.printSectionLine();
        System.out.println("Most Requested Room Type: " + rows.getEntry(1).getRoomType());
        System.out.println("Total Booking Requests  : " + totalRequests);
    }

    private void handleCheckInBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CHECK IN BOOKING");
        displayBookingTable(controller.getBookingsByStatus(BookingController.STATUS_ASSIGNED));

        System.out.print("Enter Booking ID to check in: ");
        String bookingId = scanner.nextLine().trim();
        String error = controller.checkInBooking(bookingId);
        if (error == null) {
            System.out.println("\nGuest checked in successfully. Room is now occupied.");
        } else {
            UIUtils.printError(error);
        }
    }

    private void handleCheckOutBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CHECK OUT BOOKING");

        ListInterface<BookingRequest> checkedInBookings =
                controller.getSortedCheckedInBookingsForCheckout();
        if (checkedInBookings.isEmpty()) {
            System.out.println("No checked-in bookings are available for checkout.");
            return;
        }

        displayCheckoutTable(checkedInBookings);

        System.out.print("Enter Booking ID to check out: ");
        String bookingId = scanner.nextLine().trim();
        String error = controller.checkOutBooking(bookingId);
        if (error == null) {
            System.out.println("\nGuest checked out successfully.");
            System.out.println(
                    "Room is now vacant and dirty, so it appears in housekeeping tasks.");
        } else {
            UIUtils.printError(error);
        }
    }

    private void displayCheckoutTable(ListInterface<BookingRequest> bookings) {
        System.out.printf("%-7s | %-14s | %-6s | %-10s | %-10s%n",
                "ID", "Guest", "Room", "Check-Out", "Due Status");
        UIUtils.printSectionLine();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            System.out.printf("%-7s | %-14s | %-6s | %-10s | %-10s%n",
                    booking.getBookingId(),
                    booking.getGuest().getName(),
                    booking.getAssignedRoomNumber(),
                    booking.getCheckOutDate(),
                    controller.getCheckoutDueLabel(booking));
        }
        UIUtils.printSectionLine();
        System.out.println("Total Checked-In Bookings: " + bookings.getNumberOfEntries());
        System.out.println();
    }

    private void handleCancelBooking() {
        UIUtils.clearScreen();
        UIUtils.printHeader("CANCEL BOOKING");
        displayBookingTable(controller.getCancellableBookings());

        System.out.print("Enter Booking ID to cancel: ");
        String bookingId = scanner.nextLine().trim();
        String error = controller.cancelBooking(bookingId);
        if (error == null) {
            System.out.println("\nBooking cancelled successfully.");
        } else {
            UIUtils.printError(error);
        }
    }

    // -------------------------------------------------------
    // Room table helper for manual selection
    // -------------------------------------------------------

    private void printRoomTable(ListInterface<Room> rooms) {
        System.out.printf("%-4s %-8s %-14s %s%n", "#", "Room", "Type", "Status");
        UIUtils.printSectionLine();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            System.out.printf("%-4d %-8s %-14s %s%n",
                    i, r.getRoomNumber(), r.getRoomType(), r.getCleanlinessStatus());
        }
        UIUtils.printSectionLine();
        System.out.println();
    }

    // -------------------------------------------------------
    // Prompt helpers
    // -------------------------------------------------------

    private String promptRoomType() {
        while (true) {
            System.out.println("\nSelect Room Type:");
            System.out.printf(" [1] Standard (RM %.2f per night)%n", controller.getRoomRate("Standard"));
            System.out.printf(" [2] Deluxe   (RM %.2f per night)%n", controller.getRoomRate("Deluxe"));
            System.out.printf(" [3] Suite    (RM %.2f per night)%n", controller.getRoomRate("Suite"));
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

    private String promptStatusFilter() {
        while (true) {
            System.out.println("Filter by booking status:");
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
                case 1: return BookingController.FILTER_ALL;
                case 2: return BookingController.STATUS_PENDING;
                case 3: return BookingController.STATUS_ASSIGNED;
                case 4: return BookingController.STATUS_CHECKED_IN;
                case 5: return BookingController.STATUS_CHECKED_OUT;
                case 6: return BookingController.STATUS_CANCELLED;
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
                case 1: return BookingController.FILTER_ALL;
                case 2: return "Standard";
                case 3: return "Deluxe";
                case 4: return "Suite";
            }
        }
    }

    private String promptBookingTypeFilter() {
        while (true) {
            System.out.println("Filter by booking type:");
            System.out.println(" [1] All  [2] Walk-In  [3] Standard  [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-3): ");

            Integer choice = readIntOption(0, 3);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 3.");
                continue;
            }
            switch (choice) {
                case 0: return null;
                case 1: return BookingController.FILTER_ALL;
                case 2: return BookingController.TYPE_WALK_IN;
                case 3: return BookingController.TYPE_STANDARD;
            }
        }
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

    // -------------------------------------------------------
    // Table display helpers
    // -------------------------------------------------------

    private void printGuestTable(ListInterface<Guest> guests) {
        if (guests.isEmpty()) {
            System.out.println("No guest records found. Add guest first.");
            return;
        }
        System.out.printf("%-14s | %-18s | %-14s%n", "Confirmation", "Name", "Phone");
        UIUtils.printSectionLine();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            Guest guest = guests.getEntry(i);
            System.out.printf("%-14s | %-18s | %-14s%n",
                    guest.getConfirmationNo(), guest.getName(), guest.getPhone());
        }
        UIUtils.printSectionLine();
    }

    private void displayBookingTable(ListInterface<BookingRequest> bookings) {
        displayBookingTable(bookings, true);
    }

    private void displayBookingTable(ListInterface<BookingRequest> bookings,
            boolean showTotal) {
        if (bookings.isEmpty()) {
            System.out.println("No booking records found.");
            return;
        }
        System.out.printf("%-7s | %-8s | %-14s | %-10s | %-10s | %-10s | %-11s | %-6s%n",
                "ID", "Type", "Guest", "Room Type",
                "Check-In", "Check-Out", "Status", "Room");
        UIUtils.printSectionLine();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            System.out.printf("%-7s | %-8s | %-14s | %-10s | %-10s | %-10s | %-11s | %-6s%n",
                    booking.getBookingId(),
                    booking.getBookingType(),
                    booking.getGuest().getName(),
                    booking.getRequestedRoomType(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getStatus(),
                    booking.getAssignedRoomNumber());
        }
        UIUtils.printSectionLine();
        if (showTotal) {
            System.out.println("Total Bookings: " + bookings.getNumberOfEntries());
        }
    }

    private void displayPendingQueueTable(ListInterface<BookingRequest> queue) {
        if (queue.isEmpty()) {
            System.out.println("No pending standard booking found.");
            return;
        }

        System.out.printf("%-4s | %-7s | %-14s | %-10s | %-10s | %-10s%n",
                "No.", "ID", "Guest", "Room Type", "Check-In", "Check-Out");
        UIUtils.printSectionLine();
        for (int i = 1; i <= queue.getNumberOfEntries(); i++) {
            BookingRequest booking = queue.getEntry(i);
            System.out.printf("%-4d | %-7s | %-14s | %-10s | %-10s | %-10s%n",
                    i,
                    booking.getBookingId(),
                    booking.getGuest().getName(),
                    booking.getRequestedRoomType(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate());
        }
        UIUtils.printSectionLine();
        System.out.println("Pending Queue Size: " + queue.getNumberOfEntries());
    }

    private void printBookingDetail(BookingRequest booking) {
        System.out.println("Booking ID      : " + booking.getBookingId());
        System.out.println("Guest Name      : " + booking.getGuest().getName());
        System.out.println("Booking Type    : " + booking.getBookingType());
        System.out.println("Room Type       : " + booking.getRequestedRoomType());
        System.out.println("Check-In Date   : " + booking.getCheckInDate());
        System.out.println("Check-Out Date  : " + booking.getCheckOutDate());
        System.out.println("Status          : " + booking.getStatus());
        System.out.println("Assigned Room   : " + booking.getAssignedRoomNumber());
        UIUtils.printSectionLine();
    }

    private String formatFilter(String value) {
        return value.equals(BookingController.FILTER_ALL) ? "ALL" : value;
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

    /** Reads a raw integer from nextLine — used for numbered room selection. */
    private int readIntRaw() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
