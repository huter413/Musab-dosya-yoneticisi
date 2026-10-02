package com.musab.dosyayoneticisi;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

/**
 * Fast launcher wrapper for the file manager.
 * The original MainActivity calculates directory sizes recursively on the UI
 * thread. That can block the first frame when storage contains many files.
 */
public class FastMainActivity extends MainActivity {
    @Override
    public void onCreate(Bundle b) {
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(10, 10, 12)));
        super.onCreate(b);
    }

    @Override
    long directorySize(java.io.File dir) {
        // Do not recursively scan storage during the first screen render.
        // A zero value is intentionally used here to keep startup responsive.
        return 0L;
    }
}
