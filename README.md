# TARUMT Resort Management System

## Overview

This project is a Java console application developed for the BMCS2063 Data Structures and Algorithms assignment. The system supports basic resort operations through four integrated modules:

- Housekeeping and Task Log
- Walk-In Registration and Standard Booking
- VIP and Loyalty Tier Priority Room Allocation
- Front-Desk Service

The project uses custom Abstract Data Types (ADTs) to manage records, booking queues, housekeeping rollback, VIP priority allocation, and guest searching.

## Features

### Housekeeping and Task Log

Cleaning status flow:

```text
Dirty -> Cleaning In Progress -> Inspected -> Ready
```

- Add housekeeping task
  - Changes a selected ready room to Dirty and stores the Ready -> Dirty change in  `StackInterface<HousekeepingLog>` for rollback.
  - Uses `ListInterface<Room>` to search and update room records.

- View current tasks
  - Displays rooms that still require cleaning.
  - Uses `ListInterface<Room>` to collect rooms with active housekeeping statuses.

- Update cleaning status
  - Moves a room to the next cleaning stage.
  - Uses `ListInterface<Room>` for room records and `StackInterface<HousekeepingLog>` to store rollback entries.

- Rollback status
  - Restores the latest cleaning status change.
  - Uses stack LIFO behaviour so the most recent update is undone first.

- Handle late checkout
  - Cancels the current housekeeping task when the room is still occupied by the guest.
  - Uses the rollback stack to restore the room cleaning status and updates occupancy to `Occupied`.

- View task history and reports
  - Displays update, rollback, and late checkout history.
  - Uses `ListInterface<HousekeepingLog>` for filtering, sorting, and report generation.

### Walk-In Registration and Standard Booking

- Add guest
  - Creates a guest record with a generated 8-digit confirmation number.
  - Uses front-desk guest searching to avoid duplicate confirmation numbers before saving.

- Add walk-in booking
  - Creates an immediate booking for today, assigns a ready/vacant room, and marks the booking as `Checked In`.
  - Uses `ListInterface<Room>` and `ListInterface<BookingRequest>` to check room availability, date clashes, and pending booking protection.

- Add standard booking
  - Creates a pending booking request.
  - Stores the booking in `ListInterface<BookingRequest>` and enqueues it into `QueueInterface<BookingRequest>`.

- View current bookings
  - Displays active bookings such as `Pending`, `Assigned`, and `Checked In`.
  - Uses `ListInterface<BookingRequest>` to collect current booking records.

- View pending standard queue
  - Displays standard pending bookings in FIFO order.
  - Uses `QueueInterface<BookingRequest>` with `dequeue()` and `enqueue()` to read and restore queue order.

- Auto assign room
  - Assigns one pending booking at a time.
  - Checks pending VIP bookings first, then processes the standard booking queue in FIFO order.

- Check in booking
  - Changes an assigned booking to `Checked In` and updates the room occupancy to `Occupied`.
  - Uses booking and room records to validate booking status and room condition.

- Check out booking
  - Changes a checked-in booking to `Checked Out` and updates the room to `Dirty` and `Vacant`.
  - Integrates with housekeeping because checked-out rooms become cleaning tasks.

- Cancel booking and reports
  - Cancels pending or assigned bookings and generates booking reports.
  - Uses `ListInterface<BookingRequest>` for filtering, counting, and sorting records.

### VIP and Loyalty Tier Priority Room Allocation

- Add VIP guest
  - Registers a guest with a loyalty tier for VIP processing.
  - Stores VIP guests in `ArrayPriorityQueue` so higher-tier guests can be processed first during allocation.

- Create pending VIP booking
  - Saves VIP booking preferences as a pending booking record.
  - Uses the shared booking records so VIP bookings can be considered during room assignment.

- View VIP queue
  - Displays VIP guests in priority order.
  - Uses `ArrayPriorityQueue` so higher-tier guests are processed first.

- Allocate VIP room
  - Assigns a room to the highest-priority pending VIP booking.
  - Uses priority queue ordering and room/date checking before assignment.

- VIP reports
  - Generates VIP allocation and revenue summaries.
  - Uses stored booking and billing records for filtering and summary output.

### Front-Desk Service

- View all guests
  - Displays guest records in confirmation number order.
  - Uses `BinarySearchTree<Guest>` and in-order traversal.

- Search guest by confirmation number
  - Retrieves guest details using the 8-digit confirmation number.
  - Uses `BinarySearchTree.getEntry()` for non-linear searching.

- Remove guest record
  - Removes a guest from the front-desk guest records.
  - Uses `BinarySearchTree.remove()` and saves the updated guest list.

- Guest directory report
  - Displays guest information with optional filtering.
  - Uses `ListInterface<Guest>` after retrieving records from the BST.

- Billing history report
  - Displays billing records with amount and room type filters.
  - Uses `ListInterface<BillingRecord>` for filtering and output.

## Prerequisites

- Java Development Kit (JDK) 11 or later
- NetBeans IDE, or a terminal that can run `javac` and `java`

## Running the Application

### NetBeans

1. Open NetBeans.
2. Select `File > Open Project`.
3. Select the `resort-management-system` project folder.
4. Run the project.

Main class:

```text
main
```

### Command Line

Compile:

```text
javac -d out src\main.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\adt\*.java src\utility\*.java
```

Run:

```text
java -cp out main
```

## Project Structure

```text
src/adt       Custom ADT interfaces and implementations
src/boundary  Console UI classes
src/control   Module controllers and business rules
src/dao       Text file loading and saving
src/entity    Entity classes
src/utility   Shared helper classes
```

## Data Files

The system uses text files in the project root folder:

```text
guests.txt
rooms.txt
bookings.txt
billing.txt
housekeeping_task_history.txt
```

Each file includes a header row describing its data format.

## ADTs Used

- `ListInterface` / `ArrayList`
  - Stores and processes room, guest, booking, billing, and report records.

- `QueueInterface` / `ArrayQueue`
  - Maintains pending standard bookings in FIFO order.

- `StackInterface` / `ArrayStack`
  - Supports housekeeping rollback by restoring the latest cleaning status change first.

- `BinarySearchTreeInterface` / `BinarySearchTree`
  - Supports front-desk guest searching by confirmation number.

- `ArrayPriorityQueue`
  - Processes VIP guests according to loyalty tier priority.

## Module Integration

- Booking and front desk share guest records through `guests.txt`.
- Booking, VIP, and front desk share booking records through `bookings.txt`.
- Booking and VIP generate billing records in `billing.txt` after a guest is checked in or allocated according to the module flow.
- Booking checkout updates room occupancy and cleanliness so housekeeping can continue the cleaning workflow.
- Housekeeping updates room cleanliness so booking and front desk can identify rooms that are ready for guests.
- Walk-in booking protects pending VIP and standard bookings before assigning a room immediately.
- Auto assignment gives pending VIP bookings priority before processing the standard booking queue.

## File Data Handling

DAO classes are responsible for reading and writing text files. During loading, invalid rows are skipped so corrupted text file data does not stop the application.

Protected file issues include:

- wrong number of fields
- invalid ID format
- invalid date format
- invalid status value
- invalid billing number value
- duplicate IDs in file data

Generated IDs are handled by controller classes and checked before use to avoid duplicate records.
