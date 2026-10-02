package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class MusabXmlViewerActivity extends Activity {
    private File file;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        String p = getIntent().getStringExtra("path");
        file = p == null ? null : new File(p);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,12));

        TextView title = new TextView(this);
        title.setText("Musab XML Görüntüleyici  •  " + (file == null ? "" : file.getName()));
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(18,18,18,12);
        root.addView(title);

        EditText editor = new EditText(this);
        editor.setTextColor(Color.WHITE);
        editor.setTextSize(14);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setSingleLine(false);
        editor.setHorizontallyScrolling(true);
        editor.setBackgroundColor(Color.rgb(24,24,28));
        editor.setText(readText());
        root.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = new LinearLayout(this);
        Button edit = new Button(this);
        edit.setText("Musab Text ile düzenle");
        edit.setOnClickListener(v -> {
            IntentHelper.openText(this, file);
        });
        actions.addView(edit, new LinearLayout.LayoutParams(0,72,1));

        Button close = new Button(this);
        close.setText("Kapat");
        close.setOnClickListener(v -> finish());
        actions.addView(close, new LinearLayout.LayoutParams(0,72,1));
        root.addView(actions);

        setContentView(root);
    }

    private String readText() {
        if (file == null) return "XML dosya yolu bulunamadı.";
        try {
            StringBuilder s = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) s.append(line).append('\n');
            r.close();
            return s.toString();
        } catch (Exception e) {
            return "XML okunamadı: " + e.getMessage();
        }
    }

    static class IntentHelper {
        static void openText(Activity a, File f) {
            if (f == null) return;
            android.content.Intent i = new android.content.Intent(a, MusabTextActivity.class);
            i.putExtra("path", f.getAbsolutePath());
            a.startActivity(i);
        }
    }
}
