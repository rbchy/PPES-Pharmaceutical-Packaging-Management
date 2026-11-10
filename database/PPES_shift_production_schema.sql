-- ============================================================
-- Shift Production Log — new table added to the existing "ppes" schema.
--
-- Header block requested (Order Qty, Work Order No, Lot No, Item No,
-- Product Name, Exp Date, Ctns Qty in a Shipper) is NOT duplicated as a
-- new table — it's the exact same header already stored in
-- carton_tracking_header (see PPES_carton_tracking_schema.sql), keyed
-- off the same lot_id:
--   Work Order No  -> batch_lot.batch_number   (via lot_id)
--   Lot No         -> batch_lot.lot_number      (via lot_id)
--   Item No        -> product_master.product_code (via batch_lot.product_id)
--   Product Name   -> product_master.product_name  (via batch_lot.product_id)
--   Order Qty      -> carton_tracking_header.order_qty
--   Exp Date       -> carton_tracking_header.expiry_date
--   Ctns Qty in a Shipper -> carton_tracking_header.ctns_in_shipper
--
-- If you haven't run PPES_carton_tracking_schema.sql yet, run it first
-- (creates carton_tracking_header) — the Shift Production Log form
-- reads/writes the same header table so you only ever fill it in once
-- per lot, from either screen.
--
-- Only the per-shift grid rows are genuinely new, so only ONE new
-- table is created here:
--   DATE -> log_date, SHIFT -> shift_id, FINAL SHIPPER -> final_shipper_no,
--   NO OF SHIPPER -> no_of_shipper, NO OF UNIT PRODUCED -> no_of_unit_produced,
--   TOTAL NO OF UNIT PRODUCED -> total_no_of_unit_produced (running total,
--   auto-calculated by the app), RECORDED BY -> recorded_by (employee)
-- ============================================================

USE ppes;

CREATE TABLE IF NOT EXISTS shift_production_log (
  entry_id                   INT AUTO_INCREMENT PRIMARY KEY,
  lot_id                      INT NOT NULL,
  log_date                    DATE NOT NULL,
  shift_id                    INT,
  final_shipper_no            INT,
  no_of_shipper                INT,
  no_of_unit_produced          INT,
  total_no_of_unit_produced    INT,          -- running cumulative total for this lot
  recorded_by                  INT,
  created_at                   DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_lot_date_shift (lot_id, log_date, shift_id),
  FOREIGN KEY (lot_id) REFERENCES batch_lot(lot_id),
  FOREIGN KEY (shift_id) REFERENCES shift_master(shift_id),
  FOREIGN KEY (recorded_by) REFERENCES employee(employee_id)
);

-- ============================================================
-- Sample data — 4 demo rows against whatever lot/shift/employee IDs
-- actually exist in your real database (no hardcoded IDs, so this
-- never fails with a foreign key error even on a fresh schema).
-- ============================================================

SET @demo_lot_id      = (SELECT lot_id FROM batch_lot ORDER BY lot_id LIMIT 1);
SET @demo_shift_id    = (SELECT shift_id FROM shift_master ORDER BY shift_id LIMIT 1);
SET @demo_employee_id = (SELECT employee_id FROM employee ORDER BY employee_id LIMIT 1);

INSERT INTO shift_production_log
  (lot_id, log_date, shift_id, final_shipper_no, no_of_shipper, no_of_unit_produced, total_no_of_unit_produced, recorded_by)
SELECT * FROM (
  SELECT @demo_lot_id AS lot_id, CURDATE() - INTERVAL 3 DAY AS log_date, @demo_shift_id AS shift_id, 2640 AS final_shipper_no, 124 AS no_of_shipper, 4464 AS no_of_unit_produced, 4464 AS total_no_of_unit_produced, @demo_employee_id AS recorded_by
  UNION ALL SELECT @demo_lot_id, CURDATE() - INTERVAL 2 DAY, @demo_shift_id, 2742, 102, 3672, 8136, @demo_employee_id
  UNION ALL SELECT @demo_lot_id, CURDATE() - INTERVAL 1 DAY, @demo_shift_id, 2835, 93, 3348, 11484, @demo_employee_id
  UNION ALL SELECT @demo_lot_id, CURDATE(),                  @demo_shift_id, 2957, 122, 4392, 15876, @demo_employee_id
) AS t
WHERE @demo_lot_id IS NOT NULL;
