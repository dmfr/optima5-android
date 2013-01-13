package za.dams.paracrm.explorer;

import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class DataListFragment extends ListFragment implements OnItemClickListener {
	
    private static final String TAG = "MailboxListFragment";

    private static final String BUNDLE_KEY_PARENT_MAILBOX_ID
            = "MailboxListFragment.state.parent_mailbox_id";
    private static final String BUNDLE_KEY_HIGHLIGHTED_MAILBOX_ID
            = "MailboxListFragment.state.selected_mailbox_id";
    private static final String BUNDLE_LIST_STATE = "MailboxListFragment.state.listState";
    // Total height of the top and bottom scroll zones, in pixels
    private static final int SCROLL_ZONE_SIZE = 64;
    // The amount of time to scroll by one pixel, in ms
    private static final int SCROLL_SPEED = 4;
    /** Arbitrary number for use with the loader manager */
    private static final int MAILBOX_LOADER_ID = 1;
    
    private static final String ARG_ENABLE_HIGHLIGHT = "enablehighlight";
    
    private RefreshManager mRefreshManager;

    // UI Support
    private Activity mActivity;
    private DataListFragmentAdapter mListAdapter;
    private Callback mCallback = EmptyCallback.INSTANCE;

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
        @Override
        public void onQuerySelected() { }
    }
    
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
    
    public static DataListFragment newInstance(boolean enableHighlight) {
        final DataListFragment instance = new DataListFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ENABLE_HIGHLIGHT, enableHighlight);
        instance.setArguments(args);
        return instance;
    }
    
    
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		
	}
	
	private class DataListEntry {
		public static final int DATA_BIBLE = 1 ;
		public static final int DATA_FILE = 2 ;
		public static final int DATA_QUERY = 3 ;
		
		public int dataType ;
		public String bibleCode ;
		public String fileCode ;
	}
	
	private class DataListLoaderCallbacks implements LoaderCallbacks<List<DataListEntry>> {

		@Override
		public Loader<List<DataListEntry>> onCreateLoader(int arg0, Bundle arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onLoadFinished(Loader<List<DataListEntry>> loader, List<DataListEntry> data) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onLoaderReset(Loader<List<DataListEntry>> loader) {
			// TODO Auto-generated method stub
			
		}
		
	}
	private class DataListFragmentAdapter extends ArrayAdapter<DataListFragment.DataListEntry> {

		public DataListFragmentAdapter(Context context) {
			super(context, 0);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}

		

	}

}
