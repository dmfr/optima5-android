package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.R;
import za.dams.paracrm.SdcardManager;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;

public class QwebViewActivity extends Activity implements ViewFactory, TabListener {
    /**
     * The view id used for all the views we create. It's OK to have all child
     * views have the same ID. This ID is used to pick which view receives
     * focus when a view hierarchy is saved / restore
     */
    private static final int VIEW_ID = 1;
    
    /** Argument name(s) */
    public static final String ARG_QUERYSRC_ID = "querysrcId";
    public static final String ARG_JSONRESULT_ID = "jsonresultId";
    
    private int querysrcId ;
    private int jsonresultId ;
    
    private List<Tab> mTabs ;
    private ProgressBar mProgressBar ;
    private ViewSwitcher mViewSwitcher ;
    protected Animation mInAnimationForward;
    protected Animation mOutAnimationForward;
    protected Animation mInAnimationBackward;
    protected Animation mOutAnimationBackward;
    
    private String mTitle ;
    private List<String> mTabTitles ;
    private List<String> mTabHtmlBlobs ;
    
    private int mCurrentTabIdx ;
    
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = this.getIntent().getExtras();
		querysrcId = bundle.getInt(ARG_QUERYSRC_ID) ;
		jsonresultId = bundle.getInt(ARG_JSONRESULT_ID) ;
		
		setContentView(R.layout.explorer_viewer_query);

		mProgressBar = (ProgressBar) findViewById(R.id.progressbar) ;
		mViewSwitcher = (ViewSwitcher) findViewById(R.id.switcher) ;
		mTabs = new ArrayList<Tab>() ;
		
        mInAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        mOutAnimationForward = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        mInAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        mOutAnimationBackward = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
		
		final ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
		
		
		DatabaseManager mDb = DatabaseManager.getInstance(this) ;
		Cursor c ;
		
		c = mDb.rawQuery(String.format("SELECT querysrc_name FROM input_query WHERE querysrc_id='%d'",querysrcId));
		c.moveToNext() ;
		ab.setTitle(c.getString(0));
		c.close() ;
		
