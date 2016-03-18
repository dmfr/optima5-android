package za.dams.paracrm.ui;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import android.app.Fragment;
import android.os.Bundle;

public class FiledetailFragment extends Fragment {
	
	int cache_pageInstanceTag=-1 ;

	public static FiledetailFragment newInstance(int index) {
		FiledetailFragment f = new FiledetailFragment();
	
	    // Supply index input as an argument.
	    Bundle args = new Bundle();
	    args.putInt("index", index);
	    f.setArguments(args);
	
	    return f;
	}
	
    public int getShownIndex() {
    	if( getArguments() != null ) {
    		return getArguments().getInt("index", 0);
    	}
        return -1 ;
    }
    public int getPageInstanceTag() {
    	if( cache_pageInstanceTag > 0 ) {
    		return cache_pageInstanceTag ;
    	}
    	if( getShownIndex() >= 0 ) {
    		CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    		CrmFileTransaction mTransaction = mManager.getTransaction() ;
    		cache_pageInstanceTag = mTransaction.list_getPageInstanceTag(getShownIndex()) ;
    		return cache_pageInstanceTag; 
    	}
    	return 0 ;
    }
    public void syncWithData() {
    	
    }
	
}
