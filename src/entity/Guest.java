package entity;

/**
 * @author Lim How Voon
 */
public class Guest implements Comparable<Guest> {

    private String confirmationNo;
    private String name;
    private String phone;
    private String loyaltyTier;
    private double billingAmount;
    private String roomNo;
    private int arrivalOrder; // New attribute to track the order of arrival

    public Guest(String confirmationNo, String name, String phone, String loyaltyTier, double billingAmount,
            String roomNo) {
        this.confirmationNo = confirmationNo;
        this.name = name;
        this.phone = phone;
        this.loyaltyTier = loyaltyTier;
        this.billingAmount = billingAmount;
        this.roomNo = roomNo;
        this.arrivalOrder = 0; 
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

    public int getArrivalOrder() {
        return arrivalOrder;
    }

    public void setArrivalOrder(int arrivalOrder){
        this.arrivalOrder = arrivalOrder;
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

    @Override
    public int hashCode() {
        return confirmationNo != null ? confirmationNo.hashCode() : 0;
    }


    //Determine priority of guest 
    @Override
    public int compareTo(Guest other) {
        int thisPriority = getPriority(this.loyaltyTier);
        int otherPriority = getPriority(other.loyaltyTier);

        int tierComparison = Integer.compare(thisPriority, otherPriority);
        if(tierComparison != 0) {
            return tierComparison;
        } else {
            return Integer.compare(this.arrivalOrder, other.arrivalOrder);
        }
    }

    private int getPriority(String loyaltyTier){
        if(loyaltyTier == null){
            return 0; 
        }
        switch(loyaltyTier.toLowerCase()){
            case "diamond":
                return 5;
            case "elite":
                return 4;
            case "platinum":
                return 3;
            case "gold":
                return 2;
            case "silver":
                return 1;
            default:
                return 0; // Default priority for unrecognized loyalty tiers
        }
    }


    
}