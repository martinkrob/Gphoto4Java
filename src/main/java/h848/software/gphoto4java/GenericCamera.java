package h848.software.gphoto4java;

import h848.software.gphoto4java.core.CameraProperty;
import h848.software.gphoto4java.core.CommandResult;
import h848.software.gphoto4java.core.Gphoto2Executor;
import h848.software.gphoto4java.core.PropertyMapper;
import h848.software.gphoto4java.core.StandardProperty;
import h848.software.gphoto4java.exceptions.DeviceBusyException;
import h848.software.gphoto4java.exceptions.Gphoto2Exception;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/**
 * Univerzální třída pro ovládání libovolného fotoaparátu přes gphoto2.
 * Nahrazuje specifickou Camera_D70 a umožňuje dynamickou práci s konfigurací.
 */
public class GenericCamera {

    private final List<CameraEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Gphoto2Executor executor;
    private final PropertyMapper mapper;
    private Thread eventThread;

    public GenericCamera() {
        this.executor = new Gphoto2Executor();
        this.mapper = new PropertyMapper();
    }

    // --- ROZHRANÍ PRO SLEDOVÁNÍ UDÁLOSTÍ A STAVU ---
    public interface CameraEventListener {
        void onEventReceived(String eventData);
    }

    public interface ConnectionListener {
        void onConnectionChanged(boolean connected);
    }

    public interface PropertyChangeListener {
        void onPropertyChanged(String configName, CameraProperty property);
    }

    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<PropertyChangeListener> propertyListeners = new CopyOnWriteArrayList<>();

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyListeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyListeners.remove(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    private void notifyConnectionChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            for (ConnectionListener l : connectionListeners) {
                l.onConnectionChanged(connected);
            }
        });
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
        // -q potlačí případné progress bary a warningy
        CommandResult result = executor.execute("-q", "--auto-detect");
        String[] lines = result.getStandardOutput().split("\\r?\\n");
        
        // gphoto2 auto-detect výstup:
        // Model          Port
        // -----------------------------------
        // [kamera]       usb:001,005
        
        if (lines.length > 2) {
            for (int i = 2; i < lines.length; i++) {
                if (lines[i].trim().length() > 0 && !lines[i].contains("---")) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> listConfigNames() {
        CommandResult result = executor.execute("--list-config");
        if (!result.isSuccess()) {
            System.err.println("[CHYBA GPHOTO2] --list-config selhalo. Návratový kód: " + result.getExitCode());
            System.err.println("[CHYBA GPHOTO2 STDERR] " + result.getErrorOutput());
            return new ArrayList<>();
        }
        
        List<String> configs = new ArrayList<>();
        String[] lines = result.getStandardOutput().split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                configs.add(line);
            }
        }
        return configs;
    }

    /**
     * Vrátí vlastnost dle standardizovaného názvu (Enum).
     */
    public CameraProperty getStandardProperty(StandardProperty prop) {
        String nativeName = mapper.getNativeName(prop);
        if (nativeName != null) {
            return getProperty(nativeName);
        }
        return null;
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

    /**
     * Nastaví vlastnost dle standardizovaného názvu (Enum).
     */
    public void setStandardProperty(StandardProperty prop, String value) {
        String nativeName = mapper.getNativeName(prop);
        if (nativeName != null) {
            setProperty(nativeName, value);
        }
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

    // --- AKTIVNÍ POLLING PARAMETRŮ ---
    private Thread pollingThread;
    private final Map<String, String> lastKnownProperties = new ConcurrentHashMap<>();

    public void startPolling(int intervalMs, List<String> configNames) {
        if (pollingThread != null && pollingThread.isAlive()) {
            return;
        }

        pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isConnected()) {
                        for (String configName : configNames) {
                            if (Thread.currentThread().isInterrupted()) break;

                            CameraProperty prop = getProperty(configName);
                            if (prop != null && prop.getCurrentValue() != null) {
                                String currentValue = prop.getCurrentValue();
                                String lastValue = lastKnownProperties.get(configName);

                                // Pokud došlo ke změně (nebo je to první načtení), uložíme a vyvoláme událost
                                if (!currentValue.equals(lastValue)) {
                                    lastKnownProperties.put(configName, currentValue);
                                    SwingUtilities.invokeLater(() -> {
                                        for (PropertyChangeListener l : propertyListeners) {
                                            l.onPropertyChanged(configName, prop);
                                        }
                                    });
                                }
                            }
                        }
                    }
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    public void stopPolling() {
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        lastKnownProperties.clear();
    }

    // --- ASYNCHRONNÍ NASLOUCHÁNÍ A MONITORING ---
    public void startEventListening() {
        if (eventThread != null && eventThread.isAlive()) {
            return;
        }

        eventThread = new Thread(() -> {
            boolean wasConnected = false;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!wasConnected) {
                        // Jsme odpojeni, zkusíme se připojit
                        if (isConnected()) {
                            // 1. Zkusíme okamžitě odpojit Nautilus / gvfs, aby nám uvolnil sběrnici
                            executor.releaseUsbLock();
                            
                            // 2. Chytré čekání s opakovaným potlačením OS.
                            // Pokud zapnete foťák za běhu aplikace, Linuxový udev může proces gvfs spustit znovu,
                            // proto ho v případě neúspěchu zabíjíme opakovaně.
                            for (int i = 0; i < 15; i++) {
                                Thread.sleep(500);
                                CommandResult testResult = executor.execute("-q", "--list-config");
                                if (testResult.isSuccess() && testResult.getStandardOutput().length() > 5) {
                                    // USB je uvolněné a plně naše!
                                    // Sestavíme překladový slovník parametrů
                                    List<String> configs = java.util.Arrays.asList(testResult.getStandardOutput().split("\\r?\\n"));
                                    mapper.initialize(configs);
                                    break; 
                                } else {
                                    // Gphoto2 selhalo (foťák je stále zamknutý). Zkusíme proces odstřelit znovu!
                                    executor.releaseUsbLock();
                                }
                            }
                            
                            // 3. Nyní teprve ohlásíme, že jsme připojeni a bezpečně předáme řízení GUI
                            wasConnected = true;
                            notifyConnectionChanged(true);
                        } else {
                            Thread.sleep(2000); // Polling každé 2 vteřiny
                        }
                    } else {
                        // Jsme připojeni, pouze pasivně hlídáme, jestli nebyl vytažen kabel.
                        // Už neposíláme --wait-event, protože to na starších Nikonech 
                        // fyzicky probouzí foťák a bliká to displejem/LEDkou.
                        Thread.sleep(2000);
                        
                        if (!isConnected()) {
                            wasConnected = false;
                            notifyConnectionChanged(false);
                        }
                    }

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
