package org.lh.note.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.AsyncTask;
import android.util.Log;

import com.baidu.mcs.callback.FileDownloadCallback;
import com.baidu.mcs.file.FileStorage;

public class DownloadNoteTask  {
	private static String TAG = "DownloadNoteTask";
	
	private String mTitle = null;
	private Callback mCB = null;
	
	public DownloadNoteTask(String title, Callback cb){
		mTitle = title;
		mCB = cb;
	}
	
	public void run(){
		FileStorage.downloadAsync(Constants.CLOUD_BUCKET, mTitle, new FileDownloadCallback() {
			
			@Override
			public void onSuccess(Object fileObj) {
				new DNTask().execute(fileObj);
			}
			
			@Override
			public void onFailure(int code, String msg) {
				Log.d(TAG, "downloadAsync fail");
				mCB.onFail(code, msg);
			}
			
			@Override
			public void onProgressUpdate(Integer arg0) {
				//
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
			
			mCB.onSuccess(result);
		}
	}
	
	public static interface Callback{
		void onSuccess(String note);
		void onFail(int code, String msg);
	}
}
