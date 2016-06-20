package tevonial.awonder.handler;

import android.content.Context;
import android.webkit.URLUtil;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

public class VolleyRequestHandler {
    private static VolleyRequestHandler mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;
    private Request mLastRequest;

    private VolleyRequestHandler(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized VolleyRequestHandler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleyRequestHandler(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        mLastRequest = req;
        if (URLUtil.isValidUrl(req.getUrl())) {
            getRequestQueue().add(req);
        } else {
            req.deliverError(new VolleyError());
        }
    }

    public void retryLastRequest() {
        getRequestQueue().add(mLastRequest);
    }
}