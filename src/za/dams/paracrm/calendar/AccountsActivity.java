package za.dams.paracrm.calendar;

import java.util.List;

import za.dams.paracrm.R;
import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class AccountsActivity extends PreferenceActivity {
	private static final String TAG = "Calendar/AccountsActivity";
	
	ActionBar mActionBar ;
	
    // ******** Fields for PARACRM ********
    private static final String BUNDLE_KEY_CRM_ID = "crmId";
    private static final String BUNDLE_KEY_BIBLECODE = "bibleCode";
    private int mCrmInputId ;
    private CrmCalendarManager mCrmCalendarManager ;

    @Override
    protected void onCreate(Bundle icicle) {
    	Bundle savedIcicle = icicle ;
    	
        if( icicle == null ){
        	icicle = this.getIntent().getExtras();
        }
        
        if (icicle != null && icicle.containsKey(BUNDLE_KEY_CRM_ID)) {
        	mCrmInputId = icicle.getInt(BUNDLE_KEY_CRM_ID);
        	
        	mCrmCalendarManager = new CrmCalendarManager( getApplicationContext(), mCrmInputId ) ;
        }

    	super.onCreate(savedIcicle);
    }
    
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.calendar_settings_headers, target);
        
        if( mCrmCalendarManager != null 
        		&& mCrmCalendarManager.isValid() 
        		&& mCrmCalendarManager.getCalendarInfos().mAccountIsOn ){
        	
            Header accountHeader = new Header();
            accountHeader.title = mCrmCalendarManager.getCalendarInfos().mCrmAgendaLib ;
            accountHeader.fragment =
                    "za.dams.paracrm.calendar.AccountSubscribeFragment";

            Bundle args = new Bundle();
            args.putString(BUNDLE_KEY_BIBLECODE, mCrmCalendarManager.getCalendarInfos().mAccountSrcBibleCode);
            accountHeader.fragmentArguments = args;
            target.add(accountHeader);
        }

    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(this,mCrmInputId);
                return true;
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
    
}
