<script setup>
/**
 * Terms and Conditions — website use and online sales. Written with the
 * Consumer Protection Act (CPA, Act 68 of 2008) and the Electronic
 * Communications and Transactions Act (ECTA, Act 25 of 2002) in mind;
 * section 2 carries the supplier details ECTA section 43 asks for.
 */
import { RouterLink } from "vue-router";
import LegalPage from "../../components/legal/LegalPage.vue";
import LegalSection from "../../components/legal/LegalSection.vue";
import FillIn from "../../components/legal/FillIn.vue";
import { BUSINESS } from "../../constants/business";

const sections = [
  { id: "about", title: "About these terms" },
  { id: "who-we-are", title: "Who we are" },
  { id: "health", title: "Health and dietary information" },
  { id: "allergens", title: "Allergens" },
  { id: "products-prices", title: "Products and prices" },
  { id: "orders", title: "Placing an order" },
  { id: "payment", title: "Payment" },
  { id: "delivery", title: "Delivery" },
  { id: "storage", title: "Freshness and storage" },
  { id: "refunds", title: "Cancellations, returns and refunds" },
  { id: "custom", title: "Custom and bulk orders" },
  { id: "website", title: "Using our website" },
  { id: "liability", title: "Our responsibility to you" },
  { id: "disputes", title: "Complaints and disputes" },
  { id: "changes", title: "Changes to these terms" },
];

const summary = [
  "These terms apply when you use our website or buy from us. Your rights under the Consumer Protection Act always apply.",
  "Our cookies are treats, not medicine. If you have diabetes or another condition, check with your doctor or dietitian.",
  "Always check the allergens. Our kitchen handles common allergens, so we can't guarantee any cookie is trace-free.",
  "If something is wrong with your order, tell us and we'll replace it or refund you.",
];

const allergenList = BUSINESS.kitchenAllergens.join(", ").replace(/, ([^,]*)$/, " and $1");
</script>

