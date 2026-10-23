package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userdao;

    // ===== NEW: yano nga data para sa 2, 3, 4 (in-memory) =====
    private record Vehicle(int id, String plate, String model, int capacity, String status) {}
    private record Driver(int id, String name, String license, String contact, String status) {}
    private record Trip(int id, int vehicleId, int driverId, String origin,
                        String destination, String departure, String status) {}

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Driver> drivers = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private int nextVehicleId = 1, nextDriverId = 1, nextTripId = 1;
    // ==========================================================

    public AdminMenu(Scanner sc, UserDao userdao) {
        this.sc = sc;
        this.userdao = userdao;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  FLEET OPERATIONS, DISPATCH & TRANSIT");
            System.out.println("  SCHEDULING SYSTEM");
            System.out.println("  ADMIN MENU  | Logged in: " + admin.name());
            System.out.println("==========================================");
            System.out.println("  [1] Manage User Accounts");
            System.out.println("  [2] Manage Vehicle Records");
            System.out.println("  [3] Manage Driver Records");
            System.out.println("  [4] Manage Trip Schedules");
            System.out.println("  [5] Fleet Operations Summary Report");
            System.out.println("  [6] System Activity Logs");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    manageUsers();
                    break;
                case "2":
                    manageVehicles();
                    break;
                case "3":
                    manageDrivers();
                    break;
                case "4":
                    manageTrips();
                    break;
                case "5":
                    System.out.println("Fleet Operations Summary Report is not available yet.");
                    break;
                case "6":
                    System.out.println("System Activity Logs is not available yet.");
                    break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + admin.name() + "!");
                    loggedIn = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }


    private void manageUsers() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MANAGE USER ACCOUNTS ---");
            System.out.println("  [1] Create User");
            System.out.println("  [2] Read (View) Users");
            System.out.println("  [3] Update User ");
            System.out.println("  [4] Delete User");
            System.out.println("  [5] APPROVE PENDING USER ");
            System.out.println("  [6] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": createUser(); break;
                case "2": readUsers(); break;
                case "3": updateUser(); break;
                case "4": deleteUser(); break;
                case "5": approvePendingUser(); break;   // FIXED: kaniadto back = true
                case "6": back = true; break;            // FIXED: ang Back kay 6 na
                default: System.out.println("Invalid choice. Please enter a number from 1 to 6.");
            }
        }
    }

    private void createUser() {
        String name = askText("Full name");
        String username = askText("Username");
        String password = askText("Password");
        String role = askText("Role (ADMIN/DISPATCHER/DRIVER)");
        boolean ok = userdao.addUser(name, username, password, role, "APPROVED");
        System.out.println(ok ? "User created." : "Failed to create user.");
    }

    // UPDATED: naay table na
    private void readUsers() {
        List<String[]> rows = userdao.getAllUsers();
        if (rows.isEmpty()) { System.out.println("No users found."); return; }
        printUserTable(rows);
    }

    private void updateUser() {
        int id = askInt("User ID to update");
        String[] u = userdao.findUserRow(id);
        if (u == null) { System.out.println("User not found."); return; }
        System.out.println("(Enter lang para dili usbon)");
        String name = askOptional("Name", u[1]);
        String username = askOptional("Username", u[2]);
        System.out.print("New password [kept]: ");
        String password = sc.nextLine().trim();
        String role = askOptional("Role", u[3]);
        String status = askOptional("Status (APPROVED/PENDING)", u[4]);
        System.out.println(userdao.updateUser(id, name, username, password, role, status)
                ? "User updated." : "Update failed.");
    }

    private void deleteUser() {
        int id = askInt("User ID to delete");
        if (userdao.findUserRow(id) == null) { System.out.println("User not found."); return; }
        if (userdao.isLastAdmin(id)) {
            System.out.println("You cannot delete the last ADMIN account.");
            return;
        }
        if (confirm("Delete user " + id + "?")) {
            System.out.println(userdao.deleteUser(id) ? "User deleted." : "Delete failed.");
        }
    }

    // NEW: APPROVE PENDING USER (PENDING lang ang ipakita)
    private void approvePendingUser() {
        List<String[]> pending = userdao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("No pending users to approve.");
            return;
        }

        System.out.println("\n--- PENDING USERS ---");
        printUserTable(pending);

        int id = askInt("Enter User ID to approve (0 to cancel)");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }

        // i-check kung naa ba gyud sa PENDING list ang gi-input nga ID
        boolean isPending = false;
        for (String[] r : pending) {
            if (Integer.parseInt(r[0]) == id) { isPending = true; break; }
        }
        if (!isPending) {
            System.out.println("That ID is not in the pending list.");
            return;
        }

        if (confirm("Approve user " + id + "?")) {
            System.out.println(userdao.approveUser(id)
                    ? "User approved! They can now log in."
                    : "Approval failed.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // NEW: gamiton sa readUsers() ug approvePendingUser() para pareho ang table
    private void printUserTable(List<String[]> rows) {
        String line = "+" + "-".repeat(7) + "+" + "-".repeat(24) + "+" + "-".repeat(18)
                + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+";
        String fmt = "| %-5s | %-22.22s | %-16.16s | %-12.12s | %-10s |%n";

        System.out.println(line);
        System.out.printf(fmt, "ID", "NAME", "USERNAME", "ROLE", "STATUS");
        System.out.println(line);
        for (String[] r : rows) {
            System.out.printf(fmt, r[0], r[1], r[2], r[3], r[4]);
        }
        System.out.println(line);
    }

    // =====================================================================
    //  [2] MANAGE VEHICLE RECORDS
    // =====================================================================
    private void manageVehicles() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MANAGE VEHICLE RECORDS ---");
            System.out.println("  [1] Create Vehicle");
            System.out.println("  [2] Read (View) Vehicles");
            System.out.println("  [3] Update Vehicle");
            System.out.println("  [4] Delete Vehicle");
            System.out.println("  [0] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": createVehicle(); break;
                case "2": readVehicles(); break;
                case "3": updateVehicle(); break;
                case "4": deleteVehicle(); break;
                case "0": back = true; break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void createVehicle() {
        Vehicle v = new Vehicle(nextVehicleId++,
                askText("Plate number"),
                askText("Model"),
                askInt("Seating capacity"),
                askText("Status (Available/In Service/Maintenance)"));
        vehicles.add(v);
        System.out.println("Vehicle created with ID " + v.id() + ".");
    }

    private void readVehicles() {
        if (vehicles.isEmpty()) { System.out.println("No vehicles on record."); return; }
        System.out.printf("%-5s %-12s %-18s %-9s %-12s%n", "ID", "PLATE", "MODEL", "CAPACITY", "STATUS");
        for (Vehicle v : vehicles) {
            System.out.printf("%-5d %-12s %-18s %-9d %-12s%n",
                    v.id(), v.plate(), v.model(), v.capacity(), v.status());
        }
    }

    private void updateVehicle() {
        int index = findVehicleIndex(askInt("Vehicle ID to update"));
        if (index < 0) { System.out.println("Vehicle not found."); return; }
        Vehicle v = vehicles.get(index);
        System.out.println("(Enter lang para dili usbon)");
        vehicles.set(index, new Vehicle(v.id(),
                askOptional("Plate", v.plate()),
                askOptional("Model", v.model()),
                askOptionalInt("Capacity", v.capacity()),
                askOptional("Status", v.status())));
        System.out.println("Vehicle updated.");
    }

    private void deleteVehicle() {
        int index = findVehicleIndex(askInt("Vehicle ID to delete"));
        if (index < 0) { System.out.println("Vehicle not found."); return; }
        if (confirm("Delete this vehicle?")) {
            vehicles.remove(index);
            System.out.println("Vehicle deleted.");
        }
    }

    private int findVehicleIndex(int id) {
        for (int i = 0; i < vehicles.size(); i++) if (vehicles.get(i).id() == id) return i;
        return -1;
    }

    // =====================================================================
    //  [3] MANAGE DRIVER RECORDS
    // =====================================================================
    private void manageDrivers() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MANAGE DRIVER RECORDS ---");
            System.out.println("  [1] Create Driver");
            System.out.println("  [2] Read (View) Drivers");
            System.out.println("  [3] Update Driver");
            System.out.println("  [4] Delete Driver");
            System.out.println("  [0] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": createDriver(); break;
                case "2": readDrivers(); break;
                case "3": updateDriver(); break;
                case "4": deleteDriver(); break;
                case "0": back = true; break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void createDriver() {
        Driver d = new Driver(nextDriverId++,
                askText("Full name"),
                askText("License number"),
                askText("Contact number"),
                askText("Status (Active/On Leave/Inactive)"));
        drivers.add(d);
        System.out.println("Driver created with ID " + d.id() + ".");
    }

    private void readDrivers() {
        if (drivers.isEmpty()) { System.out.println("No drivers on record."); return; }
        System.out.printf("%-5s %-22s %-16s %-14s %-10s%n", "ID", "NAME", "LICENSE", "CONTACT", "STATUS");
        for (Driver d : drivers) {
            System.out.printf("%-5d %-22s %-16s %-14s %-10s%n",
                    d.id(), d.name(), d.license(), d.contact(), d.status());
        }
    }

    private void updateDriver() {
        int index = findDriverIndex(askInt("Driver ID to update"));
        if (index < 0) { System.out.println("Driver not found."); return; }
        Driver d = drivers.get(index);
        System.out.println("(Enter lang para dili usbon)");
        drivers.set(index, new Driver(d.id(),
                askOptional("Name", d.name()),
                askOptional("License", d.license()),
                askOptional("Contact", d.contact()),
                askOptional("Status", d.status())));
        System.out.println("Driver updated.");
    }

    private void deleteDriver() {
        int index = findDriverIndex(askInt("Driver ID to delete"));
        if (index < 0) { System.out.println("Driver not found."); return; }
        if (confirm("Delete this driver?")) {
            drivers.remove(index);
            System.out.println("Driver deleted.");
        }
    }

    private int findDriverIndex(int id) {
        for (int i = 0; i < drivers.size(); i++) if (drivers.get(i).id() == id) return i;
        return -1;
    }

    // =====================================================================
    //  [4] MANAGE TRIP SCHEDULES
    // =====================================================================
    private void manageTrips() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MANAGE TRIP SCHEDULES ---");
            System.out.println("  [1] Create Trip");
            System.out.println("  [2] Read (View) Trips");
            System.out.println("  [3] Update Trip");
            System.out.println("  [4] Delete Trip");
            System.out.println("  [0] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": createTrip(); break;
                case "2": readTrips(); break;
                case "3": updateTrip(); break;
                case "4": deleteTrip(); break;
                case "0": back = true; break;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void createTrip() {
        if (vehicles.isEmpty() || drivers.isEmpty()) {
            System.out.println("Add at least one vehicle and one driver first.");
            return;
        }
        int vehicleId = askExistingVehicle("Vehicle ID", null);
        int driverId = askExistingDriver("Driver ID", null);
        Trip t = new Trip(nextTripId++, vehicleId, driverId,
                askText("Origin"),
                askText("Destination"),
                askText("Departure (e.g. 2026-11-05 08:30)"),
                "Scheduled");
        trips.add(t);
        System.out.println("Trip created with ID " + t.id() + ".");
    }

    private void readTrips() {
        if (trips.isEmpty()) { System.out.println("No trips scheduled."); return; }
        System.out.printf("%-5s %-8s %-8s %-14s %-14s %-17s %-10s%n",
                "ID", "VEHICLE", "DRIVER", "ORIGIN", "DESTINATION", "DEPARTURE", "STATUS");
        for (Trip t : trips) {
            System.out.printf("%-5d %-8d %-8d %-14s %-14s %-17s %-10s%n",
                    t.id(), t.vehicleId(), t.driverId(), t.origin(),
                    t.destination(), t.departure(), t.status());
        }
    }

    private void updateTrip() {
        int index = findTripIndex(askInt("Trip ID to update"));
        if (index < 0) { System.out.println("Trip not found."); return; }
        Trip t = trips.get(index);
        System.out.println("(Enter lang para dili usbon)");
        trips.set(index, new Trip(t.id(),
                askExistingVehicle("Vehicle ID", t.vehicleId()),
                askExistingDriver("Driver ID", t.driverId()),
                askOptional("Origin", t.origin()),
                askOptional("Destination", t.destination()),
                askOptional("Departure", t.departure()),
                askOptional("Status (Scheduled/Ongoing/Completed/Cancelled)", t.status())));
        System.out.println("Trip updated.");
    }

    private void deleteTrip() {
        int index = findTripIndex(askInt("Trip ID to delete"));
        if (index < 0) { System.out.println("Trip not found."); return; }
        if (confirm("Delete this trip?")) {
            trips.remove(index);
            System.out.println("Trip deleted.");
        }
    }

    private int findTripIndex(int id) {
        for (int i = 0; i < trips.size(); i++) if (trips.get(i).id() == id) return i;
        return -1;
    }

    private int askExistingVehicle(String label, Integer current) {
        while (true) {
            int id = (current == null) ? askInt(label) : askOptionalInt(label, current);
            if (findVehicleIndex(id) >= 0) return id;
            System.out.println("  Vehicle ID " + id + " does not exist.");
        }
    }

    private int askExistingDriver(String label, Integer current) {
        while (true) {
            int id = (current == null) ? askInt(label) : askOptionalInt(label, current);
            if (findDriverIndex(id) >= 0) return id;
            System.out.println("  Driver ID " + id + " does not exist.");
        }
    }

    // =====================================================================
    //  INPUT HELPERS
    // =====================================================================
    private String askText(String label) {
        String v;
        do {
            System.out.print(label + ": ");
            v = sc.nextLine().trim();
            if (v.isEmpty()) System.out.println("  This field is required.");
        } while (v.isEmpty());
        return v;
    }

    private String askOptional(String label, String current) {
        System.out.print(label + " [" + current + "]: ");
        String v = sc.nextLine().trim();
        return v.isEmpty() ? current : v;
    }

    private int askInt(String label) {
        while (true) {
            System.out.print(label + ": ");
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    private int askOptionalInt(String label, int current) {
        while (true) {
            System.out.print(label + " [" + current + "]: ");
            String v = sc.nextLine().trim();
            if (v.isEmpty()) return current;
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    private boolean confirm(String msg) {
        System.out.print(msg + " (y/n): ");
        return sc.nextLine().trim().equalsIgnoreCase("y");
    }
}