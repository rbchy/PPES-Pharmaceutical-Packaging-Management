package com.ppes.ui;

import com.ppes.db.Db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class DashboardPanel extends JPanel implements Refreshable {

    private final String[] statLabels = {
            "Total Orders", "Pending Orders", "Total Lots", "Active (Running) Lots",
            "Total Good Blister Qty", "Total Reject Qty", "IPC Fail Count", "Employees"
    };
    private final JLabel[] statValues = new JLabel[statLabels.length];

    private final DefaultTableModel ordersModel = new DefaultTableModel(
            new Object[]{"ID", "Consignee", "Product", "Qty", "Status", "Requested Date"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel lotsModel = new DefaultTableModel(
            new Object[]{"ID", "Lot No.", "Batch No.", "Product", "Status", "Start Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public DashboardPanel() {
        setLayout(new BorderLayout(10, 14));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));

        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 12, 12));
        for (int i = 0; i < statLabels.length; i++) {
            statValues[i] = new JLabel("0");
            statsGrid.add(buildStatCard(statLabels[i], statValues[i]));
        }

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(title, BorderLayout.NORTH);
        top.add(statsGrid, BorderLayout.CENTER);

        JPanel headerArea = new JPanel(new BorderLayout(0, 14));
        headerArea.add(new BannerPanel("/images/cover.gif",
                "PPES — Pharmaceuticals Packaging Management System",
                "Order Intake  •  Production (Lot Management)  •  Packing & Dispatch  •  Reconciliation",
                170), BorderLayout.NORTH);
        headerArea.add(top, BorderLayout.CENTER);

        JTable ordersTable = new JTable(ordersModel);
        JTable lotsTable = new JTable(lotsModel);

        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 0, 14));
        tablesPanel.add(titledScroll("Recent Orders", ordersTable));
        tablesPanel.add(titledScroll("Recent Batches / Lots", lotsTable));

        add(headerArea, BorderLayout.NORTH);
        add(tablesPanel, BorderLayout.CENTER);
    }

    private JPanel buildStatCard(String label, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe3e8ef)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(0x6c7a8f));
        lbl.setFont(lbl.getFont().deriveFont(11f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valueLabel);
        card.add(lbl);
        return card;
    }

    private JScrollPane titledScroll(String title, JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    @Override
    public void refresh() {
        try (Connection conn = Db.getConnection()) {
            statValues[0].setText(scalar(conn, "SELECT COUNT(*) FROM customer_order"));
            statValues[1].setText(scalar(conn, "SELECT COUNT(*) FROM customer_order WHERE status = 'Pending'"));
            statValues[2].setText(scalar(conn, "SELECT COUNT(*) FROM batch_lot"));
            statValues[3].setText(scalar(conn, "SELECT COUNT(*) FROM batch_lot WHERE status = 'Running'"));
            statValues[4].setText(scalar(conn, "SELECT COALESCE(SUM(good_qty),0) FROM blister_production"));
            statValues[5].setText(scalar(conn, "SELECT COALESCE(SUM(reject_qty),0) FROM blister_production"));
            statValues[6].setText(scalar(conn,
                    "SELECT COUNT(*) FROM ipc_inspection WHERE tablet_check='Fail' OR seal_check='Fail' OR coding_check='Fail'"));
            statValues[7].setText(scalar(conn, "SELECT COUNT(*) FROM employee"));

            fillTable(conn,
                    "SELECT order_id, consignee_name, product_name, order_quantity, status, requested_date " +
                            "FROM customer_order ORDER BY order_id DESC LIMIT 8",
                    ordersModel);

            fillTable(conn,
                    "SELECT l.lot_id, l.lot_number, l.batch_number, p.product_name, l.status, l.start_time " +
                            "FROM batch_lot l LEFT JOIN product_master p ON l.product_id = p.product_id " +
                            "ORDER BY l.lot_id DESC LIMIT 8",
                    lotsModel);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "ডাটাবেজ থেকে ড্যাশবোর্ড ডাটা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String scalar(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                Object v = rs.getObject(1);
                return v == null ? "0" : v.toString();
            }
            return "0";
        }
    }

    private void fillTable(Connection conn, String sql, DefaultTableModel model) throws SQLException {
        model.setRowCount(0);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                model.addRow(row);
            }
        }
    }
}
