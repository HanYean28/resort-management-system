package entity;

import java.io.Serializable;

/**
 * To track the housekeeping one complete cycle from dirty to ready
 * @author Chang Han Yean
 */
public class HousekeepingTurnoverLog implements Serializable {
    private Room room;
    private String dirtyTimestamp;
    private String readyTimestamp;
    private double turnaroundMinutes;
    private String shiftLabel;

    public HousekeepingTurnoverLog(Room room, String dirtyTimestamp, String readyTimestamp,
            double turnaroundMinutes, String shiftLabel) {
        this.room = room;
        this.dirtyTimestamp = dirtyTimestamp;
        this.readyTimestamp = readyTimestamp;
        this.turnaroundMinutes = turnaroundMinutes;
        this.shiftLabel = shiftLabel;
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
}
