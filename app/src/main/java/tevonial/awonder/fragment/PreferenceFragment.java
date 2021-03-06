package tevonial.awonder.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import tevonial.awonder.MainActivity;
import tevonial.awonder.R;
import tevonial.awonder.dialog.AnswerPollDialogFragment;
import tevonial.awonder.dialog.DialogListener;
import tevonial.awonder.handler.HttpHandler;
import tevonial.awonder.handler.PreferenceHandler;
import tevonial.awonder.library.SeekBarPreference;

public class PreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, DialogListener<Integer> {

    private SeekBarPreference mStatePreference;
    private EditTextPreference mHostPreference;
    private Preference mUidPreference;

    private final String mClickPreferenceKeys[] = { MainActivity.sContext.getString(R.string.pref_self_respond_key),
                                                    MainActivity.sContext.getString(R.string.pref_rem_host_key),
                                                    MainActivity.sContext.getString(R.string.pref_rem_uid_key),
                                                    MainActivity.sContext.getString(R.string.pref_gen_uid_key) };

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceHandler.KEY_STATE)) {
                changeState(mStatePreference.getProgress() - 1);
            } else if (key.equals(PreferenceHandler.KEY_HOST)) {
                String host = mHostPreference.getText().toLowerCase();
                if (!host.isEmpty()) {
                    if (!host.startsWith("http://")) {
                        host = "http://" + host;
                    }
                }

                HttpHandler.setHost(host);
                mHostPreference.setSummary(host);
            } else if (key.equals(PreferenceHandler.KEY_UID)) {
                mUidPreference.setSummary(HttpHandler.getUid());

                if (HttpHandler.hasUid()) {
                    HttpHandler.requestGetJson(HttpHandler.GET_STATE, new HttpHandler.RequestHandler() {
                        @Override
                        public void onResponse(boolean success, String[] s) {
                            if (success) {
                                HttpHandler.setState(Integer.valueOf(s[0]));
                                mStatePreference.setSummary(s[0]);
                            }
                        }
                    });
                }
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

        mStatePreference = (SeekBarPreference) findPreference(PreferenceHandler.KEY_STATE);
        mStatePreference.setDialogTitle("Change State");
        mStatePreference.setSummary(String.valueOf(HttpHandler.getState()));
        mStatePreference.setProgress(HttpHandler.getState()+1);

        mHostPreference = (EditTextPreference) findPreference(PreferenceHandler.KEY_HOST);
        mHostPreference.setSummary(HttpHandler.getHost());

        mUidPreference = findPreference(PreferenceHandler.KEY_UID);
        mUidPreference.setSummary(HttpHandler.getUid());

        for (String key : mClickPreferenceKeys) {
            findPreference(key).setOnPreferenceClickListener(this);
        }
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

        if (key.equals(mClickPreferenceKeys[0])) {          //Self Respond
            AnswerPollDialogFragment mDialog = new AnswerPollDialogFragment();
            mDialog.setTargetFragment(this, 0);
            mDialog.setMode(PollFragment.sPollMode);
            mDialog.show(getActivity().getSupportFragmentManager(), "fragment_dialog_poll_answer");
        } else if (key.equals(mClickPreferenceKeys[1])) {   //Clear Host
            mHostPreference.setText("");
            PreferenceHandler.removeHost();
        } else if (key.equals(mClickPreferenceKeys[2])) {   //Clear UID
            HttpHandler.setUid("");
            PreferenceHandler.removeUid();
        } else if (key.equals(mClickPreferenceKeys[3])) {   //Fetch UID
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    PreferenceHandler.initUid();
                }
            })).start();
        }

        return true;
    }

    private void changeState(final int state) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("uid", HttpHandler.getUid());
            obj.put("state", String.valueOf(state));
        } catch (JSONException e) {}

        HttpHandler.requestPostJson(HttpHandler.POST_STATE, new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                String text = (success) ? "Server updated" : "Server update error";
                Toast.makeText(MainActivity.sContext, text, Toast.LENGTH_SHORT).show();
                if (success) {
                    mStatePreference.setSummary(String.valueOf(state));
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

        HttpHandler.requestPostJson(HttpHandler.SELF_RESPOND, new HttpHandler.RequestHandler() {
            @Override
            public void onResponse(boolean success, String[] s) {
                String text = (success) ? "Respond Success" : "Respond error";
                Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
            }
        }, obj);
    }
}