<template>
  <LegalPage title="Terms and Conditions" :summary="summary" :sections="sections">
    <LegalSection id="about" :number="1" title="About these terms">
      <p>
        These terms apply when you use our website or buy from {{ BUSINESS.tradingName }}. By placing an
        order you agree to them, so please read them before you buy. They work together with our
        <RouterLink to="/privacy-policy">Privacy Policy</RouterLink>,
        <RouterLink to="/refund-policy">Refund Policy</RouterLink> and
        <RouterLink to="/cookie-policy">Cookie Policy</RouterLink>.
      </p>
      <p>
        <strong>Nothing in these terms takes away your rights under the Consumer Protection Act, 2008
        (CPA) or the Electronic Communications and Transactions Act, 2002 (ECTA).</strong> If anything here
        seems to conflict with those laws, the law wins.
      </p>
    </LegalSection>

    <LegalSection id="who-we-are" :number="2" title="Who we are">
      <dl class="grid sm:grid-cols-[19.2rem_1fr] gap-x-6 gap-y-3 rounded-2xl bg-surface border border-chocolate/10 p-6 !max-w-none">
        <dt class="font-bold text-chocolate">Trading name</dt>
        <dd>{{ BUSINESS.tradingName }}</dd>
        <dt class="font-bold text-chocolate">Registered name</dt>
        <dd><FillIn :value="BUSINESS.legalName" label="registered company name" /></dd>
        <dt class="font-bold text-chocolate">Registration number</dt>
        <dd><FillIn :value="BUSINESS.registrationNumber" label="CIPC registration number" /></dd>
        <template v-if="BUSINESS.vatRegistered">
          <dt class="font-bold text-chocolate">VAT number</dt>
          <dd><FillIn :value="BUSINESS.vatNumber" label="VAT number" /></dd>
        </template>
        <dt class="font-bold text-chocolate">Physical address</dt>
        <dd>{{ BUSINESS.address }} (also our address for legal notices)</dd>
        <dt class="font-bold text-chocolate">Phone</dt>
        <dd><a :href="BUSINESS.phoneHref">{{ BUSINESS.phone }}</a></dd>
        <dt class="font-bold text-chocolate">Email</dt>
        <dd><a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a></dd>
      </dl>
    </LegalSection>

    <LegalSection id="health" :number="3" title="Health and dietary information">
      <p>
        Words like "low sugar", "diabetic friendly", "high protein", "gluten free" and "vegan" describe how
        a recipe is made: for example, sweetened with stevia and erythritol instead of sugar, or made with
        almond flour instead of wheat. Each product page lists the ingredients so you can check for
        yourself.
      </p>
      <p>
        <strong>Our cookies are food, not medical treatment, and nothing on our website is medical
        advice.</strong> Everyone's body responds differently. If you have diabetes or any other medical
        condition, or you're pregnant, please check with your doctor or a registered dietitian about what's
        right for you, and keep an eye on how you respond.
      </p>
      <p>
        Sugar alcohols such as erythritol can have a laxative effect if you eat a lot of them in one go.
      </p>
    </LegalSection>

    <LegalSection id="allergens" :number="4" title="Allergens">
      <p>
        Every product page lists the allergens in that cookie. Please check them before you order, and
        again when your order arrives.
      </p>
      <p>
        All our cookies are made in the same kitchen, which handles {{ allergenList }}. We clean carefully
        between batches, but <strong>we can't guarantee that any cookie is free from traces of these
        allergens</strong>. If you have a severe allergy, please contact us before ordering so we can talk
        it through.
      </p>
    </LegalSection>

    <LegalSection id="products-prices" :number="5" title="Products and prices">
      <ul>
        <li>Our cookies are handmade, so size, shape and colour vary a little from the photos.</li>
        <li>All prices are in South African rand (ZAR) and include VAT where it applies.</li>
        <li>Delivery costs are shown separately at checkout, before you pay. The total you see before paying is the total you pay.</li>
        <li>
          If a price on our website is obviously wrong because of a mistake, we'll contact you before going
          ahead. You can then choose to continue at the correct price or cancel for a full refund.
        </li>
      </ul>
    </LegalSection>

    <LegalSection id="orders" :number="6" title="Placing an order">
      <p>
        Before you pay, you'll see a summary of your order so you can check it, correct any mistakes or
        change your mind. When you place your order, you're making an offer to buy. Our agreement is made
        when we email you an order confirmation.
      </p>
      <p>
        We may decline an order, for example if an item is sold out or the address is outside our delivery
        area. If that happens after you've paid, we'll refund you in full.
      </p>
    </LegalSection>

    <LegalSection id="payment" :number="7" title="Payment">
      <p>
        Payments are processed securely by our payment provider. We don't see or store your full card
        details. Your order is only confirmed once payment has been received.
      </p>
    </LegalSection>

    <LegalSection id="delivery" :number="8" title="Delivery">
      <ul>
        <li>Delivery areas, costs and estimated times are shown at checkout.</li>
        <li>Because cookies are perishable, please make sure someone can receive the order at the address you give us.</li>
        <li>The order becomes your responsibility once it has been delivered to that address.</li>
        <li>
          If we can't deliver within 30 days of your order (unless we've agreed a different date with you),
          you may cancel and we'll refund everything you paid.
        </li>
      </ul>
    </LegalSection>

    <LegalSection id="storage" :number="9" title="Freshness and storage">
      <p>
        Follow the storage instructions and best-before date on your packaging.
        Storage life depends on the recipe and storage conditions. Please contact us if
        the instructions are missing or you need more information.
      </p>
    </LegalSection>

    <LegalSection id="refunds" :number="10" title="Cancellations, returns and refunds">
      <p>
        If something is wrong with your order, we'll put it right. Our
        <RouterLink to="/refund-policy">Refund Policy</RouterLink> explains how, including what to do if
        your order arrives damaged, incomplete or not as described.
      </p>
      <p>
        Because cookies are perishable food, the seven-day "cooling-off" right in ECTA does not apply to them
        once they've been baked and sent, and we can't accept returns for a change of mind. You can still
        cancel before we start baking your order.
      </p>
    </LegalSection>

    <LegalSection id="custom" :number="11" title="Custom and bulk orders">
      <p>
        For custom, event or bulk orders, we'll agree the details, price, delivery date and any deposit with
        you in writing before we start. Because these are made to your specifications, they can't be
        cancelled for a change of mind once baking has started. Quality problems are covered in the same
        way as any other order.
      </p>
    </LegalSection>

    <LegalSection id="website" :number="12" title="Using our website">
      <ul>
        <li>Please use our website lawfully and don't try to interfere with how it works or access areas that aren't meant for you.</li>
        <li>Our name, logo, photos, recipes and text belong to us. Please don't copy them for commercial use without our written permission.</li>
        <li>We work to keep the website accurate and available, but it may occasionally be unavailable or contain errors. We'll fix them as soon as we can.</li>
        <li>Links to other websites (such as maps or social media) are there for convenience. We're not responsible for their content.</li>
      </ul>
    </LegalSection>

    <LegalSection id="liability" :number="13" title="Our responsibility to you">
      <p>
        We're responsible for loss or harm you suffer as a foreseeable result of us breaking these terms or
        failing to use reasonable care. We're not responsible for delays or failures caused by events
        outside our reasonable control, such as power outages, severe weather, strikes or courier
        disruptions, but we'll let you know and do what we can to sort it out.
      </p>
      <p>
        Nothing in these terms limits or excludes any liability that cannot be limited or excluded by law,
        including your rights under the Consumer Protection Act to safe, good-quality goods.
      </p>
    </LegalSection>

    <LegalSection id="disputes" :number="14" title="Complaints and disputes">
      <p>
        If you're unhappy, please contact us first at
        <a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a> or
        <a :href="BUSINESS.phoneHref">{{ BUSINESS.phone }}</a>. Most problems can be sorted out quickly.
      </p>
      <p>
        If we can't resolve it together, you can refer your complaint to the
        <a href="https://www.cgso.org.za" target="_blank" rel="noopener">Consumer Goods and Services Ombud</a>
        or the
        <a href="https://thencc.org.za" target="_blank" rel="noopener">National Consumer Commission</a>.
        These terms are governed by the laws of the Republic of South Africa.
      </p>
    </LegalSection>

    <LegalSection id="changes" :number="15" title="Changes to these terms">
      <p>
        We may update these terms from time to time. The version that applies to your order is the one on
        our website when you placed it. The date at the top of this page shows when they were last updated.
      </p>
    </LegalSection>
  </LegalPage>
</template>
