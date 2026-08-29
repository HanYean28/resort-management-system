# System Testing and Demo Flow Checklist

Project: TARUMT Resort Management System  
Purpose: Use this checklist to test every module, validation, and module integration before presentation.

## Recommended Testing Order

1. Front-Desk Service
2. Housekeeping and Task Log
3. Walk-In Registration and Standard Booking
4. Housekeeping integration check
5. VIP and Loyalty Tier Priority Room Allocation
6. Final reports and full system verification

This order is used because guest, room, booking, billing, and housekeeping data are connected. Front Desk is tested first to confirm existing guest and room data. Housekeeping is tested before booking because booking depends on room cleanliness and occupancy. Booking is tested next because it creates check-in, checkout, billing, and housekeeping changes. VIP is tested after the normal booking flow because it shares booking and room data.

## Module Dependency Summary

| Module | Depends On | Reason |
| --- | --- | --- |
| Front Desk | Guests, rooms, bookings, billing | It searches and displays shared system records. |
| Housekeeping | Rooms, housekeeping history | It updates room cleaning status and rollback history. |
| Booking | Guests, rooms, bookings, billing | Guest and room records are required before booking. |
| VIP | Guests, rooms, bookings | VIP priority booking uses shared guest, room, and booking records. |
| Reports | Module data files | Reports are generated from stored records. |

## Demo Data To Use

| Purpose | Data |
| --- | --- |
| Normal guest search | `80000008` Mei Ling |
| Pending standard booking | `B0002` Farah Ali |
| Assigned standard booking | `B0003` Daniel Wong, room `107` |
| Checked-in booking | `B0005` Alex Tan, room `108` |
| Checked-out booking | `B0004` Jason Ng, room `106` |
| Pending VIP booking | `B0006` Carmen Lim |
| Dirty room | `102`, `106` |
| Cleaning In Progress room | `101` |
| Inspected room | `103` |
| Ready/Vacant rooms | `104`, `109`, `110`, `111`, `112` |

## Presentation Run Sheet

Use this section during the actual demo. It is arranged so each module proves one main point and carries data to the next module.

### Before Starting

1. Compile and run the system.
2. Keep the data files unchanged before demo unless you intentionally want to test create/update actions.
3. During demo, write down any newly generated confirmation number or booking ID.

### Step 1: Main Menu Validation

1. At Main Menu, enter `abc`.
   - Expected: invalid choice/error message.
2. Enter `9`.
   - Expected: invalid choice/error message.
3. Enter `4` to open Front-Desk Service.

### Step 2: Front-Desk Service Demo

1. Choose `1` View All Guests.
   - Expected: guest records appear in confirmation number order.
2. Choose `2` Search Guest by Confirmation Number.
   - Input: `80000008`
   - Expected: Mei Ling appears.
3. Search again with:
   - Input: `99999999`
   - Expected: guest not found.
4. Choose `6` View Rooms Available Today.
   - Expected: only Ready and Vacant rooms are shown.
5. Choose `5` Guest Billing History Report.
   - First show all records.
   - Then filter room type as `Deluxe`.
   - Expected: Deluxe billing record appears.
6. Return to Main Menu.

Carry forward: We confirmed guest, room, and billing data exist.

### Step 3: Housekeeping Demo

1. Enter `1` Housekeeping.
2. Choose `2` View Current Tasks.
   - Expected: rooms `101`, `102`, `103`, and `106` appear.
3. Choose `3` Update Cleaning Status.
   - Input room: `101`
   - Confirm: `Y`
   - Expected: `101` moves from Cleaning In Progress to Inspected.
4. Choose `4` Rollback Status.
   - Input room: `101`
   - Confirm: `Y`
   - Expected: `101` returns to Cleaning In Progress.
5. Choose `1` Add Housekeeping Task.
   - Input room: `104`
   - Expected: `104` changes from Ready to Dirty.
6. Test validation:
   - Add task for `105`
   - Expected: rejected because room is Occupied.
7. Test validation:
   - Add task for `102`
   - Expected: rejected because room already has active task.
8. Choose `5` View Task History.
   - Input: `ALL`
   - Expected: Update, Rollback, and Late Checkout actions appear.
