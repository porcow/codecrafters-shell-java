import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CdCommand implements CCRunnable {
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
    public void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CCRunnable.toPrintStream(out);
        List<String> args = cmd.getArgList();
        String target = "";
        String home = System.getenv("HOME");
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
            String base = Command.getCurrentWorkspace();
            if (base == null || base.isBlank()) {
                base = System.getProperty("user.dir");
            }
            dirPath = Path.of(base).resolve(target).normalize();
        }

        if (Files.isDirectory(dirPath)) {
            Command.setCurrentWorkspace(dirPath.toAbsolutePath().toString());
        } else {
            stdout.println("cd: " + target + ": No such file or directory");
        }
    }
}
