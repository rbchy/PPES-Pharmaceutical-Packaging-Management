package com.ppes.ui;

import com.ppes.db.Db;
import com.ppes.excel.ExcelExportUtil;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;

import javax.swing.*;
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
 * "Work Order — Packaging Requirement" calculator.
 *
 * For a given lot/work order (e.g. blistering Sudafed, Order Qty = 100,000
 * cartons), this explodes the order quantity down through the packing
 * hierarchy using ratios you enter once per lot:
 *
 *   tablets/blister, blisters/carton, cartons/bundle, bundles/shipper,
 *   shippers/layer x layers/pallet
 *
 * and shows, live:
 *   - Total Required (blisters, tablets, cartons, bundles, shippers,
 *     pallets/skids, shipper labels, pallet labels) for the FULL order, and
 *   - Remaining Needed after partial production — based on how many
 *     shippers have actually been produced so far for this lot (pulled
 *     live from shift_production_log, the same table the Shift Production
 *     Log screen writes to; you can also override it by hand).
 *
 * Nothing here is stored pre-computed — only the ratios + order qty are
 * saved (work_order_packing_spec); every number in the results grid is
 * calculated fresh each time you open/recalculate the screen.
 */
public class WorkOrderPackagingPanel extends JPanel implements Refreshable {

    private static final String PLACEHOLDER = "-- লট নির্বাচন করুন --";
    private static final String[] COLUMNS = {
            "Unit", "Conversion", "Total Required (Full Order)", "Produced So Far", "Remaining Needed"
    };

    private final JComboBox<ComboItem> lotSelector = new JComboBox<>();
    private final JLabel workOrderValue = new JLabel("-");
    private final JLabel lotNoValue = new JLabel("-");
    private final JLabel productNameValue = new JLabel("-");

    private final JTextField orderQtyField = new JTextField(10);
    private final JTextField tabletsPerBlisterField = new JTextField(6);
    private final JTextField blistersPerCartonField = new JTextField(6);
    private final JTextField cartonsPerBundleField = new JTextField(6);
    private final JTextField bundlesPerShipperField = new JTextField(6);
    private final JTextField shippersPerLayerField = new JTextField(6);
    private final JTextField layersPerPalletField = new JTextField(6);
    private final JTextField shippersProducedField = new JTextField(10);

    private final JLabel summaryLabel = new JLabel(" ");

    private final DefaultTableModel resultModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(resultModel);

