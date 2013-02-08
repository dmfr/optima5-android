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
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

public class QueryViewActivity extends Activity implements ActionBar.TabListener, ViewSwitcher.ViewFactory {
	
	public static class QueryGridTemplate {
		boolean template_is_on ;
		String color_key ;
		int colorhex_columns ;
		int colorhex_row ;
		int colorhex_row_alt ;
		String data_align ;
		boolean data_select_is_bold ;
		boolean data_progress_is_bold ;
		int color_green ;
		int color_red ;
	}
	public static class ColumnDesc {
		String dataIndex ;
		String dataType ;
		
		String text ;
		boolean text_bold ;
		boolean text_italic ;
		boolean is_bold ;
		
		boolean progressColumn ;
		boolean detachedColumn ;
	}
	
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
    
    private LoadQueryTask mLoadTask ;
    
    private String mTitle ;
    private QueryGridTemplate mQgt ;
    private int mNbTabs ;
    private List<String> mTabTitles ;
    private List<List<ColumnDesc>> mTabColumnsDesc ;
    private List<List<List<String>>> mTabRowCells ;
    
    private int mCurrentTabIdx ;
    private int mCurrentRowOffset ;

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
		
		
		mLoadTask = new LoadQueryTask() ;
		mLoadTask.execute() ;
	}
	@Override
	protected void onDestroy() {
		if( mLoadTask != null && mLoadTask.getStatus() != AsyncTask.Status.FINISHED ) {
			mLoadTask.cancel(true) ;
		}
		super.onDestroy();
	}	
	
	private class LoadQueryTask extends AsyncTask<Void,Void,Boolean> {
        protected void onPreExecute() {
            
        }
        protected Boolean doInBackground(Void... arg0) {
        	return loadFromDb() ;
        }
        protected void onPostExecute(Boolean isDone) {
        	if( this.isCancelled() ) {
        		return ;
        	}
        	if( !isDone ) {
        		QueryViewActivity.this.finish() ;
        		return ;
        	}
        	
        	if( mNbTabs == 0 ) {
            	mProgressBar.setVisibility(View.GONE) ;
        		return ;
        	}
        	mCurrentTabIdx = 0 ;
        	mCurrentRowOffset = 0 ;
        	
        	
        	// Initialisation du "getter" TabGridGetter
        	mTabGridGetter = new TabGridGetter() ;
        	
        	// Initialisation du ViewSwitcher / ViewSwitcher.Factory 
        	mProgressBar.setVisibility(View.GONE) ;
        	mViewSwitcher.setVisibility(View.VISIBLE);
        	mViewSwitcher.setFactory(QueryViewActivity.this) ;
        	
        	
        	// Init des tabs + highlight 1ere tab
        	buildTabs() ;
        	initFirstTab() ;
    		//setCurrentTab(0) ;
        }
	}
	private boolean loadFromDb() {
		DatabaseManager mDb = DatabaseManager.getInstance(this) ;
		Cursor c ;
		
		c = mDb.rawQuery(String.format("SELECT querysrc_name FROM input_query WHERE querysrc_id='%d'",querysrcId));
		c.moveToNext() ;
		mTitle = c.getString(0) ;
		c.close() ;
		
		c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
		if( c.getCount() != 1 ) {
			c.close() ;
			return false ;
		}
		c.moveToNext() ;
		String jsonBlob = c.getString(0) ;
		c.close();
		
		mQgt = new QueryGridTemplate();
		c = mDb.rawQuery("SELECT * FROM querygrid_template WHERE query_id='0'") ;
		if( c.moveToNext() ) {
			mQgt.template_is_on = c.getString(c.getColumnIndex("template_is_on")).equals("O") ;
			mQgt.color_key = c.getString(c.getColumnIndex("color_key"));
			mQgt.colorhex_columns =  Color.parseColor(c.getString(c.getColumnIndex("colorhex_columns")));
			mQgt.colorhex_row =  Color.parseColor(c.getString(c.getColumnIndex("colorhex_row")));
			mQgt.colorhex_row_alt =  Color.parseColor(c.getString(c.getColumnIndex("colorhex_row_alt")));
			mQgt.data_align = c.getString(c.getColumnIndex("data_align"));
			mQgt.data_select_is_bold = c.getString(c.getColumnIndex("data_select_is_bold")).equals("O") ;
			mQgt.data_progress_is_bold = c.getString(c.getColumnIndex("data_progress_is_bold")).equals("O") ;
			mQgt.color_green = Color.parseColor("#00AA00") ;
			mQgt.color_red = Color.parseColor("#EE0000") ;
		}
		c.close();
		
		mDb.execSQL(String.format("DELETE FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
		
		loadFromJsonBlob(jsonBlob) ;
		
		return true ;
	}
	private void loadFromJsonBlob( String jsonBlob ) {
		
		mNbTabs = 0 ;
		mTabTitles = new ArrayList<String>() ;
		mTabColumnsDesc = new ArrayList<List<ColumnDesc>>() ;
		mTabRowCells = new ArrayList<List<List<String>>>() ;
		
		try {
			JSONObject jsonObj = new JSONObject(jsonBlob) ;
			JSONArray jsonTabs = jsonObj.getJSONArray("tabs") ;
			// set up tabs nav
			for (int i = 0; i < jsonTabs.length(); i++) {
				
				JSONObject jsonObjTab = jsonTabs.getJSONObject(i) ;
				loadTabFromJson(jsonObjTab) ;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}
	
	private void loadTabFromJson( JSONObject jsonObjTab ) {
		
		// column map
		String tabTitle = "" ;
		ArrayList<ColumnDesc> columnsDesc = new ArrayList<ColumnDesc>() ;
		ArrayList<List<String>> dataGrid = new ArrayList<List<String>>();
		try {
			tabTitle = jsonObjTab.getString("tab_title") ;
			
			JSONArray jsonCols = jsonObjTab.getJSONArray("columns");
			for(int i=0 ; i<jsonCols.length() ; i++) {
				JSONObject jsonCol = jsonCols.getJSONObject(i) ;
				ColumnDesc cd = new ColumnDesc() ;
				cd.text = jsonCol.optString("text") ;
				cd.text_bold = jsonCol.optBoolean("text_bold") ;
				cd.text_italic = jsonCol.optBoolean("text_italic") ;
				cd.is_bold = jsonCol.optBoolean("is_bold") ;
				cd.dataIndex = jsonCol.optString("dataIndex") ;
				cd.dataType = jsonCol.optString("dataType") ;
				cd.progressColumn = jsonCol.optBoolean("progressColumn");
				cd.detachedColumn = jsonCol.optBoolean("detachedColumn");
				columnsDesc.add(cd) ;
			}
			
			JSONArray jsonData = jsonObjTab.getJSONArray("data");
			for(int j=0 ; j<jsonData.length() ; j++) {
				JSONObject jsonRow = jsonData.getJSONObject(j) ;
				ArrayList<String> dataRow = new ArrayList<String>();
				for( ColumnDesc cd : columnsDesc ) {
					String s = jsonRow.optString(cd.dataIndex) ;
					if( s.equals("null") ) {
						s = "" ;
					}
					else if( cd.progressColumn ) {
						try {
							if( Float.parseFloat(s) > 0 ) {
								String newS = "+"+s ;
								s = newS ;
							}
						}catch(NumberFormatException e ) {
							
						}
					}
					
					dataRow.add(s) ;
				}
				dataGrid.add(dataRow) ;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mNbTabs++ ;
		mTabTitles.add(tabTitle) ;
		mTabColumnsDesc.add(columnsDesc);
		mTabRowCells.add(dataGrid) ;
	}
	
	
	
	private void buildTabs() {
		ActionBar ab = getActionBar();
		ab.setTitle(mTitle);
		for( int tabIdx=0 ; tabIdx<mNbTabs ; tabIdx++ ) {
			String tabTitle = mTabTitles.get(tabIdx) ;
			Tab t = ab.newTab().setText(tabTitle).setTabListener(this) ;
			mTabs.add(t);
			ab.addTab(t);
		}
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
	
	/*
    private void setCurrentTab(int tabIdx) {
        int idx = -1 ;
        for( View v : mTabViews ) {
        	idx++ ;
        	v.setVisibility( (idx==tabIdx)? View.VISIBLE : View.GONE ) ;
        }
    }
    */
	private void initFirstTab() {
		QueryView view = (QueryView) mViewSwitcher.getCurrentView();
		view.setTabAndOffset(0, 0) ;
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

        QueryView view = (QueryView) mViewSwitcher.getNextView();
    	
    	mCurrentTabIdx = tabIdx ;
    	mCurrentRowOffset = 0 ; // @DAMS : à voir?
    	view.setTabAndOffset(mCurrentTabIdx, mCurrentRowOffset) ;
    	mViewSwitcher.showNext();
    	
    	// On détruit l'autre vue
    	// => dégrade l'esthétique car on scroll sur du vide
    	// => pas fiable car la suivante à déja commencé son Draw
    	// => pas utile car l'autre vue est par défaut invisible (hors scroll) et sera réinitialisée avant tout scroll/switchTab
    	((QueryView)mViewSwitcher.getNextView()).manualDestroy() ;
    }
	
	
	

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTabSelected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		if( mTabs.contains(arg0) ) {
			int tabIdx = mTabs.indexOf(arg0) ;
			setCurrentTab(tabIdx) ;
		}
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		
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
    
    private void onSaveToSd() {
    	new DownloadAgainTask().execute() ;
    }
    
    
    private ProgressDialog mDownloadProgress ;
    private class DownloadAgainTask extends AsyncTask<Void,Void,Void> {
    	String fileName ;
    	byte[] data = null ;
    	
        protected void onPreExecute() {
			mDownloadProgress = ProgressDialog.show(
		    		QueryViewActivity.this,
		    		"Downloading Query",
		            "Please wait...",
		            true);
			
	    	ActionBar ab = getActionBar() ;
	    	String title = ab.getTitle().toString() ;
	    	String timestamp = String.valueOf((int)(System.currentTimeMillis() / 1000)) ; 
	    	fileName = "CrmQuery_"+title.replaceAll("[^a-zA-Z0-9]", "")+"_"+timestamp+".xlsx" ;
			return ;
        }
        protected Void doInBackground(Void... arg0) {
        	CrmQueryModel lastFetchModel = CrmQueryManager.getLastFetchModel() ;
        	Integer tmpJsonRecord = CrmQueryManager.fetchRemoteJson(QueryViewActivity.this, lastFetchModel, true) ;
        	if( tmpJsonRecord == null ) {
        		return null ;
        	}
    		
        	DatabaseManager mDb = DatabaseManager.getInstance(QueryViewActivity.this) ;
    		Cursor c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",tmpJsonRecord));
    		c.moveToNext() ;
    		String jsonBlob = c.getString(0) ;
    		c.close();
    		
    		mDb.execSQL(String.format("DELETE FROM query_cache_json WHERE json_result_id='%d'",tmpJsonRecord));
    		
    		try {
    			JSONObject jsonObj = new JSONObject(jsonBlob) ;
    			String xlsBase64 = jsonObj.getString("xlsx_base64") ;
    			data = Base64.decode(xlsBase64, Base64.DEFAULT) ;
    		}catch(JSONException e) {
    			return null ;
    		}
    		
        	return null ;
        }
        protected void onPostExecute(Void arg0) {
        	if( isCancelled() ) {
        		return ;
        	}
        	mDownloadProgress.dismiss() ;
        	if( data == null ) {
        		return ;
        	}
        	SdcardManager.saveData(QueryViewActivity.this, fileName, data, true) ;
        }
	}
    
    private TabGridGetter mTabGridGetter = null ;
    public class TabGridGetter {
    	
    	public QueryGridTemplate getQueryGridTemplate() {
    		return mQgt ;
    	}
    	
    	public List<ColumnDesc> getTabColumns( int tabIdx ) {
    		return mTabColumnsDesc.get(tabIdx) ;
    	}
    	
    	public List<List<String>> getTabRows( int tabIdx, int startOffset, int nbLimit ) {
    		if( startOffset >= getTabCount( tabIdx ) ) {
    			return new ArrayList<List<String>>() ;
    		}    		
    		return mTabRowCells.get(tabIdx).subList(startOffset, Math.min(startOffset+nbLimit,getTabCount(tabIdx))) ;
    	}
    	
    	public int getTabCount( int tabIdx ) {
    		return mTabRowCells.get(tabIdx).size() ;
    	}
    	
    }
	@Override
	public View makeView() {
		QueryView queryView = new QueryView(this,mViewSwitcher,mTabGridGetter,25) ;
		queryView.setLayoutParams(new ViewSwitcher.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		queryView.setId(VIEW_ID) ;
		queryView.setTabAndOffset(mCurrentTabIdx, mCurrentRowOffset) ;
		return queryView;
	}
    
}
