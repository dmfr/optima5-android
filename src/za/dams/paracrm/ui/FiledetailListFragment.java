package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import za.dams.paracrm.widget.InputPickerDialog;
import za.dams.paracrm.widget.InputPickerDialog.OnInputSetListener;
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
		int mindex = -1 ;
    	while( mIter.hasNext() ){
    		mindex++ ;
    		fieldDesc = mIter.next() ;
    		
    		// fieldValue =  ;
    		if( fieldDesc.fieldAutovalueIsOn ) {
    			continue ;
    		}
    		
    		// Log.w(TAG,"Adding") ;
    		
    		mPoint = new HashMap<String,Object>() ;
    		mPoint.put("mindex", new Integer(mindex)) ;
    		mPoint.put("text1",fieldDesc.fieldName.toString()) ;
    		if( mTransaction.page_getRecordFieldValue(pageId,0,mindex).isSet ) {
    			mPoint.put("text2",mTransaction.page_getRecordFieldValue(pageId,0,mindex).displayStr) ;
    		}
    		else {
    			mPoint.put("icon",R.drawable.crm_missing) ;
    		}
    		mList.add(mPoint) ;
    	}
        ((SimpleAdapter) ((ListView) getView().findViewById(R.id.mylist)).getAdapter()).notifyDataSetChanged() ;
        
        ListView listView = (ListView) getView().findViewById(R.id.mylist) ;
        
        za.dams.paracrm.widget.Utils.setListViewHeightBasedOnChildren( listView ) ;
         
        if( true ) { // cas de la 1ere page => unmask des autres pages
        	mTransaction.links_refresh() ;
        	((FilelistFragment)getFragmentManager().findFragmentById(R.id.filelist)).syncWithData() ;
        }   
	}
	public void handleClickList(int position) {
		int mindex = (Integer) mList.get(position).get("mindex") ;

		int targetPageId = getShownIndex() ;
		int targetRecordId = 0 ;
		int targetFieldId = mindex ;
		
		String targetTitle = mTransaction.page_getFields(targetPageId).get(targetFieldId).fieldName ;
		Bundle dialogBundle = new Bundle() ;
		dialogBundle.putString("title",targetTitle) ;
		
		if( mTransaction.page_getFields(getShownIndex()).get(mindex).fieldIsReadonly ) {
			return ;
		}
		FragmentTransaction ft ;
		InputPickerDialog ipd ;
		switch( mTransaction.page_getFields(getShownIndex()).get(mindex).fieldType ){
		case FIELD_TEXT :
			String curString = mTransaction.page_getRecordFieldValue( targetPageId , targetRecordId, targetFieldId ).valueString ;
			
			ipd = new InputPickerDialog(getActivity(), new InputListener(targetPageId,targetRecordId,targetFieldId), InputPickerDialog.TYPE_TEXT, curString ) ;
			ipd.setArguments(dialogBundle) ;
            ft = getFragmentManager().beginTransaction();
            ipd.show(getFragmentManager(), "dialog") ;
			break ;
			
		case FIELD_BIBLE :
			BibleHelper.BibleCode bibleCode = new BibleHelper.BibleCode(mTransaction.page_getFields(targetPageId).get(targetFieldId).fieldLinkBible) ;
			ArrayList<BibleHelper.BibleEntry> bibleConditions = mTransaction.links_getBibleConditions() ;
            BiblePickerDialog bpd = new BiblePickerDialog(getActivity(), new BibleListener(targetPageId,targetRecordId,targetFieldId), bibleCode, bibleConditions);
            ft = getFragmentManager().beginTransaction();
            bpd.show(ft, "dialog") ;
			break ;
			
		case FIELD_DATE :
		case FIELD_DATETIME :
			DatetimepickerFragment datetimePicker = DatetimepickerFragment.newInstance(getShownIndex(),0,mindex) ;
			datetimePicker.setTargetFragment(this, 0);
			// biblePicker.s
			ft = getFragmentManager().beginTransaction();

			datetimePicker.show(ft, "dialoggg") ;
			//ft.commit();
			break ;
			
		case FIELD_NUMBER :
			float curFloat = mTransaction.page_getRecordFieldValue( targetPageId , targetRecordId, targetFieldId ).valueFloat ;
			
			ipd = new InputPickerDialog(getActivity(), new InputListener(targetPageId,targetRecordId,targetFieldId), InputPickerDialog.TYPE_INTEGER, curFloat ) ;
			ipd.setArguments(dialogBundle) ;
            ft = getFragmentManager().beginTransaction();
            ipd.show(getFragmentManager(), "dialog") ;
			break ;
		}
	}
	
	
	
    private class BibleListener implements OnBibleSetListener {
    	int pageId ;
    	int recordId ;
    	int fieldId ;

    	public BibleListener(int pageId, int recordId, int fieldId) {
    		this.pageId = pageId ;
    		this.recordId = recordId ;
    		this.fieldId = fieldId ;
    	}
    	
        public void onBibleSet(BibleEntry be) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
        	
        	if( be == null ) {
        		mTransaction.page_setRecordFieldValue_unset(pageId,recordId,fieldId) ;
        	}
        	else {
        		String selectedEntryKey = be.entryKey ;
        		mTransaction.page_setRecordFieldValue_bible(pageId,recordId,fieldId, selectedEntryKey ) ;
        	}

        	FiledetailListFragment.this.syncWithData() ;
        }
    }
    private class InputListener implements OnInputSetListener {
    	int pageId ;
    	int recordId ;
    	int fieldId ;

    	public InputListener(int pageId, int recordId, int fieldId) {
    		this.pageId = pageId ;
    		this.recordId = recordId ;
    		this.fieldId = fieldId ;
    	}
    	
    	public void onInputSet( String s ) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
        	mTransaction.page_setRecordFieldValue_text( pageId , recordId, fieldId, s ) ;
        	FiledetailListFragment.this.syncWithData() ;
    	}
    	public void onInputSet( int i ) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    		mTransaction.page_setRecordFieldValue_number( pageId , recordId, fieldId, (float)i ) ;
        	FiledetailListFragment.this.syncWithData() ;
    	}
    	public void onInputSet( float f ) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    		mTransaction.page_setRecordFieldValue_number( pageId , recordId, fieldId, (float)f ) ;
        	FiledetailListFragment.this.syncWithData() ;
    	}
    }
}
