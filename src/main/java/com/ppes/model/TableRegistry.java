package com.ppes.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Central metadata registry — a direct Java port of the earlier PHP
 * includes/tables.php. Mirrors the user's REAL "ppes" MySQL schema exactly:
 * product_master, customer_order, employee, shift_master, raw_material_master,
 * users, batch_lot, raw_material_receiving, startup_checklist,
 * production_target, blister_production, ipc_inspection, pallet,
 * shipper_label, reconciliation, shift_handover.
 */
public final class TableRegistry {

    public static final LinkedHashMap<String, TableDef> TABLES = new LinkedHashMap<>();
    public static final LinkedHashMap<String, List<String>> MENU = new LinkedHashMap<>();

    private TableRegistry() {
    }

    static {
        // ---------------- product_master ----------------
        TableDef productMaster = new TableDef("product_master", "Product Master", "প্রোডাক্ট মাস্টার", "product_id")
                .displayExpr("CONCAT({A}.product_name, ' ', COALESCE({A}.strength,''), ' (', COALESCE({A}.product_code,''), ')')")
                .field(new FieldDef("product_code", "Product Code", FieldType.TEXT))
                .field(new FieldDef("product_name", "Product Name", FieldType.TEXT).required(true))
                .field(new FieldDef("strength", "Strength", FieldType.TEXT))
                .field(new FieldDef("pack_size", "Pack Size", FieldType.TEXT))
                .field(new FieldDef("blister_per_carton", "Blister per Carton", FieldType.NUMBER));
        TABLES.put(productMaster.key, productMaster);

        // ---------------- customer_order ----------------
        TableDef customerOrder = new TableDef("customer_order", "Customer Order", "কাস্টমার অর্ডার", "order_id")
                .displayExpr("CONCAT('Order #', {A}.order_id, ' - ', COALESCE({A}.consignee_name,''))")
                .field(new FieldDef("consignee_name", "Consignee Name", FieldType.TEXT).required(true))
                .field(new FieldDef("product_name", "Product Name", FieldType.TEXT).required(true))
                .field(new FieldDef("order_quantity", "Order Quantity", FieldType.NUMBER))
                .field(new FieldDef("customer_po", "Customer PO", FieldType.TEXT))
                .field(new FieldDef("requested_date", "Requested Date", FieldType.DATE))
                .field(new FieldDef("status", "Status", FieldType.SELECT)
                        .options("Pending", "Confirmed", "In Production", "Completed", "Shipped", "Cancelled"));
        TABLES.put(customerOrder.key, customerOrder);

        // ---------------- employee ----------------
        TableDef employee = new TableDef("employee", "Employee", "কর্মচারী", "employee_id")
                .display("employee_name")
                .field(new FieldDef("employee_name", "Employee Name", FieldType.TEXT).required(true))
                .field(new FieldDef("designation", "Designation", FieldType.TEXT))
                .field(new FieldDef("department", "Department", FieldType.SELECT)
                        .options("Production", "QC", "Warehouse", "Maintenance", "Admin", "Supervisor"));
        TABLES.put(employee.key, employee);

        // ---------------- shift_master ----------------
        TableDef shiftMaster = new TableDef("shift_master", "Shift Master", "শিফট মাস্টার", "shift_id")
                .display("shift_name")
                .field(new FieldDef("shift_name", "Shift Name", FieldType.TEXT).required(true))
                .field(new FieldDef("start_time", "Start Time", FieldType.TIME))
                .field(new FieldDef("end_time", "End Time", FieldType.TIME));
        TABLES.put(shiftMaster.key, shiftMaster);

        // ---------------- raw_material_master ----------------
        // Extended with full container/label detail fields (Item#, Lot#,
        // weights, container/batch/process-order info, dates, manufacturer,
        // pallet id, barcode) — matches the real raw-material container
        // labels. Run PPES_extended_fields_schema.sql before using these.
        TableDef rawMaterialMaster = new TableDef("raw_material_master", "Raw Material Master", "কাঁচামাল মাস্টার", "material_id")
                .displayExpr("CONCAT({A}.material_name, ' (', COALESCE({A}.material_code,''), ')')")
                .field(new FieldDef("material_code", "Material Code", FieldType.TEXT))
                .field(new FieldDef("product_no", "Product No (Item#)", FieldType.TEXT))
                .field(new FieldDef("material_name", "Material Name", FieldType.TEXT).required(true))
                .field(new FieldDef("uom", "Unit of Measure", FieldType.SELECT)
                        .options("Kg", "Roll", "Pcs", "Meter", "Liter", "Box"))
                .field(new FieldDef("lot_no", "Lot#", FieldType.TEXT))
                .field(new FieldDef("qty", "QTY", FieldType.DECIMAL))
                .field(new FieldDef("gross_weight", "Gross Weight", FieldType.DECIMAL))
                .field(new FieldDef("net_weight", "Net Weight", FieldType.DECIMAL))
                .field(new FieldDef("tare_weight", "Tare", FieldType.DECIMAL))
                .field(new FieldDef("container_no", "Container #", FieldType.TEXT))
                .field(new FieldDef("container_seq", "Container# of Total in Pallet", FieldType.TEXT))
                .field(new FieldDef("batch_no", "Batch#", FieldType.TEXT))
                .field(new FieldDef("process_order", "Process Order", FieldType.TEXT))
                .field(new FieldDef("label_date", "Date (Label)", FieldType.DATETIME))
                .field(new FieldDef("mfg_date", "MFG Date", FieldType.DATE))
                .field(new FieldDef("bulk_exp_date", "Bulk EXP Date", FieldType.DATE))
                .field(new FieldDef("manufactured_by", "Manufactured By", FieldType.TEXT))
                .field(new FieldDef("container_pallet_id", "Pallet ID", FieldType.TEXT))
                .field(new FieldDef("barcode", "Barcode", FieldType.TEXT).required(true));
        TABLES.put(rawMaterialMaster.key, rawMaterialMaster);

        // ---------------- users ----------------
        TableDef users = new TableDef("users", "Users", "সিস্টেম ইউজার", "user_id")
                .display("username")
                .field(new FieldDef("username", "Username", FieldType.TEXT).required(true))
                .field(new FieldDef("password_hash", "Password", FieldType.PASSWORD).required(true))
                .field(new FieldDef("role", "Role", FieldType.SELECT)
                        .options("Admin", "Production", "QC", "Warehouse", "Supervisor"));
        TABLES.put(users.key, users);

        // ---------------- batch_lot ----------------
        TableDef batchLot = new TableDef("batch_lot", "Batch / Lot", "ব্যাচ / লট", "lot_id")
                .displayExpr("CONCAT({A}.lot_number, ' / Batch-', {A}.batch_number)")
                .field(new FieldDef("lot_number", "Lot Number", FieldType.TEXT).required(true))
                .field(new FieldDef("batch_number", "Batch Number", FieldType.TEXT).required(true))
                .field(new FieldDef("product_id", "Product", FieldType.FK).refTable("product_master").required(true))
                .field(new FieldDef("order_id", "Customer Order", FieldType.FK).refTable("customer_order"))
                .field(new FieldDef("line_number", "Line Number", FieldType.TEXT))
                .field(new FieldDef("shift", "Shift", FieldType.LOOKUP).refTable("shift_master").refColumn("shift_name"))
                .field(new FieldDef("start_time", "Start Time", FieldType.DATETIME))
                .field(new FieldDef("end_time", "End Time", FieldType.DATETIME))
                .field(new FieldDef("status", "Status", FieldType.SELECT)
                        .options("Planned", "Running", "Completed", "On Hold", "Closed"));
        TABLES.put(batchLot.key, batchLot);

        // ---------------- raw_material_receiving ----------------
        TableDef receiving = new TableDef("raw_material_receiving", "Raw Material Receiving", "কাঁচামাল রিসিভিং", "receive_id")
                .display("receive_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("material_id", "Raw Material", FieldType.FK).refTable("raw_material_master").required(true))
                .field(new FieldDef("required_qty", "Required Qty", FieldType.DECIMAL))
                .field(new FieldDef("received_qty", "Received Qty", FieldType.DECIMAL))
                .field(new FieldDef("received_date", "Received Date/Time", FieldType.DATETIME))
                .field(new FieldDef("verified_by", "Verified By", FieldType.FK).refTable("employee"));
        TABLES.put(receiving.key, receiving);

        // ---------------- startup_checklist ----------------
        TableDef startupChecklist = new TableDef("startup_checklist", "Startup Checklist", "স্টার্টআপ চেকলিস্ট", "check_id")
                .display("check_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("check_item", "Check Item", FieldType.TEXT).required(true))
                .field(new FieldDef("status", "Status", FieldType.SELECT).options("OK", "Not OK", "N/A"))
                .field(new FieldDef("checked_by", "Checked By", FieldType.FK).refTable("employee"))
                .field(new FieldDef("checked_time", "Checked Time", FieldType.DATETIME));
        TABLES.put(startupChecklist.key, startupChecklist);

        // ---------------- production_target ----------------
        // Rearranged + extended: Order Qty, Today's Balance, Product to be
        // Produced, and Start Label now come first, followed by pallet
        // packing detail (bundles/shipper, cartons/bundle, pallet qty in
        // cartons), then the original hourly target/actual/variance fields.
        // Run PPES_extended_fields_2_schema.sql before using the new ones.
        TableDef productionTarget = new TableDef("production_target", "Production Target", "প্রোডাকশন টার্গেট", "target_id")
                .display("target_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("order_qty", "Order Qty", FieldType.NUMBER))
                .field(new FieldDef("todays_balance", "Today's Balance", FieldType.NUMBER))
                .field(new FieldDef("product_to_produce", "Product to be Produced", FieldType.TEXT))
                .field(new FieldDef("start_label", "Start Label", FieldType.NUMBER))
                .field(new FieldDef("bundles_per_shipper", "Bundles per Shipper", FieldType.NUMBER))
                .field(new FieldDef("cartons_per_bundle", "Cartons per Bundle", FieldType.NUMBER))
                .field(new FieldDef("pallet_qty_cartons", "Pallet Qty (Ctns)", FieldType.NUMBER))
                .field(new FieldDef("hour_mark", "Hour Mark", FieldType.NUMBER))
                .field(new FieldDef("target_cartons", "Target Cartons", FieldType.NUMBER))
                .field(new FieldDef("actual_cartons", "Actual Cartons", FieldType.NUMBER))
                .field(new FieldDef("variance", "Variance", FieldType.NUMBER));
        TABLES.put(productionTarget.key, productionTarget);

        // ---------------- blister_production ----------------
        TableDef blisterProduction = new TableDef("blister_production", "Blister Production", "ব্লিস্টার প্রোডাকশন", "production_id")
                .display("production_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("hour_mark", "Hour Mark", FieldType.NUMBER))
                .field(new FieldDef("machine_speed", "Machine Speed", FieldType.NUMBER))
                .field(new FieldDef("blister_produced", "Blister Produced", FieldType.NUMBER))
                .field(new FieldDef("reject_qty", "Reject Qty", FieldType.NUMBER))
                .field(new FieldDef("good_qty", "Good Qty", FieldType.NUMBER));
        TABLES.put(blisterProduction.key, blisterProduction);

        // ---------------- ipc_inspection ----------------
        TableDef ipcInspection = new TableDef("ipc_inspection", "IPC Inspection", "IPC ইন্সপেকশন", "inspection_id")
                .display("inspection_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("inspection_time", "Inspection Time", FieldType.DATETIME))
                .field(new FieldDef("tablet_check", "Tablet Check", FieldType.SELECT).options("Pass", "Fail"))
                .field(new FieldDef("seal_check", "Seal Check", FieldType.SELECT).options("Pass", "Fail"))
                .field(new FieldDef("coding_check", "Coding Check", FieldType.SELECT).options("Pass", "Fail"))
                .field(new FieldDef("qa_employee", "QA Employee", FieldType.FK).refTable("employee"));
        TABLES.put(ipcInspection.key, ipcInspection);

        // ---------------- pallet ----------------
        // Extended with the fields from the real pallet tag (Exp Date,
        // Customer, Product Name, Item#, Cartons Qty in Shipper/Cases, Work
        // Order No, Moved By QA, Date, Cases per Layer, Total Layers). Run
        // PPES_extended_fields_schema.sql before using these.
        TableDef pallet = new TableDef("pallet", "Pallet", "প্যালেট", "pallet_id")
                .display("pallet_number")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("pallet_number", "Pallet Number", FieldType.TEXT).required(true))
                .field(new FieldDef("carton_qty", "Carton Qty", FieldType.NUMBER))
                .field(new FieldDef("status", "Status", FieldType.SELECT).options("In Progress", "Completed", "Dispatched"))
                .field(new FieldDef("exp_date", "Exp Date", FieldType.DATE))
                .field(new FieldDef("customer", "Customer", FieldType.TEXT))
                .field(new FieldDef("product_name", "Product Name", FieldType.TEXT))
                .field(new FieldDef("item_no", "Item #", FieldType.TEXT))
                .field(new FieldDef("cartons_qty_in_shipper", "Cartons Qty in Shipper/Cases", FieldType.NUMBER))
                .field(new FieldDef("work_order_no", "Work Order No", FieldType.TEXT))
                .field(new FieldDef("moved_by_qa", "Moved By QA", FieldType.TEXT))
                .field(new FieldDef("moved_date", "Date (Moved)", FieldType.DATE))
                .field(new FieldDef("cases_per_layer", "Cases per Layer", FieldType.NUMBER))
                .field(new FieldDef("total_layers", "Total Layers", FieldType.NUMBER));
        TABLES.put(pallet.key, pallet);

        // ---------------- shipper_label ----------------
        TableDef shipperLabel = new TableDef("shipper_label", "Shipper Label", "শিপার লেবেল", "label_id")
                .display("shipper_number")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("shipper_number", "Shipper Number", FieldType.TEXT).required(true))
                .field(new FieldDef("barcode", "Barcode", FieldType.TEXT))
                .field(new FieldDef("verified_by", "Verified By", FieldType.FK).refTable("employee"))
                .field(new FieldDef("verification_time", "Verification Time", FieldType.DATETIME));
        TABLES.put(shipperLabel.key, shipperLabel);

        // ---------------- reconciliation ----------------
        // Extended with Incoming Sample Qty, Loose Products (pieces), and
        // Return Labels Qty. Run PPES_extended_fields_2_schema.sql before
        // using these.
        TableDef reconciliation = new TableDef("reconciliation", "Reconciliation", "রিকনসিলিয়েশন", "rec_id")
                .display("rec_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("material_id", "Raw Material", FieldType.FK).refTable("raw_material_master").required(true))
                .field(new FieldDef("received_qty", "Received Qty", FieldType.DECIMAL))
                .field(new FieldDef("used_qty", "Used Qty", FieldType.DECIMAL))
                .field(new FieldDef("returned_qty", "Returned Qty", FieldType.DECIMAL))
                .field(new FieldDef("waste_qty", "Waste Qty", FieldType.DECIMAL))
                .field(new FieldDef("incoming_sample_qty", "Incoming Sample Qty", FieldType.DECIMAL))
                .field(new FieldDef("loose_products_pieces", "Loose Products (Pieces)", FieldType.NUMBER))
                .field(new FieldDef("return_labels_qty", "Return Labels Qty", FieldType.NUMBER));
        TABLES.put(reconciliation.key, reconciliation);

        // ---------------- shift_handover ----------------
        TableDef shiftHandover = new TableDef("shift_handover", "Shift Handover", "শিফট হ্যান্ডওভার", "handover_id")
                .display("handover_id")
                .field(new FieldDef("lot_id", "Lot", FieldType.FK).refTable("batch_lot").required(true))
                .field(new FieldDef("from_shift", "From Shift", FieldType.LOOKUP).refTable("shift_master").refColumn("shift_name"))
                .field(new FieldDef("to_shift", "To Shift", FieldType.LOOKUP).refTable("shift_master").refColumn("shift_name"))
                .field(new FieldDef("remarks", "Remarks", FieldType.TEXTAREA))
                .field(new FieldDef("approved_by", "Approved By", FieldType.FK).refTable("employee"));
        TABLES.put(shiftHandover.key, shiftHandover);

        // ---------------- in_process_waste ----------------
        // New table (Customer, Product, W.O. No, Lot No, Exp. Date, Rejects)
        // matching the red "IN-PROCESS WASTE" tag. Run
        // PPES_extended_fields_schema.sql before using this.
        TableDef inProcessWaste = new TableDef("in_process_waste", "In-Process Waste", "ইন-প্রসেস ওয়েস্ট", "waste_id")
                .display("waste_id")
                .field(new FieldDef("customer", "Customer", FieldType.TEXT).required(true))
                .field(new FieldDef("product", "Product", FieldType.TEXT).required(true))
                .field(new FieldDef("wo_no", "W.O. No", FieldType.TEXT))
                .field(new FieldDef("lot_no", "Lot No", FieldType.TEXT))
                .field(new FieldDef("exp_date", "Exp. Date", FieldType.DATE))
                .field(new FieldDef("reject_qty", "Reject Qty", FieldType.NUMBER))
                .field(new FieldDef("reject_loose_qty", "Reject Loose Qty", FieldType.NUMBER))
                .field(new FieldDef("total_incoming", "Total Incoming (e.g. 5 Bags @ 100)", FieldType.TEXT))
                .field(new FieldDef("reject_remarks", "Reject Remarks", FieldType.TEXTAREA));
        TABLES.put(inProcessWaste.key, inProcessWaste);

        // ---------------- pallet_placard ----------------
        // New table (Part No, Description, Purchase Order No, Supplier Lot
        // No, Case Qty on Pallet) matching the supplier "PALLET PLACARD"
        // tag. Run PPES_extended_fields_schema.sql before using this.
        TableDef palletPlacard = new TableDef("pallet_placard", "Pallet Placard", "প্যালেট প্ল্যাকার্ড", "placard_id")
                .display("part_no")
                .field(new FieldDef("pallet_id", "Pallet (optional)", FieldType.FK).refTable("pallet"))
                .field(new FieldDef("part_no", "Part No", FieldType.TEXT).required(true))
                .field(new FieldDef("description", "Description", FieldType.TEXT))
                .field(new FieldDef("purchase_order_no", "Purchase Order No", FieldType.TEXT))
                .field(new FieldDef("supplier_lot_no", "Supplier Lot No", FieldType.TEXT))
                .field(new FieldDef("cases_per_layer", "Cases per Layer", FieldType.NUMBER))
                .field(new FieldDef("layers_per_pallet", "Layers per Pallet", FieldType.NUMBER))
                .field(new FieldDef("total_pieces", "Total Pieces", FieldType.NUMBER))
                .field(new FieldDef("case_qty_text", "Case Qty on Pallet (e.g. 12CS/LYR, 4LYRS-38,400PCS)", FieldType.TEXT));
        TABLES.put(palletPlacard.key, palletPlacard);

        // ---------------- Sidebar menu grouping ----------------
        MENU.put("Masters", Arrays.asList("product_master", "employee", "shift_master", "raw_material_master", "users"));
        MENU.put("Order Intake", Arrays.asList("customer_order"));
        MENU.put("Production (Lot Management)", Arrays.asList(
                "batch_lot", "raw_material_receiving", "startup_checklist",
                "production_target", "blister_production", "ipc_inspection"));
        MENU.put("Packing & Dispatch", Arrays.asList("pallet", "pallet_placard", "shipper_label"));
        MENU.put("Shift & Reconciliation", Arrays.asList("reconciliation", "shift_handover"));
        MENU.put("Waste & Placards", Arrays.asList("in_process_waste"));
    }
}
