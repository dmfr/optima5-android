package za.dams.paracrm.ui;

import android.app.Fragment;

public class FiledetailFragment extends Fragment {

    public int getShownIndex() {
    	if( getArguments() != null ) {
    		return getArguments().getInt("index", 0);
    	}
        return -1 ;
    }
    public void syncWithData() {
    	
    }
	
}
