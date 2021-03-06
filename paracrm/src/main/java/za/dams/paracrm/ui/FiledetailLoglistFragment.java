package za.dams.paracrm.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.HttpServerHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import za.dams.paracrm.widget.DatetimePickerDialog;
import za.dams.paracrm.widget.DatetimePickerDialog.OnDatetimeSetListener;
import za.dams.paracrm.widget.InputPickerDialog;
import za.dams.paracrm.widget.InputPickerDialog.OnInputSetListener;
import android.app.DialogFragment;
import android.app.Fragment;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FiledetailLoglistFragment extends FiledetailFragment {

	private CrmFileTransaction mTransaction ;
	
	private ArrayList<HashMap<String,Object>> mList ;
	int mPivotPosition ;

	private static final String TAG = "PARACRM/UI/FiledetailLoglistFragment";
    
    public static FiledetailLoglistFragment newInstance(int index) {
    	FiledetailLoglistFragment f = new FiledetailLoglistFragment();

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
        		   FiledetailLoglistFragment.this.handleClickList(position) ;
        	   }
        }) ;
    }	
	@Override
	public void onPause() {
		Fragment dialogFragment = getFragmentManager().findFragmentByTag("dialog") ;
		if( dialogFragment != null ) {
			((DialogFragment)dialogFragment).dismiss() ;
		}
		
		super.onPause() ;
	}
	public synchronized void syncWithData(){
		int pageId = getShownIndex() ;
		
		mList.clear() ;
		HashMap<String,Object> mPoint ;
		Iterator<CrmFileTransaction.CrmFileFieldDesc> mIter ;
		Iterator<CrmFileTransaction.CrmFileRecord> mIterData = mTransaction.pageTable_getRecords( pageId ).iterator() ;
		CrmFileTransaction.CrmFileFieldDesc fieldDesc ;
		CrmFileTransaction.CrmFileRecord record ;
		int mindex = 0 ;
		int a = 0 ;
    	while( mIterData.hasNext() ){
    		record = mIterData.next() ;
    		
    		// fieldValue =  ;
    		
    		// Log.w(TAG,"Adding") ;
    		
    		mPoint = new HashMap<String,Object>() ;
    		
    		ArrayList<String> pretty1 = new ArrayList<String>() ; 
    		
    		mIter = mTransaction.page_getFields(pageId).iterator() ;
    		a=0 ;
    		while(mIter.hasNext()) {
    			fieldDesc = mIter.next() ;
    			if( fieldDesc.fieldIsPivot ) {
    				mPivotPosition = a ;
    				pretty1.add(fieldDesc.fieldName) ;
    	    		if( mTransaction.page_getRecordFieldValue(pageId,mindex,a).isSet ) {
    	    			mPoint.put("text2",mTransaction.page_getRecordFieldValue(pageId,mindex,a).displayStr) ;
    	    		}
    	    		else {
    	    			mPoint.put("icon",R.drawable.crm_missing) ;
    	    		}
    			}
    			else {
    				if( mindex == 0 && fieldDesc.fieldSearchIsCondition ) {
    					//pretty1.add(mTransaction.page_getRecordFieldValue(pageId,mindex,a).displayStr) ;
    				}
    				if( mindex != 0 && !fieldDesc.fieldSearchIsCondition ) {
    					pretty1.add(mTransaction.page_getRecordFieldValue(pageId,mindex,a).displayStr) ;
    				}
    			}
    			a++ ;
    		}
    		mPoint.put("text1",implodeArray(pretty1.toArray(pretty1.toArray(new String[pretty1.size()]))," / ")) ;
    		
    		
    		
    		if( mindex > 0 && ( mPoint.get("text2") == null || mPoint.get("text2").equals("") ) ){
    			// valeur à blanc => skip
    		}
    		else{
    			mList.add(mPoint) ;
    		}
    		mindex++ ;
    	}
        ((SimpleAdapter) ((ListView) getView().findViewById(R.id.mylist)).getAdapter()).notifyDataSetChanged() ;
        
        if( mTransaction.list_getAllPages().get(pageId).loadIsLoadable
        		&& !mTransaction.list_getAllPages().get(pageId).loadIsLoaded ) {
        	mTransaction.list_getAllPages().get(pageId).loadIsLoaded = true ;
        	new PopulateLogTask().execute() ;
        }
	}
	public void handleClickList(int position) {
		if( position != 0 ){
			return ;
		}
		
		int targetPageId = getShownIndex() ;
		int targetRecordId = 0 ;
		int targetFieldId = mPivotPosition ;
		
		String targetTitle = mTransaction.page_getFields(targetPageId).get(targetFieldId).fieldName ;
		Bundle dialogBundle = new Bundle() ;
		dialogBundle.putString("title",targetTitle) ;

		InputPickerDialog ipd ;
		switch( mTransaction.page_getFields(getShownIndex()).get(mPivotPosition).fieldType ){
		case FIELD_TEXT :
			String curString = mTransaction.page_getRecordFieldValue( targetPageId , targetRecordId, targetFieldId ).valueString ;
			
			ipd = new InputPickerDialog(getActivity(), new InputListener(targetPageId,targetRecordId,targetFieldId), InputPickerDialog.TYPE_TEXT, curString ) ;
			ipd.setArguments(dialogBundle) ;
            ipd.show(getFragmentManager(), "dialog") ;
			break ;
		case FIELD_BIBLE :
			BibleHelper.BibleCode bibleCode = new BibleHelper.BibleCode(mTransaction.page_getFields(targetPageId).get(targetFieldId).fieldLinkBible) ;
			ArrayList<BibleHelper.BibleEntry> bibleConditions = mTransaction.links_getBibleConditions() ;
            BiblePickerDialog bpd = new BiblePickerDialog(getActivity(), new BibleListener(targetPageId,targetRecordId,targetFieldId), bibleCode, bibleConditions);
            //bpd.setTargetFragment(this, 0);
            //bpd.setCanceledOnTouchOutside(true);
            bpd.show(getFragmentManager(), "dialog") ;
            
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
			datetimePicker.setListener(dateListener) ;
			datetimePicker.show(getFragmentManager(), "dialog") ;
			break ;
		}
	}
	
    private static String implodeArray(String[] inputArray, String glueString) {

    	/** Output variable */
    	String output = "";

    	if (inputArray.length > 0) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(inputArray[0]);

    		for (int i=1; i<inputArray.length; i++) {
    			sb.append(glueString);
    			sb.append(inputArray[i]);
    		}

    		output = sb.toString();
    	}

    	return output;
    }
    
    
    private class PopulateLogTask extends AsyncTask<Void, Integer, Boolean> {
    	protected void onPreExecute(){
    		FiledetailLoglistFragment.this.getView().findViewById(R.id.myprogress).setVisibility(View.VISIBLE) ;
    	}
    	
        protected Boolean doInBackground(Void... Params ) {
        	if( !FiledetailLoglistFragment.this.isAdded() ) {
        		//Log.w(TAG,"Not attached !!!");
        		return new Boolean(false) ;
        	}
       	
        	int pageId = FiledetailLoglistFragment.this.getShownIndex() ;
        	
        	JSONObject jsonSort = new JSONObject() ;
        	JSONObject jsonFilter = new JSONObject() ;
        	try {
        		
        		CrmFileTransaction.CrmFileRecord pivotRecord = mTransaction.pageTable_getRecords( pageId ).get(0) ;
        		Iterator<CrmFileTransaction.CrmFileFieldDesc> mIter = mTransaction.page_getFields(pageId).iterator() ;
        		while(mIter.hasNext()){
        			CrmFileTransaction.CrmFileFieldDesc fieldDesc = mIter.next() ;
        			if( fieldDesc.fieldSearchIsCondition ){
        				jsonFilter.putOpt("type", "string") ;
        				jsonFilter.putOpt("field", "field_"+fieldDesc.fieldCode) ;
        				jsonFilter.putOpt("value", pivotRecord.recordData.get(fieldDesc.fieldCode).valueString) ;
        			}
        		}
        		
        		jsonSort.putOpt("property","filerecord_id") ;
        		jsonSort.putOpt("direction","DESC") ;
        	} catch (JSONException e) {
        		// TODO Auto-generated catch block
        		//e.printStackTrace();
        	}
        	
        	HashMap<String,String> postParams = new HashMap<String,String>() ;
        	postParams.put("_action", "android_getFileGrid_data");
        	postParams.put("file_code", mTransaction.list_getAllPages().get(pageId).fileCode );
        	postParams.put("sort", new JSONArray().put(jsonSort).toString() );
        	postParams.put("filter", new JSONArray().put(jsonFilter).toString() );
        	
	        final HttpClient httpclient = HttpServerHelper.getHttpClient(getActivity(), HttpServerHelper.TIMEOUT_DL) ;
	        final HttpPost httppost = HttpServerHelper.getHttpPostRequest(getActivity(), postParams);
        	
        	try {
        		Thread.sleep(100);
        	} catch (InterruptedException e) {
        	}

        	String response = new String() ;
        	try {
				HttpResponse httpresponse = httpclient.execute(httppost);
				HttpEntity entity = httpresponse.getEntity();
				InputStream content = entity.getContent();
				response= HttpServerHelper.readStream(content) ;
        	}
        	catch(Exception e){
        		//e.printStackTrace() ;
        		//Log.w("Bin upload","Failed 3") ;
        	}finally {
        		if ((httpclient instanceof AndroidHttpClient)) {
        			((AndroidHttpClient) httpclient).close();
        		}
        	}

        	try {
        		Thread.sleep(100);
        	} catch (InterruptedException e) {
        	}

        	// Log.w(TAG,"Response from server : "+response) ;
        	try {
				mTransaction.page_populateRecordsFromJSON( pageId, new JSONObject(response).getJSONArray("data") ) ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}

        	return new Boolean(true) ;
        }

        protected void onProgressUpdate(Integer... progress ) {
            // setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Boolean myBool) {
        	if( !FiledetailLoglistFragment.this.isAdded() ) {
        		//Log.w(TAG,"Post exec : Not attached !!!");
        		return ;
        	}
        	
        	FiledetailLoglistFragment.this.getView().findViewById(R.id.myprogress).setVisibility(View.GONE) ;
        	
        	int pageId = FiledetailLoglistFragment.this.getShownIndex() ;
        	mTransaction.list_getAllPages().get(pageId).loadIsLoaded = true ;
        	FiledetailLoglistFragment.this.syncWithData() ;
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
        	FiledetailLoglistFragment.this.syncWithData() ;
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

        	FiledetailLoglistFragment.this.syncWithData() ;
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
        	FiledetailLoglistFragment.this.syncWithData() ;
    	}
    	public void onInputSet( int i ) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    		mTransaction.page_setRecordFieldValue_number( pageId , recordId, fieldId, (float)i ) ;
    		FiledetailLoglistFragment.this.syncWithData() ;
    	}
    	public void onInputSet( float f ) {
        	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    		mTransaction.page_setRecordFieldValue_number( pageId , recordId, fieldId, (float)f ) ;
    		FiledetailLoglistFragment.this.syncWithData() ;
    	}
    }

}
