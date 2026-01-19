import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class MainParseTest {
    @Test
    void parse_preservesWhitespaceInsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello    world\"");

        assertEquals("echo", command.getName());
        assertEquals(List.of("hello    world"), command.getArgList());
    }

    @Test
    void parse_concatenatesAdjacentDoubleQuotedStrings() {
        Command command = Main.parse("echo \"hello\"\"world\"");

        assertEquals(List.of("helloworld"), command.getArgList());
    }

    @Test
    void parse_splitsOnWhitespaceOutsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello\" \"world\"");

        assertEquals(List.of("hello", "world"), command.getArgList());
    }

    @Test
    void parse_treatsSingleQuotesLiterallyInsideDoubleQuotes() {
        Command command = Main.parse("echo \"shell's test\"");

        assertEquals(List.of("shell's test"), command.getArgList());
    }

    @Test
    void parse_preservesWhitespaceInsideSingleQuotes() {
        Command command = Main.parse("echo 'hello    world'");

        assertEquals(List.of("hello    world"), command.getArgList());
    }

    @Test
    void parse_concatenatesAdjacentSingleQuotedStrings() {
        Command command = Main.parse("echo 'hello''world'");

        assertEquals(List.of("helloworld"), command.getArgList());
    }

    @Test
    void parse_splitsOnWhitespaceOutsideSingleQuotes() {
        Command command = Main.parse("echo 'hello' 'world'");

        assertEquals(List.of("hello", "world"), command.getArgList());
    }

    @Test
    void parse_keepsBackslashesLiteralInsideSingleQuotes() {
        Command command = Main.parse("echo 'test\\nexample'");

        assertEquals(List.of("test\\nexample"), command.getArgList());
    }

    @Test
    void parse_parsesQuotedExecutableNameWithSingleQuotes() {
        Command command = Main.parse("'exe  with  space' arg");

        assertEquals("exe  with  space", command.getName());
        assertEquals(List.of("arg"), command.getArgList());
    }

    @Test
    void parse_preservesEscapedSpacesOutsideQuotes() {
        Command command = Main.parse("echo three\\ \\ \\ spaces");

        assertEquals(List.of("three   spaces"), command.getArgList());
    }

    @Test
    void parse_collapsesUnescapedSpacesAfterEscapedSpace() {
        Command command = Main.parse("echo before\\     after");

        assertEquals(List.of("before ", "after"), command.getArgList());
    }

    @Test
    void parse_treatsEscapedCharactersLiterally() {
        Command command = Main.parse("echo test\\nexample");

        assertEquals(List.of("testnexample"), command.getArgList());
    }

    @Test
    void parse_escapesBackslashOutsideQuotes() {
        Command command = Main.parse("echo hello\\\\world");

        assertEquals(List.of("hello\\world"), command.getArgList());
    }

    @Test
    void parse_escapesSingleQuotesOutsideQuotes() {
        Command command = Main.parse("echo \\'hello\\'");

        assertEquals(List.of("'hello'"), command.getArgList());
    }

    @Test
    void parse_expandsTildeInArguments() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());

        Command command = Main.parse("echo ~/docs");

        assertEquals(List.of(home + "/docs"), command.getArgList());
    }

    @Test
    void parse_escapesDoubleQuoteInsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello\\\"world\"");

        assertEquals(List.of("hello\"world"), command.getArgList());
    }

    @Test
    void parse_escapesBackslashInsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello\\\\world\"");

        assertEquals(List.of("hello\\world"), command.getArgList());
    }

    @Test
    void parse_keepsOtherBackslashesLiteralInsideDoubleQuotes() {
        Command command = Main.parse("echo \"test\\nexample\"");

        assertEquals(List.of("test\\nexample"), command.getArgList());
    }
}
