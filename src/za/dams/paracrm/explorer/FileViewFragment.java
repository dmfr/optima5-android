package za.dams.paracrm.explorer;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class FileViewFragment extends Fragment implements View.OnClickListener {
	private static final String LOGTAG = "FileViewFragment";

    /** Argument name(s) */
    private static final String ARG_FILERECORD_ID = "filerecordId";
    
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
    
    public static FileViewFragment newInstance(long filerecordId) {
        if (filerecordId == 0) {
            throw new IllegalArgumentException();
        }
        final FileViewFragment instance = new FileViewFragment();
        final Bundle args = new Bundle();
        args.putLong(ARG_FILERECORD_ID, filerecordId);
        instance.setArguments(args);
        return instance;
    }
    
    
    private Long mImmutableFilerecordId;

    private void initializeArgCache() {
        if (mImmutableFilerecordId != null) return;
        mImmutableFilerecordId = getArguments().getLong(ARG_FILERECORD_ID);
    }

    /**
     * @return the message ID passed to {@link #newInstance}.  Safe to call even before onCreate.
     */
    public long getFilerecordId() {
        initializeArgCache();
        return mImmutableFilerecordId;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);
        /*
        mController.addResultCallback(mControllerCallback);

        resetView();
        new LoadMessageTask(true).executeParallel();
        */

        UiUtilities.installFragment(this);
    }
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        UiUtilities.uninstallFragment(this);
        /*
        mController.removeResultCallback(mControllerCallback);
        cancelAllTasks();
        */

        // We should clean up the Webview here, but it can't release resources until it is
        // actually removed from the view tree.

        super.onDestroyView();
    }
    
	@Override
	public void onClick(View v) {
		
		
	}
	
	
}
