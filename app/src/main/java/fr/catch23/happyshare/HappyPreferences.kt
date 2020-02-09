package fr.catch23.happyshare

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class HappyShareSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.happyshare_preferences, rootKey)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val usernamePreference = this.findPreference<EditTextPreference>("api_username")
        usernamePreference!!.setSummary(sharedPreferences.getString("api_username", getString(R.string.pref_api_username_summary)));
        usernamePreference!!.setOnBindEditTextListener { it.setSingleLine() }
        usernamePreference!!.setOnPreferenceChangeListener { _, newValue ->
            var summary = newValue.toString()
            summary = summary.replace("\n", "")
            sharedPreferences.edit().putString("api_username", summary).apply();
            if (summary.isNullOrEmpty()) {
                summary = getString(R.string.pref_api_username_summary)
            }
            usernamePreference.setSummary(summary)
            true
        }

        val urlPreference = this.findPreference<EditTextPreference>("api_root_url")
        urlPreference!!.setSummary(sharedPreferences.getString("api_root_url", getString(R.string.pref_api_rooturl_summary)));
        urlPreference!!.setOnBindEditTextListener { it.setSingleLine() }
        urlPreference!!.setOnPreferenceChangeListener { _, newValue ->
            var summary = newValue.toString()
            if (summary.isNullOrEmpty()) {
                summary = getString(R.string.pref_api_rooturl_summary)
            }
            urlPreference.setSummary(summary)
            true
        }

    }
}

class HappyShareSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preferences_layout);
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.preferences_frame, HappyShareSettingsFragment())
                .commit()
    }
}
