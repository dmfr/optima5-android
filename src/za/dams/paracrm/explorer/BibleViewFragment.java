package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.CrmImageLoader;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleDesc;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleFieldDesc;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBiblePhoto;
import za.dams.paracrm.explorer.CrmBibleManager.CrmBibleRecord;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileDesc;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileFieldDesc;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileRecord;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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

public class BibleViewFragment extends Fragment implements View.OnClickListener {
	private static final String LOGTAG = "FileViewFragment";
	private static final String BUNDLE_KEY_CURRENT_TAB = "FileViewFragment.currentTab";
	
	private Context mContext;
	private LayoutInflater mInflater ;

    /** Argument name(s) */
    private static final String ARG_BIBLE_CODE = "bibleCode";
    private static final String ARG_BIBLE_ENTRY_KEY = "bibleEntryKey";
    
	private Callback mCallback = EmptyCallback.INSTANCE;
	
	// View fields
	private View mMainView ;
	private View mLoadingProgress;
	
	private ImageView mHeaderIcon ;
	private TableLayout mHeaderTable ;
	
	private ViewGroup mTabsContainer ;
	private ViewGroup mTabViewsContainer ;
	private int mCurrentTab = -1 ;
	private int mRestoredTab = -1 ;
	private ArrayList<TextView> mTabs ;
	private ArrayList<View> mTabViews ;
	
	// Adapters pour les photos
	CrmImageLoader mCrmImageLoader ;
	private ArrayList<BaseAdapter> mAdapters ;
	
	// CRM data
	private CrmBibleDesc mBibleDesc ;
	private CrmBibleRecord mBibleRecord ;
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
    
