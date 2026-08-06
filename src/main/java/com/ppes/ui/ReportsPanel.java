package com.ppes.ui;

import com.ppes.db.Db;
import com.ppes.excel.ExcelExportUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Six pre-built reports over the production workflow, each exportable to Excel. */
public class ReportsPanel extends JPanel implements Refreshable {

    private static class ReportDef {
        final String key;
        final String title;
        final String sql;
        final LinkedHashMap<String, String> columns; // SQL column alias -> display header

        ReportDef(String key, String title, String sql, LinkedHashMap<String, String> columns) {
            this.key = key;
            this.title = title;
            this.sql = sql;
            this.columns = columns;
        }
    }

    private final List<ReportDef> reports = new ArrayList<>();
    private final JComboBox<String> reportSelector = new JComboBox<>();
    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ReportsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        buildReportDefs();

        JLabel title = new JLabel("Reports (রিপোর্ট)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        for (ReportDef r : reports) {
            reportSelector.addItem(r.title);
        }
        reportSelector.addActionListener(e -> loadSelectedReport());

        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> onExport());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JLabel("রিপোর্ট নির্বাচন করুন:"));
        toolbar.add(reportSelector);
        toolbar.add(exportBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.add(title, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private LinkedHashMap<String, String> cols(String... keyThenLabel) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyThenLabel.length; i += 2) {
            map.put(keyThenLabel[i], keyThenLabel[i + 1]);
        }
        return map;
    }

    private void buildReportDefs() {
        reports.add(new ReportDef("lot_summary", "Lot Summary (Lot_Info)",
                "SELECT l.lot_id, l.lot_number, l.batch_number, p.product_name, p.strength, " +
                        "co.consignee_name, l.line_number, l.shift, l.start_time, l.end_time, l.status " +
                        "FROM batch_lot l " +
                        "LEFT JOIN product_master p ON l.product_id = p.product_id " +
                        "LEFT JOIN customer_order co ON l.order_id = co.order_id " +
                        "ORDER BY l.lot_id DESC",
                cols("lot_id", "Lot ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "product_name", "Product", "strength", "Strength", "consignee_name", "Consignee",
                        "line_number", "Line", "shift", "Shift", "start_time", "Start Time",
                        "end_time", "End Time", "status", "Status")));

        reports.add(new ReportDef("production_vs_target", "Production vs Target",
                "SELECT l.lot_id, l.lot_number, l.batch_number, " +
                        "COALESCE(SUM(pt.target_cartons),0) AS total_target, " +
                        "COALESCE(SUM(pt.actual_cartons),0) AS total_actual, " +
                        "COALESCE(SUM(pt.variance),0) AS total_variance, " +
                        "COALESCE(SUM(bp.good_qty),0) AS total_good_qty, " +
                        "COALESCE(SUM(bp.reject_qty),0) AS total_reject_qty " +
                        "FROM batch_lot l " +
                        "LEFT JOIN (SELECT lot_id, target_cartons, actual_cartons, variance FROM production_target) pt " +
                        "  ON pt.lot_id = l.lot_id " +
                        "LEFT JOIN (SELECT lot_id, good_qty, reject_qty FROM blister_production) bp " +
                        "  ON bp.lot_id = l.lot_id " +
                        "GROUP BY l.lot_id, l.lot_number, l.batch_number " +
                        "ORDER BY l.lot_id DESC",
                cols("lot_id", "Lot ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "total_target", "Total Target Cartons", "total_actual", "Total Actual Cartons",
                        "total_variance", "Total Variance", "total_good_qty", "Total Good Blister Qty",
                        "total_reject_qty", "Total Reject Qty")));

        reports.add(new ReportDef("ipc", "IPC Inspection Report",
                "SELECT i.inspection_id, l.lot_number, l.batch_number, i.inspection_time, " +
                        "i.tablet_check, i.seal_check, i.coding_check, e.employee_name AS qa_employee " +
                        "FROM ipc_inspection i " +
                        "JOIN batch_lot l ON i.lot_id = l.lot_id " +
                        "LEFT JOIN employee e ON i.qa_employee = e.employee_id " +
                        "ORDER BY i.inspection_id DESC",
                cols("inspection_id", "ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "inspection_time", "Inspection Time", "tablet_check", "Tablet Check",
                        "seal_check", "Seal Check", "coding_check", "Coding Check", "qa_employee", "QA Employee")));

        reports.add(new ReportDef("reconciliation", "Raw Material Reconciliation",
                "SELECT r.rec_id, l.lot_number, l.batch_number, m.material_name, " +
                        "r.received_qty, r.used_qty, r.returned_qty, r.waste_qty, " +
                        "(r.received_qty - r.used_qty - r.returned_qty - r.waste_qty) AS unexplained_variance " +
                        "FROM reconciliation r " +
                        "JOIN batch_lot l ON r.lot_id = l.lot_id " +
                        "JOIN raw_material_master m ON r.material_id = m.material_id " +
                        "ORDER BY r.rec_id DESC",
                cols("rec_id", "ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "material_name", "Material", "received_qty", "Received Qty", "used_qty", "Used Qty",
                        "returned_qty", "Returned Qty", "waste_qty", "Waste Qty",
                        "unexplained_variance", "Unexplained Variance")));

        reports.add(new ReportDef("pallet_shipper", "Pallet & Shipper Report",
                "SELECT pl.pallet_id, l.lot_number, l.batch_number, pl.pallet_number, pl.carton_qty, " +
                        "pl.status AS pallet_status, sl.shipper_number, sl.barcode, " +
                        "e.employee_name AS verified_by, sl.verification_time " +
                        "FROM pallet pl " +
                        "JOIN batch_lot l ON pl.lot_id = l.lot_id " +
                        "LEFT JOIN shipper_label sl ON sl.lot_id = pl.lot_id " +
                        "LEFT JOIN employee e ON sl.verified_by = e.employee_id " +
                        "ORDER BY pl.pallet_id DESC",
                cols("pallet_id", "Pallet ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "pallet_number", "Pallet Number", "carton_qty", "Carton Qty", "pallet_status", "Pallet Status",
                        "shipper_number", "Shipper Number", "barcode", "Barcode", "verified_by", "Verified By",
                        "verification_time", "Verification Time")));

        reports.add(new ReportDef("shift_handover", "Shift Handover Report",
                "SELECT sh.handover_id, l.lot_number, l.batch_number, sh.from_shift, sh.to_shift, " +
                        "sh.remarks, e.employee_name AS approved_by " +
                        "FROM shift_handover sh " +
                        "JOIN batch_lot l ON sh.lot_id = l.lot_id " +
                        "LEFT JOIN employee e ON sh.approved_by = e.employee_id " +
                        "ORDER BY sh.handover_id DESC",
                cols("handover_id", "ID", "lot_number", "Lot Number", "batch_number", "Batch Number",
                        "from_shift", "From Shift", "to_shift", "To Shift", "remarks", "Remarks",
                        "approved_by", "Approved By")));
    }

    private void loadSelectedReport() {
        int idx = reportSelector.getSelectedIndex();
        if (idx < 0 || idx >= reports.size()) {
            return;
        }
        ReportDef r = reports.get(idx);

        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        for (String header : r.columns.values()) {
            tableModel.addColumn(header);
        }

        List<String> colKeys = new ArrayList<>(r.columns.keySet());

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(r.sql)) {

            while (rs.next()) {
                Object[] row = new Object[colKeys.size()];
                for (int i = 0; i < colKeys.size(); i++) {
                    row[i] = rs.getObject(colKeys.get(i));
                }
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "রিপোর্ট লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExport() {
        int idx = reportSelector.getSelectedIndex();
        if (idx < 0 || idx >= reports.size()) {
            return;
        }
        ReportDef r = reports.get(idx);

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(r.key + ".xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".xlsx")) {
            path = path + ".xlsx";
        }

        List<String> headers = new ArrayList<>();
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            headers.add(tableModel.getColumnName(c));
        }
        List<Object[]> rows = new ArrayList<>();
        for (int rr = 0; rr < tableModel.getRowCount(); rr++) {
            Object[] row = new Object[tableModel.getColumnCount()];
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                row[c] = tableModel.getValueAt(rr, c);
            }
            rows.add(row);
        }

        try {
            ExcelExportUtil.export(path, r.key, headers, rows);
            JOptionPane.showMessageDialog(this, "Excel ফাইল সংরক্ষিত হয়েছে:\n" + path,
                    "Export সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export ব্যর্থ হয়েছে:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        if (reportSelector.getSelectedIndex() < 0 && reportSelector.getItemCount() > 0) {
            reportSelector.setSelectedIndex(0);
        }
        loadSelectedReport();
    }
}
