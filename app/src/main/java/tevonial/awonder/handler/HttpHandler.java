package tevonial.awonder.handler;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.NetworkErrorDialogFragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.URLUtil;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class HttpHandler {
    private static String sUid;
    private static int sState;
    private static boolean mNetworkConnected = true;
    private static boolean mValidUrl;
    private static Snackbar errorSnackBar;

    public static final Object sNetLock = new Object();
    public static boolean sOnline = false;
    public static boolean sNetPause = false;
    public static String sHost;

                                //Request Type                 url                  uid     param     expected return keys
    public static RequestType   GET_SIG =      new RequestType("sig.php",           false,  ""),

                                GET_GEN_UID =  new RequestType("get_gen_uid.php",   false,  "",       "uid"),
                                GET_MY_POLL =  new RequestType("get_state.php",     true,   "poll",   "poll", "mode"),
                                GET_STATE =    new RequestType("get_state.php",     true,   "state",  "state"),
                                GET_COUNT =    new RequestType("get_state.php",     true,   "count",  "count"),
                                GET_POLL =     new RequestType("get_poll.php",      false,  "",       "p_uid", "p_poll", "p_mode"),
                                GET_RESULT =   new RequestType("get_results.php",   true,   "",       "results"),

                                POST_POLL =    new RequestType("post_poll.php",     false,  ""),
                                POST_ANSWER =  new RequestType("post_response.php", false,  ""),
                                POST_STATE =   new RequestType("post_state.php",    false,  ""),
                                SELF_RESPOND = new RequestType("post_self.php",     false,  "");

    public static void setUid(String uid) {
        HttpHandler.sUid = uid;
    }

    public static void setHost(String host) {
        HttpHandler.sHost = host;
        try {
            errorSnackBar.dismiss();
        } catch (NullPointerException e) {}

        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                HttpHandler.waitForNetwork();
                if (!hasUid() && sOnline) {
                    PreferenceHandler.initUid();
                }
                return null;
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static String getUid() {
        return HttpHandler.sUid;
    }

    public static int getState() {
        return sState;
    }

    public static void setState(int state) {
        sState = state;
    }

    public static boolean hasUid() {
        try {
            return (!sUid.isEmpty());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean hasHost() {
        try {
            return (!sHost.isEmpty());
        } catch (NullPointerException e) {
            return false;
        }
    }

    private static class RequestType {
        private String path;
        private String param;
        private String[] keys;
        private boolean requireUid;

        public RequestType(String path, boolean requireUid, String param, String... keys) {
            this.path = path;
            this.param = param;
            this.keys = keys;
            this.requireUid = requireUid;
        }

        public String getUrl() {
            String url = HttpHandler.sHost;
            if (!url.endsWith("/")) {
                url += "/" + path;
            } else {
                url += path;
            }

            String uidParam = (requireUid) ? "uid=".concat(sUid) : "";

            int a = (uidParam.isEmpty()) ? 0 : 1;
            a += (param.isEmpty()) ? 0 : 1;

            if (a == 1) {
                url += "?" + param + uidParam;
            } else if (a == 2) {
                url += "?" + param + "&" + uidParam;
            }
            return url;
        }

        public String[] getKeys() {
            return keys;
        }
    }

    public interface RequestHandler {
        void onResponse(boolean success, String[] s);
    }

    private static void onError(RequestHandler rh) {
        rh.onResponse(false, null);
        boolean errorShown = false;
        try {
            errorShown = errorSnackBar.isShown();
        } catch (NullPointerException e) {}

        if (!sNetPause && (!errorShown || !mNetworkConnected)) {
            (new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    waitForNetwork();
                    return null;
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static void postJson(final RequestType requestType, final RequestHandler rh, final JSONObject body) {
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.POST, requestType.getUrl(), body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        rh.onResponse(true, null);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onError(rh);
                    }
                });
        request.setRetryPolicy(new DefaultRetryPolicy(5 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyRequestHandler.getInstance(MainActivity.sContext).addToRequestQueue(request);
    }

    public static void getJson(final RequestType requestType, final RequestHandler rh) {
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.GET, requestType.getUrl(), null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<String> values = new ArrayList<>();

                        for (String key : requestType.getKeys()) {
                            try {
                                values.add(response.getString(key));
                            } catch (JSONException e) {
                                onError(rh);
                            }
                        }

                        String ret[] = new String[values.size()];
                        ret = values.toArray(ret);
                        rh.onResponse(true, ret);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        onError(rh);
                    }
                });
        request.setRetryPolicy(new DefaultRetryPolicy(5 * 1000, 3, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyRequestHandler.getInstance(MainActivity.sContext).addToRequestQueue(request);
    }

    public static void waitForNetwork() {
        sOnline = false; mValidUrl = true;
        if (HttpHandler.hasHost()) {
            if (!isOnline()) {
                if (mNetworkConnected && mValidUrl) {
                    (new NetWaitTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    synchronized (sNetLock) {
                        try {
                            sNetLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }

        if (!sOnline) {
            MainActivity.sUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.sLoading.setVisibility(View.INVISIBLE);

                    if (mNetworkConnected) {
                        if (mValidUrl) {
                            errorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_1_message), Snackbar.LENGTH_INDEFINITE);
                        } else {
                            errorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_3_message), Snackbar.LENGTH_INDEFINITE);
                        }
                        errorSnackBar.setAction(MainActivity.sContext.getString(R.string.net_error_edit), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                MainActivity.switchView(5);
                            }
                        });
                    } else {
                        errorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_2_message), Snackbar.LENGTH_INDEFINITE);
                        errorSnackBar.setAction(MainActivity.sContext.getString(R.string.net_error_retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                VolleyRequestHandler.getInstance(MainActivity.sContext).retryLastRequest();
                            }
                        });
                    }

                    errorSnackBar.show();
                }
            });
        }
    }

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) MainActivity.sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
            mNetworkConnected = true;
            try {
                URL url = new URL(GET_SIG.getUrl());
                if (!Patterns.WEB_URL.matcher(url.toString()).matches()) {
                    mValidUrl = false;
                    return false;
                }

                Integer in = Integer.valueOf((new BufferedReader(new InputStreamReader(url.openStream()))).readLine());
                if (Math.abs((System.currentTimeMillis() / 1000L) - in) < 10) {
                    sOnline = true;
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        } else {
            mNetworkConnected = false;
            return false;
        }
    }

    public static class NetWaitTask extends AsyncTask<Void, Void, Void> {
        private NetworkErrorDialogFragment errorDialog;

        @Override
        protected Void doInBackground(Void... params) {
            int i = 0;
            while (!isOnline()) {
                if (i++ == 5) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            synchronized (sNetLock) {
                sNetPause = false;
                sNetLock.notifyAll();
            }
            this.errorDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            MainActivity.sUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.sLoading.setVisibility(View.INVISIBLE);
                }
            });

            sOnline = false;
            synchronized (sNetLock) {
                sNetPause = true;
            }
            try {
                this.errorDialog = new NetworkErrorDialogFragment();
                this.errorDialog.show(MainActivity.sFragmentManager, "dialog_fragment_network_error");
                this.errorDialog.setCancelable(false);
            } catch (IllegalStateException e) {}
        }
    }
}
