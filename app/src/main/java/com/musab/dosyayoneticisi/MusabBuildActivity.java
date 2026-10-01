package com.musab.dosyayoneticisi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MusabBuildActivity extends Activity {
    File source;
    TextView status;
    ProgressBar progress;
    Button start;
    CheckBox sign;
    EditText githubToken;
    Spinner javaVersion,target;
    boolean androidProject,jarProject;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        source=new File(getIntent().getStringExtra("path"));
        buildUi();
        inspect();
    }

    TextView tv(String s,int sp){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(Color.WHITE);
        t.setPadding(14,10,14,10);return t;
    }
    Button btn(String s){Button b=new Button(this);b.setText(s);return b;}

    void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,12));
        TextView title=tv("Musab Derleme Merkezi",23);title.setGravity(Gravity.CENTER);
        root.addView(title);
        status=tv("Proje inceleniyor...",15);
        root.addView(status);
        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.addView(tv("Kaynak: "+source.getName(),16));

        box.addView(tv("Gerçek motor: GitHub Actions + Gradle + JDK + Android SDK",13));
        githubToken=new EditText(this);
        githubToken.setSingleLine(true);
        githubToken.setHint("GitHub token");
        githubToken.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        githubToken.setText(getSharedPreferences("musab_build",MODE_PRIVATE).getString("github_token",""));
        box.addView(githubToken);
        Button tokenInfo=btn("Tokenu kaydet / değiştir");
        tokenInfo.setOnClickListener(v->{String t=githubToken.getText().toString().trim();if(t.isEmpty()){toast("Token boş.");return;}getSharedPreferences("musab_build",MODE_PRIVATE).edit().putString("github_token",t).apply();toast("GitHub token kaydedildi.");});
        box.addView(tokenInfo);


        sign=new CheckBox(this);sign.setText("Derlerken imzala");sign.setTextColor(Color.WHITE);
        box.addView(sign);

        box.addView(tv("Minecraft Java sürümü",15));
        javaVersion=new Spinner(this);
        String[] versions={"Seç","1.21.x","1.20.x","1.19.x","1.18.x","1.17.x","1.16.x","1.15.x","1.14.x","1.13.x","1.12.x ve öncesi"};
        javaVersion.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,versions));
        box.addView(javaVersion);

        box.addView(tv("Hedef",15));
        target=new Spinner(this);
        String[] targets={"Seç","Mobil / PojavLauncher vb. uyumlu","Bilgisayar uyumlu"};
        target.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,targets));
        box.addView(target);

        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgress(0);progress.setVisibility(View.GONE);
        box.addView(progress,new LinearLayout.LayoutParams(-1,12));

        start=btn("Derlemeye başla");
        start.setEnabled(false);
        box.addView(start);

        Button apk=btn("APK derle");
        apk.setOnClickListener(v->compileAndroid());
        box.addView(apk);

        Button jar=btn("JAR derle");
        jar.setOnClickListener(v->compileJar());
        box.addView(jar);

        Button xml=btn("XML'ye dönüştür");
        xml.setOnClickListener(v->binaryXml());
        box.addView(xml);

        Button patch=btn("Patchle");
        patch.setOnClickListener(v->patchInfo());
        box.addView(patch);

        Button decompile=btn("Patchle decompile et");
        decompile.setOnClickListener(v->decompileInfo());
        box.addView(decompile);

        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        AdapterView.OnItemSelectedListener listener=new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){updateStart();}
        };
        javaVersion.setOnItemSelectedListener(listener);target.setOnItemSelectedListener(listener);
        start.setOnClickListener(v->{if(androidProject)compileAndroid();else compileJar();});
    }

    void inspect(){
        new Thread(()->{
            String error=null;
            boolean a=false,j=false;
            if(source==null||!source.isFile()) error="Kaynak proje bulunamadı.";
            else {
                String n=source.getName().toLowerCase(Locale.ROOT);
                if(n.endsWith(".jar")) j=true;
                else if(n.endsWith(".zip")){
                    try{
                        ZipFile z=new ZipFile(source);
                        Enumeration<? extends ZipEntry> e=z.entries();
                        boolean manifest=false,gradle=false,java=false,classFile=false;
                        while(e.hasMoreElements()){
                            String x=e.nextElement().getName();
                            if(x.equals("AndroidManifest.xml")||x.endsWith("/AndroidManifest.xml"))manifest=true;
                            if(x.equals("settings.gradle")||x.equals("settings.gradle.kts")||x.endsWith("/build.gradle")||x.endsWith("/build.gradle.kts"))gradle=true;
                            if(x.endsWith(".java")||x.endsWith(".kt"))java=true;
                            if(x.endsWith(".class"))classFile=true;
                        }
                        z.close();
                        a=manifest&&(gradle||java);
                        j=classFile||java;
                        if(!a&&!j)error="ZIP proje olarak tanınmadı: AndroidManifest.xml + Gradle/proje kaynakları bulunamadı.";
                    }catch(Exception ex){error="ZIP okunamadı: "+ex.getMessage();}
                } else error="Yalnızca .zip ve .jar proje kaynakları destekleniyor.";
            }
            androidProject=a;jarProject=j;
            final String msg=error;
            runOnUiThread(()->{
                status.setText(msg==null?(androidOk?"Android ZIP projesi hazır.":"JAR projesi hazır."):"Derleme öncesi hata: "+msg);
                updateStart();
            });
        }).start();
    }

    void updateStart(){
        boolean ok=androidProject || (jarProject&&javaVersion!=null&&javaVersion.getSelectedItemPosition()>0&&target!=null&&target.getSelectedItemPosition()>0);
        start.setEnabled(ok);
        start.setText(ok?"Derlemeye başla":"Derlemeye başla (seçimleri tamamla)");
    }

    void askSign(Runnable yes){
        new AlertDialog.Builder(this).setTitle("İmzalama")
            .setMessage("Bu çıktı derlenirken imzalansın mı?")
            .setPositiveButton("Evet", (d,w)->yes.run())
            .setNegativeButton("Hayır",(d,w)->yes.run()).show();
    }

    void compileAndroid(){
        askSign(()->runBuild("APK",androidProject));
    }
    void compileJar(){
        if(!jarProject){fail("Gerçek JAR projesi algılanmadı.");return;}
        if(javaVersion.getSelectedItemPosition()==0||target.getSelectedItemPosition()==0){fail("Minecraft Java sürümü ve hedef seçilmeden JAR derlenemez.");return;}
        askSign(()->runBuild("JAR",true));
    }

    void runBuild(String type,boolean valid){
        if(!valid){fail(type+" projesi doğrulaması başarısız.");return;}
        String token=githubToken==null?"":githubToken.getText().toString().trim();
        if(token.isEmpty()){
            new AlertDialog.Builder(this).setTitle("GitHub token gerekli")
                .setMessage("Gerçek Gradle/JDK/Android SDK motoru GitHub Actions üzerinde çalışır. Token girip kaydedin.")
                .setPositiveButton("Tamam",null).show();
            return;
        }
        getSharedPreferences("musab_build",MODE_PRIVATE).edit().putString("github_token",token).apply();
        status.setText(type+" gerçek derleme motoruna gönderiliyor...");
        start.setEnabled(false);
        progress.setVisibility(View.VISIBLE);progress.setProgress(1);
        final String selectedJava=javaVersion==null?"Seç":String.valueOf(javaVersion.getSelectedItem());
        final String selectedTarget=target==null?"Seç":String.valueOf(target.getSelectedItem());
        MusabRemoteBuildEngine engine=new MusabRemoteBuildEngine(token);
        engine.build(source,type,sign.isChecked(),selectedJava,selectedTarget,new MusabRemoteBuildEngine.Listener(){
            public void progress(int value,String message){runOnUiThread(()->{progress.setProgress(Math.max(0,Math.min(100,value)));status.setText(message);});}
            public void success(File output,String message){runOnUiThread(()->{
                progress.setProgress(100);start.setEnabled(true);
                status.setText(message+"\nÇıktı: "+output.getAbsolutePath());
                new AlertDialog.Builder(MusabBuildActivity.this).setTitle("Derleme başarılı")
                    .setMessage(message+"\n\n"+output.getAbsolutePath()).setPositiveButton("Tamam",null).show();
            });}
            public void failure(String message){runOnUiThread(()->fail(message));}
        });
    }

    void binaryXml(){
        if(!source.getName().toLowerCase(Locale.ROOT).endsWith(".xml")){fail("Kaynak .xml değil.");return;}
        fail("Binary AXML dönüştürme motoru bu sürümde bağlı değil; düz XML dosyası olduğu için güvenli biçimde dönüştürülmedi.");
    }
    void patchInfo(){fail("Patchleme için hedef APK/JAR ve patch tanımı gerekiyor; rastgele dosyayı değiştirmeden işlem durduruldu.");}
    void decompileInfo(){fail("Decompile için APK/JAR hedefi seçilmesi gerekiyor; işlem başlatılmadı.");}
    void fail(String s){
        progress.setVisibility(View.GONE);start.setEnabled(androidProject||jarProject);
        status.setText("Derleme başarısız\n"+s);
        new AlertDialog.Builder(this).setTitle("İşlem başarısız").setMessage(s).setPositiveButton("Tamam",null).show();
    }
}