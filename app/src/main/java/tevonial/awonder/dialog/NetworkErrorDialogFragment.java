package tevonial.awonder.dialog;

import android.os.Bundle;
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

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_network_error, container);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mHostTextView = (TextView) view.findViewById(R.id.host);
        invalidate();

        return view;
    }

    public void invalidate() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String host = (HttpHandler.sUseDefaultHost ? HttpHandler.sDefaultHost : HttpHandler.sHost);
                mHostTextView.setText(host);
            }
        });
    }
}
