/**
 * Mock cookie catalog — CrumbleMagic
 * 18 cookies across 5 categories: Chocolate, Stuffed, Classic, Seasonal, Vegan
 *
 * Each cookie matches the canonical schema:
 * { id, name, description, price, rating, category, ingredients, image, isFeatured, badges, reviews, stock }
 *
 * Images use Unsplash with `?w=800&q=80&auto=format&fit=crop` for crisp retina renders.
 */

export const cookies = [
  {
    id: 1,
    name: 'Caramel Silk Melt Cookie',
    description:
      'A rich, fudgy brownie cookie with a molten caramel centre that melts the moment you break it open. Topped with a whisper of sea salt — true chocolate lovers, this one owns your heart.',
    price: 5.99,
    rating: 4.9,
    category: 'Stuffed',
    ingredients: [
      'Belgian dark chocolate',
      'Salted caramel',
      'French butter',
      'Flaked sea salt',
      'Vanilla bean',
    ],
    image: '/cookie3.png',
    isFeatured: true,
    badges: ['Bestseller', 'Molten Centre'],
    stock: 48,
    reviews: [
      {
        id: 1,
        author: 'Rusiru Devinda',
        rating: 5,
        date: '2025.11.18',
        text: 'The moment you bite in, the caramel just melts. Absolutely divine — worth every calorie.',
      },
      {
        id: 2,
        author: 'Aisha Nair',
        rating: 5,
        date: '2025.10.02',
        text: 'The salted caramel centre is unreal. I ordered six more the next day.',
      },
    ],
  },
  {
    id: 2,
    name: 'Velvet Choco Swirl',
    description:
      'A tender cocoa dough rippled with white and milk chocolate. Soft-baked, glossy cracked tops, and a deep chocolate intensity that lingers like a fond memory.',
    price: 5.49,
    rating: 4.8,
    category: 'Chocolate',
    ingredients: [
      'Cocoa powder',
      'Milk chocolate',
      'White chocolate',
      'Cane sugar',
      'Brown butter',
    ],
    image: '/cookie.png',
    isFeatured: true,
    badges: ['Crowd Favourite'],
    stock: 60,
    reviews: [
      {
        id: 1,
        author: 'Maya Fernando',
        rating: 5,
        date: '2025.11.01',
        text: 'Triple chocolate but never cloying. The texture is cloud-soft.',
      },
    ],
  },
  {
    id: 3,
    name: 'Peanut Butter Jelly Delight',
    description:
      'Warm, nutty, melt-in-the-middle peanut butter cookie with a hidden strawberry jam core. A nostalgic lunchbox favourite, reimagined for grown-up palates.',
    price: 6.99,
    rating: 4.9,
    category: 'Stuffed',
    ingredients: [
      'Roasted peanut butter',
      'Strawberry jam',
      'Light brown sugar',
      'Eggs',
      'Sea salt',
    ],
    image:
      'https://images.unsplash.com/photo-1598373182133-52452f7691ef?w=800&q=80&auto=format&fit=crop',
    isFeatured: true,
    badges: ['Nostalgic'],
    stock: 36,
    reviews: [
      {
        id: 1,
        author: 'Liam Park',
        rating: 5,
        date: '2025.09.14',
        text: 'PB&J in a cookie. Pure genius. The jam oozes perfectly.',
      },
    ],
  },
  {
    id: 4,
    name: 'Salted Caramel Chip',
    description:
      'Brown butter dough loaded with caramel chips and finished with flaky Maldon salt. Buttery, chewy, and dangerously moreish — impossible to stop at one.',
    price: 5.99,
    rating: 4.7,
    category: 'Classic',
    ingredients: [
      'Brown butter',
      'Caramel chips',
      'Maldon sea salt',
      'Dark muscovado',
      'Madagascar vanilla',
    ],
    image:
      'https://images.unsplash.com/photo-1559054663-e8d23213f55c?w=800&q=80&auto=format&fit=crop',
    isFeatured: true,
    badges: ['House Classic'],
    stock: 72,
    reviews: [
      {
        id: 1,
        author: 'Sara Okafor',
        rating: 5,
        date: '2025.10.20',
        text: 'The brown butter smell is intoxicating. Salt balance is perfect.',
      },
    ],
  },
  {
    id: 5,
    name: 'White Cream Toffee Delight',
    description:
      'A creamy white chocolate and toffee chip cookie with a delicate crunch on the edge and a soft, cloud-like middle. Drizzled with white chocolate for a finishing flourish.',
    price: 6.49,
    rating: 4.8,
    category: 'Chocolate',
    ingredients: ['White chocolate', 'Toffee bits', 'Cream cheese', 'Vanilla bean', 'Butter'],
    image:
      'https://images.unsplash.com/photo-1606312619070-d48b4c652a52?w=800&q=80&auto=format&fit=crop',
    isFeatured: true,
    badges: ['Creamy'],
    stock: 42,
    reviews: [
      {
        id: 1,
        author: 'Noah Bennett',
        rating: 5,
        date: '2025.11.05',
        text: 'So creamy and the toffee adds the perfect crunch. Heaven in a bite.',
      },
    ],
  },
  {
    id: 6,
    name: 'Triple Choc Lava',
    description:
      'Dark chocolate dough wrapped around a molten 70% Valrhona ganache core. Best served slightly warm — break it open and watch the lava flow.',
    price: 6.99,
    rating: 5.0,
    category: 'Stuffed',
    ingredients: [
      'Valrhona 70% chocolate',
      'Cocoa powder',
      'Heavy cream',
      'Dark chocolate chips',
      'Butter',
    ],
    image:
      'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=800&q=80&auto=format&fit=crop',
    isFeatured: true,
    badges: ['Molten Centre', 'Bestseller'],
    stock: 30,
    reviews: [
      {
        id: 1,
        author: 'Elena Whitfield',
        rating: 5,
        date: '2025.11.22',
        text: 'The lava flow is mesmerising. Richest cookie I have ever had.',
      },
    ],
  },
  {
    id: 7,
    name: 'Oatmeal Raisin Comfort',
    description:
      'Old-fashioned rolled oats, plump flame raisins, and a hint of cinnamon. Chewy, hearty, and comforting — exactly like grandma used to make.',
    price: 4.99,
    rating: 4.6,
    category: 'Classic',
    ingredients: ['Rolled oats', 'Flame raisins', 'Ceylon cinnamon', 'Brown sugar', 'Butter'],
    image:
      'https://images.unsplash.com/photo-1598373182133-52452f7691ef?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Heirloom Recipe'],
    stock: 55,
    reviews: [
      {
        id: 1,
        author: 'Grace Holloway',
        rating: 4,
        date: '2025.08.30',
        text: 'Tastes like Sunday afternoons at my nan’s house. So comforting.',
      },
    ],
  },
  {
    id: 8,
    name: 'Pumpkin Spice Seasonal',
    description:
      'Autumn in a cookie. Soft pumpkin-spiced dough with white chocolate chips and a buttery maple glaze. Available only through the fall season.',
    price: 6.49,
    rating: 4.8,
    category: 'Seasonal',
    ingredients: ['Pumpkin puree', 'Pumpkin spice', 'White chocolate', 'Maple syrup', 'Pecans'],
    image:
      'https://images.unsplash.com/photo-1573812461383-e5f8b759d12e?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Limited Edition', 'Autumn'],
    stock: 28,
    reviews: [
      {
        id: 1,
        author: 'Dylan Carter',
        rating: 5,
        date: '2025.10.10',
        text: 'Pumpkin spice done right — not overpowering, perfectly spiced.',
      },
    ],
  },
  {
    id: 9,
    name: 'Vegan Choc Chip',
    description:
      'All the comfort, none of the dairy. Coconut oil and almond milk create a chewy, golden dough studded with dairy-free dark chocolate chunks.',
    price: 5.99,
    rating: 4.7,
    category: 'Vegan',
    ingredients: [
      'Coconut oil',
      'Almond milk',
      'Vegan dark chocolate',
      'Coconut sugar',
      'Spelt flour',
    ],
    image:
      'https://images.unsplash.com/photo-1607920591413-4ec007e70023?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Vegan', 'Dairy-Free'],
    stock: 40,
    reviews: [
      {
        id: 1,
        author: 'Priya Anand',
        rating: 5,
        date: '2025.09.25',
        text: 'Finally a vegan cookie that doesn’t taste vegan. So chewy and rich.',
      },
    ],
  },
  {
    id: 10,
    name: 'Matcha White Choc',
    description:
      'Stone-ground ceremonial matcha folded into a buttery dough with premium white chocolate chunks. Earthy, sweet, and impossibly green.',
    price: 6.99,
    rating: 4.6,
    category: 'Classic',
    ingredients: ['Ceremonial matcha', 'White chocolate', 'Butter', 'Cane sugar', 'Cake flour'],
    image:
      'https://images.unsplash.com/photo-1612203985729-70726954388c?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Artisan'],
    stock: 33,
    reviews: [
      {
        id: 1,
        author: 'Kenji Tanaka',
        rating: 4,
        date: '2025.07.18',
        text: 'Lovely matcha flavour. Wish it was a touch less sweet.',
      },
    ],
  },
  {
    id: 11,
    name: 'Red Velvet Crumble',
    description:
      'Deep crimson cocoa dough with white chocolate chips and a tangy cream cheese glaze. Soft, cakey, and unmistakably velvet.',
    price: 6.49,
    rating: 4.7,
    category: 'Classic',
    ingredients: [
      'Cocoa powder',
      'Red velvet emulsion',
      'White chocolate',
      'Cream cheese',
      'Buttermilk',
    ],
    image:
      'https://images.unsplash.com/photo-1481391319762-47dff72954d9?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Signature'],
    stock: 38,
    reviews: [
      {
        id: 1,
        author: 'Hannah Lee',
        rating: 5,
        date: '2025.10.15',
        text: 'Red velvet in cookie form — pure genius. Cream cheese glaze is everything.',
      },
    ],
  },
  {
    id: 12,
    name: 'Snickerdoodle Cloud',
    description:
      'Pillowy soft snickerdoodle rolled in cinnamon sugar, with crackly tops and a tender, cakey crumb. Pure cinnamon-sugar comfort.',
    price: 4.99,
    rating: 4.8,
    category: 'Classic',
    ingredients: ['Cinnamon', 'Cream of tartar', 'Cane sugar', 'Butter', 'Eggs'],
    image:
      'https://images.unsplash.com/photo-1590080875515-8a3a8dc57f5e?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Pillowy Soft'],
    stock: 65,
    reviews: [
      {
        id: 1,
        author: 'Marcus Reed',
        rating: 5,
        date: '2025.09.10',
        text: 'The crackly cinnamon top is addictive. Goes perfectly with coffee.',
      },
    ],
  },
  {
    id: 13,
    name: 'Biscoff Stuffed Wonder',
    description:
      'Brown butter dough wrapped around a generous scoop of melted Lotus Biscoff spread. Crisp edges, soft middle, spiced caramel bliss.',
    price: 6.99,
    rating: 4.9,
    category: 'Stuffed',
    ingredients: ['Lotus Biscoff spread', 'Brown butter', 'Speculoos crumbs', 'Cane sugar', 'Eggs'],
    image:
      'https://images.unsplash.com/photo-1604152135912-04a022e23696?w=800&q=80&auto=format&fit=crop',
    isFeatured: true,
    badges: ['Molten Centre', 'Bestseller'],
    stock: 32,
    reviews: [
      {
        id: 1,
        author: 'Tara Singh',
        rating: 5,
        date: '2025.11.10',
        text: 'Biscoff lovers, this is your moment. The melt is dreamy.',
      },
    ],
  },
  {
    id: 14,
    name: 'Lemon Lavender Glow',
    description:
      'Bright Sicilian lemon zest and culinary lavender folded into a delicate butter cookie. Topped with a lemon sugar glaze — floral, citrusy, refreshing.',
    price: 5.99,
    rating: 4.5,
    category: 'Seasonal',
    ingredients: ['Sicilian lemon', 'Culinary lavender', 'Lemon glaze', 'Butter', 'Powdered sugar'],
    image:
      'https://images.unsplash.com/photo-1559054663-e8d23213f55c?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Floral', 'Spring'],
    stock: 25,
    reviews: [
      {
        id: 1,
        author: 'Violet Adams',
        rating: 4,
        date: '2025.04.22',
        text: 'So delicate and fragrant. The lavender is subtle, not soapy at all.',
      },
    ],
  },
  {
    id: 15,
    name: 'Vegan Oat Cranberry',
    description:
      'Hearty rolled oats, tart cranberries, and a hint of orange zest, bound with maple syrup and coconut oil. A wholesome vegan treat for any time of day.',
    price: 5.49,
    rating: 4.6,
    category: 'Vegan',
    ingredients: ['Rolled oats', 'Cranberries', 'Orange zest', 'Maple syrup', 'Coconut oil'],
    image:
      'https://images.unsplash.com/photo-1607920591413-4ec007e70023?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Vegan', 'Wholesome'],
    stock: 44,
    reviews: [
      {
        id: 1,
        author: 'Sam Rivera',
        rating: 5,
        date: '2025.08.05',
        text: 'The orange zest makes it. Perfect with afternoon tea.',
      },
    ],
  },
  {
    id: 16,
    name: "S'mores Campfire",
    description:
      'A graham cracker dough loaded with toasted marshmallow and milk chocolate chunks. Tastes like summer nights by the campfire — without the mosquito bites.',
    price: 6.99,
    rating: 4.8,
    category: 'Seasonal',
    ingredients: ['Graham crackers', 'Toasted marshmallow', 'Milk chocolate', 'Honey', 'Butter'],
    image:
      'https://images.unsplash.com/photo-1606312619070-d48b4c652a52?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Summer Special'],
    stock: 30,
    reviews: [
      {
        id: 1,
        author: 'Chloe Mason',
        rating: 5,
        date: '2025.07.04',
        text: 'Campfire nostalgia in a cookie. The marshmallow pull is gorgeous.',
      },
    ],
  },
  {
    id: 17,
    name: 'Espresso Dark Choc',
    description:
      'Double-shot espresso dough with shards of 70% dark chocolate. A grown-up cookie with a serious caffeine kick — for the connoisseur who takes their cookies seriously.',
    price: 6.49,
    rating: 4.8,
    category: 'Chocolate',
    ingredients: ['Espresso', '70% dark chocolate', 'Cocoa powder', 'Muscovado sugar', 'Butter'],
    image:
      'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Barista Pick'],
    stock: 36,
    reviews: [
      {
        id: 1,
        author: 'Owen Phillips',
        rating: 5,
        date: '2025.10.28',
        text: 'Coffee and chocolate perfection. My afternoon ritual now.',
      },
    ],
  },
  {
    id: 18,
    name: 'Vegan Double Choc',
    description:
      'Decadent double-cocoa vegan dough with dairy-free dark chocolate chunks. Fudgy, intense, and undetectably vegan — even the dairy lovers come back for more.',
    price: 6.49,
    rating: 4.7,
    category: 'Vegan',
    ingredients: [
      'Cocoa powder',
      'Vegan dark chocolate',
      'Coconut oil',
      'Coconut sugar',
      'Oat milk',
    ],
    image:
      'https://images.unsplash.com/photo-1607920591413-4ec007e70023?w=800&q=80&auto=format&fit=crop',
    isFeatured: false,
    badges: ['Vegan', 'Intense Cocoa'],
    stock: 39,
    reviews: [
      {
        id: 1,
        author: 'Nina Petrov',
        rating: 5,
        date: '2025.09.18',
        text: 'Would never guess this is vegan. So fudgy and rich.',
      },
    ],
  },
]

