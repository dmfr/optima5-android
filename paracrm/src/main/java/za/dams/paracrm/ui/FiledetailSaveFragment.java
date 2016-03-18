package za.dams.paracrm.ui;

import za.dams.paracrm.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FiledetailSaveFragment extends FiledetailFragment {

	public static FiledetailSaveFragment newInstance(int index) {
		FiledetailSaveFragment f = new FiledetailSaveFragment();
	
	    // Supply index input as an argument.
	    Bundle args = new Bundle();
	    args.putInt("index", index);
	    f.setArguments(args);
	
	    return f;
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }
        
        View mv = inflater.inflate(R.layout.filecapture_filedetail_save, container, false ) ;

        final Button button = (Button) mv.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		// Perform action on click
        		((FileCaptureActivity)getActivity()).endOfTransaction() ;
        	}
        });

        return mv ;
    }
}
