package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.HousekeepingLog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles housekeeping history file operations.
 * @author Chang Han Yean
 */
public class HousekeepingLogDAO {
    private static final String DATA_FILE = "housekeeping_task_history.txt";

    // Load housekeeping history records from file.
    // seperate each line and create object save into list
    public ListInterface<HousekeepingLog> loadLogs() {
        ListInterface<HousekeepingLog> logs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) { // read each line until no line
                if (line.trim().isEmpty() || line.trim().startsWith("#")) { // Skip empty lines and header lines.
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 5) {
                    String roomNumber = parts[0].trim();
                    String oldStatus = parts[1].trim();
                    String newStatus = parts[2].trim();
                    String timestamp = parts[3].trim();
                    String action = parts[4].trim();

                    if (isValidLog(roomNumber, oldStatus, newStatus, timestamp, action)) {
                        logs.add(new HousekeepingLog(roomNumber, oldStatus, newStatus, timestamp, action));
                    }
                }
            }
        } catch (IOException e) {
            createFileIfMissing();
        }
        return logs;
    }

    // add one new history record to the file.
    public void appendLog(HousekeepingLog log) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE, true))) { // append:true => add new row at the end 
            bw.write(log.getRoomNumber() + "|" + log.getOldStatus() + "|"
                    + log.getNewStatus() + "|" + log.getTimestamp() + "|" + log.getAction());
            bw.newLine();
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    // Create housekeeping_task_history.txt with a header if it is missing.
    private void createFileIfMissing() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bw.write("# roomNumber|oldStatus|newStatus|timestamp|action");
            bw.newLine();
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean isValidLog(String roomNumber, String oldStatus, String newStatus, String timestamp,
            String action) {
        return !roomNumber.isEmpty()
                && isValidCleanlinessStatus(oldStatus)
                && isValidCleanlinessStatus(newStatus)
                && !timestamp.isEmpty()
                && isValidAction(action);
    }

    private boolean isValidCleanlinessStatus(String status) {
        return status.equalsIgnoreCase("Dirty")
                || status.equalsIgnoreCase("Cleaning In Progress")
                || status.equalsIgnoreCase("Inspected")
                || status.equalsIgnoreCase("Ready");
    }

    private boolean isValidAction(String action) {
        return action.equalsIgnoreCase(HousekeepingLog.ACTION_UPDATE)
                || action.equalsIgnoreCase(HousekeepingLog.ACTION_ROLLBACK)
                || action.equalsIgnoreCase(HousekeepingLog.ACTION_LATE_CHECKOUT);
    }
}
