package za.ac.cput.guiltfreecookie.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailAssetServiceTest {

    @TempDir
    private Path root;
    @Mock
    private CookieRepository cookieRepository;
    private EmailAssetService service;

    @BeforeEach
    void setUp() {
        service = new EmailAssetService(cookieRepository, root.toString());
    }

    private void cookieWithImage(String image) {
        when(cookieRepository.findById("c1"))
                .thenReturn(Optional.of(new Cookie.Builder().setCookieId("c1").setImage(image).build()));
    }

    @Test
    void rendersAnOpaqueTileCachesItAndRefreshesWhenThePhotoChanges() throws Exception {
        Path source = root.resolve("cookie.png");
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB), "png", source.toFile());
        cookieWithImage("cookie.png");

        Path file = service.image("c1");

        BufferedImage tile = ImageIO.read(file.toFile());
        assertEquals(EmailAssetService.TILE_WIDTH, tile.getWidth());
        assertEquals(EmailAssetService.TILE_HEIGHT, tile.getHeight());
        assertFalse(tile.getColorModel().hasAlpha());
        assertEquals(file, service.image("c1"));

        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB), "png", source.toFile());
        assertNotEquals(file, service.image("c1"));
    }

    @Test
    void unknownArchivedAndOutsideTheUploadFolderAreNotFound() {
        assertThrows(NotFoundException.class, () -> service.image("missing"));

        when(cookieRepository.findById("archived")).thenReturn(Optional.of(new Cookie.Builder().setArchived(true).build()));
        assertThrows(NotFoundException.class, () -> service.image("archived"));

        when(cookieRepository.findById("bad")).thenReturn(Optional.of(new Cookie.Builder().setImage("../outside.png").build()));
        assertThrows(NotFoundException.class, () -> service.image("bad"));
    }

    @Test
    void decodesTheShopsTransparentWebpPhotos() throws Exception {
        Files.copy(Path.of("frontend/public/img/cookies/double-chocolate-chunk.webp"), root.resolve("cookie.webp"));
        cookieWithImage("cookie.webp");

        assertEquals(EmailAssetService.TILE_WIDTH, ImageIO.read(service.image("c1").toFile()).getWidth());
    }
}
