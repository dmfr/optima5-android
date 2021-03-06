package za.dams.paracrm.explorer;

import java.util.LinkedList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;



public class ExplorerController implements ExplorerLayout.Callback,
		DataListFragment.Callback,
		BibleListFragment.Callback, BibleViewFragment.Callback,
		FileListFragment.Callback, FileViewFragment.Callback,
		QueryListFragment.Callback, QueryLaunchFragment.Callback {

	static final String LOGTAG = "Explorer/ExplorerController"; 
    static final boolean DEBUG_FRAGMENTS = false; // DO NOT SUBMIT WITH TRUE

    static final String KEY_EXPLORER_CONTEXT = "ExplorerController.explorerContext";

    /** The owner activity */
    final ExplorerActivity mActivity;
    final FragmentManager mFragmentManager;

    protected final ActionBarController mActionBarController;

    //final EmailAsyncTask.Tracker mTaskTracker = new EmailAsyncTask.Tracker();

    final RefreshManager mRefreshManager;

    // Other UI elements
    private ExplorerLayout mThreePane;
    /**
     * Fragments that are installed.
     *
     * A fragment is installed in {@link Fragment#onActivityCreated} and uninstalled in
     * {@link Fragment#onDestroyView}, using {@link FragmentInstallable} callbacks.
     *
     * This means fragments in the back stack are *not* installed.
     *
     * We set callbacks to fragments only when they are installed.
     *
     * @see FragmentInstallable
     */
    private DataListFragment mDataListFragment;
    
    private EmptyListFragment mEmptyListFragment;
    
    private	BibleListFragment mBibleListFragment;
    private BibleViewFragment mBibleViewFragment;
    
    private FileListFragment mFileListFragment;
    private FileViewFragment mFileViewFragment;
    
    private QueryListFragment mQueryListFragment;
    private QueryLaunchFragment mQueryLaunchFragment;

    /**
     * To avoid double-deleting a fragment (which will cause a runtime exception),
     * we put a fragment in this list when we {@link FragmentTransaction#remove(Fragment)} it,
     * and remove from the list when we actually uninstall it.
     */
    private final List<Fragment> mRemovedFragments = new LinkedList<Fragment>();
    
    protected BibleHelper.BibleEntry mExplorerBibleConstraint ;
    protected ExplorerContext mExplorerContext;

    
    private class RefreshListener implements RefreshManager.RefreshListener {
        private MenuItem mRefreshIcon;

        @Override
        public void onRefreshStart() {
            updateRefreshIcon();
        }
        @Override
        public void onRefreshFileChanged(String fileCode) {
            updateRefreshIcon();
            
            // @DAMS : Tell FileListFragment (if any) that something changed => it will tell the loader to reload
            if( isFileListInstalled() && fileCode.equals(getFileListFragment().getFileCode()) ) {
            	getFileListFragment().forceReload() ;
            }
        }

        void setRefreshIcon(MenuItem icon) {
            mRefreshIcon = icon;
            updateRefreshIcon();
        }

        private void updateRefreshIcon() {
            if (mRefreshIcon == null) {
                return;
            }

            if (isRefreshInProgress()) {
                mRefreshIcon.setActionView(R.layout.explorer_actionbar_indeterminate_progress);
            } else {
                mRefreshIcon.setActionView(null);
            }
        }
    };
    private final RefreshListener mRefreshListener = new RefreshListener();
    

    public ExplorerController(ExplorerActivity activity) {
        mActivity = activity;
        mFragmentManager = activity.getFragmentManager();
        mRefreshManager = RefreshManager.getInstance(mActivity);
        mActionBarController = createActionBarController(activity);
        if (DEBUG_FRAGMENTS) {
            FragmentManager.enableDebugLogging(true);
        }
    }
    
    /**
     * Called by the base class to let a subclass create an {@link ActionBarController}.
     */
    protected ActionBarController createActionBarController(Activity activity) {
        return new ActionBarController(activity, activity.getLoaderManager(),
                activity.getActionBar(), new ActionBarControllerCallback());
    }

    /** @return the layout ID for the activity. */
    public int getLayoutId() {
        return R.layout.explorer_activity_two_pane;
    }
    
    
    
    
    /**
     * Must be called just after the activity sets up the content view.  Used to initialize views.
     *
     * (Due to the complexity regarding class/activity initialization order, we can't do this in
     * the constructor.)
     */
    public void onActivityViewReady() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityViewReady");
        }
        
        // Load CrmPreferences
        loadSettings() ;

        // Set up content
        mThreePane = (ExplorerLayout) mActivity.findViewById(R.id.three_pane);
        mThreePane.setCallback(this);
    }

    /**
     * Called at the end of {@link EmailActivity#onCreate}.
     */
    public void onActivityCreated() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        mRefreshManager.registerListener(mRefreshListener);
        mActionBarController.onActivityCreated();
    }

    /**
     * Handles the {@link android.app.Activity#onStart} callback.
     */
    public void onActivityStart() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityStart");
        }
    }

    /**
     * Handles the {@link android.app.Activity#onResume} callback.
     */
    public void onActivityResume() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityResume");
        }
        refreshActionBar();
    }

    /**
     * Handles the {@link android.app.Activity#onPause} callback.
     */
    public void onActivityPause() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityPause");
        }
    }

    /**
     * Handles the {@link android.app.Activity#onStop} callback.
     */
    public void onActivityStop() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityStop");
        }
    }

    /**
     * Handles the {@link android.app.Activity#onDestroy} callback.
     */
    public void onActivityDestroy() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityDestroy");
        }
        mActionBarController.onActivityDestroy();
        mRefreshManager.unregisterListener(mRefreshListener);
    }

    /**
     * Handles the {@link android.app.Activity#onSaveInstanceState} callback.
     */
    public void onSaveInstanceState(Bundle outState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onSaveInstanceState");
        }
        mActionBarController.onSaveInstanceState(outState);
        outState.putParcelable(KEY_EXPLORER_CONTEXT, mExplorerContext);
    }

    /**
     * Handles the {@link android.app.Activity#onRestoreInstanceState} callback.
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " restoreInstanceState");
        }
        mActionBarController.onRestoreInstanceState(savedInstanceState);
        mExplorerContext = savedInstanceState.getParcelable(KEY_EXPLORER_CONTEXT);
    }
    
    
    
    /**
     * Load CrmPreferences
     * {@link FragmentInstallable#onInstallFragment}.
     */
    private void loadSettings() {
    	// Prefs CRM : Bind to account
    	mExplorerBibleConstraint = null ;
    	CrmExplorerConfig crmExplorerConfig = CrmExplorerConfig.getInstance(mActivity) ;
    	if( crmExplorerConfig.accountIsOn() ) {
    		PrefsCrm.ExplorerAccount ea = PrefsCrm.getExplorerAccount(mActivity) ;
    		if( ea != null && ea.bibleCode.equals(crmExplorerConfig.accountGetBibleCode())) {
    			BibleHelper bh = new BibleHelper(mActivity) ;
    			mExplorerBibleConstraint = bh.getBibleEntry(ea.bibleCode, ea.entryKey) ;
    		}
    	}
    }
    public void forceReloadSettings() {
    	loadSettings() ;
    }
    
    


    /**
     * Install a fragment.  Must be caleld from the host activity's
     * {@link FragmentInstallable#onInstallFragment}.
     */
    public final void onInstallFragment(Fragment fragment) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onInstallFragment  fragment=" + fragment);
        }
        if (fragment instanceof DataListFragment) {
            installDataListFragment((DataListFragment) fragment);
        } else if (fragment instanceof EmptyListFragment) {
            installEmptyListFragment((EmptyListFragment) fragment);
        } else if (fragment instanceof BibleListFragment) {
            installBibleListFragment((BibleListFragment) fragment);
        } else if (fragment instanceof BibleViewFragment) {
            installBibleViewFragment((BibleViewFragment) fragment);
        } else if (fragment instanceof FileListFragment) {
            installFileListFragment((FileListFragment) fragment);
        } else if (fragment instanceof FileViewFragment) {
            installFileViewFragment((FileViewFragment) fragment);
        } else if (fragment instanceof QueryListFragment) {
            installQueryListFragment((QueryListFragment) fragment);
        } else if (fragment instanceof QueryLaunchFragment) {
        	installQueryLaunchFragment((QueryLaunchFragment) fragment);
        } else {
            throw new IllegalArgumentException("Tried to install unknown fragment");
        }
    }

    /** Install fragment */
    protected void installDataListFragment(DataListFragment fragment) {
        mDataListFragment = fragment;
        mDataListFragment.setCallback(this);

        // TODO: consolidate this refresh with the one that the Fragment itself does. since
        // the fragment calls setHasOptionsMenu(true) - it invalidates when it gets attached.
        // However the timing is slightly different and leads to a delay in update if this isn't
        // here - investigate why. same for the other installs.
        refreshActionBar();
    }

    /** Install fragment */
    protected void installEmptyListFragment(EmptyListFragment fragment) {
        mEmptyListFragment = fragment;
        refreshActionBar();
    }


    /** Install fragment */
    protected void installBibleListFragment(BibleListFragment fragment) {
        mBibleListFragment = fragment;
        mBibleListFragment.setCallback(this);
        refreshActionBar();
        
        if (isDataListInstalled()) {
        	// @DAMS : TODO !
        	DataListFragment.DataListEntry dle = new DataListFragment.DataListEntry() ;
        	dle.dataType = DataListFragment.DataListEntry.DATA_BIBLE ;
        	dle.bibleCode = fragment.getBibleCode() ;
            getDataListFragment().setHighlightedDataListEntry(dle);
        }
        getBibleListFragment().setLayout(mThreePane);
    }
    protected void installBibleViewFragment(BibleViewFragment fragment) {
        mBibleViewFragment = fragment;
        mBibleViewFragment.setCallback(this);

        refreshActionBar();
        
        if( isBibleListInstalled() ) {
        	getBibleListFragment().setSelectedBibleEntry(fragment.getBibleEntryKey());
        }
    }


    /** Install fragment */
    protected void installFileListFragment(FileListFragment fragment) {
        mFileListFragment = fragment;
        mFileListFragment.setCallback(this);
        refreshActionBar();
        
        if (isDataListInstalled()) {
        	// @DAMS : TODO !
        	DataListFragment.DataListEntry dle = new DataListFragment.DataListEntry() ;
        	dle.dataType = DataListFragment.DataListEntry.DATA_FILE ;
        	dle.fileCode = fragment.getFileCode() ;
            getDataListFragment().setHighlightedDataListEntry(dle);
        }
        getFileListFragment().setLayout(mThreePane);
    }
    
    /** Install fragment */
    protected void installFileViewFragment(FileViewFragment fragment) {
        mFileViewFragment = fragment;
        mFileViewFragment.setCallback(this);

        refreshActionBar();
        
        if( isFileListInstalled() ) {
        	getFileListFragment().setSelectedFilerecord(fragment.getFilerecordId());
        }
    }
    
    
    /** Install fragment */
    protected void installQueryListFragment(QueryListFragment fragment) {
    	mQueryListFragment = fragment;
    	mQueryListFragment.setCallback(this);
        refreshActionBar();
        
        if (isDataListInstalled()) {
        	// @DAMS : TODO !
        	DataListFragment.DataListEntry dle = new DataListFragment.DataListEntry() ;
        	dle.dataType = DataListFragment.DataListEntry.DATA_QUERY ;
            getDataListFragment().setHighlightedDataListEntry(dle);
        }
        getQueryListFragment().setLayout(mThreePane);
    }
    
    /** Install fragment */
    protected void installQueryLaunchFragment(QueryLaunchFragment fragment) {
    	mQueryLaunchFragment = fragment;
    	mQueryLaunchFragment.setCallback(this);

        refreshActionBar();
        
        if( isQueryListInstalled() ) {
        	getQueryListFragment().setSelectedQuerysrc(fragment.getQuerySrcId());
        }
    }

    /**
     * Uninstall a fragment.  Must be caleld from the host activity's
     * {@link FragmentInstallable#onUninstallFragment}.
     */
    public final void onUninstallFragment(Fragment fragment) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onUninstallFragment  fragment=" + fragment);
        }
        mRemovedFragments.remove(fragment);
        if (fragment == mDataListFragment) {
            uninstallDataListFragment();
        } else if (fragment == mEmptyListFragment) {
            uninstallEmptyListFragment();
        } else if (fragment == mBibleListFragment) {
            uninstallBibleListFragment();
        } else if (fragment == mBibleViewFragment) {
            uninstallBibleViewFragment();
        } else if (fragment == mFileListFragment) {
            uninstallFileListFragment();
        } else if (fragment == mFileViewFragment) {
            uninstallFileViewFragment();
        } else if (fragment == mQueryListFragment) {
            uninstallQueryListFragment();
        } else if (fragment == mQueryLaunchFragment) {
        	uninstallQueryLaunchFragment();
        } else {
            throw new IllegalArgumentException("Tried to uninstall unknown fragment");
        }
    }

    /** Uninstall {@link DataListFragment} */
    protected void uninstallDataListFragment() {
    	mDataListFragment.setCallback(null);
    	mDataListFragment = null;
    }

    /** Uninstall {@link EmptyListFragment} */
    protected void uninstallEmptyListFragment() {
    	mEmptyListFragment = null;
    }

    /** Uninstall {@link BibleListFragment} */
    protected void uninstallBibleListFragment() {
    	mBibleListFragment.setCallback(null);
    	mBibleListFragment = null;
    }

    /** Uninstall {@link BibleViewFragment} */
    protected void uninstallBibleViewFragment() {
    	mBibleViewFragment.setCallback(null);
    	mBibleViewFragment = null;
    }

    /** Uninstall {@link FileListFragment} */
    protected void uninstallFileListFragment() {
    	mFileListFragment.setCallback(null);
    	mFileListFragment = null;
    }

    /** Uninstall {@link FileViewFragment} */
    protected void uninstallFileViewFragment() {
    	mFileViewFragment.setCallback(null);
    	mFileViewFragment = null;
    }

    /** Uninstall {@link QueryListFragment} */
    protected void uninstallQueryListFragment() {
    	mQueryListFragment.setCallback(null);
    	mQueryListFragment = null;
    }

    /** Uninstall {@link MessageViewFragment} */
    protected void uninstallQueryLaunchFragment() {
    	mQueryLaunchFragment.setCallback(null);
    	mQueryLaunchFragment = null;
    }

    /**
     * If a {@link Fragment} is not already in {@link #mRemovedFragments},
     * {@link FragmentTransaction#remove} it and add to the list.
     *
     * Do nothing if {@code fragment} is null.
     */
    protected final void removeFragment(FragmentTransaction ft, Fragment fragment) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " removeFragment fragment=" + fragment);
        }
        if (fragment == null) {
            return;
        }
        if (!mRemovedFragments.contains(fragment)) {
            // Remove try/catch when b/4981556 is fixed (framework bug)
            try {
                ft.remove(fragment);
            } catch (IllegalStateException ex) {
            	/*
                Log.e(LOGTAG, "Swalling IllegalStateException due to known bug for "
                        + " fragment: " + fragment, ex);
                Log.e(LOGTAG, Utility.dumpFragment(fragment));
                */
            }
            addFragmentToRemovalList(fragment);
        }
    }

    /**
     * Remove a {@link Fragment} from {@link #mRemovedFragments}.  No-op if {@code fragment} is
     * null.
     *
     * {@link #removeMailboxListFragment}, {@link #removeMessageListFragment} and
     * {@link #removeMessageViewFragment} all call this, so subclasses don't have to do this when
     * using them.
     *
     * However, unfortunately, subclasses have to call this manually when popping from the
     * back stack to avoid double-delete.
     */
    protected void addFragmentToRemovalList(Fragment fragment) {
        if (fragment != null) {
            mRemovedFragments.add(fragment);
        }
    }

    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeDataListFragment(FragmentTransaction ft) {
        removeFragment(ft, mDataListFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeEmptyListFragment(FragmentTransaction ft) {
        removeFragment(ft, mEmptyListFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeBibleListFragment(FragmentTransaction ft) {
        removeFragment(ft, mBibleListFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeBibleViewFragment(FragmentTransaction ft) {
        removeFragment(ft, mBibleViewFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeFileListFragment(FragmentTransaction ft) {
        removeFragment(ft, mFileListFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeFileViewFragment(FragmentTransaction ft) {
        removeFragment(ft, mFileViewFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeQueryListFragment(FragmentTransaction ft) {
        removeFragment(ft, mQueryListFragment);
        return ft;
    }
    /**
     * Remove the fragment if it's installed.
     */
    protected FragmentTransaction removeQueryLaunchFragment(FragmentTransaction ft) {
        removeFragment(ft, mQueryLaunchFragment);
        return ft;
    }
    

    /** @return true if a {@link mDataListFragment} is installed. */
    protected final boolean isDataListInstalled() {
        return mDataListFragment != null;
    }
    /** @return true if a {@link mFileListFragment} is installed. */
    protected final boolean isBibleListInstalled() {
        return mBibleListFragment != null;
    }
    /** @return true if a {@link mFileListFragment} is installed. */
    protected final boolean isBibleViewInstalled() {
        return mBibleViewFragment != null;
    }
    /** @return true if a {@link mFileListFragment} is installed. */
    protected final boolean isFileListInstalled() {
        return mFileListFragment != null;
    }
    /** @return true if a {@link mFileViewFragment} is installed. */
    protected final boolean isFileViewInstalled() {
        return mFileViewFragment != null;
    }
    /** @return true if a {@link mQueryListFragment} is installed. */
    protected final boolean isQueryListInstalled() {
        return mQueryListFragment != null;
    }
    /** @return true if a {@link mQueryLaunchFragment} is installed. */
    protected final boolean isQueryLaunchInstalled() {
        return mQueryLaunchFragment != null;
    }
    
    
    /** @return the installed {@link DataListFragment} or null. */
    protected final DataListFragment getDataListFragment() {
        return mDataListFragment;
    }
    /** @return the installed {@link FileListFragment} or null. */
    protected final BibleListFragment getBibleListFragment() {
        return mBibleListFragment;
    }
    /** @return the installed {@link FileListFragment} or null. */
    protected final BibleViewFragment getBibleViewFragment() {
        return mBibleViewFragment;
    }
    /** @return the installed {@link FileListFragment} or null. */
    protected final FileListFragment getFileListFragment() {
        return mFileListFragment;
    }
    /** @return the installed {@link FileViewFragment} or null. */
    protected final FileViewFragment getFileViewFragment() {
        return mFileViewFragment;
    }
    /** @return the installed {@link QueryListFragment} or null. */
    protected final QueryListFragment getQueryListFragment() {
        return mQueryListFragment;
    }
    /** @return the installed {@link QueryLaunchFragment} or null. */
    protected final QueryLaunchFragment getQueryLaunchFragment() {
        return mQueryLaunchFragment;
    }

    /**
     * Commit a {@link FragmentTransaction}.
     */
    protected void commitFragmentTransaction(FragmentTransaction ft) {
        if (DEBUG_FRAGMENTS) {
            Log.d(LOGTAG, this + " commitFragmentTransaction: " + ft);
        }
        if (!ft.isEmpty()) {
            // NB: there should be no cases in which a transaction is committed after
            // onSaveInstanceState. Unfortunately, the "state loss" check also happens when in
            // LoaderCallbacks.onLoadFinished, and we wish to perform transactions there. The check
            // by the framework is conservative and prevents cases where there are transactions
            // affecting Loader lifecycles - but we have no such cases.
            // TODO: use asynchronous callbacks from loaders to avoid this implicit dependency
            ft.commitAllowingStateLoss();
            mFragmentManager.executePendingTransactions();
        }
    }


    /**
     * Returns the id of the mailbox used for the message list fragment.
     *
     * IMPORTANT: Do not confuse {@link #getMailboxListMailboxId()} with
     *     {@link #getMessageListMailboxId()}
     */
    protected String getBibleListFileCode() {
        return isBibleListInstalled() ? getBibleListFragment().getBibleCode()
                : null ;
    }
    protected String getFileListFileCode() {
        return isFileListInstalled() ? getFileListFragment().getFileCode()
                : null ;
    }
    protected List<BibleHelper.BibleEntry> getFileListBibleConditions() {
        return isFileListInstalled() ? getFileListFragment().getBibleConditions()
                : null ;
    }
    protected ExplorerContext getFileListExplorerContext() {
        return isFileListInstalled() ? getFileListFragment().getExplorerContext() 
                : null ;
    }
    protected String getBibleViewBibleCode() {
        return isBibleViewInstalled() ? getBibleViewFragment().getBibleCode()
                : null ;
    }
    protected String getBibleViewBibleEntryKey() {
        return isBibleViewInstalled() ? getBibleViewFragment().getBibleEntryKey()
                : null ;
    }
    protected long getFileViewFilerecordId() {
        return isFileViewInstalled() ? getFileViewFragment().getFilerecordId()
                : 0 ;
    }
    protected long getQueryLaunchQuerySrcId() {
        return isQueryLaunchInstalled() ? getQueryLaunchFragment().getQuerySrcId()
                : 0 ;
    }

    /**
     * Opens a given list
     * @param listContext the list context for the message list to open
     * @param messageId if specified and not {@link Message#NO_MESSAGE}, will open the message
     *     in the message list.
     */
    public final void open(final ExplorerContext explorerContext, final String[] bibleCodePlusEntryKey, final long filerecordId, final int querysrcId ) {
        if (explorerContext.equals(mExplorerContext)) {
            return;
        }

        if (Explorer.DEBUG) {
            Log.i(LOGTAG, this + " open: " + explorerContext);
        }
        mExplorerContext = explorerContext;
        
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        
        // Fragment DataList
        updateDataList(ft, true);
        
        if( explorerContext.mMode == ExplorerContext.MODE_QUERY ) {
        	updateQueryList(ft, true);
        } else if( explorerContext.mMode == ExplorerContext.MODE_FILE ) {
            updateFileList(ft, true);
        } else if( explorerContext.mMode == ExplorerContext.MODE_BIBLE ) {
            updateBibleList(ft, true);
        } else {
        	updateEmptyList(ft, true);
        }

        if (querysrcId != 0 && explorerContext.mMode == ExplorerContext.MODE_QUERY ) {
            updateQueryLaunch(ft, querysrcId);
            mThreePane.showRightPane();
        } else if (filerecordId != 0 && explorerContext.mMode == ExplorerContext.MODE_FILE ) {
            updateFileView(ft, filerecordId);
            mThreePane.showRightPane();
        } else if (bibleCodePlusEntryKey != null && explorerContext.mMode == ExplorerContext.MODE_BIBLE ) {
            updateBibleView(ft, bibleCodePlusEntryKey[0], bibleCodePlusEntryKey[1]);
            mThreePane.showRightPane();
        } else if (explorerContext.isFiltered()) {
            mThreePane.showRightPane();
            mThreePane.uncollapsePane();
        } else {
            mThreePane.showLeftPane();
        }
        commitFragmentTransaction(ft);

        if (explorerContext.isFiltered()) {
            // mActionBarController.enterSearchMode(explorerContext.getSearchParams().mFilter);
        }
    }
    
    public final void openEmptyList() {
    	final ExplorerContext newExplorerContext = ExplorerContext.forNone() ;
    	if( newExplorerContext.equals(mExplorerContext) ) {
    		return ;
    	}
    	mExplorerContext = newExplorerContext;
    	updateDataList(true) ;
    	updateEmptyList(true);
    }
    
    public final void openBibleList( String bibleCode ) {
    	final ExplorerContext newExplorerContext = ExplorerContext.forBible(bibleCode, null) ;
    	if( newExplorerContext.equals(mExplorerContext) ) {
    		return ;
    	}
    	mExplorerContext = newExplorerContext;
    	updateBibleList(true);
    }
    public final void openBibleEntry( String bibleCode, String bibleEntryKey ) {
        if (getBibleViewBibleCode() != bibleCode || getBibleViewBibleEntryKey() != bibleEntryKey) {
            navigateToBible(bibleCode, bibleEntryKey);
            mThreePane.showRightPane();
        }
    }
    
    public final void openFileList( String fileCode ) {
    	final ExplorerContext newExplorerContext = ExplorerContext.forFile(fileCode, null) ;
    	if( newExplorerContext.equals(mExplorerContext) ) {
    		return ;
    	}
    	mExplorerContext = newExplorerContext;
    	updateFileList(true);
    }
    public final void openFileList( String fileCode, BibleHelper.BibleEntry filterBe ) {
    	final ExplorerContext newExplorerContext = ExplorerContext.forFile(fileCode, filterBe) ;
    	if( newExplorerContext.equals(mExplorerContext) ) {
    		Log.w(LOGTAG,"Is the same??") ;
    		return ;
    	}
    	mExplorerContext = newExplorerContext;
    	updateFileList(true);
    }
    public final void openFilerecord( long filerecordId ) {
        if (getFileViewFilerecordId() != filerecordId) {
            navigateToFile(filerecordId);
            mThreePane.showRightPane();
        }
    }

    public final void openQueryList() {
    	final ExplorerContext newExplorerContext = ExplorerContext.forQuery(null) ;
    	if( newExplorerContext.equals(mExplorerContext) ) {
    		return ;
    	}
    	mExplorerContext = newExplorerContext;
    	updateQueryList(true);
    	mThreePane.showRightPane();
    }
    public final void openQueryLaunch( int querysrcId ) {
        if (getQueryLaunchQuerySrcId() != querysrcId) {
            navigateToQuery(querysrcId);
            mThreePane.showRightPane();
        }
    }

    /**
     * Loads the given account and optionally selects the given mailbox and message. If the
     * specified account is already selected, no actions will be performed unless
     * <code>forceReload</code> is <code>true</code>.
     *
     * @param ft {@link FragmentTransaction} to use.
     * @param clearDependentPane if true, the message list and the message view will be cleared
     */
    private void updateDataList(FragmentTransaction ft, boolean clearDependentPane) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateMailboxList " );
        }
        
        // @DAMS : TODO : Construction d'un DataListFragment.DataListEntry à partir du Explorer.Context

        if ( true ) { // @DAMS : TODO : refresh everyTime ??
            removeDataListFragment(ft);
            boolean enableHighlight = true;
            ft.add(mThreePane.getLeftPaneId(),
                    DataListFragment.newInstance(enableHighlight,null));
        }
        if (clearDependentPane) {
            removeBibleListFragment(ft);
            removeBibleViewFragment(ft);
            removeFileListFragment(ft);
            removeFileViewFragment(ft);
            removeQueryListFragment(ft);
            removeQueryLaunchFragment(ft);
        }
    }
    /**
     * Shortcut to call {@link #updateDataList(FragmentTransaction, boolean)} and
     * commit.
     */
    private void updateDataList(boolean clearDependentPane) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateDataList(ft, clearDependentPane);
        commitFragmentTransaction(ft);
    }

    /**
     * Go back to a mailbox list view. If a message view is currently active, it will
     * be hidden.
     */
    private void goBackToList() {
        if (isFileViewInstalled()) {
            mThreePane.showLeftPane(); // Show mailbox list
        }
    }

    /**
     * Show the DUMMY empty fragment for the given fileCode.
     *
     * @param ft {@link FragmentTransaction} to use.
     */
    private void updateEmptyList(FragmentTransaction ft, boolean clearDependentPane) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateEmptyList ");
        }

        removeEmptyListFragment(ft);
        removeBibleListFragment(ft);
        removeFileListFragment(ft);
        removeQueryListFragment(ft);
        ft.add(mThreePane.getMiddlePaneId(), EmptyListFragment.newInstance());
        if (clearDependentPane) {
        	removeBibleViewFragment(ft);
            removeFileViewFragment(ft);
            removeQueryLaunchFragment(ft);
        }
    }
    /**
     * Shortcut to call {@link #updateFileList(FragmentTransaction, boolean)} and
     * commit.
     */
    private void updateEmptyList(boolean clearDependentPane) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateEmptyList(ft, clearDependentPane);
        commitFragmentTransaction(ft);
    }
    
    
    private void updateBibleList(FragmentTransaction ft, boolean clearDependentPane) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateBibleList " + mExplorerContext);
        }

        if ( !mExplorerContext.equals(getFileListExplorerContext()) ) {
            removeEmptyListFragment(ft);
            removeBibleListFragment(ft);
            removeFileListFragment(ft);
            removeQueryListFragment(ft);
            ft.add(mThreePane.getMiddlePaneId(), BibleListFragment.newInstance(mExplorerContext));
        }
        if (clearDependentPane) {
            removeBibleViewFragment(ft);
            removeFileViewFragment(ft);
            removeQueryLaunchFragment(ft);
        }
    }
    private void updateBibleList(boolean clearDependentPane) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateBibleList(ft, clearDependentPane);
        commitFragmentTransaction(ft);
    }
    private void updateBibleView(FragmentTransaction ft, String bibleCode, String bibleEntryKey) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateMessageView bibleEntryKey=" + bibleEntryKey);
        }
        if (bibleEntryKey == null) {
            throw new IllegalArgumentException();
        }

        if (bibleCode == getBibleViewBibleCode() && bibleEntryKey == getBibleViewBibleEntryKey()) {
            return; // nothing to do.
        }

        removeQueryLaunchFragment(ft);
        removeFileViewFragment(ft);
        removeBibleViewFragment(ft);

        ft.add(mThreePane.getRightPaneId(), BibleViewFragment.newInstance(bibleCode,bibleEntryKey));
    }
    
    

    /**
     * Show the CrmFILE list fragment for the given fileCode.
     *
     * @param ft {@link FragmentTransaction} to use.
     */
    private void updateFileList(FragmentTransaction ft, boolean clearDependentPane) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateFileList " + mExplorerContext);
        }

        if ( !mExplorerContext.equals(getFileListExplorerContext()) ) {
            removeEmptyListFragment(ft);
            removeBibleListFragment(ft);
            removeFileListFragment(ft);
            removeQueryListFragment(ft);
            ft.add(mThreePane.getMiddlePaneId(), FileListFragment.newInstance(mExplorerContext));
        }
        if (clearDependentPane) {
            removeBibleViewFragment(ft);
            removeFileViewFragment(ft);
            removeQueryLaunchFragment(ft);
        }
    }

    /**
     * Shortcut to call {@link #updateFileList(FragmentTransaction, boolean)} and
     * commit.
     */
    private void updateFileList(boolean clearDependentPane) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateFileList(ft, clearDependentPane);
        commitFragmentTransaction(ft);
    }

    /**
     * Show a message on the message view.
     *
     * @param ft {@link FragmentTransaction} to use.
     * @param messageId ID of the mailbox to load. Must never be {@link Message#NO_MESSAGE}.
     */
    private void updateFileView(FragmentTransaction ft, long filerecordId) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateMessageView filerecordId=" + filerecordId);
        }
        if (filerecordId == 0) {
            throw new IllegalArgumentException();
        }

        if (filerecordId == getFileViewFilerecordId()) {
            return; // nothing to do.
        }

        removeQueryLaunchFragment(ft);
        removeFileViewFragment(ft);
        removeBibleViewFragment(ft);

        ft.add(mThreePane.getRightPaneId(), FileViewFragment.newInstance(filerecordId));
    }

    /**
     * Shortcut to call {@link #updateMessageView(FragmentTransaction, long)} and commit.
     */
    protected void navigateToBible(String bibleCode, String bibleEntryKey) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateBibleView(ft, bibleCode, bibleEntryKey);
        commitFragmentTransaction(ft);
    }

    /**
     * Shortcut to call {@link #updateMessageView(FragmentTransaction, long)} and commit.
     */
    protected void navigateToFile(long filerecordId) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateFileView(ft, filerecordId);
        commitFragmentTransaction(ft);
    }

    /**
     * Remove the message view if shown.
     */
    private void unselectMessage() {
        commitFragmentTransaction(removeFileViewFragment(mFragmentManager.beginTransaction()));
        if (isBibleListInstalled()) {
            getBibleListFragment().setSelectedBibleEntry(null);
        }
        if (isFileListInstalled()) {
            getFileListFragment().setSelectedFilerecord(-1);
        }
    }

    
    
    /**
     * Show the CrmQUERY query fragment
     *
     * @param ft {@link FragmentTransaction} to use.
     */
    private void updateQueryList(FragmentTransaction ft, boolean clearDependentPane) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateQueryList " + mExplorerContext);
        }

        if ( !isQueryListInstalled() ) {
            removeEmptyListFragment(ft);
            removeBibleListFragment(ft);
            removeFileListFragment(ft);
            removeQueryListFragment(ft);
            ft.add(mThreePane.getMiddlePaneId(), QueryListFragment.newInstance());
        }
        if (clearDependentPane) {
            removeBibleViewFragment(ft);
            removeFileViewFragment(ft);
            removeQueryLaunchFragment(ft);
        }
    }

    /**
     * Shortcut to call {@link #updateFileList(FragmentTransaction, boolean)} and
     * commit.
     */
    private void updateQueryList(boolean clearDependentPane) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateQueryList(ft, clearDependentPane);
        commitFragmentTransaction(ft);
    }
    
    /**
     * Show a message on the message view.
     *
     * @param ft {@link FragmentTransaction} to use.
     * @param messageId ID of the mailbox to load. Must never be {@link Message#NO_MESSAGE}.
     */
    private void updateQueryLaunch(FragmentTransaction ft, int querysrcId) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " updateQueryLaunch querysrcId=" + querysrcId);
        }
        if (querysrcId == 0) {
            throw new IllegalArgumentException();
        }

        if (querysrcId == getQueryLaunchQuerySrcId()) {
            return; // nothing to do.
        }

        removeBibleViewFragment(ft);
        removeFileViewFragment(ft);
        removeQueryLaunchFragment(ft);

        ft.add(mThreePane.getRightPaneId(), QueryLaunchFragment.newInstance(querysrcId));
    }

    /**
     * Shortcut to call {@link #updateQueryLaunch(FragmentTransaction, long)} and commit.
     */
    protected void navigateToQuery(int querysrcId) {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        updateQueryLaunch(ft, querysrcId);
        commitFragmentTransaction(ft);
    }

    /**
     * Remove the query view if shown.
     */
    private void unselectQuery() {
        commitFragmentTransaction(removeQueryLaunchFragment(mFragmentManager.beginTransaction()));
        if (isQueryListInstalled()) {
            getQueryListFragment().setSelectedQuerysrc(-1);
        }
    }
    
    
    
    /**
     * Performs the back action.
     *
     * @param isSystemBackKey <code>true</code> if the system back key was pressed.
     * <code>false</code> if it's caused by the "home" icon click on the action bar.
     */
    public boolean onBackPressed(boolean isSystemBackKey) {
        if (!mThreePane.isPaneCollapsible()) {
            if (mActionBarController.onBackPressed(isSystemBackKey)) {
                return true;
            }

            if (mThreePane.showLeftPane()) {
                return true;
            }
            
            if( mExplorerContext.isFiltered() ) {
            	onSearchDiscard();
            	return true ;
            }
        } else {
            // If it's not the system back key, always attempt to uncollapse the left pane first.
            if (!isSystemBackKey && mThreePane.uncollapsePane()) {
                return true;
            }

            if (mActionBarController.onBackPressed(isSystemBackKey)) {
                return true;
            }

            if (mThreePane.showLeftPane()) {
                return true;
            }
            
            if( mExplorerContext.isFiltered() ) {
            	onSearchDiscard();
            	return true ;
            }
       }

        /*
        if (isMailboxListInstalled() && getMailboxListFragment().navigateUp()) {
            return true;
        }
        */
        return false;
    }

    
    public void onSearchStarted() {
        // Show/hide the original search icon.
        mActivity.invalidateOptionsMenu();
    }
    protected void onSearchExit() {
        // Show/hide the original search icon.
        mActivity.invalidateOptionsMenu();
    }

    /**
     * Must be called from {@link Activity#onSearchRequested()}.
     * This initiates the search entry mode - see {@link #onSearchSubmit} for when the search
     * is actually submitted.
     */
    public void onSearchRequested() {
        boolean contextIsSearchable = false ;
        if( mExplorerContext != null 
        		&& (mExplorerContext.mMode==ExplorerContext.MODE_FILE) ) {
        	contextIsSearchable = true ;
        }
        if( !contextIsSearchable ) {
        	return ;
        }
        if( isFileListReady() ) {
        	mActionBarController.enterSearchMode();
        }
    }

    /**
     * @return Whether or not the FILE list is ready and has its initial meta data loaded.
     */
    protected boolean isFileListReady() {
        return isFileListInstalled() && getFileListFragment().hasDataLoaded();
    }

    /**
     * Determines the mailbox to search, if a search was to be initiated now.
     * This will return {@code null} if the UI is not focused on any particular mailbox to search
     * on.
     */
    private String getSearchableFile() {
        if (!isFileListReady()) {
            return null;
        }
        FileListFragment fileList = getFileListFragment();

        // If already in a search, future searches will search the original mailbox.
        return fileList.getFileCode() ;
    }



    /**
     * Kicks off a search query
     */
    protected void onSearchSubmit(final BibleHelper.BibleEntry queryBe) {
    	switch( mExplorerContext.mMode ) {
    	case ExplorerContext.MODE_FILE :
    		//Log.w(LOGTAG,"Opening..."+queryBe.displayStr) ;
    		openFileList(mExplorerContext.mFileCode,queryBe) ;
    		break ;
    	}
    }
    protected void onSearchDiscard() {
    	switch( mExplorerContext.mMode ) {
    	case ExplorerContext.MODE_FILE :
    		//Log.w(LOGTAG,"Opening..."+queryBe.displayStr) ;
    		openFileList(mExplorerContext.mFileCode) ;
    		break ;
    	}
    }


    /**
     * Handles the {@link android.app.Activity#onCreateOptionsMenu} callback.
     */
    public boolean onCreateOptionsMenu(MenuInflater inflater, Menu menu) {
        inflater.inflate(R.menu.explorer_activity_options, menu);
        return true;
    }

    /**
     * Handles the {@link android.app.Activity#onPrepareOptionsMenu} callback.
     */
    public boolean onPrepareOptionsMenu(MenuInflater inflater, Menu menu) {
        // Update the refresh button.
        MenuItem item = menu.findItem(R.id.refresh);
        if (isRefreshEnabled()) {
            item.setVisible(true);
            mRefreshListener.setRefreshIcon(item);
        } else {
            item.setVisible(false);
            mRefreshListener.setRefreshIcon(null);
        }
        
        boolean contextIsSearchable = false ;
        if( mExplorerContext != null 
        		&& (mExplorerContext.mMode==ExplorerContext.MODE_BIBLE || mExplorerContext.mMode==ExplorerContext.MODE_FILE) ) {
        	contextIsSearchable = true ;
        }
        boolean showSearchIcon = (contextIsSearchable && !mActionBarController.isInSearchMode()) ;
        menu.findItem(R.id.search).setVisible(showSearchIcon);
        
        boolean showGoQuery = isQueryLaunchInstalled() && !getQueryLaunchFragment().isQueryRunning() ;
        menu.findItem(R.id.query_go).setVisible(showGoQuery);

        boolean showSettings = true ;
        menu.findItem(R.id.settings).setVisible(showSettings);
        
        return true;
    }

    /**
     * Handles the {@link android.app.Activity#onOptionsItemSelected} callback.
     *
     * @return true if the option item is handled.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Comes from the action bar when the app icon on the left is pressed.
                // It works like a back press, but it won't close the activity.
                return onBackPressed(false);
            case R.id.refresh:
                onRefresh();
                return true;
            case R.id.settings:
                return onMenuSettings();
            case R.id.search:
                onSearchRequested();
                return true;
            case R.id.query_go:
                if( isQueryLaunchInstalled() ) {
                	getQueryLaunchFragment().goQuery() ;
                }
                return true;
        }
        return false;
    }

    /**
     * Handles the "Settings" option item.  Opens the settings activity.
     */
    private boolean onMenuSettings() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(mActivity, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mActivity.startActivity(intent);
        return true;
    }



    /**
     * Performs "refesh".
     */
    protected void onRefresh() {
    	mRefreshManager.refreshFileList( getFileListFileCode(), getFileListBibleConditions() ) ;
    }

    /**
     * @return true if refresh is in progress for the current mailbox.
     */
    protected boolean isRefreshInProgress() {
    	return mRefreshManager.isFileRefreshing(getFileListFileCode()) ;
    }

    /**
     * @return true if the UI should enable the "refresh" command.
     */
    protected boolean isRefreshEnabled() {
    	if( mExplorerContext.mMode == ExplorerContext.MODE_FILE ) {
    		return true ;
    	}
    	return false ;
    }

    /**
     * Refresh the action bar and menu items, including the "refreshing" icon.
     */
    protected void refreshActionBar() {
        if (mActionBarController != null) {
            mActionBarController.refresh();
        }
        mActivity.invalidateOptionsMenu();
    }


    
    // ExplorerLayoutCallback
    @Override
    public void onVisiblePanesChanged(int previousVisiblePanes) {
        // If the right pane is gone, remove the message view.
        final int visiblePanes = mThreePane.getVisiblePanes();

        if (((visiblePanes & ExplorerLayout.PANE_RIGHT) == 0) &&
                ((previousVisiblePanes & ExplorerLayout.PANE_RIGHT) != 0)) {
            // Message view just got hidden
            unselectMessage();
            
            if( mExplorerContext.mMode == ExplorerContext.MODE_QUERY ) {
            	// En mode Queries, on ne veut pas de vue large
            	openEmptyList() ;
            }
        }
        // Disable CAB when the message list is not visible.
        // @DAMS : pas de Custom Action Bar
        /*
        if (isFileListInstalled()) {
            getFileListFragment().onHidden((visiblePanes & ExplorerLayout.PANE_MIDDLE) == 0);
        }
        */
        
        
        
        
        
        refreshActionBar();
    }
    
    
    
    private class ActionBarControllerCallback implements ActionBarController.Callback {

        @Override
        public boolean shouldShowUp() {
            final int visiblePanes = mThreePane.getVisiblePanes();
            final boolean leftPaneHidden = ((visiblePanes & ExplorerLayout.PANE_LEFT) == 0);
            return leftPaneHidden || isInFilterMode() ;
        }

        @Override
        public void onSearchStarted() {
        	ExplorerController.this.onSearchStarted();
        }

        @Override
        public void onSearchSubmit(final BibleHelper.BibleEntry queryBe) {
        	ExplorerController.this.onSearchSubmit(queryBe);
        }

        @Override
        public void onSearchExit() {
        	ExplorerController.this.onSearchExit();
        }

		@Override
		public boolean isInFilterMode() {
			if( mExplorerContext != null && mExplorerContext.mFilteredBibleEntry != null ) {
				return true ;
			}
			return false;
		}

		@Override
		public BibleEntry getExplorerConstraint() {
			if( mExplorerBibleConstraint != null ) {
				return mExplorerBibleConstraint ;
			}
			return null;
		}
		@Override
		public BibleEntry getFilteredBibleEntry() {
			if( mExplorerContext != null ) {
				return mExplorerContext.mFilteredBibleEntry ;
			}
			return null;
		}

		@Override
		public ExplorerContext getCurrentExplorerContext() {
			return mExplorerContext;
		}
    }



    // DataListFragment$Callback
	@Override
	public void onBibleSelected(String bibleCode) {
		openBibleList(bibleCode) ;
	}

	// DataListFragment$Callback
	@Override
	public void onFileSelected(String fileCode) {
		openFileList(fileCode) ;
		
	}

	// DataListFragment$Callback
	@Override
	public void onQuerySelected() {
		openQueryList() ;
	}

	// BibleListFragment$Callback
	@Override
	public void onBibleEntryOpen(String bibleCode, String bibleEntryKey) {
		openBibleEntry(bibleCode, bibleEntryKey) ;
	}

	// FileListFragment$Callback
	@Override
	public void onFilerecordOpen(long filerecordId) {
		openFilerecord(filerecordId) ;
	}

	// QueryLaunchFragment$Callback
	@Override
	public void onQueryLaunchStart(int querysrcId) {
		mActivity.invalidateOptionsMenu();
	}
	// QueryLaunchFragment$Callback
	@Override
	public void onQueryResponseFailure(int querysrcId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setMessage("Unable to complete query. Check network status.")
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
		
		
		mActivity.invalidateOptionsMenu();
	}
	// QueryLaunchFragment$Callback
	@Override
	public void onQueryResponseSuccess(int querysrcId, int cacheResultJsonId) {
		mActivity.invalidateOptionsMenu();
		// TODO Auto-generated method stub
		
		// Type de la query ?
		CrmQueryModel cqm = CrmQueryManager.model_get( mActivity, querysrcId ) ;
		
    	final Bundle bundle = new Bundle();
    	bundle.putInt(QueryViewActivity.ARG_QUERYSRC_ID, querysrcId);
    	bundle.putInt(QueryViewActivity.ARG_JSONRESULT_ID, cacheResultJsonId) ;
		
    	Intent intent ;
    	switch( cqm.querysrcType ) {
    	case QWEB:
          	intent = new Intent(mActivity, QwebViewActivity.class);
          	intent.setClass(mActivity, QwebViewActivity.class);
        	intent.putExtras(bundle);
        	mActivity.startActivity(intent);
        	break ;
    	default :
          	intent = new Intent(mActivity, QueryViewActivity.class);
          	intent.setClass(mActivity, QueryViewActivity.class);
        	intent.putExtras(bundle);
        	mActivity.startActivity(intent);
        	break ;
    	}
	}

	// QueryListFragment$Callback
	@Override
	public void onQuerySelect(int querysrcId) {
		openQueryLaunch(querysrcId) ;
	}

	@Override
	public BibleEntry getExplorerConstraint() {
		return mExplorerBibleConstraint;
	}

}
