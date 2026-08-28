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
                    guests.add(new Guest(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException e) {
            // Return empty list if file is missing.
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
}
