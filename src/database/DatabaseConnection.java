package database;

import java.sql.*;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {

        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        System.out.println("Database URL: " + url);
        System.out.println("Database User: " + user);

        if (url == null || !url.startsWith("jdbc:postgresql://")) {
            throw new SQLException("Invalid database URL");
        }

        if (user == null || password == null) {
            throw new SQLException("Missing database credentials");
        }

        Connection connection = DriverManager.getConnection(
            url, user, password
        );

        connection.setSchema("food_bank");

        System.out.println("Connected schema: " + connection.getSchema());

        return connection;
    }
}