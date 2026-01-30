package shell;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

public interface CCRunable {
    default void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
    }

    void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err);

    // Caller must consume/close the returned stream to avoid blocking the producer.
    default InputStream stdoutStream(Command source) {
        return streamFrom(source, true);
    }

    default InputStream stderrStream(Command source) {
        return streamFrom(source, false);
    }

    static PrintStream toPrintStream(OutputStream out) {
        if (out instanceof PrintStream) {
            return (PrintStream) out;
        }
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private InputStream streamFrom(Command source, boolean stdout) {
        try {
            PipedInputStream pipeIn = new PipedInputStream(64 * 1024);
            PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
            Thread thread = new Thread(() -> {
                try {
                    if (stdout) {
                        runWithStreams(source, System.in, pipeOut, System.err);
                    } else {
                        runWithStreams(source, System.in, System.out, pipeOut);
                    }
                } finally {
                    try {
                        pipeOut.close();
                    } catch (IOException e) {
                        // Ignore close failures on shutdown.
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            return pipeIn;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
