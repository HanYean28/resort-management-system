package boundary;

import java.util.Scanner;


/**
 * @author Chang Han Yean
 */

public class mainmenu {

    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        while (choice != 0) {
            UIUtils.clearScreen();
            System.out.println("=========================================");
            System.out.println("         RESORT MANAGEMENT SYSTEM        ");
            System.out.println("=========================================");
            System.out.println("1. Housekeeping & Task Log");
            System.out.println("2. Walk-In & Standard Booking");
            System.out.println("3. VIP Priority Room Allocation");
            System.out.println("4. Front-Desk Service");
            System.out.println("0. Exit");
            System.out.print("Please enter your choice (0-4): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                System.out.println("Invalid choice. Please enter a number.");
                scanner.nextLine();
                UIUtils.pressEnterToContinue();
                continue;
            }

            switch (choice) {
                case 1:
                    new HousekeepingUI().start();
                    break;
                case 2:
                case 3:
                case 4:
                    System.out.println("This module is not implemented yet.");
                    UIUtils.pressEnterToContinue();
                    break;
                case 0:
                    System.out.println("Exiting the system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    UIUtils.pressEnterToContinue();
            }
        }
    }
}
