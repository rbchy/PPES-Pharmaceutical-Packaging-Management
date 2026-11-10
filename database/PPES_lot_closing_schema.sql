-- ============================================================
-- Lot Closing (Finish Production) — new table added to "ppes".
--
-- When a lot's blistering is finished against its work order (actual
-- good + reject blister output has caught up with what the order
-- required), this records the closing details and lets you reconcile
-- packaging materials issued for the lot: unused empty cartons, empty
-- shippers, leaflets (by type), and labels get counted and returned to
-- store; reject blister quantity and any loose/unpacked product found
-- are logged separately (per your MBR/MPR).
--
-- Design notes (no duplication):
--   - Good/Reject blister totals are NOT re-entered — they're read live
--     from your existing blister_production table (SUM of good_qty /
--     reject_qty for the lot). lot_closing only stores a SNAPSHOT of
--     those totals at the moment you close the lot, for record-keeping.
--   - Required blister quantity for comparison comes from
--     work_order_packing_spec (order_qty_cartons * blisters_per_carton),
--     if you've filled in that screen for the lot.
--   - Packaging material returns (empty cartons/shippers/leaflets/
--     labels) use your EXISTING reconciliation table (lot_id,
--     material_id, received_qty, used_qty, returned_qty, waste_qty) —
--     no new material-tracking table. This script only adds the
--     packaging item TYPES to raw_material_master (if they don't
--     already exist) so they show up as rows to reconcile.
--   - Closing a lot sets batch_lot.status = 'Closed' (a value the
--     column already supports).
-- ============================================================

USE ppes;

CREATE TABLE IF NOT EXISTS lot_closing (
  closing_id                INT AUTO_INCREMENT PRIMARY KEY,
  lot_id                     INT NOT NULL UNIQUE,
  closing_date               DATE,
  mbr_no                      VARCHAR(50),
  mpr_no                      VARCHAR(50),
  order_qty_cartons           INT,
  total_good_blister_qty      INT,
  total_reject_blister_qty    INT,
  loose_product_qty           INT,
  remarks                     VARCHAR(500),
  closed_by                   INT,
  status                      VARCHAR(20) NOT NULL DEFAULT 'Draft',
  created_at                  DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (lot_id) REFERENCES batch_lot(lot_id),
  FOREIGN KEY (closed_by) REFERENCES employee(employee_id)
);

-- ------------------------------------------------------------
-- Seed packaging item types into raw_material_master, only if a
-- material with that exact name doesn't already exist (safe to
-- re-run this script any time).
-- ------------------------------------------------------------

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-CTN', 'Empty Carton', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Empty Carton');

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-SHP', 'Empty Shipper', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Empty Shipper');

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-LFA', 'Leaflet - Type A', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Leaflet - Type A');

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-LFB', 'Leaflet - Type B', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Leaflet - Type B');

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-SLB', 'Shipper Label', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Shipper Label');

INSERT INTO raw_material_master (material_code, material_name, uom)
SELECT 'PKG-CLB', 'Carton Label', 'Pcs'
WHERE NOT EXISTS (SELECT 1 FROM raw_material_master WHERE material_name = 'Carton Label');

-- ------------------------------------------------------------
-- Sample data — one demo lot_closing row for whatever lot_id is
-- actually the smallest one in your real batch_lot table (no
-- hardcoded IDs, so this never fails with a foreign key error).
-- ------------------------------------------------------------

SET @demo_lot_id      = (SELECT lot_id FROM batch_lot ORDER BY lot_id LIMIT 1);
SET @demo_employee_id = (SELECT employee_id FROM employee ORDER BY employee_id LIMIT 1);

INSERT INTO lot_closing
  (lot_id, closing_date, mbr_no, mpr_no, order_qty_cartons, total_good_blister_qty,
   total_reject_blister_qty, loose_product_qty, remarks, closed_by, status)
SELECT @demo_lot_id, CURDATE(), 'MBR-2026-001', 'MPR-2026-001', 100000, 196000, 4000,
       120, 'Demo closing entry generated with the sample data.', @demo_employee_id, 'Draft'
WHERE @demo_lot_id IS NOT NULL;
