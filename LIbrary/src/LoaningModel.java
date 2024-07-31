import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LoaningModel {
    private final JFrame parent;
    private final Connection conn;

    public LoaningModel(JFrame parent, Connection conn) {
        this.parent = parent;
        this.conn = conn;
    }

    // Borrow book by ISBN, customer ID and due date
    public String borrowBook(int isbn, int customerID, int day, int month, int year) {
        StringBuilder result = new StringBuilder("Borrow Book:\n");
        try {
            // Start transaction
            conn.setAutoCommit(false);

            // Check if customer exists
            if (!customerExists(customerID)) {
                conn.rollback(); // Rollback transaction
                String message = "Customer with ID " + customerID + " does not exist";
                result.append(message);
                return result.toString();
            }

            // Lock the customer record
            lockCust(customerID);

            // Check if there are available copies
            int copiesLeft = getAvailableCopies(isbn);
            if (copiesLeft <= 0) {
                conn.rollback(); // Rollback transaction
                String message = "No copies left of book with ISBN " + isbn;
                result.append(message);
                return result.toString();
            }

            // Lock the book record
            lockBook(isbn);

            // Show pop-up window asking the user to click "OK" to continue
            int option = pause();
            if (option != JOptionPane.OK_OPTION) {
                // If the user cancels or closes the dialog, rollback the transaction and return
                conn.rollback();
                return null;
            }

            // Borrow book
            decrementCopiesLeft(isbn);
            insertBorrowingRecord(customerID, day, month, year, isbn);

            // Commit transaction
            conn.commit();

            // Construct result message
            String customerName = getCustomerName(customerID);
            result.append("\tBook: ").append(isbn).append(" (").append(getBookTitle(isbn)).append(")\n")
                    .append("\tLoaned to: ").append(customerID).append(" (").append(customerName)
                    .append(")\n")
                    .append("\tDue Date: ").append(day).append(" ").append(getMonthName(month)).append(" ")
                    .append(year);
            return result.toString();
        } catch (SQLException ex) {
            LibraryUtils.handleRollback(parent, conn, ex.getMessage(), "Borrow Book");
            return null;
        } finally {
            LibraryUtils.restoreAutoCommit(parent, conn, "Borrow Book"); // Restore auto-commit
        }
    }

    // Check validity of customer ID
    private boolean customerExists(int customerID) throws SQLException {
        if (customerID == 0) {
            return false;
        }

        String customerQuery = "SELECT * FROM Customer WHERE CustomerID = ?";
        try (PreparedStatement customerStmt = conn.prepareStatement(customerQuery)) {
            customerStmt.setInt(1, customerID);
            try (ResultSet customerRs = customerStmt.executeQuery()) {
                return customerRs.next();
            }
        }
    }

    // Lock customer
    private void lockCust(int customerID) throws SQLException {
        String lockQuery = "SELECT * FROM Customer WHERE CustomerID = ? FOR UPDATE";
        try (PreparedStatement lockStmt = conn.prepareStatement(lockQuery)) {
            lockStmt.setInt(1, customerID);
            lockStmt.executeQuery();
        }
    }

    // Check available copies
    private int getAvailableCopies(int isbn) throws SQLException {
        String bookQuery = "SELECT NumLeft FROM Book WHERE ISBN = ?";
        try (PreparedStatement bookStmt = conn.prepareStatement(bookQuery)) {
            bookStmt.setInt(1, isbn);
            try (ResultSet bookRs = bookStmt.executeQuery()) {
                if (bookRs.next()) {
                    return bookRs.getInt("NumLeft");
                }
            }
        }
        return 0;
    }

    // Lock book
    private void lockBook(int isbn) throws SQLException {
        String lockQuery = "SELECT * FROM Book WHERE ISBN = ? FOR UPDATE";
        try (PreparedStatement lockStmt = conn.prepareStatement(lockQuery)) {
            lockStmt.setInt(1, isbn);
            lockStmt.executeQuery();
        }
    }

    private int pause() {
        String lockMessage = "Locked the tuples, ready to update. Click OK to continue";
        return JOptionPane.showConfirmDialog(parent, lockMessage, "Pausing",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
    }

    // Add new borrowed book entry
    private void insertBorrowingRecord(int customerID, int day, int month, int year, int isbn)
            throws SQLException {
        String borrowQuery = "INSERT INTO Cust_Book (CustomerID, DueDate, ISBN) VALUES (?, ?, ?)";
        try (PreparedStatement borrowStmt = conn.prepareStatement(borrowQuery)) {
            borrowStmt.setInt(1, customerID);
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, day); // Correctly setting the due date
            java.util.Date utilDueDate = calendar.getTime();
            java.sql.Date sqlDueDate = new java.sql.Date(utilDueDate.getTime());
            borrowStmt.setDate(2, sqlDueDate);
            borrowStmt.setInt(3, isbn);
            borrowStmt.executeUpdate();
        }
    }

    // Decrement copies left in book entry
    private void decrementCopiesLeft(int isbn) throws SQLException {
        String updateQuery = "UPDATE Book SET NumLeft = NumLeft - 1 WHERE ISBN = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
            updateStmt.setInt(1, isbn);
            updateStmt.executeUpdate();
        }
    }

    // Get customer information
    private String getCustomerName(int customerID) throws SQLException {
        String customerName = "";
        String customerInfoQuery = "SELECT F_Name, L_Name FROM Customer WHERE CustomerID = ?";
        try (PreparedStatement customerInfoStmt = conn.prepareStatement(customerInfoQuery)) {
            customerInfoStmt.setInt(1, customerID);
            try (ResultSet customerInfoRs = customerInfoStmt.executeQuery()) {
                if (customerInfoRs.next()) {
                    String firstName = customerInfoRs.getString("F_Name");
                    String lastName = customerInfoRs.getString("L_Name");
                    customerName = firstName.trim() + " " + lastName.trim();
                }
            }
        }
        return customerName;
    }

    // Get book information
    private String getBookTitle(int isbn) throws SQLException {
        String bookTitle = "";
        String titleQuery = "SELECT Title FROM Book WHERE ISBN = ?";
        try (PreparedStatement titleStmt = conn.prepareStatement(titleQuery)) {
            titleStmt.setInt(1, isbn);
            try (ResultSet titleRs = titleStmt.executeQuery()) {
                if (titleRs.next()) {
                    bookTitle = titleRs.getString("Title");
                }
            }
        }
        return bookTitle.trim();
    }

    // Get due date information
    private String getMonthName(int month) {
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        return months[month];
    }

    // Return book by ISBN and customer ID
    public String returnBook(int isbn, int customerID) {
        StringBuilder result = new StringBuilder("Return Book:\n");
        try {
            // Start transaction
            conn.setAutoCommit(false);

            // Check if book is borrowed by specified customer
            if (!bookIsBorrowed(isbn, customerID)) {
                conn.rollback();
                String message = "\tBook " + isbn + " is not loaned to customer " + customerID;
                result.append(message);
                return result.toString();
            }

            // Lock customer
            lockCust(customerID);

            // Lock the book record
            lockBook(isbn);

            // Show pop-up window asking the user to click "OK" to continue
            int option = pause();
            if (option != JOptionPane.OK_OPTION) {
                // If the user cancels or closes the dialog, rollback the transaction and return
                conn.rollback();
                return null;
            }

            // Return book
            removeBorrowingRecord(isbn, customerID);
            incrementCopiesLeft(isbn);

            // Finish transaction
            conn.commit();

            // Construct result mesage
            result.append("\tBook ").append(isbn).append(" returned for customer ").append(customerID);
            return result.toString();
        } catch (SQLException e) {
            LibraryUtils.handleRollback(parent, conn, e.getMessage(), "Return Book");
            return null;
        } finally {
            LibraryUtils.restoreAutoCommit(parent, conn, "Return Book");
        }
    }

    // Check that book is loaned and customer ID is correct
    private boolean bookIsBorrowed(int isbn, int customerID) throws SQLException {
        String borrowCheckQuery = "SELECT * FROM Cust_Book WHERE ISBN = ? AND CustomerID = ? FOR UPDATE";
        try (PreparedStatement borrowCheckStmt = conn.prepareStatement(borrowCheckQuery)) {
            borrowCheckStmt.setInt(1, isbn);
            borrowCheckStmt.setInt(2, customerID);
            try (ResultSet borrowCheckRs = borrowCheckStmt.executeQuery()) {
                return borrowCheckRs.next();
            }
        }
    }

    // Delete borrowed book entry
    private void removeBorrowingRecord(int isbn, int customerID) throws SQLException {
        String returnQuery = "DELETE FROM Cust_Book WHERE ISBN = ? AND CustomerID = ?";
        try (PreparedStatement returnStmt = conn.prepareStatement(returnQuery)) {
            returnStmt.setInt(1, isbn);
            returnStmt.setInt(2, customerID);
            returnStmt.executeUpdate();
        }
    }

    // Increment copies left in book entry
    private void incrementCopiesLeft(int isbn) throws SQLException {
        String updateQuery = "UPDATE Book SET NumLeft = NumLeft + 1 WHERE ISBN = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
            updateStmt.setInt(1, isbn);
            updateStmt.executeUpdate();
        }
    }
}
