package za.dams.paracrm;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;


public class CrmFileTransactionManager {
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/CrmFileTransactionManager";
	
	private static CrmFileTransactionManager _instance = null;
	
	private Context mContext ;
	private ArrayList<CrmFileTransaction> tTransactions ;

	private CrmFileTransactionManager( Context c ) {
		mContext = c ;
		//Log.w(TAG, mContext.toString()) ;
		tTransactions = new ArrayList<CrmFileTransaction>();
	}

	public static synchronized CrmFileTransactionManager getInstance( Context c ) {
		if (_instance == null) {
			_instance = new CrmFileTransactionManager(c);
		}
		return _instance;
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
	
	
}
