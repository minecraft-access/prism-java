package org.mcaccess.prism;

/**
 * Callback invoked when speech PCM audio samples are synthesized to memory.
 */
@FunctionalInterface
public interface AudioCallback {
    /**
     * Called when audio samples are available.
     *
     * @param samples    the audio PCM samples (32-bit floating point)
     * @param channels   number of audio channels (e.g. 1 for mono, 2 for stereo)
     * @param sampleRate audio sample rate in Hz (e.g. 44100, 22050, 16000)
     */
    void onAudioData(float[] samples, int channels, int sampleRate);
}
