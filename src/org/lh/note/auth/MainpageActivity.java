package org.lh.note.auth;

import org.lh.note.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainpageActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_mainpage);
		
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
	}
}
