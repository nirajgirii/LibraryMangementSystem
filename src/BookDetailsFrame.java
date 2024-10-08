import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BookDetailsFrame extends JFrame {
    private String title;
    private int quantity;
    private Blob imageBlob;
    private Connection connection;

    public BookDetailsFrame(String title, int quantity, Blob imageBlob) {
        this.title = title;
        this.quantity = quantity;
        this.imageBlob = imageBlob;

        try {
            connection = DriverManager.getConnection(DatabaseInformation.DB_URL, DatabaseInformation.DB_USER, DatabaseInformation.DB_PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setTitle("Book Details");
        setSize(400, 400);
        setLayout(new BorderLayout());
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Panel for book details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new GridLayout(4, 1));

        // Title label
        JLabel titleLabel = new JLabel("Title: " + title);
        detailsPanel.add(titleLabel);

        // Quantity label
        JLabel quantityLabel = new JLabel("Quantity Available: " + quantity);
        detailsPanel.add(quantityLabel);

        // Image label
        JLabel imageLabel = new JLabel();
        if (imageBlob != null) {
            try {
                InputStream imageStream = imageBlob.getBinaryStream();
                byte[] imageBytes = ((InputStream) imageStream).readAllBytes();
                ImageIcon imageIcon = new ImageIcon(imageBytes);
                Image scaledImage = imageIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            imageLabel.setText("No Image Available");
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        detailsPanel.add(imageLabel);

        // Panel for allocate buttons and actions
        JPanel buttonPanel = new JPanel();
        JButton studentButton = new JButton("Allocate to Student");
        studentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                allocateToStudent();
            }
        });
        buttonPanel.add(studentButton);

        JButton teacherButton = new JButton("Allocate to Teacher");
        teacherButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                allocateToTeacher();
            }
        });
        buttonPanel.add(teacherButton);

        JButton editButton = new JButton("Edit Allocation");
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editAllocation();
            }
        });
        buttonPanel.add(editButton);

        JButton copyButton = new JButton("Copy Allocation");
        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyAllocation();
            }
        });
        buttonPanel.add(copyButton);

        JButton deleteButton = new JButton("Delete Allocation");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteAllocation();
            }
        });
        buttonPanel.add(deleteButton);

        // Back button
        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();  // Close the details window
            }
        });

        add(detailsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(backButton, BorderLayout.NORTH);
    }

    private void allocateToStudent() {
        JTextField nameField = new JTextField();
        JTextField idField = new JTextField();
        Object[] message = {
                "Student Name:", nameField,
                "Student ID:", idField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Allocate to Student", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String studentName = nameField.getText();
            String studentId = idField.getText();
            if (!studentName.isEmpty() && !studentId.isEmpty()) {
                if (isBookAlreadyAllocated("Student", studentName, studentId)) {
                    JOptionPane.showMessageDialog(this, "This student has already borrowed this book.");
                    return;
                }

                Date currentDate = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String allocationDate = sdf.format(currentDate);
                String returnDate = sdf.format(new Date(currentDate.getTime() + 15L * 24 * 60 * 60 * 1000));

                try {
                    String query = "INSERT INTO BooksAllocated (name, role, role_id, book_title, date_of_allocation, date_of_return) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, studentName);
                    preparedStatement.setString(2, "Student");
                    preparedStatement.setString(3, studentId);
                    preparedStatement.setString(4, title);
                    preparedStatement.setString(5, allocationDate);
                    preparedStatement.setString(6, returnDate);
                    int result = preparedStatement.executeUpdate();

                    if (result > 0) {
                        updateBookQuantity(-1);
                        JOptionPane.showMessageDialog(this, "Book successfully allocated to student!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to allocate book.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error allocating book to student.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both name and ID.");
            }
        }
    }

    private void allocateToTeacher() {
        JTextField nameField = new JTextField();
        JTextField idField = new JTextField();
        Object[] message = {
                "Teacher Name:", nameField,
                "Teacher ID:", idField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Allocate to Teacher", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            String teacherName = nameField.getText();
            String teacherId = idField.getText();

            if (!teacherName.isEmpty() && !teacherId.isEmpty()) {
                if (isBookAlreadyAllocated("Teacher", teacherName, teacherId)) {
                    JOptionPane.showMessageDialog(this, "This teacher has already borrowed this book.");
                    return;
                }

                try {
                    String query = "INSERT INTO BooksAllocated (name, role, role_id, book_title, date_of_allocation, date_of_return) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                    preparedStatement.setString(1, teacherName);
                    preparedStatement.setString(2, "Teacher");
                    preparedStatement.setString(3, teacherId);
                    preparedStatement.setString(4, title);
                    preparedStatement.setString(5, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                    preparedStatement.setString(6, new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000)));

                    int result = preparedStatement.executeUpdate();
                    if (result > 0) {
                        updateBookQuantity(-1);
                        JOptionPane.showMessageDialog(this, "Book successfully allocated to teacher!");
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to allocate book.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error allocating book to teacher.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both name and ID.");
            }
        }
    }

    private void editAllocation() {
        // Method for editing book allocation (update existing records in BooksAllocated table)
        // Implement UI for selecting and editing records
    }

    private void copyAllocation() {
        // Method for copying book allocation (create a duplicate entry in the BooksAllocated table)
        // Implement UI for duplicating records
    }

    private void deleteAllocation() {
        // Method for deleting book allocation
        // Prompt the user to select an allocation to delete and remove it from the table
    }

    private boolean isBookAlreadyAllocated(String role, String name, String id) {
        try {
            String query = "SELECT * FROM BooksAllocated WHERE book_title = ? AND role = ? AND name = ? AND role_id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, role);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void updateBookQuantity(int change) {
        try {
            String query = "UPDATE BooksAvailable SET quantityAvailable = quantityAvailable + ? WHERE title = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, change);
            preparedStatement.setString(2, title);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
