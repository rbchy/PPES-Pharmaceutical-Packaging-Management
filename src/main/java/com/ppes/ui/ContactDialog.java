package com.ppes.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Static contact-info popup, shown from the top bar's Contact button.
 * The three constants below are placeholders — edit them to your real
 * support details before distributing this app.
 */
public final class ContactDialog {

    private static final String COMPANY_NAME = "Your Company Name";
    private static final String SUPPORT_EMAIL = "support@example.com";
    private static final String SUPPORT_PHONE = "+880-XXXX-XXXXXX";

    private ContactDialog() {
    }

    public static void show(Component parent) {
        String message = "<html><div style='width:280px;'>"
                + "<b>" + COMPANY_NAME + "</b><br><br>"
                + "সাপোর্ট ইমেইল: " + SUPPORT_EMAIL + "<br>"
                + "সাপোর্ট ফোন: " + SUPPORT_PHONE + "<br><br>"
                + "কোনো সমস্যা বা ফিচার রিকোয়েস্ট থাকলে উপরের তথ্যে যোগাযোগ করুন।"
                + "</div></html>";
        JOptionPane.showMessageDialog(parent, message, "Contact Us", JOptionPane.INFORMATION_MESSAGE);
    }
}
