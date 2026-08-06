PPES Desktop (Java Swing + MySQL + Excel) — সেটআপ নির্দেশনা
================================================================

এটি আগের PHP ওয়েব অ্যাপের হুবহু Java ভার্সন — একই "ppes" MySQL
ডাটাবেজ (১৬টি টেবিল) ব্যবহার করে, কিন্তু এবার একটি ডেস্কটপ Swing
অ্যাপ্লিকেশন হিসেবে, এবং প্রতিটি টেবিলে Excel Export/Import সুবিধাসহ।

সর্বশেষ আপডেটে যোগ হয়েছে: Login স্ক্রিন, Help ও Contact বাটন, নতুন
ডার্ক থিম (FlatLaf দিয়ে) + আলাদা নেভি/অ্যাম্বার কালার স্কিম, সাইডবারে
"Carton Tracking Log" মডিউল (ধাপ ৬), "Shift Production Log" মডিউল
(ধাপ ৭), "Work Order — Packaging Requirement" ক্যালকুলেটর মডিউল
(ধাপ ৮), নতুন "Lot Closing (Finish Production)" মডিউল (ধাপ ৯), সাইডবার
মেনু বাটনে মাউস hover করলে হালকা রঙের হাইলাইট, Dashboard-এর (মূল
মেনু) উপরে আপনার দেওয়া ফ্যাক্টরি ছবি দিয়ে একটি কভার ব্যানার, Help
বাটন এখন দ্বিভাষিক (বাংলা/English টগল বাটনসহ), সব ফর্ম মাল্টি-কলাম
লেআউটে (ধাপ ১১), এবং নতুন "Batch Yield Reconciliation Calculator"
মডিউল (ধাপ ১২, লাইভ cGMP PASS/FAIL ক্যালকুলেটর)।

প্রজেক্ট কাঠামো
----------------------------------------------------------------
PPES_JavaApp/
  pom.xml                          -> Maven বিল্ড ফাইল (dependencies + fat-jar প্লাগইন)
  src/main/resources/config.properties -> DB কানেকশন সেটিংস (এখানে এডিট করুন)
  src/main/java/com/ppes/
    Main.java                      -> এন্ট্রি পয়েন্ট
    db/Db.java                     -> JDBC কানেকশন
    db/FieldValueConverter.java    -> ফর্ম/Excel থেকে আসা টেক্সটকে সঠিক টাইপে রূপান্তর
    db/PasswordUtil.java           -> পাসওয়ার্ড হ্যাশিং (salted SHA-256)
    model/                         -> ১৬টি টেবিলের মেটাডাটা (কলাম, ফর্ম কন্ট্রোল, মেনু গ্রুপিং)
    ui/LoginDialog.java            -> লগইন স্ক্রিন (users টেবিল যাচাই + Setup Mode বাইপাস)
    ui/HelpDialog.java             -> Help বাটনের বাংলা ব্যবহার-নির্দেশিকা
    ui/ContactDialog.java          -> Contact বাটনের তথ্য (এডিট করুন আপনার নিজের তথ্য দিয়ে)
    ui/MainFrame.java              -> টপ বার + সাইডবার মেনু + মূল উইন্ডো
    ui/DashboardPanel.java         -> ড্যাশবোর্ড (স্ট্যাট কার্ড + সাম্প্রতিক অর্ডার/লট)
    ui/CrudPanel.java              -> সব টেবিলের জেনেরিক Add/List/Delete + Excel Export/Import
    ui/CartonTrackingPanel.java    -> Carton Tracking Log ফর্ম (লাইভ ফর্মুলা ক্যালকুলেশনসহ)
    ui/ShiftProductionPanel.java   -> Shift Production Log ফর্ম (প্রতি শিফটের এন্ট্রি + রানিং টোটাল)
    ui/WorkOrderPackagingPanel.java -> Work Order প্যাকেজিং রিকোয়ারমেন্ট ক্যালকুলেটর
    ui/LotClosingPanel.java        -> Lot Closing (Finish Production) ফর্ম
    ui/BatchYieldReconciliationPanel.java -> Batch Yield Reconciliation Calculator (লাইভ, ডাটাবেজ ছাড়া)
    ui/ReportsPanel.java           -> ৬টি রিপোর্ট + Excel Export
    excel/                         -> Apache POI ভিত্তিক Export/Import ইউটিলিটি
    resources/images/cover.jpg     -> Dashboard-এর কভার ব্যানারে ব্যবহৃত ছবি (বদলাতে চাইলে এই
                                       ফাইলটাই নিজের ছবি দিয়ে replace করুন, নাম একই রাখুন)
  sample_data/PPES_sample_data.xlsx -> ১৬টি টেবিলের প্রতিটিতে ১০টি করে নমুনা রেকর্ড, ১৬টি শিটে (users সহ)
  PPES_sample_data.sql              -> একই ডাটা SQL ফরম্যাটে (users-এর পাসওয়ার্ড আগে থেকেই হ্যাশ করা)
  PPES_carton_tracking_schema.sql   -> carton_tracking_header + carton_tracking_hourly টেবিল
                                        (DDL + নমুনা ডাটা আপনার batch_lot-এর প্রকৃত lot_id দিয়ে)
  PPES_shift_production_schema.sql  -> shift_production_log টেবিল (DDL + নমুনা ডাটা, একই
                                        carton_tracking_header টেবিল রি-ইউজ করে হেডার হিসেবে)
  PPES_work_order_packing_schema.sql -> work_order_packing_spec টেবিল (প্যাকিং রেশিও + অর্ডার Qty)
  PPES_lot_closing_schema.sql       -> lot_closing টেবিল + প্যাকেজিং আইটেম টাইপ (Empty Carton,
                                        Empty Shipper, Leaflet, Label) raw_material_master-এ সিড করে
  PPES_extended_fields_schema.sql   -> raw_material_master + pallet-এ নতুন কলাম, এবং নতুন
                                        in_process_waste + pallet_placard টেবিল
  PPES_extended_fields_2_schema.sql -> production_target, reconciliation, in_process_waste-এ
                                        আরও নতুন কলাম

ধাপ ১ — প্রয়োজনীয় সফটওয়্যার
----------------------------------------------------------------
১. JDK 11 বা তার বেশি ইনস্টল থাকতে হবে (java -version দিয়ে যাচাই করুন)।
২. Maven ইনস্টল থাকতে হবে (mvn -version দিয়ে যাচাই করুন)।
৩. আপনার MySQL সার্ভার আগে থেকেই "ppes" স্কিমা নিয়ে চলছে (আপনার
   MySQL Workbench স্ক্রিনশট অনুযায়ী)।

