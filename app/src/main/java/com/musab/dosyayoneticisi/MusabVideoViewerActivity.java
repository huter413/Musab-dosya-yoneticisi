package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import java.io.File;

public class MusabVideoViewerActivity extends Activity {
    private VideoView video;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        File f = getFile();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(16, 16, 16, 16);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        video = new VideoView(this);
        root.addView(video, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (f == null || !f.isFile() || !f.canRead()) {
            title.setText("Video açılamadı");
            return;
        }
        title.setText("Musab Video Görüntüleyici  •  " + f.getName());

        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        video.setOnPreparedListener(mp -> video.start());
        video.setOnErrorListener((mp, what, extra) -> {
            title.setText("Video açılamadı: desteklenmeyen veya bozuk video");
            return true;
        });

        try {
            // Use the real local file path. This avoids FileProvider/content-URI
            // decoder failures on devices where MediaPlayer cannot resolve it.
            video.setVideoPath(f.getAbsolutePath());
            video.requestFocus();
        } catch (Throwable e) {
            title.setText("Video açılamadı: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    @Override protected void onDestroy() {
        if (video != null) {
            video.stopPlayback();
            video = null;
        }
        super.onDestroy();
    }

    private File getFile() {
        String path = getIntent().getStringExtra("path");
        return path == null ? null : new File(path);
    }
}
