// Test-only, same-origin decoded-pixel probe. The crop contains the cookie
// and glass, excluding the mostly static tan background.
export function sampleVideoFrame(video) {
  const canvas = document.createElement('canvas');
  canvas.width = canvas.height = 32;
  const context = canvas.getContext('2d', { willReadFrequently: true });
  context.drawImage(video, video.videoWidth * 0.6, 0, video.videoWidth * 0.4, video.videoHeight, 0, 0, 32, 32);
  const { data } = context.getImageData(0, 0, 32, 32);
  const pixels = [];
  for (let i = 0; i < data.length; i += 4) {
    pixels.push(Math.round(data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114));
  }
  return pixels;
}

export function pixelDifference(actual, expected) {
  if (actual.length !== expected.length || !actual.length) return Infinity;
  return actual.reduce((sum, value, index) => sum + Math.abs(value - expected[index]), 0) / actual.length;
}
