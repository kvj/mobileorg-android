package com.matburt.mobileorg;
import android.content.Context;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Typeface;
import java.util.HashMap;

public class SynchronizerPreferences extends Preference {
    
    public static HashMap<String,Intent> syncIntents = new HashMap<String,Intent>();
    public SynchronizerPreferences(Context context) {
        super(context);
    }
    
    public SynchronizerPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SynchronizerPreferences(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected View onCreateView(ViewGroup parent){
        
        LinearLayout layout = new LinearLayout(getContext());
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                              LinearLayout.LayoutParams.WRAP_CONTENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.gravity = Gravity.LEFT;
        params1.weight  = 1.0f;
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                            80,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
        params2.gravity = Gravity.RIGHT;
        LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(
                            30,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
        params3.gravity = Gravity.CENTER;
        layout.setPadding(15, 10, 10, 10);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView view = new TextView(getContext());
        view.setText("Configure Synchronizer Settings...");
        view.setTextSize(18);
        view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        view.setGravity(Gravity.LEFT);
        view.setLayoutParams(params1);

        //NOTE: Need a way to dynamically pick a preferences activity based on the
        //synchro mode selected, replace with a plugin architecture
        this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String synchroMode = appSettings.getString("syncSource","");
                    if(syncIntents.containsKey(synchroMode))
                    {
                    
                        getContext().startActivity(syncIntents.get(synchroMode));
                    }
                    else {
                        //throw new ReportableError(R.string.error_synchronizer_type_unknown,
                        //                          synchroMode);
                    }
                    return true;
                }
            });

        layout.addView(view);
        layout.setId(android.R.id.widget_frame);
        return layout; 
    }    
}
