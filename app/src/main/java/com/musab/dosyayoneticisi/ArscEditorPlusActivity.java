package com.musab.dosyayoneticisi;
public class ArscEditorPlusActivity extends BinaryEditorPlusActivity {
 protected String getEditorName(){return "ARSC Editör Plus";}
 protected boolean isValid(byte[] d){return d.length>=4&&(d[0]&255)==0x02&&(d[1]&255)==0x00;}
 protected String formatInfo(byte[] d){return "resources.arsc kaynak tablosu — görüntüleme, string arama ve yerinde düzenleme";}
}