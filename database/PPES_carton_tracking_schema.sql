-- ============================================================
-- Carton Tracking Log — new tables added to the existing "ppes" schema
-- Mirrors Order_Carton_Tracking_Sheet.xlsx (header block + hourly grid).
--
-- Design note: Customer, Product, W.O. No and Lot No from the sheet are
-- NOT duplicated here — they're already reachable from batch_lot
-- (batch_lot.batch_number = W.O./batch no, plus product_master and
-- customer_order via existing FKs). Only the fields that are genuinely
-- new live in these 2 tables, both keyed off batch_lot.lot_id.
-- ============================================================

USE ppes;

-- ------------------------------------------------------------
-- 1. CARTON TRACKING HEADER (one row per lot)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carton_tracking_header (
  header_id           INT AUTO_INCREMENT PRIMARY KEY,
  lot_id               INT NOT NULL UNIQUE,
  room_no              VARCHAR(50),
  expiry_date          DATE,
  order_qty            INT,
  todays_balance       INT,
  starting_label_no    INT,
  ctns_in_shipper      INT,          -- cartons packed per shipper carton
  shift_standard_ctns  INT,          -- full-shift (8-hr) carton target; 2/4/6-hr targets are derived from this
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (lot_id) REFERENCES batch_lot(lot_id)
);

-- ------------------------------------------------------------
-- 2. CARTON TRACKING HOURLY (checkpoint rows, e.g. every 2 hrs)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS carton_tracking_hourly (
  entry_id            INT AUTO_INCREMENT PRIMARY KEY,
  lot_id               INT NOT NULL,
  hour_mark            INT NOT NULL,          -- 2, 4, 6, 8 ...
  standard_ctns        INT,                   -- = shift_standard_ctns * hour_mark / 8
  label_no_after       INT,                   -- shipper label number read at this checkpoint
  total_shippers       INT,                   -- = label_no_after - starting_label_no
  actual_ctns          INT,                   -- = ctns_in_shipper * total_shippers
  percent_complete     DECIMAL(6,2),          -- = actual_ctns / standard_ctns * 100
  comments             VARCHAR(255),
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_lot_hour (lot_id, hour_mark),
  FOREIGN KEY (lot_id) REFERENCES batch_lot(lot_id)
);

-- ============================================================
-- Sample data — reuses the real numbers from the uploaded sheet
-- so the calculated columns can be checked against the original file.
--
-- NOTE: instead of hardcoding lot_id = 1 (which fails with a foreign
-- key error if your real batch_lot table doesn't have that exact ID),
-- this picks whatever the SMALLEST existing lot_id in your batch_lot
-- table actually is. If batch_lot is empty, these two INSERTs simply
-- do nothing (no error) — add a lot first, then re-run just this
-- block, or use the app's Batch/Lot screen to add one.
-- ============================================================

SET @demo_lot_id = (SELECT lot_id FROM batch_lot ORDER BY lot_id LIMIT 1);

INSERT INTO carton_tracking_header
  (lot_id, room_no, expiry_date, order_qty, todays_balance, starting_label_no, ctns_in_shipper, shift_standard_ctns)
SELECT @demo_lot_id, 'Room-1', '2028-06-30', 145869, 90540, 2516, 36, 15750
WHERE @demo_lot_id IS NOT NULL;

INSERT INTO carton_tracking_hourly
  (lot_id, hour_mark, standard_ctns, label_no_after, total_shippers, actual_ctns, percent_complete, comments)
SELECT * FROM (
  SELECT @demo_lot_id AS lot_id, 2 AS hour_mark, 3937.50 AS standard_ctns, 2640 AS label_no_after, 124 AS total_shippers, 4464 AS actual_ctns, 113.37 AS percent_complete, 'Ahead of target, line running well' AS comments
  UNION ALL SELECT @demo_lot_id, 4, 7875.00, 2742, 226, 8136, 103.31, 'On track'
  UNION ALL SELECT @demo_lot_id, 6, 11812.50, 2835, 319, 11484, 97.22, 'Slight slowdown, minor jam cleared'
  UNION ALL SELECT @demo_lot_id, 8, 15750.00, 2957, 441, 15876, 100.80, 'Shift target met'
) AS t
WHERE @demo_lot_id IS NOT NULL;
