package control;

import adt.ArrayList;
import adt.ArrayQueue;
import adt.ListInterface;
import adt.QueueInterface;
import dao.BillingDAO;
import dao.BookingDAO;
import dao.RoomDAO;
import entity.BillingRecord;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @author Chang Han Yean
 */
public class BookingController {
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
    private BookingDAO bookingDAO;
    private RoomDAO roomDAO;
    private BillingDAO billingDAO;

    public BookingController() {
        bookings = new ArrayList<>();
        pendingQueue = new ArrayQueue<>();
        frontDeskService = new FrontDeskService();
        bookingDAO = new BookingDAO();
        roomDAO = new RoomDAO();
        billingDAO = new BillingDAO();
        loadBookingsFromFile();
        rebuildPendingQueue();
    }

    public Guest addGuest(String name, String phone) {
        if (name.isEmpty() || phone.isEmpty()) {
            return null;
        }

        String confirmationNo = generateConfirmationNo();
        frontDeskService.addGuest(new Guest(confirmationNo, name, phone, "NONE"));
        return frontDeskService.searchByConfirmationNumber(confirmationNo);
    }

    public Guest getGuest(String confirmationNo) {
        return frontDeskService.searchByConfirmationNumber(confirmationNo);
    }

    public ListInterface<Guest> getAllGuests() {
        return frontDeskService.getAllGuestsSorted();
    }

    public BookingRequest getLatestBooking() {
        if (bookings.isEmpty()) {
            return null;
        }
        return bookings.getEntry(bookings.getNumberOfEntries());
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
        if (hasOverlappingActiveBooking(confirmationNo, checkInDate, checkOutDate)) {
            return "Guest already has an active booking for this date range.";
        }

        BookingRequest booking = new BookingRequest(generateBookingId(), confirmationNo, TYPE_STANDARD,
                requestedRoomType, checkInDate, checkOutDate, STATUS_PENDING, "N/A", getCurrentTimestamp());
        booking.setGuest(guest);
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
        if (hasOverlappingActiveBooking(confirmationNo, checkInDate, checkOutDate)) {
            return "Guest already has an active booking for this date range.";
        }

        BookingRequest booking = new BookingRequest(generateBookingId(), confirmationNo, TYPE_WALK_IN,
                requestedRoomType, checkInDate, checkOutDate, STATUS_PENDING, "N/A", getCurrentTimestamp());
        booking.setGuest(guest);
        if (!hasSpareRoomAfterPendingStandardBookings(booking)) {
            return "No " + requestedRoomType
                    + " room is available for walk-in booking because pending standard bookings are reserved first.";
        }

        String roomNumber = findAvailableRoom(booking);
        if (roomNumber == null) {
            return "No ready " + requestedRoomType + " room is available for this date.";
        }

        booking.setAssignedRoomNumber(roomNumber);
        booking.setStatus(STATUS_CHECKED_IN);
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

        String roomAvailabilityError = validateAssignedRoomForCheckIn(booking.getAssignedRoomNumber());
        if (roomAvailabilityError != null) {
            return roomAvailabilityError;
        }

        booking.setStatus(STATUS_CHECKED_IN);
        updateRoomOccupancy(booking.getAssignedRoomNumber(), "Occupied");
        generatePaidBill(booking);
        saveBookingsToFile();
        return null;
    }

