-- ============================================================
-- PPES Sample Data (expanded: >=10 rows per table) --
-- Matches the user's REAL "ppes" schema. Respects FK order.
-- ============================================================

USE ppes;

-- product_master
INSERT INTO product_master (product_code, product_name, strength, pack_size, blister_per_carton) VALUES
('PARA500', 'Paracetamol', '500mg', '10x10 Blister', 100),
('AMOX250', 'Amoxicillin', '250mg', '10x10 Blister', 100),
('CET10', 'Cetirizine', '10mg', '10x10 Blister', 120),
('IBU400', 'Ibuprofen', '400mg', '10x10 Blister', 100),
('OME20', 'Omeprazole', '20mg', '10x14 Blister', 84),
('MET500', 'Metformin', '500mg', '10x10 Blister', 100),
('AZI500', 'Azithromycin', '500mg', '1x6 Blister', 60),
('VITC500', 'Vitamin C', '500mg', '10x10 Blister', 100),
('LORA10', 'Loratadine', '10mg', '10x10 Blister', 100),
('RANI150', 'Ranitidine', '150mg', '10x10 Blister', 100);

-- customer_order
INSERT INTO customer_order (consignee_name, product_name, order_quantity, customer_po, requested_date, status) VALUES
('ABC Pharma Distributors', 'Paracetamol 500mg', 50000, 'PO-2026-001', '2026-08-05', 'Confirmed'),
('MediCare Wholesale', 'Amoxicillin 250mg', 30000, 'PO-2026-002', '2026-08-10', 'Pending'),
('HealthPlus Ltd', 'Cetirizine 10mg', 20000, 'PO-2026-003', '2026-08-12', 'In Production'),
('National Drug House', 'Paracetamol 500mg', 40000, 'PO-2026-004', '2026-08-15', 'Pending'),
('Unimed Pharmaceuticals', 'Ibuprofen 400mg', 25000, 'PO-2026-005', '2026-08-18', 'Confirmed'),
('Square Health Care', 'Omeprazole 20mg', 35000, 'PO-2026-006', '2026-08-20', 'Pending'),
('Popular Pharma Ltd', 'Metformin 500mg', 45000, 'PO-2026-007', '2026-08-22', 'In Production'),
('Beximco Distribution', 'Azithromycin 500mg', 15000, 'PO-2026-008', '2026-08-25', 'Completed'),
('Incepta Trading', 'Vitamin C 500mg', 60000, 'PO-2026-009', '2026-08-28', 'Pending'),
('Renata Wholesale', 'Loratadine 10mg', 28000, 'PO-2026-010', '2026-08-30', 'Confirmed');

-- employee
INSERT INTO employee (employee_name, designation, department) VALUES
('Rahim Uddin', 'Line Operator', 'Production'),
('Karim Hossain', 'Shift Supervisor', 'Production'),
('Nasrin Akter', 'QC Officer', 'QC'),
('Jasim Ahmed', 'Warehouse Officer', 'Warehouse'),
('Farzana Islam', 'QA Executive', 'QC'),
('Salma Begum', 'Machine Operator', 'Production'),
('Tanvir Rahman', 'Maintenance Technician', 'Maintenance'),
('Shirin Sultana', 'Production Manager', 'Admin'),
('Delwar Hossain', 'Line Operator', 'Production'),
('Ayesha Siddiqua', 'QC Officer', 'QC');

-- shift_master
INSERT INTO shift_master (shift_name, start_time, end_time) VALUES
('Morning', '06:00:00', '14:00:00'),
('Evening', '14:00:00', '22:00:00'),
('Night', '22:00:00', '06:00:00'),
('Morning-Line2', '06:00:00', '14:00:00'),
('Evening-Line2', '14:00:00', '22:00:00'),
('Night-Line2', '22:00:00', '06:00:00'),
('General', '09:00:00', '17:00:00'),
('Weekend', '08:00:00', '16:00:00'),
('Overtime', '17:00:00', '21:00:00'),
('Rotating', '00:00:00', '00:00:00');

