package za.dams.paracrm;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.calendar.EditEventFragment;
import za.dams.paracrm.ui.FileCaptureActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenuActivity extends Activity {
	private boolean onForeground = false;

	protected ProgressDialog mProgressDialog;
	private Context mContext;
	private ErrorStatus status;
	
	private MainMenuAdapter mAdapter ;
	private MainMenuStaticAdapter mStaticMenuAdapter ;
	private MenuModulesAdapter mMenuModulesAdapter ;
	private Thread mStaticMenuAdapterRefreshThread ;
	
	private Thread asyncSanityCheker ;
	
	private RefreshDbTask refreshDbTask ;
	
	public static final int MSG_ERR = 0;
	public static final int MSG_CNF = 1;
	public static final int MSG_IND = 2;
	
	static final int ACT_FILECAPTURE = 0;

	 
	public static final String TAG = "PARACRM/MainMenuActivity";
	 
	enum ErrorStatus {
	    NO_ERROR, ERROR
	};
	
	public static class NetworkResult {
		public String rawResponse;
		public ErrorStatus status;
		public String errorMessage;
		
		public NetworkResult(String aRawResponse, ErrorStatus aStatus, String anErrorMessage) {
			rawResponse = aRawResponse ;
			status = aStatus;
			errorMessage = anErrorMessage;
		}
	};
	
    final class RefreshThread extends Thread{
    	public boolean isStop = false;

    	public void run(){
    		try{
    			while(!isStop){                                             
    				MainMenuActivity.this.runOnUiThread(new Runnable() {                   
    					@Override
    					public void run() {
    						((MainMenuStaticAdapter)((GridView)findViewById(R.id.staticgridview)).getAdapter()).refreshFromAnyThread() ;
    					}
    				});                         
    				try{ Thread.sleep(2000); } catch(Exception ex){}
    			}
    		}catch(Exception e){
    		}
    	}
    }
    
	/** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG,"Activity created") ;
        
        setContentView(R.layout.mainmenu);
    	
    	if( !getString(R.string.server_url).equals(getString(R.string.server_url_ref))) {
			((TextView)findViewById(R.id.warningtext)).setVisibility(View.VISIBLE) ;
			((TextView)findViewById(R.id.warningtext)).setText("URL : "+getString(R.string.server_url));
    	}
    	else {
        	try {
    			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
    			ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA) ;
    			String appLabel = (String)getPackageManager().getApplicationLabel(appInfo) ;
    			if( appLabel != null && pInfo.versionName != null ) {
    				((TextView)findViewById(R.id.versiontext)).setVisibility(View.VISIBLE) ;
    				((TextView)findViewById(R.id.versiontext)).setText(appLabel+" - "+"v"+pInfo.versionName);
    			}
    		} catch (NameNotFoundException e) {
    			// TODO Auto-generated catch block
    			// e.printStackTrace();
    		}
    	}
        
        
        mContext = getApplicationContext();
        
        
        
        
        
        mMenuModulesAdapter = new MenuModulesAdapter(this) ;
        
        GridView modulesGrid = (GridView) findViewById(R.id.modulesgridview);
        modulesGrid.setAdapter(mMenuModulesAdapter);

        modulesGrid.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            	final MenuModulesAdapter.ModuleInfo mModule = mMenuModulesAdapter.getItem(position) ;
            	if( mModule.classObject == null ) {
            		return ;
            	}
        		Intent intent = new Intent(MainMenuActivity.this, mModule.classObject);
        		startActivity(intent);
            }
        });
        
        
        
        
        mAdapter = new MainMenuAdapter(this) ;
        
        GridView gridview = (GridView) findViewById(R.id.dynamicgridview);
        gridview.setAdapter(mAdapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(MainMenuActivity.this, "Pouet pouet pouet" + position, Toast.LENGTH_SHORT).show();
            	final CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( MainMenuActivity.this.getApplicationContext() ) ;
            	CrmFileTransaction mTransaction = mManager.getTransaction() ;
            	
            	
            	final MainMenuAdapter.ModuleInfo mModule = mAdapter.getItem(position) ;
            	
            	final Bundle bundle = new Bundle();
            	bundle.putInt("crmId", mModule.crmId);
            	
            	// test saisie en cours ??
            	if( mModule.classObject == FileCaptureActivity.class && mTransaction != null ) {
            		
            		
                	AlertDialog.Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
                	builder.setMessage("Pending transaction. Resume or discard ?")
                	       .setCancelable(true)
                	       .setPositiveButton("Resume", new DialogInterface.OnClickListener() {
                	           public void onClick(DialogInterface dialog, int id) {
                  	            	Intent intent = new Intent(MainMenuActivity.this, mModule.classObject);
                	            	intent.putExtras(bundle);
                	            	startActivityForResult(intent,ACT_FILECAPTURE);
                	           }
                	       })
                	       .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                	           public void onClick(DialogInterface dialog, int id) {
                	        	   mManager.purgeTransactions() ;
                	        	   
                	        	   bundle.putBoolean("isNew", true);
                	        	   
                	            	Intent intent = new Intent(MainMenuActivity.this, mModule.classObject);
                	            	intent.putExtras(bundle);
                	            	startActivityForResult(intent,ACT_FILECAPTURE);
                	           }
                	       });
                	AlertDialog alert = builder.create();
                	alert.show();

            	}
            	else {
            		bundle.putBoolean("isNew", true);
            		
            		Intent intent = new Intent(MainMenuActivity.this, mModule.classObject);
            		intent.putExtras(bundle);
            		startActivityForResult(intent,ACT_FILECAPTURE);
            	}
            }
        });
        
        
        
        
        
        mStaticMenuAdapter = new MainMenuStaticAdapter(this) ;
        
        GridView gridstatic = (GridView) findViewById(R.id.staticgridview);
        gridstatic.setAdapter(mStaticMenuAdapter);

        gridstatic.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(MainMenuActivity.this, "Pouet pouet pouet" + this.getClass().toString(), Toast.LENGTH_SHORT).show();
                try {
					Method method = MainMenuActivity.this.getClass().getMethod(mStaticMenuAdapter.getMyMethodName(position),(Class[]) null);
					try {
						method.invoke(MainMenuActivity.this) ;
					} catch (IllegalArgumentException e) {
						//e.printStackTrace();
					} catch (IllegalAccessException e) {
						//e.printStackTrace();
					} catch (InvocationTargetException e) {
						//e.printStackTrace();
					}
                } catch (NoSuchMethodException e) {
                	//e.printStackTrace();
                }

            }
        });
        
        onForeground = true ;

        // Async check
        asyncSanityCheker = new Thread() {
            public void run() {
                asyncSanityCheck();
            };
        };
        asyncSanityCheker.start();
        
         // mStaticMenuAdapterRefreshThread.start();
    }
    protected void onStart() {
    	super.onStart() ;
        // @DAMS : build proper sync system
		SyncServiceHelper.launchSync( mContext ) ;
    }
    protected void onResume() {
    	super.onResume() ;

    	onForeground = true ;
    	
    	
        mStaticMenuAdapterRefreshThread = new Thread(){
        	public void run(){
        		try{
        			while(onForeground){                                             
        				MainMenuActivity.this.runOnUiThread(new Runnable() {                   
        					@Override
        					public void run() {
        						mStaticMenuAdapter.refreshFromAnyThread() ;
        					}
        				});                         
        				try{ Thread.sleep(2000); } catch(Exception ex){}
        			}
        		}catch(Exception e){
        		}
        	}
        };
        mStaticMenuAdapterRefreshThread.start() ;
    }
    protected void onPause(){
    	onForeground = false ;
    	
    	if( mProgressDialog != null && mProgressDialog.isShowing() ) {
    		Log.w(TAG,"Discarding dialog") ;
    		mProgressDialog.dismiss();
    	}
    	
    	/*
    	if( mStaticMenuAdapterRefreshThread!=null ) {
    		Thread moribund = mStaticMenuAdapterRefreshThread ;
    		mStaticMenuAdapterRefreshThread = null ;
    		moribund.interrupt() ;
    	}
    	*/
    	
    	super.onPause() ;
    }

    private void asyncSanityCheck(){
    	
    	// check for updates => 
    	int nightlyVersion = asyncGetNightlyVersion() ;
    	
    	int localVersion = 0 ;
    	try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			localVersion = pInfo.versionCode ;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
    	if( nightlyVersion > localVersion ) {
    		if( !SyncServiceHelper.hasPendingUploads(mContext) ) {
            	MainMenuActivity.this.runOnUiThread(new Runnable() {                   
        			@Override
        			public void run() {
        				MainMenuActivity.this.mStaticMenuAdapter.hasAvailableUpdate = true ;
        				MainMenuActivity.this.myDownloadUpdate() ; ;
        			}
        		});
    			return ;
    		}
    		
        	MainMenuActivity.this.runOnUiThread(new Runnable() {                   
    			@Override
    			public void run() {
    				MainMenuActivity.this.mStaticMenuAdapter.hasAvailableUpdate = true ;
    			}
    		});
    	}
    	else {
    		//Log.w(TAG,"Local version "+localVersion) ;
    		//Log.w(TAG,"Nightly version "+nightlyVersion) ;
    	}
    	


    	
    	// check for bible version
    	SharedPreferences settings = getPreferences(MODE_PRIVATE);
    	long localTimestamp = settings.getLong("bibleTimestamp", 0);

    	long bibleTimestamp = asyncGetBibleTimestamp() ;
    	
    	if(bibleTimestamp>localTimestamp ) {
    		if( onForeground ){
            	MainMenuActivity.this.runOnUiThread(new Runnable() {                   
        			@Override
        			public void run() {
        				MainMenuActivity.this.myRefreshDb() ;
        			}
        		});
    			return ;
    		}
    	}
    	
    	try{ Thread.sleep(100); } catch(Exception ex){}
    	return ;
    }
    private int asyncGetNightlyVersion(){
		try {
			URL url = new URL(getString(R.string.apk_update_version));
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			//httpURLConnection.setDoOutput(true);
			//httpURLConnection.setRequestMethod("POST");
			//httpURLConnection.setFixedLengthStreamingMode(postString.getBytes().length);
			//httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			try {
				/*
				PrintWriter out = new PrintWriter(httpURLConnection.getOutputStream());
				out.print(postString);
				out.close();
				*/

				InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());  
				String response= HttpPostHelper.readStream(in) ;
				
				int nightlyVersion = 0 ;
				try{
					nightlyVersion = Integer.parseInt(response) ;
				}
				catch( NumberFormatException e ){
					return 0 ;
				}

				return nightlyVersion ;
			}
			catch (IOException e) {
				return 0 ;
			}
			finally{
				httpURLConnection.disconnect() ;
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return 0 ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return 0 ;
		}
    }
    private long asyncGetBibleTimestamp(){
    	HashMap<String,String> postParams = new HashMap<String,String>() ;
    	postParams.put("_domain", "paramount");
    	postParams.put("_moduleName", "paracrm");
    	postParams.put("_action", "android_getDbImageTimestamp");
    	try {
			postParams.put("__versionCode", String.valueOf( getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionCode) ) ;
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		}
    	String postString = HttpPostHelper.getPostString(postParams) ;
    	
		try {
			URL url = new URL(getString(R.string.server_url));
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setFixedLengthStreamingMode(postString.getBytes().length);
			httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			try {
				PrintWriter out = new PrintWriter(httpURLConnection.getOutputStream());
				out.print(postString);
				out.close();

				InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());  
				String response= HttpPostHelper.readStream(in) ;
				
				long versionTimestamp = 0 ;
	            try {
	            	JSONObject jsonResp ;
	            	jsonResp = new JSONObject(response) ;
	            	if( jsonResp.getBoolean("success") ) {
	            		versionTimestamp = jsonResp.getLong("timestamp") ;
	            	}
	            } catch (JSONException e) {
	            	return 0 ;
	            }
				return versionTimestamp ;
			}
			catch (IOException e) {
				return 0 ;
			}
			finally{
				httpURLConnection.disconnect() ;
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return 0 ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return 0 ;
		}
    }
    
    
    
    
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == ACT_FILECAPTURE) {
            if (resultCode == RESULT_OK) {
            	CrmFileTransactionManager.getInstance( getApplicationContext() ).purgeTransactions() ;
            	
            	this.myUploadService() ;
            	
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setMessage("Transaction ended successfully")
            	       .setCancelable(false)
            	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();            
            	alert.show();
            }
        }
    }
    
    public void myQuitActivity() {
    	finish();
    }
    
	
    public void myClearDbAsk(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("This will clear the database ?")
    	       .setCancelable(true)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   MainMenuActivity.this.myClearDb();
    	           }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
	public void myClearDb() {
	    mProgressDialog = ProgressDialog.show(
	    		this,
	    		"Reset All",
	            "Erasing database...",
	            true);
	 
	    // useful code, variables declarations...
	    new Thread((new Runnable() {
	        @Override
	        public void run() {
	            Message msg = null;
	            /*
	            String progressBarData = "Erasing database...";
	            msg = mHandler.obtainMessage(MSG_IND, (Object) progressBarData);
	            mHandler.sendMessage(msg);
	            */
	 
	            // starts the second long operation
	            DatabaseManager mDbManager = DatabaseManager.getInstance(MainMenuActivity.this.getApplicationContext()) ;
	            DatabaseManager.DatabaseUpgradeResult dbUpResult = mDbManager.purgeReferentiel() ;
	            if( dbUpResult.success == true ) {
	            	status = ErrorStatus.NO_ERROR ;
	            	
	            	SharedPreferences settings = getPreferences(MODE_PRIVATE);
	                SharedPreferences.Editor editor = settings.edit();
	                editor.putLong("bibleTimestamp", dbUpResult.versionTimestamp);
	                editor.commit();
	            }
	            else {
	            	status = ErrorStatus.ERROR ;
	            }

	            try {
	            	Thread.sleep(1000);
	            } catch (InterruptedException e) {
	            }

	            if (ErrorStatus.NO_ERROR != status) {
	            	msg = mHandler.obtainMessage(MSG_ERR,
	            			"error while building database");
	            	mHandler.sendMessage(msg);
	            } else {
	            	msg = mHandler.obtainMessage(MSG_CNF,
	            			"Database cleared");
	            	mHandler.sendMessage(msg);
	            }
	            
	        }

	    })).start();
	    // ...
	}
	
	public void myRefreshDb(){
		if( refreshDbTask == null ) {
			refreshDbTask = new RefreshDbTask() ;
			refreshDbTask.execute() ;
		}
		if( refreshDbTask.getStatus() == AsyncTask.Status.RUNNING ) {
			return ;
		}
		else{
			refreshDbTask = new RefreshDbTask() ;
			refreshDbTask.execute() ;
		}
	}
	
	
	
	public void myPrintTrees(){
		Log.w(TAG,"Print Trees...") ;
 		BibleHelper bh = new BibleHelper(getApplicationContext()) ;
		bh.buildCaches() ;
	}
	
	
	public void myDownloadUpdate(){
		new DownloadUpdateTask().execute() ;
	}
	
	
	public void myUploadService(){
        // @DAMS : build proper sync system
		SyncServiceHelper.launchSync( mContext ) ;
	}

	
    private class RefreshDbTask extends AsyncTask<Void, Integer, DatabaseManager.DatabaseUpgradeResult> {
    	protected void onPreExecute(){
    		if( onForeground ) {
    			mProgressDialog = ProgressDialog.show(
    					MainMenuActivity.this,
    					"Database download",
    					"Refresh Bible(s)/Scenarios...",
    					true);
    		}
    	}
    	
        protected DatabaseManager.DatabaseUpgradeResult doInBackground(Void... Params ) {
            DatabaseManager mDbManager = DatabaseManager.getInstance(MainMenuActivity.this.getApplicationContext()) ;
            
        	HashMap<String,String> postParams = new HashMap<String,String>() ;
        	postParams.put("__DBversionCode", String.valueOf(mDbManager.getDbVersion()));
        	postParams.put("_domain", "paramount");
        	postParams.put("_moduleName", "paracrm");
        	postParams.put("_action", "android_getDbImageTab");
        	String postString = HttpPostHelper.getPostString(postParams) ;
        	
        	DatabaseManager.DatabaseUpgradeResult dbUpResult = new DatabaseManager.DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
    		
    		try {
    			URL url = new URL(getString(R.string.server_url));
    			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    			httpURLConnection.setDoOutput(true);
    			httpURLConnection.setRequestMethod("POST");
    			httpURLConnection.setFixedLengthStreamingMode(postString.getBytes().length);
    			httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    			try {
    				PrintWriter out = new PrintWriter(httpURLConnection.getOutputStream());
    				out.print(postString);
    				out.close();

    				InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());  
    	            dbUpResult = mDbManager.upgradeReferentielStream( in ) ;
    				// return dbUpResult ;
    			}
    			catch (IOException e) {
    				return new DatabaseManager.DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
    			}
    			finally{
    				httpURLConnection.disconnect() ;
    			}

    		} catch (MalformedURLException e) {
    			// TODO Auto-generated catch block
    			return new DatabaseManager.DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			return new DatabaseManager.DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
    		}
        	
    		
    		publishProgress(1) ;
    		
    		try{Thread.sleep(500);}catch(Exception e){}
    		
    		BibleHelper bh = new BibleHelper(getApplicationContext()) ;
    		bh.buildCaches() ;
    		
    		try{Thread.sleep(500);}catch(Exception e){}
    		
    		return dbUpResult ;
        }

        protected void onProgressUpdate(Integer... progress ) {
        	// setProgressPercent(progress[0]);
        	if( onForeground ) {
        		if (mProgressDialog.isShowing()) {
        			mProgressDialog.setMessage("Building relationships...");
        		}        	
        	}
        }

        protected void onPostExecute(DatabaseManager.DatabaseUpgradeResult dbUpResult) {
        	if( onForeground ) {
        			mProgressDialog.dismiss() ;
        	}
        	
        	String toastMsg ;
            String statusStr = "" ;
            if( dbUpResult.success == true &&
            		dbUpResult.nbTables > 0 ) {
            	SharedPreferences settings = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putLong("bibleTimestamp", dbUpResult.versionTimestamp);
                editor.commit() ;
                
                statusStr = dbUpResult.nbTables+ " tables / " + dbUpResult.nbRows + " rows" ;
                toastMsg = "Database updated ( " + statusStr + " )" ;
            }
            else {
            	toastMsg = "Error while building database" ;
            }
            Toast.makeText(mContext, toastMsg, Toast.LENGTH_LONG).show();
            mAdapter.notifyDataSetChanged();
        }
    }

    private class DownloadUpdateTask extends AsyncTask<Void, Integer, String> {
    	protected void onPreExecute(){
    		mProgressDialog = ProgressDialog.show(
    				MainMenuActivity.this,
    	    		"Checking for update",
    	            "Please Wait",
    	            true);
    	}
    	
        protected String doInBackground(Void... Params ) {
        	HttpParams httpParameters = new BasicHttpParams();
        	HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
        	HttpConnectionParams.setSoTimeout(httpParameters, 10000);

        	URL url;
			try {
				url = new URL(getString(R.string.apk_update_url));
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				return null ;
			}
        	HttpURLConnection urlConnection;
			try {
				urlConnection = (HttpURLConnection) url.openConnection();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				return null ;
			}
        	byte[] data = new byte[0] ;
        	try {
        		InputStream inputStream = urlConnection.getInputStream() ;
        		
        		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        		  // this is storage overwritten on each iteration with bytes
        		  int bufferSize = 1024;
        		  byte[] buffer = new byte[bufferSize];

        		  // we need to know how may bytes were read to write them to the byteBuffer
        		  int len = 0;
        		  while ((len = inputStream.read(buffer)) != -1) {
        		    byteBuffer.write(buffer, 0, len);
        		  }
        		  data = byteBuffer.toByteArray();

        	} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
        	finally {
        		urlConnection.disconnect();
        	}
        	
        	if( data.length < 1 ) {
        		return null ;
        	}

    		
        	// write apk to the file system
        	FileOutputStream fos ;
        	try {
				fos = openFileOutput("paracrm.apk", Context.MODE_WORLD_READABLE);
			} catch (FileNotFoundException e) {
				return null ;
			}
        	try {
				fos.write(data);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return null ;
			}

        	String fileAbsPath = "file://" + getFilesDir().getAbsolutePath() + "/" +
        			"paracrm.apk";

        	return fileAbsPath ;
        }

        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String fileAbsPath) {
        	mProgressDialog.dismiss() ;
        	if( fileAbsPath == null ){
            	AlertDialog.Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
            	builder.setMessage("Nothing to update")
            	       .setCancelable(false)
            	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	                dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();            
            	alert.show();
            	return ;
        	}
        	
            // showDialog("Downloaded " + result + " bytes");
        	
        	Intent intent = new Intent() ;
        	intent.setAction(android.content.Intent.ACTION_VIEW);
        	intent.setDataAndType(Uri.parse(fileAbsPath),
        			"application/vnd.android.package-archive");
        	startActivity(intent);
        }
    }
	
	 
	final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
	        String text2display = null;
	        switch (msg.what) {
	        case MSG_IND:
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.setMessage(((String) msg.obj));
	            }
	            break;
	        case MSG_ERR:
	            text2display = (String) msg.obj;
	            Toast.makeText(mContext, "Error: " + text2display,
	                    Toast.LENGTH_LONG).show();
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.dismiss();
	            }
	            break;
	        case MSG_CNF:
	            text2display = (String) msg.obj;
	            Toast.makeText(mContext, "Info: " + text2display,
	                    Toast.LENGTH_LONG).show();
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.dismiss();
	            }
	            mAdapter.notifyDataSetChanged();
	            break;
	        default: // should never happen
	            break;
	        }
	    }
	};
	
}