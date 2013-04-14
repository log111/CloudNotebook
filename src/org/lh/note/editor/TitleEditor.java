/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lh.note.editor;

import org.lh.note.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * This Activity allows the user to edit a note's title. It displays a floating window
 * containing an EditText.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler}
 * or {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class TitleEditor extends Activity {
	private static final String TAG = "TitleEditor";

    // An EditText object for preserving the edited title.
    private EditText mText;
    private String originalTitle = null;
    private int itemPos = -1;

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_editor);
        mText = (EditText) this.findViewById(R.id.title);
        
        if (savedInstanceState != null) {
            originalTitle = savedInstanceState.getString("title");
        }
        
        itemPos = getIntent().getIntExtra("pos", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("title", mText.getText().toString());
    }
    
    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     *
     * Displays the current title for the selected note.
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        if(originalTitle != null){
        	mText.setText(originalTitle);
        }
    }

    /**
     * This method is called when the Activity loses focus.
     *
     * For Activity objects that edit information, onPause() may be the one place where changes are
     * saved. The Android application model is predicated on the idea that "save" and "exit" aren't
     * required actions. When users navigate away from an Activity, they shouldn't have to go back
     * to it to complete their work. The act of going away should save everything and leave the
     * Activity in a state where Android can destroy it if necessary.
     *
     * Updates the note with the text currently in the text box.
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        originalTitle = mText.getText().toString();
    }

    public void onClickOk(View v) {
    	Log.d(TAG, "onClick");
    	
    	setResult(RESULT_OK, new Intent()
    		.putExtra("title", mText.getText().toString())
    		.putExtra("pos", itemPos)
    		);
        finish();
    }
}
