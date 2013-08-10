package com.andrewdutcher.indexcards;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class MainSettings extends PreferenceActivity {
	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		addPreferencesFromResource(R.layout.activity_settings);
	}
}
