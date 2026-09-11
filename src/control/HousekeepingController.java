package control;

import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import dao.HousekeepingLogDAO;
import dao.RoomDAO;
import entity.HousekeepingLog;
import entity.Room;
import utility.DateUtils;

/**
 * @author Chang Han Yean
 */
public class HousekeepingController {
    public static final String STATUS_DIRTY = "Dirty";
    public static final String STATUS_CLEANING = "Cleaning In Progress";
    public static final String STATUS_INSPECTED = "Inspected";
    public static final String STATUS_READY = "Ready";

    public static final String FILTER_ALL = "ALL";
    // order of the cleaning status
    private static final String[] STATUS_PHASES = {
            STATUS_DIRTY,
            STATUS_CLEANING,
            STATUS_INSPECTED,
            STATUS_READY
    };
// 1. 
    private ListInterface<Room> rooms;
    private ListInterface<HousekeepingLog> taskHistory;
    private ListInterface<RoomRollbackEntry> roomRollbackStacks;
    private RoomDAO roomDAO;
    private HousekeepingLogDAO housekeepingLogDAO;

    public HousekeepingController() {
        rooms = new ArrayList<>();
        taskHistory = new ArrayList<>();
        roomRollbackStacks = new ArrayList<>();
        roomDAO = new RoomDAO();
        housekeepingLogDAO = new HousekeepingLogDAO();
        loadRoomsFromFile();
        loadTaskHistoryFromFile();
    }

    public void loadRoomsFromFile() {
        rooms.clear();
        ListInterface<Room> loadedRooms = roomDAO.loadRooms();
        for (int i = 1; i <= loadedRooms.getNumberOfEntries(); i++) {
            rooms.add(loadedRooms.getEntry(i));
        }
    }

    public void saveRoomsToFile() {
        roomDAO.saveRooms(rooms);
    }

    public ListInterface<Room> getAllRooms() {
        return rooms;
    }
//7. Search
    public Room getRoom(String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }
     
    // F1: Add Housekeeping Task
    public String addHousekeepingTask(String roomNumber) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }
        // check again whether the list contains the room
        if (!rooms.contains(room)) {
            return "Room not found.";
        }
        if (room.getCleanlinessStatus().equals(STATUS_DIRTY)
                || room.getCleanlinessStatus().equals(STATUS_CLEANING)
                || room.getCleanlinessStatus().equals(STATUS_INSPECTED)) {
            return "This room already has an active housekeeping task.";
        }
        if (room.getOccupancyStatus().equalsIgnoreCase("Occupied")) {
            return "Occupied rooms cannot be marked as housekeeping tasks until checkout.";
        }
        return updateRoomStatus(roomNumber, STATUS_DIRTY);
    }
    // F2: View Current Task
    public ListInterface<Room> getActiveTasks() {
        ListInterface<Room> activeTasks = new ArrayList<>(); // to store active housekeeping tasks
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (isActiveTask(room)) { 
                activeTasks.add(room);
            }
        }
        return activeTasks;
    }
    // F6: View Today Late Checkout Tasks
    public ListInterface<Room> getLateCheckoutTasksForToday() {
        ListInterface<Room> lateCheckoutTasks = new ArrayList<>(); // to store late checkout tasks for today
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (isActiveTask(room) // check if the room has an active housekeeping task
                    && room.getOccupancyStatus().equalsIgnoreCase("Vacant") // check if the room is empty
                    && isUpdatedToday(room) // check if last update was today
                    && hasRollbackPathToReady(room.getRoomNumber())) { // check if there is a rollback path to Ready
                lateCheckoutTasks.add(room);
            }
        }
        return lateCheckoutTasks;
    }
//3.
    // F3: Update Room Status 
    public String updateRoomStatus(String roomNumber, String targetStatus) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        String currentStatus = resolveCanonicalStatus(room.getCleanlinessStatus()); // check status is valid
        String nextStatus = resolveCanonicalStatus(targetStatus);

        if (currentStatus == null || nextStatus == null) {
            return "Invalid room status.";
        }

        // check the next status is allowed based on the status order
        if (!isStrictSequentialTransition(currentStatus, nextStatus)) {
            return "Invalid transition. Follow: Ready -> Dirty -> Cleaning In Progress -> Inspected -> Ready.";
        }

        applyStatusChange(room, currentStatus, nextStatus, true);
        return null;
    }

    public String[] getAllowedTargetStatuses(String currentStatus) {
        String canonical = resolveCanonicalStatus(currentStatus);
        if (canonical == null) {
            return new String[0];
        }
        // check the next status
        if (canonical.equals(STATUS_READY)) {
            return new String[] { STATUS_DIRTY };
        }
        if (canonical.equals(STATUS_DIRTY)) {
            return new String[] { STATUS_CLEANING };
        }
        if (canonical.equals(STATUS_CLEANING)) {
            return new String[] { STATUS_INSPECTED };
        }
        if (canonical.equals(STATUS_INSPECTED)) {
            return new String[] { STATUS_READY };
        }
        return new String[0];
    }

    // F4: Rollback Last Action (showing the last rollback action for a room)
    public HousekeepingLog peekLastRollbackAction(String roomNumber) {
        if (getRoom(roomNumber) == null) { 
            return null;
        }
        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry == null || entry.getStack().isEmpty()) {
            return null;
        }
        return entry.getStack().peek();
    }

    public int getRollbackStackSize(String roomNumber) {
        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry == null) {
            return 0;
        }
        return entry.getStack().size();
    }
