package h848.software.gphoto4java;

import h848.software.gphoto4java.core.CameraProperty;
import h848.software.gphoto4java.core.StandardProperty;

/**
 * Main application class to demonstrate the Gphoto4Java wrapper.
 */
public class Gphoto4Java {

    public static void main(String[] args) {
        System.out.println("Starting Gphoto4Java with standardization layer...");
        GenericCamera camera = new GenericCamera();

        // 1. Register listeners
        camera.addConnectionListener(connected -> {
            if (connected) {
                System.out.println("\n=======================================================");
                System.out.println("  CAMERA CONNECTED AND READY FOR FULL CONTROL");
                System.out.println("=======================================================");

                // 1. Retrieve basic information
                CameraProperty modelProp = camera.getStandardProperty(StandardProperty.CAMERA_NAME);
                String modelName = (modelProp != null) ? modelProp.getCurrentValue() : "Unknown camera";

                CameraProperty lensProp = camera.getStandardProperty(StandardProperty.LENS_NAME);
                String lensName = (lensProp != null) ? lensProp.getCurrentValue() : "Unknown lens";
                
                CameraProperty modeProp = camera.getStandardProperty(StandardProperty.EXPOSURE_PROGRAM);
                String currentMode = (modeProp != null) ? modeProp.getCurrentValue() : "Unknown";

                System.out.println("\n[HARDWARE INFO]");
                System.out.println("Model: " + modelName);
                System.out.println("Mounted lens: " + lensName);
                System.out.println("Physical mode dial: " + currentMode);

                System.out.println("\n[EXPOSURE PARAMETERS STATUS AND CHOICES]");
                
                for (StandardProperty propEnum : StandardProperty.values()) {
                    // Skip items already printed in hardware info
                    if (propEnum == StandardProperty.CAMERA_NAME || 
                        propEnum == StandardProperty.LENS_NAME || 
                        propEnum == StandardProperty.EXPOSURE_PROGRAM) {
                        continue;
                    }

                    CameraProperty prop = camera.getStandardProperty(propEnum);
                    if (prop != null) {
                        System.out.println("\n-- " + prop.getLabel() + " (" + propEnum.name() + ") --");
                        System.out.println("Current: " + prop.getCurrentValue());
                        System.out.println("Available choices (" + prop.getChoices().size() + "): " + prop.getChoices());
                    } else {
                        System.out.println("\n-- " + propEnum.name() + " --");
                        System.out.println("Value is not available for this camera/mode.");
                    }
                }

                System.out.println("\n=======================================================");
                System.out.println("For testing: try to physically switch mode on camera (e.g. from P to M)");
                System.out.println("or zoom the lens, then RUN the application AGAIN.");
            } else {
                System.out.println("\n[SYSTEM] Camera DISCONNECTED / POWERED OFF!");
            }
        });

        // 2. Keep application running for connection testing
        System.out.println("Starting basic connection monitoring (waiting for camera)...");
        try {
            for (int i = 300; i > 0; i--) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Test interrupted.");
        }

        camera.stopEventListening();
        System.out.println("\nTest finished.");
    }
}
