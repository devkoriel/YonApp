package co.koriel.yonapp;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmListenerService;

import java.util.List;

/**
 * A service that listens to GCM notifications.
 */
public class PushListenerService extends GcmListenerService {

    // Intent action used in local broadcast
    public static final String ACTION_SNS_NOTIFICATION = "sns-notification";
    // Intent keys
    public static final String INTENT_SNS_NOTIFICATION_FROM = "from";
    public static final String INTENT_SNS_NOTIFICATION_DATA = "data";

    public static final String TYPE_ANNOUNCEMENT = "announcement";
    public static final String TYPE_ONELINE_NEW_COMMENT = "oneline_new_comment";
    public static final String TYPE_NEW_NOTICE = "new_notice";

    private static boolean isForeground(Context context) {
        // Gets a list of running processes.
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();

        // On some versions of android the first item in the list is what runs in the foreground,
        // but this is not true on all versions.  Check the process importance to see if the app
        // is in the foreground.
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : tasks) {
            if (ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == appProcess.importance
                && packageName.equals(appProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    private void displayNotification(final Bundle data) {
        String type = data.getString("type");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (type.equals(TYPE_ANNOUNCEMENT) && prefs.getBoolean("push_notice_all", true)) {
            Intent notificationIntent = new Intent();
            notificationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            int requestID = (int) System.currentTimeMillis();
            PendingIntent contentIntent = PendingIntent.getActivity(this, requestID, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Display a notification with an icon, message as content, and default sound. It also
            // opens the app when the notification is clicked.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_announcement_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(data.getString("title"))
                    .setTicker(data.getString("message"))
                    .setContentText(data.getString("message"))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else if (type.equals(TYPE_ONELINE_NEW_COMMENT) && prefs.getBoolean("push_oneline_new_comment", true)) {
            Intent notificationIntent = new Intent(this, SplashActivity.class);
            notificationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationIntent.putExtras(data);
            int requestID = (int) System.currentTimeMillis();
            PendingIntent contentIntent = PendingIntent.getActivity(this, requestID, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Display a notification with an icon, message as content, and default sound. It also
            // opens the app when the notification is clicked.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_chat_white_36dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(data.getString("title"))
                    .setTicker(data.getString("message"))
                    .setContentText(data.getString("message"))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } else if (type.equals(TYPE_NEW_NOTICE) && prefs.getBoolean("push_yscec_new_notice", true)) {
            Intent notificationIntent = new Intent(this, SplashActivity.class);
            notificationIntent.setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationIntent.putExtras(data);
            int requestID = (int) System.currentTimeMillis();
            PendingIntent contentIntent = PendingIntent.getActivity(this, requestID, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Display a notification with an icon, message as content, and default sound. It also
            // opens the app when the notification is clicked.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_fiber_new_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(data.getString("title"))
                    .setTicker(data.getString("message"))
                    .setContentText(data.getString("message"))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void broadcast(final String from, final Bundle data) {
        Intent intent = new Intent(ACTION_SNS_NOTIFICATION);
        intent.putExtra(INTENT_SNS_NOTIFICATION_FROM, from);
        intent.putExtras(data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs. For Set of keys use
     * data.keySet().
     */
    @Override
    public void onMessageReceived(final String from, final Bundle data) {
        if (data.getString("type") != null) {
            if (isForeground(this)) {
                broadcast(from, data);
            } else {
                displayNotification(data);
            }
        }
    }
}
