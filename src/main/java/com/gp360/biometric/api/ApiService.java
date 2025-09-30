package com.gp360.biometric.api;

import com.gp360.biometric.model.BiometricData;
import com.gp360.biometric.model.Fingerprint;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class ApiService {
    private static final String API_BASE_URL = System.getProperty("api.url", "http://localhost:8000/api");
    private String authToken;
    
    public ApiService() {
        // Get auth token from system properties or config
        this.authToken = System.getProperty("api.token", "");
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
    
    private boolean saveFingerprintToInmateBiometric(Long inmateId, Fingerprint fingerprint) throws IOException {
        URL url = new URL(API_BASE_URL + "/inmate-biometric-data");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (!authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            conn.setDoOutput(true);
            
            // Map finger position to match database enum (uses underscore instead of _little)
            String fingerType = fingerprint.getPosition();
            if (fingerType.endsWith("_little")) {
                fingerType = fingerType.replace("_little", "_pinky");
            }
            
            // Create request body for inmate_biometric_data with ANSI 378-2004 metadata
            JSONObject requestBody = new JSONObject();
            requestBody.put("inmate_id", inmateId);
            requestBody.put("finger_type", fingerType);
            requestBody.put("fingerprint_template", Base64.getEncoder().encodeToString(fingerprint.getTemplateData()));
            
            // Add image data if available
            if (fingerprint.getImageData() != null && fingerprint.getImageData().length > 0) {
                requestBody.put("fingerprint_image", Base64.getEncoder().encodeToString(fingerprint.getImageData()));
            }
            
            // Template format and metadata
            requestBody.put("template_format", "ANSI_378_2004");
            requestBody.put("template_version", "2004");
            requestBody.put("template_size", fingerprint.getTemplateData().length);
            
            // Extract minutiae count from template if possible (placeholder - would need actual parsing)
            // For now, estimate based on template size (typical ANSI template has ~15-30 minutiae)
            int estimatedMinutiae = Math.min(30, Math.max(12, fingerprint.getTemplateData().length / 26));
            requestBody.put("minutiae_count", estimatedMinutiae);
            
            // SDK and algorithm information
            requestBody.put("algorithm_used", "DigitalPersona U.are.U");
            requestBody.put("sdk_version", "U.are.U SDK 3.3.0");
            
            // Image dimensions (if we have image data)
            if (fingerprint.getImageData() != null) {
                // These would be actual values from the captured image
                requestBody.put("image_width", 256);
                requestBody.put("image_height", 360);
                requestBody.put("image_dpi", 500); // DigitalPersona 4500 is 500 DPI
            }
            
            // Quality and enrollment information
            requestBody.put("fingerprint_quality", fingerprint.getQualityScore());
            requestBody.put("nist_compliant", true); // ANSI 378-2004 is NIST compliant
            requestBody.put("iso_compliant", false); // Would need conversion for ISO
            requestBody.put("capture_date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            requestBody.put("capture_device", "DigitalPersona 4500");
            requestBody.put("enrollment_samples", 4); // We use 4-scan enrollment
            requestBody.put("is_consolidated", true); // Template is consolidated from 4 scans
            requestBody.put("match_threshold", 10000); // DigitalPersona default (0.01% FMR)
            requestBody.put("is_active", true);
            
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
    
    // Method to check if fingerprint already exists and update it
    private boolean updateExistingFingerprint(Long inmateId, Fingerprint fingerprint) throws IOException {
        // First check if fingerprint exists
        String fingerType = fingerprint.getPosition();
        if (fingerType.endsWith("_little")) {
            fingerType = fingerType.replace("_little", "_pinky");
        }
        
        URL url = new URL(API_BASE_URL + "/inmate-biometric-data?inmate_id=" + inmateId + "&finger_type=" + fingerType);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (!authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Parse response to get existing record ID
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("data") && jsonResponse.getJSONArray("data").length() > 0) {
                    // Update existing record
                    JSONObject existingRecord = jsonResponse.getJSONArray("data").getJSONObject(0);
                    int recordId = existingRecord.getInt("id");
                    return updateBiometricRecord(recordId, fingerprint);
                }
            }
        } finally {
            conn.disconnect();
        }
        
        return false;
    }
    
    private boolean updateBiometricRecord(int recordId, Fingerprint fingerprint) throws IOException {
        URL url = new URL(API_BASE_URL + "/inmate-biometric-data/" + recordId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (!authToken.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            conn.setDoOutput(true);
            
            // Update request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("fingerprint_template", Base64.getEncoder().encodeToString(fingerprint.getTemplateData()));
            requestBody.put("fingerprint_quality", fingerprint.getQualityScore());
            requestBody.put("capture_date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            requestBody.put("is_active", true);
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
            
        } finally {
            conn.disconnect();
        }
    }
    
    public boolean testConnection() {
        try {
            URL url = new URL(API_BASE_URL + "/inmate-biometric-data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            try {
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (!authToken.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + authToken);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
                
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    // Alternative method to save directly to database using JDBC
    public boolean saveDirectToDatabase(BiometricData biometricData) {
        String dbUrl = System.getProperty("db.url", "jdbc:mysql://localhost:3306/siapen");
        String dbUser = System.getProperty("db.user", "root");
        String dbPassword = System.getProperty("db.password", "");
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                conn.setAutoCommit(false);
                
                // First, deactivate existing fingerprints for this inmate
                String deactivateSql = "UPDATE inmate_biometric_data SET is_active = 0 WHERE inmate_id = ?";
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(deactivateSql)) {
                    pstmt.setLong(1, biometricData.getEnrollableId());
                    pstmt.executeUpdate();
                }
                
                // Insert into inmate_biometric_data table
                String sql = "INSERT INTO inmate_biometric_data " +
                           "(inmate_id, finger_type, fingerprint_template, fingerprint_quality, " +
                           "capture_date, capture_device, is_active, captured_by, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, CURDATE(), 'DigitalPersona 4500', 1, ?, NOW(), NOW()) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "fingerprint_template = VALUES(fingerprint_template), " +
                           "fingerprint_quality = VALUES(fingerprint_quality), " +
                           "capture_date = VALUES(capture_date), " +
                           "is_active = 1, " +
                           "updated_at = NOW()";
                
                try (java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (Fingerprint fp : biometricData.getFingerprints().values()) {
                        if (fp.isCaptured()) {
                            // Map finger position to database enum
                            String fingerType = fp.getPosition();
                            if (fingerType.endsWith("_little")) {
                                fingerType = fingerType.replace("_little", "_pinky");
                            }
                            
                            pstmt.setLong(1, biometricData.getEnrollableId());
                            pstmt.setString(2, fingerType);
                            pstmt.setBytes(3, fp.getTemplateData());
                            pstmt.setString(4, String.valueOf(fp.getQualityScore()));
                            pstmt.setInt(5, 1); // Default user ID for captured_by
                            
                            pstmt.addBatch();
                        }
                    }
                    
                    pstmt.executeBatch();
                    conn.commit();
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}