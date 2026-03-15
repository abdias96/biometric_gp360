package com.gp360.biometric.ui;

import com.gp360.biometric.model.BiometricData;
import com.gp360.biometric.model.Fingerprint;
import com.gp360.biometric.service.UareUCaptureService;
import com.gp360.biometric.api.ApiService;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public class MainWindow extends JFrame {
    private BiometricData biometricData;
    private UareUCaptureService captureService;
    private ApiService apiService;
    
    private JPanel fingerprintPanel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton saveButton;
    private JLabel capturedCountLabel;
    private JTextArea logArea;
    
    private Map<String, FingerprintButton> fingerprintButtons;
    
    public MainWindow() {
        initializeServices();
        initializeUI();
        loadParameters();
    }
    
    private void initializeServices() {
        biometricData = new BiometricData();

        // Initialize U.are.U SDK
        try {
            captureService = new UareUCaptureService(this);
            addLog("✓ Using DigitalPersona U.are.U SDK");
        } catch (Exception e) {
            addLog("✗ Error initializing U.are.U SDK: " + e.getMessage());
            addLog("Please ensure U.are.U SDK libraries are in lib/ directory");
        }

        apiService = new ApiService();
        apiService.setMainWindow(this); // Enable logging to UI
        fingerprintButtons = new java.util.HashMap<>();

        // Test API connection
        SwingUtilities.invokeLater(() -> {
            if (apiService.testConnection()) {
                addLog("✓ Conexión API establecida");
            } else {
                addLog("⚠ No se pudo conectar a la API - verifique el servidor Laravel");
                addLog("⚠ Asegúrese de que Laravel esté ejecutándose en http://localhost:8000");
                addLog("✓ Modo offline: Se guardará directamente en la base de datos");
            }
        });
    }
    
    private void loadParameters() {
        // Check for command line parameters or configuration
        String enrollableId = System.getProperty("enrollableId");
        String enrollableType = System.getProperty("enrollableType");

        if (enrollableId != null) {
            biometricData.setEnrollableId(Long.parseLong(enrollableId));
            boolean isVis = "visitor".equalsIgnoreCase(enrollableType);
            addLog("ID de " + (isVis ? "visitante" : "interno") + " cargado desde parámetros: " + enrollableId);
        } else {
            // Ask user for ID if not provided
            boolean isVis = "visitor".equalsIgnoreCase(enrollableType);
            String entityLabel = isVis ? "Visitante" : "Interno";
            SwingUtilities.invokeLater(() -> {
                String inputId = JOptionPane.showInputDialog(
                    this,
                    "Ingrese el ID del " + entityLabel + ":",
                    "ID de " + entityLabel + " Requerido",
                    JOptionPane.QUESTION_MESSAGE
                );

                if (inputId != null && !inputId.trim().isEmpty()) {
                    try {
                        Long id = Long.parseLong(inputId.trim());
                        biometricData.setEnrollableId(id);
                        updateTitle();
                        addLog("✓ ID de interno establecido: " + id);
                    } catch (NumberFormatException e) {
                        addLog("✗ ID inválido, debe ser un número");
                        JOptionPane.showMessageDialog(
                            this,
                            "ID inválido. Por favor ingrese un número.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } else {
                    addLog("⚠ No se proporcionó ID de interno");
                }
            });
        }

        if (enrollableType != null) {
            biometricData.setEnrollableType(enrollableType);
        } else {
            biometricData.setEnrollableType("inmate"); // Default
        }

        updateTitle();
    }
    
    private void updateTitle() {
        boolean isVisitor = "visitor".equalsIgnoreCase(biometricData.getEnrollableType());
        String title = isVisitor ? "GP360 Biométrico - Visitante" : "GP360 - Captura Biométrica";
        if (biometricData.getEnrollableId() != null) {
            title += " - ID: " + biometricData.getEnrollableId();
        }
        setTitle(title);
    }

    public boolean isVisitorMode() {
        return "visitor".equalsIgnoreCase(biometricData.getEnrollableType());
    }
    
    private void initializeUI() {
        setTitle("GP360 - Captura Biométrica");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        // Establecer look and feel moderno
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Usar look and feel por defecto
        }

        // Add window listener for cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });

        // Agregar tecla F11 para alternar pantalla completa
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("F11"), "toggleFullScreen");
        getRootPane().getActionMap().put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    setExtendedState(JFrame.NORMAL);
                } else {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // Create main panel con fondo degradado
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center panel with hands
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom panel with controls
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Right panel with log and statistics
        JPanel rightPanel = createLogPanel();
        mainPanel.add(rightPanel, BorderLayout.EAST);

        add(mainPanel);

        // Set size and center - Maximizado por defecto
        setMinimumSize(new Dimension(1400, 800));
        setResizable(true);

        // Maximizar la ventana automáticamente
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }
    
    private JPanel createHeaderPanel() {
        boolean isVisitor = isVisitorMode();
        Color headerBg = isVisitor ? new Color(21, 87, 36) : new Color(45, 45, 45);
        Color accentColor = isVisitor ? new Color(40, 167, 69) : new Color(0, 123, 255);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(headerBg);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, accentColor),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Title panel con icono
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setBackground(headerBg);

        String headerText = isVisitor
            ? "CAPTURA BIOMÉTRICA GP360 - MODO VISITANTE"
            : "SISTEMA DE CAPTURA BIOMÉTRICA GP360";
        JLabel titleLabel = new JLabel(headerText);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        panel.add(titlePanel, BorderLayout.WEST);

        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statusPanel.setBackground(headerBg);

        // Database status
        JLabel authLabel = new JLabel("[OK] Modo Local (BD Directa)", SwingConstants.RIGHT);
        authLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authLabel.setForeground(new Color(40, 167, 69));
        statusPanel.add(authLabel);

        // Device status
        JLabel deviceLabel = new JLabel(captureService != null ? "[OK] Lector Conectado" : "[!] Sin Lector", SwingConstants.RIGHT);
        deviceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        deviceLabel.setForeground(captureService != null ? new Color(40, 167, 69) : new Color(255, 193, 7));
        statusPanel.add(deviceLabel);

        capturedCountLabel = new JLabel("Capturadas: 0/10", SwingConstants.RIGHT);
        capturedCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        capturedCountLabel.setForeground(new Color(255, 255, 255));
        statusPanel.add(capturedCountLabel);

        statusLabel = new JLabel("LISTO", SwingConstants.RIGHT);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(40, 167, 69));
        statusPanel.add(statusLabel);

        panel.add(statusPanel, BorderLayout.EAST);

        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Panel principal para las manos
        JPanel handsPanel = new JPanel(new GridBagLayout());
        handsPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);  // Ajustado para mejor distribución

        // Título de mano izquierda
        JPanel leftTitlePanel = new JPanel();
        leftTitlePanel.setBackground(new Color(52, 152, 219));
        leftTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        JLabel leftLabel = new JLabel("MANO IZQUIERDA");
        leftLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        leftLabel.setForeground(Color.WHITE);
        leftTitlePanel.add(leftLabel);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        handsPanel.add(leftTitlePanel, gbc);

        // Título de mano derecha
        JPanel rightTitlePanel = new JPanel();
        rightTitlePanel.setBackground(new Color(46, 204, 113));
        rightTitlePanel.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        JLabel rightLabel = new JLabel("MANO DERECHA");
        rightLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        rightLabel.setForeground(Color.WHITE);
        rightTitlePanel.add(rightLabel);
        gbc.gridx = 5;
        gbc.gridwidth = 5;
        handsPanel.add(rightTitlePanel, gbc);

        // FILA SUPERIOR - Dedos principales (pulgares a medios)
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;

        // Mano izquierda - fila superior (pulgar, índice, medio)
        String[] leftTopFingers = {"left_thumb", "left_index", "left_middle"};
        String[] leftTopLabels = {"Pulgar", "Índice", "Medio"};
        for (int i = 0; i < leftTopFingers.length; i++) {
            gbc.gridx = i + 1; // Centrar un poco
            gbc.gridy = 1;
            JPanel fingerPanel = createFingerPanel(leftTopFingers[i], leftTopLabels[i], true);
            handsPanel.add(fingerPanel, gbc);
        }

        // Mano derecha - fila superior (pulgar, índice, medio)
        String[] rightTopFingers = {"right_thumb", "right_index", "right_middle"};
        String[] rightTopLabels = {"Pulgar", "Índice", "Medio"};
        for (int i = 0; i < rightTopFingers.length; i++) {
            gbc.gridx = i + 6;
            gbc.gridy = 1;
            JPanel fingerPanel = createFingerPanel(rightTopFingers[i], rightTopLabels[i], false);
            handsPanel.add(fingerPanel, gbc);
        }

        // Separador horizontal
        JSeparator horizontalSep = new JSeparator(JSeparator.HORIZONTAL);
        horizontalSep.setPreferredSize(new Dimension(1000, 2));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 10;
        gbc.insets = new Insets(15, 20, 15, 20);
        handsPanel.add(horizontalSep, gbc);

        // FILA INFERIOR - Dedos restantes (anular y meñique)
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 8, 10, 8);  // Mismo espaciado que arriba

        // Mano izquierda - fila inferior (anular, meñique)
        String[] leftBottomFingers = {"left_ring", "left_little"};
        String[] leftBottomLabels = {"Anular", "Meñique"};
        for (int i = 0; i < leftBottomFingers.length; i++) {
            gbc.gridx = i + 1; // Ajustar posición
            gbc.gridy = 3;
            JPanel fingerPanel = createFingerPanel(leftBottomFingers[i], leftBottomLabels[i], true);
            handsPanel.add(fingerPanel, gbc);
        }

        // Mano derecha - fila inferior (anular, meñique)
        String[] rightBottomFingers = {"right_ring", "right_little"};
        String[] rightBottomLabels = {"Anular", "Meñique"};
        for (int i = 0; i < rightBottomFingers.length; i++) {
            gbc.gridx = i + 6;
            gbc.gridy = 3;
            JPanel fingerPanel = createFingerPanel(rightBottomFingers[i], rightBottomLabels[i], false);
            handsPanel.add(fingerPanel, gbc);
        }

        mainPanel.add(handsPanel, BorderLayout.CENTER);

        // Panel inferior con barra de progreso y estadísticas
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Progress bar mejorado
        progressBar = new JProgressBar(0, 10);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(900, 35));
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setForeground(new Color(52, 152, 219));
        progressBar.setBackground(new Color(236, 240, 241));

        JPanel progressPanel = new JPanel();
        progressPanel.setBackground(Color.WHITE);
        progressPanel.add(new JLabel("Progreso Total:"));
        progressPanel.add(progressBar);

        bottomPanel.add(progressPanel, BorderLayout.CENTER);

        // Panel de estadísticas rápidas
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        statsPanel.setBackground(Color.WHITE);

        JLabel requiredLabel = new JLabel("[*] Requeridas: 4");
        requiredLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsPanel.add(requiredLabel);

        JLabel optionalLabel = new JLabel("[+] Opcionales: 6");
        optionalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsPanel.add(optionalLabel);

        JLabel qualityLabel = new JLabel("[Q] Calidad Promedio: --%");
        qualityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsPanel.add(qualityLabel);

        bottomPanel.add(statsPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createFingerPanel(String fingerId, String label, boolean isLeft) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(140, 180));  // Aumentado para mejor visualización

        // Botón de huella
        Fingerprint fp = biometricData.getFingerprint(fingerId);
        FingerprintButton button = new FingerprintButton(fp);
        button.setPreferredSize(new Dimension(120, 140));  // Aumentado para ver completa la huella
        button.addActionListener(e -> captureFingerprint(fp));
        fingerprintButtons.put(fingerId, button);

        // Etiqueta del dedo
        JLabel fingerLabel = new JLabel(label, SwingConstants.CENTER);
        fingerLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        fingerLabel.setForeground(new Color(52, 73, 94));

        // Indicador de calidad
        JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        qualityPanel.setBackground(Color.WHITE);
        JLabel qualityIcon = new JLabel("●");
        qualityIcon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        qualityIcon.setForeground(Color.GRAY);
        JLabel qualityText = new JLabel("--");
        qualityText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        qualityPanel.add(qualityIcon);
        qualityPanel.add(qualityText);

        panel.add(fingerLabel, BorderLayout.NORTH);
        panel.add(button, BorderLayout.CENTER);
        panel.add(qualityPanel, BorderLayout.SOUTH);

        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(15, 0, 10, 0)
        ));

        // Botón Limpiar
        JButton clearButton = new JButton("LIMPIAR TODO");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearButton.setPreferredSize(new Dimension(160, 45));
        clearButton.setBackground(new Color(108, 117, 125));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearAll());
        clearButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clearButton.setBackground(new Color(90, 98, 104));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearButton.setBackground(new Color(108, 117, 125));
            }
        });
        panel.add(clearButton);

        // Botón Verificar
        JButton verifyButton = new JButton("VERIFICAR CALIDAD");
        verifyButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyButton.setPreferredSize(new Dimension(170, 45));
        verifyButton.setBackground(new Color(255, 193, 7));
        verifyButton.setForeground(Color.BLACK);
        verifyButton.setFocusPainted(false);
        verifyButton.setBorderPainted(false);
        verifyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        verifyButton.addActionListener(e -> verifyQuality());
        verifyButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                verifyButton.setBackground(new Color(255, 176, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                verifyButton.setBackground(new Color(255, 193, 7));
            }
        });
        panel.add(verifyButton);

        // Botón Guardar
        saveButton = new JButton("GUARDAR EN BASE DE DATOS");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        saveButton.setPreferredSize(new Dimension(220, 45));
        saveButton.setBackground(new Color(40, 167, 69));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveToDatabase());
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (saveButton.isEnabled()) {
                    saveButton.setBackground(new Color(33, 136, 56));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (saveButton.isEnabled()) {
                    saveButton.setBackground(new Color(40, 167, 69));
                }
            }
        });
        panel.add(saveButton);

        // Botón Cancelar
        JButton cancelButton = new JButton("CANCELAR");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(140, 45));
        cancelButton.setBackground(new Color(220, 53, 69));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> handleClose());
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(200, 35, 51));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelButton.setBackground(new Color(220, 53, 69));
            }
        });
        panel.add(cancelButton);

        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setPreferredSize(new Dimension(350, 0));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Panel de título con contador
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(52, 58, 64));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel titleLabel = new JLabel("REGISTRO DE ACTIVIDAD");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        JLabel timeLabel = new JLabel(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(Color.WHITE);
        titlePanel.add(timeLabel, BorderLayout.EAST);

        // Actualizar hora cada segundo
        Timer timer = new Timer(1000, e -> {
            timeLabel.setText(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
        });
        timer.start();

        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Área de log mejorada
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(33, 37, 41));
        logArea.setForeground(new Color(248, 249, 250));
        logArea.setCaretColor(Color.WHITE);
        logArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(52, 58, 64)));
        scrollPane.getVerticalScrollBar().setBackground(new Color(52, 58, 64));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(108, 117, 125);
                this.trackColor = new Color(52, 58, 64);
            }
        });

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        statsPanel.setBackground(new Color(248, 249, 250));
        statsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            "Estadísticas",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(52, 58, 64)
        ));

        JLabel capturedStat = new JLabel("Capturadas: 0/10");
        capturedStat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(capturedStat);

        JLabel qualityStat = new JLabel("Calidad Prom: --");
        qualityStat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(qualityStat);

        JLabel timeStat = new JLabel("Tiempo: 00:00");
        timeStat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(timeStat);

        JLabel statusStat = new JLabel("Estado: Esperando");
        statusStat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(statusStat);

        mainPanel.add(statsPanel, BorderLayout.SOUTH);

        return mainPanel;
    }
    
    private void captureFingerprint(Fingerprint fingerprint) {
        addLog("Iniciando captura: " + fingerprint.getDisplayName());
        statusLabel.setText("Capturando...");
        statusLabel.setForeground(Color.ORANGE);
        
        // Open capture dialog
        if (captureService == null) {
            JOptionPane.showMessageDialog(this,
                "No se pudo inicializar el servicio de captura biométrica.\n" +
                "Por favor verifique que el SDK de DigitalPersona esté instalado.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        CaptureDialog dialog = new CaptureDialog(this, fingerprint, captureService);
        dialog.setVisible(true);
        
        if (dialog.isCaptured()) {
            updateProgress();
            addLog("✓ Captura exitosa: " + fingerprint.getDisplayName() + 
                   " (Calidad: " + fingerprint.getQualityScore() + "%)");
        } else {
            addLog("✗ Captura cancelada: " + fingerprint.getDisplayName());
        }
        
        statusLabel.setText("Listo");
        statusLabel.setForeground(new Color(0, 150, 0));
    }
    
    public void updateProgress() {
        int captured = biometricData.getCapturedCount();
        progressBar.setValue(captured);
        progressBar.setString(captured + " de 10 huellas capturadas (" + (captured * 10) + "%)");

        // Update header label with better formatting
        capturedCountLabel.setText("Capturadas: " + captured + "/10");

        // Update fingerprint buttons
        for (Map.Entry<String, FingerprintButton> entry : fingerprintButtons.entrySet()) {
            entry.getValue().updateStatus();
        }

        // Enable save button when at least 1 fingerprint is captured
        // (Changed from 4 required fingers to be more flexible)
        boolean canSave = captured >= 1;
        saveButton.setEnabled(canSave);

        if (biometricData.isComplete()) {
            statusLabel.setText("¡COMPLETO!");
            statusLabel.setForeground(new Color(40, 167, 69));
            addLog("✓ Todas las huellas han sido capturadas exitosamente");
        } else if (captured >= 4) {
            statusLabel.setText("CANTIDAD RECOMENDADA");
            statusLabel.setForeground(new Color(40, 167, 69));
        } else if (captured >= 1) {
            statusLabel.setText("MÍNIMO ALCANZADO");
            statusLabel.setForeground(new Color(255, 193, 7));
        } else {
            statusLabel.setText("CAPTURANDO...");
            statusLabel.setForeground(new Color(52, 152, 219));
        }
    }
    
    private void clearAll() {
        int result = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar todas las huellas capturadas?",
            "Confirmar limpieza",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            biometricData = new BiometricData();
            loadParameters();
            updateProgress();
            addLog("✗ Todas las huellas han sido eliminadas");
        }
    }
    
    private void verifyQuality() {
        if (biometricData.getCapturedCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay huellas capturadas para verificar",
                "Sin datos",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Reporte de Calidad de Huellas:\n\n");
        
        int totalQuality = 0;
        int count = 0;
        
        for (Fingerprint fp : biometricData.getFingerprints().values()) {
            if (fp.isCaptured()) {
                report.append(String.format("%-20s: %d%%\n", 
                    fp.getDisplayName(), fp.getQualityScore()));
                totalQuality += fp.getQualityScore();
                count++;
            }
        }
        
        if (count > 0) {
            report.append("\nCalidad promedio: ").append(totalQuality / count).append("%");
        }
        
        JOptionPane.showMessageDialog(this, report.toString(), 
            "Reporte de Calidad", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void saveToDatabase() {
        // Verificar mínimo requerido (al menos 1 huella capturada)
        if (biometricData.getCapturedCount() < 1) {
            JOptionPane.showMessageDialog(this,
                "Debe capturar al menos 1 huella dactilar.\n\n" +
                "Nota: Se recomienda capturar 4 o más huellas para mejor identificación.",
                "Captura incompleta",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Advertencia si hay menos de 4 huellas (recomendación)
        if (biometricData.getCapturedCount() < 4) {
            int response = JOptionPane.showConfirmDialog(this,
                "Solo se han capturado " + biometricData.getCapturedCount() + " huella(s).\n\n" +
                "Se recomienda capturar al menos 4 huellas para mejor identificación.\n\n" +
                "¿Desea continuar guardando con esta cantidad de huellas?",
                "Advertencia - Pocas Huellas",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        statusLabel.setText("Guardando...");
        statusLabel.setForeground(Color.BLUE);
        saveButton.setEnabled(false);
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Try direct database save first (for local development)
                // Falls back to API if database is not accessible
                boolean success = apiService.saveDirectToDatabase(biometricData);
                if (!success) {
                    addLog("Direct database save failed, trying API...");
                    success = apiService.saveBiometricData(biometricData);
                }
                return success;
            }
            
            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        addLog("✓ Datos biométricos guardados exitosamente");
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "Los datos biométricos se han guardado correctamente",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Close after successful save
                        dispose();
                    } else {
                        addLog("✗ Error al guardar los datos biométricos");
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "Error al guardar los datos biométricos",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        saveButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    addLog("✗ Error: " + e.getMessage());
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "Error de conexión: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    saveButton.setEnabled(true);
                }
                
                statusLabel.setText("Listo");
                statusLabel.setForeground(new Color(0, 150, 0));
            }
        };
        
        worker.execute();
    }
    
    private void handleClose() {
        if (biometricData.getCapturedCount() > 0 && !biometricData.isComplete()) {
            int result = JOptionPane.showConfirmDialog(this,
                "Hay huellas capturadas sin guardar. ¿Desea salir sin guardar?",
                "Confirmar salida",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        // Cleanup
        if (captureService != null) {
            captureService.cleanup();
        }
        
        dispose();
    }
    
    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public BiometricData getBiometricData() {
        return biometricData;
    }
}