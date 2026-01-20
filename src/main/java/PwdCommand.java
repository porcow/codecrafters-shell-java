import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class PwdCommand implements CCRunnable {
    private static PwdCommand instance;

    private PwdCommand() {
    }

    public static PwdCommand getInstance() {
        if (instance == null) {
            instance = new PwdCommand();
        }
        return instance;
    }

    @Override
    public void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CCRunnable.toPrintStream(out);
        stdout.println(Command.currentWorkspace);
    }
}
