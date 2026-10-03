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
import android.view.Gravity;
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

public class FastMainActivity extends MainActivity {
    private static final int REQ_NOTIFICATIONS = 7001;
    private final Handler permissionHandler = new Handler(Looper.getMainLooper());
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
                if (current != null && !isFilesystemRoot()) goParent();
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean isFilesystemRoot() {
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
        if (current != null && !isFilesystemRoot()) goParent();
        else super.onBackPressed();
    }

    @Override
    void navigate(String p) {
        String path = p == null ? "" : p.trim();
        if (path.isEmpty() || path.equals("/")) {
            current = new File("/");
            refresh();
            return;
        }
        super.navigate(path);
    }

    /*
     * İlk açılışta dosya erişimi isteme ekranı korunur. Kullanıcıya ZArchiver
     * benzeri şekilde tüm dosyalara erişim veya klasör seçme seçeneği gösterilir.
     * İzin verildikten sonra dosya yöneticisi / kök dizinde çalışmaya devam eder.
     */
    @Override
    void requestStorageAccess() {
        // İlk açılışta kullanıcı doğrudan sistemin "Tüm dosyalara erişim"
        // ekranına gönderilir. Kullanıcı izin verdikten sonra dosya yöneticisi
        // depolamadaki erişilebilir klasör ve dosyaları gösterebilir.
        try {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            } catch (Exception ignoredAgain) {
                // Sistem bu ekranı sağlamıyorsa uygulama normal şekilde devam eder.
            }
        }
        if (root != null && list != null) refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // İlk açılışta requestStorageAccess() izin ekranını gösterir.
        // Burada tekrar tekrar ayar ekranı açılmasını engelliyoruz.
        permissionHandler.postDelayed(this::maybeRequestNotifications, 350);
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
        if (is3DModel(f.getName().toLowerCase(Locale.ROOT))) {
            labels.add("Musab 3D Model Görüntüleyici");
            actions.add(() -> open3DViewer(f));
        }

        if (skkInstalled()) {
            labels.add("SKK Yükleyicisi");
            actions.add(() -> openWithSkk(f));
        }

        labels.add("Android uygulamasıyla aç");
        actions.add(() -> openExternalChooser(f));
        labels.add("APK / JAR / Derleme merkezi");
        actions.add(() -> openBuild(f));
        labels.add("APK'ya imzala");
        actions.add(() -> apkSignDialog(f));
        labels.add("Decompile APK");
        actions.add(() -> decompileApk(f));
        labels.add("Patchle");
        actions.add(() -> patchApk(f));
        labels.add("Material.bin derle");
        actions.add(() -> materialBinBuild(f));
        labels.add("Özellikler");
        actions.add(() -> properties(f));
        labels.add("Kontrol et");
        actions.add(() -> properties(f));
        labels.add("ZIP oluştur");
        actions.add(() -> zipSingle(f));

        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("İle aç")
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .setNegativeButton("Kapat", null)
                .show();
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

    private void open3DViewer(File f) {
        Intent i = new Intent(this, Musab3DModelViewerActivity.class);
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
    long directorySize(File dir) {
        return 0;
    }

    private void restoreLargerUi() {
        if (root == null || root.getChildCount() < 4) return;
        View title = root.getChildAt(0);
        if (title instanceof TextView) {
            TextView t = (TextView) title;
            t.setText("Musab Dosya Yöneticisi");
            t.setTextSize(29);
            t.setGravity(Gravity.CENTER_VERTICAL);
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
                ((Button) child).setGravity(Gravity.CENTER);
                ((Button) child).setIncludeFontPadding(false);
                child.getLayoutParams().height = 76;
                child.requestLayout();
            } else if (child instanceof EditText) {
                ((EditText) child).setTextSize(19);
                ((EditText) child).setGravity(Gravity.CENTER_VERTICAL);
            }
        }
    }

