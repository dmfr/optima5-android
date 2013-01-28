package za.dams.paracrm.explorer;

import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

public class SettingsAccountFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<BibleHelper.BibleEntry>> {
		
	private static final String TAG = "Explorer/SettingsAccountFragment";
	
    // ******** Fields for PARACRM ********
    private static final String BUNDLE_KEY_ACCT_IS_ON = "acctIsOn";
    private static final String BUNDLE_KEY_ACCT_SRCBIBLECODE = "acctSrcBibleCode";
    private boolean mAccountIsOn ;
    private String mAccountBibleCode ;
    
	public class AdapterRow {
		boolean isNull ;
		String entryKey;
		String displayName;
		
		boolean isChecked ;
	}
    
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(BUNDLE_KEY_ACCT_IS_ON) ) {
        	mAccountIsOn = bundle.getBoolean(BUNDLE_KEY_ACCT_IS_ON);
        	
        	if( mAccountIsOn && bundle.containsKey(BUNDLE_KEY_ACCT_SRCBIBLECODE) ) {
        		mAccountBibleCode = bundle.getString(BUNDLE_KEY_ACCT_SRCBIBLECODE);
        	}
        }
    }
    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.explorer_settings_account, null);
        
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText("Not applicable");
        
        if( !mAccountIsOn || mAccountBibleCode == null ) {
        	// sans Adapter
        	setListShown(true) ;
        	return ;
        }
        
        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public void onPause() {
        final ListAdapter listAdapter = getListAdapter();
        if (listAdapter != null) {
        	String selectedEntryKey = ((SettingsAccountAdapter)listAdapter).getSelectedEntryKey() ;
            if( selectedEntryKey != null ) {
            	PrefsCrm.ExplorerAccount ea = new PrefsCrm.ExplorerAccount() ;
            	ea.bibleCode = mAccountBibleCode ;
            	ea.entryKey = selectedEntryKey ;
            	PrefsCrm.setExplorerAccount(getActivity(), ea) ;
            } else {
            	PrefsCrm.unsetExplorerAccount(getActivity());
            }
        }
        super.onPause();
    }
    
	
    @Override
    public Loader<List<BibleHelper.BibleEntry>> onCreateLoader(int id, Bundle args) {
        return new BibleLoader( getActivity(), new BibleHelper.BibleCode(mAccountBibleCode) );
    }
    @Override
    public void onLoadFinished(Loader<List<BibleHelper.BibleEntry>> loader, List<BibleHelper.BibleEntry> data) {
    	SettingsAccountAdapter adapter = (SettingsAccountAdapter) getListAdapter();
        
    	String presetEntryKey = null ;
    	if( PrefsCrm.getExplorerAccount(getActivity()) != null ) {
    		PrefsCrm.ExplorerAccount ea = PrefsCrm.getExplorerAccount(getActivity()) ;
    		if( ea.bibleCode.equals(mAccountBibleCode) ) {
    			presetEntryKey = ea.entryKey ;
    		}
    	}
        
        if (adapter == null) {
            adapter = new SettingsAccountAdapter(getActivity(), data, presetEntryKey);
        } else {
            adapter.changeData(data);
        }
        setListAdapter(adapter);
        getListView().setOnItemClickListener(adapter);
    }

    public void onLoaderReset(Loader<List<BibleHelper.BibleEntry>> loader) {
        setListAdapter(null);
    }
	
	
	
    private class SettingsAccountAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    	
    	private static final String TAG = "Explorer/AccountSubscribeAdapter";

    	private LayoutInflater mInflater;
    	private static final int LAYOUT = R.layout.explorer_settings_account_item;
    	
    	private List<BibleHelper.BibleEntry> mData;
    	private String mSelectedEntryKey ;


    	public SettingsAccountAdapter(Context context, List<BibleHelper.BibleEntry> data, String presetEntryKey) {
    		super();
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		initData(data,presetEntryKey) ;
    	}
    	
    	private void initData( List<BibleHelper.BibleEntry> data, String presetEntryKey ) {
    		mData = data ;
    		if( presetEntryKey == null ) {
    			mSelectedEntryKey = null ;
    			return ;
    		}
    		
    		boolean found = false ;
    		for( BibleHelper.BibleEntry be : data ) {
    			if( presetEntryKey != null && be.entryKey.equals(presetEntryKey) ) {
    				found = true ;
    			}
    		}
    		if( found ) {
    			mSelectedEntryKey = presetEntryKey ;
    		}
    	}
    	
    	public void changeData( List<BibleHelper.BibleEntry> data ) {
    		initData(data,mSelectedEntryKey) ;
    	}

    	@Override
    	public int getCount() {
    		if( mSelectedEntryKey != null ) {
    			return 2 ;
    		} else {
    			return mData.size() + 1 ;
    		}
    	}

    	public AdapterRow fromBibleEntry( BibleHelper.BibleEntry be, boolean isChecked ) {
			AdapterRow ar = new AdapterRow() ;
			ar.entryKey = be.entryKey ;
			ar.displayName = be.displayStr ;
			ar.isNull = false ;
			ar.isChecked = isChecked ;
			return ar ;
    	}
    	@Override
    	public AdapterRow getItem(int position) {
    		if (position >= getCount()) {
    			return null;
    		}
    		if( position == 0 ) {
    			AdapterRow ar = new AdapterRow() ;
    			ar.displayName = "No account constraint" ;
    			ar.isNull = true ;
    			if( mSelectedEntryKey == null ) {
    				ar.isChecked = true ;
    			}
    			return ar ;
    		}
    		if( mSelectedEntryKey != null ) {
    			for( BibleHelper.BibleEntry be : mData ) {
    				if( be.entryKey.equals(mSelectedEntryKey) ) {
    					return fromBibleEntry(be,true) ;
    				}
    			}
    			AdapterRow ar = new AdapterRow() ;
    			ar.displayName = "Error !" ;
    			ar.isNull = true ;
    			return ar ;
    		}
    		return fromBibleEntry(mData.get(position-1),false);
    	}

    	@Override
    	public long getItemId(int position) {
    		if (position >= getCount()) {
    			return 0;
    		}
    		return position ;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (position >= getCount()) {
    			return null;
    		}

    		AdapterRow ar = getItem(position) ;
    		
    		View view;
    		if (convertView == null) {
    			view = mInflater.inflate(LAYOUT, parent, false);
    		} else {
    			view = convertView;
    		}

    		view.setTag(ar);

    		CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
    		cb.setChecked(ar.isChecked);

    		if( !ar.isNull ) {
    			((TextView)view.findViewById(R.id.text)).setTypeface(null, Typeface.NORMAL) ;
    			if (ar.isChecked) {
    				setText(view, R.id.status, "Account set for "+ar.entryKey);
    			} else {
    				setText(view, R.id.status, "");
    			}
    		} else {
    			((TextView)view.findViewById(R.id.text)).setTypeface(null, Typeface.ITALIC) ;
    			setText(view, R.id.status, "");
    		}

    		View colorView = view.findViewById(R.id.color);
    		colorView.setVisibility(View.INVISIBLE) ;


    		setText(view, R.id.text, ar.displayName);
    		return view;
    	}
    	private void setText(View view, int id, String text) {
    		TextView textView = (TextView) view.findViewById(id);
    		textView.setText(text);
    	}

    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
    		CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
    		cb.setChecked(true);
    		
    		AdapterRow clickedAr = getItem(position) ;
    		if( clickedAr.isNull ) {
    			mSelectedEntryKey = null ;
    		} else {
    			mSelectedEntryKey = clickedAr.entryKey ;
    		}
    		
    		notifyDataSetChanged() ;
    	}
    	
    	
    	public String getSelectedEntryKey() {
    		return mSelectedEntryKey ;
    	}
    }
	
	
	private static class BibleLoader extends AsyncTaskLoader<List<BibleHelper.BibleEntry>> {

		BibleHelper.BibleCode mBc ;
	    List<BibleHelper.BibleEntry> mData;
	    

	    public BibleLoader(Context context, BibleHelper.BibleCode bc ) {
	        super(context);
	    	mBc = bc ;
	    }

	    /**
	     * This is where the bulk of our work is done.  This function is
	     * called in a background thread and should generate a new set of
	     * data to be published by the loader.
	     */
	    @Override public List<BibleHelper.BibleEntry> loadInBackground() {
	    	
	    	BibleHelper bh = new BibleHelper(getContext()) ;
	    	List<BibleHelper.BibleEntry> entries = bh.queryBible(mBc.bibleCode) ;

	        return entries;
	    }

	    /**
	     * Called when there is new data to deliver to the client.  The
	     * super class will take care of delivering it; the implementation
	     * here just adds a little more logic.
	     */
	    @Override public void deliverResult(List<BibleHelper.BibleEntry> data) {
	        if (isReset()) {
	            // An async query came in while the loader is stopped.  We
	            // don't need the result.
	            if (data != null) {
	                onReleaseResources(data);
	            }
	        }
	        List<BibleHelper.BibleEntry> oldApps = data;
	        mData = data;

	        if (isStarted()) {
	            // If the Loader is currently started, we can immediately
	            // deliver its results.
	            super.deliverResult(data);
	        }

	        // At this point we can release the resources associated with
	        // 'oldApps' if needed; now that the new result is delivered we
	        // know that it is no longer in use.
	        if (oldApps != null) {
	            onReleaseResources(oldApps);
	        }
	    }

	    /**
	     * Handles a request to start the Loader.
	     */
	    @Override protected void onStartLoading() {
	        if (mData != null) {
	            // If we currently have a result available, deliver it
	            // immediately.
	            deliverResult(mData);
	        }

	        // Has something interesting in the configuration changed since we
	        // last built the app list?
	        boolean configChange = false ;

	        if (takeContentChanged() || mData == null || configChange) {
	            // If the data has changed since the last time it was loaded
	            // or is not currently available, start a load.
	            forceLoad();
	        }
	    }

	    /**
	     * Handles a request to stop the Loader.
	     */
	    @Override protected void onStopLoading() {
	        // Attempt to cancel the current load task if possible.
	        cancelLoad();
	    }

	    /**
	     * Handles a request to cancel a load.
	     */
	    @Override public void onCanceled(List<BibleHelper.BibleEntry> data) {
	        super.onCanceled(data);

	        // At this point we can release the resources associated with 'apps'
	        // if needed.
	        onReleaseResources(data);
	    }

	    /**
	     * Handles a request to completely reset the Loader.
	     */
	    @Override protected void onReset() {
	        super.onReset();

	        // Ensure the loader is stopped
	        onStopLoading();

	        // At this point we can release the resources associated with 'apps'
	        // if needed.
	        if (mData != null) {
	            onReleaseResources(mData);
	            mData = null;
	        }
	    }

	    /**
	     * Helper function to take care of releasing resources associated
	     * with an actively loaded data set.
	     */
	    protected void onReleaseResources(List<BibleHelper.BibleEntry> apps) {
	        // For a simple List<> there is nothing to do.  For something
	        // like a Cursor, we would close it here.
	    }
	}

}