    public String assignRoomToBookingForGuest(String confirmationNo, String roomNumber) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getConfirmationNo().equalsIgnoreCase(confirmationNo)
                    && (booking.getStatus().equals(STATUS_PENDING)
                            || booking.getStatus().equals(STATUS_ASSIGNED))) {
                if (hasDateClash(roomNumber, booking)) {
                    return "Selected room is already assigned for this booking date.";
                }

                booking.setAssignedRoomNumber(roomNumber);
                booking.setStatus(STATUS_ASSIGNED);
                rebuildPendingQueue();
                saveBookingsToFile();
                return null;
            }
        }
        return "No pending booking found for this guest.";
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
        if (booking.getStatus().equals(STATUS_CHECKED_IN)) {
            return "Checked-in bookings cannot be cancelled. Please check out the guest instead.";
        }
        if (booking.getStatus().equals(STATUS_CHECKED_OUT)) {
            return "Checked-out bookings are completed records and cannot be cancelled.";
        }

        booking.setStatus(STATUS_CANCELLED);
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

    public ListInterface<BookingRequest> getCancellableBookings() {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getStatus().equals(STATUS_PENDING)
                    || booking.getStatus().equals(STATUS_ASSIGNED)) {
                results.add(booking);
            }
        }
        return results;
    }

    public ListInterface<BookingRequest> getSortedCheckedInBookingsForCheckout() {
        ListInterface<BookingRequest> results = getBookingsByStatus(STATUS_CHECKED_IN);
        insertionSortBookingsByCheckoutDue(results);
        return results;
    }

    public String getCheckoutDueLabel(BookingRequest booking) {
        LocalDate today = LocalDate.now();
        LocalDate checkOutDate = LocalDate.parse(booking.getCheckOutDate());
        if (checkOutDate.isBefore(today)) {
            return "Overdue";
        }
        if (checkOutDate.isEqual(today)) {
            return "Due Today";
        }
        return "Not Due";
    }

    public ListInterface<BookingRequest> generateBookingReport(String bookingTypeFilter,
            String roomTypeFilter, String statusFilter) {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            boolean matchesBookingType = bookingTypeFilter.equals(FILTER_ALL)
                    || booking.getBookingType().equalsIgnoreCase(bookingTypeFilter);
            boolean matchesRoomType = roomTypeFilter.equals(FILTER_ALL)
                    || booking.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
            boolean matchesStatus = statusFilter.equals(FILTER_ALL)
                    || booking.getStatus().equalsIgnoreCase(statusFilter);

            if (matchesBookingType && matchesRoomType && matchesStatus) {
                results.add(booking);
            }
        }

        insertionSortBookingsByStatusThenDate(results);
        return results;
    }

    public ListInterface<RoomTypeDemandRow> generateRoomTypeDemandReport(String bookingTypeFilter) {
        ListInterface<RoomTypeDemandRow> rows = new ArrayList<>();
        rows.add(new RoomTypeDemandRow("Standard", countRoomTypeRequests("Standard", bookingTypeFilter)));
        rows.add(new RoomTypeDemandRow("Deluxe", countRoomTypeRequests("Deluxe", bookingTypeFilter)));
        rows.add(new RoomTypeDemandRow("Suite", countRoomTypeRequests("Suite", bookingTypeFilter)));
        insertionSortDemandRowsByRequests(rows);
        return rows;
    }

    public int countBookingsByStatus(ListInterface<BookingRequest> bookingList, String status) {
        int count = 0;
        for (int i = 1; i <= bookingList.getNumberOfEntries(); i++) {
            if (bookingList.getEntry(i).getStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalDemandRequests(ListInterface<RoomTypeDemandRow> rows) {
        int total = 0;
        for (int i = 1; i <= rows.getNumberOfEntries(); i++) {
            total += rows.getEntry(i).getRequests();
        }
        return total;
    }

    public int getPendingQueueSize() {
        return pendingQueue.size();
    }

    private void loadBookingsFromFile() {
        bookings.clear();
        ListInterface<BookingRequest> loadedBookings = bookingDAO.loadBookings();
        for (int i = 1; i <= loadedBookings.getNumberOfEntries(); i++) {
            BookingRequest booking = loadedBookings.getEntry(i);
            Guest guest = getGuest(booking.getConfirmationNo());
            if (guest != null) {
                booking.setGuest(guest);
                bookings.add(booking);
            }
        }
    }

    private void saveBookingsToFile() {
        bookingDAO.saveBookings(bookings);
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
            if (checkIn.isBefore(LocalDate.now())) {
                return "Check-in date cannot be before today.";
            }
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

    private boolean hasSpareRoomAfterPendingStandardBookings(BookingRequest walkInBooking) {
        int availableRoomCount = countAvailableRoomsForBooking(walkInBooking);
        int protectedPendingCount = countPendingStandardBookingsToProtect(walkInBooking);
        return availableRoomCount > protectedPendingCount;
    }

    private int countAvailableRoomsForBooking(BookingRequest targetBooking) {
        int count = 0;
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomType().equalsIgnoreCase(targetBooking.getRequestedRoomType())
                    && room.getCleanlinessStatus().equalsIgnoreCase("Ready")
                    && room.getOccupancyStatus().equalsIgnoreCase("Vacant")
                    && !hasDateClash(room.getRoomNumber(), targetBooking)) {
                count++;
            }
        }
        return count;
    }

    private int countPendingStandardBookingsToProtect(BookingRequest walkInBooking) {
        int count = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getBookingType().equals(TYPE_STANDARD)
                    && booking.getStatus().equals(STATUS_PENDING)
                    && booking.getRequestedRoomType().equalsIgnoreCase(walkInBooking.getRequestedRoomType())
                    && isDateOverlap(booking, walkInBooking)) {
                count++;
            }
        }
        return count;
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

    private boolean hasOverlappingActiveBooking(String confirmationNo, String checkInDate, String checkOutDate) {
        LocalDate targetCheckIn = LocalDate.parse(checkInDate);
        LocalDate targetCheckOut = LocalDate.parse(checkOutDate);

        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest existing = bookings.getEntry(i);
            if (!existing.getConfirmationNo().equalsIgnoreCase(confirmationNo)
                    || !isActiveBooking(existing)) {
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

    private boolean isDateOverlap(BookingRequest first, BookingRequest second) {
        LocalDate firstCheckIn = LocalDate.parse(first.getCheckInDate());
        LocalDate firstCheckOut = LocalDate.parse(first.getCheckOutDate());
        LocalDate secondCheckIn = LocalDate.parse(second.getCheckInDate());
        LocalDate secondCheckOut = LocalDate.parse(second.getCheckOutDate());
        return firstCheckIn.isBefore(secondCheckOut) && firstCheckOut.isAfter(secondCheckIn);
    }

    private boolean isActiveBooking(BookingRequest booking) {
        return booking.getStatus().equals(STATUS_PENDING)
                || booking.getStatus().equals(STATUS_ASSIGNED)
                || booking.getStatus().equals(STATUS_CHECKED_IN);
    }

    private ListInterface<Room> loadRoomsFromFile() {
        return roomDAO.loadRooms();
    }

    private int countRoomTypeRequests(String roomType, String bookingTypeFilter) {
        int count = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            boolean matchesBookingType = bookingTypeFilter.equals(FILTER_ALL)
                    || booking.getBookingType().equalsIgnoreCase(bookingTypeFilter);
            if (matchesBookingType && booking.getRequestedRoomType().equalsIgnoreCase(roomType)) {
                count++;
            }
        }
        return count;
    }

    private void insertionSortBookingsByStatusThenDate(ListInterface<BookingRequest> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            BookingRequest key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && compareBookingsByStatusThenDate(list.getEntry(j), key) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private int compareBookingsByStatusThenDate(BookingRequest left, BookingRequest right) {
        int statusCompare = getStatusOrder(left.getStatus()) - getStatusOrder(right.getStatus());
        if (statusCompare != 0) {
            return statusCompare;
        }

        int dateCompare = left.getCheckInDate().compareTo(right.getCheckInDate());
        if (dateCompare != 0) {
            return dateCompare;
        }
        return left.getBookingId().compareToIgnoreCase(right.getBookingId());
    }

    private int getStatusOrder(String status) {
        if (status.equals(STATUS_PENDING)) {
            return 1;
        }
        if (status.equals(STATUS_ASSIGNED)) {
            return 2;
        }
        if (status.equals(STATUS_CHECKED_IN)) {
            return 3;
        }
        if (status.equals(STATUS_CHECKED_OUT)) {
            return 4;
        }
        if (status.equals(STATUS_CANCELLED)) {
            return 5;
        }
        return 6;
    }

    private void insertionSortBookingsByCheckoutDue(ListInterface<BookingRequest> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            BookingRequest key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && compareBookingsByCheckoutDue(list.getEntry(j), key) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private int compareBookingsByCheckoutDue(BookingRequest left, BookingRequest right) {
        int dueCompare = getCheckoutDueOrder(left) - getCheckoutDueOrder(right);
        if (dueCompare != 0) {
            return dueCompare;
        }

        int dateCompare = left.getCheckOutDate().compareTo(right.getCheckOutDate());
        if (dateCompare != 0) {
            return dateCompare;
        }
        return left.getBookingId().compareToIgnoreCase(right.getBookingId());
    }

    private int getCheckoutDueOrder(BookingRequest booking) {
        String label = getCheckoutDueLabel(booking);
        if (label.equals("Overdue")) {
            return 1;
        }
        if (label.equals("Due Today")) {
            return 2;
        }
        return 3;
    }

    private void insertionSortDemandRowsByRequests(ListInterface<RoomTypeDemandRow> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            RoomTypeDemandRow key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getRequests() < key.getRequests()) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private void saveRoomsToFile(ListInterface<Room> rooms) {
        roomDAO.saveRooms(rooms);
    }

    private String validateAssignedRoomForCheckIn(String roomNumber) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                if (!room.getOccupancyStatus().equalsIgnoreCase("Vacant")) {
                    return "Assigned room is currently occupied.";
                }
                if (!room.getCleanlinessStatus().equalsIgnoreCase("Ready")) {
                    return "Assigned room is not ready for check-in. Current cleaning status: "
                            + room.getCleanlinessStatus() + ".";
                }
                return null;
            }
        }
        return "Assigned room record was not found.";
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
        ListInterface<BillingRecord> bills = billingDAO.loadBillingRecords();
        if (billExists(bills, booking.getBookingId())) {
            return;
        }

        int nights = (int) ChronoUnit.DAYS.between(
                LocalDate.parse(booking.getCheckInDate()),
                LocalDate.parse(booking.getCheckOutDate()));
        double amount = nights * getRoomRate(booking.getRequestedRoomType());

        billingDAO.appendBillingRecord(new BillingRecord(generateNextBillId(bills), booking.getBookingId(),
                booking.getConfirmationNo(), booking.getAssignedRoomNumber(),
                booking.getRequestedRoomType(), booking.getCheckInDate(), booking.getCheckOutDate(),
                nights, amount, "Paid", getCurrentTimestamp()));
    }

    private boolean billExists(ListInterface<BillingRecord> bills, String bookingId) {
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            if (bills.getEntry(i).getBookingId().equalsIgnoreCase(bookingId)) {
                return true;
            }
        }
        return false;
    }

    private String generateNextBillId(ListInterface<BillingRecord> bills) {
        int max = 0;
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            String id = bills.getEntry(i).getBillId();
            if (id.startsWith("BL")) {
                try {
                    int number = Integer.parseInt(id.substring(2));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-standard bill ids.
                }
            }
        }
        return String.format("BL%04d", max + 1);
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

    public static class RoomTypeDemandRow {
        private String roomType;
        private int requests;

        public RoomTypeDemandRow(String roomType, int requests) {
            this.roomType = roomType;
            this.requests = requests;
        }

        public String getRoomType() {
            return roomType;
        }

        public int getRequests() {
            return requests;
        }
    }
}
