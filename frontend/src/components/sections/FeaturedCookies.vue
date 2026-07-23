<script setup>
import { ref } from "vue";
import BaseButton from "../../components/ui/BaseButton.vue";

const cookies = ref([
  {
    id: 1,
    name: "Chocolate Chip",
    description:
      "Our signature chocolate chip cookie made with premium Belgian chocolate.",
    image: "/cookie.png",
    price: 35,
  },
  {
    id: 2,
    name: "Red Velvet",
    description: "A rich red velvet cookie with creamy white chocolate chunks.",
    image: "/cookie2.png",
    price: 38,
  },
]);
</script>

<template>
  <section class="featured-section">
    <div class="container-magic">
      <h2 class="featured-title">Signature Crumble Drops</h2>

      <div class="featured-grid">
        <div v-for="cookie in cookies" :key="cookie.id" class="featured-card">
          <!-- Floating Cookie -->
          <img :src="cookie.image" :alt="cookie.name" class="featured-image" />

          <!-- Glass Card -->
          <div class="glass-card">
            <h3>{{ cookie.name }}</h3>

            <p>
              {{ cookie.description }}
            </p>

            <div class="card-footer">
              <span class="price">R{{ Number(cookie.price).toFixed(2) }}</span>

              <div class="buttons">
                <BaseButton variant="primary">View More</BaseButton>

                <button class="cart-btn">🛒</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.featured-section {
  position: relative;
  overflow: hidden;
  background: #35150d;
  padding: 120px 0;
}

.featured-section::before {
  content: "";

  position: absolute;
  inset: 0;

  background-image: url("/cookie-background.jpeg");
  background-size: cover;
  background-position: center;

  opacity: 0.08;

  pointer-events: none;
  scale: 1.3;
}

.container-magic {
  position: relative;
  z-index: 2;
}

.featured-title {
  text-align: center;
  color: white;
  font-size: clamp(2.2rem, 5vw, 3.6rem);
  font-weight: 700;
  margin-bottom: 100px;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 80px;
}

.featured-card {
  position: relative;
  padding-top: 120px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(18px);
  border-radius: 28px;
  padding: 150px 40px 35px;
  min-height: 430px;

  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.featured-image {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translate(-50%, -20%);
  width: 260px;
  z-index: 5;
  filter: drop-shadow(0 20px 30px rgba(0, 0, 0, 0.35));
  transition: 0.35s;
}

.featured-card:hover .featured-image {
  transform: translate(-50%, -28%) rotate(-6deg) scale(1.05);
}

.glass-card h3 {
  color: white;
  font-size: 2rem;
  font-weight: 700;
  text-align: center;
  margin-bottom: 20px;
}

.glass-card p {
  color: rgba(255, 255, 255, 0.9);
  text-align: center;
  line-height: 1.7;
  font-size: 1rem;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 35px;
}

.price {
  color: white;
  font-size: 2rem;
  font-weight: 700;
}

.buttons {
  display: flex;
  gap: 15px;
  align-items: center;
}

.cart-btn {
  width: 55px;
  height: 55px;
  border-radius: 50%;
  border: none;
  background: white;
  cursor: pointer;
  font-size: 1.3rem;
  transition: 0.3s;
}

.cart-btn:hover {
  transform: scale(1.08);
}

/* Tablet */

@media (max-width: 1024px) {
  .featured-grid {
    grid-template-columns: 1fr;
    gap: 90px;
  }

  .glass-card {
    max-width: 600px;
    margin: auto;
  }
}

/* Mobile */

@media (max-width: 768px) {
  .featured-section {
    padding: 80px 0;
  }

  .featured-title {
    margin-bottom: 70px;
  }

  .featured-card {
    padding-top: 90px;
  }

  .featured-image {
    width: 180px;
  }

  .glass-card {
    padding: 110px 24px 24px;
    min-height: auto;
  }

  .glass-card h3 {
    font-size: 1.6rem;
  }

  .glass-card p {
    font-size: 0.95rem;
  }

  .card-footer {
    flex-direction: column;
    gap: 18px;
  }

  .price {
    font-size: 1.7rem;
  }

  .buttons {
    width: 100%;
    justify-content: center;
  }
}
</style>
