package control;

import adt.ArrayList;
import adt.ArrayQueue;
import adt.ListInterface;
import adt.QueueInterface;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @author Chang Han Yean
 */
public class BookingController {
    private static final String BOOKINGS_FILE = "bookings.txt";
    private static final String ROOMS_FILE = "rooms.txt";
    private static final String BILLING_FILE = "billing.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String FILTER_ALL = "ALL";
    public static final String TYPE_WALK_IN = "Walk-In";
    public static final String TYPE_STANDARD = "Standard";
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_CHECKED_IN = "Checked In";
    public static final String STATUS_CHECKED_OUT = "Checked Out";
    public static final String STATUS_CANCELLED = "Cancelled";

    private ListInterface<BookingRequest> bookings;
    private QueueInterface<BookingRequest> pendingQueue;
    private FrontDeskService frontDeskService;

    public BookingController() {
        bookings = new ArrayList<>();
        pendingQueue = new ArrayQueue<>();
        frontDeskService = new FrontDeskService();
        loadBookingsFromFile();
        rebuildPendingQueue();
    }

    public Guest addGuest(String name, String phone) {
        if (name.isEmpty() || phone.isEmpty()) {
            return null;
        }

        String confirmationNo = generateConfirmationNo();
        frontDeskService.addGuest(new Guest(confirmationNo, name, phone, "NONE", 0.0, "N/A"));
        return frontDeskService.searchByConfirmationNumber(confirmationNo);
    }

    public Guest getGuest(String confirmationNo) {
        return frontDeskService.searchByConfirmationNumber(confirmationNo);
    }

    public ListInterface<Guest> getAllGuests() {
        return frontDeskService.getAllGuestsSorted();
    }

    public String addStandardBooking(String confirmationNo, String requestedRoomType,
            String checkInDate, String checkOutDate) {
        Guest guest = getGuest(confirmationNo);
        if (guest == null) {
            return "Guest not found. Add guest first.";
        }

        String validation = validateBookingInput(requestedRoomType, checkInDate, checkOutDate);
        if (validation != null) {
            return validation;
        }

        BookingRequest booking = new BookingRequest(generateBookingId(), guest, TYPE_STANDARD,
                requestedRoomType, checkInDate, checkOutDate, STATUS_PENDING, "N/A", getCurrentTimestamp());
        bookings.add(booking);
        pendingQueue.enqueue(booking);
        saveBookingsToFile();
        return null;
    }

    public String addWalkInRegistration(String confirmationNo, String requestedRoomType,
            String checkOutDate) {
        Guest guest = getGuest(confirmationNo);
        if (guest == null) {
            return "Guest not found. Add guest first.";
        }

        String checkInDate = LocalDate.now().toString();
        String validation = validateBookingInput(requestedRoomType, checkInDate, checkOutDate);
        if (validation != null) {
            return validation;
        }

        BookingRequest booking = new BookingRequest(generateBookingId(), guest, TYPE_WALK_IN,
                requestedRoomType, checkInDate, checkOutDate, STATUS_PENDING, "N/A", getCurrentTimestamp());
        String roomNumber = findAvailableRoom(booking);
        if (roomNumber == null) {
            return "No ready " + requestedRoomType + " room is available for this date.";
        }

        booking.setAssignedRoomNumber(roomNumber);
        booking.setStatus(STATUS_CHECKED_IN);
        frontDeskService.updateGuestRoom(guest.getConfirmationNo(), roomNumber);
        bookings.add(booking);
        updateRoomOccupancy(roomNumber, "Occupied");
        generatePaidBill(booking);
        saveBookingsToFile();
        return null;
    }

    public BookingRequest autoAssignNextStandardBooking() {
        BookingRequest nextBooking = pendingQueue.getFront();
        if (nextBooking == null) {
            return null;
        }

        String roomNumber = findAvailableRoom(nextBooking);
        if (roomNumber == null) {
            return nextBooking;
        }

        nextBooking.setAssignedRoomNumber(roomNumber);
        nextBooking.setStatus(STATUS_ASSIGNED);
        pendingQueue.dequeue();
        saveBookingsToFile();
        return nextBooking;
    }

