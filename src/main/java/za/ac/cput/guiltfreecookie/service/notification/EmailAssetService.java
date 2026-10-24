package za.ac.cput.guiltfreecookie.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// Product photos are transparent WebP, which Outlook and some other clients can't show.
// This composites the cookie onto an opaque cream tile (PNG) once and caches it under uploads/email/.
@Service
public class EmailAssetService {

    static final int TILE_WIDTH = 600;
    static final int TILE_HEIGHT = 750;
    private static final Color TILE_COLOUR = new Color(0xF5F0E6);
    private static final Color SHADOW_COLOUR = new Color(46, 14, 11, 2);

    private final CookieRepository cookieRepository;
    private final Path rootDir;

    public EmailAssetService(CookieRepository cookieRepository, @Value("${app.upload.root-dir}") String rootDir) {
        this.cookieRepository = cookieRepository;
        this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
    }

    public Path image(String cookieId) {
        Cookie cookie = cookieRepository.findById(cookieId)
                .filter(found -> !found.isArchived())
                .orElseThrow(() -> new NotFoundException("Cookie not found"));
        if (cookie.getImage() == null) {
            throw new NotFoundException("Cookie image not found");
        }
        Path source = rootDir.resolve(cookie.getImage()).normalize();
        if (!source.startsWith(rootDir) || !Files.isRegularFile(source)) {
            throw new NotFoundException("Cookie image not found");
        }
        try {
            if (!source.toRealPath().startsWith(rootDir.toRealPath())) {
                throw new NotFoundException("Cookie image not found");
            }
            Path folder = rootDir.resolve("email");
            Path target = folder.resolve(cacheKey(cookie.getImage(), source) + ".png");
            if (Files.exists(target)) {
                return target;
            }
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null) {
                throw new NotFoundException("Cookie image cannot be decoded");
            }
            Files.createDirectories(folder);
            Path temp = Files.createTempFile(folder, "tile-", ".png");
            try {
                ImageIO.write(tile(original), "png", temp.toFile());
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Changes whenever the source file is replaced, so a re-uploaded photo gets a fresh tile.
    private static String cacheKey(String imagePath, Path source) throws IOException {
        String fingerprint = imagePath + Files.getLastModifiedTime(source) + Files.size(source);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BufferedImage tile(BufferedImage original) {
        BufferedImage tile = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(TILE_COLOUR);
        g.fillRect(0, 0, TILE_WIDTH, TILE_HEIGHT);
        for (int i = 30; i > 0; i--) {
            g.setColor(SHADOW_COLOUR);
            g.fillOval(105 - i, 535 - i / 2, 390 + 2 * i, 55 + i);
        }
        double scale = Math.min(500.0 / original.getWidth(), 570.0 / original.getHeight());
        int width = (int) (original.getWidth() * scale);
        int height = (int) (original.getHeight() * scale);
        g.drawImage(original, (TILE_WIDTH - width) / 2, (710 - height) / 2, width, height, null);
        g.dispose();
        return tile;
    }
}
