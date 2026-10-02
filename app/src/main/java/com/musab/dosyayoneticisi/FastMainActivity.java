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
import androidx.core.content.FileProvider;
import java.io.File;

/** Launcher entry point. Keeps the normal MainActivity behavior without altering touch handling. */
public class FastMainActivity extends MainActivity {
    private static final int REQ_NOTIFICATIONS = 7001;
    private final Handler permissionHandler = new Handler(Looper.getMainLooper());
    private boolean storageDialogShown;
    private boolean notificationAsked;

    @Override
    public void onCreate(Bundle b) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(10, 10, 12)));
        super.onCreate(b);
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionHandler.postDelayed(this::maybeRequestNotifications, 500);
    }

    @Override
    void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
            if (storageDialogShown) return;
            storageDialogShown = true;
            new AlertDialog.Builder(this)
                    .setTitle("Dosya erişimi gerekli")
                    .setMessage("Musab Dosya Yöneticisi dosyalarını doğrudan gösterebilmek için dosya erişimi istiyor.")
                    .setPositiveButton("İzin ver", (d, w) -> openAllFilesSettings())
                    .setNeutralButton("Klasör seç", (d, w) -> openTreeAccess())
                    .setNegativeButton("Şimdi değil", null)
                    .show();
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
