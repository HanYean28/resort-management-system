package control;

import adt.ArrayList;
import adt.ArrayPriorityQueue;
import adt.ListInterface;
import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Room;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import utility.DateUtils;

/**
 * Controller for Module 2 — VIP & Loyalty Tier Priority Room Allocation.
 *
 * All VIP business rules, file processing, report filtering/searching and
 * explicit sorting are kept in this control class. The boundary class only
 * collects input and displays returned results.
 *
 * No Java Collections Framework collection interfaces/classes are used here.
 * Custom ListInterface/ArrayList and PriorityQueueInterface are used instead.
 *
 * @author Kaizen Soh
 */
public class VIPRoomAllocationController {

    private static final String GUESTS_FILE = "guests.txt";
    private static final String ROOMS_FILE = "rooms.txt";
    private static final String BOOKINGS_FILE = "bookings.txt";
    private static final String BILLING_FILE = "billing.txt";

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_ASSIGNED = "Assigned";
    public static final String STATUS_CHECKED_IN = "Checked In";
    public static final String STATUS_CHECKED_OUT = "Checked Out";
    public static final String STATUS_CANCELLED = "Cancelled";

    /** Non-linear VIP collection ADT, implemented using a BinaryHeap. */
    private final PriorityQueueInterface<Guest> vipQueue;
    private final ListInterface<Room> allRooms;
    private final ListInterface<String> allocationLog;

    public VIPRoomAllocationController() {
        vipQueue = new ArrayPriorityQueue<>((a, b) ->
                tierScore(a.getLoyaltyTier()) - tierScore(b.getLoyaltyTier()));
        allRooms = new ArrayList<>();
        allocationLog = new ArrayList<>();
        loadGuestsFromFile();
        loadRoomsFromFile();
    }

    // ------------------------------------------------------------------
    // File loading / persistence
    // ------------------------------------------------------------------

    private void loadGuestsFromFile() {
        vipQueue.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 4) continue;

                String tier = normaliseTier(parts[3].trim());
                if ("None".equalsIgnoreCase(tier)) continue;

