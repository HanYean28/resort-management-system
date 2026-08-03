package boundary;

import adt.ListInterface;
import control.HousekeepingController;
import entity.HousekeepingAuditReport;
import entity.HousekeepingLog;
import entity.HousekeepingShiftReport;
import entity.Room;

import java.util.Scanner;

/**
 * @author Chang Han Yean
 */
public class HousekeepingUI {
    private HousekeepingController manager;
    private Scanner scanner;

    public HousekeepingUI() {
        manager = new HousekeepingController();
        scanner = new Scanner(System.in);
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("HOUSEKEEPING AND TASK LOG MENU");
            System.out.println(" [1] View All Rooms Status");
            System.out.println(" [2] Update Room Cleanliness Status");
            System.out.println(" [3] Rollback Last Status Update for a Room (Undo)");
            System.out.println(" [4] Generate Report 1: Shift Turnover Performance");
            System.out.println(" [5] Generate Report 2: Audit Trail & Activity Search");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-5): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                System.out.println("Invalid input! Please enter a number.");
                scanner.nextLine();
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }

            switch (choice) {
                case 1:
                    displayAllRooms();
                    break;
                case 2:
                    handleUpdateStatus();
                    break;
                case 3:
                    handleRollback();
                    break;
                case 4:
                    handleShiftTurnoverReport();
                    break;
                case 5:
                    handleAuditTrailReport();
                    break;
                case 0:
                    UIUtils.clearScreen();
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }

            if (choice != 0) {
                UIUtils.pressEnterToContinue(scanner);
            }
        }
    }

    private void displayAllRooms() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ALL ROOMS STATUS OVERVIEW");

        ListInterface<Room> rooms = manager.getAllRooms();
        if (rooms.isEmpty()) {
            System.out.println("No room records found.");
            return;
        }

        System.out.printf("%-14s | %-15s | %-22s | %-19s%n",
                "Room Number", "Room Type", "Cleanliness Status", "Last Updated");
        UIUtils.printSectionLine();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room r = rooms.getEntry(i);
            System.out.printf("%-14s | %-15s | %-22s | %-19s%n",
                    r.getRoomNumber(), r.getRoomType(), r.getCleanlinessStatus(), r.getLastUpdate());
        }
        UIUtils.printSectionLine();
        System.out.println("Total Rooms Listed: " + rooms.getNumberOfEntries());
    }

    private void handleUpdateStatus() {
        String roomNum = null;
        Room room = null;
        String lastError = null;

        while (room == null) {
            UIUtils.clearScreen();
            UIUtils.printHeader("UPDATE ROOM CLEANLINESS STATUS");
            if (lastError != null) {
                UIUtils.printError(lastError);
                System.out.println();
            }
            System.out.print("Enter Room Number to update (or 0 to cancel): ");
            roomNum = scanner.nextLine().trim();

            if (roomNum.equals("0")) {
                System.out.println("\nUpdate cancelled. Returning to menu.");
                return;
            }

            if (roomNum.isEmpty()) {
                lastError = "Room number cannot be empty.";
                continue;
            }

            room = manager.getRoom(roomNum);
            if (room == null) {
                lastError = "Room '" + roomNum + "' not found. Please try again.";
            }
        }

        String[] options = manager.getAllowedTargetStatuses(room.getCleanlinessStatus());
        if (options.length == 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("UPDATE ROOM CLEANLINESS STATUS");
            System.out.println("Room Number    : " + room.getRoomNumber());
            System.out.println("\nNo status updates available for this room.");
            return;
        }

        while (true) {
            UIUtils.clearScreen();
            UIUtils.printHeader("UPDATE ROOM CLEANLINESS STATUS");
            printRoomSummary(room);

            System.out.println("\nAvailable Actions for this Room:");
            for (int i = 0; i < options.length; i++) {
                System.out.printf(" [%d] %s%n", i + 1,
                        formatActionDescription(room.getCleanlinessStatus(), options[i]));
            }
            System.out.println(" [0] Cancel & Return");
            UIUtils.printSectionLine();
            System.out.print("Please press an option key [0-" + options.length + "]: ");

            Integer opt = readIntOption(0, options.length);
            if (opt == null) {
                System.out.println();
                UIUtils.printError("Invalid input! Please enter a number.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }

            if (opt == 0) {
                System.out.println("\nUpdate cancelled. Returning to menu.");
                return;
            }

            String targetStatus = options[opt - 1];
            String error = manager.updateRoomStatus(roomNum, targetStatus);
            if (error == null) {
                room = manager.getRoom(roomNum);
                System.out.println("\nSTATUS UPDATE SUCCESSFUL!");
                printRoomSummary(room);
            } else {
                System.out.println("\n" + error);
            }
            return;
        }
    }

    private void handleAuditTrailReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 2: AUDIT TRAIL & ACTIVITY SEARCH");

        String shiftCode = promptShiftSelection();
        if (shiftCode == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }

        String[] scopeSelection = promptAuditScope();
        if (scopeSelection == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }
        String scopeMode = scopeSelection[0];
        String roomNumberFilter = scopeSelection[1];
        String roomTypeFilter = scopeSelection[2];

        String transitionFilter = promptTransitionFilter();
        if (transitionFilter == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }

        String sortCode = promptSortOrder();
        if (sortCode == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }

        HousekeepingAuditReport report = manager.generateAuditTrailReport(
                scopeMode, shiftCode, roomNumberFilter, roomTypeFilter, transitionFilter, sortCode);

        while (true) {
            UIUtils.clearScreen();
            displayAuditTrailReport(report);

            System.out.println();
            UIUtils.printSectionLine();
            System.out.println(" [REPORT ACTIONS]");
            System.out.println(" [1] Save report to file");
            System.out.println(" [0] Return to menu (do not save)");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-1): ");

            Integer action = readIntOption(0, 1);
            if (action == null) {
                System.out.println("\nInvalid input! Please enter a number.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            if (action == 0) {
                System.out.println("\nReturning to menu without saving.");
                return;
            }

            if (!confirmSaveAuditReport(report)) {
                continue;
            }

            String savedPath = manager.saveAuditTrailReport(report);
            if (savedPath != null) {
                System.out.println("\n[SUCCESS] Saved to: " + savedPath);
            } else {
                UIUtils.printError("Unable to save report file.");
            }
            return;
        }
    }

    private void displayAuditTrailReport(HousekeepingAuditReport report) {
        ListInterface<String> lines = manager.buildAuditReportLines(report);
        for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
            System.out.println(lines.getEntry(i));
        }
    }

    private boolean confirmSaveAuditReport(HousekeepingAuditReport report) {
        if (!manager.auditReportFileExists(report)) {
            return true;
        }

        while (true) {
            UIUtils.clearScreen();
            System.out.println("==================================================================");
            System.out.println("                    REPORT EXPORT & OPTIONS");
            System.out.println("==================================================================");
            System.out.println("[NOTICE] Saved file detected: " + manager.getAuditReportDisplayPath(report));
            System.out.println("\nChoose Action:");
            System.out.println("  [1] Overwrite & Update Saved Report");
            System.out.println("  [0] Do Not Save (View Only)");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-1): ");

            Integer choice = readIntOption(0, 1);
            if (choice == null) {
                System.out.println("\nInvalid input! Please enter a number.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            return choice == 1;
        }
    }

    private String[] promptAuditScope() {
        while (true) {
            System.out.println("\nSelect audit scope:");
            System.out.println(" [1] Specific Room");
            System.out.println(" [2] Room Type");
            System.out.println(" [3] Shift Activity (all rooms in shift)");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-3): ");

            Integer choice = readIntOption(0, 3);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 3.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                while (true) {
                    System.out.print("\nEnter Room Number (or 0 to go back): ");
                    String roomNumber = scanner.nextLine().trim();
                    if (roomNumber.equals("0")) {
                        break;
                    }
                    if (roomNumber.isEmpty()) {
                        System.out.println("\nRoom number cannot be empty.");
                        continue;
                    }
                    if (manager.getRoom(roomNumber) == null) {
                        System.out.println("\nRoom '" + roomNumber + "' not found.");
                        continue;
                    }
                    return new String[] {
                            HousekeepingController.SCOPE_ROOM,
                            roomNumber,
                            HousekeepingController.FILTER_ALL
                    };
                }
                continue;
            }
            if (choice == 2) {
                String roomType = promptRoomTypeFilter();
                if (roomType == null) {
                    continue;
                }
                return new String[] {
                        HousekeepingController.SCOPE_ROOM_TYPE,
                        HousekeepingController.FILTER_ALL,
                        roomType
                    };
            }
            return new String[] {
                    HousekeepingController.SCOPE_SHIFT,
                    HousekeepingController.FILTER_ALL,
                    HousekeepingController.FILTER_ALL
            };
        }
    }

    private String promptTransitionFilter() {
        while (true) {
            System.out.println("\nApply transition filter:");
            System.out.println(" [1] All Transitions");
            System.out.println(" [2] To Ready");
            System.out.println(" [3] To Dirty");
            System.out.println(" [4] Completions (Inspected --> Ready)");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");

            Integer choice = readIntOption(0, 4);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 4.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return HousekeepingController.TRANSITION_ALL;
            }
            if (choice == 2) {
                return HousekeepingController.TRANSITION_TO_READY;
            }
            if (choice == 3) {
                return HousekeepingController.TRANSITION_TO_DIRTY;
            }
            return HousekeepingController.TRANSITION_COMPLETION;
        }
    }

    private String promptSortOrder() {
        while (true) {
            System.out.println("\nSelect sort order:");
            System.out.println(" [1] Newest First");
            System.out.println(" [2] Oldest First");
            System.out.println(" [3] Room Number");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-3): ");

            Integer choice = readIntOption(0, 3);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 3.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return HousekeepingController.SORT_TIMESTAMP_DESC;
            }
            if (choice == 2) {
                return HousekeepingController.SORT_TIMESTAMP_ASC;
            }
            return HousekeepingController.SORT_ROOM_NUMBER;
        }
    }

    private void handleShiftTurnoverReport() {
        UIUtils.clearScreen();
        UIUtils.printHeader("REPORT 1: SHIFT TURNOVER PERFORMANCE");

        String shiftCode = promptShiftSelection();
        if (shiftCode == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }

        String roomTypeFilter = promptRoomTypeFilter();
        if (roomTypeFilter == null) {
            System.out.println("\nReport generation cancelled.");
            return;
        }

        HousekeepingShiftReport report = manager.generateShiftTurnoverReport(shiftCode, roomTypeFilter);

        while (true) {
            UIUtils.clearScreen();
            displayShiftTurnoverReport(report);

            System.out.println();
            UIUtils.printSectionLine();
            System.out.println(" [REPORT ACTIONS]");
            System.out.println(" [1] Save report to file");
            System.out.println(" [0] Return to menu (do not save)");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-1): ");

            Integer action = readIntOption(0, 1);
            if (action == null) {
                System.out.println("\nInvalid input! Please enter a number.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            if (action == 0) {
                System.out.println("\nReturning to menu without saving.");
                return;
            }

            if (!confirmSaveReport(report)) {
                continue;
            }

            String savedPath = manager.saveShiftTurnoverReport(report);
            if (savedPath != null) {
                System.out.println("\n[SUCCESS] Saved to: " + savedPath);
            } else {
                UIUtils.printError("Unable to save report file.");
            }
            return;
        }
    }

    private void displayShiftTurnoverReport(HousekeepingShiftReport report) {
        ListInterface<String> lines = manager.buildReportLines(report);
        for (int i = 1; i <= lines.getNumberOfEntries(); i++) {
            System.out.println(lines.getEntry(i));
        }
    }

    private boolean confirmSaveReport(HousekeepingShiftReport report) {
        if (!manager.reportFileExists(report)) {
            return true;
        }

        while (true) {
            UIUtils.clearScreen();
            System.out.println("==================================================================");
            System.out.println("                    REPORT EXPORT & OPTIONS");
            System.out.println("==================================================================");
            System.out.println("[NOTICE] Saved file detected: " + manager.getReportDisplayPath(report));
            System.out.println("\nChoose Action:");
            System.out.println("  [1] Overwrite & Update Saved Report");
            System.out.println("  [0] Do Not Save (View Only)");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-1): ");

            Integer choice = readIntOption(0, 1);
            if (choice == null) {
                System.out.println("\nInvalid input! Please enter a number.");
                UIUtils.pressEnterToContinue(scanner);
                continue;
            }
            return choice == 1;
        }
    }

    private String promptShiftSelection() {
        while (true) {
            System.out.println("\nSelect shift window for turnover analysis:");
            System.out.println(" [1] Current Shift (auto-detect)");
            System.out.println(" [2] Morning Shift (07:00-14:59)");
            System.out.println(" [3] Afternoon Shift (15:00-22:59)");
            System.out.println(" [4] Night Shift (23:00-06:59)");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");

            Integer choice = readIntOption(0, 4);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 4.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return HousekeepingController.SHIFT_CURRENT;
            }
            if (choice == 2) {
                return HousekeepingController.SHIFT_MORNING;
            }
            if (choice == 3) {
                return HousekeepingController.SHIFT_AFTERNOON;
            }
            return HousekeepingController.SHIFT_NIGHT;
        }
    }

    private String promptRoomTypeFilter() {
        while (true) {
            System.out.println("\nApply room type filter:");
            System.out.println(" [1] All Room Types");
            System.out.println(" [2] Deluxe");
            System.out.println(" [3] Standard");
            System.out.println(" [4] Suite");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");

            Integer choice = readIntOption(0, 4);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 4.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return HousekeepingController.FILTER_ALL;
            }
            if (choice == 2) {
                return "Deluxe";
            }
            if (choice == 3) {
                return "Standard";
            }
            return "Suite";
        }
    }

    private void handleRollback() {
        String roomNum = null;
        Room room = null;
        String lastError = null;

        while (room == null) {
            UIUtils.clearScreen();
            UIUtils.printHeader("ROLLBACK LAST STATUS UPDATE");
            if (lastError != null) {
                UIUtils.printError(lastError);
                System.out.println();
            }
            System.out.print("Enter Room Number to undo (or 0 to cancel): ");
            roomNum = scanner.nextLine().trim();

            if (roomNum.equals("0")) {
                System.out.println("\nRollback cancelled. Returning to menu.");
                return;
            }

            if (roomNum.isEmpty()) {
                lastError = "Room number cannot be empty.";
                continue;
            }

            room = manager.getRoom(roomNum);
            if (room == null) {
                lastError = "Room '" + roomNum + "' not found. Please try again.";
            }
        }

        HousekeepingLog lastAction = manager.peekLastRollbackAction(roomNum);
        if (lastAction == null) {
            UIUtils.clearScreen();
            UIUtils.printHeader("ROLLBACK LAST STATUS UPDATE — ROOM " + roomNum);
            System.out.println("\nNo status changes found for this room in the current session.");
            System.out.println("Undo history is kept in memory only and clears when the program restarts.");
            return;
        }

        String roomType = room.getRoomType();
        int undoStepsRemaining = manager.getRollbackStackSize(roomNum);

        while (true) {
            UIUtils.clearScreen();
            UIUtils.printHeader("ROLLBACK LAST STATUS UPDATE — ROOM " + roomNum);

            System.out.println("\n [PENDING UNDO PREVIEW]");
            System.out.println(" Timestamp   : " + lastAction.getTimestamp());
            System.out.println(" Room Target : Room " + lastAction.getRoom().getRoomNumber() + " (" + roomType + ")");
            System.out.println(" Action      : '" + lastAction.getOldStatus() + "' --> '"
                    + lastAction.getNewStatus() + "'");
            System.out.println(" Restoration : Room will return to '" + lastAction.getOldStatus() + "'");
            System.out.println(" Undo Steps  : " + undoStepsRemaining + " remaining for this room in this session");
            UIUtils.printSectionLine();
            System.out.print("Confirm execution? (Y/N): ");

            String confirm = readYesNoInput();
            if (confirm == null) {
                System.out.println("\nInvalid input! Please enter Y or N only.");
                continue;
            }

            if (confirm.equalsIgnoreCase("N")) {
                System.out.println("\nRollback cancelled. Returning to menu.");
                return;
            }

            System.out.println("\nExecuting rollback...");
            HousekeepingLog log = manager.rollbackLastAction(roomNum);
            if (log != null) {
                System.out.println("ROLLBACK SUCCESSFUL!");
                printRollbackDisruptionNotice(log, roomType);
            } else {
                System.out.println("Rollback failed.");
            }
            return;
        }
    }

    private void printRoomSummary(Room room) {
        System.out.println("Room Number    : " + room.getRoomNumber());
        System.out.println("Room Type      : " + room.getRoomType());
        System.out.println("Current Status : [" + room.getCleanlinessStatus() + "]");
        System.out.println("Last Updated   : " + room.getLastUpdate());
        UIUtils.printSectionLine();
    }

    private String formatActionDescription(String currentStatus, String targetStatus) {
        if (targetStatus.equals("Cleaning In Progress")) {
            return "Start Cleaning (Set to 'Cleaning In Progress')";
        }
        if (targetStatus.equals("Inspected")) {
            return "Finish Cleaning (Set to 'Inspected')";
        }
        if (targetStatus.equals("Ready")) {
            return "Approve Room (Set to 'Ready')";
        }
        if (targetStatus.equals("Dirty")) {
            if (currentStatus.equals("Ready")) {
                return "Guest Checked Out (Set to 'Dirty')";
            }
            return "Abort & Reset (Set back to 'Dirty')";
        }
        return formatStatusOptionLabel(targetStatus);
    }

    private String formatStatusOptionLabel(String status) {
        if (status.equals("Dirty")) {
            return "Reset to Dirty";
        }
        return status;
    }

    private void printRollbackDisruptionNotice(HousekeepingLog log, String roomType) {
        System.out.println("\n[OPERATIONAL DISRUPTION DETECTED]");
        String roomLabel = "Room " + log.getRoom().getRoomNumber() + " (" + roomType + ")";
        String oldStatus = log.getOldStatus();
        String newStatus = log.getNewStatus();

        if (oldStatus.equals("Dirty") && newStatus.equals("Cleaning In Progress")) {
            System.out.println("-> Cleaning task for " + roomLabel + " has been ABORTED mid-clean.");
            System.out.println("-> Front-Desk Notified: Room marked as OCCUPIED (Late Check-Out/Guest Return).");
            System.out.println("-> System State: Room locked out from incoming walk-in check-in queues.");
        } else if (oldStatus.equals("Cleaning In Progress") && newStatus.equals("Inspected")) {
            System.out.println("-> Inspection approval for " + roomLabel + " has been REVOKED.");
            System.out.println("-> Housekeeping Team Notified: Room returned to 'Cleaning In Progress'.");
        } else if (oldStatus.equals("Inspected") && newStatus.equals("Ready")) {
            System.out.println("-> Ready status for " + roomLabel + " has been WITHDRAWN.");
            System.out.println("-> Front-Desk Notified: Room removed from available check-in pool.");
        } else if (newStatus.equals("Dirty")) {
            System.out.println("-> Dirty reset for " + roomLabel + " has been UNDONE.");
            System.out.println("-> Room restored to previous workflow state: '" + oldStatus + "'.");
        } else {
            System.out.println("-> Status change for " + roomLabel + " has been reversed.");
            System.out.println("-> Room restored to previous state: '" + oldStatus + "'.");
        }
    }

    private Integer readIntOption(int min, int max) {
        if (scanner.hasNextInt()) {
            int value = scanner.nextInt();
            scanner.nextLine();
            if (value >= min && value <= max) {
                return value;
            }
            return null;
        }
        scanner.nextLine();
        return null;
    }

    private String readYesNoInput() {
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("Y") || input.equalsIgnoreCase("N")) {
            return input;
        }
        return null;
    }
}
