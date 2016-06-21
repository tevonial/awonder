package tevonial.awonder.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;


import tevonial.awonder.MainActivity;
import tevonial.awonder.R;

public class PreferenceHandler {
    private static Context sContext = MainActivity.sContext;
    private static Object requestLock = new Object();

    private final static String uid_key =   sContext.getString(R.string.pref_uid_key),
                                state_key = sContext.getString(R.string.pref_state_key),
                                host_key =  sContext.getString(R.string.pref_host_key);

    private final static SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(sContext);

    private static HttpHandler.RequestHandler initUidHandler = new HttpHandler.RequestHandler() {
        @Override
        public void onResponse(boolean success, String[] s) {
            if (success) {
                String uid = s[0];
                HttpHandler.setUid(uid);
                editSharedPreferences(1, uid_key, uid);
            }

            synchronized (requestLock) {
                requestLock.notifyAll();
            }
        }
    };

    public static void init() {
        HttpHandler.setHost(sharedPref.getString(host_key, ""));
        initUid();
    }

    public static void initUid() {
        if (!sharedPref.contains(uid_key) && HttpHandler.hasHost()) {
            try {
                HttpHandler.getJson(HttpHandler.GET_GEN_UID, initUidHandler);
                synchronized (requestLock) {
                    requestLock.wait();
                }
            } catch (InterruptedException e) {}
        }

        HttpHandler.setUid(sharedPref.getString(uid_key, ""));
    }

    public static void saveAll() {
        editSharedPreferences(0, state_key, HttpHandler.getState());
        editSharedPreferences(1, host_key,  HttpHandler.getHost());
    }

    public static void removeUid() {
        sharedPref.edit().remove(uid_key).apply();
    }

    private static void editSharedPreferences(int type, String key, Object value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        if (type == 0) {
            editor.putInt(key, (int) value);
        } else if (type == 1) {
            editor.putString(key, (String) value);
        }
        editor.apply();
    }
}
