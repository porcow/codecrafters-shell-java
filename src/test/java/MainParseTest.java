import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class MainParseTest {
    @Test
    void parse_preservesWhitespaceInsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"hello    world\"");

        assertEquals("echo", command.getName());
        assertEquals(List.of("hello    world"), command.getArgList());
    }

    @Test
    void parse_concatenatesAdjacentDoubleQuotedStrings() {
        Command command = CCParser.parse("echo \"hello\"\"world\"");

        assertEquals(List.of("helloworld"), command.getArgList());
    }

    @Test
    void parse_splitsOnWhitespaceOutsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"hello\" \"world\"");

        assertEquals(List.of("hello", "world"), command.getArgList());
    }

    @Test
    void parse_treatsSingleQuotesLiterallyInsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"shell's test\"");

        assertEquals(List.of("shell's test"), command.getArgList());
    }

    @Test
    void parse_preservesWhitespaceInsideSingleQuotes() {
        Command command = CCParser.parse("echo 'hello    world'");

        assertEquals(List.of("hello    world"), command.getArgList());
    }

    @Test
    void parse_concatenatesAdjacentSingleQuotedStrings() {
        Command command = CCParser.parse("echo 'hello''world'");

        assertEquals(List.of("helloworld"), command.getArgList());
    }

    @Test
    void parse_splitsOnWhitespaceOutsideSingleQuotes() {
        Command command = CCParser.parse("echo 'hello' 'world'");

        assertEquals(List.of("hello", "world"), command.getArgList());
    }

    @Test
    void parse_keepsBackslashesLiteralInsideSingleQuotes() {
        Command command = CCParser.parse("echo 'test\\nexample'");

        assertEquals(List.of("test\\nexample"), command.getArgList());
    }

    @Test
    void parse_parsesQuotedExecutableNameWithSingleQuotes() {
        Command command = CCParser.parse("'exe  with  space' arg");

        assertEquals("exe  with  space", command.getName());
        assertEquals(List.of("arg"), command.getArgList());
    }

    @Test
    void parse_preservesEscapedSpacesOutsideQuotes() {
        Command command = CCParser.parse("echo three\\ \\ \\ spaces");

        assertEquals(List.of("three   spaces"), command.getArgList());
    }

    @Test
    void parse_collapsesUnescapedSpacesAfterEscapedSpace() {
        Command command = CCParser.parse("echo before\\     after");

        assertEquals(List.of("before ", "after"), command.getArgList());
    }

    @Test
    void parse_treatsEscapedCharactersLiterally() {
        Command command = CCParser.parse("echo test\\nexample");

        assertEquals(List.of("testnexample"), command.getArgList());
    }

    @Test
    void parse_escapesBackslashOutsideQuotes() {
        Command command = CCParser.parse("echo hello\\\\world");

        assertEquals(List.of("hello\\world"), command.getArgList());
    }

    @Test
    void parse_escapesSingleQuotesOutsideQuotes() {
        Command command = CCParser.parse("echo \\'hello\\'");

        assertEquals(List.of("'hello'"), command.getArgList());
    }

    @Test
    void parse_expandsTildeInArguments() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());

        Command command = CCParser.parse("echo ~/docs");

        assertEquals(List.of(home + "/docs"), command.getArgList());
    }

    @Test
    void parse_escapesDoubleQuoteInsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"hello\\\"world\"");

        assertEquals(List.of("hello\"world"), command.getArgList());
    }

    @Test
    void parse_escapesBackslashInsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"hello\\\\world\"");

        assertEquals(List.of("hello\\world"), command.getArgList());
    }

    @Test
    void parse_keepsOtherBackslashesLiteralInsideDoubleQuotes() {
        Command command = CCParser.parse("echo \"test\\nexample\"");

        assertEquals(List.of("test\\nexample"), command.getArgList());
    }
}
