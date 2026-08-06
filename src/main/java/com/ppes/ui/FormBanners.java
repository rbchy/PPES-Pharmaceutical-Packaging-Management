package com.ppes.ui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps each of the 16 real table keys to its own banner image (a distinctly
 * cropped/tinted version of the same uploaded factory photo used elsewhere
 * in the app — see src/main/resources/images/forms/*.jpg) and a one-line
 * subtitle, so every generic CrudPanel form gets a banner relevant to its
 * own title instead of all screens looking identical.
 */
public final class FormBanners {

    private static final Map<String, String> IMAGE = new LinkedHashMap<>();
    private static final Map<String, String> SUBTITLE = new LinkedHashMap<>();

    static {
        put("product_master", "Master list of packaged products (name, strength, pack size)");
        put("customer_order", "Incoming customer orders and requested quantities");
        put("employee", "Employee master — names, designation, department");
        put("shift_master", "Shift definitions (name, start/end time)");
        put("raw_material_master", "Raw material & packaging item master list");
        put("users", "System users and login roles");
        put("batch_lot", "Batch / Lot register — the work order for each production run");
        put("raw_material_receiving", "Materials issued from store for a lot");
        put("startup_checklist", "Line clearance / startup checklist per lot");
        put("production_target", "Hourly production target vs actual (cartons)");
        put("blister_production", "Blister production — good qty, reject qty, machine speed");
        put("ipc_inspection", "In-process QC checks — tablet, seal, coding");
        put("pallet", "Pallet register — carton quantity, dispatch status");
        put("shipper_label", "Shipper label register and verification");
        put("reconciliation", "Raw material reconciliation — issued/used/returned/waste");
        put("shift_handover", "Shift handover notes between outgoing/incoming shift");
        put("in_process_waste", "In-process waste tag — customer, product, W.O./Lot, rejects");
        put("pallet_placard", "Supplier pallet placard — part no, PO, supplier lot, case qty");
    }

    private static void put(String tableKey, String subtitle) {
        IMAGE.put(tableKey, "/images/forms/" + tableKey + ".jpg");
        SUBTITLE.put(tableKey, subtitle);
    }

    private FormBanners() {
    }

    public static String imageFor(String tableKey) {
        return IMAGE.getOrDefault(tableKey, "/images/cover.jpg");
    }

    public static String subtitleFor(String tableKey) {
        return SUBTITLE.getOrDefault(tableKey, "");
    }
}
