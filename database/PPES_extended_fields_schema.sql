-- ============================================================
-- Extended fields for raw_material_master + pallet, and two new
-- tables (in_process_waste, pallet_placard) — based on the real
-- raw material container labels, in-process waste tag, pallet tag,
-- and supplier pallet placard you photographed.
--
-- IMPORTANT: this script uses plain ALTER TABLE ADD COLUMN (no "IF
-- NOT EXISTS") for maximum MySQL version compatibility. Run this
-- script ONCE. If you need to re-run it, first delete or comment
-- out the ALTER TABLE lines for columns that were already added
-- (re-adding an existing column will error).
-- ============================================================

USE ppes;

-- ------------------------------------------------------------
-- 1. raw_material_master — full container/label detail fields
--    (Item#, Lot#, weights, container/batch/process-order info,
--    dates, manufacturer, pallet id, barcode)
--
-- Note: most of these (Lot#, Container#, weights, dates) describe
-- ONE SPECIFIC received container rather than the material type in
-- general, so a single material (e.g. "Sudafed PE API") will end up
-- with one raw_material_master ROW PER CONTAINER/LOT going forward,
-- not one row per material type as before. That is what you asked
-- for here; if you'd rather keep raw_material_master as a pure
-- catalog and track container-level detail separately, these same
-- columns could instead be added to raw_material_receiving — happy
-- to switch that if you'd prefer.
-- ------------------------------------------------------------

ALTER TABLE raw_material_master
  ADD COLUMN product_no          VARCHAR(30)   AFTER material_code,   -- Item# / Product#
  ADD COLUMN lot_no                VARCHAR(30),
  ADD COLUMN qty                     DECIMAL(14,3),
  ADD COLUMN gross_weight              DECIMAL(12,3),
  ADD COLUMN net_weight                 DECIMAL(12,3),
  ADD COLUMN tare_weight                  DECIMAL(12,3),
  ADD COLUMN container_no                  VARCHAR(30),   -- e.g. "GEN00010"
  ADD COLUMN container_seq                   VARCHAR(20),   -- e.g. "3 of 12" (this container out of total containers in the pallet)
  ADD COLUMN batch_no                          VARCHAR(30),
  ADD COLUMN process_order                       VARCHAR(30),
  ADD COLUMN label_date                            DATETIME,      -- date/time printed on the label
  ADD COLUMN mfg_date                                DATE,
  ADD COLUMN bulk_exp_date                             DATE,
  ADD COLUMN manufactured_by                             VARCHAR(100),
  ADD COLUMN container_pallet_id                           VARCHAR(30),  -- Pallet ID printed on this raw-material label
  ADD COLUMN barcode                                          VARCHAR(100) NOT NULL DEFAULT '';

-- ------------------------------------------------------------
-- 2. IN-PROCESS WASTE — new table
--    (Customer, Product, W.O. No, Lot No, Exp. Date, Rejects)
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS in_process_waste (
  waste_id       INT AUTO_INCREMENT PRIMARY KEY,
  customer        VARCHAR(150),
  product          VARCHAR(150),
  wo_no             VARCHAR(30),
  lot_no             VARCHAR(30),
  exp_date            DATE,
  reject_qty            INT,
  reject_remarks         VARCHAR(255),
  recorded_at              DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 3. pallet — add the fields from the pallet tag photo
--    (Exp Date, Customer, Product Name, Item#, Cartons Qty in
--    Shipper/Cases, Work Order No, Moved By QA, Date, Cases per
--    Layer, Total Layers)
-- ------------------------------------------------------------

ALTER TABLE pallet
  ADD COLUMN exp_date               DATE,
  ADD COLUMN customer                 VARCHAR(150),
  ADD COLUMN product_name               VARCHAR(150),
  ADD COLUMN item_no                     VARCHAR(30),
  ADD COLUMN cartons_qty_in_shipper        INT,        -- e.g. "72 Cases @ 36 Cartons"
  ADD COLUMN work_order_no                   VARCHAR(30),
  ADD COLUMN moved_by_qa                       VARCHAR(100),
  ADD COLUMN moved_date                          DATE,
  ADD COLUMN cases_per_layer                       INT,
  ADD COLUMN total_layers                            INT;

-- ------------------------------------------------------------
-- 4. PALLET PLACARD — new table (the supplier/Neff-style placard)
--    (Part No, Description, Purchase Order No, Supplier Lot No,
--    Case Qty on Pallet — e.g. "12CS/LYR, 4LYRS - 38,400 PCS")
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pallet_placard (
  placard_id            INT AUTO_INCREMENT PRIMARY KEY,
  pallet_id               INT,                      -- optional link to your internal pallet record
  part_no                   VARCHAR(50),
  description                 VARCHAR(200),
  purchase_order_no            VARCHAR(50),
  supplier_lot_no                 VARCHAR(50),
  cases_per_layer                   INT,
  layers_per_pallet                   INT,
  total_pieces                          INT,
  case_qty_text                           VARCHAR(100),  -- raw label text, e.g. "12CS/LYR, 4LYRS - 38,400 PCS"
  created_at                                DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (pallet_id) REFERENCES pallet(pallet_id)
);