ধাপ ২ — config.properties এডিট করুন
----------------------------------------------------------------
src/main/resources/config.properties ফাইলে আপনার আসল তথ্য দিন:

  db.host=localhost
  db.port=3306
  db.name=ppes
  db.user=root
  db.password=

⚠️ গুরুত্বপূর্ণ: অ্যাপ চালানোর আগে MySQL Workbench-এ (এই ক্রমে) ছয়টা
স্ক্রিপ্ট রান করে নিন:
  ১. PPES_carton_tracking_schema.sql   (carton_tracking_header, carton_tracking_hourly তৈরি করে)
  ২. PPES_shift_production_schema.sql  (shift_production_log তৈরি করে — carton_tracking_header ব্যবহার করে, তাই ১ নম্বরটা আগে লাগবে)
  ৩. PPES_work_order_packing_schema.sql (work_order_packing_spec তৈরি করে)
  ৪. PPES_lot_closing_schema.sql        (lot_closing তৈরি করে + প্যাকেজিং মেটেরিয়াল সিড করে)
  ৫. PPES_extended_fields_schema.sql    (raw_material_master ও pallet-এ নতুন কলাম যোগ করে +
                                          in_process_waste ও pallet_placard টেবিল তৈরি করে)
  ৬. PPES_extended_fields_2_schema.sql  (production_target, reconciliation, in_process_waste-এ
                                          আরও নতুন কলাম যোগ করে)
এই ছয়টা ছাড়া সাইডবারের "Packaging Requirement", "Carton Tracking
Log", "Shift Production Log", "Lot Closing", "In-Process Waste", ও
"Pallet Placard" মেনু খুললে ডাটাবেজ এরর দেখাবে। এছাড়া Raw Material
Master, Pallet, Production Target, Reconciliation ফর্মেও নতুন
ফিল্ডগুলো দেখা যাবে না যতক্ষণ না ৫ ও ৬ নম্বর স্ক্রিপ্ট রান হয়।

⚠️ PPES_extended_fields_schema.sql ও PPES_extended_fields_2_schema.sql
শুধু একবার করে রান করুন (ADD COLUMN স্টেটমেন্ট, IF NOT EXISTS নেই —
দ্বিতীয়বার রান করলে "column already exists" এরর আসবে; দ্বিতীয়বার
লাগলে ইতিমধ্যে যোগ হওয়া কলামের লাইনগুলো ফাইল থেকে বাদ দিয়ে বাকিটা
রান করুন)।

ধাপ ৩ — বিল্ড করুন
----------------------------------------------------------------
টার্মিনালে PPES_JavaApp ফোল্ডারে গিয়ে চালান:

  mvn clean package

এতে সব dependency (MySQL Connector/J, Apache POI) স্বয়ংক্রিয়ভাবে
ডাউনলোড হয়ে একটি "fat jar" তৈরি হবে:
  target/ppes-desktop.jar

ধাপ ৪ — রান করুন
----------------------------------------------------------------
  java -jar target/ppes-desktop.jar

সবার আগে একটি Login স্ক্রিন আসবে। যেহেতু প্রথমবার ব্যবহারে আপনার
"users" টেবিল খালি থাকবে (আগের ধাপে বলা হয়েছে ওখানে কোনো নমুনা রেকর্ড
দেওয়া হয়নি), লগইন করার বদলে "Skip Login (Setup Mode)" বাটনে ক্লিক
করুন। এতে সরাসরি অ্যাপে ঢুকে যাবেন এবং Masters > Users থেকে আপনার
প্রথম রিয়েল ইউজার অ্যাকাউন্ট তৈরি করতে পারবেন — পরের বার থেকে সেই
ইউজারনেম/পাসওয়ার্ড দিয়ে লগইন করা যাবে।

লগইনের পর একটি ডার্ক-থিমের উইন্ডো খুলবে: উপরে একটি টপ বার (লগইন
তথ্য, Help, Contact, Logout বাটন), বাম পাশে নেভি+অ্যাম্বার রঙের
সাইডবার মেনু, ডানে Dashboard/ফর্ম/রিপোর্ট।

- Help বাটন: অ্যাপ কীভাবে ব্যবহার করবেন তার বাংলা নির্দেশিকা দেখাবে।
- Contact বাটন: সাপোর্ট ইমেইল/ফোন দেখাবে (এখন placeholder তথ্য আছে —
  ui/ContactDialog.java ফাইলে গিয়ে আপনার আসল তথ্য বসিয়ে দিন)।
- Logout বাটন: আবার Login স্ক্রিনে ফিরিয়ে নেবে।

ধাপ ৫ — নমুনা ডাটা লোড করুন (প্রতিটি টেবিলে ১০টি রেকর্ড — মডিউল টেস্ট করার জন্য যথেষ্ট)
----------------------------------------------------------------
দুইভাবে করতে পারেন:

(A) সরাসরি SQL দিয়ে (দ্রুততম):
    MySQL Workbench-এ PPES_sample_data.sql ফাইলটি রান করুন। ১৬টি
    টেবিলের প্রতিটিতে ১০টি করে রেকর্ড ঢুকবে, users টেবিলসহ (নিচের
    "টেস্ট লগইন ক্রেডেনশিয়াল" অংশ দেখুন)।

(B) অ্যাপের Import ফিচার দিয়ে (Excel দিয়ে টেস্ট করতে চাইলে):
    ১. অ্যাপে যেকোনো টেবিলের মেনুতে ক্লিক করুন (যেমন Masters > Product Master)।
    ২. "Import from Excel" বাটনে ক্লিক করুন।
    ৩. sample_data/PPES_sample_data.xlsx ফাইলটি সিলেক্ট করুন।
    ৪. অ্যাপ স্বয়ংক্রিয়ভাবে ঐ টেবিলের নামে থাকা শিট (যেমন "product_master")
       খুঁজে নিয়ে ইমপোর্ট করবে — প্রতিটি টেবিলের Import বাটনে একই
       PPES_sample_data.xlsx ফাইলটি বারবার ব্যবহার করতে পারবেন।

    গুরুত্বপূর্ণ: টেবিলগুলো একে অপরকে রেফারেন্স করে (Foreign Key), তাই
    এই ক্রমে ইমপোর্ট করুন —
    Product Master → Customer Order → Employee → Shift Master →
    Raw Material Master → Users → Batch/Lot → Raw Material Receiving →
    Startup Checklist → Production Target → Blister Production →
    IPC Inspection → Pallet → Shipper Label → Reconciliation →
    Shift Handover.

    Excel-এর users শিটে পাসওয়ার্ড সাধারণ প্লেইন টেক্সট আকারে আছে
    (যেমন admin123) — Import করার সময় অ্যাপ নিজে থেকেই সেটা হ্যাশ করে
    সংরক্ষণ করবে, তাই Excel দিয়ে ইমপোর্ট করলেও লগইন ঠিকভাবে কাজ করবে।

