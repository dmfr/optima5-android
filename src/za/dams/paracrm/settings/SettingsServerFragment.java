package za.dams.paracrm.settings;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.MainPreferences;
import za.dams.paracrm.R;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SettingsServerFragment extends ListFragment implements OnItemClickListener  {
	private static final String TAG = "Settings/SettingsSrvManualFragment";
	
	private static final int ACTION_SRVAUTO = 1 ;
	private static final int ACTION_SRVMANUAL = 2 ;
	private static final int ACTION_DBPURGE = 11 ;
	private static final int ACTION_DBREFRESH = 12 ;
	
	private Activity mContext;
	private SettingsCallbacks mCallback ;
	
	List<ListRow> mRowList ;

	private class ListRow {
		boolean isHeader ;
		String headerTitle ;
		
		int itemId ;
		String itemTitle;
		String itemCaption;
	}
	
	private class ListAdapter extends BaseAdapter {
		private static final int TYPE_HEADER = 0 ;
		private static final int TYPE_ITEM = 1 ;
		
		LayoutInflater mInflater ;
		
		public ListAdapter() {
			Context context = SettingsServerFragment.this.getActivity() ;
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return mRowList.size();
		}

		@Override
		public ListRow getItem(int arg0) {
			return mRowList.get(arg0);
		}

		@Override
		public long getItemId(int position) {
			if( position < mRowList.size() ) {
				return position ;
			}
			return 0;
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
			ListRow rw = getItem(position) ;
			
			if (convertView == null) {
				if( rw.isHeader ) {
					view = mInflater.inflate(R.layout.settings_row_header, parent, false);
				} else {
					view = mInflater.inflate(R.layout.settings_row_item, parent, false);
				}
			} else {
				view = convertView;
			}
			
			if( rw.isHeader ) {
				((TextView)view.findViewById(R.id.header_title)).setText(rw.headerTitle) ;
			} else {
				((TextView)view.findViewById(R.id.item_title)).setText(rw.itemTitle) ;
				TextView caption = (TextView)view.findViewById(R.id.item_caption) ;
				if( rw.itemCaption == null ) {
					caption.setVisibility(View.GONE) ;
				} else {
					caption.setVisibility(View.VISIBLE) ;
					caption.setText(rw.itemCaption);
				}
			}
			
			return view;
		}
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		
		// Populate list
		mRowList = new ArrayList<ListRow>() ;
		ListRow row ;
		
		row = new ListRow() ;
		row.isHeader = true ;
		row.headerTitle = "Connection to server" ;
		mRowList.add(row);
		
		row = new ListRow() ;
		row.isHeader = false ;
		row.itemId = ACTION_SRVAUTO ;
		row.itemTitle = "Automatic Setup" ;
		mRowList.add(row);
		
		row = new ListRow() ;
		row.isHeader = false ;
		row.itemId = ACTION_SRVMANUAL ;
		row.itemTitle = "Manual configuration - Server Http-Domain-Sdomain" ;
		row.itemCaption = "" ;
		mRowList.add(row);
		
		
		row = new ListRow() ;
		row.isHeader = true ;
		row.headerTitle = "Database maintenance" ;
		mRowList.add(row);
		
		row = new ListRow() ;
		row.isHeader = false ;
		row.itemId = ACTION_DBPURGE ;
		row.itemTitle = "Clear local database" ;
		mRowList.add(row);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		ListAdapter listAdapter = new ListAdapter() ;
		getListView().setOnItemClickListener(this);
		setListAdapter(listAdapter);
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
	public void onResume() {
		super.onResume();
		buildCaptions();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long doNotUse) {
		final ListRow clickDle = ((ListAdapter)getListAdapter()).getItem(position) ;
		if( clickDle.isHeader ) {
			return ;
		}
		switch( clickDle.itemId ) {
		case ACTION_SRVMANUAL :
			if( mCallback != null && mCallback.IsLocalDbDirty() ) {
				UiUtilities.showAlert(mContext, "Unavailable", "Pending sync/uploads in progress !\n> Wait for completion or clear local DB") ;
				return ;
			}
			switchToFragment( "za.dams.paracrm.settings.SettingsSrvManualFragment", clickDle.itemTitle ) ;
			break ;
		case ACTION_DBPURGE :
			if( mCallback != null ) {
				mCallback.OnRequestClearDb();
			}
			break ;
		}
	}
	
	public void switchToFragment( String fragmentName, String fragmentTitle ) {
		// http://stackoverflow.com/questions/6925941/get-fragments-container-view-id
		int myContainerViewId = ((ViewGroup)getView().getParent()).getId() ;
		
		Fragment f = Fragment.instantiate(this.getActivity(), fragmentName);
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		transaction.replace(myContainerViewId, f);
		transaction.addToBackStack(null) ;
		transaction.setBreadCrumbTitle(fragmentTitle) ;
		transaction.commit() ;
	}
	
	private void buildCaptions() {
		new BuildCaptionsTask().execute() ;
	}
    private class BuildCaptionsTask extends AsyncTask<Void,Void,Void> {
    	String[] tCaptions ;

		@Override
		protected Void doInBackground(Void... params) {
			tCaptions = new String[mRowList.size()] ;
			
			int idx = -1 ;
			for( ListRow lr : mRowList ) {
				idx++ ;
				switch( lr.itemId ) {
				case ACTION_SRVMANUAL :
					tCaptions[idx] = MainPreferences.getInstance(SettingsServerFragment.this.mContext).getServerFullUrl() ;
					break ;
				default :
					tCaptions[idx] = null ;
					break;
				}
			}
			return null;
		}
    	@Override
    	protected void onPostExecute(Void arg0) {
    		if( !SettingsServerFragment.this.isAdded() ) {
    			return ;
    		}
    		for( int idx=0 ; idx<tCaptions.length ; idx++ ) {
    			mRowList.get(idx).itemCaption = tCaptions[idx] ;
    		}
    		((ListAdapter)SettingsServerFragment.this.getListAdapter()).notifyDataSetChanged() ;
    	}
    	
        public BuildCaptionsTask execute() {
    		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
        	return this ;
        }
    }
	

}
