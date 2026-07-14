package entity;

/**
 * @author Lim How Voon
 */
public class Guest {

    private String confirmationNo;
    private String name;
    private String phone;
    private String loyaltyTier;
    private double billingAmount;
    private String roomNo;

    public Guest(String confirmationNo, String name, String phone, String loyaltyTier, double billingAmount,
            String roomNo) {
        this.confirmationNo = confirmationNo;
        this.name = name;
        this.phone = phone;
        this.loyaltyTier = loyaltyTier;
        this.billingAmount = billingAmount;
        this.roomNo = roomNo;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public void setConfirmationNo(String confirmationNo) {
        this.confirmationNo = confirmationNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(String loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }

    public double getBillingAmount() {
        return billingAmount;
    }

    public void setBillingAmount(double billingAmount) {
        this.billingAmount = billingAmount;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    @Override
    public String toString() {
        return "Guest{" +
                "Confirmation No='" + confirmationNo + '\'' +
                ", Name='" + name + '\'' +
                ", Tier='" + loyaltyTier + '\'' +
                ", Room='" + roomNo + '\'' +
                ", Billing=" + billingAmount +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Guest guest = (Guest) obj;
        return confirmationNo != null ? confirmationNo.equals(guest.confirmationNo) : guest.confirmationNo == null;
    }
}