//6.
    // F4: Rollback Last Action (actually performing the rollback action for a room)
    public HousekeepingLog rollbackLastAction(String roomNumber) {
        if (getRoom(roomNumber) == null) {
            return null;
        }

        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry == null || entry.getStack().isEmpty()) {
            return null;
        }
        // Get the last log from the stack 
        HousekeepingLog lastLog = entry.getStack().pop();
        // get the room object based on the room number from last log
        Room room = getRoom(lastLog.getRoomNumber());
        if (room == null) {
            return null;
        }
        
        applyStatusChange(room, lastLog.getNewStatus(), lastLog.getOldStatus(), false,
                HousekeepingLog.ACTION_ROLLBACK);
        return lastLog;
    }
    // F6: Handle Late Checkout
    // Change the room status back to Ready and Occupied, and clear the rollback stack for that room

    public String handleLateCheckout(String roomNumber) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }
        if (!isActiveTask(room)) {
            return "Room is not currently under housekeeping task.";
        }
        if (!isUpdatedToday(room)) {
            return "Only today's housekeeping tasks can be handled as late checkout.";
        }
        if (room.getOccupancyStatus().equalsIgnoreCase("Occupied")) {
            return "Room is already occupied.";
        }

        String oldStatus = room.getCleanlinessStatus();
        String restoredStatus = null;
        boolean restoredToReady = false;
        RoomRollbackEntry entry = findRollbackEntry(room.getRoomNumber());

        while (entry != null && !entry.getStack().isEmpty()) {
            // pop the last log from stack
            HousekeepingLog lastLog = entry.getStack().pop();
            // get the old status from last log
            restoredStatus = lastLog.getOldStatus();
            // check if the restored status is Ready
            if (restoredStatus.equals(STATUS_READY)) {
                restoredToReady = true;
                break;
            }
        }

        if (!restoredToReady) {
            return "No rollback path to Ready found for this room.";
        }

        String timestamp = getCurrentTimestamp();
        room.setCleanlinessStatus(restoredStatus);
        room.setOccupancyStatus("Occupied");
        room.setLastUpdate(timestamp);
        if (restoredStatus.equals(STATUS_READY)) {
            room.setDirtySince("N/A");
        }
        // save late checkout action 
        HousekeepingLog log = new HousekeepingLog(room.getRoomNumber(), oldStatus, restoredStatus, timestamp,
                HousekeepingLog.ACTION_LATE_CHECKOUT);
        appendTaskHistory(log);
        clearRollbackStack(room.getRoomNumber());
        saveRoomsToFile();
        return null;
    }

// REPORTS 

    // F5: View Task History
    public ListInterface<HousekeepingLog> getTaskHistory(String roomNumber) {
        ListInterface<HousekeepingLog> results = new ArrayList<>();
        for (int i = 1; i <= taskHistory.getNumberOfEntries(); i++) {
            HousekeepingLog log = taskHistory.getEntry(i);
            if (roomNumber.equals(FILTER_ALL)
                    || log.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                results.add(log);
            }
        }
        return results;
    }
//9.
    // R1: Room Status Summary Report
    // filter: cleaning status + room type
    // sort: by cleaning status then room number (cleaning status order: Dirty -> Cleaning In Progress -> Inspected -> Ready)
    public ListInterface<Room> generateStatusSummaryReport(String statusFilter, String roomTypeFilter) {
        ListInterface<Room> results = new ArrayList<>();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            // filter
            boolean matchesStatus = statusFilter.equals(FILTER_ALL)
                    || room.getCleanlinessStatus().equalsIgnoreCase(statusFilter); // check the cleaning status
            boolean matchesType = roomTypeFilter.equals(FILTER_ALL)
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter); // check room type

            if (matchesStatus && matchesType) {
                results.add(room);
            }
        }
        // sort
        insertionSortRoomsByStatusThenNumber(results);
        return results;
    }
