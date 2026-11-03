package za.ac.cput.guiltfreecookie.service.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.guiltfreecookie.domain.CookieImage;
import za.ac.cput.guiltfreecookie.exception.BadRequestException;
import za.ac.cput.guiltfreecookie.exception.NotFoundException;
import za.ac.cput.guiltfreecookie.repository.CookieImageRepository;
import za.ac.cput.guiltfreecookie.repository.CookieRepository;
import za.ac.cput.guiltfreecookie.service.ImageStorageService;
import za.ac.cput.guiltfreecookie.util.Helper;

import java.util.HashSet;
import java.util.List;

@Service
public class CookieImageService {

    static final int MAX_IMAGES_PER_COOKIE = 10;
    static final int MAX_ALT_TEXT_LENGTH = 150;

    private final CookieImageRepository imageRepository;
    private final CookieRepository cookieRepository;
    private final ImageStorageService imageStorageService;

    public CookieImageService(CookieImageRepository imageRepository, CookieRepository cookieRepository,
                              ImageStorageService imageStorageService) {
        this.imageRepository = imageRepository;
        this.cookieRepository = cookieRepository;
        this.imageStorageService = imageStorageService;
    }

    public List<CookieImage> list(String cookieId) {
        return imageRepository.findByCookieIdOrderBySortOrderAsc(cookieId);
    }

    public CookieImage add(String cookieId, MultipartFile file, String altText) {
        if (!cookieRepository.existsById(cookieId)) {
            throw new NotFoundException("Cookie not found");
        }
        long count = imageRepository.countByCookieId(cookieId);
        if (count >= MAX_IMAGES_PER_COOKIE) {
            throw new BadRequestException("A cookie can have at most " + MAX_IMAGES_PER_COOKIE + " gallery photos");
        }
        String alt = Helper.clean(altText);
        if (!Helper.isWithin(alt, MAX_ALT_TEXT_LENGTH)) {
            throw new BadRequestException("Alt text must be at most " + MAX_ALT_TEXT_LENGTH + " characters");
        }
        String path;
        try {
            path = imageStorageService.store(file);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return imageRepository.save(new CookieImage.Builder()
                .setCookieId(cookieId)
                .setImage(path)
                .setAltText(alt)
                .setSortOrder((int) count)
                .build());
    }

    public void delete(String cookieId, String imageId) {
        CookieImage image = imageRepository.findById(imageId)
                .filter(found -> found.getCookieId().equals(cookieId))
                .orElseThrow(() -> new NotFoundException("Photo not found"));
        imageRepository.delete(image);
        imageStorageService.delete(image.getImage());
    }

    @Transactional
    public List<CookieImage> reorder(String cookieId, List<String> imageIds) {
        List<CookieImage> current = list(cookieId);
        if (imageIds == null || imageIds.size() != current.size()
                || !new HashSet<>(imageIds).equals(new HashSet<>(current.stream().map(CookieImage::getImageId).toList()))) {
            throw new BadRequestException("Send every photo id for this cookie exactly once");
        }
        for (CookieImage image : current) {
            imageRepository.save(new CookieImage.Builder().copy(image).setSortOrder(imageIds.indexOf(image.getImageId())).build());
        }
        return list(cookieId);
    }

    public void deleteAllFor(String cookieId) {
        for (CookieImage image : list(cookieId)) {
            imageRepository.delete(image);
            imageStorageService.delete(image.getImage());
        }
    }
}
