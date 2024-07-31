import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JFrame;

public class AuthorModel {
    private final JFrame parent;
    private final Connection conn;

    public AuthorModel(JFrame parent, Connection conn) {
        this.parent = parent;
        this.conn = conn;
    }

    // Get author by ID
    public String showAuthor(int authorID) {
        StringBuilder output = new StringBuilder("Show Author:\n");

        // Check if requested author is default entry
        if (authorID == 0) {
            appendDefaultAuthor(output);
            return output.toString();
        }

        String authorQuery = "SELECT a.Name, a.Surname, b.ISBN, b.Title FROM Author a LEFT JOIN Book_Author ba ON a.AuthorId = ba.AuthorId LEFT JOIN Book b ON ba.ISBN = b.ISBN WHERE a.AuthorId = ? ORDER BY b.ISBN";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(authorQuery)) {
                stmt.setInt(1, authorID);
                ResultSet rs = stmt.executeQuery();

                // Requested author does not exist
                if (!processAuthorData(rs, output, authorID)) {
                    output.append("\tNo such author ID: ").append(authorID);
                }
            }

            // Revert allowing the reading of locked items
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Author Lookup");
        }
        return output.toString();
    }

    // Append default author information to output
    private void appendDefaultAuthor(StringBuilder output) {
        output.append("\t0 - Default Name Default Surname\n\t(No books written)");
    }

    // Process data to extract author information
    private boolean processAuthorData(ResultSet rs, StringBuilder output, int authorID) throws SQLException {

        // Initialise books summary
        StringBuilder books = new StringBuilder();

        // Initialise book information counter variables
        boolean authorFound = false;
        int bookCount = 0;

        // Iterate through data entries
        while (rs.next()) {

            // Append new author information
            if (!authorFound) {
                appendAuthorInfo(rs, output, authorID);
                authorFound = true;
            }

            // Append new book information
            if (appendBookInfo(rs, books)) {
                bookCount++;
            }
        }

        // Finalise author information with complete book summary
        if (authorFound) {
            appendBookSummary(output, books, bookCount);
        }
        return authorFound;
    }

    // Append author information to output
    private void appendAuthorInfo(ResultSet rs, StringBuilder output, int authorID) throws SQLException {
        String firstName = rs.getString("Name").trim();
        String surname = rs.getString("Surname").trim();
        String authorFullName = firstName + " " + surname;
        output.append("\t").append(authorID).append(" - ").append(authorFullName);
    }

    // Append book information to books summary
    private boolean appendBookInfo(ResultSet rs, StringBuilder books) throws SQLException {
        String title = rs.getString("Title");
        if (title != null) {
            int isbn = rs.getInt("ISBN");
            books.append("\n\t\t").append(isbn).append(" - ").append(title.trim());
            return true;
        }
        return false;
    }

    // Append book summary to output
    private void appendBookSummary(StringBuilder output, StringBuilder books, int bookCount) {
        if (bookCount > 0) {
            output.append(bookCount == 1 ? "\n\tBook written:" : "\n\tBooks written:").append(books);
        } else {
            output.append("\n\t(No books written)");
        }
    }

    // Get all authors
    public String showAllAuthors() {
        StringBuilder output = new StringBuilder("Show All Authors:");
        String authorQuery = "SELECT DISTINCT a.AuthorId, a.Name, a.Surname FROM Author a ORDER BY a.AuthorId";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(authorQuery)) {
                ResultSet rs = stmt.executeQuery();
                processAuthorsData(rs, output);
            }

            // Revert allowing the reading of locked items
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Show All Authors");
        }
        return output.toString();
    }

    // Process data to get information about all authors
    private void processAuthorsData(ResultSet rs, StringBuilder output) throws SQLException {
        while (rs.next()) {
            processAuthorEntry(rs, output);
        }
    }

    // Process a single author
    private void processAuthorEntry(ResultSet rs, StringBuilder output) throws SQLException {
        int authorID = rs.getInt("AuthorId");
        String firstName = rs.getString("Name").trim();
        String surname = rs.getString("Surname").trim();
        output.append("\n\t").append(authorID).append(": ").append(surname).append(", ").append(firstName);
    }

    // Delete author by ID
    public String deleteAuthor(int authorID) {
        StringBuilder output = new StringBuilder("Delete Author:\n");

        // Check if requested customer is default entry
        if (authorID == 0) {
            output.append("\tCannot delete default author entry");
            return output.toString();
        }

        String deleteQuery = "DELETE FROM Author WHERE AuthorId = ?";

        // Begin transaction and disable auto commit
        try {
            conn.setAutoCommit(false);

            // Do deletion
            try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
                stmt.setInt(1, authorID);
                int rowsAffected = stmt.executeUpdate();
                handleDeletionOutput(output, authorID, rowsAffected);

                // Complete transaction
                conn.commit();
            }
        } catch (SQLException e) {
            LibraryUtils.handleRollback(parent, conn, e.getMessage(), "Delete Author");
        }

        // Enable auto commit
        finally {
            LibraryUtils.restoreAutoCommit(parent, conn, "Delete Author");
        }
        return output.toString();
    }

    // Handle deletion output
    private void handleDeletionOutput(StringBuilder output, int authorID, int rowsAffected)
            throws SQLException {
        if (rowsAffected > 0) {
            output.append("\tAuthor with ID ").append(authorID).append(" deleted");
        } else {
            output.append("\tNo such Author ID: ").append(authorID);
        }
    }
}
