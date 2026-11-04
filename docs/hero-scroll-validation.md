# Hero scroll validation

Updated 4 November 2026.

## Shipped behaviour

The cinematic sequence runs at widths of at least 1024px and heights of at least 640px, with reduced motion disabled. Phones use the static hero and a normal scrolling cookie lineup. Enabling a pinned phone sequence would require a portrait composition and separate touch-scroll testing.

The desktop sequence has a shorter pin distance (4.2 → 3.2 stage heights), faster scrub catch-up (0.5 → 0.3s), an earlier splash caption, and a finale that fits smaller laptop screens. The phone hero uses a stable small-viewport height (`svh`).

## Safari fixes

- Pending seeks advance in a cancellable timer after `seeked`, rather than depending on `requestAnimationFrame`. Native Safari testing reproduced a stalled queue when its window reported `visibilityState=hidden` and rendering callbacks stopped.
- Attaching the scrubber preserves the video's current position until a target is requested, avoiding an unnecessary initial seek to zero.
- Targets lie in the middle of the selected 30fps frame. Native Safari decoded 2.1s when asked for the fractional boundary at 2.1333333s; seeking inside the intended frame avoids rounding onto its predecessor.
- Existing safeguards retain the latest target during rapid scrolling, bound lost-event retries, handle late metadata and rejected autoplay, and cancel work during teardown.

## Validation completed

- Real iPhone / Safari homepage check, 3 November: user reported **“Smooth, nothing cut off”** after scrolling down through the static hero and lineup and back up.
- Chrome checks from 3 November: forward/reverse desktop scrolling, return to frame zero, breakpoint cleanup, and layouts at 390 × 844, 1440 × 812 and 1024 × 640. No video/pin spacer at phone width; no horizontal overflow.
- Small laptop finale: labels previously extended to 680px in a 640px viewport; corrected layout ends at 616px and clears the navigation.
- `npm --prefix frontend test`: 20 passing tests, including fractional frame selection, progress without rendering callbacks, queued-seek cleanup, rapid/reverse targets, late metadata, lost events and autoplay rejection.
- Production client/SSR build and prerender pass. Existing warning: `PRERENDER_API_URL` is unset, so catalogue product pages are excluded from prerender/sitemap.

## Real-device decoder result — complete

On 4 November 2026 the user ran `safari-seek-check-v4` on a physical iPhone in Safari with the page visible throughout. Result: **PIXELS_PASS**.

All eight decoded-image checks passed, including both rapid-target bursts. Seven images matched their reference exactly; the 0.7s burst had a mean difference of 0.952/255 (below the 2/255 threshold). Image delivery was 29–35ms (median 31.5ms), and the video was paused after cleanup. Seven of eight checkpoints also supplied matching compositor timestamps; the 0.7s burst supplied no callback despite a matching decoded image.

The [saved device result](validation/hero-video-iphone-2026-11-04.json) preserves the reported status and measurements. This closes the standalone decoder check. It does not certify full-page FPS or an unimplemented pinned phone cinematic; the shipped static phone hero already passed the user's visual scroll check.

## Diagnostic method

Earlier iPhone diagnostics failed on the first seek or rapid reversal. The detailed v2 report showed a fully buffered clip, `readyState=4`, no media error and a completed seek to 1.2s, but no post-seek video-frame callback. That report alone cannot prove the displayed frame froze.

The final development-only check is `/tests/hero-video.html?check=v4`, labelled **safari-seek-check-v4**. It:

1. Records seven reference images during normal playback in the same browser, with timestamps supplied by video-frame callbacks. This avoids differences in canvas colour conversion and downsampling between Chrome and Safari.
2. Tests six individual forward/reverse targets and two rapid target bursts against those images, while requiring a settled seek at the requested position.
3. Independently records post-seek compositor callbacks. It uses bounded timer-based pixel sampling, so absent rendering callbacks do not themselves cause a false decoder failure.
4. Reports page visibility and verifies paused cleanup. A hidden-page run is incomplete, rather than evidence of a visible rendering failure.

Results:

- **PASS:** decoded images and callback timestamps verified while visible.
- **PIXELS_PASS:** decoded images verified, but callback/display evidence is incomplete. This does not certify on-screen smoothness.
- **INCOMPLETE:** the tab was hidden during the check; rerun with Safari visible.
- **FAIL:** startup, seeking, or image verification failed; inspect the included state and media events.

The pixel comparison checks a 32 × 32 grayscale crop containing the cookie and glass, with a mean absolute difference limit of 2/255. Unit fixtures demonstrate that a frozen opening image, stale action frames and black video fail at the moving checkpoints. This check measures decoded-frame delivery, not full-page scroll FPS.

The final iPhone user agent reported iPhone OS 18_7 and Safari Version/27.0.1. Local native Safari 26.4 reproduced the queue and fractional-boundary issues, but its automation surface reported the window hidden. The recorded real-iPhone result above is the device verification.

The test HTML, calibration helper and pixel fixtures live under `frontend/tests`, outside `public`, and are excluded from the production build.
