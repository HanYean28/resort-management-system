package entity;
import java.io.Serializable;

/**
 * @author Lim How Voon
 */
public class Guest implements Serializable, Comparable<Guest> {

    private String confirmationNo;
    private String name;
    private String phone;
    private String loyaltyTier;

    public Guest(String confirmationNo, String name, String phone, String loyaltyTier) {
        this.confirmationNo = confirmationNo;
        this.name = name;
        this.phone = phone;
        this.loyaltyTier = loyaltyTier;
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

    @Override
    public String toString() {
        return "Guest{" +
                "Confirmation No='" + confirmationNo + '\'' +
                ", Name='" + name + '\'' +
                ", Tier='" + loyaltyTier + '\'' +
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

    @Override
    public int compareTo(Guest other) {
        return this.confirmationNo.compareTo(other.confirmationNo);
    }
    
}
