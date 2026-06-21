package h848.software.gphoto4java;

import java.io.File;

/**
 * Main application class to demonstrate the Gphoto4Java wrapper.
 */
public class Gphoto4Java {

    public static void main(String[] args) {
        System.out.println("Initializing Gphoto4Java Library...");
        
        Camera_D70 camera = new Camera_D70();

        try {
            System.out.println("Checking for connected cameras...");
            if (camera.isConnected()) {
                System.out.println("Camera detected successfully!");
                
                // Read a config value, e.g., battery level
                String battery = camera.getBatteryLevel();
                System.out.println("Battery Level: " + (battery != null ? battery : "Unknown"));

                // Take a photo
                System.out.println("Taking a photo...");
                File photo = camera.takePhotoAndDownload();
                
                if (photo != null) {
                    System.out.println("Photo successfully downloaded to: " + photo.getAbsolutePath());
                } else {
                    System.out.println("Photo taken, but failed to parse the exact filename.");
                }
            } else {
                System.out.println("No camera detected. Please connect your camera and try again.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
