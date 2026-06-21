package h848.software.gphoto4java.exceptions;

/**
 * Specific exception thrown when the camera is 
 * locked by an OS file manager (e.g., gvfs-volume-monitor).
 */
public class DeviceBusyException extends Gphoto2Exception {
    
    public DeviceBusyException(String message) {
        super(message);
    }
}