টেস্ট লগইন ক্রেডেনশিয়াল
----------------------------------------------------------------
নমুনা ডাটা লোড করার পর (SQL বা Excel, যেকোনো উপায়ে) নিচের যেকোনো
ইউজারনেম/পাসওয়ার্ড দিয়ে Login স্ক্রিনে ঢুকতে পারবেন:

  ইউজারনেম       পাসওয়ার্ড      রোল
  ------------   -----------   -----------
  admin          admin123      Admin
  rahim.op       rahim123      Production
  karim.sup      karim123      Production
  nasrin.qc      nasrin123     QC
  jasim.wh       jasim123      Warehouse
  farzana.qa     farzana123    QC
  salma.op       salma123      Production
  tanvir.mt      tanvir123     Maintenance
  shirin.mgr     shirin123     Admin
  delwar.op      delwar123     Production

(SQL স্ক্রিপ্টে এই পাসওয়ার্ডগুলো আগে থেকেই সঠিক ফরম্যাটে হ্যাশ করা
আছে যাতে অ্যাপের Login স্ক্রিন সরাসরি এগুলো মেলাতে পারে।)

ধাপ ৬ — Carton Tracking Log মডিউল (নতুন)
----------------------------------------------------------------
এটি আপনার আপলোড করা Order_Carton_Tracking_Sheet.xlsx-এর হুবহু Java
ভার্সন — সাইডবারে "CARTON TRACKING" সেকশনের নিচে "Carton Tracking Log"
এ ক্লিক করলে খুলবে। কোনো ডুপ্লিকেট ডাটা এন্ট্রি এড়াতে Customer,
Product, W.O. No, Lot No এখানে আলাদাভাবে রাখা হয়নি — এগুলো ইতিমধ্যে
Batch/Lot টেবিলে আছে, তাই শুধু উপরে একটি লট ড্রপডাউন থেকে সিলেক্ট
করলেই সেই তথ্য স্বয়ংক্রিয়ভাবে যুক্ত থাকবে।

ব্যবহারের ধাপ:
  ১. উপরে লট (Lot No) সিলেক্ট করুন।
  ২. Room No, Expiry Date, Order Qty, Today's Balance, Starting Label
     No, Ctns in Shipper, Shift Standard (৮ ঘণ্টার টার্গেট) ফিল্ডগুলো
     পূরণ করে "Save Header" চাপুন।
  ৩. নিচের টেবিলে প্রতিটি চেকপয়েন্ট আওয়ারে (Hour) Label No After এবং
     প্রয়োজনে Comment টাইপ করুন। বাকি কলামগুলো (Standard Ctns, Total
     Shippers, Actual Ctns, Percent) মূল এক্সেল শিটের ফর্মুলা অনুযায়ী
     স্বয়ংক্রিয়ভাবে হিসাব হয়ে যাবে, টাইপ করার সাথে সাথেই:
       - Standard Ctns   = Shift Standard × Hour ÷ 8
       - Total Shippers  = Label No After − Starting Label No
       - Actual Ctns     = Ctns in Shipper × Total Shippers
       - Percent         = Actual Ctns ÷ Standard Ctns × 100
  ৪. "Save Rows" চাপলে সব রো ডাটাবেজে সংরক্ষিত হবে (carton_tracking_hourly)।
  ৫. "Export to Excel" দিয়ে বর্তমান ডাটা .xlsx ফাইলে সেভ করা যাবে।

টেস্ট করার জন্য: PPES_carton_tracking_schema.sql আপনার batch_lot
টেবিলে যে lot_id সবচেয়ে ছোট (প্রথম লট) সেটার জন্য আপনার আসল শিটের
সংখ্যা দিয়েই ১টি হেডার ও ৪টি hourly রো আগে থেকেই ঢুকিয়ে দেয় (Hour
2/4/6/8) — লট ড্রপডাউনে সেই লট সিলেক্ট করলে দেখতে পাবেন আসল শিটের
সংখ্যাগুলোই (113.37%, 103.31%, 97.22%, 100.80%) সঠিকভাবে পুনরায় হিসাব
হচ্ছে। (আগে lot_id হার্ডকোড করা ছিল ১ দিয়ে, যেটা FK এরর দিচ্ছিল যদি
আপনার আসল batch_lot টেবিলে lot_id=1 না থাকত — এখন সেটা ঠিক করা আছে।)

ধাপ ৭ — Shift Production Log মডিউল (নতুন)
----------------------------------------------------------------
এটা আপনার চাওয়া নতুন শিট — প্রতিটি শিফটে কতগুলো শিপার ও ইউনিট
প্রোডাকশন হলো তার লগ। সাইডবারে "SHIFT PRODUCTION" সেকশনের নিচে
"Shift Production Log"-এ ক্লিক করলে খুলবে।

হেডার (Order Qty, Work Order No, Lot No, Item No, Product Name, Exp
Date, Ctns Qty in a Shipper) আলাদা কোনো নতুন টেবিল নয় — Work Order
No/Lot No/Item No/Product Name শুধু দেখানো হয় (read-only), সরাসরি
Batch/Lot ও Product Master থেকে টেনে; আর Order Qty/Exp Date/Ctns Qty
in a Shipper একই carton_tracking_header টেবিলে সংরক্ষিত হয় যেটা
Carton Tracking Log মডিউলও ব্যবহার করে — তাই একবার একটা লটের হেডার
পূরণ করলে দুটো স্ক্রিনেই সেটা দেখা যাবে, ডুপ্লিকেট এন্ট্রি লাগে না।