9. Choose `7` Generate Reports.
   - Report 1: choose Dirty + Standard.
   - Expected: Dirty Standard rooms appear.
   - Report 2: choose Late Checkout + Newest First.
   - Expected: late checkout history appears.
10. Return to Main Menu.

Carry forward: We proved room status changes affect room availability.

### Step 4: Walk-In and Standard Booking Demo

1. Enter `2` Walk-In and Standard Booking.
2. Choose `1` Add Guest.
3. First test validation:
   - Name: leave blank or enter invalid value.
   - Phone: `abc`
   - Expected: error message.
4. Add valid guest:
   - Name: `Test Guest`
   - Phone: `012-3456789`
   - Expected: new 8-digit confirmation number generated.
5. Write down the generated confirmation number.
6. Choose `2` Add Walk-In Booking.
   - Use the new confirmation number.
   - Choose room type with available room.
   - Check-out date: `2026-08-30`
   - Expected: booking becomes Checked In, room becomes Occupied, and summary is shown.
7. Choose `3` Add Standard Booking.
   - Use guest `80000011`.
   - Choose a room type.
   - Check-in date: `2026-08-30`
   - Check-out date: `2026-09-01`
   - Expected: booking becomes Pending and enters standard queue.
8. Test validation:
   - Date format: `2026-09-99`
   - Past check-in date: `2026-08-01`
   - Check-out before check-in: `2026-08-29`
   - Expected: all invalid dates are rejected.
9. Choose `5` View Pending Standard Queue.
   - Expected: pending bookings appear in FIFO order.
10. Choose `6` Auto Assign Room.
   - Expected: system attempts VIP first, then standard FIFO booking.
11. Choose `7` Check In Booking.
   - Use an Assigned booking, for example `B0003` if still assigned.
   - Expected: booking becomes Checked In and room becomes Occupied.
12. Choose `8` Check Out Booking.
   - Use a Checked In booking, for example `B0005` if still checked in.
   - Expected: booking becomes Checked Out and room becomes Dirty/Vacant.
13. Choose `9` Cancel Booking.
   - Try Checked In or Checked Out booking.
   - Expected: cancellation rejected.
14. Choose `10` Generate Reports.
   - Booking Report: filter by status.
   - Room Type Demand Report: show demand count and percentage.
15. Return to Main Menu.

Carry forward: We created/updated booking, billing, room status, and housekeeping data.

### Step 5: Housekeeping Integration Demo

1. Enter `1` Housekeeping again.
2. Choose `2` View Current Tasks.
3. Expected: the room checked out from Booking is now Dirty/Vacant and appears as a housekeeping task.
4. Return to Main Menu.

Carry forward: We proved Booking checkout connects to Housekeeping.

### Step 6: VIP Demo

1. Enter `3` VIP Priority Room Allocation.
2. Choose `5` View Priority Queue.
   - Expected: higher loyalty tier appears first.
3. Choose `1` Add VIP Guest.
   - Test invalid name or phone first.
   - Then add a valid VIP guest.
4. Choose `3` Create Booking.
   - Use a VIP guest and valid dates.
   - Expected: pending VIP booking created.
5. Choose `6` Generate Report.
   - Test VIP Booking Report filter.
   - Test VIP Revenue Summary Report sort/filter.
6. Return to Main Menu.

Carry forward: We proved VIP priority is separate but uses shared booking and room data.

### Step 7: Final Verification

1. Enter `4` Front-Desk Service.
2. Search the guest created or checked in during Booking.
3. Open Billing History Report.
4. Verify billing record appears after check-in.
5. Open Rooms Available Today.
6. Verify Dirty or Occupied rooms are not shown as available.
7. Return to Main Menu.
8. Enter `0` Exit.

Final expected result: all created and updated data remains consistent across Front Desk, Housekeeping, Booking, VIP, and Reports.

## 0% to 100% Testing Progress

| Progress | Testing Area | Done |
| --- | --- | --- |
| 0% | Compile and run system | [ ] |
| 10% | Main menu navigation and invalid menu input | [ ] |
| 25% | Front Desk guest, room, billing search/display | [ ] |
| 45% | Housekeeping task, update, rollback, history, reports | [ ] |
| 65% | Booking guest, walk-in, standard queue, assign, check-in, checkout, cancel | [ ] |
| 75% | Booking and housekeeping integration after checkout | [ ] |
| 85% | VIP priority queue, VIP booking, VIP reports | [ ] |
| 95% | Report filters and sorting across modules | [ ] |
| 100% | End-to-end flow verified and data consistency checked | [ ] |

