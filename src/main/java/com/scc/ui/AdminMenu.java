package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userdao;
    private final ActivityLogDao logDao = new ActivityLogDao();   // NEW: audit trail

    // NEW: the admin who is currently logged in + valid values for validation
    private User currentAdmin;
    private static final List<String> VALID_ROLES = List.of("ADMIN", "DISPATCHER", "DRIVER");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

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
        this.currentAdmin = admin;   // NEW
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
            System.out.println("  [6] View System Audit Logs");
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
                    viewAuditLogs();
                    break;
                case "0":
                    System.out.println("Logging out... Goodbye, " + admin.name() + "!");
                    audit("USER_LOGOUT", "User [" + admin.user_id() + "] " + admin.name() + " logged out");   // NEW LOG
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
            System.out.println("  [6] Update User Role & Status (Restricted)");   // NEW
            System.out.println("  [7] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": createUser(); break;
                case "2": readUsers(); break;
                case "3": updateUser(); break;
                case "4": deleteUser(); break;
                case "5": approvePendingUser(); break;
                case "6": updateUserRoleAndStatus(); break;   // NEW
                case "7": back = true; break;                 // Back na ang katapusan
                default: System.out.println("Invalid choice. Please enter a number from 1 to 7.");
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
        if (ok) audit("USER_CREATED", "Admin [" + currentAdmin.user_id() + "] created account for username " + username);
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
        boolean updated = userdao.updateUser(id, name, username, password, role, status);
        System.out.println(updated ? "User updated." : "Update failed.");
        if (updated) audit("USER_UPDATED", "Admin [" + currentAdmin.user_id() + "] updated profile of User " + id);
    }

    private void deleteUser() {
        int id = askInt("User ID to delete");
        if (userdao.findUserRow(id) == null) { System.out.println("User not found."); return; }
        if (userdao.isLastAdmin(id)) {
            System.out.println("You cannot delete the last ADMIN account.");
            return;
        }
        if (confirm("Delete user " + id + "?")) {
            boolean deleted = userdao.deleteUser(id);
            System.out.println(deleted ? "User deleted." : "Delete failed.");
            if (deleted) audit("USER_DELETED", "Admin [" + currentAdmin.user_id() + "] deleted User ID " + id);
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
            boolean approved = userdao.approveUser(id);
            System.out.println(approved
                    ? "User approved! They can now log in."
                    : "Approval failed.");
            if (approved) audit("USER_APPROVED", "Admin [" + currentAdmin.user_id() + "] approved account for User ID " + id);
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
    //  NEW: UPDATE USER ROLE & STATUS (with validation)
    // =====================================================================
    private void updateUserRoleAndStatus() {
        System.out.println("\n--- UPDATE USER ROLE & STATUS ---");

        List<String[]> rows = userdao.getAllUsers();
        if (rows.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        printUserTable(rows);

        int targetUserId = readIntInput("Enter target User ID (0 to cancel)");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        // Self-edit restriction
        if (targetUserId == currentAdmin.user_id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        if (userdao.findUserRow(targetUserId) == null) {
            System.out.println("User not found.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        boolean ok = userdao.updateUserRoleAndStatus(targetUserId, newRole, newStatus);
        System.out.println(ok
                ? "SUCCESS: User " + targetUserId + " updated to role " + newRole
                + " and status " + newStatus + "."
                : "Update failed.");
        if (ok) audit("USER_UPDATED", "Admin [" + currentAdmin.user_id() + "] changed role of User "
                + targetUserId + " to " + newRole + " and status to " + newStatus);
    }

    // Returns the number typed. Re-prompts on letters/symbols instead of crashing.
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input! Please enter numbers only.");
            }
        }
    }

    // Keeps asking until a valid role is typed. Returns null if the admin enters 0.
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + VALID_ROLES + " (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_ROLES.contains(input)) return input;
            System.out.println("  Invalid role! Choose from: " + VALID_ROLES);
        }
    }

    // Keeps asking until a valid status is typed. Returns null if the admin enters 0.
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + VALID_STATUSES + " (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();
            if (input.equals("0")) return null;
            if (VALID_STATUSES.contains(input)) return input;
            System.out.println("  Invalid status! Choose from: " + VALID_STATUSES);
        }
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
        audit("VEHICLE_CREATED", "Admin [" + currentAdmin.user_id() + "] added vehicle ID " + v.id() + " (" + v.plate() + ")");
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
        audit("VEHICLE_UPDATED", "Admin [" + currentAdmin.user_id() + "] updated vehicle ID " + v.id());
    }

    private void deleteVehicle() {
        int index = findVehicleIndex(askInt("Vehicle ID to delete"));
        if (index < 0) { System.out.println("Vehicle not found."); return; }
        if (confirm("Delete this vehicle?")) {
            Vehicle removed = vehicles.remove(index);
            System.out.println("Vehicle deleted.");
            audit("VEHICLE_DELETED", "Admin [" + currentAdmin.user_id() + "] deleted vehicle ID " + removed.id());
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
        audit("DRIVER_CREATED", "Admin [" + currentAdmin.user_id() + "] added driver ID " + d.id() + " (" + d.name() + ")");
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
        audit("DRIVER_UPDATED", "Admin [" + currentAdmin.user_id() + "] updated driver ID " + d.id());
    }

    private void deleteDriver() {
        int index = findDriverIndex(askInt("Driver ID to delete"));
        if (index < 0) { System.out.println("Driver not found."); return; }
        if (confirm("Delete this driver?")) {
            Driver removed = drivers.remove(index);
            System.out.println("Driver deleted.");
            audit("DRIVER_DELETED", "Admin [" + currentAdmin.user_id() + "] deleted driver ID " + removed.id());
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
        audit("TRIP_CREATED", "Admin [" + currentAdmin.user_id() + "] scheduled trip ID " + t.id()
                + " (" + t.origin() + " to " + t.destination() + ")");
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
        audit("TRIP_UPDATED", "Admin [" + currentAdmin.user_id() + "] updated trip ID " + t.id());
    }

    private void deleteTrip() {
        int index = findTripIndex(askInt("Trip ID to delete"));
        if (index < 0) { System.out.println("Trip not found."); return; }
        if (confirm("Delete this trip?")) {
            Trip removed = trips.remove(index);
            System.out.println("Trip deleted.");
            audit("TRIP_DELETED", "Admin [" + currentAdmin.user_id() + "] deleted trip ID " + removed.id());
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
    //  NEW: [6] SYSTEM AUDIT LOGS
    // =====================================================================

    // Shortcut: record an action done by the currently logged-in admin
    private void audit(String actionType, String description) {
        logDao.logAction(currentAdmin.user_id(), actionType, description);
    }

    private void viewAuditLogs() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("  [1] View All System Logs");
            System.out.println("  [2] Filter Logs by User ID");
            System.out.println("  [3] Clear All Logs");
            System.out.println("  [0] Back");
            System.out.print("Enter choice: ");
            switch (sc.nextLine().trim()) {
                case "1": printLogTable(logDao.getAllLogs()); break;
                case "2": filterLogsByUser(); break;
                case "3": clearAuditLogs(); break;
                case "0": back = true; break;
                default: System.out.println("Invalid choice. Please enter a number from 0 to 3.");
            }
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel)");
        if (userId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        printLogTable(logDao.getLogsByUserId(userId));
    }

    private void clearAuditLogs() {
        if (!confirm("This permanently deletes ALL historical logs. Continue?")) {
            System.out.println("Cancelled.");
            return;
        }
        if (logDao.clearLogs()) {
            System.out.println("All logs cleared.");
            // keep a trace of who cleared the logs
            audit("LOGS_CLEARED", "Admin [" + currentAdmin.user_id() + "] cleared all historical logs");
        } else {
            System.out.println("Failed to clear logs.");
        }
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }
        String fmt = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        String line = "-".repeat(5 + 3 + 8 + 3 + 18 + 3 + 35 + 3 + 20);

        System.out.println(line);
        System.out.printf(fmt, "ID", "USER ID", "ACTION", "DESCRIPTION", "TIMESTAMP");
        System.out.println(line);
        for (ActivityLog l : logs) {
            String desc = (l.description() == null) ? "" : l.description();
            String first = desc.length() > 35 ? desc.substring(0, 35) : desc;
            System.out.printf(fmt, l.logId(), l.userId(), l.actionType(), first, l.timestamp());
            // long descriptions continue on the next line(s) so the columns stay aligned
            for (int i = 35; i < desc.length(); i += 35) {
                System.out.printf(fmt, "", "", "", desc.substring(i, Math.min(i + 35, desc.length())), "");
            }
        }
        System.out.println(line);
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