package za.dams.paracrm.explorer.xpressfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncServiceController;
import za.dams.paracrm.explorer.Clock;
import za.dams.paracrm.explorer.CrmFileManager;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileRecord;
import za.dams.paracrm.explorer.xpressfile.CrmXpressfileManager.CrmXpressfileInput;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;

public class CrmXpressfilePuller {
	private static final long FILE_AUTO_REFRESH_INTERVAL = 5 * 60 * 1000; // in milliseconds
	
	private static CrmXpressfilePuller sInstance;
	
    private final Clock mClock;
    private final Context mContext;
    private final SyncServiceController mSyncServiceController ;
    private final ControllerResult mControllerResult ;
    private final CrmFileManager mCrmFileManager ;
	
    public static synchronized CrmXpressfilePuller getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmXpressfilePuller(context,SyncServiceController.getInstance(context),
                    Clock.INSTANCE, new Handler());
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
	private CrmXpressfilePuller( Context context, SyncServiceController controller, Clock clock, Handler handler) {
		mContext = context.getApplicationContext() ;
		mClock = clock ;
		mCrmFileManager = CrmFileManager.getInstance(mContext) ;
        mSyncServiceController = controller ;
        mControllerResult = new ControllerResult() ;
        mSyncServiceController.addResultCallback(mControllerResult) ;
	}
	
	
	private static class XpressfilePullRequest {
		String mFileCode ;
		String mFileFieldCode ;
		String mFileFieldValue ;
		int mLinkXpressfileInputId ;
		
		public boolean equals( Object o ) {
			if( o == null ) {
				return false ;
			}
			XpressfilePullRequest ec = (XpressfilePullRequest)o ;
			if( this.mFileCode == null && ec.mFileCode != null ) {
				return false ;
			}
			else if( this.mFileCode != null && !this.mFileCode.equals(ec.mFileCode) ) {
				return false ;
			}
			if( this.mFileFieldCode == null && ec.mFileFieldCode != null ) {
				return false ;
			}
			else if( this.mFileFieldCode != null && !this.mFileFieldCode.equals(ec.mFileFieldCode) ) {
				return false ;
			}
			if( this.mFileFieldValue == null && ec.mFileFieldValue != null ) {
				return false ;
			}
			else if( this.mFileFieldValue != null && !this.mFileFieldValue.equals(ec.mFileFieldValue) ) {
				return false ;
			}
			if( this.mLinkXpressfileInputId != ec.mLinkXpressfileInputId ) {
				return false ;
			}
			return true ;
		}
		
		public int hashCode() {
			int result = 17 ;
			if( mFileCode != null ) {
				result = 31 * result + mFileCode.hashCode() ;
			} else {
				result = 31 * result ;  
			}
			if( mFileFieldCode != null ) {
				result = 31 * result + mFileFieldCode.hashCode() ;
			} else {
				result = 31 * result ;  
			}
			if( mFileFieldValue != null ) {
				result = 31 * result + mFileFieldValue.hashCode() ;
			} else {
				result = 31 * result ;  
			}
			result = 31 * result + mLinkXpressfileInputId ; 
			return result ;
		}
	}
	
    // Pending pull requests
    private HashMap<XpressfilePullRequest,SyncPullRequest> mPendingPullRequests = new HashMap<XpressfilePullRequest,SyncPullRequest>() ;
    // Last refresh(s)
    private HashMap<XpressfilePullRequest,Long> mFilesLastRefresh = new HashMap<XpressfilePullRequest,Long>() ;

    public interface Listener {
    	public void onXpressfilePullDeliver(int xpressfileInputId, String xpressfilePrimarykey, CrmFileRecord pulledCrmFileRecord );
    }
    private final ArrayList<Listener> mListeners = new ArrayList<Listener>();
	
