package com.ppes.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Bilingual (বাংলা / English) help module, shown from the top bar's Help
 * button. Two tabs:
 *   1. App Usage       — how to use each screen/module in this software.
 *   2. Lot Starting SOP — a cross-department reference for how a lot gets
 *      started and run on the floor (Inventory, QC, Validation, EHS,
 *      Production), OEB1-OEB5 handling, GMP/cGMP/SOP/FDA touchpoints, the
 *      Line Leader vs Packer roles, and the vocabulary used when talking
 *      to Audit and Mechanics/Maintenance.
 *
 * The Lot Starting SOP tab is general reference material based on common
 * pharmaceutical packaging industry practice — it does NOT replace your
 * site's own approved SOPs, GMP/cGMP manual, or applicable regulator
 * (FDA/DGDA/EMA, etc.) requirements. Always follow your site's current
 * approved SOP and consult QA/Regulatory Affairs for compliance decisions.
 */
public final class HelpDialog {

    private static final String BN_APP_HELP =
            "PPES অ্যাপ কীভাবে ব্যবহার করবেন\n"
                    + "================================\n\n"
                    + "১. মেনু\n"
                    + "বাম পাশের সাইডবারে Masters, Order Intake, Production (Lot Management), "
                    + "Work Order, Carton Tracking, Shift Production, Lot Closing, Packing & Dispatch, "
                    + "Shift & Reconciliation এবং Reports — এই ভাগগুলোতে সব টেবিল ও মডিউল সাজানো আছে।\n\n"
                    + "২. এন্ট্রি যোগ করা (সাধারণ টেবিল ফর্ম)\n"
                    + "যেকোনো মেনু আইটেমে ক্লিক করলে উপরে একটি ফর্ম আসবে। যেসব ফিল্ড অন্য টেবিলকে রেফারেন্স করে "
                    + "(যেমন Product, Lot, Employee) সেগুলো ড্রপডাউন থেকে বেছে নিন। Save বাটনে ক্লিক করলে রেকর্ড যোগ হবে।\n\n"
                    + "৩. রেকর্ড তালিকা ও ডিলিট\n"
                    + "ফর্মের নিচে সাম্প্রতিক ২০০টি রেকর্ড দেখা যাবে। একটি রো সিলেক্ট করে Delete Selected বাটনে ক্লিক করলে "
                    + "সেটি মুছে যাবে (কনফার্মেশন চাইবে)।\n\n"
                    + "৪. Excel Export / Import\n"
                    + "Export to Excel বাটনে ক্লিক করলে বর্তমান তালিকা .xlsx ফাইলে সেভ হবে। Import from Excel দিয়ে "
                    + "কোনো .xlsx ফাইল থেকে একসাথে অনেক রেকর্ড যোগ করা যাবে (কলাম হেডার অবশ্যই ফিল্ডের নামের সাথে মিলতে হবে)।\n\n"
                    + "৫. Reports\n"
                    + "Reports মেনুতে ৬টি প্রি-বিল্ট রিপোর্ট আছে (Lot Summary, Production vs Target, IPC Inspection, "
                    + "Raw Material Reconciliation, Pallet & Shipper, Shift Handover) — প্রতিটি Excel এ এক্সপোর্ট করা যায়।\n\n"
                    + "৬. লগইন\n"
                    + "প্রথমবার ব্যবহার করলে বা কোনো ইউজার তৈরি না থাকলে লগইন স্ক্রিনে \"Skip Login (Setup Mode)\" বাটন "
                    + "ব্যবহার করুন, তারপর Masters > Users থেকে ইউজার তৈরি করুন।\n\n"
                    + "৭. Carton Tracking Log\n"
                    + "লট সিলেক্ট করে হেডার (Room No, Expiry Date, Order Qty, Starting Label No, Ctns in Shipper, "
                    + "Shift Standard) পূরণ করুন। নিচের গ্রিডে Hour ও Label No After টাইপ করলেই Standard Ctns, "
                    + "Total Shippers, Actual Ctns, Percent স্বয়ংক্রিয়ভাবে হিসাব হয়ে যাবে।\n\n"
                    + "৮. Shift Production Log\n"
                    + "প্রতিটি শিফটে Date, Shift, Final Shipper, No of Shipper, No of Unit Produced, Recorded By এন্ট্রি "
                    + "করুন — Total No of Unit Produced কলামটা রানিং টোটাল হিসেবে স্বয়ংক্রিয়ভাবে হিসাব হবে।\n\n"
                    + "৯. Work Order — Packaging Requirement\n"
                    + "লট সিলেক্ট করে Order Qty (Cartons) ও প্যাকিং রেশিও (Tablets/Blister, Blisters/Carton, "
                    + "Cartons/Bundle, Bundles/Shipper, Shippers/Layer, Layers/Pallet) দিন। স্ক্রিন সাথে সাথে দেখাবে "
                    + "সম্পূর্ণ অর্ডারের জন্য মোট কত Blisters/Cartons/Bundles/Shippers/Pallets/Labels লাগবে, এখন "
                    + "পর্যন্ত কত হয়েছে (Shift Production Log থেকে Shippers টেনে), এবং বাকি কত লাগবে।\n\n"
                    + "১০. Lot Closing (Finish Production)\n"
                    + "লট সিলেক্ট করলে Blister Production থেকে Good/Reject Blister সামারি দেখাবে (Work Order Packaging "
                    + "স্পেকের বিপরীতে Complete/In Progress স্ট্যাটাসসহ)। MBR No, MPR No, Closing Date, Loose Product "
                    + "Qty, Remarks পূরণ করে \"Save Closing Details\" চাপুন। নিচের গ্রিডে প্যাকেজিং মেটেরিয়াল (খালি "
                    + "কার্টন, খালি শিপার, লিফলেট, লেবেল) এর Issued/Used/Waste দিলে Returned to Store স্বয়ংক্রিয়ভাবে "
                    + "হিসাব হবে — \"Save Material Returns\" দিয়ে সংরক্ষণ করুন। সব ঠিক থাকলে \"Mark Lot as Closed\" "
                    + "চাপলে লটের স্ট্যাটাস Closed হয়ে যাবে।\n\n"
                    + "সমস্যা হলে বা প্রশ্ন থাকলে উপরের Contact বাটনে ক্লিক করুন।";

