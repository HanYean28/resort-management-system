package entity;

/**
 * Represents a hotel booking request.
 *
 * Two constructors are provided:
 *
 *   1. Guest-object constructor — used when a full Guest object is available
 *      (e.g. when creating a booking from the UI with a live Guest in memory).
 *
 *   2. String constructor — used when deserialising from bookings.txt,
 *      where only the confirmationNo string is stored per line.
 *      The confirmationNo is stored directly so BookingDAO and
 *      VIPRoomAllocation can serialise/deserialise without requiring
 *      a full Guest object to be loaded at the same time.
 *
 * @author Chang Han Yean
 */
public class BookingRequest {

    private String bookingId;
    private String confirmationNo;      // stored directly for file I/O
    private Guest  guest;               // may be null when loaded from file
    private String bookingType;
    private String requestedRoomType;
    private String checkInDate;
    private String checkOutDate;
    private String status;
    private String assignedRoomNumber;
    private String createdAt;

    // -------------------------------------------------------
    // Constructor 1 — Guest object (used by UI / controller)
    // -------------------------------------------------------

    /**
     * Creates a BookingRequest with a fully populated Guest object.
     *
     * Use this when the Guest is already loaded in memory and the full
     * object reference is available (e.g. when creating a new booking
     * from the front-desk UI).
     *
     * @param bookingId          unique booking identifier (e.g. "B0001")
     * @param guest              the Guest making the booking
     * @param bookingType        booking channel ("Walk-In", "Standard", "VIP")
     * @param requestedRoomType  preferred room type ("Standard", "Deluxe", "Suite")
     * @param checkInDate        check-in date string
     * @param checkOutDate       check-out date string
     * @param status             current status ("Pending", "Assigned", "Checked Out")
     * @param assignedRoomNumber the room number assigned, or "N/A" if not yet assigned
     * @param createdAt          timestamp when this booking was created
     */
    public BookingRequest(String bookingId, Guest guest, String bookingType,
                          String requestedRoomType, String checkInDate,
                          String checkOutDate, String status,
                          String assignedRoomNumber, String createdAt) {

        this.bookingId          = bookingId;
        this.guest              = guest;
        this.confirmationNo     = (guest != null) ? guest.getConfirmationNo() : null;
        this.bookingType        = bookingType;
        this.requestedRoomType  = requestedRoomType;
        this.checkInDate        = checkInDate;
        this.checkOutDate       = checkOutDate;
        this.status             = status;
        this.assignedRoomNumber = assignedRoomNumber;
        this.createdAt          = createdAt;
    }

    // -------------------------------------------------------
    // Constructor 2 — String confirmationNo (used by DAO / file I/O)
    // -------------------------------------------------------

    /**
     * Creates a BookingRequest using a raw confirmation number string.
     *
     * Use this when deserialising from bookings.txt, where each line
     * stores the confirmationNo as a plain string field rather than
     * embedding a full Guest object. The guest field is left null;
     * callers that need the full Guest profile should join separately
     * using GuestDAO.
     *
     * @param bookingId          unique booking identifier (e.g. "B0001")
     * @param confirmationNo     the guest's 8-digit confirmation number
     * @param bookingType        booking channel ("Walk-In", "Standard", "VIP")
     * @param requestedRoomType  preferred room type
     * @param checkInDate        check-in date string
     * @param checkOutDate       check-out date string
     * @param status             current status
     * @param assignedRoomNumber the room number assigned, or "N/A"
     * @param createdAt          timestamp when this booking was created
     */
    public BookingRequest(String bookingId, String confirmationNo,
                          String bookingType, String requestedRoomType,
                          String checkInDate, String checkOutDate,
                          String status, String assignedRoomNumber,
                          String createdAt) {

        this.bookingId          = bookingId;
        this.confirmationNo     = confirmationNo;
        this.guest              = null;   // not loaded — use GuestDAO if needed
        this.bookingType        = bookingType;
        this.requestedRoomType  = requestedRoomType;
        this.checkInDate        = checkInDate;
        this.checkOutDate       = checkOutDate;
        this.status             = status;
        this.assignedRoomNumber = assignedRoomNumber;
        this.createdAt          = createdAt;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public String getBookingId()          { return bookingId;          }
    public String getConfirmationNo()     { return confirmationNo;     }
    public Guest  getGuest()             { return guest;              }
    public String getBookingType()        { return bookingType;        }
    public String getRequestedRoomType()  { return requestedRoomType;  }
    public String getCheckInDate()        { return checkInDate;        }
    public String getCheckOutDate()       { return checkOutDate;       }
    public String getStatus()             { return status;             }
    public String getAssignedRoomNumber() { return assignedRoomNumber; }
    public String getCreatedAt()          { return createdAt;          }

    // -------------------------------------------------------
    // Setters (mutable fields only)
    // -------------------------------------------------------

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
        if (guest != null && this.confirmationNo == null) {
            this.confirmationNo = guest.getConfirmationNo();
        }
    }

    public void setAssignedRoomNumber(String assignedRoomNumber) {
        this.assignedRoomNumber = assignedRoomNumber;
    }

    // -------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------

    @Override
    public String toString() {
        return String.format(
                "BookingRequest{bookingId='%s', confirmationNo='%s', type='%s', "
                + "room='%s', checkIn='%s', checkOut='%s', status='%s'}",
                bookingId, confirmationNo, bookingType,
                requestedRoomType, checkInDate, checkOutDate, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BookingRequest other = (BookingRequest) obj;
        return bookingId != null && bookingId.equals(other.bookingId);
    }

    @Override
    public int hashCode() {
        return bookingId != null ? bookingId.hashCode() : 0;
    }
}