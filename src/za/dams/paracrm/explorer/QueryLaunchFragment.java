package za.dams.paracrm.explorer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

public class QueryLaunchFragment extends Fragment {
	private static final String LOGTAG = "QueryLaunchFragment";
	
	private Context mContext;
	private LayoutInflater mInflater ;

    /** Argument name(s) */
    private static final String ARG_QUERYSRC_ID = "querysrcId";
    
	private Callback mCallback = EmptyCallback.INSTANCE;
	
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    	public void onQueryLaunchStart( int querysrcId ) ;
    	public void onQueryResponseFailure( int querysrcId ) ;
    	public void onQueryResponseSuccess( int querysrcId , int cacheResultJsonId ) ;
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    	public void onQueryLaunchStart( int querysrcId ) {}
    	public void onQueryResponseFailure( int querysrcId ) {}
    	public void onQueryResponseSuccess( int querysrcId , int cacheResultJsonId ) {}
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
	
	
    public static QueryLaunchFragment newInstance(int querysrcId) {
        final QueryLaunchFragment instance = new QueryLaunchFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_QUERYSRC_ID, querysrcId);
        instance.setArguments(args);
        return instance;
    }
    private Integer mImmutableQuerysrcId;
    private void initializeArgCache() {
        if (mImmutableQuerysrcId != null) return;
        mImmutableQuerysrcId = getArguments().getInt(ARG_QUERYSRC_ID);
    }
    public int getQuerySrcId() {
        initializeArgCache();
        return mImmutableQuerysrcId;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        UiUtilities.installFragment(this);
    }
    @Override
    public void onDestroyView() {
        UiUtilities.uninstallFragment(this);
        super.onDestroyView();
    }

}
