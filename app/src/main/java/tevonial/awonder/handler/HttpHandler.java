package tevonial.awonder.handler;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.NetworkErrorDialogFragment;

import android.net.ConnectivityManager;
import android.os.AsyncTask;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class HttpHandler {
    private static String sUid = "";
    private static int sState;
    private static final String sSignatureScript = "sig.php";

    public static Object sNetLock = new Object();
    public static boolean sNetPause = false;
    public static String sDefaultHost = MainActivity.sContext.getString(R.string.default_host);
    public static boolean sUseDefaultHost;
    public static String sHost;

                                //Request Type                 url                  param     expected return keys
    public static RequestType   GET_GEN_UID =  new RequestType("get_gen_uid.php",   null,     "uid"),
                                GET_MY_POLL =  new RequestType("get_state.php",     "poll",   "poll", "mode"),
                                GET_STATE =    new RequestType("get_state.php",     "state",  "state"),
                                GET_COUNT =    new RequestType("get_state.php",     "count",  "count"),
                                GET_POLL =     new RequestType("get_poll.php",      null,     "p_uid", "p_poll", "p_mode"),
                                GET_RESULT =   new RequestType("get_results.php",   null,     "results"),

                                POST_POLL =    new RequestType("post_poll.php",     null),
                                POST_ANSWER =  new RequestType("post_response.php", null),
                                POST_STATE =   new RequestType("post_state.php",    null),
                                SELF_RESPOND = new RequestType("post_self.php",     null);

    public static void setUid(String uid) {
        HttpHandler.sUid = uid;
    }

    public static void setHost(String host) {
        HttpHandler.sHost = host;
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

    private static class RequestType {
        private String path;
        private String param;
        private String[] keys;

        public RequestType(String path, String param, String... keys) {
            this.path = path;
            this.param = param;
            this.keys = keys;
        }

        public String getUrl() {
            String tUrl = HttpHandler.sHost + path;

            if (param != null) {
                String tUid = (sUid.isEmpty()) ? "" : "uid=" + sUid;
                int a = (tUid.isEmpty()) ? 0 : 1;
                a = (param.isEmpty()) ? a : a + 1;

                if (a > 0) {
                    if (a == 1) {
                        tUrl += "?" + param + tUid;
                    } else if (a == 2) {
                        tUrl += "?" + param + "&" + tUid;
                    }
                }
            }

            return tUrl;
        }

        public String[] getKeys() {
            return keys;
        }
    }

    public interface RequestHandler {
        void onResponse(boolean success, String[] s);
    }

    private static void onError(RequestHandler rh) {
        if (!sNetPause) {
            rh.onResponse(false, null);
            (new NetWaitTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    public static void testNetwork() {
        if (!isOnline()) {
            (new NetWaitTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) MainActivity.sContext.getSystemService(MainActivity.sContext.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting()) {
            try {
                URL url = new URL( ((sUseDefaultHost) ? sDefaultHost : sHost) + sSignatureScript);
                Integer in = Integer.valueOf((new BufferedReader(new InputStreamReader(url.openStream()))).readLine());
                if (Math.abs((System.currentTimeMillis() / 1000L) - in) < 10) {
                    if (sUseDefaultHost) setHost(sDefaultHost);
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static class NetWaitTask extends AsyncTask<Void, Void, Void> {
        private NetworkErrorDialogFragment error;
        private boolean online;

        @Override
        protected Void doInBackground(Void... params) {
            int i = 0;
            while (!(online = isOnline())) {
                if (i++ == 5) {
                    sUseDefaultHost = true;
                    this.error.invalidate();
                } else if (i == 8) {
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
            this.error.dismiss();
            if (!online) MainActivity.switchView(5);
        }

        @Override
        protected void onPreExecute() {
            sUseDefaultHost = false;
            synchronized (sNetLock) {
                sNetPause = true;
            }
            try {
                this.error = new NetworkErrorDialogFragment();
                this.error.show(MainActivity.sFragmentManager, "dialog_fragment_network_error");
                this.error.setCancelable(false);
            } catch (IllegalStateException e) {}
        }
    }
}
