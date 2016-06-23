package tevonial.awonder.handler;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.library.ServerStatusRequest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.design.widget.Snackbar;
import android.util.Patterns;
import android.view.View;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HttpHandler {
    private static String mUid;
    private static int mState;
    private static String mHost;

    private static Snackbar mErrorSnackBar;

    private static boolean mNetConnected;
    private static boolean mNetValidUrl;
    private static boolean mNetOnline = true;

                                //Request Type                 url                  uid     param     expected return keys
    public static RequestType   GET_GEN_UID =  new RequestType("get_gen_uid.php",   false,  "",       "uid"),
                                GET_MY_POLL =  new RequestType("get_state.php",     true,   "poll",   "poll", "mode"),
                                GET_STATE =    new RequestType("get_state.php",     true,   "state",  "state"),
                                GET_COUNT =    new RequestType("get_state.php",     true,   "count",  "count"),
                                GET_POLL =     new RequestType("get_poll.php",      false,  "",       "p_uid", "p_poll", "p_mode"),
                                GET_RESULT =   new RequestType("get_results.php",   true,   "",       "results"),

                                POST_POLL =    new RequestType("post_poll.php",     false,  ""),
                                POST_ANSWER =  new RequestType("post_response.php", false,  ""),
                                POST_STATE =   new RequestType("post_state.php",    false,  ""),
                                SELF_RESPOND = new RequestType("post_self.php",     false,  "");


    public static void setHost(String host) {
        mHost = host;
        requestGetServerStatus();
    }

    public static String getHost() {
        return mHost;
    }

    public static void setUid(String uid) {
        mUid = uid;
    }

    public static String getUid() {
        return mUid;
    }

    public static void setState(int state) {
        mState = state;
    }

    public static int getState() {
        return mState;
    }

    public static boolean hasUid() {
        try {
            return (!mUid.isEmpty());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean hasHost() {
        try {
            return (!mHost.isEmpty());
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean isOnline() {
        return mNetOnline;
    }

    public interface RequestHandler {
        void onResponse(boolean success, String[] s);
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
            String url = mHost;
            if (!url.endsWith("/")) {
                url += "/";
            }

            url += path;

            String uidParam = (requireUid) ? "uid=".concat(mUid) : "";

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

    private static void requestGetServerStatus() {
        if (mHost.isEmpty()) return;
        ServerStatusRequest statusRequest = new ServerStatusRequest(mHost, new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                if (!mNetOnline) {
                    if ((int)response == 200) {
                        mNetOnline = true;
                        mErrorSnackBar.dismiss();
                    } else {
                        onError(null);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onError(null);
            }
        });
        VolleyRequestHandler.getInstance(MainActivity.sContext).addToRequestQueue(statusRequest);
    }

    public static void requestPostJson(final RequestType requestType, final RequestHandler rh, final JSONObject body) {
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.POST, requestType.getUrl(), body, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!mNetOnline) {
                            mNetOnline = true;
                            mErrorSnackBar.dismiss();
                        }
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

    public static void requestGetJson(final RequestType requestType, final RequestHandler rh) {
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.GET, requestType.getUrl(), null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (!mNetOnline) {
                            mNetOnline = true;
                            mErrorSnackBar.dismiss();
                        }
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

    private static void onError(RequestHandler rh) {
        if (rh != null) {
            rh.onResponse(false, null);
        }

        ConnectivityManager cm = (ConnectivityManager) MainActivity.sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetConnected = (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting());
        mNetValidUrl = Patterns.WEB_URL.matcher(mHost).matches();
        mNetOnline = false;

        MainActivity.sUiHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.sLoading.setVisibility(View.INVISIBLE);

                if (mNetConnected) {
                    if (mNetValidUrl) {
                        mErrorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_1_message), Snackbar.LENGTH_INDEFINITE);
                    } else {
                        mErrorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_3_message), Snackbar.LENGTH_INDEFINITE);
                    }
                    mErrorSnackBar.setAction(MainActivity.sContext.getString(R.string.net_error_edit), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.switchView(5);
                        }
                    });
                } else {
                    mErrorSnackBar = Snackbar.make(MainActivity.sRootView, MainActivity.sContext.getString(R.string.net_error_2_message), Snackbar.LENGTH_INDEFINITE);
                    mErrorSnackBar.setAction(MainActivity.sContext.getString(R.string.net_error_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            VolleyRequestHandler.getInstance(MainActivity.sContext).retryLastRequest();
                        }
                    });
                }

                mErrorSnackBar.show();
            }
        });
    }
}
