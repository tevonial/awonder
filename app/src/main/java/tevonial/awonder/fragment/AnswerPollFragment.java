package tevonial.awonder.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.LinearLayout.LayoutParams;

import org.json.JSONException;
import org.json.JSONObject;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.DialogListener;
import tevonial.awonder.dialog.AnswerPollDialogFragment;
import tevonial.awonder.handler.HttpHandler;

public class AnswerPollFragment extends Fragment implements DialogListener<Integer> {
    private AnswerPollDialogFragment mDialog;
    private TextSwitcher mTextSwitcher;
    private ProgressBar mProgress;
    private LinearLayout mDoneView;
    private Button mButton;
    private final int mQuota = 5;

    private String mPollPtr;
    private String mPollStr;
    private int mPollMode;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_answer_poll, null);
        MainActivity.sCurrentView = MainActivity.FRAGMENT_ANSWER_POLL;
        getActivity().setTitle(getString(R.string.answer_poll_fragment_title));

        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mButton = (Button) view.findViewById(R.id.confirm);
        mDoneView = (LinearLayout) view.findViewById(R.id.done);
        mTextSwitcher = (TextSwitcher) view.findViewById(R.id.text_switcher);

        int state = HttpHandler.getState();

        LinearLayout labels = (LinearLayout) view.findViewById(R.id.labels);
        for (int i = 0; i< mQuota; i++) {
            TextView tick = new TextView(getContext());
            tick.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
            tick.setWidth(0);
            tick.setText(String.valueOf(i+1));
            tick.setTypeface(null, Typeface.BOLD);
            tick.setGravity(Gravity.CENTER);

            labels.addView(tick);
        }

        if (state > 0) {
            final int done = mQuota - state;
            mProgress.post(new Runnable() {
                @Override
                public void run() {
                    mProgress.setProgress((int) (((double) done * 100) / (double) mQuota));
                }
            });
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog = new AnswerPollDialogFragment();
                    mDialog.setTargetFragment(AnswerPollFragment.this, 0);
                    mDialog.setMode(mPollMode);
                    mDialog.show(getActivity().getSupportFragmentManager(), "fragment_dialog_poll_answer");
                }
            });
            mTextSwitcher.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    refresh(false);
                }
            });
            mTextSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
                public View makeView() {
                    TextView textView = new TextView(getContext());
                    textView.setGravity(Gravity.CENTER);
                    textView.setPadding(10, 10, 10, 10);
                    textView.setTextSize(40);
                    return textView;
                }
            });

            mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.xscale_up));
            mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.xscale_down));
            if (mPollStr == null) {
                refresh(false);
            } else {
                mTextSwitcher.setCurrentText(mPollStr);
            }
        } else {
            onDone();
        }

        return view;
    }

    @Override
    public void onInput(Integer input, Integer mode) {
        sendAnswer(input);
    }

    private void onDone() {
        if (HttpHandler.getState() <= 0) {
            mProgress.post(new Runnable() {
                @Override
                public void run() {
                    mProgress.setProgress(100);
                }
            });
            mTextSwitcher.setVisibility(View.INVISIBLE);
            mDoneView.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.INVISIBLE);
            /*mButton.setText(getString(R.string.return_button));
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).switchView(0);
                }
            });*/
            mPollStr = mPollPtr = null;
        }
    }

    private void refresh(boolean getStateFirst) {
        MainActivity.sLoading.setVisibility(ProgressBar.VISIBLE);

        final HttpHandler.RequestHandler requestPollHandler = new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                if (success) {
                    mPollPtr = s[0];
                    mPollStr = s[1];
                    mPollMode = Integer.valueOf(s[2]);
                    mTextSwitcher.setText(mPollStr);
                }
                MainActivity.sLoading.setVisibility(ProgressBar.INVISIBLE);
            }
        };

        final HttpHandler.RequestHandler requestStateHandler = new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                if (success) {
                    int state = Integer.valueOf(s[0]);
                    HttpHandler.setState(state);

                    if (state > 0) {
                        int done = mQuota - state;
                        mProgress.setProgress((int) (((double) done * 100) / (double) mQuota));
                        HttpHandler.getJson(HttpHandler.GET_POLL, requestPollHandler); return;
                    } else {
                        onDone();
                    }
                }
                MainActivity.sLoading.setVisibility(ProgressBar.INVISIBLE);
            }
        };

        if (getStateFirst) {
            HttpHandler.getJson(HttpHandler.GET_STATE, requestStateHandler);
        } else {
            HttpHandler.getJson(HttpHandler.GET_POLL, requestPollHandler);
        }
    }

    private void sendAnswer(int answer) {
        MainActivity.sLoading.setVisibility(View.VISIBLE);
        JSONObject obj = new JSONObject();
        try {
            obj.put("uid", HttpHandler.getUid());
            obj.put("p_uid", mPollPtr);
            obj.put("a", String.valueOf(answer));
        } catch (JSONException e) {}

        HttpHandler.postJson(HttpHandler.POST_ANSWER, new HttpHandler.RequestHandler() {
                    @Override
                    public void onResponse(boolean success, String[] s) {
                        if (success) {
                            refresh(true);
                        } else {
                            MainActivity.sLoading.setVisibility(View.INVISIBLE);
                        }
                    }
                }, obj);
    }
}
