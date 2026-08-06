package com.ppes.model;

import java.util.Arrays;
import java.util.List;

/** Definition of a single column's form/display behaviour. */
public class FieldDef {
    public final String name;
    public final String label;
    public final FieldType type;
    public boolean required = false;
    public List<String> options;   // for SELECT
    public String refTable;        // for FK / LOOKUP
    public String refColumn;       // for LOOKUP only (text column to look up)

    public FieldDef(String name, String label, FieldType type) {
        this.name = name;
        this.label = label;
        this.type = type;
    }

    public FieldDef required(boolean required) {
        this.required = required;
        return this;
    }

    public FieldDef options(String... opts) {
        this.options = Arrays.asList(opts);
        return this;
    }

    public FieldDef refTable(String table) {
        this.refTable = table;
        return this;
    }

    public FieldDef refColumn(String column) {
        this.refColumn = column;
        return this;
    }
}
