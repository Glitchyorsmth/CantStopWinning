package com.cantstopwinning;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class CelebrationAudio {

    private Clip clip;
    private volatile boolean playing = false;
    private volatile long celebrationId = 0;

    public void play(InputStream audioStream, Runnable onFinish) {
        stop();
        final long myId = ++celebrationId;

        new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(audioStream));
                AudioFormat fmt = ais.getFormat();

                // Decode to PCM if needed
                AudioInputStream pcmStream;
                if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                        && fmt.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
                    AudioFormat pcmFmt = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            fmt.getSampleRate(), 16, fmt.getChannels(),
                            fmt.getChannels() * 2, fmt.getSampleRate(), false);
                    pcmStream = AudioSystem.getAudioInputStream(pcmFmt, ais);
                } else {
                    pcmStream = ais;
                }

                DataLine.Info info = new DataLine.Info(Clip.class, pcmStream.getFormat());
                synchronized (this) {
                    if (myId != celebrationId) return;
                    clip = (Clip) AudioSystem.getLine(info);
                    clip.open(pcmStream);
                    applyVolume(clip);
                    playing = true;
                    clip.addLineListener(e -> {
                        if (e.getType() == LineEvent.Type.STOP && myId == celebrationId) {
                            playing = false;
                            onFinish.run();
                        }
                    });
                    clip.start();
                }
            } catch (Exception e) {
                CantStopWinningClient.LOGGER.warn("Audio playback failed: {}", e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                if (myId == celebrationId) {
                    playing = false;
                    onFinish.run();
                }
            }
        }, "CSW-Audio").start();
    }

    private void applyVolume(Clip clip) {
        try {
            float vol = (float) CantStopWinningClient.CONFIG.volume;
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (vol <= 0.0001f) {
                    gain.setValue(gain.getMinimum()); // Mute
                } else {
                    // Convert linear 0.0-1.0 to decibels
                    float dB = (float) (20.0 * Math.log10(vol));
                    dB = Math.max(dB, gain.getMinimum());
                    dB = Math.min(dB, gain.getMaximum());
                    gain.setValue(dB);
                }
            }
        } catch (Exception e) {
            CantStopWinningClient.LOGGER.warn("Could not set volume: {}", e.getMessage());
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public synchronized void stop() {
        celebrationId++;
        if (clip != null) {
            try {
                if (clip.isRunning()) clip.stop();
                clip.close();
            } catch (Exception ignored) {}
        }
        playing = false;
        clip = null;
    }
}
