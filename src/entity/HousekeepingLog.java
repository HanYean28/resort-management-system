package entity;

import java.io.Serializable;

/**
 * Task history entry to track each time a room status changes.
 * @author Chang Han Yean
 */
public class HousekeepingLog implements Serializable {
    private Room room;
    private String oldStatus;
    private String newStatus;
    private String timestamp;

    public HousekeepingLog(Room room, String oldStatus, String newStatus, String timestamp) {
        this.room = room;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp;
    }

    public Room getRoom() {
        return room;
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
                timestamp, room.getRoomNumber(), oldStatus, newStatus);
    }
}
