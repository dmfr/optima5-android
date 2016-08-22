package za.dams.paracrm;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.IBinder;
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

		String req = String.format("SELECT sync_vuid, media_filename FROM upload_media") ;
		Cursor tmpCursor = mDbManager.rawQuery(req) ;
		if( tmpCursor.getCount() > 0 ) {
			while(!tmpCursor.isLast()) {
				tmpCursor.moveToNext() ;
				// Log.w("Bin upload","Uploading "+tmpCursor.getString(1)) ;




				InputStream is;
				try {
					is = this.openFileInput(tmpCursor.getString(1));
				} catch (FileNotFoundException e) {
					req = String.format("DELETE FROM upload_media WHERE sync_vuid='%s'",tmpCursor.getString(0)) ;
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


		        HashMap<String,String> nameValuePairs = new HashMap<String,String>();
		        nameValuePairs.put("_action", "android_postBinary");
		        nameValuePairs.put("sync_vuid",tmpCursor.getString(0));
		        nameValuePairs.put("base64_binary",Base64.encodeToString(byteBuffer.toByteArray(),Base64.DEFAULT));
		        
		        final HttpClient httpclient = HttpServerHelper.getHttpClient(this, HttpServerHelper.TIMEOUT_DL) ;
		        final HttpPost httppost = HttpServerHelper.getHttpPostRequest(this, nameValuePairs);
		        
				String response = new String();
				try{
					HttpResponse httpresponse = httpclient.execute(httppost);
					HttpEntity entity = httpresponse.getEntity();
					InputStream content = entity.getContent();
					response = HttpServerHelper.readStream(content) ;
					//Log.w("upload ","Result "+builder.toString()) ;
					///Toast.makeText(UploadImage.this, "Response " + the_string_response, Toast.LENGTH_LONG).show();
				}catch(Exception e){
					//e.printStackTrace() ;
					//Log.w("Bin upload","Failed 3") ;
				}finally {
		            if ((httpclient instanceof AndroidHttpClient)) {
		                ((AndroidHttpClient) httpclient).close();
		            }
				}

				// do something with builder ;
				JSONObject jsonResp = new JSONObject() ;
				try {
					jsonResp = new JSONObject(response) ;
				} catch (JSONException e) {

				}
				if( jsonResp.optBoolean("success",false) == true ) {
					req = String.format("DELETE FROM upload_media WHERE sync_vuid='%s'",tmpCursor.getString(0)) ;
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
