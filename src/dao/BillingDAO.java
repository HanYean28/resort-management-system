package dao;

import adt.ArrayList;
import adt.ListInterface;
import entity.BillingRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import utility.DateUtils;

/**
 * Handles billing file operations for billing.txt.
 * @author Lim How Voon
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
                    BillingRecord bill = createBillingRecord(parts);
                    if (bill != null && !billIdExists(bills, bill.getBillId())) {
                        bills.add(bill);
                    }
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

    private BillingRecord createBillingRecord(String[] parts) {
        String billId = parts[0].trim();
        String bookingId = parts[1].trim();
        String confirmationNo = parts[2].trim();
        String roomNumber = parts[3].trim();
        String roomType = parts[4].trim();
        String checkInDate = parts[5].trim();
        String checkOutDate = parts[6].trim();
        String paymentStatus = parts[9].trim();
        String createdAt = parts[10].trim();

        try {
            int nights = Integer.parseInt(parts[7].trim());
            double amount = Double.parseDouble(parts[8].trim());

            if (isValidBilling(billId, bookingId, confirmationNo, roomNumber, roomType,
                    checkInDate, checkOutDate, nights, amount, paymentStatus, createdAt)) {
                return new BillingRecord(billId, bookingId, confirmationNo, roomNumber, roomType,
                        checkInDate, checkOutDate, nights, amount, paymentStatus, createdAt);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private boolean isValidBilling(String billId, String bookingId, String confirmationNo, String roomNumber,
            String roomType, String checkInDate, String checkOutDate, int nights, double amount,
            String paymentStatus, String createdAt) {
        try {
            DateUtils.parseDate(checkInDate);
            DateUtils.parseDate(checkOutDate);
        } catch (Exception e) {
            return false;
        }

        return billId.matches("BL\\d{4}")
                && bookingId.matches("B\\d{4}")
                && confirmationNo.matches("\\d{8}")
                && !roomNumber.isEmpty()
                && isValidRoomType(roomType)
                && DateUtils.isAfter(checkOutDate, checkInDate)
                && nights > 0
                && amount >= 0
                && !paymentStatus.isEmpty()
                && !createdAt.isEmpty();
    }

    private boolean billIdExists(ListInterface<BillingRecord> bills, String billId) {
        for (int i = 1; i <= bills.getNumberOfEntries(); i++) {
            if (bills.getEntry(i).getBillId().equalsIgnoreCase(billId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidRoomType(String roomType) {
        return roomType.equalsIgnoreCase("Standard")
                || roomType.equalsIgnoreCase("Deluxe")
                || roomType.equalsIgnoreCase("Suite");
    }
}