		new LoadQweb().execute();
	}
	
	@Override
	protected void onDestroy() {
        if (mViewSwitcher != null) {
        	((WebView)mViewSwitcher.getCurrentView()).destroy();
        	if( mViewSwitcher.getNextView() != null ) {
        		((WebView)mViewSwitcher.getNextView()).destroy();
        	}
        }
        super.onDestroy() ;
	}
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.explorer_viewer_options, menu);
        return true;
    }
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Comes from the action bar when the app icon on the left is pressed.
                // It works like a back press, but it won't close the activity.
                onBackPressed();
                break ;
            case R.id.savetosd:
            	onSaveToSd() ;
            	break ;
        }
        return false;
    }
    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    }
    
    
    private class LoadQweb extends AsyncTask<Void,Void,Boolean> {
        protected void onPreExecute() {
        }
        protected Boolean doInBackground(Void... arg0) {
    		DatabaseManager mDb = DatabaseManager.getInstance(QwebViewActivity.this) ;
    		Cursor c ;
    		
    		c = mDb.rawQuery(String.format("SELECT querysrc_name FROM input_query WHERE querysrc_id='%d'",querysrcId));
    		c.moveToNext() ;
    		mTitle = c.getString(0) ;
    		c.close() ;
    		
    		c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
    		if( c.getCount() != 1 ) {
    			c.close();
    			return false ;
    		}
    		c.moveToNext() ;
    		String jsonBlob = c.getString(0) ;
    		c.close();
        	
    		mTabTitles = new ArrayList<String>() ;
    		mTabHtmlBlobs = new ArrayList<String>() ;
    		try {
    			JSONObject jsonObj = new JSONObject(jsonBlob) ;
    			
    			if( jsonObj.has("html") ) {
    				mTabHtmlBlobs.add(jsonObj.getString("html")) ;
    			} else if( jsonObj.has("tabs") ) {
    				JSONArray jsonTabs = jsonObj.getJSONArray("tabs") ;
    				// set up tabs nav
    				for (int i = 0; i < jsonTabs.length(); i++) {
    					JSONObject jsonObjTab = jsonTabs.getJSONObject(i) ;
    					
    					String tabTitle = jsonObjTab.getString("tab_title") ;
    					String tabHtml = jsonObjTab.getString("html") ;
    					
    					mTabTitles.add(tabTitle) ;
    					mTabHtmlBlobs.add(tabHtml) ;
    				}
    			}
    			
    		} catch (JSONException e) {
    			// e.printStackTrace();
    		}
    		
    		
    		//mDb.execSQL(String.format("DELETE FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
    		
        	
        	return true ;
        }
    	
        protected void onPostExecute(Boolean isDone) {
        	if( !isDone ) {
        		QwebViewActivity.this.finish() ;
        		return ;
        	}
        	if( mTabHtmlBlobs.size() == 0 ) {
            	mProgressBar.setVisibility(View.GONE) ;
        		return ;
        	}
        	mCurrentTabIdx = 0 ;
        	
        	// Initialisation du ViewSwitcher / ViewSwitcher.Factory 
        	mProgressBar.setVisibility(View.GONE) ;
        	mViewSwitcher.setVisibility(View.VISIBLE);
        	mViewSwitcher.setFactory(QwebViewActivity.this) ;
        	
        	// Init des tabs + highlight 1ere tab
        	if( mTabTitles.size() > 0 ) {
        		buildTabs() ;
        	}
        	initFirstTab() ;
        }
    	
    }
	private void buildTabs() {
		ActionBar ab = getActionBar();
		ab.setTitle(mTitle);
		for( int tabIdx=0 ; tabIdx<mTabTitles.size() ; tabIdx++ ) {
			String tabTitle = mTabTitles.get(tabIdx) ;
			Tab t = ab.newTab().setText(tabTitle).setTabListener(this) ;
			mTabs.add(t);
			ab.addTab(t);
		}
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
	private void initFirstTab() {
    	mCurrentTabIdx = 0 ;
		((WebView) mViewSwitcher.getCurrentView()).loadDataWithBaseURL(null, mTabHtmlBlobs.get(mCurrentTabIdx), "text/html","UTF-8", null);
	}
    private void setCurrentTab(int tabIdx) {
    	if( tabIdx == mCurrentTabIdx ) {
    		return ;
    	}
    	
        if (tabIdx-mCurrentTabIdx > 0) {
            mViewSwitcher.setInAnimation(mInAnimationForward);
            mViewSwitcher.setOutAnimation(mOutAnimationForward);
        } else {
            mViewSwitcher.setInAnimation(mInAnimationBackward);
            mViewSwitcher.setOutAnimation(mOutAnimationBackward);
        }

    	mCurrentTabIdx = tabIdx ;
    	
    	WebView view = (WebView) mViewSwitcher.getNextView();
        view.loadDataWithBaseURL(null, mTabHtmlBlobs.get(mCurrentTabIdx), "text/html","UTF-8", null);
    	mViewSwitcher.showNext();
    }
    
    private void onSaveToSd() {
    	ActionBar ab = getActionBar() ;
    	String title = ab.getTitle().toString() ;
    	String timestamp = String.valueOf((int)(System.currentTimeMillis() / 1000)) ; 
    	String queryNameBase = "CrmQuery_"+title.replaceAll("[^a-zA-Z0-9]", "")+"_"+timestamp ;
    	if( mTabTitles.size() > 0 ) {
    		for( int tabIdx=0 ; tabIdx<mTabTitles.size() ; tabIdx++ ) {
    			String tabTitle = mTabTitles.get(tabIdx) ;
    			SdcardManager.saveData(this, queryNameBase+"_"+tabTitle.replaceAll("[^a-zA-Z0-9]", "")+".html", mTabHtmlBlobs.get(tabIdx).getBytes(), true) ;
    		}
    	} else {
    		SdcardManager.saveData(this, queryNameBase+".html", mTabHtmlBlobs.get(0).getBytes(), true) ;
    	}
    }

	@Override
	public View makeView() {
		WebView webView = new WebView(this) ;
		webView.setLayoutParams(new ViewSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		webView.setId(VIEW_ID) ;
		webView.getSettings().setJavaScriptEnabled(true);
		return webView;
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}

	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		if( mTabs.contains(arg0) ) {
			int tabIdx = mTabs.indexOf(arg0) ;
			setCurrentTab(tabIdx) ;
		}
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}