export const categories = [
  { id: 'all', label: 'All', icon: '🪄' },
  { id: 'Chocolate', label: 'Chocolate', icon: '🍫' },
  { id: 'Stuffed', label: 'Stuffed', icon: '💧' },
  { id: 'Classic', label: 'Classic', icon: '🍪' },
  { id: 'Seasonal', label: 'Seasonal', icon: '🍂' },
  { id: 'Vegan', label: 'Vegan', icon: '🌿' },
]

export const sortOptions = [
  { id: 'popular', label: 'Most Popular' },
  { id: 'price-low', label: 'Price: Low to High' },
  { id: 'price-high', label: 'Price: High to Low' },
  { id: 'rating', label: 'Highest Rated' },
]

export const testimonials = [
  {
    id: 1,
    name: 'Amara Bishop',
    role: 'Verified Customer',
    avatar:
      'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80&auto=format&fit=crop',
    rating: 5,
    text: 'CrumbleMagic turned our anniversary into a moment. The caramel melt cookies arrived warm-packaged and absolutely divine. This is what premium tastes like.',
  },
  {
    id: 2,
    name: 'Theodore Quinn',
    role: 'Coffee Shop Owner',
    avatar:
      'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&q=80&auto=format&fit=crop',
    rating: 5,
    text: 'We stock CrumbleMagic at our cafe and customers drive across town for them. Consistent quality, beautiful packaging, and that signature soft-baked texture.',
  },
  {
    id: 3,
    name: 'Isabella Cruz',
    role: 'Food Blogger',
    avatar:
      'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&q=80&auto=format&fit=crop',
    rating: 5,
    text: 'I have reviewed dozens of cookie brands. None come close to the texture and flavour balance at CrumbleMagic. The stuffed series is genuinely art.',
  },
  {
    id: 4,
    name: 'Marcus Lin',
    role: 'Verified Customer',
    avatar:
      'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80&auto=format&fit=crop',
    rating: 5,
    text: 'Ordered the variety box for my team. Five minutes after opening, the box was empty and I was the office hero. They have not stopped talking about it.',
  },
  {
    id: 5,
    name: 'Priya Anand',
    role: 'Verified Customer',
    avatar:
      'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&q=80&auto=format&fit=crop',
    rating: 5,
    text: 'As a vegan I usually feel like an afterthought at bakeries. CrumbleMagic’s vegan range is genuinely the best — I had to check the box twice to confirm it was dairy-free.',
  },
]

