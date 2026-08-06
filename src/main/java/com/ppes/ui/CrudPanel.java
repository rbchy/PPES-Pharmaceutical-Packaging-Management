package com.ppes.ui;

import com.ppes.db.Db;
import com.ppes.db.FieldValueConverter;
import com.ppes.excel.ExcelExportUtil;
import com.ppes.excel.ExcelImportUtil;
import com.ppes.model.FieldDef;
import com.ppes.model.FieldType;
import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Add / List / Delete screen for a single table, driven entirely by
 * its TableDef metadata. One instance of this class is created per table
 * (see MainFrame), so the same code handles all 16 tables.
 */
public class CrudPanel extends JPanel implements Refreshable {

    private static final String PLACEHOLDER = "-- নির্বাচন করুন --";

    private final TableDef def;
    private final JPanel formFieldsPanel = new JPanel();
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();
    private final DefaultTableModel listModel;
    private final JTable listTable;
    private final JLabel statusLabel = new JLabel(" ");

    public CrudPanel(TableDef def) {
        this.def = def;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        BannerPanel banner = new BannerPanel(FormBanners.imageFor(def.key),
                def.label + "  (" + def.labelBn + ")", FormBanners.subtitleFor(def.key), 100);

        formFieldsPanel.setLayout(new BoxLayout(formFieldsPanel, BoxLayout.Y_AXIS));

        JButton saveBtn = new JButton("সংরক্ষণ করুন (Save)");
        saveBtn.addActionListener(e -> onSave());

        JPanel formCard = titledCard("নতুন এন্ট্রি যোগ করুন (Add New Entry)");
        formCard.add(new JScrollPane(formFieldsPanel), BorderLayout.CENTER);
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saveRow.add(saveBtn);
        saveRow.add(statusLabel);
        formCard.add(saveRow, BorderLayout.SOUTH);

        List<String> headers = new ArrayList<>();
        headers.add("ID");
        for (FieldDef f : def.fields.values()) {
            headers.add(f.label);
        }
        listModel = new DefaultTableModel(headers.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        listTable = new JTable(listModel);

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> onDelete());
        JButton exportBtn = new JButton("Export to Excel");
        exportBtn.addActionListener(e -> onExport());
        JButton importBtn = new JButton("Import from Excel");
        importBtn.addActionListener(e -> onImport());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JPanel listToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listToolbar.add(deleteBtn);
        listToolbar.add(exportBtn);
        listToolbar.add(importBtn);
        listToolbar.add(refreshBtn);

        JPanel listCard = titledCard("রেকর্ড তালিকা (সাম্প্রতিক ২০০টি)");
        listCard.add(listToolbar, BorderLayout.NORTH);
        listCard.add(new JScrollPane(listTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formCard, listCard);
        split.setResizeWeight(0.42);
        split.setBorder(BorderFactory.createEmptyBorder());

        add(banner, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JPanel titledCard(String title) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        return card;
    }

    // ================================================================
    // Form building
    // ================================================================

    // Forms are laid out as rows of up to FORM_COLUMNS fields side by side
    // (instead of one long single-column list) so wider tables like Raw
    // Material Master, Batch/Lot, Pallet, and Pallet Placard fit on one
    // screen with far less scrolling. Multi-line fields (TEXTAREA) always
    // get their own full-width row since they don't fit well next to
    // single-line fields.
    private static final int FORM_COLUMNS = 3;

    private void rebuildForm() {
        formFieldsPanel.removeAll();
        inputs.clear();

        List<JPanel> rowBuffer = new ArrayList<>();
        try (Connection conn = Db.getConnection()) {
            for (FieldDef f : def.fields.values()) {
                JComponent input = buildInputComponent(conn, f);
                inputs.put(f.name, input);
                JPanel cell = formRow(f, input);

                if (f.type == FieldType.TEXTAREA) {
                    flushFormRow(rowBuffer);
                    JPanel full = new JPanel(new GridLayout(1, 1, 14, 6));
                    full.setAlignmentX(Component.LEFT_ALIGNMENT);
                    full.add(cell);
                    formFieldsPanel.add(full);
                    formFieldsPanel.add(Box.createVerticalStrut(8));
                } else {
                    rowBuffer.add(cell);
                    if (rowBuffer.size() == FORM_COLUMNS) {
                        flushFormRow(rowBuffer);
                    }
                }
            }
            flushFormRow(rowBuffer);
        } catch (SQLException e) {
            formFieldsPanel.add(new JLabel("ডাটাবেজ থেকে ড্রপডাউন লোড করা যায়নি: " + e.getMessage()));
        }

        formFieldsPanel.revalidate();
        formFieldsPanel.repaint();
    }

    private void flushFormRow(List<JPanel> rowBuffer) {
        if (rowBuffer.isEmpty()) {
            return;
        }
        JPanel row = new JPanel(new GridLayout(1, rowBuffer.size(), 14, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JPanel cell : rowBuffer) {
            row.add(cell);
        }
        formFieldsPanel.add(row);
        formFieldsPanel.add(Box.createVerticalStrut(8));
        rowBuffer.clear();
    }

    private JPanel formRow(FieldDef f, JComponent input) {
        JPanel row = new JPanel(new BorderLayout(8, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        String labelText = f.label + (f.required ? " *" : "");
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(130, 20));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));

        row.add(label, BorderLayout.WEST);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    private JComponent buildInputComponent(Connection conn, FieldDef f) throws SQLException {
        switch (f.type) {
            case SELECT: {
                JComboBox<String> combo = new JComboBox<>();
                combo.addItem(PLACEHOLDER);
                for (String opt : f.options) {
                    combo.addItem(opt);
                }
                return combo;
            }
            case LOOKUP: {
                JComboBox<String> combo = new JComboBox<>();
                combo.addItem(PLACEHOLDER);
                for (String val : fetchLookupValues(conn, f.refTable, f.refColumn)) {
                    combo.addItem(val);
                }
                return combo;
            }
            case FK: {
                JComboBox<ComboItem> combo = new JComboBox<>();
                combo.addItem(new ComboItem(null, PLACEHOLDER));
                for (ComboItem item : fetchFkOptions(conn, f.refTable)) {
                    combo.addItem(item);
                }
                return combo;
            }
            case TEXTAREA: {
                JTextArea area = new JTextArea(3, 20);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                return area;
            }
            case PASSWORD:
                return new JPasswordField(20);
            case DATE: {
                JTextField tf = new JTextField(20);
                tf.setToolTipText("yyyy-MM-dd, উদাহরণ: 2026-08-05");
                return tf;
            }
            case TIME: {
                JTextField tf = new JTextField(20);
                tf.setToolTipText("HH:mm বা HH:mm:ss, উদাহরণ: 14:30");
                return tf;
            }
            case DATETIME: {
                JTextField tf = new JTextField(20);
                tf.setToolTipText("yyyy-MM-dd HH:mm, উদাহরণ: 2026-08-05 07:00");
                return tf;
            }
            case NUMBER:
            case DECIMAL:
            case TEXT:
            default:
                return new JTextField(20);
        }
    }

    private List<String> fetchLookupValues(Connection conn, String refTable, String refColumn) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT `" + refColumn + "` AS val FROM `" + refTable +
                "` WHERE `" + refColumn + "` IS NOT NULL AND `" + refColumn + "` <> '' ORDER BY `" + refColumn + "`";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString("val"));
            }
        }
        return values;
    }

