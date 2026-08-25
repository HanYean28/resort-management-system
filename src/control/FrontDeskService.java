package control;

import adt.ArrayList;
import adt.BinarySearchTree;
import adt.BinarySearchTreeInterface;
import adt.ListInterface;
import dao.BillingDAO;
import dao.BookingDAO;
import dao.GuestDAO;
import dao.RoomDAO;
import entity.BillingRecord;
import entity.BookingRequest;
import entity.Guest;
import entity.Room;
import java.time.LocalDate;
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
    private BinarySearchTreeInterface<Guest> guestTree;
    private GuestDAO guestDAO;
    private RoomDAO roomDAO;
    private BookingDAO bookingDAO;
    private BillingDAO billingDAO;

    public FrontDeskService() {
        guestTree = new BinarySearchTree<>();
        guestDAO = new GuestDAO();
        roomDAO = new RoomDAO();
        bookingDAO = new BookingDAO();
        billingDAO = new BillingDAO();
        loadGuestsFromFile();
    }

    /**
     * Loads guest records from guests.txt.
     * Format: confirmationNo|name|phone|loyaltyTier
     */
    public void loadGuestsFromFile() {
        ListInterface<Guest> guests = guestDAO.loadGuests();
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            guestTree.add(guests.getEntry(i));
        }
        if (guests.isEmpty()) {
            loadSampleData();
        }
    }

    /** Hardcoded sample data so this module can be demonstrated/tested standalone.
     *  Room numbers here match the real Housekeeping rooms.txt (101-108). */
    private void loadSampleData() {
        guestTree.add(new Guest("20260701", "Tan Wei Ling", "012-3456789", "NONE"));
        guestTree.add(new Guest("20260702", "Nurul Aisyah", "013-2345678", "Diamond"));
        guestTree.add(new Guest("20260703", "Rajesh Kumar", "016-7891234", "Platinum"));
        guestTree.add(new Guest("20260704", "Chong Mei Yee", "011-9988776", "NONE"));
        guestTree.add(new Guest("20260705", "Ahmad Faiz", "019-2233445", "Elite"));
    }

    /** Saves all current guest records back to guests.txt (in confirmationNo order). */
    public void saveGuestsToFile() {
        guestDAO.saveGuests(getAllGuestsSorted());
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
        return new Guest(confirmationNo, null, null, null);
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
            if (room.getCleanlinessStatus().equalsIgnoreCase("Ready")
                    && room.getOccupancyStatus().equalsIgnoreCase("Vacant")
                    && !hasActiveBookingToday(room.getRoomNumber())) {
                available.add(room);
            }
        }

        insertionSortByRoomNumber(available);
        return available;
    }

    private boolean hasActiveBookingToday(String roomNumber) {
        LocalDate today = LocalDate.now();
        ListInterface<BookingRequest> bookings = bookingDAO.loadBookings();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getAssignedRoomNumber().equalsIgnoreCase(roomNumber)
                    && isActiveRoomBookingStatus(booking.getStatus())
                    && isDateWithinStay(today, booking.getCheckInDate(), booking.getCheckOutDate())) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveRoomBookingStatus(String status) {
        return status.equalsIgnoreCase("Assigned")
                || status.equalsIgnoreCase("Checked In");
    }

    private boolean isCurrentStayStatus(String status) {
        return status.equalsIgnoreCase("Checked In");
    }

    private boolean isDateWithinStay(LocalDate date, String checkInDate, String checkOutDate) {
        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            return !date.isBefore(checkIn) && date.isBefore(checkOut);
        } catch (Exception e) {
            return false;
        }
    }

    private ListInterface<Room> loadRoomsFromFile() {
        return roomDAO.loadRooms();
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

    /** Looks up a room's type by room number (linear search through rooms.txt). Returns "N/A" if not found. */
    public String getRoomType(String roomNo) {
        if (roomNo == null || roomNo.equalsIgnoreCase("N/A")) {
            return "N/A";
        }

        ListInterface<Room> allRooms = loadRoomsFromFile();
        for (int i = 1; i <= allRooms.getNumberOfEntries(); i++) {
            Room r = allRooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNo)) {
                return r.getRoomType();
            }
        }
        return "N/A";
    }

    public String getGuestCurrentRoom(String confirmationNo) {
        ListInterface<BookingRequest> bookings = bookingDAO.loadBookings();
        for (int i = 1; i <= bookings.getNumberOfEntries(); i++) {
            BookingRequest booking = bookings.getEntry(i);
            if (booking.getConfirmationNo().equalsIgnoreCase(confirmationNo)
                    && isCurrentStayStatus(booking.getStatus())) {
                return booking.getAssignedRoomNumber();
            }
        }
        return "N/A";
    }

    public String getGuestCurrentRoomType(String confirmationNo) {
        return getRoomType(getGuestCurrentRoom(confirmationNo));
    }

    // Filters by TWO criteria: (1) loyalty membership (member vs non-member)
    // AND (2) room type (pass "ALL" to skip this second filter), then sorts
    // the filtered results alphabetically by guest name (insertion sort).

    public ListInterface<Guest> generateGuestDirectoryReport(boolean membersOnly, String roomTypeFilter) {
        ListInterface<Guest> allGuests = getAllGuestsSorted();
        ListInterface<Guest> filtered = new ArrayList<>();
        boolean filterByRoomType = roomTypeFilter != null && !roomTypeFilter.equalsIgnoreCase("ALL");

        // Linear search through every record, filtering on BOTH criteria at once
        for (int i = 1; i <= allGuests.getNumberOfEntries(); i++) {
            Guest g = allGuests.getEntry(i);
            boolean isMember = g.getLoyaltyTier() != null && !g.getLoyaltyTier().equalsIgnoreCase("NONE");
            boolean matchesMembership = (isMember == membersOnly);
            boolean matchesRoomType = !filterByRoomType
                    || getGuestCurrentRoomType(g.getConfirmationNo()).equalsIgnoreCase(roomTypeFilter);

            if (matchesMembership && matchesRoomType) {
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

    public ListInterface<BillingRecord> generateGuestBillingReport(double minAmount, String roomTypeFilter) {
        ListInterface<BillingRecord> allBills = loadBillingFromFile();
        ListInterface<BillingRecord> filtered = new ArrayList<>();

        for (int i = 1; i <= allBills.getNumberOfEntries(); i++) {
            BillingRecord bill = allBills.getEntry(i);
            boolean matchesAmount = bill.getAmount() >= minAmount;
            boolean matchesRoomType = roomTypeFilter.equalsIgnoreCase("ALL")
                    || bill.getRoomType().equalsIgnoreCase(roomTypeFilter);

            if (matchesAmount && matchesRoomType) {
                filtered.add(bill);
            }
        }

        quickSortBillsByAmountDescending(filtered, 1, filtered.getNumberOfEntries());
        return filtered;
    }

    public String getGuestName(String confirmationNo) {
        Guest guest = searchByConfirmationNumber(confirmationNo);
        if (guest == null) {
            return "N/A";
        }
        return guest.getName();
    }

    private ListInterface<BillingRecord> loadBillingFromFile() {
        return billingDAO.loadBillingRecords();
    }

    private void quickSortBillsByAmountDescending(ListInterface<BillingRecord> list, int low, int high) {
        if (low < high) {
            int pivotIndex = partitionBills(list, low, high);
            quickSortBillsByAmountDescending(list, low, pivotIndex - 1);
            quickSortBillsByAmountDescending(list, pivotIndex + 1, high);
        }
    }

    private int partitionBills(ListInterface<BillingRecord> list, int low, int high) {
        double pivot = list.getEntry(high).getAmount();
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (list.getEntry(j).getAmount() > pivot) {
                i++;
                swapBills(list, i, j);
            }
        }
        swapBills(list, i + 1, high);
        return i + 1;
    }

    private void swapBills(ListInterface<BillingRecord> list, int posA, int posB) {
        BillingRecord temp = list.getEntry(posA);
        list.replace(posA, list.getEntry(posB));
        list.replace(posB, temp);
    }
}
