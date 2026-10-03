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
    private PackageInstaller.Session installSession;
    private long incomingVersion = -1;
    private long installedVersion = -1;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        sourceUri = getIntent() == null ? null : getIntent().getData();
        if (sourceUri == null && getIntent() != null) {
            String raw = getIntent().getStringExtra("skk_source_uri");
            if (raw != null && !raw.trim().isEmpty()) {
                try { sourceUri = Uri.parse(raw); } catch (Exception ignored) {}
            }
            if (sourceUri == null && getIntent().getClipData() != null && getIntent().getClipData().getItemCount() > 0) {
                sourceUri = getIntent().getClipData().getItemAt(0).getUri();
            }
        }
        configureSmallWindow();
        showLoading();
        if (getIntent() != null && getIntent().hasExtra("installResult")) {
            boolean ok = "success".equals(getIntent().getStringExtra("installResult"));
            if (ok) showError("SKK başarıyla kuruldu.");
            else showError("SKK kurulumu başarısız: " + getIntent().getStringExtra("installMessage"));
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::preparePackage, 180);
        }
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
            Drawable loadedIcon = ai.loadIcon(pm);
            if (loadedIcon == null) loadedIcon = pm.getApplicationIcon(ai);
            targetIcon = loadedIcon;
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
            return java.util.Arrays.equals(a.getSigningCertificateHistory(), b.getSigningCertificateHistory());
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
        TextView sparkle = text("✦  SKK Kuruluyor  ✦", 15, Color.rgb(190,170,255));
        root.addView(sparkle, new LinearLayout.LayoutParams(-1, dp(30)));

        ImageView icon = new ImageView(this);
        if (targetIcon != null) icon.setImageDrawable(targetIcon);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        root.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView name = text(targetName, 19, Color.WHITE);
        root.addView(name, new LinearLayout.LayoutParams(-1, dp(36)));

        TextView installing = text("Özel SKK paket yöneticisine ekleniyor...", 14, Color.LTGRAY);
        root.addView(installing, new LinearLayout.LayoutParams(-1, dp(40)));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(0);
        root.addView(bar, new LinearLayout.LayoutParams(-1, dp(10)));

        TextView note = text("Android PaketInstaller / ACTION_INSTALL_PACKAGE kullanılmıyor.", 13, Color.rgb(210,210,220));
        note.setPadding(0, dp(12), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(34)));

        setContentView(root);
        resizeWindow();

        new Thread(() -> {
            try {
                File installedRoot = new File(getExternalFilesDir(null), "SKK/installed");
                if (!installedRoot.exists() && !installedRoot.mkdirs()) {
                    throw new IOException("SKK kurulum klasörü oluşturulamadı.");
                }
                String safe = (targetPackage == null || targetPackage.trim().isEmpty())
                        ? ("package-" + System.currentTimeMillis())
                        : targetPackage.replaceAll("[^A-Za-z0-9._-]", "_");
                File target = new File(installedRoot, safe + ".skk");
                File tmp = new File(installedRoot, safe + ".skk.part");

                copyFileWithProgress(apkFile, tmp, bar);
                if (target.exists() && !target.delete()) {
                    throw new IOException("Eski SKK paketi silinemedi.");
                }
                if (!tmp.renameTo(target)) {
                    throw new IOException("SKK paketi kurulum alanına taşınamadı.");
                }

                File meta = new File(installedRoot, safe + ".info");
                try (FileWriter w = new FileWriter(meta, false)) {
                    w.write("name=" + targetName + "\n");
                    w.write("package=" + (targetPackage == null ? "" : targetPackage) + "\n");
                    w.write("version=" + incomingVersion + "\n");
                    w.write("installedAt=" + System.currentTimeMillis() + "\n");
                }

                runOnUiThread(() -> {
                    bar.setProgress(100);
                    installing.setText("SKK özel yükleyiciye kuruldu.");
                    note.setText("Kurulum tamamlandı. Android sistem yükleyicisi çağrılmadı.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError("Özel SKK kurulumu başarısız: " + e.getMessage()));
            }
        }).start();
    }

    private void copyFileWithProgress(File src, File dst, ProgressBar bar) throws Exception {
        long total = Math.max(1, src.length());
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst, false))) {
            byte[] buf = new byte[8192];
            long done = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                final int p = (int)Math.min(100, done * 100L / total);
                runOnUiThread(() -> bar.setProgress(p));
            }
        }
    }

    private void deleteTemp() {
        try { if (apkFile != null) apkFile.delete(); } catch (Exception ignored) {}
    }

    private void showError(String message) {
        // SKK kurulum sonucu da Musab'ın kendi arayüzünde gösterilir.
        // Android'in ACTION_INSTALL_PACKAGE / sistem yükleyici ekranı açılmaz.
        LinearLayout root = base();

        TextView header = text("✦  Musab SKK Yükleyici  ✦", 15, Color.rgb(190,170,255));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(34)));

        ImageView icon = new ImageView(this);
        if (targetIcon != null) icon.setImageDrawable(targetIcon);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        root.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView title = text(targetName, 19, Color.WHITE);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        TextView msg = text(message, 14, Color.LTGRAY);
        root.addView(msg, new LinearLayout.LayoutParams(-1, dp(72)));

        Button close = new Button(this);
        close.setText("Kapat");
        close.setOnClickListener(v -> {
            deleteTemp();
            finish();
        });
        root.addView(close, new LinearLayout.LayoutParams(-1, dp(48)));

        setContentView(root);
        resizeWindow();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) deleteTemp();
    }
}