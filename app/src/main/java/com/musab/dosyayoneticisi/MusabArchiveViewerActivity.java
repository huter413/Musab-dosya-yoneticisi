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
    String currentPrefix = "";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        path = getIntent().getStringExtra("path");
        build();
        loadRoot();
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
        file.setPadding(18,0,18,8);
        file.setMaxLines(2);
        file.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        root.addView(file);

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);

        Button up = new Button(this);
        up.setText("↑ Üst klasör");
        up.setTextSize(15);
        up.setOnClickListener(v -> goUp());
        navigation.addView(up, new LinearLayout.LayoutParams(0, -2, 1));

        Button rootButton = new Button(this);
        rootButton.setText("Arşiv kökü");
        rootButton.setTextSize(15);
        rootButton.setOnClickListener(v -> {
            currentPrefix = "";
            loadCurrent();
        });
        navigation.addView(rootButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(navigation);

        TextView location = new TextView(this);
        location.setTextColor(Color.LTGRAY);
        location.setTextSize(14);
        location.setPadding(18,4,18,8);
        location.setId(android.R.id.text1);
        root.addView(location);

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

    void loadRoot() {
        if (path == null) {
            addMessage("Arşiv yolu bulunamadı.");
            return;
        }
        new Thread(() -> {
            try {
                zip = new ZipFile(path);
                runOnUiThread(this::loadCurrent);
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Arşiv açılamadı: " + e.getMessage()));
            }
        }).start();
    }

    void loadCurrent() {
        if (zip == null) return;
        final String prefix = normalizePrefix(currentPrefix);
        new Thread(() -> {
            try {
                LinkedHashMap<String, Boolean> children = new LinkedHashMap<>();
                Enumeration<? extends ZipEntry> en = zip.entries();
                while (en.hasMoreElements()) {
                    ZipEntry entry = en.nextElement();
                    String name = normalizeEntry(entry.getName());
                    if (name.length() == 0) continue;
                    if (!prefix.isEmpty()) {
                        if (!name.startsWith(prefix)) continue;
                    }
                    String rest = name.substring(prefix.length());
                    if (rest.length() == 0) continue;

                    int slash = rest.indexOf('/');
                    if (slash >= 0) {
                        String folder = rest.substring(0, slash);
                        if (folder.length() > 0) children.put(folder, true);
                    } else {
                        children.put(rest, entry.isDirectory());
                    }
                }

                ArrayList<String> folders = new ArrayList<>();
                ArrayList<String> files = new ArrayList<>();
                for (Map.Entry<String, Boolean> child : children.entrySet()) {
                    if (child.getValue()) folders.add(child.getKey());
                    else files.add(child.getKey());
                }
                Collections.sort(folders, String.CASE_INSENSITIVE_ORDER);
                Collections.sort(files, String.CASE_INSENSITIVE_ORDER);

                runOnUiThread(() -> {
                    list.removeAllViews();
                    TextView location = findViewById(android.R.id.text1);
                    location.setText(prefix.isEmpty() ? "/" : "/" + prefix);

                    if (folders.isEmpty() && files.isEmpty()) {
                        addMessage("Bu klasör boş.");
                        return;
                    }

                    for (String folder : folders) addFolderRow(folder, prefix);
                    for (String file : files) addFileRow(file, prefix);
                });
            } catch (Exception e) {
                runOnUiThread(() -> addMessage("Arşiv okunamadı: " + e.getMessage()));
            }
        }).start();
    }

    void addFolderRow(String name, String prefix) {
        TextView row = new TextView(this);
        row.setText("📁 " + name);
        row.setTextColor(Color.WHITE);
        row.setTextSize(17);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(18,16,18,16);
        row.setBackgroundColor(Color.rgb(24,24,28));
        row.setOnClickListener(v -> {
            currentPrefix = normalizePrefix(prefix + name);
            loadCurrent();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0,1,0,1);
        list.addView(row, lp);
    }

    void addFileRow(String name, String prefix) {
        TextView row = new TextView(this);
        row.setText("📄 " + name);
        row.setTextColor(Color.WHITE);
        row.setTextSize(16);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(18,16,18,16);
        row.setBackgroundColor(Color.rgb(20,20,24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0,1,0,1);
        list.addView(row, lp);
    }

    void goUp() {
        String prefix = normalizePrefix(currentPrefix);
        if (prefix.isEmpty()) {
            finish();
            return;
        }
        int end = prefix.length() - 1;
        int slash = prefix.lastIndexOf('/', end - 1);
        currentPrefix = slash < 0 ? "" : prefix.substring(0, slash + 1);
        loadCurrent();
    }

    String normalizeEntry(String s) {
        if (s == null) return "";
        s = s.replace('\\', '/');
        while (s.startsWith("./")) s = s.substring(2);
        while (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    String normalizePrefix(String s) {
        s = normalizeEntry(s);
        if (s.length() > 0 && !s.endsWith("/")) s += "/";
        return s;
    }

    void addMessage(String s) {
        list.removeAllViews();
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setPadding(18,24,18,24);
        list.addView(t);
    }

    @Override public void onBackPressed() {
        if (!normalizePrefix(currentPrefix).isEmpty()) {
            goUp();
        } else {
            super.onBackPressed();
        }
    }

    @Override protected void onDestroy() {
        if (zip != null) {
            try { zip.close(); } catch (Exception ignored) {}
            zip = null;
        }
        super.onDestroy();
    }
}
