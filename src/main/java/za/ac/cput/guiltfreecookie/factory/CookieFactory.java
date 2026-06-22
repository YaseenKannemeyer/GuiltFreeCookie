package za.ac.cput.guiltfreecookie.factory;


import za.ac.cput.guiltfreecookie.domain.Cookie;
import za.ac.cput.guiltfreecookie.domain.CookieCategory;
import za.ac.cput.guiltfreecookie.util.Helper;

public class CookieFactory {

    public static Cookie createCookie(
            String cookieId,
            CookieCategory category,
            String description,
            String ingredients,
            String allergies,
            Double price) {

        if (Helper.isNullOrEmpty(cookieId)
                || Helper.isNullOrEmpty(description)
                || Helper.isNullOrEmpty(ingredients)
                || Helper.isNullOrEmpty(allergies)
                || price <= 0 ) {
            return null;
        }

        return new Cookie.Builder()
                .setCookieId(cookieId)
                .setCategory(category)
                .setDescription(description)
                .setIngredients(ingredients)
                .setAllergies(allergies)
                .setPrice(price)
                .build();
    }
}
