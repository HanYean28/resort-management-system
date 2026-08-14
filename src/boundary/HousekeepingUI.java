package boundary;

import adt.ListInterface;
import control.HousekeepingController;
import entity.HousekeepingLog;
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

    public HousekeepingUI(Scanner scanner) {
        manager = new HousekeepingController();
        this.scanner = scanner;
    }

    public void start() {
        int choice = -1;
        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("HOUSEKEEPING AND TASK LOG MENU");
            System.out.println(" [1] Add Housekeeping Task");
            System.out.println(" [2] View Tasks");
            System.out.println(" [3] Search Room / Task");
            System.out.println(" [4] Update Cleaning Status");
            System.out.println(" [5] Complete Task");
            System.out.println(" [6] Rollback Status");
            System.out.println(" [7] View Task History");
            System.out.println(" [0] Return to Main Menu");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-7): ");

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
                    handleAddTask();
                    break;
                case 2:
                    displayTasks();
                    break;
                case 3:
                    handleSearch();
                    break;
                case 4:
                    handleUpdateStatus();
                    break;
                case 5:
                    handleCompleteTask();
                    break;
                case 6:
                    handleRollback();
                    break;
                case 7:
                    handleViewTaskHistory();
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

    private void handleAddTask() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ADD HOUSEKEEPING TASK");
        displayRoomTable(manager.getAllRooms());

        Room room = promptRoom("Enter Room Number to mark as Dirty (or 0 to cancel): ");
        if (room == null) {
            System.out.println("\nAdd task cancelled.");
            return;
        }

        String error = manager.addHousekeepingTask(room.getRoomNumber());
        if (error == null) {
            System.out.println("\nHousekeeping task added successfully.");
            printRoomSummary(manager.getRoom(room.getRoomNumber()));
        } else {
            UIUtils.printError(error);
        }
    }

    private void displayTasks() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ACTIVE HOUSEKEEPING TASKS");

        ListInterface<Room> tasks = manager.getActiveTasks();
        if (tasks.isEmpty()) {
            System.out.println("No active housekeeping tasks found.");
            return;
        }

        displayRoomTable(tasks);
        System.out.println("Total Active Tasks: " + tasks.getNumberOfEntries());
    }

    private void handleSearch() {
        UIUtils.clearScreen();
        UIUtils.printHeader("SEARCH ROOM / TASK");

        System.out.print("Enter keyword (room number/type/status, blank for all): ");
        String keyword = scanner.nextLine().trim();
        String statusFilter = promptStatusFilter();
        if (statusFilter == null) {
            System.out.println("\nSearch cancelled.");
            return;
        }
        String roomTypeFilter = promptRoomTypeFilter();
        if (roomTypeFilter == null) {
            System.out.println("\nSearch cancelled.");
            return;
        }

        ListInterface<Room> results = manager.searchRoomsOrTasks(keyword, statusFilter, roomTypeFilter);
        UIUtils.clearScreen();
        UIUtils.printHeader("SEARCH RESULTS");
        if (results.isEmpty()) {
            System.out.println("No matching room/task found.");
            return;
        }

        displayRoomTable(results);
        System.out.println("Total Matches: " + results.getNumberOfEntries());
    }

    private void handleUpdateStatus() {
        UIUtils.clearScreen();
        UIUtils.printHeader("UPDATE CLEANING STATUS");
        displayTasks();

        Room room = promptRoom("Enter Room Number to update (or 0 to cancel): ");
        if (room == null) {
            System.out.println("\nUpdate cancelled.");
            return;
        }

        if (room.getCleanlinessStatus().equals(HousekeepingController.STATUS_READY)) {
            UIUtils.printError("Ready rooms do not have an active cleaning task. Use Add Housekeeping Task first.");
            return;
        }
        if (room.getCleanlinessStatus().equals(HousekeepingController.STATUS_INSPECTED)) {
            UIUtils.printError("This room is inspected. Use Complete Task to set it to Ready.");
            return;
        }

        String[] options = manager.getAllowedTargetStatuses(room.getCleanlinessStatus());
        if (options.length == 0) {
            UIUtils.printError("No status update available for this room.");
            return;
        }

        String targetStatus = options[0];
        System.out.println();
        printRoomSummary(room);
        System.out.println("Next Status: " + targetStatus);
        System.out.print("Confirm update? (Y/N): ");

        String confirm = readYesNoInput();
        if (confirm == null || confirm.equalsIgnoreCase("N")) {
            System.out.println("\nUpdate cancelled.");
            return;
        }

        String error = manager.updateRoomStatus(room.getRoomNumber(), targetStatus);
        if (error == null) {
            System.out.println("\nStatus updated successfully.");
            printRoomSummary(manager.getRoom(room.getRoomNumber()));
        } else {
            UIUtils.printError(error);
        }
    }

    private void handleCompleteTask() {
        UIUtils.clearScreen();
        UIUtils.printHeader("COMPLETE HOUSEKEEPING TASK");

        ListInterface<Room> inspectedRooms = manager.searchRoomsOrTasks("",
                HousekeepingController.STATUS_INSPECTED, HousekeepingController.FILTER_ALL);
        if (inspectedRooms.isEmpty()) {
            System.out.println("No inspected rooms are ready to complete.");
            return;
        }
        displayRoomTable(inspectedRooms);

        Room room = promptRoom("Enter inspected Room Number to complete (or 0 to cancel): ");
        if (room == null) {
            System.out.println("\nComplete task cancelled.");
            return;
        }

        String error = manager.completeTask(room.getRoomNumber());
        if (error == null) {
            System.out.println("\nTask completed. Room is now Ready.");
            printRoomSummary(manager.getRoom(room.getRoomNumber()));
        } else {
            UIUtils.printError(error);
        }
    }

    private void handleRollback() {
        UIUtils.clearScreen();
        UIUtils.printHeader("ROLLBACK STATUS");

        Room room = promptRoom("Enter Room Number to rollback (or 0 to cancel): ");
        if (room == null) {
            System.out.println("\nRollback cancelled.");
            return;
        }

        HousekeepingLog lastAction = manager.peekLastRollbackAction(room.getRoomNumber());
        if (lastAction == null) {
            System.out.println("\nNo status changes found for this room in the current session.");
            System.out.println("Rollback history is kept in memory only and clears when the program restarts.");
            return;
        }

        System.out.println();
        System.out.println("Last Change : " + lastAction.getOldStatus() + " -> " + lastAction.getNewStatus());
        System.out.println("Timestamp   : " + lastAction.getTimestamp());
        System.out.println("Will Restore: " + lastAction.getOldStatus());
        System.out.println("Undo Steps  : " + manager.getRollbackStackSize(room.getRoomNumber()));
        UIUtils.printSectionLine();
        System.out.print("Confirm rollback? (Y/N): ");

        String confirm = readYesNoInput();
        if (confirm == null || confirm.equalsIgnoreCase("N")) {
            System.out.println("\nRollback cancelled.");
            return;
        }

        HousekeepingLog rolledBack = manager.rollbackLastAction(room.getRoomNumber());
        if (rolledBack == null) {
            UIUtils.printError("Rollback failed.");
        } else {
            System.out.println("\nRollback successful.");
            printRoomSummary(manager.getRoom(room.getRoomNumber()));
        }
    }

    private void handleViewTaskHistory() {
        UIUtils.clearScreen();
        UIUtils.printHeader("VIEW TASK HISTORY");

        System.out.print("Enter Room Number (or ALL for all rooms, 0 to cancel): ");
        String roomNumber = scanner.nextLine().trim();
        if (roomNumber.equals("0")) {
            System.out.println("\nView history cancelled.");
            return;
        }
        if (roomNumber.isEmpty()) {
            UIUtils.printError("Room number cannot be empty.");
            return;
        }
        if (!roomNumber.equalsIgnoreCase(HousekeepingController.FILTER_ALL)
                && manager.getRoom(roomNumber) == null) {
            UIUtils.printError("Room '" + roomNumber + "' not found.");
            return;
        }

        ListInterface<HousekeepingLog> logs = manager.getTaskHistory(
                roomNumber.equalsIgnoreCase(HousekeepingController.FILTER_ALL)
                        ? HousekeepingController.FILTER_ALL
                        : roomNumber);

        UIUtils.clearScreen();
        UIUtils.printHeader("TASK HISTORY");
        if (logs.isEmpty()) {
            System.out.println("No task history found.");
            return;
        }

        System.out.printf("%-4s | %-6s | %-19s | %-22s | %-22s%n",
                "No.", "Room", "Timestamp", "Old Status", "New Status");
        UIUtils.printSectionLine();
        for (int i = 1; i <= logs.getNumberOfEntries(); i++) {
            HousekeepingLog log = logs.getEntry(i);
            System.out.printf("%-4d | %-6s | %-19s | %-22s | %-22s%n",
                    i,
                    log.getRoom().getRoomNumber(),
                    log.getTimestamp(),
                    log.getOldStatus(),
                    log.getNewStatus());
        }
    }

    private Room promptRoom(String prompt) {
        System.out.print(prompt);
        String roomNumber = scanner.nextLine().trim();
        if (roomNumber.equals("0")) {
            return null;
        }
        if (roomNumber.isEmpty()) {
            UIUtils.printError("Room number cannot be empty.");
            return null;
        }

        Room room = manager.getRoom(roomNumber);
        if (room == null) {
            UIUtils.printError("Room '" + roomNumber + "' not found.");
        }
        return room;
    }

    private String promptStatusFilter() {
        while (true) {
            System.out.println("\nFilter by status:");
            System.out.println(" [1] All Statuses");
            System.out.println(" [2] Dirty");
            System.out.println(" [3] Cleaning In Progress");
            System.out.println(" [4] Inspected");
            System.out.println(" [5] Ready");
            System.out.println(" [0] Cancel");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-5): ");

            Integer choice = readIntOption(0, 5);
            if (choice == null) {
                System.out.println("\nInvalid choice! Please enter a number between 0 and 5.");
                continue;
            }
            if (choice == 0) {
                return null;
            }
            if (choice == 1) {
                return HousekeepingController.FILTER_ALL;
            }
            if (choice == 2) {
                return HousekeepingController.STATUS_DIRTY;
            }
            if (choice == 3) {
                return HousekeepingController.STATUS_CLEANING;
            }
            if (choice == 4) {
                return HousekeepingController.STATUS_INSPECTED;
            }
            return HousekeepingController.STATUS_READY;
        }
    }

    private String promptRoomTypeFilter() {
        while (true) {
            System.out.println("\nFilter by room type:");
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

    private void displayRoomTable(ListInterface<Room> rooms) {
        if (rooms.isEmpty()) {
            System.out.println("No room records found.");
            return;
        }

        System.out.printf("%-6s | %-10s | %-22s | %-19s%n",
                "Room", "Type", "Status", "Last Updated");
        UIUtils.printSectionLine();
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            System.out.printf("%-6s | %-10s | %-22s | %-19s%n",
                    room.getRoomNumber(),
                    room.getRoomType(),
                    room.getCleanlinessStatus(),
                    room.getLastUpdate());
        }
        UIUtils.printSectionLine();
    }

    private void printRoomSummary(Room room) {
        System.out.println("Room Number    : " + room.getRoomNumber());
        System.out.println("Room Type      : " + room.getRoomType());
        System.out.println("Current Status : " + room.getCleanlinessStatus());
        System.out.println("Last Updated   : " + room.getLastUpdate());
        UIUtils.printSectionLine();
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
