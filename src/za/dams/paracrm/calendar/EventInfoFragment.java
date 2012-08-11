package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.R;
import za.dams.paracrm.calendar.CalendarController.EventInfo;
import za.dams.paracrm.calendar.CalendarController.EventType;
import za.dams.paracrm.calendar.CrmCalendarManager.CrmCalendarInput;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class EventInfoFragment extends DialogFragment
	implements CalendarController.EventHandler, DeleteEventHelper.DeleteNotifyListener {

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
    private DeleteEventHelper mDeleteHelper;

    // Used to prevent saving changes in event if it is being deleted.
    private boolean mEventDeletionStarted = false;

    private TextView mTitle;
    private TextView mWhenDate;
    private TextView mWhenTime;
    private TextView mWhere;
    private Menu mMenu = null;
    private View mHeadlines;
    private ScrollView mScrollView;
    
    private ViewGroup mViewgroupTable ;
    private ArrayList<View> mCrmFieldViews ;
    
    

    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");

    private int mColor;

    EventLoadTask mLoadTask ;


    private Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            updateEvent(mView);
        }
    };

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
			
			if (mEventId != -1) {
				int eventId = (int)mEventId ;
				
				/// ******* Appel à CrmCalendarManager to load CrmEventModel *******
				CrmCalendarInput crmCalendarInput = CrmCalendarManager.queryInputFromEvent(mContext, eventId) ;
				if( crmCalendarInput == null ){
					return null ;
				}
				mModel = new CrmEventModel( mContext ) ; 
				mCrmCalendarManager = new CrmCalendarManager( mContext, crmCalendarInput.mCrmAgendaId ) ;
				if( mCrmCalendarManager == null ) {
					return null ;
				}
				mCrmCalendarManager.populateModelLoad(mModel, eventId) ;
				if( mModel.mAccountEntry != null ) {
					//Log.w(TAG,"Example : account is "+mModel.mAccountEntry.displayStr) ;
				}
				
				Map<String,Integer> presets = PrefsCrm.getAccountsColor(getActivity(), crmCalendarInput.mCrmAgendaId ) ;
				if( mCrmCalendarManager.getCalendarInfos().mAccountIsOn ) {
					mColor = presets.get(mModel.mAccountEntry.entryKey) ;
				}
				else if( presets.size() > 0 ) {
					mColor = presets.get("&") ;
				}
				if( mColor == 0 ) {
					mColor = Color.WHITE ;
				}
			}
			else{
				return null ;
			}
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
			updateCalendar(mView);
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
        
        mViewgroupTable = (ViewGroup)mView.findViewById(R.id.calendar_editevent_table);
        
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
                mDeleteHelper = new DeleteEventHelper(
                        mContext, mActivity,
                        !mIsDialog && !mIsTabletConfig ); // mIsTabletConfig=exitWhenDone
                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, onDeleteRunnable);
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
            if( mIsDialog ){
            	((CalendarActivity)mActivity).eventsChanged() ;
            }
        }
    };

    private void updateTitle() {
    	Resources res = getActivity().getResources();
    	getActivity().setTitle("Event Informations");
    }
    
    
    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }
    private void updateCalendar( View view ){
    	if( true ) {
            Button b = (Button) mView.findViewById(R.id.edit);
            b.setEnabled(true);
            b.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    doEdit();
                    // For dialogs, just close the fragment
                    // For full screen, close activity on phone, leave it for tablet
                    if (mIsDialog) {
                        EventInfoFragment.this.dismiss();
                    }
                    /*
                    else if (!mIsTabletConfig){
                        getActivity().finish();
                    }
                    */
                }
            });
    	}
    }
    private void updateEvent(View view) {
        if (mModel == null || view == null) {
            return;
        }

        mHeadlines.setBackgroundColor(mColor);
        
        // *** Crm Agenda Title *** 
        setTextCommon(view, R.id.title, mCrmCalendarManager.getCalendarInfos().mCrmAgendaLib);

        // *** When ? ***
        mAllDay = mModel.mAllDay ;
        String whenDate;
        int flagsTime = DateUtils.FORMAT_SHOW_TIME;
        int flagsDate = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                DateUtils.FORMAT_SHOW_YEAR;

        if (DateFormat.is24HourFormat(getActivity())) {
            flagsTime |= DateUtils.FORMAT_24HOUR;
        }
        if (mAllDay) {
            Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
            whenDate = DateUtils.formatDateRange(getActivity(), f, mStartMillis, mEndMillis,
                    flagsDate, Time.TIMEZONE_UTC).toString();
            setTextCommon(view, R.id.when_date, whenDate);
            view.findViewById(R.id.when_time).setVisibility(View.GONE);

        } else {
            // Show date for none all-day events
            whenDate = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis, flagsDate);
            String whenTime = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis,
                    flagsTime);
            setTextCommon(view, R.id.when_date, whenDate);
            setTextCommon(view, R.id.when_time, whenTime);
        }
        
        // *** Account ? ***
        if( mCrmCalendarManager.getCalendarInfos().mAccountIsOn && mModel.mAccountEntry != null ) {
        	setTextCommon(view, R.id.where, mModel.mAccountEntry.displayStr );
        }
        else {
        	view.findViewById(R.id.where).setVisibility(View.GONE);
        }
        
        
        
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
    	int crmFieldsIndex = -1 ;
    	for( CrmFileFieldDesc fd : mModel.mCrmFields ) {
    		crmFieldsIndex++ ;
    		CrmFileFieldValue fv = mModel.mCrmValues.get(crmFieldsIndex) ;
    		if( fv == null ) {
    			continue ;
    		}
    		
	    	newView = inflater.inflate(R.layout.calendar_eventinfo_crmfield,null) ;
	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
	    	((TextView)newView.findViewById(R.id.crm_text)).setText(fv.displayStr) ;
	    	mCrmFieldViews.add(newView) ;
	    	mViewgroupTable.addView(newView) ;
    	}
        
        
        
    }
    private void doEdit() {
        Context c = getActivity();
        // This ensures that we aren't in the process of closing and have been
        // unattached already
        if (c != null) {
            CalendarController.getInstance(c).sendEventRelatedEvent(
                    this, EventType.EDIT_EVENT, mEventId, mStartMillis, mEndMillis, 0
                    , 0, -1);
        }
    }
    @Override
    public void eventsChanged() {
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.EVENTS_CHANGED ) {
            // reload the data
    		if( mLoadTask != null && !mLoadTask.isCancelled() ) {
    			mLoadTask.cancel(true) ;
    		}
    		mLoadTask = new EventLoadTask() ;
    		mLoadTask.execute() ;
        }

    }

	@Override
	public void onDeleteStarted() {
		mEventDeletionStarted = true;
	}
    private Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Since OnPause will force the dialog to dismiss , do
                        // not change the dialog status
                        if (!mIsPaused) {
                            mDeleteDialogVisible = false;
                        }
                    }
                };
    }

}