# 1. System Startup and Main Menu

## Purpose

Verify that the application starts correctly and the main menu routes to every module.

## Demo Flow

1. Run the application.
2. At Main Menu, enter invalid input such as `abc` or `9`.
3. Confirm error message appears and the program does not crash.
4. Enter `4` to open Front-Desk Service.
5. Return to Main Menu.
6. Repeat basic navigation for module `1`, `2`, and `3`.

## Expected Result

- Invalid menu input is rejected.
- Valid choices open the correct module.
- `0` exits or returns correctly.

## Validation To Check

| Field / Action | Invalid Input | Expected Result |
| --- | --- | --- |
| Main menu choice | `abc` | Error message, menu repeats |
| Main menu choice | `9` | Error message, menu repeats |
| Return option | `0` | Returns/exits correctly |

# 2. Front-Desk Service

## Purpose

Verify guest search, guest list, billing history, and room availability.

## Prerequisite

Guest, room, booking, and billing files must have valid records.

## Demo Flow

1. Main Menu -> `4` Front-Desk Service.
2. Choose `1` View All Guests.
3. Verify guests are displayed in confirmation number order.
4. Choose `2` Search Guest by Confirmation Number.
5. Enter `80000008`.
6. Verify Mei Ling is displayed.
7. Search `99999999`.
8. Verify guest not found message.
9. Choose `6` View Rooms Available Today.
10. Verify only `Ready` and `Vacant` rooms are displayed.
11. Choose `5` Guest Billing History Report.
12. Filter by `All`, then filter by `Deluxe`.
13. Return to Main Menu.

## Test Cases

| No. | Function | Test Type | Steps / Input | Expected Result | Result |
| --- | --- | --- | --- | --- | --- |
| 1 | View All Guests | Positive | Choose `1` | Guest list appears sorted | [ ] |
| 2 | Search Guest | Positive | Enter `80000008` | Mei Ling details appear | [ ] |
| 3 | Search Guest | Negative | Enter `99999999` | Guest not found | [ ] |
| 4 | View Available Rooms | Positive | Choose `6` | Only Ready/Vacant rooms shown | [ ] |
| 5 | Billing Report | Positive | Filter `Deluxe` | Deluxe billing row appears | [ ] |

## Validation To Check

- Invalid confirmation number.
- Non-existing confirmation number.
- Invalid menu option.
- Billing filter with no matching result.

## Integration Check

- Guest data should match `guests.txt`.
- Current room shown should match active checked-in booking.
- Billing report should match `billing.txt`.
- Available rooms should exclude Dirty, Cleaning In Progress, Inspected, and Occupied rooms.

**Next Module: Housekeeping and Task Log**

# 3. Housekeeping and Task Log

## Purpose

Verify room cleaning task creation, sequential status update, rollback stack, late checkout handling, task history, and reports.

## Prerequisite

Room records must exist. Use rooms `101`, `102`, `103`, `104`, `105`, and `106`.

## Demo Flow

1. Main Menu -> `1` Housekeeping.
2. Choose `2` View Current Tasks.
3. Verify `101`, `102`, `103`, and `106` appear.
4. Choose `3` Update Cleaning Status.
5. Enter `101`, confirm `Y`.
6. Verify room moves to next cleaning status.
7. Choose `4` Rollback Status.
8. Enter `101`, confirm `Y`.
9. Verify status returns to previous status.
10. Choose `1` Add Housekeeping Task.
11. Enter `104`.
12. Verify `104` becomes Dirty.
13. Try add task for `105`.
14. Verify occupied room is rejected.
15. Try add task for `102`.
16. Verify active task is rejected.
17. Choose `5` View Task History.
18. Enter `ALL`.
19. Verify Update, Rollback, and Late Checkout actions are shown.
20. Choose `7` Generate Reports.
21. Report 1: filter `Dirty` and `Standard`.
22. Report 2: filter action `Late Checkout`, sort `Newest First`.

## Test Cases

