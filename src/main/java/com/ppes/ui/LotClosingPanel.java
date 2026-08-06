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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Lot Closing (Finish Production)" — closes out a lot against its work
 * order once blistering is done, considering that the actual good + reject
 * blister count (not the order qty itself) is what tells you production is
 * finished. At closing time you reconcile the packaging materials issued
 * for the lot: unused empty cartons, empty shippers, leaflets (by type),
 * and labels get counted and returned to store; reject blister qty and any
 * loose/unpacked product found are logged too, per your MBR/MPR.
 *
 * Nothing is duplicated:
 *   - Good/Reject blister totals are read live from blister_production.
 *   - Required blister qty for comparison comes from work_order_packing_spec
 *     (order_qty_cartons * blisters_per_carton), if filled in for the lot.
 *   - Packaging material returns reuse the EXISTING reconciliation table.
 *   - Closing a lot sets batch_lot.status = 'Closed'.
 */
public class LotClosingPanel extends JPanel implements Refreshable {

    private static final String PLACEHOLDER = "-- লট নির্বাচন করুন --";
    private static final String[] COLUMNS = {
            "Material", "Issued Qty", "Used Qty", "Waste/Reject Qty", "Returned to Store"
    };
    private static final int COL_MATERIAL = 0;
    private static final int COL_ISSUED = 1;
    private static final int COL_USED = 2;
    private static final int COL_WASTE = 3;
    private static final int COL_RETURNED = 4;

    private final JComboBox<ComboItem> lotSelector = new JComboBox<>();
    private final JLabel workOrderValue = new JLabel("-");
    private final JLabel lotNoValue = new JLabel("-");
    private final JLabel productNameValue = new JLabel("-");
    private final JLabel expiryDateValue = new JLabel("-");

    private final JLabel requiredBlistersValue = new JLabel("-");
    private final JLabel goodBlistersValue = new JLabel("-");
    private final JLabel rejectBlistersValue = new JLabel("-");
    private final JLabel productionStatusValue = new JLabel("-");

    private final JTextField closingDateField = new JTextField(10);
    private final JTextField mbrNoField = new JTextField(12);
    private final JTextField mprNoField = new JTextField(12);
    private final JTextField looseProductQtyField = new JTextField(8);
    private final JComboBox<String> closedByCombo = new JComboBox<>();
    private final JTextArea remarksArea = new JTextArea(3, 20);
    private final JLabel closingStatusValue = new JLabel("Draft");

