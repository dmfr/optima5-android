package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import android.app.Activity;
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
import android.widget.ListView;
import android.widget.TextView;

public class FileListFragment extends ListFragment {
	private static final String LOGTAG = "FileListFragment";

	/** Bundle keys */
    private static final String BUNDLE_LIST_STATE = "fileListFragment.state.listState";
    private static final String BUNDLE_KEY_SELECTED_FILE_ID
            = "fileListFragment.state.listState.selected_filerecord_id";
    /** Argument name(s) */
    private static final String ARG_EXPLORER_CONTEXT = "explorerContext";
    
    // Controller access
    private RefreshManager mRefreshManager;
    private final RefreshListener mRefreshListener = new RefreshListener();

    // UI Support
    private Activity mActivity;
    private Callback mCallback = EmptyCallback.INSTANCE;
    private boolean mIsViewCreated;

    private View mListPanel;
    private View mListFooterView;
    private TextView mListFooterText;
    private View mListFooterProgress;
    /*
    private ViewGroup mSearchHeader;
    private ViewGroup mWarningContainer;
    private TextView mSearchHeaderText;
    private TextView mSearchHeaderCount;
    */

    private static final int LIST_FOOTER_MODE_NONE = 0;
    private static final int LIST_FOOTER_MODE_MORE = 1;
    private int mListFooterMode;

    private FileListFragmentAdapter mListAdapter;
    private boolean mIsFirstLoad;
    
    /** Last parameters used in Loader */
    private String mLoaderFileCode ;
    private List<BibleHelper.BibleEntry> mLoaderBibleConditions ;

    /** ID of the message to hightlight. */
    private long mSelectedFilerecordId = -1;
    
    /** true between {@link #onResume} and {@link #onPause}. */
    private boolean mResumed;
    
