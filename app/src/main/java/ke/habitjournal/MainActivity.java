package ke.habitjournal;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String NOTIF_PERMISSION = "android.permission.POST_NOTIFICATIONS";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Reminders.ensureChannel(this);
        Reminders.scheduleAll(this);

        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }

        if (needsNotificationPermission()) askNotificationPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        refreshPage();
    }

    private void refreshPage() {
        if (web != null) {
            web.evaluateJavascript("window.onNativeStatus && window.onNativeStatus()", null);
        }
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(NOTIF_PERMISSION) != PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        SharedPreferences p = getSharedPreferences("app", MODE_PRIVATE);
        p.edit().putBoolean("askedNotif", true).apply();
        requestPermissions(new String[]{NOTIF_PERMISSION}, 1);
    }

    private void openSafely(Intent primary, Intent fallback) {
        try {
            startActivity(primary);
        } catch (Exception e) {
            if (fallback != null) {
                try {
                    startActivity(fallback);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Functions the journal page can call as window.Android.* */
    private class Bridge {
        private final Context ctx = MainActivity.this;

        @JavascriptInterface
        public String getReminders() {
            return Reminders.load(ctx).toString();
        }

        @JavascriptInterface
        public boolean setReminders(String json) {
            try {
                new JSONArray(json);
            } catch (JSONException e) {
                return false;
            }
            Reminders.save(ctx, json);
            Reminders.scheduleAll(ctx);
            return true;
        }

        @JavascriptInterface
        public String status() {
            JSONObject o = new JSONObject();
            try {
                NotificationManager nm = getSystemService(NotificationManager.class);
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                o.put("notifications", nm.areNotificationsEnabled());
                o.put("exact", Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms());
                o.put("battery", pm.isIgnoringBatteryOptimizations(getPackageName()));
            } catch (JSONException ignored) {
            }
            return o.toString();
        }

        @JavascriptInterface
        public void fixNotifications() {
            runOnUiThread(() -> {
                boolean asked = getSharedPreferences("app", MODE_PRIVATE).getBoolean("askedNotif", false);
                if (needsNotificationPermission() && !asked) {
                    askNotificationPermission();
                } else {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    Intent fb = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    openSafely(i, fb);
                }
            });
        }

        @JavascriptInterface
        public void fixExact() {
            if (Build.VERSION.SDK_INT < 31) return;
            runOnUiThread(() -> openSafely(
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())),
                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()))));
        }

        @JavascriptInterface
        public void fixBattery() {
            runOnUiThread(() -> openSafely(
                    new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName())),
                    new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        }

        @JavascriptInterface
        public void testNotification() {
            Reminders.show(ctx, "Test reminder", "Reminders are working.", 9999);
        }

        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Habit Journal backup", text));
        }
    }
}
