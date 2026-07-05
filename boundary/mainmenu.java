package boundary;

import java.util.Scanner;

public class mainmenu {
    public void displayMenu() {
        System.out.println("=========================================");
        System.out.println("         RESORT MANAGEMENT SYSTEM ");
        System.out.println("=========================================");
        System.out.println("\nWelcome to the System Admin Panel!");
        System.out.println("1. Manage Rooms");
        System.out.println("2. Manage Guests");
        System.out.println("3. Exit");
        Scanner scanner = new Scanner(System.in);
        int choice = 0;
        System.out.print("Please enter your choice (1-3): ");
        choice = scanner.nextInt();
        switch (choice) {
            case 1:
                System.out.println("You selected Manage Rooms.");
                break;
            case 2:
                System.out.println("You selected Manage Guests.");
                break;
            case 3:
                System.out.println("Exiting the system. Goodbye!");
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
        scanner.close();
    }
}
