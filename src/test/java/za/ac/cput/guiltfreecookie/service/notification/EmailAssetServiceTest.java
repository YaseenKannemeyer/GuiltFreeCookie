package za.ac.cput.guiltfreecookie.service.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

class EmailAssetServiceTest {

    @TempDir
    Path root;

    @Test
    void rendersOpaqueTileCachesAndInvalidatesWhenImageChanges()
        throws Exception {
        var repo = mock(CookieRepository.class);
        var service = new EmailAssetService(repo, root.toString());
        var source = root.resolve("cookie.png");
        ImageIO.write(
            new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB),
            "png",
            source.toFile()
        );
        when(repo.findById("c1")).thenReturn(
            Optional.of(
                new Cookie.Builder()
                    .setCookieId("c1")
                    .setImage("cookie.png")
                    .build()
            )
        );
        var file = service.image("c1");
        var tile = ImageIO.read(file.toFile());
        assertEquals(600, tile.getWidth());
        assertEquals(750, tile.getHeight());
        assertFalse(tile.getColorModel().hasAlpha());
        assertEquals(file, service.image("c1"));
        ImageIO.write(
            new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB),
            "png",
            source.toFile()
        );
        assertNotEquals(file, service.image("c1"));
    }

    @Test
    void rejectsUnknownArchivedAndPathsOutsideUploads() {
        var repo = mock(CookieRepository.class);
        var service = new EmailAssetService(repo, root.toString());
        assertThrows(NotFoundException.class, () -> service.image("missing"));
        when(repo.findById("archived")).thenReturn(
            Optional.of(new Cookie.Builder().setArchived(true).build())
        );
        assertThrows(NotFoundException.class, () -> service.image("archived"));
        when(repo.findById("bad")).thenReturn(
            Optional.of(new Cookie.Builder().setImage("../outside.png").build())
        );
        assertThrows(NotFoundException.class, () -> service.image("bad"));
    }

    @Test
    void decodesTheStoresTransparentWebpPhotos() throws Exception {
        var repo = mock(CookieRepository.class);
        var service = new EmailAssetService(repo, root.toString());
        Files.copy(
            Path.of("frontend/public/img/cookies/double-chocolate-chunk.webp"),
            root.resolve("cookie.webp")
        );
        when(repo.findById("c1")).thenReturn(
            Optional.of(
                new Cookie.Builder()
                    .setCookieId("c1")
                    .setImage("cookie.webp")
                    .build()
            )
        );
        assertEquals(
            600,
            ImageIO.read(service.image("c1").toFile()).getWidth()
        );
    }
}
