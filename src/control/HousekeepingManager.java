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
public class HousekeepingManager {
    private static final String DATA_FILE = "rooms.txt";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private ListInterface<Room> rooms;
    private StackInterface<HousekeepingLog> rollbackStack;

    public HousekeepingManager() {
        rooms = new ArrayList<>();
        rollbackStack = new ArrayStack<>();
        loadRoomsFromFile();
    }

    /**
     * Loads rooms from rooms.txt file.
     */
    public void loadRoomsFromFile() {
        rooms.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Room room = new Room(parts[0], parts[1], parts[2], parts[3]);
                    rooms.add(room);
                } else if (parts.length == 3) {
                    Room room = new Room(parts[0], parts[1], parts[2], "N/A");
                    rooms.add(room);
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, start with empty list or print stack trace (utility/logging)
        }
    }

    /**
     * Saves rooms to rooms.txt file.
     */
    public void saveRoomsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room r = rooms.getEntry(i);
                bw.write(r.getRoomNumber() + "|" + r.getRoomType() + "|" + r.getCleanlinessStatus()
                        + "|" + r.getLastUpdate());
                bw.newLine();
            }
        } catch (IOException e) {
            // Handle logging or exception propagation
        }
    }

    /**
     * Retrieves all rooms.
     */
    public ListInterface<Room> getAllRooms() {
        return rooms;
    }

    /**
     * Retrieves a room by its number.
     */
    public Room getRoom(String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Updates room status sequentially.
     * Status transition: Dirty -> Cleaning In Progress -> Inspected -> Ready
     *
     * @return null if successful, otherwise an error message
     */
    public String updateRoomStatus(String roomNumber, String targetStatus) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        String currentStatus = room.getCleanlinessStatus();
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            return "Room is already '" + currentStatus + "'. No update needed.";
        }

        if (!isValidTransition(currentStatus, targetStatus)) {
            return "Invalid transition from '" + currentStatus + "' to '" + targetStatus + "'. "
                    + "Allowed next status: " + getAllowedNextStatuses(currentStatus) + ".";
        }

        // Apply status update
        String timestamp = getCurrentTimestamp();
        room.setCleanlinessStatus(targetStatus);
        room.setLastUpdate(timestamp);

        // Log the change and push to rollback stack
        HousekeepingLog log = new HousekeepingLog(roomNumber, currentStatus, targetStatus, timestamp);
        rollbackStack.push(log);

        // Save immediately to persistent storage
        saveRoomsToFile();
        return null;
    }

    /**
     * Returns the last status change on the rollback stack without undoing it.
     */
    public HousekeepingLog peekLastRollbackAction() {
        if (rollbackStack.isEmpty()) {
            return null;
        }
        return rollbackStack.peek();
    }

    /**
     * Undoes the last housekeeping status change.
     */
    public HousekeepingLog rollbackLastAction() {
        if (rollbackStack.isEmpty()) {
            return null; // Nothing to rollback
        }

        HousekeepingLog lastLog = rollbackStack.pop();
        Room room = getRoom(lastLog.getRoomNumber());
        if (room != null) {
            // Restore previous status
            room.setCleanlinessStatus(lastLog.getOldStatus());
            room.setLastUpdate(getCurrentTimestamp());
            saveRoomsToFile();
            return lastLog;
        }

        return null;
    }

    /**
     * Checks if the transition between two room statuses is valid.
     */
    private boolean isValidTransition(String current, String target) {
        if (current.equals("Dirty") && target.equals("Cleaning In Progress")) return true;
        if (current.equals("Cleaning In Progress") && target.equals("Inspected")) return true;
        if (current.equals("Inspected") && target.equals("Ready")) return true;
        
        // Allow restarting clean cycle if a room becomes Dirty again (e.g. from Ready -> Dirty)
        if (current.equals("Ready") && target.equals("Dirty")) return true;
        if (current.equals("Cleaning In Progress") && target.equals("Dirty")) return true;
        if (current.equals("Inspected") && target.equals("Dirty")) return true;
        
        return false;
    }

    private String getAllowedNextStatuses(String current) {
        if (current.equals("Dirty")) {
            return "Cleaning In Progress";
        }
        if (current.equals("Cleaning In Progress")) {
            return "Inspected or Dirty";
        }
        if (current.equals("Inspected")) {
            return "Ready or Dirty";
        }
        if (current.equals("Ready")) {
            return "Dirty";
        }
        return "unknown";
    }

    /**
     * Returns the valid target statuses for the given current status.
     */
    public String[] getAllowedTargetStatuses(String currentStatus) {
        if (currentStatus.equals("Dirty")) {
            return new String[] { "Cleaning In Progress" };
        }
        if (currentStatus.equals("Cleaning In Progress")) {
            return new String[] { "Inspected", "Dirty" };
        }
        if (currentStatus.equals("Inspected")) {
            return new String[] { "Ready", "Dirty" };
        }
        if (currentStatus.equals("Ready")) {
            return new String[] { "Dirty" };
        }
        return new String[0];
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
