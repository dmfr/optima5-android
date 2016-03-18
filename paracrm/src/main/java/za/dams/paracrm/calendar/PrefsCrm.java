package za.dams.paracrm.calendar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PrefsCrm {
	private static final String TAG = "Calendar/PrefsCrm";
	private static final String SHARED_PREFS_NAME = "Calendar";
	
	
	public static boolean isCalendarEnabled(Context context, String calendarFilecode) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        String prefKey = calendarFilecode+"__accounts" ;
        return prefs.getBoolean(prefKey, false);
	}
    public static void setCalendarEnabled(Context context, String calendarFilecode, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        String prefKey = calendarFilecode+"__accounts" ;
        SharedPreferences.Editor prefsEditor = prefs.edit() ;
        
        prefsEditor.putBoolean(prefKey, enabled) ;
        prefsEditor.commit() ;
    }
	
	
	
	
    public static Set<String> getSubscribedAccounts(Context context, String calendarFilecode) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        String prefKey = calendarFilecode+"__accounts" ;
        return prefs.getStringSet(prefKey, new HashSet<String>());
    }
    public static void setSubscribedAccounts(Context context, String calendarFilecode, Set<String> set) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
        String prefKey = calendarFilecode+"__accounts" ;
        SharedPreferences.Editor prefsEditor = prefs.edit() ;
        
        prefsEditor.putStringSet(prefKey, set) ;
        prefsEditor.commit() ;
    }
    
    
    public static HashMap<String,Integer> getAccountsColor( Context context, String calendarFilecode ) {
    	SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
    	String prefKey = calendarFilecode+"__colors" ;
    	String backup = prefs.getString(prefKey, "");
    	HashMap<String, Integer> map = new HashMap<String, Integer>();
    	for (String pairs : backup.split("\\|") ) {
    		if( pairs.equals("") ) {
    			continue ;
    		}
    		String[] indiv = pairs.split("\\=");
    		map.put(indiv[0], Integer.parseInt(indiv[1]));
    	}
    	return map;
    }
    public static void setAccountsColor( Context context, String calendarFilecode, Map<String,Integer> map ) {
    	SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME,0);
    	String prefKey = calendarFilecode+"__colors" ;
    	SharedPreferences.Editor prefsEditor = prefs.edit() ;

    	StringBuilder sb = new StringBuilder();
    	for (String key : map.keySet()) {
    		sb.append(key).append("=").append(map.get(key)).append("|");
    	}

    	prefsEditor.putString(prefKey, sb.toString()) ;
    	prefsEditor.commit() ;
    }


}
