package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    LinearLayout root, list, actionBar;
    EditText pathEdit, searchEdit;
    File current, clipboard;
    boolean cutMode = false;
    final File STORAGE = Environment.getExternalStorageDirectory();
    final File MUSAB = new File(STORAGE, "MusabFolder");
    final File APKS = new File(MUSAB, "apks");
    int pad = 22; boolean gridView = false; boolean sortBySize = false; final int BG=Color.rgb(10,10,12), PANEL=Color.rgb(24,24,28), FG=Color.WHITE, MUTED=Color.rgb(170,170,180);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ensureMusabFolders();
        current = STORAGE;
        build();
        requestStorageAccess();
        refresh();
    }

    void ensureMusabFolders() {
        MUSAB.mkdirs();
        APKS.mkdirs();
    }

    void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
        } else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"}, 10);
        }
    }

    TextView tv(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setPadding(pad,pad,pad,pad);
        return t;
    }
    Button btn(String s) { Button b = new Button(this); b.setText(s); return b; }

    void build() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        TextView title = tv("☰  MusabDosya Yöneticisi", 25); title.setTextColor(FG);
        root.addView(title);

        LinearLayout nav = new LinearLayout(this);
        Button back = btn("‹");
        back.setOnClickListener(v -> goParent());
        pathEdit = new EditText(this); pathEdit.setSingleLine(true); pathEdit.setTextSize(18); pathEdit.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO); pathEdit.setOnEditorActionListener((v,id,e)->{navigate(pathEdit.getText().toString()); return true;});
        Button go = btn("Git");
        go.setOnClickListener(v -> navigate(pathEdit.getText().toString()));
        nav.addView(back,new LinearLayout.LayoutParams(56,-2));
        nav.addView(pathEdit,new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(go,new LinearLayout.LayoutParams(64,-2));
        root.addView(nav);

        LinearLayout searchRow = new LinearLayout(this);
        searchEdit = new EditText(this); searchEdit.setSingleLine(true); searchEdit.setHint("Ara...");
        Button search = btn("Ara");
        search.setOnClickListener(v -> refresh());
        searchRow.addView(searchEdit,new LinearLayout.LayoutParams(0,-2,1));
        searchRow.addView(search);
        root.addView(searchRow);

        actionBar = new LinearLayout(this);
        String[] actions = {"Yeni","Yapıştır","Araçlar","Görünüm"};
        for (String s: actions) {
            Button b=btn(s);
            if(s.equals("Yeni")) b.setOnClickListener(v->newDialog());
            if(s.equals("Yapıştır")) b.setOnClickListener(v->paste());
            if(s.equals("Araçlar")) b.setOnClickListener(v->toolsDialog());
            if(s.equals("Görünüm")) b.setOnClickListener(v->viewDialog());
            actionBar.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(actionBar);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    void navigate(String p) {
        if (p == null || p.trim().isEmpty()) return;
        File f = new File(p.trim());
        if (f.isDirectory()) { current=f.getCanonicalFile(); refresh(); }
        else Toast.makeText(this,"Dizin bulunamadı",Toast.LENGTH_SHORT).show();
    }
    void goParent() {
        if(current!=null && current.getParentFile()!=null) { current=current.getParentFile(); refresh(); }
    }

    void refresh() {
        pathEdit.setText(current.getAbsolutePath()); pathEdit.setSelection(pathEdit.length());
        list.removeAllViews();
        String q = searchEdit==null ? "" : searchEdit.getText().toString().trim().toLowerCase(Locale.ROOT);
        File[] fs = current.listFiles();
        if(fs==null){ list.addView(tv("Erişim yok veya klasör boş.",16)); return; }
        Arrays.sort(fs,(a,b)->{ if(sortBySize && !a.isDirectory() && !b.isDirectory()) return Long.compare(b.length(),a.length());
            if(a.isDirectory()!=b.isDirectory()) return a.isDirectory()?-1:1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        int shown=0;
        for(File f:fs){
            if(!q.isEmpty() && !f.getName().toLowerCase(Locale.ROOT).contains(q)) continue;
            addItem(f); shown++;
        }
        if(shown==0) list.addView(tv("Sonuç yok.",16));
    }

    void addGridItem(File f) {
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setGravity(Gravity.CENTER); row.setPadding(pad,pad,pad,pad); row.setBackgroundColor(PANEL);
        TextView ic=tv(f.isDirectory()?(specialName(f)?"☺️":"📁"):"📄",38); row.addView(ic,new LinearLayout.LayoutParams(-1,58));
        TextView nm=tv(f.getName(),16); nm.setGravity(Gravity.CENTER); nm.setTextColor(FG); row.addView(nm,new LinearLayout.LayoutParams(-1,58));
        row.setOnClickListener(v->{if(f.isDirectory()){current=f;refresh();}else openFile(f);}); row.setOnLongClickListener(v->{fileMenu(f);return true;});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,130); lp.setMargins(6,6,6,6); list.addView(row,lp);
    }

    void addItem(File f) { if(gridView){ addGridItem(f); return; }
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=tv(f.isDirectory()?"📁":"📄",34); icon.setTextColor(FG);
        TextView name=tv(specialName(f) ? "☺️ "+f.getName() : f.getName(),19); name.setTextColor(FG);
        row.addView(icon,new LinearLayout.LayoutParams(58,78));
        row.addView(name,new LinearLayout.LayoutParams(0,78,1));
        TextView size=tv(f.isDirectory()?"":human(f.length()),12);
        row.addView(size,new LinearLayout.LayoutParams(90,78));
        row.setOnClickListener(v->{ if(f.isDirectory()){current=f;refresh();} else openFile(f); });
        row.setOnLongClickListener(v->{fileMenu(f);return true;});
        list.addView(row);
    }

    boolean specialName(File f) {
        return f.getName().equals("Şirin") || f.getName().equals("Sirin");
    }

    String human(long n){
        if(n<1024) return n+" B";
        if(n<1024*1024) return String.format(Locale.US,"%.1f KB",n/1024d);
        if(n<1024L*1024*1024) return String.format(Locale.US,"%.1f MB",n/1024d/1024d);
        return String.format(Locale.US,"%.1f GB",n/1024d/1024d/1024d);
    }

    void openFile(File f) {
        if(f.getName().equals("Salak.png")) { makeSalakPng(f); return; }
        String mime=getMime(f.getName());
        if(isText(f.getName())) { editTextFile(f); return; }
        try {
            Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
            Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(u,mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch(Exception e){ Toast.makeText(this,"Açacak uygulama bulunamadı",Toast.LENGTH_SHORT).show(); }
    }

    String getMime(String n){
        String x=n.toLowerCase(Locale.ROOT);
        if(x.endsWith(".png")||x.endsWith(".jpg")||x.endsWith(".jpeg")||x.endsWith(".gif")||x.endsWith(".webp")) return "image/*";
        if(x.endsWith(".mp4")||x.endsWith(".mkv")||x.endsWith(".webm")) return "video/*";
        if(x.endsWith(".mp3")||x.endsWith(".wav")||x.endsWith(".ogg")) return "audio/*";
        if(x.endsWith(".apk")) return "application/vnd.android.package-archive";
        return "*/*";
    }
    boolean isText(String n){
        String x=n.toLowerCase(Locale.ROOT);
        return x.endsWith(".txt")||x.endsWith(".xml")||x.endsWith(".json")||x.endsWith(".java")||x.endsWith(".kt")||x.endsWith(".gradle")||x.endsWith(".smali")||x.endsWith(".properties")||x.endsWith(".md")||x.endsWith(".js")||x.endsWith(".html")||x.endsWith(".css")||x.endsWith(".py");
    }

    void editTextFile(File f){
        EditText e=new EditText(this); e.setGravity(Gravity.TOP); e.setMinLines(18); e.setTextSize(14);
        try { e.setText(read(f)); } catch(Exception ex){e.setText("Okuma hatası: "+ex.getMessage());}
        new AlertDialog.Builder(this).setTitle("Düzenle: "+f.getName()).setView(e)
            .setPositiveButton("Kaydet",(d,w)->{try{write(f,e.getText().toString());refresh();}catch(Exception ex){Toast.makeText(this,"Kaydetme hatası: "+ex.getMessage(),1).show();}})
            .setNegativeButton("İptal",null).show();
    }

    String read(File f)throws Exception{
        BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
        StringBuilder s=new StringBuilder(); String line; while((line=r.readLine())!=null)s.append(line).append('\n'); r.close(); return s.toString();
    }
    void write(File f,String s)throws Exception{
        FileOutputStream o=new FileOutputStream(f); o.write(s.getBytes("UTF-8")); o.flush(); o.close();
    }

    void newDialog(){
        new AlertDialog.Builder(this).setTitle("Yeni")
            .setItems(new String[]{"Klasör","Dosya"},(d,w)->{if(w==0)newFolder();else newFile();}).show();
    }
    void newFolder(){
        prompt("Yeni klasör adı","",s->{File f=new File(current,s);if(!f.mkdir())toast("Oluşturulamadı");refresh();});
    }
    void newFile(){
        prompt("Yeni dosya adı","",s->{try{new File(current,s).createNewFile();refresh();}catch(Exception e){toast(e.getMessage());}});
    }
    void prompt(String title,String value,final Callback cb){
        EditText e=new EditText(this);e.setText(value);
        new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Tamam",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())cb.run(s);}).setNegativeButton("İptal",null).show();
    }

    interface Callback { void run(String s); }

    void fileMenu(File f){
        String[] a={"Aç","Yeniden adlandır","Kopyala","Kes","Sil","Özellikler","Paylaş","ZIP oluştur"};
        new AlertDialog.Builder(this).setTitle(f.getName()).setItems(a,(d,w)->{
            switch(w){
                case 0: openFile(f); break;
                case 1: rename(f); break;
                case 2: clipboard=f;cutMode=false;toast("Kopyalandı");break;
                case 3: clipboard=f;cutMode=true;toast("Kesildi");break;
                case 4: confirmDelete(f);break;
                case 5: properties(f);break;
                case 6: share(f);break;
                case 7: zipSingle(f);break;
            }
        }).show();
    }

    void rename(File f){prompt("Yeniden adlandır",f.getName(),s->{if(!f.renameTo(new File(f.getParentFile(),s)))toast("Ad değiştirilemedi");refresh();});}
    void confirmDelete(File f){new AlertDialog.Builder(this).setTitle("Silinsin mi?").setMessage(f.getAbsolutePath()).setPositiveButton("Sil",(d,w)->{deleteRecursive(f);refresh();}).setNegativeButton("İptal",null).show();}
    void deleteRecursive(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteRecursive(x);}f.delete();}
    void properties(File f){toast(f.getAbsolutePath()+"\nBoyut: "+human(f.length())+"\nSon değişiklik: "+new Date(f.lastModified()));}
    void share(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType(getMime(f.getName()));i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Paylaş"));}catch(Exception e){toast(e.getMessage());}}

    void paste(){
        if(clipboard==null){toast("Panoda dosya yok");return;}
        File dst=new File(current,clipboard.getName());
        try{
            if(dst.exists())deleteRecursive(dst);
            copyRecursive(clipboard,dst);
            if(cutMode)deleteRecursive(clipboard);
            clipboard=null;refresh();
        }catch(Exception e){toast("Yapıştırma hatası: "+e.getMessage());}
    }
    void copyRecursive(File a,File b)throws Exception{
        if(a.isDirectory()){b.mkdirs();File[] c=a.listFiles();if(c!=null)for(File x:c)copyRecursive(x,new File(b,x.getName()));}
        else {InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();}
    }

    void zipSingle(File f){
        File out=new File(current,f.getName()+".zip");
        try{ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out));zipRec(f,z,f.getName());z.close();refresh();}catch(Exception e){toast("ZIP hatası: "+e.getMessage());}
    }
    void zipRec(File f,ZipOutputStream z,String path)throws Exception{
        if(f.isDirectory()){File[] c=f.listFiles();if(c==null||c.length==0){z.putNextEntry(new ZipEntry(path+"/"));z.closeEntry();}else for(File x:c)zipRec(x,z,path+"/"+x.getName());}
        else{z.putNextEntry(new ZipEntry(path));InputStream in=new FileInputStream(f);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)z.write(b,0,n);in.close();z.closeEntry();}
    }

    void apps(){
        final PackageManager pm=getPackageManager();
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        ScrollView s=new ScrollView(this);s.addView(l);
        Button user=btn("Kullanıcı Uygulamaları");Button sys=btn("Sistem Uygulamaları");
        l.addView(user);l.addView(sys);
        user.setOnClickListener(v->showApps(l,false,pm));
        sys.setOnClickListener(v->showApps(l,true,pm));
        new AlertDialog.Builder(this).setTitle("APK Çıkar").setView(s).setPositiveButton("Kapat",null).show();
    }
    void showApps(LinearLayout l,boolean system,PackageManager pm){
        l.removeAllViews();
        for(ApplicationInfo ai:pm.getInstalledApplications(PackageManager.GET_META_DATA)){
            boolean isSystem=(ai.flags & ApplicationInfo.FLAG_SYSTEM)!=0;
            if(isSystem!=system)continue;
            Button b=btn(ai.loadLabel(pm)+"\n"+ai.packageName);
            b.setOnClickListener(v->appDetails(ai,pm));
            l.addView(b);
        }
    }
    void appDetails(ApplicationInfo ai,PackageManager pm){
        String label=String.valueOf(ai.loadLabel(pm));
        PackageInfo pi;try{pi=pm.getPackageInfo(ai.packageName,0);}catch(Exception e){return;}
        String msg=label+"\n"+ai.packageName+"\nSürüm: "+pi.versionName+" ("+(Build.VERSION.SDK_INT>=28?pi.getLongVersionCode():pi.versionCode)+")\nAPK: "+ai.sourceDir;
        new AlertDialog.Builder(this).setTitle("Uygulama").setMessage(msg)
            .setPositiveButton("APK Çıkar",(d,w)->extractApk(ai))
            .setNeutralButton("APK içeriği",(d,w)->apkContents(new File(ai.sourceDir)))
            .setNegativeButton("Kapat",null).show();
    }
    void extractApk(ApplicationInfo ai){
        ensureMusabFolders();
        File src=new File(ai.sourceDir);
        File dst=new File(APKS,ai.packageName+"-"+System.currentTimeMillis()+".apk");
        try{copyRecursive(src,dst);toast("APK çıkarıldı: "+dst.getAbsolutePath());}
        catch(Exception e){toast("APK çıkarma hatası: "+e.getMessage());}
    }

    void apkContents(File apk){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        ScrollView s=new ScrollView(this);s.addView(l);
        try{
            ZipFile z=new ZipFile(apk);Enumeration<? extends ZipEntry> en=z.entries();
            while(en.hasMoreElements()){ZipEntry e=en.nextElement();l.addView(tv(e.getName(),14));}
            z.close();
        }catch(Exception e){l.addView(tv("APK okunamadı: "+e.getMessage(),14));}
        new AlertDialog.Builder(this).setTitle("APK içeriği").setView(s).setPositiveButton("Kapat",null).show();
    }

    void toolsDialog(){
        String[] a={"APK Çıkar / Uygulamalar","APK Arşiv İçeriği","Python","IPython","Pillow","Terminal","AndroidManifest / XML","MusabFolder'a git"};
        new AlertDialog.Builder(this).setTitle("Musab Araçları").setItems(a,(d,w)->{
            if(w==0)apps();
            else if(w==1)apkContentsDialog();
            else if(w>=2&&w<=4)codeEditor(a[w]);
            else if(w==5)terminal();
            else if(w==6)xmlEditor();
            else {current=MUSAB;refresh();}
        }).show();
    }
    void apkContentsDialog(){prompt("APK yolu","",p->{File f=new File(p);if(f.isFile())apkContents(f);else toast("APK bulunamadı");});}

    void codeEditor(String type){
        EditText e=new EditText(this);e.setGravity(Gravity.TOP);e.setMinLines(18);e.setTextSize(14);
        e.setHint(type+" kodu");
        new AlertDialog.Builder(this).setTitle(type+" editörü").setView(e)
            .setPositiveButton("Dosyaya kaydet",(d,w)->prompt("Dosya adı",type.toLowerCase(Locale.ROOT)+".txt",n->{try{write(new File(current,n),e.getText().toString());refresh();}catch(Exception x){toast(x.getMessage());}}))
            .setNegativeButton("Kapat",null).show();
    }
    void xmlEditor(){codeEditor("AndroidManifest / XML");}
    void terminal(){toast("Terminal: gerçek gömülü Termux servisi yalnızca desteklenen Termux API/servisi kuruluysa bağlanabilir; sahte terminal gösterilmiyor.");}

    void viewDialog(){new AlertDialog.Builder(this).setTitle("Görünüm").setItems(new String[]{"Liste","Büyük simgeler","Sırala: ad","Sırala: boyut","Yenile"},(d,w)->{if(w==0){gridView=false;refresh();}else if(w==1){gridView=true;refresh();}else if(w==2){sortBySize=false;refresh();}else if(w==3){sortBySize=true;refresh();}else refresh();}).show();}

    void makeSalakPng(File f){
        try{
            Bitmap b=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
            Random r=new Random();for(int y=0;y<512;y++)for(int x=0;x<512;x++)b.setPixel(x,y,Color.rgb(r.nextInt(256),r.nextInt(256),r.nextInt(256)));
            FileOutputStream o=new FileOutputStream(f);b.compress(Bitmap.CompressFormat.PNG,100,o);o.close();
            Intent i=new Intent(Intent.ACTION_VIEW);Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);i.setDataAndType(u,"image/png");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
        }catch(Exception e){toast("PNG oluşturulamadı: "+e.getMessage());}
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
