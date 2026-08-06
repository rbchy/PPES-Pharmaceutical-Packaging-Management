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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated "Carton Tracking Log" form — a Java port of Order_Carton_Tracking_Sheet.xlsx.
 * Unlike the generic CrudPanel, this recomputes dependent cells live as you type,
 * the same way the original spreadsheet's formulas did:
 *
 *   Standard Ctns   = Shift Standard Ctns * Hour / 8
 *   Total Shippers  = Label No After - Starting Label No
 *   Actual Ctns     = Ctns in Shipper * Total Shippers
 *   Percent         = Actual Ctns / Standard Ctns * 100
 *
 * Data is stored in two tables: carton_tracking_header (one row per lot) and
 * carton_tracking_hourly (one row per checkpoint, e.g. every 2 hours).
 */
public class CartonTrackingPanel extends JPanel implements Refreshable {

    private static final String PLACEHOLDER = "-- লট নির্বাচন করুন --";
    private static final String[] COLUMNS = {
            "Hour", "Standard Ctns", "Label No After", "Total Shippers", "Actual Ctns", "Percent", "Comments"
    };

    private final JComboBox<ComboItem> lotSelector = new JComboBox<>();
    private final JTextField roomNoField = new JTextField(10);
    private final JTextField expiryDateField = new JTextField(10);
    private final JTextField orderQtyField = new JTextField(10);
    private final JTextField todaysBalanceField = new JTextField(10);
    private final JTextField startingLabelField = new JTextField(10);
    private final JTextField ctnsInShipperField = new JTextField(10);
    private final JTextField shiftStandardField = new JTextField(10);
    private final JLabel totalsLabel = new JLabel(" ");

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 0 || column == 2 || column == 6; // Hour, Label No After, Comments
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class; // keep everything as text; we parse/format ourselves
        }
    };
    private final JTable hourlyTable = new JTable(tableModel);
    private boolean updating = false; // guards against recursive recompute during programmatic updates

    public CartonTrackingPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        add(new BannerPanel("/images/banner_carton_tracking.jpg", "Carton Tracking Log",
                "Hourly shipper & carton checkpoint tracking per lot", 110), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(buildHeaderCard(), BorderLayout.NORTH);
        body.add(buildHourlyCard(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        hourlyTable.getModel().addTableModelListener(this::onTableChanged);

        lotSelector.addActionListener(e -> loadForSelectedLot());
    }

    // A tiny helper so the anonymous listener reads clearly above.
    private void onTableChanged(javax.swing.event.TableModelEvent e) {
        if (updating || e.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int col = e.getColumn();
        int row = e.getFirstRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        if (col == 0 || col == 2 || col == TableModelEvent.ALL_COLUMNS) {
            recomputeRow(row);
            updateTotals();
        }
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
        JButton refreshLotsBtn = new JButton("Refresh Lots");
        refreshLotsBtn.addActionListener(e -> loadLotOptions());
        lotRow.add(refreshLotsBtn);

        JPanel fields = new JPanel(new GridLayout(2, 7, 8, 4));
        fields.add(labeled("Room No", roomNoField));
        fields.add(labeled("Expiry Date", expiryDateField));
        fields.add(labeled("Order Qty", orderQtyField));
        fields.add(labeled("Today's Balance", todaysBalanceField));
        fields.add(labeled("Starting Label No", startingLabelField));
        fields.add(labeled("Ctns in Shipper", ctnsInShipperField));
        fields.add(labeled("Shift Standard Ctns (8-hr)", shiftStandardField));

        // Recompute every row whenever a header value that feeds the formulas changes.
        for (JTextField f : new JTextField[]{startingLabelField, ctnsInShipperField, shiftStandardField}) {
            f.addActionListener(e -> recomputeAllRows());
            f.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    recomputeAllRows();
                }
            });
        }

        JButton saveHeaderBtn = new JButton("Save Header (Upsert)");
        saveHeaderBtn.addActionListener(e -> onSaveHeader());
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saveRow.add(saveHeaderBtn);

        card.add(lotRow, BorderLayout.NORTH);
        card.add(fields, BorderLayout.CENTER);
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
    // Hourly grid card
    // ================================================================

    private JPanel buildHourlyCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Hourly Checkpoints (স্বয়ংক্রিয়ভাবে হিসাব হবে)"),
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
        card.add(new JScrollPane(hourlyTable), BorderLayout.CENTER);
        card.add(totalsLabel, BorderLayout.SOUTH);
        return card;
    }

    // ================================================================
    // Data loading
    // ================================================================

    @Override
    public void refresh() {
        loadLotOptions();
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

    private void loadForSelectedLot() {
        Long lotId = selectedLotId();
        clearHeaderFields();
        tableModel.setRowCount(0);
        if (lotId == null) {
            return;
        }

        try (Connection conn = Db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT room_no, expiry_date, order_qty, todays_balance, starting_label_no, " +
                            "ctns_in_shipper, shift_standard_ctns FROM carton_tracking_header WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        roomNoField.setText(str(rs.getObject("room_no")));
                        expiryDateField.setText(str(rs.getObject("expiry_date")));
                        orderQtyField.setText(str(rs.getObject("order_qty")));
                        todaysBalanceField.setText(str(rs.getObject("todays_balance")));
                        startingLabelField.setText(str(rs.getObject("starting_label_no")));
                        ctnsInShipperField.setText(str(rs.getObject("ctns_in_shipper")));
                        shiftStandardField.setText(str(rs.getObject("shift_standard_ctns")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT hour_mark, standard_ctns, label_no_after, total_shippers, actual_ctns, " +
                            "percent_complete, comments FROM carton_tracking_hourly WHERE lot_id = ? ORDER BY hour_mark")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    updating = true;
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                                str(rs.getObject("hour_mark")),
                                str(rs.getObject("standard_ctns")),
                                str(rs.getObject("label_no_after")),
                                str(rs.getObject("total_shippers")),
                                str(rs.getObject("actual_ctns")),
                                formatPercent(rs.getObject("percent_complete")),
                                str(rs.getObject("comments")),
                        });
                    }
                    updating = false;
                }
            }

            if (tableModel.getRowCount() == 0) {
                updating = true;
                for (int h = 2; h <= 8; h += 2) {
                    tableModel.addRow(new Object[]{String.valueOf(h), "", "", "", "", "", ""});
                }
                updating = false;
            }
            recomputeAllRows();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ডাটা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearHeaderFields() {
        roomNoField.setText("");
        expiryDateField.setText("");
        orderQtyField.setText("");
        todaysBalanceField.setText("");
        startingLabelField.setText("");
        ctnsInShipperField.setText("");
        shiftStandardField.setText("");
        totalsLabel.setText(" ");
    }

    // ================================================================
    // Live "formula" recalculation
    // ================================================================

    private void recomputeAllRows() {
        updating = true;
        try {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                recomputeRowInternal(r);
            }
        } finally {
            updating = false;
        }
        updateTotals();
    }

    private void recomputeRow(int row) {
        updating = true;
        try {
            recomputeRowInternal(row);
        } finally {
            updating = false;
        }
    }

    private void recomputeRowInternal(int row) {
        Integer hour = parseInt((String) tableModel.getValueAt(row, 0));
        Integer labelAfter = parseInt((String) tableModel.getValueAt(row, 2));
        Integer startingLabel = parseInt(startingLabelField.getText());
        Integer ctnsInShipper = parseInt(ctnsInShipperField.getText());
        Integer shiftStandard = parseInt(shiftStandardField.getText());

        BigDecimal standardCtns = null;
        if (hour != null && shiftStandard != null) {
            standardCtns = BigDecimal.valueOf(shiftStandard)
                    .multiply(BigDecimal.valueOf(hour))
                    .divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
        }

        Integer totalShippers = null;
        if (labelAfter != null && startingLabel != null) {
            totalShippers = labelAfter - startingLabel;
        }

        Integer actualCtns = null;
        if (totalShippers != null && ctnsInShipper != null) {
            actualCtns = ctnsInShipper * totalShippers;
        }

        String percentStr = "";
        if (actualCtns != null && standardCtns != null && standardCtns.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percent = BigDecimal.valueOf(actualCtns)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(standardCtns, 2, RoundingMode.HALF_UP);
            percentStr = percent.toPlainString() + "%";
        }

        tableModel.setValueAt(standardCtns == null ? "" : standardCtns.toPlainString(), row, 1);
        tableModel.setValueAt(totalShippers == null ? "" : totalShippers.toString(), row, 3);
        tableModel.setValueAt(actualCtns == null ? "" : actualCtns.toString(), row, 4);
        tableModel.setValueAt(percentStr, row, 5);
    }

    private void updateTotals() {
        int totalShippers = 0;
        boolean any = false;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Integer v = parseInt((String) tableModel.getValueAt(r, 3));
            if (v != null) {
                totalShippers += v;
                any = true;
            }
        }
        totalsLabel.setText(any ? ("সর্বমোট Total Shippers: " + totalShippers) : " ");
    }

    // ================================================================
    // Row add/delete
    // ================================================================

    private void addBlankRow() {
        int lastHour = 0;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Integer h = parseInt((String) tableModel.getValueAt(r, 0));
            if (h != null && h > lastHour) {
                lastHour = h;
            }
        }
        updating = true;
        tableModel.addRow(new Object[]{String.valueOf(lastHour + 2), "", "", "", "", "", ""});
        updating = false;
        recomputeRow(tableModel.getRowCount() - 1);
    }

    private void deleteSelectedRow() {
        int row = hourlyTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "প্রথমে একটি রো সিলেক্ট করুন।", "কিছু সিলেক্ট করা হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.removeRow(row);
        updateTotals();
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

        String sql = "INSERT INTO carton_tracking_header " +
                "(lot_id, room_no, expiry_date, order_qty, todays_balance, starting_label_no, ctns_in_shipper, shift_standard_ctns) " +
                "VALUES (?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE room_no=VALUES(room_no), expiry_date=VALUES(expiry_date), " +
                "order_qty=VALUES(order_qty), todays_balance=VALUES(todays_balance), " +
                "starting_label_no=VALUES(starting_label_no), ctns_in_shipper=VALUES(ctns_in_shipper), " +
                "shift_standard_ctns=VALUES(shift_standard_ctns)";

        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lotId);
            ps.setString(2, blankToNull(roomNoField.getText()));
            ps.setString(3, blankToNull(expiryDateField.getText()));
            setNullableInt(ps, 4, parseInt(orderQtyField.getText()));
            setNullableInt(ps, 5, parseInt(todaysBalanceField.getText()));
            setNullableInt(ps, 6, parseInt(startingLabelField.getText()));
            setNullableInt(ps, 7, parseInt(ctnsInShipperField.getText()));
            setNullableInt(ps, 8, parseInt(shiftStandardField.getText()));
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
                        "DELETE FROM carton_tracking_hourly WHERE lot_id = ?")) {
                    del.setLong(1, lotId);
                    del.executeUpdate();
                }

                String insertSql = "INSERT INTO carton_tracking_hourly " +
                        "(lot_id, hour_mark, standard_ctns, label_no_after, total_shippers, actual_ctns, percent_complete, comments) " +
                        "VALUES (?,?,?,?,?,?,?,?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (int r = 0; r < tableModel.getRowCount(); r++) {
                        Integer hour = parseInt((String) tableModel.getValueAt(r, 0));
                        if (hour == null) {
                            continue; // skip rows with no hour entered
                        }
                        ins.setLong(1, lotId);
                        ins.setInt(2, hour);
                        setNullableBigDecimal(ins, 3, parseDecimal((String) tableModel.getValueAt(r, 1)));
                        setNullableInt(ins, 4, parseInt((String) tableModel.getValueAt(r, 2)));
                        setNullableInt(ins, 5, parseInt((String) tableModel.getValueAt(r, 3)));
                        setNullableInt(ins, 6, parseInt((String) tableModel.getValueAt(r, 4)));
                        setNullableBigDecimal(ins, 7, parsePercentDecimal((String) tableModel.getValueAt(r, 5)));
                        ins.setString(8, blankToNull((String) tableModel.getValueAt(r, 6)));
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
        chooser.setSelectedFile(new File("carton_tracking.xlsx"));
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
            ExcelExportUtil.export(path, "carton_tracking", headers, rows);
            JOptionPane.showMessageDialog(this, "Excel ফাইল সংরক্ষিত হয়েছে:\n" + path,
                    "Export সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export ব্যর্থ হয়েছে:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
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
            return (int) Double.parseDouble(v); // tolerate "2640.0"-style text too
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parsePercentDecimal(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        String v = s.trim().replace("%", "");
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatPercent(Object dbValue) {
        if (dbValue == null) {
            return "";
        }
        return dbValue.toString() + "%";
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, value);
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
