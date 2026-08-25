package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.BookingRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles booking file operations for bookings.txt.
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
                    bookings.add(new BookingRequest(parts[0], parts[1], parts[2], parts[3], parts[4],
                            parts[5], parts[6], parts[7], parts[8]));
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
}
