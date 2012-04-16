package za.dams.paracrm.ui;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class TextpickerFragment extends DialogFragment {

	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/TextpickerFragment";
	
	int pageId ;
	int recordId ;
	int fieldId ;
	
    static TextpickerFragment newInstance(int pageId , int recordId , int fieldId ) {
    	TextpickerFragment f = new TextpickerFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("pageId", pageId);
        args.putInt("recordId", recordId);
        args.putInt("fieldId", fieldId);
        f.setArguments(args);

        return f;
    }
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageId = getArguments().getInt("pageId");
        recordId = getArguments().getInt("recordId");
        fieldId = getArguments().getInt("fieldId");
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	      
        View mv =  inflater.inflate(R.layout.textpicker_fragment, container, false ) ;
        
        
        final Button button = (Button) mv.findViewById(R.id.mytextokbutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	saveText() ;
            }
        });
        
        
        return mv ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	
    	getDialog().setTitle("Text Picker");
    	
    	String curString = mTransaction.page_getRecordFieldValue( pageId , recordId, fieldId ).valueString ;
    	
    	EditText textpicker = ((EditText)getView().findViewById(R.id.mytextpicker)) ;
     	textpicker.setText(curString) ;
   }
    
    public void saveText(){
    	EditText textpicker = ((EditText)getView().findViewById(R.id.mytextpicker)) ;
    	
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	mTransaction.page_setRecordFieldValue_text(pageId,recordId,fieldId,textpicker.getText().toString() ) ;
    	
    	((FiledetailFragment)getTargetFragment()).syncWithData() ;
    	getDialog().dismiss() ;
    	
    }
	
	
}
