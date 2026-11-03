/**
 * useCookieDropScroll — the home hero's pinned scroll sequence (desktop,
 * motion allowed). One ScrollTrigger master timeline, 10 units long, so
 * 1 unit = 10% of the pinned scroll:
 *
 *   0–1.7   the resting cookie sheds its ribbons and settles onto the clip's
 *           cookie as the clip (glass of milk included) fades in around it
 *   1.4–6.7 the clip scrubs: fall, impact, milk crown
 *   5–8.3   at the crown (3.3 s) eight cookies fan out into their slots
 *   7.9–10  names, ranges and the shop link settle in, then a short hold
 *
 * The stage marks its parts with data-drop="…". Everything measured (clip
 * placement, the cookie handoff, slot positions) is re-read on every
 * ScrollTrigger refresh, so a resize keeps the split and slots aligned.
 *
 *   const drop = createCookieDrop(stageEl)
 *   drop.destroy()   // kills the trigger, unpins, removes listeners
 */
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { COOKIE_DROP_VIDEO as CLIP, HERO_ARTWORK as ART } from "../constants/cookieDrop";
import { cookieDropPinned } from "./cookieDropState";
import { createVideoScrubber } from "./videoScrubber";

gsap.registerPlugin(ScrollTrigger);

const SPLIT = CLIP.splitX / CLIP.width; // 0.8, same as the CSS split
const SCROLL_LENGTH = 3.2; // pinned distance, in stage heights
const TOTAL = 10;

// Per cookie, by lineup index: launch order out of the splash, how far it
// arcs above the straight path (share of stage height), starting spin, and
// mid-flight size (nearer the camera = bigger).
const FLIGHTS = [
  { order: 3, lift: 0.16, spin: -38, depth: 1.08 },
  { order: 1, lift: 0.22, spin: 26, depth: 1.18 },
  { order: 0, lift: 0.26, spin: -18, depth: 1.22 },
  { order: 2, lift: 0.2, spin: 34, depth: 1.12 },
  { order: 6, lift: 0.08, spin: 30, depth: 0.94 },
  { order: 4, lift: 0.12, spin: -28, depth: 1.02 },
  { order: 5, lift: 0.1, spin: 22, depth: 0.98 },
  { order: 7, lift: 0.06, spin: -34, depth: 0.9 },
];


const pct = (n) => `${+(n * 100).toFixed(2)}%`;

