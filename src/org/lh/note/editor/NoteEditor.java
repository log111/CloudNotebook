package org.lh.note.editor;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.lh.note.R;
import org.lh.note.util.Constants;
import org.lh.note.util.DownloadNoteTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.baidu.mcs.File;
import com.baidu.mcs.callback.FileDeleteCallback;
import com.baidu.mcs.callback.FileUploadCallback;

public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";

    // A label for the saved state of the activity
    private static final String ORIGINAL_CONTENT = "origContent";
    
    private static final int EDIT_TITLE_REQUEST = 0;

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant
    private static final int STATE_INSERT = 0;//creating a new note
    private static final int STATE_EDIT = 1; //editing a note

    // Global mutable variables
    private int mState;;
    private String mTitle;
    private EditText mText;
    private String mOriginalContent;
    private int mPosition = -1;
    
    private String escapedLineBreak(String src){
    	return src.replaceAll("\n", "\\\\n");
    }
    
    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);

                /*
                 * Draws a line in the background from the left of the rectangle to the right,
                 * at a vertical position one dip below the baseline, using the "paint" object
                 * for details.
                 */
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    /**
     * This method is called by Android when the Activity is first started. From the incoming
     * Intent, it determines what kind of editing is desired, and then does it.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mPosition = intent.getIntExtra("pos", 0);
        
        final String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            mState = STATE_EDIT;
            mTitle = intent.getStringExtra("title");
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        setContentView(R.layout.note_editor);
        mText = (EditText) findViewById(R.id.note);

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Modifies the window title for the Activity according to the current Activity state.
        if (mState == STATE_EDIT) {
            String text = String.format(getResources().getString(R.string.title_edit), mTitle);
            setTitle(text);
            
            Log.d(TAG, "download file " + mTitle);
            DownloadNoteTask t = new DownloadNoteTask(mTitle, new DownloadNoteTask.Callback() {
				
				@Override
				public void onSuccess(String note) {
					Log.d(TAG, "downloaded note: " + note);
					if(note != null){
						mText.setTextKeepState(note);
						// Stores the original note text, to allow the user to revert changes.
				        if (mOriginalContent == null) {
				        	mOriginalContent = note;
				        }
					}else{
						setTitle(getText(R.string.error_title));
						mText.setText(getText(R.string.error_message));
					}
				}
				
				@Override
				public void onFail(Throwable t) {
					setTitle(getText(R.string.error_title));
		            mText.setText(getText(R.string.error_message));
				}
			});
            t.run();
            
            String formatedTitle = String.format(getResources().getString(R.string.title_edit), mTitle);
            setTitle(formatedTitle);
        // Sets the title to "create" for inserts
        } else if (mState == STATE_INSERT) {
            setTitle(getText(R.string.title_create));
        }
        mText.setText(mOriginalContent);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }
    
    @Override
    protected void onPause() {
    	Log.d(TAG, "onPause");
    	
        super.onPause();

        // Get the current note text.
        String text = mText.getText().toString();
        int length = text.length();
        mOriginalContent = text;
        
        if (isFinishing() && (length == 0)) {
        	if(mState == STATE_EDIT){
        		deleteNote();
        	}
        } else if (mState == STATE_EDIT) {
            // Creates a map to contain the new values for the columns
            updateNote(text, null);
        } else if (mState == STATE_INSERT) {
            updateNote(text, text);
            mState = STATE_EDIT;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        // Only add extra menu items for a saved note 
        if (mState == STATE_EDIT) {
            // Append to the
            // menu items for any other activities that can do stuff with it
            // as well.  This does a query on the system for any activities that
            // implement the ALTERNATIVE_ACTION for our data, adding a menu item
            // for each one that is found.
            Intent intent = new Intent()
            					.putExtra("title", mTitle)
            					.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This method is called when a menu item is selected. Android passes in the selected item.
     * The switch statement in this method calls the appropriate method to perform the action the
     * user chose.
     *
     * @param item The selected MenuItem
     * @return True to indicate that the item was processed, and no further work is necessary. False
     * to proceed to further processing as indicated in the MenuItem object.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case R.id.menu_save:
        	Log.d(TAG, "option_menu save clicked");
        	String text = mText.getText().toString();
        	if(! text.isEmpty()){//if text is empty, fall back to the case as R.id.menu_delete
        		
	            if (mState == STATE_INSERT) {
	            	// Get the note's length
	                int length = text.length();
	
	                // Sets the title by getting a substring of the text that is 31 characters long
	                // or the number of characters in the note plus one, whichever is smaller.
	                String title = text.substring(0, Math.min(30, length));
	  
	                // If the resulting length is more than 30 characters, chops off any
	                // trailing spaces
	                if (length > 30) {
	                    int lastSpace = title.lastIndexOf(' ');
	                    if (lastSpace > 0) {
	                        title = title.substring(0, lastSpace);
	                    }
	                }
	                mTitle = title;
	            }
	            setResult(
	    				RESULT_OK, 
	    				new Intent()
	    					.putExtra("title", mTitle)
	    					.putExtra("pos", mPosition)
	    		);
	            finish();
	            break;
        	}
        case R.id.menu_delete:
        	Log.d(TAG, "option_menu delete clicked");
        	mText.setText("");
            //deleteNote(true);
        	if(mState == STATE_EDIT){
        		setResult(RESULT_OK, new Intent().putExtra("pos", mPosition).putExtra("delete", true));
        	}else if(mState == STATE_INSERT){
        		setResult(RESULT_CANCELED);
        	}
            finish();
            break;
            /*
        case R.id.menu_edit_title:
        	Log.d(TAG, "option_menu edit_title clicked");
        	startActivityForResult(new Intent()
    			.setComponent(new ComponentName(this, TitleEditor.class))
    			.putExtra("pos", mPosition), 
    		EDIT_TITLE_REQUEST);
        	break;
        	*/
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "onActivityResult");
    	if(requestCode == EDIT_TITLE_REQUEST){
    		if(resultCode == RESULT_OK){
    			String title = data.getStringExtra("title");
    			Log.d(TAG, "get title from titleEditor: " + title);
    			this.mTitle = title;    			
    		}
    	}
    }

    /**
     * Replaces the current note contents with the text and title provided as arguments.
     * @param text The new note contents to use.
     * @param title The new note title to use
     */
    private final void updateNote(String text, String title) {

        if (mState == STATE_INSERT) {

            // If no title was provided as an argument, create one from the note text.
            if (title == null) {
  
                // Get the note's length
                int length = text.length();

                // Sets the title by getting a substring of the text that is 31 characters long
                // or the number of characters in the note plus one, whichever is smaller.
                title = text.substring(0, Math.min(30, length));
  
                // If the resulting length is more than 30 characters, chops off any
                // trailing spaces
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            mTitle = title;
        }

        try{
        String utf = URLEncoder.encode(mTitle, "UTF8");
        File.uploadAsync(
        		Constants.CLOUD_BUCKET, 
        		utf, 
        		new ByteArrayInputStream(escapedLineBreak(text).getBytes()), 
        		new FileUploadCallback(){
        	
		        	public void onSuccess(String requestId){
		        		Log.d(TAG, "note["+ mTitle +"] saved");		        		
		        	}
		        	
		        	public void onFailure(Throwable paramThrowable){
		        		Log.d(TAG, paramThrowable.getMessage());
		        	}
        		}
        	);
        }catch(UnsupportedEncodingException e){
        	Log.e(TAG, e.getCause().getMessage());
        }
    }

    
    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
    	try{
    		String utf = URLEncoder.encode(mTitle, "UTF8");
    	File.deleteAsync(Constants.CLOUD_BUCKET, 
        		utf, 
        		new FileDeleteCallback(){
        	public void onSuccess(String requestId){
        		Log.d(TAG, "FileDeleteCallback.onSuccess");
        	}
        	
        	public void onFailure(Throwable paramThrowable){
        		Log.d(TAG, "FileDeleteCallback.onFailure");
        	}
        });
    	}catch(UnsupportedEncodingException e){
    		//
    	}
    }
}