    private static final String EN_APP_HELP =
            "How to use the PPES app\n"
                    + "================================\n\n"
                    + "1. Menu\n"
                    + "The left sidebar groups every table and module: Masters, Order Intake, "
                    + "Production (Lot Management), Work Order, Carton Tracking, Shift Production, "
                    + "Lot Closing, Packing & Dispatch, Shift & Reconciliation, and Reports.\n\n"
                    + "2. Adding a record (standard table forms)\n"
                    + "Clicking any menu item opens a form at the top. Fields that reference another "
                    + "table (e.g. Product, Lot, Employee) are dropdowns — pick a value, then click Save.\n\n"
                    + "3. Record list & delete\n"
                    + "Below the form, the most recent 200 records are listed. Select a row and click "
                    + "Delete Selected to remove it (a confirmation prompt appears first).\n\n"
                    + "4. Excel Export / Import\n"
                    + "Export to Excel saves the current list as a .xlsx file. Import from Excel loads "
                    + "many records at once from a .xlsx file (column headers must match the field names).\n\n"
                    + "5. Reports\n"
                    + "The Reports menu has 6 pre-built reports (Lot Summary, Production vs Target, "
                    + "IPC Inspection, Raw Material Reconciliation, Pallet & Shipper, Shift Handover), "
                    + "each exportable to Excel.\n\n"
                    + "6. Login\n"
                    + "On first use, if no user exists yet, click \"Skip Login (Setup Mode)\" on the "
                    + "login screen, then create a user from Masters > Users.\n\n"
                    + "7. Carton Tracking Log\n"
                    + "Select a lot, fill in the header (Room No, Expiry Date, Order Qty, Starting "
                    + "Label No, Ctns in Shipper, Shift Standard). In the grid below, type Hour and "
                    + "Label No After — Standard Ctns, Total Shippers, Actual Ctns, and Percent are "
                    + "calculated automatically.\n\n"
                    + "8. Shift Production Log\n"
                    + "For each shift, enter Date, Shift, Final Shipper, No of Shipper, No of Unit "
                    + "Produced, and Recorded By — Total No of Unit Produced is auto-calculated as a "
                    + "running total.\n\n"
                    + "9. Work Order — Packaging Requirement\n"
                    + "Select a lot, enter Order Qty (Cartons) and the packing ratios (Tablets/Blister, "
                    + "Blisters/Carton, Cartons/Bundle, Bundles/Shipper, Shippers/Layer, Layers/Pallet). "
                    + "The screen instantly shows Total Required (Blisters/Cartons/Bundles/Shippers/"
                    + "Pallets/Labels) for the full order, how many have been Produced So Far (pulled "
                    + "from Shift Production Log's shipper counts), and how many are still Remaining.\n\n"
                    + "10. Lot Closing (Finish Production)\n"
                    + "Selecting a lot shows a live Good/Reject Blister summary from Blister Production "
                    + "against the required quantity from the Work Order Packaging spec (Complete / In "
                    + "Progress status). Fill in MBR No, MPR No, Closing Date, Loose Product Qty and "
                    + "Remarks, then click \"Save Closing Details\". In the grid below, enter Issued/"
                    + "Used/Waste for each packaging material (empty cartons, empty shippers, leaflets, "
                    + "labels) — Returned to Store is calculated automatically; click \"Save Material "
                    + "Returns\" to record it. Once everything checks out, click \"Mark Lot as Closed\" "
                    + "to set the lot's status to Closed.\n\n"
                    + "For questions or issues, click the Contact button at the top.";

