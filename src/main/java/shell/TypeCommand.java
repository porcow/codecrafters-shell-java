package shell;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class TypeCommand implements CommandRunner {
    private static TypeCommand instance;

    private TypeCommand() {
    }

    public static TypeCommand getInstance() {
        if (instance == null) {
            instance = new TypeCommand();
        }
        return instance;
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CommandRunner.toPrintStream(out);
        List<String> args = cmd.getArgList();
        if (args == null || args.isEmpty()) {
            return;
        }

        for (String arg : args) {
            Command typeCommand = Command.build(cmd.getContext(), arg, "");
            if (typeCommand.isBuiltin()) {
                stdout.println(arg + " is a shell builtin");
            } else if (typeCommand.isRunable() && typeCommand.getPath() != null) {
                stdout.println(arg + " is " + typeCommand.getPath());
            } else {
                stdout.println(arg + ": not found");
            }
        }
    }
}
