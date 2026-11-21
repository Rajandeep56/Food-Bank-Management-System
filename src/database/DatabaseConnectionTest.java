package database;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    public static void main(String[] args) {

        try (Connection connection =
                 DatabaseConnection.getConnection()) {

            System.out.println(
                "Database connected successfully!"
            );

        } catch (SQLException e) {

            System.out.println(
                "Connection failed: " + e.getMessage()
            );
            System.exit(1);
        }
    }
}