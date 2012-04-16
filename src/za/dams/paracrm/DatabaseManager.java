package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseManager {
    public final static String TAG = "PARACRM/DatabaseManager";

    private static DatabaseManager instance;

    private SQLiteDatabase mDb;
    private final String DB_NAME = "_paracrm";
    private final int DB_VERSION = 5;
    
    public static class DatabaseUpgradeResult {
    	public boolean success ;
    	public long versionTimestamp ;
    	public int nbTables ;
    	public int nbRows ;
    	
    	public DatabaseUpgradeResult( boolean mSuccess, long mVersionTimestamp, int mNbTables, int mNbRows ) {
    		success = mSuccess ;
    		versionTimestamp = mVersionTimestamp ;
    		nbTables = mNbTables ;
    		nbRows = mNbRows ;
    	}
    }
    
    private DatabaseManager(Context context) {
        // create or open database
        DatabaseHelper helper = new DatabaseHelper(context);
        this.mDb = helper.getWritableDatabase();
    }

    public synchronized static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }
    private class DatabaseHelper extends SQLiteOpenHelper {

    	public DatabaseHelper( Context context ) {
    		super(context, DB_NAME, null, DB_VERSION);
    	}

    	@Override
    	public void onCreate(SQLiteDatabase db) {
    		String createTableQuery = "" ;
    		
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "define_bible" + " ("
                    + "bible_code" + " VARCHAR(50) PRIMARY KEY, "
                    + "bible_lib" + " VARCHAR(100),"
                    + "bible_iconfile" + " VARCHAR(50),"
                    + "bible_specdata" + " VARCHAR(10),"
                    + "gmap_is_on" + " VARCHAR(1)"
                    + ");";
            db.execSQL(createTableQuery);

    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "define_bible_entry" + " ("
                    + "bible_code" + " VARCHAR(50),"
                    + "entry_field_code" + " VARCHAR(50),"
                    + "entry_field_is_key" + " VARCHAR(1),"
                    + "entry_field_index" + " INTEGER,"
                    + "entry_field_lib" + " VARCHAR(100),"
                    + "entry_field_type" + " VARCHAR(50),"
                    + "entry_field_linkbible" + " VARCHAR(50),"
                    + "entry_field_is_header" + " VARCHAR(1),"
                    + "entry_field_is_highlight" + " VARCHAR(1),"
                    + "PRIMARY KEY( bible_code, entry_field_code)"
                    + ");";
            db.execSQL(createTableQuery);

    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "define_bible_tree" + " ("
                    + "bible_code" + " VARCHAR(50),"
                    + "tree_field_code" + " VARCHAR(50),"
                    + "tree_field_is_key" + " VARCHAR(1),"
                    + "tree_field_index" + " INTEGER,"
                    + "tree_field_lib" + " VARCHAR(100),"
                    + "tree_field_type" + " VARCHAR(50),"
                    + "tree_field_linkbible" + " VARCHAR(50),"
                    + "tree_field_is_header" + " VARCHAR(1),"
                    + "tree_field_is_highlight" + " VARCHAR(1),"
                    + "PRIMARY KEY( bible_code, tree_field_code)"
                    + ");";
            db.execSQL(createTableQuery);

    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "define_file" + " ("
                    + "file_code" + " VARCHAR(50) PRIMARY KEY, "
                    + "file_parent_code" + " VARCHAR(50),"
                    + "file_lib" + " VARCHAR(100),"
                    + "file_iconfile" + " VARCHAR(50),"
                    + "file_type" + " VARCHAR(50),"
                    + "file_specdata" + " VARCHAR(10),"
                    + "gmap_is_on" + " VARCHAR(1)"
                    + ");";
            db.execSQL(createTableQuery);

    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "define_file_entry" + " ("
                    + "file_code" + " VARCHAR(50),"
                    + "entry_field_code" + " VARCHAR(50),"
                    + "entry_field_index" + " INTEGER,"
                    + "entry_field_lib" + " VARCHAR(100),"
                    + "entry_field_type" + " VARCHAR(50),"
                    + "entry_field_linkbible" + " VARCHAR(50),"
                    + "entry_field_is_header" + " VARCHAR(1),"
                    + "entry_field_is_highlight" + " VARCHAR(1),"
                    + "PRIMARY KEY( file_code, entry_field_code)"
                    + ");";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_bible_entry" + " ("
                    + "bible_code" + " VARCHAR(50), "
                    + "entry_key" + " VARCHAR(100),"
                    + "treenode_key" + " VARCHAR(100),"
                    + "PRIMARY KEY( bible_code, entry_key)"
                    + ");";
            db.execSQL(createTableQuery);
    		createTableQuery = "CREATE INDEX IF NOT EXISTS "
                    + "IDX_storebibleentry_treenodekey " 
                    + "ON store_bible_entry ( bible_code, treenode_key )"
                    + ";";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_bible_entry_field" + " ("
                    + "bible_code" + " VARCHAR(50), "
                    + "entry_key" + " VARCHAR(100),"
                    + "entry_field_code" + " VARCHAR(100),"
                    + "entry_field_value_number" + " DECIMAL(10,2),"
                    + "entry_field_value_string" + " VARCHAR(100),"
                    + "entry_field_value_date" + " DATE,"
                    + "entry_field_value_link" + " VARCHAR(500),"
                    + "PRIMARY KEY( bible_code, entry_key,entry_field_code)"
                    + ");";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_bible_tree" + " ("
                    + "bible_code" + " VARCHAR(50), "
                    + "treenode_key" + " VARCHAR(100),"
                    + "treenode_parent_key" + " VARCHAR(100),"
                    + "PRIMARY KEY( bible_code, treenode_key)"
                    + ");";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_bible_tree_field" + " ("
                    + "bible_code" + " VARCHAR(50), "
                    + "treenode_key" + " VARCHAR(100),"
                    + "treenode_field_code" + " VARCHAR(100),"
                    + "treenode_field_value_number" + " DECIMAL(10,2),"
                    + "treenode_field_value_string" + " VARCHAR(100),"
                    + "treenode_field_value_date" + " DATE,"
                    + "treenode_field_value_link" + " VARCHAR(500),"
                    + "PRIMARY KEY( bible_code, treenode_key,treenode_field_code)"
                    + ");";
            db.execSQL(createTableQuery);
            
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_file" + " ("
                    + "filerecord_id" + " INTEGER PRIMARY KEY, "
                    + "filerecord_parent_id" + " INTEGER,"
                    + "file_code" + " VARCHAR(50)"
                    + ");";
            db.execSQL(createTableQuery);
    		createTableQuery = "CREATE INDEX IF NOT EXISTS "
                    + "IDX_storefile_filerecordparentid " 
                    + "ON store_file ( filerecord_parent_id )"
                    + ";";
            db.execSQL(createTableQuery);
    		createTableQuery = "CREATE INDEX IF NOT EXISTS "
                    + "IDX_storefile_filecode " 
                    + "ON store_file ( file_code )"
                    + ";";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "store_file_field" + " ("
                    + "filerecord_id" + " INTEGER, "
                    + "filerecord_field_code" + " VARCHAR(100),"
                    + "filerecord_field_value_number" + " DECIMAL(10,2),"
                    + "filerecord_field_value_string" + " VARCHAR(100),"
                    + "filerecord_field_value_date" + " DATETIME,"
                    + "filerecord_field_value_link" + " VARCHAR(500),"
                    + "PRIMARY KEY( filerecord_id, filerecord_field_code)"
                    + ");";
            db.execSQL(createTableQuery);
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "upload_media" + " ("
                    + "filerecord_id" + " INTEGER PRIMARY KEY, "
                    + "media_filename" + " VARCHAR(50)"
                    + ");";
            db.execSQL(createTableQuery);
            
            
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "input_scen" + " ("
                    + "scen_id" + " INTEGER, "
                    + "scen_name" + " VARCHAR(100),"
                    + "target_filecode" + " VARCHAR(100),"
                    + "PRIMARY KEY( scen_id )"
                    + ");";
            db.execSQL(createTableQuery);
  
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "input_scen_page" + " ("
                    + "scen_id" + " INTEGER, "
                    + "scen_page_index" + " INTEGER, "
                    + "scen_page_name" + " VARCHAR(100),"
                    + "target_filecode" + " VARCHAR(100),"
                    + "page_type" + " VARCHAR(100),"
                    + "page_table_type" + " VARCHAR(100),"
                    + "PRIMARY KEY( scen_id , scen_page_index )"
                    + ");";
            db.execSQL(createTableQuery);
  
    		createTableQuery = "CREATE TABLE IF NOT EXISTS "
                    + "input_scen_page_field" + " ("
                    + "scen_id" + " INTEGER, "
                    + "scen_page_index" + " INTEGER, "
                    + "scen_page_field_index" + " INTEGER, "
                    + "target_filecode" + " VARCHAR(100),"
                    + "target_filefield" + " VARCHAR(100),"
                    + "field_autovalue_is_on" + " VARCHAR(1),"
                    + "field_autovalue_src" + " VARCHAR(1),"
                    + "field_is_pivot" + " VARCHAR(1),"
                    + "search_is_condition" + " VARCHAR(1),"
                    + "PRIMARY KEY( scen_id , scen_page_index , scen_page_field_index )"
                    + ");";
            db.execSQL(createTableQuery);
    	}

    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    		String curTable ;
    		Cursor tmpCursor = db.rawQuery( String.format("SELECT name FROM sqlite_master WHERE type='table'"),null) ;
    		if( tmpCursor.getCount() > 0 ) {
    			db.beginTransaction() ;
    	    	while( !tmpCursor.isLast() ){
    	    		tmpCursor.moveToNext();
    	    		curTable = tmpCursor.getString(0) ;
    	    		
    	    		// vidage table existe ?
    	    		db.execSQL(String.format("DROP TABLE %s",curTable));
    	    	}
    	    	
    	    	db.setTransactionSuccessful() ;
    	    	db.endTransaction() ;
    		}
    		
    		onCreate(db); 
    	}

    }
    
    public DatabaseUpgradeResult upgradeReferentielStream( InputStream is ) {
    	long versionTimestamp = 0 ;
    	
    	int nbRows = 0 ;
    	int nbRowsExpected = -1 ;
    	int nbTables = 0 ;
    	int nbTablesExpected = -1 ;
    	
    	int nbRowstmp = 0 ;
    	
    	ArrayList<ContentValues> insertBuffer = new ArrayList<ContentValues>() ;
    	
    	HashMap<String,Columns> mapTables = new HashMap<String,Columns>() ;
    	String tableName = "" ;
    	try {
    		BufferedReader r = new BufferedReader(new InputStreamReader(is));
    		String readLine ;
    		
    		JSONObject jsonObj ;
			while( (readLine = r.readLine()) != null ) {
				try {
					jsonObj = new JSONObject(readLine) ;
					if( jsonObj.optBoolean("success",false) && jsonObj.optLong("timestamp",0) > 0 ) {
						versionTimestamp = jsonObj.optLong("timestamp",0) ;
						nbRowsExpected = jsonObj.optInt("nb_rows",0) ;
						nbTablesExpected = jsonObj.optInt("nb_tables",0) ;
						continue ;
					}
					
					if( !jsonObj.optString("table_name","").equals(tableName) ) {
						upgradeReferentielStream_bulkInsert(tableName,insertBuffer) ;
		    			insertBuffer = new ArrayList<ContentValues>() ;
		    			//Log.w("")
					}
					
					tableName = jsonObj.optString("table_name","") ;
					if( tableName.equals("") ) {
						continue ;
					}
					if( !mapTables.containsKey(tableName) ) {
						String columnNames[] ;
						try{
							Cursor c = mDb.rawQuery(String.format("SELECT * FROM %s WHERE 0",tableName),null);
							columnNames = c.getColumnNames() ;
							c.close() ;
						}
						finally{
						}
						mDb.execSQL(String.format("DELETE FROM %s",tableName));
						mapTables.put(tableName,
								new Columns(columnNames)
						);
						nbTables++ ;
					}
					
					// mDb.insert(tableName, "NULL", );
					insertBuffer.add(mapTables.get(tableName).jsonToContentValues(jsonObj.optJSONObject("data"))) ;
		    		nbRows++ ;
		    		nbRowstmp++ ;
		    		
		    		if( nbRowstmp > 1000 ) {
		    			upgradeReferentielStream_bulkInsert(tableName,insertBuffer) ;
		    			insertBuffer = new ArrayList<ContentValues>() ;
		    			nbRowstmp = 0 ;
		    		}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					break ;
				}
			}
			upgradeReferentielStream_bulkInsert(tableName,insertBuffer) ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return new DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
		}
    	
    	if( nbTables==nbTablesExpected && nbRowsExpected==nbRows ) {
        	return new DatabaseUpgradeResult( true, versionTimestamp, nbTables , nbRows ) ;
    	}
    	else{
    		upgradeReferentielStream_purge() ;
    		return new DatabaseUpgradeResult( false, 0, 0 , 0 ) ;
    	}

    	
    }
    public void upgradeReferentielStream_bulkInsert( String tableName , ArrayList<ContentValues> cvs ) {
    	if( cvs.isEmpty() ){
    		return ;
    	}
    	
    	mDb.beginTransaction() ;
    	Iterator<ContentValues> iter = cvs.iterator() ;
    	ContentValues cv ;
    	while( iter.hasNext() ) {
    		cv = iter.next() ;
    		mDb.insert(tableName, "NULL", cv);
    	}
    	
    	mDb.setTransactionSuccessful() ;
    	mDb.endTransaction() ;
    }
    public void upgradeReferentielStream_purge(){
    	String[] tables = {"define_bible",
    	                   "define_bible_entry",
    	                   "define_bible_tree",
    	                   "define_file",
    	                   "define_file_entry",
    	                   "input_scen",
    	                   "input_scen_page",
    	                   "input_scen_page_field",
    	                   "store_bible_entry",
    	                   "store_bible_entry_field",
    	                   "store_bible_tree",
    	                   "store_bible_tree_field"} ;
    	for(int a=0 ; a<tables.length ; a++){
    		mDb.execSQL(String.format("DELETE FROM %s",tables[a]));
    	}
    }
    
    public DatabaseUpgradeResult purgeReferentiel() {
    	int nbTables = 0 ;

		String curTable ;
		Cursor tmpCursor = mDb.rawQuery( String.format("SELECT name FROM sqlite_master WHERE type='table'"),null) ;
		if( tmpCursor.getCount() != 1 ) {
			mDb.beginTransaction() ;
	    	while( !tmpCursor.isLast() ){
	    		tmpCursor.moveToNext();
	    		curTable = tmpCursor.getString(0) ;
	    		
	    		// vidage table existe ?
	    		mDb.execSQL(String.format("DELETE FROM %s",curTable));
	    		
	    		nbTables++ ;
	    	}
	    	
	    	mDb.setTransactionSuccessful() ;
	    	mDb.endTransaction() ;
		}
    	
    	return new DatabaseUpgradeResult( true, 0, nbTables , 0 ) ;
    }

    public JSONArray dumpTable(String tableName) {
    	JSONArray jsonArray = new JSONArray() ;
    	
    	String columnNames[] ;
    	Cursor c = mDb.rawQuery(String.format("SELECT * FROM %s WHERE 0",tableName),
    			null);
    	try {
    	    columnNames = c.getColumnNames();
    	} finally {
    	    c.close();
    	}
    	
    	Columns cols = new Columns(columnNames);

		Cursor tmpCursor = mDb.rawQuery( String.format("SELECT * FROM %s",tableName), null) ;
		ContentValues cv ;
		if( tmpCursor.getCount() > 0 ) {
			while( !tmpCursor.isLast() ){
				tmpCursor.moveToNext();

				cv = new ContentValues() ;
				DatabaseUtils.cursorRowToContentValues(tmpCursor, cv)  ;
				jsonArray.put(cols.contentValueToJSON(cv)) ;
			}
		}
    	
    	
    	return jsonArray ;
    }
    
    public Cursor rawQuery( String mQuery ){
    	return mDb.rawQuery( mQuery, null ) ;
    }
    public void beginTransaction() {
    	mDb.beginTransaction() ;
    }
    public long insert( String table , ContentValues values ) {
    	return mDb.insert(table, "NULL", values);
    }
    public void execSQL( String sql ) {
    	mDb.execSQL(sql) ;
    }
    public void endTransaction() {
    	mDb.setTransactionSuccessful() ;
    	mDb.endTransaction() ;
    }
   
    
    public class Columns {

    	private String[] names;

    	public Columns(String[] names) {
    		this.names = names;
     	}

    	public boolean hasField(Cursor c, String name) {
    		int i = c.getColumnIndex(name);
    		return ((i != -1) && !c.isNull(i));
    	}

    	public JSONObject contentValueToJSON(ContentValues cv) {
    		JSONObject json = new JSONObject();
    		for (int i = 0; i < names.length; i++) {
    			if (!cv.containsKey(names[i])) {
    				continue;
    			}
    			try {
    				json.put(names[i], cv.get(names[i]));
     			} catch (JSONException e) {
    				Log.e("Col", "Invalid type, can't unserialize ", e);
    			}
    		}

    		return json;
    	}

    	public ContentValues jsonToContentValues(JSONObject j) {
    		ContentValues cv = new ContentValues();
    		for (int i = 0; i < names.length; i++) {
    			j2cv(j, cv, names[i]);
    		}

    		return cv;
    	}

    	private void j2cv(JSONObject j, ContentValues cv, String key) {
    		try {
    			cv.put(key, j.get(key).toString());
    		} catch (JSONException e) {
    		}
    	}
   }    
}