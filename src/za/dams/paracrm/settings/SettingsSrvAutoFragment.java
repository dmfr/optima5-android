package za.dams.paracrm.settings;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.HttpServerHelper;
import za.dams.paracrm.MainPreferences;
import za.dams.paracrm.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SettingsSrvAutoFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<SettingsSrvAutoFragment.SrvProfile>> {
		
	private static final String TAG = "Settings/SettingsSrvManualFragment";
	private static final int LOADER_ID = 1 ;
	
	private Activity mContext;
	private SettingsCallbacks mCallback ;
	
	private SrvProfile tSelectedProfile ;
    
	public static class SrvProfile {
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
    		if( mData==null ) {
    			return 0 ;
    		}
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
	    	
	        final String serverUrl = getContext().getResources().getString(R.string.directory_url) ;
	    	
	    	final HttpClient client = AndroidHttpClient.newInstance("Android");
	        client.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 10000);
	        client.getParams().setParameter(HttpConnectionParams.SO_TIMEOUT, 10000);
	    	
	        final HttpPost postRequest = new HttpPost(serverUrl);
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2) ;
	        String android_id = Settings.Secure.getString(getContext().getContentResolver(),Settings.Secure.ANDROID_ID);
	        nameValuePairs.add(new BasicNameValuePair("__ANDROID_ID", android_id));
			try {
				postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} catch (UnsupportedEncodingException e) {
				return null ;
			}

        	String response = new String() ;
        	try {
				HttpResponse httpresponse = client.execute(postRequest);
				HttpEntity entity = httpresponse.getEntity();
				InputStream content = entity.getContent();
				response= HttpServerHelper.readStream(content) ;
        	}
        	catch(Exception e){
        		postRequest.abort() ;
        		return null ;
        		//e.printStackTrace() ;
        		//Log.w("Bin upload","Failed 3") ;
        	}finally {
        		if ((client instanceof AndroidHttpClient)) {
        			((AndroidHttpClient) client).close();
        		}
        	}
			
        	//Log.w(TAG,"Response from server : "+response) ;
        	ArrayList<SrvProfile> entries = new ArrayList<SrvProfile>();
        	try {
        		JSONObject jsonResponse = new JSONObject(response) ;
        		if( !jsonResponse.optBoolean("success", false) ) {
        			return null ;
        		}
        		
        		JSONArray jsonEntries = jsonResponse.optJSONArray("data") ;
        		if( jsonEntries != null ) {
        			for( int idx=0 ; idx<jsonEntries.length() ; idx++ ) {
        				JSONObject jsonEntry = jsonEntries.getJSONObject(idx) ;

        				SrvProfile srvProfile = new SrvProfile() ;
        				srvProfile.profileCode = jsonEntry.optString("profile_code") ;
        				srvProfile.profileName = jsonEntry.optString("profile_name") ;
        				srvProfile.srvUrl = jsonEntry.optString("srv_url") ;
        				srvProfile.srvDomain = jsonEntry.optString("srv_domain") ;
        				srvProfile.srvSdomain = jsonEntry.optString("srv_sdomain") ;
        				entries.add(srvProfile);
        			}
        		}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return null ;
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

	
	private void forceReload() {
    	final LoaderManager lm = getLoaderManager();
    	if( lm.getLoader(LOADER_ID)!=null ) {
    		this.setListShown(false) ;
    		lm.getLoader(LOADER_ID).forceLoad() ;
    	}
    }
    private void closeFragment() {
    	getFragmentManager().popBackStack() ;
    }

	private void onProfileSelected(SrvProfile clickedProfile) {
		tSelectedProfile = clickedProfile ;
		
		// TODO Auto-generated method stub
    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	builder.setTitle("Selected profile")
    	       .setMessage( clickedProfile.profileName + "\n\nThis will reset local DB. Continue ?")
    	       .setCancelable(false)
    	       .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    	    	   public void onClick(DialogInterface dialog, int id) {
    	    		   
    	    		   /* Actual save */
    	    		   dialog.dismiss();
    	    		   doSelectProfile( SettingsSrvAutoFragment.this.tSelectedProfile ) ;
    	    		   if( mCallback != null ) {
    	    			   mCallback.OnServerChanged();
    	    		   }
    	    		   closeFragment() ;
    	    	   }
    	       })
    	       .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   dialog.dismiss();
    	        	   SettingsSrvAutoFragment.this.tSelectedProfile = null ;
    	           }
    	       })
    	       .show();
		return ;
	}
	private void doSelectProfile(SrvProfile clickedProfile) {
    	int dlTimeout = mContext.getResources().getInteger(R.integer.settings_srv_default_dl_timeout) ;
    	int pullTimeout = mContext.getResources().getInteger(R.integer.settings_srv_default_pull_timeout) ;
    	
    	SharedPreferences prefs = mContext.getSharedPreferences(MainPreferences.SHARED_PREFS_NAME,0);
    	SharedPreferences.Editor editor = prefs.edit() ;
    	editor.putString("srv_profilestr", clickedProfile.profileName) ;
    	editor.putString("srv_url", clickedProfile.srvUrl);
    	editor.putString("srv_domain", clickedProfile.srvDomain);
    	editor.putString("srv_sdomain", clickedProfile.srvSdomain);
    	editor.putInt("srv_dl_timeout", dlTimeout);
    	editor.putInt("srv_pull_timeout", pullTimeout) ;
    	editor.commit() ;
	}

}
