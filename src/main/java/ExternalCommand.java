import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ExternalCommand implements CCRunnable {
    private static final ExternalCommand INSTANCE = new ExternalCommand();

    private ExternalCommand() {
    }

    public static ExternalCommand getInstance() {
        return INSTANCE;
    }

    @Override
    public void run(Command cmd) {
        List<String> commandLine = new ArrayList<>();
        String exec = cmd.getPath();
        if (exec == null || exec.isBlank()) {
            exec = cmd.getName();
        }
        commandLine.add(exec);
        List<String> args = cmd.getArgList();
        if (args != null && !args.isEmpty()) {
            commandLine.addAll(args);
        }

        Process process;
        try {
            process = new ProcessBuilder(commandLine)
                    .directory(new File(cmd.getWorkspace()))
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        try (InputStream stdout = process.getInputStream();
             InputStream stderr = process.getErrorStream()) {
            byte[] outBytes = stdout.readAllBytes();
            byte[] errBytes = stderr.readAllBytes();
            if (outBytes.length > 0) {
                System.out.print(new String(outBytes, StandardCharsets.UTF_8));
            }
            if (errBytes.length > 0) {
                System.err.print(new String(errBytes, StandardCharsets.UTF_8));
            }
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }
}
