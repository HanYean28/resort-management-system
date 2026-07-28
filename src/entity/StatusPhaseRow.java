package entity;

import java.io.Serializable;

/**
 * @author Chang Han Yean
 */
public class StatusPhaseRow implements Serializable {
    private String statusLabel;
    private int count;
    private double percentage;
    private String visualBar;

    public StatusPhaseRow(String statusLabel, int count, double percentage, String visualBar) {
        this.statusLabel = statusLabel;
        this.count = count;
        this.percentage = percentage;
        this.visualBar = visualBar;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public int getCount() {
        return count;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getVisualBar() {
        return visualBar;
    }
}
