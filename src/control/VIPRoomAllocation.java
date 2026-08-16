package control;
import adt.ArrayPriorityQueue;
import entity.Guest;
import entity.Room;

public class VIPRoomAllocation {

    private ArrayPriorityQueue<Guest> vipQueue;
    private int arrivalOrder;
    private Room[] rooms;

    //Create Priority Queue
    public VIPRoomAllocation() {
        vipQueue = new ArrayPriorityQueue<>();
        arrivalOrder = 0;
        this.rooms = rooms;
    }

    public void addVIPGuest(Guest guest){
        if(guest == null){
            return;
        }

        guest.setArrivalOrder(arrivalOrder++);
        vipQueue.add(guest);
    }

    //Return all available rooms
    public Room[] getAvailableRooms(){
        int count = 0;
        for(Room room : rooms){
            if(room.getCleanlinessStatus().equals("Ready") && room != null){
                count++;
            }
        }
        Room[] availableRooms = new Room[count];
        for (Room rooms : rooms){
            if(rooms.getCleanlinessStatus().equals("Ready") && rooms != null){
                availableRooms[index++] = rooms;
            }
        }
        return availableRooms;
    }
    

    //Remove guest from priority queue and allocate room
    public Guest allocateRoom(String roomNumber){

        Guest guest = vipQueue.peek();
        if(guest == null){
            return null;
        }

        if(selectedRoom == null){
            return null;
        }
        Guest guest = vipQueue.remove();
        guest.setRoomNo(selectedRoom.getRoomNumber());
        selectedRoom.setCleanlinessStatus("Occupied");
        return guest;
    }

    //Find guest using confirmation number
    public Guest findGuest(string confirmationNo){
        Guest[] guest = getWaitingList();

        for (Guest guest : guests){
            if (geust.getConfirmationNo().equals(confirmationNo)){
                return guest;
            }
        }
        return null;
    }

    //Find available room
    private Room findAvailableRoom(String roomNumber){
        if (roomNumber == null || roomNumber.isEmpty()){
            return null;
        }
        for (Room room : rooms){
            if (room.getRoomNumber().equals(roomNumber) && room.getCleanlinessStatus().equals("Ready")){
                return room;
            }
        }
        return null;
    }

    //remove guest from priority queue
    public boolean removeGuest(String confirmationNo){
        Guest[] guests = getWatingList();

        boolean found = false;
        ArrayPriorityQueue<Guest> newQueue = new ArrayPriorityQueue<>();

        for (Guest guest : guests){
            if (guest.getConfirmationNo().equals(confirmationNo)){
                found = true;
            } else {
                newQueue.add(guest);
            }
        }

        if(found){
            vipQueue = newQueue;
        }
        return found;
    }

    //Return all guests in current priority queue without removing them
    public Guest[] getWaitingList(){
        Guest[] guests = new Guest[vipQueue.size()];
        ArrayPriorityQueue<Guest> tempQueue = new ArrayPriorityQueue<>();
        int index = 0;

        while(!vipQueue.isEmpty()){
            Guest guest = vipQueue.remove();
            guests[index++] = guest;
            tempQueue.add(guest);
        }
        vipQueue = tempQueue;
        return guests;
    }

    public int getWaitingGuestCount(){
        return vipQueue.size();
    }

    public boolean isWaitingListEmpty(){
        return vipQueue.isEmpty();
    }
}