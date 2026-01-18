import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CCRunnableTest {
    @Test
    void stdout_capturesOutputIntoTargetCommand() {
        CCRunnable runner = new CCRunnable() {
            @Override
            public void run(Command cmd) {
                System.out.print("hello world");
            }
        };
        Command source = Command.build("echo", "");
        Command target = new Command();

        runner.stdout(source, target);

        assertEquals("hello world", target.getArgString());
        assertEquals(List.of("hello", "world"), target.getArgList());
    }

    @Test
    void stderr_capturesOutputIntoTargetCommand() {
        CCRunnable runner = new CCRunnable() {
            @Override
            public void run(Command cmd) {
                System.err.print("boom");
            }
        };
        Command source = Command.build("echo", "");
        Command target = new Command();

        runner.stderr(source, target);

        assertEquals("boom", target.getArgString());
        assertEquals(List.of("boom"), target.getArgList());
    }
}