| No. | Function | Test Type | Steps / Input | Expected Result | Result |
| --- | --- | --- | --- | --- | --- |
| 1 | View Current Tasks | Positive | Choose `2` | Active tasks shown only | [ ] |
| 2 | Update Status | Positive | Room `101`, confirm `Y` | Status moves to next stage | [ ] |
| 3 | Rollback Status | Positive | Room `101`, confirm `Y` | Previous status restored | [ ] |
| 4 | Add Task | Positive | Room `104` | Room becomes Dirty | [ ] |
| 5 | Add Task | Negative | Room `105` | Rejected because Occupied | [ ] |
| 6 | Add Task | Negative | Room `102` | Rejected because active task exists | [ ] |
| 7 | View History | Positive | Enter `ALL` | Full history appears | [ ] |
| 8 | Report 1 | Positive | Dirty + Standard | Dirty Standard rooms shown | [ ] |
| 9 | Report 2 | Positive | Late Checkout + Newest First | Late checkout history shown | [ ] |

## Validation To Check

- Invalid room number.
- Blank room number.
- Occupied room cannot be added as housekeeping task.
- Room with active task cannot be added again.
- Status cannot skip sequence.
- Rollback cannot run if no stack record exists.
- Report filters handle no matching data.

## Integration Check

- Room status updates must save into `rooms.txt`.
- Task history must save into `housekeeping_task_history.txt`.
- Checkout from Booking should later create Dirty/Vacant room for housekeeping.
- Late checkout should restore cleaning status using rollback logic and set occupancy to Occupied.

**Next Module: Walk-In Registration and Standard Booking**

# 4. Walk-In Registration and Standard Booking

## Purpose

Verify guest creation, walk-in booking, standard booking queue, auto assignment, check-in, checkout, cancellation, billing generation, and booking reports.

## Prerequisite

Guest and room records must exist. At least one Ready/Vacant room must exist.

## Demo Flow

1. Main Menu -> `2` Walk-In and Standard Booking.
2. Choose `1` Add Guest.
3. Test invalid name or phone first.
4. Add valid guest:
   - Name: `Test Guest`
   - Phone: `012-3456789`
5. Record the generated confirmation number.
6. Choose `2` Add Walk-In Booking.
7. Use the new confirmation number.
8. Select a room type with available room.
9. Enter tomorrow as checkout date.
10. Verify booking summary and room assignment.
11. Choose `3` Add Standard Booking.
12. Use existing guest `80000011`.
13. Select room type.
14. Enter valid check-in and check-out dates.
15. Verify booking becomes Pending and enters queue.
16. Choose `5` View Pending Standard Queue.
17. Verify FIFO queue display.
18. Choose `6` Auto Assign Room.
19. Verify one pending booking is assigned if available.
20. Choose `7` Check In Booking.
21. Use assigned booking `B0003` if still available.
22. Verify booking becomes Checked In and room becomes Occupied.
23. Choose `8` Check Out Booking.
24. Use checked-in booking `B0005`.
25. Verify booking becomes Checked Out and room becomes Dirty/Vacant.
26. Choose `9` Cancel Booking.
27. Try cancel checked-in/checked-out booking.
28. Verify rejected.
29. Choose `10` Generate Reports.
30. Test Booking Report filter and Room Type Demand Report.

## Test Cases

| No. | Function | Test Type | Steps / Input | Expected Result | Result |
| --- | --- | --- | --- | --- | --- |
| 1 | Add Guest | Positive | Valid name and phone | Guest saved with 8-digit confirmation no | [ ] |
| 2 | Add Guest | Negative | Empty name/phone | Error message shown | [ ] |
| 3 | Add Guest | Negative | Invalid phone | Error message shown | [ ] |
| 4 | Add Walk-In | Positive | Existing guest + valid checkout | Booking Checked In, room Occupied | [ ] |
| 5 | Add Walk-In | Negative | Non-existing confirmation no | Guest not found | [ ] |
| 6 | Add Standard | Positive | Valid dates and room type | Booking Pending and queued | [ ] |
| 7 | Add Standard | Negative | Past check-in date | Rejected | [ ] |
| 8 | Add Standard | Negative | Checkout before check-in | Rejected | [ ] |
| 9 | View Queue | Positive | Choose `5` | Pending queue shown FIFO | [ ] |
| 10 | Auto Assign | Positive | Choose `6` | One booking assigned | [ ] |
| 11 | Check In | Positive | Assigned booking ID | Booking Checked In, room Occupied | [ ] |
| 12 | Check Out | Positive | Checked-in booking ID | Booking Checked Out, room Dirty/Vacant | [ ] |
| 13 | Cancel | Negative | Checked-in/checked-out booking | Rejected | [ ] |
| 14 | Reports | Positive | Apply filters | Matching rows and totals shown | [ ] |

