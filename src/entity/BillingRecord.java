package entity;

/**
 * @author Chang Han Yean
 */
public class BillingRecord {
    private String billId;
    private String bookingId;
    private String confirmationNo;
    private String roomNumber;
    private String roomType;
    private String checkInDate;
    private String checkOutDate;
    private int nights;
    private double amount;
    private String paymentStatus;
    private String createdAt;

    public BillingRecord(String billId, String bookingId, String confirmationNo, String roomNumber, String roomType,
            String checkInDate, String checkOutDate, int nights, double amount, String paymentStatus,
            String createdAt) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.confirmationNo = confirmationNo;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.nights = nights;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public String getBillId() {
        return billId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public int getNights() {
        return nights;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
