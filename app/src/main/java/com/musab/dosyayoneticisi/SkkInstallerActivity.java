package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;import android.content.pm.SigningInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class SkkInstallerActivity extends Activity {
    private Uri sourceUri;
    private File apkFile;
    private ProgressBar progress;
    private TextView state;
    private TextView appNameView;
    private ImageView appIconView;
    private String targetName = "Uygulama";
    private Drawable targetIcon;
    private String targetPackage;
    private boolean updateMode;
    private long incomingVersion = -1;
    private long installedVersion = -1;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        sourceUri = getIntent() == null ? null : getIntent().getData();
        configureSmallWindow();
        showLoading();
        new Handler(Looper.getMainLooper()).postDelayed(this::preparePackage, 180);
    }

    private void configureSmallWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = getWindow();
        w.setBackgroundDrawableResource(android.R.color.transparent);
        w.setDimAmount(0.55f);
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private LinearLayout base() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(16));
        root.setBackgroundColor(Color.rgb(20,20,24));
        return root;
    }

    private void showLoading() {
        LinearLayout root = base();

        TextView sparkle = text("✦  SKK  ✦", 15, Color.rgb(190,170,255));
        root.addView(sparkle, new LinearLayout.LayoutParams(-1, dp(30)));

        appIconView = new ImageView(this);
        appIconView.setImageResource(android.R.drawable.sym_def_app_icon);
        root.addView(appIconView, new LinearLayout.LayoutParams(dp(58), dp(58)));

        appNameView = text(targetName, 18, Color.WHITE);
        root.addView(appNameView, new LinearLayout.LayoutParams(-1, dp(34)));

        state = text("Hazırlanıyor...", 14, Color.LTGRAY);
        root.addView(state, new LinearLayout.LayoutParams(-1, dp(32)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(10)));

        TextView cute = text("♡  Paket hazırlanıyor  ♡", 13, Color.rgb(210,210,220));
        cute.setPadding(0, dp(12), 0, 0);
        root.addView(cute, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
        resizeWindow();
    }

    private void resizeWindow() {
        Window w = getWindow();
        w.setLayout(dp(300), WindowManager.LayoutParams.WRAP_CONTENT);
        w.setGravity(Gravity.CENTER);
    }

    private void preparePackage() {
        if (sourceUri == null) {
            showError("Bir .skk dosyası seçilmedi.");
            return;
        }
        new Thread(() -> {
            try {
                File tmp = new File(getCacheDir(), "pending-" + System.currentTimeMillis() + ".apk");
                copyUriWithProgress(sourceUri, tmp);
                if (!validSkk(tmp)) throw new IOException("Bu dosya geçerli bir .skk Android paketi değil.");
                apkFile = tmp;
                readTargetAppInfo(tmp);
                runOnUiThread(this::showInstallQuestion);
            } catch (Exception e) {
                runOnUiThread(() -> showError("SKK açılamadı: " + e.getMessage()));
            }
        }).start();
    }

    private void copyUriWithProgress(Uri uri, File out) throws Exception {
        long total = -1;
        try (android.database.Cursor c = getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) total = c.getLong(0);
        }
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new IOException("SKK içeriği okunamadı.");
            byte[] buf = new byte[8192];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                os.write(buf, 0, n);
                done += n;
                if (total > 0) {
                    final int p = (int)Math.min(95, done * 95L / total);
                    runOnUiThread(() -> progress.setProgress(p));
                }
            }
        }
        runOnUiThread(() -> {
            progress.setProgress(100);
            state.setText("Hazır!");
        });
    }

    private void readTargetAppInfo(File f) {
        PackageManager pm = getPackageManager();
        int flags = PackageManager.GET_META_DATA;
        if (Build.VERSION.SDK_INT >= 28) flags |= PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo pi = pm.getPackageArchiveInfo(f.getAbsolutePath(), flags);
        if (pi == null || pi.applicationInfo == null) return;
        targetPackage = pi.packageName;
        incomingVersion = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        try {
            PackageInfo installed = pm.getPackageInfo(targetPackage, flags);
            installedVersion = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
            updateMode = true;
            if (incomingVersion <= installedVersion) {
                throw new IllegalStateException("Bu paket kurulu sürümden yeni değil.");
            }
            if (!sameSigner(pi, installed)) {
                throw new IllegalStateException("Güncelleme reddedildi: mevcut uygulamanın imzası ile .skk paketi aynı değil.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            updateMode = false;
            installedVersion = -1;
        }
        ApplicationInfo ai = pi.applicationInfo;
        ai.sourceDir = f.getAbsolutePath();
        ai.publicSourceDir = f.getAbsolutePath();
        try {
            CharSequence label = pm.getApplicationLabel(ai);
            if (label != null && label.length() > 0) targetName = label.toString();
            targetIcon = pm.getApplicationIcon(ai);
        } catch (Exception ignored) {}
        runOnUiThread(() -> {
            appNameView.setText(targetName);
            if (targetIcon != null) appIconView.setImageDrawable(targetIcon);
        });
    }

    private boolean validSkk(File f) throws Exception {
        String displayName = sourceDisplayName();
        if (displayName == null || !displayName.toLowerCase(Locale.ROOT).endsWith(".skk")) return false;
        if (!f.isFile() || f.length() < 1024) return false;
        try (ZipFile z = new ZipFile(f)) {
            ZipEntry manifest = z.getEntry("AndroidManifest.xml");
            boolean dex = false;
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (n.startsWith("classes") && n.endsWith(".dex")) { dex = true; break; }
            }
            if (manifest == null || !dex) return false;
        }
        PackageManager pm = getPackageManager();
        int flags = PackageManager.GET_META_DATA;
        if (Build.VERSION.SDK_INT >= 28) flags |= PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo pi = pm.getPackageArchiveInfo(f.getAbsolutePath(), flags);
        return pi != null && pi.applicationInfo != null && pi.packageName != null && !pi.packageName.trim().isEmpty();
    }

    private String sourceDisplayName() {
        try (android.database.Cursor c = getContentResolver().query(sourceUri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        } catch (Exception ignored) {}
        String s = sourceUri == null ? null : sourceUri.getLastPathSegment();
        return s;
    }

    private boolean sameSigner(PackageInfo incoming, PackageInfo installed) {
        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo a = incoming.signingInfo;
            SigningInfo b = installed.signingInfo;
            if (a == null || b == null) return false;
            if (a.hasMultipleSigners() || b.hasMultipleSigners()) {
                return java.util.Arrays.equals(a.getApkContentsSigners(), b.getApkContentsSigners());
            }
            return a.hasSameSigner(b);
        }
        if (incoming.signatures == null || installed.signatures == null) return false;
        return java.util.Arrays.equals(incoming.signatures, installed.signatures);
    }

    private void showInstallQuestion() {
        state.setText("Kurulmaya hazır");
        progress.setProgress(100);

        LinearLayout root = base();

        TextView sparkle = text(updateMode ? "✦  Güncelleme hazır  ✦" : "✦  Paket hazır  ✦", 15, Color.rgb(190,170,255));
        root.addView(sparkle, new LinearLayout.LayoutParams(-1, dp(30)));

        appIconView = new ImageView(this);
        if (targetIcon != null) appIconView.setImageDrawable(targetIcon);
        else appIconView.setImageResource(android.R.drawable.sym_def_app_icon);
        root.addView(appIconView, new LinearLayout.LayoutParams(dp(64), dp(64)));

        appNameView = text(targetName, 19, Color.WHITE);
        root.addView(appNameView, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView message = text(updateMode ? "Bu uygulamayı güncellemek istiyor musun?" : "Bu uygulamayı yüklemek istiyor musun?", 14, Color.LTGRAY);
        root.addView(message, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button no = new Button(this);
        no.setText("Hayır");
        Button yes = new Button(this);
        yes.setText(updateMode ? "Güncelle" : "Evet");
        buttons.addView(no, new LinearLayout.LayoutParams(0, dp(46), 1));
        buttons.addView(yes, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(buttons, new LinearLayout.LayoutParams(-1, dp(52)));

        no.setOnClickListener(v -> {
            deleteTemp();
            finish();
        });
        yes.setOnClickListener(v -> installApk());

        setContentView(root);
        resizeWindow();
    }

    private void installApk() {
        if (apkFile == null || !apkFile.isFile()) {
            showError("Kurulum dosyası bulunamadı.");
            return;
        }

        LinearLayout root = base();
        TextView sparkle = text(updateMode ? "✦  Güncelleniyor  ✦" : "✦  Kuruluyor  ✦", 15, Color.rgb(190,170,255));
        root.addView(sparkle, new LinearLayout.LayoutParams(-1, dp(30)));

        ImageView icon = new ImageView(this);
        if (targetIcon != null) icon.setImageDrawable(targetIcon);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        root.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView name = text(targetName, 19, Color.WHITE);
        root.addView(name, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView installing = text(updateMode ? "Güncelleme başlatılıyor..." : "Kurulum başlatılıyor...", 14, Color.LTGRAY);
        root.addView(installing, new LinearLayout.LayoutParams(-1, dp(32)));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(10)));

        TextView note = text("♡  Lütfen bekle  ♡", 13, Color.rgb(210,210,220));
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
        resizeWindow();

        final Handler h = new Handler(Looper.getMainLooper());
        final int[] p = {0};
        Runnable animate = new Runnable() {
            @Override public void run() {
                p[0] = Math.min(92, p[0] + 4);
                bar.setProgress(p[0]);
                if (p[0] < 92) h.postDelayed(this, 80);
                else launchAndroidInstaller(bar, installing);
            }
        };
        h.postDelayed(animate, 80);
    }

    private void launchAndroidInstaller(ProgressBar bar, TextView installing) {
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent i = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getPackageName());
            bar.setProgress(100);
            installing.setText(updateMode ? "Android güncelleme ekranı açılıyor..." : "Android kurulum ekranı açılıyor...");
            startActivity(i);
        } catch (Exception e) {
            showError("Android kurulum ekranı açılamadı: " + e.getMessage());
        }
    }

    private void deleteTemp() {
        try { if (apkFile != null) apkFile.delete(); } catch (Exception ignored) {}
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
            .setTitle("SKK yüklenemedi")
            .setMessage(message)
            .setPositiveButton("Kapat", (d,w) -> finish())
            .setOnCancelListener(d -> finish())
            .show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) deleteTemp();
    }
}