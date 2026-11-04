/** Latest-frame-wins seeking. No render loop and no queue of stale scroll frames. */
export function createVideoScrubber(video, { fps, duration, prime = false }) {
  let target = Number.isFinite(video.currentTime) ? video.currentTime : 0;
  let busy = false;
  let alive = true;
  let priming = false;
  let primed = false;
  let cancelWarmup = () => {};
  let watchdog;
  let nextSeek;
  let retries = 0;

  function seek(force = false) {
    if (!alive || priming || video.readyState < 1 || video.error) return;
    if ((busy || video.seeking) && !force) return;
    const end = Math.max(0, Math.min(duration, Number.isFinite(video.duration) ? video.duration : duration) - 0.5 / fps);
    const time = Math.min(target, end);
    // currentTime changes as soon as a seek is requested, before it is painted.
    if (!video.seeking && Math.abs(video.currentTime - time) < 0.5 / fps) return;
    clearTimeout(watchdog);
    busy = true;
    try {
      video.currentTime = time;
    } catch {
      busy = false; // A later media event or scroll update can retry.
      return;
    }
    // Recover even when scrolling has stopped and a browser loses seeked.
    // Bound retries so a broken decoder cannot keep work running forever.
    if (retries < 2) watchdog = setTimeout(() => {
      busy = false;
      retries += 1;
      seek(true);
    }, 500);
  }

  function onSeeked() {
    clearTimeout(watchdog);
    retries = 0;
    // Yield out of the media event without depending on animation frames:
    // Safari may suspend those while a tab/window is obscured.
    busy = true;
    clearTimeout(nextSeek);
    nextSeek = setTimeout(() => {
      nextSeek = undefined;
      busy = false;
      seek();
    }, 0);
  }

  function firstPaint() {
    if (!video.requestVideoFrameCallback) return Promise.resolve(true);
    return new Promise(resolve => {
      let callback;
      const finish = (painted) => {
        clearTimeout(timeout);
        video.cancelVideoFrameCallback(callback);
        resolve(painted);
      };
      const timeout = setTimeout(() => finish(false), 8000);
      callback = video.requestVideoFrameCallback(() => finish(true));
      cancelWarmup = () => finish(false);
    });
  }

  async function onReady() {
    if (!alive || priming) return false;
    // Some touch browsers need one inline playback before painting seeks.
    // Rejection is harmless: retain poster/seeking and retry on a gesture.
    if (prime && !primed) {
      priming = true;
      const painted = firstPaint();
      try {
        // Call synchronously, even before metadata: this may be the only
        // user gesture Safari gives us to start fetching/decoding the clip.
        await video.play();
        // play() resolving does not prove a frame has been presented.
        primed = await painted;
      } catch {
        // Low Power Mode / autoplay policy can reject muted playback.
      } finally {
        cancelWarmup();
        video.pause();
        priming = false;
      }
    }
    seek();
    return !prime || primed;
  }

  video.muted = true;
  video.addEventListener("seeked", onSeeked);
  video.addEventListener("loadedmetadata", onReady);
  video.addEventListener("loadeddata", onReady);
  if (prime) video.ownerDocument?.addEventListener("pointerdown", onReady, { passive: true });
  const ready = onReady();

  return {
    ready,
    set(time) {
      if (!alive || !Number.isFinite(time)) return;
      // Seek inside the selected frame. At an exact fractional boundary,
      // Safari can round down and decode the preceding frame on reversal.
      const next = (Math.round(Math.max(0, time) * fps) + 0.5) / fps;
      if (target !== next) retries = 0;
      target = next;
      seek();
    },
    destroy() {
      alive = false;
      cancelWarmup();
      clearTimeout(watchdog);
      clearTimeout(nextSeek);
      video.pause();
      video.removeEventListener("seeked", onSeeked);
      video.removeEventListener("loadedmetadata", onReady);
      video.removeEventListener("loadeddata", onReady);
      video.ownerDocument?.removeEventListener("pointerdown", onReady);
    },
  };
}
