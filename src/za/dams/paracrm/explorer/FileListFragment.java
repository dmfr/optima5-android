package za.dams.paracrm.explorer;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class FileListFragment extends ListFragment {
	private static final String LOGTAG = "FileListFragment";

    /** Argument name(s) */
    private static final String ARG_EXPLORER_CONTEXT = "explorerContext";
    
	private Callback mCallback = EmptyCallback.INSTANCE;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }

    public static FileListFragment newInstance(ExplorerContext explorerContext) {
        final FileListFragment instance = new FileListFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_EXPLORER_CONTEXT, explorerContext);
        instance.setArguments(args);
        return instance;
    }
    
    private ExplorerContext mExplorerContext ;
    
    private void initializeArgCache() {
        if (mExplorerContext != null) return;
        mExplorerContext = getArguments().getParcelable(ARG_EXPLORER_CONTEXT);
    }
    
    public String getFileCode() {
    	initializeArgCache();
    	if( mExplorerContext.mMode == ExplorerContext.MODE_FILE ) {
    		return mExplorerContext.mFileCode ;
    	}
    	return null ;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);

        /*
        final ListView lv = getListView();
        lv.setOnItemLongClickListener(this);
        lv.setOnTouchListener(this);
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListFooterView = getActivity().getLayoutInflater().inflate(
                R.layout.message_list_item_footer, lv, false);
        setEmptyText(getString(R.string.message_list_no_messages));

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
        }

        startLoading();
        */

        UiUtilities.installFragment(this);
    }
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        /*
        mIsViewCreated = false; // Clear this first for updateSelectionMode(). See isViewCreated().
        updateSelectionMode();

        // Reset the footer mode since we just blew away the footer view we were holding on to.
        // This will get re-updated when/if this fragment is restored.
        mListFooterMode = LIST_FOOTER_MODE_NONE;
        */
        
        UiUtilities.uninstallFragment(this);
        super.onDestroyView();
    }
    
    public boolean hasDataLoaded() {
    	return true ;
    }
    
    
    public void setLayout(ExplorerLayout layout) {
    }
    
    
    
    /**
     * Called when a file is clicked > for the detailed view
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {

    }
}