    private Parcelable mSavedListState;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    	public BibleHelper.BibleEntry getExplorerConstraint() ;
    	public void onFilerecordOpen( long filerecordId ) ;
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    	public BibleHelper.BibleEntry getExplorerConstraint() {return null;} ;
    	public void onFilerecordOpen( long filerecordId ) {} ;
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }

    public static FileListFragment newInstance(ExplorerContext explorerContext) {
        final FileListFragment instance = new FileListFragment();
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
    
    public String getFileCode() {
    	initializeArgCache();
    	if( mExplorerContext.mMode == ExplorerContext.MODE_FILE ) {
    		return mExplorerContext.mFileCode ;
    	}
    	return null ;
    }
    
    public ExplorerContext getExplorerContext(){
    	initializeArgCache();
    	return mExplorerContext ;
    }
    
    /**
     * @return CRM list of bibleConditions
     */
    public List<BibleHelper.BibleEntry> getBibleConditions() {
    	List<BibleHelper.BibleEntry> bibleConditions = new ArrayList<BibleHelper.BibleEntry>() ;
    	if( mCallback != null && mCallback.getExplorerConstraint() != null ) {
    		bibleConditions.add(mCallback.getExplorerConstraint()) ;
    	}
    	if( mExplorerContext.isFiltered() ) {
    		bibleConditions.add(mExplorerContext.mFilteredBibleEntry) ;
    	}
    	return bibleConditions ;
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
        mRefreshManager = RefreshManager.getInstance(mActivity);

        mListAdapter = new FileListFragmentAdapter(mActivity);
        mIsFirstLoad = true;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onCreateView");
        }
        // Use a custom layout, which includes the original layout with "send messages" panel.
        View root = inflater.inflate(R.layout.explorer_filelist_fragment,null);
        mIsViewCreated = true;
        mListPanel = UiUtilities.getView(root, R.id.list_panel);
        return root;
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

        final ListView lv = getListView();
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListFooterView = getActivity().getLayoutInflater().inflate(
                R.layout.explorer_filelist_item_footer, lv, false);
        setEmptyText("No records to display");

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
        mRefreshManager.registerListener(mRefreshListener);
        mResumed = true;
        
       	restartLoadingIfNeeded() ;
    }

    @Override
    public void onPause() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onPause");
        }
        mResumed = false;
        mSavedListState = getListView().onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onStop");
        }
        mRefreshManager.unregisterListener(mRefreshListener);

        super.onStop();
    }
    
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        
        mIsViewCreated = false; // Clear this first for updateSelectionMode(). See isViewCreated().

        // Reset the footer mode since we just blew away the footer view we were holding on to.
        // This will get re-updated when/if this fragment is restored.
        mListFooterMode = LIST_FOOTER_MODE_NONE;
        
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
            outState.putParcelable(BUNDLE_LIST_STATE, getListView().onSaveInstanceState());
        }
        outState.putLong(BUNDLE_KEY_SELECTED_FILE_ID, mSelectedFilerecordId);
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " restoreInstanceState");
        }
        // mListAdapter.loadState(savedInstanceState);
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
        mSelectedFilerecordId = savedInstanceState.getLong(BUNDLE_KEY_SELECTED_FILE_ID);
    }
    
    
    
    public void startLoading() {
    	final LoaderManager lm = getLoaderManager();
    	lm.initLoader(Explorer.FILELIST_LOADER_ID, null, new FileListLoaderCallbacks());
    }
    public void restartLoadingIfNeeded() {
    	if( getFileCode() == mLoaderFileCode && getBibleConditions().equals(mLoaderBibleConditions) ) {
    		return ;
    	}
    	
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.FILELIST_LOADER_ID)!=null ) {
    		lm.restartLoader(Explorer.FILELIST_LOADER_ID, null, new FileListLoaderCallbacks());
    	}
    }
    public void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.FILELIST_LOADER_ID)!=null ) {
    		lm.getLoader(Explorer.FILELIST_LOADER_ID).forceLoad() ;
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
    public void onListItemClick(ListView parent, View view, int position, long id) {
        if (view != mListFooterView) {
        	long clickedFilerecordId = mListAdapter.getItem(position).filerecordId ;
        	mCallback.onFilerecordOpen(clickedFilerecordId) ;
        } else {
            doFooterClick();
        }
    }
    
    
    
    /**
     * Refresh the list.  JAMAIS FAIT DIRECTEMENT A PARTIR DE FRAGMENT
     *
     * Note: Manual refresh is enabled even for push accounts.
     */
    public void onRefresh(boolean userRequest) {
        if (isRefreshable()) {
            mRefreshManager.refreshFileList(getExplorerContext().mFileCode, getBibleConditions());
        }
    }
    /**
     * Load more messages.  NOOP for special mailboxes (e.g. combined inbox).
     */
    private void onLoadMore() {
        if (isRefreshable()) {
            mRefreshManager.loadMoreFileList(getExplorerContext().mFileCode, getBibleConditions());
            // Sans attendre, on peut forcer un reload de la liste (déjà en cache?)
            forceReload() ;
        }
    }
    
    private void autoRefreshStaleFile() {
        if (!isRefreshable()) {
            // Not refreshable (special box such as drafts, or magic boxes)
            return;
        }
        if (!mRefreshManager.isFileStale(getFileCode(),getBibleConditions())) {
            return;
        }
        onRefresh(false);
    }
    
    
    private void updateFooterView() {
        // Only called from onLoadFinished -- always has views.
        int mode = LIST_FOOTER_MODE_MORE;
        if (mListFooterMode == mode) {
            return;
        }
        mListFooterMode = mode;

        ListView lv = getListView();
        if (mListFooterMode != LIST_FOOTER_MODE_NONE) {
            lv.addFooterView(mListFooterView);
            if (getListAdapter() != null) {
                // Already have an adapter - reset it to force the mode. But save the scroll
                // position so that we don't get kicked to the top.
                Parcelable listState = lv.onSaveInstanceState();
                setListAdapter(mListAdapter);
                lv.onRestoreInstanceState(listState);
            }
            mListFooterProgress = mListFooterView.findViewById(R.id.progress);
            mListFooterText = (TextView) mListFooterView.findViewById(R.id.main_text);
        } else {
            lv.removeFooterView(mListFooterView);
        }
        updateListFooter();
    }

    /**
     * Set the list footer text based on mode and the current "network active" status
     */
    private void updateListFooter() {
        if (mListFooterMode != LIST_FOOTER_MODE_NONE) {
            String footerText = "";
            switch (mListFooterMode) {
                case LIST_FOOTER_MODE_MORE:
                    boolean active = mRefreshManager.isFileRefreshing(getFileCode());
                    footerText = active ? "Loading..." : "Load more records";
                    mListFooterProgress.setVisibility(active ? View.VISIBLE : View.GONE);
                    break;
            }
            mListFooterText.setText(footerText);
        }
    }
    
    /**
     * Handle a click in the list footer, which changes meaning depending on what we're looking at.
     */
    private void doFooterClick() {
    	highlightSelectedFilerecord(mIsFirstLoad) ;
        switch (mListFooterMode) {
            case LIST_FOOTER_MODE_NONE: // should never happen
                break;
            case LIST_FOOTER_MODE_MORE:
                onLoadMore();
                break;
        }
    }
    
    /**
     * Highlight the selected message.
     */
    public void setSelectedFilerecord(long filerecordId) {
        if (mSelectedFilerecordId == filerecordId) {
            return;
        }
        mSelectedFilerecordId = filerecordId;
        if (mResumed) {
        	highlightSelectedFilerecord(true);
        }
    }
    private void highlightSelectedFilerecord(boolean ensureSelectionVisible) {
        if (!isViewCreated()) {
            return;
        }

        final ListView lv = getListView();
        if (mSelectedFilerecordId == -1) {
            // No message selected
            lv.clearChoices();
            return;
        }

        final int count = lv.getCount();
        for (int i = 0; i < count; i++) {
            if ( ((CrmFileManager.CrmFileRecord)lv.getItemAtPosition(i)).filerecordId != mSelectedFilerecordId) {
                continue;
            }
            lv.setItemChecked(i, true);
            if (ensureSelectionVisible) {
                UiUtilities.listViewSmoothScrollToPosition(getActivity(), lv, i);
            }
            break;
        }
    }
    

    private class RefreshListener implements RefreshManager.RefreshListener {
		@Override
		public void onRefreshStart() {
			updateListFooter();
		}
        @Override
        public void onRefreshFileChanged(String fileCode) {
            updateListFooter();
        }
    }
	
	
	private class FileListLoaderCallbacks implements LoaderCallbacks<FileListFragmentLoaderResult> {
		
		@Override
		public Loader<FileListFragmentLoaderResult> onCreateLoader(int arg0, Bundle arg1) {
			mIsFirstLoad = true;
			mLoaderFileCode = getFileCode() ;
			mLoaderBibleConditions = getBibleConditions() ;
			return new FileListFragmentLoader(getActivity(),mLoaderFileCode,mLoaderBibleConditions);
		}

		@Override
		public void onLoadFinished(Loader<FileListFragmentLoaderResult> loader, FileListFragmentLoaderResult data) {
			
            // On peut faire tout ceci ici BECAUSE The loader manager doesn't deliver results when a fragment is stopped.
            //Parcelable listState = lv.onSaveInstanceState();
			mListAdapter.setData(data.fileDesc,data.records) ;
			
			
			autoRefreshStaleFile();
			updateFooterView();

			highlightSelectedFilerecord(mIsFirstLoad) ;
			
			
            if (mIsFirstLoad) {
                mListPanel.setVisibility(View.VISIBLE);

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
            	getListView().onRestoreInstanceState(mSavedListState);
            }
			
            mIsFirstLoad = false;
		}
		
        private boolean isEmptyAndLoading(FileListFragmentLoaderResult data) {
            return (data.records.size() == 0)
                        && mRefreshManager.isFileRefreshing(mExplorerContext.mFileCode);
        }

		@Override
		public void onLoaderReset(Loader<FileListFragmentLoaderResult> loader) {
			FileListFragment.this.mListAdapter.setData(null,null) ;
		}
		
	}
	private class FileListFragmentAdapter extends BaseAdapter {
		
		private ExplorerLayout mLayout ;
		private boolean isLoaded = false ;
		
		private CrmFileManager.CrmFileDesc mCfd ;
		private ArrayList<CrmFileManager.CrmFileRecord> mData ;
		
		LayoutInflater mInflater ;

		public FileListFragmentAdapter(Context context) {
			super();
			mData = new ArrayList<CrmFileManager.CrmFileRecord>() ;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	    public void setData( CrmFileManager.CrmFileDesc fileDesc, List<CrmFileManager.CrmFileRecord> data) {
	    	mData.clear() ;
	        mCfd = fileDesc ;
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
			FileListItem view ;
			
			if( (convertView != null && isWideLayout() && convertView instanceof FileListItemWide)
				|| (convertView != null && !isWideLayout() && convertView instanceof FileListItemNormal) ) {
				// able to reuse view
				view = (FileListItem)convertView ;
			} else {
				// build a new view
				if( isWideLayout() ) {
					view = (FileListItem)mInflater.inflate(R.layout.explorer_filelist_item_wide, parent, false) ;
				} else {
					view = (FileListItem)mInflater.inflate(R.layout.explorer_filelist_item_normal, parent, false) ;
				}
			}
			
			view.buildCrmFields(mCfd) ;
			
			view.setCrmValues(getItem(position)) ;
			
			

			return view;
		}
		@Override
		public int getCount() {
			return mData.size() ;
		}
		@Override
		public CrmFileManager.CrmFileRecord getItem(int position) {
			return mData.get(position);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
	}
	
	private static class FileListFragmentLoaderResult {
		CrmFileManager.CrmFileDesc fileDesc ;
		List<CrmFileManager.CrmFileRecord> records ;
	}
	
	private static class FileListFragmentLoader extends AsyncTaskLoader<FileListFragmentLoaderResult> {

		Context mContext ;
		
		String mFileCode ;
		List<BibleHelper.BibleEntry> mBibleConditions ;
		
		FileListFragmentLoaderResult mData ;

		public FileListFragmentLoader(Context context , String fileCode,  List<BibleHelper.BibleEntry> bibleConditions) {
			super(context);
			mContext = context ;
			
			mFileCode = fileCode ;
			mBibleConditions = bibleConditions ;
		} 

		@Override public void deliverResult(FileListFragmentLoaderResult data) {
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
		@Override public FileListFragmentLoaderResult loadInBackground() {
			//Log.w(LOGTAG,"Loading !!!") ;
			
			CrmFileManager mCfm = CrmFileManager.getInstance(mContext) ;
			if( !mCfm.isInitialized() ) {
				mCfm.fileInitDescriptors() ;
			}
			
			mCfm.setPullFilters(mFileCode,mBibleConditions) ;
			FileListFragmentLoaderResult data = new FileListFragmentLoaderResult() ;
			data.fileDesc = mCfm.fileGetFileDescriptor(mFileCode) ;
			data.records = mCfm.filePullData(mFileCode) ;
			
			
			
			return data ;
		}
		
		
	}

}
