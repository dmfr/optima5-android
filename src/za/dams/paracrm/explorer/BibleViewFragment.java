package za.dams.paracrm.explorer;

import android.app.Fragment;
import android.os.Bundle;

public class BibleViewFragment extends Fragment {
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
    
    public static BibleViewFragment newInstance(String bibleEntryKey) {
        if (bibleEntryKey == null) {
            throw new IllegalArgumentException();
        }
        final BibleViewFragment instance = new BibleViewFragment();
        final Bundle args = new Bundle();
        return instance;
    }

}
