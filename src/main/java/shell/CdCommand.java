package shell;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CdCommand implements CommandRunner {
    private static CdCommand instance;

    private CdCommand() {
    }

    public static CdCommand getInstance() {
        if (instance == null) {
            instance = new CdCommand();
        }
        return instance;
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CommandRunner.toPrintStream(out);
        List<String> args = cmd.getArgList();
        ShellContext context = cmd.getContext();
        String target = "";
        String home = context != null ? context.getEnv("HOME") : System.getenv("HOME");
        if (args == null || args.isEmpty()) {
            target = home != null ? home : "";
        } else {
            target = args.get(0);
        }

        if (target.startsWith("~")) {
            if (home != null && !home.isBlank()) {
                target = home + target.substring(1);
            }
        }

        if (target == null || target.isBlank()) {
            stdout.println("cd: : No such file or directory");
            return;
        }

        Path targetPath = Path.of(target);
        Path dirPath;
        if (targetPath.isAbsolute()) {
            dirPath = targetPath.normalize();
        } else {
            String base = context != null ? context.getWorkspace() : null;
            if (base == null || base.isBlank()) {
                base = cmd.getWorkspace();
            }
            if (base == null || base.isBlank()) {
                base = System.getProperty("user.dir");
            }
            dirPath = Path.of(base).resolve(target).normalize();
        }

        if (Files.isDirectory(dirPath)) {
            if (context != null) {
                context.setWorkspace(dirPath.toAbsolutePath().toString());
            } else {
                cmd.setWorkspace(dirPath.toAbsolutePath().toString());
            }
        } else {
            stdout.println("cd: " + target + ": No such file or directory");
        }
    }
}
