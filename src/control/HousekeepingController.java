package control;

import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import entity.HousekeepingAuditReport;
import entity.HousekeepingLog;
import entity.HousekeepingShiftReport;
import entity.HousekeepingTurnoverLog;
import entity.Room;
import entity.StatusPhaseRow;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Chang Han Yean
 */
public class HousekeepingController {
    // --- File paths & report settings (internal) ---
    private static final String DATA_FILE = "rooms.txt";
    private static final String TURNOVER_LOG_FILE = "housekeeping_turnover_log.txt";
    private static final String AUDIT_LOG_FILE = "housekeeping_audit_log.txt";   // every status change (Report 2 source)
    private static final String REPORT_DIR = "reports" + File.separator + "housekeeping" + File.separator;
    private static final double BENCHMARK_MINUTES = 45.0;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // --- Shift window (Report 1 & 2) — which time period to analyse ---
    public static final String SHIFT_CURRENT = "CURRENT";
    public static final String SHIFT_MORNING = "MORNING";
    public static final String SHIFT_AFTERNOON = "AFTERNOON";
    public static final String SHIFT_NIGHT = "NIGHT";

    // --- Generic filter sentinel — means "no filter on this field" ---
    public static final String FILTER_ALL = "ALL";

    // --- Report 2: transition filter — which status changes to include ---
    public static final String TRANSITION_ALL = "ALL";
    public static final String TRANSITION_TO_READY = "TO_READY";
    public static final String TRANSITION_TO_DIRTY = "TO_DIRTY";
    public static final String TRANSITION_COMPLETION = "COMPLETION";

    // --- Report 2: sort order for audit activity rows ---
    public static final String SORT_TIMESTAMP_DESC = "TIMESTAMP_DESC";
    public static final String SORT_TIMESTAMP_ASC = "TIMESTAMP_ASC";
    public static final String SORT_ROOM_NUMBER = "ROOM_NUMBER";

    // --- Report 2: audit scope — what subset of rooms to show ---
    public static final String SCOPE_ROOM = "ROOM";
    public static final String SCOPE_ROOM_TYPE = "ROOM_TYPE";
    public static final String SCOPE_SHIFT = "SHIFT";

    private static final String[] STATUS_PHASES = {
            "Dirty",
            "Cleaning In Progress",
            "Inspected",
            "Ready"
    };

    private ListInterface<Room> rooms;
    private ListInterface<HousekeepingTurnoverLog> turnoverLogs;
    private ListInterface<HousekeepingLog> auditLogs;
    private StackInterface<HousekeepingLog> rollbackStack;

    public HousekeepingController() {
        rooms = new ArrayList<>();
        turnoverLogs = new ArrayList<>();
        auditLogs = new ArrayList<>();
        rollbackStack = new ArrayStack<>();
        loadRoomsFromFile();
        loadTurnoverLogsFromFile();
        loadAuditLogsFromFile();
    }

