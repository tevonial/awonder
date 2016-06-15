package tevonial.awonder.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.JavaUnicodeEscaper;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.json.JSONException;
import org.json.JSONObject;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.DialogListener;
import tevonial.awonder.fragment.HistoryFragment.HistoryItem;
import tevonial.awonder.handler.HttpHandler;

public class PollFragment extends Fragment implements DialogListener<String> {
    private ViewGroup mPollView;
    private TextView mPoll, mResponseCount;
    private boolean mStoppable = false;

    public static String sPollStr;
    public static int sPollMode;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poll, null);
        MainActivity.sCurrentView = MainActivity.FRAGMENT_POLL;
        getActivity().setTitle(getString(R.string.poll_fragment_title));

        Button mStopButton = (Button) view.findViewById(R.id.stop);
        mPollView = (ViewGroup) view.findViewById(R.id.current_poll);
        mResponseCount = (TextView) view.findViewById(R.id.response_count);
        mPoll = (TextView) view.findViewById(R.id.mypoll);

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStoppable) {
                    getResults();
                }
            }
        });

        getState();

        return view;
    }

    @Override
    public void onInput(String input, Integer mode) {
        sPollMode = mode;
        sPollStr = input;
        sendPoll();
    }

    private void getState() {
        MainActivity.sLoading.setVisibility(View.VISIBLE);
        final HttpHandler.RequestHandler requestStateHandler = new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                if (!success) {
                    MainActivity.switchView(0); return;
                }
                int mStatus = Integer.valueOf(s[0]);
                if (mStatus < 0) {
                    MainActivity.sLoading.setVisibility(ProgressBar.INVISIBLE);
                    mPollView.setVisibility(View.VISIBLE);

                    HttpHandler.getJson(HttpHandler.GET_MY_POLL, new HttpHandler.RequestHandler() {
                                @Override
                                public void onResponse(boolean success, String[] s) {
                                    PollFragment.sPollStr = s[0];
                                    PollFragment.sPollMode = Integer.valueOf(s[1]);
                                    mPoll.setText(sPollStr);
                                }
                            });

                    (new ResponseCountTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        };

        HttpHandler.getJson(HttpHandler.GET_STATE, requestStateHandler);
    }

    private void getResults() {
        MainActivity.sLoading.setVisibility(ProgressBar.VISIBLE);

        final HttpHandler.RequestHandler requestResultsHandler = new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                if (!success) { return; }

                String a[] = s[0].split(",");
                int total = 0;
                double avg = 0;
                int sums[] = new int[11];
                for (String b:a) {
                    try {
                        int value = Integer.valueOf(b);
                        if (value >= 0 && value <= 10) {
                            sums[value]++;
                            avg += value;
                            total++;
                        }
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {}
                }
                avg /= (double)total;
                avg = Math.round(avg*100.0) / 100.0;

                int ptr = HistoryFragment.addItem(new HistoryItem(sPollMode, sPollStr, sums, avg, total));
                ResultsFragment.setPointer(ptr);
                MainActivity.switchView(3);
            }
        };

        HttpHandler.getJson(HttpHandler.GET_RESULT, requestResultsHandler);
    }

    private void sendPoll() {
        CharSequenceTranslator jsonTranslator =
                new AggregateTranslator(
                        new LookupTranslator(
                                new String[][] {
                                        {"'", "\\'"},
                                        {"\"", "\\\""},
                                        {"\\", "\\\\"},
                                        {"/", "\\/"}
                                }),
                        new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE()),
                        JavaUnicodeEscaper.outsideOf(32, 0x7f)
                );
        sPollStr = jsonTranslator.translate(sPollStr);

        JSONObject obj = new JSONObject();
        try {
            obj.put("uid", HttpHandler.getUid());
            obj.put("poll", sPollStr);
            obj.put("mode", sPollMode);
        } catch (JSONException e) {}

        HttpHandler.postJson(HttpHandler.POST_POLL, new HttpHandler.RequestHandler() {
                    @Override
                    public void onResponse(boolean success, String[] s) {
                        if (success) {
                            MainActivity.switchView(2);
                        }
                    }
                }, obj);
    }

    private class ResponseCountTask extends AsyncTask<Void, Void, Void> {
        Integer count = 0; boolean change = false;

        final HttpHandler.RequestHandler requestCountHandler = new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                if (success) {
                    int response = Integer.valueOf(s[0]);
                    if (count != response) {
                        count = response;
                        change = true;
                    }
                }
            }
        };

        @Override
        protected Void doInBackground(Void... v) {
            while (MainActivity.sCurrentView == 2) {
                try {
                    HttpHandler.getJson(HttpHandler.GET_COUNT, requestCountHandler);
                    for (int i=0; i<5; i++) {
                        Thread.sleep(1000);
                        if (change) {
                            publishProgress(); change = false;
                        }
                    }

                } catch (InterruptedException e) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {}

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... v) {
            if (count > 0) { mStoppable = true; }
            mResponseCount.setText(count.toString());
        }
    }
}
