package com.google.firebase.jfpinedap.kibun;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Presents a set of application settings.
 */
public class ActivitySettings extends AppCompatActivity {
    private final static String TAG = ActivitySettings.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_view, new SettingsActivity()).commit();
    }
}