//10.
    // R2: Task History Report
    // filter: room number + cleaning status + action
    // sort: by timestamp (newest first or oldest first)
    public ListInterface<HousekeepingLog> generateTaskHistoryReport(String roomNumberFilter,
            String transitionFilter, String actionFilter, boolean newestFirst) {
        ListInterface<HousekeepingLog> results = new ArrayList<>();
        // filter
        for (int i = 1; i <= taskHistory.getNumberOfEntries(); i++) {
            HousekeepingLog log = taskHistory.getEntry(i);
            boolean matchesRoom = roomNumberFilter.equals(FILTER_ALL)
                    || log.getRoomNumber().equalsIgnoreCase(roomNumberFilter); // check the room number
            boolean matchesTransition = transitionFilter.equals(FILTER_ALL)
                    || log.getNewStatus().equalsIgnoreCase(transitionFilter); // check the room cleaning status
            boolean matchesAction = actionFilter.equals(FILTER_ALL)
                    || log.getAction().equalsIgnoreCase(actionFilter); // check the action

            if (matchesRoom && matchesTransition && matchesAction) {
                results.add(log);
            }
        }
        // sort
        insertionSortLogsByTimestamp(results, newestFirst);
        return results;
    }

    public int countRoomsByStatus(ListInterface<Room> roomList, String status) {
        int count = 0;
        for (int i = 1; i <= roomList.getNumberOfEntries(); i++) {
            if (roomList.getEntry(i).getCleanlinessStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }
//4.   
    // for normal update
    private void applyStatusChange(Room room, String oldStatus, String newStatus, boolean allowRollback) {
        applyStatusChange(room, oldStatus, newStatus, allowRollback, HousekeepingLog.ACTION_UPDATE);
    }

    // for rollback or late checkout (special case)
    private void applyStatusChange(Room room, String oldStatus, String newStatus, boolean allowRollback,
            String action) {
        String timestamp = getCurrentTimestamp();
        room.setCleanlinessStatus(newStatus);
        room.setLastUpdate(timestamp);

        if (newStatus.equals(STATUS_DIRTY)) {
            room.setDirtySince(timestamp);
            room.setLastTurnaroundMinutes("N/A");
        }
        if (newStatus.equals(STATUS_READY)) {
            room.setDirtySince("N/A");
        }
        // save the housekeeping log for this status change
        HousekeepingLog log = new HousekeepingLog(room.getRoomNumber(), oldStatus, newStatus, timestamp, action);
        if (allowRollback) {
            pushRollbackLog(room.getRoomNumber(), log);
        }
        appendTaskHistory(log);
        saveRoomsToFile();
    }

    private void loadTaskHistoryFromFile() {
        taskHistory.clear();
        roomRollbackStacks.clear();
        ListInterface<HousekeepingLog> loadedLogs = housekeepingLogDAO.loadLogs();
        for (int i = 1; i <= loadedLogs.getNumberOfEntries(); i++) {
            HousekeepingLog log = loadedLogs.getEntry(i);
            taskHistory.add(log);
            rebuildRollbackStack(log);
        }
    }

    private void rebuildRollbackStack(HousekeepingLog log) {
        if (log.getAction().equals(HousekeepingLog.ACTION_UPDATE)) {
            pushRollbackLog(log.getRoomNumber(), log);
        } else if (log.getAction().equals(HousekeepingLog.ACTION_ROLLBACK)) {
            RoomRollbackEntry entry = findRollbackEntry(log.getRoomNumber());
            if (entry != null && !entry.getStack().isEmpty()) {
                entry.getStack().pop();
            }
        } else if (log.getAction().equals(HousekeepingLog.ACTION_LATE_CHECKOUT)) {
            clearRollbackStack(log.getRoomNumber());
        }
    }

    // save log to taskHistory and update file
    private void appendTaskHistory(HousekeepingLog log) {
        taskHistory.isFull(); 
        taskHistory.add(log); 
        housekeepingLogDAO.appendLog(log); 
    }

    private boolean isActiveTask(Room room) {
        return room.getCleanlinessStatus().equals(STATUS_DIRTY)
                || room.getCleanlinessStatus().equals(STATUS_CLEANING)
                || room.getCleanlinessStatus().equals(STATUS_INSPECTED);
    }

    private boolean isUpdatedToday(Room room) {
        return DateUtils.isTodayTimestamp(room.getLastUpdate());
    }

    // prevent skipping status and follow the status order Ready -> Dirty -> Cleaning In Progress -> Inspected -> Ready
    private boolean isStrictSequentialTransition(String currentStatus, String targetStatus) {
        String[] allowed = getAllowedTargetStatuses(currentStatus);
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(targetStatus)) {
                return true;
            }
        }
        return false;
    }

    // sort the rooms by status order and then by room number (R1)
    private void insertionSortRoomsByStatusThenNumber(ListInterface<Room> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) { // start from 2 because 1 is sorted
            Room key = list.remove(i); // take the log at index and remove from list
            int j = i - 1;
            while (j >= 1 && compareRoomsByStatusThenNumber(list.getEntry(j), key) > 0) {
                j--;
            }
            list.add(j + 1, key); // insert the key at the correct position
        }
    }

    private int compareRoomsByStatusThenNumber(Room left, Room right) {
        int statusCompare = getStatusOrder(left.getCleanlinessStatus()) - getStatusOrder(right.getCleanlinessStatus());
        if (statusCompare != 0) {
            return statusCompare;
        }
        return left.getRoomNumber().compareToIgnoreCase(right.getRoomNumber());
    }

    private int getStatusOrder(String status) {
        for (int i = 0; i < STATUS_PHASES.length; i++) {
            if (STATUS_PHASES[i].equals(status)) {
                return i;
            }
        }
        return STATUS_PHASES.length;
    }

    // sort the logs by timestamp (R2)
    private void insertionSortLogsByTimestamp(ListInterface<HousekeepingLog> list, boolean newestFirst) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) { // start from 2 because 1 is sorted
            HousekeepingLog key = list.remove(i); // take the log at index and remove from list
            int j = i - 1;
            while (j >= 1 && compareLogTimestamp(list.getEntry(j), key, newestFirst) > 0) { // compare the timestamp 
                j--;
            }
            list.add(j + 1, key); // insert the key at the correct position
        }
    }
    // compare which log timestamps is bigger
    private int compareLogTimestamp(HousekeepingLog left, HousekeepingLog right, boolean newestFirst) {
        int result = left.getTimestamp().compareTo(right.getTimestamp());
        if (newestFirst) {
            return -result;
        }
        return result;
    }

    // check if the status is a valid cleaning status
    private String resolveCanonicalStatus(String status) { // Canonical status means standard status that is recognized by the system
        if (status == null) {
            return null;
        }
        for (int i = 0; i < STATUS_PHASES.length; i++) {
            if (STATUS_PHASES[i].equalsIgnoreCase(status.trim())) { // trim is remove space
                return STATUS_PHASES[i];
            }
        }
        return null;
    }