                String confirmationNo = parts[0].trim();
                if (findGuestInQueue(confirmationNo) == null) {
                    vipQueue.add(new Guest(
                            confirmationNo,
                            parts[1].trim(),
                            parts[2].trim(),
                            tier));
                }
            }
        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + GUESTS_FILE + ": " + e.getMessage());
        }
    }

    private void loadRoomsFromFile() {
        allRooms.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 7) continue;

                allRooms.add(new Room(
                        parts[0].trim(), parts[1].trim(), parts[2].trim(),
                        parts[3].trim(), parts[4].trim(), parts[5].trim(),
                        parts[6].trim()));
            }
        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + ROOMS_FILE + ": " + e.getMessage());
        }
    }

    private void saveRoomsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ROOMS_FILE))) {
            bw.write("# roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes");
            bw.newLine();
            for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
                Room r = allRooms.getEntry(i);
                bw.write(r.getRoomNumber() + "|"
                        + r.getRoomType() + "|"
                        + r.getCleanlinessStatus() + "|"
                        + r.getOccupancyStatus() + "|"
                        + r.getLastUpdate() + "|"
                        + r.getDirtySince() + "|"
                        + r.getLastTurnaroundMinutes());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("[VIP] Could not save " + ROOMS_FILE + ": " + e.getMessage());
        }
    }

    private void appendGuestToFile(Guest guest) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GUESTS_FILE, true))) {
            bw.write(guest.getConfirmationNo() + "|"
                    + guest.getName() + "|"
                    + guest.getPhone() + "|"
                    + guest.getLoyaltyTier());
            bw.newLine();
        } catch (IOException e) {
            throw new IllegalStateException("Could not save guest: " + e.getMessage());
        }
    }

    /** Removes a registered guest record while preserving every other guest. */
    private boolean removeGuestFromFile(String confirmationNo) {
        ListInterface<String> lines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    String[] parts = trimmed.split("\\|", -1);
                    if (parts.length >= 1
                            && parts[0].trim().equalsIgnoreCase(confirmationNo)) {
                        found = true;
                        continue;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }

        if (!found) return false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GUESTS_FILE))) {
            for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
                bw.write(lines.getEntry(i));
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Guest / priority queue management
    // ------------------------------------------------------------------

    public void addGuest(Guest guest) {
        if (guest == null) throw new IllegalArgumentException("Guest cannot be null.");
        if (guest.getConfirmationNo() == null
                || !guest.getConfirmationNo().matches("\\d{8}")) {
            throw new IllegalArgumentException("Confirmation number must contain exactly 8 digits.");
        }
        if (findGuestByConfirmationNo(guest.getConfirmationNo()) != null) {
            throw new IllegalArgumentException("Confirmation number already exists.");
        }

        vipQueue.add(guest);
        try {
            appendGuestToFile(guest);
        } catch (RuntimeException e) {
            vipQueue.remove(guest);
            throw e;
        }
    }

    public Guest peekNextGuest() {
        return vipQueue.peek();
    }

    public Guest findGuestInQueue(String confirmationNo) {
        if (confirmationNo == null) return null;
        Guest[] guests = vipQueue.toSortedArray(new Guest[vipQueue.size()]);
        for (Guest guest : guests) {
            if (guest != null && confirmationNo.equals(guest.getConfirmationNo())) {
                return guest;
            }
        }
        return null;
    }

    /** Searches the persistent guest records, including already-assigned VIPs. */
    public Guest findGuestByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length >= 4
                        && parts[0].trim().equalsIgnoreCase(confirmationNo)) {
                    return new Guest(parts[0].trim(), parts[1].trim(), parts[2].trim(),
                            normaliseTier(parts[3].trim()));
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    public boolean removeGuestFromQueue(String confirmationNo) {
        Guest guest = findGuestInQueue(confirmationNo);
        if (guest == null) return false;

        // Do not delete a guest who still has an active booking.
        if (hasActiveVipBooking(confirmationNo)) return false;

        if (!vipQueue.remove(guest)) return false;
        if (!removeGuestFromFile(confirmationNo)) {
            vipQueue.add(guest);
            return false;
        }
        return true;
    }

    public Guest[] getAllWaitingGuests() {
        return vipQueue.toSortedArray(new Guest[vipQueue.size()]);
    }

    public int getQueueSize() {
        return vipQueue.size();
    }

    public boolean isQueueEmpty() {
        return vipQueue.isEmpty();
    }

    // ------------------------------------------------------------------
    // Booking creation / cancellation
    // ------------------------------------------------------------------

    public String createPendingVipBooking(Guest guest, String requestedRoomType,
            String checkInDate, String checkOutDate) {
        if (guest == null) return "Guest cannot be null.";
        if (requestedRoomType == null || requestedRoomType.trim().isEmpty()
                || checkInDate == null || checkInDate.trim().isEmpty()
                || checkOutDate == null || checkOutDate.trim().isEmpty()) {
            return "Booking details cannot be empty.";
        }
        if (hasActiveVipBooking(guest.getConfirmationNo())) {
            return "Guest already has an active VIP booking.";
        }

        String bookingId = generateNextBookingId();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE, true))) {
            bw.write(bookingId + "|" + guest.getConfirmationNo() + "|VIP|"
                    + requestedRoomType.trim() + "|" + checkInDate.trim() + "|"
                    + checkOutDate.trim() + "|Pending|N/A|"
                    + DateUtils.getCurrentTimestamp());
            bw.newLine();
            return null;
        } catch (IOException e) {
            return "Could not save VIP booking: " + e.getMessage();
        }
    }

    private boolean hasActiveVipBooking(String confirmationNo) {
        String[][] rows = readAllVipBookings();
        for (String[] row : rows) {
            if (row[1].equalsIgnoreCase(confirmationNo)
                    && (row[6].equalsIgnoreCase(STATUS_PENDING)
                    || row[6].equalsIgnoreCase(STATUS_ASSIGNED))) {
                return true;
            }
        }
        return false;
    }

    /** Boundary-safe cancellation: only a Pending VIP booking can be cancelled. */
    public boolean cancelPendingVipBooking(String bookingId) {
        if (bookingId == null) return false;
        ListInterface<String> lines = new ArrayList<>();
        boolean changed = false;

        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    String[] parts = trimmed.split("\\|", -1);
                    if (!changed && parts.length >= 9
                            && parts[0].trim().equalsIgnoreCase(bookingId)
                            && parts[2].trim().equalsIgnoreCase("VIP")
                            && parts[6].trim().equalsIgnoreCase(STATUS_PENDING)) {
                        parts[6] = STATUS_CANCELLED;
                        line = join(parts, "|");
                        changed = true;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }

        if (!changed) return false;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE))) {
            for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
                bw.write(lines.getEntry(i));
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getCheckInDate(String confirmationNo) {
        String[] row = findLatestVipBookingForGuest(confirmationNo);
        return row == null ? "N/A" : row[4];
    }

    public String getCheckOutDate(String confirmationNo) {
        String[] row = findLatestVipBookingForGuest(confirmationNo);
        return row == null ? "N/A" : row[5];
    }

    public String getRequestedRoomType(String confirmationNo) {
        String[] row = findLatestVipBookingForGuest(confirmationNo);
        return row == null ? null : row[3];
    }

    private String[] findLatestVipBookingForGuest(String confirmationNo) {
        String[] found = null;
        String[][] rows = readAllVipBookings();
        for (String[] row : rows) {
            if (row[1].equalsIgnoreCase(confirmationNo)) found = row;
        }
        return found;
    }

    // ------------------------------------------------------------------
    // Room management / allocation
    // ------------------------------------------------------------------

    public ListInterface<Room> getAvailableRooms() {
        ListInterface<Room> available = new ArrayList<>();
        for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
            Room room = allRooms.getEntry(i);
            if (isRoomAvailable(room)) available.add(room);
        }
        return available;
    }

    public int getAvailableRoomCount() {
        return getAvailableRooms().getNumberOfEntries();
    }

    public void addAvailableRoom(Room room) {
        if (room == null) return;
        allRooms.add(room);
        saveRoomsToFile();
    }

    public boolean hasPendingVipBooking() {
        String[][] rows = readAllVipBookings();
        for (String[] row : rows) {
            if (row[6].equalsIgnoreCase(STATUS_PENDING)
                    && findGuestInQueue(row[1]) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allocates exactly ONE Pending VIP booking. Priority is loyalty tier only;
     * same-tier bookings retain their existing file/booking order.
     */
    public AllocationResult allocateNextPendingBooking() {
        if (vipQueue.isEmpty()) return AllocationResult.failure("VIP queue is empty.");
        loadRoomsFromFile();

        String[][] pending = getVipBookingsByStatus(STATUS_PENDING);
        if (pending.length == 0) return AllocationResult.failure("No pending VIP booking found.");

        // Stable insertion sort: tier only. Equal tiers keep original booking order.
        insertionSortBookingsByTier(pending, true);

        String[] selectedBooking = null;
        Guest selectedGuest = null;
        for (String[] row : pending) {
            Guest guest = findGuestInQueue(row[1]);
            if (guest != null) {
                selectedBooking = row;
                selectedGuest = guest;
                break;
            }
        }

        if (selectedGuest == null) {
            return AllocationResult.failure("No pending VIP booking found.");
        }

        ListInterface<Room> available = getAvailableRooms();
        if (available.isEmpty()) {
            return AllocationResult.failure("No rooms are currently available.");
        }

        Room matched = null;
        String requestedType = selectedBooking[3];
        for (int i = 1; i <= available.getNumberOfEntries(); i++) {
            Room room = available.getEntry(i);
            if (room.getRoomType().equalsIgnoreCase(requestedType)
                    && !hasRoomDateClash(room.getRoomNumber(), selectedBooking)) {
                matched = room;
                break;
            }
        }

        if (matched == null) {
            // Requested room type is unavailable. Keep serving this SAME VIP guest
            // and offer only alternative rooms that are Vacant + Ready and do not
            // clash with this booking's date range.
            ListInterface<Room> alternatives = new ArrayList<>();
            for (int i = 1; i <= available.getNumberOfEntries(); i++) {
                Room room = available.getEntry(i);
                if (!hasRoomDateClash(room.getRoomNumber(), selectedBooking)) {
                    alternatives.add(room);
                }
            }

            if (alternatives.isEmpty()) {
                return AllocationResult.failure(
                        "No suitable rooms are available for " + selectedGuest.getName()
                        + " during the selected booking dates. Booking remains Pending.");
            }

            return AllocationResult.manualNeeded(
                    selectedGuest,
                    alternatives,
                    "No available room of requested type '" + requestedType
                    + "' for " + selectedGuest.getName()
                    + ". Please choose another available room.");
        }

        vipQueue.remove(selectedGuest);
        matched.setLastUpdate(DateUtils.getCurrentTimestamp());
        saveRoomsToFile();

        if (!saveBookingAssignment(selectedGuest, matched, selectedBooking)) {
            vipQueue.add(selectedGuest);
            return AllocationResult.failure("Could not update the VIP booking.");
        }

        String entry = buildLogEntry(selectedGuest, matched);
        allocationLog.add(entry);
        return AllocationResult.success(selectedGuest, matched, entry);
    }

    /** Returns the requested room type for the guest's current Pending VIP booking. */
    public String getRequestedRoomTypeForPendingBooking(String confirmationNo) {
        String[] booking = findPendingVipBookingForGuest(confirmationNo);
        return booking == null ? "N/A" : booking[3];
    }

    /** Manual allocation for a particular VIP guest. */
    public AllocationResult allocateRoom(Guest guest, String roomNumber) {
        if (guest == null || roomNumber == null) {
            return AllocationResult.failure("Invalid guest or room number.");
        }
        loadRoomsFromFile();
        String[] booking = findPendingVipBookingForGuest(guest.getConfirmationNo());
        if (booking == null) {
            return AllocationResult.failure("No Pending VIP booking found for this guest.");
        }

        Room target = null;
        for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
            Room room = allRooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && isRoomAvailable(room)
                    && !hasRoomDateClash(roomNumber, booking)) {
                target = room;
                break;
            }
        }
        if (target == null) {
            return AllocationResult.failure("Room " + roomNumber + " is not available.");
        }

        vipQueue.remove(guest);
        target.setLastUpdate(DateUtils.getCurrentTimestamp());
        saveRoomsToFile();
        if (!saveBookingAssignment(guest, target, booking)) {
            vipQueue.add(guest);
            return AllocationResult.failure("Could not update the VIP booking.");
        }

        String entry = buildLogEntry(guest, target);
        allocationLog.add(entry);
        return AllocationResult.success(guest, target, entry);
    }

    public AllocationResult allocateNextRoom() {
        return allocateNextPendingBooking();
    }

    private String[] findPendingVipBookingForGuest(String confirmationNo) {
        String[][] rows = getVipBookingsByStatus(STATUS_PENDING);
        for (String[] row : rows) {
            if (row[1].equalsIgnoreCase(confirmationNo)) return row;
        }
        return null;
    }

    private boolean saveBookingAssignment(Guest guest, Room room, String[] targetBooking) {
        ListInterface<String> lines = new ArrayList<>();
        boolean updated = false;
        String bookingId = targetBooking[0];

        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!updated && !trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    String[] parts = trimmed.split("\\|", -1);
                    if (parts.length >= 9
                            && parts[0].trim().equalsIgnoreCase(bookingId)
                            && parts[1].trim().equalsIgnoreCase(guest.getConfirmationNo())
                            && parts[2].trim().equalsIgnoreCase("VIP")
                            && parts[6].trim().equalsIgnoreCase(STATUS_PENDING)) {
                        parts[6] = STATUS_ASSIGNED;
                        parts[7] = room.getRoomNumber();
                        line = join(parts, "|");
                        updated = true;
                    }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            return false;
        }

        if (!updated) return false;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE))) {
            for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
                bw.write(lines.getEntry(i));
                bw.newLine();
            }
        } catch (IOException e) {
            return false;
        }

        saveBillingToFile(guest, room, bookingId, targetBooking[4], targetBooking[5]);
        return true;
    }

    private void saveBillingToFile(Guest guest, Room room, String bookingId,
            String checkIn, String checkOut) {
        int nights = 1;
        try {
            int calculated = DateUtils.countNights(checkIn, checkOut);
            if (calculated > 0) nights = calculated;
        } catch (Exception ignored) { }

        double amount = nights * getTierRate(guest.getLoyaltyTier());
        String billId = generateNextBillId();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BILLING_FILE, true))) {
            bw.write(billId + "|" + bookingId + "|" + guest.getConfirmationNo() + "|"
                    + room.getRoomNumber() + "|" + room.getRoomType() + "|"
                    + checkIn + "|" + checkOut + "|" + nights + "|"
                    + String.format("%.2f", amount) + "|Paid|"
                    + DateUtils.getCurrentTimestamp());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("[VIP] Could not append to " + BILLING_FILE + ": " + e.getMessage());
        }
    }

    private boolean hasRoomDateClash(String roomNumber, String[] targetBooking) {
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 9) continue;

                boolean reserves = parts[6].trim().equalsIgnoreCase(STATUS_ASSIGNED)
                        || parts[6].trim().equalsIgnoreCase(STATUS_CHECKED_IN);
                if (!reserves
                        || !parts[7].trim().equalsIgnoreCase(roomNumber)
                        || parts[0].trim().equalsIgnoreCase(targetBooking[0])) continue;

                try {
                    if (DateUtils.isDateOverlap(targetBooking[4], targetBooking[5],
                            parts[4].trim(), parts[5].trim())) return true;
                } catch (Exception ignored) { }
            }
        } catch (IOException ignored) { }
        return false;
    }

    // ------------------------------------------------------------------
    // Report 1: VIP Booking Report
    // ------------------------------------------------------------------

    /** Reads all VIP bookings and returns detached row arrays. */
    public String[][] readAllVipBookings() {
        ListInterface<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 7 || !parts[2].trim().equalsIgnoreCase("VIP")) continue;

                String[] row = new String[9];
                for (int i = 0; i < 9; i++) {
                    row[i] = i < parts.length ? parts[i].trim() : "";
                }
                rows.add(row);
            }
        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + BOOKINGS_FILE + ": " + e.getMessage());
        }
        return toBookingArray(rows);
    }

    public String[][] getVipBookingsByStatus(String status) {
        String[][] all = readAllVipBookings();
        ListInterface<String[]> result = new ArrayList<>();
        for (String[] row : all) {
            if (status == null || "ALL".equalsIgnoreCase(status)
                    || row[6].equalsIgnoreCase(status)) {
                result.add(row);
            }
        }
        return toBookingArray(result);
    }

    /**
     * Report search/filter + explicit stable insertion sort.
     * sortOrder: TIER_DESC or TIER_ASC.
     */
    public String[][] generateVipBookingReport(String statusFilter,
            String roomTypeFilter, String sortOrder) {
        String[][] all = readAllVipBookings();
        ListInterface<String[]> matches = new ArrayList<>();

        // Explicit linear search/filter across booking records.
        for (String[] row : all) {
            boolean statusMatch = "ALL".equalsIgnoreCase(statusFilter)
                    || row[6].equalsIgnoreCase(statusFilter);
            boolean roomMatch = "ALL".equalsIgnoreCase(roomTypeFilter)
                    || row[3].equalsIgnoreCase(roomTypeFilter);
            if (statusMatch && roomMatch) matches.add(row);
        }

        String[][] result = toBookingArray(matches);
        insertionSortBookingsByTier(result, !"TIER_ASC".equalsIgnoreCase(sortOrder));
        return result;
    }

    /** Stable insertion sort written explicitly; no Collections/List.sort is used. */
    private void insertionSortBookingsByTier(String[][] rows, boolean highestFirst) {
        for (int i = 1; i < rows.length; i++) {
            String[] key = rows[i];
            int j = i - 1;
            while (j >= 0 && shouldMoveBooking(rows[j], key, highestFirst)) {
                rows[j + 1] = rows[j];
                j--;
            }
            rows[j + 1] = key;
        }
    }

    private boolean shouldMoveBooking(String[] left, String[] key, boolean highestFirst) {
        int leftRank = tierRank(getTierForConfirmation(left[1]));
        int keyRank = tierRank(getTierForConfirmation(key[1]));
        return highestFirst ? leftRank > keyRank : leftRank < keyRank;
    }

    // ------------------------------------------------------------------
    // Report 2: VIP Revenue Summary
    // ------------------------------------------------------------------

    public VipRevenueRow[] generateVipRevenueSummary() {
        return generateVipRevenueSummary("ALL", "ALL", "REVENUE_DESC");
    }

    public VipRevenueRow[] generateVipRevenueSummary(String tierFilter,
            String roomTypeFilter) {
        return generateVipRevenueSummary(tierFilter, roomTypeFilter, "REVENUE_DESC");
    }

    /**
     * Generates PAID VIP revenue, applies two filters, then explicitly sorts
     * the summary with insertion sort.
     *
     * sortOrder: REVENUE_DESC, REVENUE_ASC, or TIER_DESC.
     */
    public VipRevenueRow[] generateVipRevenueSummary(String tierFilter,
            String roomTypeFilter, String sortOrder) {
        String[] tiers = {"Diamond", "Elite", "Platinum", "Gold", "Silver"};
        int[] counts = new int[tiers.length];
        double[] revenues = new double[tiers.length];

        try (BufferedReader br = new BufferedReader(new FileReader(BILLING_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 10 || !parts[9].trim().equalsIgnoreCase("Paid")) continue;

                String[] booking = findBookingById(parts[1].trim());
                if (booking == null || !booking[2].equalsIgnoreCase("VIP")) continue;

                String roomType = parts.length > 4 ? parts[4].trim() : booking[3];
                if (!"ALL".equalsIgnoreCase(roomTypeFilter)
                        && !roomType.equalsIgnoreCase(roomTypeFilter)) continue;

                String tier = getTierForConfirmation(parts[2].trim());
                if ("None".equalsIgnoreCase(tier)) {
                    try {
                        int nights = Integer.parseInt(parts[7].trim());
                        double amount = Double.parseDouble(parts[8].trim());
                        tier = inferTierFromVipBill(nights, amount);
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                }

                if (!"ALL".equalsIgnoreCase(tierFilter)
                        && !tier.equalsIgnoreCase(tierFilter)) continue;

                int index = tierIndex(tier);
                if (index < 0) continue;
                try {
                    double amount = Double.parseDouble(parts[8].trim());
                    counts[index]++;
                    revenues[index] += amount;
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException ignored) { }

        ListInterface<VipRevenueRow> resultList = new ArrayList<>();
        for (int i = 0; i < tiers.length; i++) {
            if ("ALL".equalsIgnoreCase(tierFilter)
                    || tiers[i].equalsIgnoreCase(tierFilter)) {
                resultList.add(new VipRevenueRow(tiers[i], counts[i], revenues[i]));
            }
        }

        VipRevenueRow[] result = new VipRevenueRow[resultList.getNumberOfEntries()];
        for (int i = 1; i <= resultList.getNumberOfEntries(); i++) {
            result[i - 1] = resultList.getEntry(i);
        }
        insertionSortRevenueRows(result, sortOrder);
        return result;
    }

    /** Explicit insertion sort for Report 2. */
    private void insertionSortRevenueRows(VipRevenueRow[] rows, String sortOrder) {
        for (int i = 1; i < rows.length; i++) {
            VipRevenueRow key = rows[i];
            int j = i - 1;
            while (j >= 0 && shouldMoveRevenue(rows[j], key, sortOrder)) {
                rows[j + 1] = rows[j];
                j--;
            }
            rows[j + 1] = key;
        }
    }

    private boolean shouldMoveRevenue(VipRevenueRow left, VipRevenueRow key, String sortOrder) {
        if ("REVENUE_ASC".equalsIgnoreCase(sortOrder)) {
            return left.getRevenue() > key.getRevenue();
        }
        if ("TIER_DESC".equalsIgnoreCase(sortOrder)) {
            return tierRank(left.getTier()) > tierRank(key.getTier());
        }
        return left.getRevenue() < key.getRevenue(); // REVENUE_DESC
    }

    public double computeBookingPreview(String loyaltyTier, String checkIn, String checkOut) {
        int nights = 1;
        try {
            int calculated = DateUtils.countNights(checkIn, checkOut);
            if (calculated > 0) nights = calculated;
        } catch (Exception ignored) { }
        return nights * getTierRate(loyaltyTier);
    }

    // ------------------------------------------------------------------
    // IDs / lookup helpers
    // ------------------------------------------------------------------

    /** Generates a unique confirmation number that is ALWAYS exactly 8 digits. */
    public String generateConfirmationNo() {
        int max = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length == 0 || !parts[0].trim().matches("\\d{8}")) continue;
                try {
                    int value = Integer.parseInt(parts[0].trim());
                    if (value > max) max = value;
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException ignored) { }

        int next = max + 1;
        if (next > 99999999) {
            throw new IllegalStateException("Cannot generate confirmation number: 8-digit limit reached.");
        }
        String id = String.format("%08d", next);
        while (idExistsInFile(GUESTS_FILE, id)) {
            next++;
            if (next > 99999999) {
                throw new IllegalStateException("Cannot generate confirmation number: 8-digit limit reached.");
            }
            id = String.format("%08d", next);
        }
        return id;
    }

    private String generateNextBookingId() {
        int max = findMaxPrefixedId(BOOKINGS_FILE, "B");
        return String.format("B%04d", max + 1);
    }

    private String generateNextBillId() {
        int max = findMaxPrefixedId(BILLING_FILE, "BL");
        return String.format("BL%04d", max + 1);
    }

    private int findMaxPrefixedId(String fileName, String prefix) {
        int max = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length == 0 || !parts[0].startsWith(prefix)) continue;
                try {
                    int value = Integer.parseInt(parts[0].substring(prefix.length()));
                    if (value > max) max = value;
                } catch (NumberFormatException ignored) { }
            }
        } catch (IOException ignored) { }
        return max;
    }

    private boolean idExistsInFile(String fileName, String id) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length > 0 && parts[0].trim().equals(id)) return true;
            }
        } catch (IOException ignored) { }
        return false;
    }

    private String[] findBookingById(String bookingId) {
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length >= 9 && parts[0].trim().equalsIgnoreCase(bookingId)) {
                    String[] result = new String[9];
                    for (int i = 0; i < 9; i++) result[i] = parts[i].trim();
                    return result;
                }
            }
        } catch (IOException ignored) { }
        return null;
    }

    // ------------------------------------------------------------------
    // General helpers
    // ------------------------------------------------------------------

    private boolean isRoomAvailable(Room room) {
        return room != null
                && "Vacant".equalsIgnoreCase(room.getOccupancyStatus())
                && "Ready".equalsIgnoreCase(room.getCleanlinessStatus());
    }

    private int tierScore(String tier) {
        if (tier == null) return 0;
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND": return 5;
            case "ELITE": return 4;
            case "PLATINUM": return 3;
            case "GOLD": return 2;
            case "SILVER": return 1;
            default: return 0;
        }
    }

    private int tierRank(String tier) {
        int score = tierScore(tier);
        return score == 0 ? 99 : 6 - score;
    }

    private int tierIndex(String tier) {
        if (tier == null) return -1;
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND": return 0;
            case "ELITE": return 1;
            case "PLATINUM": return 2;
            case "GOLD": return 3;
            case "SILVER": return 4;
            default: return -1;
        }
    }

    private double getTierRate(String tier) {
        if (tier == null) return 399.00;
        switch (tier.trim().toUpperCase()) {
            case "DIAMOND": return 1099.00;
            case "ELITE": return 899.00;
            case "PLATINUM": return 699.00;
            case "GOLD": return 599.00;
            case "SILVER": return 399.00;
            default: return 399.00;
        }
    }

    private String inferTierFromVipBill(int nights, double amount) {
        if (nights <= 0) return "None";
        double rate = amount / nights;
        if (Math.abs(rate - 1099.00) < 0.01) return "Diamond";
        if (Math.abs(rate - 899.00) < 0.01) return "Elite";
        if (Math.abs(rate - 699.00) < 0.01) return "Platinum";
        if (Math.abs(rate - 599.00) < 0.01) return "Gold";
        if (Math.abs(rate - 399.00) < 0.01) return "Silver";
        return "None";
    }

    private String normaliseTier(String tier) {
        int index = tierIndex(tier);
        switch (index) {
            case 0: return "Diamond";
            case 1: return "Elite";
            case 2: return "Platinum";
            case 3: return "Gold";
            case 4: return "Silver";
            default: return "None";
        }
    }

    public String getTierForConfirmation(String confirmationNo) {
        Guest guest = findGuestByConfirmationNo(confirmationNo);
        return guest == null ? "None" : guest.getLoyaltyTier();
    }

    public String getNameForConfirmation(String confirmationNo) {
        Guest guest = findGuestByConfirmationNo(confirmationNo);
        return guest == null ? confirmationNo : guest.getName();
    }

    private String buildLogEntry(Guest guest, Room room) {
        return String.format("[ALLOCATED] %s (Tier: %s, Conf#: %s) -> Room %s (%s)",
                guest.getName(), guest.getLoyaltyTier(), guest.getConfirmationNo(),
                room.getRoomNumber(), room.getRoomType());
    }

    private String join(String[] parts, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private String[][] toBookingArray(ListInterface<String[]> list) {
        String[][] result = new String[list.getNumberOfEntries()][];
        for (int i = 1; i <= list.getNumberOfEntries(); i++) {
            result[i - 1] = list.getEntry(i);
        }
        return result;
    }

    public String[] getAllocationLog() {
        String[] result = new String[allocationLog.getNumberOfEntries()];
        for (int i = 1; i <= allocationLog.getNumberOfEntries(); i++) {
            result[i - 1] = allocationLog.getEntry(i);
        }
        return result;
    }

    public void reset() {
        vipQueue.clear();
        allRooms.clear();
        allocationLog.clear();
    }

    // ------------------------------------------------------------------
    // Result DTOs
    // ------------------------------------------------------------------

    public static class VipRevenueRow {
        private final String tier;
        private final int bookingCount;
        private final double revenue;

        public VipRevenueRow(String tier, int bookingCount, double revenue) {
            this.tier = tier;
            this.bookingCount = bookingCount;
            this.revenue = revenue;
        }

        public String getTier() { return tier; }
        public int getBookingCount() { return bookingCount; }
        public double getRevenue() { return revenue; }
    }

    public static class AllocationResult {
        public enum Kind { SUCCESS, MANUAL_NEEDED, FAILURE }

        private final Kind kind;
        private final Guest guest;
        private final Room room;
        private final ListInterface<Room> availableRooms;
        private final String message;

        private AllocationResult(Kind kind, Guest guest, Room room,
                ListInterface<Room> availableRooms, String message) {
            this.kind = kind;
            this.guest = guest;
            this.room = room;
            this.availableRooms = availableRooms;
            this.message = message;
        }

        static AllocationResult success(Guest guest, Room room, String message) {
            return new AllocationResult(Kind.SUCCESS, guest, room, null, message);
        }

        static AllocationResult manualNeeded(Guest guest,
                ListInterface<Room> availableRooms, String message) {
            return new AllocationResult(Kind.MANUAL_NEEDED, guest, null,
                    availableRooms, message);
        }

        static AllocationResult failure(String message) {
            return new AllocationResult(Kind.FAILURE, null, null, null, message);
        }

        public boolean isSuccess() { return kind == Kind.SUCCESS; }
        public boolean isManualNeeded() { return kind == Kind.MANUAL_NEEDED; }
        public Guest getGuest() { return guest; }
        public Room getRoom() { return room; }
        public ListInterface<Room> getAvailableRooms() { return availableRooms; }
        public String getMessage() { return message; }
    }
}
