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
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
                }
            }
        } catch (IOException e) {
            // Return empty list if file is missing.
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
}
