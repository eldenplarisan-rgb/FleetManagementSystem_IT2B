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

public class DriverMenu {

    private final Scanner sc;
    private final UserDao userDao;
    private final TripDao tripDao = new TripDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final ActivityLogDao logDao = new ActivityLogDao();
    private final ConsoleUtil io;

    private User current;

    public DriverMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
        this.io = new ConsoleUtil(sc);
    }

    public void display(User driver) {
        this.current = driver;
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  FLEET OPERATIONS, DISPATCH & TRANSIT");
            System.out.println("  SCHEDULING SYSTEM");
            System.out.println("  DRIVER MENU  | Logged in: " + driver.name());
            System.out.println("==========================================");
            System.out.println("  [1] View My Assigned Trips");
            System.out.println("  [2] View My Assigned Vehicle(s)");
            System.out.println("  [3] Update Trip Status (Start / Complete)");
            System.out.println("  [4] View My Trip History");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            switch (sc.nextLine().trim()) {
                case "1": viewAssignedTrips(); break;
                case "2": viewAssignedVehicles(); break;
                case "3": updateTripStatus(); break;
                case "4": viewTripHistory(); break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + driver.name() + "!");
                    logDao.logAction(driver.user_id(), "USER_LOGOUT",
                            "User [" + driver.user_id() + "] " + driver.name() + " logged out");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 4.");
            }
        }
    }

    // Scheduled + Ongoing trips of this driver
    private List<Trip> myActiveTrips() {
        List<Trip> result = new ArrayList<>();
        for (Trip t : tripDao.getTripsByDriver(current.user_id())) if (t.isActive()) result.add(t);
        return result;
    }

    private void viewAssignedTrips() {
        List<Trip> trips = myActiveTrips();
        System.out.println("\n--- MY ASSIGNED TRIPS ---");
        if (trips.isEmpty()) { System.out.println("You have no scheduled or ongoing trips."); return; }
        ConsoleUtil.printTripTable(trips, userDao, vehicleDao);
    }

    private void viewAssignedVehicles() {
        List<Trip> trips = myActiveTrips();
        System.out.println("\n--- MY ASSIGNED VEHICLE(S) ---");
        if (trips.isEmpty()) { System.out.println("No vehicle is assigned to you right now."); return; }

        System.out.printf("%-6s %-5s %-12s %-18s %-9s %-12s%n",
                "TRIP", "ID", "PLATE", "MODEL", "CAPACITY", "STATUS");
        for (Trip t : trips) {
            String[] v = vehicleDao.findVehicle(t.vehicleId());
            if (v == null) {
                System.out.printf("%-6d %-5d (vehicle record not found)%n", t.id(), t.vehicleId());
            } else {
                System.out.printf("%-6d %-5s %-12s %-18.18s %-9s %-12s%n",
                        t.id(), v[0], v[1], v[2], v[3], v[4]);
            }
        }
    }

    // Drivers may only move their OWN trips forward: Scheduled -> Ongoing -> Completed.
    // Cancelling is a dispatcher/admin action.
    private void updateTripStatus() {
        List<Trip> trips = myActiveTrips();
        if (trips.isEmpty()) { System.out.println("You have no scheduled or ongoing trips."); return; }

        System.out.println("\n--- MY ACTIVE TRIPS ---");
        ConsoleUtil.printTripTable(trips, userDao, vehicleDao);

        int id = io.askInt("Trip ID (0 to cancel)");
        if (id == 0) { System.out.println("Cancelled."); return; }

        Trip t = null;
        for (Trip x : trips) if (x.id() == id) { t = x; break; }
        if (t == null) { System.out.println("That trip is not one of your active trips."); return; }

        String newStatus = t.isScheduled() ? Trip.ONGOING : Trip.COMPLETED;
        if (Trip.ONGOING.equals(newStatus)) {
            String problem = tripDao.checkCanStart(t);
            if (problem != null) { System.out.println("Cannot start trip: " + problem); return; }
        }
        String action = Trip.ONGOING.equals(newStatus) ? "Start" : "Complete";
        if (!io.confirm(action + " trip " + id + "?")) { System.out.println("Cancelled."); return; }

        boolean ok = tripDao.updateStatus(id, newStatus);
        System.out.println(ok ? "Trip " + id + " is now " + newStatus + "." : "Update failed.");
        if (ok) logDao.logAction(current.user_id(), "TRIP_STATUS_UPDATED",
                "Driver [" + current.user_id() + "] set trip ID " + id + " from " + t.status() + " to " + newStatus);
    }

    private void viewTripHistory() {
        List<Trip> history = new ArrayList<>();
        int completed = 0;
        for (Trip t : tripDao.getTripsByDriver(current.user_id())) {
            if (!t.isActive()) {
                history.add(t);
                if (Trip.COMPLETED.equalsIgnoreCase(t.status())) completed++;
            }
        }
        System.out.println("\n--- MY TRIP HISTORY ---");
        if (history.isEmpty()) { System.out.println("No completed or cancelled trips yet."); return; }
        ConsoleUtil.printTripTable(history, userDao, vehicleDao);
        System.out.println("Completed: " + completed + " | Cancelled: " + (history.size() - completed));
    }
}