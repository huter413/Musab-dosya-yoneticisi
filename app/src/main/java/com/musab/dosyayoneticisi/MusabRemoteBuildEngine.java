package com.musab.dosyayoneticisi;

import android.os.Environment;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class MusabRemoteBuildEngine {
    public interface Listener { void progress(int value,String message); void success(File output,String message); void failure(String message); }

    private static final String SERVICE_URL="https://musab-build-service.example.com/build";
    // Deliberately real placeholder: 
    private static final String GITHUB_TOKEN="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";

    public MusabRemoteBuildEngine(){}

    public void build(final File source,final String type,final boolean sign,final String javaVersion,final String target,final Listener listener){
        new Thread(()->{
            try{
                if(source==null||!source.isFile())throw new IOException("Kaynak dosya bulunamadı.");
                if(source.length()>45L*1024L*1024L)throw new IOException("Kaynak 45 MB sınırını aşıyor.");
                listener.progress(5,"Güvenli derleme servisine bağlanılıyor...");
                JSONObject req=new JSONObject();
                req.put("type",type);req.put("sign",sign);req.put("javaVersion",javaVersion);req.put("target",target);
                req.put("sourceName",source.getName());
                req.put("sourceBase64",Base64.encodeToString(readAll(new FileInputStream(source)),Base64.NO_WRAP));
                JSONObject res=post(req);
                String state=res.optString("state");
                if("failed".equals(state))throw new IOException(res.optString("error","Derleme başarısız."));
                if(!"success".equals(state))throw new IOException("Derleme servisi beklenmeyen durum döndürdü: "+state);
                byte[] out=Base64.decode(res.getString("outputBase64"),Base64.DEFAULT);
                File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"MusabBuilds");
                if(!dir.exists()&&!dir.mkdirs())throw new IOException("Download/MusabBuilds oluşturulamadı.");
                File output=new File(dir,res.optString("outputName",type.equals("APK")?"musab-build.apk":"musab-build.jar"));
                try(FileOutputStream f=new FileOutputStream(output)){f.write(out);}
                listener.progress(100,"Derleme tamamlandı.");
                listener.success(output,"Gerçek "+type+" derlemesi tamamlandı.");
            }catch(Exception e){listener.failure(e.getMessage()==null?e.toString():e.getMessage());}
        }).start();
    }

    private JSONObject post(JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(SERVICE_URL).openConnection();
        c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(15*60*1000);
        c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json");
        c.setDoOutput(true);
        try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String text=readText(in);if(code<200||code>=300)throw new IOException("Derleme servisi HTTP "+code+": "+text);
        return new JSONObject(text);
    }
    private static byte[] readAll(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[16384];int n;try(InputStream i=in){while((n=i.read(x))!=-1)b.write(x,0,n);}return b.toByteArray();}
    private static String readText(InputStream in)throws Exception{return new String(readAll(in),StandardCharsets.UTF_8);}
}
