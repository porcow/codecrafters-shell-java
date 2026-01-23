import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class CCRunnableTest {
    @Test
    void stdout_streamsOutput() throws Exception {
        CCRunnable runner = new CCRunnable() {
            @Override
            public void run(Command cmd) {
                System.out.print("hello world");
            }
        };
        Command source = Command.build("echo", "");

        String output;
        try (InputStream in = runner.stdoutStream(source)) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals("hello world", output);
    }

    @Test
    void stderr_streamsOutput() throws Exception {
        CCRunnable runner = new CCRunnable() {
            @Override
            public void run(Command cmd) {
                System.err.print("boom");
            }
        };
        Command source = Command.build("echo", "");

        String output;
        try (InputStream in = runner.stderrStream(source)) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals("boom", output);
    }
}
