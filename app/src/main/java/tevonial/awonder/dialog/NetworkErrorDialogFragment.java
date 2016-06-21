package tevonial.awonder.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import tevonial.awonder.R;
import tevonial.awonder.handler.HttpHandler;

public class NetworkErrorDialogFragment extends DialogFragment {
    private TextView mHostTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_network_error, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mHostTextView = (TextView) view.findViewById(R.id.host);
        mHostTextView.setText(HttpHandler.getHost());

        return view;
    }
}
