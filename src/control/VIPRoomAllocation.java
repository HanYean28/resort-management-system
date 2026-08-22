package control;

import adt.ArrayList;
import adt.ArrayPriorityQueue;
import entity.Guest;
import entity.Room;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Controller for VIP Room Allocation.
 *
 * Responsibilities:
 * - Load guest data from guests.txt
 * - Load room data from Room.txt
 * - Add VIP guests to the VIP priority queue
 * - Search and remove VIP guests
 * - Display available rooms
 * - Allocate rooms to the highest-priority VIP guest
 */
public class VIPRoomAllocation {

    private ArrayPriorityQueue vipQueue;
    private ArrayList<Guest> guests;
    private ArrayList<Room> rooms;

    /**
     * Purpose:
     * Initializes the VIP room allocation module.
     *
     * The constructor also loads existing guest and
     * room information from the TXT files.
     */
    public VIPRoomAllocation() {

        vipQueue = new ArrayPriorityQueue();

        guests = new ArrayList<>();

        rooms = new ArrayList<>();

        // Load existing data when the module starts.
        loadGuestData();
        loadRoomData();
    }

    /**
     * Purpose:
     * Loads guest information from guests.txt.
     *
     * FIX 1: Changed "Guest.txt" to "guests.txt" to match
     * the actual filename on disk (case-sensitive on Linux/Mac).
     *
     * VIP guests with a loyalty tier other than NONE
     * are added to the VIP priority queue.
     *
     * Standard guests with NONE loyalty are stored
     * in the guest list but are not added to this
     * module's VIP queue.
     */
    private void loadGuestData() {

        // FIX 1: filename corrected from "Guest.txt" to "guests.txt"
        try (BufferedReader reader =
                new BufferedReader(new FileReader("guests.txt"))) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                // Ignore empty lines and header lines.
                if (line.isEmpty() || line.startsWith("#")
                        || line.startsWith("confirmationNo")) {
                    continue;
                }

                String[] data = line.split("\\|");

                // Make sure the line contains all 6 fields.
                if (data.length != 6) {
                    continue;
                }

                String confirmationNo   = data[0].trim();
                String name             = data[1].trim();
                String phone            = data[2].trim();
                String loyaltyTier      = data[3].trim();
                double billingAmount    = Double.parseDouble(data[4].trim());
                String roomNo           = data[5].trim();

                Guest guest = new Guest(
                        confirmationNo,
                        name,
                        phone,
                        loyaltyTier,
                        billingAmount,
                        roomNo
                );

                addGuest(guest);
            }

