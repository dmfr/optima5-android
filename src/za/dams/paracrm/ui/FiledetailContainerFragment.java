package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;

public class FiledetailContainerFragment extends FiledetailFragment {
	private CrmFileTransaction mTransaction ;
	
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/FiledetailContainerFragment";
	private ArrayList<Fragment> mFragments ;
	
	public static FiledetailContainerFragment newInstance(int index) {
		FiledetailContainerFragment f = new FiledetailContainerFragment();
	
	    // Supply index input as an argument.
	    Bundle args = new Bundle();
	    args.putInt("index", index);
	    f.setArguments(args);
	
	    return f;
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.filecapture_filedetail_container, container, false ) ;
	}
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
		CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
		mTransaction = mManager.getTransaction() ;
		
		// Log.w(TAG,"Building fragments...") ;
		buildFraments() ;
	}
	
	public void buildFraments() {
		destroyFragments() ; // on sait jamais
		
    	mTransaction.list_getPageType(getShownIndex()) ;
    	CrmFileTransaction.CrmFilePageinfo fpi = mTransaction.list_getAllPages().get(getShownIndex()) ;
    	
    	ViewGroup vg = (ViewGroup)getView().findViewById(R.id.mycontainer) ;
    	LayoutInflater li = getActivity().getLayoutInflater() ;
    	
    	//FragmentTransaction ft = getFragmentManager().beginTransaction();
    	mFragments = new ArrayList<Fragment>() ;
    	for( int a=1 ; a<=fpi.nbChildren ; a++ ) {
    		int subPageIndex = getShownIndex() + a ;
    		// Log.w(TAG,"Show child indexes "+subPageIndex) ;
    		
    		CrmFileTransaction.PageType pageType = mTransaction.list_getPageType(subPageIndex) ;
    		
    		FiledetailFragment details = FiledetailFragmentFactory.getFiledetailFragment(subPageIndex, pageType);
            // Execute a transaction, replacing any existing fragment
            // with this one inside the frame.
    		
    		View v = li.inflate(R.layout.filecapture_filedetail_container_item, null);
    		v.setId(100000+a) ;
    		vg.addView(v) ;
    		
    		FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(100000+a, details);
            mFragments.add(details) ;
            ft.commit();
    	}
    	//ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    	//ft.commit();
	}
	public void destroyFragments() {
		if( mFragments != null ) {
			FragmentManager mFragmentMgr = getFragmentManager() ;
			FragmentTransaction mTransaction = mFragmentMgr.beginTransaction();
			for( Fragment childFragment : mFragments ) {
				mTransaction.remove(childFragment);
				// Log.w(TAG,"Removing a fragment") ;
			}
			mTransaction.commit();
			mFragments = null ;
		}
	}
	
	public void onDestroyView()
    {
		destroyFragments() ;
        super.onDestroyView();
    }

}
