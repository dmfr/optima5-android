package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;


public class ActionBarController implements AdapterView.OnItemClickListener {
	private static final String LOGTAG = "ActionBarController" ;
	
	private static final String BUNDLE_KEY_MODE = "ActionBarController.BUNDLE_KEY_MODE";
	
    private static final int MODE_NORMAL = 0;
    private static final int MODE_SEARCH = 1;
    
    private final Context mContext;
    //private final LoaderManager mLoaderManager;
    private final ActionBar mActionBar;
    
    private final ViewGroup mActionBarCustomView;
    
    private final View mTitleContainer ;
    private final TextView mTitleLine1View;
    private final TextView mTitleLine2View;
    
    private View mSearchContainer;
    private AutoCompleteTextView mSearchText ;
    
    private boolean mSearchIsReady ;
    private BibleHelper mSearchBibleHelper ;
    private List<String> mSearchedBibles ;
    private ArrayList<BibleHelper.BibleEntry> mSearchForeignConditions ;
    private ArrayAdapter<BibleHelper.BibleEntry> mSearchAdapter ;
    
    
    /** Either {@link #MODE_NORMAL} or {@link #MODE_SEARCH}. */
    private int mSearchMode = MODE_NORMAL;
    
    public final Callback mCallback;
    
    public interface Callback {
    	public ExplorerContext getCurrentExplorerContext();
    	// CrmFilter : when a search is validated
    	public boolean isInFilterMode();
    	public BibleHelper.BibleEntry getExplorerConstraint() ;
    	public BibleHelper.BibleEntry getFilteredBibleEntry();
    	
        /** @return the "UP" arrow should be shown. */
        public boolean shouldShowUp();
        

        /**
         * Called when the action bar initially shows the search entry field.
         */
        public void onSearchStarted();

        /**
         * Called when a search is submitted.
         *
         * @param queryTerm query string
         */
        public void onSearchSubmit(BibleHelper.BibleEntry be);

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
        
        mTitleContainer = UiUtilities.getView(mActionBarCustomView, R.id.actionbar_title);
        mTitleLine1View = UiUtilities.getView(mActionBarCustomView, R.id.actionbar_title_1);
        mTitleLine2View = UiUtilities.getView(mActionBarCustomView, R.id.actionbar_title_2);
        
        mSearchContainer = UiUtilities.getView(mActionBarCustomView, R.id.actionbar_searchmode);
        mSearchText = UiUtilities.getView(mActionBarCustomView, R.id.actionbar_searchmode_inputtext);
        
        /*
        mSearchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                } else {
                	mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                }
            }
        });
        */
    }

