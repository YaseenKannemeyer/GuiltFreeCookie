package za.ac.cput.guiltfreecookie.service.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import za.ac.cput.guiltfreecookie.domain.CookieImage;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.repository.CookieImageRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.service.ImageStorageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieImageServiceTest {

    @Mock
    private CookieImageRepository imageRepository;
    @Mock
    private CookieRepository cookieRepository;
    @Mock
    private ImageStorageService imageStorageService;
    @InjectMocks
    private CookieImageService service;

    private final MockMultipartFile photo = new MockMultipartFile("image", "c.png", "image/png", new byte[]{1});

    @Test
    void aCookieCanHaveAtMostTenPhotos() {
        when(cookieRepository.existsById("CK1")).thenReturn(true);
        when(imageRepository.countByCookieId("CK1")).thenReturn(10L);

        assertThrows(BadRequestException.class, () -> service.add("CK1", photo, null));
        verify(imageStorageService, never()).store(any());
    }

    @Test
    void aFileThatIsNotAnImageIsABadRequest() {
        when(cookieRepository.existsById("CK1")).thenReturn(true);
        when(imageStorageService.store(photo)).thenThrow(new IllegalArgumentException("Upload a JPEG, PNG, WebP or GIF image"));

        assertThrows(BadRequestException.class, () -> service.add("CK1", photo, "Cookie"));
    }

    @Test
    void reorderNeedsEveryPhotoExactlyOnce() {
        CookieImage a = new CookieImage.Builder().setImageId("A").setCookieId("CK1").build();
        CookieImage b = new CookieImage.Builder().setImageId("B").setCookieId("CK1").build();
        when(imageRepository.findByCookieIdOrderBySortOrderAsc("CK1")).thenReturn(List.of(a, b));

        assertThrows(BadRequestException.class, () -> service.reorder("CK1", List.of("A")));
        assertThrows(BadRequestException.class, () -> service.reorder("CK1", List.of("A", "A")));
        assertThrows(BadRequestException.class, () -> service.reorder("CK1", List.of("A", "Z")));
        verify(imageRepository, never()).save(any());
    }
}
