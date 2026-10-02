package com.musab.dosyayoneticisi;
import android.app.*;import android.os.*;import android.graphics.Color;import android.net.Uri;import android.media.MediaPlayer;import android.media.AudioAttributes;import android.view.*;import android.widget.*;import androidx.core.content.FileProvider;import java.io.*;
public class MusabAudioPlayerActivity extends Activity{
 MediaPlayer mp;
 public void onCreate(Bundle b){super.onCreate(b);File f=new File(getIntent().getStringExtra("path"));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setBackgroundColor(Color.rgb(12,12,14));TextView t=new TextView(this);t.setText("Musab Ses Dinleyici\n"+f.getName());t.setTextColor(Color.WHITE);t.setTextSize(20);t.setGravity(Gravity.CENTER);t.setPadding(20,30,20,30);r.addView(t);Button play=new Button(this);play.setText("Oynat / Duraklat");play.setOnClickListener(v->{if(mp!=null){if(mp.isPlaying())mp.pause();else mp.start();}});r.addView(play);
 try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);mp=MediaPlayer.create(this,u);if(mp!=null&&Build.VERSION.SDK_INT>=21)mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());}catch(Exception e){t.setText("Ses açılamadı: "+e.getMessage());}
 setContentView(r);}
 protected void onDestroy(){if(mp!=null){mp.release();mp=null;}super.onDestroy();}
}