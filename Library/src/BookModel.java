import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JFrame;

public class BookModel {
    private final JFrame parent;
    private final Connection conn;

    public BookModel(JFrame parent, Connection conn) {
        this.parent = parent;
        this.conn = conn;
    }

    // Get book by ISBN
    public String bookLookup(int isbn) {
        StringBuilder output = new StringBuilder("Book Lookup:\n");

        // Check if requested book is default entry
        if (isbn == 0) {
            appendDefaultBook(output);
            return output.toString();
        }

        String bookQuery = "SELECT b.ISBN, b.Title, b.Edition_No, b.NumOfCop, b.NumLeft, a.Surname FROM Book b LEFT JOIN Book_Author ba ON b.ISBN = ba.ISBN LEFT JOIN Author a ON ba.AuthorId = a.AuthorId WHERE b.ISBN = ? ORDER BY b.ISBN, ba.AuthorSeqNo";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(bookQuery)) {
                stmt.setInt(1, isbn);
                ResultSet rs = stmt.executeQuery();

                // Requested book does not exist
                if (!processBookData(rs, output, isbn)) {
                    output.append("\tNo such ISBN: ").append(isbn);
                }
            }

            // Revert allowing the reading of locked items
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Book Lookup");
        }
        return output.toString();
    }

    // Append default book information to output
    public void appendDefaultBook(StringBuilder output) {
        output.append(
                "0: Default Book Name\n\tEdition: 0 - Number of copies: 1 - Copies left: 1\n\tAuthor: (no authors)");
    }

    // Process data to extract book information
    private boolean processBookData(ResultSet rs, StringBuilder output, int isbn) throws SQLException {
        StringBuilder authors = new StringBuilder();

        // Initialise author information counter variable
        boolean bookFound = false;

        // Iterate through data entries
        while (rs.next()) {

            // Append new book information
            if (!bookFound) {
                appendBookInfo(rs, output, isbn);
                bookFound = true;
            }

            // Append new author information
            appendAuthorInfo(rs, authors);
        }

        // Finialise book information with complete author summary
        if (bookFound) {
            output.append("\t").append(formatAuthors(authors.toString()));
        }
        return bookFound;
    }

    // Append book information to output
    private void appendBookInfo(ResultSet rs, StringBuilder output, int isbn) throws SQLException {
        String title = rs.getString("Title").trim();
        int editionNo = rs.getInt("Edition_No");
        int numOfCop = rs.getInt("NumOfCop");
        int numLeft = rs.getInt("NumLeft");
        output.append("\t").append(isbn).append(": ").append(title).append("\n").append("\t")
                .append("Edition: ").append(editionNo).append(" - Number of copies: ").append(numOfCop)
                .append(" - Copies left: ").append(numLeft).append("\n");
    }

    // Append author information to authors summary
    private void appendAuthorInfo(ResultSet rs, StringBuilder authors) throws SQLException {
        String authorSurname = rs.getString("Surname");
        if (authorSurname != null) {
            if (authors.length() > 0) {
                authors.append(", ");
            }
            authors.append(authorSurname.trim());
        }
    }

    // Format authors list
    private String formatAuthors(String authors) {
        if (authors == null || authors.isEmpty()) {
            return "(no authors)";
        }
        String[] authorArray = authors.split(", ");
        return (authorArray.length > 1) ? "Authors: " + authors : "Author: " + authors;
    }

    // Get all books
    public String showCatalogue() {
        StringBuilder output = new StringBuilder("Show Catalogue:\n\n");

        // Get default entry
        appendDefaultBook(output);

        String catalogueQuery = "SELECT b.ISBN, b.Title, b.Edition_No, b.NumOfCop, b.NumLeft, a.Surname FROM Book b LEFT JOIN Book_Author ba ON b.ISBN = ba.ISBN LEFT JOIN Author a ON ba.AuthorId = a.AuthorId WHERE b.ISBN > 0 ORDER BY b.ISBN, ba.AuthorSeqNo";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(catalogueQuery)) {
                ResultSet rs = stmt.executeQuery();
                processBooksData(rs, output);
            }

            // Revert allowing reading of locked items
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Show Catalogue");
        }
        return output.toString();
    }

    // Process data to get information about all books
    private void processBooksData(ResultSet rs, StringBuilder output) throws SQLException {
        StringBuilder authors = new StringBuilder();
        int currentISBN = -1;
        boolean firstEntry = true;

        while (rs.next()) {
            int isbn = rs.getInt("ISBN");
            if (isbn != currentISBN) {
                if (!firstEntry) {
                    appendAuthors(output, authors);
                }
                processBookEntry(rs, output);

                // Clear the authors StringBuilder for the next book
                authors.setLength(0);
                appendAuthorSurname(rs, authors);
                currentISBN = isbn;
                firstEntry = false;
            } else {
                appendAuthorSurname(rs, authors);
            }
        }

        // Handle the last book entry
        if (currentISBN != -1) {
            output.append("\n\t").append(formatAuthors(authors.toString()));
        }
    }

    // Process a single book
    private void processBookEntry(ResultSet rs, StringBuilder output) throws SQLException {
        int isbn = rs.getInt("ISBN");
        String title = rs.getString("Title").trim();
        int editionNo = rs.getInt("Edition_No");
        int numOfCop = rs.getInt("NumOfCop");
        int numLeft = rs.getInt("NumLeft");
        output.append("\n\n").append(isbn).append(": ").append(title).append("\n")
                .append("\tEdition: ").append(editionNo)
                .append(" - Number of copies: ").append(numOfCop)
                .append(" - Copies left: ").append(numLeft);
    }

    // Append author information to authors StringBuilder
    private void appendAuthorSurname(ResultSet rs, StringBuilder authors) throws SQLException {
        String authorSurname = rs.getString("Surname");
        if (authorSurname != null) {
            if (authors.length() > 0) {
                authors.append(", ");
            }
            authors.append(authorSurname.trim());
        }
    }

    // Append author summary to output
    private void appendAuthors(StringBuilder output, StringBuilder authors) {
        if (authors.toString().contains(",")) {
            output.append("\n\tAuthors: ").append(authors);
        } else if (authors.length() > 0) {
            output.append("\n\tAuthor: ").append(authors);
        } else {
            output.append("\n\t(no authors)");
        }
    }

    // Get all loaned books
    public String showLoanedBooks() {
        StringBuilder output = new StringBuilder("Show Loaned Books:");
        String loanedBooksQuery = "SELECT b.ISBN, b.Title, b.Edition_No, b.NumOfCop, b.NumLeft, (SELECT STRING_AGG(a.Surname, ', ')  FROM Author a  JOIN Book_Author ba ON a.AuthorId = ba.AuthorId  WHERE ba.ISBN = b.ISBN) AS Authors, c.CustomerID, c.L_Name, c.F_Name, c.City FROM Book b JOIN Cust_Book cb ON b.ISBN = cb.ISBN JOIN Customer c ON cb.CustomerID = c.CustomerID ORDER BY b.ISBN, c.CustomerID";

        // Allow reading of locked items
        try {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

            // Get data
            try (PreparedStatement stmt = conn.prepareStatement(loanedBooksQuery)) {
                ResultSet rs = stmt.executeQuery();
                processLoanedBooksData(rs, output);
            }

            // Revert allowing the reading of locked items
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            LibraryUtils.showErrorMessage(parent, e.getMessage(), "Show Loaned Books");
        }
        return output.toString();
    }

    // Process data to get information about all loaned books
    private void processLoanedBooksData(ResultSet rs, StringBuilder output) throws SQLException {
        int currentISBN = -1;
        StringBuilder borrowers = new StringBuilder();
        boolean bookHasBorrowers = false;

        while (rs.next()) {
            int isbn = rs.getInt("ISBN");
            if (isbn != currentISBN) {
                if (currentISBN != -1) {
                    output.append(borrowers);
                }
                currentISBN = isbn;
                borrowers.setLength(0);
                processBookEntry(rs, output);
                output.append("\n\t").append(formatAuthors(rs.getString("Authors")))
                        .append("\n\tBorrowers:");
            }
            processBorrowerEntry(rs, borrowers);
            bookHasBorrowers = true;
        }

        if (bookHasBorrowers) {
            output.append(borrowers);
        }

        if (output.length() == "Show Loaned Books:".length()) {
            output.append("\n(No Loaned Books)");
        }
    }

    // Process single borrower entry
    private void processBorrowerEntry(ResultSet rs, StringBuilder borrowers) throws SQLException {
        int customerID = rs.getInt("CustomerID");
        String lastName = rs.getString("L_Name").trim();
        String firstName = rs.getString("F_Name").trim();
        String city = rs.getString("City").trim();

        borrowers.append("\n\t\t").append(customerID).append(": ").append(lastName).append(", ")
                .append(firstName).append(" - ").append(city);
    }

    // Delete book by ISBN
    public String deleteBook(int isbn) {
        StringBuilder output = new StringBuilder("Delete Book:\n");

        // Check if requested book is default entry
        if (isbn == 0) {
            output.append("\tCannot delete default book entry");
            return output.toString();
        }

        String deleteQuery = "DELETE FROM Book WHERE ISBN = ?";

        // Begin transaction and disable auto commit
        try {
            conn.setAutoCommit(false);

            // Check if book is loaned
            if (isBookLoaned(isbn)) {
                conn.rollback();
                output.append("\tCannot delete book with ISBN ").append(isbn)
                        .append(" because it is currently loaned out");
                return output.toString();
            }

            // Do deletion
            try (PreparedStatement statement = conn.prepareStatement(deleteQuery)) {
                statement.setInt(1, isbn);
                int rowsAffected = statement.executeUpdate();
                handleDeletionOutput(output, isbn, rowsAffected);

                // Complete transaction
                conn.commit();
            }
        } catch (SQLException e) {
            try {
                conn.rollback();
                output.append("\tCannot delete book with ISBN ").append(isbn)
                        .append(" because it has just been loaned out");
            } catch (SQLException ex) {
                LibraryUtils.showErrorMessage(parent, ex.getMessage(), "Delete Book");
            }
        }

        // Enable auto commit
        finally {
            LibraryUtils.restoreAutoCommit(parent, conn, "Delete Book");
        }
        return output.toString();
    }

    // Check if book is currently loaned and cannot be deleted
    private boolean isBookLoaned(int isbn) throws SQLException {
        String checkCustBookQuery = "SELECT COUNT(*) AS Count FROM Cust_Book WHERE ISBN = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkCustBookQuery)) {
            checkStmt.setInt(1, isbn);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            return rs.getInt("Count") > 0;
        }
    }

    // Handle deletion output
    private void handleDeletionOutput(StringBuilder output, int isbn, int rowsAffected) throws SQLException {
        if (rowsAffected > 0) {
            output.append("\tBook with ISBN ").append(isbn).append(" deleted");
        } else {
            output.append("\tNo such ISBN: ").append(isbn);
        }
    }

}
