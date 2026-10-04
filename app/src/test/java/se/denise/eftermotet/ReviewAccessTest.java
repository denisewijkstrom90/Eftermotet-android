package se.denise.eftermotet;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReviewAccessTest {
    private static final String CODE = "0123456789ABCDEF0123456789ABCDEF";
    private static final String HASH = "cd6c1f7d1dc6717d6371d2647910ca71ba3bf0b611083d322466b8843b4285b6";
    @Test public void rejectsMissingMalformedAndWrongCodes() {
        assertFalse(ReviewAccess.accepts(null, HASH));
        assertFalse(ReviewAccess.accepts("", HASH));
        assertFalse(ReviewAccess.accepts("0".repeat(32), HASH));
        assertFalse(ReviewAccess.accepts(CODE, ""));
        assertFalse(ReviewAccess.accepts(CODE + "A", HASH));
    }
    @Test public void acceptsCorrectCodeAndIgnoresCaseAndOuterWhitespace() {
        assertTrue(ReviewAccess.accepts(CODE, HASH));
        assertTrue(ReviewAccess.accepts("  " + CODE.toLowerCase(java.util.Locale.ROOT) + "\n", HASH));
    }
}