//8.Search
    // check whether the room has rollback stack or not
    private RoomRollbackEntry findRollbackEntry(String roomNumber) {
        for (int i = 1; i <= roomRollbackStacks.getNumberOfEntries(); i++) {
            RoomRollbackEntry entry = roomRollbackStacks.getEntry(i); // take one rollback entry from the list
            if (entry.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return entry; // return the rollback entry of the room
            }
        }
        return null;
    }

    // make sure the rollback stack exists for the room if no then create new one
    private RoomRollbackEntry getOrCreateRollbackEntry(String roomNumber) {
        RoomRollbackEntry entry = findRollbackEntry(roomNumber); // find the room rollback entry
        if (entry != null) {
            return entry;
        }
        entry = new RoomRollbackEntry(roomNumber);
        roomRollbackStacks.add(entry);
        return entry;
    }
//5.
    private void pushRollbackLog(String roomNumber, HousekeepingLog log) {
        getOrCreateRollbackEntry(roomNumber).getStack().push(log); // check the rollback entry exists, and get the stack entry and push the log into the stack
    }

    private void clearRollbackStack(String roomNumber) {
        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry != null) {
            entry.getStack().clear();
        }
    }
    // Check if there is a rollback path to Ready
    // for late checkout checking purpose
    private boolean hasRollbackPathToReady(String roomNumber) {
        RoomRollbackEntry entry = findRollbackEntry(roomNumber); 
        if (entry == null || entry.getStack().isEmpty()) {
            return false;
        }

        boolean foundReady = false;
        StackInterface<HousekeepingLog> tempStack = new ArrayStack<>();
        while (!entry.getStack().isEmpty()) {
            HousekeepingLog log = entry.getStack().pop();
            tempStack.push(log);
            if (log.getOldStatus().equals(STATUS_READY)) {
                foundReady = true;
                break;
            }
        }

        while (!tempStack.isEmpty()) {
            entry.getStack().push(tempStack.pop());
        }
        return foundReady;
    }

    private String getCurrentTimestamp() {
        return DateUtils.getCurrentTimestamp();
    }
//2.
    // inner class to store rollback stack for each room
    //each room need it's own rollback stack
    private static class RoomRollbackEntry {
        private final String roomNumber;
        private final StackInterface<HousekeepingLog> stack;

        private RoomRollbackEntry(String roomNumber) {
            this.roomNumber = roomNumber;
            this.stack = new ArrayStack<>();
        }

        private String getRoomNumber() {
            return roomNumber;
        }
        // return entire stack object
        // gives access to the whole rollback stack that belongs to that room.
        private StackInterface<HousekeepingLog> getStack() {
            return stack;
        }
    }
}
