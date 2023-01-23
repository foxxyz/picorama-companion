package com.foxxyz.picoramacompanion

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        for(i in 1..3) {
            val url = preferenceScreen.findPreference<EditTextPreference>("serverURL$i")
            // Validate URL field
            url!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val valid = Patterns.WEB_URL.matcher(newValue.toString()).matches()
                if (!valid) Toast.makeText(this.activity, "Invalid URL! Please try again.", Toast.LENGTH_SHORT).show()
                valid
            }
        }
    }
}