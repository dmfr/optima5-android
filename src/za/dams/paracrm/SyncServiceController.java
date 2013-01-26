package za.dams.paracrm;

import java.util.ArrayList;
import java.util.HashSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class SyncServiceController {
	
    private static final String TAG = "SyncServiceController";
    private static SyncServiceController sInstance;
    private final Context mContext;
    
    private Thread waitThread ;
    private boolean waitRunning ;
    private boolean serviceRunning ;
    private ArrayList<SyncPullRequest> arrPullRequest ;
    
    private final HashSet<Result> mListeners = new HashSet<Result>();
    
    private Handler mHandler=new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		SyncPullRequest pr = null ;
    		if( msg.getData().containsKey(SyncService.PULL_REQUEST) ) {
    			pr = (SyncPullRequest)msg.getData().getParcelable(SyncService.PULL_REQUEST) ;
    		}
            synchronized (mListeners) {
                for (Result listener : mListeners) {
                    listener.onServiceEndCallback(0,pr);
                }
            }    		
    		launchNextPull() ;
    	}
    };

    
    protected SyncServiceController(Context _context) {
        mContext = _context.getApplicationContext();
        arrPullRequest = new ArrayList<SyncPullRequest>() ;
    }
    
    /**
     * Gets or creates the singleton instance of Controller.
     */
    public synchronized static SyncServiceController getInstance(Context _context) {
        if (sInstance == null) {
            sInstance = new SyncServiceController(_context);
        }
        return sInstance;
    }
    public void requestPush() {
    	if( arrPullRequest.size() == 0 ) {
    		requestPull(null);
    	}
    }
    public boolean cancelPullIfPossible( SyncPullRequest pr ) {
    	if( arrPullRequest.contains(pr) && arrPullRequest.remove(pr) ) {
    		return true ;
    	}
    	return false ;
    }
    public void requestPull( SyncPullRequest pr ) {
    	arrPullRequest.add(pr) ;
    	if( !isServiceRunning(mContext) && !serviceRunning ) {
    		serviceRunning = true ;
    		launchNextPull() ;
    	}
    	else if( serviceRunning ) {
    		// sera lancé automatiquement
    	}
    	else if( waitRunning ) {
    		// sera lancé automatiquement ??
    	}
    	else {
    		waitRunning = true ;
    		waitThread = new Thread(){
    	        public void run() {
    	        	while(true) {
    	        		if( currentThread().isInterrupted() ){
    	        			break ;
    	        		}
    	        		
    	        		if( mContext==null ) {
    	        			break ;
    	        		}
    	        		
    	        		if( !isServiceRunning(mContext) ) {
    	        			serviceRunning = true ;
    	        			waitRunning = false ;
    	        			launchNextPull() ;
    	        			break ;
    	        		}
    	        		
    	        		Log.w(TAG,"SyncService not ready yet ??") ;
    	        		
    	        		try{
    	        			Thread.sleep(1000) ;
    	        		}
    	        		catch( InterruptedException e ){
    	        			break ;
    	        		}
    	        	}
    	        };
    		};
    		waitThread.start() ;
    	}
    }
	private void launchNextPull() {
		if( isServiceRunning(mContext) ) {
			return ;
		}
		if( arrPullRequest.size() == 0 ) {
			serviceRunning = false ;
			return ;
		}
		startPullService( arrPullRequest.remove(0) ) ;
	}
	private void startPullService( SyncPullRequest pr ) {
		// **** verif : déja lancé ? ****
		if( isServiceRunning(mContext) ) {
			return ;
		}
		
		Intent intent = new Intent(mContext, SyncService.class) ;
		if( pr!=null ) {
			intent.putExtra(SyncService.PULL_REQUEST, pr) ;
		} else {
			intent.putExtra(SyncService.PUSH_DO, true) ;
		}
		intent.putExtra(SyncService.EXTRA_MESSENGER, new Messenger(mHandler));
		mContext.startService(intent) ;
	}
	
	
	public static class Result {
        private volatile boolean mRegistered;

        protected void setRegistered(boolean registered) {
            mRegistered = registered;
        }

        protected final boolean isRegistered() {
            return mRegistered;
        }
		
        
        public void onServiceEndCallback( int status, SyncPullRequest pr ) {}
	}
    public void addResultCallback(Result listener) {
        synchronized (mListeners) {
            listener.setRegistered(true);
            mListeners.add(listener);
        }
    }
    public void removeResultCallback(Result listener) {
        synchronized (mListeners) {
            listener.setRegistered(false);
            mListeners.remove(listener);
        }
    }
	
	
	
	
	
	
	public static boolean isServiceRunning( Context context ) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("za.dams.paracrm.SyncService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	public static boolean hasPendingPush(Context c){
		Cursor tmpCursor ;
		boolean retValue = false ;
		DatabaseManager mDbManager = DatabaseManager.getInstance(c) ;
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM store_file WHERE sync_is_synced IS NULL") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		
		return retValue ;
	}
	
}
