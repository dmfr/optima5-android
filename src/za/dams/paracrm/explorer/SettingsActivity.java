package za.dams.paracrm.explorer;

import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity {
	private static final String TAG = "Explorer/SettingsActivity";
	
	ActionBar mActionBar ;
	
    // ******** Fields for PARACRM ********
    private static final String BUNDLE_KEY_ACCT_IS_ON = "acctIsOn";
    private static final String BUNDLE_KEY_ACCT_SRCBIBLECODE = "acctSrcBibleCode";

    @Override
    protected void onCreate(Bundle icicle) {
    	Bundle savedIcicle = icicle ;
    	
        if( icicle == null ){
        	icicle = this.getIntent().getExtras();
        }

    	super.onCreate(savedIcicle);
    }
    
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.explorer_settings_headers, target);
        
        CrmExplorerConfig cec = CrmExplorerConfig.getInstance(this) ;
        String acctFragmentTitle ;
        if( cec.accountIsOn() ) {
        	String bibleLib = BibleHelper.getBibleLib(this, cec.accountGetBibleCode()) ;
        	if( bibleLib != null ) {
        		acctFragmentTitle = "Account ("+bibleLib+")" ;
        	} else {
        		acctFragmentTitle = "Account (null)" ;
        	}
        } else {
        	acctFragmentTitle = "Account" ;
        }
        Header accountHeader = new Header();
        accountHeader.title = acctFragmentTitle ;
        accountHeader.fragment =
                "za.dams.paracrm.explorer.SettingsAccountFragment";

        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_KEY_ACCT_IS_ON, cec.accountIsOn());
        if( cec.accountIsOn() ) {
        	args.putString(BUNDLE_KEY_ACCT_SRCBIBLECODE, cec.accountGetBibleCode());
        }
        accountHeader.fragmentArguments = args;
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
    	final Bundle bundle = new Bundle();
    	bundle.putBoolean(Explorer.INTENT_EXIT_SETTINGS, true);
        Intent launchIntent = new Intent(this, ExplorerActivity.class);
        launchIntent.setAction(Intent.ACTION_DEFAULT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtras(bundle) ;
        this.startActivity(launchIntent);
    }

}
