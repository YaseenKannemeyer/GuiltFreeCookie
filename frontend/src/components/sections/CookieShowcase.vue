<script setup>
import BaseButton from "../../components/ui/BaseButton.vue";

const cookies = [
  {
    id: 1,
    title: "Chocolate Chip",
    description:
      "Our signature chocolate chip cookie made with Belgian chocolate and real butter.",
    image: "/cookie.png",
    color: "#E8992D",
  },
  {
    id: 2,
    title: "Red Velvet",
    description: "A rich red velvet cookie with creamy white chocolate chunks.",
    image: "/cookie2.png",
    color: "#C24164",
  },
  {
    id: 3,
    title: "Caramel Crunch",
    description:
      "Golden caramel with crunchy biscuit pieces baked to perfection.",
    image: "/cookie3.png",
    color: "#D97706",
  },
  {
    id: 4,
    title: "Double Chocolate",
    description: "For serious chocolate lovers with a soft gooey centre.",
    image: "/cookie4.png",
    color: "#5B3A29",
  },
];
</script>

<template>
  <section class="py-1 bg-[#d0ab7b]">
    <h2
      class="text-[#35150D] text-center font-semibold leading-tight text-3xl sm:text-4xl lg:text-5xl py-16"
    >
      Hot Picks This Week
    </h2>
    <div class="mx-auto max-w-7xl px-6 lg:px-12">
      <article
        v-for="(cookie, index) in cookies"
        :key="cookie.id"
        class="cookie-card"
        :class="{ reverse: index % 2 === 1 }"
        :style="{ '--hover-color': cookie.color }"
      >
        <!-- Text -->
        <div class="cookie-content">
          <span class="cookie-badge">Freshly Baked</span>

          <h2 class="cookie-title">
            {{ cookie.title }}
          </h2>

          <p class="cookie-desc">
            {{ cookie.description }}
          </p>
          <div class="cookie-buttons">
            <!-- Desktop -->
            <BaseButton variant="outline" class="hidden md:inline-flex"
              >Learn More</BaseButton
            >

            <!-- Desktop -->
            <BaseButton variant="primary" class="hidden md:inline-flex"
              >Order Now</BaseButton
            >

            <!-- Mobile -->
            <button class="md:hidden order-link">Order Now →</button>
          </div>
        </div>

        <!-- Image -->
        <div class="cookie-image-wrap">
          <img :src="cookie.image" :alt="cookie.title" class="cookie-big" />
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.cookie-card {
  position: relative;
  display: flex;
  align-items: center;
  background: #e3d9c8;
  border-radius: 40px;
  width: 100%;
  height: 420px;
  margin-bottom: 250px;
  padding: 50px;
  overflow: visible;
  transition: 0.35s;
}

.cookie-card:hover {
  background: var(--hover-color);
}

.reverse {
  flex-direction: row-reverse;
}

.cookie-content {
  flex: 1;
  z-index: 2;
  max-width: 55%;
}

/* Reverse cards */
.reverse .cookie-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;
  margin-left: auto;
}

.cookie-title {
  font-size: clamp(2.5rem, 5vw, 5rem);
  font-weight: 800;
  line-height: 0.95;
  color: #23130f;
  margin: 12px 0;
  transition: 0.3s;
}

.reverse .cookie-title {
  text-align: right;
}

.cookie-card:hover .cookie-title {
  color: white;
}

.cookie-desc {
  max-width: 550px;
  font-size: 1.15rem;
  line-height: 1.8;
  color: #555;
  transition: 0.3s;
}

.reverse .cookie-desc {
  text-align: right;
}

.cookie-card:hover .cookie-desc {
  color: white;
}

.cookie-badge {
  display: inline-block;
  background: #ffe3ef;
  color: #c24164;
  padding: 0.45rem 0.9rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
}

.cookie-buttons {
  display: flex;
  gap: 16px;
  margin-top: 32px;
}

.reverse .cookie-buttons {
  justify-content: flex-end;
}

.cookie-image-wrap {
  position: absolute;
  right: 20px;
  bottom: -80px;
  width: 550px;
}

.reverse .cookie-image-wrap {
  right: auto;
  left: 20px;
  justify-content: flex-start;
  transform: scaleX(1);
}

.cookie-big {
  width: 100%;
  transition: 0.4s;
}

.cookie-card:hover .cookie-big {
  transform: rotate(-5deg) scale(1.05);
}

.reverse .cookie-big {
  transform: scaleX(-1);
}

.reverse:hover .cookie-big {
  transform: scaleX(-1) rotate(-5deg) scale(1.05);
}

/* Mobile */
@media (max-width: 768px) {
  .cookie-card {
    background: transparent;
    height: 180px;
    padding: 20px;
    margin-bottom: 50px;
    border-radius: 24px;
    flex-direction: row;
    overflow: visible;
  }

  .reverse {
    flex-direction: row-reverse;
  }

  .cookie-content {
    max-width: 50%;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .reverse .cookie-content {
    margin-left: auto;
    align-items: flex-end;
    text-align: right;
  }

  /* Hide badge */
  .cookie-badge {
    display: none;
  }

  /* Smaller title */
  .cookie-title {
    font-size: 2rem;
    line-height: 1;
    margin: 0 0 16px;
  }

  /* Hide description */
  .cookie-desc {
    display: none;
  }

  /* Hide Learn More button */
  .cookie-buttons .outline,
  .cookie-buttons > :first-child {
    display: none;
  }

  .cookie-buttons {
    margin-top: 0;
  }

  /* Make Order Now smaller */
  .cookie-buttons :last-child {
    transform: scale(0.9);
    transform-origin: left center;
  }

  .reverse .cookie-buttons :last-child {
    transform-origin: right center;
  }

  /* Smaller cookie */
  .cookie-image-wrap {
    width: 220px;
    right: -35px;
    bottom: -15px;
  }

  .reverse .cookie-image-wrap {
    left: -45px;
    right: auto;
  }

  .cookie-big {
    width: 100%;
  }

  .order-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: transparent;
    border: none;
    padding: 0;
    font-size: 1rem;
    font-weight: 600;
    color: #35150d;
    cursor: pointer;
    transition: all 0.25s ease;
  }

  .order-link:hover {
    transform: translateX(4px);
  }

  .cookie-card:hover .order-link {
    color: white;
  }
}
</style>
