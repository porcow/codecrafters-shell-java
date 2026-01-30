package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.Permission;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class ExitCommandTest {
    @Test
    void run_exitsWithZeroStatus() {
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new NoExitSecurityManager());
        } catch (UnsupportedOperationException | SecurityException e) {
            Assumptions.assumeTrue(false, "SecurityManager not supported: " + e.getMessage());
            return;
        }

        try {
            ExitTrappedException ex = assertThrows(
                    ExitTrappedException.class,
                    () -> ExitCommand.getInstance().run(Command.build("exit", ""))
            );
            assertEquals(0, ex.status);
        } finally {
            System.setSecurityManager(original);
        }
    }

    private static final class ExitTrappedException extends SecurityException {
        private final int status;

        private ExitTrappedException(int status) {
            this.status = status;
        }
    }

    private static final class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkExit(int status) {
            throw new ExitTrappedException(status);
        }
    }
}
