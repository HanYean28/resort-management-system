package control;

import adt.ArrayPriorityQueue;
import adt.ListInterface;
import dao.BookingDAO;
import dao.GuestDAO;
import dao.RoomDAO;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    private GuestDAO guestDAO;
    private RoomDAO roomDAO;
    private BookingDAO bookingDAO;

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
        guestDAO           = new GuestDAO();
        roomDAO            = new RoomDAO();
        bookingDAO         = new BookingDAO();

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

        ListInterface<Guest> guests = guestDAO.loadGuests();

        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {

            Guest guest = guests.getEntry(i);
            guest.setLoyaltyTier(normaliseTier(guest.getLoyaltyTier()));

            // Skip non-VIP guests.
            if (guest.getLoyaltyTier().equalsIgnoreCase("NONE")) {
                continue;
            }

            // Skip duplicates already in the queue.
            if (vipQueue.find(guest.getConfirmationNo()) != null) {
                continue;
            }

            vipQueue.add(guest);
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
        ListInterface<Room> rooms = roomDAO.loadRooms();

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            allRooms.add(rooms.getEntry(i));
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

        ListInterface<Guest> guests = guestDAO.loadGuests();
        Guest[] all = vipQueue.getAll();

        for (Guest guest : all) {
            if (guest != null) {
                saveOrReplaceGuest(guests, guest);
            }
        }

        guestDAO.saveGuests(guests);
    }

    private void saveOrReplaceGuest(ListInterface<Guest> guests, Guest guest) {
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            if (guests.getEntry(i).getConfirmationNo().equalsIgnoreCase(guest.getConfirmationNo())) {
                guests.replace(i, guest);
                return;
            }
        }
        guests.add(guest);
    }

    /**
     * Rewrites rooms.txt with the current in-memory room list.
     *
     * Format: roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes
     */
    public void saveRoomsToFile() {

        ListInterface<Room> rooms = new adt.ArrayList<>();

        for (Room r : allRooms) {
            if (r == null) continue;
            rooms.add(r);
        }

        roomDAO.saveRooms(rooms);
    }

    /**
     * Appends a new booking record to bookings.txt when a room
     * is allocated to a VIP guest.
     *
     * Format: bookingId|confirmationNo|bookingType|requestedRoomType|checkInDate|checkOutDate|status|assignedRoomNumber|createdAt
     */
    private void saveBookingToFile(Guest guest, Room room) {

        String bookingId  = generateNextBookingId();
        String createdAt  = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        ListInterface<BookingRequest> bookings = bookingDAO.loadBookings();
        BookingRequest booking = new BookingRequest(
                bookingId,
                guest.getConfirmationNo(),
                "VIP",
                room.getRoomType(),
                "N/A",
                "N/A",
                "Assigned",
                room.getRoomNumber(),
                createdAt);
        bookings.add(booking);
        bookingDAO.saveBookings(bookings);
    }

    /**
     * Generates the next booking ID by reading the highest existing
     * B-prefixed ID from bookings.txt and incrementing it.
     */
    private String generateNextBookingId() {

        int max = 0;
        ListInterface<BookingRequest> bookings = bookingDAO.loadBookings();

        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            String id = bookings.getEntry(i).getBookingId();
            if (id.startsWith("B")) {
                try {
                    int num = Integer.parseInt(id.substring(1));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }

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
     * Adds a VIP guest with their requested room type.
     * The requested room type is stored separately and used
     * during allocation to auto-match a suitable room.
     */
    public void addGuest(Guest guest, String requestedRoomType) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }

        vipQueue.add(guest);

        if (requestedRoomType != null && !requestedRoomType.trim().isEmpty()) {
            requestedRoomTypes.put(guest.getConfirmationNo(), requestedRoomType.trim());
        }

        saveGuestsToFile();
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

        // Update room status.
        target.setOccupancyStatus("Occupied");
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
    public AllocationResult allocateNextRoom() {

        if (vipQueue.isEmpty()) {
            return AllocationResult.failure("No VIP guests are currently waiting.");
        }

        List<Room> available = getAvailableRooms();

        if (available.isEmpty()) {
            return AllocationResult.failure("No rooms are currently available.");
        }

        Guest guest = vipQueue.peek();
        String requestedType = requestedRoomTypes.get(guest.getConfirmationNo());

        Room matched = null;

        if (requestedType != null) {
            // Try to find a room matching the requested type.
            for (Room r : available) {
                if (r.getRoomType().equalsIgnoreCase(requestedType)) {
                    matched = r;
                    break;
                }
            }

            // No room of requested type available — signal manual selection needed.
            if (matched == null) {
                return AllocationResult.failure(
                        "No available room of type '" + requestedType
                        + "' for " + guest.getName() + ". Please select manually.");
            }
        } else {
            // No preference — take first available.
            matched = available.get(0);
        }

        // Remove guest from queue.
        vipQueue.remove();

        // Update room status.
        matched.setOccupancyStatus("Occupied");
        matched.setLastUpdate(LocalDateTime.now().format(TIMESTAMP_FORMAT));

        // Persist.
        saveRoomsToFile();
        saveGuestsToFile();
        saveBookingToFile(guest, matched);

        String entry = buildLogEntry(guest, matched);
        allocationLog.add(entry);

        return AllocationResult.success(guest, matched, entry);
    }

    /**
     * Processes the entire queue, allocating rooms one by one until
     * either the queue or the available room pool is exhausted.
     */
    public List<AllocationResult> allocateAll() {

        List<AllocationResult> results = new ArrayList<>();

        while (!vipQueue.isEmpty() && !getAvailableRooms().isEmpty()) {
            results.add(allocateNextRoom());
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
        ListInterface<Guest> guests = guestDAO.loadGuests();

        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            try {
                int num = Integer.parseInt(guests.getEntry(i).getConfirmationNo());
                if (num > max) {
                    max = num;
                }
            } catch (NumberFormatException ignored) {}
        }

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
