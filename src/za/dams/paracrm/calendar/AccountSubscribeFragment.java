package za.dams.paracrm.calendar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

public class AccountSubscribeFragment extends ListFragment 
	implements View.OnClickListener, LoaderManager.LoaderCallbacks<List<BibleHelper.BibleEntry>> {
	
	private static final String TAG = "Calendar/AccountSubscribeFragment";
	
    private TextView mSyncStatus;
    private Button mAccountsButton;
    
    // ******** Fields for PARACRM ********
    private static final String BUNDLE_KEY_CALFILECODE = "fileCode";
    private static final String BUNDLE_KEY_SRCBIBLECODE = "srcBibleCode";
    private String mCalendarFileCode ;
    private String mStrBibleCode ;
    
	public class CalendarRow {
		String id;
		String displayName;
		int color;
		boolean synced;
		boolean originalSynced;
	}
	
    public AccountSubscribeFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Bundle bundle = getArguments();
        if (bundle != null 
        		&& bundle.containsKey(BUNDLE_KEY_CALFILECODE)
        		&& bundle.containsKey(BUNDLE_KEY_SRCBIBLECODE) ) {
        	mCalendarFileCode = bundle.getString(BUNDLE_KEY_CALFILECODE);
        	mStrBibleCode = bundle.getString(BUNDLE_KEY_SRCBIBLECODE);
        	// Log.w(TAG,"Bible code is "+mStrBibleCode) ;
        }
    }
    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.calendar_accounts, null);
        
        mSyncStatus = (TextView) v.findViewById(R.id.account_status);
        mSyncStatus.setVisibility(View.GONE);

        mAccountsButton = (Button) v.findViewById(R.id.sync_settings);
        mAccountsButton.setVisibility(View.GONE);
        mAccountsButton.setOnClickListener(this);

        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText("Account bible empty");
        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public void onPause() {
    	//Log.w(TAG,"Save changes ?") ;
        final ListAdapter listAdapter = getListAdapter();
        if (listAdapter != null) {
        	//Log.w(TAG,"Save changes !") ;
        	// **** Save changes ****
        	
            HashMap<String, CalendarRow> changes = ((AccountSubscribeAdapter) listAdapter)
                    .getChanges();
            if (changes != null && changes.size() > 0) {
            	Map<String,Integer> presets = PrefsCrm.getAccountsColor(getActivity(), mCalendarFileCode) ;
            	
                for (CalendarRow row : changes.values()) {
                	presets.remove(row.id) ;
                    if( row.synced  ) {
                    	presets.put(row.id, row.color) ;
                    }
                }
                changes.clear();
                PrefsCrm.setAccountsColor(getActivity(), mCalendarFileCode, presets) ;
                
                
                Set<String> accountsSet = presets.keySet() ;
                PrefsCrm.setSubscribedAccounts(getActivity(), mCalendarFileCode, accountsSet) ;
            }
            
            
            
            
        }
        super.onPause();
    }
    
    
	
    @Override
    public Loader<List<BibleHelper.BibleEntry>> onCreateLoader(int id, Bundle args) {
    	//Log.w(TAG,"Initialize loader for bible code "+mStrBibleCode) ;
    	
        return new BibleLoader( getActivity(), new BibleHelper.BibleCode(mStrBibleCode) );
    }
    @Override
    public void onLoadFinished(Loader<List<BibleHelper.BibleEntry>> loader, List<BibleHelper.BibleEntry> data) {
        AccountSubscribeAdapter adapter = (AccountSubscribeAdapter) getListAdapter();
        
        Map<String,Integer> presets = PrefsCrm.getAccountsColor(getActivity(), mCalendarFileCode) ;
        
        if (adapter == null) {
            adapter = new AccountSubscribeAdapter(getActivity(), data, presets);
        } else {
            adapter.changeCursor(data, presets);
        }
        setListAdapter(adapter);
        getListView().setOnItemClickListener(adapter);
    }

    public void onLoaderReset(Loader<List<BibleHelper.BibleEntry>> loader) {
        setListAdapter(null);
    }
	
    // Called when the Accounts button is pressed. Takes the user to the
    // Accounts and Sync settings page.
    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.setAction("android.settings.SYNC_SETTINGS");
        getActivity().startActivity(intent);
    }
	
	
	
    private class AccountSubscribeAdapter extends BaseAdapter
    	implements ListAdapter, AdapterView.OnItemClickListener {
    	
    	private static final String TAG = "Calendar/AccountSubscribeAdapter";
    	private int COLOR_CHIP_SIZE = 30;
    	private RectShape r = new RectShape();

    	private LayoutInflater mInflater;
    	private static final int LAYOUT = R.layout.calendar_accounts_item;
    	private CalendarRow[] mData;
    	private HashMap<String, CalendarRow> mChanges = new HashMap<String, CalendarRow>();
    	private int mRowCount = 0;
    	private int mRowSyncedCount = 0 ;

    	private final String mSyncedString;
    	private final String mNotSyncedString;


    	public AccountSubscribeAdapter(Context context, List<BibleHelper.BibleEntry> data, Map<String,Integer> presets ) {
    		super();
    		initData(data,presets);
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		COLOR_CHIP_SIZE *= context.getResources().getDisplayMetrics().density;
    		r.resize(COLOR_CHIP_SIZE, COLOR_CHIP_SIZE);
    		Resources res = context.getResources();
    		mSyncedString = "Synced";
    		mNotSyncedString = "";
    	}

    	private void initData(List<BibleHelper.BibleEntry> data, Map<String,Integer> presets) {
    		if (data == null) {
    			mRowCount = 0;
    			mData = null;
    			return;
    		}

    		mRowCount = data.size();
    		mRowSyncedCount = 0 ;
    		mData = new CalendarRow[mRowCount];
    		int p = 0;
    		for (BibleHelper.BibleEntry be : data) {
    			mData[p] = new CalendarRow();
    			mData[p].id = be.entryKey ;
    			//mData[p].color = Color.BLUE ;
    			mData[p].displayName = be.displayStr ;
    			mData[p].originalSynced = presets.containsKey(be.entryKey) ;
    			if( mData[p].originalSynced ) {
    				mData[p].color = presets.get(be.entryKey).intValue() ;
    			}
    			if (mChanges.containsKey(be.entryKey)) {
    				mData[p].synced = mChanges.get(be.entryKey).synced;
    				mData[p].color = mChanges.get(be.entryKey).color;
    			} else {
    				mData[p].synced = mData[p].originalSynced;
    			}
    			if( mData[p].synced ) {
    				mRowSyncedCount++ ;
    			}
    			p++ ;
    		}
    	}

    	public void changeCursor(List<BibleHelper.BibleEntry> data, Map<String,Integer> presets) {
    		initData(data,presets);
    		notifyDataSetChanged();
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (position >= mRowCount) {
    			return null;
    		}
    		String name = mData[position].displayName;
    		boolean selected = mData[position].synced;
    		int color = Utils.getDisplayColorFromColor(mData[position].color);
    		View view;
    		if (convertView == null) {
    			view = mInflater.inflate(LAYOUT, parent, false);
    		} else {
    			view = convertView;
    		}

    		view.setTag(mData[position]);

    		CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
    		cb.setChecked(selected);

    		if (selected) {
    			setText(view, R.id.status, mSyncedString);
    		} else {
    			setText(view, R.id.status, mNotSyncedString);
    		}

    		View colorView = view.findViewById(R.id.color);

    		if( selected ) {
    			colorView.setVisibility(View.VISIBLE) ;
    			colorView.setBackgroundColor(color);
    		}
    		else {
    			colorView.setVisibility(View.INVISIBLE) ;
    		}

    		setText(view, R.id.calendar, name);
    		return view;
    	}

    	private void setText(View view, int id, String text) {
    		TextView textView = (TextView) view.findViewById(id);
    		textView.setText(text);
    	}

    	@Override
    	public int getCount() {
    		return mRowCount;
    	}

    	@Override
    	public Object getItem(int position) {
    		if (position >= mRowCount) {
    			return null;
    		}
    		CalendarRow item = mData[position];
    		return item;
    	}

    	@Override
    	public long getItemId(int position) {
    		if (position >= mRowCount) {
    			return 0;
    		}
    		return position ;
    	}

    	@Override
    	public boolean hasStableIds() {
    		return true;
    	}

    	public int getSynced(int position) {
    		return mData[position].synced ? 1 : 0;
    	}

    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
    		CalendarRow row = (CalendarRow) view.getTag();
    		row.synced = !row.synced;
    		row.color = getColor() ;
    		String status;
    		if (row.synced) {
    			status = mSyncedString;
    			mRowSyncedCount++ ;
    		} else {
    			status = mNotSyncedString;
    			mRowSyncedCount-- ;
    		}
    		setText(view, R.id.status, status);

    		CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
    		cb.setChecked(row.synced);

    		
    		View colorView = view.findViewById(R.id.color);
    		if( row.synced ) {
    			int color = Utils.getDisplayColorFromColor(mData[position].color);
    			colorView.setVisibility(View.VISIBLE) ;
    			colorView.setBackgroundColor(color);
    		}
    		else {
    			colorView.setVisibility(View.INVISIBLE) ;
    		}
    		// There is some data loss in long -> int, but we should never see it in
    		// practice regarding calendar ids.
    		mChanges.put(row.id, row);
    	}

    	public HashMap<String, CalendarRow> getChanges() {
    		return mChanges;
    	}
    	
    	private int getColor(){
    		switch( mRowSyncedCount % 5 ) {
    		case 0 :
    			return Color.BLUE ;
    		case 1 :
    			return Color.CYAN ;
    		case 2 :
    			return Color.MAGENTA ;
    		case 3 :
    			return Color.GREEN ;
    		case 4 :
    			return Color.RED ;
    		default :
    			return 0 ;
    		}
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

	        // Log.w(TAG,"Loading loading !") ;
	        
	        

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
