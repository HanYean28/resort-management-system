package control;

import adt.ArrayPriorityQueue;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Module 2 — VIP & Loyalty Tier Priority Room Allocation.
 *
 * Reads from and writes to:
 *   guests.txt   — confirmationNo|name|phone|loyaltyTier
 *   rooms.txt    — roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes
 *   bookings.txt — bookingId|confirmationNo|bookingType|requestedRoomType|checkInDate|checkOutDate|status|assignedRoomNumber|createdAt
 *
 * @author Lim How Voon
 */
public class VIPRoomAllocation {

    // -------------------------------------------------------
    // File paths
    // -------------------------------------------------------

    private static final String GUESTS_FILE   = "guests.txt";
    private static final String ROOMS_FILE    = "rooms.txt";
    private static final String BOOKINGS_FILE = "bookings.txt";
    private static final String BILLING_FILE  = "billing.txt";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Nightly rates per loyalty tier.
    private static final Map<String, Double> TIER_RATES = new HashMap<>();
    static {
        TIER_RATES.put("DIAMOND",  1099.00);
        TIER_RATES.put("ELITE",     899.00);
        TIER_RATES.put("PLATINUM",  699.00);
        TIER_RATES.put("GOLD",      599.00);
        TIER_RATES.put("SILVER",    399.00);
    }

    // -------------------------------------------------------
    // Fields
    // -------------------------------------------------------

    /** Priority queue — highest-tier guest always at the front. */
    private ArrayPriorityQueue vipQueue;

    /** All rooms loaded from rooms.txt (full list, not just available). */
    private List<Room> allRooms;

    /** Record of every (guest, room) pair that has been assigned. */
    private List<String> allocationLog;

    /** Maps confirmationNo → requested room type for each VIP guest. */
    private Map<String, String> requestedRoomTypes;

    /** Maps confirmationNo → check-in date (yyyy-MM-dd). */
    private Map<String, String> checkInDates;

    /** Maps confirmationNo → check-out date (yyyy-MM-dd). */
    private Map<String, String> checkOutDates;

    // -------------------------------------------------------
    // Constructor
    // -------------------------------------------------------

    /**
     * Initialises the controller and immediately loads
     * guests and rooms from their respective txt files.
     */
    public VIPRoomAllocation() {
        vipQueue           = new ArrayPriorityQueue();
        allRooms           = new ArrayList<>();
        allocationLog      = new ArrayList<>();
        requestedRoomTypes = new HashMap<>();
        checkInDates       = new HashMap<>();
        checkOutDates      = new HashMap<>();

        loadGuestsFromFile();
        loadRoomsFromFile();
    }

    // -------------------------------------------------------
    // File loaders
    // -------------------------------------------------------

    /**
     * Reads guests.txt and adds all VIP guests (tier != NONE)
     * to the priority queue.
     *
     * File format (4 fields):
     *   confirmationNo|name|phone|loyaltyTier
     *
     * Skips duplicate confirmation numbers already in the queue.
     * Normalises loyalty tier to title case (e.g. "PLAtinum" → "Platinum").
     */
    private void loadGuestsFromFile() {

        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length < 4) {
                    continue;
                }

                String confirmationNo = parts[0].trim();
                String name           = parts[1].trim();
                String phone          = parts[2].trim();
                String loyaltyTier    = normaliseTier(parts[3].trim());

                // Skip non-VIP guests.
                if (loyaltyTier.equalsIgnoreCase("NONE")) {
                    continue;
                }

                // Skip duplicates already in the queue.
                if (vipQueue.find(confirmationNo) != null) {
                    continue;
                }

