package za.dams.paracrm.widget;

import android.app.DialogFragment;

public class DatetimePickerDialog extends DialogFragment {
	@SuppressWarnings("unused")
	private static final String TAG = "Widget/DatetimePickerDialog";
	
    public interface OnDatetimeSetListener {
        void onDatetimeSet(int year, int month, int monthDay, int hourOfDay, int minute) ;
    }
    
    private OnDatetimeSetListener mListener ;

}
