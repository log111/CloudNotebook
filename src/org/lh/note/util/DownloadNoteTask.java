package org.lh.note.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


import android.os.AsyncTask;
import android.util.Log;

import com.baidu.mcs.File;
import com.baidu.mcs.callback.FileDownloadCallback;

public class DownloadNoteTask  {
	private static String TAG = "DownloadNoteTask";
	
	private String mTitle = null;
	private Callback mCB = null;
	
	public DownloadNoteTask(String title, Callback cb){
		mTitle = title;
		mCB = cb;
	}
	
	public void run(){
		try{
		String title = URLEncoder.encode(mTitle, "UTF8");
		File.downloadAsync(Constants.CLOUD_BUCKET, title, new FileDownloadCallback() {
			
			@Override
			public void onSuccess(InputStream arg0) {
				new DNTask(DownloadNoteTask.this).execute(arg0);
			}
			
			@Override
			public void onFailure(Throwable arg0) {
				DownloadNoteTask.this.mCB.onFail(arg0);
			}
		});
		}catch(UnsupportedEncodingException e){
			Log.e(TAG, e.getCause().getMessage());
		}
	}
	
	private static class DNTask extends AsyncTask<InputStream, Void, String> { 
		
		private DownloadNoteTask parent = null;
		
		public DNTask(DownloadNoteTask t){
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
			parent.mCB.onSuccess(result);
		}
	}
	
	public static interface Callback{
		void onSuccess(String note);
		void onFail(Throwable t);
	}
}
