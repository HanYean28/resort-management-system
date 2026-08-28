package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.BookingRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import utility.DateUtils;

/**
 * Handles booking file operations for bookings.txt.
 * @author Elwin Goh Yao Zu
 */
public class BookingDAO {
    private static final String DATA_FILE = "bookings.txt";

    public ListInterface<BookingRequest> loadBookings() {
        ListInterface<BookingRequest> bookings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 9) {
                    String bookingId = parts[0].trim();
                    String confirmationNo = parts[1].trim();
                    String bookingType = parts[2].trim();
                    String roomType = parts[3].trim();
                    String checkInDate = parts[4].trim();
                    String checkOutDate = parts[5].trim();
                    String status = parts[6].trim();
                    String assignedRoomNumber = parts[7].trim();
                    String createdAt = parts[8].trim();

                    if (isValidBooking(bookingId, confirmationNo, bookingType, roomType,
                            checkInDate, checkOutDate, status, assignedRoomNumber, createdAt)
                            && !bookingIdExists(bookings, bookingId)) {
                        bookings.add(new BookingRequest(bookingId, confirmationNo, bookingType, roomType,
                                checkInDate, checkOutDate, status, assignedRoomNumber, createdAt));
                    }
                }
            }
        } catch (IOException e) {
            createFileIfMissing();
        }
        return bookings;
    }

    public void saveBookings(ListInterface<BookingRequest> bookings) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bw.write("# bookingId|confirmationNo|bookingType|requestedRoomType|checkInDate|checkOutDate|status|assignedRoomNumber|createdAt");
            bw.newLine();
            for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
                BookingRequest booking = bookings.getEntry(i);
                bw.write(booking.getBookingId() + "|" + booking.getConfirmationNo() + "|"
                        + booking.getBookingType() + "|" + booking.getRequestedRoomType() + "|"
                        + booking.getCheckInDate() + "|" + booking.getCheckOutDate() + "|"
                        + booking.getStatus() + "|" + booking.getAssignedRoomNumber() + "|"
                        + booking.getCreatedAt());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    private void createFileIfMissing() {
        saveBookings(new ArrayList<BookingRequest>());
    }

    private boolean isValidBooking(String bookingId, String confirmationNo, String bookingType, String roomType,
            String checkInDate, String checkOutDate, String status, String assignedRoomNumber, String createdAt) {
        try {
            DateUtils.parseDate(checkInDate);
            DateUtils.parseDate(checkOutDate);
        } catch (Exception e) {
            return false;
        }

        return bookingId.matches("B\\d{4}")
                && confirmationNo.matches("\\d{8}")
                && isValidBookingType(bookingType)
                && isValidRoomType(roomType)
                && DateUtils.isAfter(checkOutDate, checkInDate)
                && isValidStatus(status)
                && !assignedRoomNumber.isEmpty()
                && !createdAt.isEmpty();
    }

    private boolean bookingIdExists(ListInterface<BookingRequest> bookings, String bookingId) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            if (bookings.getEntry(i).getBookingId().equalsIgnoreCase(bookingId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidBookingType(String bookingType) {
        return bookingType.equalsIgnoreCase("Walk-In")
                || bookingType.equalsIgnoreCase("Standard")
                || bookingType.equalsIgnoreCase("VIP");
    }

    private boolean isValidRoomType(String roomType) {
        return roomType.equalsIgnoreCase("Standard")
                || roomType.equalsIgnoreCase("Deluxe")
                || roomType.equalsIgnoreCase("Suite");
    }

    private boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("Pending")
                || status.equalsIgnoreCase("Assigned")
                || status.equalsIgnoreCase("Checked In")
                || status.equalsIgnoreCase("Checked Out")
                || status.equalsIgnoreCase("Cancelled");
    }
}
