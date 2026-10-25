<script setup>
/**
 * Privacy Policy — written for South Africa's Protection of Personal
 * Information Act (POPIA, Act 4 of 2013). Business facts come from
 * constants/business.js; blanks show until they're filled in.
 */
import { RouterLink } from "vue-router";
import LegalPage from "../../components/legal/LegalPage.vue";
import LegalSection from "../../components/legal/LegalSection.vue";
import FillIn from "../../components/legal/FillIn.vue";
import { BUSINESS } from "../../constants/business";

const sections = [
  { id: "who-we-are", title: "Who we are" },
  { id: "what-we-collect", title: "What we collect" },
  { id: "health-information", title: "Health and dietary information" },
  { id: "how-we-use-it", title: "How we use it, and why" },
  { id: "marketing", title: "Marketing emails" },
  { id: "sharing", title: "Who we share it with" },
  { id: "outside-sa", title: "Information stored outside South Africa" },
  { id: "keeping", title: "How long we keep it" },
  { id: "security", title: "Keeping it safe" },
  { id: "your-rights", title: "Your rights" },
  { id: "children", title: "Children" },
  { id: "changes", title: "Changes to this policy" },
  { id: "contact", title: "Contact and complaints" },
];

const summary = [
  "We only collect what we need to bake, deliver and answer your questions.",
  "We never sell your information, and we only send marketing emails if you ask for them.",
  "You don't have to tell us about your health. If you do, we use it only to help you, and only with your consent.",
  "You can ask to see, correct or delete your information at any time.",
];
</script>

