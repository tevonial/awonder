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
import tevonial.awonder.handler.HttpHandler;
import tevonial.awonder.library.SeekBarPreference;

public class PreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

    private SeekBarPreference mStatePreference;
    private EditTextPreference mHostPreference;
    private String mScreenSize;

    private final String mStateKey = MainActivity.sContext.getString(R.string.pref_state_key),
                         mHostKey  = MainActivity.sContext.getString(R.string.pref_host_key);

    private final String mClickPreferenceKeys[] = {MainActivity.sContext.getString(R.string.pref_rem_history_key),
                                                   MainActivity.sContext.getString(R.string.pref_rem_uid_key)};

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d("EW", "onSharedPreferenceChanged: " + key);
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
                    host = "http://" + host;
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
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        mStatePreference = (SeekBarPreference) findPreference(mStateKey);
        mStatePreference.setDialogTitle("Change State");
        mStatePreference.setSummary(String.valueOf(HttpHandler.getState()));
        mStatePreference.setProgress(HttpHandler.getState()+1);

        mHostPreference = (EditTextPreference) findPreference(mHostKey);
        mHostPreference.setSummary(HttpHandler.sHost);

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
            Toast.makeText(getContext(), "Delete history", Toast.LENGTH_SHORT).show();
        } else if (key.equals(mClickPreferenceKeys[1])) {
            Toast.makeText(getContext(), "Delete Uid", Toast.LENGTH_SHORT).show();
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
                }
            }
        }, obj);

    }
}
