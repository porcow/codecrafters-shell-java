import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

public interface CCRunnable {
    void run(Command cmd);

    default void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        PrintStream newOut = toPrintStream(out);
        PrintStream newErr = toPrintStream(err);
        try {
            if (in != null) {
                System.setIn(in);
            }
            System.setOut(newOut);
            System.setErr(newErr);
            run(cmd);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

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
