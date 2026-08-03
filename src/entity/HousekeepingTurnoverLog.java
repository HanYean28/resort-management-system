package entity;

import java.io.Serializable;

/**
 * To track the housekeeping one complete cycle from dirty to ready
 * @author Chang Han Yean
 */
public class HousekeepingTurnoverLog implements Serializable {
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private Room room;
    private String dirtyTimestamp;
    private String readyTimestamp;
    private double turnaroundMinutes;
    private String shiftLabel;
    private String recordStatus;
    private String cancellationTimestamp;

    public HousekeepingTurnoverLog(Room room, String dirtyTimestamp, String readyTimestamp,
            double turnaroundMinutes, String shiftLabel) {
        this(room, dirtyTimestamp, readyTimestamp, turnaroundMinutes, shiftLabel,
                STATUS_COMPLETED, "N/A");
    }

    public HousekeepingTurnoverLog(Room room, String dirtyTimestamp, String readyTimestamp,
            double turnaroundMinutes, String shiftLabel, String recordStatus,
            String cancellationTimestamp) {
        this.room = room;
        this.dirtyTimestamp = dirtyTimestamp;
        this.readyTimestamp = readyTimestamp;
        this.turnaroundMinutes = turnaroundMinutes;
        this.shiftLabel = shiftLabel;
        this.recordStatus = recordStatus;
        this.cancellationTimestamp = cancellationTimestamp;
    }

    public Room getRoom() {
        return room;
    }

    public String getDirtyTimestamp() {
        return dirtyTimestamp;
    }

    public String getReadyTimestamp() {
        return readyTimestamp;
    }

    public double getTurnaroundMinutes() {
        return turnaroundMinutes;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public String getCancellationTimestamp() {
        return cancellationTimestamp;
    }
}
