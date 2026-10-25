package za.ac.cput.guiltfreecookie.service.flow;

import za.ac.cput.guiltfreecookie.domain.EmailFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The words in every flow email. Plain and warm, no health claims, and reviews only ever come
 * from real approved reviews (the block disappears when there are none).
 */
public final class FlowCopy {

    private FlowCopy() {
    }

    public record Message(String subject, Map<String, Object> model) {
    }

    // link wraps every storefront URL (click tracking); previews pass it through unchanged.
    public static Message compose(EmailFlow flow, int step, FlowData data, String storefront, UnaryOperator<String> link) {
        Draft draft = switch (flow) {
            case WELCOME -> welcome(step, data);
            case BROWSE_ABANDONMENT -> browse(step, data);
            case CART_ABANDONMENT -> cart(step, data);
            case POST_PURCHASE -> postPurchase(step);
            case REPLENISHMENT -> replenishment(data);
            case WINBACK -> winback(step, data);
            case SUNSET -> sunset(step);
        };
        Map<String, Object> model = new HashMap<>();
        model.put("preheader", draft.preheader);
        model.put("headline", draft.headline);
        model.put("paragraphs", draft.paragraphs);
        model.put("bullets", draft.bullets);
        model.put("closing", draft.closing);
        model.put("showFounder", draft.founder);
        model.put("products", draft.products ? links(data.products(), link) : List.of());
        model.put("cartItems", draft.items ? data.items() : List.of());
        model.put("reviews", draft.reviews ? data.reviews() : List.of());
        boolean code = draft.code && data.discountCode() != null;
        model.put("discountCode", code ? data.discountCode() : null);
        model.put("discountExpiry", code ? data.discountExpiry() : null);
        String target = draft.usesPrimary && data.primaryUrl() != null ? data.primaryUrl() : storefront + "/products";
        model.put("actionUrl", link.apply(target));
        model.put("actionLabel", draft.action);
        model.put("whyReceived", why(flow));
        return new Message(draft.subject, model);
    }

    private static Draft welcome(int step, FlowData data) {
        int p = data.discountPercent();
        boolean code = data.discountCode() != null;
        return switch (step) {
            case 0 -> new Draft(code ? "Welcome! Here's " + p + "% off your first order" : "Welcome to GuiltFreeCookie",
                    "A small Cape Town bakery that bakes every order fresh.", "Welcome in")
                    .say("Thanks for joining us. We're a small bakery in Strandfontein, Cape Town, and we bake every order fresh after you place it.")
                    .say(code ? "Here's " + p + "% off your first order. The code is yours alone and works once." : null)
                    .code().founder().action("Choose your cookies");
            case 1 -> new Draft("Where to start", "A few of our favourites to begin with.", "Our favourites")
                    .say("Not sure where to start? These are a few of the cookies we're proudest of.")
                    .say("Every cookie lists its ingredients and allergens on its page, so you can check before you choose.")
                    .products().action("See all cookies");
            case 2 -> new Draft("How we bake your order", "Nothing sits on a shelf.", "Baked to order")
                    .say("We don't bake ahead. When you order, we bake, pack and send your cookies.")
                    .say("That's why our delivery times are estimates: fresh takes a day or two.")
                    .facts(data.deliveryFacts()).action("Shop cookies");
            case 3 -> new Draft("Made for sharing", "Singles, boxes and gifts.", "Made for sharing")
                    .say("Our cookies come as singles or in boxes, which makes them easy to send as a gift or bring to the office.")
                    .say(data.reviews().isEmpty() ? null : "Here's what a few customers have said.")
                    .reviews().products().action("Browse boxes");
            default -> new Draft("Your " + p + "% off ends tomorrow", "Your welcome code is nearly up.", "Last call for your code")
                    .say("A quick reminder: your welcome code ends tomorrow. It's still yours if you'd like it.")
                    .code().action("Use my code");
        };
    }

    private static Draft browse(int step, FlowData data) {
        String name = data.cookieName();
        if (step == 0) {
            return new Draft("Still thinking about " + name + "?", "Here it is again.", "Still thinking it over?")
                    .say("You were looking at " + name + ". Here it is again, in case you'd like another look.")
                    .say("Its ingredients and allergens are on its page.")
                    .products().primary().action("View " + name);
        }
        return new Draft("A few more you might like", "From the same shelf as " + name + ".", "You might also like")
                .say("If " + name + " wasn't quite right, here are a few others like it.")
                .reviews().products().action("Browse cookies");
    }

    private static Draft cart(int step, FlowData data) {
        int p = data.discountPercent();
        return switch (step) {
            case 0 -> new Draft("Your cookies are still in your cart", "We saved your cart for you.", "Your cart is saved")
                    .say("You left a few cookies in your cart. They're saved, so you can pick up where you left off.")
                    .items().primary().action("Back to my cart");
            case 1 -> {
                List<String> facts = new ArrayList<>(data.deliveryFacts());
                facts.add("We bake after you order, so delivery dates are estimates.");
                facts.add("Ingredients and allergens are on every cookie's page.");
                facts.add("You pay on PayFast's secure page: card, Instant EFT, SnapScan and more.");
                yield new Draft("Any questions before you order?", "A few things people often ask.", "Good to know")
                        .say("If something held you back, this might help.")
                        .facts(facts)
                        .close("Still unsure? Reply to this email and we'll help.")
                        .primary().action("Back to my cart");
            }
            default -> new Draft(data.discountCode() != null ? p + "% off the cookies in your cart" : "Your cart is still waiting",
                    "Your cart is still saved.", "A little nudge")
                    .say(data.discountCode() != null
                            ? "Your cart is still saved. Here's " + p + "% off if you'd like to finish your order. The code works once and ends in 3 days."
                            : "Your cart is still saved whenever you're ready.")
                    .code().items().primary().action("Finish my order");
        };
    }

