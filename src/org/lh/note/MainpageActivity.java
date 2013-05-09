package org.lh.note;

import org.lh.note.auth.LoginActivity;
import org.lh.note.auth.SignupActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.baidu.mcs.Mcs;

public class MainpageActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_mainpage);
		
		//The following buttons both send an intent to start NoteList.
		findViewById(R.id.to_login_button).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent();
						i.setClass(MainpageActivity.this, LoginActivity.class);
						MainpageActivity.this.startActivity(i);
					}
				});
		findViewById(R.id.to_sign_up_button).setOnClickListener(
				new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent i = new Intent();
						i.setClass(MainpageActivity.this, SignupActivity.class);
						MainpageActivity.this.startActivity(i);
					}
				});

		Mcs.init(getApplicationContext(), getString(R.string.app_id), getString(R.string.app_key));
	}
}
