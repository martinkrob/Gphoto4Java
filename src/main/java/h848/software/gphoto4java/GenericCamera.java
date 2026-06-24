package h848.software.gphoto4java;

import h848.software.gphoto4java.core.CameraProperty;
import h848.software.gphoto4java.core.CommandResult;
import h848.software.gphoto4java.core.Gphoto2Executor;
import h848.software.gphoto4java.exceptions.DeviceBusyException;
import h848.software.gphoto4java.exceptions.Gphoto2Exception;
import java.io.File;
import java.util.ArrayList;
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
        if (!result.isSuccess()) {
            return null;
        }

        CameraProperty prop = new CameraProperty();
        prop.setName(configName);

        String[] lines = result.getStandardOutput().split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Label:")) {
                prop.setLabel(line.substring(6).trim());
            } else if (line.startsWith("Type:")) {
                prop.setType(line.substring(5).trim());
            } else if (line.startsWith("Readonly:")) {
                prop.setReadOnly("1".equals(line.substring(9).trim()));
            } else if (line.startsWith("Current:")) {
                prop.setCurrentValue(line.substring(8).trim());
            } else if (line.startsWith("Choice:")) {
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
        if (eventThread != null && eventThread.isAlive()) {
            return;
        }

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
                                        for (CameraEventListener l : listeners) {
                                            l.onEventReceived(eventData);
                                        }
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
        if (eventThread != null) {
            eventThread.interrupt();
        }
    }

    // --- ZÁKLADNÍ AKCE ---
    public List<CameraFile> listFiles() {
        CommandResult result = executor.execute("--list-files");
        checkErrors(result);

        List<CameraFile> files = new ArrayList<>();
        String[] lines = result.getStandardOutput().split("\\r?\\n");

        String currentFolder = "";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("There is no file in folder")) {
                continue;
            }
            if (line.startsWith("There is ")) {
                int start = line.indexOf("'/");
                int end = line.lastIndexOf("'");
                if (start != -1 && end != -1 && end > start) {
                    currentFolder = line.substring(start + 1, end);
                }
            } else if (line.startsWith("#")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int index = Integer.parseInt(parts[0].substring(1));
                        files.add(new CameraFile(currentFolder, parts[1], index));
                    } catch (NumberFormatException e) {
                        // ignore unparseable
                    }
                }
            }
        }
        return files;
    }

    public void takePhoto() {
        CommandResult result = executor.execute("--capture-image");
        checkErrors(result);
    }

    public CameraFile takePhotoAndGetFile() {
        CommandResult result = executor.execute("--capture-image");
        checkErrors(result);

        String[] lines = result.getStandardOutput().split("\\r?\\n");
        String folder = null;
        String filename = null;
        for (String line : lines) {
            if (line.startsWith("New file is in location")) {
                String[] parts = line.split(" ");
                if (parts.length >= 6) {
                    String fullPath = parts[5];
                    int lastSlash = fullPath.lastIndexOf('/');
                    if (lastSlash != -1) {
                        folder = fullPath.substring(0, lastSlash);
                        filename = fullPath.substring(lastSlash + 1);
                    }
                }
            }
        }

        if (folder != null && filename != null) {
            List<CameraFile> files = listFiles();
            for (CameraFile cf : files) {
                if (cf.getFolder().equals(folder) && cf.getFilename().equals(filename)) {
                    return cf;
                }
            }
            return new CameraFile(folder, filename, -1);
        }
        return null;
    }

    public File downloadFile(CameraFile file) {
        CommandResult result = executor.execute("--folder", file.getFolder(), "--get-file", String.valueOf(file.getIndex()));
        checkErrors(result);
        return new File(file.getFilename());
    }

    public void deleteFile(CameraFile file) {
        CommandResult result = executor.execute("--folder", file.getFolder(), "--delete-file", String.valueOf(file.getIndex()));
        checkErrors(result);
    }

    public void captureSequence(int frames, int intervalSeconds) {
        CommandResult result = executor.execute("--capture-image", "-F", String.valueOf(frames), "-I", String.valueOf(intervalSeconds));
        checkErrors(result);
    }
}
