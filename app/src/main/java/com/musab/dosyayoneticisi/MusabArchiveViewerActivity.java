package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MusabArchiveViewerActivity extends Activity {
    LinearLayout list;
    ZipFile zip;
    String path;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        path = getIntent().getStringExtra("path");
        build();
        load();
    }

    void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,12));

        TextView title = new TextView(this);
        title.setText("Musab Arşiv Görüntüleyici");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setPadding(18,18,18,12);
        root.addView(title);

        TextView file = new TextView(this);
        file.setText(path == null ? "" : new File(path).getName());
        file.setTextColor(Color.LTGRAY);
        file.setTextSize(15);
        file.setPadding(18,0,18,12);
        file.setMaxLines(2);
        file.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        root.addView(file);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));

        Button close = new Button(this);
        close.setText("Kapat");
        close.setOnClickListener(v -> finish());
        root.addView(close);

        setContentView(root);
    }

    void load() {
        if (path == null) {
            addMessage("Arşiv yolu bulunamadı.");
            return;
        }
        new Thread(() -> {
            try {
                zip = new ZipFile(path);
                ArrayList<ZipEntry> entries = new ArrayList<>();
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) entries.add(en.nextElement());
                entries.sort((a,b) -> {
                    if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                runOnUiThread(() -> {
                    if (entries.isEmpty()) {
                        addMessage("Arşiv boş.");
                        return;
                    }
                    for (ZipEntry e : entries) {
                        TextView row = new TextView(this);
                        String size = e.isDirectory() ? "klasör" : human(e.getSize());
                        row.setText((e.isDirectory() ? "📁 " : "📄 ") + e.getName() + "\n" + size);
                        row.setTextColor(Color.WHITE);
                        row.setTextSize(15);
                        row.setPadding(18,14,18,14);
                        row.setBackgroundColor(Color.rgb(24,24,28));
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                        lp.setMargins(0,1,0,1);
                        list.addView(row, lp);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Arşiv açılamadı: " + e.getMessage()));
            }
        }).start();
    }

    void addMessage(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setPadding(18,24,18,24);
        list.addView(t);
    }

    String human(long n) {
        if (n < 0) return "boyut bilinmiyor";
        if (n < 1024) return n + " B";
        if (n < 1024*1024) return String.format(Locale.US,"%.1f KB",n/1024d);
        if (n < 1024L*1024*1024) return String.format(Locale.US,"%.1f MB",n/1024d/1024d);
        return String.format(Locale.US,"%.1f GB",n/1024d/1024d/1024d);
    }

    @Override protected void onDestroy() {
        if (zip != null) {
            try { zip.close(); } catch (Exception ignored) {}
            zip = null;
        }
        super.onDestroy();
    }
}
