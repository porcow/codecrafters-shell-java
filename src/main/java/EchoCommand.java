import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class EchoCommand implements CCRunnable {
    private static EchoCommand instance;

    private EchoCommand() {
    }

    public static EchoCommand getInstance() {
        if (instance == null) {
            instance = new EchoCommand();
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
        if (cmd.getArgList() == null) {
            stdout.println();
            return;
        }
        stdout.println(String.join(" ", cmd.getArgList()));
    }

}
