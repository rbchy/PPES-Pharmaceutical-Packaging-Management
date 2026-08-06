package com.ppes.ui;

import com.ppes.model.TableDef;
import com.ppes.model.TableRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window. Professional slate-teal dark theme. Navigation
 * is now a single top navigation bar with three rows (no sidebar — content
 * gets the full window width):
 *   Row 1: brand + logged-in user + Help/Contact/Logout
 *   Row 2: "quick access" priority buttons — the 7 day-to-day operational
 *          screens (Dashboard, Work Order, Carton Tracking, Shift
 *          Production, Lot Closing, Yield Calculator, Reports)
 *   Row 3: "data tables" navigation — built generically from
 *          TableRegistry.MENU. Sections with more than one table (Masters,
 *          Production (Lot Management), Packing & Dispatch,
 *          Shift & Reconciliation) become dropdown menu buttons; sections
 *          with a single table (Order Intake, Waste & Placards) become
 *          plain direct buttons.
 */
public class MainFrame extends JFrame {

    // Professional slate-teal palette. Deep navy-slate background reads
    // clean/clinical for a pharma QA tool, with a teal accent for the
    // active/selected screen and a lighter slate for hover states.
    private static final Color SIDEBAR_BG = new Color(0x0e2436);
    private static final Color SIDEBAR_HOVER = new Color(0x1c3a52); // light hover highlight
    private static final Color SIDEBAR_TEXT = new Color(0xd7e4ec);
    private static final Color SIDEBAR_MUTED = new Color(0x7ea0b8);
    private static final Color ACCENT = new Color(0x2dd4bf);
    private static final Color TOPBAR_BG = new Color(0x0a1a28);
    private static final Color GROUP_BTN_BG = new Color(0x122c40);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private final Map<String, JButton> topNavButtons = new LinkedHashMap<>();
    private final Map<JButton, List<String>> dropdownGroupButtons = new LinkedHashMap<>();
    private String activeKey = "dashboard";

    // Priority-ordered quick-nav strip (Row 2 of the top bar) — the
    // day-to-day operational screens a line leader/packer/QA person opens
    // most often.
    private static final String[][] TOP_NAV = {
            {"dashboard", "Dashboard"},
            {"work_order_packaging", "Work Order"},
            {"carton_tracking", "Carton Tracking"},
            {"shift_production", "Shift Production"},
            {"lot_closing", "Lot Closing"},
            {"batch_yield_reconciliation", "Yield Calculator"},
            {"reports", "Reports"}
    };

    public MainFrame(LoginDialog.LoginResult loginInfo, Runnable onLogout) {
        super("PPES - Pharmaceuticals Packaging Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 840);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildTopBar(loginInfo, onLogout), BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        DashboardPanel dashboard = new DashboardPanel();
        addCard("dashboard", dashboard);

        for (Map.Entry<String, TableDef> entry : TableRegistry.TABLES.entrySet()) {
            CrudPanel panel = new CrudPanel(entry.getValue());
            addCard(entry.getKey(), panel);
        }

        WorkOrderPackagingPanel workOrderPackagingPanel = new WorkOrderPackagingPanel();
        addCard("work_order_packaging", workOrderPackagingPanel);

        CartonTrackingPanel cartonTrackingPanel = new CartonTrackingPanel();
        addCard("carton_tracking", cartonTrackingPanel);

        ShiftProductionPanel shiftProductionPanel = new ShiftProductionPanel();
        addCard("shift_production", shiftProductionPanel);

        LotClosingPanel lotClosingPanel = new LotClosingPanel();
        addCard("lot_closing", lotClosingPanel);

        ReportsPanel reportsPanel = new ReportsPanel();
        addCard("reports", reportsPanel);

        BatchYieldReconciliationPanel batchYieldPanel = new BatchYieldReconciliationPanel();
        addCard("batch_yield_reconciliation", batchYieldPanel);

        showCard("dashboard");
    }

    private void addCard(String key, JPanel panel) {
        cardPanel.add(panel, key);
        if (panel instanceof Refreshable) {
            refreshables.put(key, (Refreshable) panel);
        }
    }

    private void showCard(String key) {
        activeKey = key;
        cardLayout.show(cardPanel, key);
        Refreshable r = refreshables.get(key);
        if (r != null) {
            r.refresh();
        }
        updateActiveStyles();
    }

    private void updateActiveStyles() {
        for (Map.Entry<String, JButton> e : topNavButtons.entrySet()) {
            styleTopNavButton(e.getValue(), e.getKey().equals(activeKey));
        }
        for (Map.Entry<JButton, List<String>> e : dropdownGroupButtons.entrySet()) {
            styleDropdownButton(e.getKey(), e.getValue().contains(activeKey));
        }
    }

    // ================================================================
    // Top bar (brand row + 2 navigation rows)
    // ================================================================

    private JComponent buildTopBar(LoginDialog.LoginResult loginInfo, Runnable onLogout) {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBackground(TOPBAR_BG);
        outer.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT));

        // Row 1 — brand + user/help/contact/logout
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("PPES");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Pharmaceuticals Packaging Management");
        subtitle.setForeground(SIDEBAR_MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        left.add(title);
        left.add(subtitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        String who = loginInfo.setupMode
                ? "Setup Mode"
                : loginInfo.username + " (" + (loginInfo.role == null || loginInfo.role.isEmpty() ? "-" : loginInfo.role) + ")";
        JLabel userLabel = new JLabel(who);
        userLabel.setForeground(ACCENT);
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD, 12.5f));

