package entity;

import adt.ListInterface;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */
public class HousekeepingAuditReport implements Serializable {
    private String generatedAt;
    private String reportTarget;
    private String dateWindow;
    private String firstCreatedAt;
    private String lastUpdatedAt;
    private int revisionNumber;
    private String roomTypeFilter;
    private String roomNumberFilter;
    private String transitionFilter;
    private String sortLabel;
    private ListInterface<HousekeepingLog> activityRows;
    private int totalEvents;
    private int completionCount;
    private int resetCount;
    private String mostActiveRoom;
    private String reportFileName;
    private String savedFilePath;

    public HousekeepingAuditReport(String generatedAt, String reportTarget, String dateWindow,
            String roomTypeFilter, String roomNumberFilter, String transitionFilter, String sortLabel,
            ListInterface<HousekeepingLog> activityRows, int totalEvents, int completionCount,
            int resetCount, String mostActiveRoom, String reportFileName) {
        this.generatedAt = generatedAt;
        this.reportTarget = reportTarget;
        this.dateWindow = dateWindow;
        this.roomTypeFilter = roomTypeFilter;
        this.roomNumberFilter = roomNumberFilter;
        this.transitionFilter = transitionFilter;
        this.sortLabel = sortLabel;
        this.activityRows = activityRows;
        this.totalEvents = totalEvents;
        this.completionCount = completionCount;
        this.resetCount = resetCount;
        this.mostActiveRoom = mostActiveRoom;
        this.reportFileName = reportFileName;
        this.firstCreatedAt = generatedAt;
        this.lastUpdatedAt = generatedAt;
        this.revisionNumber = 1;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public String getReportTarget() {
        return reportTarget;
    }

    public String getDateWindow() {
        return dateWindow;
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

    public String getRoomNumberFilter() {
        return roomNumberFilter;
    }

    public String getTransitionFilter() {
        return transitionFilter;
    }

    public String getSortLabel() {
        return sortLabel;
    }

    public ListInterface<HousekeepingLog> getActivityRows() {
        return activityRows;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public int getResetCount() {
        return resetCount;
    }

    public String getMostActiveRoom() {
        return mostActiveRoom;
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