            System.out.println("Guest data loaded successfully.");

        } catch (IOException e) {

            System.out.println(
                    "Error loading guests.txt: " + e.getMessage());

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid billing amount in guests.txt.");
        }
    }

    /**
     * Purpose:
     * Loads room information from Room.txt.
     *
     * FIX 2: Room.txt has 7 fields, not 6.
     * The field order is:
     *   roomNumber | roomType | cleanlinessStatus | occupancyStatus
     *   | lastUpdate | dirtySince | lastTurnaroundMinutes
     *
     * The original code checked for 6 fields (rejecting every line)
     * and skipped occupancyStatus, causing wrong field mapping.
     */
    private void loadRoomData() {

        try (BufferedReader reader =
                new BufferedReader(new FileReader("rooms.txt"))) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                // Ignore empty lines and header lines.
                if (line.isEmpty() || line.startsWith("#")
                        || line.startsWith("roomNumber")) {
                    continue;
                }

                String[] data = line.split("\\|");

                // FIX 2: Room.txt has 7 fields — changed from 6 to 7.
                if (data.length != 7) {
                    continue;
                }

                // FIX 2: Correct field mapping including occupancyStatus.
                String roomNumber            = data[0].trim();
                String roomType              = data[1].trim();
                String cleanlinessStatus     = data[2].trim();
                String occupancyStatus       = data[3].trim(); // was missing
                String lastUpdate            = data[4].trim(); // was data[3]
                String dirtySince            = data[5].trim(); // was data[4]
                String lastTurnaroundMinutes = data[6].trim(); // was data[5]

                // FIX 2: Use the 7-argument constructor to pass occupancyStatus.
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

            System.out.println(
                    "Error loading Room.txt: " + e.getMessage());
        }
    }

    /**
     * Purpose:
     * Adds a guest to the general guest list.
     *
     * Only guests with a VIP loyalty tier are placed
     * into the VIP priority queue.
     */
    public void addGuest(Guest guest) {

        if (guest == null) {
            return;
        }

        guests.add(guest);

        /*
         * Only VIP loyalty tiers should enter the
         * VIP room allocation queue.
         */
        if (isVIP(guest)) {
            vipQueue.add(guest);
        }
    }

    /**
     * Purpose:
     * Determines whether a guest belongs to the
     * VIP room allocation queue.
     */
    private boolean isVIP(Guest guest) {

        if (guest.getLoyaltyTier() == null) {
            return false;
        }

        return !guest.getLoyaltyTier()
                .equalsIgnoreCase("NONE");
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

    /**
     * Purpose:
     * Returns the highest-priority VIP guest
     * without removing the guest.
     */
    public Guest getNextVIPGuest() {

        return vipQueue.peek();
    }

    /**
     * Purpose:
     * Returns all VIP guests currently waiting
     * in priority order.
     */
    public Guest[] getWaitingList() {

        return vipQueue.getAll();
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
     * Checks whether there are no VIP guests
     * waiting for room allocation.
     */
    public boolean isWaitingListEmpty() {

        return vipQueue.isEmpty();
    }

    /**
     * Purpose:
     * Returns all rooms that are currently Ready
     * for guest allocation.
     */
    public Room[] getAvailableRooms() {

        int count = 0;

        for (int i = 1;
                i <= rooms.getNumberOfEntries();
                i++) {

            Room room = rooms.getEntry(i);

            if (room != null
                    && "Ready".equalsIgnoreCase(
                            room.getCleanlinessStatus())) {

                count++;
            }
        }

        Room[] availableRooms = new Room[count];

        int index = 0;

        for (int i = 1;
                i <= rooms.getNumberOfEntries();
                i++) {

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
     * Allocates a selected available room to the
     * highest-priority VIP guest.
     *
     * The guest is removed from the VIP queue after
     * successful allocation.
     */
    public Guest allocateRoom(String roomNumber) {

        Guest guest = vipQueue.peek();

        if (guest == null) {
            return null;
        }

        Room selectedRoom =
                findAvailableRoom(roomNumber);

        if (selectedRoom == null) {
            return null;
        }

        // Remove the highest-priority guest.
        guest = vipQueue.remove();

        // Assign the selected room to the guest.
        guest.setRoomNo(
                selectedRoom.getRoomNumber());

        // Change room status after allocation.
        selectedRoom.setCleanlinessStatus("Occupied");

        return guest;
    }

    /**
     * Purpose:
     * Searches for a VIP guest using their
     * confirmation number.
     */
    public Guest findGuest(String confirmationNo) {

        return vipQueue.find(confirmationNo);
    }

    /**
     * Purpose:
     * Removes a VIP guest from the waiting queue
     * using the confirmation number.
     */
    public boolean removeGuest(String confirmationNo) {

        return vipQueue.removeByConfirmationNo(
                confirmationNo);
    }

    /**
     * Purpose:
     * Finds a specific room if the room exists,
     * is currently Ready, and matches the room number.
     */
    private Room findAvailableRoom(String roomNumber) {

        if (roomNumber == null
                || roomNumber.trim().isEmpty()) {

            return null;
        }

        for (int i = 1;
                i <= rooms.getNumberOfEntries();
                i++) {

            Room room = rooms.getEntry(i);

            if (room != null
                    && roomNumber.equalsIgnoreCase(
                            room.getRoomNumber())
                    && "Ready".equalsIgnoreCase(
                            room.getCleanlinessStatus())) {

                return room;
            }
        }

        return null;
    }

    /**
     * Purpose:
     * Adds a newly created VIP booking to the
     * same VIP queue used by existing TXT data,
     * then saves the updated guest list to guests.txt
     * so the booking persists after the program exits.
     */
    public void addVIPGuest(Guest guest) {

        if (guest == null) {
            return;
        }

        addGuest(guest);

        // Persist the new guest to disk immediately.
        saveGuestData();
    }

    /**
     * Purpose:
     * Rewrites guests.txt with the current in-memory
     * guest list so that any newly added guests are
     * saved and available on the next program launch.
     *
     * The header line is preserved at the top of the file.
     * Every guest is written as a pipe-delimited line:
     *   confirmationNo|name|phone|loyaltyTier|billingAmount|roomNo
     */
    private void saveGuestData() {

        try (BufferedWriter writer =
                new BufferedWriter(new FileWriter("guests.txt"))) {

            // Write the header so loadGuestData() can skip it.
            writer.write(
                    "# confirmationNo|name|phone|loyaltyTier|billingAmount|roomNo");
            writer.newLine();

            for (int i = 1; i <= guests.getNumberOfEntries(); i++) {

                Guest g = guests.getEntry(i);

                if (g == null) {
                    continue;
                }

                // Use "N/A" when roomNo has not been assigned yet.
                String roomNo = (g.getRoomNo() == null
                        || g.getRoomNo().trim().isEmpty())
                        ? "N/A"
                        : g.getRoomNo();

                writer.write(
                        g.getConfirmationNo() + "|" +
                        g.getName()           + "|" +
                        g.getPhone()          + "|" +
                        g.getLoyaltyTier()    + "|" +
                        g.getBillingAmount()  + "|" +
                        roomNo
                );

                writer.newLine();
            }

            System.out.println("Guest data saved successfully.");

        } catch (IOException e) {

            System.out.println(
                    "Error saving guests.txt: " + e.getMessage());
        }
    }
}