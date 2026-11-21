package database;

import java.sql.*;
import java.time.ZoneId;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {

        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new SQLException("Invalid database URL");
        }

        if (user == null || password == null) {
            throw new SQLException("Missing database credentials");
        }

        String schema = getSchemaName();
        ZoneId zone = ZoneId.of(System.getenv().getOrDefault("AI_TIMEZONE", "America/Vancouver"));
        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            connection.setSchema(schema);
            try (PreparedStatement stmt = connection.prepareStatement("SELECT set_config('TimeZone', ?, false)")) {
                stmt.setString(1, zone.getId());
                stmt.execute();
            }
            return connection;
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    public static String getSchemaName() {
        String defaultSchema = System.getenv().getOrDefault("DB_SCHEMA", "food_bank");
        String schema = System.getProperty("foodbank.schema", defaultSchema);
        if (!schema.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid schema name");
        }
        return schema;
    }

}