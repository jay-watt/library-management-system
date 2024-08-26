import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.*;

public class LibraryModel {
    private final Connection conn;
    private final BookModel bookModel;
    private final AuthorModel authorModel;
    private final CustomerModel customerModel;
    private final LoaningModel loaningModel;

    public LibraryModel(JFrame parent, String userid, String password) {
        this.conn = LibraryUtils.setupDatabaseConnection(parent, userid, password);
        this.bookModel = new BookModel(parent, conn);
        this.authorModel = new AuthorModel(parent, conn);
        this.customerModel = new CustomerModel(parent, conn);
        this.loaningModel = new LoaningModel(parent, conn);

        initialiseDatabase();
    }

    public void initialiseDatabase() {
        executeSqlFromFile("../../Data/library.data");
    }

    public String bookLookup(int isbn) {
        return bookModel.bookLookup(isbn);
    }

    public String showCatalogue() {
        return bookModel.showCatalogue();
    }

    public String showLoanedBooks() {
        return bookModel.showLoanedBooks();
    }

    public String showAuthor(int authorID) {
        return authorModel.showAuthor(authorID);
    }

    public String showAllAuthors() {
        return authorModel.showAllAuthors();
    }

    public String showCustomer(int customerID) {
        return customerModel.showCustomer(customerID);
    }

    public String showAllCustomers() {
        return customerModel.showAllCustomers();
    }

    public String borrowBook(int isbn, int customerID, int day, int month, int year) {
        return loaningModel.borrowBook(isbn, customerID, day, month, year);
    }

    public String returnBook(int isbn, int customerID) {
        return loaningModel.returnBook(isbn, customerID);
    }

    public void closeDBConnection() {
        LibraryUtils.closeDBConnection(conn);
    }

    public String deleteCus(int customerID) {
        return customerModel.deleteCus(customerID);
    }

    public String deleteAuthor(int authorID) {
        return authorModel.deleteAuthor(authorID);
    }

    public String deleteBook(int isbn) {
        return bookModel.deleteBook(isbn);
    }

    public void executeSqlFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                    continue; // Skip empty lines and comments
                }
                executeSql(line);
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle file reading exceptions
        }
    }

    private void executeSql(String sql) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL execution exceptions
        }
    }

}