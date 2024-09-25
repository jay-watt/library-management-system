# Library Management System

## Overview

This **Library Management System** is a Java-based application designed to manage a library's inventory, including books, authors, customers and loans. It uses a Victoria University-provided GUI and **PostgreSQL** for database management. The system allows users to search for books, manage customers, track borrowed books, and perform other operations like author and book deletion.

## Features

1. **Book Management**:
   - Lookup books by ISBN.
   - View the entire catalogue of available books.
   - View all loaned books.
   - Delete books from the library.

2. **Author Management**:
   - Lookup specific authors by their ID.
   - View a list of all authors.
   - Delete authors from the system.

3. **Customer Management**:
   - Lookup customer details.
   - View all registered customers.
   - Delete customers from the system.

4. **Loan Management**:
   - Borrow a book for a customer by specifying the ISBN, customer ID, and loan date.
   - Return a borrowed book for a customer.

5. **Database Initialization**:
   - Loads initial library data from a provided SQL file.

6. **Database Operations**:
   - The system supports executing raw SQL queries from a file to initialize the database or perform batch operations.

## System Architecture

The system is structured using the following key components:

- **BookModel**: Handles all book-related operations, including lookups, showing catalogues, and deleting books.
- **AuthorModel**: Manages author-related operations such as showing all authors and deleting specific authors.
- **CustomerModel**: Manages customer-related operations like showing customer information, listing all customers, and deleting customers.
- **LoaningModel**: Handles the borrowing and returning of books by customers.
- **LibraryModel**: The central component of the system that integrates all other models and provides a unified interface to manage books, authors, customers, and loans.

## Technology Stack

- **Java**: The core programming language used to build the application.
- **PostgreSQL**: The relational database management system used to store all library data.
- **Swing**: A university-provided GUI framework used for the user interface.
- **JDBC (Java Database Connectivity)**: Used to connect and interact with the PostgreSQL database.

## Database Initialization

The `LibraryModel` initializes the database by reading SQL queries from the `library.data` file located in the `Data` directory. The file is read line by line, and each SQL query is executed to set up the initial state of the database.

To modify or update the initial data, you can edit the `library.data` file and provide appropriate SQL commands for data insertion.
