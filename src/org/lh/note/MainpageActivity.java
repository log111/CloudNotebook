package org.lh.note;

import org.lh.note.auth.LoginActivity;
import org.lh.note.auth.SignupActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.baidu.mcs.Mcs;
import com.baidu.mcs.callback.UserCallback;
import com.baidu.mcs.user.User;

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
						
						/*
						User user = new User(getApplicationContext());
						user.loginAsync(User.LoginType.BAIDU_PASSPORT, new UserCallback(){
						 	@Override
						 	public void onSuccess(User user){
						 		finish();
						 		
						 		Intent intent = new Intent();
								intent.setClassName("org.lh.note", "org.lh.note.NotesList");
								startActivity(intent);
								
						 	}
						 	
						 	@Override
						 	public void onFailure(int code, String msg){
						 		//TODO
						 	}
						 });
						 */
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
