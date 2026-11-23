package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.TripDao;
import com.scc.dao.UserDao;
import com.scc.dao.VehicleDao;
import com.scc.model.Trip;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DispatcherMenu {

    private final Scanner sc;
    private final UserDao userDao;
    private final TripDao tripDao = new TripDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final ConsoleUtil io;

    private User current;

    public DispatcherMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
        this.io = new ConsoleUtil(sc);
    }

    public void display(User dispatcher) {
        this.current = dispatcher;
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  FLEET OPERATIONS, DISPATCH & TRANSIT");
            System.out.println("  SCHEDULING SYSTEM");
            System.out.println("  DISPATCHER MENU  | Logged in: " + dispatcher.name());
            System.out.println("==========================================");
            System.out.println("  [1] View Available Vehicles");
            System.out.println("  [2] View Available Drivers");
            System.out.println("  [3] Create Trip (Assign Vehicle & Driver)");
            System.out.println("  [4] Manage Trip Assignments (Edit / Reassign)");
            System.out.println("  [5] Update Trip Status");
            System.out.println("  [6] View Dispatch Records");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            switch (sc.nextLine().trim()) {
                case "1": viewAvailableVehicles(); break;
                case "2": viewAvailableDrivers(); break;
                case "3": createTrip(); break;
                case "4": manageAssignments(); break;
                case "5": updateTripStatus(); break;
                case "6": viewDispatchRecords(); break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + dispatcher.name() + "!");
                    audit("USER_LOGOUT", "User [" + dispatcher.user_id() + "] " + dispatcher.name() + " logged out");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }

    // =====================================================================
    //  [1] / [2] AVAILABLE VEHICLES AND DRIVERS
    // =====================================================================
    private List<String[]> availableVehicles() {
        List<String[]> rows = new ArrayList<>();
        for (String[] v : vehicleDao.getAllVehicles()) {
            if (v[4].equalsIgnoreCase("Available")) rows.add(v);
        }
        return rows;
    }

    // Approved drivers who are not on an ONGOING trip right now
    private List<String[]> availableDrivers() {
        List<String[]> rows = new ArrayList<>();
        for (String[] d : userDao.getApprovedDrivers()) {
            if (!tripDao.hasOngoingTrip(Integer.parseInt(d[0]))) rows.add(d);
        }
        return rows;
    }

    private void viewAvailableVehicles() {
        List<String[]> rows = availableVehicles();
        System.out.println("\n--- AVAILABLE VEHICLES ---");
        if (rows.isEmpty()) { System.out.println("No available vehicles."); return; }
        ConsoleUtil.printVehicleTable(rows);
    }

    private void viewAvailableDrivers() {
        List<String[]> rows = availableDrivers();
        System.out.println("\n--- AVAILABLE DRIVERS ---");
        if (rows.isEmpty()) { System.out.println("No available drivers."); return; }
        ConsoleUtil.printDriverTable(rows);
    }

    // =====================================================================
    //  [3] CREATE TRIP
    // =====================================================================
    private void createTrip() {
        List<String[]> vehicles = availableVehicles();
        List<String[]> drivers = availableDrivers();
        if (vehicles.isEmpty()) { System.out.println("No available vehicles. Ask the Admin to add or free one."); return; }
        if (drivers.isEmpty())  { System.out.println("No available drivers. Ask the Admin to approve a DRIVER account."); return; }

        System.out.println("\n--- AVAILABLE VEHICLES ---");
        ConsoleUtil.printVehicleTable(vehicles);
        System.out.println("\n--- AVAILABLE DRIVERS ---");
        ConsoleUtil.printDriverTable(drivers);
        System.out.println();

        int vehicleId = io.askIdFrom("Vehicle ID", vehicles);
        if (vehicleId == 0) { System.out.println("Cancelled."); return; }
        int driverId = io.askIdFrom("Driver ID", drivers);
        if (driverId == 0) { System.out.println("Cancelled."); return; }

        String origin = io.askText("Origin");
        String destination = io.askText("Destination");
        String departure = io.askDeparture("Departure", null);

        int conflict = tripDao.findConflict(vehicleId, driverId, departure, -1);
        if (conflict > 0) {
            System.out.println("Scheduling conflict: that vehicle or driver is already assigned to Trip "
                    + conflict + " at " + departure + ".");
            return;
        }

        int id = tripDao.addTrip(vehicleId, driverId, origin, destination, departure, Trip.SCHEDULED, current.user_id());
        if (id < 0) { System.out.println("Failed to create trip."); return; }

        System.out.println("Trip created with ID " + id + ".");
        audit("TRIP_CREATED", "Dispatcher [" + current.user_id() + "] created trip ID " + id
                + " (" + origin + " to " + destination + ")");
        audit("TRIP_ASSIGNED", "Dispatcher [" + current.user_id() + "] assigned vehicle " + vehicleId
                + " and driver " + driverId + " to trip ID " + id);
    }

    // =====================================================================
    //  [4] MANAGE TRIP ASSIGNMENTS (only SCHEDULED trips can be edited)
    // =====================================================================
    private void manageAssignments() {
        List<Trip> scheduled = tripDao.getTripsByStatus(Trip.SCHEDULED);
        if (scheduled.isEmpty()) { System.out.println("No scheduled trips to manage."); return; }

        System.out.println("\n--- SCHEDULED TRIPS ---");
        ConsoleUtil.printTripTable(scheduled, userDao, vehicleDao);

        int id = io.askInt("Trip ID to edit (0 to cancel)");
        if (id == 0) { System.out.println("Cancelled."); return; }
        Trip t = tripDao.findById(id);
        if (t == null) { System.out.println("Trip not found."); return; }
        if (!t.isScheduled()) { System.out.println("Only SCHEDULED trips can be edited."); return; }

        // The trip's own vehicle/driver stay selectable even if they are not "available"
        List<String[]> vehicleChoices = availableVehicles();
        String[] ownVehicle = vehicleDao.findVehicle(t.vehicleId());
        if (ownVehicle != null && vehicleChoices.stream().noneMatch(v -> v[0].equals(ownVehicle[0]))) {
            vehicleChoices.add(ownVehicle);
        }
        List<String[]> driverChoices = availableDrivers();
        String[] ownDriver = userDao.findUserRow(t.driverId());
        if (ownDriver != null && driverChoices.stream().noneMatch(d -> d[0].equals(ownDriver[0]))) {
            driverChoices.add(ownDriver);
        }

        System.out.println("\n--- VEHICLES YOU CAN ASSIGN ---");
        ConsoleUtil.printVehicleTable(vehicleChoices);
        System.out.println("\n--- DRIVERS YOU CAN ASSIGN ---");
        ConsoleUtil.printDriverTable(driverChoices);
        System.out.println("\n(Press Enter to keep the current value)");

        int vehicleId = askKeepOrPick("Vehicle ID", t.vehicleId(), vehicleChoices);
        int driverId = askKeepOrPick("Driver ID", t.driverId(), driverChoices);
        String origin = io.askOptional("Origin", t.origin());
        String destination = io.askOptional("Destination", t.destination());
        String departure = io.askDeparture("Departure", t.departure());

        int conflict = tripDao.findConflict(vehicleId, driverId, departure, t.id());
        if (conflict > 0) {
            System.out.println("Scheduling conflict: that vehicle or driver is already assigned to Trip "
                    + conflict + " at " + departure + ".");
            return;
        }

        boolean ok = tripDao.updateTrip(new Trip(t.id(), vehicleId, driverId, origin, destination, departure, t.status()));
        System.out.println(ok ? "Trip updated." : "Update failed.");
        if (!ok) return;

        audit("TRIP_UPDATED", "Dispatcher [" + current.user_id() + "] updated trip ID " + t.id());
        if (vehicleId != t.vehicleId() || driverId != t.driverId()) {
            audit("TRIP_ASSIGNED", "Dispatcher [" + current.user_id() + "] reassigned trip ID " + t.id()
                    + " to vehicle " + vehicleId + " and driver " + driverId);
        }
    }

    private int askKeepOrPick(String label, int currentId, List<String[]> choices) {
        while (true) {
            int id = io.askOptionalInt(label, currentId);
            for (String[] r : choices) if (Integer.parseInt(r[0]) == id) return id;
            System.out.println("  ID " + id + " is not in the list above.");
        }
    }

    // =====================================================================
    //  [5] UPDATE TRIP STATUS
    //  Scheduled -> Ongoing | Cancelled      Ongoing -> Completed | Cancelled
    // =====================================================================
    private void updateTripStatus() {
        List<Trip> active = new ArrayList<>();
        for (Trip t : tripDao.getAllTrips()) if (t.isActive()) active.add(t);
        if (active.isEmpty()) { System.out.println("No scheduled or ongoing trips."); return; }

        System.out.println("\n--- ACTIVE TRIPS ---");
        ConsoleUtil.printTripTable(active, userDao, vehicleDao);

        int id = io.askInt("Trip ID (0 to cancel)");
        if (id == 0) { System.out.println("Cancelled."); return; }
        Trip t = tripDao.findById(id);
        if (t == null) { System.out.println("Trip not found."); return; }
        if (!t.isActive()) { System.out.println("Trip " + id + " is already " + t.status() + " and can't be changed."); return; }

        String first = t.isScheduled() ? Trip.ONGOING : Trip.COMPLETED;
        System.out.println("Trip " + id + " is currently " + t.status() + ". Change it to:");
        System.out.println("  [1] " + first);
        System.out.println("  [2] " + Trip.CANCELLED);
        System.out.println("  [0] Back");
        System.out.print("Enter choice: ");

        String newStatus;
        switch (sc.nextLine().trim()) {
            case "1": newStatus = first; break;
            case "2": newStatus = Trip.CANCELLED; break;
            default: System.out.println("Cancelled."); return;
        }

        if (Trip.ONGOING.equals(newStatus)) {
            String problem = tripDao.checkCanStart(t);
            if (problem != null) { System.out.println("Cannot start trip: " + problem); return; }
        }
        if (!io.confirm("Set trip " + id + " to " + newStatus + "?")) { System.out.println("Cancelled."); return; }

        boolean ok = tripDao.updateStatus(id, newStatus);
        System.out.println(ok ? "Trip status updated to " + newStatus + "." : "Update failed.");
        if (ok) audit("TRIP_STATUS_UPDATED", "Dispatcher [" + current.user_id() + "] set trip ID " + id
                + " from " + t.status() + " to " + newStatus);
    }

    // =====================================================================
    //  [6] VIEW DISPATCH RECORDS
    // =====================================================================
    private void viewDispatchRecords() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- DISPATCH RECORDS ---");
            System.out.println("  [1] All Trips");
            System.out.println("  [2] Filter by Status");
            System.out.println("  [0] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1":
                    ConsoleUtil.printTripTable(tripDao.getAllTrips(), userDao, vehicleDao);
                    break;
                case "2":
                    System.out.print("Status " + Trip.ALL_STATUSES + ": ");
                    String status = Trip.normalize(sc.nextLine());
                    if (status == null) System.out.println("Invalid status.");
                    else ConsoleUtil.printTripTable(tripDao.getTripsByStatus(status), userDao, vehicleDao);
                    break;
                case "0": back = true; break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void audit(String actionType, String description) {
        logDao.logAction(current.user_id(), actionType, description);
    }
}