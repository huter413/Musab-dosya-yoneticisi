package com.musab.dosyayoneticisi;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Launcher entry point that fixes the blank startup screen caused by the
 * recursive directory-size scan running on the UI thread.
 * Directory sizes are calculated after the first screen is displayed.
 */
public class FastMainActivity extends MainActivity {
    private static final ExecutorService SIZE_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ConcurrentHashMap<String, Long> SIZE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> SIZE_PENDING = new ConcurrentHashMap<>();

    @Override
    public void onCreate(Bundle b) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(10, 10, 12)));
        super.onCreate(b);
    }

    @Override
    long directorySize(File dir) {
        final String key;
        try {
            key = dir.getCanonicalPath();
        } catch (Exception e) {
            return 0L;
        }

        Long cached = SIZE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (SIZE_PENDING.putIfAbsent(key, Boolean.TRUE) == null) {
            SIZE_EXECUTOR.execute(() -> {
                long size = super.directorySize(dir);
                SIZE_CACHE.put(key, size);
                SIZE_PENDING.remove(key);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        refresh();
                    }
                });
            });
        }
        return 0L;
    }
}