    public static BibleViewFragment newInstance(String bibleCode, String bibleEntryKey) {
        if (bibleEntryKey == null) {
            throw new IllegalArgumentException();
        }
        final BibleViewFragment instance = new BibleViewFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_BIBLE_CODE, bibleCode);
        args.putString(ARG_BIBLE_ENTRY_KEY, bibleEntryKey);
        instance.setArguments(args);
        return instance;
    }
    
    private String mImmutableBibleCode;
    private String mImmutableBibleEntryKey;

    private void initializeArgCache() {
        if (mImmutableBibleCode != null && mImmutableBibleEntryKey != null) return;
        mImmutableBibleCode = getArguments().getString(ARG_BIBLE_CODE);
        mImmutableBibleEntryKey = getArguments().getString(ARG_BIBLE_ENTRY_KEY);
    }
    
    
    public String getBibleCode() {
        initializeArgCache();
        return mImmutableBibleCode;
    }
    public String getBibleEntryKey() {
        initializeArgCache();
        return mImmutableBibleEntryKey;
    }
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        mCrmImageLoader = new CrmImageLoader(mContext) ;
        mAdapters = new ArrayList<BaseAdapter>() ;
        
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.explorer_fileview_fragment, container, false);

        mLoadingProgress = UiUtilities.getView(view, R.id.loading_progress);
        mMainView = UiUtilities.getView(view, R.id.main_panel);
        
        mHeaderIcon = (ImageView) view.findViewById(R.id.fileicon) ;
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
			CrmBibleManager crmBibleMan = CrmBibleManager.getInstance(c) ;
			crmBibleMan.bibleInitDescriptors() ;
			
			String bibleCode = BibleViewFragment.this.getBibleCode() ;
			String bibleEntryKey = BibleViewFragment.this.getBibleEntryKey() ;
			if( bibleCode == null || bibleEntryKey == null ) {
				return false ;
			}
			
			mBibleDesc = crmBibleMan.bibleGetBibleDescriptor(bibleCode) ;
			mBibleRecord = crmBibleMan.biblePullData(bibleCode, bibleEntryKey).get(0) ;
    
			return true ;
		}
		protected void onPostExecute(Boolean isOk) {
			if( isOk ) {
				setupAll() ;
			}
		}
	}
    
	private void setupAll() {
		setViewHeader() ;
		setViewDetails() ;
		setViewGallery() ;
		
		// connectMediaAdapters() ;
		enableTabButtons() ;
		showContent(true) ;
	}
    
    private void setViewHeader() {
    	// Banner (view group)
    	for( CrmBibleFieldDesc cbfd : mBibleDesc.fieldsDesc ) {
    		if( !cbfd.fieldIsHeader ) {
    			continue ;
    		}
    		
    		if( mBibleDesc.bibleHasGallery && mBibleRecord.defaultPhoto != null ) {
    			ImageView imageView = mHeaderIcon ;
    			
    			CrmImageLoader.CrmUrl crmUrl = new CrmImageLoader.CrmUrl() ;
    			crmUrl.mediaId = mBibleRecord.defaultPhoto.mediaId ;
    			crmUrl.thumbnail = true ;
    			
    			mCrmImageLoader.download(crmUrl, imageView) ;
    		}
    		
    		TableRow tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_header_row, null) ;
    		String label = cbfd.fieldName ;
    		String text = mBibleRecord.recordData.get(cbfd.fieldCode).displayStr ;
    		
    		((TextView)tr.findViewById(R.id.crm_label)).setText(label) ;
    		((TextView)tr.findViewById(R.id.crm_text)).setText(text) ;
    		
    		mHeaderTable.addView(tr) ;
    	}
    	
    }
    private void setViewDetails() {
    	// 1ere TAB
    	Button b = (Button)mInflater.inflate(R.layout.explorer_fileview_tab, null) ;
    	b.setText(mBibleDesc.bibleName) ;
    	b.setTypeface(null, Typeface.BOLD) ;
    	mTabsContainer.addView(b) ;
    	mTabs.add(b) ;
    	
    	TableLayout table = (TableLayout)mInflater.inflate(R.layout.explorer_fileview_mstr_table, null) ;
    	for( CrmBibleFieldDesc cbfd : mBibleDesc.fieldsDesc ) {
    		
    		TableRow tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_mstr_row, null) ;
    		String label = cbfd.fieldName ;
    		String text = mBibleRecord.recordData.get(cbfd.fieldCode).displayStr ;
    		
    		((TextView)tr.findViewById(R.id.crm_label)).setText(label) ;
    		((TextView)tr.findViewById(R.id.crm_text)).setText(text) ;
    		
    		table.addView(tr) ;
    	}
    	mTabViewsContainer.addView(table) ;
    	mTabViews.add(table) ;
    }
    private void setViewGallery() {
    	if( !mBibleDesc.bibleHasGallery || mBibleRecord.galleryPhotos.size() < 1 ) {
    		return ;
    	}
    	
    	// ici création :
    	// - tab (textview) > mTabs
    	// - grid view > mTabViews liée à un adapter
    	// - baseadapter > mMediaAdapters
    	// - enregistrement du l
    	Button b = (Button)mInflater.inflate(R.layout.explorer_fileview_tab, null) ;
    	b.setText("Gallery") ;
    	mTabsContainer.addView(b) ;
    	mTabs.add(b) ;
    	
    	MediaAdapter gridAdapter = new MediaAdapter(mContext);
    	gridAdapter.setData(mBibleRecord.galleryPhotos) ;
    	GridView gridView = (GridView)mInflater.inflate(R.layout.explorer_gallery, null) ;
    	gridView.setAdapter(gridAdapter) ;
    	mAdapters.add(gridAdapter) ;
    	mTabViewsContainer.addView(gridView) ;
    	mTabViews.add(gridView) ;
    	
    	gridView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long doNotUse) {
				String mediaId = ((CrmBiblePhoto)parent.getAdapter().getItem(position)).mediaId ;
				
				FileViewImageDialog fragment = FileViewImageDialog.newInstance(FileViewImageDialog.MODE_CRMMEDIAID,mediaId,true) ;
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
		ArrayList<CrmBiblePhoto> mArrCbp ;
		
		
		
		public MediaAdapter( Context c ) {
			super() ;
			mAdapterContext = c.getApplicationContext() ;
			mInflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			mCrmImageLoader = new CrmImageLoader(mAdapterContext) ;
		}
		
		public void setData( ArrayList<CrmBiblePhoto> arrCbp ) {
			mArrCbp = arrCbp ;
		}

		@Override
		public int getCount() {
			return mArrCbp.size() ;
		}

		@Override
		public Object getItem(int position) {
			return mArrCbp.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position ;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			CrmBiblePhoto cbp = mArrCbp.get(position) ;
			
			if( convertView==null ) {
				convertView = mInflater.inflate(R.layout.explorer_gallery_item, null) ;
			}
			//((ImageView)convertView.findViewById(R.id.imageView)).setImageResource(R.drawable.sample_2) ;
			//((TextView)convertView.findViewById(R.id.textView)).setText("oookok") ;
			convertView.findViewById(R.id.textView).setVisibility(View.GONE) ;
			
			CrmImageLoader.CrmUrl crmUrl = new CrmImageLoader.CrmUrl() ;
			crmUrl.mediaId = cbp.mediaId ;
			crmUrl.thumbnail = true ;
			
			ImageView imgView = (ImageView)convertView.findViewById(R.id.imageView) ;
			
			mCrmImageLoader.download(crmUrl, imgView) ;
			

			return convertView;
		}
		
	}
	
}