-- raw_material_master
INSERT INTO raw_material_master (material_code, material_name, uom) VALUES
('FOIL-ALU', 'Aluminium Foil', 'Roll'),
('FILM-PVC', 'PVC Film', 'Roll'),
('CTN-BOX', 'Carton Box', 'Pcs'),
('INK-COD', 'Coding Ink', 'Liter'),
('LBL-SHIP', 'Shipper Label Roll', 'Roll'),
('FILM-PVDC', 'PVDC Film', 'Roll'),
('FOIL-BLST', 'Blister Foil Roll', 'Roll'),
('TAPE-GUM', 'Gum Tape', 'Roll'),
('SILICA-GEL', 'Silica Gel Packet', 'Box'),
('LEAFLET-INS', 'Insert Leaflet', 'Pcs');

-- users
INSERT INTO users (username, password_hash, role) VALUES
('admin', 'sha256$iWlhbxtxGli4PpnQnTTzDA==$LF7k722ahOyCn4vIRb2f2bh50pembqZPj7ZTC7r5Cek=', 'Admin'),
('rahim.op', 'sha256$wTcayhAZBedE9VJeRgFsCg==$12eJAJsgvKSnmWO8fevqSOf+MfCMhN/B81A7OM71Qk8=', 'Production'),
('karim.sup', 'sha256$QxsAvnaurhVBZLHr6T6N+g==$ZVzLe6SeHvQLI04YECDFM2/GY4diIbpcfXE1NYyvyCc=', 'Production'),
('nasrin.qc', 'sha256$sHnKFxQOb3lGttxbbdygCA==$OqFaINiPTPYdUTC+ZDEztJoHwcUTnnsrdFnKjbk3IIY=', 'QC'),
('jasim.wh', 'sha256$t8hE7e6jfs6EdznMHZUx7A==$yYSTMfnbMCrMT5CvoceRdGVr2NiNp3PI6iauKCo/6Qg=', 'Warehouse'),
('farzana.qa', 'sha256$wDnQEp/Lvcor1LUCsHdSGg==$44gd1NMLMHK5MP9vTheoGXH/yGfPWP5+dGD0jOhol04=', 'QC'),
('salma.op', 'sha256$3LAtpdDH7GW8mqfSt7FttA==$mgRgy5KpUaP7YnBHs0O+shrhFdbQDAOcnS3iHn7St4o=', 'Production'),
('tanvir.mt', 'sha256$kx8y8ELY0XIdSE/fl+cikw==$jASo8ZZQXlM2QQLuMA5UKhU4QxH/ecv5zuD9VlV/z6c=', 'Maintenance'),
('shirin.mgr', 'sha256$h6LXZLKOuFEzFXtTAMJSzA==$eALeCj7KNunPbqCBnTP7AeDluNfNLmsAElgV5HPLZrI=', 'Admin'),
('delwar.op', 'sha256$nOmkI4ljhFgWw8qqYfEXsw==$aW0Bl3eO7VNiQgYsdjnsIz9aSa5bobE/nPM0xb7n9TM=', 'Production');

-- batch_lot
INSERT INTO batch_lot (lot_number, batch_number, product_id, order_id, line_number, shift, start_time, end_time, status) VALUES
('LOT-0001', 'BATCH-A101', 1, 1, 'Line-1', 'Morning', '2026-08-05 07:00:00', '2026-08-05 15:00:00', 'Completed'),
('LOT-0002', 'BATCH-A102', 2, 2, 'Line-2', 'Evening', '2026-08-10 14:30:00', NULL, 'Running'),
('LOT-0003', 'BATCH-A103', 3, 3, 'Line-1', 'Morning', '2026-08-12 06:30:00', NULL, 'Running'),
('LOT-0004', 'BATCH-A104', 4, 4, 'Line-3', 'Night', NULL, NULL, 'Planned'),
('LOT-0005', 'BATCH-A105', 5, 5, 'Line-2', 'Morning-Line2', '2026-08-18 06:15:00', '2026-08-18 14:00:00', 'Completed'),
('LOT-0006', 'BATCH-A106', 6, 6, 'Line-1', 'Evening-Line2', '2026-08-20 14:00:00', NULL, 'Running'),
('LOT-0007', 'BATCH-A107', 7, 7, 'Line-3', 'General', '2026-08-22 09:00:00', NULL, 'Running'),
('LOT-0008', 'BATCH-A108', 8, 8, 'Line-2', 'Night-Line2', '2026-08-24 22:00:00', '2026-08-25 06:00:00', 'Completed'),
('LOT-0009', 'BATCH-A109', 9, 9, 'Line-1', 'Weekend', NULL, NULL, 'Planned'),
('LOT-0010', 'BATCH-A110', 10, 10, 'Line-3', 'Overtime', '2026-08-29 17:00:00', '2026-08-29 21:00:00', 'Completed');

