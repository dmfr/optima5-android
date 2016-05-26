package za.dams.paracrm.calendar;

import java.util.Arrays;
import java.util.List;

import za.dams.paracrm.R;
import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class AccountsActivity extends PreferenceActivity {
	private static final String TAG = "Calendar/AccountsActivity";

    private static String[] authorizedFragments = new String[] {
            "za.dams.paracrm.calendar.AccountSubscribeDummyFragment",
            "za.dams.paracrm.calendar.AccountSubscribeFragment"
    };

    ActionBar mActionBar ;
	
    // ******** Fields for PARACRM ********
    private static final String BUNDLE_KEY_CALFILECODE = "fileCode";
    private static final String BUNDLE_KEY_CALFILELIB = "fileLib";
    private static final String BUNDLE_KEY_SRCBIBLECODE = "srcBibleCode";

    @Override
    protected void onCreate(Bundle icicle) {
    	Bundle savedIcicle = icicle ;
    	
        if( icicle == null ){
        	icicle = this.getIntent().getExtras();
        }

    	super.onCreate(savedIcicle);
    }

    @Override
    public boolean onIsMultiPane() {
        return true;
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.calendar_settings_headers, target);
        
        for( CrmCalendarManager.CrmCalendarInput cci : CrmCalendarManager.inputsList(getApplicationContext()) ){
        	
        	CrmCalendarManager tCrmCalendarManager = new CrmCalendarManager(getApplicationContext(), cci.mCrmInputId) ;
        	
    		if( tCrmCalendarManager.getCalendarInfos().mAccountIsOn ){
                Header accountHeader = new Header();
                accountHeader.title = tCrmCalendarManager.getCalendarInfos().mCrmAgendaLib ;
                accountHeader.fragment =
                        "za.dams.paracrm.calendar.AccountSubscribeFragment";

                Bundle args = new Bundle();
                args.putString(BUNDLE_KEY_CALFILECODE, tCrmCalendarManager.getCalendarInfos().mCrmAgendaFilecode);
                args.putString(BUNDLE_KEY_SRCBIBLECODE, tCrmCalendarManager.getCalendarInfos().mAccountSrcBibleCode);
                accountHeader.fragmentArguments = args;
                target.add(accountHeader);
            } else {
                Header accountHeader = new Header();
                accountHeader.title = tCrmCalendarManager.getCalendarInfos().mCrmAgendaLib ;
                accountHeader.fragment =
                        "za.dams.paracrm.calendar.AccountSubscribeDummyFragment";

                Bundle args = new Bundle();
                args.putString(BUNDLE_KEY_CALFILECODE, tCrmCalendarManager.getCalendarInfos().mCrmAgendaFilecode);
                args.putString(BUNDLE_KEY_CALFILELIB, tCrmCalendarManager.getCalendarInfos().mCrmAgendaLib);
                accountHeader.fragmentArguments = args;
                target.add(accountHeader);
            }
        }

    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(this);
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

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (Arrays.asList(authorizedFragments).contains(fragmentName) ) {
            return true;
        }
        return false;
    }
}
