package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MusabTextActivity extends Activity {
    EditText editor;
    File file;
    boolean archiveTemp;
    String archivePath;
    String archiveEntry;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        String p=getIntent().getStringExtra("path");
        file=p==null?null:new File(p);
        archiveTemp=getIntent().getBooleanExtra("archiveTemp",false);
        archivePath=getIntent().getStringExtra("archivePath");
        archiveEntry=getIntent().getStringExtra("archiveEntry");

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12,12,14));

        TextView title=new TextView(this);
        title.setText("Musab Kod / Metin Editörü  •  "+(file==null?"":file.getName()));
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setPadding(20,20,20,14);
        root.addView(title);

        editor=new EditText(this);
        editor.setTextColor(Color.WHITE);
        editor.setTextSize(14);
        editor.setGravity(Gravity.TOP|Gravity.START);
        editor.setSingleLine(false);
        editor.setHorizontallyScrolling(false);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setBackgroundColor(Color.rgb(24,24,28));
        try {
            if (file==null) throw new IOException("Dosya yolu yok");
            editor.setText(read());
        } catch(Exception e) {
            editor.setText("Okuma hatası: "+e.getMessage());
        }
        root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button save=new Button(this);
        save.setText("Kaydet");
        save.setOnClickListener(v->saveNow());
        actions.setPadding(8,4,8,8);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(save,new LinearLayout.LayoutParams(0,64,1));

        Button exit=new Button(this);
        exit.setText("Çık");
        exit.setOnClickListener(v->confirmExit());
        actions.addView(exit,new LinearLayout.LayoutParams(0,64,1));

        root.addView(actions);
        setContentView(root);
    }

    String read() throws Exception {
        BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
        StringBuilder s=new StringBuilder();
        String l;
        while((l=r.readLine())!=null) s.append(l).append('\n');
        r.close();
        return s.toString();
    }

    void saveNow() {
        try {
            write(editor.getText().toString());
            if (archiveTemp) {
                replaceArchiveEntry(new File(archivePath),archiveEntry,file);
            }
            Toast.makeText(this,"Kaydedildi",Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
        } catch(Exception e) {
            Toast.makeText(this,"Kaydetme hatası: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    void write(String s) throws Exception {
        FileOutputStream o=new FileOutputStream(file,false);
        o.write(s.getBytes("UTF-8"));
        o.close();
    }

    void confirmExit() {
        if (archiveTemp) {
            new AlertDialog.Builder(this)
                .setTitle("Değişiklikleri kaydet?")
                .setMessage("Kaydetmeden çıkarsan yaptığın değişiklikler silinecek.")
                .setNegativeButton("Kaydetme", (d,w)->finish())
                .setPositiveButton("Kaydet", (d,w)->{saveNow(); finish();})
                .setNeutralButton("İptal", null)
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("Çık")
                .setMessage("Editörden çıkmak istiyor musun?")
                .setNegativeButton("Çık", (d,w)->finish())
                .setPositiveButton("İptal", null)
                .show();
        }
    }

    @Override public void onBackPressed() {
        confirmExit();
    }

    static void replaceArchiveEntry(File archive, String entryName, File replacement) throws Exception {
        File temp=new File(archive.getParentFile(),archive.getName()+".musabtmp");
        boolean replaced=false;
        try(ZipFile in=new ZipFile(archive); ZipOutputStream out=new ZipOutputStream(new FileOutputStream(temp))) {
            Enumeration<? extends ZipEntry> en=in.entries();
            byte[] buf=new byte[8192];
            while(en.hasMoreElements()) {
                ZipEntry e=en.nextElement();
                if(e.getName().replace('\\','/').replaceFirst("^/+","").equals(entryName)) {
                    ZipEntry n=new ZipEntry(e.getName());
                    n.setTime(e.getTime());
                    out.putNextEntry(n);
                    try(InputStream r=new FileInputStream(replacement)) {
                        int count;
                        while((count=r.read(buf))!=-1) out.write(buf,0,count);
                    }
                    out.closeEntry();
                    replaced=true;
                } else {
                    out.putNextEntry(new ZipEntry(e.getName()));
                    try(InputStream r=in.getInputStream(e)) {
                        int count;
                        while((count=r.read(buf))!=-1) out.write(buf,0,count);
                    }
                    out.closeEntry();
                }
            }
            if(!replaced) throw new IOException("Arşiv içindeki dosya bulunamadı");
        }
        if(!archive.delete()) throw new IOException("Eski arşiv silinemedi");
        if(!temp.renameTo(archive)) throw new IOException("Yeni arşiv yerleştirilemedi");
    }
}