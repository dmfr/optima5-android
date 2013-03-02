package za.dams.paracrm.widget;

import za.dams.paracrm.R;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class DatetimePickerDialog extends DialogFragment implements View.OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = "Widget/DatetimePickerDialog";
	
	DatePicker mDatePicker ;
	TimePicker mTimePicker ;
	View mOkButton ;
	
	private Runnable mWorkaround = new Runnable(){
		@Override
		public void run() {
			Time t = new Time() ;
			try{
				t.set(mDatePicker.getDayOfMonth(), mDatePicker.getMonth(), mDatePicker.getYear());
				t.monthDay += 1 ;
				mDatePicker.getCalendarView().setDate(t.toMillis(true));
				t.monthDay -= 1 ;
				mDatePicker.getCalendarView().setDate(t.toMillis(true));
			} catch(Exception e) {
				
			}
			try{
				t.set(mDatePicker.getDayOfMonth(), mDatePicker.getMonth(), mDatePicker.getYear());
				t.monthDay -= 1 ;
				mDatePicker.getCalendarView().setDate(t.toMillis(true));
				t.monthDay += 1 ;
				mDatePicker.getCalendarView().setDate(t.toMillis(true));
			} catch(Exception e) {
				
			}
			
		}
	};
	
    public interface OnDatetimeSetListener {
        void onDatetimeSet(int year, int month, int monthDay, int hourOfDay, int minute) ;
    }
    private OnDatetimeSetListener mListener ;
    public void setListener( OnDatetimeSetListener listener ) {
    	mListener = listener ;
    }
    
    public static DatetimePickerDialog newInstance( int year, int month, int monthDay, int hourOfDay, int minute ) {
    	return newInstance(year,month,monthDay,hourOfDay,minute,false,-1,-1);
    }
    public static DatetimePickerDialog newInstance( int year, int month, int monthDay, int hourOfDay, int minute, boolean hideTime ) {
    	return newInstance(year,month,monthDay,hourOfDay,minute,hideTime,-1,-1);
    }
    public static DatetimePickerDialog newInstance( int year, int month, int monthDay, int hourOfDay, int minute,
    		boolean hideTime, long minMillis, long maxMillis) {
    	DatetimePickerDialog f = new DatetimePickerDialog() ;
    	
        // Supply arguments.
        Bundle args = new Bundle();
        args.putInt("year", year);
        args.putInt("month", month);
        args.putInt("monthDay", monthDay);
        args.putInt("hourOfDay", hourOfDay);
        args.putInt("minute", minute);
        args.putBoolean("hideTime", hideTime);
       	args.putLong("minMillis", minMillis);
       	args.putLong("maxMillis", maxMillis);
        f.setArguments(args);
        
        return f;
    }
    public void setTitle(CharSequence dialogTitle) {
    	getArguments().putCharSequence("dialogTitle", dialogTitle);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View mv =  inflater.inflate(R.layout.datetimepicker_fragment, null, false ) ;
    	
    	mDatePicker = ((DatePicker)mv.findViewById(R.id.mydatepicker)) ;
    	mTimePicker = ((TimePicker)mv.findViewById(R.id.mytimepicker)) ;
    	mOkButton = mv.findViewById(R.id.mydatetimeokbutton) ;
    	
    	Bundle args = getArguments() ;
    	
    	if( args.containsKey("dialogTitle") ) {
    		getDialog().setTitle(args.getCharSequence("dialogTitle"));
    	} else {
    		getDialog().setTitle("Date Picker");
    	}
    	
     	int year, month, monthDay, hourOfDay, minute ; 
    	if( savedInstanceState != null ) {
    		year = savedInstanceState.getInt("year") ;
    		month = savedInstanceState.getInt("month") ;
    		monthDay = savedInstanceState.getInt("monthDay") ;
    		hourOfDay = savedInstanceState.getInt("hourOfDay") ;
    		minute = savedInstanceState.getInt("minute") ;
    	} else {
    		year = args.getInt("year") ;
    		month = args.getInt("month") ;
    		monthDay = args.getInt("monthDay") ;
    		hourOfDay = args.getInt("hourOfDay") ;
    		minute = args.getInt("minute") ;
    	}
    	mDatePicker.init(year, month, monthDay, null) ;
    	mTimePicker.setIs24HourView(true);
    	mTimePicker.setCurrentHour(hourOfDay) ;
    	mTimePicker.setCurrentMinute(minute) ;
    	
    	if( args.getLong("minMillis") != -1 ) {
    		mDatePicker.setMinDate(normalizeAllDayMillis(args.getLong("minMillis"))) ;
    	}
    	if( args.getLong("maxMillis") != -1 ) {
    		mDatePicker.setMaxDate(normalizeAllDayMillis(args.getLong("maxMillis"))) ;
    	}
    	if( args.getBoolean("hideTime") ) {
    		mTimePicker.setVisibility(View.GONE) ;
    	}
    	
    	mOkButton.setOnClickListener(this);
    	
    	return mv ;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	outState.putInt("year", mDatePicker.getYear()) ;
    	outState.putInt("month", mDatePicker.getMonth()) ;
    	outState.putInt("monthDay", mDatePicker.getDayOfMonth()) ;
    	outState.putInt("hourOfDay",mTimePicker.getCurrentHour());
    	outState.putInt("minute",mTimePicker.getCurrentMinute());
    	super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	getView().postDelayed(mWorkaround,10);
    }
    
	@Override
	public void onClick(View arg0) {
		if( arg0==mOkButton ) {
			onSubmit();
		}
	}
    
	public void onSubmit() {
		if( mListener != null ) {
			mListener.onDatetimeSet(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(),
					mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute()) ;
		}
		dismiss();
	}
	
	
	
	public static long normalizeAllDayMillis( long millis ) {
		Time t = new Time() ;
		t.set(millis);
		t.hour = 0 ;
		t.minute = 0 ;
		t.second = 0 ;
		t.normalize(true) ;
		return t.toMillis(true);
	}
	public static long getDayOffsetMillis( int deltaDay ) {
		Time t = new Time() ;
		t.allDay = true ;
		t.setToNow() ;
		t.monthDay += deltaDay ;
		t.normalize(true) ;
		return t.toMillis(true);
	}

}