    private void initSearch() {
    	if( mCallback != null && mCallback.getCurrentExplorerContext() != null ) {
    		new InitSearchTask().execute(mCallback.getCurrentExplorerContext()) ;
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
            enterSearchMode();
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
    public void enterSearchMode() {
        initSearch();
        if (isInSearchMode()) {
            return;
        }

        mSearchText.setText("") ;
        mSearchMode = MODE_SEARCH;
        refresh();
        
        // Focus to the input text ?
        mSearchText.requestFocus() ;
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mSearchText, InputMethodManager.SHOW_IMPLICIT) ;
        
        mCallback.onSearchStarted();
    }
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long doNotUse) {
        if (!isInSearchMode()) {
            return;
        }
		if( parent.getAdapter() == mSearchAdapter ) {
			mCallback.onSearchSubmit(mSearchAdapter.getItem(position)) ;
			exitSearchMode() ;
		}
	}
    public void exitSearchMode() {
        if (!isInSearchMode()) {
            return;
        }
        mSearchMode = MODE_NORMAL;

        refresh();
        
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), InputMethodManager.SHOW_IMPLICIT) ;
        
        
        mSearchText.setAdapter(null) ;
        mSearchAdapter = null ;
        mSearchText.setOnItemClickListener(null) ;
        
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
        
        // Update Title
        if( isInSearchMode() ) {
        	mTitleContainer.setVisibility(View.GONE);
        	mSearchContainer.setVisibility(View.VISIBLE);
        } else {
        	mSearchContainer.setVisibility(View.GONE);
        	mTitleContainer.setVisibility(View.VISIBLE);
        	
        	StringBuilder sb = new StringBuilder() ;
        	sb.append("Explorer") ;
        	switch( mCallback.getCurrentExplorerContext().mMode ) {
        	case ExplorerContext.MODE_QUERY :
        		sb.append(" : Queries") ;
        		break ;
        	case ExplorerContext.MODE_BIBLE :
            	CrmBibleManager cbm = CrmBibleManager.getInstance(mContext) ;
            	cbm.bibleInitDescriptors() ;
        		sb.append(" : ") ;
        		sb.append(cbm.bibleGetBibleDescriptor(mCallback.getCurrentExplorerContext().mBibleCode).bibleName) ;
        		break ;
        	case ExplorerContext.MODE_FILE :
            	CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
            	cfm.fileInitDescriptors() ;
        		sb.append(" : ") ;
        		sb.append(cfm.fileGetFileDescriptor(mCallback.getCurrentExplorerContext().mFileCode).fileName) ;
        		break ;
        	}
        	String ligne1 = sb.toString() ;
        	String ligne2 = null ;
        	if( mCallback.isInFilterMode() ) {
        		ligne2 = mCallback.getFilteredBibleEntry().displayStr ;
        	}
        	
        	if( ligne2 != null ) {
        		mTitleLine1View.setText(ligne2) ;
        		mTitleLine1View.setVisibility(View.VISIBLE) ;
        		mTitleLine2View.setText(ligne1) ;
        		mTitleLine2View.setVisibility(View.VISIBLE) ;
        		//mActionBar.setIcon(R.drawable.mainmenu_filemanager_red) ;
        	} else {
        		mTitleLine1View.setText(ligne1) ;
        		mTitleLine1View.setVisibility(View.VISIBLE) ;
        		if( mCallback.getExplorerConstraint() != null ) {
        			mTitleLine2View.setText(mCallback.getExplorerConstraint().displayStr) ;
        			mTitleLine2View.setVisibility(View.VISIBLE) ;
        		} else {
        			mTitleLine2View.setText("") ;
        			mTitleLine2View.setVisibility(View.GONE) ;
        		}
        		//mActionBar.setIcon(R.drawable.mainmenu_filemanager) ;
        	}
       }
    }

    
    private class InitSearchTask extends AsyncTask<ExplorerContext,Void,Boolean> {
    	CrmFileManager mCrmFileManager ;
    	
    	@Override
    	protected void onPreExecute() {
    		mCrmFileManager = CrmFileManager.getInstance(mContext.getApplicationContext()) ;
    		mSearchIsReady = false ;
    		mSearchedBibles = new ArrayList<String>() ;
    		mSearchForeignConditions = new ArrayList<BibleHelper.BibleEntry>() ;
    	}

		@Override
		protected Boolean doInBackground(ExplorerContext... explorerContexts) {
			if( explorerContexts.length != 1 ) {
				return false ;
			}
			
			mSearchBibleHelper = new BibleHelper(mContext) ;
			
			if( mCallback.getExplorerConstraint() != null ) {
				mSearchForeignConditions.add(mCallback.getExplorerConstraint()) ;
			}
			
			ExplorerContext explorerContext = explorerContexts[0] ;			
			switch( explorerContext.mMode ) {
			case ExplorerContext.MODE_FILE :
				mCrmFileManager.fileInitDescriptors() ;
				for( CrmFileManager.CrmFileFieldDesc cffd : mCrmFileManager.fileGetFileDescriptor(explorerContext.mFileCode).fieldsDesc ){
					if( cffd.fieldType == CrmFileManager.FieldType.FIELD_BIBLE ) {
						mSearchedBibles.add(cffd.fieldLinkBible) ;
					}
				}
				break ;
			default :
				return false ;
			}
			
			return true ;
		}
		@Override
    	protected void onPostExecute(Boolean goSearch) {
			mSearchIsReady = goSearch ;
			mSearchAdapter = new SearchAdapter(mContext,mSearchBibleHelper,mSearchedBibles,mSearchForeignConditions) ;
			mSearchText.setAdapter(mSearchAdapter) ;
			mSearchText.setOnItemClickListener(ActionBarController.this) ;
     	}
		
		public void execute(ExplorerContext explorerContext) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR , explorerContext) ;
		}
    }
    
    private static class SearchAdapter extends ArrayAdapter<BibleHelper.BibleEntry> {
    	LayoutInflater mInflater ;
    	
    	BibleHelper mBibleHelper ;
    	List<String> mSearchedBibles ;
    	ArrayList<BibleHelper.BibleEntry> mSearchForeignConditions ;
    	
		public SearchAdapter(Context context, BibleHelper bibleHelper, List<String> searchedBibles, ArrayList<BibleHelper.BibleEntry> searchForeignConditions ) {
			super(context, R.layout.explorer_actionbar_search_dropdown);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mBibleHelper = bibleHelper ;
			mSearchedBibles = searchedBibles ;
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
	        			for( String bibleCode : mSearchedBibles ) {
	        				
	        				// on ne cherche pas dans la bible "bibleCode" si elle fait partie des conditions !
	        				boolean skipThisBible = false ;
	        				for( BibleHelper.BibleEntry be : mSearchForeignConditions ) {
	        					if( be.bibleCode.equals(bibleCode) ) {
	        						skipThisBible = true ;
	        					}
	        				}
	        				if( skipThisBible ) {
	        					continue ;
	        				}
	        				
	        				data.addAll( mBibleHelper.queryBible(bibleCode, mSearchForeignConditions, typedText, 10) ) ;
	        			}
	        			
	                    // Now assign the values and count to the FilterResults object
	                    filterResults.values = data;
	                    filterResults.count = data.size();
	                }
	                return filterResults;
	            }

	            @SuppressWarnings("unchecked")
				@Override
	            protected void publishResults(CharSequence contraint, FilterResults results) {
	            	
        			SearchAdapter.this.clear() ;
        			if( results != null && results.values != null ) {
        				SearchAdapter.this.addAll((ArrayList<BibleHelper.BibleEntry>)results.values) ;
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
				view = mInflater.inflate(R.layout.explorer_actionbar_search_dropdown, parent, false) ;
			}
			
			BibleHelper.BibleEntry be = getItem(position) ;
			
			((TextView)view.findViewById(R.id.text_1)).setText(be.displayStr1) ;
			((TextView)view.findViewById(R.id.text_2)).setText(be.displayStr2) ;

			return view;
		}
    }


    
}
