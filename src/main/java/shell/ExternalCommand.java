package shell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExternalCommand implements CCRunable {
    private static final ExternalCommand INSTANCE = new ExternalCommand();

    private ExternalCommand() {
    }

    public static ExternalCommand getInstance() {
        return INSTANCE;
    }

    @Override
    public void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
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
            stdinThread = pipe(in, process.getOutputStream(), true, null);
        }
        Runnable onStdoutFailure = out == System.out ? null : process::destroy;
        Thread stdoutThread = pipe(process.getInputStream(), out, false, onStdoutFailure);
        Thread stderrThread = pipe(process.getErrorStream(), err, false, null);

        try {
            process.waitFor();
            if (stdinThread != null) {
                if (in != null && in != System.in) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ignore close failures.
                    }
                }
                stdinThread.join();
            }
            stdoutThread.join();
            stderrThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroy();
            return;
        }
    }

    private static Thread pipe(InputStream in,
                               OutputStream out,
                               boolean closeOut,
                               Runnable onFailure) {
        Thread thread = new Thread(() -> {
            try {
                if (in != null) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                if (onFailure != null) {
                    onFailure.run();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // Ignore close failures.
                    }
                }
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
