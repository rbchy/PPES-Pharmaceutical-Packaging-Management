-- ============================================================
-- Second round of field extensions — production_target,
-- reconciliation, in_process_waste.
--
-- IMPORTANT: plain ALTER TABLE ADD COLUMN (no "IF NOT EXISTS") for
-- maximum MySQL version compatibility. Run this script ONCE.
-- ============================================================

USE ppes;

-- ------------------------------------------------------------
-- 1. production_target — add Order Qty, Today's Balance, Product to
--    be Produced, Start Label, and pallet packing detail (bundles
--    per shipper, cartons per bundle, pallet qty in cartons)
-- ------------------------------------------------------------

ALTER TABLE production_target
  ADD COLUMN order_qty              INT,
  ADD COLUMN todays_balance           INT,
  ADD COLUMN product_to_produce         VARCHAR(150),
  ADD COLUMN start_label                 INT,
  ADD COLUMN bundles_per_shipper           INT,     -- how many bundles in one shipper
  ADD COLUMN cartons_per_bundle              INT,     -- how many cartons in one bundle
  ADD COLUMN pallet_qty_cartons                INT;     -- pallet quantity, in cartons

-- ------------------------------------------------------------
-- 2. reconciliation — add Incoming Sample Qty, Loose Products (in
--    pieces), Return Labels Qty
-- ------------------------------------------------------------

ALTER TABLE reconciliation
  ADD COLUMN incoming_sample_qty     DECIMAL(12,3),
  ADD COLUMN loose_products_pieces     INT,
  ADD COLUMN return_labels_qty           INT;

-- ------------------------------------------------------------
-- 3. in_process_waste — add Reject Loose Qty and Total Incoming
--    (e.g. "5 Bags @ 100")
-- ------------------------------------------------------------

ALTER TABLE in_process_waste
  ADD COLUMN reject_loose_qty     INT,
  ADD COLUMN total_incoming         VARCHAR(50);   -- e.g. "5 Bags @ 100"
