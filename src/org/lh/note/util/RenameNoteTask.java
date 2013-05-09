package org.lh.note.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.mcs.callback.FileDeleteCallback;
import com.baidu.mcs.callback.FileDownloadCallback;
import com.baidu.mcs.callback.FileUploadCallback;
import com.baidu.mcs.file.FileStorage;

public class RenameNoteTask {
	private static String TAG = "RenameNoteTask";
	
	private String mOldTitle = null;
	private String mNewTitle = null;
	private Callback mCB = null;
	
	public RenameNoteTask(String oldTitle, String newTitle, Callback cb){
		mOldTitle = oldTitle;
		mNewTitle = newTitle;
		mCB = cb;
	}
	
	public void run(){
		FileStorage.downloadAsync(Constants.CLOUD_BUCKET, mOldTitle, new FileDownloadCallback() {
			
			@Override
			public void onSuccess(Object fileObj) {
				new DNTask().execute(fileObj);
			}
			
			@Override
			public void onFailure(int code, String msg) {
				mCB.onFail(code, msg);
			}
			
			@Override
			public void onProgressUpdate(Integer arg0) {	
			}
		});
	}
	
	private class DNTask extends AsyncTask<Object, Void, String> { 
		
		@Override
		protected String doInBackground(Object... arg) {
			
			InputStreamReader in = 
					new InputStreamReader(
						new ByteArrayInputStream((byte[])arg[0])
					);
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[2048];
			try{
				int c = in.read(buf, 0, 2048);
				while(c != -1){
					sb.append(buf, 0, c);
					c = in.read(buf, 0, 2048);    					
				}
				
				return sb.toString();
				
			}catch(IOException e){
				Log.e(TAG, e.getCause().getMessage());
			}finally{
				try{
					in.close();
				}catch(IOException e){
					Log.e(TAG, e.getCause().getMessage());
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			FileStorage.uploadAsync(
					Constants.CLOUD_BUCKET, 
					mNewTitle, 
					result.getBytes(),
					new FileUploadCallback() {
						
						@Override
						public void onSuccess(String requestId) {
							FileStorage.deleteAsync(
									Constants.CLOUD_BUCKET, 
									mOldTitle,
									new FileDeleteCallback() {
										
										@Override
										public void onSuccess(String requestId) {
											mCB.onSuccess(mNewTitle);
										}
										
										@Override
										public void onFailure(int code, String msg) {
											mCB.onFail(code, msg);
										}
									});
						}
						
						@Override
						public void onFailure(int code, String msg) {
							mCB.onFail(code, msg);
						}
						
						@Override
						public void onProgressUpdate(Integer arg0) {							
						}
					});
		}
	}
	
	public static interface Callback{
		void onSuccess(String newTitle);
		void onFail(int code, String msg);
	}
}
