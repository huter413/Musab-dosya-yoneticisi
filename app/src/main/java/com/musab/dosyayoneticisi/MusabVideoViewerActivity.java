package com.musab.dosyayoneticisi;
import android.app.*;import android.os.*;import android.graphics.Color;import android.net.Uri;import android.media.AudioManager;import android.view.*;import android.widget.*;import androidx.core.content.FileProvider;import java.io.*;
public class MusabVideoViewerActivity extends Activity{
 VideoView video;
 public void onCreate(Bundle b){super.onCreate(b);File f=new File(getIntent().getStringExtra("path"));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(Color.BLACK);TextView t=new TextView(this);t.setText("Musab Video Görüntüleyici  •  "+f.getName());t.setTextColor(Color.WHITE);t.setTextSize(18);t.setPadding(16,16,16,16);r.addView(t);video=new VideoView(this);
 try{Uri u=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);video.setVideoURI(u);MediaController c=new MediaController(this);c.setAnchorView(video);video.setMediaController(c);video.setOnPreparedListener(mp->video.start());}catch(Exception e){t.setText("Video açılamadı: "+e.getMessage());}
 r.addView(video,new LinearLayout.LayoutParams(-1,0,1));setContentView(r);}
 protected void onDestroy(){if(video!=null){video.stopPlayback();video=null;}super.onDestroy();}
}