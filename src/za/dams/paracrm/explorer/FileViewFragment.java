package za.dams.paracrm.explorer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

public class FileViewFragment extends Fragment implements View.OnClickListener {

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
    
    
    public long getFilerecordId() {
    	return 0 ;
    }
    
    
    
	@Override
	public void onClick(View v) {
		
		
	}
	
	
}
