package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list; TextView path; File current;
    int pad=18;
    @Override public void onCreate(Bundle b){super.onCreate(b); build(); current=Environment.getExternalStorageDirectory(); refresh();}
    TextView tv(String s,int sp){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.DKGRAY); t.setPadding(pad,pad,pad,pad); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); return b; }
    void build(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar=new LinearLayout(this);
        Button back=btn("‹"); back.setOnClickListener(v->{if(current!=null&&current.getParentFile()!=null){current=current.getParentFile();refresh();}});
        path=tv("",14); path.setSingleLine(true); bar.addView(back,new LinearLayout.LayoutParams(60,-2)); bar.addView(path,new LinearLayout.LayoutParams(0,-2,1));
        EditText go=new EditText(this); go.setHint("/storage/emulated/0"); Button goBtn=btn("Git"); goBtn.setOnClickListener(v->{File f=new File(go.getText().toString());if(f.isDirectory()){current=f;refresh();}else Toast.makeText(this,"Dizin bulunamadı",0).show();});
        root.addView(bar); LinearLayout g=new LinearLayout(this); g.addView(go,new LinearLayout.LayoutParams(0,-2,1));g.addView(goBtn);root.addView(g);
        LinearLayout tools=new LinearLayout(this); tools.setOrientation(LinearLayout.HORIZONTAL);
        Button up=btn("Yeni klasör");up.setOnClickListener(v->newFolder()); Button menu=btn("Araçlar");menu.setOnClickListener(v->toolsDialog());tools.addView(up,new LinearLayout.LayoutParams(0,-2,1));tools.addView(menu,new LinearLayout.LayoutParams(0,-2,1));root.addView(tools);
        ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    void refresh(){path.setText(current.getAbsolutePath());list.removeAllViews();File[] fs=current.listFiles();if(fs==null){list.addView(tv("Erişim yok",16));return;}Arrays.sort(fs,(a,b)->a.getName().compareToIgnoreCase(b.getName()));for(File f:fs){TextView x=tv((f.isDirectory()?"📁 ":"📄 ")+f.getName(),17);if(f.getName().equals("Şirin")||f.getName().equals("Sirin"))x.append(" ✨");x.setOnClickListener(v->{if(f.isDirectory()){current=f;refresh();}else open(f);});x.setOnLongClickListener(v->{fileMenu(f);return true;});list.addView(x);}}
    void open(File f){Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.fromFile(f),"*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Açacak uygulama bulunamadı",0).show();}}
    void newFolder(){EditText e=new EditText(this);e.setHint("Klasör adı");new AlertDialog.Builder(this).setTitle("Yeni klasör").setView(e).setPositiveButton("Oluştur",(d,w)->{File f=new File(current,e.getText().toString());if(f.mkdir())refresh();}).setNegativeButton("İptal",null).show();}
    void fileMenu(File f){new AlertDialog.Builder(this).setTitle(f.getName()).setItems(new String[]{"Yeniden adlandır","Sil","Özellikler","Kopyala"},(d,w)->{if(w==0)rename(f);else if(w==1){f.delete();refresh();}else if(w==2)Toast.makeText(this,f.getAbsolutePath()+"\n"+f.length()+" bayt",1).show();});}
    void rename(File f){EditText e=new EditText(this);e.setText(f.getName());new AlertDialog.Builder(this).setTitle("Yeniden adlandır").setView(e).setPositiveButton("Kaydet",(d,w)->{f.renameTo(new File(f.getParent(),e.getText().toString()));refresh();}).setNegativeButton("İptal",null).show();}
    void toolsDialog(){String[] a={"APK / Uygulamalar","Python / IPython / Pillow","Terminal","Android Manifest / XML","Ayarlar"};new AlertDialog.Builder(this).setTitle("Musab Araçları").setItems(a,(d,w)->{if(w==0)apps();else if(w==1)code();else if(w==2)terminal();else if(w==3)xml();});}
    void apps(){PackageManager pm=getPackageManager();LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);ScrollView s=new ScrollView(this);s.addView(l);for(ApplicationInfo ai:pm.getInstalledApplications(PackageManager.GET_META_DATA)){TextView t=tv(ai.loadLabel(pm)+"\n"+ai.packageName,16);t.setOnClickListener(v->Toast.makeText(this,"APK: "+ai.sourceDir,1).show());l.addView(t);}new AlertDialog.Builder(this).setTitle("Uygulamalar").setView(s).setPositiveButton("Kapat",null).show();}
    void code(){EditText e=new EditText(this);e.setGravity(Gravity.TOP);e.setMinLines(14);e.setHint("Python / IPython / Pillow kodu");new AlertDialog.Builder(this).setTitle("Kod").setView(e).setPositiveButton("Kaydet",(d,w)->Toast.makeText(this,"Kod metin olarak hazırlandı",0).show()).setNegativeButton("Kapat",null).show();}
    void terminal(){Toast.makeText(this,"Termux entegrasyonu için Termux'un desteklenen servis/API bağlantısı gerekir.",1).show();}
    void xml(){EditText e=new EditText(this);e.setGravity(Gravity.TOP);e.setMinLines(18);e.setText("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");new AlertDialog.Builder(this).setTitle("AndroidManifest.xml / XML").setView(e).setPositiveButton("Kaydet",null).setNegativeButton("Kapat",null).show();}
}
