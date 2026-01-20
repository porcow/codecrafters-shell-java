import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        String execPath = cmd.getPath();
        String execName = cmd.getName();

        if (execName == null || execName.isBlank()) {
            commandLine.add(execPath);
        } else {
            commandLine.add(execName);
        }

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

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        List<String> commandLine = new ArrayList<>();
        String execPath = cmd.getPath();
        String execName = cmd.getName();

        if (execName == null || execName.isBlank()) {
            commandLine.add(execPath);
        } else {
            commandLine.add(execName);
        }

        List<String> args = cmd.getArgList();
        if (args != null && !args.isEmpty()) {
            commandLine.addAll(args);
        }

        Process process;
        boolean inheritInput = in == System.in;
        try {
            ProcessBuilder builder = new ProcessBuilder(commandLine)
                    .directory(new File(cmd.getWorkspace()));
            if (inheritInput) {
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            process = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        Thread stdinThread = null;
        if (!inheritInput) {
            stdinThread = pipe(in, process.getOutputStream(), true);
        }
        Thread stdoutThread = pipe(process.getInputStream(), out, false);
        Thread stderrThread = pipe(process.getErrorStream(), err, false);

        try {
            process.waitFor();
            if (stdinThread != null) {
                stdinThread.join();
            }
            stdoutThread.join();
            stderrThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        }
    }

    private static Thread pipe(InputStream in, OutputStream out, boolean closeOut) {
        Thread thread = new Thread(() -> {
            try {
                if (in != null) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                // Ignore pipe errors once the process exits or streams close.
            } finally {
                try {
                    out.flush();
                } catch (IOException e) {
                    // Ignore flush failures.
                }
                if (closeOut) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // Ignore close failures.
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

}
