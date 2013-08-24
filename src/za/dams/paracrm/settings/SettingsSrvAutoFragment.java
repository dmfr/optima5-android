package za.dams.paracrm.settings;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.MainPreferences;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.Explorer;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

public class SettingsSrvAutoFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<SettingsSrvAutoFragment.SrvProfile>> {
		
	private static final String TAG = "Settings/SettingsSrvManualFragment";
	private static final int LOADER_ID = 1 ;
	
	private Activity mContext;
	private SettingsCallbacks mCallback ;
    
	public class SrvProfile {
		String profileCode ; // UNUSED
		String profileName ;
		
		String srvUrl ;
		String srvDomain ;
		String srvSdomain ;
	}
    
	
	public SettingsSrvAutoFragment() {
		setHasOptionsMenu(true);
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        mContext = activity;
        if (activity instanceof SettingsCallbacks) {
        	mCallback = (SettingsCallbacks)activity;
        } else {
        	Log.e(TAG,activity.toString()+" must implement SettingsCallbacks");
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

        setEmptyText("No profiles available");
        
        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.settings_srvauto_titlebar, menu);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(R.id.refresh).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }
    private boolean onActionBarItemSelected(int itemId) {
        switch (itemId) {
        case R.id.refresh:
        	forceReload();
         	break;
        }
        return true;
    }
    
	
    @Override
    public Loader<List<SrvProfile>> onCreateLoader(int id, Bundle args) {
        return new SrvProfilesLoader( getActivity() );
    }
    @Override
    public void onLoadFinished(Loader<List<SrvProfile>> loader, List<SrvProfile> data) {
    	SrvProfilesAdapter adapter = (SrvProfilesAdapter) getListAdapter();
        
    	// TODO (maybe ?) mark currently defined profile
        
        if (adapter == null) {
            adapter = new SrvProfilesAdapter(getActivity(), data, null);
        } else {
            adapter.changeData(data);
        }
        setListAdapter(adapter);
        setListShown(true) ;
        getListView().setOnItemClickListener(adapter);
    }

    public void onLoaderReset(Loader<List<SrvProfile>> loader) {
        setListAdapter(null);
    }
	
	
	
    private class SrvProfilesAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    	
    	private static final String TAG = "Settings/AccountSubscribeAdapter";

    	private LayoutInflater mInflater;
    	private static final int LAYOUT = R.layout.settings_row_item;
    	
    	private List<SrvProfile> mData;
    	private String mSelectedEntryKey ;


    	public SrvProfilesAdapter(Context context, List<SrvProfile> data, String presetEntryKey) {
    		super();
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		initData(data,presetEntryKey) ;
    	}
    	
    	private void initData( List<SrvProfile> data, String presetEntryKey ) {
    		mData = data ;
    		if( presetEntryKey == null ) {
    			mSelectedEntryKey = null ;
    			return ;
    		}
    		
    		boolean found = false ;
    		for( SrvProfile be : data ) {
    			if( presetEntryKey != null && be.profileCode.equals(presetEntryKey) ) {
    				found = true ;
    			}
    		}
    		if( found ) {
    			mSelectedEntryKey = presetEntryKey ;
    		}
    	}
    	
    	public void changeData( List<SrvProfile> data ) {
    		initData(data,mSelectedEntryKey) ;
    	}

    	@Override
    	public int getCount() {
   			return mData.size() ;
    	}

    	@Override
    	public SrvProfile getItem(int position) {
    		if (position >= getCount()) {
    			return null;
    		}
    		
    		return mData.get(position) ;
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

    		SrvProfile srvProfile = getItem(position) ;
    		
    		View view;
    		if (convertView == null) {
    			view = mInflater.inflate(LAYOUT, parent, false);
    		} else {
    			view = convertView;
    		}

    		view.setTag(srvProfile);

    		setText(view, R.id.item_title, srvProfile.profileName);
    		setText(view, R.id.item_caption, MainPreferences.buildServerFullUrl(srvProfile.srvUrl, srvProfile.srvDomain, srvProfile.srvSdomain));
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
    		
    		SrvProfile clickedProfile = getItem(position) ;
    		
    		SettingsSrvAutoFragment.this.onProfileSelected(clickedProfile) ;
    	}
    	
    	
    	public String getSelectedEntryKey() {
    		return mSelectedEntryKey ;
    	}
    }
	
	
	private static class SrvProfilesLoader extends AsyncTaskLoader<List<SrvProfile>> {

	    List<SrvProfile> mData;
	    

	    public SrvProfilesLoader(Context context) {
	        super(context);
	    }

	    /**
	     * This is where the bulk of our work is done.  This function is
	     * called in a background thread and should generate a new set of
	     * data to be published by the loader.
	     */
	    @Override public List<SrvProfile> loadInBackground() {
	    	ArrayList<SrvProfile> entries = new ArrayList<SrvProfile>();
	    	
	    	try {
	    		Thread.sleep(2000) ;
	    	} catch( Exception e ) {
	    		
	    	}


	        return entries;
	    }

	    /**
	     * Called when there is new data to deliver to the client.  The
	     * super class will take care of delivering it; the implementation
	     * here just adds a little more logic.
	     */
	    @Override public void deliverResult(List<SrvProfile> data) {
	        if (isReset()) {
	            // An async query came in while the loader is stopped.  We
	            // don't need the result.
	            if (data != null) {
	                onReleaseResources(data);
	            }
	        }
	        List<SrvProfile> oldApps = data;
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
	    @Override public void onCanceled(List<SrvProfile> data) {
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
	    protected void onReleaseResources(List<SrvProfile> apps) {
	        // For a simple List<> there is nothing to do.  For something
	        // like a Cursor, we would close it here.
	    }
	}

	
    public void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(LOADER_ID)!=null ) {
    		this.setListShown(false) ;
    		lm.getLoader(LOADER_ID).forceLoad() ;
    	}
    }

	public void onProfileSelected(SrvProfile clickedProfile) {
		// TODO Auto-generated method stub
		
	}

}
