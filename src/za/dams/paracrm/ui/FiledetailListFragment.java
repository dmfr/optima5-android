package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FiledetailListFragment extends FiledetailFragment {

	private CrmFileTransaction mTransaction ;
	
	private ArrayList<HashMap<String,Object>> mList ;

	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/FiledetailListFragment";
    
    public static FiledetailListFragment newInstance(int index) {
    	FiledetailListFragment f = new FiledetailListFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);

        return f;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	// LayoutInflater mInflate = getActivity().getLayoutInflater() ;
    	
    	return inflater.inflate(R.layout.filecapture_filedetail_list, container, false ) ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	mTransaction = mManager.getTransaction() ;
    	
    	// Log.w(TAG,"My Fragment Id is "+mTransaction.getCrmFileCode() );
        mList = new ArrayList<HashMap<String,Object>>() ;
    	String[] adaptFrom = { new String("icon") ,new String("text1") , new String("text2") } ;
    	int[] adaptTo = { R.id.icon5 ,R.id.label3 , R.id.label4 } ;
        ListView mlv = (ListView) getView().findViewById(R.id.mylist) ;
        mlv.setAdapter(new SimpleAdapter(getActivity().getApplicationContext(), mList, R.layout.filecapture_filedetail_listrow, adaptFrom, adaptTo )) ;
        syncWithData() ;
        
        mlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	   public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
        		   FiledetailListFragment.this.handleClickList(position) ;
        	   }
        }) ;
    }	
	public void syncWithData(){
		int pageId = getShownIndex() ;
		
		mList.clear() ;
		HashMap<String,Object> mPoint ;
		Iterator<CrmFileTransaction.CrmFileFieldDesc> mIter = mTransaction.page_getFields(pageId).iterator() ;
		CrmFileTransaction.CrmFileFieldDesc fieldDesc ;
		int mindex = 0 ;
    	while( mIter.hasNext() ){
    		fieldDesc = mIter.next() ;
    		
    		// fieldValue =  ;
    		
    		// Log.w(TAG,"Adding") ;
    		
    		mPoint = new HashMap<String,Object>() ;
    		mPoint.put("text1",fieldDesc.fieldName.toString()) ;
    		if( mTransaction.page_getRecordFieldValue(pageId,0,mindex).isSet ) {
    			mPoint.put("text2",mTransaction.page_getRecordFieldValue(pageId,0,mindex).displayStr) ;
    		}
    		else {
    			mPoint.put("icon",R.drawable.crm_missing) ;
    		}
    		mList.add(mPoint) ;
    		mindex++ ;
    	}
        ((SimpleAdapter) ((ListView) getView().findViewById(R.id.mylist)).getAdapter()).notifyDataSetChanged() ;
        
        
        if( pageId==0 ) { // cas de la 1ere page => unmask des autres pages
        	mTransaction.links_refresh() ;
        	((FilelistFragment)getFragmentManager().findFragmentById(R.id.filelist)).syncWithData() ;
        }   
	}
	public void handleClickList(int position) {
		if( mTransaction.page_getFields(getShownIndex()).get(position).fieldIsReadonly ) {
			return ;
		}
		FragmentTransaction ft ;
		switch( mTransaction.page_getFields(getShownIndex()).get(position).fieldType ){
		case FIELD_TEXT :
			TextpickerFragment textPicker = TextpickerFragment.newInstance(getShownIndex(),0,position) ;
			textPicker.setTargetFragment(this, 0);
			// biblePicker.s
			ft = getFragmentManager().beginTransaction();

			textPicker.show(ft, "dialoggg") ;
			//ft.commit();
			break ;
		case FIELD_BIBLE :
			BiblepickerFragment biblePicker = BiblepickerFragment.newInstance(getShownIndex(),0,position) ;
			biblePicker.setTargetFragment(this, 0);
			// biblePicker.s
			ft = getFragmentManager().beginTransaction();

			biblePicker.show(ft, "dialoggg") ;
			//ft.commit();
			break ;
		case FIELD_DATE :
		case FIELD_DATETIME :
			DatetimepickerFragment datetimePicker = DatetimepickerFragment.newInstance(getShownIndex(),0,position) ;
			datetimePicker.setTargetFragment(this, 0);
			// biblePicker.s
			ft = getFragmentManager().beginTransaction();

			datetimePicker.show(ft, "dialoggg") ;
			//ft.commit();
			break ;
		}
	}
}
