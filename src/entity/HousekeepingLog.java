package entity;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */

public class HousekeepingLog implements Serializable {
    private String roomNumber;
    private String oldStatus;
    private String newStatus;
    private String timestamp; // Formatted date/time of the status change

    public HousekeepingLog(String roomNumber, String oldStatus, String newStatus, String timestamp) {
        this.roomNumber = roomNumber;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] Room %s status changed from '%s' to '%s'", 
                timestamp, roomNumber, oldStatus, newStatus);
    }
}
