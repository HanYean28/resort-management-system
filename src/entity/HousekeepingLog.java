package entity;

import java.io.Serializable;

/**
 * Task history entry to track each time a room status changes.
 * @author Chang Han Yean
 */
public class HousekeepingLog implements Serializable {
    public static final String ACTION_UPDATE = "Update";
    public static final String ACTION_ROLLBACK = "Rollback";
    public static final String ACTION_LATE_CHECKOUT = "Late Checkout";

    private String roomNumber;
    private String oldStatus;
    private String newStatus;
    private String timestamp;
    private String action;

    public HousekeepingLog(String roomNumber, String oldStatus, String newStatus, String timestamp, String action) {
        this.roomNumber = roomNumber;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = timestamp;
        this.action = action;
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

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return String.format("[%s] Room %s %s from '%s' to '%s'",
                timestamp, roomNumber, action, oldStatus, newStatus);
    }
}
