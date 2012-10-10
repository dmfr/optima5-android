package za.dams.paracrm;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class UploadServiceHelper {
	
	private static boolean hasPendingRequest = false ;
	
	public static boolean hasPendingUploads(Context c){
		Cursor tmpCursor ;
		boolean retValue = false ;
		DatabaseManager mDbManager = DatabaseManager.getInstance(c) ;
		
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
			if ("za.dams.paracrm.UploadService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	public static void launchUpload( Context context ) {
		// Log.w("ParacrmUploadHelper","Launch upload requested") ;
		
		// **** verif : déja lancé ? ****
		if( isServiceRunning(context) ) {
			//Log.w("UploadHelper","Service running ?") ;
			return ;
		}
		if( !hasPendingUploads(context) ) {
			//Log.w("UploadHelper","No pending uploads ?") ;
			return ;
		}
		Intent intent = new Intent(context, UploadService.class) ;
		context.startService(intent) ;
	}

}
