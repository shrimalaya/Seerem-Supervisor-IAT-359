package com.example.supervisor_seerem.UI;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.supervisor_seerem.R;

public class UIPreferencesActivity extends AppCompatActivity {

    public static Intent launchUIPreferencesIntent(Context context) {
        Intent uiPrefsIntent = new Intent(context, UIPreferencesActivity.class);
        return uiPrefsIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_u_i_preferences);
    }
}