    private static final String BN_SOP_HELP =
            "লট স্টার্টিং প্রসেস — বিভাগভিত্তিক দায়িত্ব, OEB ও শব্দকোষ\n"
                    + "================================================================\n\n"
                    + "⚠️ দ্রষ্টব্য: এটি ফার্মা প্যাকেজিং শিল্পের প্রচলিত অনুশীলনের ওপর ভিত্তি করে একটি "
                    + "সাধারণ রেফারেন্স গাইড — আপনার প্রতিষ্ঠানের অনুমোদিত SOP, GMP/cGMP ম্যানুয়াল, বা "
                    + "নিয়ন্ত্রক (FDA/DGDA/EMA) নির্দেশনার বিকল্প নয়। কমপ্লায়েন্স-সংক্রান্ত যেকোনো "
                    + "সিদ্ধান্তে সবসময় নিজের QA/Regulatory Affairs বিভাগ ও প্রতিষ্ঠানের বর্তমান "
                    + "অনুমোদিত SOP অনুসরণ করুন।\n\n"
                    + "১. লট শুরুর আগে বিভাগভিত্তিক কাজ\n"
                    + "----------------------------------------------------------------\n"
                    + "Inventory (স্টোর) বিভাগ:\n"
                    + "  - BMR/BPR (Batch Manufacturing/Packing Record) অনুযায়ী কাঁচামাল ও প্যাকেজিং "
                    + "মেটেরিয়াল মজুদ যাচাই করা।\n"
                    + "  - অনুমোদিত রিকুইজিশন অনুযায়ী ইস্যু, ব্যাচ/লট নম্বর মিলিয়ে, Released স্ট্যাটাস "
                    + "নিশ্চিত করে (Quarantine/Rejected স্টক ইস্যু করা যাবে না)।\n"
                    + "  - FEFO (First-Expiry-First-Out) মেনে ইস্যু।\n"
                    + "  - এই অ্যাপে: Raw Material Receiving মডিউলে ইস্যু হওয়া কোয়ান্টিটি রেকর্ড হয়।\n\n"
                    + "QC (Quality Control) বিভাগ:\n"
                    + "  - ব্যবহারের আগে কাঁচামাল/প্যাকেজিং মেটেরিয়াল স্যাম্পলিং ও টেস্টিং।\n"
                    + "  - ব্লিস্টারিং চলাকালীন IPC (Tablet Check, Seal Check, Coding Check) — এই অ্যাপে "
                    + "IPC Inspection মডিউলে।\n"
                    + "  - স্পেসিফিকেশন অনুযায়ী অ্যাপ্রুভ/রিজেক্ট সিদ্ধান্ত।\n"
                    + "  - ফাইনাল রিলিজের আগে এক্সিকিউটেড BMR/BPR রিভিউ।\n\n"
                    + "Validation বিভাগ:\n"
                    + "  - লট শুরুর আগে ইকুইপমেন্ট/প্রসেস কোয়ালিফিকেশন (IQ/OQ/PQ) বৈধ আছে কিনা নিশ্চিত করা।\n"
                    + "  - লাইন ক্লিয়ারেন্স (আগের প্রোডাক্ট/ব্যাচের অবশিষ্টাংশ নেই) যাচাই।\n"
                    + "  - প্রসেস পরিবর্তন হলে Change Control-এর মাধ্যমে অনুমোদন লাগবে।\n\n"
                    + "EHS (Environment, Health & Safety) বিভাগ:\n"
                    + "  - প্রোডাক্টের OEB ক্লাসিফিকেশন অনুযায়ী PPE প্রয়োজনীয়তা নিশ্চিত করা।\n"
                    + "  - OEB4/OEB5 পোটেন্ট কম্পাউন্ডের ক্ষেত্রে কন্টেইনমেন্ট ব্যবস্থা কার্যকর আছে কিনা যাচাই।\n"
                    + "  - হ্যাজার্ডাস/পোটেন্ট API হ্যান্ডলিংয়ের আগে সেফটি ব্রিফিং।\n\n"
                    + "Production বিভাগ:\n"
                    + "  - লাইন ক্লিয়ারেন্স চেকলিস্ট (এই অ্যাপে Startup Checklist মডিউল) পূরণ ও স্বাক্ষরিত।\n"
                    + "  - সঠিক ভার্সনের BMR/BPR ইস্যু হয়েছে কিনা যাচাই।\n"
                    + "  - শিফট হ্যান্ডওভার নোট রিভিউ করা (Shift Handover মডিউল)।\n"
                    + "  - Inventory, QC, Validation, EHS-এর সাইন-অফ পাওয়ার পরই প্রোডাকশন শুরু করা।\n\n"
                    + "২. OEB1–OEB5 (Occupational Exposure Band) ক্লাসিফিকেশন\n"
                    + "----------------------------------------------------------------\n"
                    + "বিভিন্ন ধরনের ওষুধ/API-এর পোটেন্সি/হ্যাজার্ড লেভেল অনুযায়ী কন্টেইনমেন্ট ও PPE ঠিক করতে "
                    + "OEB ব্যবহার হয়:\n"
                    + "  - OEB1 (কম পোটেন্সি): সাধারণ PPE (গ্লাভস, মাস্ক, গাউন) — সাধারণত ওপেন হ্যান্ডলিং "
                    + "গ্রহণযোগ্য।\n"
                    + "  - OEB2 (কম-মাঝারি পোটেন্সি): সাধারণ PPE + ভালো হাউজকিপিং প্র্যাকটিস।\n"
                    + "  - OEB3 (মাঝারি-উচ্চ পোটেন্সি): লোকাল এক্সহস্ট ভেন্টিলেশন প্রয়োজন, অতিরিক্ত PPE "
                    + "(রেসপিরেটর লাগতে পারে)।\n"
                    + "  - OEB4 (উচ্চ পোটেন্সি): কন্টেইনমেন্ট (আইসোলেটর/ক্লোজড সিস্টেম) প্রয়োজন, ফুল PPE, "
                    + "সীমিত এক্সেস।\n"
                    + "  - OEB5 (সর্বোচ্চ পোটেন্সি, যেমন কিছু হরমোন/সাইটোটক্সিক): ফুল কন্টেইনমেন্ট, প্রায়ই "
                    + "ডেডিকেটেড সুবিধা/ইকুইপমেন্ট, সর্বোচ্চ PPE, কড়া এক্সেস কন্ট্রোল।\n\n"
                    + "(দ্রষ্টব্য: OEB ব্যান্ড সাধারণত প্রতিটি প্রতিষ্ঠানের নিজস্ব টক্সিকোলজি/EHS টিম প্রতিটি "
                    + "কম্পাউন্ডের জন্য নির্ধারণ করে — উপরের বর্ণনা সাধারণ শিল্প-প্রচলিত নির্দেশিকা, নির্দিষ্ট "
                    + "কোনো ফিক্সড মান নয়। আপনার প্রকৃত OEL (Occupational Exposure Limit) মান আপনার "
                    + "EHS/টক্সিকোলজি বিভাগের অ্যাসেসমেন্ট থেকে নিতে হবে।)\n\n"
                    + "৩. GMP/cGMP/SOP/FDA — সংক্ষিপ্ত পরিচিতি\n"
                    + "----------------------------------------------------------------\n"
                    + "  - GMP/cGMP: একটি সামগ্রিক কোয়ালিটি সিস্টেম, যা নিশ্চিত করে প্রোডাক্ট সবসময় নির্দিষ্ট "
                    + "মান অনুযায়ী তৈরি/নিয়ন্ত্রিত হয় (ডকুমেন্টেশন, ইকুইপমেন্ট কোয়ালিফিকেশন, ট্রেনিং, "
                    + "ডেভিয়েশন/CAPA ম্যানেজমেন্ট)।\n"
                    + "  - SOP: প্রতিষ্ঠানের নিজস্ব অনুমোদিত, নিয়ন্ত্রিত ডকুমেন্ট যা প্রতিটি কাজ ঠিক কীভাবে করতে "
                    + "হবে তা বর্ণনা করে — এই অ্যাপ SOP-এর বিকল্প নয়, শুধু ডাটা ক্যাপচার/ট্র্যাকিংয়ে সাহায্য করে।\n"
                    + "  - FDA (বা স্থানীয় নিয়ন্ত্রক, যেমন বাংলাদেশে DGDA): সামগ্রিক cGMP প্রয়োজনীয়তা "
                    + "নির্ধারণ করে (US FDA-র জন্য 21 CFR Part 210/211) — ভ্যালিডেশন, ডকুমেন্টেশন, চেঞ্জ "
                    + "কন্ট্রোল, ডেভিয়েশন হ্যান্ডলিং কভার করে।\n"
                    + "  - এই অ্যাপ একটি ডাটা-ক্যাপচার/ট্র্যাকিং টুল; এটি আপনার Quality Management System, "
                    + "DMS, বা প্রতিষ্ঠানের SOP-র বিকল্প নয়। কমপ্লায়েন্স-সংক্রান্ত সিদ্ধান্তে সবসময় QA-র সাথে "
                    + "পরামর্শ করুন।\n\n"
                    + "৪. Line Leader বনাম Packer — দায়িত্ব\n"
                    + "----------------------------------------------------------------\n"
                    + "Line Leader:\n"
                    + "  - শিফটে পুরো প্যাকিং লাইনের সার্বিক দায়িত্বে থাকেন।\n"
                    + "  - স্টার্টআপের আগে লাইন ক্লিয়ারেন্স সম্পন্ন ও স্বাক্ষরিত হয়েছে তা নিশ্চিত করেন।\n"
                    + "  - QC-এর সাথে IPC চেক, Mechanics/Maintenance-এর সাথে মেশিন ইস্যু, Inventory-এর সাথে "
                    + "মেটেরিয়াল রিপ্লেনিশমেন্ট সমন্বয় করেন।\n"
                    + "  - প্রোডাকশন রেট বনাম টার্গেট মনিটর করেন (Shift Production Log / Work Order "
                    + "Packaging Requirement মডিউল ব্যবহার করে)।\n"
                    + "  - ডেভিয়েশন হলে সাথে সাথে QA/Production Manager-কে এসকেলেট করেন।\n"
                    + "  - শিফট হ্যান্ডওভার নোট সাইন-অফ করেন।\n\n"
                    + "Packer:\n"
                    + "  - SOP অনুযায়ী প্রকৃত প্যাকিং অপারেশন (ব্লিস্টার কার্টনে, কার্টন বান্ডেল/শিপারে "
                    + "লোড করা) করেন।\n"
                    + "  - প্যাকিং চলাকালীন বেসিক ভিজ্যুয়াল চেক করেন (স্পষ্ট ডিফেক্ট পেলে Line Leader/QC-কে "
                    + "জানান)।\n"
                    + "  - GMP প্র্যাকটিস মেনে চলেন: গাউনিং, হাইজিন, লাইনে অননুমোদিত জিনিস না রাখা।\n"
                    + "  - মেশিন ইস্যু হলে Line Leader-কে জানান (যিনি Mechanics/Maintenance-কে এসকেলেট করবেন)।\n"
                    + "  - কাউন্টিং একুরেসি বজায় রাখেন (গরমিল পেলে সাথে সাথে জানান)।\n\n"
                    + "৫. আন্তঃবিভাগীয় যোগাযোগে ব্যবহৃত শব্দকোষ (Audit ও Mechanics সহ)\n"
                    + "----------------------------------------------------------------\n"
                    + "Audit বিভাগের সাথে কথা বলার সময়: Audit Trail, Deviation, CAPA (Corrective and "
                    + "Preventive Action), Non-conformance, Observation, Root Cause Analysis, "
                    + "Self-Inspection, Regulatory Audit, Data Integrity (ALCOA+)।\n\n"
                    + "Mechanics/Maintenance বিভাগের সাথে কথা বলার সময়: Breakdown, Preventive Maintenance "
                    + "(PM), Changeover, Downtime, Spare Parts, Calibration, Machine Qualification, OEE "
                    + "(Overall Equipment Effectiveness)।\n\n"
                    + "সাধারণ আন্তঃবিভাগীয় শব্দ: Line Clearance, Batch Record, Deviation, Change Control, "
                    + "Hold Time, In-Process Control (IPC), Release, Quarantine, Rework, Reprocessing, "
                    + "Yield, Reconciliation।";

