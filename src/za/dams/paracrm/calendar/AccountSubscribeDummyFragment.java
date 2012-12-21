package za.dams.paracrm.calendar;

import za.dams.paracrm.R;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
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

public class AccountSubscribeDummyFragment extends ListFragment {
	private static final String TAG = "Calendar/AccountSubscribeDummyFragment";

	private TextView mSyncStatus;

	// ******** Fields for PARACRM ********
	private static final String BUNDLE_KEY_CALFILECODE = "fileCode";
	private static final String BUNDLE_KEY_CALFILELIB = "fileLib";
	private String mCalendarFileCode ;
	private String mCalendarFileLib ;

	public class CalendarRow {
		String id;
		String displayName;
		int color;
		boolean synced;
		boolean originalSynced;
	}

	public AccountSubscribeDummyFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		Bundle bundle = getArguments();
		if (bundle != null 
				&& bundle.containsKey(BUNDLE_KEY_CALFILECODE) ) {
			mCalendarFileCode = bundle.getString(BUNDLE_KEY_CALFILECODE);
			mCalendarFileLib = bundle.getString(BUNDLE_KEY_CALFILELIB);
		}
	}

	@Override
	public View onCreateView(
			LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.calendar_accounts, null);

		mSyncStatus = (TextView) v.findViewById(R.id.account_status);
		mSyncStatus.setVisibility(View.GONE);
		
        Button mAccountsButton = (Button) v.findViewById(R.id.sync_settings);
        mAccountsButton.setVisibility(View.GONE);

		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		boolean preset = PrefsCrm.isCalendarEnabled(getActivity(), mCalendarFileCode) ;
		
		AccountSubscribeDummyAdapter adapter = new AccountSubscribeDummyAdapter(getActivity().getApplicationContext(),preset) ;
		setListAdapter(adapter) ;
		getListView().setOnItemClickListener(adapter);
	}

	@Override
	public void onPause() {
		//Log.w(TAG,"Save changes ?") ;
		final ListAdapter listAdapter = getListAdapter();
		if (listAdapter != null) {
			//Log.w(TAG,"Save changes !") ;
			// **** Save changes ****
			boolean enabled = ((AccountSubscribeDummyAdapter) listAdapter).isEnabled() ;
			PrefsCrm.setCalendarEnabled(getActivity(), mCalendarFileCode, enabled) ;
		}
		super.onPause();
	}





	private class AccountSubscribeDummyAdapter extends BaseAdapter
	implements ListAdapter, AdapterView.OnItemClickListener {

		private static final int LAYOUT = R.layout.calendar_accounts_item;
		private boolean mEnabled;

		private final String mSyncedString;
		private final String mNotSyncedString;


		public AccountSubscribeDummyAdapter(Context context, boolean preset ) {
			super();
			initData(preset);
			mSyncedString = "Enabled";
			mNotSyncedString = "Disabled";
		}

		private void initData(boolean preset) {
			mEnabled = preset ;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position >= 1) {
				return null;
			}
			View view;
			if (convertView == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater() ;
				view = inflater.inflate(LAYOUT, parent, false);
			} else {
				view = convertView;
			}

			view.setTag(mEnabled);

			CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
			cb.setChecked(mEnabled);

			if (mEnabled) {
				setText(view, R.id.status, mSyncedString);
			} else {
				setText(view, R.id.status, mNotSyncedString);
			}

			
			View colorView = view.findViewById(R.id.color);
			colorView.setVisibility(View.INVISIBLE) ;
			
			
			setText(view, R.id.calendar, AccountSubscribeDummyFragment.this.mCalendarFileLib);
			return view;
		}

		private void setText(View view, int id, String text) {
			TextView textView = (TextView) view.findViewById(id);
			textView.setText(text);
		}

		@Override
		public int getCount() {
			return 1 ;
		}

		@Override
    	public Object getItem(int position) {
    		return new Boolean(mEnabled);
    	}

		@Override
		public long getItemId(int position) {
			if (position >= 1) {
				return 0;
			}
			return position ;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
			if( position != 0 ) {
				return ;
			}

			mEnabled = !mEnabled;
			String status;
			if (mEnabled) {
				status = mSyncedString;
			} else {
				status = mNotSyncedString;
			}
			setText(view, R.id.status, status);

			CheckBox cb = (CheckBox) view.findViewById(R.id.sync);
			cb.setChecked(mEnabled);


			View colorView = view.findViewById(R.id.color);
			colorView.setVisibility(View.INVISIBLE) ;
		}

		
		public boolean isEnabled() {
			return mEnabled ;
		}

	}




}
