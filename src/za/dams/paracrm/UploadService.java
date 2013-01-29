package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

public class UploadService extends Service {
	

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new UploadTask().execute() ;
    	return START_STICKY;
	}
	
	
	private String getDeviceAndroidId() {
		return Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
	}
	
	
    private class UploadTask extends AsyncTask<Void, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
    	
        protected Boolean doInBackground(Void... Params ) {
        	
        	UploadService.this.uploadBinaries() ;
        	
        	return new Boolean(true) ;
        }

        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean myBool) {
        	UploadService.this.stopSelf() ;
        }
    }
    
    

    
	
	public void uploadBinaries() {
		DatabaseManager mDbManager = DatabaseManager.getInstance(UploadService.this.getApplicationContext()) ;

		String req = String.format("SELECT filerecord_id, media_filename FROM upload_media") ;
		Cursor tmpCursor = mDbManager.rawQuery(req) ;
		if( tmpCursor.getCount() > 0 ) {
			while(!tmpCursor.isLast()) {
				tmpCursor.moveToNext() ;
				// Log.w("Bin upload","Uploading "+tmpCursor.getString(1)) ;




				InputStream is;
				try {
					is = this.openFileInput(tmpCursor.getString(1));
				} catch (FileNotFoundException e) {
					req = String.format("DELETE FROM upload_media WHERE filerecord_id='%s'",tmpCursor.getString(0)) ;
					mDbManager.execSQL(req) ;
					// TODO Auto-generated catch block
					//Log.w("Bin upload","Failed 1") ;
					continue ;
				}
				ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

				// this is storage overwritten on each iteration with bytes
				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];

				// we need to know how may bytes were read to write them to the byteBuffer
				int len = 0;
				try {
					while ((len = is.read(buffer)) != -1) {
						byteBuffer.write(buffer, 0, len);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//Log.w("Bin upload","Failed 2") ;
					continue ;
				}


				ArrayList<NameValuePair> nameValuePairs = new  ArrayList<NameValuePair>();
				StringBuilder builder = new StringBuilder();
				nameValuePairs.add(new BasicNameValuePair("__ANDROID_ID", getDeviceAndroidId()));
				nameValuePairs.add(new BasicNameValuePair("_domain", "paramount"));
				nameValuePairs.add(new BasicNameValuePair("_moduleName", "paracrm"));
				nameValuePairs.add(new BasicNameValuePair("_action", "android_postBinary"));
				nameValuePairs.add(new BasicNameValuePair("filerecord_id",tmpCursor.getString(0)));
				nameValuePairs.add(new BasicNameValuePair("base64_binary",Base64.encodeToString(byteBuffer.toByteArray(),Base64.DEFAULT)));
				try{
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(getString(R.string.server_url));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					HttpResponse response = httpclient.execute(httppost);
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
					//Log.w("upload ","Result "+builder.toString()) ;
					///Toast.makeText(UploadImage.this, "Response " + the_string_response, Toast.LENGTH_LONG).show();
				}catch(Exception e){
					//e.printStackTrace() ;
					//Log.w("Bin upload","Failed 3") ;
				}

				// do something with builder ;
				JSONObject jsonResp = new JSONObject() ;
				try {
					jsonResp = new JSONObject(builder.toString()) ;
				} catch (JSONException e) {

				}
				if( jsonResp.optBoolean("success",false) == true ) {
					req = String.format("DELETE FROM upload_media WHERE filerecord_id='%s'",tmpCursor.getString(0)) ;
					mDbManager.execSQL(req) ;

					this.getFileStreamPath(tmpCursor.getString(1)).delete() ;
				}


			}
		}
		tmpCursor.close() ;
	}
    
    
    
    
	
	public void onDestroy(){
		Log.w("UploadService", "Stopping service");
		super.onDestroy() ;
	}

}
