package shell;

import java.io.InputStream;
import java.io.OutputStream;

public class ExitCommand implements CCRunable {
    private static ExitCommand instance;

    private ExitCommand() {
    }

    public static ExitCommand getInstance() {
        if (instance == null) {
            instance = new ExitCommand();
        }
        return instance;
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        HistoryCommand.writeOnExit();
        System.exit(0);
    }
}
