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
import android.util.Base64;
import android.util.Log;

public class UploadService extends Service {
	
	private static boolean isRunning = false ;
	
	public static boolean isRunning(){
		return isRunning ;
	}
	public static boolean hasPendingUploads(Context c){
		Cursor tmpCursor ;
		boolean retValue = false ;
		DatabaseManager mDbManager = DatabaseManager.getInstance(c) ;
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM store_file") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM store_file_field") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM upload_media") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		return retValue ;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		isRunning = true ;
		
		Log.w("UploadService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		new UploadTask().execute() ;
		return START_STICKY;
	}
	
	
    private class UploadTask extends AsyncTask<Void, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
    	
        protected Boolean doInBackground(Void... Params ) {
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
        	
        	DatabaseManager mDbManager = DatabaseManager.getInstance(UploadService.this.getApplicationContext()) ;
        	
        	JSONObject jsonDump = new JSONObject() ;
        	try {
        		jsonDump.putOpt("store_file", mDbManager.dumpTable("store_file")) ;
				jsonDump.putOpt("store_file_field", mDbManager.dumpTable("store_file_field")) ;
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	
        	// Log.i("UploadService",jsonObject.toString()) ;
        	ArrayList<UploadEntry> arrUploadEntry = uploadToServer(jsonDump) ;
        	
        	mDbManager.beginTransaction() ;
        	Iterator<UploadEntry> mIter = arrUploadEntry.iterator() ;
        	String req ;
        	while( mIter.hasNext() ){
        		
        		
        		UploadEntry uploadEntry = mIter.next() ;

        		if(uploadEntry.isSlot) {
        			req = String.format("SELECT filerecord_field_value_string FROM store_file_field WHERE filerecord_id='%d' AND filerecord_field_code='media_title'",uploadEntry.localId) ;
        			Cursor tmpCursor = mDbManager.rawQuery(req) ;
        			if( tmpCursor.getCount() == 1 ) {
        				tmpCursor.moveToNext() ;
        			
        				ContentValues cv = new ContentValues() ;
        				cv.put("filerecord_id",uploadEntry.remoteId) ;
        				cv.put("media_filename",tmpCursor.getString(0)) ;
        				mDbManager.insert( "upload_media" , cv ) ;
        			}
        			tmpCursor.close() ;
        		}
        		
          		req = String.format("DELETE FROM store_file WHERE filerecord_id='%d'",uploadEntry.localId) ;
        		mDbManager.execSQL(req) ;
          		req = String.format("DELETE FROM store_file_field WHERE filerecord_id='%d'",uploadEntry.localId) ;
        		mDbManager.execSQL(req) ;
        	}
        	mDbManager.endTransaction() ;
        	
        	uploadBinaries() ;
        	
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

        	return new Boolean(true) ;
        }

        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean myBool) {
        	isRunning = false ;
        	UploadService.this.stopSelf() ;
        }
    }
    
    
    public static class UploadEntry {
    	public int localId ;
    	public int remoteId ;
    	public boolean isSlot ;
    	
    	public UploadEntry( int localId, int remoteId, boolean isSlot ) {
    		this.localId = localId ;
    		this.remoteId = remoteId ;
    		this.isSlot = isSlot ;
    	}
    }
    
    
	public ArrayList<UploadEntry> uploadToServer(JSONObject jsonDump) {
    	HttpParams httpParameters = new BasicHttpParams();
    	HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
    	HttpConnectionParams.setSoTimeout(httpParameters, 10000);
    	
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("_domain", "paramount"));
		nameValuePairs.add(new BasicNameValuePair("_moduleName", "paracrm"));
		nameValuePairs.add(new BasicNameValuePair("_action", "android_postDbData"));
		nameValuePairs.add(new BasicNameValuePair("data", jsonDump.toString()));
		
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient(httpParameters);
		HttpPost httpPost = new HttpPost(getString(R.string.server_url));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			return new ArrayList<UploadEntry>() ;
		}
		try {
			HttpResponse response = client.execute(httpPost);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				return new ArrayList<UploadEntry>() ;
			}
		} catch (ClientProtocolException e) {
			return new ArrayList<UploadEntry>() ;
		} catch (IOException e) {
			return new ArrayList<UploadEntry>() ;
		}
		
		
		Log.w("UploadService",builder.toString()) ;
		
		// do something with builder ;
        JSONObject jsonResp = new JSONObject() ;
        try {
        	jsonResp = new JSONObject(builder.toString()) ;
        } catch (JSONException e) {
        	return new ArrayList<UploadEntry>() ;
        }
        
        JSONArray jsonUploadSlots = jsonResp.optJSONArray("upload_slots") ;
        JSONObject jsonMap = jsonResp.optJSONObject("map_tmpid_fileid") ;
        if( jsonMap == null ) {
        	jsonMap = new JSONObject() ;
        }
        
        ArrayList<UploadEntry> arrUploadEntry = new ArrayList<UploadEntry>() ;
        Iterator<?> mIter ;
        if( jsonResp.optBoolean("success",false) == true ) {
        	ArrayList<Integer> remoteSlots = new ArrayList<Integer>() ;
        	int a=0 ;
        	for( a=0 ; a<jsonUploadSlots.length() ; a++ ) {
        		remoteSlots.add(new Integer(jsonUploadSlots.optString(a))) ;
        	}
        	
        	
        	
        	 mIter = jsonMap.keys();
        	 Integer localId ;
        	 Integer remoteId ;
        	 String curKey ;
        	while( mIter.hasNext() ){
        		curKey = (String)mIter.next() ;
        		localId = new Integer(curKey) ;
        		remoteId = new Integer(jsonMap.optString(curKey)) ;
        		arrUploadEntry.add(new UploadEntry(localId.intValue(),remoteId.intValue(),remoteSlots.contains(remoteId))) ; 
        	}
        }
				
		
		
		return arrUploadEntry ;
	}
	
	public void uploadBinaries() {
		DatabaseManager mDbManager = DatabaseManager.getInstance(UploadService.this.getApplicationContext()) ;

		String req = String.format("SELECT filerecord_id, media_filename FROM upload_media") ;
		Cursor tmpCursor = mDbManager.rawQuery(req) ;
		if( tmpCursor.getCount() > 0 ) {
			while(!tmpCursor.isLast()) {
				tmpCursor.moveToNext() ;
				Log.w("Bin upload","Uploading "+tmpCursor.getString(1)) ;




				InputStream is;
				try {
					is = this.openFileInput(tmpCursor.getString(1));
				} catch (FileNotFoundException e) {
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
		Log.w("Bin upload","All done ?") ;

	}
    
    
    
    
	
	public void onDestroy(){
		Log.w("UploadService", "Stopping service");
		super.onDestroy() ;
	}

}
