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
 * Controller for Module 1 — Walk-In Registrations & Standard Booking Procedure.
 *
 * @author Chang Han Yean
 */
public class BookingController {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String FILTER_ALL       = "ALL";
    public static final String TYPE_WALK_IN     = "Walk-In";
    public static final String TYPE_STANDARD    = "Standard";
    public static final String STATUS_PENDING   = "Pending";
    public static final String STATUS_ASSIGNED  = "Assigned";
    public static final String STATUS_CHECKED_IN  = "Checked In";
    public static final String STATUS_CHECKED_OUT = "Checked Out";
    public static final String STATUS_CANCELLED   = "Cancelled";

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------

    private ListInterface<BookingRequest>  bookings;
    private QueueInterface<BookingRequest> pendingQueue;
    private VIPRoomAllocation              vipAllocation;
    private FrontDeskService               frontDeskService;
    private BookingDAO                     bookingDAO;
    private RoomDAO                        roomDAO;
    private BillingDAO                     billingDAO;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    public BookingController() {
        bookings         = new ArrayList<>();
        pendingQueue     = new ArrayQueue<>();
        vipAllocation    = new VIPRoomAllocation();
        frontDeskService = new FrontDeskService();
        bookingDAO       = new BookingDAO();
        roomDAO          = new RoomDAO();
        billingDAO       = new BillingDAO();
        loadBookingsFromFile();
        rebuildPendingQueue();
    }

    // ═══════════════════════════════════════════════════════
    // Guest management
    // ═══════════════════════════════════════════════════════

    public Guest addGuest(String name, String phone) {
        if (name.isEmpty() || phone.isEmpty()) return null;
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
        if (bookings.isEmpty()) return null;
        return bookings.getEntry(bookings.getNumberOfEntries());
    }

    // ═══════════════════════════════════════════════════════
    // Standard booking creation
    // ═══════════════════════════════════════════════════════

    public String addStandardBooking(String confirmationNo, String requestedRoomType,
            String checkInDate, String checkOutDate) {

        Guest guest = getGuest(confirmationNo);
        if (guest == null) return "Guest not found. Add guest first.";

        String validation = validateBookingInput(requestedRoomType, checkInDate, checkOutDate);
        if (validation != null) return validation;

        if (hasOverlappingActiveBooking(confirmationNo, checkInDate, checkOutDate)) {
            return "Guest already has an active booking for this date range.";
        }

        BookingRequest booking = new BookingRequest(
                generateBookingId(), confirmationNo, TYPE_STANDARD,
                requestedRoomType, checkInDate, checkOutDate,
                STATUS_PENDING, "N/A", getCurrentTimestamp());
        booking.setGuest(guest);
        bookings.add(booking);
        pendingQueue.enqueue(booking);
        saveBookingsToFile();
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // Walk-in registration
    // ═══════════════════════════════════════════════════════

    public WalkInResult addWalkInRegistration(String confirmationNo,
            String requestedRoomType, String checkOutDate) {

        Guest guest = getGuest(confirmationNo);
        if (guest == null) return WalkInResult.error("Guest not found. Add guest first.");

        String checkInDate = LocalDate.now().toString();
        String validation  = validateBookingInput(requestedRoomType, checkInDate, checkOutDate);
        if (validation != null) return WalkInResult.error(validation);

        if (hasOverlappingActiveBooking(confirmationNo, checkInDate, checkOutDate)) {
            return WalkInResult.error("Guest already has an active booking for this date range.");
        }

        BookingRequest draftBooking = new BookingRequest(
                generateBookingId(), confirmationNo, TYPE_WALK_IN,
                requestedRoomType, checkInDate, checkOutDate,
                STATUS_PENDING, "N/A", getCurrentTimestamp());
        draftBooking.setGuest(guest);

        if (!hasSpareRoomAfterPendingStandardBookings(draftBooking)) {
            return WalkInResult.error("No " + requestedRoomType
                    + " room is available — pending standard bookings are reserved first.");
        }

        String roomNumber = findAvailableRoom(draftBooking);

        if (roomNumber != null) {
            return completeWalkIn(draftBooking, roomNumber);
        }

        ListInterface<Room> allAvailable = getAllAvailableRooms();
        if (allAvailable.getNumberOfEntries() == 0) {
            return WalkInResult.error("No rooms are currently available.");
        }

        return WalkInResult.manualNeeded(draftBooking, allAvailable,
                "No '" + requestedRoomType + "' room available. "
                + "Please select from the rooms below:");
    }

    public String completeWalkInManual(BookingRequest draftBooking, String roomNumber) {

        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                if (!r.getCleanlinessStatus().equalsIgnoreCase("Ready")
                        || !r.getOccupancyStatus().equalsIgnoreCase("Vacant")) {
                    return "Room " + roomNumber
                            + " is no longer available. Please choose another.";
                }
                break;
            }
        }

