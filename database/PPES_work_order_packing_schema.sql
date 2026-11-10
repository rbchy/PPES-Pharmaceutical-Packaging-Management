-- ============================================================
-- Work Order Packaging Requirement — new table added to "ppes".
--
-- Stores the packing conversion ratios for a work order (lot), e.g.
-- for blistering Sudafed: 12 tablets/blister, 2 blisters/carton,
-- 6 cartons/bundle, 12 bundles/shipper (= 72 cartons/shipper),
-- 12 shippers/layer x 7 layers/pallet (= 84 shippers/pallet =
-- 6048 cartons/pallet).
--
-- Everything else (total blisters/cartons/bundles/shippers/pallets
-- needed for the full order, and how many are still needed after
-- partial production) is CALCULATED LIVE by the app from these
-- ratios + order_qty_cartons + how many shippers have actually been
-- produced so far (pulled from shift_production_log) — nothing
-- duplicated or pre-computed into the database.
-- ============================================================

USE ppes;

CREATE TABLE IF NOT EXISTS work_order_packing_spec (
  spec_id                INT AUTO_INCREMENT PRIMARY KEY,
  lot_id                  INT NOT NULL UNIQUE,
  order_qty_cartons       INT NOT NULL,
  tablets_per_blister     INT NOT NULL DEFAULT 12,
  blisters_per_carton     INT NOT NULL DEFAULT 2,
  cartons_per_bundle      INT NOT NULL DEFAULT 6,
  bundles_per_shipper     INT NOT NULL DEFAULT 12,
  shippers_per_layer      INT NOT NULL DEFAULT 12,
  layers_per_pallet       INT NOT NULL DEFAULT 7,
  created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (lot_id) REFERENCES batch_lot(lot_id)
);

-- ============================================================
-- Sample data — the exact packing spec described for Sudafed,
-- applied to whatever lot_id is actually the smallest one in your
-- real batch_lot table (no hardcoded IDs, so this never fails with
-- a foreign key error).
-- ============================================================

SET @demo_lot_id = (SELECT lot_id FROM batch_lot ORDER BY lot_id LIMIT 1);

INSERT INTO work_order_packing_spec
  (lot_id, order_qty_cartons, tablets_per_blister, blisters_per_carton,
   cartons_per_bundle, bundles_per_shipper, shippers_per_layer, layers_per_pallet)
SELECT @demo_lot_id, 100000, 12, 2, 6, 12, 12, 7
WHERE @demo_lot_id IS NOT NULL;
