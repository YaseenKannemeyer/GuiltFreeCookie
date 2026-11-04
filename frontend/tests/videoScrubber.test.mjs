import test from 'node:test';
import assert from 'node:assert/strict';
import { createVideoScrubber } from '../src/composables/videoScrubber.js';

class Video extends EventTarget {
  readyState = 4;
  duration = 6;
  seeking = false;
  error = null;
  seeks = [];
  time = 0;
  paused = true;
  ownerDocument = new EventTarget();
  get currentTime() { return this.time; }
  set currentTime(value) { this.time = value; this.seeking = true; this.seeks.push(value); }
  play() { this.paused = false; return Promise.resolve(); }
  pause() { this.paused = true; }
  land() { this.seeking = false; this.dispatchEvent(new Event('seeked')); }
}
const options = { fps: 30, duration: 6 };
const center = time => (time * 30 + 0.5) / 30;

test('fast scrolling retains only the latest frame, including reverse scrolling', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  const scrub = createVideoScrubber(video, options);
  scrub.set(1);
  scrub.set(2);
  scrub.set(3);
  assert.deepEqual(video.seeks, [center(1)]);
  video.land();
  t.mock.timers.tick(0);
  assert.deepEqual(video.seeks, [center(1), center(3)]);
  scrub.set(0.5);
  video.land();
  t.mock.timers.tick(0);
  assert.deepEqual(video.seeks, [center(1), center(3), center(0.5)]);
  video.land();
  scrub.set(0.501);
  assert.equal(video.seeks.length, 3, 'sub-frame motion does not decode another frame');
  scrub.destroy();
});

test('metadata arriving late applies the latest target and clamps to the actual duration', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  video.readyState = 0;
  const scrub = createVideoScrubber(video, options);
  scrub.set(5);
  assert.equal(video.seeks.length, 0);
  video.duration = 4;
  video.readyState = 1;
  video.dispatchEvent(new Event('loadedmetadata'));
  assert.equal(video.seeks[0], 4 - 1 / 60);
  video.land();
  t.mock.timers.tick(0);
  scrub.set(-1);
  assert.equal(video.seeks.at(-1), center(0));
  scrub.destroy();
});

test('a lost seeked event recovers the final target after scrolling stops, with bounded retries', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  const scrub = createVideoScrubber(video, options);
  scrub.set(1);
  scrub.set(3);
  t.mock.timers.tick(500);
  assert.deepEqual(video.seeks, [center(1), center(3)]);
  t.mock.timers.tick(500);
  t.mock.timers.tick(5000);
  assert.equal(video.seeks.length, 3, 'broken media does not retry forever');
  scrub.destroy();
});

test('destroy cancels pending seeks and late media events', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  const scrub = createVideoScrubber(video, options);
  scrub.set(1);
  scrub.set(4);
  scrub.destroy();
  video.land();
  video.dispatchEvent(new Event('loadeddata'));
  scrub.set(2);
  t.mock.timers.tick(1000);
  assert.deepEqual(video.seeks, [center(1)]);
  assert.equal(video.paused, true);
});

test('a pending touch playback cannot restart seeking after unmount', async () => {
  const video = new Video();
  let resolvePlay;
  video.play = () => new Promise(resolve => { resolvePlay = resolve; });
  const scrub = createVideoScrubber(video, { ...options, prime: true });
  scrub.set(2);
  scrub.destroy();
  resolvePlay();
  await Promise.resolve();
  assert.deepEqual(video.seeks, []);
  assert.equal(video.paused, true);
});

test('autoplay rejection still permits seeking and a later touch retries priming', async () => {
  const video = new Video();
  let plays = 0;
  video.play = () => ++plays === 1 ? Promise.reject(new Error('NotAllowedError')) : Promise.resolve();
  const scrub = createVideoScrubber(video, { ...options, prime: true });
  scrub.set(2);
  await Promise.resolve();
  assert.deepEqual(video.seeks, [center(2)]);
  video.land();
  video.ownerDocument.dispatchEvent(new Event('pointerdown'));
  await Promise.resolve();
  assert.equal(plays, 2);
  assert.equal(video.paused, true);
  scrub.destroy();
});

