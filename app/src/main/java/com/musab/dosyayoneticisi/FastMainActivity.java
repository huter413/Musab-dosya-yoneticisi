package com.musab.dosyayoneticisi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/** Launcher entry point with stable touch handling and no recursive startup folder scan. */
public class FastMainActivity extends MainActivity {
    private static final int REQ_NOTIFICATIONS = 7001;
    private final Handler permissionHandler = new Handler(Looper.getMainLooper());
    private boolean storageSettingsOpened;
    private boolean notificationAsked;
    private float gestureDownX;
    private float gestureDownY;

    @Override
    public void onCreate(Bundle b) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(10, 10, 12)));
        super.onCreate(b);
        if (root != null) {
            root.setOnTouchListener(null);
            installRightEdgeGestureExclusion();
            View nav = root.getChildAt(1);
            if (nav instanceof LinearLayout) {
                LinearLayout navLayout = (LinearLayout) nav;
                if (navLayout.getChildCount() > 0 && navLayout.getChildAt(0) instanceof Button) {
                    ((Button) navLayout.getChildAt(0)).setOnClickListener(v -> {
                        current = STORAGE;
                        refresh();
                    });
                }
            }
        }
        restoreLargerUi();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            gestureDownX = event.getRawX();
            gestureDownY = event.getRawY();
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = event.getRawX() - gestureDownX;
            float dy = event.getRawY() - gestureDownY;
            if (dx > 120f && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                if (current != null && !isStorageRoot()) goParent();
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean isStorageRoot() {
        try {
            return current != null && current.getCanonicalFile().equals(STORAGE.getCanonicalFile());
        } catch (Exception e) {
            return current != null && current.equals(STORAGE);
        }
    }

    private void installRightEdgeGestureExclusion() {
        if (Build.VERSION.SDK_INT >= 29 && root != null) {
            root.post(() -> {
                int w = root.getWidth();
                int h = root.getHeight();
                if (w > 0 && h > 0) {
                    ArrayList<Rect> rects = new ArrayList<>();
                    rects.add(new Rect(Math.max(0, w - 48), 0, w, h));
                    root.setSystemGestureExclusionRects(rects);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (current != null && !isStorageRoot()) goParent();
        else super.onBackPressed();
    }

    void openWithOptions(File f) {
        if (f == null) return;
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();

        labels.add("Musab Text");
        actions.add(() -> openTextViewer(f));
        labels.add("Musab Görsel Görüntüleyici");
        actions.add(() -> openImageViewer(f));
        labels.add("Musab Video Görüntüleyici");
        actions.add(() -> openVideoViewer(f));
        labels.add("Musab Ses Çalar");
        actions.add(() -> openAudioPlayer(f));
        labels.add("Musab XML Görüntüleyici");
        actions.add(() -> openXmlViewer(f));
        labels.add("Musab Arşiv Görüntüleyici");
        actions.add(() -> openArchiveViewer(f));

        if (skkInstalled()) {
            labels.add("SKK Yükleyicisi");
            actions.add(() -> openWithSkk(f));
        }

        labels.add("Android uygulamasıyla aç");
        actions.add(() -> openExternalChooser(f));
        labels.add("APK / JAR / Derleme merkezi");
        actions.add(() -> openBuild(f));

        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("İle aç")
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .setNegativeButton("Kapat", null)
                .show();
    }

    private boolean isXml(String x) {
        return x.endsWith(".xml") || x.endsWith(".sxml");
    }

    private void openTextViewer(File f) {
        Intent i = new Intent(this, MusabTextActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openXmlViewer(File f) {
        Intent i = new Intent(this, MusabXmlViewerActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openImageViewer(File f) {
        Intent i = new Intent(this, MusabImageViewerActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openVideoViewer(File f) {
        Intent i = new Intent(this, MusabVideoViewerActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openAudioPlayer(File f) {
        Intent i = new Intent(this, MusabAudioPlayerActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openArchiveViewer(File f) {
        Intent i = new Intent(this, MusabArchiveViewerActivity.class);
        i.putExtra("path", f.getAbsolutePath());
        startActivity(i);
    }

    private void openExternalChooser(File f) {
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent external = new Intent(Intent.ACTION_VIEW);
            external.setDataAndType(u, getMime(f.getName().toLowerCase(Locale.ROOT)));
            external.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(Intent.createChooser(external, "Android uygulamasıyla aç"));
        } catch (Exception e) {
            toast("Uygulama bulunamadı: " + e.getMessage());
        }
    }

    @Override
    void navigate(String p) {
        String path = p == null ? "" : p.trim();
        if (path.isEmpty() || path.equals("/") || path.equals("/storage") || path.equals("/storage/emulated")) {
            current = STORAGE;
            refresh();
            return;
        }
        super.navigate(p);
    }

    @Override
    long directorySize(File dir) {
        if (dir == null || !dir.isDirectory()) return dir == null ? 0 : dir.length();
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 30 && !android.os.Environment.isExternalStorageManager()) {
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
        if (nav instanceof LinearLayout) resizeControls((LinearLayout) nav);
        View searchRow = root.getChildAt(2);
        if (searchRow instanceof LinearLayout) resizeControls((LinearLayout) searchRow);
        View actions = root.getChildAt(3);
        if (actions instanceof LinearLayout) resizeButtons((LinearLayout) actions);
    }

    private void resizeControls(LinearLayout row) {
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

    private void resizeButtons(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setTextSize(16);
                child.getLayoutParams().height = 76;
                child.requestLayout();
            }
        }
    }

    @Override
    void fileMenu(File f) {
        if (f == null) return;
        final boolean skk = isSkk(f.getName().toLowerCase(Locale.ROOT)) && skkInstalled();
        final boolean archive = isArchive(f.getName().toLowerCase(Locale.ROOT));
        final boolean apk = f.getName().toLowerCase(Locale.ROOT).endsWith(".apk");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 4, 10, 10);

        addMenuItem(box, "Aç", R.drawable.ic_file, () -> openFile(f));
        if (skk) addMenuItem(box, "SKK ile aç", R.drawable.ic_skk, () -> openWithSkk(f));
        addMenuItem(box, "İle aç", R.drawable.ic_tool_archive, () -> openWithOptions(f));
        addMenuItem(box, "Yeniden adlandır", R.drawable.ic_file, () -> rename(f));
        addMenuItem(box, "Kopyala", R.drawable.ic_file, () -> { clipboard = f; cutMode = false; toast("Kopyalandı"); });
        addMenuItem(box, "Kes", R.drawable.ic_file, () -> { clipboard = f; cutMode = true; toast("Kesildi"); });
        addMenuItem(box, "Sil", R.drawable.ic_file, () -> confirmDelete(f));
        addMenuItem(box, "Özellikler", R.drawable.ic_tool_xml, () -> properties(f));
        addMenuItem(box, "Paylaş", R.drawable.ic_file, () -> share(f));
        if (apk) addMenuItem(box, "APK'ya imzala", R.drawable.ic_tool_apk, () -> apkSignDialog(f));
        addMenuItem(box, "ZIP oluştur", R.drawable.ic_tool_archive, () -> zipSingle(f));

        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(f.getName())
                .setView(box)
                .setNegativeButton("Kapat", null)
                .show();
    }

    private void addMenuItem(LinearLayout box, String label, int iconId, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(12, 8, 12, 8);
        row.setBackgroundColor(Color.rgb(24, 24, 28));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconId);
        row.addView(icon, new LinearLayout.LayoutParams(52, 52));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setGravity(android.view.Gravity.CENTER_VERTICAL);
        text.setSingleLine(false);
        text.setPadding(14, 4, 8, 4);
        row.addView(text, new LinearLayout.LayoutParams(0, 68, 1));

        row.setOnClickListener(v -> action.run());
        box.addView(row, new LinearLayout.LayoutParams(-1, 72));
    }

    @Override
    void openFile(File f) {
        if (f == null) return;
        String x = f.getName().toLowerCase(Locale.ROOT);

        // Özel paket/arşiv dosyaları kendi Musab Activity'lerine gider.
        if (isArchive(x) || isSkk(x)) {
            super.openFile(f);
            return;
        }

        // Normal dosyalar için de Android'in harici chooser'ına düşme:
        // her dosya önce uygun Musab Activity'sinde açılır.
        Intent internal;
        if (isImage(x)) {
            internal = new Intent(this, MusabImageViewerActivity.class);
        } else if (isVideo(x)) {
            internal = new Intent(this, MusabVideoViewerActivity.class);
        } else if (isAudio(x)) {
            internal = new Intent(this, MusabAudioPlayerActivity.class);
        } else {
            // Metin, XML ve tanınmayan dosyalar için ortak düzenleyici.
            internal = new Intent(this, MusabTextActivity.class);
        }
        internal.putExtra("path", f.getAbsolutePath());
        startActivity(internal);
    }

    @Override
    void extractApk(ApplicationInfo ai) {
        if (ai == null) {
            toast("APK bilgisi bulunamadı.");
            return;
        }
        ensureMusabFolders();
        try {
            String label = String.valueOf(ai.loadLabel(getPackageManager())).trim();
            if (label.isEmpty() || "null".equalsIgnoreCase(label)) label = ai.packageName;
            String safe = label.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safe.isEmpty()) safe = "Uygulama";

            File output = uniqueApkFile(new File(APKS, safe + ".apk"));
            copyRecursive(new File(ai.sourceDir), output);

            if (ai.splitSourceDirs != null) {
                for (int n = 0; n < ai.splitSourceDirs.length; n++) {
                    File split = new File(APKS, safe + "-split-" + (n + 1) + ".apk");
                    split = uniqueApkFile(split);
                    copyRecursive(new File(ai.splitSourceDirs[n]), split);
                }
            }
            toast("APK çıkarıldı: " + output.getAbsolutePath());
        } catch (Exception e) {
            toast("APK çıkarma hatası: " + e.getMessage());
        }
    }

    private File uniqueApkFile(File wanted) {
        if (!wanted.exists()) return wanted;
        String name = wanted.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : ".apk";
        int n = 1;
        File candidate;
        do {
            candidate = new File(wanted.getParentFile(), base + " (" + n + ")" + ext);
            n++;
        } while (candidate.exists());
        return candidate;
    }
}
