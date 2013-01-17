package za.dams.paracrm.explorer;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;

public class CrmMediaLoader {
	
	private static CrmMediaLoader sInstance ;
	
	private Context mContext;
	private Handler mHandler;
	
	private ArrayList<Listener> mListeners = new ArrayList<Listener>();
	
	private ArrayList<String> mPendingDownloadRequests ;
	
    public interface Listener {
    	public void onDownloadComplete(String syncVuid);
    }
	
    public static synchronized CrmMediaLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmMediaLoader(context,new Handler()) ;
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
    protected CrmMediaLoader(Context context, Handler handler) {
        mContext = context.getApplicationContext() ;
        mHandler = handler ;
        
        mPendingDownloadRequests = new ArrayList<String>() ;
    }
    
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
	

}
