package com.ppes;

import com.formdev.flatlaf.FlatDarkLaf;
import com.ppes.ui.LoginDialog;
import com.ppes.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {
            // If FlatLaf can't install for some reason, fall back to whatever
            // Swing's default look and feel is rather than crashing.
        }
        loginAndShowMainFrame();
    }

    /** Shows the login screen; on success opens MainFrame; on logout, loops back here. */
    private static void loginAndShowMainFrame() {
        LoginDialog.LoginResult result = LoginDialog.showLoginDialog();
        if (result == null || !result.success) {
            System.exit(0);
            return;
        }
        MainFrame frame = new MainFrame(result, Main::loginAndShowMainFrame);
        frame.setVisible(true);
    }
}
