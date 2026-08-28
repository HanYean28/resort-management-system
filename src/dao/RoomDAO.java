package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Room;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles room file operations for rooms.txt.
 * @author Chang Han Yean
 */
public class RoomDAO {
    private static final String DATA_FILE = "rooms.txt";

    // Load room data
    // seperate each line data and one by one create object and save into list
    public ListInterface<Room> loadRooms() {
        ListInterface<Room> rooms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 7) {
                    String roomNumber = parts[0].trim();
                    String roomType = parts[1].trim();
                    String cleanlinessStatus = parts[2].trim();
                    String occupancyStatus = parts[3].trim();
                    String lastUpdate = parts[4].trim();
                    String dirtySince = parts[5].trim();
                    String lastTurnaroundMinutes = parts[6].trim();

                    if (isValidRoom(roomNumber, roomType, cleanlinessStatus, occupancyStatus,
                            lastUpdate, dirtySince, lastTurnaroundMinutes)
                            && !roomNumberExists(rooms, roomNumber)) {
                        rooms.add(new Room(roomNumber, roomType, cleanlinessStatus, occupancyStatus,
                                lastUpdate, dirtySince, lastTurnaroundMinutes));
                    }
                }
            }
        } catch (IOException e) {
            createFileIfMissing();
        }
        return rooms;
    }
    // save the current room list by overwriting the entire file
    public void saveRooms(ListInterface<Room> rooms) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) { // Overwrite the file because didnt put append true
            bw.write("# roomNumber|roomType|cleanlinessStatus|occupancyStatus|lastUpdate|dirtySince|lastTurnaroundMinutes");
            bw.newLine();
            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room room = rooms.getEntry(i);
                bw.write(room.getRoomNumber() + "|" + room.getRoomType() + "|"
                        + room.getCleanlinessStatus() + "|" + room.getOccupancyStatus() + "|"
                        + room.getLastUpdate() + "|" + room.getDirtySince() + "|"
                        + room.getLastTurnaroundMinutes());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean isValidRoom(String roomNumber, String roomType, String cleanlinessStatus, String occupancyStatus,
            String lastUpdate, String dirtySince, String lastTurnaroundMinutes) {
        return !roomNumber.isEmpty()
                && isValidRoomType(roomType)
                && isValidCleanlinessStatus(cleanlinessStatus)
                && isValidOccupancyStatus(occupancyStatus)
                && !lastUpdate.isEmpty()
                && !dirtySince.isEmpty()
                && !lastTurnaroundMinutes.isEmpty();
    }

    private boolean roomNumberExists(ListInterface<Room> rooms, String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            if (rooms.getEntry(i).getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidRoomType(String roomType) {
        return roomType.equalsIgnoreCase("Standard")
                || roomType.equalsIgnoreCase("Deluxe")
                || roomType.equalsIgnoreCase("Suite");
    }

    private boolean isValidCleanlinessStatus(String status) {
        return status.equalsIgnoreCase("Dirty")
                || status.equalsIgnoreCase("Cleaning In Progress")
                || status.equalsIgnoreCase("Inspected")
                || status.equalsIgnoreCase("Ready");
    }

    private boolean isValidOccupancyStatus(String status) {
        return status.equalsIgnoreCase("Vacant")
                || status.equalsIgnoreCase("Occupied");
    }

    private void createFileIfMissing() {
        saveRooms(new ArrayList<Room>());
    }
}