-- raw_material_receiving
INSERT INTO raw_material_receiving (lot_id, material_id, required_qty, received_qty, received_date, verified_by) VALUES
(1, 1, 320.0, 318.0, '2026-08-01 09:00:00', 4),
(2, 2, 340.0, 336.0, '2026-08-02 09:00:00', 4),
(3, 3, 360.0, 360.0, '2026-08-03 09:00:00', 4),
(4, 4, 380.0, 378.0, '2026-08-04 09:00:00', 4),
(5, 5, 400.0, 396.0, '2026-08-05 09:00:00', 4),
(6, 6, 420.0, 420.0, '2026-08-06 09:00:00', 4),
(7, 7, 440.0, 438.0, '2026-08-07 09:00:00', 4),
(8, 8, 460.0, 456.0, '2026-08-08 09:00:00', 4),
(9, 9, 480.0, 480.0, '2026-08-09 09:00:00', 4),
(10, 10, 500.0, 498.0, '2026-08-10 09:00:00', 4);

-- startup_checklist
INSERT INTO startup_checklist (lot_id, check_item, status, checked_by, checked_time) VALUES
(1, 'Line clearance completed', 'OK', 2, '2026-08-01 06:45:00'),
(2, 'Machine cleaning verified', 'OK', 2, '2026-08-02 06:45:00'),
(3, 'Coding machine set-up checked', 'OK', 2, '2026-08-03 06:45:00'),
(4, 'Weighing scale calibration checked', 'OK', 2, '2026-08-04 06:45:00'),
(5, 'Blister foil batch verified', 'OK', 2, '2026-08-05 06:45:00'),
(6, 'Carton stock verified', 'OK', 2, '2026-08-06 06:45:00'),
(7, 'AC/temperature check done', 'OK', 2, '2026-08-07 06:45:00'),
(8, 'Humidity check done', 'OK', 2, '2026-08-08 06:45:00'),
(9, 'Previous batch documents cleared', 'OK', 2, '2026-08-09 06:45:00'),
(10, 'Safety guard check done', 'OK', 2, '2026-08-10 06:45:00');

-- production_target
INSERT INTO production_target (lot_id, hour_mark, target_cartons, actual_cartons, variance) VALUES
(1, 1, 410, 400, -10),
(2, 1, 420, 430, 10),
(3, 1, 430, 420, -10),
(4, 1, 440, 450, 10),
(5, 1, 450, 440, -10),
(6, 1, 460, 470, 10),
(7, 1, 470, 460, -10),
(8, 1, 480, 490, 10),
(9, 1, 490, 480, -10),
(10, 1, 500, 510, 10);

-- blister_production
INSERT INTO blister_production (lot_id, hour_mark, machine_speed, blister_produced, reject_qty, good_qty) VALUES
(1, 1, 110, 4100, 35, 4065),
(2, 1, 120, 4200, 40, 4160),
(3, 1, 100, 4300, 45, 4255),
(4, 1, 110, 4400, 50, 4350),
(5, 1, 120, 4500, 30, 4470),
(6, 1, 100, 4600, 35, 4565),
(7, 1, 110, 4700, 40, 4660),
(8, 1, 120, 4800, 45, 4755),
(9, 1, 100, 4900, 50, 4850),
(10, 1, 110, 5000, 30, 4970);

