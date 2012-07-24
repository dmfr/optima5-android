package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;

import android.app.ListFragment;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.LoaderManager ;

public class AccountSubscribeFragment extends ListFragment 
	implements LoaderManager.LoaderCallbacks<List<BibleHelper.BibleEntry>> {
	
    // ******** Fields for PARACRM ********
	private static final String BUNDLE_KEY_BIBLECODE = "bibleCode";
    private String mStrBibleCode ;
	
    public AccountSubscribeFragment() {
    }

    public AccountSubscribeFragment(Bundle bundle) {
    	mStrBibleCode = bundle.getString(BUNDLE_KEY_BIBLECODE);
    }
    
    
	
    @Override
    public Loader<List<BibleHelper.BibleEntry>> onCreateLoader(int id, Bundle args) {
        return new BibleLoader( getActivity(), new BibleHelper.BibleCode(mStrBibleCode) );
    }
    @Override
    public void onLoadFinished(Loader<List<BibleHelper.BibleEntry>> loader, List<BibleHelper.BibleEntry> data) {
    	/*
        SelectCalendarsSyncAdapter adapter = (SelectCalendarsSyncAdapter) getListAdapter();
        if (adapter == null) {
            adapter = new SelectCalendarsSyncAdapter(getActivity(), data);
        } else {
            adapter.changeCursor(data);
        }
        setListAdapter(adapter);
        getListView().setOnItemClickListener(adapter);
        */
    }

    public void onLoaderReset(Loader<List<BibleHelper.BibleEntry>> loader) {
        setListAdapter(null);
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

	    	List<BibleHelper.BibleEntry> entries = new ArrayList<BibleHelper.BibleEntry>();
	        
	        
	        
	        

	        // Done!
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
