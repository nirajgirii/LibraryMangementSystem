import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

public class LibraryManagementMain {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        // Search field and button
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(30);
        topPanel.add(searchField);

        JButton searchButton = new JButton("Search");
        topPanel.add(searchButton);

        JButton addBookButton = new JButton("Add Book");
        topPanel.add(addBookButton);

        JButton deleteBookButton = new JButton("Delete Book");
        topPanel.add(deleteBookButton);

        frame.add(topPanel, BorderLayout.NORTH);

        // Label to display search result
        JLabel resultLabel = new JLabel("");
        frame.add(resultLabel, BorderLayout.SOUTH);

        // JScrollPane to make the book cards scrollable
        JPanel booksPanel = new JPanel();
        booksPanel.setLayout(new GridLayout(0, 3, 10, 10)); // 3 columns with space
        JScrollPane scrollPane = new JScrollPane(booksPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Call loadBooks to load the books initially
        loadBooks(booksPanel, DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS);

        // Action listener for the search button
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchQuery = searchField.getText();

                // Clear the booksPanel before displaying search results
                booksPanel.removeAll();
                booksPanel.revalidate();
                booksPanel.repaint();

                // Perform database search
                try (Connection connection = DriverManager.getConnection(DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS)) {
                    // SQL query to search for books by title
                    String query = "SELECT * FROM BooksAvailable WHERE title LIKE ?";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, "%" + searchQuery + "%"); // Search for books matching the search query
                    ResultSet resultSet = preparedStatement.executeQuery();

                    // If books are found, display them in the panel
                    while (resultSet.next()) {
                        String title = resultSet.getString("title");
                        int quantity = resultSet.getInt("quantityAvailable");
                        Blob imageBlob = resultSet.getBlob("image");

                        // Create a book card (panel)
                        JPanel bookCard = createBookCard(title, quantity, imageBlob);
                        booksPanel.add(bookCard);
                    }

                    // Revalidate and repaint the panel after adding the search results
                    booksPanel.revalidate();
                    booksPanel.repaint();

                    if (!resultSet.next()) {
                        resultLabel.setText("No books found for: '" + searchQuery + "'");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    resultLabel.setText("Error searching for the book.");
                }
            }
        });

        // Action listener for Add Book button
        addBookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a dialog for adding book details
                JFrame addBookFrame = new JFrame("Add Book");
                addBookFrame.setSize(400, 300);
                addBookFrame.setLayout(new GridLayout(5, 2));
                addBookFrame.setResizable(false);

                // Fields for book details
                JTextField titleField = new JTextField();
                JTextField quantityField = new JTextField();
                JLabel imageLabel = new JLabel("Image: (optional)", SwingConstants.CENTER);
                JButton uploadImageButton = new JButton("Upload Image");
                JLabel imagePathLabel = new JLabel("No image selected");

                // Add components to the dialog
                addBookFrame.add(new JLabel("Title:"));
                addBookFrame.add(titleField);
                addBookFrame.add(new JLabel("Quantity:"));
                addBookFrame.add(quantityField);
                addBookFrame.add(imageLabel);
                addBookFrame.add(uploadImageButton);
                addBookFrame.add(imagePathLabel);

                JButton submitButton = new JButton("Add Book");
                addBookFrame.add(submitButton);

                // Image selection logic
                uploadImageButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JFileChooser fileChooser = new JFileChooser();
                        int returnValue = fileChooser.showOpenDialog(null);
                        if (returnValue == JFileChooser.APPROVE_OPTION) {
                            imagePathLabel.setText(fileChooser.getSelectedFile().getPath());
                        }
                    }
                });

                submitButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String title = titleField.getText();
                        String quantityText = quantityField.getText();
                        if (title.isEmpty() || quantityText.isEmpty()) {
                            JOptionPane.showMessageDialog(addBookFrame, "Title and Quantity cannot be empty.");
                            return;
                        }

                        try {
                            int quantity = Integer.parseInt(quantityText);

                            // Check if image exists and load the image data
                            InputStream imageStream = null;
                            String imagePath = imagePathLabel.getText();
                            if (!imagePath.equals("No image selected")) {
                                imageStream = new ByteArrayInputStream(new ImageIcon(imagePath).toString().getBytes());
                            }

                            // Insert the new book into the database
                            try (Connection connection = DriverManager.getConnection(DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS)) {
                                String insertQuery = "INSERT INTO BooksAvailable (title, quantityAvailable, image) VALUES (?, ?, ?)";
                                PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                                insertStatement.setString(1, title);
                                insertStatement.setInt(2, quantity);
                                if (imageStream != null) {
                                    insertStatement.setBlob(3, imageStream);
                                } else {
                                    insertStatement.setNull(3, Types.BLOB);
                                }
                                insertStatement.executeUpdate();
                                JOptionPane.showMessageDialog(addBookFrame, "Book added successfully!");

                                // Reload the books after adding a new book
                                booksPanel.removeAll();
                                loadBooks(booksPanel, DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS);
                                addBookFrame.dispose();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(addBookFrame, "Error adding the book.");
                            }

                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(addBookFrame, "Invalid quantity format.");
                        }
                    }
                });

                addBookFrame.setVisible(true);
            }
        });

        // Action listener for Delete Book button
        deleteBookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Create a dialog for deleting book details
                JFrame deleteBookFrame = new JFrame("Delete Book");
                deleteBookFrame.setSize(400, 200);
                deleteBookFrame.setLayout(new GridLayout(3, 2));
                deleteBookFrame.setResizable(false);

                // Field for book title to delete
                JTextField titleField = new JTextField();

                // Add components to the dialog
                deleteBookFrame.add(new JLabel("Book Title:"));
                deleteBookFrame.add(titleField);

                JButton deleteButton = new JButton("Delete Book");
                deleteBookFrame.add(deleteButton);

                deleteButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String title = titleField.getText();
                        if (title.isEmpty()) {
                            JOptionPane.showMessageDialog(deleteBookFrame, "Title cannot be empty.");
                            return;
                        }

                        // Perform database operation to delete book
                        try (Connection connection = DriverManager.getConnection(DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS)) {
                            String deleteQuery = "DELETE FROM BooksAvailable WHERE title = ?";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.setString(1, title);
                            int deletedRows = deleteStatement.executeUpdate();

                            if (deletedRows > 0) {
                                JOptionPane.showMessageDialog(deleteBookFrame, "Book deleted successfully!");

                                // Reload the books after deletion
                                booksPanel.removeAll();
                                loadBooks(booksPanel, DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS);
                                deleteBookFrame.dispose();
                            } else {
                                JOptionPane.showMessageDialog(deleteBookFrame, "No book found with that title.");
                            }

                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(deleteBookFrame, "Error deleting the book.");
                        }
                    }
                });

                deleteBookFrame.setVisible(true);
            }
        });

        frame.setVisible(true);
    }

    // Method to load books from the database
    public static void loadBooks(JPanel booksPanel, String dbUrl, String dbUser, String dbPass) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String query = "SELECT * FROM BooksAvailable";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String title = resultSet.getString("title");
                int quantity = resultSet.getInt("quantityAvailable");
                Blob imageBlob = resultSet.getBlob("image");

                // Create a book card and add it to the panel
                JPanel bookCard = createBookCard(title, quantity, imageBlob);
                booksPanel.add(bookCard);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to create a book card
    public static JPanel createBookCard(String title, int quantity, Blob imageBlob) {
        JPanel bookCard = new JPanel();
        bookCard.setLayout(new BorderLayout());
        bookCard.setPreferredSize(new Dimension(200, 250));
        bookCard.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        // Book image
        JLabel imageLabel = new JLabel();
        if (imageBlob != null) {
            try {
                InputStream imageStream = imageBlob.getBinaryStream();
                byte[] imageBytes = imageStream.readAllBytes();
                ImageIcon imageIcon = new ImageIcon(imageBytes);
                Image scaledImage = imageIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            imageLabel.setText("Image Not Available");
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }

        // Title and quantity info
        bookCard.add(imageLabel, BorderLayout.NORTH);
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridLayout(2, 1));
        textPanel.add(new JLabel("Title: " + title));
        textPanel.add(new JLabel("Quantity: " + quantity));
        bookCard.add(textPanel, BorderLayout.CENTER);

        return bookCard;
    }
}