-- ipc_inspection
INSERT INTO ipc_inspection (lot_id, inspection_time, tablet_check, seal_check, coding_check, qa_employee) VALUES
(1, '2026-08-01 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(2, '2026-08-02 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(3, '2026-08-03 10:00:00', 'Pass', 'Fail', 'Pass', 5),
(4, '2026-08-04 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(5, '2026-08-05 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(6, '2026-08-06 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(7, '2026-08-07 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(8, '2026-08-08 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(9, '2026-08-09 10:00:00', 'Pass', 'Pass', 'Pass', 5),
(10, '2026-08-10 10:00:00', 'Pass', 'Pass', 'Pass', 5);

-- pallet
INSERT INTO pallet (lot_id, pallet_number, carton_qty, status) VALUES
(1, 'PLT-0001', 405, 'Completed'),
(2, 'PLT-0002', 410, 'Completed'),
(3, 'PLT-0003', 415, 'Completed'),
(4, 'PLT-0004', 420, 'Completed'),
(5, 'PLT-0005', 425, 'Completed'),
(6, 'PLT-0006', 430, 'Completed'),
(7, 'PLT-0007', 435, 'In Progress'),
(8, 'PLT-0008', 440, 'In Progress'),
(9, 'PLT-0009', 445, 'In Progress'),
(10, 'PLT-0010', 450, 'In Progress');

-- shipper_label
INSERT INTO shipper_label (lot_id, shipper_number, barcode, verified_by, verification_time) VALUES
(1, 'SHP-0001', '8901234500011', 4, '2026-08-01 16:00:00'),
(2, 'SHP-0002', '8901234500012', 4, '2026-08-02 16:00:00'),
(3, 'SHP-0003', '8901234500013', 4, '2026-08-03 16:00:00'),
(4, 'SHP-0004', '8901234500014', 4, '2026-08-04 16:00:00'),
(5, 'SHP-0005', '8901234500015', 4, '2026-08-05 16:00:00'),
(6, 'SHP-0006', '8901234500016', 4, '2026-08-06 16:00:00'),
(7, 'SHP-0007', '8901234500017', 4, '2026-08-07 16:00:00'),
(8, 'SHP-0008', '8901234500018', 4, '2026-08-08 16:00:00'),
(9, 'SHP-0009', '8901234500019', NULL, NULL),
(10, 'SHP-0010', '8901234500020', NULL, NULL);

-- reconciliation
INSERT INTO reconciliation (lot_id, material_id, received_qty, used_qty, returned_qty, waste_qty) VALUES
(1, 1, 320.0, 300.0, 10.0, 10.0),
(2, 2, 340.0, 320.0, 10.0, 10.0),
(3, 3, 360.0, 340.0, 10.0, 10.0),
(4, 4, 380.0, 360.0, 10.0, 10.0),
(5, 5, 400.0, 380.0, 10.0, 10.0),
(6, 6, 420.0, 400.0, 10.0, 10.0),
(7, 7, 440.0, 420.0, 10.0, 10.0),
(8, 8, 460.0, 440.0, 10.0, 10.0),
(9, 9, 480.0, 460.0, 10.0, 10.0),
(10, 10, 500.0, 480.0, 10.0, 10.0);

-- shift_handover
INSERT INTO shift_handover (lot_id, from_shift, to_shift, remarks, approved_by) VALUES
(1, 'Morning', 'Evening', 'All targets met, no pending issue.', 2),
(2, 'Evening', 'Night', 'Machine speed reduced due to minor jam, resolved.', 2),
(3, 'Night', 'Morning-Line2', 'IPC seal check failed once, corrected and re-verified.', 2),
(4, 'Morning-Line2', 'Evening-Line2', 'Handover done smoothly, no open issue.', 2),
(5, 'Evening-Line2', 'Night-Line2', 'Minor material shortage noted, replenished next shift.', 2),
(6, 'Night-Line2', 'General', 'Line cleared and ready for next shift.', 2),
(7, 'General', 'Weekend', 'QA hold released before handover.', 2),
(8, 'Weekend', 'Overtime', 'Carton shortage flagged for next shift.', 2),
(9, 'Overtime', 'Rotating', 'No issues, production on schedule.', 2),
(10, 'Rotating', 'Morning', 'Overtime shift completed target early.', 2);
