package tevonial.awonder.dialog;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import tevonial.awonder.R;
import tevonial.awonder.fragment.AnswerPollFragment;

public class AnswerPollDialogFragment extends DialogFragment {
    private int mPollMode = 1;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_answer_poll, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (mPollMode == 1) {
            ((RelativeLayout) view.findViewById(R.id.mode1)).setVisibility(View.VISIBLE);
            final SeekBar mSeekBar = (SeekBar) view.findViewById(R.id.seek);
            final Button mSendButton = (Button) view.findViewById(R.id.send);
            final TextView seekProgress = (TextView) view.findViewById(R.id.seek_progress);

            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekProgress.setText(String.valueOf(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            mSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AnswerPollFragment) getTargetFragment()).onInput(mSeekBar.getProgress(), null);
                    getDialog().dismiss();
                }
            });
        } else if (mPollMode == 2) {
            ((LinearLayout) view.findViewById(R.id.mode2)).setVisibility(View.VISIBLE);
            Button mYesButton = (Button) view.findViewById(R.id.button_yes);
            Button mNoButton = (Button) view.findViewById(R.id.button_no);

            mNoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AnswerPollFragment) getTargetFragment()).onInput(0, null);
                    getDialog().dismiss();
                }
            });

            mYesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((AnswerPollFragment) getTargetFragment()).onInput(1, null);
                    getDialog().dismiss();
                }
            });

        }

        return view;
    }

    public void setMode(int mode) {
        this.mPollMode = mode;
    }
}
