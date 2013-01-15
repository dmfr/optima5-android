package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.SyncBroadcastReceiver;
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
    
    private class TestSync extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( pr != null ) {
    			Log.w(LOGTAG,"Finished log "+pr.fileCode) ;
    		} else {
    			Log.w(LOGTAG,"Finished simple push") ;
    		}
    	}
    }
    private class TestRefresh implements RefreshManager.RefreshListener {
    	public void onRefreshStart() {
   			Log.w(LOGTAG,"Starting REFRESH ") ;
    	}
    	public void onRefreshFileChanged( String refreshedFileCode ) {
    		if( refreshedFileCode != null ) {
    			Log.w(LOGTAG,"Finished REFRESH "+refreshedFileCode) ;
    		} else {
    			Log.w(LOGTAG,"Finished simple push") ;
    		}
    	}
    }

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
            mUIController.open(viewContext,"",0) ;
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
	
	
	
	// ************ DAMS : Dummy tests ***************
	
    private void myTest3() {
    	BibleHelper bh = new BibleHelper(getApplicationContext()) ;
    	BibleHelper.BibleEntry be2 = null ;
    	for( BibleHelper.BibleEntry be : bh.queryBible("STORE", null, "CHELLES", 0) ) {
    		be2 = be ;
    	}
    	if( be2 == null ) {
    		return ;
    	}
    	Log.w(LOGTAG,"Dugged "+be2.entryKey+" == "+be2.displayStr) ;
    	
    	TestRefresh tr = new TestRefresh() ;
    	
    	RefreshManager rm = RefreshManager.getInstance(getApplicationContext()) ;
    	rm.registerListener(tr) ;
    	rm.refreshFileList("STORE_LOG", be2) ;
    }
    
    
    private void myTest2() {
    	CrmFileManager fileManager = CrmFileManager.getInstance(getApplicationContext()) ;
    	fileManager.fileInitDescriptors() ;
    	
    	for( CrmFileManager.CrmFileDesc cfd : fileManager.fileGetRootDescriptors() ) {
    		Log.w(LOGTAG, cfd.fileCode+" / "+cfd.fileName) ;
    		myTest2PrintFields(cfd) ;
    		for( CrmFileManager.CrmFileDesc cfd2 : fileManager.fileGetChildDescriptors(cfd.fileCode) ) {
    			Log.w(LOGTAG, cfd.fileCode+" > "+cfd2.fileCode+" / "+cfd2.fileName) ;
    			myTest2PrintFields(cfd2) ;
    		}
    	}
    	
    }
    private void myTest2PrintFields( CrmFileManager.CrmFileDesc cfd ) {
    	for( CrmFileManager.CrmFileFieldDesc cffd : cfd.fieldsDesc ) {
    		Log.w(LOGTAG,"    --> "+cffd.fieldCode+" = "+cffd.fieldName) ;
    	}
    }
    private void myTest() {
    	// Log.w(LOGTAG,"Activity created") ;
    	
    	TestSync ts = new TestSync() ;
    	
    	
    	SyncServiceController ssc = SyncServiceController.getInstance(this) ;
    	ssc.addResultCallback(ts) ;
    	
    	ssc.requestPush() ;
    	
    	SyncPullRequest pr ;
    	SyncPullRequest pr1 ;
    	SyncPullRequest pr2 ;
    	
    	pr1 = new SyncPullRequest() ;
    	pr1.fileCode = "AGENDA_CS" ;
    	pr2 = new SyncPullRequest() ;
    	pr2.fileCode = "AGENDA_CS" ;
    	ssc.requestPull(pr1) ;
    	
    	SyncPullRequest.SyncPullRequestFileCondition prfc1 = new SyncPullRequest.SyncPullRequestFileCondition() ;
    	prfc1.fileFieldCode = "test" ;
    	prfc1.conditionSign = "test2" ;
    	prfc1.conditionValue = "test3" ;
    	SyncPullRequest.SyncPullRequestFileCondition prfc2 = new SyncPullRequest.SyncPullRequestFileCondition() ;
    	prfc2.fileFieldCode = "test" ;
    	prfc2.conditionSign = "test9" ;
    	prfc2.conditionValue = "test3" ;
    	
    	
    	pr1 = new SyncPullRequest() ;
    	pr1.fileCode = "STORE_LOG" ;
    	pr1.fileConditions = new ArrayList<SyncPullRequest.SyncPullRequestFileCondition>() ;
    	pr1.fileConditions.add(prfc1) ;
    	pr1.limitResults = 50 ;
    	pr1.supplyTimestamp = true ;
    	pr2 = new SyncPullRequest() ;
    	pr2.fileCode = "STORE_LOG" ;
    	pr2.fileConditions = new ArrayList<SyncPullRequest.SyncPullRequestFileCondition>() ;
    	pr2.fileConditions.add(prfc2) ;
    	pr2.limitResults = 50 ;
    	pr2.supplyTimestamp = true ;
    	ssc.requestPull(pr2) ;
    	
    	if( pr1.equals(pr2) ) {
    		Log.w(LOGTAG,"pr1 + pr2 are equals !!!") ;
    	} else {
    		Log.w(LOGTAG,"pr1 + pr2 are NOTTTTT equals !!!") ;
    	}
    }

}
