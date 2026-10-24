package za.ac.cput.guiltfreecookie.dto.engagement;

public record SiteInfo(
        String supportEmail,
        String phone,
        String whatsapp,
        String hours,
        String responseTime,
        String address,
        Social social,
        Stats stats) {

    public record Social(String instagram, String facebook, String tiktok) {
    }

    // customersServed and cookiesSold count paid, unrefunded orders (cookies are individual cookies, not packs).
    public record Stats(Double averageRating, long reviewCount, Long customersServed, Long cookiesSold) {
    }
}
