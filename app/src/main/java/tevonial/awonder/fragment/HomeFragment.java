package tevonial.awonder.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.PollDialogFragment;
import tevonial.awonder.handler.HttpHandler;
import tevonial.awonder.handler.PreferenceHandler;

public class HomeFragment extends Fragment implements HttpHandler.RequestHandler  {
    private View mMainView;
    private Button mAskButton, mAnswerButton;
    private int mStatus;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (MainActivity.allowCreateHome()) {
            View view = inflater.inflate(R.layout.fragment_home, null);
            MainActivity.sCurrentView = MainActivity.FRAGMENT_HOME;
            getActivity().setTitle(getString(R.string.app_name));

            mMainView = view.findViewById(R.id.main);
            mAskButton = (Button) view.findViewById(R.id.ask);
            mAnswerButton = (Button) view.findViewById(R.id.answer);

            mAskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mStatus == 0) {
                        (new PollDialogFragment()).show(getActivity().getSupportFragmentManager(), "fragment_dialog_poll");
                    } else {
                        MainActivity.switchView(2);
                    }
                }
            });

            mAnswerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.switchView(1);
                }
            });

            MainActivity.sLoading.setVisibility(View.VISIBLE);

            HttpHandler.requestGetJson(HttpHandler.GET_STATE, this);

            return view;
        } else {
            return null;
        }
    }

    @Override
    public void onResponse(boolean success, String[] s) {
        if (success) {
            mStatus = Integer.valueOf(s[0]);
            HttpHandler.setState(mStatus);

            if (mStatus > 0) {
                mAskButton.setEnabled(false);
            } else if (mStatus == 0) {
                mAskButton.setEnabled(true);
            } else if (mStatus == -1) {
                mAskButton.setEnabled(true);
                mAskButton.setText("Current Poll");
            }
            mMainView.setVisibility(View.VISIBLE);
        }

        MainActivity.sLoading.setVisibility(ProgressBar.INVISIBLE);
    }
}
