package za.ac.cput.guiltfreecookie.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperTest {

    @Test
    void slugifyMakesReadableUrlNames() {
        assertEquals("toasted-almond-vanilla", Helper.slugify("Toasted Almond & Vanilla"));
        assertEquals("creme-brulee", Helper.slugify("  Crème Brûlée!! "));
        assertEquals("", Helper.slugify("&&&"));
        assertEquals("", Helper.slugify(null));
        assertTrue(Helper.slugify("a".repeat(200)).length() <= Helper.MAX_SLUG_LENGTH);
    }

    @Test
    void isValidSlugAcceptsOnlyCleanSlugs() {
        assertTrue(Helper.isValidSlug("double-chocolate-chunk"));
        assertTrue(Helper.isValidSlug("box2"));
        assertFalse(Helper.isValidSlug("Double-Chunk"));
        assertFalse(Helper.isValidSlug("-chunk"));
        assertFalse(Helper.isValidSlug("chunk--two"));
        assertFalse(Helper.isValidSlug(""));
        assertFalse(Helper.isValidSlug(null));
    }
}
