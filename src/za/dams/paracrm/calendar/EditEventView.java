package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleCode;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.R;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

public class EditEventView implements View.OnClickListener, DialogInterface.OnCancelListener,
	DialogInterface.OnClickListener, OnItemSelectedListener {
	
	private static final String TAG = "Calendar/EditEventView";
	
    private boolean mIsMultipane;
    private ProgressDialog mLoadingCalendarsDialog;
    private AlertDialog mNoCalendarsDialog;
    private AlertDialog mTimezoneDialog;
    private Activity mActivity;
    private Runnable mDone;
    private View mView;
    private CrmEventModel mModel;
    private Cursor mCalendarsCursor;
    
    private Time mStartTime;
    private Time mEndTime;
    
    ViewGroup mViewgroupTable ;
    TextView mLoadingMessage;
    ScrollView mScrollView;
    Button mStartDateButton;
    Button mEndDateButton;
    Button mStartTimeButton;
    Button mEndTimeButton;
    Button mTimezoneButton;
    View mTimezoneRow;
    CheckBox mAllDayCheckBox;
    Spinner mCalendarsSpinner;
    View mCalendarSelectorGroup;
    View mCalendarSelectorWrapper;
    View mCalendarStaticGroup;
    View mAllDayRow ;
    ArrayList<View> mCrmFieldViews ;
    
    
    private String mTimezone ;
    
    private static StringBuilder mSB = new StringBuilder(50);
    private static Formatter mF = new Formatter(mSB, Locale.getDefault());
	
	public EditEventView(Activity activity, View view, Runnable done) {
        mActivity = activity;
        mView = view;
        mDone = done;
        
        // System timezone
		mTimezone = Utils.getTimeZone(mActivity, null); 
		
        // cache top level view elements
        mLoadingMessage = (TextView) view.findViewById(R.id.loading_message);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);

        // cache all the widgets
        mViewgroupTable = (ViewGroup)view.findViewById(R.id.calendar_editevent_table);
        
        mCalendarsSpinner = (Spinner) view.findViewById(R.id.calendars_spinner);
        mStartDateButton = (Button) view.findViewById(R.id.start_date);
        mEndDateButton = (Button) view.findViewById(R.id.end_date);
        mStartTimeButton = (Button) view.findViewById(R.id.start_time);
        mEndTimeButton = (Button) view.findViewById(R.id.end_time);
        mCalendarSelectorGroup = view.findViewById(R.id.calendar_selector_group);
        mCalendarSelectorWrapper = view.findViewById(R.id.calendar_selector_wrapper);
        mCalendarStaticGroup = view.findViewById(R.id.calendar_group);
        mAllDayRow = view.findViewById(R.id.all_day_row);
        
        mStartTime = new Time(mTimezone);
        mEndTime = new Time(mTimezone);
	}
	
	
    private class TimeListener implements OnTimeSetListener {
        private View mView;

        public TimeListener(View view) {
            mView = view;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Cache the member variables locally to avoid inner class overhead.
            Time startTime = mStartTime;
            Time endTime = mEndTime;

            // Cache the start and end millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long startMillis;
            long endMillis;
            if (mView == mStartTimeButton) {
                // The start time was changed.
                int hourDuration = endTime.hour - startTime.hour;
                int minuteDuration = endTime.minute - startTime.minute;

                startTime.hour = hourOfDay;
                startTime.minute = minute;
                startMillis = startTime.normalize(true);

                // Also update the end time to keep the duration constant.
                endTime.hour = hourOfDay + hourDuration;
                endTime.minute = minute + minuteDuration;
            } else {
                // The end time was changed.
                startMillis = startTime.toMillis(true);
                endTime.hour = hourOfDay;
                endTime.minute = minute;

                // Move to the start time if the end time is before the start
                // time.
                if (endTime.before(startTime)) {
                    endTime.monthDay = startTime.monthDay + 1;
                }
            }

            endMillis = endTime.normalize(true);

            setDate(mEndDateButton, endMillis);
            setTime(mStartTimeButton, startMillis);
            setTime(mEndTimeButton, endMillis);
        }
    }

    private class TimeClickListener implements View.OnClickListener {
        private Time mTime;

        public TimeClickListener(Time time) {
            mTime = time;
        }

        @Override
        public void onClick(View v) {
            TimePickerDialog tp = new TimePickerDialog(mActivity, new TimeListener(v), mTime.hour,
                    mTime.minute, DateFormat.is24HourFormat(mActivity));
            tp.setCanceledOnTouchOutside(true);
            tp.show();
        }
    }

    private class DateListener implements OnDateSetListener {
        View mView;

        public DateListener(View view) {
            mView = view;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            Log.d(TAG, "onDateSet: " + year +  " " + month +  " " + monthDay);
            // Cache the member variables locally to avoid inner class overhead.
            Time startTime = mStartTime;
            Time endTime = mEndTime;

            // Cache the start and end millis so that we limit the number
            // of calls to normalize() and toMillis(), which are fairly
            // expensive.
            long startMillis;
            long endMillis;
            if (mView == mStartDateButton) {
                // The start date was changed.
                int yearDuration = endTime.year - startTime.year;
                int monthDuration = endTime.month - startTime.month;
                int monthDayDuration = endTime.monthDay - startTime.monthDay;

                startTime.year = year;
                startTime.month = month;
                startTime.monthDay = monthDay;
                startMillis = startTime.normalize(true);

                // Also update the end date to keep the duration constant.
                endTime.year = year + yearDuration;
                endTime.month = month + monthDuration;
                endTime.monthDay = monthDay + monthDayDuration;
                endMillis = endTime.normalize(true);

                // If the start date has changed then update the repeats.
                // populateRepeats(); // *** Pas géré dans ParaCRM ***
            } else {
                // The end date was changed.
                startMillis = startTime.toMillis(true);
                endTime.year = year;
                endTime.month = month;
                endTime.monthDay = monthDay;
                endMillis = endTime.normalize(true);

                // Do not allow an event to have an end time before the start
                // time.
                if (endTime.before(startTime)) {
                    endTime.set(startTime);
                    endMillis = startMillis;
                }
            }

            setDate(mStartDateButton, startMillis);
            setDate(mEndDateButton, endMillis);
            setTime(mEndTimeButton, endMillis); // In case end time had to be
        }
    }
	
    private class DateClickListener implements View.OnClickListener {
        private Time mTime;

        public DateClickListener(Time time) {
            mTime = time;
        }

        public void onClick(View v) {
            DatePickerDialog dpd = new DatePickerDialog(
                    mActivity, new DateListener(v), mTime.year, mTime.month, mTime.monthDay);
            CalendarView cv = dpd.getDatePicker().getCalendarView();
            cv.setShowWeekNumber(Utils.getShowWeekNumber(mActivity));
            int startOfWeek = Utils.getFirstDayOfWeek(mActivity);
            // Utils returns Time days while CalendarView wants Calendar days
            if (startOfWeek == Time.SATURDAY) {
                startOfWeek = Calendar.SATURDAY;
            } else if (startOfWeek == Time.SUNDAY) {
                startOfWeek = Calendar.SUNDAY;
            } else {
                startOfWeek = Calendar.MONDAY;
            }
            cv.setFirstDayOfWeek(startOfWeek);
            dpd.setCanceledOnTouchOutside(true);
            dpd.show();
        }
    }
    
    private class BibleListener implements OnBibleSetListener {
        View mView;
        int mCrmFieldIndex ;

        public BibleListener(View view, int crmFieldIndex) {
        	mCrmFieldIndex = crmFieldIndex ;
            mView = view;
        }

        @Override
        public void onBibleSet(BibleEntry be) {
            if( be != null ) {
            	Log.d(TAG, "onBibleSet: " + be.displayStr + " for field idx "+mCrmFieldIndex);
            	((Button)mView).setText(be.displayStr) ;
            	mModel.mCrmValues.get(mCrmFieldIndex).isSet = true ;
            	mModel.mCrmValues.get(mCrmFieldIndex).displayStr = be.displayStr ;
            	mModel.mCrmValues.get(mCrmFieldIndex).valueString = be.entryKey ;
            }
            else{
            	((Button)mView).setText("") ;
            	mModel.mCrmValues.get(mCrmFieldIndex).isSet = false ;
                mModel.mCrmValues.get(mCrmFieldIndex).displayStr = "" ;
                mModel.mCrmValues.get(mCrmFieldIndex).valueString = "" ;
            }
        }
    }
	
    private class BibleClickListener implements View.OnClickListener {
    	BibleCode mBc ;
    	int mCrmFieldIndex ;
    	
        public BibleClickListener( BibleCode bc, int crmFieldIndex ) {
        	mBc = bc ;
        	mCrmFieldIndex = crmFieldIndex ;
        }

        public void onClick(View v) {
        	FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        	
        	ArrayList<BibleEntry> bibleConditions = null ;
        	if( mModel.mAccountEntry != null ) {
        		bibleConditions = new ArrayList<BibleEntry>() ;
        		bibleConditions.add(mModel.mAccountEntry) ;
        	}
        	
            BiblePickerDialog bpd = new BiblePickerDialog(mActivity, new BibleListener(v,mCrmFieldIndex), mBc,bibleConditions);
            //bpd.setTargetFragment(this, 0);
            //bpd.setCanceledOnTouchOutside(true);
            bpd.show(ft, "dialog") ;
        }
    }
    
    
	public static class AccountRow {
		String id;
		String displayName;
		int color;
		boolean synced;
		boolean originalSynced;
		BibleHelper.BibleEntry be ;
	}
    private class AccountsAdapter extends BaseAdapter
    	implements ListAdapter {

    	private LayoutInflater mInflater;
    	private static final int LAYOUT = R.layout.calendar_accounts_dropdown_item;
    	private AccountRow[] mData;
    	private int mRowCount = 0;


    	public AccountsAdapter(Context context, AccountRow[] accountRows ) {
    		super();
    		initData(accountRows);
    		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}

    	private void initData(AccountRow[] accountRows) {
    		if (accountRows == null) {
    			mRowCount = 0;
    			mData = null;
    			return;
    		}
    		mRowCount = accountRows.length ;
     		mData = accountRows ;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		if (position >= mRowCount) {
    			return null;
    		}
     		int color = Utils.getDisplayColorFromColor(mData[position].color);
    		View view;
    		if (convertView == null) {
    			view = mInflater.inflate(LAYOUT, parent, false);
    		} else {
    			view = convertView;
    		}
    		
            View colorBar = view.findViewById(R.id.color);
            if (colorBar != null) {
                colorBar.setBackgroundColor(color);
            }

            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                name.setText(mData[position].displayName);

                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                    accountName.setText(mData[position].displayName);
                    accountName.setVisibility(TextView.VISIBLE);
                }
            }

    		view.setTag(mData[position]);

    		return view;
    	}

    	@Override
    	public int getCount() {
    		return mRowCount;
    	}

    	@Override
    	public Object getItem(int position) {
    		if (position >= mRowCount) {
    			return null;
    		}
    		AccountRow item = mData[position];
    		return item;
    	}

    	@Override
    	public long getItemId(int position) {
    		if (position >= mRowCount) {
    			return 0;
    		}
    		return position ;
    	}
    	
    	public int searchPositionById( String searchedId ) {
        	int accountsIdx = -1 ;
        	for( AccountRow ar : mData ) {
        		accountsIdx++ ;
        		if( ar.id.equals(searchedId) ) {
        			return accountsIdx ;
        		}
        	}
        	return -1 ;
    	}
    }
   
    
    
    
    private void setDate(TextView view, long millis) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
                | DateUtils.FORMAT_ABBREV_WEEKDAY;

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String dateString;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(mTimezone));
            dateString = DateUtils.formatDateTime(mActivity, millis, flags);
            // setting the default back to null restores the correct behavior
            TimeZone.setDefault(null);
        }
        view.setText(dateString);
    }

    private void setTime(TextView view, long millis) {
        int flags = DateUtils.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(mActivity)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        // Unfortunately, DateUtils doesn't support a timezone other than the
        // default timezone provided by the system, so we have this ugly hack
        // here to trick it into formatting our time correctly. In order to
        // prevent all sorts of craziness, we synchronize on the TimeZone class
        // to prevent other threads from reading an incorrect timezone from
        // calls to TimeZone#getDefault()
        // TODO fix this if/when DateUtils allows for passing in a timezone
        String timeString;
        synchronized (TimeZone.class) {
            TimeZone.setDefault(TimeZone.getTimeZone(mTimezone));
            timeString = DateUtils.formatDateTime(mActivity, millis, flags);
            TimeZone.setDefault(null);
        }
        view.setText(timeString);
    }
	
    /**
     * Configures the Calendars (Accounts) spinner.  This is only done for new events, because only new
     * events allow you to select a calendar while editing an event.
     * <p>
     * We tuck a reference to a Cursor with calendar database data into the spinner, so that
     * we can easily extract calendar-specific values when the value changes (the spinner's
     * onItemSelected callback is configured).
     */
    public void setAccountsData( AccountRow[] accountRows ) {
    	Log.w(TAG,"Spinner size = "+accountRows.length ) ;
    	
        // populate the calendars spinner
        AccountsAdapter adapter = new AccountsAdapter(mActivity, accountRows);
        mCalendarsSpinner.setAdapter(adapter);
        // mCalendarsSpinner.setSelection(defaultCalendarPosition);
        mCalendarsSpinner.setOnItemSelectedListener(this);
    }
    
    public void setModel(CrmEventModel model) {
        mModel = model;
        
        Log.w(TAG,"Dams : Setting model") ;

        if (model == null) {
            // Display loading screen
            mLoadingMessage.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            return;
        }
        
        long begin = model.mStart;
        long end = model.mEnd;
        // Set up the starting times
        if (begin > 0) {
            mStartTime.timezone = mTimezone;
            mStartTime.set(begin);
            mStartTime.normalize(true);
        }
        if (end > 0) {
            mEndTime.timezone = mTimezone;
            mEndTime.set(end);
            mEndTime.normalize(true);
        }
        adjustSpinner() ;
        populateWhen();
        populateCrmFields() ;
        updateView();
        mScrollView.setVisibility(View.VISIBLE);
        mLoadingMessage.setVisibility(View.GONE);
        mAllDayRow.setVisibility(View.GONE);
    }
    
    private void adjustSpinner() {
    	if( mModel.mAccountEntry != null ) {
    		int position = ((AccountsAdapter)mCalendarsSpinner.getAdapter()).searchPositionById(mModel.mAccountEntry.entryKey) ;
    		if( position > -1 ) {
    			mCalendarsSpinner.setSelection(position);
    		}
    	}
    }
    
    // Fills in the date and time fields
    private void populateWhen() {
        long startMillis = mStartTime.toMillis(false /* use isDst */);
        long endMillis = mEndTime.toMillis(false /* use isDst */);
        setDate(mStartDateButton, startMillis);
        setDate(mEndDateButton, endMillis);

        setTime(mStartTimeButton, startMillis);
        setTime(mEndTimeButton, endMillis);

        mStartDateButton.setOnClickListener(new DateClickListener(mStartTime));
        mEndDateButton.setOnClickListener(new DateClickListener(mEndTime));

        mStartTimeButton.setOnClickListener(new TimeClickListener(mStartTime));
        mEndTimeButton.setOnClickListener(new TimeClickListener(mEndTime));
    }
    private void populateCrmFields() {
    	// ***** Detach de toutes les fields CRM ****
    	if( mCrmFieldViews != null ){
    		for( View v : mCrmFieldViews ) {
    			if( v.getParent() != null ) {
    				((LinearLayout)v.getParent()).removeView(v);
    			}
    		}
    		mCrmFieldViews.clear() ;
    	}
    	else {
    		mCrmFieldViews = new ArrayList<View>();
    	}
    	// Fin du detach 
    	
    	LayoutInflater inflater = mActivity.getLayoutInflater() ;
    	View newView ;
    	int crmFieldsIndex = 0 ;
    	for( CrmFileFieldDesc fd : mModel.mCrmFields ) {
    		CrmFileFieldValue fv = mModel.mCrmValues.get(crmFieldsIndex) ;
    		if( fv == null ) {
    			continue ;
    		}
    		
    		switch( fd.fieldType ) {
    		case FIELD_BIBLE :
    	    	newView = inflater.inflate(R.layout.calendar_editevent_crmfield_bible,null) ;
    	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
    	    	((Button)newView.findViewById(R.id.crm_button)).setOnClickListener(new BibleClickListener(new BibleCode(fd.fieldLinkBible),crmFieldsIndex));
    	    	if( fv.isSet ) {
    	    		((Button)newView.findViewById(R.id.crm_button)).setText(fv.displayStr) ;
    	    	}
    	    	mCrmFieldViews.add(newView) ;
    	    	mViewgroupTable.addView(newView) ;
    			break ;
    			
    		case FIELD_TEXT :
    	    	newView = inflater.inflate(R.layout.calendar_editevent_crmfield_text,null) ;
    	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
    	    	if( fv.isSet ) {
    	    		((TextView)newView.findViewById(R.id.crm_text)).setText(fv.displayStr) ;
    	    	}
    	    	mCrmFieldViews.add(newView) ;
    	    	mViewgroupTable.addView(newView) ;
    			break ;
    			
    		case FIELD_NUMBER :
    	    	newView = inflater.inflate(R.layout.calendar_editevent_crmfield_number,null) ;
    	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
    	    	if( fv.isSet ) {
    	    		((TextView)newView.findViewById(R.id.crm_text)).setText(fv.displayStr) ;
    	    	}
    	    	mCrmFieldViews.add(newView) ;
    	    	mViewgroupTable.addView(newView) ;
    			break ;
    			
    		default:
    			newView = new View(mActivity);
    			newView.setVisibility(View.GONE) ;
    	    	mCrmFieldViews.add(newView) ;
    	    	mViewgroupTable.addView(newView) ;
    			break ;
    		}
    		crmFieldsIndex++ ;
    	}
    }
    private void updateView(){
        mCalendarSelectorGroup.setVisibility(View.VISIBLE);
        mCalendarStaticGroup.setVisibility(View.GONE);
    }
	
	
	public boolean prepareForSave(){
		// Log.w(TAG,"Prepare for save !!") ;
		fillModelFromUI() ;
		return true ;
	}
	private boolean fillModelFromUI(){
		
		// Champs spécifiques : dates (start/end) + accounts => capture en direct (date/spinner listeners) 
		
		// Champs dynamiques CRM
		int crmFieldIndex = 0 ;
		String capturedStr ;
		for( CrmFileFieldDesc fd : mModel.mCrmFields ) {
			CrmFileFieldValue record = mModel.mCrmValues.get(crmFieldIndex) ;
			
			switch( fd.fieldType ) {
			case FIELD_BIBLE :
				// bible capturée en direct : BibleListener => rien à faire
				break ;
				
			case FIELD_TEXT :
				capturedStr = ((EditText)mCrmFieldViews.get(crmFieldIndex).findViewById(R.id.crm_text)).getText().toString() ;
				record.valueString = capturedStr ;
				record.displayStr = capturedStr ;
				if( capturedStr.length() > 0 ) {
					record.isSet = true ;
				}
				else {
					record.isSet = false ;
				}
				break ;
				
			case FIELD_NUMBER :
				capturedStr = ((EditText)mCrmFieldViews.get(crmFieldIndex).findViewById(R.id.crm_text)).getText().toString() ;
				float num = Float.parseFloat(capturedStr) ;
				record.valueFloat = num ;
				if( num==Math.ceil(num) ) {
					record.displayStr = String.valueOf((int)num) ;
				}
				else {
					record.displayStr = String.valueOf(num) ;
				}
				if( capturedStr.length() > 0 ) {
					record.isSet = true ;
				}
				else {
					record.isSet = false ;
				}
				break ;
				
			default :
				// non supporté => skip
				break ;
			}
			
			crmFieldIndex++ ;
		}
		
		return true ;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if( parent == mCalendarsSpinner ) {
			if( view.getTag() != null ) {
				mModel.mAccountEntry = ((AccountRow)view.getTag()).be ;
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

}
