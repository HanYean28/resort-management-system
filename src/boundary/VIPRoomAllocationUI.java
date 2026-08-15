package boundary;

import control.VIPRoomAllocation;
import entity.Guest;
import java.util.scanner;

pubic class VIPRoomAllocationUI {

    private Scanner scanner;
    private VIPRoomAllocation controller;

    public VIPRoomAllocationUI() {
        scanner = new Scanner(System.in);
        controller = new VIPRoomAllocation();
    }

    public void start(){
        int choice;

        do{
            displayMenu();
            choice = scanner.nextInt();
            switch(choice){
                case 1:
                    addVIPGuest();
                    break;
                case 2:
                    allocateRoom();
                    break;
                case 3:
                    findGuest();
                    break;
                case 4:
                    removeGuest();
                    break;
                case 5:
                    displayWaitingList();
                    break;
                case 6:
                    System.out.println("Exiting VIP Room Allocation System.");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while(choice != 6);
    }

    private void displayMenu(){
        System.out.println("\nVIP Room Allocation System");
        System.out.println("[1] Add VIP Guest");
        System.out.println("[2] Allocate Room to VIP Guest");
        System.out.println("[3] Find Guest by Confirmation Number");
        System.out.println("[4] Remove Guest from Waiting List");
        System.out.println("[5] Display Waiting List");
        System.out.println("[6] Exit");
        System.out.print("Enter your choice: ");
    }

    //Collect guest info and add to queue
    private void addVIPGuest(){
        System.out.println("Add New VIP Guest");
        System.out.print("Enter Confirmation Number: ");
        String confirmationNo = scanner.next();
        System.out.print("Enter Name: ");
        String name = scanner.next();
        System.out.print("Enter Phone: ");
        String phone = scanner.next();
        System.out.print("Enter Loyalty Tier (Silver, Gold, Platinum, Elite, Diamond): ");
        String loyaltyTier = scanner.next();
        System.out.print("Enter Billing Amount: ");
        double billingAmount = scanner.nextDouble();

        Guest guest = new Guest(confirmationNo, name,
             phone, loyaltyTier, 
             billingAmount, null);
        controller.addVIPGuest(guest);
        System.out.println("VIP Guest added successfully.");
    }

    //Display current highest priority guest
    private void viewNextVIPGuest(){
        Guest guest = controller.getNextVIPGuest();
        if(guest != null){
            System.out.println("Next VIP Guest: " + guest);
        } else {
            System.out.println("No VIP guests in the waiting list.");
        }
        displayGuestDetails(guest);
    }

    //Display all guest in priority order
    private void displayWaitingList(){
        Guest[] guests = controller.getWaitingList();
        if(guests.length == 0){
            System.out.println("No VIP guests in the waiting list.");
            return;
        }
        System.out.println("Current VIP Waiting List:");
        for (int i = 0; i < guests.length; i++){
            System.out.println((i + 1) + ". " + guests[i]);
            displayGuestDetails(guests[i]);
        }
    }

    //Display detail of guest
    private void displayGuestDetails(Guest guest){
        if(guest != null){
            System.out.println("Confirmation No: " + guest.getConfirmationNo());
            System.out.println("Name: " + guest.getName());
            System.out.println("Phone: " + guest.getPhone());
            System.out.println("Loyalty Tier: " + guest.getLoyaltyTier());
            System.out.println("Billing Amount: " + guest.getBillingAmount());
            System.out.println("Room No: " + guest.getRoomNo());
        }
    }

    private void allocateRoom(){
        if (controller.isQueueEmpty()){
            System.out.println("No VIP guests in the waiting list.");
            return;
        }
        Room[] availableRooms = controller.getAvailableRooms();

        if(availableRooms.length == 0){
            System.out.println("No available rooms for allocation.");
            return;
        }

        System.out.println("Available Rooms:");
        for (int i = 0; i < availableRooms.length; i++){
            System.out.println((i + 1) + ". Room Number: " + availableRooms[i]);
        }
        System.out.print("Select a room: ");
        int roomChoice = scanner.nextInt();
        scanner.nextLine();

        if(selection < 1 || selection > availableRooms.length){
            System.out.println("Invalid room selection.");
            return;
        }

        String selectedRoomNumber = availableRooms[selection - 1].getRoomNumber();

        Guest guest = controller.allocateRoom(selectedRoomNumber);

        if (guest == null){
            System.out.println("Room allocation failed. Please try again.");
            return;
        }

        System.out.println();
        System.out.println("ROOM ALLOCATED SUCCESSFULLY");
        System.out.println("Guest: " + guest.getName());
        System.out.println("Confirmation No: " + guest.getConfirmationNo());
        System.out.println("Loyalty Tier: " + guest.getLoyaltyTier());
        System.out.println("Room No: "+ guest.getRoomNo());
    }

    private void removeGuest(){
        System.out.println("Remove Guest")
        System.out.println("Confrimation Number: ");
        String confirmationNo = scanner.nextLine();

        boolean success = controller.removeGuest(confirmationNo);

        if(success){
            System.out.println("Guest removed successfully");
        } else {
            System.out.println("Guest not found");
        }
    }
}