    private void resizeButtons(LinearLayout row) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child instanceof Button) {
                ((Button) child).setTextSize(16);
                ((Button) child).setGravity(Gravity.CENTER);
                ((Button) child).setIncludeFontPadding(false);
                child.getLayoutParams().height = 76;
                child.requestLayout();
            }
        }
    }

    @Override
    void toolsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(8, 10, 8, 10);

        AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Musab Araçları")
                .setView(box)
                .setNegativeButton("Kapat", null)
                .create();

        addToolRow(box, "APK Çıkar / Uygulamalar", R.drawable.ic_tool_apk, () -> apps(), dialog);
        addToolRow(box, "APK Arşiv İçeriği", R.drawable.ic_tool_archive, () -> apkContentsDialog(), dialog);
        addToolRow(box, "Python düzenleyici", R.drawable.ic_tool_python, () -> codeEditor("Python"), dialog);
        addToolRow(box, "IPython düzenleyici", R.drawable.ic_tool_ipython, () -> codeEditor("IPython"), dialog);
        addToolRow(box, "Pillow düzenleyici", R.drawable.ic_tool_pillow, () -> codeEditor("Pillow"), dialog);
        addToolRow(box, "AndroidManifest / XML düzenleyici", R.drawable.ic_tool_xml, () -> xmlEditor(), dialog);
        addToolRow(box, "Terminal", R.drawable.ic_tool_terminal, () -> terminal(), dialog);
        addToolRow(box, "MusabFolder'a git", R.drawable.ic_tool_folder, () -> { current = MUSAB; refresh(); }, dialog);

        dialog.show();
    }

    private void addToolRow(LinearLayout box, String title, int iconId, final Runnable action, final AlertDialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(12, 6, 12, 6);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconId);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(icon, new LinearLayout.LayoutParams(58, 72));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(Color.WHITE);
        label.setTextSize(17);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        label.setIncludeFontPadding(false);
        label.setMaxLines(2);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(16, 0, 10, 0);
        row.addView(label, new LinearLayout.LayoutParams(0, 72, 1));

        row.setBackgroundColor(Color.rgb(24, 24, 28));
        row.setOnClickListener(v -> {
            dialog.dismiss();
            action.run();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 82);
        lp.setMargins(0, 4, 0, 4);
        box.addView(row, lp);
    }

    @Override
    void fileMenu(File f) {
        if (f == null) return;
        final boolean skk = isSkk(f.getName().toLowerCase(Locale.ROOT)) && skkInstalled();
        final boolean apk = f.getName().toLowerCase(Locale.ROOT).endsWith(".apk");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 4, 10, 10);

        addMenuItem(box, "Aç", R.drawable.ic_file, () -> openFile(f));
        if (skk) addMenuItem(box, "SKK ile aç", R.drawable.ic_skk, () -> openWithSkk(f));
        addMenuItem(box, "İle aç", R.drawable.ic_tool_archive, () -> openWithOptions(f));
        addMenuItem(box, "Kontrol et", R.drawable.ic_tool_xml, () -> properties(f));
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

    private boolean is3DModel(String x) {
        return x.endsWith(".obj") || x.endsWith(".stl") || x.endsWith(".ply");
    }

    private void addMenuItem(LinearLayout box, String label, int iconId, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(12, 8, 12, 8);
        row.setBackgroundColor(Color.rgb(24, 24, 28));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconId);
        row.addView(icon, new LinearLayout.LayoutParams(52, 52));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setIncludeFontPadding(false);
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

        if (is3DModel(x)) {
            open3DViewer(f);
            return;
        }

        if (isArchive(x) || isSkk(x)) {
            super.openFile(f);
            return;
        }

        Intent internal;
        if (isImage(x)) {
            internal = new Intent(this, MusabImageViewerActivity.class);
        } else if (isVideo(x)) {
            internal = new Intent(this, MusabVideoViewerActivity.class);
        } else if (isAudio(x)) {
            internal = new Intent(this, MusabAudioPlayerActivity.class);
        } else {
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