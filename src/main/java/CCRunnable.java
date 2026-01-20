import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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

    default void stdout(Command source, Command target) {
        String output = captureStream(true, () -> run(source));
        target.setArgString(output);
        target.setArgList(CCParser.parseArguments(output));
    }

    default void stderr(Command source, Command target) {
        String output = captureStream(false, () -> run(source));
        target.setArgString(output);
        target.setArgList(CCParser.parseArguments(output));
    }

    static PrintStream toPrintStream(OutputStream out) {
        if (out instanceof PrintStream) {
            return (PrintStream) out;
        }
        return new PrintStream(out, true, StandardCharsets.UTF_8);
    }

    private static String captureStream(boolean stdout, Runnable action) {
        PrintStream original = stdout ? System.out : System.err;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        if (stdout) {
            System.setOut(capture);
        } else {
            System.setErr(capture);
        }
        try {
            action.run();
        } finally {
            capture.flush();
            if (stdout) {
                System.setOut(original);
            } else {
                System.setErr(original);
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
