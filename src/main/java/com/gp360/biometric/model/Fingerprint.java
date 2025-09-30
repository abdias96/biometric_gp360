package com.gp360.biometric.model;

public class Fingerprint {
    private String position;
    private String displayName;
    private byte[] templateData;
    private byte[] imageData;
    private int qualityScore;
    private boolean captured;
    
    public Fingerprint(String position, String displayName) {
        this.position = position;
        this.displayName = displayName;
        this.captured = false;
        this.qualityScore = 0;
    }
    
    // Getters and setters
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public byte[] getTemplateData() { return templateData; }
    public void setTemplateData(byte[] templateData) { 
        this.templateData = templateData;
        this.captured = templateData != null;
    }
    
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    
    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
    
    public boolean isCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }
}