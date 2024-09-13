import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.math.BigDecimal;

public class UniversityDatabaseApp extends JFrame {

    private DefaultTableModel tableModel;
    private JTable dataTable;
    private Connection conn;
    private JTextField searchField;  

    public UniversityDatabaseApp() {
        conn = connectToDatabase();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Unable to connect to the database.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        createView();
        setTitle("University Database Application");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
    }

    private Connection connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/universities?serverTimezone=UTC&useSSL=false";
            String user = "root";
            String password = "0000";
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to database: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    private void createView() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton viewAllButton = new JButton("View All Universities");
        JButton addToFavoritesButton = new JButton("Add to Favorites");
        JButton viewFavoritesButton = new JButton("View Favorites");
        JButton removeFromFavoritesButton = new JButton("Remove from Favorites");
        JButton openHomepageButton = new JButton("Open University Homepage");
        topPanel.add(viewAllButton);
        topPanel.add(addToFavoritesButton);
        topPanel.add(viewFavoritesButton);
        topPanel.add(openHomepageButton);
        topPanel.add(removeFromFavoritesButton);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        JButton resetButton = new JButton("Reset");
        JComboBox<String> criteriaDropdown = new JComboBox<>(new String[]{
        	    "State", "Max Tuition Fee", "Type of Institution", "Max Undergraduate Population", "National Ranking"
        	});

