package tevonial.awonder.handler;

import tevonial.awonder.MainActivity;
import tevonial.awonder.dialog.NetworkErrorDialogFragment;

import android.app.Activity;
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
    private static MainActivity sMainActivity;
    private static String sUid = "";
    private static int sState;
    private static final String sSignatureScript = "sig.php";

    public static Object sNetLock = new Object();
    public static boolean sNetPause = false;
    public static String sDefaultHost = "http://everwonder.ddns.net/app/";
    public static String sHost;


    public static RequestType //Request Type         include params  url                  params    expected return keys
                                GET_UID =     new RequestType(false, "get_uid.php",       "",       "uid"),
                                GET_MYPOLL =  new RequestType(true,  "get_state.php",     "inits",  "quota", "poll", "mode"),
                                GET_STATE =   new RequestType(true,  "get_state.php",     "state",  "state"),
                                GET_COUNT =   new RequestType(true,  "get_state.php",     "count",  "count"),
                                GET_POLL =    new RequestType(false, "get_poll.php",      "",       "p_uid", "p_poll", "p_mode"),
                                GET_RESULT =  new RequestType(true,  "get_results.php",   "",       "results"),

                                POST_POLL =   new RequestType(false, "post_poll.php",     null),
                                POST_ANSWER = new RequestType(false, "post_response.php", null),
                                POST_STATE =  new RequestType(false, "post_state.php",    null);

    public static void setUid(String uid) {
        HttpHandler.sUid = uid;
    }

    public static void setHost(String host) {
        HttpHandler.sHost = host;
    }

    public static String getUid() {
        return HttpHandler.sUid;
    }

    public static void setActivity(Activity a) {
        HttpHandler.sMainActivity = (MainActivity) a;
    }

    public static int getState() {
        return sState;
    }

    public static void setState(int state) {
        sState = state;
    }

    private static class RequestType {
        private String url;
        private String params;
        private String[] keys;
        private boolean includeParams;

        public RequestType(boolean includeParams, String url, String params, String... keys) {
            this.includeParams = includeParams;
            this.url = url;
            this.params = params;
            this.keys = keys;
        }

        public String getUrl() {
            String tUrl = HttpHandler.sHost + url;

            if (includeParams) {
                String tUid = (sUid.isEmpty()) ? "" : "uid=" + sUid;
                int a = (tUid.isEmpty()) ? 0 : 1;
                a = (params.isEmpty()) ? a : a + 1;

                if (a > 0) {
                    if (a == 1) {
                        tUrl += "?" + params + tUid;
                    } else if (a == 2) {
                        tUrl += "?" + params + "&" + tUid;
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
                URL url = new URL(sHost + sSignatureScript);
                Integer in = Integer.valueOf((new BufferedReader(new InputStreamReader(url.openStream()))).readLine());
                return (Math.abs((System.currentTimeMillis() / 1000L) - in) < 10);
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static class NetWaitTask extends AsyncTask<Void, Void, Void> {
        private NetworkErrorDialogFragment error;

        @Override
        protected Void doInBackground(Void... params) {
            int i = 0;
            while (!isOnline()) {
                if (i++ == 5) {
                    HttpHandler.setHost(sDefaultHost);
                    this.error.invalidate();
                    PreferenceHandler.saveHost();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
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
        }

        @Override
        protected void onPreExecute() {
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
