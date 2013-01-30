package za.dams.paracrm.explorer;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.R;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class QueryViewActivity extends Activity implements ActionBar.TabListener {
	
	private static class QueryGridTemplate {
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
	private static class ColumnDesc {
		String dataIndex ;
		String dataType ;
		
		String text ;
		boolean text_bold ;
		boolean text_italic ;
		boolean is_bold ;
		
		boolean progressColumn ;
		boolean detachedColumn ;
	}
	
    /** Argument name(s) */
    public static final String ARG_QUERYSRC_ID = "querysrcId";
    public static final String ARG_JSONRESULT_ID = "jsonresultId";
    
    private int querysrcId ;
    private int jsonresultId ;
    
    private ProgressBar mProgressBar ;
    private ViewGroup mTabViewsContainer;
    private ArrayList<Tab> mTabs ;
    private ArrayList<View> mTabViews ;
    
    private LoadQueryTask mLoadTask ;
    
    private String mTitle ;
    private QueryGridTemplate mQgt ;
    private int mNbTabs ;
    private ArrayList<String> mTabTitles ;
    private ArrayList<ArrayList<ColumnDesc>> mTabColumnsDesc ;
    private ArrayList<ArrayList<ArrayList<String>>> mTabRowCells ;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = this.getIntent().getExtras();
		querysrcId = bundle.getInt(ARG_QUERYSRC_ID) ;
		jsonresultId = bundle.getInt(ARG_JSONRESULT_ID) ;
		
		setContentView(R.layout.explorer_viewer);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar) ;
		mTabViewsContainer = (ViewGroup) findViewById(R.id.content) ;
		mTabs = new ArrayList<Tab>() ;
		mTabViews = new ArrayList<View>() ;
		
		
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
	
	private class LoadQueryTask extends AsyncTask<Void,Void,Void> {
        protected void onPreExecute() {
            
        }
        protected Void doInBackground(Void... arg0) {
        	loadFromDb() ;
        	return null ;
        }
        protected void onPostExecute(Void arg0) {
        	if( this.isCancelled() ) {
        		return ;
        	}
        	
        	buildViews() ;
        	
        	ActionBar ab = getActionBar() ;
    		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    		setCurrentTab(0) ;
    		
    		mProgressBar.setVisibility(View.GONE) ;
        }
	}
	private void loadFromDb() {
		DatabaseManager mDb = DatabaseManager.getInstance(this) ;
		Cursor c ;
		
		c = mDb.rawQuery(String.format("SELECT querysrc_name FROM input_query WHERE querysrc_id='%d'",querysrcId));
		c.moveToNext() ;
		mTitle = c.getString(0) ;
		c.close() ;
		
		c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
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
	}
	private void loadFromJsonBlob( String jsonBlob ) {
		
		mNbTabs = 0 ;
		mTabTitles = new ArrayList<String>() ;
		mTabColumnsDesc = new ArrayList<ArrayList<ColumnDesc>>() ;
		mTabRowCells = new ArrayList<ArrayList<ArrayList<String>>>() ;
		
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
		ArrayList<ArrayList<String>> dataGrid = new ArrayList<ArrayList<String>>();
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
	
	
	
	
	private void buildViews() {
		
		ActionBar ab = getActionBar();
		ab.setTitle(mTitle);
		
		LayoutInflater mInflater = getLayoutInflater() ;
		
		for( int tabIdx=0 ; tabIdx<mNbTabs ; tabIdx++ ) {
			
			String tabTitle = mTabTitles.get(tabIdx) ;
			Tab t = ab.newTab().setText(tabTitle).setTabListener(this) ;
			mTabs.add(t);
			ab.addTab(t);
			
			
			ArrayList<ColumnDesc> columnsDesc = mTabColumnsDesc.get(tabIdx) ;
			ArrayList<ArrayList<String>> dataGrid = mTabRowCells.get(tabIdx) ;
		
			View scrollview = mInflater.inflate(R.layout.explorer_viewer_table, null) ;
			TableLayout table = (TableLayout)scrollview.findViewById(R.id.table) ;
			TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
			for( ColumnDesc cd : columnsDesc ) {
				TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
				tv.setText(cd.text) ;
				if( cd.text_italic ) {
					tv.setTypeface(null, Typeface.BOLD) ;
				} else if( cd.text_bold ) {
					tv.setTypeface(null, Typeface.ITALIC) ;
				}
				tv.setGravity(Gravity.LEFT) ;
				tr.addView(tv) ;
			}
			if( mQgt.template_is_on ) {
				tr.setBackgroundColor(mQgt.colorhex_columns) ;
			}
			table.addView(tr) ;

			// iteration sur la data
			int cnt=0 ;
			for( ArrayList<String> dataRow : dataGrid ) {
				View v = new View(this);
				v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
				v.setBackgroundColor(Color.BLACK) ;
				// v.setVisibility(View.VISIBLE) ;
				table.addView(v) ;

				cnt++ ;
				TableRow trData = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
				if( mQgt.template_is_on ) {
					trData.setBackgroundColor((cnt%2==0)?mQgt.colorhex_row:mQgt.colorhex_row_alt) ;
				}
				int i = -1 ;
				for( ColumnDesc cd : columnsDesc ) {
					i++ ;
					String s = dataRow.get(i) ;
					TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
					tv.setText(s) ;
					if( cd.is_bold ) {
						tv.setTypeface(null, Typeface.BOLD) ;
					}
					if( (cd.detachedColumn || cd.progressColumn) ) {
						if( mQgt.data_progress_is_bold ) {
							tv.setTypeface(null, Typeface.BOLD) ;
						}
					} else {
						if( mQgt.data_select_is_bold ) {
							tv.setTypeface(null, Typeface.BOLD) ;
						}
					}
					if( !cd.progressColumn && !cd.is_bold ) {
						tv.setGravity(Gravity.CENTER) ;
					} else {
						tv.setGravity(Gravity.LEFT) ;
					}

					if( cd.progressColumn && mQgt.template_is_on ) {
						try{
							if( Float.parseFloat(s) > 0 ) {
								tv.setTextColor(mQgt.color_green) ;
							}
							else if( Float.parseFloat(s) < 0 ) {
								tv.setTextColor(mQgt.color_red) ;
							}
							else if( Float.parseFloat(s) == 0 ) {
								tv.setText("=") ;
							}
						} catch( NumberFormatException e ) {

						}
					}

					trData.addView(tv) ;
				}
				table.addView(trData) ;
			}


			mTabViewsContainer.addView(scrollview) ;
			mTabViews.add(scrollview) ;

		}
	}
	
    private void setCurrentTab(int tabIdx) {
        int idx = -1 ;
        for( View v : mTabViews ) {
        	idx++ ;
        	v.setVisibility( (idx==tabIdx)? View.VISIBLE : View.GONE ) ;
        }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Comes from the action bar when the app icon on the left is pressed.
                // It works like a back press, but it won't close the activity.
                onBackPressed();
        }
        return false;
    }
    @Override
    public void onBackPressed() {
    	super.onBackPressed();
    }
    
}