        filterPanel.add(new JLabel("Filter Criteria:"));
        filterPanel.add(criteriaDropdown);
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(resetButton);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 4: return Integer.class;  // Tuition fees
                    case 6: return Integer.class;  // Undergraduate population
                    case 7: return Double.class;   // Acceptance rate
                    case 8: return Integer.class;  // Average SAT/ACT score
                    case 9: return Integer.class;  // Ranking national
                    case 10: return Double.class;  // Graduation rate
                    case 11: return Double.class;  // Percentage of international students
                    default: return String.class;
                }
            }
        };
        dataTable = new JTable(tableModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        dataTable.setRowSorter(sorter);
        sorter.setSortsOnUpdates(true); // Enable sorting updates

        JScrollPane scrollPane = new JScrollPane(dataTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        viewAllButton.addActionListener(e -> showAllUniversities());
        addToFavoritesButton.addActionListener(e -> addToFavorites());
        viewFavoritesButton.addActionListener(e -> viewFavorites());
        searchButton.addActionListener(e -> applyFilter(criteriaDropdown.getSelectedItem().toString(), searchField.getText()));
        resetButton.addActionListener(e -> resetFilters());
        removeFromFavoritesButton.addActionListener(e -> removeFromFavorites());
        openHomepageButton.addActionListener(e -> openSelectedUniversityHomepage());
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(filterPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);
    }

	private void resetFilters() {
	    if (!searchField.getText().isEmpty()) {
	        searchField.setText("");  // Clear the text in the search field
	        showAllUniversities();    // Display all universities after resetting the search filter
	    } else {
	        JOptionPane.showMessageDialog(this, "Search field is already empty.", "Information", JOptionPane.INFORMATION_MESSAGE);
	    }
	}

	private void removeFromFavorites() {
	    int selectedRow = dataTable.getSelectedRow();
	    if (selectedRow == -1) {
	        JOptionPane.showMessageDialog(this, "No university selected.", "Selection Error", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    int modelRow = dataTable.convertRowIndexToModel(selectedRow);
	    String universityName = (String) tableModel.getValueAt(modelRow, 0);  // Assuming the first column is the university name

	    try {
	        String deleteQuery = "DELETE FROM favorites WHERE UniversityName = ?";
	        try (PreparedStatement pstmt = conn.prepareStatement(deleteQuery)) {
	            pstmt.setString(1, universityName);
	            int affectedRows = pstmt.executeUpdate();
	            if (affectedRows > 0) {
	                JOptionPane.showMessageDialog(this, "Removed from Favorites: " + universityName, "Success", JOptionPane.INFORMATION_MESSAGE);
	                viewFavorites(); // Optionally refresh the favorites view if it is visible
	            } else {
	                JOptionPane.showMessageDialog(this, "No rows removed. University may not be in favorites.", "Error", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	    } catch (SQLException ex) {
	        JOptionPane.showMessageDialog(this, "Error removing from favorites: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
	    }
	}


	private void addToFavorites() {
	    int selectedRow = dataTable.getSelectedRow();
	    if (selectedRow == -1) {
	        JOptionPane.showMessageDialog(this, "No university selected.", "Selection Error", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    int modelRow = dataTable.convertRowIndexToModel(selectedRow);
	    // Retrieve all necessary data from the table
	    try {
	        String insertQuery = "INSERT INTO favorites (UniversityName, City, State, UniversityLink, TuitionFeesDomestic, TypeOfInstitution, UndergraduatePopulation, AcceptanceRate, AverageSAT_ACTScore, RankingNational, GraduationRate, PercentageOfInternationalStudents) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
	            pstmt.setString(1, (String) tableModel.getValueAt(modelRow, 0)); // UniversityName
	            pstmt.setString(2, (String) tableModel.getValueAt(modelRow, 1)); // City
	            pstmt.setString(3, (String) tableModel.getValueAt(modelRow, 2)); // State
	            pstmt.setString(4, (String) tableModel.getValueAt(modelRow, 3)); // UniversityLink
	            pstmt.setInt(5, (Integer) tableModel.getValueAt(modelRow, 4)); // TuitionFeesDomestic
	            pstmt.setString(6, (String) tableModel.getValueAt(modelRow, 5)); // TypeOfInstitution
	            pstmt.setInt(7, (Integer) tableModel.getValueAt(modelRow, 6)); // UndergraduatePopulation
	            pstmt.setBigDecimal(8, (BigDecimal) tableModel.getValueAt(modelRow, 7)); // AcceptanceRate
	            pstmt.setString(9, (String) tableModel.getValueAt(modelRow, 8)); // AverageSAT_ACTScore
	            pstmt.setInt(10, (Integer) tableModel.getValueAt(modelRow, 9)); // RankingNational
	            pstmt.setBigDecimal(11, (BigDecimal) tableModel.getValueAt(modelRow, 10)); // GraduationRate
	            pstmt.setBigDecimal(12, (BigDecimal) tableModel.getValueAt(modelRow, 11)); // PercentageOfInternationalStudents
	            
	            int affectedRows = pstmt.executeUpdate();
	            if (affectedRows > 0) {
	                JOptionPane.showMessageDialog(this, "Added to Favorites: " + tableModel.getValueAt(modelRow, 0), "Success", JOptionPane.INFORMATION_MESSAGE);
	            } else {
	                JOptionPane.showMessageDialog(this, "No rows added. Check database constraints.", "Database Error", JOptionPane.ERROR_MESSAGE);
	            }
	        }
	    } catch (SQLException ex) {
	        JOptionPane.showMessageDialog(this, "Error adding to favorites: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
	    }
	}

	    private void viewFavorites() {
	        String query = "SELECT * FROM favorites"; // Ensure your table name and columns are correct
	        try (PreparedStatement stmt = conn.prepareStatement(query)) {
	            executeQuery(stmt, "View Favorites");
	        } catch (SQLException e) {
	            JOptionPane.showMessageDialog(this, "Error accessing database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
	        }
	    }


	private void showAllUniversities() {
	    String query = "SELECT * FROM US_Universities";  // Ensure your table name and columns are correct
	    try (PreparedStatement stmt = conn.prepareStatement(query)) {
	        executeQuery(stmt, "View All Universities");
	    } catch (SQLException e) {
	        JOptionPane.showMessageDialog(this, "Error accessing database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
	    }
	}
 
	
	private void applyFilter(String criterion, String searchValue) {
	    // Start with a base query
	    String query = "SELECT * FROM US_Universities WHERE ";
	    boolean validFilter = true;

	    try {
	        switch (criterion) {
	            case "State":
	                query += "State = ?";
	                break;
	            case "Max Tuition Fee":
	                query += "TuitionFeesDomestic <= ?";
	                break;
	            case "Type of Institution":
	                query += "TypeOfInstitution = ?";
	                break;
	            case "Max Undergraduate Population":
	                query += "UndergraduatePopulation <= ?";
	                break;
	            case "National Ranking":
	                query += "RankingNational = ?";
	                break;
	            default:
	                JOptionPane.showMessageDialog(this, "Invalid filter criterion selected.", "Filter Error", JOptionPane.ERROR_MESSAGE);
	                validFilter = false;
	                break;
	        }

	        if (validFilter) {
	            try (PreparedStatement stmt = conn.prepareStatement(query)) {
	                // Set parameter based on expected data type
	                if (criterion.equals("State") || criterion.equals("Type of Institution")) {
	                    stmt.setString(1, searchValue);
	                } else if (criterion.equals("Max Undergraduate Population") || criterion.equals("Max Tuition Fee") || criterion.equals("National Ranking")) {
	                    // Assume numeric values are needed for these fields
	                    stmt.setInt(1, Integer.parseInt(searchValue));
	                }
	                executeQuery(stmt, "Search Results");
	            }
	        }
	    } catch (NumberFormatException ex) {
	        JOptionPane.showMessageDialog(this, "Please enter a valid number for " + criterion, "Invalid Input", JOptionPane.ERROR_MESSAGE);
	    } catch (SQLException e) {
	        JOptionPane.showMessageDialog(this, "Error accessing database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
	    }
	}




    private void executeQuery(PreparedStatement stmt, String title) {
        try (ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            Vector<String> columnNames = new Vector<>();
            int columnCount = metaData.getColumnCount();
            for (int column = 1; column <= columnCount; column++) {
                columnNames.add(metaData.getColumnName(column));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> vector = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    vector.add(rs.getObject(columnIndex));
                }
                data.add(vector);
            }

            tableModel.setDataVector(data, columnNames);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error executing query: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    private void openSelectedUniversityHomepage() {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "No university selected.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int modelRow = dataTable.convertRowIndexToModel(selectedRow);
        String url = (String) tableModel.getValueAt(modelRow, 3); // Adjust this index based on where the URL is in your table
        openWebPage(url);
    }
    

    private void openWebPage(String url) {
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to open the URL: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UniversityDatabaseApp app = new UniversityDatabaseApp();
            app.setVisible(true);
        });
    }}