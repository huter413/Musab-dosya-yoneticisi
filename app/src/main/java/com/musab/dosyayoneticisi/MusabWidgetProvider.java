package com.musab.dosyayoneticisi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

public class MusabWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TICK = "com.musab.dosyayoneticisi.WIDGET_TICK";
    private static int frame = 0;

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { updateAll(context, manager, ids); }
    @Override public void onEnabled(Context context) { super.onEnabled(context); scheduleTicks(context); }
    @Override public void onDisabled(Context context) { cancelTicks(context); super.onDisabled(context); }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TICK.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, MusabWidgetProvider.class));
            frame = (frame + 1) % 4;
            updateAll(context, manager, ids);
        }
    }

    private void updateAll(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null) return;
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_musab);
            String[] faces = {"✦", "✧", "✦", "✿"};
            views.setTextViewText(R.id.widget_face, faces[frame]);
            views.setTextViewText(R.id.widget_status, frame % 2 == 0 ? "Dosyaların yanında ✦" : "Musab burada ✿");
            Intent launch = new Intent(context, FastMainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, views);
        }
    }

    private void scheduleTicks(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000L, 5000L, tickPendingIntent(context));
    }

    private void cancelTicks(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.cancel(tickPendingIntent(context));
    }

    private PendingIntent tickPendingIntent(Context context) {
        Intent i = new Intent(ACTION_TICK).setPackage(context.getPackageName());
        return PendingIntent.getBroadcast(context, 9911, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
