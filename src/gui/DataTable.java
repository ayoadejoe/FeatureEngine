package gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DataTable extends JScrollPane {

    private JTable table;
    private DefaultTableModel tableModel;
    private Color rowColour;

    public DataTable(Color colour) {
        this.rowColour = colour;
    }

    public void createTable(String filePath) {
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Make "Row #" column read-only
            }
        };
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFont(new Font("Calibri", Font.PLAIN, 12));
        table.setFillsViewportHeight(true);

        styleTable();
        setViewportView(table);
        setPreferredSize(new Dimension(500, 400));

        loadCsvData(filePath);
        adjustColumnWidths();
        this.setViewportView(table);
        this.revalidate();
    }

    private void loadCsvData(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;
            int rowNumber = 0;

            while ((line = br.readLine()) != null) {
                String[] rowData = line.split(",");
                if (isHeader) {
                    // Prepend "Row #" to headers
                    String[] newHeaders = new String[rowData.length + 1];
                    newHeaders[0] = "Row #";
                    System.arraycopy(rowData, 0, newHeaders, 1, rowData.length);
                    tableModel.setColumnIdentifiers(newHeaders);
                    isHeader = false;
                } else {
                    // Prepend row number to data
                    String[] newRowData = new String[rowData.length + 1];
                    newRowData[0] = String.valueOf(++rowNumber);
                    System.arraycopy(rowData, 0, newRowData, 1, rowData.length);
                    tableModel.addRow(newRowData);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading CSV file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adjustColumnWidths() {
        TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = columnModel.getColumn(column);

            // Get header width
            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, tableColumn.getHeaderValue(), false, false, 0, column);
            int width = headerComp.getPreferredSize().width;

            // Check content width
            TableCellRenderer renderer = table.getDefaultRenderer(Object.class);
            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = renderer.getTableCellRendererComponent(
                        table, table.getValueAt(row, column), false, false, row, column);
                width = Math.max(width, comp.getPreferredSize().width);
            }

            width += 10; // Padding
            tableColumn.setPreferredWidth(width);
            tableColumn.setMinWidth(width);
        }
    }

    private void styleTable() {
        table.setRowHeight(35);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Cambria", Font.BOLD, 14));
        header.setBackground(new Color(142, 163, 183, 255));
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    cell.setBackground(row % 2 == 0 ? rowColour : Color.WHITE);
                }
                if (column == 0) { // Style "Row #" column differently
                    cell.setFont(new Font("Calibri", Font.BOLD, 12));
                }
                return cell;
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    public void destroyTable() {
        tableModel = null;
        table = null;
        this.setViewportView(null);
        this.revalidate();
    }
}