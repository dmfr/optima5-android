package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncPullRequest.SyncPullRequestFileCondition;
import za.dams.paracrm.SyncServiceController;
import za.dams.paracrm.calendar.CalendarController.EventInfo;
import za.dams.paracrm.calendar.CalendarController.EventType;
import za.dams.paracrm.calendar.CrmCalendarManager.CrmCalendarInput;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

public class RefreshManager implements CalendarController.EventHandler {
	private static final String TAG = "CalendarRefreshManager" ;
    private static final boolean DEBUG = false; // DONT SUBMIT WITH TRUE
    private static final long AUTO_REFRESH_INTERVAL = 5 * 60 * 1000; // in milliseconds
    
    private static RefreshManager sInstance;

    private final Context mContext;
    private final SyncServiceController mSyncServiceController ;
    private final ControllerResult mControllerResult ;
    
    // Async task to "prepare" pull request
    private boolean mLaunchPrepareTaskRunning ;

    // Pending pull requests
    private ArrayList<SyncPullRequest> mPendingPullRequests = new ArrayList<SyncPullRequest>() ;
    
    // Calendar visible bounds
    private Time cVisibleStart ;
    private Time cVisibleEnd ;
    // Last refresh(s) / Cache calendar config / state
    private long mLastRefreshTimestampMillis = 0 ;
    private String mCacheLastRefreshSqlDateStart = "0000-00-00" ;
    private String mCacheLastRefreshSqlDateEnd = "0000-00-00" ;
    private HashMap<String,Set<String>> mCacheFileSubscribedAccounts = new HashMap<String,Set<String>>() ;

    public interface RefreshListener {
    	public void onRefreshStart() ;
    	//public void onRefreshBibleChanged(String bibleCode);
    	public void onRefreshFileChanged(String fileCode);
    }
    private final ArrayList<RefreshListener> mListeners = new ArrayList<RefreshListener>();
    
