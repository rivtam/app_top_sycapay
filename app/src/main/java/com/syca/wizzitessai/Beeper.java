package com.syca.wizzitessai;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class Beeper {

    public static void beep(int durationMS) {
        // Create a ToneGenerator for generating a beep sound
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        // Start the tone with a beep sound
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMS);

        // Use a Handler to release the ToneGenerator after the duration
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toneGenerator.release();
            }
        }, durationMS + 50);  // Adding a small delay to release the tone after the beep
    }
}

