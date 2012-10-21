package za.dams.paracrm.calendar;


import java.util.List;
import java.util.Map;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.SyncServiceHelper;
import za.dams.paracrm.calendar.CalendarController.EventType;
import za.dams.paracrm.calendar.CalendarController.EventHandler;
import za.dams.paracrm.calendar.CalendarController.EventInfo;
import za.dams.paracrm.calendar.EditEventView.AccountRow;
import za.dams.paracrm.ui.FileCaptureActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

public class EditEventFragment extends Fragment 
	implements EventHandler, EditEventView.OnEditEventViewChangeListener {
	
	private static final String TAG = "Calendar/EditEventFragment";
	
	private EventInfo mEvent ;
	private boolean mIsReadOnly ;
	private Intent mIntent ;
	
	private Activity mContext;
	private CrmCalendarManager mCrmCalendarManager ;
	
	CrmEventModel mModel;
	EditEventView mView;
	
	EventLoadTask mLoadTask ;
	
	private InputMethodManager mInputMethodManager;
	
	public boolean mShowModifyDialogOnLaunch = false;
	private boolean mUseCustomActionBar = false ;
	private boolean mSaveOnDetach = true;
	
	ProgressDialog mProgressDialog ;
	
	
	private Runnable mOnDone = new Runnable(){
		public void run(){
			
		}
	};
	
	private class EventLoadTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... arg0) {
			
	        if (mEvent != null) {
	            if (mEvent.id != -1) {
	            	/// ******* Appel à CrmCalendarManager to load CrmEventModel *******
	            	//Log.w(TAG,"Trying to load eventId "+mEvent.id) ;
	            	mCrmCalendarManager.populateModelLoad(mModel, (int)mEvent.id) ;
	            } else {
	                // New event. All day?
	                mModel.mAllDay = (mEvent.extraLong == CalendarController.EXTRA_CREATE_ALL_DAY);
	            }
	            if (mEvent.startTime != null) {
	            	mModel.mStart = mEvent.startTime.toMillis(true);
	            }
	            if (mEvent.endTime != null) {
	            	mModel.mEnd = mEvent.endTime.toMillis(true);
	            }
	        }/* else if (mEventBundle != null) {
	            if (mEventBundle.id != -1) {
	                mModel.mId = mEventBundle.id;
	                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventBundle.id);
	            }
	            mBegin = mEventBundle.start;
	            mEnd = mEventBundle.end;
	        }*/
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

	    		int mRowCount = 0 ;
	    		for (BibleHelper.BibleEntry be : entries) {
	    			if( !presets.containsKey(be.entryKey) ) {
	    				continue ;
	    			}
	    			mRowCount++ ;
	    		}
	    		AccountRow[] accountRows = new AccountRow[mRowCount];
	    		int p = 0;
	    		for (BibleHelper.BibleEntry be : entries) {
	    			if( !presets.containsKey(be.entryKey) ) {
	    				continue ;
	    			}
	    			accountRows[p] = new AccountRow();
	    			accountRows[p].id = be.entryKey ;
	    			//mData[p].color = Color.BLUE ;
	    			accountRows[p].displayName = be.displayStr ;
	    			accountRows[p].color = presets.get(be.entryKey).intValue() ;
	    			accountRows[p].originalSynced = true ;
	    			accountRows[p].synced = true ;
	    			accountRows[p].be = be ;
	    			p++ ;
	    		}

				
				mView.setAccountsData( accountRows ) ;
			}
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
			setModelIfDone() ;
		}
	}
	private class EventRefreshTask extends AsyncTask<Void, Void, Void> {
    	protected void onPreExecute(){
    		EditEventFragment.this.mView.prepareForSave() ;
    	}
		protected Void doInBackground(Void... arg0) {
			EditEventFragment.this.mModel.mAllDay = mCrmCalendarManager.modelIsAllDay( mModel ) ;
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
			setModelIfDone() ;
		}
	}
	private class EventSaveTask extends AsyncTask<Void, Void, Boolean> {
    	protected void onPreExecute(){
    		if( EditEventFragment.this.isAdded() ) {
    		mProgressDialog = ProgressDialog.show(
    				EditEventFragment.this.getActivity(),
    	    		"Saving event",
    	            "Please wait...",
    	            true);
    		}
    	}
		
		protected Boolean doInBackground(Void... arg0) {
			try{
				Thread.sleep(1000) ;
			}
			catch(Exception e){
			}
			
			if( !EditEventFragment.this.asyncCheckModel()  ) {
				return false ;
			}
			EditEventFragment.this.asyncSaveModel() ;
			return true ;
		}
		protected void onPostExecute(Boolean success) {
			if( !EditEventFragment.this.isAdded() )
				return ;
			if( mProgressDialog != null ){
				mProgressDialog.dismiss() ;
			}
			if( !success ) {
	           	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
	        	builder.setMessage("Event incomplete. Please Check")
	        	       .setCancelable(false)
	        	       .setIcon(android.R.drawable.stat_notify_error)
	        	       .setTitle("Error")
	        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	                dialog.cancel();
	        	           }
	        	       });
	        	AlertDialog alert = builder.create();            
	        	alert.show();
	        	return ;
			}
			
			// @DAMS : build proper sync system
			SyncServiceHelper.launchSync( EditEventFragment.this.mContext ) ;
			
			Activity a = EditEventFragment.this.getActivity() ;
            if (a != null) {
                a.finish();
            }
		}
	}
	private synchronized boolean asyncCheckModel(){
		return mCrmCalendarManager.doneCheckModel(mModel) ;
	}
	private synchronized boolean asyncSaveModel(){
		return mCrmCalendarManager.doneSaveModel(mModel) ;
	}
	
	
	private void startQuery() {
		if( mLoadTask != null && !mLoadTask.isCancelled() ) {
			mLoadTask.cancel(true) ;
		}
		mLoadTask = new EventLoadTask() ;
		mLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
	}
	
    public EditEventFragment() {
        this(null, false, null);
    }

    public EditEventFragment(EventInfo event, boolean readOnly, Intent intent) {
    	// mCrmCalendarManager = crmCalendarManager ;
        mEvent = event;
        mIsReadOnly = readOnly;
        mIntent = intent;
        setHasOptionsMenu(true);
    }
    public void setCrmCalendarManager( CrmCalendarManager crmCalendarManager ){
    	mCrmCalendarManager = crmCalendarManager ;
    }
    
    private void setModelIfDone() {
        synchronized (this) {
        	CrmCalendarManager.CrmCalendarInput crmCalendarInput = CrmCalendarManager.queryInputFromFilecode(mContext, mCrmCalendarManager.getCalendarInfos().mCrmAgendaFilecode) ;
        	mView.setModel(crmCalendarInput,mModel);
        }
    }
    
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        
        if( mCrmCalendarManager == null || !mCrmCalendarManager.isValid() ) {
        	Log.e(TAG,"CrmCalendarManager missing or invalid !") ;
        	mCrmCalendarManager.isValid() ;
        }

        //mHelper = new EditEventHelper(activity, null);
        //mHandler = new QueryHandler(activity.getContentResolver());
        mModel = new CrmEventModel(activity, mIntent);
        mCrmCalendarManager.populateModelEmpty(mModel) ;
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
        mView.setViewChangeListener( this ) ;
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
        	//Log.w(TAG,"Inflating...") ;
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
            	if(mView != null && mView.prepareForSave()) {
            		new EventSaveTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
            	}
            	else {
            		
            	}
                break;
            case R.id.action_cancel:
                Activity a = EditEventFragment.this.getActivity();
                if (a != null) {
                    a.finish();
                }
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
	@Override
	public void onEditEventViewChanged() {
		// TODO Auto-generated method stub
		//Log.w(TAG,"Edit Event View changed !!!") ;
		new EventRefreshTask().execute() ;
	}

}
