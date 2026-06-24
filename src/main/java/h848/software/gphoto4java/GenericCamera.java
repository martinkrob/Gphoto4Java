package h848.software.gphoto4java;

import h848.software.gphoto4java.core.CameraProperty;
import h848.software.gphoto4java.core.CommandResult;
import h848.software.gphoto4java.core.Gphoto2Executor;
import h848.software.gphoto4java.exceptions.DeviceBusyException;
import h848.software.gphoto4java.exceptions.Gphoto2Exception;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/**
 * Univerzální třída pro ovládání libovolného fotoaparátu přes gphoto2.
 * Nahrazuje specifickou Camera_D70 a umožňuje dynamickou práci s konfigurací.
 */
public class GenericCamera {

    private final List<CameraEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Gphoto2Executor executor;
    private Thread eventThread;

    public GenericCamera() {
        this.executor = new Gphoto2Executor();
    }

    // --- ROZHRANÍ PRO SLEDOVÁNÍ UDÁLOSTÍ ---
    public interface CameraEventListener {
        void onEventReceived(String eventData);
    }

    public void addCameraEventListener(CameraEventListener listener) {
        listeners.add(listener);
    }

    public void removeCameraEventListener(CameraEventListener listener) {
        listeners.remove(listener);
    }

    // --- LOGIKA KOMUNIKACE ---
    private void checkErrors(CommandResult result) {
        if (!result.isSuccess()) {
            String err = result.getErrorOutput().toLowerCase();
            
            if (err.contains("device or resource busy") || err.contains("could not claim the usb device")) {
                throw new DeviceBusyException("Zařízení je obsazeno systémem (gvfs).");
            }
            
            throw new Gphoto2Exception("Chyba komunikace gphoto2: " + result.getExitCode() + "\n" + result.getErrorOutput());
        }
    }

    public boolean isConnected() {
        CommandResult result = executor.execute("--auto-detect");
        return result.getStandardOutput().split("\\r?\\n").length > 2;
    }

    public CameraProperty getProperty(String configName) {
        CommandResult result = executor.execute("--get-config", configName);
        if (!result.isSuccess()) return null;

        CameraProperty prop = new CameraProperty();
        prop.setName(configName);

        String[] lines = result.getStandardOutput().split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Label:")) prop.setLabel(line.substring(6).trim());
            else if (line.startsWith("Type:")) prop.setType(line.substring(5).trim());
            else if (line.startsWith("Readonly:")) prop.setReadOnly("1".equals(line.substring(9).trim()));
            else if (line.startsWith("Current:")) prop.setCurrentValue(line.substring(8).trim());
            else if (line.startsWith("Choice:")) {
                String choiceStr = line.substring(7).trim();
                int spaceIndex = choiceStr.indexOf(' ');
                prop.getChoices().add(spaceIndex != -1 ? choiceStr.substring(spaceIndex + 1).trim() : choiceStr);
            }
        }
        
        return prop;
    }

    public boolean setProperty(String configName, String value) {
        CommandResult result = executor.execute("--set-config", configName + "=" + value);
        try {
            checkErrors(result);
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    // --- ASYNCHRONNÍ NASLOUCHÁNÍ ---
    public void startEventListening() {
        if (eventThread != null && eventThread.isAlive()) return;

        eventThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CommandResult result = executor.execute("--wait-event=500ms");
                    if (result.isSuccess()) {
                        String output = result.getStandardOutput();
                        if (output != null && !output.isEmpty()) {
                            for (String line : output.split("\\r?\\n")) {
                                if (!line.trim().isEmpty() && !line.toLowerCase().contains("timeout")) {
                                    final String eventData = line.trim();
                                    SwingUtilities.invokeLater(() -> {
                                        for (CameraEventListener l : listeners) l.onEventReceived(eventData);
                                    });
                                }
                            }
                        }
                    }
                    
                    Thread.sleep(100);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        eventThread.setDaemon(true);
        eventThread.start();
    }

    public void stopEventListening() {
        if (eventThread != null) eventThread.interrupt();
    }

    // --- ZÁKLADNÍ AKCE ---
    public void takePhoto() {
        checkErrors(executor.execute("--capture-image"));
    }
    
    public File takePhotoAndDownload() {
        CommandResult result = executor.execute("--capture-image-and-download");
        checkErrors(result);
        return new File("last_captured_file"); // Zde by se dalo vylepšit parsování jména z outputu
    }
}
