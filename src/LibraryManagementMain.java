import javax.swing.*;

public class LibraryManagementMain {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Library Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        frame.setLayout(null);
        frame.setResizable(false);

        JTextField searchField = new JTextField();
        searchField.setBounds(20, 20, 500, 50);
        frame.add(searchField);

        JButton searchButton = new JButton("Search");
        searchButton.setBounds(550, 20, 200, 50);
        frame.add(searchButton);

        JButton addBookButton = new JButton("Add Book");
        addBookButton.setBounds(20, 90, 200, 50);
        frame.add(addBookButton);

        JButton deleteBookButton = new JButton("Delete Book");
        deleteBookButton.setBounds(240, 90, 200, 50);
        frame.add(deleteBookButton);

        frame.setVisible(true);
    }
}