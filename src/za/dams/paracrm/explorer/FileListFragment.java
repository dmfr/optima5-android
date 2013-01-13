package za.dams.paracrm.explorer;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class FileListFragment extends ListFragment {

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

    public static FileListFragment newInstance(ExplorerContext listContext) {
        final FileListFragment instance = new FileListFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_EXPLORER_CONTEXT, listContext);
        instance.setArguments(args);
        return instance;
    }
    
    
    public boolean hasDataLoaded() {
    	return true ;
    }
    public String getFileCode() {
    	return null ;
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
