package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.calendar.EditEventView.AccountRow;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class EventInfoFragment extends DialogFragment {

    public static final String TAG = "EventInfoFragment";

    protected static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    protected static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    protected static final String BUNDLE_KEY_IS_DIALOG = "key_fragment_is_dialog";
    protected static final String BUNDLE_KEY_DELETE_DIALOG_VISIBLE = "key_delete_dialog_visible";
    protected static final String BUNDLE_KEY_WINDOW_STYLE = "key_window_style";
    protected static final String BUNDLE_KEY_ATTENDEE_RESPONSE = "key_attendee_response";

    private static final String PERIOD_SPACE = ". ";

    /**
     * These are the corresponding indices into the array of strings
     * "R.array.change_response_labels" in the resource file.
     */
    static final int UPDATE_SINGLE = 0;
    static final int UPDATE_ALL = 1;

    // Style of view
    public static final int FULL_WINDOW_STYLE = 0;
    public static final int DIALOG_WINDOW_STYLE = 1;

    private int mWindowStyle = DIALOG_WINDOW_STYLE;

    private View mView;

    private long mEventId = -1 ;

    private static float mScale = 0; // Used for supporting different screen densities

    private long mStartMillis;
    private long mEndMillis;
    private boolean mAllDay;

    private boolean mDeleteDialogVisible = false;
    // private DeleteEventHelper mDeleteHelper;

    // Used to prevent saving changes in event if it is being deleted.
    private boolean mEventDeletionStarted = false;

    private TextView mTitle;
    private TextView mWhenDate;
    private TextView mWhenTime;
    private TextView mWhere;
    private Menu mMenu = null;
    private View mHeadlines;
    private ScrollView mScrollView;

    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");

    private int mColor;

    EventLoadTask mLoadTask ;


    private Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            updateEvent(mView);
        }
    };

    private OnItemSelectedListener mReminderChangeListener;

    private static int mDialogWidth = 500;
    private static int mDialogHeight = 600;
    private static int DIALOG_TOP_MARGIN = 8;
    private boolean mIsDialog = false;
    private boolean mIsPaused = true;
    private boolean mDismissOnResume = false;
    private int mX = -1;
    private int mY = -1;
    private int mMinTop;         // Dialog cannot be above this location
    private boolean mIsTabletConfig;
    private Activity mActivity;
    private Context mContext;
	
    CrmCalendarManager mCrmCalendarManager ;
    CrmEventModel mModel ;
	
	
	private class EventLoadTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... arg0) {
			if(true){
				return null ;
			}
			
			if (mEventId != -1) {
				/// ******* Appel à CrmCalendarManager to load CrmEventModel *******


				/*
	                mModel.mId = mEvent.id;
	                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEvent.id);
				 */
			} 
			
	        if (mModel.mStart <= 0) {
	            // use a default value instead
	        	// mModel.mStart = mHelper.constructDefaultStartTime(System.currentTimeMillis());
	        }
	        if (mModel.mEnd < mModel.mStart) {
	            // use a default value instead
	        	mModel.mEnd = mModel.mStart + DateUtils.HOUR_IN_MILLIS ;
	        } 
	        
			
			if( mCrmCalendarManager.getCalendarInfos().mAccountIsOn ) {
				BibleHelper bh = new BibleHelper(mContext) ;
				List<BibleHelper.BibleEntry> entries = bh.queryBible(mCrmCalendarManager.getCalendarInfos().mAccountSrcBibleCode) ;

				String calendarFileCode = mCrmCalendarManager.getCalendarInfos().mCrmAgendaFilecode ;
				Map<String,Integer> presets = PrefsCrm.getAccountsColor(getActivity(), calendarFileCode) ;

				
			}
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
			updateEvent(mView);
		}
	}
	
    public EventInfoFragment(Context context, long eventId, long startMillis, long endMillis,
            boolean isDialog, int windowStyle) {

        if (isDialog) {
            Resources r = context.getResources();

            mDialogWidth = r.getInteger(R.integer.event_info_dialog_width);
            mDialogHeight = r.getInteger(R.integer.event_info_dialog_height);

            if (mScale == 0) {
                mScale = context.getResources().getDisplayMetrics().density;
                if (mScale != 1) {
                    mDialogWidth *= mScale;
                    mDialogHeight *= mScale;
                    DIALOG_TOP_MARGIN *= mScale;
                }
            }
        }
        mIsDialog = isDialog;

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mEventId = eventId;
        mStartMillis = startMillis;
        mEndMillis = endMillis;
        mWindowStyle = windowStyle;
    }

    // This is currently required by the fragment manager.
    public EventInfoFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
        }

        if (mIsDialog) {
            applyDialogParams();
        }
        mContext = getActivity();
    }
    
    private void applyDialogParams() {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams a = window.getAttributes();
        a.dimAmount = .4f;

        a.width = mDialogWidth;
        a.height = mDialogHeight;


        // On tablets , do smart positioning of dialog
        // On phones , use the whole screen

        if (mX != -1 || mY != -1) {
            a.x = mX - mDialogWidth / 2;
            a.y = mY - mDialogHeight / 2;
            if (a.y < mMinTop) {
                a.y = mMinTop + DIALOG_TOP_MARGIN;
            }
            a.gravity = Gravity.LEFT | Gravity.TOP;
        }
        window.setAttributes(a);
    }
    public void setDialogParams(int x, int y, int minTop) {
        mX = x;
        mY = y;
        mMinTop = minTop;
    }

    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        if (!mIsDialog) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
            mDeleteDialogVisible =
                savedInstanceState.getBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE,false);

        }

        if (mWindowStyle == DIALOG_WINDOW_STYLE) {
            mView = inflater.inflate(R.layout.calendar_eventinfo_dialog, container, false);
        } else {
            mView = inflater.inflate(R.layout.calendar_eventinfo, container, false);
        }
        mScrollView = (ScrollView) mView.findViewById(R.id.event_info_scroll_view);
        mTitle = (TextView) mView.findViewById(R.id.title);
        mWhenDate = (TextView) mView.findViewById(R.id.when_date);
        mWhenTime = (TextView) mView.findViewById(R.id.when_time);
        mWhere = (TextView) mView.findViewById(R.id.where);
        mHeadlines = mView.findViewById(R.id.event_info_headline);
        //mIsTabletConfig = Utils.getConfigBool(mActivity, R.bool.tablet_config);
        mIsTabletConfig = true ;

        if (mEventId == -1) {
            // restore event ID from bundle
            mEventId = savedInstanceState.getLong(BUNDLE_KEY_EVENT_ID);
            mStartMillis = savedInstanceState.getLong(BUNDLE_KEY_START_MILLIS);
            mEndMillis = savedInstanceState.getLong(BUNDLE_KEY_END_MILLIS);
        }

        // start loading the data
		if( mLoadTask != null && !mLoadTask.isCancelled() ) {
			mLoadTask.cancel(true) ;
		}
		mLoadTask = new EventLoadTask() ;
		mLoadTask.execute() ;

        Button b = (Button) mView.findViewById(R.id.delete);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	/*
                if (!mCanModifyCalendar) {
                    return;
                }
                mDeleteHelper = new DeleteEventHelper(
                        mContext, mActivity,
                        !mIsDialog && !mIsTabletConfig ); // mIsTabletConfig=exitWhenDone
                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
                */
            }});

        // Hide Edit/Delete buttons if in full screen mode on a phone
        if (!mIsDialog && !mIsTabletConfig || mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) {
            mView.findViewById(R.id.event_info_buttons_container).setVisibility(View.GONE);
        }


        return mView;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(BUNDLE_KEY_END_MILLIS, mEndMillis);
        outState.putBoolean(BUNDLE_KEY_IS_DIALOG, mIsDialog);
        outState.putInt(BUNDLE_KEY_WINDOW_STYLE, mWindowStyle);
        outState.putBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE, mDeleteDialogVisible);
    }
    @Override
    public void onDestroyView() {
        if (!mEventDeletionStarted) {
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onPause() {
        mIsPaused = true;
        super.onPause();
        // Remove event deletion alert box since it is being rebuild in the OnResume
        // This is done to get the same behavior on OnResume since the AlertDialog is gone on
        // rotation but not if you press the HOME key
        /*
        if (mDeleteDialogVisible && mDeleteHelper != null) {
            mDeleteHelper.dismissAlertDialog();
            mDeleteHelper = null;
        }
        */
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsPaused = false;
        if (mDismissOnResume) {
            //mHandler.post(onDeleteRunnable);
        }
        // Display the "delete confirmation" dialog if needed
        if (mDeleteDialogVisible) {
        	/*
            mDeleteHelper = new DeleteEventHelper(
                    mContext, mActivity,
                    !mIsDialog && !mIsTabletConfig);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            */
        }
    }
    
    
    

    private Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (EventInfoFragment.this.mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            if (EventInfoFragment.this.isVisible()) {
                EventInfoFragment.this.dismiss();
            }
        }
    };

    private void updateTitle() {
    	Resources res = getActivity().getResources();
    	getActivity().setTitle("Event Informations");
    }
    
    
    private void updateEvent(View view) {
        if (mModel == null || view == null) {
            return;
        }

        /*
        String eventName = mEventCursor.getString(EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = getActivity().getString(R.string.no_title_label);
        }

        mAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        String location = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
        String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        String eventTimezone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);

        mColor = Utils.getDisplayColorFromColor(mEventCursor.getInt(EVENT_INDEX_COLOR));
        mHeadlines.setBackgroundColor(mColor);

        // What
        if (eventName != null) {
            setTextCommon(view, R.id.title, eventName);
        }

        // When
        // Set the date and repeats (if any)
        String whenDate;
        int flagsTime = DateUtils.FORMAT_SHOW_TIME;
        int flagsDate = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                DateUtils.FORMAT_SHOW_YEAR;

        if (DateFormat.is24HourFormat(getActivity())) {
            flagsTime |= DateUtils.FORMAT_24HOUR;
        }

        // Put repeat after the date (if any)
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Time date = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
            if (mAllDay) {
                date.timezone = Time.TIMEZONE_UTC;
            }
            date.set(mStartMillis);
            eventRecurrence.setStartDate(date);
            repeatString = EventRecurrenceFormatter.getRepeatString(
                    getActivity().getResources(), eventRecurrence);
        }
        // If an all day event , show the date without the time
        if (mAllDay) {
            Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
            whenDate = DateUtils.formatDateRange(getActivity(), f, mStartMillis, mEndMillis,
                    flagsDate, Time.TIMEZONE_UTC).toString();
            if (repeatString != null) {
                setTextCommon(view, R.id.when_date, whenDate + " (" + repeatString + ")");
            } else {
                setTextCommon(view, R.id.when_date, whenDate);
            }
            view.findViewById(R.id.when_time).setVisibility(View.GONE);

        } else {
            // Show date for none all-day events
            whenDate = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis, flagsDate);
            String whenTime = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis,
                    flagsTime);
            if (repeatString != null) {
                setTextCommon(view, R.id.when_date, whenDate + " (" + repeatString + ")");
            } else {
                setTextCommon(view, R.id.when_date, whenDate);
            }

            // Show the event timezone if it is different from the local timezone after the time
            String localTimezone = Utils.getTimeZone(mActivity, mTZUpdater);
            if (!TextUtils.equals(localTimezone, eventTimezone)) {
                String displayName;
                // Figure out if this is in DST
                Time date = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
                if (mAllDay) {
                    date.timezone = Time.TIMEZONE_UTC;
                }
                date.set(mStartMillis);

                TimeZone tz = TimeZone.getTimeZone(localTimezone);
                if (tz == null || tz.getID().equals("GMT")) {
                    displayName = localTimezone;
                } else {
                    displayName = tz.getDisplayName(date.isDst != 0, TimeZone.LONG);
                }
                setTextCommon(view, R.id.when_time, whenTime + " (" + displayName + ")");
            }
            else {
                setTextCommon(view, R.id.when_time, whenTime);
            }
        }


        // Organizer view is setup in the updateCalendar method


        // Where
        if (location == null || location.trim().length() == 0) {
            setVisibilityCommon(view, R.id.where, View.GONE);
        } else {
            final TextView textView = mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                textView.setText(location.trim());
                linkifyTextView(textView);

                textView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            return v.onTouchEvent(event);
                        } catch (ActivityNotFoundException e) {
                            // ignore
                            return true;
                        }
                    }
                });
            }
        }

        // Description
        if (description != null && description.length() != 0) {
            mDesc.setText(description);
        }
        */
    }

}
