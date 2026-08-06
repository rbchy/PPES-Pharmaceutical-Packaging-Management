# PPES—Pharmaceuticals_Packaging_Management_System

![Java](https://img.shields.io/badge/Java-11-orange?logo=java)
![Maven](https://img.shields.io/badge/Build-Maven-blue?logo=apachemaven)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![JDBC](https://img.shields.io/badge/JDBC-Connector%2FJ-green)
![Swing](https://img.shields.io/badge/Java-Swing%20%2F%20AWT-red)
![PHP](https://img.shields.io/badge/PHP-8-777BB4?logo=php)
![Bootstrap](https://img.shields.io/badge/UI-Bootstrap%205-7952B3?logo=bootstrap)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**Author:** Ranajit Baran Chowdhury — Software Programmer
**Email:** chyranajit@gmail.com
**GitHub:** [@ranajitchowdhury](https://github.com/rbchy)

A pharmaceuticals packaging line management system built as **two apps sharing one MySQL database**: a **Java Swing** desktop application (`PPES_JavaApp`) and a **PHP + Bootstrap** web application (`PPES_WebApp`) for mobile/tablet/browser access. It covers product & customer masters, batch/lot tracking, raw material receiving, in-process checks, shift production logs, carton tracking, work-order packaging math, lot closing, pallets/shippers, reconciliation, and login authentication — with data entered in either app instantly visible in the other.

---

## 📋 Quick Overview

| Component | Desktop — `PPES_JavaApp` | Web — `PPES_WebApp` |
|-----------|---------------------------|------------------------|
| **Frontend** | Java Swing / AWT (top-bar navigation GUI) | PHP + Bootstrap 5 (responsive, mobile-first) |
| **Backend Logic** | Core Java (OOP), `TableDef`/`FieldDef` metadata-driven CRUD | PHP (PDO), metadata-driven generic CRUD engine (`crud.php`) |
| **Database** | MySQL 8.0 | MySQL 8.0 — same `ppes` schema, no sync needed |
| **Connectivity** | JDBC via `mysql-connector-j` | PDO MySQL driver |
| **Build** | Maven (`pom.xml`, Java 11, shaded runnable jar) | No build step — plain PHP, run via Apache/XAMPP |
| **Auth** | Salted SHA-256, shared `users` table | Identical hash format — same login works in both apps |
| **Document Output** | Apache POI (Excel export/import) | — |
| **Look & Feel** | FlatLaf dark theme, teal/slate accent | Matching teal/slate Bootstrap theme + shared banner images |

---

## ✨ Key Features

### 🔐 Login / Authentication
- Username/password login screen in both apps (`LoginDialog.java` / `login.php`), backed by the same `users` table
- Salted SHA-256 password hashing, byte-identical format on both sides — one account, works everywhere
- "Skip Login (Setup Mode)" escape hatch for first-time use before any user exists

### 🗂️ Masters & Data Tables (18 tables)
- Product Master, Customer Order, Employee, Shift Master, Raw Material Master, Users, Batch/Lot, Raw Material Receiving, Startup Checklist, Production Target, Blister Production, IPC Inspection, Pallet, Shipper Label, Reconciliation, Shift Handover, In-Process Waste, Pallet Placard
- Full Add / Edit / Delete / List for every table, generated from a single shared metadata registry (`TableRegistry.java` / `includes/tables.php`) — no per-table boilerplate
- Grouped navigation: Masters, Order Intake, Production (Lot Management), Packing & Dispatch, Shift & Reconciliation, Waste & Placards

### 🏭 Production & Lot Management
- **Work Order — Packaging Requirement**: converts an order quantity into required blisters → cartons → bundles → shippers → pallets → labels, tracking produced-so-far vs remaining
- **Carton Tracking Log**: hourly checkpoint sheet with live standard-vs-actual carton and percent-complete formulas
- **Shift Production Log**: per-shift entries with an auto-calculated running unit-produced total
- **Lot Closing**: good/reject blister summary against spec (Complete/In Progress status), MBR/MPR closing details, and packaging-material return reconciliation

### 🧮 Batch Yield Reconciliation Calculator
- Live cGMP PASS/FAIL yield check — Actual Yield and Reconciliation Yield percentages against a 99.0%–101.0% limit, with instant status coloring

### 📥 Excel Export / Import (Desktop)
- Export any table's current list to `.xlsx` via Apache POI
- Bulk-import records from `.xlsx` (column headers matched to field names)

### 📊 Reports (Desktop)
- 6 pre-built reports: Lot Summary, Production vs Target, IPC Inspection, Raw Material Reconciliation, Pallet & Shipper, Shift Handover — each exportable to Excel

### 📚 Bilingual Help (Bangla / English)
- In-app Help covering app usage per module, plus a general Lot-Starting SOP reference: department responsibilities (Inventory, QC, Validation, EHS, Production), OEB1–OEB5 exposure bands, GMP/cGMP/SOP/FDA glossary, and Line Leader vs Packer roles

---

## 📂 Repository Structure

```
PPES/
├── PPES_JavaApp/                Desktop app (Java Swing, Maven)
│   ├── pom.xml
│   ├── src/main/java/com/ppes/
│   │   ├── ui/                   Swing screens (MainFrame, CrudPanel, 5 custom modules, dialogs)
│   │   ├── model/                 TableDef / FieldDef / TableRegistry (schema metadata)
│   │   ├── db/                    DB connection + PasswordUtil
│   │   └── excel/                 Apache POI export/import
│   ├── src/main/resources/images/     Banners + per-table form images
│   ├── *.sql                      Schema + sample data scripts
│   └── README.txt                 Bangla build/run instructions
│
├── PPES_WebApp/                  Web app (PHP + MySQL, Bootstrap 5)
│   ├── config.php                 DB connection settings
│   ├── includes/tables.php         Table metadata (drives the generic CRUD engine)
│   ├── includes/auth.php           Login/session guard, password hashing
│   ├── includes/header.php          Shared 3-row top navigation
│   ├── crud.php                      Generic Add/Edit/Delete/List engine for all 18 tables
│   ├── login.php / logout.php         Authentication
│   ├── help.php                        Bilingual help + SOP reference
│   ├── work_order_packaging.php, carton_tracking.php, shift_production.php,
│   │   lot_closing.php, batch_yield.php    The 5 custom modules
│   ├── images/                          Shared banner images
│   └── README.txt                       Bangla setup/run instructions
│
└── *.sql                          Root-level schema migration scripts
```

---

## 🚀 Getting Started

### Prerequisites
- MySQL 8.x (or MariaDB) with a database named `ppes`. Run the `.sql` files in this repo (base schema, then the `*_schema.sql` migration files in the order they were added) to create the tables.
- Desktop app: JDK 11+ and Maven.
- Web app: PHP 8+ with the `pdo_mysql` extension (e.g. via XAMPP/MAMP) and Apache/Nginx.

### Desktop app (`PPES_JavaApp`)
```bash
cd PPES_JavaApp
mvn clean package
java -jar target/ppes-desktop.jar
```
Edit the DB connection details before first run — see `PPES_JavaApp/README.txt` for full Bangla step-by-step instructions.

### Web app (`PPES_WebApp`)
```bash
# 1. Copy the PPES_WebApp folder into your web server's document root
# 2. Edit PPES_WebApp/config.php with your MySQL host/user/password
# 3. Start Apache/PHP, then open in a browser:
http://localhost/PPES_WebApp/
```
See `PPES_WebApp/README.txt` for full Bangla setup instructions, including running it over a local WiFi network so mobile devices can reach it.

### First login
If the `users` table is empty, use **"Skip Login (Setup Mode)"** on the login screen (either app), then create your first user from Masters → Users. The same username/password then works in both the desktop and web apps.

---

## 🛠️ Tech Stack

**Desktop:** Java 11, Swing, FlatLaf (dark theme), MySQL Connector/J, Apache POI, Maven Shade Plugin (single runnable jar).

**Web:** PHP 8 (PDO), MySQL, Bootstrap 5 (CDN), vanilla JS (no build step required).

---

## 📄 License

MIT — see [LICENSE](LICENSE) (add a `LICENSE` file before making the repository public).
