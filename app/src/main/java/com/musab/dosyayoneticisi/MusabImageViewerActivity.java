package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

public class MusabImageViewerActivity extends Activity {
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

        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (f == null || !f.isFile() || !f.canRead()) {
            title.setText("Görsel açılamadı");
            return;
        }
        title.setText("Musab Görsel Görüntüleyici  •  " + f.getName());

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                title.setText("Görsel açılamadı: geçersiz veya desteklenmeyen görsel");
                return;
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = calculateSample(bounds.outWidth, bounds.outHeight);
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            if (bitmap == null) throw new IllegalStateException("Bitmap çözülemedi");
            image.setImageBitmap(bitmap);
            image.setOnClickListener(v -> image.setScaleType(
                    image.getScaleType() == ImageView.ScaleType.FIT_CENTER
                            ? ImageView.ScaleType.CENTER_INSIDE
                            : ImageView.ScaleType.FIT_CENTER));
        } catch (Throwable e) {
            title.setText("Görsel açılamadı: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private int calculateSample(int w, int h) {
        long pixels = (long) w * h;
        int sample = 1;
        while (pixels / ((long) sample * sample) > 12000000L && sample < 16) sample *= 2;
        return sample;
    }

    private File getFile() {
        String path = getIntent().getStringExtra("path");
        return path == null ? null : new File(path);
    }
}
