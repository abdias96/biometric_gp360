package com.gp360.biometric.ui;

import com.gp360.biometric.model.Fingerprint;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FingerprintButton extends JButton {
    private Fingerprint fingerprint;
    private static final Color CAPTURED_COLOR = new Color(76, 175, 80);
    private static final Color PENDING_COLOR = new Color(158, 158, 158);
    private static final Color HOVER_COLOR = new Color(33, 150, 243);
    
    public FingerprintButton(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
        
        // Make the button larger and more visible
        setPreferredSize(new Dimension(130, 170));
        setMinimumSize(new Dimension(120, 160));
        setMaximumSize(new Dimension(140, 180));
        
        setBackground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(true);
        setContentAreaFilled(true);
        setOpaque(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Set initial text to make button visible
        setText("");
        setToolTipText(fingerprint.getDisplayName() + " - Click para capturar");
        
        // Add hover effect
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!fingerprint.isCaptured()) {
                    setBorder(BorderFactory.createLineBorder(HOVER_COLOR, 3));
                    setBackground(new Color(227, 242, 253));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                updateStatus();
            }
        });
        
        updateStatus();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw fingerprint icon - Ajustado para mejor visualización
        int iconSize = 80;
        int iconX = (getWidth() - iconSize) / 2;
        int iconY = 10;
        
        if (fingerprint.isCaptured()) {
            // Draw captured fingerprint
            drawFingerprintIcon(g2, iconX, iconY, iconSize, true);
            
            // Draw check mark
            g2.setColor(CAPTURED_COLOR);
            g2.fillOval(getWidth() - 25, 5, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("✓", getWidth() - 20, 20);
            
            // Draw quality indicator
            drawQualityIndicator(g2, 10, iconY + iconSize + 10, getWidth() - 20, fingerprint.getQualityScore());
        } else {
            // Draw placeholder fingerprint
            drawFingerprintIcon(g2, iconX, iconY, iconSize, false);
        }
        
        // Draw label at bottom
        g2.setColor(new Color(52, 73, 94));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String text = fingerprint.getDisplayName();
        int textX = (getWidth() - fm.stringWidth(text)) / 2;
        int textY = getHeight() - 25;
        g2.drawString(text, textX, textY);
        
        // Draw position below if needed
        if (fingerprint.getPosition() != null && !fingerprint.getPosition().equals(fingerprint.getDisplayName())) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(Color.GRAY);
            String position = "(" + fingerprint.getPosition() + ")";
            fm = g2.getFontMetrics();
            textX = (getWidth() - fm.stringWidth(position)) / 2;
            textY = getHeight() - 5;
            g2.drawString(position, textX, textY);
        }
        
        g2.dispose();
    }
    
    private void drawFingerprintIcon(Graphics2D g2, int x, int y, int size, boolean captured) {
        // Draw oval shape for finger
        if (captured) {
            g2.setColor(new Color(76, 175, 80, 30));
            g2.fillOval(x, y, size, size + 20);
            g2.setColor(CAPTURED_COLOR);
        } else {
            g2.setColor(new Color(158, 158, 158, 30));
            g2.fillOval(x, y, size, size + 20);
            g2.setColor(PENDING_COLOR);
        }
        
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(x, y, size, size + 20);
        
        // Draw fingerprint ridges
        int centerX = x + size / 2;
        int centerY = y + size / 2 + 10;
        
        g2.setStroke(new BasicStroke(1.5f));
        
        // Draw concentric curves to simulate fingerprint
        for (int i = 0; i < 6; i++) {
            int radius = 5 + i * 7;
            // Top arc
            g2.drawArc(centerX - radius, centerY - radius/2, 
                      radius * 2, radius, 0, 180);
            // Bottom arc
            g2.drawArc(centerX - radius, centerY - radius/2, 
                      radius * 2, radius, 180, 180);
        }
        
        // Draw whorl pattern in center
        g2.drawOval(centerX - 8, centerY - 8, 16, 16);
        g2.drawOval(centerX - 4, centerY - 4, 8, 8);
    }
    
    private void drawQualityIndicator(Graphics2D g2, int x, int y, int width, int quality) {
        int barHeight = 6;
        
        // Background
        g2.setColor(new Color(224, 224, 224));
        g2.fillRoundRect(x, y, width, barHeight, 3, 3);
        
        // Quality bar
        Color qualityColor;
        if (quality >= 80) {
            qualityColor = new Color(76, 175, 80);
        } else if (quality >= 60) {
            qualityColor = new Color(255, 193, 7);
        } else {
            qualityColor = new Color(244, 67, 54);
        }
        
        g2.setColor(qualityColor);
        int fillWidth = (width * quality) / 100;
        g2.fillRoundRect(x, y, fillWidth, barHeight, 3, 3);
        
        // Draw quality text
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        g2.setColor(Color.DARK_GRAY);
        String qualityText = quality + "%";
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (width - fm.stringWidth(qualityText)) / 2;
        g2.drawString(qualityText, textX, y + barHeight + 10);
    }
    
    public void updateStatus() {
        if (fingerprint.isCaptured()) {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CAPTURED_COLOR, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
            setBackground(new Color(232, 245, 233));
            setToolTipText(fingerprint.getDisplayName() + " - Capturado (" + 
                          fingerprint.getQualityScore() + "%)");
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PENDING_COLOR, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
            setBackground(Color.WHITE);
            setToolTipText(fingerprint.getDisplayName() + " - Click para capturar");
        }
        repaint();
    }
    
    public Fingerprint getFingerprint() {
        return fingerprint;
    }
}