    private static Draft postPurchase(int step) {
        if (step == 0) {
            return new Draft("Keeping your cookies at their best", "A few storage tips.", "Keep them fresh")
                    .say("We hope your cookies arrived safely. A few tips to enjoy them at their best:")
                    .facts(List.of(
                            "Keep them in an airtight container at room temperature, out of direct sun.",
                            "Check the label for the best-before date.",
                            "For that just-baked feel, warm one for a few seconds in the microwave."))
                    .action("Shop again");
        }
        return new Draft("How were your cookies?", "Tell other shoppers what you thought.", "How did we do?")
                .say("If you have a minute, tell other shoppers what you thought. We read every review and check it before it goes up.")
                .primary().action("Write a review");
    }

    private static Draft replenishment(FlowData data) {
        return new Draft("Time for a refill?", "Your last order, one tap away.", "Running low?")
                .say("It's been about " + data.replenishmentDays() + " days since your cookies arrived. Want the same again?")
                .say("One tap puts your last order back in your cart, and you can change it before you pay.")
                .items().primary().action("Reorder");
    }

    private static Draft winback(int step, FlowData data) {
        int p = data.discountPercent();
        return switch (step) {
            case 0 -> new Draft("It's been a while", "Here's what's new in our kitchen.", "We've been baking")
                    .say("It's been a while since your last order, so here's what's been coming out of our oven.")
                    .products().action("See what's new");
            case 1 -> new Draft("A few favourites", "From our kitchen to your door.", "Still tempted?")
                    .say("A few favourites from our kitchen, baked fresh when you order.")
                    .reviews().products().action("Browse cookies");
            default -> new Draft(data.discountCode() != null ? p + "% off, because we miss you" : "We'd love to bake for you again",
                    "A treat for your next order.", "Come back for a treat")
                    .say(data.discountCode() != null
                            ? "Here's " + p + "% off your next order. It's yours alone, works once and ends in 7 days."
                            : "Whenever you're ready, we'd love to bake for you again.")
                    .code().action(data.discountCode() != null ? "Use my code" : "Browse cookies");
        };
    }

    private static Draft sunset(int step) {
        return switch (step) {
            case 0 -> new Draft("Do you still want our emails?", "We don't want to crowd your inbox.", "Still with us?")
                    .say("We haven't seen you open our emails in a while, and we don't want to crowd your inbox.")
                    .say("If you'd still like bakery news and offers, tap below. If not, you don't need to do anything.")
                    .primary().action("Yes, keep me subscribed");
            case 1 -> new Draft("In case you missed it", "A few favourites.", "What's been baking")
                    .say("A quick look at a few favourites, in case one tempts you.")
                    .products().action("See all cookies");
            default -> new Draft("We'll stop emailing you soon", "Unless you'd like to stay.", "Last one from us")
                    .say("Unless you tap below, this is the last marketing email we'll send you.")
                    .say("Order updates still arrive as usual, and you can rejoin any time on our website.")
                    .primary().action("Keep me subscribed");
        };
    }

    private static String why(EmailFlow flow) {
        return switch (flow) {
            case WELCOME, WINBACK, SUNSET -> "You're getting this because you signed up for bakery news.";
            case BROWSE_ABANDONMENT, CART_ABANDONMENT -> "You're getting this because you started an order with us or signed up for bakery news.";
            case POST_PURCHASE, REPLENISHMENT -> "You're getting this because you ordered from us.";
        };
    }

    private static List<Map<String, Object>> links(List<Map<String, Object>> products, UnaryOperator<String> link) {
        return products.stream().<Map<String, Object>>map(product -> {
            Map<String, Object> copy = new HashMap<>(product);
            copy.put("url", link.apply(String.valueOf(product.get("url"))));
            return copy;
        }).toList();
    }

    private static final class Draft {
        private final String subject;
        private final String preheader;
        private final String headline;
        private final List<String> paragraphs = new ArrayList<>();
        private List<String> bullets = List.of();
        private boolean products;
        private boolean items;
        private boolean reviews;
        private boolean code;
        private boolean founder;
        private boolean usesPrimary;
        private String closing;
        private String action;

        private Draft(String subject, String preheader, String headline) {
            this.subject = subject;
            this.preheader = preheader;
            this.headline = headline;
        }

        private Draft say(String paragraph) {
            if (paragraph != null) {
                paragraphs.add(paragraph);
            }
            return this;
        }

        private Draft facts(List<String> facts) {
            this.bullets = facts;
            return this;
        }

        private Draft close(String closing) {
            this.closing = closing;
            return this;
        }

        private Draft products() {
            this.products = true;
            return this;
        }

        private Draft items() {
            this.items = true;
            return this;
        }

        private Draft reviews() {
            this.reviews = true;
            return this;
        }

        private Draft code() {
            this.code = true;
            return this;
        }

        private Draft founder() {
            this.founder = true;
            return this;
        }

        private Draft primary() {
            this.usesPrimary = true;
            return this;
        }

        private Draft action(String label) {
            this.action = label;
            return this;
        }
    }
}