    /**
     * Loads rooms from rooms.txt file.
     */
    public void loadRoomsFromFile() {
        rooms.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                } else if (parts.length == 4) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], parts[3]));
                } else if (parts.length == 3) {
                    rooms.add(new Room(parts[0], parts[1], parts[2], "N/A"));
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, start with empty list
        }
    }

    /**
     * Saves rooms to rooms.txt file.
     */
    public void saveRoomsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room r = rooms.getEntry(i);
                bw.write(r.getRoomNumber() + "|" + r.getRoomType() + "|" + r.getCleanlinessStatus()
                        + "|" + r.getLastUpdate() + "|" + r.getDirtySince() + "|" + r.getLastTurnaroundMinutes());
                bw.newLine();
            }
        } catch (IOException e) {
            // Handle logging or exception propagation
        }
    }

    private void loadTurnoverLogsFromFile() {
        turnoverLogs.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(TURNOVER_LOG_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    Room room = getRoom(parts[0]);
                    if (room != null) {
                        turnoverLogs.add(new HousekeepingTurnoverLog(
                                room, parts[2], parts[3],
                                Double.parseDouble(parts[4]), parts[5]));
                    }
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, start with empty list
        }
    }

    private void appendTurnoverLog(HousekeepingTurnoverLog log) {
        turnoverLogs.add(log);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TURNOVER_LOG_FILE, true))) {
            bw.write(log.getRoom().getRoomNumber() + "|" + log.getRoom().getRoomType() + "|" + log.getDirtyTimestamp()
                    + "|" + log.getReadyTimestamp() + "|" + String.format("%.1f", log.getTurnaroundMinutes())
                    + "|" + log.getShiftLabel());
            bw.newLine();
        } catch (IOException e) {
            // Handle logging or exception propagation
        }
    }

    /**
     * Retrieves all rooms.
     */
    public ListInterface<Room> getAllRooms() {
        return rooms;
    }

    /**
     * Retrieves a room by its number.
     */
    public Room getRoom(String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            if (r.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Updates room status sequentially.
     * Status transition: Dirty -> Cleaning In Progress -> Inspected -> Ready
     *
     * @return null if successful, otherwise an error message
     */
    public String updateRoomStatus(String roomNumber, String targetStatus) {
        Room room = getRoom(roomNumber);
        if (room == null) {
            return "Room not found.";
        }

        String currentStatus = room.getCleanlinessStatus();
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            return "Room is already '" + currentStatus + "'. No update needed.";
        }

        if (!isValidTransition(currentStatus, targetStatus)) {
            return "Invalid transition from '" + currentStatus + "' to '" + targetStatus + "'. "
                    + "Allowed next status: " + getAllowedNextStatuses(currentStatus) + ".";
        }

        String timestamp = getCurrentTimestamp();
        room.setCleanlinessStatus(targetStatus);
        room.setLastUpdate(timestamp);

        if (targetStatus.equals("Dirty")) {
            room.setDirtySince(timestamp);
            room.setLastTurnaroundMinutes("N/A");
        } else if (targetStatus.equals("Ready") && currentStatus.equals("Inspected")) {
            recordTurnoverCompletion(room, timestamp);
        }

        HousekeepingLog log = new HousekeepingLog(room, currentStatus, targetStatus, timestamp);
        rollbackStack.push(log);
        appendAuditLog(log);
        saveRoomsToFile();
        return null;
    }

    private void loadAuditLogsFromFile() {
        auditLogs.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(AUDIT_LOG_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    Room room = getRoom(parts[0]);
                    if (room != null) {
                        auditLogs.add(new HousekeepingLog(room, parts[1], parts[2], parts[3]));
                    }
                }
            }
        } catch (IOException e) {
            // If file doesn't exist, start with empty list
        }
    }

    private void appendAuditLog(HousekeepingLog log) {
        auditLogs.add(log);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(AUDIT_LOG_FILE, true))) {
            bw.write(log.getRoom().getRoomNumber() + "|" + log.getOldStatus() + "|" + log.getNewStatus()
                    + "|" + log.getTimestamp());
            bw.newLine();
        } catch (IOException e) {
            // Handle logging or exception propagation
        }
    }

    private void recordTurnoverCompletion(Room room, String readyTimestamp) {
        if (!isValidTimestamp(room.getDirtySince())) {
            return;
        }

        double minutes = minutesBetween(room.getDirtySince(), readyTimestamp);
        room.setLastTurnaroundMinutes(String.format("%.1f", minutes));

        LocalDateTime readyDateTime = LocalDateTime.parse(readyTimestamp, TIMESTAMP_FORMAT);
        String shiftLabel = resolveShiftLabel(readyDateTime);
        HousekeepingTurnoverLog turnoverLog = new HousekeepingTurnoverLog(
                room,
                room.getDirtySince(),
                readyTimestamp,
                minutes,
                shiftLabel);
        appendTurnoverLog(turnoverLog);
    }

    /**
     * Generates Report 1 with search, filter, and sort applied to turnover and room data.
     */
    public HousekeepingShiftReport generateShiftTurnoverReport(String shiftCode, String roomTypeFilter) {
        LocalDateTime now = LocalDateTime.now();
        String resolvedShift = shiftCode.equals(SHIFT_CURRENT) ? detectCurrentShiftCode(now) : shiftCode;
        LocalDateTime[] window = resolveShiftWindow(shiftCode, now);
        LocalDateTime snapshotPoint = resolveSnapshotPoint(now, window[0], window[1]);
        String shiftLabel = resolveShiftLabel(resolvedShift);
        String shiftDateWindow = window[0].format(TIMESTAMP_FORMAT) + " to " + window[1].format(TIMESTAMP_FORMAT);
        String statusSnapshotAt = snapshotPoint.format(TIMESTAMP_FORMAT);
        String filterLabel = roomTypeFilter.equals(FILTER_ALL) ? "All Room Types" : roomTypeFilter;

        ListInterface<Room> snapshotRooms = reconstructRoomsAt(snapshotPoint);
        ListInterface<Room> filteredRooms = filterRoomsByType(snapshotRooms, roomTypeFilter);
        ListInterface<StatusPhaseRow> phaseRows = buildStatusPhaseBreakdown(filteredRooms);
        ListInterface<HousekeepingTurnoverLog> shiftTurnovers = searchTurnoversForShift(shiftCode, roomTypeFilter);

        double averageMinutes = 0.0;
        int sampleSize = shiftTurnovers.getNumberOfEntries();
        if (sampleSize > 0) {
            double total = 0.0;
            for (int i = 1; i <= sampleSize; i++) {
                total += shiftTurnovers.getEntry(i).getTurnaroundMinutes();
            }
            averageMinutes = total / sampleSize;
        }

        boolean onTarget = sampleSize > 0 && averageMinutes <= BENCHMARK_MINUTES;
        String speedRating = resolveSpeedRating(averageMinutes, sampleSize);
        String strategyNote = buildHandoverStrategy(filteredRooms, averageMinutes, sampleSize, onTarget);
        String reportFileName = buildReportFileName(resolvedShift, window[0]);
        String shiftTarget = buildShiftTargetLabel(resolvedShift, window[0]);

        HousekeepingShiftReport report = new HousekeepingShiftReport(
                now.format(TIMESTAMP_FORMAT),
                shiftTarget,
                shiftLabel,
                shiftDateWindow,
                statusSnapshotAt,
                filterLabel,
                phaseRows,
                filteredRooms.getNumberOfEntries(),
                averageMinutes,
                sampleSize,
                BENCHMARK_MINUTES,
                onTarget,
                speedRating,
                strategyNote,
                reportFileName);
        enrichRevisionMetadata(report);
        return report;
    }

    /**
     * Returns true if the authoritative shift report file already exists.
     */
    public boolean reportFileExists(HousekeepingShiftReport report) {
        return new File(REPORT_DIR + report.getReportFileName()).exists();
    }

    public String getReportDisplayPath(HousekeepingShiftReport report) {
        return formatReportDisplayPath(report.getReportFileName());
    }

    /**
     * Persists the report to the single authoritative file for this shift (overwrite mode).
     */
    public String saveShiftTurnoverReport(HousekeepingShiftReport report) {
        enrichRevisionMetadata(report);
        String fileName = report.getReportFileName();
        String filePath = REPORT_DIR + fileName;

        File directory = new File(REPORT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        ListInterface<String> lines = buildReportLines(report);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, false))) {
            for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
                bw.write(lines.getEntry(i));
                bw.newLine();
            }
        } catch (IOException e) {
            return null;
        }

        String displayPath = formatReportDisplayPath(fileName);
        report.setSavedFilePath(displayPath);
        return displayPath;
    }

    private void enrichRevisionMetadata(HousekeepingShiftReport report) {
        File file = new File(REPORT_DIR + report.getReportFileName());
        String now = getCurrentTimestamp();

        if (!file.exists()) {
            report.setFirstCreatedAt(now);
            report.setLastUpdatedAt(now);
            report.setRevisionNumber(1);
            return;
        }

        String firstCreated = parseFirstCreatedFromFile(file);
        if (firstCreated == null) {
            firstCreated = now;
        }

        int existingRevision = parseRevisionFromFile(file);
        report.setFirstCreatedAt(firstCreated);
        report.setLastUpdatedAt(now);
        report.setRevisionNumber(existingRevision + 1);
    }

    private String parseFirstCreatedFromFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("First Created:")) {
                    return line.substring("First Created:".length()).trim();
                }
            }
        } catch (IOException e) {
            // Fall back to new report metadata
        }
        return null;
    }

    private int parseRevisionFromFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                int marker = line.indexOf("(Revision #");
                if (marker >= 0) {
                    int start = marker + "(Revision #".length();
                    int end = line.indexOf(")", start);
                    if (end > start) {
                        return Integer.parseInt(line.substring(start, end).trim());
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Fall back to revision 0 so next save becomes revision 1
        }
        return 0;
    }

    private String buildShiftTargetLabel(String shiftCode, LocalDateTime windowStart) {
        String shiftName;
        if (shiftCode.equals(SHIFT_MORNING)) {
            shiftName = "Morning";
        } else if (shiftCode.equals(SHIFT_AFTERNOON)) {
            shiftName = "Afternoon";
        } else {
            shiftName = "Night";
        }
        return windowStart.format(REPORT_DATE_FORMAT) + " " + shiftName + " Shift";
    }

    private String formatReportDisplayPath(String fileName) {
        return ("reports/housekeeping/" + fileName).replace('\\', '/');
    }

    /**
     * Builds formatted report lines for console display or file export.
     */
    public ListInterface<String> buildReportLines(HousekeepingShiftReport report) {
        ListInterface<String> lines = new ArrayList<>();

        lines.add("==================================================================");
        lines.add("        REPORT 1: HOUSEKEEPING SHIFT TURNOVER PERFORMANCE");
        lines.add("==================================================================");
        lines.add("Shift Target : " + report.getShiftTarget());
        lines.add("First Created: " + report.getFirstCreatedAt());
        lines.add("Last Updated : " + report.getLastUpdatedAt() + " (Revision #" + report.getRevisionNumber() + ")");
        lines.add("Date Window  : " + report.getShiftDateWindow());
        lines.add("Status Snapshot: " + report.getStatusSnapshotAt());
        lines.add("Filter       : " + report.getRoomTypeFilter());
        lines.add("------------------------------------------------------------------");
        lines.add("");
        lines.add(" [RESORT CAPACITY & PHASE BREAKDOWN]");
        lines.add("");
        lines.add("  STATUS PHASE            COUNT   PERCENTAGE   TURNOVER VISUAL");
        lines.add("  --------------------------------------------------------------");

        for (int i = 1; i <= report.getPhaseRows().getNumberOfEntries(); i++) {
            StatusPhaseRow row = report.getPhaseRows().getEntry(i);
            String displayStatus = formatStatusForReport(row.getStatusLabel());
            lines.add(String.format("  %-22s [ %2d ]   %6.1f%%      %s",
                    displayStatus, row.getCount(), row.getPercentage(), row.getVisualBar()));
        }

        lines.add("  --------------------------------------------------------------");
        lines.add(String.format("  Total Cataloged Rooms  [ %2d ]   %6.1f%%",
                report.getTotalRooms(), report.getTotalRooms() > 0 ? 100.0 : 0.0));
        lines.add("");
        lines.add(" [SHIFT VELOCITY METRICS]");
        if (report.getTurnaroundSampleSize() == 0) {
            lines.add("  - Average Turnaround Time : N/A (no completed turnovers in selected shift)");
        } else {
            lines.add(String.format("  - Average Turnaround Time : %.1f mins (Checkout --> Ready)",
                    report.getAverageTurnaroundMinutes()));
        }
        lines.add(String.format("  - Target Industry Benchmark: Under %.1f mins [%s]",
                report.getBenchmarkMinutes(), resolveBenchmarkStatus(report)));
        lines.add("  - Turnover Speed Rating   : " + report.getSpeedRating());
        lines.add("");
        lines.add(" [SHIFT HANDOVER STRATEGY]");
        lines.add("  " + report.getStrategyNote());
        lines.add("------------------------------------------------------------------");

        return lines;
    }

    private LocalDateTime resolveSnapshotPoint(LocalDateTime now, LocalDateTime shiftStart,
            LocalDateTime shiftEnd) {
        if (now.isBefore(shiftStart)) {
            return shiftEnd;
        }
        if (now.isAfter(shiftEnd)) {
            return shiftEnd;
        }
        return now;
    }

    private ListInterface<Room> reconstructRoomsAt(LocalDateTime snapshotPoint) {
        ListInterface<Room> snapshot = cloneAllRooms();
        ListInterface<HousekeepingLog> logsAfterSnapshot = searchAuditLogsAfter(snapshotPoint);
        selectionSortLogsByTimestampDescending(logsAfterSnapshot);

        for (int i = 1; i <= logsAfterSnapshot.getNumberOfEntries(); i++) {
            HousekeepingLog log = logsAfterSnapshot.getEntry(i);
            Room room = findRoomInList(snapshot, log.getRoom().getRoomNumber());
            if (room != null) {
                room.setCleanlinessStatus(log.getOldStatus());
                room.setLastUpdate(log.getTimestamp());
            }
        }

        applyHistoricalFallback(snapshot, snapshotPoint);
        return snapshot;
    }

    private ListInterface<Room> cloneAllRooms() {
        ListInterface<Room> clones = new ArrayList<>();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            clones.add(cloneRoom(rooms.getEntry(i)));
        }
        return clones;
    }

    private Room cloneRoom(Room room) {
        return new Room(room.getRoomNumber(), room.getRoomType(), room.getCleanlinessStatus(),
                room.getLastUpdate(), room.getDirtySince(), room.getLastTurnaroundMinutes());
    }

    private Room findRoomInList(ListInterface<Room> list, String roomNumber) {
        for (int i = 1; i <= list.getNumberOfEntries(); i++) {
            Room room = list.getEntry(i);
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private ListInterface<HousekeepingLog> searchAuditLogsAfter(LocalDateTime snapshotPoint) {
        ListInterface<HousekeepingLog> matched = new ArrayList<>();
        for (int i = 1; i <= auditLogs.getNumberOfEntries(); i++) {
            HousekeepingLog log = auditLogs.getEntry(i);
            if (!isValidTimestamp(log.getTimestamp())) {
                continue;
            }
            LocalDateTime changeTime = LocalDateTime.parse(log.getTimestamp(), TIMESTAMP_FORMAT);
            if (changeTime.isAfter(snapshotPoint)) {
                matched.add(log);
            }
        }
        return matched;
    }

    private void applyHistoricalFallback(ListInterface<Room> snapshot, LocalDateTime snapshotPoint) {
        for (int i = 1; i <= snapshot.getNumberOfEntries(); i++) {
            Room room = snapshot.getEntry(i);
            if (!isValidTimestamp(room.getLastUpdate())) {
                continue;
            }
            LocalDateTime lastUpdate = LocalDateTime.parse(room.getLastUpdate(), TIMESTAMP_FORMAT);
            if (!lastUpdate.isAfter(snapshotPoint)) {
                continue;
            }

            HousekeepingLog latestBefore = findLatestAuditLogBefore(room.getRoomNumber(), snapshotPoint);
            if (latestBefore != null) {
                room.setCleanlinessStatus(latestBefore.getNewStatus());
                room.setLastUpdate(latestBefore.getTimestamp());
            }
        }
    }

    private HousekeepingLog findLatestAuditLogBefore(String roomNumber, LocalDateTime snapshotPoint) {
        HousekeepingLog latest = null;
        LocalDateTime latestTime = null;

        for (int i = 1; i <= auditLogs.getNumberOfEntries(); i++) {
            HousekeepingLog log = auditLogs.getEntry(i);
            if (!log.getRoom().getRoomNumber().equalsIgnoreCase(roomNumber)) {
                continue;
            }
            if (!isValidTimestamp(log.getTimestamp())) {
                continue;
            }
            LocalDateTime changeTime = LocalDateTime.parse(log.getTimestamp(), TIMESTAMP_FORMAT);
            if (changeTime.isAfter(snapshotPoint)) {
                continue;
            }
            if (latest == null || changeTime.isAfter(latestTime)) {
                latest = log;
                latestTime = changeTime;
            }
        }
        return latest;
    }

    private void selectionSortLogsByTimestampDescending(ListInterface<HousekeepingLog> list) {
        int n = list.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j <= n; j++) {
                LocalDateTime timeJ = LocalDateTime.parse(list.getEntry(j).getTimestamp(), TIMESTAMP_FORMAT);
                LocalDateTime timeMax = LocalDateTime.parse(list.getEntry(maxIndex).getTimestamp(), TIMESTAMP_FORMAT);
                if (timeJ.isAfter(timeMax)) {
                    maxIndex = j;
                }
            }
            if (maxIndex != i) {
                HousekeepingLog temp = list.getEntry(i);
                list.replace(i, list.getEntry(maxIndex));
                list.replace(maxIndex, temp);
            }
        }
    }

    private ListInterface<Room> filterRoomsByType(ListInterface<Room> source, String roomTypeFilter) {
        ListInterface<Room> filtered = new ArrayList<>();
        for (int i = 1; i <= source.getNumberOfEntries(); i++) {
            Room room = source.getEntry(i);
            if (roomTypeFilter.equals(FILTER_ALL)
                    || room.getRoomType().equalsIgnoreCase(roomTypeFilter)) {
                filtered.add(room);
            }
        }
        insertionSortRoomsByNumber(filtered);
        return filtered;
    }

    private ListInterface<StatusPhaseRow> buildStatusPhaseBreakdown(ListInterface<Room> filteredRooms) {
        ListInterface<StatusPhaseRow> rows = new ArrayList<>();
        int total = filteredRooms.getNumberOfEntries();

        for (int p = 0; p < STATUS_PHASES.length; p++) {
            String phase = STATUS_PHASES[p];
            int count = 0;
            for (int i = 1; i <= total; i++) {
                if (filteredRooms.getEntry(i).getCleanlinessStatus().equals(phase)) {
                    count++;
                }
            }
            double percentage = total > 0 ? (count * 100.0) / total : 0.0;
            rows.add(new StatusPhaseRow(phase, count, percentage, buildVisualBar(percentage)));
        }

        return rows;
    }

    private ListInterface<HousekeepingTurnoverLog> searchTurnoversForShift(String shiftCode, String roomTypeFilter) {
        ListInterface<HousekeepingTurnoverLog> matched = new ArrayList<>();
        LocalDateTime reference = LocalDateTime.now();
        LocalDateTime[] window = resolveShiftWindow(shiftCode, reference);
        LocalDateTime shiftStart = window[0];
        LocalDateTime shiftEnd = window[1];

        for (int i = 1; i <= turnoverLogs.getNumberOfEntries(); i++) {
            HousekeepingTurnoverLog log = turnoverLogs.getEntry(i);
            if (!roomTypeFilter.equals(FILTER_ALL)
                    && !log.getRoom().getRoomType().equalsIgnoreCase(roomTypeFilter)) {
                continue;
            }

            if (!isValidTimestamp(log.getReadyTimestamp())) {
                continue;
            }

            LocalDateTime readyTime = LocalDateTime.parse(log.getReadyTimestamp(), TIMESTAMP_FORMAT);
            if (!readyTime.isBefore(shiftStart) && !readyTime.isAfter(shiftEnd)) {
                matched.add(log);
            }
        }

        return matched;
    }

    private void insertionSortRoomsByNumber(ListInterface<Room> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            Room key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && list.getEntry(j).getRoomNumber().compareToIgnoreCase(key.getRoomNumber()) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private String buildHandoverStrategy(ListInterface<Room> filteredRooms, double averageMinutes,
            int sampleSize, boolean onTarget) {
        StringBuilder strategy = new StringBuilder();

        if (sampleSize == 0) {
            strategy.append("No completed room turnovers recorded for this shift window yet. ");
        } else if (onTarget) {
            strategy.append("Average turnover speed is within target benchmarks. ");
        } else {
            strategy.append("Average turnover exceeds the benchmark. Escalate supervisor support and ");
            strategy.append("re-prioritize attendant assignments immediately. ");
        }

        ListInterface<Room> dirtyRooms = new ArrayList<>();
        for (int i = 1; i <= filteredRooms.getNumberOfEntries(); i++) {
            Room room = filteredRooms.getEntry(i);
            if (room.getCleanlinessStatus().equals("Dirty")) {
                dirtyRooms.add(room);
            }
        }
        insertionSortRoomsByNumber(dirtyRooms);

        if (dirtyRooms.isEmpty()) {
            strategy.append("All filtered rooms have progressed beyond the Dirty phase.");
        } else {
            strategy.append("Focus remaining attendants on the ").append(dirtyRooms.getNumberOfEntries())
                    .append(" 'Dirty' room(s)");
            if (dirtyRooms.getNumberOfEntries() == 1) {
                Room room = dirtyRooms.getEntry(1);
                strategy.append(" (Room ").append(room.getRoomNumber()).append(", ")
                        .append(room.getRoomType()).append(")");
            } else {
                strategy.append(" including Room ").append(dirtyRooms.getEntry(1).getRoomNumber())
                        .append(" (").append(dirtyRooms.getEntry(1).getRoomType()).append(")");
            }
            strategy.append(" ahead of upcoming check-ins.");
        }

        if (sampleSize > 0 && averageMinutes > 0) {
            strategy.append(String.format(" Observed shift average: %.1f minutes.", averageMinutes));
        }

        return strategy.toString();
    }

    private String buildVisualBar(double percentage) {
        int barLength = 16;
        int filled = (int) Math.round((percentage / 100.0) * barLength);
        if (filled > barLength) {
            filled = barLength;
        }
        if (filled < 0) {
            filled = 0;
        }

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? '\u2588' : '\u2591');
        }
        return bar.toString();
    }

    private String resolveSpeedRating(double averageMinutes, int sampleSize) {
        if (sampleSize == 0) {
            return "Insufficient Data";
        }
        if (averageMinutes <= BENCHMARK_MINUTES) {
            return "Optimal";
        }
        if (averageMinutes <= 60.0) {
            return "Moderate";
        }
        return "Below Standard";
    }

    private String formatStatusForReport(String status) {
        if (status.equals("Ready")) {
            return "Ready for Check-In";
        }
        return status;
    }

    private String detectCurrentShiftCode(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        if (hour >= 7 && hour <= 14) {
            return SHIFT_MORNING;
        }
        if (hour >= 15 && hour <= 22) {
            return SHIFT_AFTERNOON;
        }
        return SHIFT_NIGHT;
    }

    /**
     * Resolves the shift time window for report filtering.
     * Explicit shift selection uses the most recent occurrence of that shift
     * (e.g. Night Shift at 10:00 resolves to last night 23:00-06:59, not tonight).
     */
    private LocalDateTime[] resolveShiftWindow(String shiftCode, LocalDateTime reference) {
        String resolvedCode = shiftCode.equals(SHIFT_CURRENT)
                ? detectCurrentShiftCode(reference)
                : shiftCode;

        LocalDate date = reference.toLocalDate();
        int hour = reference.getHour();
        LocalDateTime start;
        LocalDateTime end;

        if (resolvedCode.equals(SHIFT_MORNING)) {
            LocalDate shiftDate = hour >= 7 ? date : date.minusDays(1);
            start = LocalDateTime.of(shiftDate, LocalTime.of(7, 0));
            end = LocalDateTime.of(shiftDate, LocalTime.of(14, 59, 59));
        } else if (resolvedCode.equals(SHIFT_AFTERNOON)) {
            LocalDate shiftDate = hour >= 15 ? date : date.minusDays(1);
            start = LocalDateTime.of(shiftDate, LocalTime.of(15, 0));
            end = LocalDateTime.of(shiftDate, LocalTime.of(22, 59, 59));
        } else {
            if (hour >= 23) {
                start = LocalDateTime.of(date, LocalTime.of(23, 0));
                end = LocalDateTime.of(date.plusDays(1), LocalTime.of(6, 59, 59));
            } else {
                start = LocalDateTime.of(date.minusDays(1), LocalTime.of(23, 0));
                end = LocalDateTime.of(date, LocalTime.of(6, 59, 59));
            }
        }

        return new LocalDateTime[] { start, end };
    }

    private String buildReportFileName(String shiftCode, LocalDateTime windowStart) {
        String shiftName;
        if (shiftCode.equals(SHIFT_MORNING)) {
            shiftName = "Morning";
        } else if (shiftCode.equals(SHIFT_AFTERNOON)) {
            shiftName = "Afternoon";
        } else {
            shiftName = "Night";
        }
        return "REPORT1_" + windowStart.format(REPORT_DATE_FORMAT) + "_" + shiftName + ".txt";
    }

    private String resolveBenchmarkStatus(HousekeepingShiftReport report) {
        if (report.getTurnaroundSampleSize() == 0) {
            return "PENDING DATA";
        }
        if (report.isOnTarget()) {
            return "ON TARGET";
        }
        return "BELOW TARGET";
    }

    private String resolveShiftLabel(String shiftCode) {
        if (shiftCode.equals(SHIFT_MORNING)) {
            return "Morning Shift Summary (07:00 - 14:59)";
        }
        if (shiftCode.equals(SHIFT_AFTERNOON)) {
            return "Afternoon Shift Summary (15:00 - 22:59)";
        }
        if (shiftCode.equals(SHIFT_NIGHT)) {
            return "Night Shift Summary (23:00 - 06:59)";
        }
        return "Current Shift Summary";
    }

    private String resolveShiftLabel(LocalDateTime dateTime) {
        return resolveShiftLabel(detectCurrentShiftCode(dateTime));
    }

    private double minutesBetween(String start, String end) {
        LocalDateTime startTime = LocalDateTime.parse(start, TIMESTAMP_FORMAT);
        LocalDateTime endTime = LocalDateTime.parse(end, TIMESTAMP_FORMAT);
        return Duration.between(startTime, endTime).toMillis() / 60000.0;
    }

    private boolean isValidTimestamp(String timestamp) {
        return timestamp != null && !timestamp.trim().isEmpty() && !timestamp.equals("N/A");
    }

    /**
     * Returns the last status change on the rollback stack without undoing it.
     */
    public HousekeepingLog peekLastRollbackAction() {
        if (rollbackStack.isEmpty()) {
            return null;
        }
        return rollbackStack.peek();
    }

    /**
     * Undoes the last housekeeping status change.
     */
    public HousekeepingLog rollbackLastAction() {
        if (rollbackStack.isEmpty()) {
            return null;
        }

        HousekeepingLog lastLog = rollbackStack.pop();
        Room room = lastLog.getRoom();
        if (room != null) {
            String timestamp = getCurrentTimestamp();
            room.setCleanlinessStatus(lastLog.getOldStatus());
            room.setLastUpdate(timestamp);
            appendAuditLog(new HousekeepingLog(room, lastLog.getNewStatus(), lastLog.getOldStatus(), timestamp));
            saveRoomsToFile();
            return lastLog;
        }

        return null;
    }

    /**
     * Checks if the transition between two room statuses is valid.
     */
    private boolean isValidTransition(String current, String target) {
        if (current.equals("Dirty") && target.equals("Cleaning In Progress")) {
            return true;
        }
        if (current.equals("Cleaning In Progress") && target.equals("Inspected")) {
            return true;
        }
        if (current.equals("Inspected") && target.equals("Ready")) {
            return true;
        }
        if (current.equals("Ready") && target.equals("Dirty")) {
            return true;
        }
        if (current.equals("Cleaning In Progress") && target.equals("Dirty")) {
            return true;
        }
        if (current.equals("Inspected") && target.equals("Dirty")) {
            return true;
        }
        return false;
    }

    private String getAllowedNextStatuses(String current) {
        if (current.equals("Dirty")) {
            return "Cleaning In Progress";
        }
        if (current.equals("Cleaning In Progress")) {
            return "Inspected or Dirty";
        }
        if (current.equals("Inspected")) {
            return "Ready or Dirty";
        }
        if (current.equals("Ready")) {
            return "Dirty";
        }
        return "unknown";
    }

    /**
     * Returns the valid target statuses for the given current status.
     */
    public String[] getAllowedTargetStatuses(String currentStatus) {
        if (currentStatus.equals("Dirty")) {
            return new String[] { "Cleaning In Progress" };
        }
        if (currentStatus.equals("Cleaning In Progress")) {
            return new String[] { "Inspected", "Dirty" };
        }
        if (currentStatus.equals("Inspected")) {
            return new String[] { "Ready", "Dirty" };
        }
        if (currentStatus.equals("Ready")) {
            return new String[] { "Dirty" };
        }
        return new String[0];
    }

    /**
     * Generates Report 2 with search, filter, and sort applied to status log data.
     */
    public HousekeepingAuditReport generateAuditTrailReport(String scopeMode, String shiftCode,
            String roomNumberFilter, String roomTypeFilter, String transitionFilter, String sortCode) {
        LocalDateTime now = LocalDateTime.now();
        String resolvedShift = shiftCode.equals(SHIFT_CURRENT) ? detectCurrentShiftCode(now) : shiftCode;
        LocalDateTime[] window = resolveShiftWindow(shiftCode, now);
        String dateWindow = window[0].format(TIMESTAMP_FORMAT) + " to " + window[1].format(TIMESTAMP_FORMAT);
        String shiftTarget = buildShiftTargetLabel(resolvedShift, window[0]);

        ListInterface<HousekeepingLog> matched = searchAuditLogsForReport(
                scopeMode, window[0], window[1], roomNumberFilter, roomTypeFilter, transitionFilter);
        sortAuditLogs(matched, sortCode);

        int totalEvents = matched.getNumberOfEntries();
        int completionCount = countCompletions(matched);
        int resetCount = countResets(matched);
        String mostActiveRoom = scopeMode.equals(SCOPE_ROOM)
                ? "N/A (single room scope)"
                : findMostActiveRoom(matched);

        String reportTarget = buildAuditReportTargetLabel(scopeMode, shiftTarget, roomNumberFilter, roomTypeFilter);
        String transitionLabel = resolveTransitionLabel(transitionFilter);
        String sortLabel = resolveSortLabel(sortCode);
        String reportFileName = buildAuditReportFileName(resolvedShift, window[0], scopeMode, roomNumberFilter,
                roomTypeFilter);

        HousekeepingAuditReport report = new HousekeepingAuditReport(
                now.format(TIMESTAMP_FORMAT),
                reportTarget,
                dateWindow,
                roomTypeFilter.equals(FILTER_ALL) ? "All Room Types" : roomTypeFilter,
                roomNumberFilter.equals(FILTER_ALL) ? "All Rooms" : roomNumberFilter,
                transitionLabel,
                sortLabel,
                matched,
                totalEvents,
                completionCount,
                resetCount,
                mostActiveRoom,
                reportFileName);
        enrichAuditRevisionMetadata(report);
        return report;
    }

    /**
     * Returns true if the authoritative audit report file already exists.
     */
    public boolean auditReportFileExists(HousekeepingAuditReport report) {
        return new File(REPORT_DIR + report.getReportFileName()).exists();
    }

    public String getAuditReportDisplayPath(HousekeepingAuditReport report) {
        return formatReportDisplayPath(report.getReportFileName());
    }

    /**
     * Persists the audit report to the authoritative file for this filter set (overwrite mode).
     */
    public String saveAuditTrailReport(HousekeepingAuditReport report) {
        enrichAuditRevisionMetadata(report);
        String fileName = report.getReportFileName();
        String filePath = REPORT_DIR + fileName;

        File directory = new File(REPORT_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        ListInterface<String> lines = buildAuditReportLines(report);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, false))) {
            for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
                bw.write(lines.getEntry(i));
                bw.newLine();
            }
        } catch (IOException e) {
            return null;
        }

        String displayPath = formatReportDisplayPath(fileName);
        report.setSavedFilePath(displayPath);
        return displayPath;
    }

    /**
     * Builds formatted audit report lines for console display or file export.
     */
    public ListInterface<String> buildAuditReportLines(HousekeepingAuditReport report) {
        ListInterface<String> lines = new ArrayList<>();
        boolean singleRoomScope = !report.getRoomNumberFilter().equals("All Rooms");

        lines.add("==================================================================");
        lines.add("     REPORT 2: HOUSEKEEPING AUDIT TRAIL & ACTIVITY SEARCH");
        lines.add("==================================================================");
        lines.add("Report Target : " + report.getReportTarget());
        lines.add("First Created : " + report.getFirstCreatedAt());
        lines.add("Last Updated  : " + report.getLastUpdatedAt() + " (Revision #" + report.getRevisionNumber() + ")");
        lines.add("Date Window   : " + report.getDateWindow());
        lines.add("Filters       : Transition: " + report.getTransitionFilter()
                + " | Sort: " + report.getSortLabel());
        lines.add("------------------------------------------------------------------");
        lines.add("");
        lines.add(" [MANAGEMENT SUMMARY]");
        lines.add("  Total Status Events     : " + report.getTotalEvents());
        lines.add("  Room Completions        : " + report.getCompletionCount());
        lines.add("  Dirty Resets / Aborts   : " + report.getResetCount());
        lines.add("  Most Active Room        : " + report.getMostActiveRoom());
        lines.add("");
        lines.add(" [DECISION NOTE]");
        lines.add("  " + buildAuditDecisionNote(report));
        lines.add("");
        lines.add(" [ACTIVITY DETAIL]");

        if (report.getActivityRows().isEmpty()) {
            lines.add("  No matching audit events found for the selected filters.");
        } else if (singleRoomScope) {
            Room room = getRoom(report.getRoomNumberFilter());
            if (room != null) {
                lines.add("  Room Target     : Room " + room.getRoomNumber() + " (" + room.getRoomType() + ")");
                lines.add("  Current Status  : " + room.getCleanlinessStatus());
                lines.add("");
            }
            lines.add(String.format("  %3s | %-19s | %s", "#", "Timestamp", "Status Transition"));
            lines.add("  ---|---------------------|---------------------------");
            for (int i = 1; i <= report.getActivityRows().getNumberOfEntries(); i++) {
                HousekeepingLog log = report.getActivityRows().getEntry(i);
                lines.add(String.format("  %3d | %-19s | %s --> %s",
                        i, log.getTimestamp(), log.getOldStatus(), log.getNewStatus()));
            }
        } else {
            lines.add(String.format("  %3s | %-6s | %-10s | %-19s | %s",
                    "#", "Room", "Type", "Timestamp", "Transition"));
            lines.add("  ---|--------|------------|---------------------|---------------------------");
            for (int i = 1; i <= report.getActivityRows().getNumberOfEntries(); i++) {
                HousekeepingLog log = report.getActivityRows().getEntry(i);
                lines.add(String.format("  %3d | %-6s | %-10s | %-19s | %s --> %s",
                        i,
                        log.getRoom().getRoomNumber(),
                        log.getRoom().getRoomType(),
                        log.getTimestamp(),
                        log.getOldStatus(),
                        log.getNewStatus()));
            }
        }

        lines.add("------------------------------------------------------------------");
        return lines;
    }

    private ListInterface<HousekeepingLog> searchAuditLogsForReport(String scopeMode,
            LocalDateTime shiftStart, LocalDateTime shiftEnd,
            String roomNumberFilter, String roomTypeFilter, String transitionFilter) {
        ListInterface<HousekeepingLog> matched = new ArrayList<>();

        for (int i = 1; i <= auditLogs.getNumberOfEntries(); i++) {
            HousekeepingLog log = auditLogs.getEntry(i);
            if (!isValidTimestamp(log.getTimestamp())) {
                continue;
            }

            LocalDateTime changeTime = LocalDateTime.parse(log.getTimestamp(), TIMESTAMP_FORMAT);
            if (changeTime.isBefore(shiftStart) || changeTime.isAfter(shiftEnd)) {
                continue;
            }

            if (scopeMode.equals(SCOPE_ROOM)
                    && !log.getRoom().getRoomNumber().equalsIgnoreCase(roomNumberFilter)) {
                continue;
            }

            if (scopeMode.equals(SCOPE_ROOM_TYPE)
                    && !log.getRoom().getRoomType().equalsIgnoreCase(roomTypeFilter)) {
                continue;
            }

            if (!matchesTransitionFilter(log, transitionFilter)) {
                continue;
            }

            matched.add(log);
        }

        return matched;
    }

    private boolean matchesTransitionFilter(HousekeepingLog log, String transitionFilter) {
        if (transitionFilter.equals(TRANSITION_ALL)) {
            return true;
        }
        if (transitionFilter.equals(TRANSITION_TO_READY)) {
            return log.getNewStatus().equals("Ready");
        }
        if (transitionFilter.equals(TRANSITION_TO_DIRTY)) {
            return log.getNewStatus().equals("Dirty");
        }
        if (transitionFilter.equals(TRANSITION_COMPLETION)) {
            return log.getOldStatus().equals("Inspected") && log.getNewStatus().equals("Ready");
        }
        return true;
    }

    private void sortAuditLogs(ListInterface<HousekeepingLog> list, String sortCode) {
        if (list.isEmpty()) {
            return;
        }
        if (sortCode.equals(SORT_TIMESTAMP_ASC)) {
            selectionSortLogsByTimestampAscending(list);
        } else if (sortCode.equals(SORT_ROOM_NUMBER)) {
            insertionSortLogsByRoomNumberThenTimestamp(list);
        } else {
            selectionSortLogsByTimestampDescending(list);
        }
    }

    private void selectionSortLogsByTimestampAscending(ListInterface<HousekeepingLog> list) {
        int n = list.getNumberOfEntries();
        for (int i = 1; i <= n - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j <= n; j++) {
                LocalDateTime timeJ = LocalDateTime.parse(list.getEntry(j).getTimestamp(), TIMESTAMP_FORMAT);
                LocalDateTime timeMin = LocalDateTime.parse(list.getEntry(minIndex).getTimestamp(), TIMESTAMP_FORMAT);
                if (timeJ.isBefore(timeMin)) {
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                HousekeepingLog temp = list.getEntry(i);
                list.replace(i, list.getEntry(minIndex));
                list.replace(minIndex, temp);
            }
        }
    }

    private void insertionSortLogsByRoomNumberThenTimestamp(ListInterface<HousekeepingLog> list) {
        for (int i = 2; i <= list.getNumberOfEntries(); i++) {
            HousekeepingLog key = list.getEntry(i);
            int j = i - 1;
            while (j >= 1 && compareLogsByRoomThenTimestamp(list.getEntry(j), key) > 0) {
                list.replace(j + 1, list.getEntry(j));
                j--;
            }
            list.replace(j + 1, key);
        }
    }

    private int compareLogsByRoomThenTimestamp(HousekeepingLog left, HousekeepingLog right) {
        int roomCompare = left.getRoom().getRoomNumber().compareToIgnoreCase(right.getRoom().getRoomNumber());
        if (roomCompare != 0) {
            return roomCompare;
        }
        LocalDateTime leftTime = LocalDateTime.parse(left.getTimestamp(), TIMESTAMP_FORMAT);
        LocalDateTime rightTime = LocalDateTime.parse(right.getTimestamp(), TIMESTAMP_FORMAT);
        return leftTime.compareTo(rightTime);
    }

    private int countCompletions(ListInterface<HousekeepingLog> logs) {
        int count = 0;
        for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
            HousekeepingLog log = logs.getEntry(i);
            if (log.getOldStatus().equals("Inspected") && log.getNewStatus().equals("Ready")) {
                count++;
            }
        }
        return count;
    }

    private int countResets(ListInterface<HousekeepingLog> logs) {
        int count = 0;
        for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
            HousekeepingLog log = logs.getEntry(i);
            if (log.getNewStatus().equals("Dirty") && !log.getOldStatus().equals("Ready")) {
                count++;
            }
        }
        return count;
    }

    private String findMostActiveRoom(ListInterface<HousekeepingLog> logs) {
        if (logs.isEmpty()) {
            return "N/A";
        }

        String[] roomNumbers = new String[logs.getNumberOfEntries()];
        int[] counts = new int[logs.getNumberOfEntries()];
        int uniqueRooms = 0;

        for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
            String roomNumber = logs.getEntry(i).getRoom().getRoomNumber();
            int index = findRoomCountIndex(roomNumbers, counts, uniqueRooms, roomNumber);
            if (index == -1) {
                roomNumbers[uniqueRooms] = roomNumber;
                counts[uniqueRooms] = 1;
                uniqueRooms++;
            } else {
                counts[index]++;
            }
        }

        int maxIndex = 0;
        for (int i = 1; i < uniqueRooms; i++) {
            if (counts[i] > counts[maxIndex]) {
                maxIndex = i;
            }
        }

        return roomNumbers[maxIndex] + " (" + counts[maxIndex] + " events)";
    }

    private int findRoomCountIndex(String[] roomNumbers, int[] counts, int size, String roomNumber) {
        for (int i = 0; i < size; i++) {
            if (roomNumbers[i].equalsIgnoreCase(roomNumber)) {
                return i;
            }
        }
        return -1;
    }

    private String buildAuditReportTargetLabel(String scopeMode, String shiftTarget,
            String roomNumberFilter, String roomTypeFilter) {
        if (scopeMode.equals(SCOPE_ROOM)) {
            Room room = getRoom(roomNumberFilter);
            String roomType = room != null ? room.getRoomType() : "Unknown";
            return shiftTarget + " — Room " + roomNumberFilter + " (" + roomType + ")";
        }
        if (scopeMode.equals(SCOPE_ROOM_TYPE)) {
            return shiftTarget + " — " + roomTypeFilter + " Rooms";
        }
        return shiftTarget + " — Shift Activity";
    }

    private String buildAuditReportFileName(String shiftCode, LocalDateTime windowStart,
            String scopeMode, String roomNumberFilter, String roomTypeFilter) {
        String shiftName;
        if (shiftCode.equals(SHIFT_MORNING)) {
            shiftName = "Morning";
        } else if (shiftCode.equals(SHIFT_AFTERNOON)) {
            shiftName = "Afternoon";
        } else {
            shiftName = "Night";
        }

        String suffix = "ShiftActivity";
        if (scopeMode.equals(SCOPE_ROOM)) {
            suffix = "Room" + roomNumberFilter;
        } else if (scopeMode.equals(SCOPE_ROOM_TYPE)) {
            suffix = roomTypeFilter;
        }

        return "REPORT2_" + windowStart.format(REPORT_DATE_FORMAT) + "_" + shiftName + "_" + suffix + ".txt";
    }

    private String resolveTransitionLabel(String transitionFilter) {
        if (transitionFilter.equals(TRANSITION_TO_READY)) {
            return "To Ready";
        }
        if (transitionFilter.equals(TRANSITION_TO_DIRTY)) {
            return "To Dirty";
        }
        if (transitionFilter.equals(TRANSITION_COMPLETION)) {
            return "Completions";
        }
        return "All Transitions";
    }

    private String resolveSortLabel(String sortCode) {
        if (sortCode.equals(SORT_TIMESTAMP_ASC)) {
            return "Oldest First";
        }
        if (sortCode.equals(SORT_ROOM_NUMBER)) {
            return "Room Number";
        }
        return "Newest First";
    }

    private String buildAuditDecisionNote(HousekeepingAuditReport report) {
        if (report.getTotalEvents() == 0) {
            return "No audit activity recorded for this filter set. Verify shift window or broaden filters.";
        }

        if (report.getResetCount() > 0 && report.getResetCount() >= report.getCompletionCount()) {
            return "Elevated abort/reset activity detected. Review attendant workflow and supervisor escalation paths.";
        }

        if (report.getCompletionCount() > 0 && report.getResetCount() == 0) {
            return "Completion flow is stable with no abort/reset events in the selected window.";
        }

        if (report.getCompletionCount() == 0 && report.getTotalEvents() > 0) {
            return "Activity exists but no room completions recorded. Prioritize inspection and ready approvals.";
        }

        return "Audit trail captured successfully. Cross-check most active room for workload balancing.";
    }

    private void enrichAuditRevisionMetadata(HousekeepingAuditReport report) {
        File file = new File(REPORT_DIR + report.getReportFileName());
        String now = getCurrentTimestamp();

        if (!file.exists()) {
            report.setFirstCreatedAt(now);
            report.setLastUpdatedAt(now);
            report.setRevisionNumber(1);
            return;
        }

        String firstCreated = parseFirstCreatedFromFile(file);
        if (firstCreated == null) {
            firstCreated = now;
        }

        int existingRevision = parseRevisionFromFile(file);
        report.setFirstCreatedAt(firstCreated);
        report.setLastUpdatedAt(now);
        report.setRevisionNumber(existingRevision + 1);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
