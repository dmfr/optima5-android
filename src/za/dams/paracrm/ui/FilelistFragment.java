package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FilelistFragment extends ListFragment {
    boolean mDualPane;
    int mCurCheckPosition = 0;
    
	private CrmFileTransaction mTransaction ;
	
	private ArrayList<HashMap<String,Object>> mList ;

	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/FilelistFragment";
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.filecapture_filelist, container, false ) ;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	mTransaction = mManager.getTransaction() ;
    	// Log.w(TAG,"dsffd : "+mTransaction.getCrmFileCode() ) ;
    	
    	// Log.w(TAG,"My Fragment Id is "+mTransaction.getCrmFileCode() );
        
        mList = new ArrayList<HashMap<String,Object>>() ;
    	String[] adaptFrom = { new String("tab_icon") , new String("tab_lib") } ;
    	int[] adaptTo = { R.id.icon , R.id.label } ;
        setListAdapter(new SimpleAdapter(getActivity().getApplicationContext(), mList, R.layout.filecapture_filelist_row, adaptFrom, adaptTo )) ;
        syncWithData() ;

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.filedetail);
        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            showDetails(mCurCheckPosition);
        }
        else {
        	showDetails(mCurCheckPosition);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
    }
    
    public void syncWithData(){
    	mList.clear();
    	
    	// Log.w(TAG,"dsffd : "+mTransaction.getCrmFileCode() ) ;
    	
        HashMap<String,Object> mMap ;
        int pageId = -1 ;
        CrmFileTransaction.CrmFilePageinfo tFileinfo ;
        Iterator<CrmFileTransaction.CrmFilePageinfo> mIter = mTransaction.list_getAllPages().iterator() ;
    	while( mIter.hasNext() ){
    		pageId++ ;
    		tFileinfo = mIter.next() ;
    		
    		if( tFileinfo.pageIsHidden ){
    			continue ;
    		}
    		
    		mMap = new HashMap<String,Object>() ;
    		mMap.put("tab_id", new Integer(pageId)) ;
    		mMap.put("tab_code", tFileinfo.pageCode ) ;
    		mMap.put("tab_lib", tFileinfo.pageLib ) ;
    		if(tFileinfo.pageType == CrmFileTransaction.PageType.PAGETYPE_SAVE ) {
    			mMap.put("tab_icon", R.drawable.crm_filesave) ;
    		}
    		else{
    			if(tFileinfo.fileIsSubfile) {
    				mMap.put("tab_icon", R.drawable.crm_filechild) ;
    			}
    			else {
    				mMap.put("tab_icon", R.drawable.crm_fileparent) ;
    			}
    		}
    		mList.add(mMap) ;
    	}
    	
		SimpleAdapter mAdapt = (SimpleAdapter) getListAdapter() ;
    	mAdapt.notifyDataSetChanged() ;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails( position );
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    void showDetails(int index) {
        mCurCheckPosition = index;
        
        //Log.w(TAG,"Changed! But Fragment Id is "+mCurCheckPosition );
        
        int tab_to_show = ((Integer)mList.get(index).get("tab_id")).intValue() ;
        //Log.w(TAG,"Tab to show is "+tab_to_show );
        CrmFileTransaction.PageType pageType = mTransaction.list_getPageType(tab_to_show) ;
        

        if (mDualPane || true) {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            getListView().setItemChecked(index, true);
            
            // POP the back stack
            FragmentManager fm = getFragmentManager() ;
            for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                fm.popBackStack();
            }
            
            
            // Check what fragment is currently shown, replace if needed.
            FiledetailFragment details = (FiledetailFragment) getFragmentManager().findFragmentById(R.id.filedetail);
            if (details == null || details.getShownIndex() != tab_to_show) {
            	//Log.w(TAG,"Creating a fragment !" );
            	
                // Make new fragment to show this selection.
                details = FiledetailFragmentFactory.getFiledetailFragment(tab_to_show, pageType);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.filedetail, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
        	/*
            Intent intent = new Intent();
            intent.setClass(getActivity(), DetailsActivity.class);
            intent.putExtra("index", index);
            startActivity(intent);
            */
        }
    }
	
}
