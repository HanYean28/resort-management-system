package entity;

import adt.ListInterface;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */
public class HousekeepingShiftReport implements Serializable {
    private String generatedAt;
    private String shiftTarget;
    private String shiftLabel;
    private String shiftDateWindow;
    private String statusSnapshotAt;
    private String firstCreatedAt;
    private String lastUpdatedAt;
    private int revisionNumber;
    private String roomTypeFilter;
    private ListInterface<StatusPhaseRow> phaseRows;
    private int totalRooms;
    private double averageTurnaroundMinutes;
    private int turnaroundSampleSize;
    private double benchmarkMinutes;
    private boolean onTarget;
    private String speedRating;
    private String strategyNote;
    private String reportFileName;
    private String savedFilePath;

    public HousekeepingShiftReport(String generatedAt, String shiftTarget, String shiftLabel,
            String shiftDateWindow, String snapshotAt, String roomTypeFilter,
            ListInterface<StatusPhaseRow> phaseRows, int totalRooms, double averageTurnaroundMinutes,
            int turnaroundSampleSize, double benchmarkMinutes, boolean onTarget, String speedRating,
            String strategyNote, String reportFileName) {
        this.generatedAt = generatedAt;
        this.shiftTarget = shiftTarget;
        this.shiftLabel = shiftLabel;
        this.shiftDateWindow = shiftDateWindow;
        this.statusSnapshotAt = snapshotAt;
        this.roomTypeFilter = roomTypeFilter;
        this.phaseRows = phaseRows;
        this.totalRooms = totalRooms;
        this.averageTurnaroundMinutes = averageTurnaroundMinutes;
        this.turnaroundSampleSize = turnaroundSampleSize;
        this.benchmarkMinutes = benchmarkMinutes;
        this.onTarget = onTarget;
        this.speedRating = speedRating;
        this.strategyNote = strategyNote;
        this.reportFileName = reportFileName;
        this.firstCreatedAt = generatedAt;
        this.lastUpdatedAt = generatedAt;
        this.revisionNumber = 1;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public String getShiftTarget() {
        return shiftTarget;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public String getShiftDateWindow() {
        return shiftDateWindow;
    }

    public String getStatusSnapshotAt() {
        return statusSnapshotAt;
    }

    public String getFirstCreatedAt() {
        return firstCreatedAt;
    }

    public void setFirstCreatedAt(String firstCreatedAt) {
        this.firstCreatedAt = firstCreatedAt;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public String getRoomTypeFilter() {
        return roomTypeFilter;
    }

    public ListInterface<StatusPhaseRow> getPhaseRows() {
        return phaseRows;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public double getAverageTurnaroundMinutes() {
        return averageTurnaroundMinutes;
    }

    public int getTurnaroundSampleSize() {
        return turnaroundSampleSize;
    }

    public double getBenchmarkMinutes() {
        return benchmarkMinutes;
    }

    public boolean isOnTarget() {
        return onTarget;
    }

    public String getSpeedRating() {
        return speedRating;
    }

    public String getStrategyNote() {
        return strategyNote;
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public String getSavedFilePath() {
        return savedFilePath;
    }

    public void setSavedFilePath(String savedFilePath) {
        this.savedFilePath = savedFilePath;
    }
}
