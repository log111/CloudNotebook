package org.lh.note;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.lh.note.editor.NoteEditor;
import org.lh.note.editor.TitleEditor;
import org.lh.note.util.Constants;
import org.lh.note.util.RenameNoteTask;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.baidu.mcs.File;
import com.baidu.mcs.Mcs;
import com.baidu.mcs.User;
import com.baidu.mcs.callback.FileDeleteCallback;
import com.baidu.mcs.callback.FileListCallback;
import com.baidu.mcs.callback.UserLogoutCallback;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NoteProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    private static final int ADD_NOTE_REQUEST = 0;
    private static final int EDIT_TITLE_REQUEST = 1;
    private static final int EDIT_NOTE_REQUEST = 2;
    
    private User me = null;
    private List<String> titleList = null;

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        
        me = Mcs.getCurrentUser();
        Log.d(TAG, (me == null) ? "me is null" : "me not null" );

        getListView().setOnCreateContextMenuListener(this);

        File.listFileAsync(new FileListCallback(){

			@Override
			public void onFailure(Throwable arg0) {
				Log.d(TAG, "fail to retrieve the file list");
			}

			@Override
			public void onSuccess(List<File> flist) {
				titleList = new ArrayList<String>();
				for(File f : flist){
					titleList.add(f.getName());
				}
				setListAdapter(
						new ArrayAdapter<String>(
								NotesList.this,
								R.layout.noteslist_item, 
								titleList.toArray(new String[0])
						)
					);
			}
		});
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "NotesList onActivityResult");
    	switch(requestCode){
	    	case ADD_NOTE_REQUEST:
	    		if(resultCode == RESULT_OK){
	    			Log.d(TAG, "new note added");
	    			titleList.add(data.getStringExtra("title"));
	    			setListAdapter(
							new ArrayAdapter<String>(
									NotesList.this,
									R.layout.noteslist_item, 
									titleList.toArray(new String[0])
							)
						);
	    		}
	    	break;
	    	case EDIT_NOTE_REQUEST:
	    		if(resultCode == RESULT_OK){
	    			Log.d(TAG, "note edited");
	    			boolean toRemoved = data.getBooleanExtra("delete", false);
	    			int pos = data.getIntExtra("pos", 0);
	    			if(! toRemoved){    			
		    			String title = data.getStringExtra("title");
		    			titleList.set(pos, title);
	    			}else{
	    				titleList.remove(pos);
	    			}
	    			setListAdapter(
							new ArrayAdapter<String>(
									NotesList.this,
									R.layout.noteslist_item, 
									titleList.toArray(new String[0])
							)
						);
	    		}
	    		break;
	    	case EDIT_TITLE_REQUEST:
	    		if(resultCode == RESULT_OK){
	    			Log.d(TAG, "title changed");
	    			Bundle bundle = data.getExtras();
	    			String title = bundle.getString("title");
	    			final int pos = bundle.getInt("pos");
	    			final String oldTitle = titleList.get(pos);
	    			Log.d(TAG, "rename " + oldTitle + " to " + title);
	    			
	    			RenameNoteTask t = new RenameNoteTask(oldTitle, title, new RenameNoteTask.Callback() {
						
						@Override
						public void onSuccess(String newTitle) {
							titleList.set(pos, newTitle);
			    			setListAdapter(
									new ArrayAdapter<String>(
											NotesList.this,
											R.layout.noteslist_item, 
											titleList.toArray(new String[0])
									)
								);
						}
						
						@Override
						public void onFail(Throwable t) {
							Log.d(TAG, "fail to rename" + oldTitle + ", cause=" + t.getCause().getMessage());
						}
					});
	    			
	    			t.run();	    			
	    		}
	    		break;
    	}
    }
    
    /**
     * This method is called when the user selects an option from the menu, but no item
     * in the list is selected. If the option was INSERT, then a new Intent is sent out with action
     * ACTION_INSERT. The data from the incoming Intent is put into the new Intent. In effect,
     * this triggers the NoteEditor activity in the CloudNotebook application.
     *
     * If the item was not INSERT, then most likely it was an alternative option from another
     * application. The parent method is called to process the item.
     * @param item The menu item that was selected by the user
     * @return True, if the INSERT menu item was selected; otherwise, the result of calling
     * the parent method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
        	Log.d(TAG, "menu add clicked");
           startActivityForResult(
        		new Intent(Intent.ACTION_INSERT)
        			.setComponent(new ComponentName(this, NoteEditor.class)),
        		ADD_NOTE_REQUEST
        	);
           return true;
        case R.id.menu_logout:
        	final Context ctx = this;
        	me.logoutAsync(new UserLogoutCallback() {
				
				@Override
				public void onSuccess(String requestId) {
					me = null;
					finish();
		        	Intent i = new Intent();
			 		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        	i.setClass(ctx, MainpageActivity.class);
		        	startActivity(i);
				}
				
				@Override
				public void onFailure(Throwable arg0) {
					Log.d(TAG, "logoutAsync fail");
				}
			});
        	
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        String title = (String) getListAdapter().getItem(info.position);
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);
        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(title);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        
    	// The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
        	Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        String title = titleList.get(info.position);
        Log.d(TAG, "item id=" + info.id + " position=" + info.position + " content="+titleList.get(info.position));

        switch (item.getItemId()) {
        case R.id.context_open:
        	Log.d(TAG, "edit note at " + info.position);
        	startActivityForResult(
            		new Intent(Intent.ACTION_EDIT)
            			.setComponent(new ComponentName(this, NoteEditor.class))
            			.putExtra("pos", info.position)
            			.putExtra("title", title),
            		EDIT_NOTE_REQUEST
            	);
            return true;
        case R.id.context_delete:
        	final int pos = info.position;
        	try{
        		String utf = URLEncoder.encode(title, "UTF8");
        	
        	File.deleteAsync(Constants.CLOUD_BUCKET, 
        			utf, 
            		new FileDeleteCallback(){
	                	public void onSuccess(String requestId){
	                		Log.d(TAG, "FileDeleteCallback.onSuccess");
	                		titleList.remove(pos);
	                		NotesList.this.setListAdapter(
                				new ArrayAdapter<String>(
                						NotesList.this,
                						R.layout.noteslist_item, 
                						titleList.toArray(new String[0])
                				)
	                		);
	                	}
	                	
	                	public void onFailure(Throwable paramThrowable){
	                		Log.d(TAG, "FileDeleteCallback.onFailure");
	                	}
            	}
        	
    		);
        	}catch(UnsupportedEncodingException e){}
        	Log.d(TAG, "local note deleted");
            return true;
        case R.id.context_edit_title:
        	startActivityForResult(new Intent()
        		.setComponent(new ComponentName(this, TitleEditor.class))
        		.putExtra("pos", info.position), 
        		EDIT_TITLE_REQUEST);
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	
    	String title = titleList.get(Long.valueOf(id).intValue());
    	Log.d(TAG, "id="+id+" title=" + title);
    	
        // Constructs a new URI from the incoming URI and the row ID
        //Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            //setResult(RESULT_OK, new Intent().setData(uri));
        	setResult(RESULT_OK, new Intent().putExtra("title", title));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            //startActivity(new Intent(Intent.ACTION_EDIT, uri);
        	Intent i = new Intent(Intent.ACTION_EDIT)
        		.setComponent(new ComponentName(this, NoteEditor.class))
        		.putExtra("pos", position)
        		.putExtra("title", title);
        	Log.d(TAG, "edit note at " + position);
            startActivityForResult(i, EDIT_NOTE_REQUEST);
        }
    }
}
