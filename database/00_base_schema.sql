-- ============================================================
-- PPES base schema (run FIRST, before the PPES_*_schema.sql scripts)
-- Generated from src/main/java/com/ppes/model/TableRegistry.java.
-- The original base script was never committed; this rebuilds the
-- core tables WITHOUT the columns/tables that the extended-fields and
-- module scripts add, so those scripts still apply cleanly on top.
-- DatabaseSchemaIT checks every TableRegistry column exists afterwards.
-- ============================================================

CREATE DATABASE IF NOT EXISTS ppes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ppes;

CREATE TABLE IF NOT EXISTS `product_master` (
  `product_id` INT AUTO_INCREMENT PRIMARY KEY,
  `product_code` VARCHAR(150),
  `product_name` VARCHAR(150) NOT NULL,
  `strength` VARCHAR(150),
  `pack_size` VARCHAR(150),
  `blister_per_carton` INT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `customer_order` (
  `order_id` INT AUTO_INCREMENT PRIMARY KEY,
  `consignee_name` VARCHAR(150) NOT NULL,
  `product_name` VARCHAR(150) NOT NULL,
  `order_quantity` INT,
  `customer_po` VARCHAR(150),
  `requested_date` DATE,
  `status` VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `employee` (
  `employee_id` INT AUTO_INCREMENT PRIMARY KEY,
  `employee_name` VARCHAR(150) NOT NULL,
  `designation` VARCHAR(150),
  `department` VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shift_master` (
  `shift_id` INT AUTO_INCREMENT PRIMARY KEY,
  `shift_name` VARCHAR(150) NOT NULL,
  `start_time` TIME,
  `end_time` TIME
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `raw_material_master` (
  `material_id` INT AUTO_INCREMENT PRIMARY KEY,
  `material_code` VARCHAR(150),
  `material_name` VARCHAR(150) NOT NULL,
  `uom` VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `users` (
  `user_id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(150) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `batch_lot` (
  `lot_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_number` VARCHAR(150) NOT NULL,
  `batch_number` VARCHAR(150) NOT NULL,
  `product_id` INT NOT NULL,
  `order_id` INT,
  `line_number` VARCHAR(150),
  `shift` VARCHAR(150),
  `start_time` DATETIME,
  `end_time` DATETIME,
  `status` VARCHAR(50),
  FOREIGN KEY (`product_id`) REFERENCES `product_master`(`product_id`),
  FOREIGN KEY (`order_id`) REFERENCES `customer_order`(`order_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `raw_material_receiving` (
  `receive_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `material_id` INT NOT NULL,
  `required_qty` DECIMAL(14,3),
  `received_qty` DECIMAL(14,3),
  `received_date` DATETIME,
  `verified_by` INT,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`material_id`) REFERENCES `raw_material_master`(`material_id`),
  FOREIGN KEY (`verified_by`) REFERENCES `employee`(`employee_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `startup_checklist` (
  `check_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `check_item` VARCHAR(150) NOT NULL,
  `status` VARCHAR(50),
  `checked_by` INT,
  `checked_time` DATETIME,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`checked_by`) REFERENCES `employee`(`employee_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `production_target` (
  `target_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `hour_mark` INT,
  `target_cartons` INT,
  `actual_cartons` INT,
  `variance` INT,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `blister_production` (
  `production_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `hour_mark` INT,
  `machine_speed` INT,
  `blister_produced` INT,
  `reject_qty` INT,
  `good_qty` INT,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `ipc_inspection` (
  `inspection_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `inspection_time` DATETIME,
  `tablet_check` VARCHAR(50),
  `seal_check` VARCHAR(50),
  `coding_check` VARCHAR(50),
  `qa_employee` INT,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`qa_employee`) REFERENCES `employee`(`employee_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `pallet` (
  `pallet_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `pallet_number` VARCHAR(150) NOT NULL,
  `carton_qty` INT,
  `status` VARCHAR(50),
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shipper_label` (
  `label_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `shipper_number` VARCHAR(150) NOT NULL,
  `barcode` VARCHAR(150),
  `verified_by` INT,
  `verification_time` DATETIME,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`verified_by`) REFERENCES `employee`(`employee_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `reconciliation` (
  `rec_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `material_id` INT NOT NULL,
  `received_qty` DECIMAL(14,3),
  `used_qty` DECIMAL(14,3),
  `returned_qty` DECIMAL(14,3),
  `waste_qty` DECIMAL(14,3),
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`material_id`) REFERENCES `raw_material_master`(`material_id`)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `shift_handover` (
  `handover_id` INT AUTO_INCREMENT PRIMARY KEY,
  `lot_id` INT NOT NULL,
  `from_shift` VARCHAR(150),
  `to_shift` VARCHAR(150),
  `remarks` TEXT,
  `approved_by` INT,
  FOREIGN KEY (`lot_id`) REFERENCES `batch_lot`(`lot_id`),
  FOREIGN KEY (`approved_by`) REFERENCES `employee`(`employee_id`)
) ENGINE=InnoDB;

