package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.xpressfile.XpressfileActivity;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import za.dams.paracrm.widget.DatetimePickerDialog;
import za.dams.paracrm.widget.DatetimePickerDialog.OnDatetimeSetListener;
import za.dams.paracrm.widget.InputPickerDialog;
import za.dams.paracrm.widget.InputPickerDialog.OnInputSetListener;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FiledetailListFragment extends FiledetailFragment implements UtilityFragment.Listener {

	private CrmFileTransaction mTransaction ;
	
	private UtilityFragment mUtilityFragment ;
	
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
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;
		mUtilityFragment = (UtilityFragment) getFragmentManager().findFragmentByTag(UtilityFragment.TAG) ;
	}
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	// LayoutInflater mInflate = getActivity().getLayoutInflater() ;
    	
    	return inflater.inflate(R.layout.filecapture_filedetail_list, container, false ) ;
    }
	@Override
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
	@Override
	public void onResume(){
		super.onResume() ;
		mUtilityFragment.registerListener(this);
		
        int fieldIdx=-1 ;
        for( CrmFileTransaction.CrmFileFieldDesc cffd : mTransaction.page_getFields(getShownIndex()) ) {
        	fieldIdx++ ;
        	if( cffd.fieldLinkfileIsOn ) {
        		CrmFileTransaction.CrmFileFieldValue cffv = mTransaction.page_getRecordFieldValue(getShownIndex(), 0, fieldIdx) ;
        		if( cffv==null ) {
        			continue ;
        		}
        		if( !cffv.xpresscheckIsDone ) {
        			mUtilityFragment.xpresscheckInvalidate(getPageInstanceTag(), fieldIdx);
        		}
        		mUtilityFragment.xpresscheckInit(getPageInstanceTag(), fieldIdx);
        	}
        }
	}
	@Override
	public void onPause() {
		mUtilityFragment.unregisterListener(this);
		
		Fragment dialogFragment = getFragmentManager().findFragmentByTag("dialog") ;
		if( dialogFragment != null ) {
			((DialogFragment)dialogFragment).dismiss() ;
		}
		
		super.onPause() ;
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
    		if( fieldDesc.fieldAutovalueIsOn && !fieldDesc.fieldLinkfileIsOn ) {
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
    		
    		if( fieldDesc.fieldLinkfileIsOn ) {
    			CrmFileTransaction.CrmFileFieldValue cffv = mTransaction.page_getRecordFieldValue(pageId,0,mindex) ;
    			if( !cffv.xpresscheckIsDone ) {
    				mPoint.put("icon",null) ;
    			} else {
    				if( cffv.xpresscheckHasRecord ) {
    					mPoint.put("icon",R.drawable.crm_foldergreen) ;
    				} else {
    					mPoint.put("icon",R.drawable.crm_missing) ;
    				}
    			}
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
		
		if( mTransaction.page_getFields(getShownIndex()).get(mindex).fieldLinkfileIsOn ) {
			mUtilityFragment.xpresscheckInvalidate(getPageInstanceTag(), mindex);
			
			int xpressfileId = mTransaction.page_getFields(getShownIndex()).get(mindex).fieldLinkfileXpressfileId ;
			String xpressfilePrimaryKey = mTransaction.page_getRecordFieldValue( targetPageId , targetRecordId, targetFieldId ).valueString ;
			launchXpressfileActivity(xpressfileId,xpressfilePrimaryKey) ;
			return ;
		}
		
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
			DateListener dateListener = new DateListener(targetPageId,targetRecordId,targetFieldId) ;
			
			boolean hideTime = (mTransaction.page_getFields(targetPageId).get(targetFieldId).fieldType == FieldType.FIELD_DATE) ;
			long boundDateMinMillis = -1 ;
			if( mTransaction.page_getFields(targetPageId).get(targetFieldId).inputCfg_dateBoundMin ) {
				boundDateMinMillis = DatetimePickerDialog.getDayOffsetMillis(mTransaction.page_getFields(targetPageId).get(targetFieldId).inputCfg_dateBoundMin_dayOffset) ;
			}
			long boundDateMaxMillis = -1 ;
			if( mTransaction.page_getFields(targetPageId).get(targetFieldId).inputCfg_dateBoundMax ) {
				boundDateMaxMillis = DatetimePickerDialog.getDayOffsetMillis(mTransaction.page_getFields(targetPageId).get(targetFieldId).inputCfg_dateBoundMax_dayOffset) ;
			}
			
			Date curDate = mTransaction.page_getRecordFieldValue( targetPageId , targetRecordId, targetFieldId ).valueDate ;
			DatetimePickerDialog datetimePicker = DatetimePickerDialog.newInstance(curDate.getYear() + 1900, curDate.getMonth(), curDate.getDate(), curDate.getHours(), curDate.getMinutes(), hideTime, boundDateMinMillis, boundDateMaxMillis);
			datetimePicker.setTitle(targetTitle);
			datetimePicker.setListener(dateListener) ;
			ft = getFragmentManager().beginTransaction();
			datetimePicker.show(ft, "dialog") ;
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
	
	
	private class DateListener implements OnDatetimeSetListener {
    	int pageId ;
    	int recordId ;
    	int fieldId ;

    	public DateListener(int pageId, int recordId, int fieldId) {
    		this.pageId = pageId ;
    		this.recordId = recordId ;
    		this.fieldId = fieldId ;
    	}

		@Override
		public void onDatetimeSet(int year, int month, int monthDay,
				int hourOfDay, int minute) {
	    	Date newDate = new Date(year-1900,month,monthDay,hourOfDay,minute) ;
			
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
        	mTransaction.page_setRecordFieldValue_date(pageId,recordId,fieldId,newDate ) ;
        	FiledetailListFragment.this.syncWithData() ;
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
    
    private void launchXpressfileActivity( int xpressfileInputId , String xpressfilePrimarykey ) {
    	final Bundle bundle = new Bundle();
		bundle.putInt(XpressfileActivity.BUNDLE_KEY_MODE, XpressfileActivity.MODE_XPRESSFILE);
		bundle.putInt(XpressfileActivity.BUNDLE_KEY_XPRESSFILE_INPUTID, xpressfileInputId);
		if( xpressfilePrimarykey != null ) {
			bundle.putString(XpressfileActivity.BUNDLE_KEY_XPRESSFILE_PRIMARYKEY, xpressfilePrimarykey);
		}
		Intent intent = new Intent(getActivity(), XpressfileActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
    }
    
	@Override
	public void onPageInstanceChanged(int pageInstanceTag) {
		if( pageInstanceTag==getPageInstanceTag() ) {
			syncWithData() ;
		}
	}
}
