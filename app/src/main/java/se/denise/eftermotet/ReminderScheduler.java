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

    static synchronized void sync(Context context, String json) {
        JSONArray next;
        try { next = new JSONArray(json); } catch (Exception ignored) { return; }
        if (next.length() > 500) return;
        String previous = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray old = new JSONArray(previous);
            for (int i = 0; i < old.length(); i++) {
                JSONObject before = old.optJSONObject(i);
                JSONObject after = find(next, before == null ? "" : before.optString("id"));
                if (!same(before, after)) cancel(context, before);
            }
            for (int i = 0; i < next.length(); i++) {
                JSONObject after = next.optJSONObject(i);
                JSONObject before = find(old, after == null ? "" : after.optString("id"));
                // Keep an overdue alarm: Android may still be preparing to deliver it.
                if (!same(before, after)) schedule(context, after);
            }
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

    static boolean unchanged(long beforeAt, String beforeTitle, String beforeWhen,
            long afterAt, String afterTitle, String afterWhen) {
        return beforeAt == afterAt && beforeTitle.equals(afterTitle) && beforeWhen.equals(afterWhen);
    }

    private static JSONObject find(JSONArray items, String id) {
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null && id.equals(item.optString("id"))) return item;
        }
        return null;
    }

    private static boolean same(JSONObject before, JSONObject after) {
        return before != null && after != null && unchanged(
            before.optLong("at"), before.optString("title"), before.optString("when"),
            after.optLong("at"), after.optString("title"), after.optString("when"));
    }

    static boolean preciseAllowed(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms();
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
        if (preciseAllowed(context)) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
                return;
            } catch (SecurityException ignored) {
                // Permission can be revoked between the check and scheduling.
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
    }
}
