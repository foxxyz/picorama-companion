package com.foxxyz.chronovision

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        val url = preferenceScreen.findPreference<EditTextPreference>("serverURL")
        // Validate URL field
        url!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val valid = Patterns.WEB_URL.matcher(newValue.toString()).matches()
            if (!valid) Toast.makeText(this.activity, "Invalid URL! Please try again.", Toast.LENGTH_SHORT).show()
            valid
        }
    }
}