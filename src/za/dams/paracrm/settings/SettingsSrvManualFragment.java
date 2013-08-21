package za.dams.paracrm.settings;

import za.dams.paracrm.R;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class SettingsSrvManualFragment extends Fragment {
	private static final String TAG = "Settings/SettingsSrvManualFragment";
	
	private static final int SAVE_ERR_NONE = 0 ;
	private static final int SAVE_ERR_URL = 1 ;
	private static final int SAVE_ERR_MISSING = 2 ;
	
	
	private Activity mContext;
	private SettingsCallbacks mCallback ;
	
	Spinner mDlTimeoutSpinner ;
	Spinner mPullTimeoutSpinner ;
	
	public SettingsSrvManualFragment() {
		setHasOptionsMenu(true);
	}
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        if (activity instanceof SettingsCallbacks) {
        	mCallback = (SettingsCallbacks)activity;
        } else {
        	Log.e(TAG,activity.toString()+" must implement OnHeadlineSelectedListener");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.settings_srvmanual_fragment, null);
        
        // Attach UI to class members = cache all the widgets
        mDlTimeoutSpinner = (Spinner)view.findViewById(R.id.srv_dl_timeout) ;
        mPullTimeoutSpinner = (Spinner)view.findViewById(R.id.srv_pull_timeout) ;
        
        return view ;
    }
    

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		/* Enable spinners */
		TimeoutSpinnerAdapter adapter = new TimeoutSpinnerAdapter( mContext ) ;
        mDlTimeoutSpinner.setAdapter(adapter);
        mPullTimeoutSpinner.setAdapter(adapter);
        
        /* Start loading */
        loadValues() ;
	}

	@Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
        	// TODO: restore keyed-in values
        }
    }
    @Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO : save currently keyin values
		super.onSaveInstanceState(outState);
	}
    @Override
    public void onDestroy() {
    	super.onDestroy() ;
    }

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.settings_srvmanual_titlebar, menu);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(R.id.action_cancel).setVisible(true);
    	menu.findItem(R.id.action_done).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }

    /**
     * Handles menu item selections, whether they come from our custom action bar buttons or from
     * the standard menu items. Depends on the menu item ids matching the custom action bar button
     * ids.
     *
     * @param itemId the button or menu item id
     * @return whether the event was handled here
     */
    private boolean onActionBarItemSelected(int itemId) {
        switch (itemId) {
        case R.id.action_done:
        	int returnCode = saveValues() ;
        	if( returnCode == SAVE_ERR_NONE ) {
        		if( mCallback != null ) {
        			mCallback.OnServerChanged();
        		}
        		getFragmentManager().popBackStack() ;
        		break ;
        	}
        	
        	String errMsg ;
        	switch( returnCode ) {
        	case SAVE_ERR_URL :
        		errMsg = "Server URL is not valid" ;
        		break ;
        	case SAVE_ERR_MISSING :
        		errMsg = "Parameters are missing" ;
        		break ;
        	default:
        		errMsg = "Undefined error" ;
        		break ;
        	}
        	UiUtilities.showAlert(mContext, "Error", errMsg) ;
        	
        	break;

        case R.id.action_cancel:
        	getFragmentManager().popBackStack() ;
        	break;
        }
        return true;
    }
    
    private void loadValues() {
    	
    }
    private int saveValues() {
    	
    	
    	return SAVE_ERR_NONE ;
    }
    
    
    private static class TimeoutSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    	
    	private static final int[] timeoutChoices = {10,30,60,120} ;
    	private LayoutInflater mInflater ;
    	
    	public TimeoutSpinnerAdapter( Context c ) {
    		mInflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
    	}
    	
		@Override
		public int getCount() {
			return timeoutChoices.length ;
		}

		@Override
		public Object getItem(int position) {
			if( position < timeoutChoices.length ) {
				return new Integer(timeoutChoices[position]) ;
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv ;
			if( convertView != null ) {
				tv = (TextView)convertView ;
			} else {
				tv = (TextView)mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
			}
			tv.setText(getItem(position).toString()+" "+"s") ;
			return tv;
		}
    	
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView tv ;
			if( convertView != null ) {
				tv = (TextView)convertView ;
			} else {
				tv = (TextView)mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
			}
			tv.setText(getItem(position).toString()+" "+"s") ;
			return tv;
		}
    	
    }
}
