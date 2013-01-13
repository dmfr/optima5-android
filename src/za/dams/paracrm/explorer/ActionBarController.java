package za.dams.paracrm.explorer;

import za.dams.paracrm.R;
import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;


public class ActionBarController {
	private static final String BUNDLE_KEY_MODE = "ActionBarController.BUNDLE_KEY_MODE";
	
    private static final int MODE_NORMAL = 0;
    private static final int MODE_SEARCH = 1;
    
    private final Context mContext;
    //private final LoaderManager mLoaderManager;
    private final ActionBar mActionBar;
    
    private final ViewGroup mActionBarCustomView;
    
    private View mSearchContainer;
    private SearchView mSearchView;
    
    /** Either {@link #MODE_NORMAL} or {@link #MODE_SEARCH}. */
    private int mSearchMode = MODE_NORMAL;
    
    public final Callback mCallback;
    
    public interface Callback {
        /** @return the "UP" arrow should be shown. */
        public boolean shouldShowUp();
        
        /**
         * Retrieves the hint text to be shown for when a search entry is being made.
         */
        public String getSearchHint();

        /**
         * Called when the action bar initially shows the search entry field.
         */
        public void onSearchStarted();

        /**
         * Called when a search is submitted.
         *
         * @param queryTerm query string
         */
        public void onSearchSubmit(String queryTerm);

        /**
         * Called when the search box is closed.
         */
        public void onSearchExit();
    }
    
    public ActionBarController(Context context, LoaderManager loaderManager,
            ActionBar actionBar, Callback callback) {
        mContext = context;
        mActionBar = actionBar;
        mCallback = callback;

        // Configure action bar.
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_CUSTOM);

        // Prepare the custom view
        mActionBar.setCustomView(R.layout.explorer_actionbar_custom_view);
        mActionBarCustomView = (ViewGroup) mActionBar.getCustomView();

    }

    private void initSearchViews() {
        if (mSearchContainer == null) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            mSearchContainer = inflater.inflate(R.layout.explorer_actionbar_search, null);
            mSearchView = UiUtilities.getView(mSearchContainer, R.id.search_view);
            mSearchView.setSubmitButtonEnabled(false);
            mSearchView.setOnQueryTextListener(mOnQueryText);
            mSearchView.onActionViewExpanded();
            mActionBarCustomView.addView(mSearchContainer);
        }
    }
    
    /** Must be called from {@link UIControllerBase#onActivityCreated()} */
    public void onActivityCreated() {
        refresh();
    }

    /** Must be called from {@link UIControllerBase#onActivityDestroy()} */
    public void onActivityDestroy() {
    }

    /** Must be called from {@link UIControllerBase#onSaveInstanceState} */
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(BUNDLE_KEY_MODE, mSearchMode);
    }

    /** Must be called from {@link UIControllerBase#onRestoreInstanceState} */
    public void onRestoreInstanceState(Bundle savedState) {
        int mode = savedState.getInt(BUNDLE_KEY_MODE);
        if (mode == MODE_SEARCH) {
            // No need to re-set the initial query, as the View tree restoration does that
            enterSearchMode(null);
        }
    }

    /**
     * @return true if the search box is shown.
     */
    public boolean isInSearchMode() {
        return mSearchMode == MODE_SEARCH;
    }

    /**
     * @return Whether or not the search bar should be shown. This is a function of whether or not a
     *     search is active, and if the current layout supports it.
     */
    private boolean shouldShowSearchBar() {
        return isInSearchMode() ;
    }
    
    /**
     * Show the search box.
     *
     * @param initialQueryTerm if non-empty, set to the search box.
     */
    public void enterSearchMode(String initialQueryTerm) {
        initSearchViews();
        if (isInSearchMode()) {
            return;
        }
        if (!TextUtils.isEmpty(initialQueryTerm)) {
            mSearchView.setQuery(initialQueryTerm, false);
        } else {
            mSearchView.setQuery("", false);
        }
        mSearchView.setQueryHint(mCallback.getSearchHint());

        mSearchMode = MODE_SEARCH;

        // Focus on the search input box and throw up the IME if specified.
        // TODO: HACK. this is a workaround IME not popping up.
        mSearchView.setIconified(false);

        refresh();
        mCallback.onSearchStarted();
    }

    public void exitSearchMode() {
        if (!isInSearchMode()) {
            return;
        }
        mSearchMode = MODE_NORMAL;

        refresh();
        mCallback.onSearchExit();
    }
    
    /**
     * Performs the back action.
     *
     * @param isSystemBackKey <code>true</code> if the system back key was pressed.
     * <code>false</code> if it's caused by the "home" icon click on the action bar.
     */
    public boolean onBackPressed(boolean isSystemBackKey) {
        if (shouldShowSearchBar()) {
            exitSearchMode();
            return true;
        }
        return false;
    }

    /** Refreshes the action bar display. */
    public void refresh() {
        final boolean showUp = isInSearchMode() || mCallback.shouldShowUp();
        mActionBar.setDisplayOptions(showUp
                ? ActionBar.DISPLAY_HOME_AS_UP : 0, ActionBar.DISPLAY_HOME_AS_UP);
    }

    private final SearchView.OnQueryTextListener mOnQueryText
    = new SearchView.OnQueryTextListener() {
    	@Override
    	public boolean onQueryTextChange(String newText) {
    		// Event not handled.  Let the search do the default action.
    		return false;
    	}

    	@Override
    	public boolean onQueryTextSubmit(String query) {
    		mCallback.onSearchSubmit(mSearchView.getQuery().toString());
    		return true; // Event handled.
    	}
    };
    
}
