package com.musab.dosyayoneticisi;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class BinaryEditorPlusActivity extends Activity {
    protected File file; protected LinearLayout root; protected TextView info; protected EditText search; protected byte[] pending;
    protected abstract boolean isValid(byte[] data); protected abstract String formatInfo(byte[] data); protected abstract String getEditorName();
    @Override public void onCreate(Bundle b){super.onCreate(b);file=new File(getIntent().getStringExtra("path"));build();load();}
    protected void build(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,12,12,12);root.setBackgroundColor(Color.rgb(10,10,12));
        TextView title=new TextView(this);title.setText(getEditorName());title.setTextColor(Color.WHITE);title.setTextSize(25);title.setPadding(8,8,8,12);root.addView(title,new LinearLayout.LayoutParams(-1,72));
        info=new TextView(this);info.setTextColor(Color.LTGRAY);info.setTextSize(14);root.addView(info,new LinearLayout.LayoutParams(-1,72));
        LinearLayout tools=new LinearLayout(this);
        search=new EditText(this);search.setHint("UTF-8 string ara");search.setTextColor(Color.WHITE);search.setHintTextColor(Color.GRAY);
        Button find=button("Bul");find.setOnClickListener(v->findString());Button replace=button("Değiştir");replace.setOnClickListener(v->replaceStringDialog());Button hex=button("Hex");hex.setOnClickListener(v->hexPatchDialog());
        tools.addView(search,new LinearLayout.LayoutParams(0,70,1));tools.addView(find,new LinearLayout.LayoutParams(65,70));tools.addView(replace,new LinearLayout.LayoutParams(100,70));tools.addView(hex,new LinearLayout.LayoutParams(65,70));root.addView(tools);
        TextView note=new TextView(this);note.setText("Görüntüleme + yerinde düzenleme: eşit byte uzunluğundaki UTF-8 metinleri değiştirme ve offset/hex byte yaması.");note.setTextColor(Color.LTGRAY);note.setTextSize(13);root.addView(note,new LinearLayout.LayoutParams(-1,70));
        ScrollView sv=new ScrollView(this);TextView content=new TextView(this);content.setTextColor(Color.WHITE);content.setTextSize(13);content.setTypeface(android.graphics.Typeface.MONOSPACE);content.setPadding(8,8,8,30);content.setId(android.R.id.text1);sv.addView(content);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        Button save=button("Değişiklikleri diske kaydet");save.setOnClickListener(v->saveData());root.addView(save,new LinearLayout.LayoutParams(-1,72));setContentView(root);
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);return b;}
    protected byte[] read()throws Exception{if(file==null||!file.isFile())throw new IOException("Dosya bulunamadı");if(file.length()>16L*1024*1024)throw new IOException("16 MB üzerindeki dosyalarda düzenleme kapalı");ByteArrayOutputStream o=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)o.write(b,0,n);}return o.toByteArray();}
    protected void load(){try{byte[] d=read();info.setText(formatInfo(d)+"\nBoyut: "+d.length+" byte"+(isValid(d)?"":"\nUyarı: beklenen başlık doğrulanamadı; salt görüntüleme devam ediyor."));((TextView)root.findViewById(android.R.id.text1)).setText(dump(d));}catch(Exception e){info.setText("Hata: "+e.getMessage());}}
    protected String dump(byte[] d){StringBuilder s=new StringBuilder("Dosya: "+file.getName()+"\nİlk 256 byte (hex):\n");int n=Math.min(256,d.length);for(int i=0;i<n;i++){if(i%16==0)s.append(String.format(Locale.US,"%08X  ",i));s.append(String.format(Locale.US,"%02X ",d[i]&255));if(i%16==15)s.append("\n");}s.append("\nYazdırılabilir dizeler:\n");int start=-1;for(int i=0;i<d.length;i++){int c=d[i]&255;boolean ok=c>=32&&c<127;if(ok&&start<0)start=i;if((!ok||i==d.length-1)&&start>=0){int end=ok&&i==d.length-1?i+1:i;if(end-start>=4)s.append(String.format(Locale.US,"%08X  ",start)).append(new String(d,start,end-start,StandardCharsets.UTF_8)).append("\n");start=-1;}if(s.length()>60000){s.append("... görüntüleme sınırı ...");break;}}return s.toString();}
    private void findString(){String q=search.getText().toString();if(q.isEmpty())return;try{byte[] d=read(),n=q.getBytes(StandardCharsets.UTF_8);int at=indexOf(d,n,0);info.setText(at>=0?"Bulundu: 0x"+Integer.toHexString(at)+" ("+at+")":"Bulunamadı: "+q);}catch(Exception e){info.setText("Hata: "+e.getMessage());}}
    private void replaceStringDialog(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText oldE=new EditText(this);oldE.setHint("Eski UTF-8 metin");EditText newE=new EditText(this);newE.setHint("Yeni metin (aynı byte uzunluğu)");box.addView(oldE);box.addView(newE);new AlertDialog.Builder(this).setTitle(getEditorName()+" — string düzenle").setView(box).setPositiveButton("Uygula",(d,w)->{try{byte[] data=pending!=null?pending:read();byte[] a=oldE.getText().toString().getBytes(StandardCharsets.UTF_8),b=newE.getText().toString().getBytes(StandardCharsets.UTF_8);if(a.length==0||a.length!=b.length)throw new Exception("Yeni metin byte uzunluğu aynı olmalı");int count=0,at=0;while((at=indexOf(data,a,at))>=0){System.arraycopy(b,0,data,at,b.length);at+=b.length;count++;}if(count==0)throw new Exception("Eşleşme bulunamadı");pending=data;info.setText(count+" eşleşme düzenlendi. Kaydet'e bas.");((TextView)root.findViewById(android.R.id.text1)).setText(dump(data));}catch(Exception e){info.setText("Hata: "+e.getMessage());}}).setNegativeButton("İptal",null).show();}
    private void hexPatchDialog(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText off=new EditText(this);off.setHint("Offset: 0x20 veya 32");EditText hex=new EditText(this);hex.setHint("Byte: 00 FF 41");box.addView(off);box.addView(hex);new AlertDialog.Builder(this).setTitle(getEditorName()+" — Hex düzenle").setView(box).setPositiveButton("Uygula",(d,w)->{try{byte[] data=pending!=null?pending:read();String os=off.getText().toString().trim().toLowerCase(Locale.ROOT);int o=os.startsWith("0x")?Integer.parseInt(os.substring(2),16):Integer.parseInt(os);String[] p=hex.getText().toString().trim().split("\\s+");if(o<0||o+p.length>data.length)throw new Exception("Offset aralık dışında");for(int i=0;i<p.length;i++)data[o+i]=(byte)Integer.parseInt(p[i],16);pending=data;info.setText("Hex yaması uygulandı. Kaydet'e bas.");((TextView)root.findViewById(android.R.id.text1)).setText(dump(data));}catch(Exception e){info.setText("Hex hatası: "+e.getMessage());}}).setNegativeButton("İptal",null).show();}
    private void saveData(){if(pending==null){info.setText("Kaydedilecek değişiklik yok.");return;}try(FileOutputStream out=new FileOutputStream(file,false)){out.write(pending);pending=null;load();Toast.makeText(this,"Kaydedildi: "+file.getName(),Toast.LENGTH_SHORT).show();}catch(Exception e){info.setText("Kaydetme hatası: "+e.getMessage());}}
    private int indexOf(byte[] d,byte[] n,int from){outer:for(int i=Math.max(0,from);i<=d.length-n.length;i++){for(int j=0;j<n.length;j++)if(d[i+j]!=n[j])continue outer;return i;}return -1;}
}