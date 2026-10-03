package com.musab.dosyayoneticisi;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;

/**
 * Launcher activity variant that keeps the fast UI but hides the split-APK count
 * from the application details dialog.
 */
public class MusabFastMainActivity extends FastMainActivity {
    @Override
    void appDetails(ApplicationInfo ai, PackageManager pm) {
        String label = String.valueOf(ai.loadLabel(pm));
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(ai.packageName, 0);
        } catch (Exception e) {
            toast("Uygulama bilgisi okunamadı");
            return;
        }
        long vc = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        File apk = new File(ai.sourceDir);
        String msg = label + "\n" + ai.packageName
                + "\nSürüm: " + pi.versionName + " (" + vc + ")"
                + "\nTemel APK: " + ai.sourceDir
                + "\nBoyut: " + human(apk.length());
        new AlertDialog.Builder(this)
                .setTitle("Uygulama")
                .setMessage(msg)
                .setPositiveButton("APK Çıkar", (d, w) -> extractApk(ai))
                .setNeutralButton("APK içeriği", (d, w) -> apkContents(apk))
                .setNegativeButton("Kapat", null)
                .show();
    }
}
