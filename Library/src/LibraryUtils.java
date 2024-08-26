import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.*;

public class LibraryUtils {

    private static final String URL = "jdbc:postgresql://db.ecs.vuw.ac.nz:5432/wattjess2_jdbc";

    private LibraryUtils() {
    }

    // Set up connection to database
    public static Connection setupDatabaseConnection(JFrame parent, String userid, String password) {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, userid, password);
        } catch (ClassNotFoundException e) {
            handleClassNotFoundException(parent, e, userid);
            return null;
        } catch (SQLException e) {
            handleLoginException(parent, e, userid);
            return null;
        }
    }

    // Handle ClassNotFoundException
    private static void handleClassNotFoundException(JFrame parent, ClassNotFoundException e, String userid) {
        showErrorMessage(parent, e.getMessage(), userid);
        System.exit(1);
    }

    // Handle incorrect username or password
    private static void handleLoginException(JFrame parent, SQLException e, String userid) {
        if (e.getMessage().contains("password authentication failed")) {
            showErrorMessage(parent,
                    "SQLException:\nFATAL: password authentication failed for user \"" + userid + "\"",
                    "Login");
        } else if (e.getMessage().contains("GSS Authentication failed")) {
            showErrorMessage(parent, "SQLException:\nGSS Authentication failed", "Login");
        } else {
            showErrorMessage(parent, e.getMessage(), userid);
        }
        System.exit(1);
    }

    // Display error message on popup window
    public static void showErrorMessage(JFrame parent, String message, String title) {
        String errorTitle = title + " Error";
        JOptionPane.showMessageDialog(parent, message, errorTitle, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    // Close connection to database
    public static void closeDBConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed successfully.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // Revert disabling of auto commit used in modification and deletion operations
    public static void restoreAutoCommit(JFrame parent, Connection conn, String processTitle) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            showErrorMessage(parent, e.getMessage(), processTitle);
            System.exit(1);
        }
    }

    // Handle rollback of modification and deletion operations
    public static void handleRollback(JFrame parent, Connection conn, String message, String processTitle) {
        try {
            conn.rollback(); // Rollback transaction on error
        } catch (SQLException rollbackE) {
            showErrorMessage(parent, rollbackE.getMessage(), processTitle);
            System.exit(1);
        }
        showErrorMessage(parent, message, processTitle);
        System.exit(1);
    }
}
