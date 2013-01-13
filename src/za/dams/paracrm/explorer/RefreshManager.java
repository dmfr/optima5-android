package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.HashMap;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncServiceController;

import android.content.Context;
import android.os.Handler;

public class RefreshManager {
    private static final boolean LOG_ENABLED = false; // DONT SUBMIT WITH TRUE
    private static final long FILE_AUTO_REFRESH_INTERVAL = 5 * 60 * 1000; // in milliseconds
    
    private static RefreshManager sInstance;

    private final Clock mClock;
    private final Context mContext;
    private final SyncServiceController mSyncServiceController ;
    private final ControllerResult mControllerResult ;
    private final CrmFileManager mCrmFileManager ;

    // Pending pull requests
    private ArrayList<SyncPullRequest> mPendingPullRequests = new ArrayList<SyncPullRequest>() ;
    

    public interface RefreshListener {
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
    private void notifyRefreshFileChanged(String fileCode) {
        for (RefreshListener l : mListeners) {
            l.onRefreshFileChanged(fileCode) ;
        }
    }
    private class ControllerResult extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( pr != null && mPendingPullRequests.contains(pr) ) {
    			mPendingPullRequests.remove(pr) ;
    			notifyRefreshFileChanged(pr.fileCode) ;
    		}
    	}
    }
    
    
    public void refreshFileList( String fileCode, BibleHelper.BibleEntry conditionBe ) {
    	refreshFileList( fileCode, false, conditionBe ) ;
    }
    public void loadMoreFileList( String fileCode, BibleHelper.BibleEntry conditionBe ) {
    	refreshFileList( fileCode, true, conditionBe ) ;
    }
    public void refreshFileList( String fileCode, boolean loadMore, BibleHelper.BibleEntry conditionBe ) {
    	if( fileCode == null ) {
    		return ;
    	}
    	
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
    	if( conditionBe != null ) {
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
    	// Ajout dans la Queue des resultats
    	mPendingPullRequests.add(spr) ;
    	// Execution de PullRequest
    	mSyncServiceController.requestPull(spr) ;
    }
    
}
