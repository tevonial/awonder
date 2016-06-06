package tevonial.awonder.library;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import tevonial.awonder.R;

public class SeekBarPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private SeekBar mSeekBar;
    private TextView mProgressText;

    public static SeekBarPreferenceDialogFragment newInstance(String key) {
        SeekBarPreferenceDialogFragment fragment = new SeekBarPreferenceDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_KEY, key);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_PLUS
                            || keyCode == KeyEvent.KEYCODE_EQUALS) {
                        mSeekBar.setProgress(mSeekBar.getProgress() + 1);
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_MINUS) {
                        mSeekBar.setProgress(mSeekBar.getProgress() - 1);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        // Don't do this in onCreate(): our target PreferenceFragment may not be ready yet.
        mSeekBar = getSeekBarPreference().getSeekBar();
        mSeekBar.setProgress(getSeekBarPreference().getProgress());

        ViewParent oldParent = mSeekBar.getParent();
        if (oldParent != view) {
            if (oldParent != null) {
                ((ViewGroup) oldParent).removeView(mSeekBar);
            }
            onAddSeekBarToDialogView(view, mSeekBar);
        }
    }

    private void onAddSeekBarToDialogView(View dialogView, SeekBar seekBar) {
        ViewGroup container = (ViewGroup) dialogView.findViewById(R.id.sbp_seekbar_container);

        if (container != null) {
            container.addView(seekBar, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) seekBar.getLayoutParams();
            params.addRule(RelativeLayout.LEFT_OF, R.id.seek_progress);
            params.addRule(RelativeLayout.TEXT_ALIGNMENT_GRAVITY, RelativeLayout.CENTER_VERTICAL);
            seekBar.setPadding(40, 20, 30, 20);
            seekBar.setMinimumHeight(seekBar.getHeight());

        }

        mProgressText = (TextView) dialogView.findViewById(R.id.seek_progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgressText.setText(String.valueOf(progress - 1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mProgressText.setText(String.valueOf(seekBar.getProgress() - 1));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int value = mSeekBar.getProgress();
            SeekBarPreference preference = getSeekBarPreference();
            preference.setProgress(value);


            if (preference.callChangeListener(value)) {

            }
        }
    }

    private SeekBarPreference getSeekBarPreference() {
        return (SeekBarPreference) getPreference();
    }
}
