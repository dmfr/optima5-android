package za.dams.paracrm;

import za.dams.paracrm.explorer.CrmFileManager;
import android.content.Context;
import android.content.SharedPreferences;

public class MainPreferences {
	public static final String SHARED_PREFS_NAME = "Main";
	
	private static MainPreferences sInstance;
	private final Context mContext;
	
    public static synchronized MainPreferences getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MainPreferences(context);
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
	private MainPreferences( Context context ) {
		mContext = context.getApplicationContext() ;
	}
	
	private String cachedServerFullUrl = null ;
	private int cachedServerDlTimeout = 0 ;
	private int cachedServerPullTimeout = 0 ;
	
	public String getServerFullUrl() {
		if( cachedServerFullUrl != null ) {
			return cachedServerFullUrl ;
		}
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	String url = prefs.getString("srv_url", "") ;
    	String domain = prefs.getString("srv_domain", "") ;
    	String sdomain = prefs.getString("srv_sdomain", "") ;
    	return (cachedServerFullUrl = buildServerFullUrl(url,domain,sdomain)) ;
	}
	public static String buildServerFullUrl(String url, String domain, String sdomain) {
		if (url.length() > 0 && url.charAt(url.length()-1)=='/') {
			url = url.substring(0, url.length()-1);
		}
		
		if( url.length() == 0 || domain.length() == 0 || sdomain.length() == 0 ) {
			return new String() ;
		}
		return url + "/" + domain + "/" + sdomain ;
	}
	
	public int getServerDlTimeout() {
		if( cachedServerDlTimeout > 0 ) {
			return cachedServerDlTimeout ;
		}
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	int dlTimeout = prefs.getInt("srv_dl_timeout", mContext.getResources().getInteger(R.integer.settings_srv_default_dl_timeout)) ;
    	return (cachedServerDlTimeout = dlTimeout) ;
	}
	public int getServerPullTimeout() {
		if( cachedServerPullTimeout > 0 ) {
			return cachedServerPullTimeout ;
		}
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	int pullTimeout = prefs.getInt("srv_pull_timeout", mContext.getResources().getInteger(R.integer.settings_srv_default_pull_timeout)) ;
    	return (cachedServerPullTimeout = pullTimeout) ;
	}
	
}
