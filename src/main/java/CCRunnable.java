import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public interface CCRunnable {
    void run(Command cmd);

    default void stdout(Command source, Command target) {
        String output = captureStream(true, () -> run(source));
        target.setArgString(output);
        target.setArgList(Main.parseArguments(output));
    }

    default void stderr(Command source, Command target) {
        String output = captureStream(false, () -> run(source));
        target.setArgString(output);
        target.setArgList(Main.parseArguments(output));
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
