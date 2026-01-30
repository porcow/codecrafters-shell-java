package shell;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class PwdCommand implements CommandRunner {
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
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CommandRunner.toPrintStream(out);
        ShellContext context = cmd.getContext();
        String workspace = context != null ? context.getWorkspace() : cmd.getWorkspace();
        if (workspace == null || workspace.isBlank()) {
            workspace = System.getProperty("user.dir");
        }
        stdout.println(workspace);
    }
}