        WalkInResult result = completeWalkIn(draftBooking, roomNumber);
        return result.isSuccess() ? null : result.getErrorMessage();
    }

    // ═══════════════════════════════════════════════════════
    // Standard booking room allocation
    // ═══════════════════════════════════════════════════════

    public AssignResult autoAssignNextStandardBooking() {

        // VIP allocation is handled exclusively by the VIP module.
        // Standard booking auto-assign must NOT trigger VIP allocation
        // as a side effect — they are independent workflows.
        VIPRoomAllocation.AllocationResult vipResult =
                VIPRoomAllocation.AllocationResult.failure(
                        "VIP allocation runs independently.");

        BookingRequest nextBooking = pendingQueue.getFront();
        if (nextBooking == null) {
            return AssignResult.emptyQueue(vipResult);
        }

        String roomNumber = findAvailableRoom(nextBooking);

        if (roomNumber != null) {
            nextBooking.setAssignedRoomNumber(roomNumber);
            nextBooking.setStatus(STATUS_ASSIGNED);
            pendingQueue.dequeue();
            // Room stays Vacant — becomes Occupied only when guest checks in.
            saveBookingsToFile();
            return AssignResult.success(nextBooking, vipResult);
        }

        ListInterface<Room> allAvailable = getAllAvailableRooms();
        if (allAvailable.getNumberOfEntries() == 0) {
            return AssignResult.noRooms(nextBooking, vipResult);
        }

        return AssignResult.manualNeeded(nextBooking, allAvailable, vipResult,
                "No '" + nextBooking.getRequestedRoomType()
                + "' room available. Please select from the rooms below:");
    }

    public String assignRoomToBookingForGuest(String confirmationNo, String roomNumber) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getConfirmationNo().equalsIgnoreCase(confirmationNo)
                    && (booking.getStatus().equals(STATUS_PENDING)
                    ||  booking.getStatus().equals(STATUS_ASSIGNED))) {

                if (hasDateClash(roomNumber, booking)) {
                    return "Selected room is already assigned for this booking date.";
                }
                booking.setAssignedRoomNumber(roomNumber);
                booking.setStatus(STATUS_ASSIGNED);
                // Room stays Vacant until guest checks in.
                rebuildPendingQueue();
                saveBookingsToFile();
                return null;
            }
        }
        return "No pending booking found for this guest.";
    }

    // ═══════════════════════════════════════════════════════
    // Check in / check out / cancel
    // ═══════════════════════════════════════════════════════

    public String checkInBooking(String bookingId) {
        BookingRequest booking = getBooking(bookingId);
        if (booking == null) return "Booking not found.";
        if (!booking.getStatus().equals(STATUS_ASSIGNED)) {
            return "Only assigned standard bookings can be checked in.";
        }

        LocalDate today    = LocalDate.now();
        LocalDate checkIn  = LocalDate.parse(booking.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(booking.getCheckOutDate());
        if (today.isBefore(checkIn) || !today.isBefore(checkOut)) {
            return "Guest can only check in from check-in date until before check-out date.";
        }

        String roomError = validateAssignedRoomForCheckIn(booking.getAssignedRoomNumber());
        if (roomError != null) return roomError;

        booking.setStatus(STATUS_CHECKED_IN);
        updateRoomOccupancy(booking.getAssignedRoomNumber(), "Occupied");
        generatePaidBill(booking);
        saveBookingsToFile();
        return null;
    }

    public String checkOutBooking(String bookingId) {
        BookingRequest booking = getBooking(bookingId);
        if (booking == null) return "Booking not found.";
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
        if (booking == null)                                 return "Booking not found.";
        if (booking.getStatus().equals(STATUS_CANCELLED))   return "Booking is already cancelled.";
        if (booking.getStatus().equals(STATUS_CHECKED_IN))  return "Checked-in bookings cannot be cancelled. Please check out instead.";
        if (booking.getStatus().equals(STATUS_CHECKED_OUT)) return "Checked-out bookings are completed and cannot be cancelled.";

        booking.setStatus(STATUS_CANCELLED);
        rebuildPendingQueue();
        saveBookingsToFile();
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // Query helpers
    // ═══════════════════════════════════════════════════════

    public BookingRequest getBooking(String bookingId) {
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (b.getBookingId().equalsIgnoreCase(bookingId)) return b;
        }
        return null;
    }

    public ListInterface<BookingRequest> getBookingsByStatus(String statusFilter) {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (statusFilter.equals(FILTER_ALL)
                    || b.getStatus().equalsIgnoreCase(statusFilter)) {
                results.add(b);
            }
        }
        return results;
    }

    public ListInterface<BookingRequest> getCurrentBookings() {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (b.getStatus().equals(STATUS_PENDING)
                    || b.getStatus().equals(STATUS_ASSIGNED)
                    || b.getStatus().equals(STATUS_CHECKED_IN)) {
                results.add(b);
            }
        }
        return results;
    }

    public ListInterface<BookingRequest> getCancellableBookings() {
        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (b.getStatus().equals(STATUS_PENDING)
                    || b.getStatus().equals(STATUS_ASSIGNED)) {
                results.add(b);
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
        LocalDate today        = LocalDate.now();
        LocalDate checkOutDate = LocalDate.parse(booking.getCheckOutDate());
        if (checkOutDate.isBefore(today)) return "Overdue";
        if (checkOutDate.isEqual(today))  return "Due Today";
        return "Not Due";
    }

    public int getPendingQueueSize() {
        return pendingQueue.size();
    }

    // ═══════════════════════════════════════════════════════
    // Reports
    // ═══════════════════════════════════════════════════════

    public ListInterface<BookingRequest> generateBookingReport(
            String bookingTypeFilter, String roomTypeFilter, String statusFilter) {

        ListInterface<BookingRequest> results = new ArrayList<>();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            boolean matchType   = bookingTypeFilter.equals(FILTER_ALL)
                    || b.getBookingType().equalsIgnoreCase(bookingTypeFilter);
            boolean matchRoom   = roomTypeFilter.equals(FILTER_ALL)
                    || b.getRequestedRoomType().equalsIgnoreCase(roomTypeFilter);
            boolean matchStatus = statusFilter.equals(FILTER_ALL)
                    || b.getStatus().equalsIgnoreCase(statusFilter);
            if (matchType && matchRoom && matchStatus) results.add(b);
        }
        insertionSortBookingsByStatusThenDate(results);
        return results;
    }

    public ListInterface<RoomTypeDemandRow> generateRoomTypeDemandReport(
            String bookingTypeFilter) {
        ListInterface<RoomTypeDemandRow> rows = new ArrayList<>();
        rows.add(new RoomTypeDemandRow("Standard",
                countRoomTypeRequests("Standard", bookingTypeFilter)));
        rows.add(new RoomTypeDemandRow("Deluxe",
                countRoomTypeRequests("Deluxe", bookingTypeFilter)));
        rows.add(new RoomTypeDemandRow("Suite",
                countRoomTypeRequests("Suite", bookingTypeFilter)));
        insertionSortDemandRowsByRequests(rows);
        return rows;
    }

    public int countBookingsByStatus(ListInterface<BookingRequest> list, String status) {
        int count = 0;
        for (int i = 1; i <= list.getNumberOfEntries(); i++) {
            if (list.getEntry(i).getStatus().equals(status)) count++;
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

    // ═══════════════════════════════════════════════════════
    // VIP integration — FIXED
    // ═══════════════════════════════════════════════════════

    /**
     * Runs VIP allocation only if there is a guest in the VIP queue
     * who has a booking with status exactly "Pending" in bookings.txt.
     *
     * FIX: The old version called vipAllocation.findNextPendingGuest()
     * which checks the VIP module's internal booking list. That list
     * can be out of sync with BookingController's bookings list, causing
     * already-assigned VIP guests to be re-allocated and stealing rooms
     * from standard bookings.
     *
     * The fix cross-checks THIS controller's in-memory bookings list —
     * the single source of truth — to confirm a VIP booking is truly
     * Pending before triggering allocation.
     */
    private VIPRoomAllocation.AllocationResult runVIPAllocationIfPending() {

        if (vipAllocation.isQueueEmpty()) {
            return VIPRoomAllocation.AllocationResult.failure("VIP queue is empty.");
        }

        // Cross-check: scan THIS controller's bookings for a truly Pending VIP booking
        // whose guest is still in the VIP priority queue.
        boolean hasTrulyPendingVIP = false;

        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);

            if ("VIP".equals(b.getBookingType())
                    && STATUS_PENDING.equals(b.getStatus())
                    && vipAllocation.findGuestInQueue(b.getConfirmationNo()) != null) {
                hasTrulyPendingVIP = true;
                break;
            }
        }

        if (!hasTrulyPendingVIP) {
            return VIPRoomAllocation.AllocationResult.failure(
                    "No VIP guest with a Pending booking.");
        }

        return vipAllocation.allocateNextRoom();
    }

    // ═══════════════════════════════════════════════════════
    // Walk-in completion helper
    // ═══════════════════════════════════════════════════════

    private WalkInResult completeWalkIn(BookingRequest booking, String roomNumber) {
        booking.setAssignedRoomNumber(roomNumber);
        booking.setStatus(STATUS_CHECKED_IN);
        bookings.add(booking);
        // Walk-in means immediate check-in — room IS occupied right away.
        updateRoomOccupancy(roomNumber, "Occupied");
        generatePaidBill(booking);
        saveBookingsToFile();
        return WalkInResult.success(booking);
    }

    // ═══════════════════════════════════════════════════════
    // Room helpers
    // ═══════════════════════════════════════════════════════

    public ListInterface<Room> getAllAvailableRooms() {
        ListInterface<Room> rooms     = loadRoomsFromFile();
        ListInterface<Room> available = new ArrayList<>();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if ("Vacant".equalsIgnoreCase(r.getOccupancyStatus())
                    && "Ready".equalsIgnoreCase(r.getCleanlinessStatus())) {
                available.add(r);
            }
        }
        return available;
    }

    public ListInterface<Room> getAvailableRoomsByType(String roomType) {
        ListInterface<Room> all      = getAllAvailableRooms();
        ListInterface<Room> filtered = new ArrayList<>();
        for (int i = 1; i <= all.getNumberOfEntries(); i++) {
            Room r = all.getEntry(i);
            if (r.getRoomType().equalsIgnoreCase(roomType)) filtered.add(r);
        }
        return filtered;
    }

    // ═══════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════

    private void loadBookingsFromFile() {
        bookings.clear();
        ListInterface<BookingRequest> loaded = bookingDAO.loadBookings();
        for (int i = 1; i <= loaded.getNumberOfEntries(); i++) {
            BookingRequest b = loaded.getEntry(i);
            Guest g = getGuest(b.getConfirmationNo());
            if (g != null) { b.setGuest(g); bookings.add(b); }
        }
    }

    private void saveBookingsToFile() {
        bookingDAO.saveBookings(bookings);
    }

    private void rebuildPendingQueue() {
        pendingQueue.clear();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (b.getBookingType().equals(TYPE_STANDARD)
                    && b.getStatus().equals(STATUS_PENDING)) {
                pendingQueue.enqueue(b);
            }
        }
    }

    private String validateBookingInput(String requestedRoomType,
            String checkInDate, String checkOutDate) {
        if (!isValidRoomType(requestedRoomType)) {
            return "Room type must be Standard, Deluxe or Suite.";
        }
        try {
            LocalDate checkIn  = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (checkIn.isBefore(LocalDate.now()))
                return "Check-in date cannot be before today.";
            if (!checkOut.isAfter(checkIn))
                return "Check-out date must be after check-in date.";
        } catch (Exception e) {
            return "Date format must be YYYY-MM-DD.";
        }
        return null;
    }

    private String findAvailableRoom(BookingRequest targetBooking) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomType().equalsIgnoreCase(targetBooking.getRequestedRoomType())
                    && r.getCleanlinessStatus().equalsIgnoreCase("Ready")
                    && r.getOccupancyStatus().equalsIgnoreCase("Vacant")
                    && !hasDateClash(r.getRoomNumber(), targetBooking)) {
                return r.getRoomNumber();
            }
        }
        return null;
    }

    private boolean hasSpareRoomAfterPendingStandardBookings(
            BookingRequest walkInBooking) {
        int available  = countAvailableRoomsForBooking(walkInBooking);
        int protected_ = countPendingStandardBookingsToProtect(walkInBooking);
        return available > protected_;
    }

    private int countAvailableRoomsForBooking(BookingRequest target) {
        int count = 0;
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomType().equalsIgnoreCase(target.getRequestedRoomType())
                    && r.getCleanlinessStatus().equalsIgnoreCase("Ready")
                    && r.getOccupancyStatus().equalsIgnoreCase("Vacant")
                    && !hasDateClash(r.getRoomNumber(), target)) {
                count++;
            }
        }
        return count;
    }

    private int countPendingStandardBookingsToProtect(BookingRequest walkIn) {
        int count = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            if (b.getBookingType().equals(TYPE_STANDARD)
                    && b.getStatus().equals(STATUS_PENDING)
                    && b.getRequestedRoomType().equalsIgnoreCase(
                            walkIn.getRequestedRoomType())
                    && isDateOverlap(b, walkIn)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasDateClash(String roomNumber, BookingRequest target) {
        LocalDate tIn  = LocalDate.parse(target.getCheckInDate());
        LocalDate tOut = LocalDate.parse(target.getCheckOutDate());
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest ex = bookings.getEntry(i);
            if (!(ex.getStatus().equals(STATUS_ASSIGNED)
                    || ex.getStatus().equals(STATUS_CHECKED_IN))
                    || !ex.getAssignedRoomNumber().equalsIgnoreCase(roomNumber)
                    || ex.getBookingId().equals(target.getBookingId())) {
                continue;
            }
            LocalDate eIn  = LocalDate.parse(ex.getCheckInDate());
            LocalDate eOut = LocalDate.parse(ex.getCheckOutDate());
            if (tIn.isBefore(eOut) && tOut.isAfter(eIn)) return true;
        }
        return false;
    }

    private boolean hasOverlappingActiveBooking(String confirmationNo,
            String checkInDate, String checkOutDate) {
        LocalDate tIn  = LocalDate.parse(checkInDate);
        LocalDate tOut = LocalDate.parse(checkOutDate);
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest ex = bookings.getEntry(i);
            if (!ex.getConfirmationNo().equalsIgnoreCase(confirmationNo)
                    || !isActiveBooking(ex)) continue;
            LocalDate eIn  = LocalDate.parse(ex.getCheckInDate());
            LocalDate eOut = LocalDate.parse(ex.getCheckOutDate());
            if (tIn.isBefore(eOut) && tOut.isAfter(eIn)) return true;
        }
        return false;
    }

    private boolean isDateOverlap(BookingRequest a, BookingRequest b) {
        LocalDate aIn  = LocalDate.parse(a.getCheckInDate());
        LocalDate aOut = LocalDate.parse(a.getCheckOutDate());
        LocalDate bIn  = LocalDate.parse(b.getCheckInDate());
        LocalDate bOut = LocalDate.parse(b.getCheckOutDate());
        return aIn.isBefore(bOut) && aOut.isAfter(bIn);
    }

    private boolean isActiveBooking(BookingRequest b) {
        return b.getStatus().equals(STATUS_PENDING)
                || b.getStatus().equals(STATUS_ASSIGNED)
                || b.getStatus().equals(STATUS_CHECKED_IN);
    }

    private ListInterface<Room> loadRoomsFromFile() {
        return roomDAO.loadRooms();
    }

    private void saveRoomsToFile(ListInterface<Room> rooms) {
        roomDAO.saveRooms(rooms);
    }

    private void updateRoomOccupancy(String roomNumber, String occupancyStatus) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                r.setOccupancyStatus(occupancyStatus);
                r.setLastUpdate(getCurrentTimestamp());
                saveRoomsToFile(rooms);
                return;
            }
        }
    }

    private void markRoomDirtyAfterCheckout(String roomNumber) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        String ts = getCurrentTimestamp();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                r.setOccupancyStatus("Vacant");
                r.setCleanlinessStatus("Dirty");
                r.setLastUpdate(ts);
                r.setDirtySince(ts);
                r.setLastTurnaroundMinutes("N/A");
                saveRoomsToFile(rooms);
                return;
            }
        }
    }

    private String validateAssignedRoomForCheckIn(String roomNumber) {
        ListInterface<Room> rooms = loadRoomsFromFile();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                if (!r.getOccupancyStatus().equalsIgnoreCase("Vacant"))
                    return "Assigned room is currently occupied.";
                if (!r.getCleanlinessStatus().equalsIgnoreCase("Ready"))
                    return "Assigned room is not ready. Status: "
                            + r.getCleanlinessStatus();
                return null;
            }
        }
        return "Assigned room record was not found.";
    }

    private void generatePaidBill(BookingRequest booking) {
        ListInterface<BillingRecord> bills = billingDAO.loadBillingRecords();
        if (billExists(bills, booking.getBookingId())) return;
        int nights = (int) ChronoUnit.DAYS.between(
                LocalDate.parse(booking.getCheckInDate()),
                LocalDate.parse(booking.getCheckOutDate()));
        double amount = nights * getRoomRate(booking.getRequestedRoomType());
        billingDAO.appendBillingRecord(new BillingRecord(
                generateNextBillId(bills), booking.getBookingId(),
                booking.getConfirmationNo(), booking.getAssignedRoomNumber(),
                booking.getRequestedRoomType(), booking.getCheckInDate(),
                booking.getCheckOutDate(), nights, amount, "Paid",
                getCurrentTimestamp()));
    }

    private boolean billExists(ListInterface<BillingRecord> bills, String bookingId) {
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            if (bills.getEntry(i).getBookingId().equalsIgnoreCase(bookingId))
                return true;
        }
        return false;
    }

    private String generateNextBillId(ListInterface<BillingRecord> bills) {
        int max = 0;
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            String id = bills.getEntry(i).getBillId();
            if (id.startsWith("BL")) {
                try {
                    int n = Integer.parseInt(id.substring(2));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("BL%04d", max + 1);
    }

    public double getRoomRate(String roomType) {
        if (roomType.equalsIgnoreCase("Standard")) return 200.0;
        if (roomType.equalsIgnoreCase("Deluxe"))   return 300.0;
        if (roomType.equalsIgnoreCase("Suite"))     return 500.0;
        return 0.0;
    }

    private String generateBookingId() {
        int max = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            String id = bookings.getEntry(i).getBookingId();
            if (id.startsWith("B")) {
                try {
                    int n = Integer.parseInt(id.substring(1));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("B%04d", max + 1);
    }

    private String generateConfirmationNo() {
        int max = 0;
        ListInterface<Guest> guests = frontDeskService.getAllGuestsSorted();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            String no = guests.getEntry(i).getConfirmationNo();
            if (no != null && no.matches("\\d{8}")) {
                int n = Integer.parseInt(no);
                if (n > max) max = n;
            }
        }
        if (max == 0) return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("%08d", max + 1);
    }

    private boolean isValidRoomType(String t) {
        return t.equalsIgnoreCase("Standard")
                || t.equalsIgnoreCase("Deluxe")
                || t.equalsIgnoreCase("Suite");
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    private int countRoomTypeRequests(String roomType, String bookingTypeFilter) {
        int count = 0;
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest b = bookings.getEntry(i);
            boolean matchType = bookingTypeFilter.equals(FILTER_ALL)
                    || b.getBookingType().equalsIgnoreCase(bookingTypeFilter);
            if (matchType && b.getRequestedRoomType().equalsIgnoreCase(roomType))
                count++;
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════
    // Sorting algorithms
    // ═══════════════════════════════════════════════════════

    private void insertionSortBookingsByStatusThenDate(
            ListInterface<BookingRequest> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            BookingRequest key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && compareByStatusThenDate(list.getEntry(j), key) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private int compareByStatusThenDate(BookingRequest a, BookingRequest b) {
        int cmp = getStatusOrder(a.getStatus()) - getStatusOrder(b.getStatus());
        if (cmp != 0) return cmp;
        cmp = a.getCheckInDate().compareTo(b.getCheckInDate());
        if (cmp != 0) return cmp;
        return a.getBookingId().compareToIgnoreCase(b.getBookingId());
    }

    private int getStatusOrder(String s) {
        if (s.equals(STATUS_PENDING))     return 1;
        if (s.equals(STATUS_ASSIGNED))    return 2;
        if (s.equals(STATUS_CHECKED_IN))  return 3;
        if (s.equals(STATUS_CHECKED_OUT)) return 4;
        if (s.equals(STATUS_CANCELLED))   return 5;
        return 6;
    }

    private void insertionSortBookingsByCheckoutDue(
            ListInterface<BookingRequest> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            BookingRequest key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && compareByCheckoutDue(list.getEntry(j), key) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private int compareByCheckoutDue(BookingRequest a, BookingRequest b) {
        int cmp = getDueOrder(a) - getDueOrder(b);
        if (cmp != 0) return cmp;
        cmp = a.getCheckOutDate().compareTo(b.getCheckOutDate());
        if (cmp != 0) return cmp;
        return a.getBookingId().compareToIgnoreCase(b.getBookingId());
    }

    private int getDueOrder(BookingRequest b) {
        String label = getCheckoutDueLabel(b);
        if (label.equals("Overdue"))   return 1;
        if (label.equals("Due Today")) return 2;
        return 3;
    }

    private void insertionSortDemandRowsByRequests(
            ListInterface<RoomTypeDemandRow> list) {
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

    // ═══════════════════════════════════════════════════════
    // Inner classes — result objects
    // ═══════════════════════════════════════════════════════

    public static class WalkInResult {
        public enum Kind { SUCCESS, MANUAL_NEEDED, ERROR }

        private final Kind                kind;
        private final BookingRequest      booking;
        private final ListInterface<Room> availableRooms;
        private final String              message;

        private WalkInResult(Kind k, BookingRequest b,
                ListInterface<Room> rooms, String msg) {
            kind = k; booking = b; availableRooms = rooms; message = msg;
        }

        public static WalkInResult success(BookingRequest b) {
            return new WalkInResult(Kind.SUCCESS, b, null, null);
        }
        public static WalkInResult manualNeeded(BookingRequest draft,
                ListInterface<Room> rooms, String msg) {
            return new WalkInResult(Kind.MANUAL_NEEDED, draft, rooms, msg);
        }
        public static WalkInResult error(String msg) {
            return new WalkInResult(Kind.ERROR, null, null, msg);
        }

        public boolean            isSuccess()       { return kind == Kind.SUCCESS;       }
        public boolean            isManualNeeded()  { return kind == Kind.MANUAL_NEEDED; }
        public boolean            isError()         { return kind == Kind.ERROR;         }
        public BookingRequest     getBooking()      { return booking;        }
        public BookingRequest     getDraftBooking() { return booking;        }
        public ListInterface<Room>getAvailableRooms(){ return availableRooms; }
        public String             getErrorMessage() { return message;        }
        public String             getMessage()      { return message;        }
    }

    public static class AssignResult {
        public enum Kind { SUCCESS, MANUAL_NEEDED, EMPTY_QUEUE, NO_ROOMS }

        private final Kind                              kind;
        private final BookingRequest                    booking;
        private final ListInterface<Room>               availableRooms;
        private final String                            message;
        private final VIPRoomAllocation.AllocationResult vipResult;

        private AssignResult(Kind k, BookingRequest b, ListInterface<Room> rooms,
                String msg, VIPRoomAllocation.AllocationResult vip) {
            kind = k; booking = b; availableRooms = rooms; message = msg; vipResult = vip;
        }

        public static AssignResult success(BookingRequest b,
                VIPRoomAllocation.AllocationResult vip) {
            return new AssignResult(Kind.SUCCESS, b, null, null, vip);
        }
        public static AssignResult manualNeeded(BookingRequest b,
                ListInterface<Room> rooms, VIPRoomAllocation.AllocationResult vip,
                String msg) {
            return new AssignResult(Kind.MANUAL_NEEDED, b, rooms, msg, vip);
        }
        public static AssignResult emptyQueue(VIPRoomAllocation.AllocationResult vip) {
            return new AssignResult(Kind.EMPTY_QUEUE, null, null,
                    "No pending standard bookings in the queue.", vip);
        }
        public static AssignResult noRooms(BookingRequest b,
                VIPRoomAllocation.AllocationResult vip) {
            return new AssignResult(Kind.NO_ROOMS, b, null,
                    "No rooms are currently available.", vip);
        }

        public boolean            isSuccess()       { return kind == Kind.SUCCESS;       }
        public boolean            isManualNeeded()  { return kind == Kind.MANUAL_NEEDED; }
        public boolean            isEmptyQueue()    { return kind == Kind.EMPTY_QUEUE;   }
        public boolean            isNoRooms()       { return kind == Kind.NO_ROOMS;      }

        public BookingRequest                     getBooking()        { return booking;        }
        public ListInterface<Room>                getAvailableRooms() { return availableRooms; }
        public String                             getMessage()        { return message;        }
        public VIPRoomAllocation.AllocationResult getVIPResult()      { return vipResult;      }
    }

    public static class RoomTypeDemandRow {
        private final String roomType;
        private final int    requests;
        public RoomTypeDemandRow(String roomType, int requests) {
            this.roomType = roomType; this.requests = requests;
        }
        public String getRoomType() { return roomType; }
        public int    getRequests() { return requests; }
    }
}