ব্যবহারের ধাপ:
  ১. উপরে লট সিলেক্ট করুন — Work Order No, Lot No, Item No, Product
     Name স্বয়ংক্রিয়ভাবে দেখাবে।
  ২. Order Qty, Exp Date, Ctns Qty in a Shipper পূরণ করে "Save Header"
     চাপুন (আগে থেকে ভরা থাকলে, দরকার না হলে বাদ দিতে পারেন)।
  ৩. নিচের টেবিলে ডিফল্টভাবে ১০টি খালি রো থাকে। প্রতিটি রোতে টাইপ
     করুন: Date (yyyy-MM-dd), Shift (ড্রপডাউন থেকে বাছুন, Shift
     Master টেবিলের তালিকা), Final Shipper, No of Shipper, No of Unit
     Produced, Recorded By (ড্রপডাউন থেকে বাছুন, Employee টেবিলের
     তালিকা)।
  ৪. "Total No of Unit Produced" কলামটা স্বয়ংক্রিয়ভাবে হিসাব হয় —
     উপরের রো থেকে নিচের রো পর্যন্ত "No of Unit Produced"-এর running
     (ক্রমসঞ্চিত) যোগফল। তাই রোগুলো তারিখের ক্রম অনুযায়ী উপর থেকে
     নিচে সাজিয়ে টাইপ করাই ভালো।
  ৫. "Add Row" দিয়ে আরও রো যোগ করা যাবে, "Delete Selected Row" দিয়ে
     বাদ দেওয়া যাবে।
  ৬. "Save Rows to Database" চাপলে সব রো shift_production_log
     টেবিলে সংরক্ষিত হবে। "Export to Excel" দিয়ে .xlsx-এ সেভ করা যাবে।

PPES_shift_production_schema.sql-এ (carton tracking-এর মতোই) আপনার
প্রথম লটের জন্য ৪টি নমুনা রো আগে থেকে ঢোকানো আছে, যাতে সাথে সাথে
টেস্ট করা যায়।

ধাপ ৮ — Work Order: Packaging Requirement ক্যালকুলেটর (নতুন)
----------------------------------------------------------------
এই মডিউলটা আপনার দেওয়া উদাহরণ থেকেই বানানো — ধরুন Sudafed-এর একটা
Work Order-এ Order Qty = ১,০০,০০০ Cartons, এবং প্যাকিং রেশিও:
  ১২ Tablets = ১ Blister,  ২ Blisters = ১ Carton,
  ৬ Cartons = ১ Bundle,   ১২ Bundles = ১ Shipper (= ৭২ Cartons),
  ১২ Shipper/Layer × ৭ Layers = ১ Pallet (= ৮৪ Shippers = ৬০৪৮ Cartons)

সাইডবারে "WORK ORDER" সেকশনের নিচে "Packaging Requirement"-এ ক্লিক
করলে খুলবে। এটা কোনো নতুন ডুপ্লিকেট ডাটা তৈরি করে না — শুধু আপনার
দেওয়া রেশিও (work_order_packing_spec টেবিলে সংরক্ষিত) আর Shift
Production Log-এ এন্ট্রি করা "No of Shipper"-এর যোগফল ব্যবহার করে
প্রতিবার তাজা হিসাব করে।

