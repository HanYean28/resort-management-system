package boundary;

import adt.ListInterface;
import control.HousekeepingManager;
import entity.HousekeepingLog;
import entity.Room;

import java.util.Scanner;

/**
 * @author Chang Han Yean
 */
public class HousekeepingUI {
    private HousekeepingManager manager;
    private Scanner scanner;

    public HousekeepingUI() {
        manager = new HousekeepingManager();
        scanner = new Scanner(System.in);
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("HOUSEKEEPING AND TASK LOG MENU");
            System.out.println(" [1] View All Rooms Status");
            System.out.println(" [2] Update Room Cleanliness Status");
            System.out.println(" [3] Rollback Last Status Update (Undo)");
            System.out.println(" [4] Generate Report 1: Room Status Summary");
            System.out.println(" [5] Generate Report 2: Housekeeping Overview & Search");
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
                case 5:
                    System.out.println("\nThis report is not implemented yet.");
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

    private void handleRollback() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ROLLBACK LAST STATUS UPDATE");

        HousekeepingLog lastAction = manager.peekLastRollbackAction();
        if (lastAction == null) {
            System.out.println("\nNo status changes found in the history log stack.");
            return;
        }

        Room room = manager.getRoom(lastAction.getRoomNumber());
        String roomType = room != null ? room.getRoomType() : "Unknown";

        while (true) {
            UIUtils.clearScreen();
            UIUtils.printHeader("ROLLBACK LAST STATUS UPDATE");

            System.out.println("\n [PENDING UNDO PREVIEW]");
            System.out.println(" Timestamp   : " + lastAction.getTimestamp());
            System.out.println(" Room Target : Room " + lastAction.getRoomNumber() + " (" + roomType + ")");
            System.out.println(" Action      : '" + lastAction.getOldStatus() + "' --> '"
                    + lastAction.getNewStatus() + "'");
            System.out.println(" Restoration : Room will return to '" + lastAction.getOldStatus() + "'");
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
            HousekeepingLog log = manager.rollbackLastAction();
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
        String roomLabel = "Room " + log.getRoomNumber() + " (" + roomType + ")";
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
