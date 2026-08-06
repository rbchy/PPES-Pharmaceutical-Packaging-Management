package com.ppes.ui;

import com.ppes.db.Db;
import com.ppes.excel.ExcelExportUtil;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * "Shift Production Log" form — one row per shift, tracking shipper counts
 * and unit production per lot. The header block (Order Qty, Work Order No,
 * Lot No, Item No, Product Name, Exp Date, Ctns Qty in a Shipper) is NOT a
 * new table: Work Order No / Lot No / Item No / Product Name are looked up
 * live from batch_lot + product_master, and Order Qty / Exp Date / Ctns Qty
 * in a Shipper are read from/written to the SAME carton_tracking_header
 * table used by the Carton Tracking Log screen, so you only fill each lot's
 * header in once no matter which screen you use.
 *
 * Only the per-shift grid is new (shift_production_log table). "Total No of
 * Unit Produced" is auto-calculated as a running total down the grid, the
 * same live-recompute pattern used in CartonTrackingPanel.
 */
public class ShiftProductionPanel extends JPanel implements Refreshable {

    private static final String PLACEHOLDER = "-- লট নির্বাচন করুন --";
    private static final String[] COLUMNS = {
            "Date", "Shift", "Final Shipper", "No of Shipper",
            "No of Unit Produced", "Total No of Unit Produced", "Recorded By"
    };
    private static final int COL_DATE = 0;
    private static final int COL_SHIFT = 1;
    private static final int COL_FINAL_SHIPPER = 2;
    private static final int COL_NO_OF_SHIPPER = 3;
    private static final int COL_UNITS = 4;
    private static final int COL_TOTAL_UNITS = 5;
    private static final int COL_RECORDED_BY = 6;
    private static final int DEFAULT_BLANK_ROWS = 10;

    private final JComboBox<ComboItem> lotSelector = new JComboBox<>();
    private final JLabel workOrderValue = new JLabel("-");
    private final JLabel lotNoValue = new JLabel("-");
    private final JLabel itemNoValue = new JLabel("-");
    private final JLabel productNameValue = new JLabel("-");
    private final JTextField orderQtyField = new JTextField(10);
    private final JTextField expiryDateField = new JTextField(10);
    private final JTextField ctnsInShipperField = new JTextField(10);
    private final JLabel totalsLabel = new JLabel(" ");

