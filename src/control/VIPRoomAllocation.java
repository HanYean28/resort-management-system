package control;

import adt.ArrayList;
import adt.ArrayPriorityQueue;
import adt.ListInterface;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Controller for VIP Room Allocation.
 *
 * Responsibilities:
 * - Load guest data from guests.txt
 * - Load room data from Room.txt
 * - Delegate all booking data operations to BookingController
 * - Add VIP guests to the VIP priority queue
 * - Search and remove VIP guests
 * - Display available rooms
 * - Allocate rooms to the highest-priority VIP guest
 *
 * Priority ordering:
 *   1. Loyalty tier  (Diamond > Elite > Platinum > Gold > Silver)
 *   2. Booking createdAt (earlier booking wins within same tier)
 */
public class VIPRoomAllocation {

    private ArrayPriorityQueue   vipQueue;
    private ArrayList<Guest>     guests;
    private ArrayList<Room>      rooms;
    private BookingController    bookingController;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Purpose:
     * Initializes the VIP room allocation module.
     *
     * BookingController is reused here so bookings.txt is
     * managed by one controller only — no duplicate reads
     * or writes.
     */
    public VIPRoomAllocation() {

        vipQueue          = new ArrayPriorityQueue();
        guests            = new ArrayList<>();
        rooms             = new ArrayList<>();
        bookingController = new BookingController();

        loadGuestData();
        loadRoomData();
    }

    // -------------------------------------------------------
    // File loaders
    // -------------------------------------------------------

    /**
     * Purpose:
     * Loads guest information from guests.txt.
     *
     * VIP guests (loyalty tier != NONE) are added to
     * the VIP priority queue.
     */
    private void loadGuestData() {

        try (BufferedReader reader =
                new BufferedReader(new FileReader("guests.txt"))) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")
                        || line.startsWith("confirmationNo")) {
                    continue;
                }

                String[] data = line.split("\\|");

                if (data.length < 5) {
                    continue;
                }

                String confirmationNo = data[0].trim();
                String name           = data[1].trim();
                String phone          = data[2].trim();
                String loyaltyTier    = data[3].trim();
                double billingAmount  = Double.parseDouble(data[4].trim());

                Guest guest = new Guest(
                        confirmationNo,
                        name,
                        phone,
                        loyaltyTier,
                        billingAmount
                );

