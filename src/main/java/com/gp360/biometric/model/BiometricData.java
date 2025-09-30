package com.gp360.biometric.model;

import java.util.HashMap;
import java.util.Map;

public class BiometricData {
    private Long enrollableId;
    private String enrollableType;
    private Map<String, Fingerprint> fingerprints;
    
    public BiometricData() {
        initializeFingerprints();
    }
    
    private void initializeFingerprints() {
        fingerprints = new HashMap<>();
        
        // Right hand - Note: We use _little internally but save as _pinky to database
        fingerprints.put("right_thumb", new Fingerprint("right_thumb", "Pulgar Derecho"));
        fingerprints.put("right_index", new Fingerprint("right_index", "Índice Derecho"));
        fingerprints.put("right_middle", new Fingerprint("right_middle", "Medio Derecho"));
        fingerprints.put("right_ring", new Fingerprint("right_ring", "Anular Derecho"));
        fingerprints.put("right_little", new Fingerprint("right_little", "Meñique Derecho")); // Saved as right_pinky
        
        // Left hand - Note: We use _little internally but save as _pinky to database
        fingerprints.put("left_thumb", new Fingerprint("left_thumb", "Pulgar Izquierdo"));
        fingerprints.put("left_index", new Fingerprint("left_index", "Índice Izquierdo"));
        fingerprints.put("left_middle", new Fingerprint("left_middle", "Medio Izquierdo"));
        fingerprints.put("left_ring", new Fingerprint("left_ring", "Anular Izquierdo"));
        fingerprints.put("left_little", new Fingerprint("left_little", "Meñique Izquierdo")); // Saved as left_pinky
    }
    
    public int getCapturedCount() {
        return (int) fingerprints.values().stream()
            .filter(Fingerprint::isCaptured)
            .count();
    }
    
    public boolean isComplete() {
        // Only require the 4 main fingers: both thumbs and index fingers
        return fingerprints.get("right_thumb").isCaptured() &&
               fingerprints.get("right_index").isCaptured() &&
               fingerprints.get("left_thumb").isCaptured() &&
               fingerprints.get("left_index").isCaptured();
    }
    
    public boolean hasMinimumRequired() {
        // Alternative check for minimum required fingers
        return isComplete();
    }
    
    // Getters and setters
    public Long getEnrollableId() { return enrollableId; }
    public void setEnrollableId(Long enrollableId) { this.enrollableId = enrollableId; }
    
    public String getEnrollableType() { return enrollableType; }
    public void setEnrollableType(String enrollableType) { this.enrollableType = enrollableType; }
    
    public Map<String, Fingerprint> getFingerprints() { return fingerprints; }
    
    public Fingerprint getFingerprint(String position) {
        return fingerprints.get(position);
    }
}