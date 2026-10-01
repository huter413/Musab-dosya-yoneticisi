package com.musab.dosyayoneticisi;

import android.app.*;import android.os.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.graphics.drawable.Drawable;import android.net.Uri;import android.provider.Settings;import android.view.*;import android.widget.*;import androidx.core.content.FileProvider;import java.io.*;import java.util.*;import java.util.zip.*;

public class MainActivity extends Activity{
 LinearLayout root,list,actionBar; EditText pathEdit,searchEdit; File current,clipboard; boolean cutMode=false; float downX,downY; final File STORAGE=Environment.getExternalStorageDirectory(),MUSAB=new File(STORAGE,"MusabFolder"),APKS=new File(MUSAB,"apks"); final int REQ_STORAGE=10,REQ_TREE=11; Uri treeUri; int pad=18; boolean gridView=false,sortBySize=false; static final String SKK_PACKAGE="com.musab.skkinstaller"; final int BG=Color.rgb(10,10,12),PANEL=Color.rgb(24,24,28),FG=Color.WHITE;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(10,10,12));getWindow().setNavigationBarColor(Color.rgb(10,10,12));ensureMusabFolders();current=STORAGE;build();refresh();requestStorageAccess();}
 @Override protected void onResume(){super.onResume();if(root!=null){ensureMusabFolders();refresh();}}
 void ensureMusabFolders(){MUSAB.mkdirs();APKS.mkdirs();}
 boolean storageReady(){return Build.VERSION.SDK_INT<30||Environment.isExternalStorageManager();}
 void openTreeAccess(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,REQ_TREE);}catch(Exception e){toast("Klasör erişimi açılamadı");}}
 void requestStorageAccess(){if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){toast("Dosya erişimi için aşağıdaki erişim düğmesini kullanabilirsiniz.");}else if(Build.VERSION.SDK_INT>=23&&Build.VERSION.SDK_INT<=32&&checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"},REQ_STORAGE);}}
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_TREE&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){treeUri=data.getData();try{getContentResolver().takePersistableUriPermission(treeUri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}toast("Klasör erişimi verildi: "+treeUri);refresh();}}
 TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(FG);t.setPadding(pad,pad,pad,pad);return t;}
 Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
 void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL); root.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();}else if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dx)>180&&Math.abs(dx)>Math.abs(dy)*1.3f&&dx>0){goParent();return true;}}return false;});root.setBackgroundColor(BG);root.addView(tv("MusabDosya Yöneticisi",25));
  LinearLayout nav=new LinearLayout(this);Button rootBtn=btn("Kök");rootBtn.setOnClickListener(v->navigate("/"));nav.addView(rootBtn,new LinearLayout.LayoutParams(68,-2));Button back=btn("Geri");back.setOnClickListener(v->goParent());pathEdit=new EditText(this);pathEdit.setSingleLine(true);pathEdit.setTextSize(17);pathEdit.setTextColor(FG);pathEdit.setHintTextColor(Color.GRAY);pathEdit.setImeOptions(5);pathEdit.setOnEditorActionListener((v,id,e)->{navigate(pathEdit.getText().toString());return true;});Button go=btn("Git");go.setOnClickListener(v->navigate(pathEdit.getText().toString()));nav.addView(back,new LinearLayout.LayoutParams(68,-2));nav.addView(pathEdit,new LinearLayout.LayoutParams(0,-2,1));nav.addView(go,new LinearLayout.LayoutParams(68,-2));root.addView(nav);
  LinearLayout searchRow=new LinearLayout(this);searchEdit=new EditText(this);searchEdit.setSingleLine(true);searchEdit.setHint("Ara...");searchEdit.setTextColor(FG);Button search=btn("Ara");search.setOnClickListener(v->refresh());searchRow.addView(searchEdit,new LinearLayout.LayoutParams(0,-2,1));searchRow.addView(search);root.addView(searchRow);
  actionBar=new LinearLayout(this);String[] actions={"Yeni","Yapıştır","Araçlar","Görünüm"};for(String s:actions){Button b=btn(s);if(s.equals("Yeni"))b.setOnClickListener(v->newDialog());if(s.equals("Yapıştır"))b.setOnClickListener(v->paste());if(s.equals("Araçlar"))b.setOnClickListener(v->toolsDialog());if(s.equals("Görünüm"))b.setOnClickListener(v->viewDialog());actionBar.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(actionBar);
  ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
 void navigate(String p){if(p==null||p.trim().isEmpty())return;File f=new File(p.trim());if(!f.isAbsolute())f=new File(STORAGE,p.trim());try{if(f.isDirectory()){current=f.getCanonicalFile();refresh();}else toast("Dizin bulunamadı");}catch(Exception e){toast(e.getMessage());}}
 void goParent(){if(current!=null&&current.getParentFile()!=null){current=current.getParentFile();refresh();}}
 void refresh(){pathEdit.setText(current.getAbsolutePath());pathEdit.setSelection(pathEdit.length());list.removeAllViews();String q=searchEdit==null?"":searchEdit.getText().toString().trim().toLowerCase(Locale.ROOT);File[] fs=current.listFiles();if(fs==null){TextView msg=tv("Bu konuma doğrudan erişilemiyor. Tüm dosyalar iznini veya klasör erişimini ver.",16);msg.setGravity(Gravity.CENTER);list.addView(msg,new LinearLayout.LayoutParams(-1,120));Button access=btn("Tüm dosya erişimini aç");access.setOnClickListener(v->requestStorageAccess());list.addView(access,new LinearLayout.LayoutParams(-1,56));Button tree=btn("Klasör erişimi seç");tree.setOnClickListener(v->openTreeAccess());list.addView(tree,new LinearLayout.LayoutParams(-1,56));return;}Arrays.sort(fs,(a,b)->{if(sortBySize&&!a.isDirectory()&&!b.isDirectory())return Long.compare(b.length(),a.length());if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return a.getName().compareToIgnoreCase(b.getName());});int shown=0;for(File f:fs){if(!q.isEmpty()&&!f.getName().toLowerCase(Locale.ROOT).contains(q))continue;addItem(f);shown++;}if(shown==0)list.addView(tv("Sonuç yok.",16));}
 boolean specialName(File f){return f.getName().equals("Şirin")||f.getName().equals("Sirin");}
 int iconFor(File f){if(specialName(f))return R.drawable.ic_cute;if(f.isDirectory())return R.drawable.ic_folder;String x=f.getName().toLowerCase(Locale.ROOT);if(isText(x))return R.drawable.ic_text;if(isImage(x))return R.drawable.ic_image;if(isVideo(x))return R.drawable.ic_video;if(isAudio(x))return R.drawable.ic_audio;if(isSkk(x))return R.drawable.ic_skk;if(isArchive(x))return R.drawable.ic_tool_archive;return R.drawable.ic_file;}
 void addItem(File f){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(6,3,6,3);ImageView icon=new ImageView(this);icon.setImageResource(iconFor(f));TextView name=tv(f.getName(),17);name.setGravity(Gravity.CENTER_VERTICAL);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(8,2,8,2);row.addView(icon,new LinearLayout.LayoutParams(52,64));row.addView(name,new LinearLayout.LayoutParams(0,64,1));
  TextView size=tv(human(f.isDirectory()?directorySize(f):f.length()),11);size.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);size.setMaxLines(1);size.setEllipsize(android.text.TextUtils.TruncateAt.END);
  TextView date=tv(formatDate(f.lastModified()),10);date.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);date.setMaxLines(2);date.setEllipsize(android.text.TextUtils.TruncateAt.END);
  LinearLayout meta=new LinearLayout(this);meta.setOrientation(LinearLayout.VERTICAL);meta.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);meta.addView(size,new LinearLayout.LayoutParams(92,32));meta.addView(date,new LinearLayout.LayoutParams(92,32));row.addView(meta,new LinearLayout.LayoutParams(94,64));row.setBackgroundColor(PANEL);row.setOnClickListener(v->{if(f.isDirectory()){current=f;refresh();}else openFile(f);});row.setOnLongClickListener(v->{fileMenu(f);return true;});list.addView(row,new LinearLayout.LayoutParams(-1,72));}
 String human(long n){if(n<1024)return n+" B";if(n<1024*1024)return String.format(Locale.US,"%.1f KB",n/1024d);if(n<1024L*1024*1024)return String.format(Locale.US,"%.1f MB",n/1024d/1024d);return String.format(Locale.US,"%.1f GB",n/1024d/1024d/1024d);}
 String formatDate(long time){if(time<=0)return "-";return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date(time));}
 long directorySize(File dir){long total=0;File[] children=dir.listFiles();if(children==null)return 0;for(File child:children){if(child.isFile())total+=child.length();else if(child.isDirectory())total+=directorySize(child);}return total;}
 void openBuild(File f){try{Intent i=new Intent(this,MusabBuildActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}catch(Exception e){toast("Derleme merkezi açılamadı: "+e.getMessage());}}
 void openFile(File f){if(f.getName().equals("Salak.png")){makeSalakPng(f);return;}String x=f.getName().toLowerCase(Locale.ROOT);try{if(isSkk(x)){if(skkInstalled()){openWithSkk(f);return;}toast("SKK Yükleyicisi yüklü değil.");return;}Intent i;if(isText(x))i=new Intent(this,MusabTextActivity.class);else if(isImage(x))i=new Intent(this,MusabImageViewerActivity.class);else if(isVideo(x))i=new Intent(this,MusabVideoViewerActivity.class);else if(isAudio(x))i=new Intent(this,MusabAudioPlayerActivity.class);else if(isArchive(x))i=new Intent(this,MusabArchiveViewerActivity.class);else{i=new Intent(Intent.ACTION_VIEW);Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);i.setDataAndType(u,getMime(x));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}if(isText(x)||isImage(x)||isVideo(x)||isAudio(x)||isArchive(x))i.putExtra("path",f.getAbsolutePath());startActivity(i);}catch(Exception e){toast("Dosya açılamadı: "+e.getMessage());}}
 boolean isText(String x){return x.endsWith(".txt")||x.endsWith(".xml")||x.endsWith(".json")||x.endsWith(".java")||x.endsWith(".kt")||x.endsWith(".gradle")||x.endsWith(".smali")||x.endsWith(".properties")||x.endsWith(".md")||x.endsWith(".js")||x.endsWith(".html")||x.endsWith(".css")||x.endsWith(".py")||x.endsWith(".yml")||x.endsWith(".yaml")||x.endsWith(".ini")||x.endsWith(".cfg")||x.endsWith(".sh")||x.endsWith(".c")||x.endsWith(".cpp")||x.endsWith(".h")||x.endsWith(".hpp");}
 boolean isImage(String x){return x.endsWith(".png")||x.endsWith(".jpg")||x.endsWith(".jpeg")||x.endsWith(".gif")||x.endsWith(".webp")||x.endsWith(".bmp")||x.endsWith(".heic");}
 boolean isVideo(String x){return x.endsWith(".mp4")||x.endsWith(".mkv")||x.endsWith(".webm")||x.endsWith(".3gp")||x.endsWith(".avi")||x.endsWith(".mov")||x.endsWith(".m4v");}
 boolean isSkk(String x){return x.endsWith(".skk");}
 boolean skkInstalled(){try{return getPackageManager().getLaunchIntentForPackage(SKK_PACKAGE)!=null;}catch(Exception e){return false;}}
 void openWithSkk(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);i.setPackage(SKK_PACKAGE);i.setDataAndType(u,"application/x-skk");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception e){toast("SKK Yükleyicisi açılamadı: "+e.getMessage());}}
 boolean isAudio(String x){return x.endsWith(".mp3")||x.endsWith(".wav")||x.endsWith(".ogg")||x.endsWith(".m4a")||x.endsWith(".aac")||x.endsWith(".flac")||x.endsWith(".opus");}
 boolean isArchive(String x){return x.endsWith(".zip")||x.endsWith(".jar")||x.endsWith(".apk")||x.endsWith(".aab")||x.endsWith(".xapk")||x.endsWith(".apks");}
 String getMime(String x){if(isImage(x))return "image/*";if(isVideo(x))return "video/*";if(isAudio(x))return "audio/*";if(isSkk(x))return "application/x-skk";if(x.endsWith(".apk"))return "application/vnd.android.package-archive";if(isText(x))return "text/plain";return "*/*";}
 void newDialog(){new AlertDialog.Builder(this).setTitle("Yeni").setItems(new String[]{"Klasör","Dosya","Arşiv oluştur"},(d,w)->{if(w==0)newFolder();else if(w==1)newFile();else archiveSourceDialog();}).show();}
 void newFolder(){prompt("Yeni klasör adı","",s->{File f=new File(current,s);if(!f.mkdir())toast("Oluşturulamadı");refresh();});}
 void newFile(){prompt("Yeni dosya adı","",s->{try{new File(current,s).createNewFile();refresh();}catch(Exception e){toast(e.getMessage());}});}
 void prompt(String title,String value,Callback cb){EditText e=new EditText(this);e.setText(value);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Tamam",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())cb.run(s);}).setNegativeButton("İptal",null).show();}
 interface Callback{void run(String s);}
 void apkSignDialog(File f){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(12,4,12,4);CheckBox v1=new CheckBox(this);v1.setText("V1");CheckBox v2=new CheckBox(this);v2.setText("V2");CheckBox v3=new CheckBox(this);v3.setText("V3");v1.setChecked(true);v2.setChecked(true);box.addView(v1);box.addView(v2);box.addView(v3);new AlertDialog.Builder(this).setTitle("APK'ya imzala").setMessage(f.getName()+" için imza seçeneklerini seçin.").setView(box).setPositiveButton("İmzala",(d,w)->{String modes=(v1.isChecked()?"V1 ":"")+(v2.isChecked()?"V2 ":"")+(v3.isChecked()?"V3":"");if(modes.trim().isEmpty()){toast("En az bir imza sürümü seçin");return;}toast("İmzalama başarısız: cihaz içi APK imzalama motoru/keystore bağlı değil.");}).setNegativeButton("İptal",null).show();}
 void fileMenu(File f){final boolean skk=isSkk(f.getName().toLowerCase(Locale.ROOT))&&skkInstalled();final boolean project=isArchive(f.getName().toLowerCase(Locale.ROOT));final boolean apk=f.getName().toLowerCase(Locale.ROOT).endsWith(".apk");final ArrayList<String> labels=new ArrayList<>();final ArrayList<Integer> icons=new ArrayList<>();labels.add("Aç");icons.add(R.drawable.ic_file);if(skk){labels.add("SKK ile aç");icons.add(R.drawable.ic_skk);}if(project){labels.add("İle aç");icons.add(R.drawable.ic_tool_archive);}labels.add("Yeniden adlandır");icons.add(R.drawable.ic_file);labels.add("Kopyala");icons.add(R.drawable.ic_file);labels.add("Kes");icons.add(R.drawable.ic_file);labels.add("Sil");icons.add(R.drawable.ic_file);labels.add("Özellikler");icons.add(R.drawable.ic_tool_xml);labels.add("Paylaş");icons.add(R.drawable.ic_file);if(apk){labels.add("APK'ya imzala");icons.add(R.drawable.ic_tool_apk);}labels.add("ZIP oluştur");icons.add(R.drawable.ic_tool_archive);GridLayout grid=new GridLayout(this);grid.setColumnCount(3);grid.setPadding(8,8,8,8);for(int n=0;n<labels.size();n++){final int idx=n;LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);ImageView iv=new ImageView(this);iv.setImageResource(icons.get(n));TextView tv=tv(labels.get(n),12);tv.setGravity(Gravity.CENTER);cell.addView(iv,new LinearLayout.LayoutParams(56,50));cell.addView(tv,new LinearLayout.LayoutParams(88,42));cell.setOnClickListener(v->{String a=labels.get(idx);if(a.equals("Aç"))openFile(f);else if(a.equals("SKK ile aç"))openWithSkk(f);else if(a.equals("İle aç"))openBuild(f);else if(a.equals("Yeniden adlandır"))rename(f);else if(a.equals("Kopyala")){clipboard=f;cutMode=false;toast("Kopyalandı");}else if(a.equals("Kes")){clipboard=f;cutMode=true;toast("Kesildi");}else if(a.equals("Sil"))confirmDelete(f);else if(a.equals("Özellikler"))properties(f);else if(a.equals("Paylaş"))share(f);else if(a.equals("APK'ya imzala"))apkSignDialog(f);else if(a.equals("ZIP oluştur"))zipSingle(f);});grid.addView(cell,new ViewGroup.LayoutParams(96,94));}new AlertDialog.Builder(this).setTitle(f.getName()).setView(grid).show();}
 void rename(File f){prompt("Yeniden adlandır",f.getName(),s->{if(!f.renameTo(new File(f.getParentFile(),s)))toast("Ad değiştirilemedi");refresh();});}
 void confirmDelete(File f){new AlertDialog.Builder(this).setTitle("Silinsin mi?").setMessage(f.getAbsolutePath()).setPositiveButton("Sil",(d,w)->{deleteRecursive(f);refresh();}).setNegativeButton("İptal",null).show();}
 void deleteRecursive(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteRecursive(x);}f.delete();}
 void properties(File f){new AlertDialog.Builder(this).setTitle("Özellikler").setMessage("Yol: "+f.getAbsolutePath()+"\nBoyut: "+human(f.length())+"\nSon değişiklik: "+new Date(f.lastModified())).setPositiveButton("Tamam",null).show();}
 void share(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType(getMime(f.getName().toLowerCase(Locale.ROOT)));i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Paylaş"));}catch(Exception e){toast(e.getMessage());}}
 void paste(){if(clipboard==null){toast("Panoda dosya yok");return;}File dst=new File(current,clipboard.getName());try{if(dst.exists())deleteRecursive(dst);copyRecursive(clipboard,dst);if(cutMode)deleteRecursive(clipboard);clipboard=null;refresh();}catch(Exception e){toast("Yapıştırma hatası: "+e.getMessage());}}
 void copyRecursive(File a,File b)throws Exception{if(a.isDirectory()){b.mkdirs();File[] c=a.listFiles();if(c!=null)for(File x:c)copyRecursive(x,new File(b,x.getName()));}else{InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();}}
 void zipSingle(File f){File out=new File(current,f.getName()+".zip");try{ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out));zipRec(f,z,f.getName());z.close();refresh();}catch(Exception e){toast("ZIP hatası: "+e.getMessage());}}
 void archiveSourceDialog(){
  File[] fs=current.listFiles();
  if(fs==null){toast("Bu klasöre erişilemiyor");return;}
  ArrayList<File> items=new ArrayList<>();
  for(File f:fs)items.add(f);
  if(items.isEmpty()){toast("Bu klasör boş");return;}
  String[] names=new String[items.size()];
  for(int i=0;i<items.size();i++)names[i]=items.get(i).getName();
  new AlertDialog.Builder(this).setTitle("Arşivlenecek öğeyi seç").setItems(names,(d,w)->archiveDialog(items.get(w))).setNegativeButton("İptal",null).show();
 }
 void archiveDialog(File source){
  LinearLayout box=new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.setPadding(20,4,20,4);
  box.addView(tv("Arşiv türü",14));
  Spinner type=new Spinner(this);
  String[] types={"ZIP","ZIP (sıkıştırmasız)"};
  type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types));
  box.addView(type);
  EditText name=new EditText(this);
  name.setSingleLine(true);
  String base=source.getName();
  if(base.toLowerCase(Locale.ROOT).endsWith(".zip"))base=base.substring(0,base.length()-4);
  name.setText(base);
  name.setSelectAllOnFocus(true);
  box.addView(name);
  TextView info=tv("Çıktı: "+current.getAbsolutePath(),12);
  info.setMaxLines(2);
  info.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
  box.addView(info);
  new AlertDialog.Builder(this).setTitle("Arşiv oluştur: "+source.getName()).setView(box)
    .setPositiveButton("Oluştur",(d,w)->{
      String n=name.getText().toString().trim();
      if(n.isEmpty()){toast("Arşiv adı boş olamaz");return;}
      if(!n.toLowerCase(Locale.ROOT).endsWith(".zip"))n+=".zip";
      File out=new File(current,n);
      createZipArchive(source,out,type.getSelectedItemPosition()==1);
    }).setNegativeButton("İptal",null).show();
 }
 void createZipArchive(File source,File out,boolean stored){
  String src=source.getAbsolutePath();
  String dst=out.getAbsolutePath();
  if(dst.equals(src)||dst.startsWith(src+File.separator)){
    toast("Arşiv hedefi kaynak klasörün içinde olamaz");
    return;
  }
  if(out.exists()){
    new AlertDialog.Builder(this).setTitle("Dosya zaten var")
      .setMessage(out.getName()+" üzerine yazılsın mı?")
      .setPositiveButton("Üzerine yaz",(d,w)->writeZip(source,out,stored))
      .setNegativeButton("İptal",null).show();
  }else writeZip(source,out,stored);
 }
 void writeZip(File source,File out,boolean stored){
  File tmp=new File(out.getAbsolutePath()+".part");
  try{
    if(tmp.exists()&&!tmp.delete())throw new IOException("Geçici dosya silinemedi");
    ZipOutputStream z=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
    try{
      if(stored)z.setLevel(Deflater.NO_COMPRESSION);
      zipRec(source,z,source.getName());
    }finally{
      z.close();
    }
    ZipFile verify=new ZipFile(tmp);
    verify.close();
    if(out.exists()&&!out.delete())throw new IOException("Eski arşiv silinemedi");
    if(!tmp.renameTo(out))throw new IOException("Arşiv dosyası oluşturulamadı");
    refresh();
    toast("Geçerli ZIP oluşturuldu: "+out.getName());
  }catch(Exception e){
    tmp.delete();
    toast("ZIP oluşturma hatası: "+e.getMessage());
  }
 }
 void zipRec(File f,ZipOutputStream z,String path)throws Exception{
  if(f.isDirectory()){
    File[] children=f.listFiles();
    if(children==null||children.length==0){
      z.putNextEntry(new ZipEntry(path+"/"));
      z.closeEntry();
      return;
    }
    for(File child:children)zipRec(child,z,path+"/"+child.getName());
  }else{
    z.putNextEntry(new ZipEntry(path));
    FileInputStream in=new FileInputStream(f);
    try{
      byte[] buffer=new byte[8192];
      int n;
      while((n=in.read(buffer))!=-1)z.write(buffer,0,n);
    }finally{
      in.close();
    }
    z.closeEntry();
  }
 }
 void apps(){final PackageManager pm=getPackageManager();LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);TextView count=tv("Uygulamalar yükleniyor...",16);l.addView(count);ScrollView s=new ScrollView(this);s.addView(l);new AlertDialog.Builder(this).setTitle("APK Çıkar").setView(s).setPositiveButton("Kapat",null).show();new Thread(()->{List<ApplicationInfo> all;try{all=pm.getInstalledApplications(PackageManager.GET_META_DATA);}catch(Exception ex){all=new ArrayList<>();}if(all==null)all=new ArrayList<>();all.sort((a,b)->String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));final List<ApplicationInfo> apps=all;runOnUiThread(()->{l.removeAllViews();long uc=apps.stream().filter(a->(a.flags&ApplicationInfo.FLAG_SYSTEM)==0).count(),sc=apps.size()-uc;Button user=btn("Kullanıcı Uygulamaları ("+uc+")"),sys=btn("Sistem Uygulamaları ("+sc+")");l.addView(user);l.addView(sys);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);l.addView(results);count.setText("Toplam görünür uygulama: "+apps.size());if(apps.isEmpty()){count.setText("Uygulama listesi boş. APK dosyaları için APK klasörünü de tarayabilirsiniz.");Button scan=btn("Depolamadaki APK dosyalarını tara");scan.setOnClickListener(v->scanApks(results));l.addView(scan);}user.setOnClickListener(v->showAppsList(results,apps,false,pm));sys.setOnClickListener(v->showAppsList(results,apps,true,pm));showAppsList(results,apps,false,pm);});}).start();}
 void scanApks(LinearLayout l){l.removeAllViews();l.addView(tv("Depolamadaki APK dosyaları",18));new Thread(()->{ArrayList<File> found=new ArrayList<>();scanApkRecursive(STORAGE,found,0);runOnUiThread(()->{if(found.isEmpty())l.addView(tv("APK bulunamadı.",16));else for(File f:found){TextView t=tv(f.getAbsolutePath()+"\n"+human(f.length()),15);t.setOnClickListener(v->apkContents(f));l.addView(t,new LinearLayout.LayoutParams(-1,72));}});}).start();}
 void scanApkRecursive(File dir,ArrayList<File> out,int depth){if(dir==null||depth>5||out.size()>500)return;File[] fs=dir.listFiles();if(fs==null)return;for(File f:fs){if(f.isFile()&&f.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))out.add(f);else if(f.isDirectory()&&!f.getName().equals("Android"))scanApkRecursive(f,out,depth+1);}}
 void showAppsList(LinearLayout l,List<ApplicationInfo> all,boolean system,PackageManager pm){l.removeAllViews();l.addView(tv(system?"Tüm sistem uygulamaları":"Tüm kullanıcı uygulamaları",18));int shown=0;for(ApplicationInfo ai:all){boolean isSystem=(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0;if(isSystem!=system)continue;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);ImageView iv=new ImageView(this);Drawable d=ai.loadIcon(pm);iv.setImageDrawable(d);TextView t=tv(String.valueOf(ai.loadLabel(pm))+"\n"+ai.packageName,16);t.setGravity(Gravity.CENTER_VERTICAL);t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setPadding(8,3,8,3);row.addView(iv,new LinearLayout.LayoutParams(54,72));row.addView(t,new LinearLayout.LayoutParams(0,72,1));row.setPadding(4,3,4,3);row.setOnClickListener(v->appDetails(ai,pm));l.addView(row,new LinearLayout.LayoutParams(-1,78));shown++;}if(shown==0)l.addView(tv("Bu kategoride uygulama bulunamadı.",16));}
 void appDetails(ApplicationInfo ai,PackageManager pm){String label=String.valueOf(ai.loadLabel(pm));PackageInfo pi;try{pi=pm.getPackageInfo(ai.packageName,0);}catch(Exception e){toast("Uygulama bilgisi okunamadı");return;}long vc=Build.VERSION.SDK_INT>=28?pi.getLongVersionCode():pi.versionCode;File apk=new File(ai.sourceDir);String msg=label+"\n"+ai.packageName+"\nSürüm: "+pi.versionName+" ("+vc+")\nTemel APK: "+ai.sourceDir+"\nBoyut: "+human(apk.length())+"\nEk APK parçaları: "+(ai.splitSourceDirs==null?0:ai.splitSourceDirs.length);new AlertDialog.Builder(this).setTitle("Uygulama").setMessage(msg).setPositiveButton("APK Çıkar",(d,w)->extractApk(ai)).setNeutralButton("APK içeriği",(d,w)->apkContents(apk)).setNegativeButton("Kapat",null).show();}
 void extractApk(ApplicationInfo ai){ensureMusabFolders();String safe=ai.packageName.replaceAll("[^A-Za-z0-9._-]","_");File dir=new File(APKS,safe+"-"+System.currentTimeMillis());dir.mkdirs();try{copyRecursive(new File(ai.sourceDir),new File(dir,"base.apk"));if(ai.splitSourceDirs!=null)for(int n=0;n<ai.splitSourceDirs.length;n++)copyRecursive(new File(ai.splitSourceDirs[n]),new File(dir,"split-"+n+".apk"));toast("APK çıkarıldı: "+dir.getAbsolutePath());}catch(Exception e){toast("APK çıkarma hatası: "+e.getMessage());}}
 void toolsDialog(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(8,8,8,8);
  final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Musab Araçları").setView(box).setNegativeButton("Kapat",null).create();
  toolRow(box,"APK Çıkar / Uygulamalar",R.drawable.ic_tool_apk,()->apps(),dialog);
  toolRow(box,"APK Arşiv İçeriği",R.drawable.ic_tool_archive,()->apkContentsDialog(),dialog);
  toolRow(box,"Python düzenleyici",R.drawable.ic_tool_python,()->codeEditor("Python"),dialog);
  toolRow(box,"IPython düzenleyici",R.drawable.ic_tool_ipython,()->codeEditor("IPython"),dialog);
  toolRow(box,"Pillow düzenleyici",R.drawable.ic_tool_pillow,()->codeEditor("Pillow"),dialog);
  toolRow(box,"AndroidManifest / XML düzenleyici",R.drawable.ic_tool_xml,()->xmlEditor(),dialog);
  toolRow(box,"Terminal",R.drawable.ic_tool_terminal,()->terminal(),dialog);
  toolRow(box,"MusabFolder'a git",R.drawable.ic_tool_folder,()->{current=MUSAB;refresh();},dialog);
  dialog.show();
 }
 void toolRow(LinearLayout box,String title,int iconId,final Runnable action,final AlertDialog dialog){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setGravity(Gravity.CENTER);row.setPadding(4,5,4,5);ImageView icon=new ImageView(this);icon.setImageResource(iconId);TextView label=tv(title,12);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(icon,new LinearLayout.LayoutParams(54,48));row.addView(label,new LinearLayout.LayoutParams(-1,38));row.setBackgroundColor(PANEL);row.setOnClickListener(v->{dialog.dismiss();action.run();});box.addView(row,new LinearLayout.LayoutParams(-1,88));}
 void apkContentsDialog(){prompt("APK yolu","",p->{File f=new File(p);if(f.isFile())apkContents(f);else toast("APK bulunamadı");});}
 void codeEditor(String type){EditText e=new EditText(this);e.setGravity(Gravity.TOP);e.setMinLines(18);e.setTextSize(14);e.setHint(type+" kodu");new AlertDialog.Builder(this).setTitle(type+" editörü").setView(e).setPositiveButton("Dosyaya kaydet",(d,w)->prompt("Dosya adı",type.toLowerCase(Locale.ROOT)+".txt",n->{try{write(new File(current,n),e.getText().toString());refresh();}catch(Exception x){toast(x.getMessage());}})).setNegativeButton("Kapat",null).show();}
 void xmlEditor(){codeEditor("AndroidManifest / XML");}
 void terminal(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(Color.BLACK);TextView h=tv("Musab Terminal",20);h.setTextColor(Color.WHITE);r.addView(h);TextView p=tv(current.getAbsolutePath(),13);p.setTextColor(Color.LTGRAY);r.addView(p);ScrollView sv=new ScrollView(this);TextView out=tv("Musab Terminal\\n$ ",14);out.setTextColor(Color.WHITE);sv.addView(out);r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));LinearLayout line=new LinearLayout(this);EditText in=new EditText(this);in.setSingleLine(true);in.setTextColor(Color.WHITE);in.setHintTextColor(Color.GRAY);in.setHint("komut");Button run=btn("Çalıştır");line.addView(in,new LinearLayout.LayoutParams(0,-2,1));line.addView(run);r.addView(line);Button back=btn("Dosya Yöneticisine Dön");back.setOnClickListener(v->setContentView(root));r.addView(back);setContentView(r);run.setOnClickListener(v->{String cmd=in.getText().toString().trim();if(cmd.isEmpty())return;out.append(cmd+"\\n"+localCommand(cmd)+"\\n$ ");in.setText("");sv.post(()->sv.fullScroll(View.FOCUS_DOWN));});}
