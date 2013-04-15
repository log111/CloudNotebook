package org.lh.note.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


import android.os.AsyncTask;
import android.util.Log;

import com.baidu.mcs.File;
import com.baidu.mcs.callback.FileDeleteCallback;
import com.baidu.mcs.callback.FileDownloadCallback;
import com.baidu.mcs.callback.FileUploadCallback;

public class RenameNoteTask {
	private static String TAG = "RenameNoteTask";
	
	private String mOldUtf = null;
	private String mNewUtf = null;
	private String mNewTitle = null;
	private Callback mCB = null;
	
	public RenameNoteTask(String oldTitle, String newTitle, Callback cb){
		try{
			mOldUtf = URLEncoder.encode(oldTitle, "UTF8");
			mNewTitle = newTitle;
			mNewUtf = URLEncoder.encode(newTitle, "UTF8");
			mCB = cb;
		}catch(UnsupportedEncodingException e){
			Log.e(TAG, e.getCause().getMessage());
		}
	}
	
	public void run(){
		File.downloadAsync(Constants.CLOUD_BUCKET, mOldUtf, new FileDownloadCallback() {
			
			@Override
			public void onSuccess(InputStream arg0) {
				new DNTask(RenameNoteTask.this).execute(arg0);
			}
			
			@Override
			public void onFailure(Throwable arg0) {
				RenameNoteTask.this.mCB.onFail(arg0);
			}
		});
	}
	
	private static class DNTask extends AsyncTask<InputStream, Void, String> { 
		
		private RenameNoteTask parent = null;
		
		public DNTask(RenameNoteTask t){
			parent = t;
		}
		
		@Override
		protected String doInBackground(InputStream... arg) {
			
			InputStreamReader in = new InputStreamReader(arg[0]);
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
			File.uploadAsync(
					Constants.CLOUD_BUCKET, 
					parent.mNewUtf, 
					new ByteArrayInputStream(result.getBytes()),
					new FileUploadCallback() {
						
						@Override
						public void onSuccess(String requestId) {
							File.deleteAsync(
									Constants.CLOUD_BUCKET, 
									parent.mOldUtf,
									new FileDeleteCallback() {
										
										@Override
										public void onSuccess(String requestId) {
											parent.mCB.onSuccess(parent.mNewTitle);
										}
										
										@Override
										public void onFailure(Throwable arg0) {
											parent.mCB.onFail(arg0);
										}
									});
						}
						
						@Override
						public void onFailure(Throwable arg0) {
							parent.mCB.onFail(arg0);
						}
					});
		}
	}
	
	public static interface Callback{
		void onSuccess(String newTitle);
		void onFail(Throwable t);
	}
}
