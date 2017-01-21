package co.amazonaws.mobile.push;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.HashSet;
import java.util.Set;

/** The GCMTokenHelper registers the app on the device with Google Cloud Messaging (GCM) */
public class GCMTokenHelper {

    public interface GCMTokenUpdateObserver {
        void onGCMTokenUpdate(final String gcmToken, boolean didTokenChange);
        void onGCMTokenUpdateFailed(final Exception ex);
    }

    // Name of the shared preferences
    private static final String SHARED_PREFS_FILE_NAME = GCMTokenHelper.class.getName();
    // Keys in shared preferences
    private static final String SHARED_PREFS_KEY_DEVICE_TOKEN = "deviceToken";

    private final SharedPreferences sharedPreferences;

    private final InstanceID instanceID;
    private final String gcmSenderID;

    volatile private String deviceToken;

    private Set<GCMTokenUpdateObserver> updateObservers;

    public GCMTokenHelper(final Context context, final String gcmSenderID) {
        if (gcmSenderID == null || gcmSenderID.isEmpty()) {
            throw new IllegalArgumentException("Missing GCM sender ID.");
        }

        this.gcmSenderID = gcmSenderID;
        this.instanceID = InstanceID.getInstance(context);
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_FILE_NAME,
            Context.MODE_PRIVATE);

        // load previously saved device token and endpoint arn
        deviceToken = sharedPreferences.getString(SHARED_PREFS_KEY_DEVICE_TOKEN, "");

        updateObservers = new HashSet<>();
    }

    public void init() {
        // Ensure device is registered for push and subscribe to the default topic.
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateGCMToken();
            }
        }).start();
    }

    synchronized
    public void addTokenUpdateObserver(final GCMTokenUpdateObserver tokenUpdateObserver) {
        updateObservers.add(tokenUpdateObserver);
    }

    /**
     * Updates the GCM Token.
     */
    synchronized
    public void updateGCMToken() {
        String newDeviceToken;

        // GCM throws a NullPointerException in some failure cases.
        try {
            newDeviceToken = instanceID.getToken(gcmSenderID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        } catch (final Exception re) {
            for (GCMTokenUpdateObserver observer : updateObservers) {
                observer.onGCMTokenUpdateFailed(re);
            }
            return;
        }

        final boolean didTokenChange = !newDeviceToken.equals(deviceToken);
        if (didTokenChange) {
            deviceToken = newDeviceToken;
            sharedPreferences.edit()
                .putString(SHARED_PREFS_KEY_DEVICE_TOKEN, deviceToken)
                .apply();
        }
        for (GCMTokenUpdateObserver observer : updateObservers) {
            observer.onGCMTokenUpdate(deviceToken, didTokenChange);
        }
    }

    public String getGCMToken() {
        return deviceToken;
    }
}
