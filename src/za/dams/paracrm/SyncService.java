package za.dams.paracrm;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import android.content.Intent;
import android.database.Cursor;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings.Secure;

public class SyncService extends Service {
	
	public static final String TAG = "SyncService" ;
	
	public static final String EXTRA_MESSENGER = "za.dams.paracrm.SyncService.EXTRA_MESSENGER" ;
	
	public static final String SYNCSERVICE_BROADCAST = "SyncServiceBroadcast" ;
	
	public static final String SYNCSERVICE_STATUS = "SyncServiceStatus";
	public static final int SYNCSERVICE_STARTED = 1 ;
	public static final int SYNCSERVICE_COMPLETE = 2 ;
	
	public static final String SYNCPULL_FILECODE = "SyncPullFilecode" ;
	public static final String SYNCPULL_NO_INCREMENTIAL = "SyncPullNoIncrement" ;
	public static final String PULL_REQUEST = "PullRequest" ;
	public static final String PUSH_DO = "PushDo" ;
	
	private static final int FILE_TTL_PURGE = 2 * 24 * 60 * 60 ; // 2 days in seconds
	
	private Messenger mMessenger ;
	private SyncPullRequest mOriginalPullRequest ;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Log.w("ParacrmSyncService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		
		Bundle bundle = null ;
		if( intent != null ){
			bundle = intent.getExtras() ;
		}
		
		if( bundle != null && bundle.containsKey(EXTRA_MESSENGER) ) {
			mMessenger = (Messenger)bundle.get(EXTRA_MESSENGER);
		}
		
		if( bundle != null && bundle.containsKey(PULL_REQUEST) ) {
			mOriginalPullRequest = (SyncPullRequest)bundle.getParcelable(PULL_REQUEST) ;
			new SyncTask().execute(mOriginalPullRequest) ;
		} else if( bundle != null && bundle.containsKey(PUSH_DO) && bundle.getBoolean(PUSH_DO)==true ) {
			new SyncTask().execute() ;
		} else {
			SyncService.this.stopSelf() ;
		}
		
		return Service.START_REDELIVER_INTENT ;
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
	
	
	private class SyncTask extends AsyncTask<SyncPullRequest, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
        protected Boolean doInBackground(SyncPullRequest... prs ) {
        	if( SyncServiceController.hasPendingPush(getApplicationContext())) {
        		doPush() ;
        	}
        	for( SyncPullRequest pr : prs ) {
        		if( pr != null ) {
        			SyncService.this.doPull( pr ) ;
        		}
        	}
        	
        	return true ;
        }
        protected void onPostExecute(Boolean myBool) {
        	sendBroadcastComplete() ;
        	
        	if (SyncService.this.mMessenger !=null) {
        		Message msg=Message.obtain();

        		Bundle msgBundle = new Bundle() ;
        		if( SyncService.this.mOriginalPullRequest != null ) {
        			msgBundle.putParcelable(PULL_REQUEST,SyncService.this.mOriginalPullRequest) ;
        		}
        		msg.setData(msgBundle);
        		try {
        			// Log.w(TAG,"Sending end message ") ;
        			SyncService.this.mMessenger.send(msg);
        		}
        		catch (android.os.RemoteException e1) {
        			// Log.w(getClass().getName(), "Exception sending message", e1);
        		}
        	}

        	UploadServiceHelper.launchUpload( SyncService.this.getApplicationContext() ) ;
        	SyncService.this.stopSelf() ;
        }
        
        public SyncTask execute( SyncPullRequest spr ) {
        	executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,spr) ;
        	return this ;
        }
	}
	
	
    
    
    
    private boolean doPush() {
    	DatabaseManager mDbManager = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
    	
    	Long tsLong = System.currentTimeMillis()/1000;
    	String ts = tsLong.toString();
    	
    	String android_id = Secure.getString(SyncService.this.getApplicationContext().getContentResolver(),
                Secure.ANDROID_ID);
    	
    	mDbManager.syncTagVuid(android_id,ts) ;
    	
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
    	
    	// uploadBinaries() ;
    	
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

    	return new Boolean(true) ;
    }
    
    private boolean doPull( SyncPullRequest pr ) {
    	
    	// ***** Constition de la liste locale (WHAT-I-HAVE) ****    	
    	HashMap<String,Integer> hashmapLocalVuidSync = doPull_getLocalSyncHashmap(pr);
    	JSONObject jsonHashmapLocalVuidSync = new JSONObject() ;
    	try {
    		for( Map.Entry<String,Integer> me : hashmapLocalVuidSync.entrySet() ) {
    			jsonHashmapLocalVuidSync.put(me.getKey(), ((int)me.getValue())) ;
    		}
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}


    	HashMap<String,String> postParams = new HashMap<String,String>() ;
    	postParams.put("_domain", "paramount");
    	postParams.put("_moduleName", "paracrm");
    	postParams.put("_action", "android_syncPull");
    	postParams.put("file_code", pr.fileCode);
    	postParams.put("local_sync_hashmap", jsonHashmapLocalVuidSync.toString()) ;
    	if( pr.fileConditions != null && pr.fileConditions.size() > 0 ) {
			try {
				JSONArray jsonArr = new JSONArray() ;
				for( SyncPullRequest.SyncPullRequestFileCondition prfc : pr.fileConditions ) {
					JSONObject jsonObj = new JSONObject() ;
					jsonObj.put("entry_field_code", prfc.fileFieldCode) ;
					jsonObj.put("condition_sign", prfc.conditionSign) ;
					if( prfc.conditionValueArr!=null && prfc.conditionValueArr.size() > 0 ) {
						JSONArray jsonArrValues = new JSONArray() ;
						for( String s : prfc.conditionValueArr ) {
							jsonArrValues.put(s) ;
						}
						jsonObj.put("condition_value", jsonArrValues) ;
					} else {
						jsonObj.put("condition_value", prfc.conditionValue) ;
					}
					jsonArr.put(jsonObj) ;
				}
				postParams.put("filter", jsonArr.toString());
			} catch (JSONException e) {
    		}
    	}
    	if( pr.limitResults != 0 ) {
    		postParams.put("limit", String.valueOf(pr.limitResults));
    	}
    	
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpPost postRequest = new HttpPost(getString(R.string.server_url));
        client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
        client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, 10000);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            for( Map.Entry<String,String> kv : postParams.entrySet() ) {
            	nameValuePairs.add(new BasicNameValuePair(kv.getKey(), kv.getValue()));
            }
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        	
            HttpResponse response = client.execute(postRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return false ;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    BufferedInputStream bis = new BufferedInputStream(inputStream);
        			int newPullTimestamp = syncDbFromPull( pr.fileCode, bis ) ;
        			afterPullTTLpurge(newPullTimestamp) ;
        			
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
        	postRequest.abort();
        } catch (IllegalStateException e) {
        	postRequest.abort();
        } catch (Exception e) {
        	postRequest.abort();
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return true ;
    }
    private HashMap<String,Integer> doPull_getLocalSyncHashmap( SyncPullRequest pr ) {
    	
    	// execution d'une requete locale avec les conditions pour trouver :
    	StringBuilder sb = new StringBuilder() ;
    	sb.append(String.format("SELECT sync_vuid,sync_timestamp FROM store_file mstr")) ;
    	for( SyncPullRequest.SyncPullRequestFileCondition sprfc : pr.fileConditions ) {
    		String joinPrefix = "det_"+sprfc.fileFieldCode ;
    		
    		sb.append(" JOIN store_file_field "+joinPrefix) ;
    		sb.append(" ON ( "+joinPrefix+".filerecord_id=mstr.filerecord_id") ;
    		sb.append(" AND " +joinPrefix+".filerecord_field_code='"+sprfc.fileFieldCode+"'") ;
    		sb.append(")") ;
    	}
    	sb.append(String.format(" WHERE mstr.file_code='%s'",pr.fileCode)) ;
    	for( SyncPullRequest.SyncPullRequestFileCondition sprfc : pr.fileConditions ) {
    		String joinPrefix = "det_"+sprfc.fileFieldCode ;
    		String storeFileFieldValue = doPull_getDefineFieldValue( pr.fileCode, sprfc.fileFieldCode ) ;
    		
    		if( sprfc.conditionSign.equals("in") && sprfc.conditionValueArr.size() == 0 ) {
    			sb.append(" AND 0") ;
    			continue ;
    		}
    		
    		sb.append(" AND "+joinPrefix+"."+storeFileFieldValue) ;
    		if( sprfc.conditionSign.equals("eq") || sprfc.conditionSign.equals("=") ) {
    			sb.append(" = ") ;
    		} else if( sprfc.conditionSign.equals("lt") || sprfc.conditionSign.equals("<=") || sprfc.conditionSign.equals("<") ) {
    			sb.append(" <= ") ;
    		} else if( sprfc.conditionSign.equals("gt") || sprfc.conditionSign.equals(">=") || sprfc.conditionSign.equals(">") ) {
    			sb.append(" >= ") ;
    		} else if( sprfc.conditionSign.equals("in") ) {
    			sb.append(" IN ") ;
    		} else {
    			sb.append(" = ") ;
    		}
    		
    		if( sprfc.conditionValueArr.size() > 0 ) {
    			sb.append("(") ;
    			for( String s : sprfc.conditionValueArr ) {
    				sb.append("'"+s+"'") ;
    			}
    			sb.append(")") ;
    		} else {
    			sb.append("'"+sprfc.conditionValue+"'") ;
    		}
    	}
    	if( pr.limitResults > 0 ) {
    		sb.append("ORDER BY sync_timestamp DESC LIMIT " + pr.limitResults) ;
    	}
    	
    	// Log.w(TAG,sb.toString()) ;
    	String query = sb.toString() ;
    	
    	HashMap<String,Integer> hashmapLocalVuidSync = new HashMap<String,Integer>() ;
    	DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
    	Cursor c = mDb.rawQuery(query) ;
    	while( c.moveToNext() ) {
    		hashmapLocalVuidSync.put(c.getString(0), c.getInt(1)) ;
    	}
    	c.close() ;
    	
    	// retour sync_vuid => sync_timestamp
    	return hashmapLocalVuidSync ;
    }
    private String doPull_getDefineFieldValue( String fileCode, String fileFieldCode ) {
    	String query = String.format("SELECT entry_field_type FROM define_file_entry WHERE file_code='%s' AND entry_field_code='%s'",fileCode,fileFieldCode) ;
    	
    	String defineFieldValue = null ;
    	DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
    	Cursor c = mDb.rawQuery(query) ;
    	if( !c.moveToNext() ) {
    		return null ;
    	}
    	defineFieldValue = c.getString(0) ;
    	c.close() ;
    	
    	if( defineFieldValue.equals("date") ) {
    		defineFieldValue = "filerecord_field_value_date" ;
    	} else if( defineFieldValue.equals("string") ) {
    		defineFieldValue = "filerecord_field_value_string" ;
    	} else if( defineFieldValue.equals("link") ) {
    		defineFieldValue = "filerecord_field_value_link" ;
    	} else if( defineFieldValue.equals("number") || defineFieldValue.equals("bool") ) {
    		defineFieldValue = "filerecord_field_value_number" ;
    	} else {
    		return null ;
    	}
    	
    	return defineFieldValue ;
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
    	HttpConnectionParams.setConnectionTimeout(httpParameters, 30000);
    	HttpConnectionParams.setSoTimeout(httpParameters, 30000);
    	
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
		
		
		// Log.w("UploadService",builder.toString()) ;
		
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
        if( jsonUploadSlots == null ) {
        	jsonUploadSlots = new JSONArray() ;
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
	
	
	
	
	
	private int syncDbFromPull( String bibleCode , InputStream is ) {
		DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
		int pullTimestamp = (int)(System.currentTimeMillis() / 1000) ;
		
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
						syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId,pullTimestamp) ;
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
		    			syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId,pullTimestamp) ;
		    			insertBuffer = new ArrayList<ContentValues>() ;
		    			nbRowstmp = 0 ;
		    		}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break ;
				}
			}
			syncDbFromPull_bulkReplace(tableName,insertBuffer,eqivRemoteIdToLocalId,pullTimestamp) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return 0 ;
		}
    	
    	return pullTimestamp ;
	}
	private void syncDbFromPull_bulkReplace( String tableName, ArrayList<ContentValues> cvs, HashMap<Integer,Integer> eqivRemoteIdToLocalId, int pullTimestamp ) {
		DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
		
    	if( cvs.isEmpty() ){
    		return ;
    	}
    	
    	mDb.beginTransaction() ;
    	
    	if( tableName.equals("store_file") ) {
        	
    		Iterator<ContentValues> iter = cvs.iterator() ;
    		ContentValues cv ;
    		while( iter.hasNext() ) {
    			cv = iter.next() ;
    			
    			int updateId = 0 ;

    			if( cv.getAsString("sync_vuid").equals("") ) {
    				continue ;
    			}
    			
    			Cursor cursor = mDb.rawQuery(String.format("SELECT filerecord_id , sync_timestamp FROM store_file WHERE sync_vuid='%s'",cv.getAsString("sync_vuid"))) ;
    			while( cursor.moveToNext() ) {
    				updateId = cursor.getInt(0) ;
    			}
    			cursor.close();
    			
    			cv.put("sync_is_synced", "O") ;
    			cv.put("pull_timestamp",pullTimestamp) ;
    			int localId ;
    			int remoteId = cv.getAsInteger("filerecord_id") ;
    			int remoteParentId = cv.getAsInteger("filerecord_parent_id") ;
    			if( remoteParentId > 0 ) {
    				if( !eqivRemoteIdToLocalId.containsKey(remoteParentId) ) {
    					continue ;
    				}
    				int localParentId = eqivRemoteIdToLocalId.get(remoteParentId) ;
    				cv.put("filerecord_parent_id", localParentId) ;
    			}
    			if( updateId > 0 ) {
    				cv.put("filerecord_id",updateId) ;
    				mDb.update(tableName, cv, "filerecord_id="+updateId);
    				localId = updateId ;
    			}
    			else {
    				cv.putNull("filerecord_id") ;
        			localId = (int)mDb.insert(tableName, cv);
    			}

    			eqivRemoteIdToLocalId.put(remoteId, localId) ;
    		}
    	
    	}
    	if( tableName.equals("store_file_field") ) {
        	
    		Iterator<ContentValues> iter = cvs.iterator() ;
    		ContentValues cv ;
    		while( iter.hasNext() ) {
    			cv = iter.next() ;
    			int remoteId = cv.getAsInteger("filerecord_id") ;
    			if( !eqivRemoteIdToLocalId.containsKey(remoteId) ) {
    				continue ;
    			}
    			int localId = eqivRemoteIdToLocalId.get(remoteId) ;
    			cv.put("filerecord_id", localId) ;
    			
    			String fieldCode = cv.getAsString("filerecord_field_code") ;
    			mDb.execSQL(String.format("DELETE FROM store_file_field WHERE filerecord_id='%d' AND filerecord_field_code='%s'",localId,fieldCode)) ;
    			
    			mDb.insert(tableName, cv);
    		}
    	
    	}
    	
		mDb.endTransaction() ;
	}
    
	
	
	
    
    private boolean afterPullTTLpurge( int newPullTimestamp )
    {
    	int deadLine = (newPullTimestamp - FILE_TTL_PURGE) ;
    	boolean doIt = false ;
    	
    	DatabaseManager mDb = DatabaseManager.getInstance(SyncService.this.getApplicationContext()) ;
    	Cursor c = mDb.rawQuery(String.format("SELECT count(*) FROM store_file WHERE pull_timestamp<'%d' AND pull_timestamp NOT NULL",deadLine)) ;
    	while( c.moveToNext() ) {
    		doIt = (c.getInt(0)>0) ? true : false ;
    		break ;
    	}
    	c.close() ;
    	
    	if( doIt ) {
    		mDb.execSQL(String.format("DELETE FROM store_file WHERE pull_timestamp<'%d' AND pull_timestamp NOT NULL",deadLine)) ;
    		mDb.execSQL("delete from store_file_field where filerecord_id NOT IN (select filerecord_id from store_file)") ;
    	}
    	
    	return true ;
    }
    
    
	
	public void onDestroy(){
		// Log.w("ParacrmSyncService", "Stopping service");
		super.onDestroy() ;
	}

}