export function createCookieDrop(stage) {
  const one = (name) => stage.querySelector(`[data-drop="${name}"]`);
  const all = (name) => [...stage.querySelectorAll(`[data-drop="${name}"]`)];

  const media = one("media");
  const video = one("video");
  const anchor = one("cookie-anchor");
  const cookie = one("cookie");
  const copy = one("copy");
  const lineup = one("lineup");
  const slots = all("slot");
  const fliers = all("flier");
  const lifts = all("lift");

  // Stage-space geometry. ScrollTrigger fires refreshInit while the stage is
  // still pinned at its old size, so just mark it stale there; the timeline's
  // function-based values call g() after the pins are released.
  const geo = { sh: 0, cookieDx: 0, cookieDy: 0, cookieScale: 1, slots: [] };
  let stale = true;
  const g = () => {
    if (stale) measure();
    return geo;
  };
  const markStale = () => (stale = true);

  function measure() {
    const sw = stage.clientWidth;
    const sh = stage.clientHeight;
    const navH = parseFloat(getComputedStyle(stage).getPropertyValue("--nav-h")) || 76;
    const { content } = CLIP;
    const contentH = content.bottom - content.top;

    // Browsers decode the clip's flat backdrop a few levels off the CSS
    // colours, so a frame edge inside the stage shows. Cover the whole stage
    // whenever cookie-to-glass-base still fits (the cookie may tuck halfway
    // under the transparent navbar, the glass base may run 40px off the
    // bottom); otherwise (very wide windows) fit it under the navbar and
    // feather the edges. Either way the frame slides sideways so its split
    // sits on the CSS split.
    const mid = (content.top + content.bottom) / 2;
    const minTop = navH / 2;
    const maxBottom = sh + 40;
    const cover = Math.max(sw / CLIP.width, sh / CLIP.height);
    const covers = contentH * cover <= maxBottom - minTop;
    const s = covers ? cover : Math.min(sw / CLIP.width, (sh - navH - 32) / contentH);
    const w = CLIP.width * s;
    const h = CLIP.height * s;
    const left = SPLIT * (sw - w);
    const top = covers
      ? Math.min(0, Math.max(sh - h, (minTop + maxBottom) / 2 - mid * s))
      : (navH + sh) / 2 - mid * s;
    media.classList.toggle("is-framed", !covers);
    Object.assign(media.style, {
      left: `${left}px`,
      top: `${top}px`,
      width: `${w}px`,
      height: `${h}px`,
    });

    const box = stage.getBoundingClientRect();
    const rest = anchor.getBoundingClientRect(); // never transformed by the timeline
    geo.cookieDx = left + CLIP.cookie.cx * s - (rest.left - box.left + rest.width * ART.cookieX);
    geo.cookieDy = top + CLIP.cookie.cy * s - (rest.top - box.top + rest.height * ART.cookieY);
    geo.cookieScale = (CLIP.cookie.width * s) / (rest.width * ART.cookieWidth);

    const crownX = left + CLIP.crown.x * s;
    const crownY = top + CLIP.crown.y * s;
    geo.slots = slots.map((slot) => {
      const r = slot.getBoundingClientRect();
      return {
        dx: crownX - (r.left - box.left + r.width / 2),
        dy: crownY - (r.top - box.top + r.height / 2),
      };
    });
    geo.sh = sh;
    stale = false;
  }

  const scrubber = createVideoScrubber(video, {
    fps: CLIP.fps,
    duration: CLIP.duration,
    prime: matchMedia("(pointer: coarse)").matches,
  });
  const clip = { t: 0 };
  const T = CLIP.times;
  // Timeline position of the 3.3 s reveal inside the impact-to-crown stretch.
  const revealAt = 3.9 + (1.6 * (T.reveal - T.impact)) / (T.crownPeak - T.impact);

  let tl;
  // Runs on every render, scrub smoothing included. Tween-level callbacks
  // are skipped while ScrollTrigger re-renders during a refresh.
  const sync = () => {
    scrubber.set(clip.t);
    // Lineup links only take clicks once the cookies have landed.
    lineup.classList.toggle("is-settled", tl.progress() > 0.88);
  };

  const ctx = gsap.context(() => {
    tl = gsap.timeline({
      defaults: { ease: "none" },
      onUpdate: sync,
      scrollTrigger: {
        trigger: stage,
        start: "top top",
        end: () => `+=${Math.round(stage.clientHeight * SCROLL_LENGTH)}`,
        pin: true,
        scrub: 0.3,
        anticipatePin: 1,
        invalidateOnRefresh: true,
        onRefresh: () => tl && sync(),
        onToggle: (self) => (cookieDropPinned.value = self.isActive),
      },
    });

    // Every segment is a fromTo, later ones with immediateRender: false,
    // so scrubbing backwards after a refresh never replays stale values.
    const ribbons = (r) => `circle(${r}% at ${pct(ART.cookieX)} ${pct(ART.cookieY)})`;
    gsap.set(cookie, { transformOrigin: `${pct(ART.cookieX)} ${pct(ART.cookieY)}` });
    // Scaling about the split line keeps the clip's split on the CSS split.
    gsap.set(media, {
      transformOrigin: `${pct(CLIP.crown.x / CLIP.width)} ${pct(CLIP.crown.y / CLIP.height)}`,
    });

    // 0–1.7: the resting cookie sheds its ribbons and settles where the
    // clip's cookie hangs, the clip fades in around it (glass included),
    // then the artwork fades to reveal the clip's cookie underneath.
    tl.fromTo(one("glow"), { autoAlpha: 1 }, { autoAlpha: 0, duration: 0.8 }, 0)
      .fromTo(
        copy,
        { autoAlpha: 1, y: 0 },
        { autoAlpha: 0, y: () => -g().sh * 0.06, duration: 1.3, ease: "power1.in" },
        0.2,
      )
      .fromTo(
        cookie,
        { x: 0, y: 0, scale: 1, rotation: 0, clipPath: ribbons(75) },
        {
          x: () => g().cookieDx,
          y: () => g().cookieDy,
          scale: () => g().cookieScale,
          rotation: ART.rotationToClip,
          clipPath: ribbons(40),
          duration: 1,
          ease: "power2.inOut",
        },
        0,
      )
      .fromTo(
        media,
        { autoAlpha: 0, scale: 1.05 },
        { autoAlpha: 1, scale: 1, duration: 0.6, ease: "power1.out" },
        0.8,
      )
      .fromTo(cookie, { autoAlpha: 1 }, { autoAlpha: 0, duration: 0.3, immediateRender: false }, 1.4);

    // 1.4–6.7: fall, impact, crown. Stops before the late upward rebound.
    tl.fromTo(clip, { t: 0 }, { t: T.fallStart, duration: 0.3 }, 1.4)
      .fromTo(clip, { t: T.fallStart }, { t: T.impact, duration: 2.2, immediateRender: false }, 1.7)
      .fromTo(clip, { t: T.impact }, { t: T.crownPeak, duration: 1.6, immediateRender: false }, 3.9)
      .fromTo(clip, { t: T.crownPeak }, { t: T.stop, duration: 1.2, immediateRender: false }, 5.5)
      // Poster stand-in while the clip loads (or if it never does).
      .fromTo(one("splash-poster"), { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.6 }, 4.4)
      .fromTo(
        one("tagline"),
        { autoAlpha: 0, y: 30 },
        { autoAlpha: 1, y: 0, duration: 0.7, ease: "power2.out" },
        1.9,
      )
      .fromTo(
        one("tagline"),
        { autoAlpha: 1, y: 0 },
        { autoAlpha: 0, y: -20, duration: 0.6, immediateRender: false },
        5.6,
      )
      // Push in slightly as it fades, so a covering frame never shows an edge.
      .fromTo(
        media,
        { autoAlpha: 1, scale: 1 },
        { autoAlpha: 0, scale: 1.04, duration: 1.4, ease: "power1.in", immediateRender: false },
        5.6,
      );

    // From 3.3 s: eight cookies leave the crown on their own arcs. The outer
    // layer travels crown -> slot; the inner layer adds the rise, spin and
    // depth scale, so each path curves differently.
    FLIGHTS.forEach((f, i) => {
      if (!fliers[i]) return;
      const at = revealAt + f.order * 0.14;
      const d = 2.3;
      const half = d / 2;
      const peak = () => -g().sh * f.lift;

      tl.fromTo(
        fliers[i],
        { x: () => g().slots[i].dx, y: () => g().slots[i].dy },
        { x: 0, y: 0, duration: d, ease: "power2.inOut" },
        at,
      )
        .fromTo(fliers[i], { autoAlpha: 0 }, { autoAlpha: 1, duration: 0.3 }, at)
        .fromTo(
          lifts[i],
          { y: 0, scale: 0.18, rotation: f.spin },
          { y: peak, scale: f.depth, rotation: f.spin * 0.35, duration: half, ease: "power2.out" },
          at,
        )
        .fromTo(
          lifts[i],
          { y: peak, scale: f.depth, rotation: f.spin * 0.35 },
          { y: 0, scale: 1, rotation: 0, duration: half, ease: "power2.inOut", immediateRender: false },
          at + half,
        );
    });

    // 7.4–10: heading, labels, shop link, then a short hold before unpinning.
    tl.fromTo(
      one("lineup-heading"),
      { autoAlpha: 0, y: 24 },
      { autoAlpha: 1, y: 0, duration: 0.8, ease: "power2.out" },
      7.4,
    )
      .fromTo(
        all("label"),
        { autoAlpha: 0, y: 12 },
        { autoAlpha: 1, y: 0, duration: 0.6, stagger: 0.08, ease: "power2.out" },
        7.9,
      )
      .fromTo(
        one("lineup-cta"),
        { autoAlpha: 0, y: 16 },
        { autoAlpha: 1, y: 0, duration: 0.6, ease: "power2.out" },
        8.6,
      )
      .set({}, {}, TOTAL);
  }, stage);

  ScrollTrigger.addEventListener("refreshInit", markStale);

  // Keyboard users: focusing a lineup link jumps to the settled lineup;
  // tabbing back into the hero copy returns to the top of the sequence.
  const jump = (top) => window.scrollTo({ top, behavior: "instant" });
  function onFocusIn(event) {
    const st = tl.scrollTrigger;
    if (!st) return;
    if (lineup.contains(event.target) && st.progress < 0.99) jump(st.end);
    else if (copy.contains(event.target) && st.progress > 0) jump(st.start);
  }
  stage.addEventListener("focusin", onFocusIn);

  // Positions depend on web fonts (label heights) and on the page
  // transition's offset at mount, so measure again once both have settled.
  let alive = true;
  const refresh = () => alive && ScrollTrigger.refresh();
  document.fonts?.ready.then(refresh);
  const enteringPage = stage.closest(".page-enter-active");
  enteringPage?.addEventListener("transitionend", refresh, { once: true });

  return {
    destroy() {
      alive = false;
      stage.removeEventListener("focusin", onFocusIn);
      enteringPage?.removeEventListener("transitionend", refresh);
      ScrollTrigger.removeEventListener("refreshInit", markStale);
      scrubber.destroy();
      ctx.revert();
      cookieDropPinned.value = false;
      lineup.classList.remove("is-settled");
      for (const prop of ["left", "top", "width", "height"]) media.style.removeProperty(prop);
    },
  };
}