    public void registerListener(Listener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        if( mListeners.contains(listener) ) {
        	return ;
        }
        mListeners.add(listener);
    }
    public void unregisterListener(Listener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        mListeners.remove(listener);
    }
    
    
    public boolean isStale( XpressfilePullRequest xpr ) {
    	if( !mFilesLastRefresh.containsKey(xpr) ) {
    		return true ;
    	}
    	long lastRefresh = mFilesLastRefresh.get(xpr) ;
    	if( (mClock.getTime() - lastRefresh) > FILE_AUTO_REFRESH_INTERVAL ) {
    		return true ;
    	}
    	return false ;
    }
    public void requestPull( int xpressfileInputId, String xpressfilePrimarykey ) {
    	CrmXpressfileInput cxi = CrmXpressfileManager.inputsList(mContext, xpressfileInputId).get(0);
    	
    	XpressfilePullRequest xpr = new XpressfilePullRequest() ;
    	xpr.mFileCode = cxi.mTargetFilecode ;
    	xpr.mFileFieldCode = cxi.mTargetPrimarykeyFileField ;
    	xpr.mFileFieldValue = xpressfilePrimarykey ;
    	xpr.mLinkXpressfileInputId = xpressfileInputId ;
    	
    	if( !isStale(xpr) ) {
    		// directement
    		deliverForRequest( xpr ) ;
    		return ;
    	}
    	
    	
    	
     	SyncPullRequest.SyncPullRequestFileCondition sprfc = new SyncPullRequest.SyncPullRequestFileCondition();
    	sprfc.conditionSign = "eq" ;
    	sprfc.fileFieldCode = xpr.mFileFieldCode ;
    	sprfc.conditionValue = xpressfilePrimarykey ;
       	SyncPullRequest spr = new SyncPullRequest() ;
    	spr.fileCode = xpr.mFileCode ;
    	spr.fileConditions.add(sprfc) ;
    	spr.limitResults = 1 ;
    	
    	// Deja lancée ?
    	if( mPendingPullRequests.containsValue(spr) ) {
    		mPendingPullRequests.put(xpr, spr) ;
    		return ;
    	}
    	mPendingPullRequests.put(xpr, spr) ;
    	mSyncServiceController.requestPull(spr) ;
    	return ;
    }
    private void deliverForRequest( XpressfilePullRequest xpr ) {
    	CrmFileRecord crmFileRecord = null ;
    	
    	// Requete dans la base local Explorer.CrmFileManager
		DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
		Cursor c = mDb.rawQuery(String.format("SELECT store_file.filerecord_id FROM store_file " +
				"JOIN store_file_field ON store_file_field.filerecord_id = store_file.filerecord_id AND store_file_field.filerecord_field_code='%s' " +
				"WHERE store_file.file_code='%s' AND store_file_field.filerecord_field_value_link='%s' " +
				"ORDER BY sync_timestamp DESC LIMIT 1",
				xpr.mFileFieldCode,xpr.mFileCode,xpr.mFileFieldValue)) ;
		if( c.getCount() == 1 ) {
			c.moveToNext() ;
			int filerecordId = c.getInt(0) ;
			crmFileRecord = mCrmFileManager.filePullData(xpr.mFileCode, filerecordId, 0).get(0) ;
		}
		c.close() ;
		
		if( crmFileRecord != null ) {
			mFilesLastRefresh.put(xpr, mClock.getTime()) ;
		}
		
        for (Listener l : mListeners) {
            l.onXpressfilePullDeliver(xpr.mLinkXpressfileInputId,xpr.mFileFieldValue,crmFileRecord) ;
        }
    }
    
    private class ControllerResult extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( pr == null || !mPendingPullRequests.containsValue(pr) ) {
    			return ;
    		}
    		for( Map.Entry<XpressfilePullRequest,SyncPullRequest> tPair : mPendingPullRequests.entrySet() ) {
    			if( !tPair.getValue().equals(pr) ) {
    				continue ;
    			}
    			XpressfilePullRequest tMatchingXpr = tPair.getKey() ;
    			mPendingPullRequests.remove(tMatchingXpr) ;
    			deliverForRequest( tMatchingXpr ) ;
    		}
    	}
    }


}
