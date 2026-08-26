package entity;

/**
 * @author Elwin Goh Yao Zu
 */
public class BookingRequest {
    private String bookingId;
    private String confirmationNo;
    private Guest guest;
    private String bookingType;
    private String requestedRoomType;
    private String checkInDate;
    private String checkOutDate;
    private String status;
    private String assignedRoomNumber;
    private String createdAt;

    public BookingRequest(String bookingId, String confirmationNo, String bookingType, String requestedRoomType,
            String checkInDate, String checkOutDate, String status, String assignedRoomNumber, String createdAt) {
        this.bookingId = bookingId;
        this.confirmationNo = confirmationNo;
        this.guest = null;
        this.bookingType = bookingType;
        this.requestedRoomType = requestedRoomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.status = status;
        this.assignedRoomNumber = assignedRoomNumber;
        this.createdAt = createdAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
        if (guest != null) {
            this.confirmationNo = guest.getConfirmationNo();
        }
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public String getBookingType() {
        return bookingType;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedRoomNumber() {
        return assignedRoomNumber;
    }

    public void setAssignedRoomNumber(String assignedRoomNumber) {
        this.assignedRoomNumber = assignedRoomNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
