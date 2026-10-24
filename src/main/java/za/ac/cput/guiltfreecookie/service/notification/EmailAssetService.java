package za.ac.cput.guiltfreecookie.service.notification;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;

@Service
public class EmailAssetService {

    private final CookieRepository cookies;
    private final Path root;

    public EmailAssetService(
        CookieRepository cookies,
        @Value("${app.upload.root-dir}") String root
    ) {
        this.cookies = cookies;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public Path image(String id) {
        var cookie = cookies
            .findById(id)
            .filter(c -> !c.isArchived())
            .orElseThrow(() -> new NotFoundException("Cookie not found"));
        if (cookie.getImage() == null) throw new NotFoundException(
            "Cookie image not found"
        );
        Path source = root.resolve(cookie.getImage()).normalize();
        if (
            !source.startsWith(root) || !Files.isRegularFile(source)
        ) throw new NotFoundException("Cookie image not found");
        try {
            if (
                !source.toRealPath().startsWith(root.toRealPath())
            ) throw new NotFoundException("Cookie image not found");
            String key = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                    (
                        cookie.getImage() +
                        Files.getLastModifiedTime(source) +
                        Files.size(source)
                    ).getBytes(StandardCharsets.UTF_8)
                )
            );
            Path folder = root.resolve("email"),
                target = folder.resolve(key + ".png");
            if (Files.exists(target)) return target;
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null) throw new NotFoundException(
                "Cookie image cannot be decoded"
            );
            BufferedImage tile = new BufferedImage(
                600,
                750,
                BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = tile.createGraphics();
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g.setColor(new Color(0xF5F0E6));
            g.fillRect(0, 0, 600, 750);
            for (int i = 30; i > 0; i--) {
                g.setColor(new Color(46, 14, 11, 2));
                g.fillOval(105 - i, 535 - i / 2, 390 + 2 * i, 55 + i);
            }
            double scale = Math.min(
                500.0 / original.getWidth(),
                570.0 / original.getHeight()
            );
            int w = (int) (original.getWidth() * scale),
                h = (int) (original.getHeight() * scale);
            g.drawImage(original, (600 - w) / 2, (710 - h) / 2, w, h, null);
            g.dispose();
            Files.createDirectories(folder);
            Path temp = Files.createTempFile(folder, "tile-", ".png");
            try {
                ImageIO.write(tile, "png", temp.toFile());
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
