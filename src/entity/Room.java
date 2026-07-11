package entity;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */
public class Room implements Serializable {
    private String roomNumber;
    private String roomType;
    private String cleanlinessStatus; // "Dirty", "Cleaning In Progress", "Inspected", "Ready"

    public Room(String roomNumber, String roomType, String cleanlinessStatus) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.cleanlinessStatus = cleanlinessStatus;
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

    @Override
    public String toString() {
        return String.format("Room %-6s | Type: %-12s | Status: %-20s", roomNumber, roomType, cleanlinessStatus);
    }
}
