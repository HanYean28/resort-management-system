package control;

import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.HousekeepingLog;
import entity.Room;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Chang Han Yean
 */
public class HousekeepingController {
    private static final String DATA_FILE = "rooms.txt";
    private static final String TASK_HISTORY_FILE = "housekeeping_task_history.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String STATUS_DIRTY = "Dirty";
    public static final String STATUS_CLEANING = "Cleaning In Progress";
    public static final String STATUS_INSPECTED = "Inspected";
    public static final String STATUS_READY = "Ready";

    public static final String FILTER_ALL = "ALL";

    private static final String[] STATUS_PHASES = {
            STATUS_DIRTY,
            STATUS_CLEANING,
            STATUS_INSPECTED,
            STATUS_READY
    };

    private ListInterface<Room> rooms;
    private ListInterface<HousekeepingLog> taskHistory;
    private ListInterface<RoomRollbackEntry> roomRollbackStacks;

    public HousekeepingController() {
        rooms = new ArrayList<>();
        taskHistory = new ArrayList<>();
        roomRollbackStacks = new ArrayList<>();
        loadRoomsFromFile();
        loadTaskHistoryFromFile();
    }

    public void loadRoomsFromFile() {
        rooms.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
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
            // Start with an empty room list if the file is missing.
        }
    }

    public void saveRoomsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bw.write("# roomNumber|roomType|cleanlinessStatus|lastUpdate|dirtySince|lastTurnaroundMinutes");
            bw.newLine();
            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room room = rooms.getEntry(i);
                bw.write(room.getRoomNumber() + "|" + room.getRoomType() + "|" + room.getCleanlinessStatus()
                        + "|" + room.getLastUpdate() + "|" + room.getDirtySince() + "|"
                        + room.getLastTurnaroundMinutes());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    public ListInterface<Room> getAllRooms() {
        return rooms;
    }

    public Room getRoom(String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    public String addHousekeepingTask(String roomNumber) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }
        if (room.getCleanlinessStatus().equals(STATUS_DIRTY)
                || room.getCleanlinessStatus().equals(STATUS_CLEANING)
                || room.getCleanlinessStatus().equals(STATUS_INSPECTED)) {
            return "This room already has an active housekeeping task.";
        }
        return updateRoomStatus(roomNumber, STATUS_DIRTY);
    }

    public ListInterface<Room> getActiveTasks() {
        ListInterface<Room> activeTasks = new ArrayList<>();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            if (isActiveTask(room)) {
                activeTasks.add(room);
            }
        }
        return activeTasks;
    }

    public ListInterface<Room> searchRoomsOrTasks(String keyword, String statusFilter, String roomTypeFilter) {
        ListInterface<Room> results = new ArrayList<>();
        String searchText = keyword == null ? "" : keyword.trim().toLowerCase();

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            boolean matchesKeyword = searchText.isEmpty()
                    || room.getRoomNumber().toLowerCase().contains(searchText)
                    || room.getRoomType().toLowerCase().contains(searchText)
                    || room.getCleanlinessStatus().toLowerCase().contains(searchText);
            boolean matchesStatus = statusFilter.equals(FILTER_ALL)
                    || room.getCleanlinessStatus().equalsIgnoreCase(statusFilter);
            boolean matchesType = roomTypeFilter.equals(FILTER_ALL)
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter);

            if (matchesKeyword && matchesStatus && matchesType) {
                results.add(room);
            }
        }
        return results;
    }

    public String updateRoomStatus(String roomNumber, String targetStatus) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        String currentStatus = resolveCanonicalStatus(room.getCleanlinessStatus());
        String nextStatus = resolveCanonicalStatus(targetStatus);
        if (currentStatus == null || nextStatus == null) {
            return "Invalid room status.";
        }
        if (!isStrictSequentialTransition(currentStatus, nextStatus)) {
            return "Invalid transition. Follow: Ready -> Dirty -> Cleaning In Progress -> Inspected -> Ready.";
        }

        applyStatusChange(room, currentStatus, nextStatus, true);
        return null;
    }

    public String completeTask(String roomNumber) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }
        if (!room.getCleanlinessStatus().equals(STATUS_INSPECTED)) {
            return "Only inspected rooms can be completed.";
        }
        return updateRoomStatus(roomNumber, STATUS_READY);
    }

    public String[] getAllowedTargetStatuses(String currentStatus) {
        String canonical = resolveCanonicalStatus(currentStatus);
        if (canonical == null) {
            return new String[0];
        }

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

    public HousekeepingLog rollbackLastAction(String roomNumber) {
        if (getRoom(roomNumber) == null) {
            return null;
        }

        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry == null || entry.getStack().isEmpty()) {
            return null;
        }

        HousekeepingLog lastLog = entry.getStack().pop();
        Room room = lastLog.getRoom();
        applyStatusChange(room, lastLog.getNewStatus(), lastLog.getOldStatus(), false);
        return lastLog;
    }

    public ListInterface<HousekeepingLog> getTaskHistory(String roomNumber) {
        ListInterface<HousekeepingLog> results = new ArrayList<>();
        for (int i = 1; i <= taskHistory.getNumberOfEntries(); i++) {
            HousekeepingLog log = taskHistory.getEntry(i);
            if (roomNumber.equals(FILTER_ALL)
                    || log.getRoom().getRoomNumber().equalsIgnoreCase(roomNumber)) {
                results.add(log);
            }
        }
        return results;
    }

    private void applyStatusChange(Room room, String oldStatus, String newStatus, boolean allowRollback) {
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

        HousekeepingLog log = new HousekeepingLog(room, oldStatus, newStatus, timestamp);
        if (allowRollback) {
            pushRollbackLog(room.getRoomNumber(), log);
        }
        appendTaskHistory(log);
        saveRoomsToFile();
    }

    private void loadTaskHistoryFromFile() {
        taskHistory.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(TASK_HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Room room = getRoom(parts[0]);
                    if (room != null) {
                        taskHistory.add(new HousekeepingLog(room, parts[1], parts[2], parts[3]));
                    }
                }
            }
        } catch (IOException e) {
            // Start with an empty task history if the file is missing.
        }
    }

    private void appendTaskHistory(HousekeepingLog log) {
        taskHistory.add(log);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TASK_HISTORY_FILE, true))) {
            bw.write(log.getRoom().getRoomNumber() + "|" + log.getOldStatus() + "|" + log.getNewStatus()
                    + "|" + log.getTimestamp());
            bw.newLine();
        } catch (IOException e) {
            // Keep the console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean isActiveTask(Room room) {
        return room.getCleanlinessStatus().equals(STATUS_DIRTY)
                || room.getCleanlinessStatus().equals(STATUS_CLEANING)
                || room.getCleanlinessStatus().equals(STATUS_INSPECTED);
    }

    private boolean isStrictSequentialTransition(String currentStatus, String targetStatus) {
        String[] allowed = getAllowedTargetStatuses(currentStatus);
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(targetStatus)) {
                return true;
            }
        }
        return false;
    }

    private String resolveCanonicalStatus(String status) {
        if (status == null) {
            return null;
        }
        for (int i = 0; i < STATUS_PHASES.length; i++) {
            if (STATUS_PHASES[i].equalsIgnoreCase(status.trim())) {
                return STATUS_PHASES[i];
            }
        }
        return null;
    }

    private RoomRollbackEntry findRollbackEntry(String roomNumber) {
        for (int i = 1; i <= roomRollbackStacks.getNumberOfEntries(); i++) {
            RoomRollbackEntry entry = roomRollbackStacks.getEntry(i);
            if (entry.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return entry;
            }
        }
        return null;
    }

    private RoomRollbackEntry getOrCreateRollbackEntry(String roomNumber) {
        RoomRollbackEntry entry = findRollbackEntry(roomNumber);
        if (entry != null) {
            return entry;
        }
        entry = new RoomRollbackEntry(roomNumber);
        roomRollbackStacks.add(entry);
        return entry;
    }

    private void pushRollbackLog(String roomNumber, HousekeepingLog log) {
        getOrCreateRollbackEntry(roomNumber).getStack().push(log);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

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

        private StackInterface<HousekeepingLog> getStack() {
            return stack;
        }
    }
}