    private List<ComboItem> fetchFkOptions(Connection conn, String refTable) throws SQLException {
        List<ComboItem> options = new ArrayList<>();
        TableDef refDef = TableRegistry.TABLES.get(refTable);
        String expr = refDef.displayExprFor("`" + refDef.key + "`");
        String sql = "SELECT `" + refDef.pk + "` AS id_val, (" + expr + ") AS label_val FROM `" + refDef.key +
                "` ORDER BY `" + refDef.pk + "` DESC LIMIT 500";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                options.add(new ComboItem(rs.getObject("id_val"), rs.getString("label_val")));
            }
        }
        return options;
    }

    private String rawValueOf(JComponent comp) {
        if (comp instanceof JComboBox) {
            @SuppressWarnings("unchecked")
            JComboBox<Object> combo = (JComboBox<Object>) comp;
            Object selected = combo.getSelectedItem();
            if (selected == null) {
                return "";
            }
            if (selected instanceof ComboItem) {
                ComboItem item = (ComboItem) selected;
                return item.id == null ? "" : item.id.toString();
            }
            String s = selected.toString();
            return PLACEHOLDER.equals(s) ? "" : s;
        } else if (comp instanceof JPasswordField) {
            return new String(((JPasswordField) comp).getPassword());
        } else if (comp instanceof JTextArea) {
            return ((JTextArea) comp).getText();
        } else if (comp instanceof JTextField) {
            return ((JTextField) comp).getText();
        }
        return "";
    }

    // ================================================================
    // Save
    // ================================================================

    private void onSave() {
        try (Connection conn = Db.getConnection()) {
            List<String> cols = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (Map.Entry<String, FieldDef> entry : def.fields.entrySet()) {
                FieldDef f = entry.getValue();
                String raw = rawValueOf(inputs.get(f.name));
                if (f.required && raw.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "\"" + f.label + "\" পূরণ করা আবশ্যক।", "প্রয়োজনীয় তথ্য নেই",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Object val;
                try {
                    val = FieldValueConverter.convert(conn, f, raw);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "ভুল তথ্য", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                cols.add(f.name);
                values.add(val);
            }

            StringBuilder sql = new StringBuilder("INSERT INTO `").append(def.key).append("` (");
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                    placeholders.append(", ");
                }
                sql.append('`').append(cols.get(i)).append('`');
                placeholders.append('?');
            }
            sql.append(") VALUES (").append(placeholders).append(")");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    ps.setObject(i + 1, values.get(i));
                }
                ps.executeUpdate();
            }

            statusLabel.setText("রেকর্ড সফলভাবে যোগ হয়েছে।");
            refresh();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "এন্ট্রি সংরক্ষণে সমস্যা হয়েছে:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // Delete
    // ================================================================

    private void onDelete() {
        int row = listTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "প্রথমে একটি রো সিলেক্ট করুন।", "কিছু সিলেক্ট করা হয়নি",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object pkValue = listModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "আপনি কি নিশ্চিত এই রেকর্ডটি (ID = " + pkValue + ") ডিলিট করতে চান?",
                "ডিলিট নিশ্চিত করুন", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM `" + def.key + "` WHERE `" + def.pk + "` = ?")) {
            ps.setObject(1, pkValue);
            ps.executeUpdate();
            reloadList();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "ডিলিট করা যায়নি (সম্ভবত অন্য টেবিল থেকে এটি রেফারেন্স করা আছে):\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // List loading
    // ================================================================

    private void reloadList() {
        listModel.setRowCount(0);
        StringBuilder select = new StringBuilder("SELECT t.`").append(def.pk).append("` AS pk_val");
        StringBuilder joins = new StringBuilder();
        int aliasIdx = 0;

        for (FieldDef f : def.fields.values()) {
            if (f.type == com.ppes.model.FieldType.FK) {
                aliasIdx++;
                String alias = "f" + aliasIdx;
                TableDef refDef = TableRegistry.TABLES.get(f.refTable);
                String expr = refDef.displayExprFor(alias);
                select.append(", (").append(expr).append(") AS `col_").append(f.name).append('`');
                joins.append(" LEFT JOIN `").append(f.refTable).append("` ").append(alias)
                        .append(" ON t.`").append(f.name).append("` = ").append(alias).append(".`")
                        .append(refDef.pk).append('`');
            } else {
                select.append(", t.`").append(f.name).append("` AS `col_").append(f.name).append('`');
            }
        }
        select.append(" FROM `").append(def.key).append("` t").append(joins)
                .append(" ORDER BY t.`").append(def.pk).append("` DESC LIMIT 200");

        try (Connection conn = Db.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(select.toString())) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 0; i < colCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                listModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "রেকর্ড তালিকা লোড করা যায়নি:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================
    // Excel export / import
    // ================================================================

    private void onExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(def.key + ".xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.toLowerCase().endsWith(".xlsx")) {
            path = path + ".xlsx";
        }

        List<String> headers = new ArrayList<>();
        for (int c = 0; c < listModel.getColumnCount(); c++) {
            headers.add(listModel.getColumnName(c));
        }
        List<Object[]> rows = new ArrayList<>();
        for (int r = 0; r < listModel.getRowCount(); r++) {
            Object[] row = new Object[listModel.getColumnCount()];
            for (int c = 0; c < listModel.getColumnCount(); c++) {
                row[c] = listModel.getValueAt(r, c);
            }
            rows.add(row);
        }

        try {
            ExcelExportUtil.export(path, def.key, headers, rows);
            JOptionPane.showMessageDialog(this, "Excel ফাইল সংরক্ষিত হয়েছে:\n" + path,
                    "Export সফল", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export ব্যর্থ হয়েছে:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onImport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        String path = chooser.getSelectedFile().getAbsolutePath();

        List<Map<String, String>> rows;
        try {
            // If the chosen file has a sheet named after this table (e.g. the
            // multi-sheet PPES_sample_data.xlsx), that sheet is used automatically;
            // otherwise it falls back to the first sheet in the file.
            rows = ExcelImportUtil.readSheetAsMaps(path, def.key);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Excel ফাইল পড়া যায়নি:\n" + e.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ফাইলে কোনো ডাটা রো পাওয়া যায়নি।", "Import",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try (Connection conn = Db.getConnection()) {
            int rowNum = 1;
            for (Map<String, String> rowMap : rows) {
                rowNum++;
                try {
                    insertOneImportedRow(conn, rowMap);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "ডাটাবেজ কানেকশন সমস্যা:\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder summary = new StringBuilder();
        summary.append(successCount).append(" / ").append(rows.size()).append(" রো সফলভাবে ইমপোর্ট হয়েছে।");
        if (!errors.isEmpty()) {
            summary.append("\n\nবিফল রো:\n");
            int shown = 0;
            for (String err : errors) {
                if (shown >= 15) {
                    summary.append("... (আরও ").append(errors.size() - shown).append("টি)");
                    break;
                }
                summary.append(err).append('\n');
                shown++;
            }
        }
        JOptionPane.showMessageDialog(this, summary.toString(), "Import সম্পন্ন",
                errors.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        reloadList();
    }

    /** Matches Excel column headers to field names case-insensitively; unknown headers are ignored. */
    private void insertOneImportedRow(Connection conn, Map<String, String> rowMap) throws SQLException {
        List<String> cols = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, FieldDef> entry : def.fields.entrySet()) {
            FieldDef f = entry.getValue();
            String raw = findByHeader(rowMap, f.name, f.label);
            if (f.required && (raw == null || raw.trim().isEmpty())) {
                throw new IllegalArgumentException("প্রয়োজনীয় কলাম \"" + f.label + "\" খালি।");
            }
            Object val = FieldValueConverter.convert(conn, f, raw);
            cols.add(f.name);
            values.add(val);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO `").append(def.key).append("` (");
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append('`').append(cols.get(i)).append('`');
            placeholders.append('?');
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.executeUpdate();
        }
    }

    private String findByHeader(Map<String, String> rowMap, String fieldName, String fieldLabel) {
        for (Map.Entry<String, String> e : rowMap.entrySet()) {
            String key = e.getKey();
            if (key.equalsIgnoreCase(fieldName) || key.equalsIgnoreCase(fieldLabel)) {
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public void refresh() {
        rebuildForm();
        reloadList();
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