                Guest guest = new Guest(confirmationNo, name, phone, loyaltyTier);
                vipQueue.add(guest);
            }

            System.out.println("[VIP] Guests loaded from " + GUESTS_FILE);

        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + GUESTS_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Reads rooms.txt and loads all rooms into allRooms.
     *
     * File format (7 fields):
     *   roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes
     *
     * Available rooms are those that are Vacant AND Ready.
     */
    private void loadRoomsFromFile() {

        allRooms.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length < 7) {
                    continue;
                }

                String roomNumber            = parts[0].trim();
                String roomType              = parts[1].trim();
                String cleanlinessStatus     = parts[2].trim();
                String occupancyStatus       = parts[3].trim();
                String lastUpdate            = parts[4].trim();
                String dirtySince            = parts[5].trim();
                String lastTurnaroundMinutes = parts[6].trim();

                Room room = new Room(
                        roomNumber,
                        roomType,
                        cleanlinessStatus,
                        occupancyStatus,
                        lastUpdate,
                        dirtySince,
                        lastTurnaroundMinutes
                );

                allRooms.add(room);
            }

        } catch (IOException e) {
            System.out.println("[VIP] Could not read " + ROOMS_FILE + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // File savers
    // -------------------------------------------------------

    /**
     * Rewrites guests.txt with all guests currently in the queue
     * plus any non-VIP guests that were skipped on load.
     *
     * Format: confirmationNo|name|phone|loyaltyTier
     */
    public void saveGuestsToFile() {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(GUESTS_FILE))) {

            bw.write("# confirmationNo|name|phone|loyaltyTier");
            bw.newLine();

            Guest[] all = vipQueue.getAll();

            for (Guest g : all) {
                if (g == null) continue;
                bw.write(g.getConfirmationNo() + "|"
                        + g.getName()          + "|"
                        + g.getPhone()         + "|"
                        + g.getLoyaltyTier());
                bw.newLine();
            }

        } catch (IOException e) {
            System.out.println("[VIP] Could not save " + GUESTS_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Rewrites rooms.txt with the current in-memory room list.
     *
     * Format: roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes
     */
    public void saveRoomsToFile() {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ROOMS_FILE))) {

            bw.write("# roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes");
            bw.newLine();

            for (Room r : allRooms) {
                if (r == null) continue;
                bw.write(r.getRoomNumber()            + "|"
                        + r.getRoomType()             + "|"
                        + r.getCleanlinessStatus()    + "|"
                        + r.getOccupancyStatus()      + "|"
                        + r.getLastUpdate()           + "|"
                        + r.getDirtySince()           + "|"
                        + r.getLastTurnaroundMinutes());
                bw.newLine();
            }

        } catch (IOException e) {
            System.out.println("[VIP] Could not save " + ROOMS_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Appends a new booking record to bookings.txt when a room
     * is allocated to a VIP guest.
     *
     * Uses the stored checkInDate and checkOutDate for the guest.
     */
    private void saveBookingToFile(Guest guest, Room room) {

        String bookingId = null;
        String checkIn = checkInDates.getOrDefault(guest.getConfirmationNo(), "N/A");
        String checkOut = checkOutDates.getOrDefault(guest.getConfirmationNo(), "N/A");
        List<String> lines = new ArrayList<>();
        boolean updated = false;

        // A VIP booking is created as Pending first. Allocation must update that
        // same booking to Assigned instead of creating a second booking record.
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();

                if (!updated && !trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    String[] parts = trimmed.split("\\|", -1);

                    if (parts.length >= 9
                            && parts[1].trim().equalsIgnoreCase(guest.getConfirmationNo())
                            && parts[2].trim().equalsIgnoreCase("VIP")
                            && parts[6].trim().equalsIgnoreCase("Pending")) {

                        bookingId = parts[0].trim();
                        parts[6] = "Assigned";
                        parts[7] = room.getRoomNumber();
                        line = String.join("|", parts);
                        updated = true;
                    }
                }

                lines.add(line);
            }
        } catch (IOException ignored) {
            // Fall back to creating an Assigned record below for old data/workflows.
        }

        if (updated) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE))) {
                for (String line : lines) {
                    bw.write(line);
                    bw.newLine();
                }
            } catch (IOException e) {
                System.out.println("[VIP] Could not update " + BOOKINGS_FILE
                        + ": " + e.getMessage());
                return;
            }
        } else {
            // Backward-compatible fallback if no Pending record exists.
            bookingId = generateNextBookingId();
            String createdAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE, true))) {
                bw.write(bookingId + "|"
                        + guest.getConfirmationNo() + "|"
                        + "VIP|"
                        + room.getRoomType() + "|"
                        + checkIn + "|"
                        + checkOut + "|"
                        + "Assigned|"
                        + room.getRoomNumber() + "|"
                        + createdAt);
                bw.newLine();
            } catch (IOException e) {
                System.out.println("[VIP] Could not append to " + BOOKINGS_FILE
                        + ": " + e.getMessage());
                return;
            }
        }

        // Generate billing only after the room has actually been assigned.
        saveBillingToFile(guest, room, bookingId, checkIn, checkOut);
    }

    /**
     * Appends a billing record to billing.txt.
     *
     * Calculates nights from checkIn/checkOut dates.
     * Rate is determined by the guest's loyalty tier.
     *
     * File format:
     *   billId|bookingId|confirmationNo|roomNumber|roomType
     *   |checkInDate|checkOutDate|nights|amount|paymentStatus|createdAt
     */
    private void saveBillingToFile(Guest guest, Room room,
            String bookingId, String checkIn, String checkOut) {

        String billId    = generateNextBillId();
        String createdAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        long nights = 1; // default if dates are invalid

        try {
            LocalDate in  = LocalDate.parse(checkIn);
            LocalDate out = LocalDate.parse(checkOut);
            long computed = ChronoUnit.DAYS.between(in, out);
            if (computed > 0) nights = computed;
        } catch (Exception ignored) {}

        double rate   = TIER_RATES.getOrDefault(
                guest.getLoyaltyTier().toUpperCase(), 399.00);
        double amount = nights * rate;

        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(BILLING_FILE, true))) {

            bw.write(billId                      + "|"
                    + bookingId                   + "|"
                    + guest.getConfirmationNo()   + "|"
                    + room.getRoomNumber()         + "|"
                    + room.getRoomType()           + "|"
                    + checkIn                      + "|"
                    + checkOut                     + "|"
                    + nights                       + "|"
                    + String.format("%.2f", amount) + "|"
                    + "Paid"                       + "|"
                    + createdAt);
            bw.newLine();

        } catch (IOException e) {
            System.out.println("[VIP] Could not append to " + BILLING_FILE
                    + ": " + e.getMessage());
        }
    }

    /**
     * Generates the next bill ID (BL####) by reading the highest
     * existing BL-prefixed ID from billing.txt and incrementing it.
     */
    private String generateNextBillId() {

        int max = 0;

        try (BufferedReader br = new BufferedReader(
                new FileReader(BILLING_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length > 0 && parts[0].startsWith("BL")) {
                    try {
                        int num = Integer.parseInt(parts[0].substring(2));
                        if (num > max) max = num;
                    } catch (NumberFormatException ignored) {}
                }
            }

        } catch (IOException ignored) {}

        return String.format("BL%04d", max + 1);
    }

    /**
     * Generates the next booking ID by reading the highest existing
     * B-prefixed ID from bookings.txt and incrementing it.
     */
    private String generateNextBookingId() {

        int max = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length > 0 && parts[0].startsWith("B")) {
                    try {
                        int num = Integer.parseInt(parts[0].substring(1));
                        if (num > max) max = num;
                    } catch (NumberFormatException ignored) {}
                }
            }

        } catch (IOException ignored) {}

        return String.format("B%04d", max + 1);
    }

    // -------------------------------------------------------
    // Guest queue management
    // -------------------------------------------------------

    /**
     * Adds a VIP guest to the priority queue and saves to guests.txt.
     *
     * @param guest the VIP guest to enqueue
     * @throws IllegalArgumentException if guest is null
     */
    public void addGuest(Guest guest) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }

        vipQueue.add(guest);
        saveGuestsToFile();
    }

    /**
     * Creates a Pending VIP booking for an existing VIP guest.
     *
     * The guest is NOT added to vipQueue again because the guest is already
     * registered in the queue by addGuest(Guest). This method only stores the
     * booking preferences and appends the Pending booking to bookings.txt.
     *
     * @return null when successful, otherwise an error message
     */
    public String createPendingVipBooking(Guest guest, String requestedRoomType,
            String checkInDate, String checkOutDate) {

        if (guest == null) {
            return "Guest cannot be null.";
        }

        if (requestedRoomType == null || requestedRoomType.trim().isEmpty()
                || checkInDate == null || checkInDate.trim().isEmpty()
                || checkOutDate == null || checkOutDate.trim().isEmpty()) {
            return "Booking details cannot be empty.";
        }

        // The current VIP design stores one set of booking preferences per guest,
        // so prevent another active Pending/Assigned booking for the same guest.
        if (hasActiveVipBooking(guest.getConfirmationNo())) {
            return "Guest already has an active VIP booking.";
        }

        requestedRoomTypes.put(guest.getConfirmationNo(), requestedRoomType.trim());
        checkInDates.put(guest.getConfirmationNo(), checkInDate.trim());
        checkOutDates.put(guest.getConfirmationNo(), checkOutDate.trim());

        String bookingId = generateNextBookingId();
        String createdAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(BOOKINGS_FILE, true))) {
            bw.write(bookingId + "|"
                    + guest.getConfirmationNo() + "|"
                    + "VIP|"
                    + requestedRoomType.trim() + "|"
                    + checkInDate.trim() + "|"
                    + checkOutDate.trim() + "|"
                    + "Pending|"
                    + "N/A|"
                    + createdAt);
            bw.newLine();
        } catch (IOException e) {
            return "Could not save VIP booking: " + e.getMessage();
        }

        return null;
    }

    /** Returns true if the guest already has a Pending or Assigned VIP booking. */
    private boolean hasActiveVipBooking(String confirmationNo) {
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 9) continue;

                boolean sameGuest = parts[1].trim().equalsIgnoreCase(confirmationNo);
                boolean vip = parts[2].trim().equalsIgnoreCase("VIP");
                String status = parts[6].trim();
                boolean active = status.equalsIgnoreCase("Pending")
                        || status.equalsIgnoreCase("Assigned");

                if (sameGuest && vip && active) return true;
            }
        } catch (IOException ignored) {
            // If the file does not exist yet, there is no active booking to block.
        }
        return false;
    }

    /**
     * Returns the check-in date for a given confirmation number.
     */
    public String getCheckInDate(String confirmationNo) {
        return checkInDates.getOrDefault(confirmationNo, "N/A");
    }

    /**
     * Returns the check-out date for a given confirmation number.
     */
    public String getCheckOutDate(String confirmationNo) {
        return checkOutDates.getOrDefault(confirmationNo, "N/A");
    }

    /**
     * Returns the requested room type for a given confirmation number.
     * Returns null if no preference was recorded.
     */
    public String getRequestedRoomType(String confirmationNo) {
        return requestedRoomTypes.get(confirmationNo);
    }

    /**
     * Allocates a specific room (by room number) to the given guest
     * and removes that guest from the queue.
     *
     * Used for manual allocation when auto-match fails.
     */
    public AllocationResult allocateRoom(Guest guest, String roomNumber) {

        if (guest == null || roomNumber == null) {
            return AllocationResult.failure("Invalid guest or room number.");
        }

        // Find the room in allRooms by number.
        Room target = null;
        for (Room r : allRooms) {
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && isRoomAvailable(r)) {
                target = r;
                break;
            }
        }

        if (target == null) {
            return AllocationResult.failure(
                    "Room " + roomNumber + " is not available.");
        }

        // Remove guest from queue.
        vipQueue.removeByConfirmationNo(guest.getConfirmationNo());

        // Room stays Vacant — it becomes Occupied only when guest checks in.
        // Just update lastUpdate to record when the assignment happened.
        target.setLastUpdate(LocalDateTime.now().format(TIMESTAMP_FORMAT));

        // Persist.
        saveRoomsToFile();
        saveGuestsToFile();
        saveBookingToFile(guest, target);

        String entry = buildLogEntry(guest, target);
        allocationLog.add(entry);

        return AllocationResult.success(guest, target, entry);
    }

    /**
     * Returns the highest-priority guest without removing them.
     */
    public Guest peekNextGuest() {
        return vipQueue.peek();
    }

    /**
     * Removes a specific guest from the queue by confirmation number
     * and saves the updated guest list to guests.txt.
     */
    public boolean removeGuestFromQueue(String confirmationNo) {

        boolean removed = vipQueue.removeByConfirmationNo(confirmationNo);

        if (removed) {
            saveGuestsToFile();
        }

        return removed;
    }

    /**
     * Finds a guest in the queue by confirmation number without removing them.
     */
    public Guest findGuestInQueue(String confirmationNo) {
        return vipQueue.find(confirmationNo);
    }

    /**
     * Returns all guests currently waiting, ordered highest-tier first.
     */
    public Guest[] getAllWaitingGuests() {
        return vipQueue.getAll();
    }

    /** Returns the number of guests currently waiting in the VIP queue. */
    public int getQueueSize() {
        return vipQueue.size();
    }

    /** Returns true if no guests are waiting. */
    public boolean isQueueEmpty() {
        return vipQueue.isEmpty();
    }

    // -------------------------------------------------------
    // Room pool management
    // -------------------------------------------------------

    /**
     * Returns all rooms that are currently Vacant and Ready.
     * Derived live from allRooms so it always reflects the latest state.
     */
    public List<Room> getAvailableRooms() {

        List<Room> available = new ArrayList<>();

        for (Room r : allRooms) {
            if (isRoomAvailable(r)) {
                available.add(r);
            }
        }

        return available;
    }

    /** Returns the count of currently available rooms. */
    public int getAvailableRoomCount() {
        return getAvailableRooms().size();
    }

    /**
     * Manually adds a room to the in-memory room list and saves to rooms.txt.
     * Used by the sample data loader and the UI.
     */
    public void addAvailableRoom(Room room) {

        if (room == null) {
            return;
        }

        allRooms.add(room);
        saveRoomsToFile();
    }

    // -------------------------------------------------------
    // Allocation
    // -------------------------------------------------------

    /**
     * Assigns the best available room to the highest-priority VIP guest.
     *
     * Auto-match logic:
     *   1. Check if the guest has a requested room type.
     *   2. If yes, find the first available room of that type.
     *   3. If no match, return a failure so the UI can prompt manual selection.
     *   4. If no preference, take the first available room.
     */
    /**
     * Allocates the next Pending VIP booking according to VIP priority.
     *
     * Priority order:
     *   1. Loyalty tier: Diamond, Elite, Platinum, Gold, Silver
     *   2. For guests in the same tier, keep their existing booking/queue order
     *
     * This method owns all VIP-specific decision logic so other controllers
     * only need to call this method.
     */
    /**
     * Returns true when at least one guest in the VIP priority queue has a
     * Pending VIP booking. BookingController uses this only to decide whether
     * the next allocation click must be handled by the VIP module first.
     */
    public boolean hasPendingVipBooking() {
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 9) {
                    continue;
                }

                if (parts[2].trim().equalsIgnoreCase("VIP")
                        && parts[6].trim().equalsIgnoreCase("Pending")
                        && vipQueue.find(parts[1].trim()) != null) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    public AllocationResult allocateNextPendingBooking() {

        if (vipQueue.isEmpty()) {
            return AllocationResult.failure("VIP queue is empty.");
        }

        // Always reload the shared room file before allocating so this controller
        // sees room changes made by other modules.
        loadRoomsFromFile();

        Guest selectedGuest = null;
        String[] selectedBooking = null;

        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 9) {
                    continue;
                }

                // Only Pending VIP bookings are eligible for VIP allocation.
                if (!parts[2].trim().equalsIgnoreCase("VIP")
                        || !parts[6].trim().equalsIgnoreCase("Pending")) {
                    continue;
                }

                Guest guest = vipQueue.find(parts[1].trim());
                if (guest == null) {
                    continue;
                }

                if (selectedGuest == null
                        || isHigherVipBookingPriority(guest, selectedGuest)) {
                    selectedGuest = guest;
                    selectedBooking = parts;
                }
            }

        } catch (IOException e) {
            return AllocationResult.failure(
                    "Could not read VIP bookings: " + e.getMessage());
        }

        if (selectedGuest == null || selectedBooking == null) {
            return AllocationResult.failure("No pending VIP booking found.");
        }

        String requestedType = selectedBooking[3].trim();
        String checkIn = selectedBooking[4].trim();
        String checkOut = selectedBooking[5].trim();

        // Restore the selected booking preferences into the VIP controller maps.
        // This is important when BookingController creates a fresh VIP controller
        // after the program has been restarted.
        requestedRoomTypes.put(selectedGuest.getConfirmationNo(), requestedType);
        checkInDates.put(selectedGuest.getConfirmationNo(), checkIn);
        checkOutDates.put(selectedGuest.getConfirmationNo(), checkOut);

        List<Room> available = getAvailableRooms();
        if (available.isEmpty()) {
            return AllocationResult.failure("No rooms are currently available.");
        }

        Room matched = null;
        for (Room room : available) {
            if (room.getRoomType().equalsIgnoreCase(requestedType)
                    && !hasRoomDateClash(room.getRoomNumber(), selectedBooking)) {
                matched = room;
                break;
            }
        }

        if (matched == null) {
            return AllocationResult.failure(
                    "No available room of type '" + requestedType
                    + "' for " + selectedGuest.getName() + ".");
        }

        // Remove only the selected Pending VIP guest from the priority queue.
        vipQueue.removeByConfirmationNo(selectedGuest.getConfirmationNo());

        // Room stays Vacant until check-in. The booking itself reserves the room
        // for its date range, and hasRoomDateClash prevents double-booking.
        matched.setLastUpdate(LocalDateTime.now().format(TIMESTAMP_FORMAT));

        saveRoomsToFile();
        saveGuestsToFile();
        saveBookingToFile(selectedGuest, matched);

        String entry = buildLogEntry(selectedGuest, matched);
        allocationLog.add(entry);

        return AllocationResult.success(selectedGuest, matched, entry);
    }

    /**
     * Backward-compatible entry point used by existing VIP UI code.
     * Allocation is now restricted to guests with a Pending VIP booking.
     */
    public AllocationResult allocateNextRoom() {
        return allocateNextPendingBooking();
    }

    /**
     * Returns true only when the candidate has a higher loyalty tier.
     * Guests in the same tier keep their existing booking/queue order.
     */
    private boolean isHigherVipBookingPriority(Guest candidate, Guest current) {

        int candidateTier = getTierPriority(candidate.getLoyaltyTier());
        int currentTier = getTierPriority(current.getLoyaltyTier());

        return candidateTier < currentTier;
    }

    private int getTierPriority(String tier) {
        if (tier == null) return 99;

        switch (tier.trim().toUpperCase()) {
            case "DIAMOND":  return 1;
            case "ELITE":    return 2;
            case "PLATINUM": return 3;
            case "GOLD":     return 4;
            case "SILVER":   return 5;
            default:         return 99;
        }
    }

    /** Parses a booking date for room-overlap checking only. */
    private LocalDate parseBookingDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return LocalDate.MAX;
        }
    }

    /**
     * Checks whether a room is already reserved for an overlapping Assigned or
     * Checked-In booking. Pending bookings do not reserve a room yet.
     */
    private boolean hasRoomDateClash(String roomNumber, String[] targetBooking) {
        LocalDate targetIn = parseBookingDate(targetBooking[4]);
        LocalDate targetOut = parseBookingDate(targetBooking[5]);

        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length < 9) {
                    continue;
                }

                String status = parts[6].trim();
                boolean reservesRoom = status.equalsIgnoreCase("Assigned")
                        || status.equalsIgnoreCase("Checked In");

                if (!reservesRoom
                        || !parts[7].trim().equalsIgnoreCase(roomNumber)
                        || parts[0].trim().equalsIgnoreCase(targetBooking[0].trim())) {
                    continue;
                }

                LocalDate existingIn = parseBookingDate(parts[4]);
                LocalDate existingOut = parseBookingDate(parts[5]);

                if (targetIn.isBefore(existingOut) && targetOut.isAfter(existingIn)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // If the file cannot be read here, normal allocation error handling
            // will still occur when the booking is persisted.
        }

        return false;
    }

    /**
     * Processes the entire queue, allocating rooms one by one until
     * either the queue or the available room pool is exhausted.
     */
    public List<AllocationResult> allocateAll() {

        List<AllocationResult> results = new ArrayList<>();

        while (!vipQueue.isEmpty() && !getAvailableRooms().isEmpty()) {
            AllocationResult result = allocateNextPendingBooking();
            results.add(result);
            if (!result.isSuccess()) {
                break;
            }
        }

        // Report remaining guests who could not be allocated.
        if (!vipQueue.isEmpty()) {
            Guest[] remaining = vipQueue.getAll();
            for (Guest g : remaining) {
                if (g != null) {
                    results.add(AllocationResult.failure(
                            "No room available for " + g.getName()
                            + " [" + g.getLoyaltyTier() + "]"
                    ));
                }
            }
        }

        return results;
    }

    /** Returns the full allocation log. */
    public List<String> getAllocationLog() {
        return new ArrayList<>(allocationLog);
    }

    /**
     * Purpose:
     * Generates a unique 8-digit confirmation number by finding
     * the highest existing numeric confirmation number in guests.txt
     * and incrementing it by 1.
     */
    public String generateConfirmationNo() {

        int max = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(GUESTS_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");

                if (parts.length >= 1) {
                    try {
                        int num = Integer.parseInt(parts[0].trim());
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

        } catch (IOException ignored) {}

        return String.format("%08d", max + 1);
    }

    /**
     * Clears the queue, room list, and allocation log.
     * Does NOT touch the txt files.
     */
    public void reset() {
        vipQueue.clear();
        allRooms.clear();
        allocationLog.clear();
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    /**
     * A room is available if it is Vacant AND has cleanliness "Ready".
     * Matches the actual value used in rooms.txt.
     */
    private boolean isRoomAvailable(Room room) {

        return room != null
                && "Vacant".equalsIgnoreCase(room.getOccupancyStatus())
                && "Ready".equalsIgnoreCase(room.getCleanlinessStatus());
    }

    /**
     * Normalises a loyalty tier string to title case.
     * Handles mixed-case inputs like "PLAtinum" or "DIAMOND".
     */
    private String normaliseTier(String tier) {

        if (tier == null || tier.isEmpty()) {
            return "None";
        }

        String upper = tier.toUpperCase();

        switch (upper) {
            case "DIAMOND":  return "Diamond";
            case "ELITE":    return "Elite";
            case "PLATINUM": return "Platinum";
            case "GOLD":     return "Gold";
            case "SILVER":   return "Silver";
            default:         return "None";
        }
    }

    /** Builds a human-readable log entry for one allocation. */
    private String buildLogEntry(Guest guest, Room room) {

        return String.format(
                "[ALLOCATED] %s (Tier: %s, Conf#: %s) → Room %s (%s)",
                guest.getName(),
                guest.getLoyaltyTier(),
                guest.getConfirmationNo(),
                room.getRoomNumber(),
                room.getRoomType()
        );
    }

    // -------------------------------------------------------
    // Inner class — AllocationResult
    // -------------------------------------------------------

    /**
     * Immutable result object returned by allocateNextRoom() and allocateAll().
     */
    public static class AllocationResult {

        private final boolean success;
        private final Guest   guest;
        private final Room    room;
        private final String  message;

        private AllocationResult(boolean success, Guest guest, Room room, String message) {
            this.success = success;
            this.guest   = guest;
            this.room    = room;
            this.message = message;
        }

        static AllocationResult success(Guest guest, Room room, String message) {
            return new AllocationResult(true, guest, room, message);
        }

        static AllocationResult failure(String message) {
            return new AllocationResult(false, null, null, message);
        }

        public boolean isSuccess()  { return success; }
        public Guest   getGuest()   { return guest;   }
        public Room    getRoom()    { return room;     }
        public String  getMessage() { return message;  }
    }
}