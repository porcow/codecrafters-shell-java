import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class PipelineTest {
    @Test
    void eval_redirectsStdoutToNextCommand() {
        List<Command> commands = Main.parsePipeline("echo hello world > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(commands));

        assertEquals("hello world" + System.lineSeparator(), output);
    }

    @Test
    void eval_splitsOnOneGreaterThan() {
        List<Command> commands = Main.parsePipeline("echo hello 1> echo");

        String output = TestUtils.captureStdout(() -> Main.eval(commands));

        assertEquals("hello" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresEscapedGreaterThan() {
        List<Command> commands = Main.parsePipeline("echo a\\>b > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(commands));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresQuotedGreaterThan() {
        List<Command> commands = Main.parsePipeline("echo \"a>b\" > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(commands));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_chainsMultipleRedirections() {
        List<Command> commands = Main.parsePipeline("echo hi > echo > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(commands));

        assertEquals("hi" + System.lineSeparator(), output);
    }
}
