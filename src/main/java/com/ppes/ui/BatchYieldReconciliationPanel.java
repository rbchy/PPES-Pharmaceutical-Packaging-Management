package com.ppes.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import com.ppes.calc.YieldCalculator;

/**
 * "Batch Yield Reconciliation Calculator" — Java Swing recreation of the
 * standalone HTML/Tailwind/JS widget the user shared.
 *
 * Inputs (all in units — tablets/blisters/packs, whichever unit the batch
 * is being reconciled in):
 *   I = Total Bulk Input
 *   P = Packed Good Yield (finished packs)
 *   S = Sampling Quantity (IPC / QA / retention)
 *   R = Rejected Waste (setup loss + rejects)
 *   U = Unused Good Returned
 *
 * Live outputs (recalculated on every keystroke via a DocumentListener —
 * no button needed, exactly like the HTML widget's oninput handler):
 *   Actual Output Yield        = P / I * 100
 *   Overall Reconciliation     = (P + S + R + U) / I * 100
 *
 * Compliance banner follows the same cGMP rule as the HTML version:
 *   99.0% <= Reconciliation <= 101.0%  -> PASS (green)
 *   Reconciliation < 99.0%             -> FAIL, Unaccounted Loss (red)
 *   Reconciliation > 101.0%            -> FAIL, Surplus Discrepancy (red)
 *
 * This panel does not read/write the database — it is a quick pocket
 * calculator, same as the HTML page. If useful later it can be wired to
 * pull Input/Packed/Reject numbers straight from work_order_packing_spec
 * and shift_production_log instead of manual entry.
 */

public class BatchYieldReconciliationPanel extends JPanel {

    private static final Color PASS_BG = new Color(0xecfdf5);
    private static final Color PASS_BORDER = new Color(0x10b981);
    private static final Color PASS_TEXT = new Color(0x065f46);

    private static final Color FAIL_BG = new Color(0xfff1f2);
    private static final Color FAIL_BORDER = new Color(0xf43f5e);
    private static final Color FAIL_TEXT = new Color(0x9f1239);

    private static final Color WARN_BG = new Color(0xfffbeb);
    private static final Color WARN_BORDER = new Color(0xf59e0b);
    private static final Color WARN_TEXT = new Color(0x92400e);

    private final JTextField inputField = new JTextField("100000", 12);
    private final JTextField packedField = new JTextField("98500", 12);
    private final JTextField samplingField = new JTextField("300", 12);
    private final JTextField rejectedField = new JTextField("1150", 12);
    private final JTextField unusedField = new JTextField("0", 12);

    private final JLabel actualYieldValue = new JLabel("0.00 %");
    private final JLabel reconciliationYieldValue = new JLabel("0.00 %");

    private final JPanel statusBanner = new JPanel();
    private final JLabel statusTitle = new JLabel("COMPLIANCE STATUS");
    private final JLabel statusText = new JLabel("PASS");
    private final JLabel statusSubText = new JLabel(" ");

