package za.dams.paracrm.ui;

import java.util.Date;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DatetimepickerFragment extends DialogFragment {

	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/DatetimepickerFragment";
	
	int pageId ;
	int recordId ;
	int fieldId ;
	
    static DatetimepickerFragment newInstance(int pageId , int recordId , int fieldId ) {
    	DatetimepickerFragment f = new DatetimepickerFragment();

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
	      
        View mv =  inflater.inflate(R.layout.datetimepicker_fragment, container, false ) ;
        
        ((TimePicker)mv.findViewById(R.id.mytimepicker)).setIs24HourView(true) ;
        
        final Button button = (Button) mv.findViewById(R.id.mydatetimeokbutton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	saveDateTime() ;
            }
        });
        
        
        return mv ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	
    	getDialog().setTitle("Date/Time Picker");
    	
    	Date curDate = mTransaction.page_getRecordFieldValue( pageId , recordId, fieldId ).valueDate ;
    	
    	DatePicker datepicker = ((DatePicker)getView().findViewById(R.id.mydatepicker)) ;
    	TimePicker timepicker = ((TimePicker)getView().findViewById(R.id.mytimepicker)) ;
    	datepicker.init( curDate.getYear() + 1900 , curDate.getMonth() , curDate.getDate() , null ) ;
    	timepicker.setCurrentHour( curDate.getHours() ) ;
    	timepicker.setCurrentMinute( curDate.getMinutes() ) ;
    	
   }
    
    public void saveDateTime(){
    	DatePicker datepicker = ((DatePicker)getView().findViewById(R.id.mydatepicker)) ;
    	TimePicker timepicker = ((TimePicker)getView().findViewById(R.id.mytimepicker)) ;
    	
    	int annee = datepicker.getYear() - 1900 ;
    	int mois = datepicker.getMonth();
    	int jour = datepicker.getDayOfMonth() ;
    	int heure = timepicker.getCurrentHour() ;
    	int minute = timepicker.getCurrentMinute() ;
    	
    	Date newDate = new Date(annee,mois,jour,heure,minute) ;
    	
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	mTransaction.page_setRecordFieldValue_date(pageId,recordId,fieldId,newDate ) ;
    	
    	((FiledetailListFragment)getTargetFragment()).syncWithData() ;
    	getDialog().dismiss() ;
    	
    }
	
	
}
