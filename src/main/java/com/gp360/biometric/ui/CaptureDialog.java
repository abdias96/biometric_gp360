package com.gp360.biometric.ui;

import com.gp360.biometric.model.Fingerprint;
import com.gp360.biometric.service.UareUCaptureService;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public class CaptureDialog extends JDialog {
    private Fingerprint fingerprint;
    private UareUCaptureService captureService;
    private MainWindow parent;
    
    private JLabel imageLabel;
    private JLabel statusLabel;
    private JProgressBar qualityBar;
    private JButton captureButton;
    private JButton acceptButton;
    private JButton retryButton;
    private JButton cancelButton;
    
    private boolean captured = false;
    private BufferedImage currentImage;
    private SwingWorker<Void, String> currentWorker;
    
    public CaptureDialog(MainWindow parent, Fingerprint fingerprint, UareUCaptureService captureService) {
        super(parent, "Captura de Huella - " + fingerprint.getDisplayName(), true);
        this.parent = parent;
        this.fingerprint = fingerprint;
        this.captureService = captureService;
        
        initializeUI();
        setLocationRelativeTo(parent);
        
        // Add window closing handler
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleCancel();
            }
        });
        
        // Start capture automatically
        startCapture();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 600);
        
        // Title Panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(240, 240, 240));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel(fingerprint.getDisplayName());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titlePanel.add(titleLabel);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Center Panel - Fingerprint Image
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        imageLabel.setPreferredSize(new Dimension(256, 350));
        
        // Show placeholder initially
        showPlaceholder();
        
        centerPanel.add(imageLabel, BorderLayout.CENTER);
        
        // Quality Panel
        JPanel qualityPanel = new JPanel(new BorderLayout(5, 5));
        qualityPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel qualityLabel = new JLabel("Calidad:");
        qualityBar = new JProgressBar(0, 100);
        qualityBar.setStringPainted(true);
        qualityBar.setValue(0);
        
        qualityPanel.add(qualityLabel, BorderLayout.WEST);
        qualityPanel.add(qualityBar, BorderLayout.CENTER);
        
        centerPanel.add(qualityPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        
        // Status Panel
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Esperando...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusPanel.add(statusLabel);
        
        add(statusPanel, BorderLayout.SOUTH);
        
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        captureButton = new JButton("Capturar");
        captureButton.addActionListener(e -> startCapture());
        captureButton.setEnabled(false); // Will auto-start
        
        retryButton = new JButton("Reintentar");
        retryButton.addActionListener(e -> retry());
        retryButton.setEnabled(false);
        
        acceptButton = new JButton("Aceptar");
        acceptButton.addActionListener(e -> accept());
        acceptButton.setEnabled(false);
        
        cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> handleCancel());
        
        buttonPanel.add(captureButton);
        buttonPanel.add(retryButton);
        buttonPanel.add(acceptButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void showPlaceholder() {
        BufferedImage placeholder = new BufferedImage(256, 350, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = placeholder.createGraphics();
        
        // Background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 256, 350);
        
        // Draw fingerprint outline
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(80, 50, 96, 140);
        
        // Draw ridges
        g2.setStroke(new BasicStroke(1));
        for (int i = 0; i < 8; i++) {
            int y = 60 + i * 15;
            g2.drawArc(60, y, 80, 30, 0, 180);
            g2.drawArc(116, y, 80, 30, 0, 180);
        }
        
        // Text
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.drawString("Coloque el dedo en el lector", 45, 230);
        g2.drawString("4 escaneos requeridos", 65, 250);
        
        g2.dispose();
        
        imageLabel.setIcon(new ImageIcon(placeholder));
    }
    
    private void startCapture() {
        // Cancel any existing capture
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            if (captureService != null) {
                captureService.stopCapture();
            }
        }
        
        statusLabel.setText("Iniciando captura...");
        statusLabel.setForeground(Color.ORANGE);
        captureButton.setEnabled(false);
        retryButton.setEnabled(false);
        acceptButton.setEnabled(false);
        qualityBar.setValue(0);
        
        parent.addLog("Iniciando captura: " + fingerprint.getDisplayName());
        
        // Start capture in background thread
        currentWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    publish("Inicializando lector...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    
                    if (captureService != null && captureService.isReaderAvailable()) {
                        // Real capture with U.are.U SDK
                        captureService.startCapture(fingerprint);
                        
                        // Wait for enrollment to complete (4 scans)
                        // The enrollment process handles its own UI updates
                        int waitTime = 0;
                        byte[] lastImageData = null;
                        while (!fingerprint.isCaptured() && waitTime < 30000 && !isCancelled()) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // Thread was interrupted, probably because dialog was closed
                                Thread.currentThread().interrupt(); // Restore interrupted status
                                publish("Captura cancelada");
                                break;
                            }
                            waitTime += 500;

                            // Check if we have new image data
                            byte[] currentImageData = fingerprint.getImageData();
                            if (currentImageData != null && currentImageData != lastImageData) {
                                updateImage(currentImageData);
                                lastImageData = currentImageData;
                                publish("Image updated");
                            }
                        }
                        
                        if (fingerprint.isCaptured()) {
                            publish("Lector listo. Coloque el dedo...");
                            // Update with final captured data
                            updateQuality(fingerprint.getQualityScore());
                            if (fingerprint.getImageData() != null) {
                                updateImage(fingerprint.getImageData());
                            }
                        } else if (waitTime >= 30000) {
                            publish("Tiempo de espera agotado");
                        }
                    } else {
                        // Fallback to simulation if no reader
                        publish("No hay lector disponible - Modo simulación");
                        simulateCapture();
                    }
                } catch (Exception e) {
                    publish("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    statusLabel.setText(message);
                    parent.addLog(message);
                }
            }
            
            @Override
            protected void done() {
                if (!isCancelled()) {
                    captureComplete();
                }
            }
        };
        
        currentWorker.execute();
    }
    
    private void updateImage(byte[] imageData) {
        if (imageData != null && imageData.length > 0) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
                if (img != null) {
                    currentImage = img;
                    // Scale image to fit
                    Image scaled = img.getScaledInstance(256, 350, Image.SCALE_SMOOTH);
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(new ImageIcon(scaled));
                        imageLabel.revalidate();
                        imageLabel.repaint();
                        parent.addLog("Fingerprint image displayed (size: " + imageData.length + " bytes)");
                    });
                }
            } catch (Exception e) {
                parent.addLog("Error mostrando imagen: " + e.getMessage());
            }
        }
    }
    
    private void updateQuality(int quality) {
        SwingUtilities.invokeLater(() -> {
            qualityBar.setValue(quality);
            if (quality >= 80) {
                qualityBar.setForeground(Color.GREEN);
            } else if (quality >= 60) {
                qualityBar.setForeground(Color.ORANGE);
            } else {
                qualityBar.setForeground(Color.RED);
            }
        });
    }
    
    private void simulateCapture() throws InterruptedException {
        try {
            Thread.sleep(2000); // Simulate capture time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e; // Re-throw to let caller handle it
        }
        
        // Generate simulated template data
        byte[] templateData = new byte[512];
        for (int i = 0; i < templateData.length; i++) {
            templateData[i] = (byte)(Math.random() * 256);
        }
        
        // Generate simulated image
        BufferedImage capturedImage = new BufferedImage(256, 350, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = capturedImage.createGraphics();
        
        // Draw simulated fingerprint
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 256, 350);
        
        g2.setColor(new Color(100, 100, 100));
        for (int i = 0; i < 15; i++) {
            int y = 30 + i * 20;
            for (int j = 0; j < 3; j++) {
                int x = 80 + j * 30;
                g2.drawArc(x, y, 40, 20, 0, 180);
            }
        }
        
        g2.dispose();
        
        currentImage = capturedImage;
        
        // Update fingerprint data
        fingerprint.setTemplateData(templateData);
        fingerprint.setQualityScore(75 + (int)(Math.random() * 25));
        fingerprint.setCaptured(true);
        
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(new ImageIcon(currentImage));
            updateQuality(fingerprint.getQualityScore());
        });
    }
    
    private void captureComplete() {
        if (fingerprint.isCaptured()) {
            statusLabel.setText("Captura completada");
            statusLabel.setForeground(Color.GREEN);
            acceptButton.setEnabled(true);
            retryButton.setEnabled(true);
            parent.addLog("Huella capturada: " + fingerprint.getDisplayName() + 
                         " (Calidad: " + fingerprint.getQualityScore() + "%)");
            captured = true;
        } else {
            statusLabel.setText("Error en la captura");
            statusLabel.setForeground(Color.RED);
            captureButton.setEnabled(true);
            retryButton.setEnabled(true);
            parent.addLog("Error al capturar: " + fingerprint.getDisplayName());
        }
        cancelButton.setEnabled(true);
    }
    
    private void retry() {
        fingerprint.setCaptured(false);
        fingerprint.setTemplateData(null);
        fingerprint.setQualityScore(0);
        currentImage = null;
        captured = false;
        showPlaceholder();
        qualityBar.setValue(0);
        startCapture();
    }
    
    private void accept() {
        if (fingerprint.isCaptured()) {
            parent.addLog("✓ Captura exitosa: " + fingerprint.getDisplayName() + 
                         " (Calidad: " + fingerprint.getQualityScore() + "%)");
            dispose();
        }
    }
    
    private void handleCancel() {
        // Stop any ongoing capture
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
        if (captureService != null) {
            captureService.stopCapture();
        }
        
        fingerprint.setCaptured(false);
        captured = false;
        parent.addLog("✗ Captura cancelada: " + fingerprint.getDisplayName());
        dispose();
    }
    
    public boolean isCaptured() {
        return captured;
    }
}