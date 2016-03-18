package za.dams.paracrm.explorer;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsCrm {
	private static final String TAG = "Explorer/PrefsCrm";
	private static final String SHARED_PREFS_NAME = "Explorer";
	private static final String PREFS_KEY_ACCT_BIBLECODE = "account_bibleCode" ;
	private static final String PREFS_KEY_ACCT_ENTRYKEY = "account_entryKey" ;

	public static class ExplorerAccount {
		String bibleCode;
		String entryKey;
	}
	public static ExplorerAccount getExplorerAccount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
		if( prefs.contains(PREFS_KEY_ACCT_BIBLECODE) && prefs.contains(PREFS_KEY_ACCT_ENTRYKEY) ) {
			ExplorerAccount explorerAccount = new ExplorerAccount() ;
			explorerAccount.bibleCode = prefs.getString(PREFS_KEY_ACCT_BIBLECODE, "") ;
			explorerAccount.entryKey = prefs.getString(PREFS_KEY_ACCT_ENTRYKEY, "") ;
			return explorerAccount ;
		}
		return null ;
	}
	
	public static void setExplorerAccount(Context context, ExplorerAccount explorerAccount) {
		if( explorerAccount==null ) {
			unsetExplorerAccount(context) ;
		}
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        SharedPreferences.Editor prefsEditor = prefs.edit() ;
        prefsEditor.putString(PREFS_KEY_ACCT_BIBLECODE, explorerAccount.bibleCode);
        prefsEditor.putString(PREFS_KEY_ACCT_ENTRYKEY, explorerAccount.entryKey);
        prefsEditor.commit() ;
	}
	public static void unsetExplorerAccount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        SharedPreferences.Editor prefsEditor = prefs.edit() ;
        prefsEditor.remove(PREFS_KEY_ACCT_BIBLECODE);
        prefsEditor.remove(PREFS_KEY_ACCT_ENTRYKEY);
        prefsEditor.commit() ;
	}
}
