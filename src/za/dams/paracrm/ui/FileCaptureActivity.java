/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.dams.paracrm.ui ;


import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;

/**
 * Demonstration of using fragments to implement different activity layouts.
 * This sample provides a different layout (and activity flow) when run in
 * landscape.
 */
public class FileCaptureActivity extends Activity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/FileCaptureActivity";
	
	protected ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle bundle = this.getIntent().getExtras();
        
        if( bundle.getBoolean("isNew") ) {
        	String[] fileList = fileList() ;
        	int a ;
        	for( a=0 ; a<fileList.length ; a++ ){
        		if( fileList[a].contains(".save") ) {
        			continue ;
        		}
        		
        		deleteFile(fileList[a]) ;
        	}
        }
        
        
        new QuickGmapsTask().execute() ;
        

        
        
        
        
        
        
        int CrmInputScenId = bundle.getInt("crmId");
        CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getApplicationContext() ) ;
        CrmFileTransaction transaction = mManager.newTransaction(CrmInputScenId) ;
        
        //Log.w(TAG,"My Id is "+mTransaction.getCrmFileCode());
        if( bundle.getBundle("bibleForwards") != null 
        		&& transaction.list_getPageType(0) == CrmFileTransaction.PageType.PAGETYPE_LIST  ) {
        	
        	int fieldId = -1 ;
        	for( CrmFileTransaction.CrmFileFieldDesc fd : transaction.page_getFields(0) ) {
        		fieldId++ ;
        		
        		if( fd.fieldType == CrmFileTransaction.FieldType.FIELD_BIBLE ) {
        			String bibleCode = fd.fieldLinkBible ;
        			String forwardValue = bundle.getBundle("bibleForwards").getString(bibleCode) ;
        			
        			if( forwardValue!=null ) {
        				transaction.page_setRecordFieldValue_bible(0,0,fieldId,forwardValue ) ;
        				transaction.page_setFieldReadonly(0,fieldId,true) ;
        			}
        			
        		}
        	}
        }
        
        
        // 
        
        setContentView(R.layout.filecapture);
    }
    protected void onPause() {
    	super.onPause();
    	CrmFileTransactionManager.saveInstance( getApplicationContext() ) ;
    }
    
    
    
    
    public void endOfTransaction() {
    	// ----- check saisie complète ------
        CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getApplicationContext() ) ;
        CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	if( !mTransaction.isComplete() ){
           	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("Transaction incomplete. Please Check")
        	       .setCancelable(false)
        	       .setIcon(android.R.drawable.stat_notify_error)
        	       .setTitle("Error")
        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();            
        	alert.show();
        	return ;
    	}
    	
    	
    	new DownloadFilesTask().execute() ;
    	
    	
    	String[] fileList = fileList() ;
    	int a ;
    	for( a=0 ; a<fileList.length ; a++ ){
    		if( fileList[a].contains(".save") ) {
    			continue ;
    		}
    		
    		// deleteFile(fileList[a]) ;
    	}
    }
    
    private class DownloadFilesTask extends AsyncTask<Void, Integer, Integer> {
    	protected void onPreExecute(){
    		mProgressDialog = ProgressDialog.show(
    				FileCaptureActivity.this,
    	    		"End of transaction",
    	            "Saving transaction",
    	            true);
    	}
    	
        protected Integer doInBackground(Void... Params ) {
            CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getApplicationContext() ) ;
            CrmFileTransaction mTransaction = mManager.getTransaction() ;
            mTransaction.saveAll() ;
        	try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	return new Integer(0);
         }

        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Integer mInteger) {
            // showDialog("Downloaded " + result + " bytes");
        	mProgressDialog.dismiss() ;
        	Intent myIntent = FileCaptureActivity.this.getIntent() ;
        	FileCaptureActivity.this.setResult(RESULT_OK,myIntent) ;
        	FileCaptureActivity.this.finish() ;
        }
    }


    private class QuickGmapsTask extends AsyncTask<Void, Integer, Boolean> {
    	protected void onPreExecute(){
    	}
    	
        protected Boolean doInBackground(Void... Params ) {
        	// Acquire a reference to the system Location Manager
        	LocationManager locationManager = (LocationManager) FileCaptureActivity.this.getSystemService(Context.LOCATION_SERVICE);

        	// Define a listener that responds to location updates
        	LocationListener locationListener = new LocationListener() {
        	    public void onLocationChanged(Location location) {
        	      // Called when a new location is found by the network location provider.
       				//Log.w(TAG,"Lat "+location.getLatitude()) ;
    				//Log.w(TAG,"Long "+location.getLongitude()) ;
        	    }

        	    public void onStatusChanged(String provider, int status, Bundle extras) {}

        	    public void onProviderEnabled(String provider) {}

        	    public void onProviderDisabled(String provider) {}
        	  };

        	// Register the listener with the Location Manager to receive location updates
        	  try {
        	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener,Looper.getMainLooper());
        	  }
        	  catch( IllegalArgumentException e ) {
        		  return new Boolean(false) ;
        	  }
        	  catch( SecurityException e ){
        		  return new Boolean(false) ;
        	  }
        	
        	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        	locationManager.removeUpdates(locationListener) ;
        	
        	
        	return new Boolean(true) ;
        }

        protected void onProgressUpdate(Integer... mInt) {
            
        }

        protected void onPostExecute(Boolean mbool) {
        }
    }

    /**
     * This is a secondary activity, to show what the user has selected
     * when the screen is not large enough to show it all in one activity.
     */
    /*
    public static class DetailsActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                // If the screen is now in landscape mode, we can show the
                // dialog in-line with the list so we don't need this activity.
                finish();
                return;
            }
            
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;

            if (savedInstanceState == null) {
                // During initial setup, plug in the details fragment.
            	int index = getIntent().getExtras().getInt("index") ;
            	
            	FiledetailFragment details ;
            	details = FiledetailFragmentFactory.getFiledetailFragment(index, mTransaction.list_getPageType(index));
                details.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
            }
        }
    }
	*/
}
