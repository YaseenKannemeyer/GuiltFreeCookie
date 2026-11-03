# Hero scroll validation — 3 November 2026

## Changes

- Desktop pin distance: 4.2 → 3.2 viewport heights (24% shorter). At 812px height this is 3410 → 2598px of pinned scrolling.
- Scrub catch-up: 0.5 → 0.3 seconds. The splash caption now appears during the fall instead of leaving a long empty interval.
- Video requests align to 30fps frames and retain the latest target while decoding. A bounded watchdog recovers missing seek events, including after scrolling stops. Cleanup cancels timers, listeners and late playback continuations.
- The final lineup fits its available height. At 1024 × 640, the bottom labels previously ended at 680px; after the fix they end at 616px. The heading clears the navigation.
- The static phone hero uses `svh` so Safari's expanding/collapsing browser chrome does not change its minimum height. Static lineup images no longer request permanent compositor layers.
- Cinematic eligibility remains landscape-oriented: at least 1024px wide, now at least 640px tall, and no reduced-motion preference. Lazy-module failure returns to the static hero.

## Verified

- Desktop Chrome 156, local Vite development server: forward/reverse scroll, final lineup, breakpoint transitions, and return to frame zero. One pin spacer on desktop; none at phone width.
- Chrome viewport checks at 390 × 844, 1440 × 812 and 1024 × 640. Phone layout has no video element or horizontal overflow. Viewport testing is not iPhone emulation.
- Real iPhone / Safari: user reported **“Smooth, nothing cut off”** after scrolling the homepage hero and lineup down and back up. Model, iOS version and frame-rate trace were not supplied.
- Actual MP4 decoder integration in desktop Chrome: seven forward/reverse/latest-target cases passed using `requestVideoFrameCallback`, with decoded timestamps matching requested frames. Observed latency: 15–23ms, median 17ms on this Mac; this is seek latency, not full-page FPS or a mobile performance claim.
- `npm --prefix frontend test`: 17 tests passed, including ten scrubber tests and two decoded-pixel diagnostic tests for late metadata, rapid/reverse seeking, lost seek events, teardown, and autoplay rejection.
- Production client/SSR build and prerender passed. Existing build warning: `PRERENDER_API_URL` is unset, so catalogue product pages are omitted from prerender/sitemap.

## Real-phone video check

The homepage intentionally does not run the cinematic video on phones. Its static scroll check passed. The user's first standalone iPhone Safari run failed to present the 1.2s frame. A subsequent run passed the six single-target checks (16–25ms reported latency) but timed out on the rapid target burst ending at 0.7s. These are real failures, not a mobile pass.

Revision `safari-startup-and-burst-v2` initiates playback before metadata when triggered by a tap, waits for a presented frame before pausing, and defers queued seeks until the next rendering frame rather than seeking inside the `seeked` event. The test separates startup from seek timing and records media state/events on failure. The real iPhone v2 result still failed: metadata and buffering succeeded, `seeked` completed at 1.2s after 134ms, no media error occurred, and the video remained paused with `readyState=4`, but no post-seek compositor callback arrived. This does not distinguish an absent callback from a stale displayed image.

Revision `safari-pixel-diagnostic-v3` adds independent comparisons against reference images sampled from the actual MP4 in Chrome at compositor-confirmed timestamps. A 32 × 32 grayscale crop of the cookie/glass must match within 2 luminance levels of mean absolute error while the seek is settled. Tests verify that frozen opening images, stale action frames and black video fail this comparison. Callback evidence remains separate: `PIXELS_PASS` is deliberately not an on-screen smoothness or compositor certification. Eight Chrome cases pass both pixel and timestamp checks. iPhone v3 confirmation is pending.

With Vite running, visit `/tests/hero-video.html` and tap **Run video check**. It checks actual decoded frames at 1.2, 2.7, 3.3, 3.9, 2.1 and 0 seconds, a rapid target burst ending at 0.7 seconds, and paused cleanup. This development-only harness is outside `public` and is not included in the production build.

Before enabling a pinned cinematic on phones, create an appropriate portrait composition and test touch scrolling, orientation changes, Safari toolbar movement, Low Power Mode and reduced motion on physical hardware. The current result does not certify that unimplemented experience.
