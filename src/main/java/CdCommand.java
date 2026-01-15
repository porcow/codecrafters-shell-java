import java.nio.file.Files;
import java.nio.file.Path;

public class CdCommand implements Runnable {
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
        String rawArgs = cmd.getArgString().trim();
        String target = rawArgs;
        String home = System.getenv("HOME");
        if (rawArgs.isBlank()) {
            target = home != null ? home : "";
        } else {
            String[] parts = rawArgs.split("\\s+");
            target = parts[0];
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
                base = System.getProperty("user.dir");
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