export const faqs = [
  {
    id: 1,
    question: 'How long do your cookies stay fresh after purchase?',
    answer:
      'Our cookies stay fresh for 5–7 days when stored in an airtight container at room temperature. For longer storage, they can be frozen for up to 3 months — just warm them in a 160°C oven for 4 minutes and they taste freshly baked.',
  },
  {
    id: 2,
    question: 'Do you use any artificial flavours or preservatives?',
    answer:
      'Never. Every CrumbleMagic cookie is made with 100% natural ingredients — real Belgian chocolate, French butter, Madagascan vanilla, and stone-ground spices. No preservatives, no artificial flavourings, no shortcuts.',
  },
  {
    id: 3,
    question: 'How are the cookies packaged to stay fresh during delivery?',
    answer:
      'Each cookie is individually sealed in food-grade kraft pouches to lock in freshness, then packed in a protective CrumbleMagic gift box with thermal padding for temperature-sensitive items. We use carbon-neutral couriers for all deliveries.',
  },
  {
    id: 4,
    question: 'Do you offer bulk orders for parties, weddings, or corporate events?',
    answer:
      'Absolutely. We create bespoke cookie bars and gift boxes for weddings, corporate events, and parties of any size. Minimum order is 50 cookies, with 7 days notice. Reach out via our contact form for a tailored quote and tasting box.',
  },
  {
    id: 5,
    question: 'Do you have eggless or gluten-free cookie options?',
    answer:
      'Yes — our Vegan range is naturally eggless, and we offer a rotating gluten-free selection baked in a dedicated GF kitchen. Look for the Vegan and Gluten-Free badges on each product card, or filter by category on the Menu page.',
  },
]
