package za.dams.paracrm.settings;

import java.util.regex.Pattern;

import za.dams.paracrm.MainMenuActivity;
import za.dams.paracrm.MainPreferences;
import za.dams.paracrm.R;
import za.dams.paracrm.calendar.EditEventView.AccountRow;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class SettingsSrvManualFragment extends Fragment {
	private static final String TAG = "Settings/SettingsSrvManualFragment";
	
	private Activity mContext;
	private SettingsCallbacks mCallback ;
	
	TextView mTxtUrl ;
	TextView mTxtDomain ;
	TextView mTxtSdomain ;
	View mErrorUrl ;
	View mErrorDomain ;
	View mErrorSdomain ;
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
        mTxtUrl = (TextView)view.findViewById(R.id.txt_srv_url) ;
        mTxtDomain = (TextView)view.findViewById(R.id.txt_srv_domain) ;
        mTxtSdomain = (TextView)view.findViewById(R.id.txt_srv_sdomain) ;
        mErrorUrl = view.findViewById(R.id.error_srv_url) ;
        mErrorDomain = view.findViewById(R.id.error_srv_domain) ;
        mErrorSdomain = view.findViewById(R.id.error_srv_sdomain) ;
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
        doLoadValues() ;
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
    private boolean onActionBarItemSelected(int itemId) {
        switch (itemId) {
        case R.id.action_done:
        	handleSaveValues();
         	break;

        case R.id.action_cancel:
        	closeFragment();
        	break;
        }
        return true;
    }
    
    private void handleSaveValues() {
    	if( !validateValues() ) {
    		UiUtilities.showAlert(mContext, "Error", "Incorrect values for URL/(S)Domain parameters") ;
         	return ;
    	}
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	builder.setMessage("Save parameters ?\n(Change of parameters will reset local DB)")
    	       .setCancelable(true)
    	       .setPositiveButton("Save", new DialogInterface.OnClickListener() {
    	    	   public void onClick(DialogInterface dialog, int id) {
    	    		   
    	    		   /* Actual save */
    	    		   dialog.dismiss();
    	    		   doSaveValues() ;
    	    		   if( mCallback != null ) {
    	    			   mCallback.OnServerChanged();
    	    		   }
    	    		   closeFragment() ;
    	    	   }
    	       })
    	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   dialog.dismiss();
    	           }
    	       })
    	       .show();
		return ;
    }
    
    private void doLoadValues() {
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	String url = prefs.getString("srv_url", "") ;
    	String domain = prefs.getString("srv_domain", "") ;
    	String sdomain = prefs.getString("srv_sdomain", "") ;
    	int dlTimeout = prefs.getInt("srv_dl_timeout", getResources().getInteger(R.integer.settings_srv_default_dl_timeout)) ;
    	int pullTimeout = prefs.getInt("srv_pull_timeout", getResources().getInteger(R.integer.settings_srv_default_pull_timeout)) ;
    	
    	mTxtUrl.setText(url);
    	mTxtDomain.setText(domain);
    	mTxtSdomain.setText(sdomain);
    	mDlTimeoutSpinner.setSelection( ((TimeoutSpinnerAdapter)mDlTimeoutSpinner.getAdapter()).searchPositionByItem(dlTimeout) ) ;
    	mPullTimeoutSpinner.setSelection( ((TimeoutSpinnerAdapter)mPullTimeoutSpinner.getAdapter()).searchPositionByItem(pullTimeout) ) ;
    }
    private boolean validateValues() {
    	String url = mTxtUrl.getText().toString().trim() ;
    	String domain = mTxtDomain.getText().toString().trim() ;
    	String sdomain = mTxtSdomain.getText().toString().trim() ;
    	
    	String regExpPattern = "^[a-z]*$";
    	
    	boolean hasError = false ;
    	
    	if( Patterns.WEB_URL.matcher(url).matches() && (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url)) ) {
    		mErrorUrl.setVisibility(View.GONE) ;
    	} else {
    		hasError = true ;
    		mErrorUrl.setVisibility(View.VISIBLE) ;
    	}
    	if( domain.length() > 0 && domain.matches(regExpPattern) ) {
    		mErrorDomain.setVisibility(View.GONE) ;
    	} else {
    		hasError = true ;
    		mErrorDomain.setVisibility(View.VISIBLE) ;
    	}
    	if( sdomain.length() > 0 && sdomain.matches(regExpPattern) ) {
    		mErrorSdomain.setVisibility(View.GONE) ;
    	} else {
    		hasError = true ;
    		mErrorSdomain.setVisibility(View.VISIBLE) ;
    	}
    	
    	return !hasError ;
    }
    private void doSaveValues() {
    	String url = mTxtUrl.getText().toString().trim() ;
    	String domain = mTxtDomain.getText().toString().trim() ;
    	String sdomain = mTxtSdomain.getText().toString().trim() ;
    	int dlTimeout = (Integer)mDlTimeoutSpinner.getAdapter().getItem(mDlTimeoutSpinner.getSelectedItemPosition()) ;
    	int pullTimeout = (Integer)mPullTimeoutSpinner.getAdapter().getItem(mPullTimeoutSpinner.getSelectedItemPosition()) ;
    	
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	SharedPreferences.Editor editor = prefs.edit() ;
    	editor.remove("srv_profilestr") ;
    	editor.putString("srv_url", url);
    	editor.putString("srv_domain", domain);
    	editor.putString("srv_sdomain", sdomain);
    	editor.putInt("srv_dl_timeout", dlTimeout);
    	editor.putInt("srv_pull_timeout", pullTimeout) ;
    	editor.commit() ;
    }
    
    private void closeFragment() {
    	final InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);   	
    	
    	getFragmentManager().popBackStack() ;
    }
    
    
    private static class TimeoutSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    	
    	private final int[] timeoutChoices ;
    	private LayoutInflater mInflater ;
    	
    	public TimeoutSpinnerAdapter( Context c ) {
    		mInflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
    		timeoutChoices = c.getResources().getIntArray(R.array.settings_srv_choices_timeout);
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
		
    	public int searchPositionByItem( int searchedTimeout ) {
        	for( int idx=0 ; idx<timeoutChoices.length ; idx++ ) {
        		if( timeoutChoices[idx] == searchedTimeout ) {
        			return idx ;
        		}
        	}
        	return -1 ;
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
