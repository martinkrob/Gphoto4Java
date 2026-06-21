package h848.software.gphoto4java.core;

/**
 * Zapouzdřuje výsledek po spuštění příkazu v příkazové řádce.
 * Udržuje návratový kód, standardní výstup (stdout) a chybový výstup (stderr).
 */
public class CommandResult {
    
    private final int exitCode;
    private final String standardOutput;
    private final String errorOutput;

    public CommandResult(int exitCode, String standardOutput, String errorOutput) {
        this.exitCode = exitCode;
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStandardOutput() {
        return standardOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    /**
     * Vrací true, pokud příkaz proběhl úspěšně (obvykle návratový kód 0).
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
