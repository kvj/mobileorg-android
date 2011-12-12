package com.matburt.mobileorg.Capture;

import org.kvj.bravo7.SuperActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.MobileOrgApplication;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;

public class Capture extends SuperActivity<App, DataController, DataService> implements OnClickListener
{
    private EditText orgEditDisplay;
    private Button saveButton;
    private Button advancedButton;
    private boolean editMode = false;
    private String id = null;
    private String editType = null;
    private String srcText = null;
    private String nodeTitle = null;
    private CreateEditNote noteCreator = null;
    private MobileOrgApplication appinst;
    public static final String LT = "MobileOrg";

    public Capture() {
		super(DataService.class);
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpleedittext);
        this.saveButton = (Button)this.findViewById(R.id.captureSave);
        this.advancedButton = (Button)this.findViewById(R.id.captureAdvanced);
        this.orgEditDisplay = (EditText)this.findViewById(R.id.orgEditTxt);
        this.saveButton.setOnClickListener(this);
        this.advancedButton.setOnClickListener(this);
        this.populateDisplay();
    }
    
    @Override
    public void onController(DataController controller) {
    	super.onController(controller);
    	noteCreator = new CreateEditNote(this, controller);
    }

    public boolean onSave() {
        if (this.orgEditDisplay.getText().toString().length() > 0) {
            if (this.editType == null) {
                this.noteCreator.writeNewNote(this.orgEditDisplay.getText().toString());
            }
        }
        Intent result = new Intent();
        result.putExtra("text", this.orgEditDisplay.getText().toString());
        this.setResult(RESULT_OK, result);
        this.finish();
        return true;
    }

    public void onClick(View v) {
        if (v == this.advancedButton) {
            Log.i(LT, "Advanced");
            Intent advancedIntent = new Intent(this, ViewNodeDetailsActivity.class);
            advancedIntent.putExtra("actionMode", "create");
            startActivity(advancedIntent);
            this.finish();
        }
        else if (v == this.saveButton) {
            if (!this.onSave()) {
                Log.e(LT, "Failed to save file");
            }
        }
    }

    public void populateDisplay() {
        Intent txtIntent = getIntent();
        String actionMode =  txtIntent.getStringExtra("actionMode");
        if (actionMode == null) {
            this.advancedButton.setVisibility(View.GONE);
        }
        this.srcText = txtIntent.getStringExtra("txtValue");
		if((this.srcText == null || this.srcText.length() == 0) &&
		   txtIntent != null) {
			String subject = txtIntent.getStringExtra("android.intent.extra.SUBJECT");
			String text = txtIntent.getStringExtra("android.intent.extra.TEXT");

			if(subject == null) {
				subject = "";
			} else {
				subject += "\n";
			}

			if(text == null) {
				text = "";
			}

			if(text.startsWith("http")) {
				this.srcText = "[["+text.trim()+"]["+subject.trim()+"]]";
			} else {
				this.srcText = subject + text;
			}
		}
        this.id = txtIntent.getStringExtra("nodeId");
        this.editType = txtIntent.getStringExtra("editType");
        this.orgEditDisplay.setText(this.srcText);
        this.nodeTitle = txtIntent.getStringExtra("nodeTitle");
        this.appinst = (MobileOrgApplication)this.getApplication();
    }
}
