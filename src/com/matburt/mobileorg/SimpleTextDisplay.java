package com.matburt.mobileorg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

public class SimpleTextDisplay extends Activity
{
    private TextView orgDisplay;
    public static final String LT = "MobileOrg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpletext);
        this.orgDisplay = (TextView)this.findViewById(R.id.orgTxt);
        this.poplateDisplay();
    }

    public void poplateDisplay() {
    	SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Intent txtIntent = getIntent();
        String srcText = txtIntent.getStringExtra("txtValue");
        int fontSize = 20;
        try {
			fontSize = Integer.parseInt(appPrefs.getString("docFontSize", Integer.toString(fontSize)));
		} catch (Exception e) {
		}
        this.orgDisplay.setTextSize(fontSize);
        this.orgDisplay.setText(srcText);
    }
}