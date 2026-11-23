package com.scc.model;

import java.util.List;

public record Trip(int id, int vehicleId, int driverId, String origin,
                   String destination, String departure, String status) {

    public static final String SCHEDULED = "Scheduled";
    public static final String ONGOING = "Ongoing";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";
    public static final List<String> ALL_STATUSES = List.of(SCHEDULED, ONGOING, COMPLETED, CANCELLED);

    public boolean isScheduled() { return SCHEDULED.equalsIgnoreCase(status); }
    public boolean isOngoing()   { return ONGOING.equalsIgnoreCase(status); }
    public boolean isActive()    { return isScheduled() || isOngoing(); }

    // Returns the canonical status name (e.g. "ongoing" -> "Ongoing") or null if invalid
    public static String normalize(String input) {
        if (input == null) return null;
        for (String s : ALL_STATUSES) if (s.equalsIgnoreCase(input.trim())) return s;
        return null;
    }
}