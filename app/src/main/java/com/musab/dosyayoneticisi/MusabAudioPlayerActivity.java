package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

public class MusabAudioPlayerActivity extends Activity {
    private MediaPlayer mp;
    private Button play;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        File f = getFile();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(12, 12, 14));
        root.setPadding(24, 24, 24, 24);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(20);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        play = new Button(this);
        play.setText("Oynat");
        play.setEnabled(false);
        root.addView(play, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        if (f == null || !f.isFile() || !f.canRead()) {
            status.setText("Ses açılamadı");
            return;
        }
        status.setText("Musab Ses Dinleyici\n" + f.getName());

        try {
            mp = new MediaPlayer();
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                mp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
            }
            mp.setDataSource(f.getAbsolutePath());
            mp.setOnPreparedListener(player -> {
                play.setEnabled(true);
                play.setText("Oynat");
            });
            mp.setOnCompletionListener(player -> play.setText("Tekrar oynat"));
            mp.setOnErrorListener((player, what, extra) -> {
                status.setText("Ses açılamadı: desteklenmeyen veya bozuk ses");
                play.setEnabled(false);
                return true;
            });
            mp.prepareAsync();
        } catch (Throwable e) {
            status.setText("Ses açılamadı: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            releasePlayer();
        }

        play.setOnClickListener(v -> {
            if (mp == null) return;
            if (mp.isPlaying()) {
                mp.pause();
                play.setText("Devam et");
            } else {
                try {
                    if (mp.getCurrentPosition() >= mp.getDuration()) mp.seekTo(0);
                    mp.start();
                    play.setText("Duraklat");
                } catch (IllegalStateException ignored) {}
            }
        });
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (mp != null) {
            try { mp.stop(); } catch (Exception ignored) {}
            mp.release();
            mp = null;
        }
    }

    private File getFile() {
        String path = getIntent().getStringExtra("path");
        return path == null ? null : new File(path);
    }
}
