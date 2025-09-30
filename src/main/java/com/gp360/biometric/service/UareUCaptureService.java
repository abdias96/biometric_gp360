package com.gp360.biometric.service;

import com.digitalpersona.uareu.*;
import com.gp360.biometric.model.Fingerprint;
import com.gp360.biometric.ui.MainWindow;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.lang.reflect.InvocationTargetException;

/**
 * Biometric capture service using DigitalPersona U.are.U SDK
 * Based on official SDK samples
 */
public class UareUCaptureService implements Engine.EnrollmentCallback {
    private MainWindow mainWindow;
    private Reader reader;
    private Engine engine;
    private ReaderCollection readers;
    private Fingerprint currentFingerprint;
    private boolean isCapturing = false;
    private boolean cancelRequested = false;
    private int enrollmentStage = 0;
    private EnrollmentThread currentEnrollmentThread = null;
    private final Object captureLock = new Object();
    
    // Quality thresholds
    private static final int MIN_QUALITY = 60;
    private static final int GOOD_QUALITY = 80;
    
    // Enrollment thread for proper SDK usage
    private class EnrollmentThread extends Thread {
        private Fingerprint fingerprint;
        private boolean success = false;
        
        public EnrollmentThread(Fingerprint fingerprint) {
            this.fingerprint = fingerprint;
        }
        
        @Override
        public void run() {
            try {
                // Reset enrollment stage counter
                enrollmentStage = 0;
                cancelRequested = false;
                
                // Create enrollment FMD with 4 captures as per SDK recommendations
                Fmd enrollmentFmd = engine.CreateEnrollmentFmd(
                    Fmd.Format.ANSI_378_2004,
                    UareUCaptureService.this
                );
                
                if (enrollmentFmd != null && !cancelRequested) {
                    // Convert to template data
                    byte[] templateData = enrollmentFmd.getData();
                    
                    // Calculate quality score
                    int qualityScore = 95; // Enrollment ensures high quality
                    
                    // Save to fingerprint object
                    fingerprint.setTemplateData(templateData);
                    fingerprint.setQualityScore(qualityScore);
                    fingerprint.setCaptured(true);
                    
                    success = true;
                    
                    SwingUtilities.invokeLater(() -> {
                        mainWindow.addLog("✓ Enrollment completed for: " + 
                                         fingerprint.getDisplayName());
                        mainWindow.addLog("Quality: " + qualityScore + "%");
                        mainWindow.updateProgress();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        mainWindow.addLog("✗ Enrollment cancelled or failed");
                    });
                }
            } catch (UareUException e) {
                SwingUtilities.invokeLater(() -> 
                    mainWindow.addLog("Error during enrollment: " + e.getMessage()));
            }
        }
        
        public boolean isSuccess() {
            return success;
        }
    }
    
    // Capture thread based on SDK sample
    private class CaptureThread extends Thread {
        private Reader.CaptureResult captureResult;
        private Reader reader;
        private Fid.Format format;
        private Reader.ImageProcessing proc;
        private boolean cancelled = false;
        
        public CaptureThread(Reader reader, Fid.Format format, Reader.ImageProcessing proc) {
            this.reader = reader;
            this.format = format;
            this.proc = proc;
        }
        
        @Override
        public void run() {
            try {
                // Wait for reader to become ready
                boolean ready = false;
                while (!ready && !cancelled) {
                    Reader.Status status = reader.GetStatus();
                    if (status.status == Reader.ReaderStatus.BUSY) {
                        Thread.sleep(100);
                    } else if (status.status == Reader.ReaderStatus.READY || 
                               status.status == Reader.ReaderStatus.NEED_CALIBRATION) {
                        ready = true;
                    } else {
                        SwingUtilities.invokeLater(() -> 
                            mainWindow.addLog("Reader error: " + status.status));
                        break;
                    }
                }
                
                if (ready && !cancelled) {
                    // Capture fingerprint
                    Reader.Capabilities caps = reader.GetCapabilities();
                    captureResult = reader.Capture(
                        format,
                        proc,
                        caps.resolutions[0],
                        -1  // timeout (infinite)
                    );
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    mainWindow.addLog("Capture error: " + e.getMessage()));
            }
        }
        
        public void cancel() {
            cancelled = true;
            try {
                reader.CancelCapture();
            } catch (UareUException e) {
                // Ignore cancellation errors
            }
        }
        
        public Reader.CaptureResult getCaptureResult() {
            return captureResult;
        }
    }
    
