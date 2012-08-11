package za.dams.paracrm.calendar;

import za.dams.paracrm.calendar.CrmCalendarManager.CrmCalendarInput;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class DeleteEventHelper {
    private final Activity mParent;
    private Context mContext;

    private long mStartMillis;
    private long mEndMillis;
    private CrmEventModel mModel;
    
    private AlertDialog mAlertDialog;
    private Dialog.OnDismissListener mDismissListener;
    
    /**
     * If true, then call finish() on the parent activity when done.
     */
    private boolean mExitWhenDone;
    // the runnable to execute when the delete is confirmed
    private Runnable mCallback;
    

    // private EventDeleteTask mDeleteTask;

    private DeleteNotifyListener mDeleteStartedListener = null;

    public interface DeleteNotifyListener {
        public void onDeleteStarted();
    }
    
	private class EventDeleteTask extends AsyncTask<Void, Void, Void> {
		protected Void doInBackground(Void... arg0) {
			CrmCalendarManager crmCalendarManager = new CrmCalendarManager( mContext, DeleteEventHelper.this.mModel.mCrmFileCode ) ;
			crmCalendarManager.doneDeleteModel( DeleteEventHelper.this.mModel ) ;
			
			
			return null;
		}
		protected void onPostExecute(Void arg0) {
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mParent.finish();
            }
		}
	}

    public DeleteEventHelper(Context context, Activity parentActivity, boolean exitWhenDone) {
        if (exitWhenDone && parentActivity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }

        mContext = context;
        mParent = parentActivity;
        mExitWhenDone = exitWhenDone;
    }

    public void setExitWhenDone(boolean exitWhenDone) {
        mExitWhenDone = exitWhenDone;
    }

    /**
     * This callback is used when a normal event is deleted.
     */
    private DialogInterface.OnClickListener mDeleteNormalDialogListener =
            new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            deleteStarted();
            new EventDeleteTask().execute() ;
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mParent.finish();
            }
        }
    };

    public void delete(long begin, long end, long eventId, Runnable callback) {
    	mCallback = callback;
    	delete(begin,end,eventId) ;
    }
    public void delete(long begin, long end, long eventId) {
        mStartMillis = begin;
        mEndMillis = end;
        
        // ***** Load du model ****
		CrmCalendarInput crmCalendarInput = CrmCalendarManager.queryInputFromEvent(mContext, (int)eventId) ;
		if( crmCalendarInput == null ){
			return ;
		}
		mModel = new CrmEventModel( mContext ) ; 
		CrmCalendarManager crmCalendarManager = new CrmCalendarManager( mContext, crmCalendarInput.mCrmAgendaId ) ;
		crmCalendarManager.populateModelLoad(mModel, (int)eventId) ;
        
        
        
        // mModel = model;
    	
        AlertDialog dialog = new AlertDialog.Builder(mContext)
        .setMessage("Delete this event ?")
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .setNegativeButton(android.R.string.cancel, null).create();

        // This is a normal event. Pop up a confirmation dialog.
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
        		mContext.getText(android.R.string.ok),
        		mDeleteNormalDialogListener);

        dialog.setOnDismissListener(mDismissListener);
        dialog.show();
        mAlertDialog = dialog;
    }

    public void setDeleteNotificationListener(DeleteNotifyListener listener) {
        mDeleteStartedListener = listener;
    }

    private void deleteStarted() {
        if (mDeleteStartedListener != null) {
            mDeleteStartedListener.onDeleteStarted();
        }
    }

    public void setOnDismissListener(Dialog.OnDismissListener listener) {
        if (mAlertDialog != null) {
            mAlertDialog.setOnDismissListener(listener);
        }
        mDismissListener = listener;
    }

    public void dismissAlertDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

}
