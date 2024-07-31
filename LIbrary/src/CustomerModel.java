import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JFrame;

public class CustomerModel {
    private final JFrame parent;
    private final Connection conn;

    public CustomerModel(JFrame parent, Connection conn) {
        this.parent = parent;
        this.conn = conn;
    }

    // Get customer by ID
    public String showCustomer(int customerID) {
        StringBuilder output = new StringBuilder("Show Customer:\n");

        // Check if requested customer is default entry
        if (customerID == 0) {
            appendDefaultCustomer(output);
            return output.toString();
        }

        String customerQuery = "SELECT c.CustomerID, c.L_Name, c.F_Name, c.City, b.ISBN, b.Title FROM Customer c LEFT JOIN Cust_Book cb ON c.CustomerID = cb.CustomerID LEFT JOIN Book b ON cb.ISBN = b.ISBN WHERE c.CustomerID = ? ORDER BY b.ISBN";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(customerQuery)) {
                stmt.setInt(1, customerID);
                ResultSet rs = stmt.executeQuery();

                processCustomerData(rs, output, customerID);
            }
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Show Customer");
        }

        return output.toString();
    }

    // Append default customer information to output
    private void appendDefaultCustomer(StringBuilder output) {
        output.append("\t0: Default, Customer - (no city)\n\t(No books borrowed)");
    }

    // Process data to extract customer information
    private void processCustomerData(ResultSet rs, StringBuilder output, int customerID) throws SQLException {

        // Initialise borrowed books summary
        StringBuilder books = new StringBuilder();

        // Initialise borrowed books counter variables
        boolean customerFound = false;
        int bookCount = 0;

        // Iterate through data entries
        while (rs.next()) {

            // Append new customer information
            if (!customerFound) {
                appendCustomerInfo(rs, output, customerID);
                customerFound = true;
            }

            // Append new borrowed book information
            bookCount += appendBookInfo(rs, books);
        }

        // Finalise customer information with complete borrowed book summary
        appendBookSummary(output, customerFound, bookCount, books, customerID);
    }

    // Append customer information to output
    private void appendCustomerInfo(ResultSet rs, StringBuilder output, int customerID)
            throws SQLException {
        String firstName = rs.getString("F_Name");
        String lastName = rs.getString("L_Name");
        String city = rs.getString("City");
        if (city == null) {
            city = "(no city)";
        }
        String customerFullName = lastName.trim() + ", " + firstName.trim();
        output.append("\t").append(customerID).append(": ").append(customerFullName)
                .append(" - ").append(city.trim());
    }

    // Append book information to books StringBuilder
    private int appendBookInfo(ResultSet rs, StringBuilder books) throws SQLException {
        int bookCount = 0;
        int isbn = rs.getInt("ISBN");
        String title = rs.getString("Title");
        if (title != null) {
            books.append("\n\t\t").append(isbn).append(" - ").append(title.trim());
            bookCount++;
        }
        return bookCount;
    }

    // Finalise output with books StringBuilder
    private void appendBookSummary(StringBuilder output, boolean customerFound, int bookCount,
            StringBuilder books, int customerID) {
        if (!customerFound) {
            output.append("\tNo such customer ID: ").append(customerID);
        } else if (bookCount > 0) {
            output.append(bookCount == 1 ? "\n\tBook Borrowed:" : "\n\tBooks Borrowed:").append(books);
        } else {
            output.append("\n\t(No books borrowed)");
        }
    }

    // Get all customers
    public String showAllCustomers() {
        StringBuilder output = new StringBuilder("Show All Customers:");
        String customerQuery = "SELECT CustomerID, L_Name, F_Name, City FROM Customer ORDER BY CustomerID";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(customerQuery)) {
                ResultSet rs = stmt.executeQuery();
                processCustomersData(rs, output);
            }
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Show All Customers");
        }

        return output.toString();
    }

    // Process data to get information about all customers
    private void processCustomersData(ResultSet rs, StringBuilder output) throws SQLException {
        while (rs.next()) {
            processCustomerEntry(rs, output);
        }
    }

    // Process a single customer
    private void processCustomerEntry(ResultSet rs, StringBuilder output) throws SQLException {
        int customerID = rs.getInt("CustomerID");
        String lastName = rs.getString("L_Name");
        String firstName = rs.getString("F_Name");
        String city = rs.getString("City");
        if (city == null) {
            city = "(no city)";
        }

        output.append("\n\t").append(customerID).append(": ")
                .append(lastName.trim()).append(", ").append(firstName.trim())
                .append(" - ").append(city);
    }

    // Delete customer by ID
    public String deleteCus(int customerID) {
        StringBuilder output = new StringBuilder("Delete Customer:\n");

        // Check if requested customer is default entry
        if (customerID == 0) {
            output.append("\tCannot delete default customer entry");
            return output.toString();
        }

        String deleteQuery = "DELETE FROM Customer WHERE CustomerID = ?";

        // Begin transaction and disable auto commit
        try {
            conn.setAutoCommit(false);

            // Check if customer has loaned books
            int numBorrowed = numBorrowedBooks(customerID);
            if (numBorrowed > 0) {
                conn.rollback();
                String message;
                if (numBorrowed == 1) {
                    message = "\tCannot delete customer with ID " + customerID
                            + " because they currently have a book loaned out";
                } else {
                    message = "\tCannot delete customer with ID " + customerID
                            + " because they currently have " + numBorrowed + " books loaned out";
                }
                output.append(message);
                return output.toString();
            }

            // Do deletion
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setInt(1, customerID);
                int rowsAffected = stmt.executeUpdate();
                handleDeletionOutput(output, customerID, rowsAffected);

                // Complete transaction
                conn.commit();
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
                output.append("\tCannot delete customer with ID ").append(customerID)
                        .append(" because they have just borrowed a book");
            } catch (SQLException ex) {
                LibraryUtils.showErrorMessage(parent, ex.getMessage(), "Delete Book");
            }
        }

        // Enable auto commit
        finally {
            LibraryUtils.restoreAutoCommit(parent, conn, "Delete Customer");
        }
        return output.toString();
    }

    // Check if customer has loaned books
    private int numBorrowedBooks(int customerID) throws SQLException {
        String checkCustBookQuery = "SELECT COUNT(*) AS Count FROM Cust_Book WHERE CustomerID = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkCustBookQuery)) {
            checkStmt.setInt(1, customerID);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            return rs.getInt("Count");
        }
    }

    // Handle output of deletion
    private void handleDeletionOutput(StringBuilder output, int customerID, int rowsAffected) {
        if (rowsAffected > 0) {
            output.append("\tCustomer with ID ").append(customerID).append(" deleted");
        } else {
            output.append("\tNo such customer ID: ").append(customerID);
        }
    }

}
