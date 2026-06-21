package h848.software.gphoto4java.exceptions;

/**
 * Base exception for all errors returned from gphoto2.
 */
public class Gphoto2Exception extends RuntimeException {
    
    public Gphoto2Exception(String message) {
        super(message);
    }

    public Gphoto2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