                addGuest(guest);
            }

            System.out.println("Guest data loaded successfully.");

        } catch (IOException e) {
            System.out.println("Error loading guests.txt: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid billing amount in guests.txt.");
        }
    }

    /**
     * Purpose:
     * Loads room information from Room.txt.
     * Room.txt has 7 fields including occupancyStatus.
     */
    private void loadRoomData() {

        try (BufferedReader reader =
                new BufferedReader(new FileReader("Room.txt"))) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")
                        || line.startsWith("roomNumber")) {
                    continue;
                }

                String[] data = line.split("\\|");

                if (data.length != 7) {
                    continue;
                }

                String roomNumber            = data[0].trim();
                String roomType              = data[1].trim();
                String cleanlinessStatus     = data[2].trim();
                String occupancyStatus       = data[3].trim();
                String lastUpdate            = data[4].trim();
                String dirtySince            = data[5].trim();
                String lastTurnaroundMinutes = data[6].trim();

                Room room = new Room(
                        roomNumber,
                        roomType,
                        cleanlinessStatus,
                        occupancyStatus,
                        lastUpdate,
                        dirtySince,
                        lastTurnaroundMinutes
                );

                addRoom(room);
            }

            System.out.println("Room data loaded successfully.");

        } catch (IOException e) {
            System.out.println("Error loading Room.txt: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Guest management
    // -------------------------------------------------------

    /**
     * Purpose:
     * Adds a guest to the general guest list.
     * VIP guests (tier != NONE) also enter the priority queue.
     */
    public void addGuest(Guest guest) {

        if (guest == null) {
            return;
        }

        guests.add(guest);

        if (isVIP(guest)) {
            vipQueue.add(guest);
        }
    }

    /**
     * Purpose:
     * Determines whether a guest qualifies as VIP.
     */
    private boolean isVIP(Guest guest) {

        if (guest.getLoyaltyTier() == null) {
            return false;
        }

        return !guest.getLoyaltyTier().equalsIgnoreCase("NONE");
    }

    /**
     * Purpose:
     * Adds a room into the room collection.
     */
    public void addRoom(Room room) {

        if (room == null) {
            return;
        }

        rooms.add(room);
    }

    // -------------------------------------------------------
    // Queue queries
    // -------------------------------------------------------

    /**
     * Purpose:
     * Returns the highest-priority VIP guest without
     * removing them from the queue.
     *
     * When two guests share the same tier, the one with
     * the earlier booking createdAt (from BookingController)
     * is returned.
     */
    public Guest getNextVIPGuest() {

        Guest[] sorted = getSortedWaitingList();

        if (sorted.length == 0) {
            return null;
        }

        return sorted[0];
    }

    /**
     * Purpose:
     * Returns all VIP guests in the waiting queue,
     * sorted by priority then by booking createdAt.
     */
    public Guest[] getWaitingList() {

        return getSortedWaitingList();
    }

    /**
     * Purpose:
     * Returns the number of VIP guests waiting.
     */
    public int getWaitingGuestCount() {

        return vipQueue.size();
    }

    /**
     * Purpose:
     * Checks whether there are no VIP guests waiting.
     */
    public boolean isWaitingListEmpty() {

        return vipQueue.isEmpty();
    }

    // -------------------------------------------------------
    // Room operations
    // -------------------------------------------------------

    /**
     * Purpose:
     * Returns all rooms whose cleanliness status is Ready.
     */
    public Room[] getAvailableRooms() {

        int count = 0;

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {

            Room room = rooms.getEntry(i);

            if (room != null
                    && "Ready".equalsIgnoreCase(
                            room.getCleanlinessStatus())) {
                count++;
            }
        }

        Room[] availableRooms = new Room[count];
        int index = 0;

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {

            Room room = rooms.getEntry(i);

            if (room != null
                    && "Ready".equalsIgnoreCase(
                            room.getCleanlinessStatus())) {
                availableRooms[index++] = room;
            }
        }

        return availableRooms;
    }

    /**
     * Purpose:
     * Allocates a selected room to the highest-priority
     * VIP guest (priority then createdAt tiebreaker).
     *
     * The guest is removed from the queue after allocation.
     */
    public Guest allocateRoom(String roomNumber) {

        Guest nextGuest = getNextVIPGuest();

        if (nextGuest == null) {
            return null;
        }

        Room selectedRoom = findAvailableRoom(roomNumber);

        if (selectedRoom == null) {
            return null;
        }

        // Remove this specific guest from the queue.
        vipQueue.removeByConfirmationNo(nextGuest.getConfirmationNo());

        String error = bookingController.assignRoomToBookingForGuest(
                nextGuest.getConfirmationNo(),
                selectedRoom.getRoomNumber()
        );
        if (error != null) {
            vipQueue.add(nextGuest);
            return null;
        }

        return nextGuest;
    }

    /**
     * Purpose:
     * Searches for a VIP guest by confirmation number.
     */
    public Guest findGuest(String confirmationNo) {

        return vipQueue.find(confirmationNo);
    }

    /**
     * Purpose:
     * Removes a VIP guest from the queue by confirmation number.
     */
    public boolean removeGuest(String confirmationNo) {

        return vipQueue.removeByConfirmationNo(confirmationNo);
    }

    // -------------------------------------------------------
    // Booking delegation — all booking data goes through
    // BookingController so there is one source of truth.
    // -------------------------------------------------------

    /**
     * Purpose:
     * Adds a new VIP guest and registers a standard booking
     * via BookingController.
     *
     * BookingController.addGuest() saves to guests.txt.
     * BookingController.addStandardBooking() saves to bookings.txt.
     * The guest is also added to the local VIP queue.
     */
    public String addVIPGuest(String name, String phone,
            String loyaltyTier, String requestedRoomType,
            String checkInDate, String checkOutDate) {

        // Register the guest through BookingController.
        Guest guest = bookingController.addGuest(name, phone);

        if (guest == null) {
            return "Failed to create guest. Name and phone cannot be empty.";
        }

        // Update the loyalty tier — BookingController creates guests
        // with NONE tier by default.
        guest.setLoyaltyTier(loyaltyTier);

        // Add to local VIP queue if tier qualifies.
        if (isVIP(guest)) {
            guests.add(guest);
            vipQueue.add(guest);
        }

        // Register the booking via BookingController.
        // This saves to bookings.txt automatically.
        String error = bookingController.addStandardBooking(
                guest.getConfirmationNo(),
                requestedRoomType,
                checkInDate,
                checkOutDate
        );

        return error; // null means success
    }

    /**
     * Purpose:
     * Returns the createdAt timestamp for a guest's booking
     * by delegating to BookingController.
     *
     * Used by the UI to display booking date and by the
     * tiebreaker logic in getSortedWaitingList().
     */
    public String getBookingCreatedAt(String confirmationNo) {

        if (confirmationNo == null) {
            return null;
        }

        // Ask BookingController for all bookings matching
        // this confirmation number.
        ListInterface<BookingRequest> all =
                bookingController.getBookingsByStatus(
                        BookingController.FILTER_ALL);

        for (int i = 1; i <= all.getNumberOfEntries(); i++) {

            BookingRequest booking = all.getEntry(i);

            if (booking != null
                    && confirmationNo.equals(
                            booking.getGuest().getConfirmationNo())) {
                return booking.getCreatedAt();
            }
        }

        return null;
    }

    public String getGuestCurrentRoom(String confirmationNo) {

        if (confirmationNo == null) {
            return "N/A";
        }

        ListInterface<BookingRequest> all =
                bookingController.getBookingsByStatus(
                        BookingController.FILTER_ALL);

        for (int i = 1; i <= all.getNumberOfEntries(); i++) {

            BookingRequest booking = all.getEntry(i);

            if (booking != null
                    && confirmationNo.equals(
                            booking.getGuest().getConfirmationNo())
                    && (BookingController.STATUS_ASSIGNED.equals(booking.getStatus())
                            || BookingController.STATUS_CHECKED_IN.equals(booking.getStatus()))) {
                return booking.getAssignedRoomNumber();
            }
        }

        return "N/A";
    }

    // -------------------------------------------------------
    // Persistence
    // -------------------------------------------------------

    /**
     * Purpose:
     * Saves the current in-memory guest list back to guests.txt.
     *
     * Called after VIP guest data changes that are not
     * handled by BookingController (e.g. room assignment).
     */
    private void saveGuestData() {

        try (BufferedWriter writer =
                new BufferedWriter(new FileWriter("guests.txt"))) {

            writer.write(
                    "# confirmationNo|name|phone|loyaltyTier|billingAmount");
            writer.newLine();

            for (int i = 1; i <= guests.getNumberOfEntries(); i++) {

                Guest g = guests.getEntry(i);

                if (g == null) {
                    continue;
                }

                writer.write(
                        g.getConfirmationNo() + "|" +
                        g.getName()           + "|" +
                        g.getPhone()          + "|" +
                        g.getLoyaltyTier()    + "|" +
                        g.getBillingAmount()
                );

                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println("Error saving guests.txt: " + e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    /**
     * Purpose:
     * Returns all VIP guests from the queue sorted by:
     *   1. Loyalty tier (highest first)
     *   2. Booking createdAt from BookingController (earliest first)
     */
    private Guest[] getSortedWaitingList() {

        Guest[] all = vipQueue.getAll();

        // Insertion sort — queue sizes are small.
        for (int i = 1; i < all.length; i++) {

            Guest key = all[i];
            int j = i - 1;

            while (j >= 0 && compare(all[j], key) > 0) {
                all[j + 1] = all[j];
                j--;
            }

            all[j + 1] = key;
        }

        // Ascending result — reverse so highest priority is first.
        reverse(all);

        return all;
    }

    /**
     * Purpose:
     * Compares two guests for sorting.
     *
     * Rule 1: Higher tier = higher priority.
     * Rule 2: Same tier — earlier createdAt wins (from BookingController).
     */
    private int compare(Guest a, Guest b) {

        int tierA = getPriority(a.getLoyaltyTier());
        int tierB = getPriority(b.getLoyaltyTier());

        if (tierA != tierB) {
            return tierB - tierA;
        }

        String dateA = getBookingCreatedAt(a.getConfirmationNo());
        String dateB = getBookingCreatedAt(b.getConfirmationNo());

        if (dateA == null && dateB == null) return 0;
        if (dateA == null) return 1;
        if (dateB == null) return -1;

        try {
            LocalDateTime timeA = LocalDateTime.parse(dateA, FORMATTER);
            LocalDateTime timeB = LocalDateTime.parse(dateB, FORMATTER);
            return timeA.compareTo(timeB);
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * Purpose:
     * Converts a loyalty tier string into a numeric priority.
     */
    private int getPriority(String loyaltyTier) {

        if (loyaltyTier == null) return 0;

        switch (loyaltyTier.toUpperCase()) {
            case "DIAMOND":  return 5;
            case "ELITE":    return 4;
            case "PLATINUM": return 3;
            case "GOLD":     return 2;
            case "SILVER":   return 1;
            default:         return 0;
        }
    }

    /**
     * Purpose:
     * Reverses a Guest array in-place.
     */
    private void reverse(Guest[] arr) {

        int left  = 0;
        int right = arr.length - 1;

        while (left < right) {
            Guest temp  = arr[left];
            arr[left]   = arr[right];
            arr[right]  = temp;
            left++;
            right--;
        }
    }

    /**
     * Purpose:
     * Finds a specific room by number if it is Ready.
     */
    private Room findAvailableRoom(String roomNumber) {

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return null;
        }

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {

            Room room = rooms.getEntry(i);

            if (room != null
                    && roomNumber.equalsIgnoreCase(room.getRoomNumber())
                    && "Ready".equalsIgnoreCase(room.getCleanlinessStatus())) {

                return room;
            }
        }

        return null;
    }
}
