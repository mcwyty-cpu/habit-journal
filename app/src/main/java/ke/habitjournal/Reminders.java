package ke.habitjournal;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/** Stores the daily reminder list and schedules one alarm per reminder. */
final class Reminders {
    private Reminders() {}

    static final String CHANNEL_ID = "habit_reminders";
    private static final String PREFS = "reminders";
    private static final String KEY_LIST = "list";
    private static final String KEY_SCHEDULED = "scheduled";

    static final String DEFAULT_LIST = "["
            + "{\"id\":\"r1\",\"time\":\"07:00\",\"title\":\"Wake up\",\"on\":true},"
            + "{\"id\":\"r2\",\"time\":\"07:05\",\"title\":\"Warm water + pray\",\"on\":true},"
            + "{\"id\":\"r3\",\"time\":\"07:15\",\"title\":\"Morning manifestation (1 of 3)\",\"on\":true},"
            + "{\"id\":\"r4\",\"time\":\"07:20\",\"title\":\"Meditation - 10 mins\",\"on\":true},"
            + "{\"id\":\"r5\",\"time\":\"07:35\",\"title\":\"Exercise - 20 mins\",\"on\":true},"
            + "{\"id\":\"r6\",\"time\":\"08:00\",\"title\":\"Cold shower + healthy breakfast\",\"on\":true},"
            + "{\"id\":\"r7\",\"time\":\"10:00\",\"title\":\"Water check - on track for 3L?\",\"on\":true},"
            + "{\"id\":\"r8\",\"time\":\"12:00\",\"title\":\"Noon manifestation (2 of 3)\",\"on\":true},"
            + "{\"id\":\"r9\",\"time\":\"13:00\",\"title\":\"Water check - about 1.5L by now\",\"on\":true},"
            + "{\"id\":\"r10\",\"time\":\"16:00\",\"title\":\"Water check - finish strong to 3L\",\"on\":true},"
            + "{\"id\":\"r11\",\"time\":\"16:30\",\"title\":\"Evening walk - 10 mins\",\"on\":true},"
            + "{\"id\":\"r12\",\"time\":\"18:30\",\"title\":\"Dinner - finish before 7 PM\",\"on\":true},"
            + "{\"id\":\"r13\",\"time\":\"20:30\",\"title\":\"Study - 2 hrs (until 10:30)\",\"on\":true},"
            + "{\"id\":\"r14\",\"time\":\"22:00\",\"title\":\"Phone down until 8:30 AM\",\"on\":true},"
            + "{\"id\":\"r15\",\"time\":\"22:40\",\"title\":\"Brush your teeth\",\"on\":true},"
            + "{\"id\":\"r16\",\"time\":\"22:45\",\"title\":\"Night manifestation (3 of 3)\",\"on\":true},"
            + "{\"id\":\"r17\",\"time\":\"23:00\",\"title\":\"Lights out - sleep by 11\",\"on\":true}"
            + "]";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static JSONArray load(Context c) {
        String s = prefs(c).getString(KEY_LIST, null);
        try {
            return new JSONArray(s != null ? s : DEFAULT_LIST);
        } catch (JSONException e) {
            try {
                return new JSONArray(DEFAULT_LIST);
            } catch (JSONException e2) {
                return new JSONArray();
            }
        }
    }

    static void save(Context c, String json) {
        prefs(c).edit().putString(KEY_LIST, json).apply();
    }

    static JSONObject find(Context c, String id) {
        JSONArray a = load(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    private static PendingIntent alarmIntent(Context c, String id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setData(Uri.parse("habitjournal://reminder/" + Uri.encode(id)));
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Cancels every alarm this app set, then sets one for each enabled reminder. */
    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences p = prefs(c);
        Set<String> old = new HashSet<>(p.getStringSet(KEY_SCHEDULED, new HashSet<>()));
        for (String id : old) am.cancel(alarmIntent(c, id));

        JSONArray a = load(c);
        Set<String> now = new HashSet<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("id", "");
            if (id.isEmpty() || !o.optBoolean("on", true)) continue;
            scheduleNext(c, o);
            now.add(id);
        }
        p.edit().putStringSet(KEY_SCHEDULED, now).apply();
    }

    /** Sets the alarm for the next time this reminder's clock time comes round. */
    static void scheduleNext(Context c, JSONObject o) {
        String id = o.optString("id", "");
        int h = 7, m = 0;
        try {
            String[] parts = o.optString("time", "07:00").split(":");
            h = Integer.parseInt(parts[0].trim());
            m = Integer.parseInt(parts[1].trim());
        } catch (Exception ignored) {
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        setAlarm(c, cal.getTimeInMillis(), alarmIntent(c, id));
    }

    private static void setAlarm(Context c, long at, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Daily reminders for your habits");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    static void show(Context c, String title, String text, int notificationId) {
        ensureChannel(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (!nm.areNotificationsEnabled()) return;

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_check)
                .setColor(0xFF2747C9)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();
        nm.notify(notificationId, n);
    }
}
