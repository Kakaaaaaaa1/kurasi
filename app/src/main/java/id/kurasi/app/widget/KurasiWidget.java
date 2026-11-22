package id.kurasi.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import id.kurasi.app.R;
import id.kurasi.app.data.Repo;
import id.kurasi.app.ui.BinderActivity;
import id.kurasi.app.util.Fmt;

/** Widget layar utama: total nilai koleksi dan jumlah kartu. */
public class KurasiWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, views(c));
    }

    public static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, KurasiWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = views(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    private static RemoteViews views(Context c) {
        Repo repo = Repo.get(c);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_kurasi);
        v.setTextViewText(R.id.wValue, Fmt.juta(repo.totalValue()));
        v.setTextViewText(R.id.wCount, repo.ownedCount() + " KARTU");
        Intent i = new Intent(c, BinderActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.wRoot, pi);
        return v;
    }
}
