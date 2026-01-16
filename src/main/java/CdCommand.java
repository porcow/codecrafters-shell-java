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
            System.out.println("cd: : No such file or directory");
            return;
        }

        Path targetPath = Path.of(target);
        Path dirPath;
        if (targetPath.isAbsolute()) {
            dirPath = targetPath.normalize();
        } else {
            String base = Command.getCurrentWorkspace();
            if (base == null || base.isBlank()) {
                base = System.getenv("HOME");
            }
            dirPath = Path.of(base).resolve(target).normalize();
        }

        if (Files.isDirectory(dirPath)) {
            Command.setCurrentWorkspace(dirPath.toAbsolutePath().toString());
        } else {
            System.out.println("cd: " + target + ": No such file or directory");
        }
    }
}
