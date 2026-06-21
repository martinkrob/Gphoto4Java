package h848.software.gphoto4java.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Třída zodpovědná za nízkoúrovňové volání příkazu gphoto2.
 * Skrývá veškerou logiku ProcessBuilderu.
 */
public class Gphoto2Executor {

    // Výchozí název příkazu (předpokládá se, že gphoto2 je v systémové cestě PATH)
    private static final String GPHOTO2_CMD = "gphoto2";

    /**
     * Spustí gphoto2 se zadanými argumenty a vrátí výsledek.
     * 
     * @param args Argumenty pro gphoto2 (např. "--auto-detect")
     * @return CommandResult obsahující výstup, chybový výstup a návratový kód
     * @throws RuntimeException Pokud dojde k interní chybě I/O nebo přerušení
     */
    public CommandResult execute(String... args) {
        
        List<String> command = new ArrayList<>();
        command.add(GPHOTO2_CMD);
        if (args != null) {
            command.addAll(Arrays.asList(args));
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // Čtení standardního výstupu
            StringBuilder stdOut = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOut.append(line).append(System.lineSeparator());
                }
            }

            // Čtení chybového výstupu
            StringBuilder stdErr = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    stdErr.append(line).append(System.lineSeparator());
                }
            }

            // Čekání na dokončení příkazu
            int exitCode = process.waitFor();

            return new CommandResult(exitCode, stdOut.toString().trim(), stdErr.toString().trim());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Kritická chyba při spouštění gphoto2: " + e.getMessage(), e);
        }
    }
}
