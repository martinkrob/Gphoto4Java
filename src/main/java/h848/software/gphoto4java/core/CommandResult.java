package h848.software.gphoto4java.core;

/**
 * Encapsulates the result of executing a command in the command line.
 * Holds the exit code, standard output (stdout), and error output (stderr).
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
     * Returns true if the command executed successfully (usually exit code 0).
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
}