<template>
  <LegalPage title="Privacy Policy" :summary="summary" :sections="sections">
    <LegalSection id="who-we-are" :number="1" title="Who we are">
      <p>
        {{ BUSINESS.tradingName }} (<FillIn :value="BUSINESS.legalName" label="registered company name" />,
        registration number <FillIn :value="BUSINESS.registrationNumber" label="CIPC registration number" />)
        bakes and sells cookies from {{ BUSINESS.address }}. In this policy, "we", "us" and "our" mean
        {{ BUSINESS.tradingName }}, and "you" means anyone who visits our website, contacts us or orders
        from us.
      </p>
      <p>
        We are the "responsible party" for your personal information under the Protection of Personal
        Information Act, 2013 (POPIA). That means we decide how and why your information is used, and
        we're accountable for looking after it.
      </p>
      <p>
        Our Information Officer is <FillIn :value="BUSINESS.informationOfficer" label="Information Officer's name" />.
        You can reach them at <a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a>.
      </p>
    </LegalSection>

    <LegalSection id="what-we-collect" :number="2" title="What we collect">
      <h3>Information you give us</h3>
      <ul>
        <li><strong>When you order:</strong> your name, email address, mobile number, delivery address and what you ordered.</li>
        <li><strong>When you create an account:</strong> your name, email address and mobile number.</li>
        <li><strong>When you contact us:</strong> your name, email address and whatever you choose to tell us in your message.</li>
        <li><strong>When you subscribe to our newsletter:</strong> your email address.</li>
      </ul>
      <h3>Payment details</h3>
      <p>
        Card payments are handled by our payment provider. We never see or store your full card number.
        We only receive confirmation that you paid, and the amount.
      </p>
      <h3>Information collected automatically</h3>
      <p>
        Like most websites, our servers may record basic technical details when you visit, such as your
        IP address, browser type and the pages you open. We use this only to keep the site running and
        secure. We don't use advertising trackers or analytics cookies. Our
        <RouterLink to="/cookie-policy">Cookie Policy</RouterLink> lists exactly what the site stores in
        your browser.
      </p>
    </LegalSection>

    <LegalSection id="health-information" :number="3" title="Health and dietary information">
      <p>
        Our cookies are made for people who watch their sugar, train hard or avoid certain ingredients.
        You never have to tell us about your health to shop with us.
      </p>
      <p>
        If you choose to share health information (for example, that you have diabetes or an allergy),
        POPIA treats it as <strong>special personal information</strong>. By sending it to us you consent
        to us using it, and we will use it only to answer your question or prepare your order safely. We
        won't use it for marketing, and we won't share it with anyone who doesn't need it for that
        purpose. You can withdraw your consent at any time by asking us to delete it.
      </p>
    </LegalSection>

    <LegalSection id="how-we-use-it" :number="4" title="How we use it, and why">
      <p>POPIA only allows us to use your information for a lawful reason. Ours are:</p>
      <ul>
        <li><strong>To fulfil your order</strong> (baking, payment, delivery, and updates about your order), because we need it to carry out our agreement with you.</li>
        <li><strong>To answer your questions</strong> and handle complaints or refunds.</li>
        <li><strong>To keep our website and business secure</strong> and prevent fraud, which is in our legitimate interest and yours.</li>
        <li><strong>To keep financial records</strong> such as invoices, because tax law requires it.</li>
        <li><strong>To send you marketing emails</strong>, but only with your consent (see section 5).</li>
      </ul>
      <p>We won't use your information for anything unrelated to these purposes without telling you first.</p>
    </LegalSection>

    <LegalSection id="marketing" :number="5" title="Marketing emails">
      <p>
        We only send newsletters and offers if you've subscribed, or if you're an existing customer and we're
        telling you about our own similar products. Every marketing email has an unsubscribe link, and you
        can also just reply and ask us to stop. We'll stop straight away, at no cost to you.
      </p>
      <p>
        Some of these emails send themselves when something happens: a short welcome series when you subscribe,
        a reminder if you leave cookies in your cart, or a note asking how your order was. Our emails contain a
        tiny image and tracked links, so we can tell whether an email was opened or a link was clicked. We use
        that only to send fewer emails to people who aren't reading them, and to see which emails are useful. If
        you haven't opened our emails for a few months, we'll ask whether you'd like to stay subscribed, and
        stop if you don't answer.
      </p>
    </LegalSection>

    <LegalSection id="sharing" :number="6" title="Who we share it with">
      <p>
        <strong>We never sell or rent your personal information.</strong> We share it only with service
        providers who help us run the business, and only what they need:
      </p>
      <ul>
        <li>our payment provider, to process payments;</li>
        <li>our delivery or courier partners, to get your order to you;</li>
        <li>our email provider, to send order updates and newsletters you've asked for;</li>
        <li>our website hosting and IT providers, who store the site and its data.</li>
      </ul>
      <p>
        These providers act on our instructions and must keep your information confidential and secure.
        We may also disclose information if the law requires it, for example to comply with a court order.
      </p>
    </LegalSection>

    <LegalSection id="outside-sa" :number="7" title="Information stored outside South Africa">
      <p>
        Some of our service providers may store or process information on servers outside South Africa.
        When that happens, we only use providers in countries with data protection laws similar to POPIA,
        or providers who are bound by an agreement to protect your information to the same standard.
      </p>
    </LegalSection>

    <LegalSection id="keeping" :number="8" title="How long we keep it">
      <ul>
        <li><strong>Order and payment records:</strong> five years, because South African tax law requires us to keep them.</li>
        <li><strong>Messages and enquiries:</strong> about 12 months after we've resolved them, unless they relate to an order or a complaint.</li>
        <li><strong>Newsletter subscriptions:</strong> until you unsubscribe.</li>
        <li><strong>Accounts:</strong> until you ask us to close your account, after which we keep only what the law requires.</li>
      </ul>
      <p>When we no longer need information, we delete it or make sure it can no longer identify you.</p>
    </LegalSection>

    <LegalSection id="security" :number="9" title="Keeping it safe">
      <p>
        We use reasonable technical and organisational measures to protect your information, including
        encrypted connections to our website, hashed passwords, and limiting access to the people who need
        it to do their job.
      </p>
      <p>
        If we ever have reason to believe your personal information has been accessed without
        authorisation, we'll tell you and the Information Regulator as soon as reasonably possible, as
        POPIA requires, and explain what we're doing about it.
      </p>
    </LegalSection>

    <LegalSection id="your-rights" :number="10" title="Your rights">
      <p>Under POPIA you have the right to:</p>
      <ul>
        <li>ask whether we hold information about you, and get a copy of it;</li>
        <li>ask us to correct information that's wrong or out of date;</li>
        <li>ask us to delete information we no longer have a lawful reason to keep;</li>
        <li>object to us using your information, including for marketing;</li>
        <li>withdraw any consent you've given us;</li>
        <li>complain to the Information Regulator (see section 13).</li>
      </ul>
      <p>
        To use any of these rights, email <a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a>.
        We may need to confirm your identity first. We'll respond within 30 days. You can also ask for
        access to records under the Promotion of Access to Information Act (PAIA) by contacting our
        Information Officer.
      </p>
    </LegalSection>

    <LegalSection id="children" :number="11" title="Children">
      <p>
        Our website and shop are meant for adults. We don't knowingly collect information about anyone
        under 18 without the consent of a parent or guardian. If you think a child has sent us their
        information, contact us and we'll delete it.
      </p>
    </LegalSection>

    <LegalSection id="changes" :number="12" title="Changes to this policy">
      <p>
        We may update this policy when our services or the law change. The date at the top shows when it
        was last updated. If we make a significant change, we'll make it clear on this page and, where
        appropriate, let you know by email.
      </p>
    </LegalSection>

    <LegalSection id="contact" :number="13" title="Contact and complaints">
      <p>
        Questions about your information? Email
        <a :href="`mailto:${BUSINESS.email}`">{{ BUSINESS.email }}</a>, call
        <a :href="BUSINESS.phoneHref">{{ BUSINESS.phone }}</a>, or write to us at {{ BUSINESS.address }}.
      </p>
      <p>
        If you're not happy with how we've handled your information, please tell us first so we can put it
        right. You also have the right to complain to the
        <a href="https://inforegulator.org.za" target="_blank" rel="noopener">Information Regulator (South Africa)</a>.
        Their website lists their current contact details and complaint forms.
      </p>
    </LegalSection>
  </LegalPage>
</template>
