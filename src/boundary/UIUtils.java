package boundary;

/**
 * @author Chang Han Yean
 */

public class UIUtils {

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

    public static void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue");
        try {
            System.in.read(new byte[System.in.available()]);
            System.in.read();
        } catch (Exception e) {
        }
    }
}
