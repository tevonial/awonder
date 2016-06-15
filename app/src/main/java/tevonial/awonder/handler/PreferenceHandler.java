package tevonial.awonder.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;


import tevonial.awonder.MainActivity;
import tevonial.awonder.R;

public class PreferenceHandler {

    private static Context sContext = MainActivity.sContext;
    private static boolean sUidReady = false;
    private static String keyStr;
    private static Object mRequestLock = new Object();

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
                Log.d("EW", "Save preference: " + uid_key + ": " + uid);
                sUidReady = true;
            }

            synchronized (mRequestLock) {
                Log.d("EW", "mRequestLock.notifyAll()");
                mRequestLock.notifyAll();
            }
        }
    };

    public static void init() {
        HttpHandler.setHost(sharedPref.getString(host_key, HttpHandler.sDefaultHost));

        if (!sharedPref.contains(uid_key)) {
            HttpHandler.testNetwork();
            while (!sUidReady) {
                try {
                    HttpHandler.getJson(HttpHandler.GET_GEN_UID, initUidHandler);
                    synchronized (mRequestLock) {
                        Log.d("EW", "mRequestLock.wait()");
                        mRequestLock.wait();
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        HttpHandler.setUid(sharedPref.getString(uid_key, ""));

        String keys[] = {state_key, uid_key, host_key}; keyStr  = "";
        for (String key : keys) {
            keyStr += key + ", ";
        }
        keyStr = keyStr.replaceAll(", $", "");

        Log.d("EW", "Read ALL preferences: " + keyStr);
    }

    public static void saveAll() {
        editSharedPreferences(0, state_key, HttpHandler.getState());
        editSharedPreferences(1, host_key,  HttpHandler.sHost);
        Log.d("EW", "Save ALL preferences: " + keyStr);
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