## Validation To Check

- Empty guest name.
- Invalid phone number.
- Non-existing confirmation number.
- Invalid date format.
- Check-in date before today.
- Checkout date same as check-in.
- Checkout date before check-in.
- Check in Pending booking.
- Check out Assigned/Pending booking.
- Cancel Checked In/Checked Out booking.
- Walk-in when no room is available.

## Integration Check

- Add Guest updates guest file and can be searched in Front Desk.
- Walk-in booking updates booking status to Checked In.
- Walk-in booking updates room occupancy to Occupied.
- Check-in creates billing record.
- Checkout updates room to Dirty/Vacant.
- Checkout room appears in Housekeeping current tasks.
- Standard booking queue uses FIFO.
- Auto assign checks VIP priority before standard booking.

**Next Module: Housekeeping Integration Check**

# 5. Housekeeping Integration Check After Checkout

## Purpose

Verify that Booking checkout correctly connects to Housekeeping.

## Demo Flow

1. After checkout in Booking, return to Main Menu.
2. Open Housekeeping.
3. Choose `2` View Current Tasks.
4. Confirm the checked-out room appears as Dirty/Vacant.
5. Choose `5` View Task History.
6. Confirm the status change is recorded.

## Expected Result

- Checked-out room becomes a housekeeping task.
- Room is not available for new booking until cleaned back to Ready.

**Next Module: VIP and Loyalty Tier Priority Room Allocation**

# 6. VIP and Loyalty Tier Priority Room Allocation

## Purpose

Verify VIP guest priority queue, VIP booking, VIP cancellation, and VIP reports.

## Prerequisite

VIP guests and rooms must exist. Use:

- `80000003` Carmen Lim, Platinum
- `80000002` Brandon Lee, Diamond
- `80000005` Emily Chan, Elite

## Demo Flow

1. Main Menu -> `3` VIP Priority Room Allocation.
2. Choose `5` View Priority Queue.
3. Verify higher tier appears before lower tier.
4. Choose `1` Add VIP Guest.
5. Test invalid name/phone first.
6. Add valid VIP guest.
7. Choose `3` Create Booking.
8. Use VIP guest and valid date range.
9. Verify pending VIP booking created.
10. Choose `4` Cancel Booking.
11. Cancel a pending VIP booking if safe for demo.
12. Choose `6` Generate Report.
13. Test VIP Booking Report filter.
14. Test VIP Revenue Summary Report sort/filter.

## Test Cases

| No. | Function | Test Type | Steps / Input | Expected Result | Result |
| --- | --- | --- | --- | --- | --- |
| 1 | View Queue | Positive | Choose `5` | VIP queue ordered by tier | [ ] |
| 2 | Add VIP Guest | Positive | Valid name, phone, tier | VIP guest added | [ ] |
| 3 | Add VIP Guest | Negative | Invalid phone/name | Error message shown | [ ] |
| 4 | Create VIP Booking | Positive | Valid VIP guest + dates | Pending VIP booking saved | [ ] |
| 5 | Cancel VIP Booking | Positive | Pending VIP booking | Status becomes Cancelled | [ ] |
| 6 | VIP Reports | Positive | Apply filter/sort | Correct filtered report shown | [ ] |

## Validation To Check

- Empty VIP guest name.
- Name with numbers.
- Invalid phone.
- Invalid loyalty tier choice.
- Invalid room type.
- Invalid date format.
- Checkout before check-in.
- Cancel non-existing booking.

## Integration Check

- VIP booking is saved in shared booking data.
- VIP priority should be checked before standard booking auto assignment.
- VIP assigned rooms must not clash with existing assigned/checked-in bookings.
- VIP report should match booking/billing records.

**Next Module: Final Front Desk and Reports Check**

