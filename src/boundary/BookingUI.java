package boundary;

import adt.ListInterface;
import control.BookingController;
import entity.BookingRequest;
import entity.Guest;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * @author Chang Han Yean
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
            System.out.println(" [5] Auto Assign Standard Booking");
            System.out.println(" [6] Check In Booking");
            System.out.println(" [7] Check Out Booking");
            System.out.println(" [8] Cancel Booking");
            System.out.println(" [9] Generate Reports");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-9): ");

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
                    handleAddGuest();
                    break;
                case 2:
                    handleAddWalkIn();
                    break;
                case 3:
                    handleAddStandardBooking();
                    break;
                case 4:
                    handleViewBookings();
                    break;
                case 5:
                    handleAutoAssign();
                    break;
                case 6:
                    handleCheckInBooking();
                    break;
                case 7:
                    handleCheckOutBooking();
                    break;
                case 8:
                    handleCancelBooking();
                    break;
                case 9:
                    handleReports();
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
            UIUtils.printError("Guest name and phone cannot be empty.");
        }
    }

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

        String error = controller.addWalkInRegistration(confirmationNo, roomType, checkOutDate);
        if (error == null) {
            BookingRequest booking = controller.getLatestBooking();
            System.out.println();
            if (booking != null) {
                UIUtils.printSectionLine();
                printBookingDetail(booking);
            }
            System.out.println("Walk-in booking added and room assigned successfully.");
        } else {
            UIUtils.printError(error);
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
        String checkInDate = promptCheckInDate("Enter Check-In Date (YYYY-MM-DD, or 0 to cancel): ");
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

        String error = controller.addStandardBooking(confirmationNo, roomType, checkInDate, checkOutDate);
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

    private void handleAutoAssign() {
        UIUtils.clearScreen();
        UIUtils.printHeader("AUTO ASSIGN STANDARD BOOKING");

        BookingRequest booking = controller.autoAssignNextStandardBooking();
        if (booking == null) {
            System.out.println("No pending standard booking found.");
            return;
        }

        if (booking.getStatus().equals(BookingController.STATUS_ASSIGNED)) {
            printBookingDetail(booking);
            System.out.println("Booking assigned successfully.");
        } else {
            printBookingDetail(booking);
            UIUtils.printError("The first pending booking cannot be assigned yet.");
            UIUtils.printError("Reason: No ready and vacant room of requested type is available for the selected date.");
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
        if (bookingTypeFilter == null) {
            System.out.println("\nReport cancelled.");
            return;
        }
        String roomTypeFilter = promptRoomTypeFilter();
        if (roomTypeFilter == null) {
            System.out.println("\nReport cancelled.");
            return;
        }
        String statusFilter = promptStatusFilter();
        if (statusFilter == null) {
            System.out.println("\nReport cancelled.");
            return;
        }

        ListInterface<BookingRequest> reportBookings = controller.generateBookingReport(
                bookingTypeFilter, roomTypeFilter, statusFilter);

        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: BOOKING REPORT");
        System.out.println("Booking Type Filter : " + formatFilter(bookingTypeFilter));
        System.out.println("Room Type Filter    : " + formatFilter(roomTypeFilter));
        System.out.println("Status Filter       : " + formatFilter(statusFilter));
        UIUtils.printSectionLine();
        if (statusFilter.equals(BookingController.FILTER_ALL)) {
            System.out.println("Pending      : "
                    + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_PENDING));
            System.out.println("Assigned     : "
                    + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_ASSIGNED));
            System.out.println("Checked In   : "
                    + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CHECKED_IN));
            System.out.println("Checked Out  : "
                    + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CHECKED_OUT));
            System.out.println("Cancelled    : "
                    + controller.countBookingsByStatus(reportBookings, BookingController.STATUS_CANCELLED));
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
        if (bookingTypeFilter == null) {
            System.out.println("\nReport cancelled.");
            return;
        }

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
            System.out.println("Room is now vacant and dirty, so it appears in housekeeping tasks.");
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
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return "Standard";
            }
            if (choice == 2) {
                return "Deluxe";
            }
            return "Suite";
        }
    }

    private String promptStatusFilter() {
        while (true) {
            System.out.println("Filter by booking status:");
            System.out.println(" [1] All Bookings");
            System.out.println(" [2] Pending");
            System.out.println(" [3] Assigned");
            System.out.println(" [4] Checked In");
            System.out.println(" [5] Checked Out");
            System.out.println(" [6] Cancelled");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-6): ");

            Integer choice = readIntOption(0, 6);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 6.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return BookingController.FILTER_ALL;
            }
            if (choice == 2) {
                return BookingController.STATUS_PENDING;
            }
            if (choice == 3) {
                return BookingController.STATUS_ASSIGNED;
            }
            if (choice == 4) {
                return BookingController.STATUS_CHECKED_IN;
            }
            if (choice == 5) {
                return BookingController.STATUS_CHECKED_OUT;
            }
            return BookingController.STATUS_CANCELLED;
        }
    }

    private String promptRoomTypeFilter() {
        while (true) {
            System.out.println("\nFilter by room type:");
            System.out.println(" [1] All Room Types");
            System.out.println(" [2] Standard");
            System.out.println(" [3] Deluxe");
            System.out.println(" [4] Suite");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");

            Integer choice = readIntOption(0, 4);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 4.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return BookingController.FILTER_ALL;
            }
            if (choice == 2) {
                return "Standard";
            }
            if (choice == 3) {
                return "Deluxe";
            }
            return "Suite";
        }
    }

    private String promptBookingTypeFilter() {
        while (true) {
            System.out.println("Filter by booking type:");
            System.out.println(" [1] All Booking Types");
            System.out.println(" [2] Walk-In");
            System.out.println(" [3] Standard");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-3): ");

            Integer choice = readIntOption(0, 3);
            if (choice == null) {
                UIUtils.printError("Invalid choice! Please enter a number between 0 and 3.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return BookingController.FILTER_ALL;
            }
            if (choice == 2) {
                return BookingController.TYPE_WALK_IN;
            }
            return BookingController.TYPE_STANDARD;
        }
    }

    private String promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
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
            if (input == null) {
                return null;
            }
            LocalDate checkInDate = LocalDate.parse(input);
            if (!checkInDate.isBefore(LocalDate.now())) {
                return input;
            }
            UIUtils.printError("Check-in date cannot be before today.");
        }
    }

    private String promptCheckOutDate(LocalDate checkInDate, String prompt) {
        while (true) {
            String input = promptDate(prompt);
            if (input == null) {
                return null;
            }
            LocalDate checkOutDate = LocalDate.parse(input);
            if (checkOutDate.isAfter(checkInDate)) {
                return input;
            }
            UIUtils.printError("Check-out date must be after check-in date.");
        }
    }

    private void printGuestTable(ListInterface<Guest> guests) {
        if (guests.isEmpty()) {
            System.out.println("No guest records found. Add guest first.");
            return;
        }

        System.out.printf("%-14s | %-18s | %-14s%n",
                "Confirmation", "Name", "Phone");
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

    private void displayBookingTable(ListInterface<BookingRequest> bookings, boolean showTotal) {
        if (bookings.isEmpty()) {
            System.out.println("No booking records found.");
            return;
        }

        System.out.printf("%-7s | %-8s | %-14s | %-10s | %-10s | %-10s | %-11s | %-6s%n",
                "ID", "Type", "Guest", "Room Type", "Check-In", "Check-Out", "Status", "Room");
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
        if (value.equals(BookingController.FILTER_ALL)) {
            return "ALL";
        }
        return value;
    }

    private Integer readIntOption(int min, int max) {
        if (scanner.hasNextInt()) {
            int value = scanner.nextInt();
            scanner.nextLine();
            if (value >= min && value <= max) {
                return value;
            }
            return null;
        }
        scanner.nextLine();
        return null;
    }
}
