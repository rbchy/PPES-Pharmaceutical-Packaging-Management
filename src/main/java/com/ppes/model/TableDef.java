package com.ppes.model;

import java.util.LinkedHashMap;

/** Definition of one database table: its key, labels, primary key, and fields (in display order). */
public class TableDef {
    public final String key;
    public final String label;
    public final String labelBn;
    public final String pk;
    public String displayColumn;   // simple column used to label this row in FK dropdowns elsewhere
    public String displayExpr;     // SQL expression template; use the token {A} for "this table's alias"
    public final LinkedHashMap<String, FieldDef> fields = new LinkedHashMap<>();

    public TableDef(String key, String label, String labelBn, String pk) {
        this.key = key;
        this.label = label;
        this.labelBn = labelBn;
        this.pk = pk;
    }

    public TableDef field(FieldDef f) {
        fields.put(f.name, f);
        return this;
    }

    public TableDef display(String column) {
        this.displayColumn = column;
        return this;
    }

    public TableDef displayExpr(String expr) {
        this.displayExpr = expr;
        return this;
    }

    /**
     * The SQL fragment (unparenthesized) used to produce a human label for a
     * row of this table, with {A} replaced by the given table alias/name.
     */
    public String displayExprFor(String alias) {
        if (displayExpr != null) {
            return displayExpr.replace("{A}", alias);
        }
        return alias + ".`" + displayColumn + "`";
    }
}