# 7. Final Front Desk and Reports Check

## Purpose

Verify all module changes appear correctly across the system.

## Demo Flow

1. Open Front Desk.
2. Search the guest used for booking/check-in.
3. Verify guest current room if still checked in.
4. Open Billing History Report.
5. Confirm new billing appears after check-in.
6. Open Room Availability.
7. Confirm checked-out rooms are not shown as available if Dirty.
8. Return to Main Menu.
9. Exit system.
10. Reopen system.
11. Check that updated data is still saved.

## Final Integration Test Cases

| No. | Integration | Steps | Expected Result | Result |
| --- | --- | --- | --- | --- |
| 1 | Booking -> Front Desk | Add guest, then search in Front Desk | Guest appears | [ ] |
| 2 | Booking -> Rooms | Walk-in booking assigns room | Room becomes Occupied | [ ] |
| 3 | Booking -> Billing | Check in booking | Billing record generated | [ ] |
| 4 | Booking -> Housekeeping | Check out booking | Room becomes Dirty/Vacant | [ ] |
| 5 | Housekeeping -> Booking | Clean room to Ready | Room becomes available for booking | [ ] |
| 6 | VIP -> Booking | Create VIP booking | VIP booking appears in shared booking records | [ ] |
| 7 | Reports -> Data Files | Generate reports after changes | Reports match updated data | [ ] |

# Final Presentation Flow

Use this order during presentation:

1. Main Menu
   - Show invalid input validation.

2. Front Desk
   - View all guests.
   - Search guest `80000008`.
   - View rooms available today.
   - View billing report with Deluxe filter.

3. Housekeeping
   - View current tasks.
   - Update room `101`.
   - Rollback room `101`.
   - Add task for room `104`.
   - Show validation using room `105` or `102`.
   - Show task history and reports.

4. Booking
   - Add guest.
   - Add walk-in booking.
   - Add standard booking.
   - View pending queue.
   - Auto assign room.
   - Check in booking.
   - Check out booking.
   - Show booking reports.

5. Housekeeping Again
   - Show checked-out room is now a housekeeping task.

6. VIP
   - View priority queue.
   - Add VIP guest.
   - Create VIP booking.
   - Show VIP reports.

7. Final Front Desk
   - Search updated guest.
   - View billing.
   - View available rooms.
   - Exit system.

# Final Completion Checklist

- [ ] Application compiles successfully.
- [ ] Main menu navigation works.
- [ ] Invalid menu input does not crash system.
- [ ] Front Desk search works.
- [ ] Front Desk invalid search works.
- [ ] Room availability displays correctly.
- [ ] Billing report shows Standard, Deluxe, and Suite.
- [ ] Housekeeping add task works.
- [ ] Housekeeping update status works.
- [ ] Housekeeping rollback works.
- [ ] Housekeeping late checkout works.
- [ ] Housekeeping reports filter and sort correctly.
- [ ] Booking add guest works.
- [ ] Booking guest validation works.
- [ ] Walk-in booking works.
- [ ] Standard booking queue works.
- [ ] Auto assign works.
- [ ] Check-in works.
- [ ] Billing generated after check-in.
- [ ] Checkout works.
- [ ] Checkout creates housekeeping task.
- [ ] Cancel booking validation works.
- [ ] Booking reports filter correctly.
- [ ] VIP priority queue works.
- [ ] VIP booking works.
- [ ] VIP reports work.
- [ ] Data remains saved after returning to menu or restarting.

# Potential Issues To Verify

## Potential Issue 1: VIP Built-In Collection Usage

Earlier code checks showed VIP may still use Java built-in collections such as `java.util.ArrayList`, `List`, `HashMap`, and `Map`. The assignment Q&A says Java Collections Framework should not be used for collection ADTs.

Expected action: Verify your friend replaced or plans to replace those with custom ADTs.

## Potential Issue 2: Queue Blocking Behaviour

Standard booking uses FIFO. If the first pending booking cannot be assigned, later bookings should wait if your team wants strict chronological queue behaviour.

Expected action: Explain this clearly during presentation as FIFO fairness.

## Potential Issue 3: Destructive Demo Actions

Remove guest and cancel booking can change shared demo data.

Expected action: Only test these if you have backup data or use newly created test records.
