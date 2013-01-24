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
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class QueryViewActivity extends Activity implements ActionBar.TabListener {
	
	private static class ColumnDesc {
		String dataIndex ;
		String dataType ;
		
		String text ;
		boolean text_bold ;
		boolean text_italic ;
		boolean is_bold ;
		
		boolean progressColumn ;
	}
	
    /** Argument name(s) */
    public static final String ARG_QUERYSRC_ID = "querysrcId";
    public static final String ARG_JSONRESULT_ID = "querysrcId";
    
    private int querysrcId ;
    private int jsonresultId ;
    
    private ViewGroup mTabViewsContainer;
    private ArrayList<View> mTabViews ;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = this.getIntent().getExtras();
		querysrcId = bundle.getInt(ARG_QUERYSRC_ID) ;
		jsonresultId = bundle.getInt(ARG_JSONRESULT_ID) ;
		
		setContentView(R.layout.explorer_viewer);
		
		mTabViewsContainer = (ViewGroup) findViewById(R.id.content) ;
		mTabViews = new ArrayList<View>() ;
		
		
		
		
		final ActionBar ab = getActionBar();

		// set defaults for logo & home up
		ab.setDisplayHomeAsUpEnabled(true);
		//ab.setDisplayUseLogoEnabled(useLogo);
		
		DatabaseManager mDb = DatabaseManager.getInstance(this) ;
		Cursor c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
		c.moveToNext() ;
		String jsonBlob = c.getString(0) ;
		c.close();
		
		try {
			JSONObject jsonObj = new JSONObject(jsonBlob) ;
			JSONArray jsonTabs = jsonObj.getJSONArray("tabs") ;
			// set up tabs nav
			for (int i = 0; i < jsonTabs.length(); i++) {
				
				JSONObject jsonObjTab = jsonTabs.getJSONObject(i) ;
				buildTab(jsonObjTab) ;
				ab.addTab(ab.newTab().setText(jsonObjTab.getString("tab_title")).setTabListener(this));
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		
		
		
		
		
		
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		setCurrentTab(0) ;
		
		mDb.execSQL(String.format("DELETE FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
	}
	
	
	
	public void buildTab( JSONObject jsonObjTab ) {
		
		LayoutInflater mInflater = getLayoutInflater() ;
		
		// column map
		ArrayList<ColumnDesc> columnsDesc = new ArrayList<ColumnDesc>() ;
		ArrayList<ArrayList<String>> dataGrid = new ArrayList<ArrayList<String>>();
		try {
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
				columnsDesc.add(cd) ;
			}
			
			JSONArray jsonData = jsonObjTab.getJSONArray("data");
			for(int j=0 ; j<jsonCols.length() ; j++) {
				JSONObject jsonRow = jsonData.getJSONObject(j) ;
				ArrayList<String> dataRow = new ArrayList<String>();
				for( ColumnDesc cd : columnsDesc ) {
					dataRow.add(jsonRow.optString(cd.dataIndex)) ;
				}
				dataGrid.add(dataRow) ;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		View scrollview = mInflater.inflate(R.layout.explorer_viewer_table, null) ;
		TableLayout table = (TableLayout)scrollview.findViewById(R.id.table) ;
    	TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
    	for( ColumnDesc cd : columnsDesc ) {
    		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
    		tv.setText(cd.text) ;
    		tv.setTypeface(null, Typeface.BOLD) ;
    		tv.setGravity(Gravity.LEFT) ;
    		tr.addView(tv) ;
    	}
    	table.addView(tr) ;
		
		// iteration sur la data
    	for( ArrayList<String> dataRow : dataGrid ) {
    		View v = new View(this);
            v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
            v.setBackgroundColor(Color.BLACK) ;
            // v.setVisibility(View.VISIBLE) ;
    		table.addView(v) ;
    		
    		TableRow trData = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
    		for( String s : dataRow ) {
        		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
        		tv.setText(s) ;
        		//tv.setTypeface(null, Typeface.BOLD) ;
        		tv.setGravity(Gravity.CENTER) ;
        		trData.addView(tv) ;
    		}
    		table.addView(trData) ;
    	}
		
		
    	mTabViewsContainer.addView(scrollview) ;
    	mTabViews.add(scrollview) ;
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
