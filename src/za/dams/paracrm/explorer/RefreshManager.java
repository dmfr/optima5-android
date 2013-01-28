package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncServiceController;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class RefreshManager {
	private static final String TAG = "CalendarRefreshManager" ;
    private static final boolean LOG_ENABLED = false; // DONT SUBMIT WITH TRUE
    private static final long FILE_AUTO_REFRESH_INTERVAL = 5 * 60 * 1000; // in milliseconds
    private static final int MAX_PENDING_REQUESTS = 3 ; // in milliseconds
    
    private static RefreshManager sInstance;

    private final Clock mClock;
    private final Context mContext;
    private final SyncServiceController mSyncServiceController ;
    private final ControllerResult mControllerResult ;
    private final CrmFileManager mCrmFileManager ;

    // Pending pull requests
    private ArrayList<SyncPullRequest> mPendingPullRequests = new ArrayList<SyncPullRequest>() ;
    // Last refresh(s)
    private HashMap<String,Long> mFilesLastRefresh = new HashMap<String,Long>() ;
    private HashMap<String,List<BibleHelper.BibleEntry>> mFilesLastBibleConditions = new HashMap<String,List<BibleHelper.BibleEntry>>() ;

    public interface RefreshListener {
    	public void onRefreshStart() ;
    	//public void onRefreshBibleChanged(String bibleCode);
    	public void onRefreshFileChanged(String fileCode);
    }
    private final ArrayList<RefreshListener> mListeners = new ArrayList<RefreshListener>();
    
    public static synchronized RefreshManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RefreshManager(context, SyncServiceController.getInstance(context),
                    Clock.INSTANCE, new Handler());
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
    protected RefreshManager(Context context, SyncServiceController controller, Clock clock,
            Handler handler) {
        mClock = clock ;
        mContext = context.getApplicationContext() ;
        mSyncServiceController = controller ;
        mControllerResult = new ControllerResult() ;
        mSyncServiceController.addResultCallback(mControllerResult) ;
        mCrmFileManager = CrmFileManager.getInstance(context) ;
        
        mPendingPullRequests = new ArrayList<SyncPullRequest>() ;
    }
    
    public void registerListener(RefreshListener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        if( mListeners.contains(listener) ) {
        	return ;
        }
        mListeners.add(listener);
    }

    public void unregisterListener(RefreshListener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        mListeners.remove(listener);
    }
    
    
    public boolean isRefreshing() {
    	if( mPendingPullRequests.size() > 0 ) {
    		return true ;
    	}
    	return false ;
    }
    public boolean isFileRefreshing( String fileCode ) {
    	for( SyncPullRequest pr : mPendingPullRequests ) {
    		if( pr.fileCode.equals(fileCode) ) {
    			return true ;
    		}
    	}
    	return false ;
    }
    public boolean isFileStale( String fileCode, List<BibleHelper.BibleEntry> conditionsBe ) {
    	if( !mFilesLastRefresh.containsKey(fileCode) ) {
    		return true ;
    	}
    	long lastRefresh = mFilesLastRefresh.get(fileCode) ;
    	if( (mClock.getTime() - lastRefresh) > FILE_AUTO_REFRESH_INTERVAL ) {
    		return true ;
    	}
    	
    	// condition changée ?
    	if( conditionsBe == null ) {
    		conditionsBe = new ArrayList<BibleHelper.BibleEntry>() ;
    	}
    	if( !conditionsBe.equals(mFilesLastBibleConditions.get(fileCode)) ) {
    		return true ;
    	}
    	
    	return false ;
    }
    private void notifyRefreshStart() {
        for (RefreshListener l : mListeners) {
            l.onRefreshStart() ;
        }
    }
    private void notifyRefreshFileChanged(String fileCode) {
        for (RefreshListener l : mListeners) {
            l.onRefreshFileChanged(fileCode) ;
        }
    }
    private class ControllerResult extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( pr != null && mPendingPullRequests.contains(pr) ) {
    			mPendingPullRequests.remove(pr) ;
    			mFilesLastRefresh.put(pr.fileCode, mClock.getTime()) ;
    			notifyRefreshFileChanged(pr.fileCode) ;
    		}
    	}
    }
    
    
    public void refreshFileList( String fileCode, List<BibleHelper.BibleEntry> conditionsBe ) {
    	refreshFileList( fileCode, false, conditionsBe ) ;
    }
    public void loadMoreFileList( String fileCode, List<BibleHelper.BibleEntry> conditionsBe ) {
    	refreshFileList( fileCode, true, conditionsBe ) ;
    }
    public void refreshFileList( String fileCode, boolean loadMore, List<BibleHelper.BibleEntry> conditionsBe ) {
    	if( fileCode == null ) {
    		return ;
    	}
    	
    	// Mise en cache de la "condition sur bible" (pour le slate)
    	mFilesLastBibleConditions.put(fileCode, conditionsBe) ;
    	
    	// Construction de la SyncPullRequest
    	SyncPullRequest spr = new SyncPullRequest();
    	spr.fileCode = fileCode ;
    	spr.supplyTimestamp = false ;
    	
    	// Appel au mCrmFileManager
    	if( loadMore ) {
    		mCrmFileManager.increaseFileVisibleLimit(fileCode) ;
    	}
    	spr.limitResults = mCrmFileManager.getFileVisibleLimit(fileCode) ;
    	
    	// Gestion des conditions
    	if( conditionsBe == null ) {
    		conditionsBe = new ArrayList<BibleHelper.BibleEntry>() ;
    	}
    	for( BibleHelper.BibleEntry conditionBe : conditionsBe ) {
    		String bibleCode = conditionBe.bibleCode ;
    		String bibleEntryKey = conditionBe.entryKey ;
    		
    		CrmFileManager.CrmFileDesc cfd = mCrmFileManager.fileGetFileDescriptor(fileCode) ;
    		for( CrmFileManager.CrmFileFieldDesc cffd : cfd.fieldsDesc ) {
    			if( ( cffd.fieldIsHeader || cffd.fieldIsHighlight ) 
    					&& cffd.fieldType == CrmFileManager.FieldType.FIELD_BIBLE
    					&& cffd.fieldLinkBible.equals(bibleCode) ) {
    				
    				SyncPullRequest.SyncPullRequestFileCondition sprfc = new SyncPullRequest.SyncPullRequestFileCondition() ;
    				sprfc.fileFieldCode = cffd.fieldCode ;
    				sprfc.conditionSign = "eq" ;
    				sprfc.conditionValue = bibleEntryKey ;
    				spr.fileConditions.add(sprfc) ;
    				
    				break ;
    			}
    		}
    	}
    	
    	// Deja lancée ?
    	if( mPendingPullRequests.contains(spr) ) {
    		return ;
    	}
    	// Tentative de purge de la queue ?
    	boolean noneCanceled = false ;
    	while( mPendingPullRequests.size() >= MAX_PENDING_REQUESTS && !noneCanceled ) {
    		Log.w(TAG,"Stack overflow !") ;
    		for( SyncPullRequest sprToCancel : mPendingPullRequests ) {
    			if( mSyncServiceController.cancelPullIfPossible(sprToCancel) ) {
    				Log.w(TAG,"Earliest pull request canceled.") ;
    				mPendingPullRequests.remove(sprToCancel) ;
    				break ;
    			}
    		}
    		noneCanceled = true ;
    	}    	
    	// Ajout dans la Queue des resultats
    	mPendingPullRequests.add(spr) ;
    	// Execution de PullRequest
    	notifyRefreshStart() ;
    	mSyncServiceController.requestPull(spr) ;
    }
    
}
