package tevonial.awonder.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.fragment.PollFragment;

public class PollDialogFragment extends DialogFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_poll, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final EditText mEditPoll = (EditText) view.findViewById(R.id.edit);
        final RadioGroup mRadioGroup = (RadioGroup) view.findViewById(R.id.rg);

        ((Button) view.findViewById(R.id.send)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mEditPoll.getText().toString().isEmpty()) {
                    getDialog().dismiss();
                    //((MainActivity) getActivity()).switchView(2);
                    int mode = mRadioGroup.getCheckedRadioButtonId();
                    if      (mode == R.id.radio1) { mode = 1; }
                    else if (mode == R.id.radio2) { mode = 2; }

                    ((PollFragment) MainActivity.pollFragment).onInput(mEditPoll.getText().toString(), mode);
                }
            }
        });

        return view;
    }
}
