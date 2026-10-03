package com.musab.dosyayoneticisi;
public class DexEditorPlusActivity extends BinaryEditorPlusActivity {
 protected String getEditorName(){return "Dex Editör Plus";}
 protected boolean isValid(byte[] d){return d.length>=8&&d[0]=='d'&&d[1]=='e'&&d[2]=='x'&&d[3]=='\n';}
 protected String formatInfo(byte[] d){return "DEX başlığı — görüntüleme, string arama ve yerinde düzenleme";}
}