    private static final String EN_SOP_HELP =
            "Lot Starting Process — Department Responsibilities, OEB & Vocabulary\n"
                    + "================================================================\n\n"
                    + "⚠️ Note: this is general reference material based on common pharmaceutical "
                    + "packaging industry practice — it does NOT replace your site's approved SOPs, "
                    + "GMP/cGMP manual, or applicable regulator (FDA/DGDA/EMA, etc.) requirements. "
                    + "Always follow your site's current approved SOP and consult QA/Regulatory Affairs "
                    + "for compliance decisions.\n\n"
                    + "1. Department-wise work before starting a lot\n"
                    + "----------------------------------------------------------------\n"
                    + "Inventory (Store) department:\n"
                    + "  - Verify raw material and packaging material stock against BMR/BPR (Batch "
                    + "Manufacturing/Packing Record) requirements.\n"
                    + "  - Issue against an approved requisition, matching batch/lot number, confirming "
                    + "Released status (never issue Quarantine/Rejected stock).\n"
                    + "  - Follow FEFO (First-Expiry-First-Out) for issuance.\n"
                    + "  - In this app: quantities issued are recorded in the Raw Material Receiving "
                    + "module.\n\n"
                    + "QC (Quality Control) department:\n"
                    + "  - Sample and test raw/packaging materials before release for use.\n"
                    + "  - Perform in-process checks (IPC) during blistering — Tablet Check, Seal Check, "
                    + "Coding Check — tracked in this app's IPC Inspection module.\n"
                    + "  - Approve/reject against specification.\n"
                    + "  - Review the executed BMR/BPR before final release.\n\n"
                    + "Validation department:\n"
                    + "  - Confirm equipment/process qualification (IQ/OQ/PQ) is current before lot "
                    + "start.\n"
                    + "  - Verify line clearance (no residue from the previous product/batch).\n"
                    + "  - Any process change must go through Change Control before use.\n\n"
                    + "EHS (Environment, Health & Safety) department:\n"
                    + "  - Confirm PPE requirements per the product's OEB classification.\n"
                    + "  - For OEB4/OEB5 potent compounds, verify containment measures are functioning.\n"
                    + "  - Safety briefing before handling hazardous/potent APIs.\n\n"
                    + "Production department:\n"
                    + "  - Complete and sign the line clearance checklist (Startup Checklist module in "
                    + "this app).\n"
                    + "  - Confirm the correct BMR/BPR version has been issued.\n"
                    + "  - Review shift handover notes (Shift Handover module).\n"
                    + "  - Start production only after sign-off from Inventory, QC, Validation, and EHS.\n\n"
                    + "2. OEB1-OEB5 (Occupational Exposure Band) classification\n"
                    + "----------------------------------------------------------------\n"
                    + "OEB bands set containment and PPE requirements based on a compound's potency/"
                    + "hazard level:\n"
                    + "  - OEB1 (low potency): standard PPE (gloves, mask, gown) — open handling "
                    + "generally acceptable.\n"
                    + "  - OEB2 (low-moderate potency): standard PPE + good housekeeping practice.\n"
                    + "  - OEB3 (moderate-high potency): requires local exhaust ventilation, additional "
                    + "PPE (a respirator may be needed).\n"
                    + "  - OEB4 (high potency): requires containment (isolators/closed systems), full "
                    + "PPE, restricted access.\n"
                    + "  - OEB5 (highest potency, e.g. certain hormones/cytotoxics): full containment, "
                    + "often a dedicated facility/equipment, highest-level PPE, strict access control.\n\n"
                    + "(Note: OEB bands are normally set by each company's own toxicology/EHS team per "
                    + "compound — the descriptions above are general industry convention, not fixed "
                    + "universal values. Your actual OEL — Occupational Exposure Limit — figures must "
                    + "come from your EHS/toxicology department's assessment.)\n\n"
                    + "3. GMP / cGMP / SOP / FDA — quick reference\n"
                    + "----------------------------------------------------------------\n"
                    + "  - GMP/cGMP: an overarching quality system that ensures product is consistently "
                    + "produced and controlled to a defined standard (documentation, equipment "
                    + "qualification, training, deviation/CAPA management).\n"
                    + "  - SOP: your site's own approved, controlled document describing exactly how "
                    + "each task must be performed — this app does not replace your SOPs, it only "
                    + "supports data capture/tracking.\n"
                    + "  - FDA (or your local regulator, e.g. DGDA in Bangladesh): sets overarching "
                    + "cGMP requirements (21 CFR Part 210/211 for the US FDA) covering validation, "
                    + "documentation, change control, and deviation handling.\n"
                    + "  - This app is a data-capture/tracking tool; it is not a substitute for your "
                    + "Quality Management System, DMS, or your site's SOPs. Always consult QA for "
                    + "compliance-critical decisions.\n\n"
                    + "4. Line Leader vs Packer — responsibilities\n"
                    + "----------------------------------------------------------------\n"
                    + "Line Leader:\n"
                    + "  - Overall responsible for the packing line during the shift.\n"
                    + "  - Ensures line clearance is completed and signed before startup.\n"
                    + "  - Coordinates with QC for IPC checks, with Mechanics/Maintenance for machine "
                    + "issues, and with Inventory for material replenishment.\n"
                    + "  - Monitors production rate vs target (using the Shift Production Log / Work "
                    + "Order Packaging Requirement modules).\n"
                    + "  - Escalates deviations to QA/Production Manager immediately.\n"
                    + "  - Signs off shift handover notes.\n\n"
                    + "Packer:\n"
                    + "  - Executes the actual packing operations (loading blisters into cartons, "
                    + "cartons into bundles/shippers) per SOP.\n"
                    + "  - Performs basic visual checks while packing (reports obvious defects to the "
                    + "Line Leader/QC).\n"
                    + "  - Follows GMP practice: gowning, hygiene, no unauthorized items on the line.\n"
                    + "  - Reports machine issues to the Line Leader (who escalates to Mechanics/"
                    + "Maintenance).\n"
                    + "  - Maintains counting accuracy (reports discrepancies immediately).\n\n"
                    + "5. Cross-department vocabulary (including Audit and Mechanics)\n"
                    + "----------------------------------------------------------------\n"
                    + "With the Audit department: Audit Trail, Deviation, CAPA (Corrective and "
                    + "Preventive Action), Non-conformance, Observation, Root Cause Analysis, "
                    + "Self-Inspection, Regulatory Audit, Data Integrity (ALCOA+).\n\n"
                    + "With the Mechanics/Maintenance department: Breakdown, Preventive Maintenance "
                    + "(PM), Changeover, Downtime, Spare Parts, Calibration, Machine Qualification, OEE "
                    + "(Overall Equipment Effectiveness).\n\n"
                    + "General cross-department terms: Line Clearance, Batch Record, Deviation, Change "
                    + "Control, Hold Time, In-Process Control (IPC), Release, Quarantine, Rework, "
                    + "Reprocessing, Yield, Reconciliation.";

