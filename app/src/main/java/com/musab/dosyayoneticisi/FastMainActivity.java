package com.musab.dosyayoneticisi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;

/** Launcher entry point with normal file-manager touch handling. */
public class FastMainActivity extends MainActivity {
    private static final int REQ_NOTIFICATIONS = 7001;
    private final Handler permissionHandler = new Handler(Looper.getMainLooper());
    private boolean storageSettingsOpened;
    private boolean notificationAsked;

    @Override
    public void onCreate(Bundle b) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(10, 10, 12)));
        super.onCreate(b);
        // Keep child controls clickable; the old root swipe listener could consume touch events.
        if (root != null) root.setOnTouchListener(null);
        restoreLargerUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
            // The storage permission is handled first, like a file manager's first launch.
            if (!storageSettingsOpened) {
                storageSettingsOpened = true;
                permissionHandler.postDelayed(this::openAllFilesSettings, 250);
            }
            return;
        }
        permissionHandler.postDelayed(this::maybeRequestNotifications, 500);
    }

    @Override
    void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
            if (!storageSettingsOpened) {
                storageSettingsOpened = true;
                openAllFilesSettings();
            }
        } else if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 32
                && checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, REQ_STORAGE);
        }
    }

    private void maybeRequestNotifications() {
        if (notificationAsked || Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationAsked = true;
            return;
        }
        notificationAsked = true;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
    }

    void openAllFilesSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            } catch (Exception ignored) {
                toast("Dosya erişimi ekranı açılamadı");
            }
        }
    }

    private void restoreLargerUi() {
        if (root == null || root.getChildCount() < 4) return;

        View title = root.getChildAt(0);
        if (title instanceof TextView) {
            TextView t = (TextView) title;
            t.setText("Musab Dosya Yöneticisi");
            t.setTextSize(29);
            t.setPadding(14, 18, 14, 12);
        }

        View nav = root.getChildAt(1);
        if (nav instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) nav;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof Button) {
                    ((Button) child).setTextSize(16);
                    child.getLayoutParams().height = 76;
                    child.requestLayout();
                } else if (child instanceof EditText) {
                    ((EditText) child).setTextSize(19);
                }
            }
        }

        View searchRow = root.getChildAt(2);
        if (searchRow instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) searchRow;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof Button) {
                    ((Button) child).setTextSize(16);
                    child.getLayoutParams().height = 76;
                    child.requestLayout();
                } else if (child instanceof EditText) {
                    ((EditText) child).setTextSize(19);
                }
            }
        }

        View actions = root.getChildAt(3);
        if (actions instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) actions;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof Button) {
                    ((Button) child).setTextSize(16);
                    child.getLayoutParams().height = 76;
                    child.requestLayout();
                }
            }
        }
    }

    @Override
    void openFile(File f) {
        if (f == null) return;
        String x = f.getName().toLowerCase(java.util.Locale.ROOT);
        if (isArchive(x) || isSkk(x)) {
            super.openFile(f);
            return;
        }
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            String mime = getMime(x);
            Intent external = new Intent(Intent.ACTION_VIEW);
            external.setDataAndType(u, mime);
            external.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent internal = null;
            if (isText(x)) internal = new Intent(this, MusabTextActivity.class);
            else if (isImage(x)) internal = new Intent(this, MusabImageViewerActivity.class);
            else if (isVideo(x)) internal = new Intent(this, MusabVideoViewerActivity.class);
            else if (isAudio(x)) internal = new Intent(this, MusabAudioPlayerActivity.class);
            if (internal != null) {
                internal.putExtra("path", f.getAbsolutePath());
                internal.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(external, "Bununla aç");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{internal});
                startActivity(chooser);
            } else {
                startActivity(Intent.createChooser(external, "Bununla aç"));
            }
        } catch (Exception e) {
            toast("Dosya açılamadı: " + e.getMessage());
        }
    }
}
