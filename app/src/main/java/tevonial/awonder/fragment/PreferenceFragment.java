package tevonial.awonder.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.AnswerPollDialogFragment;
import tevonial.awonder.dialog.DialogListener;
import tevonial.awonder.dialog.PollDialogFragment;
import tevonial.awonder.handler.HttpHandler;
import tevonial.awonder.library.SeekBarPreference;

public class PreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, DialogListener<Integer> {

    private SeekBarPreference mStatePreference;
    private EditTextPreference mHostPreference;
    private Preference mSelfRespondPreference;
    private String mScreenSize;

    private final String mStateKey = MainActivity.sContext.getString(R.string.pref_state_key),
                         mHostKey  = MainActivity.sContext.getString(R.string.pref_host_key);

    private final String mClickPreferenceKeys[] = {MainActivity.sContext.getString(R.string.pref_rem_history_key),
                                                   MainActivity.sContext.getString(R.string.pref_rem_uid_key),
                                                   MainActivity.sContext.getString(R.string.pref_self_respond_key)};

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(mStateKey)) {
                changeState(mStatePreference.getProgress() - 1);
            } else if (key.equals(mHostKey)) {
                String host = mHostPreference.getText();
                if (host.isEmpty()) {
                    host = HttpHandler.sDefaultHost;
                }
                if (!host.endsWith("/")) {
                    host += "/";
                }
                if (!host.contains("://")) {
                    host = "http://".concat(host);
                }
                HttpHandler.setHost(host);
                mHostPreference.setSummary(host);
                mHostPreference.setText(host);
            }
        }
    };

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity.sCurrentView = MainActivity.FRAGMENT_PREFERENCE;
        getActivity().setTitle(getString(R.string.preference_fragment_title));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);

        mStatePreference = (SeekBarPreference) findPreference(mStateKey);
        mStatePreference.setDialogTitle("Change State");
        mStatePreference.setSummary(String.valueOf(HttpHandler.getState()));
        mStatePreference.setProgress(HttpHandler.getState()+1);

        mHostPreference = (EditTextPreference) findPreference(mHostKey);
        mHostPreference.setSummary(HttpHandler.sHost);

        mSelfRespondPreference = findPreference(mClickPreferenceKeys[2]);
        mSelfRespondPreference.setEnabled(HttpHandler.getState() == -1);

        for (String key : mClickPreferenceKeys) {
            findPreference(key).setOnPreferenceClickListener(this);
        }

        initDetails();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (!SeekBarPreference.onDisplayPreferenceDialog(this, preference)) {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(mClickPreferenceKeys[0])) {

        } else if (key.equals(mClickPreferenceKeys[1])) {

        } else if (key.equals(mClickPreferenceKeys[2])) {
            AnswerPollDialogFragment mDialog = new AnswerPollDialogFragment();
            mDialog.setTargetFragment(this, 0);
            mDialog.setMode(PollFragment.sPollMode);
            mDialog.show(getActivity().getSupportFragmentManager(), "fragment_dialog_poll_answer");
        }

        return true;
    }

    public void setScreenSize(int w, int h) {
        mScreenSize = String.valueOf(w) + "x" + String.valueOf(h);
    }

    private void initDetails() {
        findPreference(MainActivity.sContext.getString(R.string.pref_det_default_host_key)).setSummary(HttpHandler.sDefaultHost);
        findPreference(MainActivity.sContext.getString(R.string.pref_det_uid_key)).setSummary(HttpHandler.getUid());
        findPreference(MainActivity.sContext.getString(R.string.pref_det_screen_key)).setSummary(mScreenSize);
    }

    private void changeState(final int state) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("uid", HttpHandler.getUid());
            obj.put("state", String.valueOf(state));
        } catch (JSONException e) {}

        HttpHandler.postJson(HttpHandler.POST_STATE, new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                String text = (success) ? "Server updated" : "Server update error";
                Toast.makeText(MainActivity.sContext, text, Toast.LENGTH_SHORT).show();
                if (success) {
                    mStatePreference.setSummary(String.valueOf(state));
                    mSelfRespondPreference.setEnabled(state == -1);
                }
            }
        }, obj);

    }

    @Override
    public void onInput(Integer input, Integer mode) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("p_uid", HttpHandler.getUid());
            obj.put("a", String.valueOf(input));
        } catch (JSONException e) {}

        HttpHandler.postJson(HttpHandler.SELF_RESPOND, new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                String text = (success) ? "Respond Success" : "Respond error";
                Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
            }
        }, obj);
    }
}