        JButton helpBtn = new JButton("Help");
        helpBtn.addActionListener(e -> HelpDialog.show(this));

        JButton contactBtn = new JButton("Contact");
        contactBtn.addActionListener(e -> ContactDialog.show(this));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            onLogout.run();
        });

        right.add(userLabel);
        right.add(helpBtn);
        right.add(contactBtn);
        right.add(logoutBtn);

        row1.add(left, BorderLayout.WEST);
        row1.add(right, BorderLayout.EAST);
        row1.setBorder(BorderFactory.createEmptyBorder(8, 16, 6, 16));

        // Row 2 — priority quick-nav strip: the day-to-day operational
        // screens, one click away.
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setBorder(BorderFactory.createEmptyBorder(0, 12, 2, 16));
        JLabel row2Tag = new JLabel("QUICK ACCESS ");
        row2Tag.setForeground(SIDEBAR_MUTED);
        row2Tag.setFont(row2Tag.getFont().deriveFont(Font.BOLD, 10.5f));
        row2.add(row2Tag);
        for (String[] entry : TOP_NAV) {
            row2.add(topNavButton(entry[1], entry[0]));
        }

        // Row 3 — data-table navigation, built generically from
        // TableRegistry.MENU. Multi-table sections become dropdown menus
        // (this is where "Shift & Reconciliation" now lives, more visible
        // than before, one click away instead of a sidebar scroll);
        // single-table sections become plain direct buttons.
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row3.setOpaque(false);
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        row3.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 16));
        JLabel row3Tag = new JLabel("DATA TABLES ");
        row3Tag.setForeground(SIDEBAR_MUTED);
        row3Tag.setFont(row3Tag.getFont().deriveFont(Font.BOLD, 10.5f));
        row3.add(row3Tag);
        for (Map.Entry<String, List<String>> section : TableRegistry.MENU.entrySet()) {
            List<String> keys = section.getValue();
            if (keys.size() == 1) {
                row3.add(topNavButton(section.getKey(), keys.get(0)));
            } else {
                List<String[]> items = new ArrayList<>();
                for (String key : keys) {
                    TableDef td = TableRegistry.TABLES.get(key);
                    if (td != null) {
                        items.add(new String[]{key, td.label});
                    }
                }
                row3.add(dropdownButton(section.getKey(), items));
            }
        }

        outer.add(row1);
        outer.add(row2);
        outer.add(row3);
        return outer;
    }

    private JButton topNavButton(String text, String cardKey) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12.5f));
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        b.addActionListener(e -> showCard(cardKey));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!cardKey.equals(activeKey)) {
                    b.setBackground(SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                styleTopNavButton(b, cardKey.equals(activeKey));
            }
        });
        topNavButtons.put(cardKey, b);
        styleTopNavButton(b, cardKey.equals(activeKey));
        return b;
    }

    private void styleTopNavButton(JButton b, boolean active) {
        if (active) {
            b.setBackground(ACCENT);
            b.setForeground(new Color(0x0a1a28));
        } else {
            b.setBackground(GROUP_BTN_BG);
            b.setForeground(SIDEBAR_TEXT);
        }
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
    }

    /**
     * A top-bar button that opens a dropdown (JPopupMenu) listing every
     * table in that menu section. The button itself highlights (same teal
     * accent as a direct nav button) whenever the currently open screen is
     * one of its children, so you can tell which group you're inside even
     * after the menu closes.
     */
    private JButton dropdownButton(String groupLabel, List<String[]> items) {
        JButton b = new JButton(groupLabel + "  ▾");
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 12.5f));
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(SIDEBAR_BG);
        popup.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        for (String[] item : items) {
            String cardKey = item[0];
            String label = item[1];
            JMenuItem mi = new JMenuItem(label);
            mi.setBackground(SIDEBAR_BG);
            mi.setForeground(SIDEBAR_TEXT);
            mi.setFont(mi.getFont().deriveFont(13f));
            mi.setBorderPainted(false);
            mi.setOpaque(true);
            mi.addActionListener(e -> showCard(cardKey));
            mi.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    mi.setBackground(SIDEBAR_HOVER);
                    mi.setForeground(ACCENT);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    mi.setBackground(SIDEBAR_BG);
                    mi.setForeground(SIDEBAR_TEXT);
                }
            });
            popup.add(mi);
        }

        b.addActionListener(e -> popup.show(b, 0, b.getHeight()));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                List<String> keys = dropdownGroupButtons.get(b);
                if (keys == null || !keys.contains(activeKey)) {
                    b.setBackground(SIDEBAR_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                List<String> keys = dropdownGroupButtons.get(b);
                styleDropdownButton(b, keys != null && keys.contains(activeKey));
            }
        });

        List<String> keys = new ArrayList<>();
        for (String[] item : items) {
            keys.add(item[0]);
        }
        dropdownGroupButtons.put(b, keys);
        styleDropdownButton(b, keys.contains(activeKey));
        return b;
    }

    private void styleDropdownButton(JButton b, boolean active) {
        if (active) {
            b.setBackground(ACCENT);
            b.setForeground(new Color(0x0a1a28));
        } else {
            b.setBackground(GROUP_BTN_BG);
            b.setForeground(SIDEBAR_TEXT);
        }
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
    }
}
