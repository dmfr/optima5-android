package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

import za.dams.paracrm.DatabaseManager.DatabaseUpgradeResult;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.util.Log;

public class SyncService extends Service {
	
	public static final String SYNCSERVICE_BROADCAST = "SyncServiceBroadcast" ;
	
	public static final String SYNCSERVICE_STATUS = "SyncServiceStatus";
	public static final int SYNCSERVICE_STARTED = 1 ;
	public static final int SYNCSERVICE_COMPLETE = 2 ;
	
	public static final String SYNCPULL_FILECODE = "SyncPullFilecode" ;
	public static final String SYNCPULL_NO_INCREMENTIAL = "SyncPullFilecode" ;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.w("ParacrmSyncService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		new UploadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
		//stopSelf() ;
		
		Bundle bundle = intent.getExtras() ;
		if( bundle.containsKey(SYNCPULL_FILECODE) ){
			String fileCode = bundle.getString(SYNCPULL_FILECODE) ;
			if( bundle.getBoolean(SYNCPULL_NO_INCREMENTIAL, false)) {
				syncPullResetLastTimestamp( fileCode ) ;
			}
			new PullTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,fileCode) ;
		}
		
		
		
		return START_STICKY;
	}
	
	private synchronized void sendBroadcastStarted() {
		Intent i = new Intent(SYNCSERVICE_BROADCAST);  
		i.putExtra(SYNCSERVICE_STATUS,SYNCSERVICE_STARTED);
		sendBroadcast(i);
	}
	private synchronized void sendBroadcastComplete() {
		Intent i = new Intent(SYNCSERVICE_BROADCAST);  
		i.putExtra(SYNCSERVICE_STATUS,SYNCSERVICE_COMPLETE);
		sendBroadcast(i);
	}
	
	
    private class UploadTask extends AsyncTask<Void, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
    	
        protected Boolean doInBackground(Void... Params ) {
        	
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

        	SyncService.this.sendBroadcastStarted() ;
        	
        	DatabaseManager mDbManager = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
        	
        	String android_id = Secure.getString(SyncService.this.getApplicationContext().getContentResolver(),
                    Secure.ANDROID_ID);
        	mDbManager.syncTagVuid(android_id) ;
        	
        	JSONObject jsonDump = new JSONObject() ;
        	try {
        		jsonDump.putOpt("store_file", mDbManager.syncDumpTable("store_file")) ;
				jsonDump.putOpt("store_file_field", mDbManager.syncDumpTable("store_file_field")) ;
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
        		
          		req = String.format("UPDATE store_file SET sync_is_synced='O' WHERE filerecord_id='%d'",uploadEntry.localId) ;
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
        	sendBroadcastComplete() ;
        	SyncService.this.stopSelf() ;
        }
    }
    
    
    
    private class PullTask extends AsyncTask<String, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
    
        protected Boolean doInBackground(String... filesCodes ) {
        	for( String fileCode : filesCodes ){
        		SharedPreferences settings = getSharedPreferences("SyncFiles",MODE_PRIVATE);
        		
        		// ***** timestamp du dernier sync pour ce fileCode ****
            	long lastFileSyncTimestamp = settings.getLong(fileCode, 0) ;
                
            	
            	InputStream is = null ;
            	long newSyncTimestamp = syncDbFromPull( fileCode, is ) ;
            	if( newSyncTimestamp <= 0 ) {
            		return false ;
            	}
            	
            	// stockage du timetamp de cette sync
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong(fileCode, newSyncTimestamp);
                editor.commit() ;
        	}
        	
        	return true ;
        }	  
        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean myBool) {
        	sendBroadcastComplete() ;
        	SyncService.this.stopSelf() ;
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
		nameValuePairs.add(new BasicNameValuePair("_action", "android_syncPush"));
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
		DatabaseManager mDbManager = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;

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
	
	private long syncDbFromPull( String bibleCode , InputStream is ) {
		DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
		
		
    	long versionTimestamp = 0 ;
    	
    	int nbRows = 0 ;
    	int nbRowsExpected = -1 ;
    	int nbTables = 0 ;
    	int nbTablesExpected = -1 ;
    	
    	int nbRowstmp = 0 ;
    	
    	
    	HashMap<Integer,Integer> eqivRemoteIdToLocalId = new HashMap<Integer,Integer>() ;
    	
    	
    	ArrayList<String> DBtables = new ArrayList<String>() ;
    	Cursor tmpCursor = mDb.rawQuery( String.format("SELECT name FROM sqlite_master WHERE type='table'")) ;
    	while( tmpCursor.moveToNext() ) {
    		DBtables.add(tmpCursor.getString(0)) ;
    	}
    	tmpCursor.close() ;
    	
    	ArrayList<ContentValues> insertBuffer = new ArrayList<ContentValues>() ;
    	
    	String tableName = null ;
    	boolean skipTable = false ;
    	try {
    		BufferedReader r = new BufferedReader(new InputStreamReader(is));
    		String readLine ;
    		boolean isFirst = true ;
    		String remoteColumnNames[] = new String[0] ;
    		ContentValues cv ;
    		
    		JSONObject jsonObj ;
    		JSONArray jsonArr ;
			while( (readLine = r.readLine()) != null ) {
				// Log.w(TAG,readLine) ;
				try {
					if( isFirst ) {
						jsonObj = new JSONObject(readLine) ;
						if( jsonObj.optBoolean("success",false) && jsonObj.optLong("timestamp",0) > 0 ) {
							versionTimestamp = jsonObj.optLong("timestamp",0) ;
							nbRowsExpected = jsonObj.optInt("nb_rows",0) ;
							nbTablesExpected = jsonObj.optInt("nb_tables",0) ;
							isFirst = false ;
							continue ;
						}
						else {
							// return new DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
							return 0 ;
						}
					}
					
					if( tableName == null ) {
						jsonArr = new JSONArray(readLine) ;
						if( jsonArr.length() != 1 ) {
							// return new DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
							return 0 ;
						}
						tableName = jsonArr.getString(0) ;
						skipTable = false ;
						if( !DBtables.contains(tableName) ) {
							skipTable = true ;
							readLine = r.readLine() ;
							continue ;
						}
						
						Collection<String> localColumnNames = new ArrayList<String>() ;
						try{
							mDb.execSQL(String.format("DELETE FROM %s",tableName));
							Cursor c = mDb.rawQuery(String.format("SELECT * FROM %s WHERE 0",tableName));
							localColumnNames = Arrays.asList(c.getColumnNames()) ;
							c.close() ;
						}
						finally{
						}
						
						readLine = r.readLine() ;
						jsonArr = new JSONArray(readLine) ;
						remoteColumnNames = new String[jsonArr.length()] ;
						for( int a=0 ; a<jsonArr.length() ; a++ ) {
							String colName = jsonArr.optString(a) ;
							if( localColumnNames.contains(colName) ) {
								remoteColumnNames[a] = colName ;
							}
							else {
								remoteColumnNames[a] = null ;
							}
						}
						
						continue ;
					}
					
					jsonArr = new JSONArray(readLine) ;
					
					if( skipTable ) {
						if(jsonArr.length() == 0 ) {
							insertBuffer = new ArrayList<ContentValues>() ;
							tableName = null ;
							nbRowstmp = 0 ;
							nbTables++ ;
							continue ;
						}
						else {
							nbRows++ ;
							continue ;
						}
					}
					
					if(jsonArr.length() == 0 ) {
						//Log.w(TAG,"Committing "+tableName) ;
						syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId) ;
						insertBuffer = new ArrayList<ContentValues>() ;
						tableName = null ;
						nbRowstmp = 0 ;
						nbTables++ ;
						continue ;
					}
					
					if( jsonArr.length() != remoteColumnNames.length ) {
						//Log.w(TAG,"Bad length !!!") ;
						return 0 ;
					}
					
					cv = new ContentValues() ;
					for( int a=0 ; a<remoteColumnNames.length ; a++ ) {
						if( remoteColumnNames[a] == null ) {
							continue ;
						}
						// Log.w(TAG,"adding "+remoteColumnNames[a]+" "+jsonArr.getString(a)) ;
						cv.put(remoteColumnNames[a], jsonArr.getString(a)) ;
					}
					insertBuffer.add(cv) ;
		    		nbRows++ ;
		    		nbRowstmp++ ;
		    		
		    		if( nbRowstmp > 1000 ) {
		    			syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId) ;
		    			insertBuffer = new ArrayList<ContentValues>() ;
		    			nbRowstmp = 0 ;
		    		}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break ;
				}
			}
			syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return 0 ;
		}
    	
    	return versionTimestamp ;
	}
	private void syncDbFromPull_bulkReplace( String tableName, ArrayList<ContentValues> insertBuffer, HashMap<Integer,Integer> eqivRemoteIdToLocalId ) {
		
		
		
		
	}
    
	
	
	
	private void syncPullResetLastTimestamp( String fileCode ) {
    	SharedPreferences settings = getApplicationContext().getSharedPreferences("SyncFiles",MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(fileCode,0);
        editor.commit() ;
	}
    
    
    
	
	public void onDestroy(){
		Log.w("ParacrmSyncService", "Stopping service");
		super.onDestroy() ;
	}

}
