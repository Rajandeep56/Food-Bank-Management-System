package web;

import java.sql.SQLException;

class RequestErrors {
    static String message(RuntimeException error) {
        Throwable cause = error;
        while (cause != null && !(cause instanceof SQLException)) cause = cause.getCause();
        if (cause == null) return "Could not save this change. Check the Java console for the error location.";
        SQLException sql = (SQLException) cause;
        String state = sql.getSQLState();
        String detail = sql.getMessage() == null ? "" : sql.getMessage();
        if (detail.equals("Invalid distribution status")) return "Only scheduled or delayed pickups can be fulfilled. Reload the distributions.";
        if (detail.equals("Insufficient inventory")) return "Stock is too low, expired, or unavailable. Reload inventory and check the item quantities.";
        if (detail.equals("Invalid hamper request")) return "The hamper request is no longer scheduled. Reload the requests.";
        if ("23505".equals(state)) {
            if (detail.contains("inventory_history_pkey")) {
                return "Inventory history could not be saved because its automatic ID is already in use. The database ID counter needs repair before retrying.";
            }
            return "A record with this ID, name, or email already exists. Refresh and check your entry.";
        }
        if ("23503".equals(state)) return "A related record is missing. Restart Java to reload the database records.";
        if ("23514".equals(state)) return "The database rejected this status or quantity. Check the pickup and inventory values.";
        if ("42P01".equals(state) || "42703".equals(state)) return "The database tables do not match the application. Check the database schema.";
        if ("42501".equals(state)) return "The database user does not have permission to save this change.";
        if (state != null && state.startsWith("08")) return "Could not connect to the database. Check the database connection and try again.";
        if (state != null && state.matches("[A-Z0-9]{5}")) return "Database could not save this change (SQL state " + state + ").";
        return "Database could not save this change. Check the database setup.";
    }
}
