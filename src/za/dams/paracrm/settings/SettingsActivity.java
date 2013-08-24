package za.dams.paracrm.settings;

import java.util.List;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.MainPreferences;
import za.dams.paracrm.R;
import za.dams.paracrm.SyncServiceController;
import za.dams.paracrm.UploadServiceHelper;
import za.dams.paracrm.explorer.Explorer;
import za.dams.paracrm.explorer.xpressfile.Xpressfile;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements SettingsCallbacks {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mContext = getApplicationContext();
	}
	
    @Override
    public void onBuildHeaders(List<Header> target) {
        Header accountHeader = new Header();
        accountHeader.title = "Server/Domain" ;
        accountHeader.fragment = "za.dams.paracrm.settings.SettingsServerFragment";
        accountHeader.iconRes = R.drawable.ic_settings_wireless ;
        target.add(accountHeader);
    }
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	onBackPressed();
            	break ;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.settings_title_bar, menu);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        return true;
    }
    @Override
    public void onBackPressed(){
    	//this.finish() ;
    	super.onBackPressed() ;
    }

    
    
	enum ErrorStatus {
	    NO_ERROR, ERROR
	};
	
	protected ProgressDialog mProgressDialog;
	private Context mContext;
	private ErrorStatus status;
    
	public static final int MSG_ERR = 0;
	public static final int MSG_CNF = 1;
	public static final int MSG_IND = 2;
	
	final Handler mHandler = new Handler() {
	    public void handleMessage(Message msg) {
	        String text2display = null;
	        switch (msg.what) {
	        case MSG_IND:
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.setMessage(((String) msg.obj));
	            }
	            break;
	        case MSG_ERR:
	            text2display = (String) msg.obj;
	            Toast.makeText(mContext, "Error: " + text2display,
	                    Toast.LENGTH_LONG).show();
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.dismiss();
	            }
	            break;
	        case MSG_CNF:
	            text2display = (String) msg.obj;
	            Toast.makeText(mContext, "Info: " + text2display,
	                    Toast.LENGTH_LONG).show();
	            if (mProgressDialog.isShowing()) {
	                mProgressDialog.dismiss();
	            }
	            break;
	        default: // should never happen
	            break;
	        }
	    }
	};
	
    public void myClearDbAsk(){
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("This will clear the database ?")
    	       .setCancelable(true)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   SettingsActivity.this.myClearDb();
    	           }
    	       })
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
    }
	public void myClearDb() {
	    mProgressDialog = ProgressDialog.show(
	    		this,
	    		"Reset All",
	            "Erasing database...",
	            true);
	 
	    // useful code, variables declarations...
	    new Thread((new Runnable() {
	        @Override
	        public void run() {
	            Message msg = null;
	            /*
	            String progressBarData = "Erasing database...";
	            msg = mHandler.obtainMessage(MSG_IND, (Object) progressBarData);
	            mHandler.sendMessage(msg);
	            */
	 
	            // starts the second long operation
	            DatabaseManager mDbManager = DatabaseManager.getInstance(SettingsActivity.this.getApplicationContext()) ;
	            DatabaseManager.DatabaseUpgradeResult dbUpResult = mDbManager.purgeReferentiel() ;
	            if( dbUpResult.success == true ) {
	            	status = ErrorStatus.NO_ERROR ;
	            	
	            	SharedPreferences settings = getPreferences(MODE_PRIVATE);
	                SharedPreferences.Editor editor = settings.edit();
	                editor.putLong("bibleTimestamp", dbUpResult.versionTimestamp);
	                editor.putBoolean("appEnabled", false);
	                editor.commit();
	            }
	            else {
	            	status = ErrorStatus.ERROR ;
	            }

	            try {
	            	Thread.sleep(1000);
	            } catch (InterruptedException e) {
	            }

	            if (ErrorStatus.NO_ERROR != status) {
	            	msg = mHandler.obtainMessage(MSG_ERR,
	            			"error while building database");
	            	mHandler.sendMessage(msg);
	            } else {
	            	msg = mHandler.obtainMessage(MSG_CNF,
	            			"Database cleared");
	            	mHandler.sendMessage(msg);
	            }
	            
	        }

	    })).start();
	    // ...
	    
	    Explorer.clearContext() ;
	    Xpressfile.clearContext() ;
	}

	
	@Override
	public void OnServerChanged() {
		MainPreferences.clearInstance() ;
		myClearDb();
	}

	@Override
	public void OnRequestClearDb() {
		myClearDbAsk();
	}

	@Override
	public boolean IsLocalDbDirty() {
		if( SyncServiceController.hasPendingPush(mContext) || UploadServiceHelper.hasPendingUploads(mContext) ) {
			return true;
		}
		return false ;
	}
    
}
