package ke.habitjournal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

/** Fires at a reminder's time: shows the notification and sets tomorrow's alarm. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        if (id == null) return;
        JSONObject o = Reminders.find(context, id);
        if (o == null || !o.optBoolean("on", true)) return;

        Reminders.show(context, o.optString("title", "Habit reminder"),
                "Tap to open your journal and tick it off.", id.hashCode());
        Reminders.scheduleNext(context, o);
    }
}
