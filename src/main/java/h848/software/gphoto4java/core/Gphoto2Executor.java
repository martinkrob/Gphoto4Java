package h848.software.gphoto4java.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class responsible for low-level execution of the gphoto2 command. Hides all
 * ProcessBuilder logic.
 */
public class Gphoto2Executor {

    // Default command name (assumes gphoto2 is in the system PATH)
    private static final String GPHOTO2_CMD = "gphoto2";

    /**
     * Executes gphoto2 with the given arguments and returns the result.
     *
     * @param args Arguments for gphoto2 (e.g., "--auto-detect")
     * @return CommandResult containing stdout, stderr, and exit code
     * @throws RuntimeException If an internal I/O error or interruption occurs
     */
    public CommandResult execute(String... args) {

        List<String> command = new ArrayList<>();
        command.add(GPHOTO2_CMD);
        if (args != null) {
            command.addAll(Arrays.asList(args));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            // Force English output for reliable text parsing
            pb.environment().put("LC_ALL", "C");

            Process process = pb.start();

            // Read standard output
            StringBuilder stdOut = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOut.append(line).append(System.lineSeparator());
                }
            }

            // Read error output
            StringBuilder stdErr = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    stdErr.append(line).append(System.lineSeparator());
                }
            }

            // Wait for command completion
            int exitCode = process.waitFor();

            return new CommandResult(exitCode, stdOut.toString().trim(), stdErr.toString().trim());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Critical error executing gphoto2: " + e.getMessage(), e);
        }
    }

    /**
     * Pokusí se uvolnit USB zařízení od operačního systému (např. GNOME Nautilus / gvfs),
     * který si ho automaticky zamyká jako disk.
     */
    public void releaseUsbLock() {
        try {
            // Nejtvrdší a nejspolehlivější řešení: zabít proces, který USB blokuje
            new ProcessBuilder("pkill", "-f", "gvfs-gphoto2-volume-monitor").start().waitFor();
        } catch (Exception e) {
            // Ignorujeme
        }
        
        try {
            // Moderní systémy (Ubuntu/GNOME) - pokus o čistý unmount
            new ProcessBuilder("gio", "mount", "-s", "gphoto2").start().waitFor();
        } catch (Exception e) {
            // Ignorujeme
        }
    }
}
