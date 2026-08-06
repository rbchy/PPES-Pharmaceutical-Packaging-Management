
# PPES — Pharmaceuticals Packaging Management System

PPES is a management system for a pharmaceutical packaging line — product masters, customer orders, batch/lot tracking, raw materials, in-process checks, shift production, carton tracking, work-order packaging math, lot closing, pallets/shippers, and reconciliation reporting. It ships as **two applications that share one MySQL database**, so data entered in either one is instantly visible in the other:

| | PPES_JavaApp | PPES_WebApp |
|---|---|---|
| Platform | Windows / macOS / Linux desktop | Any browser (mobile, tablet, PC) |
| Stack | Java 11, Swing, Maven | PHP 8 + PDO, Bootstrap 5 |
| Use case | Primary desktop workstation app | Floor/mobile access over WiFi or the internet |

Both connect to the same `ppes` MySQL schema — there is no sync step, no data duplication.

## Features

- **18 data tables** across Masters, Order Intake, Production (Lot Management), Packing & Dispatch, Shift & Reconciliation, and Waste & Placards — full Add / Edit / Delete / List for each.
- **5 custom operational modules**:
  - **Work Order — Packaging Requirement**: converts an order quantity into required blisters/cartons/bundles/shippers/pallets/labels, tracks produced-so-far vs remaining.
  - **Carton Tracking Log**: hourly checkpoint sheet with live standard-vs-actual carton and percent-complete formulas.
  - **Shift Production Log**: per-shift production entries with a running unit-produced total.
  - **Lot Closing**: good/reject blister summary against spec, MBR/MPR closing details, and packaging-material return reconciliation.
  - **Batch Yield Reconciliation Calculator**: instant cGMP PASS/FAIL yield check (99.0%–101.0% reconciliation limit).
- **Login / password protection** — both apps read the same `users` table and use an identical salted SHA-256 password hash format, so one account works in both.
- **Bilingual (Bangla/English) Help**, including a general Lot-Starting SOP reference (department responsibilities, OEB1–OEB5, GMP/cGMP/SOP/FDA glossary, Line Leader vs Packer roles).
- Excel export/import (desktop app, via Apache POI) and pre-built reports (Lot Summary, Production vs Target, IPC Inspection, Raw Material Reconciliation, Pallet & Shipper, Shift Handover).

## Repository structure

```
PPES/
├── PPES_JavaApp/               Desktop app (Java Swing, Maven)
│   ├── pom.xml
│   ├── src/main/java/com/ppes/
│   │   ├── ui/                 Swing screens (MainFrame, CrudPanel, 5 custom modules, dialogs)
│   │   ├── model/               TableDef / FieldDef / TableRegistry (schema metadata)
│   │   ├── db/                  DB connection + PasswordUtil
│   │   └── excel/               Apache POI export/import
│   ├── src/main/resources/images/   Banners + per-table form images
│   ├── *.sql                    Schema + sample data scripts
│   └── README.txt               Bangla build/run instructions
│
├── PPES_WebApp/                 Web app (PHP + MySQL, Bootstrap 5)
│   ├── config.php               DB connection settings
│   ├── includes/tables.php       Table metadata (drives the generic CRUD engine)
│   ├── includes/auth.php         Login/session guard, password hashing
│   ├── includes/header.php        Shared 3-row top navigation
│   ├── crud.php                    Generic Add/Edit/Delete/List engine for all 18 tables
│   ├── login.php / logout.php       Authentication
│   ├── help.php                      Bilingual help + SOP reference
│   ├── work_order_packaging.php, carton_tracking.php, shift_production.php,
│   │   lot_closing.php, batch_yield.php    The 5 custom modules
│   ├── images/                        Shared banner images
│   └── README.txt                     Bangla setup/run instructions
│
└── *.sql                        Root-level schema migration scripts
```

## Getting started

### Prerequisites

- MySQL 8.x (or MariaDB) with a database named `ppes`. Run the `.sql` files in this repo (start with the base schema, then the `*_schema.sql` migration files in the order they were added) to create the tables.
- For the desktop app: JDK 11+ and Maven.
- For the web app: PHP 8+ with the `pdo_mysql` extension (e.g. via XAMPP/MAMP) and a web server (Apache/Nginx).

### Desktop app (PPES_JavaApp)

```bash
cd PPES_JavaApp
mvn clean package
java -jar target/ppes-desktop.jar
```

Edit the DB connection details in the app's config before first run (see `PPES_JavaApp/README.txt` for full Bangla step-by-step instructions).

### Web app (PPES_WebApp)

```bash
# 1. Copy the PPES_WebApp folder into your web server's document root
# 2. Edit PPES_WebApp/config.php with your MySQL host/user/password
# 3. Start Apache/PHP, then open in a browser:
http://localhost/PPES_WebApp/
```

See `PPES_WebApp/README.txt` for full Bangla setup instructions, including running it over a local WiFi network so mobile devices can reach it.

### First login

If the `users` table is empty, use the **"Skip Login (Setup Mode)"** option on the login screen (both apps), then create your first user from Masters → Users. From then on the same username/password works in both the desktop and web apps.

## Tech stack

**Desktop:** Java 11, Swing, FlatLaf (dark theme), MySQL Connector/J, Apache POI, Maven Shade Plugin (single runnable jar).

**Web:** PHP 8 (PDO), MySQL, Bootstrap 5 (CDN), vanilla JS (no build step required).

## License

Add a license of your choice here (e.g. MIT) before making the repository public.
