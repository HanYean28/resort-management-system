package boundary;

import java.util.Scanner;

/**
 * @author Chang Han Yean
 * @author Elwin Goh Yao Zu
 * @author Lim How Voon
 * @author Kaizen Soh
 */

public class mainmenu {

    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        while (choice != 0) {
            UIUtils.clearScreen();
            UIUtils.printHeader("RESORT MANAGEMENT SYSTEM");
            System.out.println(" [1] Housekeeping & Task Log");
            System.out.println(" [2] Walk-In & Standard Booking");
            System.out.println(" [3] VIP Priority Room Allocation");
            System.out.println(" [4] Front-Desk Service");
            System.out.println(" [0] Exit");
            UIUtils.printSectionLine();
            System.out.print("Please enter choice (0-4): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                scanner.nextLine();
            } else {
                System.out.println("Invalid choice. Please enter a number.");
                scanner.nextLine();
                UIUtils.pressEnterToContinue(scanner);
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
                    UIUtils.pressEnterToContinue(scanner);
                    break;
                case 0:
                    System.out.println("Exiting the system. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    UIUtils.pressEnterToContinue(scanner);
            }
        }
        scanner.close();
    }
}
