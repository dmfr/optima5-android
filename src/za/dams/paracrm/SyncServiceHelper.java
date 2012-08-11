package za.dams.paracrm;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class SyncServiceHelper {
	
	private static boolean hasPendingRequest = false ;
	
	public static boolean hasPendingUploads(Context c){
		Cursor tmpCursor ;
		boolean retValue = false ;
		DatabaseManager mDbManager = DatabaseManager.getInstance(c) ;
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM store_file") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM store_file_field") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		tmpCursor = mDbManager.rawQuery("SELECT count(*) FROM upload_media") ;
		tmpCursor.moveToNext() ;
		if( tmpCursor.getInt(0) > 0 ) {
			retValue = true ;
		}
		tmpCursor.close() ;
		
		return retValue ;
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
	
	public static void launchSync( Context context ) {
		// Log.w("ParacrmSyncHelper","Launch sync requested") ;
		
		// **** verif : déja lancé ? ****
		if( isServiceRunning(context) ) {
			return ;
		}
		
		context.startService(new Intent(context, SyncService.class)) ;
	}

}
