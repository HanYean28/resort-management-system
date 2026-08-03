package control;

import adt.ArrayList;
import adt.BinarySearchTree;
import adt.BinarySearchTreeInterface;
import adt.ListInterface;
import entity.Guest;
import entity.Room;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

/**
 * Front-Desk Service module.
 * Non-Linear ADT: BinarySearchTree<Guest>, keyed by confirmationNo, for
 * instant guest search (add / getEntry / contains — O(log n) on a
 * balanced tree).
 *
 * @author Lim How Voon
 */

public class FrontDeskService {
    private static final String DATA_FILE = "guests.txt";
    private static final String ROOMS_FILE = "rooms.txt";

    private BinarySearchTreeInterface<Guest> guestTree;

    public FrontDeskService() {
        guestTree = new BinarySearchTree<>();
        loadGuestsFromFile();
    }

    /**
     * Loads guest records from guests.txt.
     * Format: confirmationNo|name|phone|loyaltyTier|billingAmount|roomNo
     */
    public void loadGuestsFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    Guest guest = new Guest(parts[0], parts[1], parts[2], parts[3],
                            Double.parseDouble(parts[4]), parts[5]);
                    guestTree.add(guest);
                }
            }
        } catch (IOException e) {
            // If file doesn't exist yet, fall back to hardcoded sample data
            loadSampleData();
        }
    }

    /** Hardcoded sample data so this module can be demonstrated/tested standalone. */
    private void loadSampleData() {
        guestTree.add(new Guest("20260701", "Tan Wei Ling", "012-3456789", "NONE", 0.00, "101"));
        guestTree.add(new Guest("20260702", "Nurul Aisyah", "013-2345678", "Diamond", 150.50, "205"));
        guestTree.add(new Guest("20260703", "Rajesh Kumar", "016-7891234", "Platinum", 0.00, "310"));
        guestTree.add(new Guest("20260704", "Chong Mei Yee", "011-9988776", "NONE", 45.00, "102"));
        guestTree.add(new Guest("20260705", "Ahmad Faiz", "019-2233445", "Elite", 320.00, "208"));
    }

    /** Saves all current guest records back to guests.txt (in confirmationNo order). */
    public void saveGuestsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            Iterator<Guest> it = guestTree.getInorderIterator();
            while (it.hasNext()) {
                Guest g = it.next();
                bw.write(g.getConfirmationNo() + "|" + g.getName() + "|" + g.getPhone() + "|"
                        + g.getLoyaltyTier() + "|" + g.getBillingAmount() + "|" + g.getRoomNo());
                bw.newLine();
            }
        } catch (IOException e) {
            // Handle logging or exception propagation
        }
    }

    // ---------- Core BST operations (Non-Linear ADT & Searching) ----------

    /**
     * Registers a new guest record into the BST, keyed by confirmationNo.
     * @return null if added as new, or the previous Guest if confirmationNo already existed (overwritten)
     */
    public Guest addGuest(Guest guest) {
        Guest replaced = guestTree.add(guest);
        saveGuestsToFile();
        return replaced;
    }

    /** Instant search by confirmation number using BST getEntry(). Returns null if not found. */
    public Guest searchByConfirmationNumber(String confirmationNo) {
        return guestTree.getEntry(searchKey(confirmationNo));
    }

    /** Checks existence without retrieving the full record, using BST contains(). */
    public boolean confirmationNumberExists(String confirmationNo) {
        return guestTree.contains(searchKey(confirmationNo));
    }

    /** Removes a guest record by confirmation number. */
    public Guest removeGuest(String confirmationNo) {
        Guest removed = guestTree.remove(searchKey(confirmationNo));
        if (removed != null) {
            saveGuestsToFile();
        }
        return removed;
    }

    /** Builds a placeholder Guest used only as a search key (equals/compareTo use confirmationNo only). */
    private Guest searchKey(String confirmationNo) {
        return new Guest(confirmationNo, null, null, null, 0.0, null);
    }

    public boolean isEmpty() {
        return guestTree.isEmpty();
    }

    /** Returns every guest record, sorted ascending by confirmationNo (BST in-order). */
    public ListInterface<Guest> getAllGuestsSorted() {
        ListInterface<Guest> list = new ArrayList<>();
        Iterator<Guest> it = guestTree.getInorderIterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    /**
     * Returns rooms with cleanliness status Ready for check-in.
     * Reads live data from rooms.txt (shared with Housekeeping module).
     */
    public ListInterface<Room> getAvailableRooms() {
        ListInterface<Room> allRooms = loadRoomsFromFile();
        ListInterface<Room> available = new ArrayList<>();

        for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
            Room room = allRooms.getEntry(i);
            if (room.getCleanlinessStatus().equalsIgnoreCase("Ready")) {
                available.add(room);
            }
        }

        insertionSortByRoomNumber(available);
        return available;
    }

    private ListInterface<Room> loadRoomsFromFile() {
        ListInterface<Room> rooms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                } else if (parts.length == 4) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3]));
                } else if (parts.length == 3) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], "N/A"));
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, return empty list
        }
        return rooms;
    }

    private void insertionSortByRoomNumber(ListInterface<Room> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            Room key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getRoomNumber().compareToIgnoreCase(key.getRoomNumber()) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    // Filters by loyalty membership (member vs non-member), then sorts the
    // filtered results alphabetically by guest name (insertion sort).

    public ListInterface<Guest> generateGuestDirectoryReport(boolean membersOnly) {
        ListInterface<Guest> allGuests = getAllGuestsSorted();
        ListInterface<Guest> filtered = new ArrayList<>();

        // Linear search through every record, filtering on loyalty membership
        for (int i = 1; i <= allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.getEntry(i);
            boolean isMember = g.getLoyaltyTier() != null && !g.getLoyaltyTier().equalsIgnoreCase("NONE");
            if (isMember == membersOnly) {
                filtered.add(g);
            }
        }

        insertionSortByName(filtered);
        return filtered;
    }

    // Hand-written insertion sort, ascending alphabetically by guest name. /
    private void insertionSortByName(ListInterface<Guest> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            Guest key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getName().compareToIgnoreCase(key.getName()) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    // Filters guests whose billing amount >= minBalance, then quicksorts the
    // filtered results by billing amount, descending (highest debt first).

    public ListInterface<Guest> generateOutstandingBillingReport(double minBalance) {
        ListInterface<Guest> allGuests = getAllGuestsSorted();
        ListInterface<Guest> filtered = new ArrayList<>();

        for (int i = 1; i <= allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.getEntry(i);
            if (g.getBillingAmount() >= minBalance) {
                filtered.add(g);
            }
        }

        if (!filtered.isEmpty()) {
            quickSortByBillingDescending(filtered, 1, filtered.getNumberOfEntries());
        }
        return filtered;
    }

    private void quickSortByBillingDescending(ListInterface<Guest> list, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(list, low, high);
            quickSortByBillingDescending(list, low, pivotIndex - 1);
            quickSortByBillingDescending(list, pivotIndex + 1, high);
        }
    }

    private int partition(ListInterface<Guest> list, int low, int high) {
        double pivot = list.getEntry(high).getBillingAmount();
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.getEntry(j).getBillingAmount() > pivot) { // descending order
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private void swap(ListInterface<Guest> list, int posA, int posB) {
        Guest temp = list.getEntry(posA);
        list.replace(posA, list.getEntry(posB));
        list.replace(posB, temp);
    }
}