ব্যবহারের ধাপ:
  ১. লট সিলেক্ট করুন — Work Order No, Lot No, Product Name
     স্বয়ংক্রিয়ভাবে দেখাবে।
  ২. Order Qty (Cartons) ও ৬টি প্যাকিং রেশিও ফিল্ড পূরণ করুন (নতুন
     লটের জন্য ডিফল্ট মান আগে থেকেই ভরা থাকবে: 12/2/6/12/12/7— এগুলো
     বদলে নিজের প্রোডাক্টের রেশিও বসাতে পারেন)।
  ৩. "Save Packing Spec (Upsert)" চাপুন।
  ৪. "Shippers Produced So Far" ফিল্ডে "Pull from Shift Production
     Log" বাটনে ক্লিক করলে Shift Production Log মডিউলে ওই লটের জন্য
     এ পর্যন্ত যত শিপার প্রোডাকশন এন্ট্রি হয়েছে তার যোগফল
     স্বয়ংক্রিয়ভাবে বসে যাবে (চাইলে হাতেও বদলে দেখতে পারেন — "what
     if" হিসাবের জন্য)।
  ৫. নিচের টেবিলে সাথে সাথে দেখা যাবে প্রতিটি লেভেলের জন্য —
     Tablets, Blisters, Cartons, Bundles, Shippers, Pallets (Skids),
     Shipper Labels, Pallet Labels — এই তিনটি সংখ্যা:
       - Total Required (Full Order)  = সম্পূর্ণ অর্ডারের জন্য মোট কত লাগবে
       - Produced So Far              = এখন পর্যন্ত কত তৈরি হয়েছে
       - Remaining Needed             = অর্ডার শেষ করতে আর কত লাগবে
  ৬. "Export to Excel" দিয়ে পুরো টেবিলটা .xlsx ফাইলে সেভ করা যাবে।

উদাহরণ (আপনার দেওয়া সংখ্যা দিয়ে যাচাই করা): Order Qty = ১,০০,০০০
Cartons হলে মোট লাগবে ২,০০,০০০ Blisters, ২৪,০০,০০০ Tablets, ১৬,৬৬৭
Bundles, ১,৩৮৯ Shippers, ১৭ Pallets। ধরুন এখন পর্যন্ত ৫০০টা Shipper
প্রোডাকশন হয়েছে (= ৩৬,০০০ Cartons) — তাহলে বাকি থাকবে ৬৪,০০০ Cartons,
১০,৬৬৭ Bundles, ৮৮৯ Shippers, ১১ Pallets।

ধাপ ৯ — Lot Closing (Finish Production) মডিউল (নতুন)
----------------------------------------------------------------
বাস্তবে ব্লিস্টার প্রোডাকশন শেষ হয় Order Qty অনুযায়ী নয় — Reject-এর
কারণে Good + Reject Blister-এর মোট সংখ্যা প্রয়োজনীয় Blister সংখ্যায়
পৌঁছালে তখনই লট শেষ ধরা হয়। এই মডিউল ঠিক সেটাই ট্র্যাক করে, এবং
প্যাকেজিং মেটেরিয়াল (খালি কার্টন, খালি শিপার, বিভিন্ন টাইপের লিফলেট,
লেবেল) MBR/MPR অনুযায়ী স্টোরে ফেরত দেওয়ার হিসাবও রাখে।

সাইডবারে "LOT CLOSING" সেকশনে "Lot Closing (Finish Production)"-এ
ক্লিক করলে খুলবে। কোনো নতুন ডুপ্লিকেট ডাটা টেবিল বানানো হয়নি —
Good/Reject Blister সংখ্যা সরাসরি Blister Production থেকে, প্রয়োজনীয়
সংখ্যা Work Order Packaging Requirement থেকে, আর প্যাকেজিং মেটেরিয়াল
রিটার্ন আপনার আগে থেকে থাকা Reconciliation টেবিলেই সংরক্ষিত হয়।

ব্যবহারের ধাপ:
  ১. লট সিলেক্ট করুন — Work Order No, Lot No, Product Name, Expiry
     Date (Carton Tracking Log থেকে) দেখাবে।
  ২. "প্রোডাকশন সামারি" অংশে দেখবেন Required Blisters (Work Order
     Packaging স্পেক থেকে), Good Blisters, Reject Blisters, এবং
     Production Status — "সম্পন্ন (Complete)" অথবা "চলমান — বাকি X
     ব্লিস্টার"।
  ৩. Closing Date, MBR No, MPR No, Loose Product Qty (লুজ/আনপ্যাকড
     প্রোডাক্ট গোনা সংখ্যা), Remarks, Closed By পূরণ করে "Save
     Closing Details" চাপুন।
  ৪. নিচের গ্রিডে প্রতিটি প্যাকেজিং মেটেরিয়ালের জন্য Issued Qty, Used
     Qty, Waste/Reject Qty দিন — Returned to Store কলামটা
     স্বয়ংক্রিয়ভাবে হিসাব হবে (= Issued − Used − Waste)। ডিফল্টভাবে
     Empty Carton, Empty Shipper, Leaflet - Type A/B, Shipper Label,
     Carton Label রো হিসেবে থাকবে (এবং এই লটে যা কিছু ইতিমধ্যে
     Raw Material Receiving-এ ইস্যু করা হয়েছে সেগুলোও)। "Add
     Material Row" দিয়ে আরও মেটেরিয়াল যোগ করা যাবে।
  ৫. "Save Material Returns" চাপলে এই ডাটা Reconciliation টেবিলে
     সংরক্ষিত হবে।
  ৬. সব ঠিকঠাক থাকলে "Mark Lot as Closed" চাপুন — এতে একটা কনফার্মেশন
     চাইবে, তারপর লটের Batch/Lot স্ট্যাটাস 'Closed' হয়ে যাবে।
  ৭. "Export to Excel" দিয়ে মেটেরিয়াল রিটার্নের তালিকা .xlsx ফাইলে
     সেভ করা যাবে।

PPES_lot_closing_schema.sql-এ আপনার প্রথম লটের জন্য একটা ডেমো Draft
ক্লোজিং রেকর্ড আগে থেকে ঢোকানো আছে (Good=196000, Reject=4000,
Loose=120), যাতে সাথে সাথে টেস্ট করা যায়।

ধাপ ১০ — Raw Material Master, Pallet সম্প্রসারণ + In-Process Waste ও
Pallet Placard (নতুন)
----------------------------------------------------------------
আপনার আপলোড করা আসল লেবেল/ট্যাগের ছবি অনুযায়ী নিচের পরিবর্তনগুলো
করা হয়েছে:

Raw Material Master ফর্মে নতুন ফিল্ড যোগ হয়েছে:
  Product No (Item#), Lot#, QTY, Gross Weight, Net Weight, Tare,
  Container #, Container# of Total in Pallet, Batch#, Process Order,
  Date (Label), MFG Date, Bulk EXP Date, Manufactured By, Pallet ID,
  Barcode (আবশ্যক)।
  দ্রষ্টব্য: এই ফিল্ডগুলোর বেশিরভাগ (Lot#, Container#, Weight, Date)
  আসলে একটা নির্দিষ্ট কন্টেইনার/লট-এর তথ্য, পুরো মেটেরিয়াল টাইপের নয় —
  তাই এখন থেকে একই মেটেরিয়ালের (যেমন "Sudafed PE API") প্রতিটা
  কন্টেইনারের জন্য আলাদা রো তৈরি হবে raw_material_master-এ। এটাই
  আপনি চেয়েছেন বলে এভাবেই করা হয়েছে; ভবিষ্যতে চাইলে raw_material_master
  কে বিশুদ্ধ ক্যাটালগ রেখে এই কন্টেইনার-লেভেল তথ্য raw_material_receiving
  টেবিলে সরিয়ে নেওয়া যায় — বললে সেটাও করে দেব।

In-Process Waste (নতুন মডিউল, লাল "IN-PROCESS WASTE" ট্যাগ অনুযায়ী):
  Customer, Product, W.O. No, Lot No, Exp. Date, Reject Qty, Reject
  Remarks — সাইডবারে "WASTE & PLACARDS" সেকশনে পাবেন।

Pallet ফর্মে নতুন ফিল্ড যোগ হয়েছে (পুরনো Pallet Number/Carton Qty/
Status-এর পাশাপাশি): Exp Date, Customer, Product Name, Item #,
Cartons Qty in Shipper/Cases, Work Order No, Moved By QA, Date
(Moved), Cases per Layer, Total Layers।

Pallet Placard (নতুন মডিউল, সাপ্লায়ারের "PALLET PLACARD" ট্যাগ
অনুযায়ী, Packing & Dispatch সেকশনে "Pallet"-এর পরেই পাবেন):
  Pallet (ঐচ্ছিক, আপনার Pallet টেবিলের সাথে লিংক করতে চাইলে), Part No,
  Description, Purchase Order No, Supplier Lot No, Cases per Layer,
  Layers per Pallet, Total Pieces, এবং Case Qty on Pallet — এখানে
  লেবেলে যেমন লেখা থাকে ঠিক সেভাবেই টাইপ করতে পারেন (যেমন
  "12CS/LYR, 4LYRS - 38,400 PCS")।

ধাপ ১১ — মাল্টি-কলাম ফর্ম লেআউট + আরও ফিল্ড (নতুন)
----------------------------------------------------------------
ফর্ম লেআউট: আগে প্রতিটা টেবিলের Add Entry ফর্মে ফিল্ডগুলো একটার নিচে
একটা (single column) সাজানো থাকত, যেটা Raw Material Master, Batch/Lot,
Pallet, Pallet Placard-এর মতো অনেক ফিল্ডের ফর্মে অনেক স্ক্রল করতে
হতো। এখন থেকে সব টেবিলের ফর্মেই ফিল্ডগুলো ৩টা কলামে সাজানো থাকবে
(একেক রোতে ৩টা ফিল্ড পাশাপাশি), তাই পুরো ফর্মটা একনজরে অনেক কম
স্ক্রল করে দেখা যাবে। শুধু বড় টেক্সট (Remarks-জাতীয় TEXTAREA) ফিল্ড
নিজের পুরো-প্রস্থের আলাদা রোতে থাকবে, যাতে সেগুলো বেশি জায়গা পায়।

Production Target ফর্ম পুনর্বিন্যাস + নতুন ফিল্ড:
  এখন ক্রম অনুযায়ী: Lot, Order Qty, Today's Balance, Product to be
  Produced, Start Label, Bundles per Shipper, Cartons per Bundle,
  Pallet Qty (Ctns), তারপর আগের Hour Mark, Target Cartons, Actual
  Cartons, Variance।

Reconciliation ফর্মে নতুন ফিল্ড:
  Incoming Sample Qty, Loose Products (Pieces), Return Labels Qty —
  আগের Received/Used/Returned/Waste Qty-এর পাশাপাশি।

In-Process Waste ফর্মে নতুন ফিল্ড:
  Reject Loose Qty, এবং Total Incoming (যেমন "5 Bags @ 100") — আগের
  Reject Qty/Reject Remarks-এর পাশাপাশি।

UI উন্নতি (নতুন) — Hover ও কভার ব্যানার
----------------------------------------------------------------
- সাইডবারের প্রতিটি মেনু বাটনের ওপর মাউস নিয়ে গেলে এখন হালকা রঙের
  হাইলাইট (hover effect) দেখাবে, যেটা আগে ছিল না (কাস্টম ডার্ক কালার
  সেট করার কারণে ডিফল্ট hover চাপা পড়ে গিয়েছিল)।
- Dashboard (মূল মেনু, লগইনের পরে সবার আগে যেটা দেখা যায়) স্ক্রিনের
  একদম উপরে এখন আপনার দেওয়া ফ্যাক্টরি ছবি দিয়ে একটা কভার ব্যানার
  আছে, উপরে টাইটেল লেখাসহ। ছবিটা বদলাতে চাইলে
  src/main/resources/images/cover.jpg ফাইলটা নিজের ছবি দিয়ে replace
  করে (নাম ঠিক রেখে) আবার mvn clean package চালান।
- আপনার আপলোড করা ফ্যাক্টরি ছবিটা এখন প্রতিটি নতুন মডিউলের উপরেও
  আলাদা আলাদা রঙে (crop + duotone) ব্যানার হিসেবে বসানো হয়েছে —
  Carton Tracking Log (অ্যাম্বার), Shift Production Log (সায়ান/নীল),
  Work Order Packaging Requirement (সবুজ), Lot Closing (কোরাল/লাল) —
  একই আসল ছবি থেকে বানানো, যাতে প্রতিটা স্ক্রিন দেখতে আলাদা লাগে কিন্তু
  সবগুলোই আপনার কারখানার আসল ছবির অংশ। ছবিগুলো
  src/main/resources/images/banner_*.jpg নামে আছে, বদলাতে চাইলে
  একই নামে নিজের ছবি দিয়ে replace করে আবার mvn clean package চালান।
- এখন Masters, Order Intake, Production, Packing & Dispatch, Shift &
  Reconciliation-এর ১৬টি সাধারণ টেবিল ফর্মেরও (Product Master, Customer
  Order, Employee ... Shift Handover পর্যন্ত) প্রতিটির উপরে নিজস্ব রঙে
  একটা ব্যানার আছে — একই ছবি থেকে ১৬টা ভিন্ন ক্রপ+রঙ দিয়ে বানানো
  (src/main/resources/images/forms/<table_key>.jpg, ম্যাপিং আছে নতুন
  ui/FormBanners.java ফাইলে)। কোনো টেবিলের ছবি বদলাতে চাইলে সেই টেবিলের
  নামের .jpg ফাইলটা নিজের ছবি দিয়ে replace করুন।
- Help বাটন এখন দ্বিভাষিক ও দুটো ট্যাবে ভাগ করা:
    ট্যাব ১ "App Usage" — সব ১০টা মডিউলের ব্যবহার-নির্দেশিকা (Carton
      Tracking, Shift Production, Work Order Packaging, Lot Closing সহ)।
    ট্যাব ২ "Lot Starting SOP" — লট শুরুর প্রসেসে Inventory, QC,
      Validation, EHS, Production বিভাগের কাজ; OEB1–OEB5 ক্লাসিফিকেশন;
      GMP/cGMP/SOP/FDA সংক্ষিপ্ত পরিচিতি; Line Leader বনাম Packer-এর
      দায়িত্ব; এবং Audit ও Mechanics/Maintenance বিভাগের সাথে
      যোগাযোগে ব্যবহৃত শব্দকোষ।
  উপরে "বাংলা"/"English" বাটনে ক্লিক করলে দুটো ট্যাবের কনটেন্টই ভাষা
  বদলে যাবে। ⚠️ "Lot Starting SOP" ট্যাবটি একটি সাধারণ রেফারেন্স গাইড
  (ফার্মা প্যাকেজিং শিল্পের প্রচলিত অনুশীলনের ওপর ভিত্তি করে) — এটি
  আপনার প্রতিষ্ঠানের নিজস্ব অনুমোদিত SOP, GMP/cGMP ম্যানুয়াল, বা
  নিয়ন্ত্রক (FDA/DGDA/EMA) নির্দেশনার বিকল্প নয়; কমপ্লায়েন্স-সংক্রান্ত
  সিদ্ধান্তে সবসময় QA/Regulatory Affairs বিভাগের সাথে পরামর্শ করুন।

ধাপ ১২ — Batch Yield Reconciliation Calculator (নতুন মডিউল)
----------------------------------------------------------------
আপনি একটা রেডিমেড HTML/Tailwind/JS ক্যালকুলেটর দিয়েছিলেন যেটা Total
Bulk Input (I), Packed Good Yield (P), Sampling Qty (S), Rejected
Waste (R), Unused Good Returned (U) — এই ৫টা সংখ্যা থেকে লাইভ Actual
Yield ও Overall Reconciliation Yield বের করে, এবং cGMP সীমা (99.0% -
101.0%) অনুযায়ী PASS/FAIL দেখায়। এটা এখন দুইভাবে পাওয়া যাচ্ছে:

(ক) স্বতন্ত্র HTML ফাইল হিসেবে — চালানোর ধাপ (কোনো ইনস্টল লাগবে না):
  ১. আপনি যেটা পাঠিয়েছিলেন সেটাই "batch_yield_reconciliation_calculator.html"
     নামে সেভ করে দেওয়া হয়েছে — এই ফাইলটা ডাউনলোড করুন।
  ২. ফাইলটার ওপর ডাবল-ক্লিক করুন (অথবা রাইট-ক্লিক করে "Open With" >
     আপনার ব্রাউজার যেমন Chrome/Safari বেছে নিন) — এটা সরাসরি ব্রাউজারে
     খুলে যাবে, কোনো সার্ভার/ইনস্টলেশন লাগে না, ইন্টারনেট শুধু Tailwind
     CDN লোড করতে লাগবে।
  ৩. খোলার সাথে সাথে ডেমো সংখ্যা দিয়ে ক্যালকুলেশন দেখাবে; বাম পাশের
     ৫টা ইনপুট বক্সে নিজের ব্যাচের আসল সংখ্যা টাইপ করলেই ডান পাশে
     Actual Yield, Overall Reconciliation, ও PASS/FAIL ব্যানার সাথে
     সাথে (টাইপ করার সময়ই) আপডেট হয়ে যাবে।
  ৪. যেকোনো কম্পিউটার/ল্যাপটপে (এমনকি প্যাকেজিং লাইনের ফ্লোরে থাকা
     মোবাইল/ট্যাবলেটেও) কপি করে চালানো যাবে — এটা সম্পূর্ণ standalone,
     PPES ডাটাবেজের সাথে কোনো সংযোগ নেই।

(খ) PPES ডেস্কটপ অ্যাপের ভেতরে Java মডিউল হিসেবে (নতুন যোগ হয়েছে):
  সাইডবারে "YIELD CALCULATOR" সেকশনের নিচে "Batch Yield Reconciliation"
  বাটনে ক্লিক করলেই এই স্ক্রিন খুলবে (ui/BatchYieldReconciliationPanel.java)।
  এতে HTML ভার্সনের সবগুলো ফিচার হুবহু আছে:
    - ৫টা ইনপুট ফিল্ড (I, P, S, R, U) — টাইপ করার সাথে সাথেই (কোনো
      বাটন চাপা লাগবে না) Actual Yield ও Overall Reconciliation Yield
      রিক্যালকুলেট হয়ে যায় (Java-তে DocumentListener দিয়ে করা, HTML-এর
      oninput ইভেন্টের সমতুল্য)।
    - একই cGMP লজিক: 99.0%–101.0% এর মধ্যে থাকলে সবুজ "PASS", কম হলে
      লাল "FAIL (UNACCOUNTED LOSS)" (ঠিক কত % ঘাটতি সেটাও দেখাবে), বেশি
      হলে লাল "FAIL (SURPLUS DISCREPANCY)"।
    - এই মডিউলটা এখনো ডাটাবেজে কিছু সেভ করে না (ঠিক HTML ভার্সনের মতোই
      — এটা একটা পকেট ক্যালকুলেটর)। ভবিষ্যতে চাইলে এটাকে
      work_order_packing_spec ও shift_production_log টেবিলের সাথে যুক্ত
      করে I/P/R সংখ্যাগুলো সিলেক্ট করা লট থেকে অটো-লোড করানো যাবে —
      বললে সেটাও যোগ করে দেব।
  এই স্ক্রিনেও আপনার আপলোড করা ফ্যাক্টরি ছবির একটা নতুন ইন্ডিগো/ভায়োলেট
  duotone ব্যানার আছে (banner_batch_yield.jpg)।

ধাপ ১৩ — Dashboard-এ "video-like" মোশন ব্যানার (নতুন)
----------------------------------------------------------------
আপনি একটা আসল ভিডিও চেয়েছিলেন, কিন্তু ভিডিও ফাইল সত্যিকারের প্লেব্যাক
(play/pause সহ) করাতে VLC media player ইনস্টল + vlcj লাইব্রেরি — একটা
নতুন বাইরের ডিপেন্ডেন্সি — লাগত। আপনি সেটার বদলে আগে দেওয়া ফ্যাক্টরি
ছবিটা থেকেই একটা "video-like" motion effect চেয়েছেন, যেটা এখন করা
হয়েছে — কোনো নতুন সফটওয়্যার/লাইব্রেরি ছাড়াই।

কীভাবে করা হয়েছে: আপনার আপলোড করা ছবিটা থেকে ধীর zoom + pan
("Ken Burns effect", ডকুমেন্টারি/স্লাইডশোতে যেভাবে স্থির ছবিকে
জীবন্ত দেখানো হয়) দিয়ে ৪০ ফ্রেমের একটা animated GIF
(src/main/resources/images/cover.gif, ~3 MB) বানানো হয়েছে, যেটা
Dashboard-এর উপরের ব্যানারে লুপে (বারবার) চলতে থাকবে। Java-এর Swing
নিজেই animated GIF অটো-প্লে করতে পারে (আলাদা কোনো ভিডিও লাইব্রেরি
লাগে না) — তাই ui/DashboardPanel.java-তে শুধু ব্যানারের ইমেজ path
"cover.jpg" থেকে "cover.gif"-এ বদলানো হয়েছে; ui/BannerPanel.java-এর
কোনো কোড বদলাতে হয়নি।

- অ্যাপ চালু করলে Dashboard-এর উপরের ব্যানারটা এখন ধীরে ধীরে
  zoom in/out ও pan হতে থাকবে, লুপে বারবার — সত্যিকারের ভিডিওর মতো
  অনুভূতি দেবে যদিও এটা প্রযুক্তিগতভাবে একটা animated ছবি।
- ভবিষ্যতে যদি সত্যিকারের ভিডিও ফাইল (mp4/mov) চালাতে চান
  (play/pause বাটনসহ), সেটার জন্য আলাদাভাবে vlcj লাইব্রেরি +
  আপনার Mac-এ VLC media player ইনস্টল লাগবে — ভিডিও ফাইলটা দিলে
  সেই মডিউলও বানিয়ে দেব।
- GIF ফাইলটা বদলাতে চাইলে src/main/resources/images/cover.gif
  নিজের অ্যানিমেটেড GIF দিয়ে replace করে (নাম ঠিক রেখে) আবার
  mvn clean package চালান।

ধাপ ১৪/১৫ — সাইডবার সম্পূর্ণ সরিয়ে ৩-সারির টপ নেভিগেশন বার (নতুন, চূড়ান্ত)
----------------------------------------------------------------
আগে বাম পাশে একটা সাইডবার ছিল (স্ক্রল করে খুঁজতে হতো) এবং টপ বারে
আলাদাভাবে ৭টা প্রায়োরিটি বাটন ছিল। এখন সাইডবারটা সম্পূর্ণ সরিয়ে
পুরো নেভিগেশন টপ বারে ৩টা সারিতে নিয়ে আসা হয়েছে — এতে কনটেন্ট এরিয়া
পুরো উইন্ডো-প্রস্থ জুড়ে জায়গা পায়, এবং মেনু খুঁজতে স্ক্রল করা লাগে না।

সারি ১ (উপরে) — PPES ব্র্যান্ড নাম + লগইন করা ইউজারের নাম + Help/
  Contact/Logout বাটন (আগের মতোই)।

সারি ২ "QUICK ACCESS" — দৈনন্দিন/ঘন ঘন ব্যবহৃত ৭টা অপারেশনাল স্ক্রিন,
  সরাসরি বাটন হিসেবে: Dashboard, Work Order, Carton Tracking,
  Shift Production, Lot Closing, Yield Calculator, Reports।

সারি ৩ "DATA TABLES" — মাস্টার/রেফারেন্স ডাটা-টেবিলগুলোর নেভিগেশন,
  TableRegistry.MENU থেকে স্বয়ংক্রিয়ভাবে তৈরি:
    - একাধিক টেবিল থাকা সেকশনগুলো এখন ড্রপডাউন বাটন (▾ চিহ্নসহ) —
      ক্লিক করলে নিচে একটা মেনু খুলবে যেখানে সেই সেকশনের প্রতিটা
      টেবিল থাকবে: "Masters ▾", "Production (Lot Management) ▾",
      "Packing & Dispatch ▾", এবং বিশেষভাবে আপনার চাওয়া
      "Shift & Reconciliation ▾" (Reconciliation ও Shift Handover
      টেবিল দুটো এখন এখানে, সাইডবারের বদলে টপ বারে, অনেক বেশি
      দৃশ্যমান)।
    - মাত্র ১টা টেবিল থাকা সেকশনগুলো (Order Intake, Waste & Placards)
      সরাসরি বাটন — কোনো ড্রপডাউন ছাড়াই একক্লিকে খুলবে।

হোভার ও অ্যাক্টিভ স্টেট: যেকোনো বাটন বা ড্রপডাউন-মেনু আইটেমের ওপর
মাউস নিলে হালকা রঙের হাইলাইট (hover) দেখাবে; বর্তমানে খোলা স্ক্রিনের
বাটন (এবং সেটা যে সেকশনের ভেতরে আছে, সেই ড্রপডাউন বাটনও) টিল রঙে
হাইলাইট হয়ে থাকবে, যাতে বোঝা যায় আপনি এখন কোথায় আছেন।

এই ডিজাইনটা "more attractive and user-friendly" চাওয়ার ভিত্তিতে
আমার নিজের বিচারে করা হয়েছে। অন্য কোনোভাবে সাজাতে চাইলে (যেমন
নির্দিষ্ট কোনো সেকশন আলাদাভাবে বসাতে চান, বা সাইডবার আবার ফিরিয়ে
আনতে চান), বললেই বদলে দেব।

Excel ফিচার সম্পর্কে
----------------------------------------------------------------
- প্রতিটি টেবিলের "Export to Excel" বাটনে ক্লিক করলে বর্তমানে তালিকায়
  থাকা সব রেকর্ড একটি .xlsx ফাইলে সেভ হবে (হেডার বোল্ড করা)।
- "Import from Excel" বাটনে ক্লিক করলে যেকোনো .xlsx ফাইল থেকে ডাটা
  লোড করা যাবে — কলাম হেডার অবশ্যই টেবিলের ফিল্ড নামের সাথে মিলতে হবে
  (বড়/ছোট হাতের অক্ষরে সমস্যা নেই)। যেসব ফিল্ড অন্য টেবিল রেফারেন্স করে
  (Product, Lot, Employee ইত্যাদি), সেখানে হয় সরাসরি সংখ্যাসূচক ID
  দিতে পারেন, অথবা সেই টেবিলের নাম/লেবেল টেক্সট দিলেও অ্যাপ নিজে থেকে
  সঠিক ID খুঁজে বের করার চেষ্টা করবে।
- Reports স্ক্রিনেও প্রতিটি রিপোর্টের পাশে "Export to Excel" বাটন আছে।

গুরুত্বপূর্ণ নোট
----------------------------------------------------------------
- Login স্ক্রিন আছে, কিন্তু কোনো রোল-ভিত্তিক এক্সেস কন্ট্রোল নেই (যেমন
  QC রোলের ইউজার এখনো সব মেনু দেখতে ও এডিট করতে পারবে)। পাসওয়ার্ড
  salted SHA-256 দিয়ে হ্যাশ করা হয় (bcrypt নয়) — এটি প্রোডাকশন-গ্রেড
  নিরাপত্তার জন্য যথেষ্ট নাও হতে পারে; বাইরের নেটওয়ার্কে এক্সপোজ করার
  আগে jBCrypt-এর মতো লাইব্রেরি ও রোল-ভিত্তিক এক্সেস কন্ট্রোল যোগ করার
  পরামর্শ দেওয়া হলো।
- date/time ফিল্ডে টেক্সট ফরম্যাট মেনে টাইপ করতে হবে (টুলটিপে ফরম্যাট
  দেখানো আছে): Date = yyyy-MM-dd, Time = HH:mm বা HH:mm:ss,
  DateTime = yyyy-MM-dd HH:mm।
- আমার এনভায়রনমেন্টে কোনো JDK compiler (javac), Maven, বা Maven
  Central-এ নেটওয়ার্ক অ্যাক্সেস ছিল না — তাই আমি এই কোড সরাসরি
  compile/run করে টেস্ট করতে পারিনি। যাচাইয়ের জন্য যা করা হয়েছে:
  (১) javalang দিয়ে প্রতিটি .java ফাইলের প্রকৃত Java সিনট্যাক্স পার্স
  করে নিশ্চিত করা হয়েছে (২৫/২৫ ফাইল সফলভাবে পার্স হয়েছে, Login/Help/
  Contact/CartonTrackingPanel/ShiftProductionPanel/WorkOrderPackagingPanel/
  LotClosingPanel/BannerPanel/FormBanners/BatchYieldReconciliationPanel
  ফাইলসহ),
  (২) pom.xml-এর XML ভ্যালিডিটি যাচাই করা হয়েছে,
  (৩) প্রতিটি SQL কোয়েরি ও ফিল্ড ম্যাপিং আপনার আসল স্কিমার বিপরীতে
  ম্যানুয়ালি রিভিউ করা হয়েছে,
  (৪) নমুনা ডাটা (Excel + SQL উভয়) একই Python স্ক্রিপ্ট থেকে জেনারেট
  ও crosschecked করা হয়েছে যেন প্রতিটি টেবিলে ঠিক ১০টি করে রো থাকে এবং
  দুই ফাইলের ডাটা হুবহু মেলে, এবং users-এর পাসওয়ার্ড হ্যাশ Java-এর
  PasswordUtil অ্যালগরিদম হুবহু নকল করে গণনা করে সত্যিই ভেরিফাই করা
  হয়েছে (সঠিক পাসওয়ার্ডে true, ভুল পাসওয়ার্ডে false — উভয়ই টেস্ট
  করা হয়েছে)।
  তারপরও `mvn clean package` চালানোর সময় প্রথমবার কোনো কম্পাইল এরর
  এলে (বিশেষ করে dependency ভার্সন সংক্রান্ত), এরর মেসেজটি আমাকে
  জানালে সাথে সাথে ঠিক করে দেব।
