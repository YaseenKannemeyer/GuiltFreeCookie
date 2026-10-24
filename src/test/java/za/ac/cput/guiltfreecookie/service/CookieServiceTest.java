package za.ac.cput.guiltfreecookie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.service.catalog.CookieImageService;
import za.ac.cput.guiltfreecookie.service.catalog.CookieVariantService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieServiceTest {

    @Mock
    private CookieRepository cookieRepository;

    @Mock
    private CookieVariantService variantService;

    @Mock
    private CookieImageService imageService;

    @InjectMocks
    private CookieService cookieService;

    private Cookie existingCookie;

    @BeforeEach
    void setUp() {
        existingCookie = new Cookie.Builder()
                .setCookieId("CK001")
                .setCategory(CookieCategory.HIGH_PROTEIN)
                .setDescription("Protein Chocolate Cookie")
                .setIngredients("Whey Protein, Cocoa Powder, Peanut Butter")
                .setAllergies("Dairy, Peanuts")
                .setPrice(35.00)
                .setImage("cookies/original.jpg")
                .setArchived(false)
                .build();
    }

    @Test
    void testCreate() {
        when(cookieRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(cookieRepository.save(any(Cookie.class))).thenAnswer(call -> call.getArgument(0));

        Cookie result = cookieService.create(existingCookie);

        assertEquals("CK001", result.getCookieId());
        assertEquals("protein-chocolate-cookie", result.getSlug());
        assertNotNull(result.getCreatedAt());
        verify(variantService).ensureDefault(result);
    }

    @Test
    void testCreateGivesADuplicateNameTheNextFreeSlug() {
        Cookie other = new Cookie.Builder().setCookieId("CK777").setSlug("protein-chocolate-cookie").build();
        when(cookieRepository.findBySlug("protein-chocolate-cookie")).thenReturn(Optional.of(other));
        when(cookieRepository.findBySlug("protein-chocolate-cookie-2")).thenReturn(Optional.empty());
        when(cookieRepository.save(any(Cookie.class))).thenAnswer(call -> call.getArgument(0));

        Cookie result = cookieService.create(existingCookie);

        assertEquals("protein-chocolate-cookie-2", result.getSlug());
    }

    @Test
    void testReadFound() {
        when(cookieRepository.findById("CK001")).thenReturn(Optional.of(existingCookie));

        Cookie result = cookieService.read("CK001");

        assertEquals("Protein Chocolate Cookie", result.getDescription());
    }

    @Test
    void testReadNotFound() {
        when(cookieRepository.findById("missing")).thenReturn(Optional.empty());

        Cookie result = cookieService.read("missing");

        assertNull(result);
    }

    @Test
    void testUpdateWhenFoundMergesEditableFieldsAndKeepsArchivedFlag() {
        Cookie archivedExisting = new Cookie.Builder().copy(existingCookie).setArchived(true).build();
        Cookie incoming = new Cookie.Builder()
                .setCookieId("CK001")
                .setCategory(CookieCategory.VEGAN)
                .setDescription("Updated Description")
                .setLongDescription("Updated long description")
                .setIngredients("Updated Ingredients")
                .setAllergies("Updated Allergies")
                .setPrice(40.00)
                .setImage("cookies/new.jpg")
                .build();

        when(cookieRepository.findById("CK001")).thenReturn(Optional.of(archivedExisting));
        when(cookieRepository.save(any(Cookie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cookie result = cookieService.update(incoming);

        assertEquals("CK001", result.getCookieId());
        assertEquals(CookieCategory.VEGAN, result.getCategory());
        assertEquals("Updated Description", result.getDescription());
        assertEquals("Updated long description", result.getLongDescription());
        assertEquals("Updated Ingredients", result.getIngredients());
        assertEquals("Updated Allergies", result.getAllergies());
        assertEquals(40.00, result.getPrice());
        assertEquals("cookies/new.jpg", result.getImage());
        assertTrue(result.isArchived(), "update() must not silently un-archive a cookie");
    }

    @Test
    void testLegacyUpdateKeepsCatalogFieldsTheOldFormDoesNotSend() {
        Cookie existing = new Cookie.Builder().copy(existingCookie)
                .setSlug("protein-chocolate-cookie")
                .setStockQuantity(40)
                .setFeatured(true)
                .setCalories(180)
                .build();
        Cookie fromOldForm = new Cookie.Builder().copy(existingCookie).setPrice(39.00).build();
        when(cookieRepository.findById("CK001")).thenReturn(Optional.of(existing));
        when(cookieRepository.save(any(Cookie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cookie result = cookieService.update(fromOldForm);

        assertEquals(39.00, result.getPrice());
        assertEquals("protein-chocolate-cookie", result.getSlug());
        assertEquals(40, result.getStockQuantity());
        assertTrue(result.isFeatured());
        assertEquals(180, result.getCalories());
    }

    @Test
    void testUpdateWhenNotFoundReturnsNull() {
        when(cookieRepository.findById("missing")).thenReturn(Optional.empty());

        Cookie incoming = new Cookie.Builder().setCookieId("missing").build();
        Cookie result = cookieService.update(incoming);

        assertNull(result);
        verify(cookieRepository, never()).save(any(Cookie.class));
    }

    @Test
    void testDeleteWhenExists() {
        when(cookieRepository.existsById("CK001")).thenReturn(true);

        boolean result = cookieService.delete("CK001");

        assertTrue(result);
        verify(cookieRepository).deleteById("CK001");
        verify(variantService).deleteAllFor("CK001");
        verify(imageService).deleteAllFor("CK001");
    }

    @Test
    void testDeleteWhenNotExists() {
        when(cookieRepository.existsById("missing")).thenReturn(false);

        boolean result = cookieService.delete("missing");

        assertFalse(result);
        verify(cookieRepository, never()).deleteById(anyString());
    }

    @Test
    void testGetAllReturnsEveryCookieIncludingArchived() {
        Cookie archived = new Cookie.Builder().copy(existingCookie).setCookieId("CK002").setArchived(true).build();
        when(cookieRepository.findAll()).thenReturn(List.of(existingCookie, archived));

        List<Cookie> result = cookieService.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void testGetAllActiveDelegatesToArchivedFalseQuery() {
        when(cookieRepository.findByArchivedFalse()).thenReturn(List.of(existingCookie));

        List<Cookie> result = cookieService.getAllActive();

        assertEquals(1, result.size());
        assertFalse(result.get(0).isArchived());
        verify(cookieRepository, times(1)).findByArchivedFalse();
    }

    @Test
    void testSetArchivedTrue() {
        when(cookieRepository.findById("CK001")).thenReturn(Optional.of(existingCookie));
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        when(cookieRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Cookie result = cookieService.setArchived("CK001", true);

        assertTrue(result.isArchived());
        assertEquals("CK001", captor.getValue().getCookieId());
        assertTrue(captor.getValue().isArchived());
    }

    @Test
    void testSetArchivedFalse() {
        Cookie archived = new Cookie.Builder().copy(existingCookie).setArchived(true).build();
        when(cookieRepository.findById("CK001")).thenReturn(Optional.of(archived));
        when(cookieRepository.save(any(Cookie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cookie result = cookieService.setArchived("CK001", false);

        assertFalse(result.isArchived());
    }

    @Test
    void testSetArchivedWhenNotFoundReturnsNull() {
        when(cookieRepository.findById("missing")).thenReturn(Optional.empty());

        Cookie result = cookieService.setArchived("missing", true);

        assertNull(result);
        verify(cookieRepository, never()).save(any(Cookie.class));
    }
}
