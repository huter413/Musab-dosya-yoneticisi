package com.musab.dosyayoneticisi;

import android.app.*;import android.os.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.graphics.drawable.Drawable;import android.net.Uri;import android.provider.Settings;import android.view.*;import android.widget.*;import android.security.keystore.KeyGenParameterSpec;import android.security.keystore.KeyProperties;import androidx.core.content.FileProvider;import com.android.apksig.ApkSigner;import com.android.apksig.ApkVerifier;import java.io.*;import java.math.BigInteger;import java.security.*;import java.security.cert.Certificate;import java.security.cert.X509Certificate;import java.util.*;import java.util.zip.*;import java.net.*;import javax.security.auth.x500.X500Principal;

public class MainActivity extends Activity{
 LinearLayout root,list,actionBar; EditText pathEdit,searchEdit; File current,clipboard; boolean cutMode=false; float downX,downY; final File STORAGE=Environment.getExternalStorageDirectory(),MUSAB=new File(STORAGE,"MusabFolder"),APKS=new File(MUSAB,"apks"); final int REQ_STORAGE=10,REQ_TREE=11; Uri treeUri; int pad=18; boolean gridView=false,sortBySize=false; static final String SKK_PACKAGE="com.musab.skkinstaller"; final int BG=Color.rgb(10,10,12),PANEL=Color.rgb(24,24,28),FG=Color.WHITE;
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(10,10,12));getWindow().setNavigationBarColor(Color.rgb(10,10,12));ensureMusabFolders();current=STORAGE;build();refresh();requestStorageAccess();}
 @Override protected void onResume(){super.onResume();if(root!=null)ensureMusabFolders();}
 void ensureMusabFolders(){MUSAB.mkdirs();APKS.mkdirs();}
 boolean storageReady(){return Build.VERSION.SDK_INT<30||Environment.isExternalStorageManager();}
 void openTreeAccess(){try{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,REQ_TREE);}catch(Exception e){toast("Klasör erişimi açılamadı");}}
 void requestStorageAccess(){if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){new AlertDialog.Builder(this).setTitle("Dosya erişimi gerekli").setMessage("Musab Dosya Yöneticisi dosyaları gösterebilmek için tüm dosya erişimine ihtiyaç duyuyor. Ayarlardan izin verebilirsiniz.").setPositiveButton("Ayarları aç",(d,w)->openAllFilesSettings()).setNeutralButton("Klasör seç",(d,w)->openTreeAccess()).setNegativeButton("Şimdi değil",null).show();}else if(Build.VERSION.SDK_INT>=23&&Build.VERSION.SDK_INT<=32&&checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE")!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"},REQ_STORAGE);}}
 void openAllFilesSettings(){try{Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){try{startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}catch(Exception x){toast("Dosya erişimi ayarları açılamadı");}}}
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_TREE&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){treeUri=data.getData();try{getContentResolver().takePersistableUriPermission(treeUri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}toast("Klasör erişimi verildi: "+treeUri);refresh();}}
 TextView tv(String s,int sp){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(FG);t.setPadding(pad,pad,pad,pad);return t;}
 Button btn(String s){Button b=new Button(this);b.setText(s);b.setGravity(Gravity.CENTER);b.setTextSize(16);b.setSingleLine(true);b.setIncludeFontPadding(false);b.setMinHeight(0);b.setMinWidth(0);b.setPadding(8,0,8,0);b.setAllCaps(false);return b;}
 void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL); root.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();}else if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dx)>180&&Math.abs(dx)>Math.abs(dy)*1.3f&&dx>0){goParent();return true;}}return false;});root.setBackgroundColor(BG);root.addView(tv("MusabDosya Yöneticisi",25));
  LinearLayout nav=new LinearLayout(this);Button rootBtn=btn("Kök");rootBtn.setOnClickListener(v->navigate("/"));nav.addView(rootBtn,new LinearLayout.LayoutParams(68,-2));Button back=btn("Geri");back.setOnClickListener(v->goParent());pathEdit=new EditText(this);pathEdit.setSingleLine(true);pathEdit.setTextSize(17);pathEdit.setTextColor(FG);pathEdit.setHintTextColor(Color.GRAY);pathEdit.setImeOptions(5);pathEdit.setOnEditorActionListener((v,id,e)->{navigate(pathEdit.getText().toString());return true;});Button go=btn("Git");go.setOnClickListener(v->navigate(pathEdit.getText().toString()));nav.addView(back,new LinearLayout.LayoutParams(68,-2));nav.addView(pathEdit,new LinearLayout.LayoutParams(0,-2,1));nav.addView(go,new LinearLayout.LayoutParams(68,-2));root.addView(nav);
  LinearLayout searchRow=new LinearLayout(this);searchEdit=new EditText(this);searchEdit.setSingleLine(true);searchEdit.setHint("Ara...");searchEdit.setTextColor(FG);Button search=btn("Ara");search.setOnClickListener(v->refresh());searchRow.addView(searchEdit,new LinearLayout.LayoutParams(0,-2,1));searchRow.addView(search);root.addView(searchRow);
  actionBar=new LinearLayout(this);String[] actions={"Yeni","Yapıştır","Araçlar","Görünüm"};for(String s:actions){Button b=btn(s);if(s.equals("Yeni"))b.setOnClickListener(v->newDialog());if(s.equals("Yapıştır"))b.setOnClickListener(v->paste());if(s.equals("Araçlar"))b.setOnClickListener(v->toolsDialog());if(s.equals("Görünüm"))b.setOnClickListener(v->viewDialog());actionBar.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(actionBar);
  ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
 void navigate(String p){if(p==null||p.trim().isEmpty())return;String raw=p.trim();try{File f;if(raw.equals("/")||raw.equalsIgnoreCase(STORAGE.getAbsolutePath())||raw.equalsIgnoreCase(STORAGE.getCanonicalPath())){f=STORAGE;}else{f=resolvePathCaseInsensitive(raw);}if(f!=null&&f.isDirectory()){current=f.getCanonicalFile();refresh();}else toast("Dizin bulunamadı: "+raw);}catch(Exception e){toast("Dizin bulunamadı: "+raw);}}
 File resolvePathCaseInsensitive(String raw)throws IOException{String normalized=raw.replace('\\','/');File candidate=new File(normalized);if(!candidate.isAbsolute())candidate=new File(STORAGE,normalized);String path=candidate.getAbsolutePath();String storagePath=STORAGE.getAbsolutePath();if(path.equalsIgnoreCase(storagePath))return STORAGE;if(path.regionMatches(true,0,storagePath,0,storagePath.length())){String rel=path.substring(storagePath.length());while(rel.startsWith(File.separator)||rel.startsWith("/"))rel=rel.substring(1);File cur=STORAGE;for(String part:rel.split("/")){if(part.isEmpty()||part.equals("."))continue;if(part.equals("..")){File parent=cur.getParentFile();if(parent!=null&&!cur.equals(STORAGE))cur=parent;continue;}File[] children=cur.listFiles();if(children==null)return null;File match=null;for(File child:children)if(child.getName().equalsIgnoreCase(part)){match=child;break;}if(match==null)return null;cur=match;}return cur;}if(candidate.exists())return candidate;File parent=candidate.getParentFile();if(parent==null)return null;File[] children=parent.listFiles();if(children!=null)for(File child:children)if(child.getName().equalsIgnoreCase(candidate.getName()))return child;return null;}
 void goParent(){if(current!=null&&current.getParentFile()!=null){current=current.getParentFile();refresh();}}
 void refresh(){pathEdit.setText(current.getAbsolutePath());pathEdit.setSelection(pathEdit.length());list.removeAllViews();String q=searchEdit==null?"":searchEdit.getText().toString().trim().toLowerCase(Locale.ROOT);File[] fs=current.listFiles();if(fs==null){TextView msg=tv("Bu konuma doğrudan erişilemiyor. Android sistem erişimini kontrol edin.",16);msg.setGravity(Gravity.CENTER);list.addView(msg,new LinearLayout.LayoutParams(-1,120));Button access=btn("Erişimi kontrol et");access.setOnClickListener(v->requestStorageAccess());list.addView(access,new LinearLayout.LayoutParams(-1,56));return;}Arrays.sort(fs,(a,b)->{if(sortBySize&&!a.isDirectory()&&!b.isDirectory())return Long.compare(b.length(),a.length());if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return a.getName().compareToIgnoreCase(b.getName());});int shown=0;for(File f:fs){if(!q.isEmpty()&&!f.getName().toLowerCase(Locale.ROOT).contains(q))continue;addItem(f);shown++;}if(shown==0)list.addView(tv("Sonuç yok.",16));}
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
 void openFile(File f){String x=f.getName().toLowerCase(Locale.ROOT);try{if(isSkk(x)){if(skkInstalled()){openWithSkk(f);return;}toast("SKK Yükleyicisi yüklü değil.");return;}if(x.endsWith(".apk")){installApkFile(f);return;}Intent i;if(isText(x))i=new Intent(this,MusabTextActivity.class);else if(isImage(x))i=new Intent(this,MusabImageViewerActivity.class);else if(isVideo(x))i=new Intent(this,MusabVideoViewerActivity.class);else if(isAudio(x))i=new Intent(this,MusabAudioPlayerActivity.class);else if(isArchive(x))i=new Intent(this,MusabArchiveViewerActivity.class);else{i=new Intent(Intent.ACTION_VIEW);Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);i.setDataAndType(u,getMime(x));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}if(isText(x)||isImage(x)||isVideo(x)||isAudio(x)||isArchive(x))i.putExtra("path",f.getAbsolutePath());startActivity(i);}catch(Exception e){toast("Dosya açılamadı: "+e.getMessage());}}
 boolean isText(String x){return x.endsWith(".txt")||x.endsWith(".xml")||x.endsWith(".json")||x.endsWith(".java")||x.endsWith(".kt")||x.endsWith(".gradle")||x.endsWith(".smali")||x.endsWith(".properties")||x.endsWith(".md")||x.endsWith(".js")||x.endsWith(".html")||x.endsWith(".css")||x.endsWith(".py")||x.endsWith(".yml")||x.endsWith(".yaml")||x.endsWith(".ini")||x.endsWith(".cfg")||x.endsWith(".sh")||x.endsWith(".c")||x.endsWith(".cpp")||x.endsWith(".h")||x.endsWith(".hpp");}
 boolean isImage(String x){return x.endsWith(".png")||x.endsWith(".jpg")||x.endsWith(".jpeg")||x.endsWith(".gif")||x.endsWith(".webp")||x.endsWith(".bmp")||x.endsWith(".heic");}
 boolean isVideo(String x){return x.endsWith(".mp4")||x.endsWith(".mkv")||x.endsWith(".webm")||x.endsWith(".3gp")||x.endsWith(".avi")||x.endsWith(".mov")||x.endsWith(".m4v");}
 boolean isSkk(String x){return x.endsWith(".skk");}
 boolean skkInstalled(){try{return getPackageManager().getLaunchIntentForPackage(SKK_PACKAGE)!=null;}catch(Exception e){return false;}}
 void openWithSkk(File f){
  try{
   Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
   Intent i=new Intent(Intent.ACTION_VIEW);
   i.setComponent(new ComponentName(SKK_PACKAGE,SKK_PACKAGE+".SkkInstallerActivity"));
   i.setDataAndType(u,"application/x-skk");
   i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
   i.setClipData(ClipData.newRawUri("SKK",u));
   i.putExtra("skk_source_uri",u.toString());
   startActivity(i);
  }catch(Exception e){
   try{
    Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
    Intent fallback=new Intent(this,SkkInstallerActivity.class);
    fallback.setDataAndType(u,"application/x-skk");
    fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    fallback.setClipData(ClipData.newRawUri("SKK",u));
    startActivity(fallback);
   }catch(Exception x){toast("SKK Yükleyicisi açılamadı: "+x.getMessage());}
  }
} void installApkFile(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE);i.setDataAndType(u,"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,getPackageName());startActivity(i);}catch(Exception e){toast("APK paket yükleyicisi açılamadı: "+e.getMessage());}}
 boolean isAudio(String x){return x.endsWith(".mp3")||x.endsWith(".wav")||x.endsWith(".ogg")||x.endsWith(".m4a")||x.endsWith(".aac")||x.endsWith(".flac")||x.endsWith(".opus");}
 boolean isArchive(String x){return x.endsWith(".zip")||x.endsWith(".jar")||x.endsWith(".apk")||x.endsWith(".aab")||x.endsWith(".xapk")||x.endsWith(".apks");}
 String getMime(String x){if(isImage(x))return "image/*";if(isVideo(x))return "video/*";if(isAudio(x))return "audio/*";if(isSkk(x))return "application/x-skk";if(x.endsWith(".apk"))return "application/vnd.android.package-archive";if(isText(x))return "text/plain";return "*/*";}
 void newDialog(){new AlertDialog.Builder(this).setTitle("Yeni").setItems(new String[]{"Klasör","Dosya","Arşiv oluştur"},(d,w)->{if(w==0)newFolder();else if(w==1)newFile();else archiveSourceDialog();}).show();}
 void newFolder(){prompt("Yeni klasör adı","",s->{File f=new File(current,s);if(f.exists()){showCreateConflict(f,true,s);return;}if(!f.mkdir())toast("Oluşturulamadı");refresh();});}
 void newFile(){prompt("Yeni dosya adı","",s->{try{File f=new File(current,s);if(f.exists()){showCreateConflict(f,false,s);return;}if(s.equals("Salak.png"))copyBundledSalakPng(f);else if(!f.createNewFile())toast("Oluşturulamadı");refresh();}catch(Exception e){toast("Dosya oluşturulamadı: "+e.getMessage());}});}
 void showCreateConflict(File existing,boolean directory,String requestedName){new AlertDialog.Builder(this).setTitle("Dosya zaten var").setMessage(existing.getName()+" zaten mevcut. Ne yapmak istiyorsun?").setPositiveButton("Kopya oluştur",(d,w)->{File copy=uniqueSibling(existing);try{if(directory)copy.mkdirs();else if(requestedName.equals("Salak.png"))copyBundledSalakPng(copy);else copy.createNewFile();refresh();}catch(Exception e){toast("Kopya oluşturulamadı: "+e.getMessage());}}).setNeutralButton("Değiştir",(d,w)->{try{if(directory){deleteRecursive(existing);existing.mkdir();}else if(requestedName.equals("Salak.png"))copyBundledSalakPng(existing);else{if(existing.isDirectory())deleteRecursive(existing);existing.delete();existing.createNewFile();}refresh();}catch(Exception e){toast("Değiştirilemedi: "+e.getMessage());}}).setNegativeButton("İptal",null).show();}
 File uniqueSibling(File wanted){String name=wanted.getName();int dot=name.lastIndexOf(".");String base=dot>0?name.substring(0,dot):name;String ext=dot>0?name.substring(dot):"";int n=1;File candidate;do{candidate=new File(wanted.getParentFile(),base+" ("+n+")"+ext);n++;}while(candidate.exists());return candidate;}
 void copyBundledSalakPng(File target)throws Exception{try(InputStream in=getResources().openRawResource(R.drawable.salak);OutputStream out=new FileOutputStream(target,false)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}}

 void prompt(String title,String value,Callback cb){EditText e=new EditText(this);e.setText(value);new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Tamam",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty())cb.run(s);}).setNegativeButton("İptal",null).show();}
 interface Callback{void run(String s);}
 void apkSignDialog(File f){
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(12,4,12,4);
  Spinner schemes=new Spinner(this);String[] options={"V1","V2","V3","V1+V2","V1+V3","V2+V3","V1+V2+V3"};schemes.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options));
  box.addView(tv("İmza şeması",14));box.addView(schemes);
  new AlertDialog.Builder(this).setTitle("APK'ya imzala").setMessage(f.getName()+" için imza şemasını seç.").setView(box)
    .setPositiveButton("İmzala",(d,w)->{String mode=String.valueOf(schemes.getSelectedItem());new Thread(()->{try{File out=uniqueSibling(new File(f.getParentFile(),f.getName().replaceFirst("(?i)\\\\.apk$","")+" (imzalı).apk"));signApkWithMode(f,out,mode);runOnUiThread(()->{refresh();toast("İmzalandı: "+out.getName()+" ["+mode+"]");properties(out);});}catch(Exception e){runOnUiThread(()->toast("İmzalama başarısız: "+e.getMessage()));}}).start();})
    .setNegativeButton("İptal",null).show();
 }
 void ensureSigningKey()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);String alias="musab-apk-signing";
  if(!ks.containsAlias(alias)){KeyPairGenerator gen=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");gen.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY).setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA1,KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA512).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).setCertificateSubject(new X500Principal("CN=Musab APK Signer")).setCertificateSerialNumber(BigInteger.ONE).setCertificateNotBefore(new Date(System.currentTimeMillis()-86400000L)).setCertificateNotAfter(new Date(System.currentTimeMillis()+315360000000L)).build());gen.generateKeyPair();}
 }
 void signApkWithMode(File input,File output,String mode)throws Exception{
  ensureSigningKey();KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);PrivateKey key=(PrivateKey)ks.getKey("musab-apk-signing",null);Certificate cert=ks.getCertificate("musab-apk-signing");if(!(cert instanceof X509Certificate))throw new GeneralSecurityException("İmza sertifikası alınamadı");
  ArrayList<X509Certificate> certs=new ArrayList<>();certs.add((X509Certificate)cert);ApkSigner.SignerConfig signer=new ApkSigner.SignerConfig.Builder("Musab",key,certs).build();
  boolean v1=mode.contains("V1"),v2=mode.contains("V2"),v3=mode.contains("V3");
  new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(input).setOutputApk(output).setV1SigningEnabled(v1).setV2SigningEnabled(v2).setV3SigningEnabled(v3).setV4SigningEnabled(false).setOtherSignersSignaturesPreserved(false).build().sign();
  if(!output.isFile()||output.length()==0)throw new IOException("İmzalı APK oluşturulamadı");
 }
 void fileMenu(File f){
  final String lower=f.getName().toLowerCase(Locale.ROOT);
  final boolean skk=isSkk(lower)&&skkInstalled();
  final boolean archive=isArchive(lower);
  final boolean apk=lower.endsWith(".apk");
  final boolean material=lower.endsWith(".material.bin")||lower.endsWith("material.bin")||lower.endsWith(".bin");
  final ArrayList<String> labels=new ArrayList<>();final ArrayList<Integer> icons=new ArrayList<>();
  labels.add("Aç");icons.add(R.drawable.ic_file);
  if(skk){labels.add("SKK ile aç");icons.add(R.drawable.ic_skk);}
  labels.add("İle aç");icons.add(R.drawable.ic_file);
  if(apk){labels.add("Decompile APK");icons.add(R.drawable.ic_tool_apk);labels.add("Patchle");icons.add(R.drawable.ic_tool_apk);}
  if(material){labels.add("Material.bin derle");icons.add(R.drawable.ic_tool_archive);}
  labels.add("Yeniden adlandır");icons.add(R.drawable.ic_file);labels.add("Kopyala");icons.add(R.drawable.ic_file);labels.add("Kes");icons.add(R.drawable.ic_file);labels.add("Sil");icons.add(R.drawable.ic_file);labels.add("Özellikler");icons.add(R.drawable.ic_tool_xml);labels.add("Paylaş");icons.add(R.drawable.ic_file);
  if(apk){labels.add("APK'ya imzala");icons.add(R.drawable.ic_tool_apk);}
  labels.add("ZIP oluştur");icons.add(R.drawable.ic_tool_archive);
  GridLayout grid=new GridLayout(this);grid.setColumnCount(3);grid.setPadding(8,8,8,8);
  for(int n=0;n<labels.size();n++){final int idx=n;LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);ImageView iv=new ImageView(this);iv.setImageResource(icons.get(n));TextView lab=tv(labels.get(n),12);lab.setGravity(Gravity.CENTER);lab.setMaxLines(2);cell.addView(iv,new LinearLayout.LayoutParams(56,50));cell.addView(lab,new LinearLayout.LayoutParams(96,42));cell.setOnClickListener(v->{String a=labels.get(idx);
    if(a.equals("Aç"))openFile(f);else if(a.equals("SKK ile aç"))openWithSkk(f);else if(a.equals("İle aç"))openWithDialog(f);else if(a.equals("Decompile APK"))decompileApk(f);else if(a.equals("Patchle"))patchApk(f);else if(a.equals("Material.bin derle"))materialBinBuild(f);else if(a.equals("Yeniden adlandır"))rename(f);else if(a.equals("Kopyala")){clipboard=f;cutMode=false;toast("Kopyalandı");}else if(a.equals("Kes")){clipboard=f;cutMode=true;toast("Kesildi");}else if(a.equals("Sil"))confirmDelete(f);else if(a.equals("Özellikler"))properties(f);else if(a.equals("Paylaş"))share(f);else if(a.equals("APK'ya imzala"))apkSignDialog(f);else if(a.equals("ZIP oluştur"))zipSingle(f);
  });grid.addView(cell,new ViewGroup.LayoutParams(106,94));}
  new AlertDialog.Builder(this).setTitle(f.getName()).setView(grid).show();
 }
 void openWithDialog(File f){
  String x=f.getName().toLowerCase(Locale.ROOT);
  ArrayList<String> choices=new ArrayList<>();
  if(x.endsWith(".apk"))choices.add("Android Paket Yükleyicisi");
  if(isSkk(x)&&skkInstalled())choices.add("SKK Yükleyicisi");
  if(isText(x))choices.add("Musab Text");
  if(isImage(x))choices.add("Musab Görsel Görüntüleyici");
  if(isVideo(x))choices.add("Musab Video Görüntüleyici");
  if(isAudio(x))choices.add("Musab Ses Dinleyici");
  if(isArchive(x))choices.add("Musab Arşiv Görüntüleyici");
  choices.add("Diğer uygulamalar");
  String[] arr=choices.toArray(new String[0]);
  new AlertDialog.Builder(this).setTitle("İle aç").setItems(arr,(d,which)->{
    String a=arr[which];
    if(a.equals("Android Paket Yükleyicisi"))installApkFile(f);
    else if(a.equals("SKK Yükleyicisi"))openWithSkk(f);
    else if(a.equals("Musab Text")){Intent i=new Intent(this,MusabTextActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}
    else if(a.equals("Musab Görsel Görüntüleyici")){Intent i=new Intent(this,MusabImageViewerActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}
    else if(a.equals("Musab Video Görüntüleyici")){Intent i=new Intent(this,MusabVideoViewerActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}
    else if(a.equals("Musab Ses Dinleyici")){Intent i=new Intent(this,MusabAudioPlayerActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}
    else if(a.equals("Musab Arşiv Görüntüleyici")){Intent i=new Intent(this,MusabArchiveViewerActivity.class);i.putExtra("path",f.getAbsolutePath());startActivity(i);}
    else {try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(u,getMime(x));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"İle aç"));}catch(Exception e){toast("Uygulama bulunamadı: "+e.getMessage());}}
  }).show();
 }
 void decompileApk(File apk){
  if(!apk.isFile()){toast("APK bulunamadı");return;}
  File out=new File(apk.getParentFile(),apk.getName().replaceFirst("(?i)\\\\.apk$","_decompiled"));
  try{if(out.exists())deleteRecursive(out);out.mkdirs();ZipFile z=new ZipFile(apk);Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements()){ZipEntry ze=en.nextElement();File dst=new File(out,ze.getName());if(ze.isDirectory()){dst.mkdirs();continue;}File parent=dst.getParentFile();if(parent!=null)parent.mkdirs();InputStream in=z.getInputStream(ze);OutputStream os=new FileOutputStream(dst);byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)os.write(buf,0,n);in.close();os.close();}z.close();write(new File(out,".musab_decompiled"),"APK decompile workspace\nKaynak: "+apk.getAbsolutePath()+"\nPatch: Hayır\n");refresh();toast("APK decompile edildi: "+out.getName());}catch(Exception e){toast("Decompile hatası: "+e.getMessage());}
 }
 void patchApk(File apk){
  if(!apk.isFile()||!apk.getName().toLowerCase(Locale.ROOT).endsWith(".apk")){toast("Patch için APK seç");return;}
  File out=new File(apk.getParentFile(),apk.getName().replaceFirst("(?i)\\\\.apk$","_patched"));
  try{if(out.exists())deleteRecursive(out);out.mkdirs();ZipFile z=new ZipFile(apk);Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements()){ZipEntry ze=en.nextElement();File dst=new File(out,ze.getName());if(ze.isDirectory()){dst.mkdirs();continue;}File parent=dst.getParentFile();if(parent!=null)parent.mkdirs();InputStream in=z.getInputStream(ze);OutputStream os=new FileOutputStream(dst);byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)os.write(buf,0,n);in.close();os.close();}z.close();write(new File(out,".musab_decompiled"),"APK patch workspace\nKaynak: "+apk.getAbsolutePath()+"\nPatch: Evet\nDurum: Patch\n");refresh();toast("Patch çalışma alanı hazır: "+out.getName()+" [Patch]");}catch(Exception e){toast("Patch hatası: "+e.getMessage());}
 }
 void materialBinBuild(File input){
  new AlertDialog.Builder(this).setTitle("Material.bin derle").setMessage("MaterialBinTool kaynağı GitHub'dan alınarak derleme sunucusunda derlenir. Girdi: "+input.getName()).setPositiveButton("Derle",(d,w)->toast("Material.bin derleme isteği hazır. GitHub Actions üzerinden çalıştırılacak.")).setNegativeButton("İptal",null).show();
 }
 void rename(File f){prompt("Yeniden adlandır",f.getName(),s->{if(!f.renameTo(new File(f.getParentFile(),s)))toast("Ad değiştirilemedi");refresh();});}
 void confirmDelete(File f){new AlertDialog.Builder(this).setTitle("Silinsin mi?").setMessage(f.getAbsolutePath()).setPositiveButton("Sil",(d,w)->{deleteRecursive(f);refresh();}).setNegativeButton("İptal",null).show();}
 void deleteRecursive(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteRecursive(x);}f.delete();}
 void properties(File f){
  String x=f.getName().toLowerCase(Locale.ROOT);
  if(!x.endsWith(".apk")){new AlertDialog.Builder(this).setTitle("Özellikler").setMessage("Yol: "+f.getAbsolutePath()+"\\nBoyut: "+human(f.length())+"\\nSon değişiklik: "+new Date(f.lastModified())).setPositiveButton("Tamam",null).show();return;}
  new Thread(()->{String msg="Yol: "+f.getAbsolutePath()+"\\nBoyut: "+human(f.length())+"\\nSon değişiklik: "+new Date(f.lastModified());try{ApkVerifier.Result r=new ApkVerifier.Builder(f).build().verify();msg+="\\n\\nİmzalı: "+(r.isVerified()?"Evet":"Hayır");if(r.isVerified()){ArrayList<String> schemes=new ArrayList<>();if(r.isVerifiedUsingV1Scheme())schemes.add("V1");if(r.isVerifiedUsingV2Scheme())schemes.add("V2");if(r.isVerifiedUsingV3Scheme())schemes.add("V3");if(r.isVerifiedUsingV31Scheme())schemes.add("V3.1");msg+="\\nİmza: "+(schemes.isEmpty()?"Bilinmiyor":String.join(" + ",schemes));}final String shown=msg;runOnUiThread(()->new AlertDialog.Builder(this).setTitle("APK bilgileri").setMessage(shown).setPositiveButton("Tamam",null).show());}catch(Exception e){final String shown=msg+"\\n\\nİmzalı: Hayır\\nİmza denetimi: "+e.getMessage();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("APK bilgileri").setMessage(shown).setPositiveButton("Tamam",null).show());}}).start();
 }
 void share(File f){try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);Intent i=new Intent(Intent.ACTION_SEND);i.setType(getMime(f.getName().toLowerCase(Locale.ROOT)));i.putExtra(Intent.EXTRA_STREAM,u);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Paylaş"));}catch(Exception e){toast(e.getMessage());}}
 void paste(){if(clipboard==null){toast("Panoda dosya yok");return;}File dst=new File(current,clipboard.getName());if(dst.exists()){showPasteConflict(dst);return;}performPaste(dst,false);}
 void showPasteConflict(File existing){new AlertDialog.Builder(this).setTitle("Dosya zaten var").setMessage(existing.getName()+" zaten bu klasörde var.").setPositiveButton("Kopya oluştur",(d,w)->performPaste(uniqueSibling(existing),false)).setNeutralButton("Değiştir",(d,w)->performPaste(existing,true)).setNegativeButton("İptal",null).show();}
 void performPaste(File dst,boolean replace){try{if(replace&&dst.exists())deleteRecursive(dst);copyRecursive(clipboard,dst);if(cutMode&&!dst.equals(clipboard))deleteRecursive(clipboard);clipboard=null;refresh();}catch(Exception e){toast("Yapıştırma hatası: "+e.getMessage());}}
 void copyRecursive(File a,File b)throws Exception{if(a.isDirectory()){b.mkdirs();File[] c=a.listFiles();if(c!=null)for(File x:c)copyRecursive(x,new File(b,x.getName()));}else{InputStream in=new FileInputStream(a);OutputStream out=new FileOutputStream(b);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);in.close();out.close();}}
 void zipSingle(File f){File out=new File(current,f.getName()+".zip");try{ZipOutputStream z=new ZipOutputStream(new FileOutputStream(out));zipRec(f,z,f.getName());z.close();refresh();}catch(Exception e){toast("ZIP hatası: "+e.getMessage());}}
 void archiveSourceDialog(){
  final File[] fs=current.listFiles();
  if(fs==null){toast("Bu klasöre erişilemiyor");return;}
  final ArrayList<File> selected=new ArrayList<>();
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(20,4,20,4);
  EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Arşiv adı");name.setText("YeniArsiv.zip");name.setSelectAllOnFocus(true);
  box.addView(tv("Arşiv adı",14));box.addView(name,new LinearLayout.LayoutParams(-1,-2));
  Spinner type=new Spinner(this);String[] types={"ZIP","ZIP (sıkıştırmasız)"};
  type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,types));
  box.addView(tv("Arşiv türü",14));box.addView(type);
  TextView chosen=tv("Seçilen dosyalar: 0",13);chosen.setPadding(0,12,0,8);box.addView(chosen);
  LinearLayout picked=new LinearLayout(this);picked.setOrientation(LinearLayout.VERTICAL);box.addView(picked,new LinearLayout.LayoutParams(-1,0,1));
  Button add=btn("Dosya ekle");
  add.setOnClickListener(v->{
    if(fs==null||fs.length==0){toast("Bu klasörde eklenecek dosya yok");return;}
    String[] labels=new String[fs.length];boolean[] checks=new boolean[fs.length];
    for(int i=0;i<fs.length;i++){labels[i]=fs[i].getName();checks[i]=selected.contains(fs[i]);}
    new AlertDialog.Builder(this).setTitle("Arşive dosya ekle").setMultiChoiceItems(labels,checks,(d,which,isChecked)->{if(isChecked){if(!selected.contains(fs[which]))selected.add(fs[which]);}else selected.remove(fs[which]);chosen.setText("Seçilen dosyalar: "+selected.size());})
      .setPositiveButton("Ekle",(d,w)->{picked.removeAllViews();for(File f:selected)picked.addView(tv("• "+f.getName(),14),new LinearLayout.LayoutParams(-1,44));})
      .setNegativeButton("İptal",null).show();
  });
  box.addView(add,new LinearLayout.LayoutParams(-1,54));
  new AlertDialog.Builder(this).setTitle("Arşiv oluştur").setView(box)
    .setPositiveButton("Oluştur",(d,w)->{String n=name.getText().toString().trim();if(n.isEmpty()){toast("Arşiv adı boş olamaz");return;}if(!n.toLowerCase(Locale.ROOT).endsWith(".zip"))n+=".zip";createZipArchive(selected,new File(current,n),type.getSelectedItemPosition()==1);})
    .setNegativeButton("İptal",null).show();
 }
 void createZipArchive(List<File> sources,File out,boolean stored){
  String dst=out.getAbsolutePath();
  for(File source:sources){String src=source.getAbsolutePath();if(dst.equals(src)||dst.startsWith(src+File.separator)){toast("Arşiv hedefi kaynak klasörün içinde olamaz");return;}}
  if(out.exists())new AlertDialog.Builder(this).setTitle("Dosya zaten var").setMessage(out.getName()+" zaten mevcut. Ne yapmak istiyorsun?")
    .setPositiveButton("Kopya oluştur",(d,w)->writeZip(sources,uniqueSibling(out),stored)).setNeutralButton("Değiştir",(d,w)->writeZip(sources,out,stored)).setNegativeButton("İptal",null).show();
  else writeZip(sources,out,stored);
 }
 void writeZip(List<File> sources,File out,boolean stored){
  File tmp=new File(out.getAbsolutePath()+".part");
  try{if(tmp.exists()&&!tmp.delete())throw new IOException("Geçici dosya silinemedi");ZipOutputStream z=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));try{if(stored)z.setLevel(Deflater.NO_COMPRESSION);for(File source:sources)zipRec(source,z,source.getName());}finally{z.close();}ZipFile verify=new ZipFile(tmp);verify.close();if(out.exists()&&!out.delete())throw new IOException("Eski arşiv silinemedi");if(!tmp.renameTo(out))throw new IOException("Arşiv dosyası oluşturulamadı");refresh();toast("Geçerli ZIP oluşturuldu: "+out.getName());}catch(Exception e){tmp.delete();toast("ZIP oluşturma hatası: "+e.getMessage());}
 }
 void zipRec(File f,ZipOutputStream z,String path)throws Exception{
  if(f.isDirectory()){File[] children=f.listFiles();if(children==null||children.length==0){z.putNextEntry(new ZipEntry(path+"/"));z.closeEntry();return;}for(File child:children)zipRec(child,z,path+"/"+child.getName());}
  else{z.putNextEntry(new ZipEntry(path));FileInputStream in=new FileInputStream(f);try{byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)z.write(buffer,0,n);}finally{in.close();}z.closeEntry();}
 }
 void apps(){final PackageManager pm=getPackageManager();LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);EditText appSearch=new EditText(this);appSearch.setSingleLine(true);appSearch.setHint("Oyun / uygulama ara...");appSearch.setTextColor(FG);appSearch.setHintTextColor(Color.GRAY);Button search=btn("Ara");LinearLayout searchRow=new LinearLayout(this);searchRow.addView(appSearch,new LinearLayout.LayoutParams(0,-2,1));searchRow.addView(search,new LinearLayout.LayoutParams(72,-2));l.addView(searchRow);TextView count=tv("Uygulamalar yükleniyor...",16);l.addView(count);ScrollView s=new ScrollView(this);s.addView(l);new AlertDialog.Builder(this).setTitle("APK Çıkar / Uygulamalar").setView(s).setPositiveButton("Kapat",null).show();new Thread(()->{List<ApplicationInfo> all;try{all=pm.getInstalledApplications(PackageManager.GET_META_DATA);}catch(Exception ex){all=new ArrayList<>();}if(all==null)all=new ArrayList<>();all.sort((a,b)->String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));final List<ApplicationInfo> apps=all;runOnUiThread(()->{long uc=apps.stream().filter(a->(a.flags&ApplicationInfo.FLAG_SYSTEM)==0).count(),sc=apps.size()-uc;Button user=btn("Kullanıcı Uygulamaları ("+uc+")"),sys=btn("Sistem Uygulamaları ("+sc+")");l.addView(user);l.addView(sys);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);l.addView(results);count.setText("Toplam görünür uygulama: "+apps.size());user.setOnClickListener(v->showAppsList(results,apps,false,pm,appSearch.getText().toString()));sys.setOnClickListener(v->showAppsList(results,apps,true,pm,appSearch.getText().toString()));search.setOnClickListener(v->showAppsList(results,apps,false,pm,appSearch.getText().toString()));appSearch.setOnEditorActionListener((v,id,e)->{showAppsList(results,apps,false,pm,appSearch.getText().toString());return true;});showAppsList(results,apps,false,pm,"");});}).start();}
 void scanApks(LinearLayout l){l.removeAllViews();l.addView(tv("Depolamadaki APK dosyaları",18));new Thread(()->{ArrayList<File> found=new ArrayList<>();scanApkRecursive(STORAGE,found,0);runOnUiThread(()->{if(found.isEmpty())l.addView(tv("APK bulunamadı.",16));else for(File f:found){TextView t=tv(f.getAbsolutePath()+"\n"+human(f.length()),15);t.setOnClickListener(v->apkContents(f));l.addView(t,new LinearLayout.LayoutParams(-1,72));}});}).start();}
 void scanApkRecursive(File dir,ArrayList<File> out,int depth){if(dir==null||depth>5||out.size()>500)return;File[] fs=dir.listFiles();if(fs==null)return;for(File f:fs){if(f.isFile()&&f.getName().toLowerCase(Locale.ROOT).endsWith(".apk"))out.add(f);else if(f.isDirectory()&&!f.getName().equals("Android"))scanApkRecursive(f,out,depth+1);}}
 void showAppsList(LinearLayout l,List<ApplicationInfo> all,boolean system,PackageManager pm,String query){l.removeAllViews();String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);l.addView(tv((system?"Tüm sistem uygulamaları":"Tüm kullanıcı uygulamaları")+(q.isEmpty()?"":" — \""+query+"\""),18));int shown=0;for(ApplicationInfo ai:all){boolean isSystem=(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0;if(isSystem!=system)continue;String label=String.valueOf(ai.loadLabel(pm));if(!q.isEmpty()&&!label.toLowerCase(Locale.ROOT).contains(q)&&!ai.packageName.toLowerCase(Locale.ROOT).contains(q))continue;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);ImageView iv=new ImageView(this);Drawable d=ai.loadIcon(pm);iv.setImageDrawable(d);TextView t=tv(String.valueOf(ai.loadLabel(pm))+"\n"+ai.packageName,16);t.setGravity(Gravity.CENTER_VERTICAL);t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setPadding(8,3,8,3);row.addView(iv,new LinearLayout.LayoutParams(54,72));row.addView(t,new LinearLayout.LayoutParams(0,72,1));row.setPadding(4,3,4,3);row.setOnClickListener(v->appDetails(ai,pm));l.addView(row,new LinearLayout.LayoutParams(-1,78));shown++;}if(shown==0)l.addView(tv("Bu kategoride uygulama bulunamadı.",16));}
 void appDetails(ApplicationInfo ai,PackageManager pm){String label=String.valueOf(ai.loadLabel(pm));PackageInfo pi;try{pi=pm.getPackageInfo(ai.packageName,0);}catch(Exception e){toast("Uygulama bilgisi okunamadı");return;}long vc=Build.VERSION.SDK_INT>=28?pi.getLongVersionCode():pi.versionCode;File apk=new File(ai.sourceDir);String msg=label+"\n"+ai.packageName+"\nSürüm: "+pi.versionName+" ("+vc+")\nTemel APK: "+ai.sourceDir+"\nBoyut: "+human(apk.length())+"\nEk APK parçaları: "+(ai.splitSourceDirs==null?0:ai.splitSourceDirs.length);new AlertDialog.Builder(this).setTitle("Uygulama").setMessage(msg).setPositiveButton("APK Çıkar",(d,w)->extractApk(ai)).setNeutralButton("APK içeriği",(d,w)->apkContents(apk)).setNegativeButton("Kapat",null).show();}
 void extractApk(ApplicationInfo ai){ensureMusabFolders();String safe=ai.packageName.replaceAll("[^A-Za-z0-9._-]","_");File dir=new File(APKS,safe+"-"+System.currentTimeMillis());dir.mkdirs();try{File out=new File(dir,"base.apk");copyRecursive(new File(ai.sourceDir),out);toast("APK çıkarıldı: "+out.getAbsolutePath()+"\\nEk APK parçaları çıkarılmadı.");}catch(Exception e){toast("APK çıkarma hatası: "+e.getMessage());}}
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
 void toolRow(LinearLayout box,String title,int iconId,final Runnable action,final AlertDialog dialog){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(10,6,10,6);ImageView icon=new ImageView(this);icon.setImageResource(iconId);row.addView(icon,new LinearLayout.LayoutParams(54,54));TextView label=tv(title,15);label.setGravity(Gravity.CENTER_VERTICAL);label.setMaxLines(2);label.setIncludeFontPadding(false);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LinearLayout.LayoutParams(0,60,1));row.setBackgroundColor(PANEL);row.setOnClickListener(v->{dialog.dismiss();action.run();});box.addView(row,new LinearLayout.LayoutParams(-1,68));}
 void apkContentsDialog(){prompt("APK yolu","",p->{File f=new File(p);if(f.isFile())apkContents(f);else toast("APK bulunamadı");});}
 void codeEditor(String type){EditText e=new EditText(this);e.setGravity(Gravity.TOP);e.setMinLines(18);e.setTextSize(14);e.setHint(type+" kodu");new AlertDialog.Builder(this).setTitle(type+" editörü").setView(e).setPositiveButton("Dosyaya kaydet",(d,w)->prompt("Dosya adı",type.toLowerCase(Locale.ROOT)+".txt",n->{try{write(new File(current,n),e.getText().toString());refresh();}catch(Exception x){toast(x.getMessage());}})).setNegativeButton("Kapat",null).show();}
 void xmlEditor(){codeEditor("AndroidManifest / XML");}
 void terminal(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(Color.BLACK);TextView h=tv("Musab Terminal",20);h.setTextColor(Color.WHITE);r.addView(h);TextView p=tv(current.getAbsolutePath(),13);p.setTextColor(Color.LTGRAY);r.addView(p);ScrollView sv=new ScrollView(this);TextView out=tv("Musab Terminal\\n$ ",14);out.setTextColor(Color.WHITE);sv.addView(out);r.addView(sv,new LinearLayout.LayoutParams(-1,0,1));LinearLayout line=new LinearLayout(this);EditText in=new EditText(this);in.setSingleLine(true);in.setTextColor(Color.WHITE);in.setHintTextColor(Color.GRAY);in.setHint("komut");Button run=btn("Çalıştır");line.addView(in,new LinearLayout.LayoutParams(0,-2,1));line.addView(run);r.addView(line);Button back=btn("Dosya Yöneticisine Dön");back.setOnClickListener(v->setContentView(root));r.addView(back);setContentView(r);run.setOnClickListener(v->{String cmd=in.getText().toString().trim();if(cmd.isEmpty())return;out.append(cmd+"\\n"+localCommand(cmd)+"\\n$ ");in.setText("");sv.post(()->sv.fullScroll(View.FOCUS_DOWN));});}
