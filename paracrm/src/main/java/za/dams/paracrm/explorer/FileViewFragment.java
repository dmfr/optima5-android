package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.CrmImageLoader;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileDesc;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileFieldDesc;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileRecord;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FileViewFragment extends Fragment implements View.OnClickListener {
	private static final String LOGTAG = "FileViewFragment";
	private static final String BUNDLE_KEY_CURRENT_TAB = "FileViewFragment.currentTab";
	
	private Context mContext;
	private LayoutInflater mInflater ;

    /** Argument name(s) */
    private static final String ARG_FILERECORD_ID = "filerecordId";
    
	private Callback mCallback = EmptyCallback.INSTANCE;
	
	// View fields
	private View mMainView ;
	private View mLoadingProgress;
	
	private TableLayout mHeaderTable ;
	
	private ViewGroup mTabsContainer ;
	private ViewGroup mTabViewsContainer ;
	private int mCurrentTab = -1 ;
	private int mRestoredTab = -1 ;
	private ArrayList<TextView> mTabs ;
	private ArrayList<View> mTabViews ;
	
	// Adapters pour les photos
	private ArrayList<MediaAdapter> mMediaAdapters ;
	
	// CRM data
	private CrmFileDesc mMstrDesc ;
	private CrmFileRecord mMstrRecord ;
	private ArrayList<CrmFileDesc> mChildrenDescs ;
	private ArrayList<ArrayList<CrmFileRecord>> mChildrenRecords ;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
    
    public static FileViewFragment newInstance(long filerecordId) {
        if (filerecordId == 0) {
            throw new IllegalArgumentException();
        }
        final FileViewFragment instance = new FileViewFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_FILERECORD_ID, filerecordId);
        instance.setArguments(args);
        return instance;
    }
    
    
    private Long mImmutableFilerecordId;

    private void initializeArgCache() {
        if (mImmutableFilerecordId != null) return;
        mImmutableFilerecordId = getArguments().getLong(ARG_FILERECORD_ID);
    }

    /**
     * @return the message ID passed to {@link #newInstance}.  Safe to call even before onCreate.
     */
    public long getFilerecordId() {
        initializeArgCache();
        return mImmutableFilerecordId;
    }
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        
        mMediaAdapters = new ArrayList<MediaAdapter>() ;
        
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.explorer_fileview_fragment, container, false);

        mLoadingProgress = UiUtilities.getView(view, R.id.loading_progress);
        mMainView = UiUtilities.getView(view, R.id.main_panel);
        
        mHeaderTable = (TableLayout) view.findViewById(R.id.header_table) ;
        mTabsContainer = (ViewGroup) view.findViewById(R.id.file_tabs_bar) ;
        mTabViewsContainer = (ViewGroup) view.findViewById(R.id.content) ;
        
        mTabs = new ArrayList<TextView>() ;
        mTabViews = new ArrayList<View>() ;
     
        return view ;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);
        
        showContent(false) ;
        /*
		// Not needed
        resetView();
        */
        
        mInflater = getActivity().getLayoutInflater() ;
        
        new FileViewLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        

        UiUtilities.installFragment(this);
    }
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        UiUtilities.uninstallFragment(this);
        
        /*
        // Virer l'AsyncTask ?
        cancelAllTasks();
        */

        // We should clean up the Webview here, but it can't release resources until it is
        // actually removed from the view tree.

        super.onDestroyView();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_KEY_CURRENT_TAB, mCurrentTab);
    }

    private void restoreInstanceState(Bundle state) {
        // At this point (in onCreate) no tabs are visible (because we don't know if the message has
        // an attachment or invite before loading it).  We just remember the tab here.
        // We'll make it current when the tab first becomes visible in updateTabs().
        mRestoredTab = state.getInt(BUNDLE_KEY_CURRENT_TAB);
    }
    
    
	private class FileViewLoadTask extends AsyncTask<Void, Void, Boolean> {
		protected Boolean doInBackground(Void... arg0) {
			Context c = mContext.getApplicationContext() ;
			CrmFileManager crmFileMan = CrmFileManager.getInstance(c) ;
			crmFileMan.fileInitDescriptors() ;
			
			long filerecordId = FileViewFragment.this.getFilerecordId() ;
			String fileCode = crmFileMan.lookupFilecodeForId(filerecordId) ;
			if( fileCode == null ) {
				return false ;
			}
			
			mMstrDesc = crmFileMan.fileGetFileDescriptor(fileCode) ;
			mMstrRecord = crmFileMan.filePullData(fileCode, filerecordId, 0).get(0) ;
			mChildrenDescs = new ArrayList<CrmFileDesc>() ;
			mChildrenRecords = new ArrayList<ArrayList<CrmFileRecord>>() ;
			for( CrmFileDesc cfd : crmFileMan.fileGetChildDescriptors(fileCode) ) {
				String childFileCode = cfd.fileCode ;
				ArrayList<CrmFileRecord> arrFileRecords = new ArrayList<CrmFileRecord>() ;
				arrFileRecords.addAll( crmFileMan.filePullData(childFileCode, 0, filerecordId) ) ;
				if( arrFileRecords.size() == 0 ) {
					continue ;
				}
				mChildrenDescs.add(cfd) ;
				mChildrenRecords.add(arrFileRecords) ;
			}
			
    
			return true ;
		}
		protected void onPostExecute(Boolean isOk) {
			if( isOk ) {
				setupAll() ;
			}
		}
	}
    
	
	private void setupAll() {
		setViewMstrHeader() ;
		setViewMstrDetails() ;
		setViewChildren() ;
		
		// connectMediaAdapters() ;
		enableTabButtons() ;
		showContent(true) ;
	}
	
    private void setViewMstrHeader() {
    	// Banner (view group)
    	for( CrmFileFieldDesc cffd : mMstrDesc.fieldsDesc ) {
    		if( !cffd.fieldIsHeader ) {
    			continue ;
    		}
    		
    		TableRow tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_header_row, null) ;
    		String label = cffd.fieldName ;
    		String text = mMstrRecord.recordData.get(cffd.fieldCode).displayStr ;
    		
    		((TextView)tr.findViewById(R.id.crm_label)).setText(label) ;
    		((TextView)tr.findViewById(R.id.crm_text)).setText(text) ;
    		
    		mHeaderTable.addView(tr) ;
    	}
    	
    }
    private void setViewMstrDetails() {
    	// 1ere TAB
    	Button b = (Button)mInflater.inflate(R.layout.explorer_fileview_tab, null) ;
    	b.setText(mMstrDesc.fileName) ;
    	b.setTypeface(null, Typeface.BOLD) ;
    	mTabsContainer.addView(b) ;
    	mTabs.add(b) ;
    	
    	TableLayout table = (TableLayout)mInflater.inflate(R.layout.explorer_fileview_mstr_table, null) ;
    	for( CrmFileFieldDesc cffd : mMstrDesc.fieldsDesc ) {
    		
    		TableRow tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_mstr_row, null) ;
    		String label = cffd.fieldName ;
    		String text = mMstrRecord.recordData.get(cffd.fieldCode).displayStr ;
    		
    		((TextView)tr.findViewById(R.id.crm_label)).setText(label) ;
    		((TextView)tr.findViewById(R.id.crm_text)).setText(text) ;
    		
    		table.addView(tr) ;
    	}
    	mTabViewsContainer.addView(table) ;
    	mTabViews.add(table) ;
    }
    private void setViewChildren() {
    	int idx = -1 ;
    	for( CrmFileDesc cfd : mChildrenDescs ) {
    		// Log.w(LOGTAG,"Adding for "+cfd.fileCode) ;
    		
    		idx++ ;
    		ArrayList<CrmFileRecord> arrCfr = mChildrenRecords.get(idx) ;
    		
    		if( cfd.fileIsMediaImg ) {
    			subSetViewChildMediaGrid(cfd,arrCfr) ;
    		} else {
    			subSetViewChildTable( cfd, arrCfr) ;
    		}
    	}
    }
    private void subSetViewChildTable( CrmFileDesc cfd, ArrayList<CrmFileRecord> arrCfr ) {
    	// enregistrement d'une tab + une view
    	TableLayout table = subBuildViewForChildFile(cfd,arrCfr) ;
    	mTabViewsContainer.addView(table) ;
    	mTabViews.add(table) ;
    	
    	Button b = (Button)mInflater.inflate(R.layout.explorer_fileview_tab, null) ;
    	b.setText(cfd.fileName) ;
    	mTabsContainer.addView(b) ;
    	mTabs.add(b) ;
    }
    private TableLayout subBuildViewForChildFile( CrmFileDesc cfd, ArrayList<CrmFileRecord> arrCfr ) {
    	// retour d'une vue TableLayout
    	TableLayout table = (TableLayout)mInflater.inflate(R.layout.explorer_fileview_sub_table, null) ;
    	
    	TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_fileview_sub_row, null) ;
    	for( CrmFileFieldDesc cffd : cfd.fieldsDesc ) {
    		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_fileview_sub_cell, null) ;
    		tv.setText(cffd.fieldName) ;
    		tv.setTypeface(null, Typeface.BOLD) ;
    		tv.setGravity(Gravity.CENTER) ;
    		tr.addView(tv) ;
    	}
    	table.addView(tr) ;
    	
    	for( CrmFileRecord cfr : arrCfr ) {
    		View v = new View(mContext);
            v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
            v.setBackgroundColor(Color.BLACK) ;
            // v.setVisibility(View.VISIBLE) ;
    		table.addView(v) ;
    		
    		TableRow trData = (TableRow)mInflater.inflate(R.layout.explorer_fileview_sub_row, null) ;
        	for( CrmFileFieldDesc cffd : cfd.fieldsDesc ) {
        		String displayStr = "" ;
        		if( cfr.recordData.get(cffd.fieldCode) != null ) {
        			displayStr = cfr.recordData.get(cffd.fieldCode).displayStr ;
        		}
        		
        		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_fileview_sub_cell, null) ;
        		tv.setText(displayStr) ;
        		// tv.setTypeface(null, Typeface.BOLD) ;
        		switch( cffd.fieldType ) {
        		case FIELD_NUMBER :
        			tv.setGravity(Gravity.RIGHT) ;
        			break ;
        		default :
        			tv.setGravity(Gravity.LEFT) ;
        			break ;
        		}
        		trData.addView(tv) ;
        	}
        	table.addView(trData) ;
    	}
    	
    	return table ;
    }
    
    private void subSetViewChildMediaGrid( CrmFileDesc cfd, ArrayList<CrmFileRecord> arrCfr ) {
    	// ici création :
    	// - tab (textview) > mTabs
    	// - grid view > mTabViews liée à un adapter
    	// - baseadapter > mMediaAdapters
    	// - enregistrement du l
    	Button b = (Button)mInflater.inflate(R.layout.explorer_fileview_tab, null) ;
    	b.setText(cfd.fileName) ;
    	mTabsContainer.addView(b) ;
    	mTabs.add(b) ;
    	
    	MediaAdapter gridAdapter = new MediaAdapter(mContext);
    	gridAdapter.setData(arrCfr) ;
    	GridView gridView = (GridView)mInflater.inflate(R.layout.explorer_gallery, null) ;
    	gridView.setAdapter(gridAdapter) ;
    	mMediaAdapters.add(gridAdapter) ;
    	mTabViewsContainer.addView(gridView) ;
    	mTabViews.add(gridView) ;
    	
    	gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long doNotUse) {
				String syncVuid = ((CrmFileRecord)parent.getAdapter().getItem(position)).syncVuid ;
				
				FileViewImageDialog fragment = FileViewImageDialog.newInstance(FileViewImageDialog.MODE_CRMSYNCVUID, syncVuid,true) ;
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                // if we have an old popup replace it
                Fragment fOld = fm.findFragmentByTag(FileViewImageDialog.FILEVIEWIMAGE_DIALOG_TAG);
                if (fOld != null && fOld.isAdded()) {
                    ft.remove(fOld);
                }
                ft.add(fragment, FileViewImageDialog.FILEVIEWIMAGE_DIALOG_TAG);
                ft.commit();
				
			}
    		
    	}) ;
    }
    
    
    
    
    /**
     * Show/hide the content.  We hide all the content (except for the bottom buttons) when loading,
     * to avoid flicker.
     */
    private void showContent(boolean showContent) {
        mMainView.setVisibility( showContent ? View.VISIBLE : View.GONE ) ;
        mLoadingProgress.setVisibility( !showContent ? View.VISIBLE : View.GONE ) ;
    }
    
    
    private void enableTabButtons() {
    	for( TextView tabButton : mTabs ) {
    		tabButton.setOnClickListener(this) ;
    	}
    	if( (mRestoredTab > -1) && (mRestoredTab < mTabs.size()) ) {
    		setCurrentTab(mRestoredTab) ;
    		mRestoredTab = -1 ;
    	} else {
    		setCurrentTab(0) ;
    	}
    }
	@Override
	public void onClick(View v) {
		if( mTabs.contains(v) ) {
			setCurrentTab(mTabs.indexOf(v)) ;
		}
		
	}
	
    /**
     * Set the current tab.
     *
     * @param tab any of {@link #TAB_MESSAGE}, {@link #TAB_ATTACHMENT} or {@link #TAB_INVITE}.
     */
    private void setCurrentTab(int tabIdx) {
        mCurrentTab = tabIdx;
        
        int idx = -1 ;
        for( View v : mTabViews ) {
        	idx++ ;
        	TextView tabButton =  mTabs.get(idx) ;
        	
        	v.setVisibility( (idx==tabIdx)? View.VISIBLE : View.GONE ) ;
        	tabButton.setSelected( (idx==tabIdx)? true : false ) ;
        }
    }
	
	private class MediaAdapter extends BaseAdapter {
		
		Context mAdapterContext ;
		
		LayoutInflater mInflater ;
		ArrayList<CrmFileRecord> mArrCfr ;
		
		CrmImageLoader mCrmImageLoader ;
		
		public MediaAdapter( Context c ) {
			super() ;
			mAdapterContext = c.getApplicationContext() ;
			mInflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			mCrmImageLoader = new CrmImageLoader(mAdapterContext) ;
		}
		
		public void setData( ArrayList<CrmFileRecord> arrCfr ) {
			mArrCfr = arrCfr ;
		}

		@Override
		public int getCount() {
			return mArrCfr.size() ;
		}

		@Override
		public Object getItem(int position) {
			return mArrCfr.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position ;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			CrmFileRecord cfr = mArrCfr.get(position) ;
			
			if( convertView==null ) {
				convertView = mInflater.inflate(R.layout.explorer_gallery_item, null) ;
			}
			//((ImageView)convertView.findViewById(R.id.imageView)).setImageResource(R.drawable.sample_2) ;
			//((TextView)convertView.findViewById(R.id.textView)).setText("oookok") ;
			convertView.findViewById(R.id.textView).setVisibility(View.GONE) ;
			
			CrmImageLoader.CrmUrl crmUrl = new CrmImageLoader.CrmUrl() ;
			crmUrl.syncVuid = cfr.syncVuid ;
			crmUrl.thumbnail = true ;
			
			ImageView imgView = (ImageView)convertView.findViewById(R.id.imageView) ;
			
			mCrmImageLoader.download(crmUrl, imgView) ;
			

			return convertView;
		}
		
	}
	
	
}
