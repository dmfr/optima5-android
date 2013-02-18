package za.dams.paracrm.explorer.xpressfile;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.CrmFileManager;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

public class XpressfileActivity extends Activity {
	
    private static final String TAG = "Xpressfile/XpressfileActivity";

    public static final int MODE_RAWFILE = 1 ;
    public static final int MODE_XPRESSFILE = 2 ;
    
    public static final String BUNDLE_KEY_MODE = "key_mode";
    public static final String BUNDLE_KEY_XPRESSFILE_INPUTID = "key_xpressfileInputId";
    public static final String BUNDLE_KEY_XPRESSFILE_PRIMARYKEY = "key_xpressfilePrimarykey";
    public static final String BUNDLE_KEY_RAWFILE_FILECODE = "key_rawfileFilecode";
    public static final String BUNDLE_KEY_RAWFILE_FILERECORDID = "key_rawfileFilerecordId";

    private XpressfileFragment mXpressFragment;
    private XpressfileActivityTitleTask mTitleTask ;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.simple_frame_layout);
        
        mXpressFragment = (XpressfileFragment) getFragmentManager().findFragmentById(R.id.main_frame);
        
        Bundle bundle = this.getIntent().getExtras();
        
		// Set ActionBar
        /*
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);*/
        if( bundle.getInt(BUNDLE_KEY_MODE) == MODE_XPRESSFILE && !bundle.containsKey(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ) {
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Set title
        mTitleTask = new XpressfileActivityTitleTask() ;
        mTitleTask.execute(bundle) ;
        
    	// Create fragment
        if( mXpressFragment==null ) {
        	switch( bundle.getInt(BUNDLE_KEY_MODE) ) {
        	case MODE_RAWFILE :
        		mXpressFragment = XpressfileFragment.newInstanceRawfile(
        				bundle.getString(BUNDLE_KEY_RAWFILE_FILECODE),
        				bundle.containsKey(BUNDLE_KEY_RAWFILE_FILERECORDID) ? bundle.getInt(BUNDLE_KEY_RAWFILE_FILERECORDID) : -1 ) ;
        		break ;
        	case MODE_XPRESSFILE :
        		mXpressFragment = XpressfileFragment.newInstanceXpressfile(
        				bundle.getInt(BUNDLE_KEY_XPRESSFILE_INPUTID),
        				bundle.getString(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ) ;
        		break ;
        	default :
        		finish() ;
        		return ;
        	}
        	
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, mXpressFragment);
            ft.show(mXpressFragment);
            ft.commit();
        }
    }
    @Override
    protected void onDestroy() {
    	if( mTitleTask.getStatus() != AsyncTask.Status.FINISHED ) {
    		mTitleTask.cancel(true) ;
    	}
    	super.onDestroy() ;
    }
    
    @Override
    public void onBackPressed() {
    	if( mXpressFragment==null ) {
    		finish() ;
    	}
    	mXpressFragment.handleBackPressed() ;
    }
    
    private class XpressfileActivityTitleTask extends AsyncTask<Bundle,Void,String> {
    	Context context ;
    	
    	@Override
    	protected void onPreExecute() {
    		context = XpressfileActivity.this.getApplicationContext() ;
    	}

		@Override
		protected String doInBackground(Bundle... arg0) {
			if( arg0.length < 1 ) {
				return null ;
			}
			Bundle bundle = arg0[0] ;
			
			// Set title
			CrmFileManager cfm = CrmFileManager.getInstance(context) ;
			cfm.fileInitDescriptors() ;
			
	    	switch( bundle.getInt(BUNDLE_KEY_MODE) ) {
	    	case MODE_RAWFILE :
	    		String rawFilecode = bundle.getString(BUNDLE_KEY_RAWFILE_FILECODE) ;
	    		String rawFileLib = cfm.fileGetFileDescriptor(rawFilecode).fileName ;
	    		
	        	StringBuilder sb = new StringBuilder() ;
	        	sb.append(bundle.containsKey(BUNDLE_KEY_RAWFILE_FILERECORDID) ? "Create File Record" : "Modify File Record") ;
	       		sb.append(" / ") ;
	       		sb.append(rawFileLib) ;
	        	
	            return sb.toString() ;
	    		
	    	case MODE_XPRESSFILE :
	    		int xpressfileInputId = bundle.getInt(BUNDLE_KEY_XPRESSFILE_INPUTID) ;
	    		CrmXpressfileManager.CrmXpressfileInput cxi = CrmXpressfileManager.inputsList(context, xpressfileInputId).get(0);
	    		String title = cxi.mTargetLib ;
	    		CrmFileManager.CrmFileDesc cfd = cfm.fileGetFileDescriptor(cxi.mTargetFilecode) ;
	    		
	    		if( bundle.containsKey(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ) {
	    			CrmFileManager.CrmFileFieldDesc cffdPrimarykey = null ;
	    			for( CrmFileManager.CrmFileFieldDesc cfddTest : cfd.fieldsDesc ) {
	    				if( cfddTest.fieldCode.equals(cxi.mTargetPrimarykeyFileField) ) {
	    					cffdPrimarykey = cfddTest ;
	    				}
	    			}
	    			if( cffdPrimarykey != null ) {
	    				String primarykeyBibleCode = cffdPrimarykey.fieldLinkBible ;
	    				String primarykeyBibleEntryKey = bundle.getString(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ;
	    				BibleHelper bh = new BibleHelper(context);
	    				BibleHelper.BibleEntry be = bh.getBibleEntry(primarykeyBibleCode, primarykeyBibleEntryKey);
	    				if( be != null ) {
	    					title = be.displayStr ;
	    				}
	    			}
	    		}
	    		
	            return title ;
	    	}
			
			
			return null;
		}
    	
		protected void onPostExecute(String resultTitle) {
			if( isCancelled() ) {
				return ;
			}
			if( resultTitle != null ) {
				XpressfileActivity.this.getActionBar().setTitle(resultTitle) ;
			}
		}
		
        public XpressfileActivityTitleTask execute( Bundle bundle ) {
        	executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,bundle) ;
        	return this ;
        }
    }

}