test('a tap starts playback even before Safari has loaded metadata', async () => {
  const video = new Video();
  video.readyState = 0;
  let plays = 0;
  video.play = () => { plays += 1; return Promise.resolve(); };
  const scrub = createVideoScrubber(video, { ...options, prime: true });
  assert.equal(plays, 1, 'play is called synchronously within the initiating gesture');
  assert.equal(await scrub.ready, true);
  scrub.set(1.2);
  assert.deepEqual(video.seeks, []);
  video.readyState = 2;
  video.dispatchEvent(new Event('loadeddata'));
  assert.deepEqual(video.seeks, [center(1.2)]);
  scrub.destroy();
});

test('touch priming waits for a presented frame before pausing and seeking', async () => {
  const video = new Video();
  let paint;
  video.requestVideoFrameCallback = callback => { paint = callback; return 1; };
  video.cancelVideoFrameCallback = () => {};
  const scrub = createVideoScrubber(video, { ...options, prime: true });
  scrub.set(1.2);
  await Promise.resolve();
  assert.equal(video.paused, false, 'play() resolving alone must not pause the decoder');
  assert.deepEqual(video.seeks, []);
  paint(0, { mediaTime: 0 });
  assert.equal(await scrub.ready, true);
  assert.equal(video.paused, true);
  assert.deepEqual(video.seeks, [center(1.2)]);
  scrub.destroy();
});

test('destroy cancels a pending presented-frame warmup', async () => {
  const video = new Video();
  let canceled = 0;
  video.requestVideoFrameCallback = () => 1;
  video.cancelVideoFrameCallback = () => { canceled += 1; };
  const scrub = createVideoScrubber(video, { ...options, prime: true });
  await Promise.resolve();
  scrub.destroy();
  assert.equal(await scrub.ready, false);
  assert.ok(canceled > 0);
  assert.deepEqual(video.seeks, []);
});

test('rapid reverse targets advance without a rendering callback', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  video.ownerDocument.defaultView = {
    requestAnimationFrame() { throw new Error('No rendering callback is available'); },
  };
  const scrub = createVideoScrubber(video, options);
  for (const target of [1, 3, 2, 0.7]) scrub.set(target);
  video.land();
  assert.deepEqual(video.seeks, [center(1)], 'never seek reentrantly inside seeked');
  scrub.set(0.5);
  t.mock.timers.tick(0);
  assert.deepEqual(video.seeks, [center(1), center(0.5)]);
  scrub.destroy();
});

test('attaching to a loaded video preserves its position until a target is requested', () => {
  const video = new Video();
  video.time = 3.9;
  const scrub = createVideoScrubber(video, options);
  assert.deepEqual(video.seeks, []);
  scrub.set(1.2);
  assert.deepEqual(video.seeks, [center(1.2)]);
  scrub.destroy();
});

test('teardown cancels a queued post-seek handoff', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const video = new Video();
  const scrub = createVideoScrubber(video, options);
  scrub.set(1);
  scrub.set(3);
  video.land();
  scrub.destroy();
  t.mock.timers.tick(0);
  assert.deepEqual(video.seeks, [center(1)]);
});

test('fractional reverse targets lie inside the intended frame, away from its boundary', () => {
  const video = new Video();
  video.time = 3.9;
  const scrub = createVideoScrubber(video, options);
  const frame = 64;
  scrub.set(frame / 30);
  assert.ok(video.seeks[0] > frame / 30);
  assert.ok(video.seeks[0] < (frame + 1) / 30);
  assert.equal(Math.floor(video.seeks[0] * 30), frame);
  scrub.destroy();
});
