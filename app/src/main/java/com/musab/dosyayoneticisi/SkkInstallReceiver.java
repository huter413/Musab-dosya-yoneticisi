package com.musab.dosyayoneticisi;

import android.app.*;
import android.content.*;
import android.content.pm.PackageInstaller;
import android.os.*;

public class SkkInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!"com.musab.dosyayoneticisi.SKK_INSTALL_RESULT".equals(intent.getAction())) return;
        int status=intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String msg=intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        Intent ui=new Intent(context,SkkInstallerActivity.class);
        ui.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(status==PackageInstaller.STATUS_SUCCESS){
            ui.putExtra("installResult","success");
        } else {
            ui.putExtra("installResult","failure");
            ui.putExtra("installMessage",msg==null?"Bilinmeyen kurulum hatası":msg);
        }
        context.startActivity(ui);
    }
}
