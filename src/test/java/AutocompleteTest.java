import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

public class AutocompleteTest {
    private static String uniqueMatch(String token) throws Exception {
        Method method = Main.class.getDeclaredMethod("uniqueBuiltinMatch", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, token);
    }

    @Test
    void uniqueBuiltinMatch_completesEcho() throws Exception {
        assertEquals("echo", uniqueMatch("ech"));
    }

    @Test
    void uniqueBuiltinMatch_completesExit() throws Exception {
        assertEquals("exit", uniqueMatch("exi"));
    }

    @Test
    void uniqueBuiltinMatch_returnsNullWhenAmbiguous() throws Exception {
        assertNull(uniqueMatch("e"));
    }

    @Test
    void uniqueBuiltinMatch_returnsNullWhenNoMatch() throws Exception {
        assertNull(uniqueMatch("nope"));
    }
}