    public static synchronized RefreshManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RefreshManager(context, SyncServiceController.getInstance(context), new Handler());
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
    protected RefreshManager(Context context, SyncServiceController controller, Handler handler) {
        mContext = context.getApplicationContext() ;
        mSyncServiceController = controller ;
        mControllerResult = new ControllerResult() ;
        mSyncServiceController.addResultCallback(mControllerResult) ;
        
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
    			notifyRefreshFileChanged(pr.fileCode) ;
    		}
    	}
    }
    
    
    public void refreshCalendars( boolean forceRefresh ) {
		if( cVisibleStart == null || cVisibleEnd==null ) {
			// pas de visible bounds > on ne peut pas charger
			if(DEBUG){
				Log.w(TAG,"No visible bounds received (yet ?)") ;
			}
			return ;
		}
    	new LaunchRefreshTask().execute(forceRefresh) ;
    }
    
    
    private class LaunchRefreshTask extends AsyncTask<Boolean,Void,Void> {
    	
    	private List<SyncPullRequest> mGeneratedPullRequests ;

    	@Override
    	protected void onPreExecute() {
    		mGeneratedPullRequests = new ArrayList<SyncPullRequest>() ;
			if(DEBUG){
				Log.w("CalendarRefreshManager","Pre-launch task begins!") ;
			}
    	}
		@Override
		protected Void doInBackground(Boolean... arrForceRefresh) {
			if( arrForceRefresh.length != 1 ) {
				return null ;
			}
			boolean forceRefresh = arrForceRefresh[0] ;
			
			
			// Load config for each calendar (subscriptions/accounts)
		   	ArrayList<String> calendarFilecodes = new ArrayList<String>() ;
		   	HashMap<String,Set<String>> currentAccounts = new HashMap<String,Set<String>>() ;
	    	for( CrmCalendarInput cci : CrmCalendarManager.inputsList( mContext ) ) {
	    		String calendarFilecode = cci.mCrmAgendaId ;
	    		calendarFilecodes.add(calendarFilecode);
	    		
	    		CrmCalendarManager lCalManager = new CrmCalendarManager(mContext,calendarFilecode) ;
	    		if( lCalManager.getCalendarInfos().mAccountIsOn ) {
	    			currentAccounts.put(calendarFilecode,PrefsCrm.getSubscribedAccounts(mContext, calendarFilecode));
	    		} else if( PrefsCrm.isCalendarEnabled(mContext, calendarFilecode) ) {
	    			currentAccounts.put(calendarFilecode, null) ;
	    		}
	    	}
			
			// SQL visible bounds
			Time mBaseDate = new Time(Utils.getTimeZone(mContext, null)) ;
			int firstJulianDay = Time.getJulianDay(cVisibleStart.toMillis(true), mBaseDate.gmtoff);
			int lastJulianDay = Time.getJulianDay(cVisibleEnd.toMillis(true), mBaseDate.gmtoff);
			lastJulianDay = lastJulianDay + 1 ;
			
			int nbDaysSpanned = lastJulianDay - firstJulianDay ;
			
			// Refresh (or so) => 3 weeks
			firstJulianDay = firstJulianDay - nbDaysSpanned ;
			lastJulianDay = lastJulianDay + nbDaysSpanned ;
			
			// Translate to SQL
	    	Time timeStart = new Time() ;
	    	timeStart.setJulianDay(firstJulianDay) ;
	    	String refreshSqlDateStart = timeStart.format("%Y-%m-%d");
	    	Time timeEnd = new Time() ;
	    	timeEnd.setJulianDay(lastJulianDay+1) ;
	    	String refreshSqlDateEnd = timeEnd.format("%Y-%m-%d");
			
			if( !forceRefresh ) {
				boolean doIt = false ;
				
				// Time Up ?
				if( (System.currentTimeMillis() - mLastRefreshTimestampMillis) > AUTO_REFRESH_INTERVAL ) {
					if(DEBUG){
						Log.w(TAG,"Time's up ! Refresh") ;
					}
					doIt = true ;
				}

				// Visible bounds changed ?
				if( !refreshSqlDateStart.equals(mCacheLastRefreshSqlDateStart) 
						|| !refreshSqlDateEnd.equals(mCacheLastRefreshSqlDateEnd) ) {
					if(DEBUG){
						Log.w(TAG,"Visible now "+refreshSqlDateStart+" to "+refreshSqlDateEnd+ "! Refresh") ;
					}
					doIt = true ;
				}


				// Calendar config changed (subscriptions/accounts) ?
				if( !currentAccounts.equals(mCacheFileSubscribedAccounts) ) {
					if(DEBUG){
						Log.w(TAG,"Accounts prefs changed ! Refresh") ;
					}
					doIt = true ;
				}


				if( !doIt ) {
					return null ;
				}
			}
				
			mLastRefreshTimestampMillis = System.currentTimeMillis() ;
			mCacheLastRefreshSqlDateStart = refreshSqlDateStart ;
			mCacheLastRefreshSqlDateEnd = refreshSqlDateEnd ;
			mCacheFileSubscribedAccounts = currentAccounts ;

			// Construction des syncPullRequest, une par Calendar
			for( CrmCalendarInput cci : CrmCalendarManager.inputsList( mContext ) ) {
				String calendarFilecode = cci.mCrmAgendaId ;

				CrmCalendarManager lCalManager = new CrmCalendarManager(mContext,calendarFilecode) ;
				if( !lCalManager.getCalendarInfos().mAccountIsOn && !PrefsCrm.isCalendarEnabled(mContext, calendarFilecode) ) {
					continue ;
				}

				SyncPullRequest spr = new SyncPullRequest() ;
				SyncPullRequestFileCondition sprfc ;
				spr.fileCode = calendarFilecode ;
				spr.limitResults = 0 ;

				sprfc = new SyncPullRequestFileCondition() ;
				sprfc.fileFieldCode = lCalManager.getCalendarInfos().mEventStartFileField ;
				sprfc.conditionSign = "lt" ;
				sprfc.conditionValue = refreshSqlDateEnd ;
				spr.fileConditions.add(sprfc);

				sprfc = new SyncPullRequestFileCondition() ;
				sprfc.fileFieldCode = lCalManager.getCalendarInfos().mEventEndFileField ;
				sprfc.conditionSign = "gt" ;
				sprfc.conditionValue = refreshSqlDateStart ;
				spr.fileConditions.add(sprfc);

				if( lCalManager.getCalendarInfos().mAccountIsOn ) {
					sprfc = new SyncPullRequestFileCondition() ;
					sprfc.fileFieldCode = lCalManager.getCalendarInfos().mAccountTargetFileField ;
					sprfc.conditionSign = "in" ;
					sprfc.conditionValueArr.addAll(PrefsCrm.getSubscribedAccounts(mContext, calendarFilecode)) ;
					spr.fileConditions.add(sprfc);
				}

				mGeneratedPullRequests.add(spr) ;
			}

			
			
			return null ;
		}
		@Override
		protected void onPostExecute(Void dummy) {
			if(DEBUG){
				Log.w("CalendarRefreshManager","Pre-launch task end") ;
			}
			
			mLaunchPrepareTaskRunning = false ;
			
			if( mGeneratedPullRequests.size() == 0 ) {
				return ;
			}
			for( SyncPullRequest spr : mGeneratedPullRequests ) {
				executeSyncPullRequest(spr) ;
			}
		}
		
		public void execute( boolean forceRefresh ) {
			if( mLaunchPrepareTaskRunning ) {
				return ;
			}
			mLaunchPrepareTaskRunning = true ;
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR , forceRefresh) ;
		}
    	
    }
    private void executeSyncPullRequest( SyncPullRequest spr ) {
    	// Deja lancée ?
    	if( mPendingPullRequests.contains(spr) ) {
    		return ;
    	}
    	// Ajout dans la Queue des resultats
    	mPendingPullRequests.add(spr) ;
    	// Execution de PullRequest
    	notifyRefreshStart() ;
    	mSyncServiceController.requestPull(spr) ;
    }
    
    
    
    
    // ******* CalendarController callbacks (CEvent handling) ************
	@Override
	public long getSupportedEventTypes() {
		return EventType.UPDATE_TITLE ;
	}
	@Override
	public void handleEvent(EventInfo eventInfo) {
		if( eventInfo.eventType == EventType.UPDATE_TITLE ) {
			if(DEBUG){
				Log.w(TAG,"Received title "+eventInfo.selectedTime+" "+eventInfo.startTime+" "+eventInfo.endTime) ;
			}
			cVisibleStart = eventInfo.startTime ;
			cVisibleEnd = eventInfo.endTime ;
			
			refreshCalendars(false) ;
		}
	}
	@Override
	public void eventsChanged() {}

}
