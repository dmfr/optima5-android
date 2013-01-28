package za.dams.paracrm.explorer;

import za.dams.paracrm.DatabaseManager;
import android.content.Context;
import android.database.Cursor;

public class CrmExplorerConfig {
	private static CrmExplorerConfig sInstance;
	
	private final Context mContext;
	
	private final boolean mAccountIsOn ;
	private final String mAccountBibleCode ;
	
    public static synchronized CrmExplorerConfig getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmExplorerConfig(context);
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
	private CrmExplorerConfig( Context context ) {
		mContext = context.getApplicationContext() ;
	
		DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
		Cursor c ;
		
		c = mDb.rawQuery("SELECT account_is_on, account_linkbible FROM input_explorer_cfg WHERE explorercfg_id='0'") ;
		if( c.moveToNext() ) {
			mAccountIsOn = c.getString(0).equals("O") ;
			mAccountBibleCode = c.getString(1);
		} else {
			mAccountIsOn = false ;
			mAccountBibleCode = null;
		}
		c.close() ;
	}

	
	public boolean accountIsOn() {
		return mAccountIsOn ;
	}
	public String accountGetBibleCode() {
		if( accountIsOn() ) {
			return mAccountBibleCode ;
		}
		return null ;
	}
}
