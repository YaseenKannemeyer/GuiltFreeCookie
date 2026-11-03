import { sampleVideoFrame } from './videoFramePixels.js';

// Calibrate on this browser/decoder during real playback. Cross-browser
// canvas resampling and colour conversion must not produce false failures.
export function captureVideoFrames(video) {
  const milestones = [0, 0.7, 1.2, 2.1, 2.7, 3.3, 3.9];
  return new Promise((resolve, reject) => {
    let callback;
    let done = false;
    const frames = [];
    const finish = error => {
      if (done) return;
      done = true;
      clearTimeout(timeout);
      video.cancelVideoFrameCallback(callback);
      video.pause();
      if (error) reject(error);
      else resolve(frames);
    };
    const timeout = setTimeout(() => finish(new Error('Reference playback did not finish within 12s')), 12000);
    const capture = (_, metadata) => {
      try {
        const milestone = milestones[frames.length];
        if (metadata.mediaTime >= milestone - 0.001) {
          if (metadata.mediaTime > milestone + 0.2) throw new Error(`Reference playback skipped the ${milestone}s checkpoint`);
          frames.push({ milestone, expected: metadata.mediaTime, pixels: sampleVideoFrame(video) });
          if (frames.length === milestones.length) return finish();
        }
        callback = video.requestVideoFrameCallback(capture);
      } catch (error) { finish(error); }
    };
    try {
      if (!video.requestVideoFrameCallback) throw new Error('This diagnostic requires video-frame callbacks during playback');
      video.muted = true;
      video.currentTime = 0;
      callback = video.requestVideoFrameCallback(capture);
      // No await before play: keep the initiating button's user gesture.
      Promise.resolve(video.play()).catch(finish);
    } catch (error) { finish(error); }
  });
}