    private final JComboBox<String> materialEditorCombo = new JComboBox<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == COL_ISSUED || column == COL_USED || column == COL_WASTE;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };
    private final JTable materialTable = new JTable(tableModel);
    private final List<Integer> rowMaterialIds = new ArrayList<>(); // parallel to tableModel rows
    private final Map<String, Integer> materialNameToId = new LinkedHashMap<>();
    private boolean updating = false;

    public LotClosingPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        add(new BannerPanel("/images/banner_lot_closing.jpg", "Lot Closing (Finish Production)",
                "Good/Reject blister check, MBR/MPR closing details & packaging material returns", 110),
                BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(buildTopCard(), BorderLayout.NORTH);
        body.add(buildMaterialCard(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        materialTable.getColumnModel().getColumn(COL_MATERIAL).setCellEditor(null);
        materialTable.getModel().addTableModelListener(this::onTableChanged);
        lotSelector.addActionListener(e -> loadForSelectedLot());
        closedByCombo.setEditable(true);
    }

    private void onTableChanged(TableModelEvent e) {
        if (updating || e.getType() != TableModelEvent.UPDATE) {
            return;
        }
        int col = e.getColumn();
        if (col == COL_ISSUED || col == COL_USED || col == COL_WASTE || col == TableModelEvent.ALL_COLUMNS) {
            recomputeReturns();
        }
    }

    // ================================================================
    // Top card — lot info + production summary + closing details
    // ================================================================

    private JPanel buildTopCard() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));

        JPanel lotCard = new JPanel(new BorderLayout(0, 8));
        lotCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("লট / ওয়ার্ক অর্ডার নির্বাচন করুন"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel lotRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lotRow.add(new JLabel("Lot:"));
        lotSelector.setPreferredSize(new Dimension(320, 26));
        lotRow.add(lotSelector);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        lotRow.add(refreshBtn);

        JPanel infoRow = new JPanel(new GridLayout(1, 4, 10, 4));
        infoRow.add(labeled("Work Order No", workOrderValue));
        infoRow.add(labeled("Lot No", lotNoValue));
        infoRow.add(labeled("Product Name", productNameValue));
        infoRow.add(labeled("Expiry Date", expiryDateValue));

        lotCard.add(lotRow, BorderLayout.NORTH);
        lotCard.add(infoRow, BorderLayout.CENTER);

        JPanel summaryCard = new JPanel(new BorderLayout(0, 8));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("প্রোডাকশন সামারি (Blister Production থেকে লাইভ)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JPanel summaryRow = new JPanel(new GridLayout(1, 4, 10, 4));
        summaryRow.add(labeled("Required Blisters (Order অনুযায়ী)", requiredBlistersValue));
        summaryRow.add(labeled("Good Blisters (উৎপাদিত)", goodBlistersValue));
        summaryRow.add(labeled("Reject Blisters", rejectBlistersValue));
        summaryRow.add(labeled("Production Status", productionStatusValue));
        JButton refreshSummaryBtn = new JButton("Refresh Production Summary");
        refreshSummaryBtn.addActionListener(e -> loadProductionSummary());
        JPanel summaryBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryBtnRow.add(refreshSummaryBtn);
        summaryCard.add(summaryRow, BorderLayout.CENTER);
        summaryCard.add(summaryBtnRow, BorderLayout.SOUTH);

        JPanel closingCard = new JPanel(new BorderLayout(0, 8));
        closingCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("ক্লোজিং তথ্য (MBR / MPR অনুযায়ী)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel closingRow1 = new JPanel(new GridLayout(1, 5, 8, 4));
        closingRow1.add(labeled("Closing Date (yyyy-MM-dd)", closingDateField));
        closingRow1.add(labeled("MBR No", mbrNoField));
        closingRow1.add(labeled("MPR No", mprNoField));
        closingRow1.add(labeled("Loose Product Qty", looseProductQtyField));
        closingRow1.add(labeled("Closed By", closedByCombo));

        JPanel remarksRow = new JPanel(new BorderLayout(0, 2));
        JLabel remarksLabel = new JLabel("Remarks");
        remarksLabel.setFont(remarksLabel.getFont().deriveFont(11f));
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        remarksRow.add(remarksLabel, BorderLayout.NORTH);
        remarksRow.add(new JScrollPane(remarksArea), BorderLayout.CENTER);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.add(new JLabel("বর্তমান স্ট্যাটাস:"));
        closingStatusValue.setFont(closingStatusValue.getFont().deriveFont(Font.BOLD));
        statusRow.add(closingStatusValue);

        JButton saveClosingBtn = new JButton("Save Closing Details");
        saveClosingBtn.addActionListener(e -> onSaveClosingDetails());
        JButton closeLotBtn = new JButton("Mark Lot as Closed");
        closeLotBtn.addActionListener(e -> onMarkLotClosed());
        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsRow.add(saveClosingBtn);
        buttonsRow.add(closeLotBtn);

        JPanel closingMiddle = new JPanel();
        closingMiddle.setLayout(new BoxLayout(closingMiddle, BoxLayout.Y_AXIS));
        closingRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        remarksRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        closingMiddle.add(closingRow1);
        closingMiddle.add(Box.createVerticalStrut(6));
        closingMiddle.add(remarksRow);
        closingMiddle.add(Box.createVerticalStrut(4));
        closingMiddle.add(statusRow);

        closingCard.add(closingMiddle, BorderLayout.CENTER);
        closingCard.add(buttonsRow, BorderLayout.SOUTH);

        outer.add(lotCard);
        outer.add(Box.createVerticalStrut(10));
        outer.add(summaryCard);
        outer.add(Box.createVerticalStrut(10));
        outer.add(closingCard);
        return outer;
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
    // Material return grid card
    // ================================================================

    private JPanel buildMaterialCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        "প্যাকেজিং মেটেরিয়াল রিটার্ন (খালি কার্টন, খালি শিপার, লিফলেট, লেবেল ইত্যাদি স্টোরে ফেরত)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JButton addRowBtn = new JButton("Add Material Row");
        addRowBtn.addActionListener(e -> onAddMaterialRow());
        JButton deleteRowBtn = new JButton("Delete Selected Row");
        deleteRowBtn.addActionListener(e -> deleteSelectedRow());
        JButton saveBtn = new JButton("Save Material Returns");
        saveBtn.addActionListener(e -> onSaveMaterialReturns());
        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> onExport());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(addRowBtn);
        toolbar.add(deleteRowBtn);
        toolbar.add(saveBtn);
        toolbar.add(exportBtn);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(new JScrollPane(materialTable), BorderLayout.CENTER);
        return card;
    }

    // ================================================================
    // Data loading
    // ================================================================

    @Override
    public void refresh() {
        loadLotOptions();
        loadEmployeeOptions();
        loadMaterialOptions();
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

    private void loadEmployeeOptions() {
        closedByCombo.removeAllItems();
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT employee_name FROM employee ORDER BY employee_name")) {
            while (rs.next()) {
                closedByCombo.addItem(rs.getString("employee_name"));
            }
        } catch (SQLException ignored) {
            // dropdown just stays empty/editable
        }
    }

    private void loadMaterialOptions() {
        materialNameToId.clear();
        materialEditorCombo.removeAllItems();
        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT material_id, material_name FROM raw_material_master ORDER BY material_name")) {
            while (rs.next()) {
                String name = rs.getString("material_name");
                materialNameToId.put(name, rs.getInt("material_id"));
                materialEditorCombo.addItem(name);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "কাঁচামালের তালিকা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForSelectedLot() {
        Long lotId = selectedLotId();
        clearFields();
        tableModel.setRowCount(0);
        rowMaterialIds.clear();
        if (lotId == null) {
            return;
        }

        try (Connection conn = Db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT bl.batch_number, bl.lot_number, pm.product_name " +
                            "FROM batch_lot bl JOIN product_master pm ON bl.product_id = pm.product_id " +
                            "WHERE bl.lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        workOrderValue.setText(str(rs.getObject("batch_number")));
                        lotNoValue.setText(str(rs.getObject("lot_number")));
                        productNameValue.setText(str(rs.getObject("product_name")));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT expiry_date FROM carton_tracking_header WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getObject("expiry_date") != null) {
                        expiryDateValue.setText(str(rs.getObject("expiry_date")));
                    } else {
                        expiryDateValue.setText("সেট করা হয়নি (Carton Tracking Log থেকে দিন)");
                    }
                }
            } catch (SQLException ignored) {
                expiryDateValue.setText("N/A");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT closing_date, mbr_no, mpr_no, loose_product_qty, remarks, status, " +
                            "e.employee_name FROM lot_closing lc LEFT JOIN employee e ON lc.closed_by = e.employee_id " +
                            "WHERE lc.lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        closingDateField.setText(str(rs.getObject("closing_date")));
                        mbrNoField.setText(str(rs.getObject("mbr_no")));
                        mprNoField.setText(str(rs.getObject("mpr_no")));
                        looseProductQtyField.setText(str(rs.getObject("loose_product_qty")));
                        remarksArea.setText(str(rs.getObject("remarks")));
                        closingStatusValue.setText(str(rs.getObject("status")));
                        String recordedBy = rs.getString("employee_name");
                        if (recordedBy != null) {
                            closedByCombo.setSelectedItem(recordedBy);
                        }
                    }
                }
            }

            loadMaterialRows(conn, lotId);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ডাটা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        loadProductionSummary();
    }

    private void loadMaterialRows(Connection conn, Long lotId) throws SQLException {
        // Union of: materials already reconciled for this lot, materials actually
        // issued for this lot (raw_material_receiving), and the standard packaging
        // item types — so the grid is useful even before anything else is filled in.
        Map<Integer, String> namesById = new LinkedHashMap<>();
        Map<Integer, BigDecimal> issuedById = new LinkedHashMap<>();
        Map<Integer, BigDecimal[]> reconciledById = new LinkedHashMap<>(); // [received, used, waste]

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT rmr.material_id, rm.material_name, SUM(rmr.received_qty) AS total_issued " +
                        "FROM raw_material_receiving rmr JOIN raw_material_master rm ON rmr.material_id = rm.material_id " +
                        "WHERE rmr.lot_id = ? GROUP BY rmr.material_id, rm.material_name")) {
            ps.setLong(1, lotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("material_id");
                    namesById.put(id, rs.getString("material_name"));
                    issuedById.put(id, rs.getBigDecimal("total_issued"));
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT r.material_id, rm.material_name, r.received_qty, r.used_qty, r.waste_qty " +
                        "FROM reconciliation r JOIN raw_material_master rm ON r.material_id = rm.material_id " +
                        "WHERE r.lot_id = ?")) {
            ps.setLong(1, lotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("material_id");
                    namesById.put(id, rs.getString("material_name"));
                    reconciledById.put(id, new BigDecimal[]{
                            rs.getBigDecimal("received_qty"), rs.getBigDecimal("used_qty"), rs.getBigDecimal("waste_qty")
                    });
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT material_id, material_name FROM raw_material_master " +
                        "WHERE material_name IN ('Empty Carton','Empty Shipper','Shipper Label','Carton Label') " +
                        "OR material_name LIKE 'Leaflet%'")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    namesById.putIfAbsent(rs.getInt("material_id"), rs.getString("material_name"));
                }
            }
        }

        updating = true;
        for (Map.Entry<Integer, String> entry : namesById.entrySet()) {
            int materialId = entry.getKey();
            String name = entry.getValue();
            BigDecimal[] rec = reconciledById.get(materialId);
            String issued;
            String used;
            String waste;
            if (rec != null) {
                issued = str(rec[0]);
                used = str(rec[1]);
                waste = str(rec[2]);
            } else {
                BigDecimal issuedSum = issuedById.get(materialId);
                issued = issuedSum == null ? "" : issuedSum.toPlainString();
                used = "";
                waste = "";
            }
            tableModel.addRow(new Object[]{name, issued, used, waste, ""});
            rowMaterialIds.add(materialId);
        }
        updating = false;
        recomputeReturns();
    }

    private void loadProductionSummary() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            requiredBlistersValue.setText("-");
            goodBlistersValue.setText("-");
            rejectBlistersValue.setText("-");
            productionStatusValue.setText("-");
            return;
        }

        try (Connection conn = Db.getConnection()) {
            long goodQty = 0;
            long rejectQty = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(good_qty),0) AS good, COALESCE(SUM(reject_qty),0) AS reject " +
                            "FROM blister_production WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        goodQty = rs.getLong("good");
                        rejectQty = rs.getLong("reject");
                    }
                }
            }

            Long requiredBlisters = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_qty_cartons, blisters_per_carton FROM work_order_packing_spec WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long orderQty = rs.getLong("order_qty_cartons");
                        long blistersPerCarton = rs.getLong("blisters_per_carton");
                        requiredBlisters = orderQty * blistersPerCarton;
                    }
                }
            } catch (SQLException ignored) {
                // work_order_packing_spec schema not run yet — leave requiredBlisters null
            }

            goodBlistersValue.setText(String.format("%,d", goodQty));
            rejectBlistersValue.setText(String.format("%,d", rejectQty));

            if (requiredBlisters == null) {
                requiredBlistersValue.setText("N/A (Work Order Packaging স্ক্রিনে সেট করুন)");
                productionStatusValue.setText("N/A");
            } else {
                requiredBlistersValue.setText(String.format("%,d", requiredBlisters));
                long produced = goodQty + rejectQty;
                if (produced >= requiredBlisters) {
                    productionStatusValue.setText("সম্পন্ন (Complete)");
                } else {
                    productionStatusValue.setText(String.format("চলমান — বাকি %,d ব্লিস্টার", requiredBlisters - produced));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "প্রোডাকশন সামারি লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        workOrderValue.setText("-");
        lotNoValue.setText("-");
        productNameValue.setText("-");
        expiryDateValue.setText("-");
        requiredBlistersValue.setText("-");
        goodBlistersValue.setText("-");
        rejectBlistersValue.setText("-");
        productionStatusValue.setText("-");
        closingDateField.setText("");
        mbrNoField.setText("");
        mprNoField.setText("");
        looseProductQtyField.setText("");
        remarksArea.setText("");
        closingStatusValue.setText("Draft");
    }

    // ================================================================
    // Live recompute — Returned to Store = Issued - Used - Waste
    // ================================================================

    private void recomputeReturns() {
        updating = true;
        try {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                BigDecimal issued = parseDecimal((String) tableModel.getValueAt(r, COL_ISSUED));
                BigDecimal used = parseDecimal((String) tableModel.getValueAt(r, COL_USED));
                BigDecimal waste = parseDecimal((String) tableModel.getValueAt(r, COL_WASTE));
                if (issued == null) {
                    tableModel.setValueAt("", r, COL_RETURNED);
                    continue;
                }
                BigDecimal returned = issued
                        .subtract(used == null ? BigDecimal.ZERO : used)
                        .subtract(waste == null ? BigDecimal.ZERO : waste);
                if (returned.signum() < 0) {
                    returned = BigDecimal.ZERO;
                }
                tableModel.setValueAt(returned.toPlainString(), r, COL_RETURNED);
            }
        } finally {
            updating = false;
        }
    }

    // ================================================================
    // Row add/delete
    // ================================================================

    private void onAddMaterialRow() {
        if (materialEditorCombo.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "কোনো Raw Material পাওয়া যায়নি। আগে Masters > Raw Material Master এ যোগ করুন.",
                    "কিছু পাওয়া যায়নি", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int result = JOptionPane.showConfirmDialog(this, materialEditorCombo,
                "কোন মেটেরিয়াল যোগ করবেন?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String name = (String) materialEditorCombo.getSelectedItem();
        if (name == null) {
            return;
        }
        Integer materialId = materialNameToId.get(name);
        if (materialId == null) {
            return;
        }
        if (rowMaterialIds.contains(materialId)) {
            JOptionPane.showMessageDialog(this, "এই মেটেরিয়াল ইতিমধ্যে তালিকায় আছে।", "ডুপ্লিকেট",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        updating = true;
        tableModel.addRow(new Object[]{name, "", "", "", ""});
        rowMaterialIds.add(materialId);
        updating = false;
    }

    private void deleteSelectedRow() {
        int row = materialTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "প্রথমে একটি রো সিলেক্ট করুন।", "কিছু সিলেক্ট করা হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        tableModel.removeRow(row);
        rowMaterialIds.remove(row);
    }

    // ================================================================
    // Save actions
    // ================================================================

    private void onSaveClosingDetails() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            JOptionPane.showMessageDialog(this, "আগে একটি লট নির্বাচন করুন।", "লট নির্বাচিত হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = Db.getConnection()) {
            long goodQty = 0;
            long rejectQty = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(good_qty),0) AS good, COALESCE(SUM(reject_qty),0) AS reject " +
                            "FROM blister_production WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        goodQty = rs.getLong("good");
                        rejectQty = rs.getLong("reject");
                    }
                }
            }

            Integer orderQtyCartons = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT order_qty_cartons FROM work_order_packing_spec WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        orderQtyCartons = rs.getInt("order_qty_cartons");
                    }
                }
            } catch (SQLException ignored) {
                // spec table may not exist yet
            }

            // Note: `status` is deliberately left out of this INSERT/UPDATE — it
            // defaults to 'Draft' on first insert and is only ever changed by the
            // separate "Mark Lot as Closed" action, never overwritten here.
            String sql = "INSERT INTO lot_closing " +
                    "(lot_id, closing_date, mbr_no, mpr_no, order_qty_cartons, total_good_blister_qty, " +
                    "total_reject_blister_qty, loose_product_qty, remarks, closed_by) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE closing_date=VALUES(closing_date), mbr_no=VALUES(mbr_no), " +
                    "mpr_no=VALUES(mpr_no), order_qty_cartons=VALUES(order_qty_cartons), " +
                    "total_good_blister_qty=VALUES(total_good_blister_qty), " +
                    "total_reject_blister_qty=VALUES(total_reject_blister_qty), " +
                    "loose_product_qty=VALUES(loose_product_qty), remarks=VALUES(remarks), " +
                    "closed_by=VALUES(closed_by)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, lotId);
                ps.setString(2, blankToNull(closingDateField.getText()));
                ps.setString(3, blankToNull(mbrNoField.getText()));
                ps.setString(4, blankToNull(mprNoField.getText()));
                setNullableInt(ps, 5, orderQtyCartons);
                ps.setLong(6, goodQty);
                ps.setLong(7, rejectQty);
                setNullableInt(ps, 8, parseInt(looseProductQtyField.getText()));
                ps.setString(9, blankToNull(remarksArea.getText()));
                setNullableInt(ps, 10, resolveEmployeeId(conn, (String) closedByCombo.getSelectedItem()));
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "ক্লোজিং তথ্য সংরক্ষিত হয়েছে।", "সফল", JOptionPane.INFORMATION_MESSAGE);
            loadForSelectedLot();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onMarkLotClosed() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            JOptionPane.showMessageDialog(this, "আগে একটি লট নির্বাচন করুন।", "লট নির্বাচিত হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "এই লটটি Closed হিসেবে মার্ক করতে চান? এটি Batch/Lot স্ট্যাটাসও 'Closed' করে দেবে।",
                "নিশ্চিত করুন", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE lot_closing SET status = 'Closed' WHERE lot_id = ?")) {
                    ps.setLong(1, lotId);
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        // no closing row saved yet — create a minimal one so status is tracked
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO lot_closing (lot_id, status) VALUES (?, 'Closed')")) {
                            ins.setLong(1, lotId);
                            ins.executeUpdate();
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE batch_lot SET status = 'Closed' WHERE lot_id = ?")) {
                    ps.setLong(1, lotId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            }
            JOptionPane.showMessageDialog(this, "লট Closed হিসেবে মার্ক করা হয়েছে।", "সফল",
                    JOptionPane.INFORMATION_MESSAGE);
            loadForSelectedLot();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "লট Close করতে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSaveMaterialReturns() {
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
                        "DELETE FROM reconciliation WHERE lot_id = ?")) {
                    del.setLong(1, lotId);
                    del.executeUpdate();
                }

                String insertSql = "INSERT INTO reconciliation " +
                        "(lot_id, material_id, received_qty, used_qty, returned_qty, waste_qty) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (int r = 0; r < tableModel.getRowCount(); r++) {
                        BigDecimal issued = parseDecimal((String) tableModel.getValueAt(r, COL_ISSUED));
                        if (issued == null) {
                            continue; // skip rows with nothing entered
                        }
                        ins.setLong(1, lotId);
                        ins.setInt(2, rowMaterialIds.get(r));
                        ins.setBigDecimal(3, issued);
                        setNullableDecimal(ins, 4, parseDecimal((String) tableModel.getValueAt(r, COL_USED)));
                        setNullableDecimal(ins, 5, parseDecimal((String) tableModel.getValueAt(r, COL_RETURNED)));
                        setNullableDecimal(ins, 6, parseDecimal((String) tableModel.getValueAt(r, COL_WASTE)));
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "মেটেরিয়াল রিটার্ন সংরক্ষিত হয়েছে।", "সফল",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException inner) {
                conn.rollback();
                throw inner;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("lot_closing_material_returns.xlsx"));
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
            ExcelExportUtil.export(path, "lot_closing_returns", headers, rows);
            JOptionPane.showMessageDialog(this, "Excel ফাইল সংরক্ষিত হয়েছে:\n" + path,
                    "Export সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export ব্যর্থ হয়েছে:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // FK resolution
    // ================================================================

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

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private static void setNullableDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
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