    public BatchYieldReconciliationPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        add(new BannerPanel("/images/banner_batch_yield.jpg", "Batch Yield Reconciliation Calculator",
                "Pharma packaging line cGMP yield verification — live actual & reconciliation yield", 110),
                BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 18, 0));
        body.add(buildInputCard());
        body.add(buildResultCard());
        add(body, BorderLayout.CENTER);

        DocumentListener liveListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalculate(); }
            @Override public void removeUpdate(DocumentEvent e) { recalculate(); }
            @Override public void changedUpdate(DocumentEvent e) { recalculate(); }
        };
        for (JTextField f : new JTextField[]{inputField, packedField, samplingField, rejectedField, unusedField}) {
            f.getDocument().addDocumentListener(liveListener);
        }

        recalculate();
    }

    // ================================================================
    // Left column: inputs
    // ================================================================

    private JPanel buildInputCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("১. ব্যাচ ইনপুট (Batch Inputs — Units)"),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        card.add(fieldRow("Total Bulk Input (I)", "যেমন: ট্যাবলেট / ব্লিস্টার", inputField));
        card.add(Box.createVerticalStrut(8));
        card.add(fieldRow("Packed Good Yield (P)", "ফিনিশড প্যাক সংখ্যা", packedField));
        card.add(Box.createVerticalStrut(8));
        card.add(fieldRow("Sampling Quantity (S)", "IPC / QA / রিটেনশন স্যাম্পল", samplingField));
        card.add(Box.createVerticalStrut(8));
        card.add(fieldRow("Rejected Waste (R)", "সেটআপ লস + রিজেক্ট", rejectedField));
        card.add(Box.createVerticalStrut(8));
        card.add(fieldRow("Unused Good Returned (U)", "স্টোরে ফেরত অব্যবহৃত ভালো মাল", unusedField));
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel fieldRow(String label, String hint, JTextField field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel h = new JLabel(hint);
        h.setFont(h.getFont().deriveFont(Font.PLAIN, 10.5f));
        h.setForeground(new Color(0x64748b));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        row.add(l);
        row.add(h);
        row.add(Box.createVerticalStrut(3));
        row.add(field);
        return row;
    }

    // ================================================================
    // Right column: live results + status
    // ================================================================

    private JPanel buildResultCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("২. রিকনসিলিয়েশন ফলাফল (লাইভ)"),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)));

        JPanel actualCard = resultCard("Actual Output Yield  (P / I × 100)", actualYieldValue,
                "লক্ষ্যমাত্রা: ≥ 98.0%", 20f);
        JPanel reconCard = resultCard("Overall Reconciliation  ([P+S+R+U] / I × 100)", reconciliationYieldValue,
                "cGMP সীমা: 99.0% থেকে 101.0%", 26f);

        card.add(actualCard);
        card.add(Box.createVerticalStrut(10));
        card.add(reconCard);
        card.add(Box.createVerticalStrut(14));

        statusBanner.setLayout(new BoxLayout(statusBanner, BoxLayout.Y_AXIS));
        statusBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PASS_BORDER, 2),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        statusTitle.setFont(statusTitle.getFont().deriveFont(Font.BOLD, 10.5f));
        statusTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusText.setFont(statusText.getFont().deriveFont(Font.BOLD, 24f));
        statusText.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusSubText.setFont(statusSubText.getFont().deriveFont(Font.PLAIN, 11f));
        statusSubText.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusBanner.add(statusTitle);
        statusBanner.add(Box.createVerticalStrut(4));
        statusBanner.add(statusText);
        statusBanner.add(Box.createVerticalStrut(4));
        statusBanner.add(statusSubText);

        card.add(statusBanner);
        card.add(Box.createVerticalStrut(10));

        JLabel formula = new JLabel("<html>Formula: Reconciliation = (P + S + R + U) / I &times; 100</html>");
        formula.setFont(formula.getFont().deriveFont(Font.ITALIC, 10.5f));
        formula.setForeground(new Color(0x94a3b8));
        formula.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(formula);

        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel resultCard(String title, JLabel valueLabel, String subtitle, float valueFontSize) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBackground(new Color(0xf8fafc));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe2e8f0)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.PLAIN, 11f));
        t.setForeground(new Color(0x64748b));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, valueFontSize));
        valueLabel.setForeground(new Color(0x1e293b));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(s.getFont().deriveFont(Font.PLAIN, 9.5f));
        s.setForeground(new Color(0x94a3b8));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(t);
        p.add(valueLabel);
        p.add(s);
        return p;
    }

    // ================================================================
    // Live calculation — mirrors the HTML widget's calculateYield()
    // ================================================================

    private void recalculate() {
        double input = parse(inputField);
        double packed = parse(packedField);
        double sampling = parse(samplingField);
        double rejected = parse(rejectedField);
        double unused = parse(unusedField);

        YieldCalculator.Result r = YieldCalculator.calculate(input, packed, sampling, rejected, unused);
        if (r.status == YieldCalculator.Status.INVALID_INPUT) {
            actualYieldValue.setText("0.00 %");
            reconciliationYieldValue.setText("0.00 %");
            updateStatus("INVALID INPUT",
                    "Input (I) শূন্যের চেয়ে বেশি এবং কোনো পরিমাণ ঋণাত্মক হতে পারবে না।", WARN_BG, WARN_BORDER, WARN_TEXT);
            return;
        }

        actualYieldValue.setText(String.format("%.2f %%", r.actualYield));
        reconciliationYieldValue.setText(String.format("%.2f %%", r.reconciliationYield));

        switch (r.status) {
            case PASS:
                updateStatus("PASS", "Reconciliation cGMP সীমার মধ্যে আছে (99.0% - 101.0%)।", PASS_BG, PASS_BORDER, PASS_TEXT);
                break;
            case UNACCOUNTED_LOSS:
                updateStatus("FAIL (UNACCOUNTED LOSS)",
                        String.format("অজানা ক্ষতি শনাক্ত হয়েছে (%.2f%%)। OOS ইনভেস্টিগেশন শুরু করুন।", r.unaccountedPercent()),
                        FAIL_BG, FAIL_BORDER, FAIL_TEXT);
                break;
            default:
                updateStatus("FAIL (SURPLUS DISCREPANCY)",
                        String.format("Reconciliation 101.0%%-এর বেশি (%.2f%%)। কাউন্ট ও র’ মেটেরিয়াল ইনপুট যাচাই করুন।",
                                r.reconciliationYield),
                        FAIL_BG, FAIL_BORDER, FAIL_TEXT);
        }
    }

    private double parse(JTextField field) {
        try {
            String text = field.getText().trim();
            if (text.isEmpty()) {
                return 0;
            }
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void updateStatus(String text, String subtext, Color bg, Color border, Color fg) {
        statusText.setText(text);
        statusSubText.setText("<html><div style='text-align:center;width:220px;'>" + subtext + "</div></html>");
        statusBanner.setBackground(bg);
        statusBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 2),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        statusTitle.setForeground(fg);
        statusText.setForeground(fg);
        statusSubText.setForeground(fg);
    }
}
