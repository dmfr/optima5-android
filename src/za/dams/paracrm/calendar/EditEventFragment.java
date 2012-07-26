package za.dams.paracrm.calendar;


import za.dams.paracrm.R;
import za.dams.paracrm.calendar.CalendarController.EventType;
import za.dams.paracrm.calendar.CalendarController.EventHandler;
import za.dams.paracrm.calendar.CalendarController.EventInfo;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

public class EditEventFragment extends Fragment implements EventHandler {
	private static final String TAG = "Calendar/EditEventFragment";
	
	private EventInfo mEvent ;
	private boolean mIsReadOnly ;
	private Intent mIntent ;
	
	private Activity mContext;
	
	CrmEventModel mModel;
	EditEventView mView;
	
	EventLoadTask mLoadTask ;
	
	private InputMethodManager mInputMethodManager;
	
	public boolean mShowModifyDialogOnLaunch = false;
	private boolean mUseCustomActionBar = false ;
	private boolean mSaveOnDetach = true;
	
	
	private Runnable mOnDone = new Runnable(){
		public void run(){
			
		}
	};
	
	private class EventLoadTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... arg0) {
			
			
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
			setModelIfDone() ;
		}
	}
	private void startQuery() {
		if( mLoadTask != null && !mLoadTask.isCancelled() ) {
			mLoadTask.cancel(true) ;
		}
		mLoadTask = new EventLoadTask() ;
		mLoadTask.execute() ;
	}
	
    public EditEventFragment() {
        this(null, false, null);
    }

    public EditEventFragment(EventInfo event, boolean readOnly, Intent intent) {
        mEvent = event;
        mIsReadOnly = readOnly;
        mIntent = intent;
        setHasOptionsMenu(true);
    }
    
    
    private void setModelIfDone() {
        synchronized (this) {
        	mView.setModel(mModel);
        }
    }
    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;

        //mHelper = new EditEventHelper(activity, null);
        //mHandler = new QueryHandler(activity.getContentResolver());
        mModel = new CrmEventModel(activity, mIntent);
        mInputMethodManager = (InputMethodManager)
                activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        mUseCustomActionBar = false ;
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
//        mContext.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        View view;
        if (mIsReadOnly) {
            view = inflater.inflate(R.layout.calendar_editevent_singlecolumn, null);
        } else {
            view = inflater.inflate(R.layout.calendar_editevent, null);
        }
        mView = new EditEventView(mContext, view, mOnDone);
        startQuery(); // **** chargement des données ??? ***
        

        if (mUseCustomActionBar) {
        	// ******* Pas utilisé sur ParaCRM tablette
        	/*
            View actionBarButtons = inflater.inflate(R.layout.edit_event_custom_actionbar,
                    new LinearLayout(mContext), false);
            View cancelActionView = actionBarButtons.findViewById(R.id.action_cancel);
            cancelActionView.setOnClickListener(mActionBarListener);
            View doneActionView = actionBarButtons.findViewById(R.id.action_done);
            doneActionView.setOnClickListener(mActionBarListener);

            mContext.getActionBar().setCustomView(actionBarButtons);
            */
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUseCustomActionBar) {
            mContext.getActionBar().setCustomView(null);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
        	/*
            if (savedInstanceState.containsKey(BUNDLE_KEY_MODEL)) {
                mRestoreModel = (CalendarEventModel) savedInstanceState.getSerializable(
                        BUNDLE_KEY_MODEL);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_STATE)) {
                mModification = savedInstanceState.getInt(BUNDLE_KEY_EDIT_STATE);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_ON_LAUNCH)) {
                mShowModifyDialogOnLaunch = savedInstanceState
                        .getBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EVENT)) {
                mEventBundle = (EventBundle) savedInstanceState.getSerializable(BUNDLE_KEY_EVENT);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_READ_ONLY)) {
                mIsReadOnly = savedInstanceState.getBoolean(BUNDLE_KEY_READ_ONLY);
            }
            */
        }
    }

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (!mUseCustomActionBar) {
        	Log.w(TAG,"Inflating...") ;
            inflater.inflate(R.menu.calendar_editevent_titlebar, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }

    /**
     * Handles menu item selections, whether they come from our custom action bar buttons or from
     * the standard menu items. Depends on the menu item ids matching the custom action bar button
     * ids.
     *
     * @param itemId the button or menu item id
     * @return whether the event was handled here
     */
    private boolean onActionBarItemSelected(int itemId) {
        switch (itemId) {
            case R.id.action_done:
            	/*
                if (EditEventHelper.canModifyEvent(mModel) || EditEventHelper.canRespond(mModel)) {
                    if (mView != null && mView.prepareForSave()) {
                        if (mModification == Utils.MODIFY_UNINITIALIZED) {
                            mModification = Utils.MODIFY_ALL;
                        }
                        mOnDone.setDoneCode(Utils.DONE_SAVE | Utils.DONE_EXIT);
                        mOnDone.run();
                    } else {
                        mOnDone.setDoneCode(Utils.DONE_REVERT);
                        mOnDone.run();
                    }
                } else if (EditEventHelper.canAddReminders(mModel) && mModel.mId != -1
                        && mOriginalModel != null && mView.prepareForSave()) {
                    saveReminders();
                    mOnDone.setDoneCode(Utils.DONE_EXIT);
                    mOnDone.run();
                } else {
                    mOnDone.setDoneCode(Utils.DONE_REVERT);
                    mOnDone.run();
                }
                */
                break;
            case R.id.action_cancel:
            	/*
                mOnDone.setDoneCode(Utils.DONE_REVERT);
                mOnDone.run();
                */
                break;
        }
        return true;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        mView.prepareForSave();
        /*
        outState.putSerializable(BUNDLE_KEY_MODEL, mModel);
        outState.putInt(BUNDLE_KEY_EDIT_STATE, mModification);
        if (mEventBundle == null && mEvent != null) {
            mEventBundle = new EventBundle();
            mEventBundle.id = mEvent.id;
            if (mEvent.startTime != null) {
                mEventBundle.start = mEvent.startTime.toMillis(true);
            }
            if (mEvent.endTime != null) {
                mEventBundle.end = mEvent.startTime.toMillis(true);
            }
        }
        outState.putBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH, mShowModifyDialogOnLaunch);
        outState.putSerializable(BUNDLE_KEY_EVENT, mEventBundle);
        outState.putBoolean(BUNDLE_KEY_READ_ONLY, mIsReadOnly);
        */
    }
    
    
	@Override
	public long getSupportedEventTypes() {
		return EventType.USER_HOME;
	}

	@Override
	public void handleEvent(EventInfo event) {
        // It's currently unclear if we want to save the event or not when home
        // is pressed. When creating a new event we shouldn't save since we
        // can't get the id of the new event easily.
        if ((false && event.eventType == EventType.USER_HOME) || (event.eventType == EventType.GO_TO
                && mSaveOnDetach)) {
            if (mView != null && mView.prepareForSave()) {
                //mOnDone.setDoneCode(Utils.DONE_SAVE);
                //mOnDone.run();
            }
        }
	}

	@Override
	public void eventsChanged() {
		// TODO Auto-generated method stub
		
	}

}
