package za.dams.paracrm;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class CrmFileTransactionManager {
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/CrmFileTransactionManager";
	private static final String SHARED_PREFS_NAME = "CrmFileTransactionManager" ;
	private static final String SHARED_PREFS_KEY  = "jsonSavedInstance" ;
	
	
	private static CrmFileTransactionManager _instance = null;
	
	private Context mContext ;
	private ArrayList<CrmFileTransaction> tTransactions ;
	private long tForwardedEventId = -1 ;

	private CrmFileTransactionManager( Context c ) {
		mContext = c ;
		//Log.w(TAG, mContext.toString()) ;
		tTransactions = new ArrayList<CrmFileTransaction>();
	}
	private CrmFileTransactionManager( Context c , JSONObject jsonObject ) {
		mContext = c ;
		try {
			tForwardedEventId = jsonObject.getLong("tForwardedEventId");
			
			tTransactions = new ArrayList<CrmFileTransaction>();
			JSONArray jsonArrCft = jsonObject.getJSONArray("tTransactions") ;
			if( jsonArrCft != null ) {
				for( int idx=0 ; idx<jsonArrCft.length() ; idx++ ) {
					JSONObject jsonCft = jsonArrCft.getJSONObject(idx) ;
					if( jsonCft != null ) {
						CrmFileTransaction cft = new CrmFileTransaction( c , jsonCft ) ;
						tTransactions.add(cft) ;
					}
				}
			}
		} catch( JSONException e ) {
			e.printStackTrace() ;
		}
	}

	public static synchronized CrmFileTransactionManager getInstance( Context c ) {
		if (_instance == null) {
			SharedPreferences prefs = c.getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE);
			try {
				String jsonString = prefs.getString(SHARED_PREFS_KEY, "") ;
				JSONObject jsonObject = new JSONObject(jsonString) ;
				_instance = new CrmFileTransactionManager(c,jsonObject) ;
			}
			catch( Exception e ) {
				_instance = new CrmFileTransactionManager(c);
			}
		}
		return _instance;
	}
	public static synchronized void saveInstance( Context c ) {
		if( _instance != null ) {
			JSONObject jsonObject = _instance.toJSONObject() ;
			if( jsonObject != null ) {
				SharedPreferences prefs = c.getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE);
				SharedPreferences.Editor prefsEditor = prefs.edit() ;
				prefsEditor.putString( SHARED_PREFS_KEY , jsonObject.toString() ) ;
				prefsEditor.commit() ;
			}
		}
	}
	public static synchronized void purgeInstance( Context c ) {
		_instance = null ;
		SharedPreferences prefs = c.getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit() ;
		prefsEditor.remove(SHARED_PREFS_KEY) ;
		prefsEditor.commit() ;
	}
	
	public int getNbTransactions() {
		return tTransactions.size();
	}
	
	public CrmFileTransaction newTransaction( int CrmInputScenId ) {
		if( getNbTransactions() == 0 ) {
			CrmFileTransaction tTransaction = new CrmFileTransaction(mContext,CrmInputScenId) ;
			// tTransaction.dummyLoad() ;
			tTransactions.add(tTransaction) ;
			return tTransactions.get(0) ;
		}
		else{
			if( tTransactions.get(0).getCrmInputScenId() == CrmInputScenId ) {
				return tTransactions.get(0) ;
			}
			tTransactions.clear();
			CrmFileTransaction tTransaction = new CrmFileTransaction(mContext,CrmInputScenId) ;
			// tTransaction.dummyLoad() ;
			tTransactions.add(tTransaction) ;
			return tTransactions.get(0) ;
		}
	}
	
	
	public CrmFileTransaction getTransaction() {
		if( getNbTransactions() == 0 ) {
			return null ;
		}
		else{
			return tTransactions.get(0) ;
		}
	}
	
	public void purgeTransactions() {
		tTransactions.clear();
	}
	
	
	
	
	public void setForwardedEventId(long eventId) {
		tForwardedEventId = eventId ;
	}
	public void clearForwardedEventId() {
		tForwardedEventId = -1 ;
	}
	public long getForwardedEventId() {
		return tForwardedEventId ;
	}
	
	
	public JSONObject toJSONObject() {
		try {
			JSONObject jsonObj = new JSONObject() ;
			jsonObj.put("tForwardedEventId", tForwardedEventId) ;
			JSONArray jsonArr = new JSONArray() ;
			for( CrmFileTransaction cft : tTransactions ) {
				if( cft != null ) {
					//Log.w(TAG,cft.toJSONObject().toString(3)) ;
					jsonArr.put( cft.toJSONObject() ) ;
				}
			}
			jsonObj.put("tTransactions", jsonArr) ;
			
			return jsonObj ;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null ;
		}
	}
}