    public String checkInBooking(String bookingId) {
        BookingRequest booking = getBooking(bookingId);
        if (booking == null) {
            return "Booking not found.";
        }
        if (!booking.getStatus().equals(STATUS_ASSIGNED)) {
            return "Only assigned standard bookings can be checked in.";
        }

        LocalDate today = LocalDate.now();
        LocalDate checkIn = LocalDate.parse(booking.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(booking.getCheckOutDate());
        if (today.isBefore(checkIn) || !today.isBefore(checkOut)) {
            return "Guest can only check in from check-in date until before check-out date.";
        }

        if (!isRoomVacant(booking.getAssignedRoomNumber())) {
            return "Assigned room is currently occupied.";
        }

        booking.setStatus(STATUS_CHECKED_IN);
        frontDeskService.updateGuestRoom(booking.getGuest().getConfirmationNo(), booking.getAssignedRoomNumber());
        updateRoomOccupancy(booking.getAssignedRoomNumber(), "Occupied");
        generatePaidBill(booking);
        saveBookingsToFile();
        return null;
    }

    public String checkOutBooking(String bookingId) {
        BookingRequest booking = getBooking(bookingId);
        if (booking == null) {
            return "Booking not found.";
        }
        if (!booking.getStatus().equals(STATUS_CHECKED_IN)) {
            return "Only checked-in bookings can be checked out.";
        }

        booking.setStatus(STATUS_CHECKED_OUT);
        frontDeskService.updateGuestRoom(booking.getGuest().getConfirmationNo(), "N/A");
        markRoomDirtyAfterCheckout(booking.getAssignedRoomNumber());
        saveBookingsToFile();
        return null;
    }

    public String cancelBooking(String bookingId) {
        BookingRequest booking = getBooking(bookingId);
        if (booking == null) {
            return "Booking not found.";
        }
        if (booking.getStatus().equals(STATUS_CANCELLED)) {
            return "Booking is already cancelled.";
        }

        booking.setStatus(STATUS_CANCELLED);
        if (booking.getGuest().getRoomNo() != null
                && booking.getGuest().getRoomNo().equalsIgnoreCase(booking.getAssignedRoomNumber())) {
            updateRoomOccupancy(booking.getAssignedRoomNumber(), "Vacant");
            frontDeskService.updateGuestRoom(booking.getGuest().getConfirmationNo(), "N/A");
        }
        rebuildPendingQueue();
        saveBookingsToFile();
        return null;
    }

    public BookingRequest getBooking(String bookingId) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getBookingId().equalsIgnoreCase(bookingId)) {
                return booking;
            }
        }
        return null;
    }

    public ListInterface<BookingRequest> getBookingsByStatus(String statusFilter) {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (statusFilter.equals(FILTER_ALL) || booking.getStatus().equalsIgnoreCase(statusFilter)) {
                results.add(booking);
            }
        }
        return results;
    }

    public ListInterface<BookingRequest> getCurrentBookings() {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getStatus().equals(STATUS_PENDING)
                    || booking.getStatus().equals(STATUS_ASSIGNED)
                    || booking.getStatus().equals(STATUS_CHECKED_IN)) {
                results.add(booking);
            }
        }
        return results;
    }

    public int getPendingQueueSize() {
        return pendingQueue.size();
    }

    private void loadBookingsFromFile() {
        bookings.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 9) {
                    Guest guest = frontDeskService.searchByConfirmationNumber(parts[1]);
                    if (guest != null) {
                        String status = normalizeLoadedStatus(parts[2], parts[6]);
                        bookings.add(new BookingRequest(parts[0], guest, parts[2], parts[3], parts[4],
                                parts[5], status, parts[7], parts[8]));
                    }
                }
            }
        } catch (IOException e) {
            saveBookingsToFile();
        }
    }

    private void saveBookingsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE))) {
            bw.write("# bookingId|confirmationNo|bookingType|requestedRoomType|checkInDate|checkOutDate|status|assignedRoomNumber|createdAt");
            bw.newLine();
            for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
                BookingRequest booking = bookings.getEntry(i);
                bw.write(booking.getBookingId() + "|" + booking.getGuest().getConfirmationNo() + "|"
                        + booking.getBookingType() + "|" + booking.getRequestedRoomType() + "|"
                        + booking.getCheckInDate() + "|" + booking.getCheckOutDate() + "|"
                        + booking.getStatus() + "|" + booking.getAssignedRoomNumber() + "|"
                        + booking.getCreatedAt());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    private void rebuildPendingQueue() {
        pendingQueue.clear();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getBookingType().equals(TYPE_STANDARD) && booking.getStatus().equals(STATUS_PENDING)) {
                pendingQueue.enqueue(booking);
            }
        }
    }

    private String validateBookingInput(String requestedRoomType, String checkInDate, String checkOutDate) {
        if (!isValidRoomType(requestedRoomType)) {
            return "Room type must be Standard, Deluxe or Suite.";
        }

        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (!checkOut.isAfter(checkIn)) {
                return "Check-out date must be after check-in date.";
            }
        } catch (Exception e) {
            return "Date format must be YYYY-MM-DD.";
        }
        return null;
    }

    private String findAvailableRoom(BookingRequest targetBooking) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomType().equalsIgnoreCase(targetBooking.getRequestedRoomType())
                    && room.getCleanlinessStatus().equalsIgnoreCase("Ready")
                    && room.getOccupancyStatus().equalsIgnoreCase("Vacant")
                    && !hasDateClash(room.getRoomNumber(), targetBooking)) {
                return room.getRoomNumber();
            }
        }
        return null;
    }

    private boolean hasDateClash(String roomNumber, BookingRequest targetBooking) {
        LocalDate targetCheckIn = LocalDate.parse(targetBooking.getCheckInDate());
        LocalDate targetCheckOut = LocalDate.parse(targetBooking.getCheckOutDate());

        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest existing = bookings.getEntry(i);
            if (!(existing.getStatus().equals(STATUS_ASSIGNED) || existing.getStatus().equals(STATUS_CHECKED_IN))
                    || !existing.getAssignedRoomNumber().equalsIgnoreCase(roomNumber)
                    || existing.getBookingId().equals(targetBooking.getBookingId())) {
                continue;
            }

            LocalDate existingCheckIn = LocalDate.parse(existing.getCheckInDate());
            LocalDate existingCheckOut = LocalDate.parse(existing.getCheckOutDate());
            if (targetCheckIn.isBefore(existingCheckOut) && targetCheckOut.isAfter(existingCheckIn)) {
                return true;
            }
        }
        return false;
    }

    private ListInterface<Room> loadRoomsFromFile() {
        ListInterface<Room> rooms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 7) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
                } else if (parts.length >= 6) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                } else if (parts.length == 4) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3]));
                } else if (parts.length == 3) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], "N/A"));
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, return empty room list.
        }
        return rooms;
    }

    private String normalizeLoadedStatus(String bookingType, String status) {
        if (bookingType.equals(TYPE_WALK_IN) && status.equals(STATUS_ASSIGNED)) {
            return STATUS_CHECKED_IN;
        }
        return status;
    }

    private void saveRoomsToFile(ListInterface<Room> rooms) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ROOMS_FILE))) {
            bw.write("# roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes");
            bw.newLine();
            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room room = rooms.getEntry(i);
                bw.write(room.getRoomNumber() + "|" + room.getRoomType() + "|" + room.getCleanlinessStatus()
                        + "|" + room.getOccupancyStatus() + "|" + room.getLastUpdate() + "|"
                        + room.getDirtySince() + "|" + room.getLastTurnaroundMinutes());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean isRoomVacant(String roomNumber) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room.getOccupancyStatus().equalsIgnoreCase("Vacant");
            }
        }
        return false;
    }

    private void updateRoomOccupancy(String roomNumber, String occupancyStatus) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                room.setOccupancyStatus(occupancyStatus);
                room.setLastUpdate(getCurrentTimestamp());
                saveRoomsToFile(rooms);
                return;
            }
        }
    }

    private void markRoomDirtyAfterCheckout(String roomNumber) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        String timestamp = getCurrentTimestamp();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                room.setOccupancyStatus("Vacant");
                room.setCleanlinessStatus("Dirty");
                room.setLastUpdate(timestamp);
                room.setDirtySince(timestamp);
                room.setLastTurnaroundMinutes("N/A");
                saveRoomsToFile(rooms);
                return;
            }
        }
    }

    private boolean isValidRoomType(String roomType) {
        return roomType.equalsIgnoreCase("Standard")
                || roomType.equalsIgnoreCase("Deluxe")
                || roomType.equalsIgnoreCase("Suite");
    }

    private void generatePaidBill(BookingRequest booking) {
        if (billExists(booking.getBookingId())) {
            return;
        }

        int nights = (int) ChronoUnit.DAYS.between(
                LocalDate.parse(booking.getCheckInDate()),
                LocalDate.parse(booking.getCheckOutDate()));
        double amount = nights * getRoomRate(booking.getRequestedRoomType());

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BILLING_FILE, true))) {
            bw.write(generateBillId() + "|" + booking.getBookingId() + "|"
                    + booking.getGuest().getConfirmationNo() + "|" + booking.getAssignedRoomNumber() + "|"
                    + booking.getRequestedRoomType() + "|" + booking.getCheckInDate() + "|"
                    + booking.getCheckOutDate() + "|" + nights + "|" + amount + "|Paid|"
                    + getCurrentTimestamp());
            bw.newLine();
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean billExists(String bookingId) {
        try (BufferedReader br = new BufferedReader(new FileReader(BILLING_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 2 && parts[1].equalsIgnoreCase(bookingId)) {
                    return true;
                }
            }
        } catch (IOException e) {
            createBillingFileIfMissing();
        }
        return false;
    }

    private String generateBillId() {
        int max = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(BILLING_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length > 0 && parts[0].startsWith("BL")) {
                    try {
                        int number = Integer.parseInt(parts[0].substring(2));
                        if (number > max) {
                            max = number;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore non-standard bill ids.
                    }
                }
            }
        } catch (IOException e) {
            createBillingFileIfMissing();
        }
        return String.format("BL%04d", max + 1);
    }

    private void createBillingFileIfMissing() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BILLING_FILE))) {
            bw.write("# billId|bookingId|confirmationNo|roomNumber|roomType|checkInDate|checkOutDate|nights|amount|paymentStatus|createdAt");
            bw.newLine();
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    private double getRoomRate(String roomType) {
        if (roomType.equalsIgnoreCase("Standard")) {
            return 200.0;
        }
        if (roomType.equalsIgnoreCase("Deluxe")) {
            return 300.0;
        }
        if (roomType.equalsIgnoreCase("Suite")) {
            return 500.0;
        }
        return 0.0;
    }

    private String generateBookingId() {
        int max = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            String id = bookings.getEntry(i).getBookingId();
            if (id.startsWith("B")) {
                try {
                    int number = Integer.parseInt(id.substring(1));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-standard booking ids.
                }
            }
        }
        return String.format("B%04d", max + 1);
    }

    private String generateConfirmationNo() {
        int max = 0;
        ListInterface<Guest> guests = frontDeskService.getAllGuestsSorted();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            String confirmationNo = guests.getEntry(i).getConfirmationNo();
            if (confirmationNo != null && confirmationNo.matches("\\d{8}")) {
                int number = Integer.parseInt(confirmationNo);
                if (number > max) {
                    max = number;
                }
            }
        }

        if (max == 0) {
            return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        return String.format("%08d", max + 1);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
