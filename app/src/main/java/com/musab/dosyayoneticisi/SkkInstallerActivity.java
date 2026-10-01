package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
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

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        sourceUri = getIntent() == null ? null : getIntent().getData();
        showLoading();
        new Handler(Looper.getMainLooper()).postDelayed(() -> preparePackage(), 900);
    }

    private void showLoading() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(22, 28, 22, 28);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout appHead = new LinearLayout(this);
        appHead.setOrientation(LinearLayout.VERTICAL);
        appHead.setGravity(Gravity.CENTER);
        try {
            android.content.pm.ApplicationInfo ai=getApplicationInfo();
            ImageView appIcon=new ImageView(this); appIcon.setImageDrawable(ai.loadIcon(getPackageManager()));
            appHead.addView(appIcon,new LinearLayout.LayoutParams(72,72));
            TextView appName=new TextView(this); appName.setText(String.valueOf(ai.loadLabel(getPackageManager()))); appName.setTextColor(Color.WHITE); appName.setTextSize(17); appName.setGravity(Gravity.CENTER);
            appHead.addView(appName,new LinearLayout.LayoutParams(-1,42));
        } catch(Exception ignored) {}
        root.addView(appHead,new LinearLayout.LayoutParams(-1,125));

        TextView character = new TextView(this);
        character.setText("  /\\_/\\\\\n (  o o  )\\n  >  <3  <");
        character.setTextColor(Color.WHITE);
        character.setTextSize(25);
        character.setGravity(Gravity.CENTER);
        character.setPadding(0, 18, 0, 12);
        root.addView(character, new LinearLayout.LayoutParams(-1, 105));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(20, 22, 20, 22);
        panel.setBackgroundColor(Color.rgb(18,18,22));

        TextView title = new TextView(this);
        title.setText("SKK Yükleyicisi");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        panel.addView(title);

        state = new TextView(this);
        state.setText("SKK dosyası yükleniyor...");
        state.setTextColor(Color.LTGRAY);
        state.setTextSize(15);
        state.setGravity(Gravity.CENTER);
        state.setPadding(0, 10, 0, 16);
        panel.addView(state);

        progress = new ProgressBar(this);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.rgb(33,150,243)));
        panel.addView(progress, new LinearLayout.LayoutParams(180, 10));

        root.addView(panel, new LinearLayout.LayoutParams(250, 220));
        setContentView(root);
    }

    private void preparePackage() {
        if (sourceUri == null) {
            showError("Bir .skk dosyası seçilmedi.");
            return;
        }
        new Thread(() -> {
            try {
                File tmp = new File(getCacheDir(), "pending-" + System.currentTimeMillis() + ".apk");
                copyUri(sourceUri, tmp);
                if (!validSkk(tmp)) throw new IOException("SKK dosyası geçerli bir Android APK yapısı değil.");
                apkFile = tmp;
                runOnUiThread(this::confirmInstall);
            } catch (Exception e) {
                runOnUiThread(() -> showError("SKK açılamadı: " + e.getMessage()));
            }
        }).start();
    }

    private boolean validSkk(File f) throws Exception {
        if (!f.isFile() || f.length() < 1024) return false;
        ZipFile z = new ZipFile(f);
        try {
            ZipEntry manifest = z.getEntry("AndroidManifest.xml");
            boolean dex = false;
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (n.startsWith("classes") && n.endsWith(".dex")) { dex = true; break; }
            }
            return manifest != null && dex;
        } finally {
            z.close();
        }
    }

    private void copyUri(Uri uri, File out) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("SKK içeriği okunamadı.");
        OutputStream os = new FileOutputStream(out);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
        } finally {
            in.close();
            os.close();
        }
    }

    private void confirmInstall() {
        new AlertDialog.Builder(this)
            .setTitle("SKK yükleme")
            .setMessage("Bu SKK paketi Android uygulaması olarak yüklenmek isteniyor. Devam edilsin mi?")
            .setPositiveButton("Evet", (d,w) -> installApk())
            .setNegativeButton("Hayır", (d,w) -> finish())
            .setOnCancelListener(d -> finish())
            .show();
    }

    private void installApk() {
        if (apkFile == null || !apkFile.isFile()) {
            showError("Kurulum dosyası bulunamadı.");
            return;
        }
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent i = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, getPackageName());
            startActivity(i);
        } catch (Exception e) {
            showError("Android kurulum ekranı açılamadı: " + e.getMessage());
        }
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
            .setTitle("SKK yüklenemedi")
            .setMessage(message)
            .setPositiveButton("Kapat", (d,w) -> finish())
            .setOnCancelListener(d -> finish())
            .show();
    }
}