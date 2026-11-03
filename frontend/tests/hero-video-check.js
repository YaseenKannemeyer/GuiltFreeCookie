import { sampleVideoFrame, pixelDifference } from './videoFramePixels.js';
import { captureVideoFrames } from './captureVideoFrames.js';
import { createVideoScrubber } from '../src/composables/videoScrubber.js';

const video = document.querySelector('video');
const result = document.querySelector('#result');
const button = document.querySelector('#run');
const revision = 'safari-calibrated-frames-v3';
const mediaState = () => ({
  time: video.currentTime, readyState: video.readyState, networkState: video.networkState,
  seeking: video.seeking, paused: video.paused,
  error: video.error && { code: video.error.code, message: video.error.message },
  buffered: Array.from({ length: video.buffered.length }, (_, i) => [video.buffered.start(i), video.buffered.end(i)]),
});
button.addEventListener('click', async () => {
  button.disabled = true;
  result.textContent = 'Recording reference images during playback (about 4 seconds)…';
  const events = [];
  const startedAt = performance.now();
  const eventNames = ['loadstart', 'loadedmetadata', 'loadeddata', 'playing', 'pause', 'seeking', 'seeked', 'waiting', 'stalled', 'error'];
  const record = event => {
    events.push({ event: event.type, ms: Math.round(performance.now() - startedAt), ...mediaState() });
    if (events.length > 24) events.shift();
  };
  eventNames.forEach(name => video.addEventListener(name, record));
  let scrubber;
  const samples = [];
  let references = [];
  let lastDecoded = null;
  let lastPixelDifference = null;
  try {
    references = await captureVideoFrames(video);
    // Playback already proved this decoder can present frames. Now test
    // paused seeking independently of the production warmup path.
    scrubber = createVideoScrubber(video, { fps: 30, duration: 6 });
    result.textContent = 'Checking paused seeks against this browser’s recorded images…';
    const at = milestone => references.find(frame => frame.milestone === milestone);
    for (const steps of [[1.2], [2.7], [3.3], [3.9], [2.1], [0], [1, 3, 2, 0.7], [0, 3.9, 1, 2.7]]) {
      const milestone = steps.at(-1);
      const reference = at(milestone);
      const expected = reference.expected;
      const targets = [...steps.slice(0, -1), expected];
      const started = performance.now();
      const sample = await new Promise((resolve, reject) => {
        let callback;
        let animation;
        let decoded = null;
        let firstMatch = null;
        lastDecoded = null;
        lastPixelDifference = null;
        const cleanup = () => {
          clearTimeout(timeout);
          if (callback !== undefined) video.cancelVideoFrameCallback(callback);
          cancelAnimationFrame(animation);
        };
        const timeout = setTimeout(() => {
          cleanup();
          reject(new Error(`Decoded pixels did not match the ${expected}s reference within 4s`));
        }, 4000);
        const inspect = (_, metadata) => {
          lastDecoded = metadata.mediaTime;
          if (Math.abs(metadata.mediaTime - expected) <= 1 / 30 + 0.001) decoded = metadata.mediaTime;
          else callback = video.requestVideoFrameCallback(inspect);
        };
        const compare = () => {
          try {
            if (!video.seeking && video.readyState >= 2 && Math.abs(video.currentTime - expected) <= 1 / 30 + 0.001) {
              lastPixelDifference = pixelDifference(sampleVideoFrame(video), reference.pixels);
              if (lastPixelDifference <= 2) {
                firstMatch ??= performance.now();
                // Give an outstanding compositor callback time to arrive;
                // missing callbacks are reported separately, never invented.
                if (decoded !== null || performance.now() - firstMatch >= 150) {
                  cleanup();
                  resolve({ milestone, expected, decoded, pixelDifference: +lastPixelDifference.toFixed(3), pixelLatencyMs: Math.round(firstMatch - started) });
                  return;
                }
              } else firstMatch = null;
            }
          } catch (error) {
            cleanup();
            reject(error);
            return;
          }
          animation = requestAnimationFrame(compare);
        };
        if (video.requestVideoFrameCallback) callback = video.requestVideoFrameCallback(inspect);
        for (const target of targets) scrubber.set(target);
        animation = requestAnimationFrame(compare);
      });
      samples.push(sample);
    }
    scrubber.destroy();
    if (!video.paused) throw new Error('Video still playing after cleanup');
    const missingCallbacks = samples.some(sample => sample.decoded === null);
    result.textContent = JSON.stringify({
      status: missingCallbacks ? 'PIXELS_PASS' : 'PASS', revision,
      message: missingCallbacks
        ? 'Decoded images match the reference frames. Some compositor callbacks were missing; on-screen smoothness still needs visual confirmation.'
        : 'Decoded images and compositor timestamps verified.',
      userAgent: navigator.userAgent, samples, pausedAfterCleanup: video.paused,
    }, null, 2);
  } catch (error) {
    result.textContent = JSON.stringify({ status: 'FAIL', revision, message: error.message, userAgent: navigator.userAgent, media: mediaState(), lastDecoded, lastPixelDifference, samples, events }, null, 2);
  } finally {
    eventNames.forEach(name => video.removeEventListener(name, record));
    scrubber?.destroy();
    video.pause();
    button.disabled = false;
  }
});