    public WorkOrderPackagingPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        add(new BannerPanel("/images/banner_work_order.jpg", "Work Order — Packaging Requirement",
                "Blister/carton/bundle/shipper/pallet explosion & remaining-needed calculator", 110), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.add(buildHeaderCard(), BorderLayout.NORTH);
        body.add(buildResultsCard(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        lotSelector.addActionListener(e -> loadForSelectedLot());

        for (JTextField f : new JTextField[]{
                orderQtyField, tabletsPerBlisterField, blistersPerCartonField, cartonsPerBundleField,
                bundlesPerShipperField, shippersPerLayerField, layersPerPalletField, shippersProducedField}) {
            f.addActionListener(e -> recalculate());
            f.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    recalculate();
                }
            });
        }
    }

    // ================================================================
    // Header card
    // ================================================================

    private JPanel buildHeaderCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("লট নির্বাচন করুন ও প্যাকিং রেশিও (Packing Spec)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JPanel lotRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lotRow.add(new JLabel("Lot:"));
        lotSelector.setPreferredSize(new Dimension(320, 26));
        lotRow.add(lotSelector);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        lotRow.add(refreshBtn);

        JPanel readOnlyRow = new JPanel(new GridLayout(1, 3, 10, 4));
        readOnlyRow.add(labeled("Work Order No", workOrderValue));
        readOnlyRow.add(labeled("Lot No", lotNoValue));
        readOnlyRow.add(labeled("Product Name", productNameValue));

        JPanel ratioRow = new JPanel(new GridLayout(1, 7, 8, 4));
        ratioRow.add(labeled("Order Qty (Cartons)", orderQtyField));
        ratioRow.add(labeled("Tablets / Blister", tabletsPerBlisterField));
        ratioRow.add(labeled("Blisters / Carton", blistersPerCartonField));
        ratioRow.add(labeled("Cartons / Bundle", cartonsPerBundleField));
        ratioRow.add(labeled("Bundles / Shipper", bundlesPerShipperField));
        ratioRow.add(labeled("Shippers / Layer", shippersPerLayerField));
        ratioRow.add(labeled("Layers / Pallet", layersPerPalletField));

        JPanel progressRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        progressRow.add(labeled("Shippers Produced So Far", shippersProducedField));
        JButton pullBtn = new JButton("Pull from Shift Production Log");
        pullBtn.addActionListener(e -> pullShippersProducedFromShiftLog());
        progressRow.add(pullBtn);

        JButton saveBtn = new JButton("Save Packing Spec (Upsert)");
        saveBtn.addActionListener(e -> onSaveSpec());
        JButton recalcBtn = new JButton("Recalculate");
        recalcBtn.addActionListener(e -> recalculate());
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saveRow.add(saveBtn);
        saveRow.add(recalcBtn);

        JPanel middle = new JPanel();
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        readOnlyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ratioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        middle.add(readOnlyRow);
        middle.add(Box.createVerticalStrut(6));
        middle.add(ratioRow);
        middle.add(Box.createVerticalStrut(6));
        middle.add(progressRow);

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
    // Results card
    // ================================================================

    private JPanel buildResultsCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("প্যাকেজিং প্রয়োজনীয়তা (স্বয়ংক্রিয় হিসাব)"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> onExport());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(exportBtn);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        card.add(summaryLabel, BorderLayout.SOUTH);
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
        clearFields();
        resultModel.setRowCount(0);
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
                    "SELECT order_qty_cartons, tablets_per_blister, blisters_per_carton, cartons_per_bundle, " +
                            "bundles_per_shipper, shippers_per_layer, layers_per_pallet " +
                            "FROM work_order_packing_spec WHERE lot_id = ?")) {
                ps.setLong(1, lotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        orderQtyField.setText(str(rs.getObject("order_qty_cartons")));
                        tabletsPerBlisterField.setText(str(rs.getObject("tablets_per_blister")));
                        blistersPerCartonField.setText(str(rs.getObject("blisters_per_carton")));
                        cartonsPerBundleField.setText(str(rs.getObject("cartons_per_bundle")));
                        bundlesPerShipperField.setText(str(rs.getObject("bundles_per_shipper")));
                        shippersPerLayerField.setText(str(rs.getObject("shippers_per_layer")));
                        layersPerPalletField.setText(str(rs.getObject("layers_per_pallet")));
                    } else {
                        // sensible defaults matching the Sudafed example, so the screen is usable immediately
                        tabletsPerBlisterField.setText("12");
                        blistersPerCartonField.setText("2");
                        cartonsPerBundleField.setText("6");
                        bundlesPerShipperField.setText("12");
                        shippersPerLayerField.setText("12");
                        layersPerPalletField.setText("7");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ডাটা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        pullShippersProducedFromShiftLog();
        recalculate();
    }

    private void pullShippersProducedFromShiftLog() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            return;
        }
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(no_of_shipper), 0) AS total FROM shift_production_log WHERE lot_id = ?")) {
            ps.setLong(1, lotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    shippersProducedField.setText(str(rs.getObject("total")));
                }
            }
            recalculate();
        } catch (SQLException e) {
            // shift_production_log may not exist yet (schema not run) — quietly leave the field as-is
        }
    }

    private void clearFields() {
        workOrderValue.setText("-");
        lotNoValue.setText("-");
        productNameValue.setText("-");
        orderQtyField.setText("");
        tabletsPerBlisterField.setText("");
        blistersPerCartonField.setText("");
        cartonsPerBundleField.setText("");
        bundlesPerShipperField.setText("");
        shippersPerLayerField.setText("");
        layersPerPalletField.setText("");
        shippersProducedField.setText("");
        summaryLabel.setText(" ");
    }

    // ================================================================
    // The packaging math
    // ================================================================

    private void recalculate() {
        resultModel.setRowCount(0);

        Long orderQty = parseLong(orderQtyField.getText());
        Long tabletsPerBlister = parseLong(tabletsPerBlisterField.getText());
        Long blistersPerCarton = parseLong(blistersPerCartonField.getText());
        Long cartonsPerBundle = parseLong(cartonsPerBundleField.getText());
        Long bundlesPerShipper = parseLong(bundlesPerShipperField.getText());
        Long shippersPerLayer = parseLong(shippersPerLayerField.getText());
        Long layersPerPallet = parseLong(layersPerPalletField.getText());
        Long shippersProduced = parseLong(shippersProducedField.getText());
        if (shippersProduced == null) {
            shippersProduced = 0L;
        }

        if (orderQty == null || tabletsPerBlister == null || blistersPerCarton == null
                || cartonsPerBundle == null || bundlesPerShipper == null
                || shippersPerLayer == null || layersPerPallet == null
                || cartonsPerBundle == 0 || bundlesPerShipper == 0 || shippersPerLayer == 0 || layersPerPallet == 0) {
            summaryLabel.setText("Order Qty ও সব প্যাকিং রেশিও পূরণ করুন (কোনোটাই ০ বা খালি রাখা যাবে না)।");
            return;
        }

        long cartonsPerShipper = cartonsPerBundle * bundlesPerShipper;
        long shippersPerPallet = shippersPerLayer * layersPerPallet;
        long cartonsPerPallet = cartonsPerShipper * shippersPerPallet;

        // ---- Total required for the FULL order ----
        long totalCartons = orderQty;
        long totalBlisters = totalCartons * blistersPerCarton;
        long totalTablets = totalBlisters * tabletsPerBlister;
        long totalBundles = ceilDiv(totalCartons, cartonsPerBundle);
        long totalShippers = ceilDiv(totalCartons, cartonsPerShipper);
        long totalPallets = ceilDiv(totalShippers, shippersPerPallet);
        long totalShipperLabels = totalShippers;
        long totalPalletLabels = totalPallets;

        // ---- Produced so far (derived from Shippers Produced) ----
        long cartonsProduced = Math.min(totalCartons, shippersProduced * cartonsPerShipper);
        long blistersProduced = cartonsProduced * blistersPerCarton;
        long tabletsProduced = blistersProduced * tabletsPerBlister;
        long bundlesProduced = cartonsProduced / cartonsPerBundle;
        long shippersProducedCapped = Math.min(totalShippers, shippersProduced);
        long palletsProduced = shippersProducedCapped / shippersPerPallet;
        long shipperLabelsProduced = shippersProducedCapped;
        long palletLabelsProduced = palletsProduced;

        // ---- Remaining needed to finish the order ----
        long remainingCartons = Math.max(0, totalCartons - cartonsProduced);
        long remainingBlisters = remainingCartons * blistersPerCarton;
        long remainingTablets = remainingBlisters * tabletsPerBlister;
        long remainingBundles = ceilDiv(remainingCartons, cartonsPerBundle);
        long remainingShippers = Math.max(0, totalShippers - shippersProducedCapped);
        long remainingPallets = ceilDiv(remainingShippers, shippersPerPallet);
        long remainingShipperLabels = remainingShippers;
        long remainingPalletLabels = remainingPallets;

        addResultRow("Tablets", tabletsPerBlister + " per Blister", totalTablets, tabletsProduced, remainingTablets);
        addResultRow("Blisters", blistersPerCarton + " per Carton", totalBlisters, blistersProduced, remainingBlisters);
        addResultRow("Cartons", "Order Qty", totalCartons, cartonsProduced, remainingCartons);
        addResultRow("Bundles", cartonsPerBundle + " Cartons per Bundle", totalBundles, bundlesProduced, remainingBundles);
        addResultRow("Shippers", cartonsPerShipper + " Cartons per Shipper", totalShippers, shippersProducedCapped, remainingShippers);
        addResultRow("Pallets (Skids)", shippersPerPallet + " Shippers per Pallet (" + cartonsPerPallet + " Cartons)",
                totalPallets, palletsProduced, remainingPallets);
        addResultRow("Shipper Labels", "1 per Shipper", totalShipperLabels, shipperLabelsProduced, remainingShipperLabels);
        addResultRow("Pallet Labels", "1 per Pallet", totalPalletLabels, palletLabelsProduced, remainingPalletLabels);

        summaryLabel.setText(String.format(
                "মোট অর্ডার: %,d Cartons = %,d Shippers = %,d Pallets  |  এখন পর্যন্ত উৎপাদিত: %,d Shippers  |  বাকি: %,d Shippers (%,d Pallets)",
                totalCartons, totalShippers, totalPallets, shippersProducedCapped, remainingShippers, remainingPallets));
    }

    private void addResultRow(String unit, String conversion, long total, long produced, long remaining) {
        resultModel.addRow(new Object[]{
                unit, conversion, fmt(total), fmt(produced), fmt(remaining)
        });
    }

    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    private static long ceilDiv(long a, long b) {
        return b == 0 ? 0 : (a + b - 1) / b;
    }

    // ================================================================
    // Save
    // ================================================================

    private void onSaveSpec() {
        Long lotId = selectedLotId();
        if (lotId == null) {
            JOptionPane.showMessageDialog(this, "আগে একটি লট নির্বাচন করুন।", "লট নির্বাচিত হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Long orderQty = parseLong(orderQtyField.getText());
        Long tabletsPerBlister = parseLong(tabletsPerBlisterField.getText());
        Long blistersPerCarton = parseLong(blistersPerCartonField.getText());
        Long cartonsPerBundle = parseLong(cartonsPerBundleField.getText());
        Long bundlesPerShipper = parseLong(bundlesPerShipperField.getText());
        Long shippersPerLayer = parseLong(shippersPerLayerField.getText());
        Long layersPerPallet = parseLong(layersPerPalletField.getText());

        if (orderQty == null) {
            JOptionPane.showMessageDialog(this, "Order Qty (Cartons) আবশ্যক।", "প্রয়োজনীয় তথ্য নেই",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO work_order_packing_spec " +
                "(lot_id, order_qty_cartons, tablets_per_blister, blisters_per_carton, cartons_per_bundle, " +
                "bundles_per_shipper, shippers_per_layer, layers_per_pallet) VALUES (?,?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE order_qty_cartons=VALUES(order_qty_cartons), " +
                "tablets_per_blister=VALUES(tablets_per_blister), blisters_per_carton=VALUES(blisters_per_carton), " +
                "cartons_per_bundle=VALUES(cartons_per_bundle), bundles_per_shipper=VALUES(bundles_per_shipper), " +
                "shippers_per_layer=VALUES(shippers_per_layer), layers_per_pallet=VALUES(layers_per_pallet)";

        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lotId);
            ps.setLong(2, orderQty);
            setDefaultingLong(ps, 3, tabletsPerBlister, 12);
            setDefaultingLong(ps, 4, blistersPerCarton, 2);
            setDefaultingLong(ps, 5, cartonsPerBundle, 6);
            setDefaultingLong(ps, 6, bundlesPerShipper, 12);
            setDefaultingLong(ps, 7, shippersPerLayer, 12);
            setDefaultingLong(ps, 8, layersPerPallet, 7);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "প্যাকিং স্পেক সংরক্ষিত হয়েছে।", "সফল", JOptionPane.INFORMATION_MESSAGE);
            recalculate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void setDefaultingLong(PreparedStatement ps, int index, Long value, long fallback) throws SQLException {
        ps.setLong(index, value == null ? fallback : value);
    }

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("work_order_packaging_requirement.xlsx"));
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
        for (int r = 0; r < resultModel.getRowCount(); r++) {
            Object[] row = new Object[COLUMNS.length];
            for (int c = 0; c < COLUMNS.length; c++) {
                row[c] = resultModel.getValueAt(r, c);
            }
            rows.add(row);
        }

        try {
            ExcelExportUtil.export(path, "packaging_requirement", headers, rows);
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

    private static Long parseLong(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
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
