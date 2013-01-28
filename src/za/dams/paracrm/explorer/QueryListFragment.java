package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class QueryListFragment extends ListFragment {
	private static final String LOGTAG = "QueryListFragment";

	/** Bundle keys */
    private static final String BUNDLE_LIST_STATE = "queryListFragment.state.listState";
    private static final String BUNDLE_KEY_SELECTED_QUERYSRC_ID
            = "queryListFragment.state.listState.selected_querysrc_id";
    
    // UI Support
    private Activity mActivity;
    private Callback mCallback = EmptyCallback.INSTANCE;
    private boolean mIsViewCreated;

    private View mListPanel;
    
    private QueryListFragmentAdapter mListAdapter;
    private boolean mIsFirstLoad;

    /** ID of the query to hightlight. */
    private int mSelectedQuerysrcId = -1;
    
    /** true between {@link #onResume} and {@link #onPause}. */
    private boolean mResumed;
    
    private Parcelable mSavedListState;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    	public BibleHelper.BibleEntry getExplorerConstraint() ;
    	public void onQuerySelect( int querysrcId ) ;
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    	public BibleHelper.BibleEntry getExplorerConstraint() {return null;} ;
    	public void onQuerySelect( int querysrcId ) {} ;
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
    public static QueryListFragment newInstance() {
        final QueryListFragment instance = new QueryListFragment();
        return instance;
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

        mListAdapter = new QueryListFragmentAdapter(mActivity);
        mIsFirstLoad = true;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onCreateView");
        }
        // Use a custom layout, which includes the original layout with "send messages" panel.
        View root = inflater.inflate(R.layout.explorer_querylist_fragment,null);
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

        setEmptyText("No published queries");

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        }

        startLoading();

        UiUtilities.installFragment(this);
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
            outState.putParcelable(BUNDLE_LIST_STATE, getListView().onSaveInstanceState());
        }
        outState.putInt(BUNDLE_KEY_SELECTED_QUERYSRC_ID, mSelectedQuerysrcId);
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " restoreInstanceState");
        }
        // mListAdapter.loadState(savedInstanceState);
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
        mSelectedQuerysrcId = savedInstanceState.getInt(BUNDLE_KEY_SELECTED_QUERYSRC_ID);
    }
    
    
    
    public void startLoading() {
    	final LoaderManager lm = getLoaderManager();
    	lm.initLoader(Explorer.QUERYLIST_LOADER_ID, null, new QueryListLoaderCallbacks());
    }
    public void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.QUERYLIST_LOADER_ID)!=null ) {
    		lm.getLoader(Explorer.QUERYLIST_LOADER_ID).forceLoad() ;
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
       	int clickedQuerysrcId = mListAdapter.getItem(position).querysrcId ;
        mCallback.onQuerySelect(clickedQuerysrcId) ;
    }
    
    
    
    
    /**
     * Highlight the selected message.
     */
    public void setSelectedQuerysrc(int querysrcId) {
        if (mSelectedQuerysrcId == querysrcId) {
            return;
        }
        mSelectedQuerysrcId = querysrcId;
        if (mResumed) {
        	highlightSelectedQuerysrc(true);
        }
    }
    private void highlightSelectedQuerysrc(boolean ensureSelectionVisible) {
        if (!isViewCreated()) {
            return;
        }

        final ListView lv = getListView();
        if (mSelectedQuerysrcId == -1) {
            // No message selected
            lv.clearChoices();
            return;
        }

        final int count = lv.getCount();
        for (int i = 0; i < count; i++) {
            if ( ((CrmQueryModel)lv.getItemAtPosition(i)).querysrcId != mSelectedQuerysrcId) {
                continue;
            }
            lv.setItemChecked(i, true);
            if (ensureSelectionVisible) {
                UiUtilities.listViewSmoothScrollToPosition(getActivity(), lv, i);
            }
            break;
        }
    }
    
    
    
    
	private class QueryListLoaderCallbacks implements LoaderCallbacks<QueryListFragmentLoaderResult> {
		
		@Override
		public Loader<QueryListFragmentLoaderResult> onCreateLoader(int arg0, Bundle arg1) {
			// TODO Auto-generated method stub
			mIsFirstLoad = true;
			return new QueryListFragmentLoader(getActivity());
		}

		@Override
		public void onLoadFinished(Loader<QueryListFragmentLoaderResult> loader, QueryListFragmentLoaderResult data) {
			
            // On peut faire tout ceci ici BECAUSE The loader manager doesn't deliver results when a fragment is stopped.
            //Parcelable listState = lv.onSaveInstanceState();
			mListAdapter.setData(data.records) ;
			
			
			highlightSelectedQuerysrc(mIsFirstLoad) ;
			
			
            if (mIsFirstLoad) {
                mListPanel.setVisibility(View.VISIBLE);

                // Setting the adapter will automatically transition from "Loading" to showing
                // the list, which could show "No messages". Avoid showing that on the first sync,
                // if we know we're still potentially loading more.
                if ( true ) {
                	setListAdapter(mListAdapter);
                }
            } else if ((getListAdapter() == null) ) {
                setListAdapter(mListAdapter);
            }
            
            // This is the query list, open the first query.
            if ( mIsFirstLoad && data.records.size() > 0 ) {
                
            	mCallback.onQuerySelect(data.records.get(0).querysrcId);
            }
			
            if (mSavedListState != null) {
            	getListView().onRestoreInstanceState(mSavedListState);
            }
			
            mIsFirstLoad = false;
		}
		
        private boolean isEmptyAndLoading(QueryListFragmentLoaderResult data) {
            return (data.records.size() == 0) ;
        }

		@Override
		public void onLoaderReset(Loader<QueryListFragmentLoaderResult> loader) {
			QueryListFragment.this.mListAdapter.setData(null) ;
		}
		
	}
	private class QueryListFragmentAdapter extends BaseAdapter {
		
		private ExplorerLayout mLayout ;
		private boolean isLoaded = false ;
		
		private ArrayList<CrmQueryModel> mData ;
		
		LayoutInflater mInflater ;

		public QueryListFragmentAdapter(Context context) {
			super();
			mData = new ArrayList<CrmQueryModel>() ;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	    public void setData( List<CrmQueryModel> data) {
	    	mData.clear() ;
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
			View view ;
			
			if( convertView != null ) {
				view = convertView ;
			} else {
				view = mInflater.inflate(R.layout.explorer_querylist_item, parent, false) ;
			}
			
			/*
			view.buildCrmFields(mCfd) ;
			
			view.setCrmValues(getItem(position)) ;
			*/
			ImageView icon = (ImageView)view.findViewById(R.id.icon) ;
			TextView text = (TextView)view.findViewById(R.id.text) ;
			
			if( isWideLayout() ) {
				icon.setVisibility(View.GONE) ;
				text.setText("Shouldn't happen !!!") ;
			}
			
			
			switch( getItem(position).querysrcType ) {
			case QWEB :
				icon.setImageResource(R.drawable.ic_menu_add_event_holo_light) ;
				break ;
			case QMERGE :
			case QUERY :
				icon.setImageResource(R.drawable.ic_explorer_fileicon) ;
				break ;
			}
			text.setText(getItem(position).querysrcName) ;
			

			return view;
		}
		@Override
		public int getCount() {
			return mData.size() ;
		}
		@Override
		public CrmQueryModel getItem(int position) {
			return mData.get(position);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
	}
	
	private static class QueryListFragmentLoaderResult {
		List<CrmQueryModel> records ;
	}
	
	private static class QueryListFragmentLoader extends AsyncTaskLoader<QueryListFragmentLoaderResult> {

		Context mContext ;
		
		QueryListFragmentLoaderResult mData ;

		public QueryListFragmentLoader(Context context) {
			super(context);
			mContext = context ;
		}

		@Override public void deliverResult(QueryListFragmentLoaderResult data) {
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
		@Override public QueryListFragmentLoaderResult loadInBackground() {
			//Log.w(LOGTAG,"Loading !!!") ;
			QueryListFragmentLoaderResult data = new QueryListFragmentLoaderResult() ;
			data.records = CrmQueryManager.model_getAll(mContext) ;
			return data ;
		}
		
		
	}

}
