package com.musab.dosyayoneticisi;

import android.os.Environment;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public final class MusabRemoteBuildEngine {
    public interface Listener {
        void progress(int value, String message);
        void success(File output, String message);
        void failure(String message);
    }

    private static final String API="https://api.github.com";
    private static final String REPO="huter413/Musab-dosya-yoneticisi";
    private final String token;

    public MusabRemoteBuildEngine(String token){this.token=token==null?"":token.trim();}

    public void build(final File source, final String type, final boolean sign,
                      final String javaVersion, final String target, final Listener listener){
        new Thread(() -> {
            try {
                if(token.isEmpty()) throw new IOException("GitHub token ayarlanmadı. Derleme motorunu kullanmak için token ekleyin.");
                if(source==null || !source.isFile()) throw new IOException("Kaynak dosya bulunamadı.");
                if(source.length()>45L*1024L*1024L) throw new IOException("Kaynak 45 MB sınırını aşıyor. Daha küçük bir ZIP/JAR kullanın.");
                String branch="musab-build-"+System.currentTimeMillis();
                listener.progress(5,"Derleme dalı oluşturuluyor...");
                JSONObject ref=get("/repos/"+REPO+"/git/ref/heads/main");
                String mainSha=ref.getJSONObject("object").getString("sha");
                post("/repos/"+REPO+"/git/refs",new JSONObject().put("ref","refs/heads/"+branch).put("sha",mainSha));
                String dir="build-input/"+branch;
                JSONObject cfg=new JSONObject();
                cfg.put("type",type);
                cfg.put("sign",sign);
                cfg.put("javaVersion",javaVersion);
                cfg.put("target",target);
                cfg.put("sourceName",source.getName());
                listener.progress(15,"Derleme ayarları gönderiliyor...");
                putFile(dir+"/config.json",Base64.encodeToString(cfg.toString(2).getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP),branch,"Musab build config");
                byte[] bytes=readAll(source);
                String ext=source.getName().toLowerCase(Locale.ROOT).endsWith(".jar")?".jar":".zip";
                listener.progress(30,"Proje GitHub derleme motoruna yükleniyor...");
                putFile(dir+"/source"+ext,Base64.encodeToString(bytes,Base64.NO_WRAP),branch,"Musab build source");
                listener.progress(40,"Gradle/JDK/Android SDK derlemesi başlatıldı...");
                JSONObject run=waitForRun(branch,listener);
                long runId=run.getLong("id");
                String conclusion=run.optString("conclusion","");
                if(!"success".equals(conclusion)){
                    String log=downloadFailureLog(runId);
                    throw new IOException("Derleme başarısız. "+extractError(log));
                }
                listener.progress(92,"Derleme çıktısı indiriliyor...");
                File output=downloadArtifact(runId,type);
                deleteRef(branch);
                listener.success(output,"Gerçek "+type+" derlemesi tamamlandı.");
            } catch(Exception e){
                listener.failure(e.getMessage()==null?e.toString():e.getMessage());
            }
        }).start();
    }

    private JSONObject waitForRun(String branch,Listener l)throws Exception{
        long deadline=System.currentTimeMillis()+12*60*1000L;
        while(System.currentTimeMillis()<deadline){
            JSONObject root=get("/repos/"+REPO+"/actions/runs?branch="+URLEncoder.encode(branch,"UTF-8")+"&event=push&per_page=10");
            JSONArray a=root.optJSONArray("workflow_runs");
            if(a!=null){
                for(int i=0;i<a.length();i++){
                    JSONObject r=a.getJSONObject(i);
                    if(branch.equals(r.optString("head_branch"))){
                        String s=r.optString("status");
                        if("completed".equals(s)) return r;
                        l.progress(40+Math.min(45,(int)((System.currentTimeMillis()%60000)/1400)),"Gerçek derleme çalışıyor: "+s);
                    }
                }
            }
            Thread.sleep(5000);
        }
        throw new IOException("GitHub Actions derlemesi 12 dakika içinde tamamlanmadı.");
    }

    private File downloadArtifact(long runId,String type)throws Exception{
        JSONObject root=get("/repos/"+REPO+"/actions/runs/"+runId+"/artifacts");
        JSONArray a=root.optJSONArray("artifacts");
        if(a==null)throw new IOException("Derleme çıktısı bulunamadı.");
        String url=null;
        for(int i=0;i<a.length();i++){
            JSONObject x=a.getJSONObject(i);
            if("musab-build-output".equals(x.optString("name"))&&!x.optBoolean("expired",false)){
                url=x.optString("archive_download_url");break;
            }
        }
        if(url==null)throw new IOException("Derleme başarılı fakat çıktı artefaktı yok.");
        byte[] zip=download(url);
        File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"MusabBuilds");
        if(!dir.exists()&&!dir.mkdirs())throw new IOException("Download/MusabBuilds oluşturulamadı.");
        File found=null;
        ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry e;
        while((e=zin.getNextEntry())!=null){
            if(e.isDirectory())continue;
            String name=new File(e.getName()).getName();
            if(name.equals("build-info.txt")||name.endsWith(".jks"))continue;
            File out=new File(dir,name);
            try(FileOutputStream fos=new FileOutputStream(out)){
                byte[] b=new byte[8192];int n;
                while((n=zin.read(b))!=-1)fos.write(b,0,n);
            }
            if(("APK".equals(type)&&name.endsWith(".apk"))||("JAR".equals(type)&&name.endsWith(".jar")))found=out;
        }
        zin.close();
        if(found==null)throw new IOException("Çıktı arşivinden "+type+" dosyası çıkarılamadı.");
        return found;
    }

    private String downloadFailureLog(long runId){
        try{
            byte[] b=download(API+"/repos/"+REPO+"/actions/runs/"+runId+"/logs");
            ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(b));
            StringBuilder s=new StringBuilder();ZipEntry e;
            while((e=z.getNextEntry())!=null){
                if(!e.getName().endsWith(".txt"))continue;
                BufferedReader r=new BufferedReader(new InputStreamReader(z,StandardCharsets.UTF_8));
                String line;while((line=r.readLine())!=null){
                    if(line.contains("error")||line.contains("Error")||line.contains("FAILURE")||line.contains("Could not")||line.contains("failed")||line.contains("not found"))s.append(line).append('\n');
                }
            }
            z.close();
            return s.toString();
        }catch(Exception ignored){return "";}
    }

    private String extractError(String log){
        if(log==null||log.trim().isEmpty())return "Actions günlükleri alınamadı. GitHub Actions run günlüklerinde ilk ERROR/FAILURE satırını kontrol edin.";
        String[] lines=log.split("\\n");
        StringBuilder s=new StringBuilder();
        for(String x:lines){if(s.length()>1800)break;s.append(x).append('\n');}
        return s.toString().trim();
    }

    private JSONObject get(String path)throws Exception{return request("GET",path,null);}
    private JSONObject post(String path,JSONObject body)throws Exception{return request("POST",path,body.toString());}
    private JSONObject putFile(String path,String content,String branch,String message)throws Exception{
        JSONObject body=new JSONObject().put("message",message).put("content",content).put("branch",branch);
        return request("PUT","/repos/"+REPO+"/contents/"+path,body.toString());
    }
    private JSONObject request(String method,String path,String body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(path.startsWith("http")?path:API+path).openConnection();
        c.setRequestMethod(method);c.setConnectTimeout(20000);c.setReadTimeout(30000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("X-GitHub-Api-Version","2026-03-10");
        c.setRequestProperty("Content-Type","application/json");
        if(body!=null){c.setDoOutput(true);try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String text=readText(in);
        if(code<200||code>=300)throw new IOException("GitHub API "+code+": "+text);
        return new JSONObject(text);
    }
    private byte[] download(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setInstanceFollowRedirects(true);c.setConnectTimeout(20000);c.setReadTimeout(120000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+token);
        c.setRequestProperty("X-GitHub-Api-Version","2026-03-10");
        int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("Çıktı indirme HTTP "+code);
        return readAll(c.getInputStream());
    }
    private void deleteRef(String branch){
        try{
            HttpURLConnection c=(HttpURLConnection)new URL(API+"/repos/"+REPO+"/git/refs/heads/"+branch).openConnection();
            c.setRequestMethod("DELETE");c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/vnd.github+json");c.getResponseCode();
        }catch(Exception ignored){}
    }
    private static byte[] readAll(InputStream in)throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[16384];int n;
        try(InputStream i=in){while((n=i.read(x))!=-1)b.write(x,0,n);}return b.toByteArray();
    }
    private static String readText(InputStream in)throws Exception{return new String(readAll(in),StandardCharsets.UTF_8);}
}