    private final JComboBox<String> shiftEditorCombo = new JComboBox<>();
    private final JComboBox<String> employeeEditorCombo = new JComboBox<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column != COL_TOTAL_UNITS; // Total column is auto-calculated
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };
    private final JTable shiftTable = new JTable(tableModel);
    private boolean updating = false;

    public ShiftProductionPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        add(new BannerPanel("/images/banner_shift_production.jpg", "Shift Production Log",
                "Per-shift shipper & unit production entries with running total", 110), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(buildHeaderCard(), BorderLayout.NORTH);
        body.add(buildGridCard(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        shiftEditorCombo.setEditable(true);
        employeeEditorCombo.setEditable(true);
        shiftTable.getColumnModel().getColumn(COL_SHIFT).setCellEditor(new DefaultCellEditor(shiftEditorCombo));
        shiftTable.getColumnModel().getColumn(COL_RECORDED_BY).setCellEditor(new DefaultCellEditor(employeeEditorCombo));

        shiftTable.getModel().addTableModelListener(this::onTableChanged);
        lotSelector.addActionListener(e -> loadForSelectedLot());
    }

    private void onTableChanged(TableModelEvent e) {
        if (updating || e.getType() != TableModelEvent.UPDATE) {
            return;
        }
        recomputeTotals();
    }

    // ================================================================
    // Header card
    // ================================================================

    private JPanel buildHeaderCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("লট নির্বাচন করুন ও হেডার তথ্য (Header)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel lotRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lotRow.add(new JLabel("Lot:"));
        lotSelector.setPreferredSize(new Dimension(320, 26));
        lotRow.add(lotSelector);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        lotRow.add(refreshBtn);

        JPanel readOnlyRow = new JPanel(new GridLayout(1, 4, 10, 4));
        readOnlyRow.add(labeled("Work Order No", workOrderValue));
        readOnlyRow.add(labeled("Lot No", lotNoValue));
        readOnlyRow.add(labeled("Item No", itemNoValue));
        readOnlyRow.add(labeled("Product Name", productNameValue));

        JPanel editableRow = new JPanel(new GridLayout(1, 3, 10, 4));
        editableRow.add(labeled("Order Qty", orderQtyField));
        editableRow.add(labeled("Exp Date (yyyy-MM-dd)", expiryDateField));
        editableRow.add(labeled("Ctns Qty in a Shipper", ctnsInShipperField));

        JButton saveHeaderBtn = new JButton("Save Header (Upsert)");
        saveHeaderBtn.addActionListener(e -> onSaveHeader());
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saveRow.add(saveHeaderBtn);

        JPanel middle = new JPanel();
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        readOnlyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        editableRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.add(readOnlyRow);
        middle.add(Box.createVerticalStrut(6));
        middle.add(editableRow);

        card.add(lotRow, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(saveRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(11f));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ================================================================
    // Grid card
    // ================================================================

    private JPanel buildGridCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("প্রতি শিফটের এন্ট্রি (Total No of Unit Produced স্বয়ংক্রিয়ভাবে হিসাব হবে)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JButton addRowBtn = new JButton("Add Row");
        addRowBtn.addActionListener(e -> addBlankRow());
        JButton deleteRowBtn = new JButton("Delete Selected Row");
        deleteRowBtn.addActionListener(e -> deleteSelectedRow());
        JButton saveRowsBtn = new JButton("Save Rows to Database");
        saveRowsBtn.addActionListener(e -> onSaveRows());
        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> onExport());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(addRowBtn);
        toolbar.add(deleteRowBtn);
        toolbar.add(saveRowsBtn);
        toolbar.add(exportBtn);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(new JScrollPane(shiftTable), BorderLayout.CENTER);
        card.add(totalsLabel, BorderLayout.SOUTH);
        return card;
    }

    // ================================================================
    // Data loading
    // ================================================================

    @Override
    public void refresh() {
        loadLotOptions();
        loadShiftAndEmployeeOptions();
    }

    private void loadLotOptions() {
        Object previouslySelected = lotSelector.getSelectedItem();
        lotSelector.removeAllItems();
        lotSelector.addItem(new ComboItem(null, PLACEHOLDER));

        TableDef lotDef = TableRegistry.TABLES.get("batch_lot");
        String expr = lotDef.displayExprFor("`batch_lot`");
        String sql = "SELECT lot_id AS id_val, (" + expr + ") AS label_val FROM `batch_lot` ORDER BY lot_id DESC LIMIT 500";

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lotSelector.addItem(new ComboItem(rs.getObject("id_val"), rs.getString("label_val")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "লটের তালিকা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        if (previouslySelected instanceof ComboItem) {
            lotSelector.setSelectedItem(previouslySelected);
        }
    }

    private void loadShiftAndEmployeeOptions() {
        shiftEditorCombo.removeAllItems();
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT shift_name FROM shift_master ORDER BY shift_name")) {
            while (rs.next()) {
                shiftEditorCombo.addItem(rs.getString("shift_name"));
            }
        } catch (SQLException ignored) {
            // dropdown just stays empty/editable; user can still type a value
        }

        employeeEditorCombo.removeAllItems();
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT employee_name FROM employee ORDER BY employee_name")) {
            while (rs.next()) {
                employeeEditorCombo.addItem(rs.getString("employee_name"));
            }
        } catch (SQLException ignored) {
            // same fallback as above
        }
    }

    private void loadForSelectedLot() {
        Long lotId = selectedLotId();
        clearHeaderFields();
        tableModel.setRowCount(0);
        if (lotId == null) {
            fillDefaultBlankRows();
            return;
        }

        try (Connection conn = Db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT bl.batch_number, bl.lot_number, pm.product_code, pm.product_name " +
                            "FROM batch_lot bl JOIN product_master pm ON bl.product_id = pm.product_id " +
                            "WHERE bl.lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        workOrderValue.setText(str(rs.getObject("batch_number")));
                        lotNoValue.setText(str(rs.getObject("lot_number")));
                        itemNoValue.setText(str(rs.getObject("product_code")));
                        productNameValue.setText(str(rs.getObject("product_name")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_qty, expiry_date, ctns_in_shipper FROM carton_tracking_header WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        orderQtyField.setText(str(rs.getObject("order_qty")));
                        expiryDateField.setText(str(rs.getObject("expiry_date")));
                        ctnsInShipperField.setText(str(rs.getObject("ctns_in_shipper")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT spl.log_date, sm.shift_name, spl.final_shipper_no, spl.no_of_shipper, " +
                            "spl.no_of_unit_produced, spl.total_no_of_unit_produced, e.employee_name " +
                            "FROM shift_production_log spl " +
                            "LEFT JOIN shift_master sm ON spl.shift_id = sm.shift_id " +
                            "LEFT JOIN employee e ON spl.recorded_by = e.employee_id " +
                            "WHERE spl.lot_id = ? ORDER BY spl.log_date, spl.entry_id")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    updating = true;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                                str(rs.getObject("log_date")),
                                str(rs.getObject("shift_name")),
                                str(rs.getObject("final_shipper_no")),
                                str(rs.getObject("no_of_shipper")),
                                str(rs.getObject("no_of_unit_produced")),
                                str(rs.getObject("total_no_of_unit_produced")),
                                str(rs.getObject("employee_name")),
                        });
                    }
                    updating = false;
                }
            }

            if (tableModel.getRowCount() == 0) {
                fillDefaultBlankRows();
            }
            recomputeTotals();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ডাটা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillDefaultBlankRows() {
        updating = true;
        for (int i = 0; i < DEFAULT_BLANK_ROWS; i++) {
            tableModel.addRow(new Object[]{"", "", "", "", "", "", ""});
        }
        updating = false;
    }

    private void clearHeaderFields() {
        workOrderValue.setText("-");
        lotNoValue.setText("-");
        itemNoValue.setText("-");
        productNameValue.setText("-");
        orderQtyField.setText("");
        expiryDateField.setText("");
        ctnsInShipperField.setText("");
        totalsLabel.setText(" ");
    }

    // ================================================================
    // Live "running total" recalculation
    // ================================================================

    private void recomputeTotals() {
        updating = true;
        try {
            int running = 0;
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                Integer units = parseInt((String) tableModel.getValueAt(r, COL_UNITS));
                if (units != null) {
                    running += units;
                    tableModel.setValueAt(String.valueOf(running), r, COL_TOTAL_UNITS);
                } else {
                    tableModel.setValueAt("", r, COL_TOTAL_UNITS);
                }
            }
            totalsLabel.setText(running > 0
                    ? ("সর্বমোট Total No of Unit Produced: " + running)
                    : " ");
        } finally {
            updating = false;
        }
    }

    // ================================================================
    // Row add/delete
    // ================================================================

    private void addBlankRow() {
        updating = true;
        tableModel.addRow(new Object[]{"", "", "", "", "", "", ""});
        updating = false;
    }

    private void deleteSelectedRow() {
        int row = shiftTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "প্রথমে একটি রো সিলেক্ট করুন।", "কিছু সিলেক্ট করা হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.removeRow(row);
        recomputeTotals();
    }

    // ================================================================
    // Save
    // ================================================================

    private void onSaveHeader() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            JOptionPane.showMessageDialog(this, "আগে একটি লট নির্বাচন করুন।", "লট নির্বাচিত হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO carton_tracking_header (lot_id, order_qty, expiry_date, ctns_in_shipper) " +
                "VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE order_qty=VALUES(order_qty), expiry_date=VALUES(expiry_date), " +
                "ctns_in_shipper=VALUES(ctns_in_shipper)";

        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lotId);
            setNullableInt(ps, 2, parseInt(orderQtyField.getText()));
            ps.setString(3, blankToNull(expiryDateField.getText()));
            setNullableInt(ps, 4, parseInt(ctnsInShipperField.getText()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "হেডার সংরক্ষিত হয়েছে।", "সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "হেডার সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSaveRows() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            JOptionPane.showMessageDialog(this, "আগে একটি লট নির্বাচন করুন।", "লট নির্বাচিত হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM shift_production_log WHERE lot_id = ?")) {
                    del.setLong(1, lotId);
                    del.executeUpdate();
                }

                String insertSql = "INSERT INTO shift_production_log " +
                        "(lot_id, log_date, shift_id, final_shipper_no, no_of_shipper, no_of_unit_produced, " +
                        "total_no_of_unit_produced, recorded_by) VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (int r = 0; r < tableModel.getRowCount(); r++) {
                        String date = blankToNull((String) tableModel.getValueAt(r, COL_DATE));
                        if (date == null) {
                            continue; // skip rows with no date entered
                        }
                        ins.setLong(1, lotId);
                        ins.setString(2, date);
                        setNullableInt(ins, 3, resolveShiftId(conn, (String) tableModel.getValueAt(r, COL_SHIFT)));
                        setNullableInt(ins, 4, parseInt((String) tableModel.getValueAt(r, COL_FINAL_SHIPPER)));
                        setNullableInt(ins, 5, parseInt((String) tableModel.getValueAt(r, COL_NO_OF_SHIPPER)));
                        setNullableInt(ins, 6, parseInt((String) tableModel.getValueAt(r, COL_UNITS)));
                        setNullableInt(ins, 7, parseInt((String) tableModel.getValueAt(r, COL_TOTAL_UNITS)));
                        setNullableInt(ins, 8, resolveEmployeeId(conn, (String) tableModel.getValueAt(r, COL_RECORDED_BY)));
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "সব রো সংরক্ষিত হয়েছে।", "সফল", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "রো সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("shift_production_log.xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".xlsx")) {
            path = path + ".xlsx";
        }

        List<String> headers = new ArrayList<>();
        for (String c : COLUMNS) {
            headers.add(c);
        }
        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object[] row = new Object[COLUMNS.length];
            for (int c = 0; c < COLUMNS.length; c++) {
                row[c] = tableModel.getValueAt(r, c);
            }
            rows.add(row);
        }

        try {
            ExcelExportUtil.export(path, "shift_production_log", headers, rows);
            JOptionPane.showMessageDialog(this, "Excel ফাইল সংরক্ষিত হয়েছে:\n" + path,
                    "Export সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export ব্যর্থ হয়েছে:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // FK resolution — accepts either a numeric ID or a human label
    // (shift_name / employee_name), same fallback style used elsewhere
    // in the app (e.g. FieldValueConverter).
    // ================================================================

    private Integer resolveShiftId(Connection conn, String text) throws SQLException {
        String v = blankToNull(text);
        if (v == null) {
            return null;
        }
        Integer asId = parseInt(v);
        if (asId != null) {
            return asId;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT shift_id FROM shift_master WHERE shift_name = ? LIMIT 1")) {
            ps.setString(1, v);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private Integer resolveEmployeeId(Connection conn, String text) throws SQLException {
        String v = blankToNull(text);
        if (v == null) {
            return null;
        }
        Integer asId = parseInt(v);
        if (asId != null) {
            return asId;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT employee_id FROM employee WHERE employee_name = ? LIMIT 1")) {
            ps.setString(1, v);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // ================================================================
    // Small helpers
    // ================================================================

    private Long selectedLotId() {
        Object selected = lotSelector.getSelectedItem();
        if (!(selected instanceof ComboItem)) {
            return null;
        }
        Object id = ((ComboItem) selected).id;
        if (id == null) {
            return null;
        }
        return Long.valueOf(id.toString());
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static Integer parseInt(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /** Wraps a foreign-key id with its human-readable label for display in a JComboBox. */
    private static class ComboItem {
        final Object id;
        final String label;

        ComboItem(Object id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
