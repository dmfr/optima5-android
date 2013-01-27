package za.dams.paracrm.explorer;

import za.dams.paracrm.R;
import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncServiceController;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ExplorerActivity extends Activity implements View.OnClickListener, FragmentInstallable {
	
	static final String LOGTAG = "Explorer/ExplorerActivity"; 
	
    private ExplorerController mUIController;
    
    private SyncServiceController mController;
    private SyncServiceController.Result mControllerResult;
    private final String ERROR_MESSAGE = "No internet connectivity. Remote queries/files not available" ;
    private ExplorerBanner mErrorBanner;
    

    private void initUIController() {
    	/*
        mUIController = UiUtilities.useTwoPane(this)
                ? new UIControllerTwoPane(this) : new UIControllerOnePane(this);
        */
    	mUIController = new ExplorerController(this) ;
    }
    protected void onCreate(Bundle savedInstanceState) {
        if (Explorer.DEBUG) Log.d(LOGTAG, this + " onCreate");

        // UIController is used in onPrepareOptionsMenu(), which can be called from within
        // super.onCreate(), so we need to initialize it here.
        initUIController();

        super.onCreate(savedInstanceState);
        setContentView(mUIController.getLayoutId());

        mUIController.onActivityViewReady();

        mController = SyncServiceController.getInstance(this);
        mControllerResult = new ControllerResult();
        mController.addResultCallback(mControllerResult);

        // Set up views
        // TODO Probably better to extract mErrorMessageView related code into a separate class,
        // so that it'll be easy to reuse for the phone activities.
        TextView errorMessage = (TextView) findViewById(R.id.error_message);
        errorMessage.setOnClickListener(this);
        int errorBannerHeight = getResources().getDimensionPixelSize(R.dimen.error_message_height);
        mErrorBanner = new ExplorerBanner(this, errorMessage, errorBannerHeight);

        if (savedInstanceState != null) {
            mUIController.onRestoreInstanceState(savedInstanceState);
        } else {
        	// @DAMS TODO : récupérer le dernier ExplorerContext dans mController
        	
            ExplorerContext viewContext = ExplorerContext.forNone() ;
            mUIController.open(viewContext,"",0,0) ;
        }
        mUIController.onActivityCreated();
    }
    

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onSaveInstanceState");
        }
        super.onSaveInstanceState(outState);
        mUIController.onSaveInstanceState(outState);
    }

    // FragmentInstallable
    @Override
    public void onInstallFragment(Fragment fragment) {
    	if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onInstallFragment fragment=" + fragment);
        }
        mUIController.onInstallFragment(fragment);
    }

    // FragmentInstallable
    @Override
    public void onUninstallFragment(Fragment fragment) {
    	if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onUninstallFragment fragment=" + fragment);
        }
        mUIController.onUninstallFragment(fragment);
    }

    @Override
    protected void onStart() {
    	if (Explorer.DEBUG)  Log.d(LOGTAG, this + " onStart");
        super.onStart();
        mUIController.onActivityStart();
        //myTest() ;
    	//myTest2() ;
    	//myTest3() ;
    }

    @Override
    protected void onResume() {
    	if (Explorer.DEBUG)  Log.d(LOGTAG, this + " onResume");
        super.onResume();
        mUIController.onActivityResume();
        /**
         * In {@link MessageList#onResume()}, we go back to {@link Welcome} if an account
         * has been added/removed. We don't need to do that here, because we fetch the most
         * up-to-date account list. Additionally, we detect and do the right thing if all
         * of the accounts have been removed.
         */
    }

    @Override
    protected void onPause() {
    	if (Explorer.DEBUG)  Log.d(LOGTAG, this + " onPause");
        super.onPause();
        mUIController.onActivityPause();
    }

    @Override
    protected void onStop() {
    	if (Explorer.DEBUG)  Log.d(LOGTAG, this + " onStop");
        super.onStop();
        mUIController.onActivityStop();
    }

    @Override
    protected void onDestroy() {
    	if (Explorer.DEBUG)  Log.d(LOGTAG, this + " onDestroy");
        mController.removeResultCallback(mControllerResult);
        // mTaskTracker.cancellAllInterrupt(); //@DAMS
        mUIController.onActivityDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
    	if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onBackPressed");
        }
        if (!mUIController.onBackPressed(true)) {
            // Not handled by UIController -- perform the default. i.e. close the app.
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mUIController.onCreateOptionsMenu(getMenuInflater(), menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return mUIController.onPrepareOptionsMenu(getMenuInflater(), menu);
    }
	@Override
    public boolean onSearchRequested() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onSearchRequested");
        }
        mUIController.onSearchRequested();
        return true; // Event handled.
    }
    @Override
    @SuppressWarnings("deprecation")
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mUIController.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onClick(View v) {
        switch (v.getId()) {
        case R.id.error_message:
            dismissErrorMessage();
            break;
        }
	}
    private void dismissErrorMessage() {
        mErrorBanner.dismiss();
    }
	
	
	
	private class ControllerResult extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( status != 0 ) { // connection not working
    			mErrorBanner.show(ERROR_MESSAGE) ;
    		} else {
    			dismissErrorMessage() ;
    		}
    	}
	}
}
