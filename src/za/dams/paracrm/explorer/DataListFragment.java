package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.R;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DataListFragment extends ListFragment implements OnItemClickListener {
	
    @SuppressWarnings("unused")
	private static final String LOGTAG = "DataListFragment";

    private static final String BUNDLE_KEY_HIGHLIGHTED_DATALISTENTRY = "DataListFragment.state.selected_datalist_entry"; 
    private static final String BUNDLE_LIST_STATE = "DataListFragment.state.listState"; 
    
    private static final String ARG_INITIAL_DATALISTENTRY = "initialDataListEntry";
    private static final String ARG_ENABLE_HIGHLIGHT = "enablehighlight";
    
    // UI Support
    private Activity mActivity;
    private DataListFragmentAdapter mListAdapter;
    private Callback mCallback = EmptyCallback.INSTANCE;
    private DataListEntry mHighlightedDataListEntry;
    private DataListEntry mNextHighlightedDataListEntry ; // when next load is successful

    private DataListEntry mImmutableInitialDataListEntry ;
    private boolean mImmutableEnableHighlight;
    
    private Parcelable mSavedListState;
    
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
        public void onBibleSelected(String bibleCode);
        public void onFileSelected(String fileCode);
        public void onQuerySelected();
    }

    private static class EmptyCallback implements Callback {
        public static final Callback INSTANCE = new EmptyCallback();
        @Override public void onBibleSelected(String bibleCode) { }
        @Override public void onFileSelected(String fileCode) { }
        @Override public void onQuerySelected() { }
    }
    
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
    
    public static DataListFragment newInstance(boolean enableHighlight, DataListEntry initialDle ) {
        final DataListFragment instance = new DataListFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_INITIAL_DATALISTENTRY, initialDle);
        args.putBoolean(ARG_ENABLE_HIGHLIGHT, enableHighlight);
        instance.setArguments(args);
        return instance;
    }
    private void initializeArgCache() {
    	mImmutableInitialDataListEntry = getArguments().getParcelable(ARG_INITIAL_DATALISTENTRY);
    	mImmutableEnableHighlight = getArguments().getBoolean(ARG_ENABLE_HIGHLIGHT);
    }
    public DataListEntry getInitialDataListEntry() {
        initializeArgCache();
        return mImmutableInitialDataListEntry;
    }
    public boolean getEnableHighlight() {
        initializeArgCache();
        return mImmutableEnableHighlight;
    }
 
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before {@link #onActivityCreated(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = getActivity();
        mListAdapter = new DataListFragmentAdapter(mActivity);
        setListAdapter(mListAdapter); // It's safe to do even before the list view is created.

        if (savedInstanceState == null) {
            setInitialHighlight();
        } else {
            restoreInstanceState(savedInstanceState);
        }
    }

    /**
     * Set {@link #mParentMailboxId} and {@link #mHighlightedMailboxId} from the fragment arguments.
     */
    private void setInitialHighlight() {
        final DataListEntry initialDataListEntry = getInitialDataListEntry();
        // Highlight the mailbox of interest
        if (getEnableHighlight()) {
            mHighlightedDataListEntry = initialDataListEntry;
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explorer_datalist_fragment, container, false);
    }

    /**
     * @return true if the content view is created and not destroyed yet. (i.e. between
     * {@link #onCreateView} and {@link #onDestroyView}.
     */
    private boolean isViewCreated() {
        return getView() != null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Note we can't do this in onCreateView.
        // getListView() is only usable after onCreateView().
        final ListView lv = getListView();
        lv.setOnItemClickListener(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        startLoading();

        UiUtilities.installFragment(this);
    }

    /**
     * Called when the Fragment is visible to the user.
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        mSavedListState = getListView().onSaveInstanceState();
        super.onPause();
    }

    /**
     * Called when the Fragment is no longer started.
     */
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        UiUtilities.uninstallFragment(this);
        super.onDestroyView();
    }

    /**
     * Called when the fragment is no longer in use.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_KEY_HIGHLIGHTED_DATALISTENTRY, mHighlightedDataListEntry);
        if (isViewCreated()) {
            outState.putParcelable(BUNDLE_LIST_STATE, getListView().onSaveInstanceState());
        }
    }

    private void restoreInstanceState(Bundle savedInstanceState) {
        mNextHighlightedDataListEntry = savedInstanceState.getParcelable(BUNDLE_KEY_HIGHLIGHTED_DATALISTENTRY);
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
    }

    /**
     * @return "Selected" mailbox ID.
     */
    public DataListEntry getSelectedDataListEntry() {
        return mHighlightedDataListEntry ;
    }
    
    
    public void startLoading() {
    	setListShown(false);
    	final LoaderManager lm = getLoaderManager();
    	lm.initLoader(Explorer.DATALIST_LOADER_ID, null, new DataListLoaderCallbacks());
    }
    public void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(Explorer.DATALIST_LOADER_ID)!=null ) {
    		lm.getLoader(Explorer.DATALIST_LOADER_ID).forceLoad() ;
    	}
    }
    
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long doNotUse) {
		final DataListEntry clickDle = mListAdapter.getItem(position) ;
		if( clickDle.isHeader ) {
			return ;
		}
		switch( clickDle.dataType ) {
		case DataListEntry.DATA_BIBLE :
			mCallback.onBibleSelected(clickDle.bibleCode) ;
			break ;
		case DataListEntry.DATA_FILE :
			mCallback.onFileSelected(clickDle.fileCode) ;
			break ;
		case DataListEntry.DATA_QUERY :
			mCallback.onQuerySelected() ;
			break ;
		default :
			break ;
		}
	}
	
    public void setHighlightedDataListEntry(DataListEntry dle) {
        if (!getEnableHighlight()) {
            return;
        }
        if (dle!=null && dle.equals(mHighlightedDataListEntry)) {
            return; // already highlighted.
        }
        if ( !mListAdapter.isDataLoaded() ) {
            // List not loaded yet.  Just remember the ID here and let onLoadFinished() update
            // mHighlightedMailboxId.
        	mNextHighlightedDataListEntry = dle;
            return;
        }
        mHighlightedDataListEntry = dle;
        updateHighlightedDataListEntry(true);
    }
	public boolean updateHighlightedDataListEntry( boolean ensureSelectionVisible ) {
        if (!getEnableHighlight() || !isViewCreated()) {
            return true; // Nothing to highlight
        }
        final ListView lv = getListView();
        boolean found = false;
        if (mHighlightedDataListEntry == null) {
            // No mailbox selected
            lv.clearChoices();
            found = true;
        } else {
            // TODO Don't mix list view & list adapter indices. This is a recipe for disaster.
            final int count = lv.getCount();
            for (int i = 0; i < count; i++) {
                if (!mListAdapter.getItem(i).equals(mHighlightedDataListEntry)) {
                    continue;
                }
                found = true;
                lv.setItemChecked(i, true);
                if (ensureSelectionVisible) {
                    UiUtilities.listViewSmoothScrollToPosition(getActivity(), lv, i);
                }
                break;
            }
        }
        if (!found) {
        	mHighlightedDataListEntry = null ;
        }
        return found;
	}
	
	public static class DataListEntry implements Parcelable {
		public static final int DATA_BIBLE = 1 ;
		public static final int DATA_FILE = 2 ;
		public static final int DATA_QUERY = 3 ;
		
		public int dataType ;
		public boolean isHeader ;
		public String bibleCode = "" ;
		public String bibleName = "" ;
		public String fileCode = "" ;
		public String fileName = "" ;
		
		public DataListEntry( Parcel in ) {
			dataType = in.readInt() ;
			isHeader = (in.readByte() == 1 ) ? true : false ;
			bibleCode = in.readString() ;
			bibleName = in.readString() ;
			fileCode = in.readString() ;
			fileName = in.readString() ;
		}
		
		public DataListEntry() {
		}

		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel out, int arg1) {
			out.writeInt(dataType) ;
			out.writeByte((byte)(isHeader ? 1 : 0)) ;
			out.writeString(bibleCode);
			out.writeString(bibleName);
			out.writeString(fileCode);
			out.writeString(fileName);
		}
		public static Parcelable.Creator<DataListEntry> CREATOR =
				new Parcelable.Creator<DataListEntry>() {
			@Override
			public DataListEntry createFromParcel(Parcel source) {
				return new DataListEntry(source);
			}

			@Override
			public DataListEntry[] newArray(int size) {
				return new DataListEntry[size];
			}
		};
		
		public boolean equals( Object o ) {
			if( o==null ) {
				return false ;
			}
			
			DataListEntry dle = (DataListEntry)o ;
			if( this.dataType != dle.dataType ) {
				return false ;
			}
			if( this.isHeader != dle.isHeader ) {
				return false ;
			}
			if( !this.bibleCode.equals(dle.bibleCode) ) {
				return false ;
			}
			if( !this.fileCode.equals(dle.fileCode) ) {
				return false ;
			}
			return true ;
		}
		public int hashCode() {
			int result = 17 ;
			
			result = 31 * result + dataType ;
			result = 31 * result + (isHeader ? 1 : 0) ;
			result = 31 * result + bibleCode.hashCode() ;
			result = 31 * result + fileCode.hashCode() ;
			
			return result ;
		}
	}
	
	private class DataListLoaderCallbacks implements LoaderCallbacks<List<DataListEntry>> {
		private boolean mIsFirstLoad;
		
		@Override
		public Loader<List<DataListEntry>> onCreateLoader(int arg0, Bundle arg1) {
			// TODO Auto-generated method stub
			mIsFirstLoad = true;
			return new DataListFragmentLoader(getActivity());
		}

		@Override
		public void onLoadFinished(Loader<List<DataListEntry>> loader, List<DataListEntry> data) {
            // Save list view state (primarily scroll position)
            final ListView lv = getListView();
            final Parcelable listState;
            if (mSavedListState != null) {
                listState = mSavedListState;
                mSavedListState = null;
            } else {
                listState = lv.onSaveInstanceState();
            }
            
            // on peut faire tout ceci ici BECAUSE The loader manager doesn't deliver results when a fragment is stopped.
			mListAdapter.setData(data) ;
			setListShown(true);
			
            lv.onRestoreInstanceState(listState);

            // Update the highlighted mailbox
            if (mNextHighlightedDataListEntry != null) {
                setHighlightedDataListEntry(mNextHighlightedDataListEntry);
                mNextHighlightedDataListEntry = null;
            }
			updateHighlightedDataListEntry(mIsFirstLoad) ;
			
			mIsFirstLoad = false;
		}

		@Override
		public void onLoaderReset(Loader<List<DataListEntry>> loader) {
			DataListFragment.this.mListAdapter.setData(null) ;
		}
		
	}
	private class DataListFragmentAdapter extends ArrayAdapter<DataListFragment.DataListEntry> {
		private static final int TYPE_HEADER = 0 ;
		private static final int TYPE_ITEM = 1 ;
		
		private boolean isLoaded = false ;
		
		LayoutInflater mInflater ;

		public DataListFragmentAdapter(Context context) {
			super(context, android.R.layout.simple_list_item_2);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	    public void setData(List<DataListFragment.DataListEntry> data) {
	        clear();
	        if (data != null) {
	            addAll(data);
	            isLoaded = true ;
	        } else {
	        	isLoaded = false ;
	        }
	    }
	    public boolean isDataLoaded() {
	    	return isLoaded ;
	    }
	    
	    @Override
	    public int getViewTypeCount() {
	    	return 2 ;
	    }
	    @Override
	    public int getItemViewType( int position ) {
	    	return (getItem(position).isHeader)? TYPE_HEADER : TYPE_ITEM ;
	    }
	    @Override
	    public boolean isEnabled(int position) {
	        return !(getItem(position).isHeader);
	    }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			DataListEntry dle = getItem(position) ;
			
			if (convertView == null) {
				if( dle.isHeader ) {
					view = mInflater.inflate(R.layout.explorer_datalist_header, parent, false);
				} else {
					view = mInflater.inflate(R.layout.explorer_datalist_item, parent, false);
				}
			} else {
				view = convertView;
			}
			
			if( dle.isHeader ) {
				final TextView nameView = (TextView) view.findViewById(R.id.display_name);
				if( dle.dataType == DataListEntry.DATA_BIBLE ) {
					nameView.setText("Bibles") ;
				}
				if( dle.dataType == DataListEntry.DATA_FILE ) {
					nameView.setText("Files") ;
				}
				if( dle.dataType == DataListEntry.DATA_QUERY ) {
					nameView.setText("Queries") ;
				}
			} else {
				final TextView nameView = (TextView) view.findViewById(R.id.mailbox_name);
				final ImageView folderIcon = (ImageView) view.findViewById(R.id.folder_icon);
		        final ImageView mailboxExpandedIcon =
		                (ImageView) view.findViewById(R.id.folder_expanded_icon);
		        
		        if( dle.dataType == DataListEntry.DATA_BIBLE ) {
		        	folderIcon.setImageResource(R.drawable.ic_explorer_file);
		        	nameView.setText(dle.bibleName) ;
		        }
		        if( dle.dataType == DataListEntry.DATA_FILE ) {
		        	folderIcon.setImageResource(R.drawable.ic_explorer_file);
		        	nameView.setText(dle.fileName) ;
		        }
		        if( dle.dataType == DataListEntry.DATA_QUERY ) {
		        	folderIcon.setImageResource(R.drawable.ic_explorer_query);
		        	nameView.setText("Query List") ;
	                mailboxExpandedIcon.setVisibility(View.VISIBLE);
	                mailboxExpandedIcon.setImageResource(R.drawable.ic_mailbox_collapsed_holo_light);
		        }
			}

			return view;
		}

		

	}
	private static class DataListFragmentLoader extends AsyncTaskLoader<List<DataListFragment.DataListEntry>> {

		Context mContext ;
		List<DataListFragment.DataListEntry> mData ;

		public DataListFragmentLoader(Context context) {
			super(context);
			mContext = context ;
		}

		@Override public void deliverResult(List<DataListEntry> data) {
			mData = data ;
			if( isStarted()) {
				super.deliverResult(data);
			}
		}
		@Override protected void onStartLoading() {
			if( takeContentChanged() || mData == null || true ) {
				forceLoad() ;
				return ;
			}
			//deliverResult(mData);
		}
		@Override protected void onStopLoading() {
	        // Attempt to cancel the current load task if possible.
	        cancelLoad();
	    }
		@Override protected void onReset() {
			mData = null ;
		}
		@Override public List<DataListEntry> loadInBackground() {
			//Log.w(LOGTAG,"Loading !!!") ;
			
			CrmBibleManager cbm = CrmBibleManager.getInstance(mContext) ;
			if( !cbm.isInitialized() ) {
				cbm.bibleInitDescriptors() ;
				try {
					Thread.sleep(250) ;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
			if( !cfm.isInitialized() ) {
				cfm.fileInitDescriptors() ;
				try {
					Thread.sleep(250) ;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			ArrayList<DataListEntry> data = new  ArrayList<DataListEntry>() ;
			if( cbm.bibleGetDescriptors().size() > 0 ) {
				DataListEntry dle = new DataListEntry() ;
				dle.isHeader = true ;
				dle.dataType = DataListEntry.DATA_BIBLE ;
				data.add(dle) ;
			}
			for( CrmBibleManager.CrmBibleDesc cfd : cbm.bibleGetDescriptors() ) {
				DataListEntry dle = new DataListEntry() ;
				dle.isHeader = false ;
				dle.dataType = DataListEntry.DATA_BIBLE ;
				dle.bibleCode = cfd.bibleCode ;
				dle.bibleName = cfd.bibleName ;
				data.add(dle) ;
			}
			if( cfm.fileGetRootDescriptors().size() > 0 ) {
				DataListEntry dle = new DataListEntry() ;
				dle.isHeader = true ;
				dle.dataType = DataListEntry.DATA_FILE ;
				data.add(dle) ;
			}
			for( CrmFileManager.CrmFileDesc cfd : cfm.fileGetRootDescriptors() ) {
				DataListEntry dle = new DataListEntry() ;
				dle.isHeader = false ;
				dle.dataType = DataListEntry.DATA_FILE ;
				dle.fileCode = cfd.fileCode ;
				dle.fileName = cfd.fileName ;
				data.add(dle) ;
			}
			if( true ) {
				DataListEntry dle ;
				
				dle = new DataListEntry() ;
				dle.isHeader = true ;
				dle.dataType = DataListEntry.DATA_QUERY ;
				data.add(dle) ;
				
				dle = new DataListEntry() ;
				dle.isHeader = false ;
				dle.dataType = DataListEntry.DATA_QUERY ;
				data.add(dle) ;
			}
			
			return data ;
		}
		
		
	}

}