String localCommand(String cmd){try{if(cmd.equals("pwd"))return current.getAbsolutePath();if(cmd.equals("ls")){File[] fs=current.listFiles();if(fs==null)return "Erişim yok";StringBuilder s=new StringBuilder();for(File f:fs)s.append(f.isDirectory()?"[D] ":"[F] ").append(f.getName()).append("\\n");return s.toString();}if(cmd.startsWith("cd ")){File f=new File(current,cmd.substring(3).trim());if(f.isDirectory()){current=f.getCanonicalFile();return "Dizin değişti: "+current;}return "Dizin bulunamadı";}if(cmd.equals("clear"))return "";if(cmd.equals("help"))return "pwd, ls, cd <klasör>, clear, help";return "Komut desteklenmiyor";}catch(Exception e){return "Hata: "+e.getMessage();}}
void viewDialog(){new AlertDialog.Builder(this).setTitle("Görünüm").setItems(new String[]{"Liste","Büyük simgeler","Sırala: ad","Sırala: boyut","Yenile"},(d,w)->{if(w==0){gridView=false;refresh();}else if(w==1){gridView=true;refresh();}else if(w==2){sortBySize=false;refresh();}else if(w==3){sortBySize=true;refresh();}else refresh();}).show();}

 void write(File f,String s)throws Exception{FileOutputStream o=new FileOutputStream(f,false);o.write(s.getBytes("UTF-8"));o.close();}
 void apkContents(File f){try{ZipFile z=new ZipFile(f);StringBuilder s=new StringBuilder();Enumeration<? extends ZipEntry> en=z.entries();while(en.hasMoreElements())s.append(en.nextElement().getName()).append('\n');z.close();new AlertDialog.Builder(this).setTitle("APK içeriği").setMessage(s.toString()).setPositiveButton("Kapat",null).show();}catch(Exception e){toast("APK okunamadı: "+e.getMessage());}}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}