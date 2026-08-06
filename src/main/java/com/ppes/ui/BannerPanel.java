package com.ppes.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable header banner: a photo (loaded from classpath resources), a dark
 * gradient overlay for text readability, and a bold title + subtitle drawn
 * on top. Used at the top of the Dashboard and each of the custom lot
 * screens (Carton Tracking, Shift Production, Work Order Packaging, Lot
 * Closing) with a different cropped/tinted image per screen so each module
 * has its own visual identity while reusing the same source photo.
 */
public class BannerPanel extends JPanel {

    private final Image image;
    private final String title;
    private final String subtitle;

    public BannerPanel(String classpathImage, String title, String subtitle, int height) {
        this.image = loadImage(classpathImage);
        this.title = title;
        this.subtitle = subtitle;
        setPreferredSize(new Dimension(10, height));
        setOpaque(true);
    }

    private static Image loadImage(String classpathImage) {
        if (classpathImage == null) {
            return null;
        }
        java.net.URL url = BannerPanel.class.getResource(classpathImage);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        if (image != null) {
            int iw = image.getWidth(this);
            int ih = image.getHeight(this);
            if (iw > 0 && ih > 0) {
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) Math.ceil(iw * scale);
                int sh = (int) Math.ceil(ih * scale);
                int x = (w - sw) / 2;
                int y = (h - sh) / 2;
                g2.drawImage(image, x, y, sw, sh, this);
            }
        } else {
            g2.setColor(new Color(0x0a1a28));
            g2.fillRect(0, 0, w, h);
        }

        g2.setPaint(new GradientPaint(0, 0, new Color(15, 20, 30, 190), 0, h, new Color(15, 20, 30, 90)));
        g2.fillRect(0, 0, w, h);

        if (title != null) {
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString(title, 20, h - (subtitle != null ? 40 : 18));
        }
        if (subtitle != null) {
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12.5f));
            g2.setColor(new Color(0xd7dee8));
            g2.drawString(subtitle, 20, h - 16);
        }
        g2.dispose();
    }
}
