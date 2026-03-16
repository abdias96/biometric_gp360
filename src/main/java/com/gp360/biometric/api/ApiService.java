package com.gp360.biometric.api;

import com.digitalpersona.uareu.*;
import com.gp360.biometric.model.BiometricData;
import com.gp360.biometric.model.Fingerprint;
import com.gp360.biometric.service.WSQConverter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class ApiService {
    private static String API_BASE_URL = "http://localhost:8000/api";
    private String authToken = "";
    private java.util.Properties config;
    private WSQConverter wsqConverter;

    public ApiService() {
        loadConfig();
        wsqConverter = new WSQConverter();

        // Check if session ID is provided (new secure method)
        // Priority: System Properties > Config File
        String sessionId = System.getProperty("api.session", config.getProperty("api.session", ""));
        if (!sessionId.isEmpty()) {
            log("Intercambiando session ID por token de autenticación...");
            this.authToken = exchangeSessionForToken(sessionId);
            if (this.authToken != null && !this.authToken.isEmpty()) {
                log("✓ Token de autenticación obtenido exitosamente");
            } else {
                log("✗ No se pudo obtener el token de autenticación");
            }
        } else {
            // Fallback to direct token (legacy method)
            this.authToken = System.getProperty("api.token", config.getProperty("api.token", ""));
            if (!this.authToken.isEmpty()) {
                log("Usando token de autenticación directo");
            }
        }

        // API URL: Config file takes precedence over System Properties (protocol URL)
        // This allows the client to override the server-provided URL in production environments
        String configApiUrl = config.getProperty("api.url", "");
        if (!configApiUrl.isEmpty()) {
            API_BASE_URL = configApiUrl;
            log("API Base URL desde config.properties: " + API_BASE_URL);
        } else {
            API_BASE_URL = System.getProperty("api.url", "http://localhost:8000/api");
            log("API Base URL desde protocolo: " + API_BASE_URL);
        }
    }

    private void loadConfig() {
        config = new java.util.Properties();
        try {
            // Try to load from external file first
            java.io.File configFile = new java.io.File("config.properties");
            if (configFile.exists()) {
                config.load(new java.io.FileInputStream(configFile));
            } else {
                // Try to load from resources
                java.io.InputStream is = getClass().getResourceAsStream("/config/application.properties");
                if (is != null) {
                    config.load(is);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }

    /**
     * Exchange a session ID for the actual authentication token
     * This provides better security by not passing the token in the protocol URL
     */
    private String exchangeSessionForToken(String sessionId) {
        try {
            // Config file takes precedence over protocol URL
            // This allows clients to override the server-provided URL
            String configApiUrl = config.getProperty("api.url", "");
            String apiUrl;
            if (!configApiUrl.isEmpty()) {
                apiUrl = configApiUrl;
                log("Usando API URL de config.properties: " + apiUrl);
            } else {
                apiUrl = System.getProperty("api.url", "http://localhost:8000/api");
                log("Usando API URL del protocolo: " + apiUrl);
            }

            // Build URL with session parameter
            URL url = new URL(apiUrl + "/biometric/session?session=" + sessionId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(10000); // 10 seconds
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line);
                        }

                        // Parse JSON response
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject data = jsonResponse.getJSONObject("data");

                            // Extract and set enrollable_id from session data if available
                            if (data.has("enrollable_id")) {
                                String enrollId = data.optString("enrollable_id");
                                if (enrollId != null && !enrollId.isEmpty()) {
                                    System.setProperty("enrollableId", enrollId);
                                    System.out.println("✓ Set enrollableId from session: " + enrollId);
                                }
                            } else if (data.has("inmate_id")) {
                                String inmateId = data.optString("inmate_id");
                                if (inmateId != null && !inmateId.isEmpty()) {
                                    System.setProperty("enrollableId", inmateId);
                                    System.out.println("✓ Set enrollableId from session: " + inmateId);
                                }
                            }

                            // Extract and set enrollable_type from session data
                            if (data.has("enrollable_type")) {
                                String enrollType = data.optString("enrollable_type", "inmate");
                                System.setProperty("enrollableType", enrollType);
                                System.out.println("✓ Set enrollableType from session: " + enrollType);
                            }

                            // Extract and set user_id from session data for audit tracking
                            // Note: user_id comes as integer from PHP/JSON, so we use opt() to handle both types
                            if (data.has("user_id") && !data.isNull("user_id")) {
                                // Handle both integer and string values
                                Object userIdObj = data.opt("user_id");
                                String userId = String.valueOf(userIdObj);
                                if (userId != null && !userId.isEmpty() && !userId.equals("null")) {
                                    System.setProperty("capturedByUserId", userId);
                                    System.out.println("✓ Set capturedByUserId from session: " + userId);
                                } else {
                                    System.err.println("⚠ user_id is null or empty in session data");
                                }
                            } else {
                                System.err.println("⚠ user_id not found in session data");
                            }

                            // Return the API token
                            return data.optString("api_token", "");
                        } else {
                            System.err.println("Session exchange failed: " + jsonResponse.optString("message"));
                            return "";
                        }
                    }
                } else {
                    // Read error response
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        System.err.println("Session exchange error (HTTP " + responseCode + "): " + errorResponse.toString());
                    }
                    return "";
                }

            } finally {
                conn.disconnect();
            }

        } catch (Exception e) {
            System.err.println("Error exchanging session for token: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    public boolean saveBiometricData(BiometricData biometricData) {
        try {
            boolean allSuccess = true;
            
            // Save each fingerprint directly to inmate_biometric_data table
            for (Map.Entry<String, Fingerprint> entry : biometricData.getFingerprints().entrySet()) {
                Fingerprint fp = entry.getValue();
                if (fp.isCaptured()) {
                    boolean success = saveFingerprintToInmateBiometric(biometricData.getEnrollableId(), fp);
                    if (!success) {
                        allSuccess = false;
                        System.err.println("Failed to save fingerprint: " + fp.getPosition());
                    }
                }
            }
            
            return allSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean saveFingerprintToInmateBiometric(Long enrollableId, Fingerprint fingerprint) throws IOException {
        // Use the public BiometricService endpoint instead of authenticated endpoint
        URL url = new URL(API_BASE_URL + "/biometric-service/save-fingerprint");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            // Use X-Service-Token header for BiometricService authentication
            conn.setRequestProperty("X-Service-Token", authToken);
            conn.setDoOutput(true);
            
            // Map finger position to match database enum (uses underscore instead of _little)
            String fingerType = fingerprint.getPosition();
            if (fingerType.endsWith("_little")) {
                fingerType = fingerType.replace("_little", "_pinky");
            }

            // Validate template before saving
            byte[] templateData = fingerprint.getTemplateData();
            if (templateData == null || templateData.length < 26) {
                log("Warning: Invalid template for " + fingerType + " (null or too small), skipping");
                return false;
            }

            // Create request body for BiometricService endpoint
            JSONObject requestBody = new JSONObject();
            // Determine enrollable type from BiometricData
            String enrollableType = System.getProperty("enrollableType", "inmate");
            requestBody.put("enrollable_id", enrollableId);
            requestBody.put("enrollable_type", enrollableType);
            requestBody.put("finger_type", fingerType);
            requestBody.put("fingerprint_template", Base64.getEncoder().encodeToString(templateData));

            // Add image data if available
            if (fingerprint.getImageData() != null && fingerprint.getImageData().length > 0) {
                requestBody.put("fingerprint_image", Base64.getEncoder().encodeToString(fingerprint.getImageData()));
            }

            // Add quality score
            requestBody.put("fingerprint_quality", fingerprint.getQualityScore());

            // New fields: minutiae_count, iso_template, nist_quality_score, template_size
            requestBody.put("minutiae_count", fingerprint.getMinutiaeCount());
            requestBody.put("template_size", templateData.length);
            requestBody.put("nist_quality_score", convertQualityToNIST(fingerprint.getQualityScore()));

            // ISO template (real conversion from SDK)
            String isoTemplate = convertToISO19794(fingerprint);
            if (isoTemplate != null) {
                requestBody.put("iso_template", isoTemplate);
            }

            // Add captured_by (user_id from session)
            String capturedByStr = System.getProperty("capturedByUserId", "1");
            int capturedBy = 1;
            try {
                capturedBy = Integer.parseInt(capturedByStr);
            } catch (NumberFormatException e) {
                System.err.println("⚠ Invalid capturedByUserId: " + capturedByStr + ", using default: 1");
            }
            System.out.println("[API] Usuario capturador (captured_by): " + capturedBy);
            requestBody.put("captured_by", capturedBy);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = conn.getResponseCode();
            
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                // Read error response for debugging
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.err.println("API Error: " + errorResponse.toString());
                }
            }
            
            return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED;
            
        } finally {
            conn.disconnect();
        }
    }
    
    // Update methods not needed - BiometricService endpoint handles updates automatically
    // The controller uses updateOrCreate() which will update existing records
    
    public boolean testConnection() {
        try {
            // Test connection to Laravel server using a simple endpoint
            URL url = new URL(API_BASE_URL.replace("/api", ""));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            try {
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                // Accept any 2xx or 3xx status code as success (Laravel may redirect)
                return responseCode >= 200 && responseCode < 400;

            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private com.gp360.biometric.ui.MainWindow mainWindow;

    public void setMainWindow(com.gp360.biometric.ui.MainWindow window) {
        this.mainWindow = window;
    }

    private void log(String message) {
        System.out.println(message);
        if (mainWindow != null) {
            mainWindow.addLog(message);
        }
    }

    // Alternative method to save directly to database using JDBC
    public boolean saveDirectToDatabase(BiometricData biometricData) {
        // Priority: System Properties > Config File
        String dbUrl = System.getProperty("db.url", config.getProperty("db.url", "jdbc:mysql://127.0.0.1:3306/siapen?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
        String dbUser = System.getProperty("db.user", config.getProperty("db.user", config.getProperty("db.username", "root")));
        String dbPassword = System.getProperty("db.password", config.getProperty("db.password", ""));

        log("[BD] Intentando guardar directamente en base de datos...");
        log("[BD] URL: " + dbUrl);
        log("[BD] Usuario: " + dbUser);
        log("[BD] Interno ID: " + biometricData.getEnrollableId());
        log("[BD] Huellas a guardar: " + biometricData.getFingerprints().size());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            log("[BD] ✓ Driver JDBC cargado");

            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                log("[BD] ✓ Conexión a base de datos establecida");
                conn.setAutoCommit(false);

                // Determine enrollable type
                String enrollableType = biometricData.getEnrollableType();
                boolean isVisitor = "visitor".equalsIgnoreCase(enrollableType);
                String morphType = isVisitor ? "App\\Models\\VisitorRegistry" : "App\\Models\\Inmate\\Inmate";
                String validateTable = isVisitor ? "visitor_registry" : "inmates";

                // Validate that entity exists before attempting to save
                String validateSql = "SELECT id FROM " + validateTable + " WHERE id = ?";
                boolean entityExists = false;
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(validateSql)) {
                    pstmt.setLong(1, biometricData.getEnrollableId());
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        entityExists = rs.next();
                    }
                }

                if (!entityExists) {
                    String entityLabel = isVisitor ? "visitante" : "interno";
                    log("[BD] ✗ ERROR: El " + entityLabel + " con ID " + biometricData.getEnrollableId() + " NO EXISTE en la base de datos");
                    log("[BD] Por favor verifique que el ID sea correcto");
                    return false;
                }

                log("[BD] ✓ " + (isVisitor ? "Visitante" : "Interno") + " validado, procediendo con el guardado...");

                // First, deactivate existing fingerprints in unified table
                String deactivateUnifiedSql = "UPDATE biometric_data SET is_active = 0 WHERE enrollable_id = ? AND enrollable_type = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(deactivateUnifiedSql)) {
                    pstmt.setLong(1, biometricData.getEnrollableId());
                    pstmt.setString(2, morphType);
                    int updated = pstmt.executeUpdate();
                    log("[BD] Desactivados " + updated + " registros en biometric_data");
                }

                // Note: inmate_biometric_data is now a VIEW pointing to biometric_data
                // No separate deactivation needed

                // === biometric_data TABLE (renamed from inmate_biometric_data) ===
                String unifiedSql = "INSERT INTO biometric_data " +
                           "(inmate_id, enrollable_id, enrollable_type, finger_type, fingerprint_template, iso_template, wsq_image, " +
                           "fingerprint_quality, nist_quality_score, minutiae_count, wsq_compression_ratio, wsq_bitrate, " +
                           "fingerprint_image, image_width, image_height, image_dpi, " +
                           "template_size, sdk_version, enrollment_samples, " +
                           "capture_date, capture_device, is_active, captured_by, " +
                           "template_format, iso_compliant, fbi_compliant, nist_compliant, " +
                           "created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), 'DigitalPersona U.are.U 4500', 1, ?, 'ANSI_378_2004', TRUE, ?, TRUE, NOW(), NOW()) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "fingerprint_template = VALUES(fingerprint_template), " +
                           "iso_template = VALUES(iso_template), " +
                           "wsq_image = VALUES(wsq_image), " +
                           "fingerprint_quality = VALUES(fingerprint_quality), " +
                           "nist_quality_score = VALUES(nist_quality_score), " +
                           "minutiae_count = VALUES(minutiae_count), " +
                           "wsq_compression_ratio = VALUES(wsq_compression_ratio), " +
                           "wsq_bitrate = VALUES(wsq_bitrate), " +
                           "fingerprint_image = VALUES(fingerprint_image), " +
                           "image_width = VALUES(image_width), " +
                           "image_height = VALUES(image_height), " +
                           "image_dpi = VALUES(image_dpi), " +
                           "template_size = VALUES(template_size), " +
                           "sdk_version = VALUES(sdk_version), " +
                           "enrollment_samples = VALUES(enrollment_samples), " +
                           "capture_date = VALUES(capture_date), " +
                           "capture_device = VALUES(capture_device), " +
                           "is_active = 1, " +
                           "iso_compliant = VALUES(iso_compliant), " +
                           "fbi_compliant = VALUES(fbi_compliant), " +
                           "nist_compliant = VALUES(nist_compliant), " +
                           "updated_at = NOW()";

                try (java.sql.PreparedStatement unifiedPstmt = conn.prepareStatement(unifiedSql)) {
                    int unifiedCount = 0;
                    for (Fingerprint fp : biometricData.getFingerprints().values()) {
                        if (fp.isCaptured()) {
                            String fingerType = fp.getPosition();
                            if (fingerType.endsWith("_little")) {
                                fingerType = fingerType.replace("_little", "_pinky");
                            }
                            byte[] templateData = fp.getTemplateData();
                            if (templateData == null || templateData.length < 26) continue;

                            int idx = 1;
                            // inmate_id: set for inmates, null for visitors
                            if (!isVisitor) {
                                unifiedPstmt.setLong(idx++, biometricData.getEnrollableId()); // inmate_id
                            } else {
                                unifiedPstmt.setNull(idx++, java.sql.Types.BIGINT); // inmate_id = null for visitors
                            }
                            unifiedPstmt.setLong(idx++, biometricData.getEnrollableId()); // enrollable_id
                            unifiedPstmt.setString(idx++, morphType); // enrollable_type
                            unifiedPstmt.setString(idx++, fingerType); // finger_type
                            unifiedPstmt.setString(idx++, Base64.getEncoder().encodeToString(templateData)); // template
                            String isoTemplate = convertToISO19794(fp);
                            unifiedPstmt.setString(idx++, isoTemplate); // iso_template

                            // WSQ
                            Double compressionRatio = null;
                            String wsqData = null;
                            if (fp.getImageData() != null && fp.getImageData().length > 0) {
                                wsqData = convertToWSQ(fp);
                                if (wsqData != null) {
                                    unifiedPstmt.setString(idx++, wsqData);
                                    byte[] wsqBytes = Base64.getDecoder().decode(wsqData);
                                    compressionRatio = (double) fp.getImageData().length / wsqBytes.length;
                                } else {
                                    unifiedPstmt.setNull(idx++, java.sql.Types.LONGVARCHAR);
                                }
                            } else {
                                unifiedPstmt.setNull(idx++, java.sql.Types.LONGVARCHAR);
                            }

                            unifiedPstmt.setInt(idx++, fp.getQualityScore()); // quality
                            unifiedPstmt.setString(idx++, convertQualityToNIST(fp.getQualityScore())); // nist
                            unifiedPstmt.setInt(idx++, fp.getMinutiaeCount()); // minutiae

                            if (compressionRatio != null) unifiedPstmt.setDouble(idx++, compressionRatio);
                            else unifiedPstmt.setNull(idx++, java.sql.Types.DECIMAL);

                            if (wsqData != null) unifiedPstmt.setInt(idx++, 750);
                            else unifiedPstmt.setNull(idx++, java.sql.Types.INTEGER);

                            // Image
                            if (fp.getImageData() != null && fp.getImageData().length > 0) {
                                unifiedPstmt.setString(idx++, Base64.getEncoder().encodeToString(fp.getImageData()));
                                try {
                                    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(fp.getImageData());
                                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(bais);
                                    if (img != null) { unifiedPstmt.setInt(idx++, img.getWidth()); unifiedPstmt.setInt(idx++, img.getHeight()); }
                                    else { unifiedPstmt.setInt(idx++, 320); unifiedPstmt.setInt(idx++, 400); }
                                } catch (Exception e) { unifiedPstmt.setInt(idx++, 320); unifiedPstmt.setInt(idx++, 400); }
                            } else {
                                unifiedPstmt.setNull(idx++, java.sql.Types.LONGVARCHAR);
                                unifiedPstmt.setNull(idx++, java.sql.Types.INTEGER);
                                unifiedPstmt.setNull(idx++, java.sql.Types.INTEGER);
                            }

                            unifiedPstmt.setInt(idx++, 500); // dpi
                            unifiedPstmt.setInt(idx++, templateData.length); // template_size
                            unifiedPstmt.setString(idx++, "DigitalPersona U.are.U SDK 2.2.2"); // sdk_version
                            unifiedPstmt.setInt(idx++, 4); // enrollment_samples

                            // captured_by
                            String capturedByStr = System.getProperty("capturedByUserId",
                                config.getProperty("user.id", config.getProperty("captured_by", "1")));
                            int capturedByUserId = 1;
                            try { capturedByUserId = Integer.parseInt(capturedByStr); } catch (NumberFormatException e) { }
                            unifiedPstmt.setInt(idx++, capturedByUserId);
                            unifiedPstmt.setBoolean(idx++, wsqData != null); // fbi_compliant

                            unifiedPstmt.addBatch();
                            unifiedCount++;
                        }
                    }
                    log("[BD] Ejecutando batch unificado de " + unifiedCount + " huellas en biometric_data...");
                    unifiedPstmt.executeBatch();
                    log("[BD] ✓ Batch unificado ejecutado");
                }

                // For visitors, update has_biometric_data flag
                if (isVisitor) {
                    String updateVisitor = "UPDATE visitor_registry SET has_biometric_data = 1, biometric_enrollment_date = NOW(), updated_at = NOW() WHERE id = ?";
                    try (java.sql.PreparedStatement pstmt = conn.prepareStatement(updateVisitor)) {
                        pstmt.setLong(1, biometricData.getEnrollableId());
                        pstmt.executeUpdate();
                        log("[BD] ✓ Visitante marcado con biometría");
                    }
                }

                // biometric_data IS the renamed inmate_biometric_data table
                // No separate legacy insert needed
                conn.commit();
                log("[BD] ✓ Commit exitoso");
                return true;
            }
        } catch (ClassNotFoundException e) {
            log("[BD] ✗ ERROR: No se encontró el driver MySQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (java.sql.SQLException e) {
            log("[BD] ✗ ERROR SQL: " + e.getMessage());
            log("[BD] SQLState: " + e.getSQLState());
            log("[BD] Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            log("[BD] ✗ ERROR: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convert ANSI 378-2004 template to ISO 19794-2:2005 format
     * Uses the DigitalPersona SDK native ImportFmd() for real format conversion
     */
    private String convertToISO19794(Fingerprint fp) {
        try {
            // Use pre-converted ISO template if available (from UareUCaptureService)
            byte[] isoData = fp.getIsoTemplateData();
            if (isoData != null && isoData.length > 0) {
                return Base64.getEncoder().encodeToString(isoData);
            }

            // Fallback: convert on the fly using SDK
            byte[] templateData = fp.getTemplateData();
            if (templateData == null || templateData.length == 0) return null;

            Fmd isoFmd = UareUGlobal.GetImporter().ImportFmd(
                templateData,
                Fmd.Format.ANSI_378_2004,
                Fmd.Format.ISO_19794_2_2005
            );

            return Base64.getEncoder().encodeToString(isoFmd.getData());
        } catch (UareUException e) {
            log("Warning: ISO 19794-2 conversion failed: " + e.getMessage());
            return null;
        } catch (Exception e) {
            log("Warning: ISO 19794-2 conversion error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert quality score (0-100) to NIST quality scale (1-5)
     * NIST scale:
     * 1 = Excellent (80-100)
     * 2 = Very Good (60-79)
     * 3 = Good (40-59)
     * 4 = Fair (20-39)
     * 5 = Poor (0-19)
     */
    private String convertQualityToNIST(int qualityScore) {
        if (qualityScore >= 80) return "1";  // Excellent
        if (qualityScore >= 60) return "2";  // Very Good
        if (qualityScore >= 40) return "3";  // Good
        if (qualityScore >= 20) return "4";  // Fair
        return "5";  // Poor
    }

    /**
     * Convert fingerprint image to WSQ format (FBI standard)
     * Uses WSQConverter which wraps NIST NBIS toolkit
     */
    private String convertToWSQ(Fingerprint fp) {
        try {
            if (!wsqConverter.isAvailable()) {
                // Only log once to avoid spam
                if (fp.getPosition().equals("right_thumb")) {
                    log("⚠ WSQ conversion unavailable - NBIS not installed");
                    log("  " + wsqConverter.getInstallationInstructions().split("\n")[0]);
                }
                return null;
            }

            byte[] imageData = fp.getImageData();
            if (imageData == null || imageData.length == 0) {
                return null;
            }

            // Convert to WSQ using standard FBI parameters
            // DPI: 500 (standard for fingerprints)
            // Bitrate: 0.75 (FBI standard)
            return wsqConverter.convertToWSQ(imageData, 500, 0.75);

        } catch (Exception e) {
            log("⚠ Error converting to WSQ: " + e.getMessage());
            return null;
        }
    }
}