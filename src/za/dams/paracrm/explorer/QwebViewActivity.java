package za.dams.paracrm.explorer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.R;
import za.dams.paracrm.SdcardManager;
import android.app.ActionBar;
import android.app.Activity;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

public class QwebViewActivity extends Activity {
    /** Argument name(s) */
    public static final String ARG_QUERYSRC_ID = "querysrcId";
    public static final String ARG_JSONRESULT_ID = "jsonresultId";
    
    private int querysrcId ;
    private int jsonresultId ;
    
    private boolean mDone ;
    private ProgressBar mProgressBar ;
    private WebView mWebView ;
    
    private String mContentHtml ;
    private HashMap<String,String> mImgMap ;
    
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle bundle = this.getIntent().getExtras();
		querysrcId = bundle.getInt(ARG_QUERYSRC_ID) ;
		jsonresultId = bundle.getInt(ARG_JSONRESULT_ID) ;
		
		setContentView(R.layout.explorer_viewer_qweb);
		
		mDone = false ;
		mProgressBar = (ProgressBar)this.findViewById(R.id.progressbar) ;
		mWebView = (WebView)this.findViewById(R.id.webview) ;
		
		
		
		final ActionBar ab = getActionBar();

		// set defaults for logo & home up
		ab.setDisplayHomeAsUpEnabled(true);
		//ab.setDisplayUseLogoEnabled(useLogo);
		
		
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
        if (mWebView != null) {
        	mWebView.destroy();
        	mWebView = null;
        }
        /*
        if( mImgMap != null ) {
        	for( Map.Entry<String,String> entry : mImgMap.entrySet() ) {
        		String filePath = entry.getValue() ;
        		File f = new File(filePath) ;
        		f.delete() ;
        	}
        }*/
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
    
    
    private class LoadQweb extends AsyncTask<Void,Void,Void> {
        protected void onPreExecute() {
        	mDone = false ;
        }
        protected Void doInBackground(Void... arg0) {
        	mContentHtml = "" ;
        	//mImgMap = new HashMap<String,String>() ;
        	
    		DatabaseManager mDb = DatabaseManager.getInstance(QwebViewActivity.this) ;
    		Cursor c ;
    		
    		c = mDb.rawQuery(String.format("SELECT json_blob FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
    		c.moveToNext() ;
    		String jsonBlob = c.getString(0) ;
    		c.close();
        	
    		try {
    			JSONObject jsonObj = new JSONObject(jsonBlob) ;
    			mContentHtml = jsonObj.getString("html") ;
    			/*
    			JSONObject jsonImgs = jsonObj.getJSONObject("img") ;
    			Iterator<String> myIter = jsonImgs.keys();
    		    while(myIter.hasNext()){
    		        String imgTag = myIter.next();
    		        String imgBase64 = jsonImgs.getString(imgTag);
    		        byte[] imgBinary = Base64.decode(imgBase64, Base64.DEFAULT);
    		        
    		        String imgFilename = "Qweb_"+String.valueOf(System.currentTimeMillis())+"_"+imgTag+".png";
    		        CacheManager.cacheData(QwebViewActivity.this, imgBinary, imgFilename);
    		        
    		        String imgPath = QwebViewActivity.this.getCacheDir()+"/"+imgFilename ;
    		        
    		        mImgMap.put(imgTag, imgPath);
    		    }
    		    */
    			
    		} catch (JSONException e) {
    			// TODO Auto-generated catch block
    			// e.printStackTrace();
    		}
    		
    		
    		mDb.execSQL(String.format("DELETE FROM query_cache_json WHERE json_result_id='%d'",jsonresultId));
    		
        	
        	return null ;
        }
    	
        protected void onPostExecute(Void arg0) {
            mWebView.loadData(mContentHtml, "text/html", null);
            mProgressBar.setVisibility(View.GONE) ;
            mWebView.setVisibility(View.VISIBLE) ;
            mDone = true ;
        }
    	
    }
    
    private void onSaveToSd() {
    	ActionBar ab = getActionBar() ;
    	String title = ab.getTitle().toString() ;
    	String timestamp = String.valueOf((int)(System.currentTimeMillis() / 1000)) ; 
    	String queryName = "CrmQuery_"+title.replaceAll("[^a-zA-Z0-9]", "")+"_"+timestamp+".html" ;
    	SdcardManager.saveData(this, queryName, mContentHtml.getBytes(), true) ;
    }
}
