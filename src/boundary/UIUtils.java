package boundary;

import java.util.Scanner;

/**
 * @author Chang Han Yean
 */

public class UIUtils {
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    public static void clearScreen() {
        try {
            String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            for (int i = 0; i < 40; i++) {
                System.out.println();
            }
        }
    }

    public static void pressEnterToContinue(Scanner scanner) {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    public static void printHeader(String title) {
        System.out.println("====================================================================================================");
        System.out.println(centerText(title, 100));
        System.out.println("====================================================================================================");
    }

    public static void printSectionLine() {
        System.out.println("----------------------------------------------------------------------------------------------------");
    }

    public static void printError(String message) {
        System.out.println(RED + "[ERROR] " + message + RESET);
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            line.append(' ');
        }
        line.append(text);
        return line.toString();
    }
}
