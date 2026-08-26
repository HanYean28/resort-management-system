package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.BillingRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles billing file operations for billing.txt.
 */
public class BillingDAO {
    private static final String DATA_FILE = "billing.txt";

    public ListInterface<BillingRecord> loadBillingRecords() {
        ListInterface<BillingRecord> bills = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length == 11) {
                    bills.add(new BillingRecord(parts[0], parts[1], parts[2], parts[3], parts[4],
                            parts[5], parts[6], Integer.parseInt(parts[7]), Double.parseDouble(parts[8]),
                            parts[9], parts[10]));
                }
            }
        } catch (IOException e) {
            createFileIfMissing();
        }
        return bills;
    }

    public void appendBillingRecord(BillingRecord bill) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE, true))) {
            bw.write(bill.getBillId() + "|" + bill.getBookingId() + "|" + bill.getConfirmationNo() + "|"
                    + bill.getRoomNumber() + "|" + bill.getRoomType() + "|" + bill.getCheckInDate() + "|"
                    + bill.getCheckOutDate() + "|" + bill.getNights() + "|" + bill.getAmount() + "|"
                    + bill.getPaymentStatus() + "|" + bill.getCreatedAt());
            bw.newLine();
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }

    public void createFileIfMissing() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE))) {
            bw.write("# billId|bookingId|confirmationNo|roomNumber|roomType|checkInDate|checkOutDate|nights|amount|paymentStatus|createdAt");
            bw.newLine();
        } catch (IOException e) {
            // Keep console flow simple; failed saves are ignored in this prototype.
        }
    }
}
