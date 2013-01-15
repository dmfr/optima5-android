package za.dams.paracrm.explorer;

import za.dams.paracrm.R;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class EmptyListFragment extends Fragment {
	
    public static EmptyListFragment newInstance() {
        final EmptyListFragment instance = new EmptyListFragment();
        return instance;
    }
    
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explorer_welcome_fragment, container, false);
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
