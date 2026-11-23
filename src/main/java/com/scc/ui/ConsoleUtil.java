package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.VehicleDao;
import com.scc.model.Trip;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

// Shared console input + table helpers for the menus.
public class ConsoleUtil {

    private static final DateTimeFormatter DEPARTURE_FMT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner sc;

    public ConsoleUtil(Scanner sc) {
        this.sc = sc;
    }

    public String askText(String label) {
        String v;
        do {
            System.out.print(label + ": ");
            v = sc.nextLine().trim();
            if (v.isEmpty()) System.out.println("  This field is required.");
        } while (v.isEmpty());
        return v;
    }

    public String askOptional(String label, String current) {
        System.out.print(label + " [" + current + "]: ");
        String v = sc.nextLine().trim();
        return v.isEmpty() ? current : v;
    }

    public int askInt(String label) {
        while (true) {
            System.out.print(label + ": ");
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    public int askOptionalInt(String label, int current) {
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

    public boolean confirm(String msg) {
        System.out.print(msg + " (y/n): ");
        return sc.nextLine().trim().equalsIgnoreCase("y");
    }

    // Departure must be "yyyy-MM-dd HH:mm". current == null means the value is required.
    public String askDeparture(String label, String current) {
        while (true) {
            System.out.print(label + " (yyyy-MM-dd HH:mm)" + (current == null ? "" : " [" + current + "]") + ": ");
            String v = sc.nextLine().trim();
            if (v.isEmpty() && current != null) return current;
            String normalized = normalizeDeparture(v);
            if (normalized != null) return normalized;
            System.out.println("  Invalid date/time. Example: 2026-11-25 08:30");
        }
    }

    // Returns the cleaned-up "yyyy-MM-dd HH:mm" text, or null if it is not a real date/time.
    public static String normalizeDeparture(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DEPARTURE_FMT).format(DEPARTURE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Keeps asking until the ID is one of the rows (column 0). Returns 0 if the user cancels.
    public int askIdFrom(String label, List<String[]> rows) {
        while (true) {
            int id = askInt(label + " (0 to cancel)");
            if (id == 0) return 0;
            for (String[] r : rows) if (Integer.parseInt(r[0]) == id) return id;
            System.out.println("  ID " + id + " is not in the list above.");
        }
    }

    // ---------------------------------------------------------------- tables

    // rows: {id, plate, model, capacity, status}
    public static void printVehicleTable(List<String[]> rows) {
        System.out.printf("%-5s %-12s %-18s %-9s %-12s%n", "ID", "PLATE", "MODEL", "CAPACITY", "STATUS");
        for (String[] v : rows) {
            System.out.printf("%-5s %-12s %-18.18s %-9s %-12s%n", v[0], v[1], v[2], v[3], v[4]);
        }
    }

    // rows: {id, name, username, role, status}
    public static void printDriverTable(List<String[]> rows) {
        System.out.printf("%-5s %-24s %-16s%n", "ID", "NAME", "USERNAME");
        for (String[] d : rows) {
            System.out.printf("%-5s %-24.24s %-16.16s%n", d[0], d[1], d[2]);
        }
    }

    // Shows driver names and vehicle plates instead of bare IDs.
    public static void printTripTable(List<Trip> trips, UserDao userDao, VehicleDao vehicleDao) {
        if (trips.isEmpty()) {
            System.out.println("No trips found.");
            return;
        }
        Map<Integer, String> driverNames = new HashMap<>();
        Map<Integer, String> plates = new HashMap<>();
        System.out.printf("%-5s %-12s %-18s %-14s %-14s %-17s %-10s%n",
                "ID", "VEHICLE", "DRIVER", "ORIGIN", "DESTINATION", "DEPARTURE", "STATUS");
        for (Trip t : trips) {
            String driver = driverNames.computeIfAbsent(t.driverId(), id -> {
                String[] u = userDao.findUserRow(id);
                return u == null ? "#" + id : u[1];
            });
            String plate = plates.computeIfAbsent(t.vehicleId(), id -> {
                String[] v = vehicleDao.findVehicle(id);
                return v == null ? "#" + id : v[1];
            });
            System.out.printf("%-5d %-12.12s %-18.18s %-14.14s %-14.14s %-17s %-10s%n",
                    t.id(), plate, driver, t.origin(), t.destination(), t.departure(), t.status());
        }
    }
}