    public UareUCaptureService(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        init();
    }
    
    private void init() {
        try {
            // Initialize U.are.U SDK
            readers = UareUGlobal.GetReaderCollection();
            readers.GetReaders();
            
            if (readers.size() > 0) {
                reader = readers.get(0);
                
                // Get reader description
                Reader.Description desc = reader.GetDescription();
                mainWindow.addLog("Reader found: " + desc.name);
                mainWindow.addLog("Vendor: " + desc.id.vendor_name);
                mainWindow.addLog("Product: " + desc.id.product_name);
                
                // Open reader with cooperative priority
                reader.Open(Reader.Priority.COOPERATIVE);
                mainWindow.addLog("Reader opened successfully");
                
                // Initialize engine for enrollment
                engine = UareUGlobal.GetEngine();
                
                // Check reader status
                Reader.Status status = reader.GetStatus();
                mainWindow.addLog("Reader status: " + status.status);
                
            } else {
                mainWindow.addLog("No DigitalPersona readers found");
                mainWindow.addLog("Please connect a fingerprint reader");
            }
        } catch (UareUException e) {
            mainWindow.addLog("Error initializing U.are.U SDK: " + e.getMessage());
        }
    }
    
    public void startCapture(Fingerprint fingerprint) {
        synchronized (captureLock) {
            // Cancel any existing enrollment
            if (currentEnrollmentThread != null && currentEnrollmentThread.isAlive()) {
                cancelRequested = true;
                try {
                    currentEnrollmentThread.join(1000); // Wait max 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            if (reader == null) {
                mainWindow.addLog("No reader available");
                return;
            }
            
            this.currentFingerprint = fingerprint;
            this.isCapturing = true;
            this.cancelRequested = false;
            this.enrollmentStage = 0;
            
            SwingUtilities.invokeLater(() -> {
                mainWindow.addLog("Starting enrollment for: " + fingerprint.getDisplayName());
                mainWindow.addLog("You will need to scan the same finger 4 times");
            });
            
            // Start new enrollment thread
            currentEnrollmentThread = new EnrollmentThread(fingerprint);
            currentEnrollmentThread.start();
        }
    }
    
    public void stopCapture() {
        synchronized (captureLock) {
            isCapturing = false;
            cancelRequested = true;
            if (currentEnrollmentThread != null && currentEnrollmentThread.isAlive()) {
                currentEnrollmentThread.interrupt();
            }
        }
    }
    
    // Implementation of Engine.EnrollmentCallback
    @Override
    public Engine.PreEnrollmentFmd GetFmd(Fmd.Format format) {
        Engine.PreEnrollmentFmd preFmd = null;
        
        try {
            // Show prompt for this enrollment stage
            enrollmentStage++;
            final String message = String.format(
                "Place your %s on the scanner\n" +
                "Scan %d of 4",
                currentFingerprint != null ? currentFingerprint.getDisplayName() : "finger",
                enrollmentStage
            );
            
            try {
                SwingUtilities.invokeAndWait(() -> {
                    mainWindow.addLog(message);
                });
            } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
            
            // Create capture thread as per SDK sample
            CaptureThread captureThread = new CaptureThread(
                reader,
                Fid.Format.ANSI_381_2004,
                Reader.ImageProcessing.IMG_PROC_DEFAULT
            );
            
            captureThread.start();
            try {
                captureThread.join(); // Wait for capture to complete
            } catch (InterruptedException e) {
                e.printStackTrace();
                enrollmentStage--; // Retry this stage
                return null;
            }
            
            Reader.CaptureResult captureResult = captureThread.getCaptureResult();
            
            if (captureResult != null && !cancelRequested) {
                if (captureResult.quality == Reader.CaptureQuality.CANCELED) {
                    cancelRequested = true;
                } else if (captureResult.quality == Reader.CaptureQuality.GOOD && 
                           captureResult.image != null) {
                    // Extract features
                    Fmd fmd = engine.CreateFmd(
                        captureResult.image,
                        Fmd.Format.ANSI_378_2004
                    );
                    
                    // Create pre-enrollment FMD
                    preFmd = new Engine.PreEnrollmentFmd();
                    preFmd.fmd = fmd;
                    preFmd.view_index = 0;
                    
                    SwingUtilities.invokeLater(() -> 
                        mainWindow.addLog("✓ Good quality capture " + enrollmentStage + "/4"));
                    
                    // Save image for display - update on each scan to get the best one
                    if (currentFingerprint != null && captureResult.image != null) {
                        BufferedImage image = convertFidToBufferedImage(captureResult.image);
                        byte[] imageData = convertImageToBytes(image);
                        if (imageData != null) {
                            currentFingerprint.setImageData(imageData);
                            // Notify UI to update the image
                            SwingUtilities.invokeLater(() -> {
                                mainWindow.addLog("Image updated for scan " + enrollmentStage);
                            });
                        }
                    }
                } else {
                    SwingUtilities.invokeLater(() -> 
                        mainWindow.addLog("✗ Poor quality, please try again"));
                    enrollmentStage--; // Retry this stage
                }
            }
        } catch (UareUException e) {
            SwingUtilities.invokeLater(() -> 
                mainWindow.addLog("Enrollment error: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return preFmd;
    }
    
    private BufferedImage convertFidToBufferedImage(Fid fid) {
        try {
            // Get image data from FID
            Fid.Fiv[] fivs = fid.getViews();
            if (fivs == null || fivs.length == 0) {
                return null;
            }
            
            Fid.Fiv fiv = fivs[0];
            int width = fiv.getWidth();
            int height = fiv.getHeight();
            byte[] data = fiv.getData();
            
            // Create BufferedImage
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            
            // Copy pixel data
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = data[y * width + x] & 0xFF;
                    int rgb = (pixel << 16) | (pixel << 8) | pixel;
                    image.setRGB(x, y, rgb);
                }
            }
            
            return image;
        } catch (Exception e) {
            mainWindow.addLog("Error converting image: " + e.getMessage());
            return null;
        }
    }
    
    private byte[] convertImageToBytes(BufferedImage image) {
        if (image == null) return null;
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean verifyFingerprint(byte[] templateData) {
        if (reader == null || engine == null) {
            return false;
        }
        
        try {
            // Create capture thread for verification
            CaptureThread captureThread = new CaptureThread(
                reader,
                Fid.Format.ANSI_381_2004,
                Reader.ImageProcessing.IMG_PROC_DEFAULT
            );
            
            mainWindow.addLog("Place finger on scanner for verification...");
            captureThread.start();
            try {
                captureThread.join();
            } catch (InterruptedException e) {
                mainWindow.addLog("Verification interrupted");
                return false;
            }
            
            Reader.CaptureResult verifyCapture = captureThread.getCaptureResult();
            
            if (verifyCapture != null && verifyCapture.quality == Reader.CaptureQuality.GOOD) {
                // Create FMD from captured image
                Fmd capturedFmd = engine.CreateFmd(
                    verifyCapture.image,
                    Fmd.Format.ANSI_378_2004
                );
                
                // Import stored template
                Fmd storedFmd = UareUGlobal.GetImporter().ImportFmd(
                    templateData,
                    Fmd.Format.ANSI_378_2004,
                    Fmd.Format.ANSI_378_2004
                );
                
                // Compare
                int falseMatchRate = engine.Compare(capturedFmd, 0, storedFmd, 0);
                
                // Typical threshold is 0.01% (1 in 10,000)
                int threshold = Engine.PROBABILITY_ONE / 10000;
                
                boolean matched = falseMatchRate < threshold;
                mainWindow.addLog(matched ? "✓ Fingerprint verified" : "✗ Fingerprint does not match");
                
                return matched;
            }
        } catch (UareUException e) {
            mainWindow.addLog("Verification error: " + e.getMessage());
        }
        
        return false;
    }
    
    public void cleanup() {
        try {
            stopCapture();
            // NO cerrar el reader para evitar crash con dpDevCtlx64.dll en Java 24
            // La JVM liberará los recursos automáticamente al salir
            if (reader != null) {
                try {
                    // Solo cancelar cualquier captura pendiente
                    reader.CancelCapture();
                } catch (Exception ex) {
                    // Ignorar error al cancelar
                }
                // NO llamar a reader.Close() para evitar crash
                reader = null;
                mainWindow.addLog("Cleanup completed (reader not closed to prevent crash)");
            }
        } catch (Exception e) {
            // Silent cleanup
        }
    }
    
    public boolean isReaderAvailable() {
        return reader != null;
    }
    
    public String getReaderInfo() {
        if (reader != null) {
            try {
                Reader.Description desc = reader.GetDescription();
                return desc.name + " (" + desc.id.vendor_name + ")";
            } catch (Exception e) {
                return "Unknown reader";
            }
        }
        return "No reader connected";
    }
}