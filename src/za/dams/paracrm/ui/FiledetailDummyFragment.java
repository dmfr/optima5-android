package za.dams.paracrm.ui;

import za.dams.paracrm.ImageAdapter;
import za.dams.paracrm.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TableLayout;

public class FiledetailDummyFragment extends FiledetailFragment {
    public static FiledetailDummyFragment newInstance(int index) {
    	FiledetailDummyFragment f = new FiledetailDummyFragment();

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
        
        /*
        CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
        CrmFileTransaction mTransaction = mManager.getTransaction() ;
        mTransaction.increment() ;
        Log.w(TAG,String.valueOf(mTransaction.getCounter()));
        */
        
        
        if( getShownIndex() == 1 ) {
        	View tbview =  inflater.inflate(R.layout.table_layout, container, false ) ;
        	
            final Button button = (Button) tbview.findViewById(R.id.mybutton1);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Perform action on click
                	TableLayout mTabLayout = (TableLayout) FiledetailDummyFragment.this.getView().findViewById(R.id.mytable) ;
                	mTabLayout.removeAllViews() ;
                }
            });

            return tbview ;
        }
        else {
        	if( getShownIndex() == 2 ) {
        		View tbview = inflater.inflate(R.layout.pictures_grid, container, false ) ;
        		GridView gridview = (GridView) tbview.findViewById(R.id.gridview);
        	    gridview.setAdapter(new ImageAdapter(getActivity()));

        		return tbview ;
        	
        	}
        	else {
               	if( getShownIndex() == 0 ) {
               		return inflater.inflate(R.layout.linear_layout, container, false ) ;
            	}
            	else {
            		View mv = inflater.inflate(R.layout.save_layout, container, false ) ;
            		
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
        }
    }

}
