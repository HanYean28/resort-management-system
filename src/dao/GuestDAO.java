package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.Guest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles guest file operations for guests.txt.
 * @author Lim How Voon
 */
public class GuestDAO {
    private static final String DATA_FILE = "guests.txt";

    public ListInterface<Guest> loadGuests() {
        ListInterface<Guest> guests = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 4) {
                    String confirmationNo = parts[0].trim();
                    String name = parts[1].trim();
                    String phone = parts[2].trim();
                    String loyaltyTier = parts[3].trim();

                    if (isValidGuest(confirmationNo, name, phone)
                            && !confirmationNoExists(guests, confirmationNo)) {
                        guests.add(new Guest(confirmationNo, name, phone, loyaltyTier));
                    }
                }
            }
        } catch (IOException e) {
            createFileIfMissing();
        }
        return guests;
    }

    public void saveGuests(ListInterface<Guest> guests) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bw.write("# confirmationNo|name|phone|loyaltyTier");
            bw.newLine();
            for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
                Guest guest = guests.getEntry(i);
                bw.write(guest.getConfirmationNo() + "|" + guest.getName() + "|"
                        + guest.getPhone() + "|" + guest.getLoyaltyTier());
                bw.newLine();
            }
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    private boolean isValidGuest(String confirmationNo, String name, String phone) {
        return confirmationNo.matches("\\d{8}")
                && !name.isEmpty()
                && !phone.isEmpty();
    }

    private boolean confirmationNoExists(ListInterface<Guest> guests, String confirmationNo) {
        for (int i = 1; i <= guests.getNumberOfEntries(); i++) {
            if (guests.getEntry(i).getConfirmationNo().equalsIgnoreCase(confirmationNo)) {
                return true;
            }
        }
        return false;
    }

    private void createFileIfMissing() {
        saveGuests(new ArrayList<Guest>());
    }
}
