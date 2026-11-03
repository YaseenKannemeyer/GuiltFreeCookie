import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { pixelDifference } from './videoFramePixels.js';
const { frames } = JSON.parse(readFileSync(new URL('./hero-video-frames.json', import.meta.url)));
const frameAt = time => frames.find(frame => frame.expected === time).pixels;

test('pixel diagnostic rejects a frozen opening frame at every moving checkpoint', () => {
  for (const time of [2.1, 2.7, 3.3, 3.9]) {
    assert.ok(pixelDifference(frameAt(0), frameAt(time)) > 8);
  }
});

test('pixel diagnostic rejects stale adjacent motion checkpoints and black video', () => {
  for (const [before, after] of [[2.7, 3.3], [3.3, 3.9], [3.9, 2.1]]) {
    assert.ok(pixelDifference(frameAt(before), frameAt(after)) > 8);
  }
  for (const frame of frames) {
    assert.ok(pixelDifference(new Array(1024).fill(0), frame.pixels) > 2);
    assert.ok(pixelDifference(frame.pixels.map(value => value + 1), frame.pixels) <= 2,
      'small decoder colour differences remain acceptable');
  }
});