    private HelpDialog() {
    }

    public static void show(Component parent) {
        JTextArea appArea = new JTextArea(BN_APP_HELP);
        styleArea(appArea);

        JTextArea sopArea = new JTextArea(BN_SOP_HELP);
        styleArea(sopArea);

        JButton bnBtn = new JButton("বাংলা");
        JButton enBtn = new JButton("English");
        bnBtn.addActionListener(e -> {
            appArea.setText(BN_APP_HELP);
            appArea.setCaretPosition(0);
            sopArea.setText(BN_SOP_HELP);
            sopArea.setCaretPosition(0);
        });
        enBtn.addActionListener(e -> {
            appArea.setText(EN_APP_HELP);
            appArea.setCaretPosition(0);
            sopArea.setText(EN_SOP_HELP);
            sopArea.setCaretPosition(0);
        });
        JPanel langRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        langRow.add(new JLabel("Language / ভাষা:"));
        langRow.add(bnBtn);
        langRow.add(enBtn);

        JScrollPane appScroll = new JScrollPane(appArea);
        JScrollPane sopScroll = new JScrollPane(sopArea);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("App Usage", appScroll);
        tabs.addTab("Lot Starting SOP", sopScroll);
        tabs.setPreferredSize(new Dimension(640, 520));

        JPanel container = new JPanel(new BorderLayout(0, 6));
        container.add(langRow, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(parent, container, "Help - সহায়তা", JOptionPane.PLAIN_MESSAGE);
    }

    private static void styleArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(area.getFont().deriveFont(13f));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setCaretPosition(0);
    }
}
