package com.ppes.model;

/**
 * The kinds of form control / value a column can need.
 * Mirrors the field "type" values used in the earlier PHP includes/tables.php.
 */
public enum FieldType {
    TEXT,
    TEXTAREA,
    PASSWORD,
    NUMBER,
    DECIMAL,
    DATE,       // yyyy-MM-dd
    TIME,       // HH:mm:ss
    DATETIME,   // yyyy-MM-dd HH:mm:ss
    SELECT,     // fixed dropdown options
    FK,         // dropdown backed by another table's primary key
    LOOKUP      // dropdown of DISTINCT text values from another table's column
}
