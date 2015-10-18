package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransaction.CrmFilePageinfo;
import za.dams.paracrm.CrmImageLoader;
import za.dams.paracrm.explorer.BibleViewFragment;
import za.dams.paracrm.explorer.CrmBibleManager;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleDesc;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleFieldDesc;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleRecord;
import za.dams.paracrm.R;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class FiledetailTablePickerFragment extends FiledetailTableFragment 
implements OnClickListener, OnItemClickListener, FileCaptureActivity.OnBackPressedListener, FileCaptureActivity.OnKeyboardShownListener, View.OnTouchListener{
	private Context mAppContext ;
	
	private CrmImageLoader mCrmImageLoader ;
	
	private List<View> mTouchMonitorUpperViews ;
	private View mTouchMonitorLastTouched ;
 	
	private View mUpperPanel;
	private View mLowerPanel;
 	
	private View mPivotPanel;
	private View mSearchPanel;
	private View mProgressPanel;
	private AutoCompleteTextView mSearchText ;
	private CompoundBarcodeView mBarcodeView ;
	private boolean mBarcodeViewActive ;
	
	BibleHelper.BibleEntry mPivotBibleEntry ;
	
	private View mPivotDirectInputGroup ;
	private TextView mPivotDirectInputText ;
	private Integer mPivotDirectInputValue ;
	private View mPivotDirectInputMinus ;
	private View mPivotDirectInputPlus ;
	private View mPivotSubmit ;
	
    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
        	String barcodeText = result.getText() ;
        	new SearchBarcodeTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, barcodeText);
        }

 		@Override
		public void possibleResultPoints(List<ResultPoint> resultPoints) {
 			
			
		}
    };
    
    
	public static FiledetailTablePickerFragment newInstance(int index) {
	FiledetailTablePickerFragment f = new FiledetailTablePickerFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putInt("index", index);
		f.setArguments(args);

		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.filecapture_filedetail_table_picker, container, false ) ;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mAppContext = getActivity().getApplicationContext() ;
		mCrmImageLoader = new CrmImageLoader(mAppContext) ;
		
		View view = getView() ;
		mUpperPanel = view.findViewById(R.id.upper) ;
		mLowerPanel = view.findViewById(R.id.lower) ;
		
		mPivotPanel = view.findViewById(R.id.pivot_panel) ;
		mSearchPanel = view.findViewById(R.id.search_panel) ;
		mProgressPanel = view.findViewById(R.id.progress_panel) ;
		
		mBarcodeView = (CompoundBarcodeView)mSearchPanel.findViewById(R.id.barcode_scanner);
		
		mSearchText = (AutoCompleteTextView)mSearchPanel.findViewById(R.id.search_text) ;
		
		mPivotDirectInputGroup = mPivotPanel.findViewById(R.id.pivot_directinput) ;
		mPivotDirectInputText = (TextView)mPivotDirectInputGroup.findViewById(R.id.pivot_directinput_text) ;
		mPivotDirectInputMinus = mPivotDirectInputGroup.findViewById(R.id.pivot_directinput_minus) ;
		mPivotDirectInputPlus = mPivotDirectInputGroup.findViewById(R.id.pivot_directinput_plus) ;
		mPivotSubmit = mPivotPanel.findViewById(R.id.pivot_submit) ;
		
		super.onActivityCreated(savedInstanceState);
		
		// Register views for touch monitoring
		mTouchMonitorUpperViews = getAllChildrenBFS( view.findViewById(R.id.upper) );
		for( View upperView : mTouchMonitorUpperViews ) {
			if( upperView instanceof EditText ) {
				upperView.setOnTouchListener(this);
			}
		}
		mPivotDirectInputMinus.setOnClickListener(this);
		mPivotDirectInputPlus.setOnClickListener(this);
		mPivotSubmit.setOnClickListener(this);
		
		// Open search :
		setPivotEntry(null) ;
	}
	
    @Override
    public void onResume() {
        super.onResume();
    	if( mBarcodeViewActive ) {
    		//mBarcodeView.resume();
    	}
    }
    @Override
    public void onPause() {
    	if( mBarcodeViewActive ) {
    		mBarcodeView.pause();
    	}
        super.onPause();
    }
    
	
	@Override
	public boolean onBackPressed() {
		if( mPivotBibleEntry != null ) {
			setPivotEntry(null) ;
			return true ;
		}
		return false ;
	}
	@Override
	public void onKeyboardShown(boolean keyboardShown) {
		if( keyboardShown ) {
			// whereIsFocus (? http://stackoverflow.com/questions/17341946/how-can-i-detect-focused-edittext-in-android
			if( this.mTouchMonitorUpperViews.contains(mTouchMonitorLastTouched) ) {
				mUpperPanel.setVisibility(View.VISIBLE);
				mLowerPanel.setVisibility(View.GONE);
			} else {
				mUpperPanel.setVisibility(View.GONE);
				mLowerPanel.setVisibility(View.VISIBLE);
			}
		} else {
			mUpperPanel.setVisibility(View.VISIBLE);
			mLowerPanel.setVisibility(View.VISIBLE);
		}
	}

	public void setPivotEntry( BibleHelper.BibleEntry bibleEntry ) {
		// Clean open elements
		stopSearch() ;
		
		// Hide keyboard
		if( getActivity().getCurrentFocus() != null ) {
			final InputMethodManager imm = (InputMethodManager)mAppContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
		}
		
		
		mPivotBibleEntry = bibleEntry ;
		
		if( bibleEntry == null ) {
			// erase & hide pivot_panel
			mProgressPanel.setVisibility(View.GONE) ;
			mPivotPanel.setVisibility(View.GONE) ;
			mSearchPanel.setVisibility(View.VISIBLE) ;
			initSearch() ;
			initScan() ;
			return ;
		}
		
		stopSearch() ;
		stopScan() ;
		mPivotPanel.setVisibility(View.GONE) ;
		mSearchPanel.setVisibility(View.GONE) ;
		mProgressPanel.setVisibility(View.VISIBLE) ;
		new SetPivotTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		return ;
	}
	private void setPivotView( CrmBibleDesc tBibleDesc, CrmBibleRecord tBibleRecord ) {
		LayoutInflater mInflater = (LayoutInflater) mAppContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		ImageView tPivotImage = (ImageView)mPivotPanel.findViewById(R.id.pivot_image) ;
		ViewGroup tPivotFields = (ViewGroup)mPivotPanel.findViewById(R.id.pivot_fields) ;
		
		tPivotImage.setImageDrawable(null);
		tPivotFields.removeAllViews();
		
		if( tBibleDesc.bibleHasGallery && tBibleRecord.defaultPhoto != null ) {
			CrmImageLoader.CrmUrl crmUrl = new CrmImageLoader.CrmUrl() ;
			crmUrl.mediaId = tBibleRecord.defaultPhoto.mediaId ;
			crmUrl.thumbnail = false ;
			
			mCrmImageLoader.download(crmUrl, tPivotImage) ;
		}
    	for( CrmBibleFieldDesc cbfd : tBibleDesc.fieldsDesc ) {
    		if( !cbfd.fieldIsHeader ) {
    			continue ;
    		}
    		
    		View row = mInflater.inflate(R.layout.filecapture_filedetail_table_picker_field, null) ;
    		String label = cbfd.fieldName ;
    		String text = tBibleRecord.recordData.get(cbfd.fieldCode).displayStr ;
    		
    		((TextView)row.findViewById(R.id.crm_label)).setText(label) ;
    		((TextView)row.findViewById(R.id.crm_text)).setText(text) ;
    		
    		tPivotFields.addView(row) ;
    	}
    	
    	// Direct input
		int pageId = getShownIndex() ;
		CrmFileTransaction.CrmFileOpenpivotParams cfopp = mTransaction.pivots_getOpenpivotParams(pageId);
		if( cfopp != null && cfopp.directInputFieldIsOn ) {
			CrmFileTransaction.CrmFileFieldDesc cffd = mTransaction.page_getFields(pageId).get(cfopp.directInputFieldId) ;
			if( cffd.fieldType == CrmFileTransaction.FieldType.FIELD_NUMBER ) {
				mPivotDirectInputGroup.setVisibility(View.VISIBLE);
				mPivotDirectInputValue = 0 ;
				syncPivotViewDirectInput() ;
			} else {
				mPivotDirectInputGroup.setVisibility(View.GONE);
			}
		} else {
			mPivotDirectInputGroup.setVisibility(View.GONE);
		}

		
		mPivotPanel.setVisibility(View.VISIBLE) ;
		mSearchPanel.setVisibility(View.GONE) ;
		mProgressPanel.setVisibility(View.GONE) ;
	}
	private void syncPivotViewDirectInput() {
		mPivotDirectInputText.setText(" "+mPivotDirectInputValue+" ");
	}
	private void submitPivot() {
		int pageId = getShownIndex() ;
		CrmFileTransaction.CrmFileOpenpivotParams cfopp = mTransaction.pivots_getOpenpivotParams(pageId);
		if( cfopp == null || mPivotBibleEntry == null ) {
			return ;
		}
		
		saveValues() ;
		
		mTransaction.page_addRecordForOpenPivot(pageId, mPivotBibleEntry);
		if( cfopp.directInputFieldIsOn ) {
			mTransaction.page_setRecordFieldValue_number(pageId, 0, cfopp.directInputFieldId, mPivotDirectInputValue);
		}
		
		mTransaction.links_refresh() ;
		syncWithData() ;
		
		setPivotEntry(null) ;
	}
	
	private void initScan() {
		mBarcodeView.setStatusText("Scan barcode...");
		mBarcodeView.decodeContinuous(callback);
		mBarcodeView.resume();
		mBarcodeViewActive = true ;
	}
	private void initSearch() {
		// Get search params (bible, conditions...)
		String tSearchedBible ;
		ArrayList<BibleHelper.BibleEntry> tSearchForeignConditions ;
		
		int pageId = getShownIndex() ;
		CrmFileTransaction.CrmFileOpenpivotParams cfopp = mTransaction.pivots_getOpenpivotParams(pageId);
		if( cfopp == null ) {
			return ;
		}
		
		BibleHelper bh = new BibleHelper(mAppContext) ;
		
		tSearchedBible = cfopp.targetBibleCode ;
		tSearchForeignConditions = null ;
		if( cfopp.foreignIsOn ) {
			tSearchForeignConditions = new ArrayList<BibleHelper.BibleEntry>() ;
			tSearchForeignConditions.add( bh.getBibleEntry(cfopp.foreignBiblecode, cfopp.foreignEntrykey) ) ;
		}
		
		mSearchText.setAdapter( new SearchBibleAdapter(mAppContext, bh, tSearchedBible, tSearchForeignConditions) ) ;
		mSearchText.setOnItemClickListener(this);
 	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if( parent.getAdapter() != mSearchText.getAdapter() ) {
			return ;
		}
		BibleHelper.BibleEntry be = (BibleHelper.BibleEntry)parent.getAdapter().getItem(position) ;
		setPivotEntry(be) ;
	}
	private void stopSearch() {
		mSearchText.setAdapter(null);
		mSearchText.setOnItemClickListener(null);
		mSearchText.setText("");
	}
	private void stopScan() {
		mBarcodeViewActive = false ;
		mBarcodeView.pause();
		mBarcodeView.getBarcodeView().stopDecoding();
	}
	
	
	private class SearchBarcodeTask extends AsyncTask<String, Void, Boolean> {
		private BibleHelper.BibleEntry mFoundBibleEntry ;
		
		protected Boolean doInBackground(String... barcodeResult) {
			mFoundBibleEntry = null ;
			if( barcodeResult.length != 1 ) {
				return false ;
			}
			
			int pageId = getShownIndex() ;
			CrmFileTransaction.CrmFileOpenpivotParams cfopp = mTransaction.pivots_getOpenpivotParams(pageId);
			if( cfopp == null ) {
				return false ;
			}
			
			BibleHelper bh = new BibleHelper(mAppContext) ;
			
			String tSearchedBible = cfopp.targetBibleCode ;
			ArrayList<BibleHelper.BibleEntry> tSearchForeignConditions = null ;
			if( cfopp.foreignIsOn ) {
				tSearchForeignConditions = new ArrayList<BibleHelper.BibleEntry>() ;
				tSearchForeignConditions.add( bh.getBibleEntry(cfopp.foreignBiblecode, cfopp.foreignEntrykey) ) ;
			}
			
			ArrayList<BibleHelper.BibleEntry> tResults = bh.queryBible(tSearchedBible, tSearchForeignConditions, barcodeResult[0], 10) ;
			
			if( tResults.size() != 1 ) {
				return false ;
			}
			
			mFoundBibleEntry = tResults.get(0) ;
			return true ;
		}
		protected void onPostExecute(Boolean isOk) {
			if( isOk ) {
				setPivotEntry(mFoundBibleEntry) ;
			} else {
				Toast.makeText(mAppContext, "Not found !", Toast.LENGTH_SHORT).show();
			}
		}
	}
    private static class SearchBibleAdapter extends ArrayAdapter<BibleHelper.BibleEntry> {
    	LayoutInflater mInflater ;
    	
    	BibleHelper mBibleHelper ;
    	String mSearchedBible ;
    	ArrayList<BibleHelper.BibleEntry> mSearchForeignConditions ;
    	
		public SearchBibleAdapter(Context context, BibleHelper bibleHelper, String searchedBible, ArrayList<BibleHelper.BibleEntry> searchForeignConditions ) {
			super(context, R.layout.filecapture_filedetail_table_picker_search_dropdown);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mBibleHelper = bibleHelper ;
			mSearchedBible = searchedBible ;
			mSearchForeignConditions = searchForeignConditions ;
		}
		
		@Override
		public Filter getFilter(){
	        Filter myFilter = new Filter() {
	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) {
	                FilterResults filterResults = new FilterResults();
	                if(constraint != null) {
	        			String typedText = constraint.toString() ;
	        			ArrayList<BibleHelper.BibleEntry> data = new ArrayList<BibleHelper.BibleEntry>();
	        			data.addAll( mBibleHelper.queryBible(mSearchedBible, mSearchForeignConditions, typedText, 10) ) ;
	        			
	                    // Now assign the values and count to the FilterResults object
	                    filterResults.values = data;
	                    filterResults.count = data.size();
	                }
	                return filterResults;
	            }

	            @SuppressWarnings("unchecked")
				@Override
	            protected void publishResults(CharSequence contraint, FilterResults results) {
	            	
	            	SearchBibleAdapter.this.clear() ;
        			if( results != null && results.values != null ) {
        				SearchBibleAdapter.this.addAll((ArrayList<BibleHelper.BibleEntry>)results.values) ;
        			}
	            	
	            }
	        };
	        return myFilter;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view ;
			
			if( convertView != null ) {
				// able to reuse view
				view = convertView ;
			} else {
				// build a new view
				view = mInflater.inflate(R.layout.filecapture_filedetail_table_picker_search_dropdown, parent, false) ;
			}
			
			BibleHelper.BibleEntry be = getItem(position) ;
			
			((TextView)view.findViewById(R.id.text_1)).setText(be.displayStr1) ;
			((TextView)view.findViewById(R.id.text_2)).setText(be.displayStr2) ;

			return view;
		}
    }
    
    /*
     * http://stackoverflow.com/questions/18668897/android-get-all-children-elements-of-a-viewgroup
     */
    private List<View> getAllChildrenBFS(View v) {
        List<View> visited = new ArrayList<View>();
        List<View> unvisited = new ArrayList<View>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mTouchMonitorLastTouched = v ;
		return false;
	}
	
	public void syncWithData(){
		super.syncWithData();
		for( View v : getAllChildrenBFS( getView().findViewById(R.id.mytable) ) ) {
			if( v instanceof EditText ) {
				v.setOnTouchListener(this);
			}
		}
	}
	
	
	private class SetPivotTask extends AsyncTask<Void, Void, Boolean> {
		private CrmBibleDesc mBibleDesc ;
		private CrmBibleRecord mBibleRecord ;
		
		protected void onPreExecute() {
			mBibleDesc = null ;
			mBibleRecord = null ;
		}
		
		protected Boolean doInBackground(Void... arg0) {
			CrmBibleManager crmBibleMan = CrmBibleManager.getInstance(mAppContext) ;
			crmBibleMan.bibleInitDescriptors() ;
			
			if( mPivotBibleEntry == null ) {
				return false ;
			}
			
			mBibleDesc = crmBibleMan.bibleGetBibleDescriptor(mPivotBibleEntry.bibleCode) ;
			mBibleRecord = crmBibleMan.biblePullData(mPivotBibleEntry.bibleCode, mPivotBibleEntry.entryKey).get(0) ;
    
			return true ;
		}
		protected void onPostExecute(Boolean isOk) {
			if( isOk ) {
				setPivotView(mBibleDesc,mBibleRecord) ;
			} else {
				setPivotEntry(null) ;
			}
		}
	}

	@Override
	public void onClick(View view) {
		if( view == mPivotDirectInputMinus ) {
			mPivotDirectInputValue-- ;
		}
		if( view == mPivotDirectInputPlus ) {
			mPivotDirectInputValue++ ;
		}
		if( view == mPivotSubmit ) {
			submitPivot() ;
		}
		syncPivotViewDirectInput() ;
	}

}
