package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.widget.GridFragment;
import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

public class BibleListFragment extends GridFragment {
	private static final String LOGTAG = "BibleListFragment";

	/** Bundle keys */
    private static final String BUNDLE_LIST_STATE = "bibleListFragment.state.listState";
    private static final String BUNDLE_KEY_SELECTED_ENTRY_KEY
            = "fileListFragment.state.listState.selected_bibleEntry_key";
    /** Argument name(s) */
    private static final String ARG_EXPLORER_CONTEXT = "explorerContext";
    
    // UI Support
    private Activity mActivity;
    private Callback mCallback = EmptyCallback.INSTANCE;
    private boolean mIsViewCreated;

    private View mGridPanel;
    /*
    private ViewGroup mSearchHeader;
    private ViewGroup mWarningContainer;
    private TextView mSearchHeaderText;
    private TextView mSearchHeaderCount;
    */

    private BibleListFragmentAdapter mListAdapter;
    private boolean mIsFirstLoad;
    
    /** Last parameters used in Loader */
    private String mLoaderBibleCode ;

    /** ID of the message to hightlight. */
    private String mSelectedBibleEntryKey = null;
    
    /** true between {@link #onResume} and {@link #onPause}. */
    private boolean mResumed;
    
    private Parcelable mSavedListState;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    	public void onBibleEntryOpen( String bibleEntryKey ) ;
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    	public void onBibleEntryOpen( String bibleEntryKey ) {} ;
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }

    public static BibleListFragment newInstance(ExplorerContext explorerContext) {
        final BibleListFragment instance = new BibleListFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_EXPLORER_CONTEXT, explorerContext);
        instance.setArguments(args);
        return instance;
    }
    
    private ExplorerContext mExplorerContext ;
    
    private void initializeArgCache() {
        if (mExplorerContext != null) return;
        mExplorerContext = getArguments().getParcelable(ARG_EXPLORER_CONTEXT);
    }
    
    public String getBibleCode() {
    	initializeArgCache();
    	if( mExplorerContext.mMode == ExplorerContext.MODE_BIBLE ) {
    		return mExplorerContext.mBibleCode ;
    	}
    	return null ;
    }
    
    public ExplorerContext getExplorerContext(){
    	initializeArgCache();
    	return mExplorerContext ;
    }
    
    /**
     * @return Whether or not initial data is loaded in this list.
     */
    public boolean hasDataLoaded() {
        return mListAdapter.isDataLoaded() ;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onCreate");
        }
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        setHasOptionsMenu(true);

        mListAdapter = new BibleListFragmentAdapter(mActivity);
        mIsFirstLoad = true;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onCreateView");
        }
        // Use a custom layout, which includes the original layout with "send messages" panel.
        View root = inflater.inflate(R.layout.explorer_biblelist_fragment,null);
        mIsViewCreated = true;
        mGridPanel = UiUtilities.getView(root, R.id.grid_panel);
        
        return root;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        int gridSpacing = (int)mActivity.getResources().getDimension(R.dimen.explorer_gridview_spacing) ;
        getGridView().setStretchMode(GridView.NO_STRETCH);
        getGridView().setHorizontalSpacing(gridSpacing) ;
        getGridView().setVerticalSpacing(gridSpacing) ;
    }

    public void setLayout(ExplorerLayout layout) {
    	/*
        if (UiUtilities.useTwoPane(mActivity)) {
            mListAdapter.setLayout(layout);
        }
        */
    	mListAdapter.setLayout(layout);
    }
    
    /**
     * @return true if the content view is created and not destroyed yet. (i.e. between
     * {@link #onCreateView} and {@link #onDestroyView}.
     */
    private boolean isViewCreated() {
        // Note that we don't use "getView() != null".  This method is used in updateSelectionMode()
        // to determine if CAB shold be shown.  But because it's called from onDestroyView(), at
        // this point the fragment still has views but we want to hide CAB, we can't use
        // getView() here.
        return mIsViewCreated;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);

        final GridView gv = getGridView();
        gv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setEmptyText("No records to display !!!!!!");

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        }

        UiUtilities.installFragment(this);
        
        startLoading();
    }

    @Override
    public void onStart() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onStart");
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onResume");
        }
        super.onResume();
        mResumed = true;
        
       	restartLoadingIfNeeded() ;
    }

    @Override
    public void onPause() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onPause");
        }
        mResumed = false;
        mSavedListState = getGridView().onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onStop");
        }

        super.onStop();
    }
    
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        
        mIsViewCreated = false; // Clear this first for updateSelectionMode(). See isViewCreated().

        UiUtilities.uninstallFragment(this);
        super.onDestroyView();
    }
    
    @Override
    public void onDestroy() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroy");
        }

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDetach");
        }
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onSaveInstanceState");
        }
        super.onSaveInstanceState(outState);
        // mListAdapter.onSaveInstanceState(outState);
        if (isViewCreated()) {
            outState.putParcelable(BUNDLE_LIST_STATE, getGridView().onSaveInstanceState());
        }
        outState.putString(BUNDLE_KEY_SELECTED_ENTRY_KEY, mSelectedBibleEntryKey);
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " restoreInstanceState");
        }
        // mListAdapter.loadState(savedInstanceState);
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
        mSelectedBibleEntryKey = savedInstanceState.getString(BUNDLE_KEY_SELECTED_ENTRY_KEY);
    }
    
    
    
    public void startLoading() {
    	final LoaderManager lm = getLoaderManager();
    	lm.initLoader(Explorer.BIBLELIST_LOADER_ID, null, new BibleListLoaderCallbacks());
    }
    public void restartLoadingIfNeeded() {
    	if( getBibleCode() == mLoaderBibleCode ) {
    		return ;
    	}
    	
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.BIBLELIST_LOADER_ID)!=null ) {
    		lm.restartLoader(Explorer.BIBLELIST_LOADER_ID, null, new BibleListLoaderCallbacks());
    	}
    }
    public void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.BIBLELIST_LOADER_ID)!=null ) {
    		lm.getLoader(Explorer.BIBLELIST_LOADER_ID).forceLoad() ;
    	}
    }
    
    /**
     * @return true if the file is refreshable.  TOUJOURS VRAI
     */
    public boolean isRefreshable() {
        return true ;
    }
    
    
    /**
     * Called when a file is clicked > for the detailed view
     */
    @Override
    public void onGridItemClick(GridView parent, View view, int position, long id) {
        	String clickedBibleEntryKey = mListAdapter.getItem(position).entryKey ;
        	mCallback.onBibleEntryOpen(clickedBibleEntryKey) ;
    }
    
    
    
    /**
     * Highlight the selected message.
     */
    public void setSelectedBibleEntry(String bibleEntryKey) {
        if (mSelectedBibleEntryKey == bibleEntryKey) {
            return;
        }
        mSelectedBibleEntryKey = bibleEntryKey;
        if (true) {
        	highlightSelectedBibleEntry(true);
        }
    }
    private void highlightSelectedBibleEntry(boolean ensureSelectionVisible) {
        if (!isViewCreated()) {
            return;
        }

        final GridView gv = getGridView();
        if (mSelectedBibleEntryKey == null) {
            // No message selected
            gv.clearChoices();
            return;
        }

        final int count = gv.getCount();
        for (int i = 0; i < count; i++) {
        	if( !(gv.getItemAtPosition(i) instanceof CrmBibleManager.CrmBibleRecord ) ) {
        		// Footer or unknown record !
        		continue ;
        	}
        	if ( ((CrmBibleManager.CrmBibleRecord)gv.getItemAtPosition(i)).entryKey != mSelectedBibleEntryKey) {
        		continue;
        	}
        	gv.setItemChecked(i, true);
        	if (ensureSelectionVisible) {
        		UiUtilities.gridViewSmoothScrollToPosition(getActivity(), gv, i);
        	}
        	break;
        }
    }
    

	private class BibleListLoaderCallbacks implements LoaderCallbacks<BibleListFragmentLoaderResult> {
		
		@Override
		public Loader<BibleListFragmentLoaderResult> onCreateLoader(int arg0, Bundle arg1) {
			mIsFirstLoad = true;
			mLoaderBibleCode = getBibleCode() ;
			return new BibleListFragmentLoader(getActivity(),mLoaderBibleCode);
		}

		@Override
		public void onLoadFinished(Loader<BibleListFragmentLoaderResult> loader, BibleListFragmentLoaderResult data) {
			
            // On peut faire tout ceci ici BECAUSE The loader manager doesn't deliver results when a fragment is stopped.
            //Parcelable listState = lv.onSaveInstanceState();
			mListAdapter.setData(data.bibleDesc,data.records) ;
			
			
			highlightSelectedBibleEntry(mIsFirstLoad) ;
			
			
            if (mIsFirstLoad) {
                mGridPanel.setVisibility(View.VISIBLE);

                // Setting the adapter will automatically transition from "Loading" to showing
                // the list, which could show "No messages". Avoid showing that on the first sync,
                // if we know we're still potentially loading more.
                if (!isEmptyAndLoading(data) ) {
                	setListAdapter(mListAdapter);
                }
            } else if ((getListAdapter() == null) && !isEmptyAndLoading(data) ) {
                setListAdapter(mListAdapter);
            }
			
            if (mSavedListState != null) {
            	getGridView().onRestoreInstanceState(mSavedListState);
            }
			
            mIsFirstLoad = false;
		}
		
        private boolean isEmptyAndLoading(BibleListFragmentLoaderResult data) {
            return false;
        }

		@Override
		public void onLoaderReset(Loader<BibleListFragmentLoaderResult> loader) {
			BibleListFragment.this.mListAdapter.setData(null,null) ;
		}
		
	}
	private class BibleListFragmentAdapter extends BaseAdapter {
		
		private ExplorerLayout mLayout ;
		private boolean isLoaded = false ;
		
		private CrmBibleManager.CrmBibleDesc mCbd ;
		private ArrayList<CrmBibleManager.CrmBibleRecord> mData ;
		
		LayoutInflater mInflater ;

		public BibleListFragmentAdapter(Context context) {
			super();
			mData = new ArrayList<CrmBibleManager.CrmBibleRecord>() ;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	    public void setData( CrmBibleManager.CrmBibleDesc bibleDesc, List<CrmBibleManager.CrmBibleRecord> data) {
	    	mData.clear() ;
	        mCbd = bibleDesc ;
	        if (data != null) {
	        	mData.addAll(data);
	            isLoaded = true ;
	        } else {
	        	isLoaded = false ;
	        }
	        notifyDataSetChanged() ;
	    }
	    public boolean isDataLoaded() {
	    	return isLoaded ;
	    }
	    
	    public void setLayout( ExplorerLayout eLayout ) {
	    	mLayout = eLayout ;
	    }
	    public boolean isWideLayout() {
	    	if( mLayout != null && mLayout.isLeftPaneVisible() ) {
	    		return true ;
	    	}
	    	return false ;
	    }
	    

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BibleGridItem view ;
			
			if( convertView != null ) {
				// able to reuse view
				view = (BibleGridItem)convertView ;
			} else {
				view = (BibleGridItem)mInflater.inflate(R.layout.explorer_biblegrid_item, parent, false) ;
			}
			
			view.buildCrmFields(mCbd) ;
			
			view.setCrmValues(getItem(position)) ;
			
			

			return view;
		}
		@Override
		public int getCount() {
			return mData.size() ;
		}
		@Override
		public CrmBibleManager.CrmBibleRecord getItem(int position) {
			return mData.get(position);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
	}
	
	private static class BibleListFragmentLoaderResult {
		CrmBibleManager.CrmBibleDesc bibleDesc ;
		List<CrmBibleManager.CrmBibleRecord> records ;
	}
	
	private static class BibleListFragmentLoader extends AsyncTaskLoader<BibleListFragmentLoaderResult> {

		Context mContext ;
		
		String mBibleCode ;
		
		BibleListFragmentLoaderResult mData ;

		public BibleListFragmentLoader(Context context , String bibleCode) {
			super(context);
			mContext = context ;
			
			mBibleCode = bibleCode ;
		} 

		@Override public void deliverResult(BibleListFragmentLoaderResult data) {
			mData = data ;
			if( isStarted()) {
				super.deliverResult(data);
			}
		}
		@Override protected void onStartLoading() {
			if( takeContentChanged() || mData == null ) {
				forceLoad() ;
				return ;
			}
			deliverResult(mData);
		}
		@Override protected void onStopLoading() {
	        // Attempt to cancel the current load task if possible.
	        cancelLoad();
	    }
		@Override protected void onReset() {
			mData = null ;
		}
		@Override public BibleListFragmentLoaderResult loadInBackground() {
			//Log.w(LOGTAG,"Loading !!!") ;
			
			CrmBibleManager mCbm = CrmBibleManager.getInstance(mContext) ;
			if( !mCbm.isInitialized() ) {
				mCbm.bibleInitDescriptors() ;
			}
			
			//mCbm.setPullFilters(mFileCode,mBibleConditions) ;
			BibleListFragmentLoaderResult data = new BibleListFragmentLoaderResult() ;
			data.bibleDesc = mCbm.bibleGetBibleDescriptor(mBibleCode) ;
			data.records = mCbm.biblePullData(mBibleCode) ;
			
			
			
			return data ;
		}
		
		
	}

}
