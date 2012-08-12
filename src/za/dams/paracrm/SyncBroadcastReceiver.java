package za.dams.paracrm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SyncBroadcastReceiver extends BroadcastReceiver {
	
	private OnSyncListener mListener ;

	public interface OnSyncListener {
		void onSyncStarted() ;
		void onSyncComplete(boolean hasChanged) ;
	}
	
	public SyncBroadcastReceiver( OnSyncListener listener ){
		mListener = listener ;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(action.equalsIgnoreCase(SyncService.SYNCSERVICE_BROADCAST)){    
			Bundle extra = intent.getExtras();
			int status = extra.getInt(SyncService.SYNCSERVICE_STATUS,-1);
			switch( status ) {
			case SyncService.SYNCSERVICE_STARTED :
				if( mListener != null ) {
					mListener.onSyncStarted() ;
				}
				break ;
			case SyncService.SYNCSERVICE_COMPLETE :
				if( mListener != null ) {
					mListener.onSyncComplete(true) ;
				}
				break ;
			default :
				break ;
			}
		}
	}

}
