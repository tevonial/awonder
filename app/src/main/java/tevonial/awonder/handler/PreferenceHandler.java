package tevonial.awonder.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;


import tevonial.awonder.MainActivity;
import tevonial.awonder.R;

public class PreferenceHandler {
    private static Object requestLock = new Object();

    public final static String KEY_UID =   MainActivity.sContext.getString(R.string.pref_uid_key),
                               KEY_STATE = MainActivity.sContext.getString(R.string.pref_state_key),
                               KEY_HOST =  MainActivity.sContext.getString(R.string.pref_host_key);

    private final static SharedPreferences sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(MainActivity.sContext);

    public static void initHost() {
        HttpHandler.setHost(sharedPreferences.getString(KEY_HOST, ""));
    }

    public static void initUid() {
        if (!sharedPreferences.contains(KEY_UID) && HttpHandler.hasHost()) {
            try {
                HttpHandler.requestGetJson(HttpHandler.GET_GEN_UID, new HttpHandler.RequestHandler() {
                    @Override
                    public void onResponse(boolean success, String[] s) {
                        if (success) {
                            String uid = s[0];
                            HttpHandler.setUid(uid);
                            editSharedPreference(1, KEY_UID, uid);
                        }

                        synchronized (requestLock) {
                            requestLock.notifyAll();
                        }
                    }
                });
                synchronized (requestLock) {
                    requestLock.wait();
                }
            } catch (InterruptedException e) {}
        }

        HttpHandler.setUid(sharedPreferences.getString(KEY_UID, ""));
    }

    public static void saveAll() {
        editSharedPreference(0, KEY_STATE, HttpHandler.getState());
        editSharedPreference(1, KEY_HOST,  HttpHandler.getHost());
    }

    public static void removeUid() {
        sharedPreferences.edit().remove(KEY_UID).apply();
    }

    public static void removeHost() {
        sharedPreferences.edit().remove(KEY_HOST).apply();
    }

    private static void editSharedPreference(int type, String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (type == 0) {
            editor.putInt(key, (int) value);
        } else if (type == 1) {
            editor.putString(key, (String) value);
        }
        editor.apply();
    }
}