String localCommand(String cmd){try{if(cmd.equals("pwd"))return current.getAbsolutePath();if(cmd.equals("ls")){File[] fs=current.listFiles();if(fs==null)return "Erişim yok";StringBuilder s=new StringBuilder();for(File f:fs)s.append(f.isDirectory()?"[D] ":"[F] ").append(f.getName()).append("\\n");return s.toString();}if(cmd.startsWith("cd ")){File f=new File(current,cmd.substring(3).trim());if(f.isDirectory()){current=f.getCanonicalFile();return "Dizin değişti: "+current;}return "Dizin bulunamadı";}if(cmd.equals("clear"))return "";if(cmd.equals("help"))return "pwd, ls, cd <klasör>, clear, help";return "Komut desteklenmiyor";}catch(Exception e){return "Hata: "+e.getMessage();}}
void viewDialog(){new AlertDialog.Builder(this).setTitle("Görünüm").setItems(new String[]{"Liste","Büyük simgeler","Sırala: ad","Sırala: boyut","Yenile"},(d,w)->{if(w==0){gridView=false;refresh();}else if(w==1){gridView=true;refresh();}else if(w==2){sortBySize=false;refresh();}else if(w==3){sortBySize=true;refresh();}else refresh();}).show();}
 void makeSalakPng(File f){try{Bitmap b=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Random r=new Random();for(int y=0;y<512;y++)for(int x=0;x<512;x++)b.setPixel(x,y,Color.rgb(r.nextInt(256),r.nextInt(256),r.nextInt(256)));FileOutputStream o=new FileOutputStream(f);b.compress(Bitmap.CompressFormat.PNG,100,o);o.close();openFile(f);}catch(Exception e){toast("PNG oluşturulamadı: "+e.getMessage());}}
 void write(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f,false);o.write(s.getBytes("UTF-8"));o.close();}
 void apkContents(File f){try{ZipFile z=new ZipFile(f);StringBuilder s=new StringBuilder();Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements())s.append(en.nextElement().getName()).append('\n');z.close();new AlertDialog.Builder(this).setTitle("APK içeriği").setMessage(s.toString()).setPositiveButton("Kapat",null).show();}catch(Exception e){toast("APK okunamadı: "+e.getMessage());}}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}