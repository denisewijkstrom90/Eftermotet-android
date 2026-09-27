package se.denise.eftermotet;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

final class ReminderScheduler {
    private static final String PREFS = "reminders";
    private static final String KEY = "items";

    static void sync(Context context, String json) {
        JSONArray next;
        try { next = new JSONArray(json); } catch (Exception ignored) { return; }
        if (next.length() > 500) return;
        String previous = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray old = new JSONArray(previous);
            for (int i = 0; i < old.length(); i++) cancel(context, old.optJSONObject(i));
            for (int i = 0; i < next.length(); i++) schedule(context, next.optJSONObject(i));
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, next.toString()).apply();
        } catch (Exception ignored) { }
    }

    static void restore(Context context) {
        String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray items = new JSONArray(stored);
            for (int i = 0; i < items.length(); i++) schedule(context, items.optJSONObject(i));
        } catch (Exception ignored) { }
    }

    private static PendingIntent intent(Context context, JSONObject item, int flags) {
        if (item == null) return null;
        String id = item.optString("id", "");
        if (!id.matches("[-a-fA-F0-9]{36}")) return null;
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", id.hashCode());
        intent.putExtra("title", item.optString("title", "Möte"));
        intent.putExtra("when", item.optString("when", ""));
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancel(Context context, JSONObject item) {
        PendingIntent pending = intent(context, item, PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(pending);
            pending.cancel();
        }
    }

    private static void schedule(Context context, JSONObject item) {
        if (item == null) return;
        long at = item.optLong("at", 0);
        if (at <= System.currentTimeMillis() || at > System.currentTimeMillis() + 366L * 24 * 60 * 60 * 1000) return;
        PendingIntent pending = intent(context, item, PendingIntent.FLAG_UPDATE_CURRENT);
        if (pending == null) return;
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
    }
}
