package entity;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */
public class Room implements Serializable {
    private String roomNumber;
    private String roomType;
    private String cleanlinessStatus; // "Dirty", "Cleaning In Progress", "Inspected", "Ready"
    private String occupancyStatus; // "Vacant", "Occupied"
    private String lastUpdate;
    private String dirtySince;
    private String lastTurnaroundMinutes;

    public Room(String roomNumber, String roomType, String cleanlinessStatus, String lastUpdate) {
        this(roomNumber, roomType, cleanlinessStatus, "Vacant", lastUpdate, "N/A", "N/A");
    }

    public Room(String roomNumber, String roomType, String cleanlinessStatus, String lastUpdate,
            String dirtySince, String lastTurnaroundMinutes) {
        this(roomNumber, roomType, cleanlinessStatus, "Vacant", lastUpdate, dirtySince, lastTurnaroundMinutes);
    }

    public Room(String roomNumber, String roomType, String cleanlinessStatus, String occupancyStatus,
            String lastUpdate, String dirtySince, String lastTurnaroundMinutes) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.cleanlinessStatus = cleanlinessStatus;
        this.occupancyStatus = occupancyStatus;
        this.lastUpdate = lastUpdate;
        this.dirtySince = dirtySince;
        this.lastTurnaroundMinutes = lastTurnaroundMinutes;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getCleanlinessStatus() {
        return cleanlinessStatus;
    }

    public void setCleanlinessStatus(String cleanlinessStatus) {
        this.cleanlinessStatus = cleanlinessStatus;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getDirtySince() {
        return dirtySince;
    }

    public void setDirtySince(String dirtySince) {
        this.dirtySince = dirtySince;
    }

    public String getLastTurnaroundMinutes() {
        return lastTurnaroundMinutes;
    }

    public void setLastTurnaroundMinutes(String lastTurnaroundMinutes) {
        this.lastTurnaroundMinutes = lastTurnaroundMinutes;
    }

    @Override
    public String toString() {
        return String.format("Room %-6s | Type: %-12s | Clean: %-20s | Occupancy: %-10s | Last Update: %s",
                roomNumber, roomType, cleanlinessStatus, occupancyStatus, lastUpdate);
    }
}
