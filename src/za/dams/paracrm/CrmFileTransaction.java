package za.dams.paracrm;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class CrmFileTransaction {
	public final static String TAG = "PARACRM/CrmFileTransaction";
	
	private Context mContext ;
	private DatabaseManager mDb ;
	
	private int CrmInputScenId ;
	private String CrmFileCode ;
	
	private ArrayList<CrmFilePageinfo> TransactionPages ;
	private ArrayList<ArrayList<CrmFileFieldDesc>>  TransactionPageFields ;
	private ArrayList<ArrayList<CrmFileRecord>> TransactionPageRecords ;
	
	public enum PageType {
		PAGETYPE_LIST, PAGETYPE_LOGLIST, PAGETYPE_TABLE, PAGETYPE_PHOTO, PAGETYPE_SAVE, PAGETYPE_NULL
	}
	public enum PageTableType {
		TABLE_NULL, TABLE_MANUAL, TABLE_AUTOFILL_DISABLED, TABLE_AUTOFILL_ENABLED
	}
	public enum FieldType {
	    FIELD_TEXT, FIELD_NUMBER, FIELD_DATE, FIELD_DATETIME, FIELD_BIBLE, FIELD_NULL
	};
	
	public static class CrmFilePageinfo {
    	public int pageId ;
    	public PageType pageType ;
    	public PageTableType pageTableType ;
    	public String pageCode ;
    	public String pageLib ;
    	public String fileCode ;
    	public String fileLib ;
    	public boolean fileIsSubfile ;
    	public boolean fileHasGmap ;
    	public boolean pageIsHidden ;
    	public boolean loadIsLoadable ;
    	public boolean loadIsLoaded ;
	
    	public CrmFilePageinfo( int aPageId , PageType aPageType , PageTableType aPageTableType , String aFileCode, String aFileLib, boolean aIsSubfile, boolean aFileHasGmap ) {
    		pageId = aPageId ;
    		pageType = aPageType ;
    		pageTableType = aPageTableType ;
    		pageCode = aFileCode ;
    		pageLib = aFileLib ;
    		fileCode = aFileCode ;
    		fileLib = aFileLib ;
    		fileIsSubfile = aIsSubfile ;
    		if( !fileIsSubfile ){
    			fileHasGmap = aFileHasGmap ;
    		}
    		pageIsHidden = true ;
    	}
    	public CrmFilePageinfo( JSONObject jsonObject ) {
    		try {
				pageId = jsonObject.getInt("pageId");
				pageType = PageType.values()[jsonObject.getInt("pageType")] ;
				pageTableType = PageTableType.values()[jsonObject.getInt("pageTableType")] ;
				pageCode = jsonObject.getString("pageCode");
				pageLib = jsonObject.getString("pageLib");
				fileCode = jsonObject.getString("fileCode");
				fileLib = jsonObject.getString("fileLib");
				fileIsSubfile = jsonObject.getBoolean("fileIsSubfile");
				fileHasGmap = jsonObject.getBoolean("fileHasGmap");
				pageIsHidden = jsonObject.getBoolean("pageIsHidden");
				loadIsLoadable = jsonObject.getBoolean("loadIsLoadable");
				loadIsLoaded = jsonObject.getBoolean("loadIsLoaded");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
				jsonObject.put("pageId", pageId) ;
				jsonObject.put("pageType",pageType.ordinal()) ;
				jsonObject.put("pageTableType",pageTableType.ordinal()) ;
				jsonObject.put("pageCode", pageCode) ;
				jsonObject.put("pageLib", pageLib) ;
				jsonObject.put("fileCode", fileCode) ;
				jsonObject.put("fileLib", fileLib) ;
				jsonObject.put("fileIsSubfile", fileIsSubfile) ;
				jsonObject.put("fileHasGmap", fileHasGmap) ;
				jsonObject.put("pageIsHidden", pageIsHidden) ;
				jsonObject.put("loadIsLoadable", loadIsLoadable) ;
				jsonObject.put("loadIsLoaded", loadIsLoaded) ;
				return jsonObject ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null ;
			}
    	}
	}
	
	public static class CrmFileFieldValue implements Cloneable {
    	public FieldType fieldType ;
    	public float valueFloat ;
    	public boolean valueBoolean ;
    	public String valueString ;
    	public Date valueDate ;
    	public String displayStr ;
    	public boolean isSet ;
   	
    	public CrmFileFieldValue( FieldType fieldType,
    			float valueFloat ,
    			boolean valueBoolean ,
    			String valueString ,
    			Date valueDate ,
    			String displayStr,
    			boolean isSet )  {
    		this.fieldType = fieldType ;
    		this.valueFloat = valueFloat ;
    		this.valueBoolean = valueBoolean ;
    		this.valueString = valueString ;
    		this.valueDate = valueDate ;
    		this.displayStr = displayStr ;
    		this.isSet = isSet ;
    	}
    	public CrmFileFieldValue( FieldType fieldType,
    			float valueFloat ,
    			boolean valueBoolean ,
    			String valueString ,
    			Date valueDate ,
    			String displayStr )  {
    		this.fieldType = fieldType ;
    		this.valueFloat = valueFloat ;
    		this.valueBoolean = valueBoolean ;
    		this.valueString = valueString ;
    		this.valueDate = valueDate ;
    		this.displayStr = displayStr ;
    	}
    	public CrmFileFieldValue( JSONObject jsonObject ) {
    		try {
				this.fieldType = FieldType.values()[jsonObject.getInt("fieldType")] ;
				this.valueFloat = Float.parseFloat(jsonObject.optString("valueFloat","")) ;
				this.valueBoolean = jsonObject.getBoolean("valueBoolean") ;
				if( jsonObject.has("valueString") ) {
					this.valueString = jsonObject.getString("valueString") ;
				}
				if( jsonObject.has("valueDate") ) {
					this.valueDate = new Date(jsonObject.getLong("valueDate")) ;
				}
				if( jsonObject.has("displayStr") ) {
					this.displayStr = jsonObject.getString("displayStr") ;
				}
				this.isSet = jsonObject.getBoolean("isSet") ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	public CrmFileFieldValue clone() {
    		Object o = null;
    		try {
    			// On récupère l'instance à renvoyer par l'appel de la 
    			// méthode super.clone()
    			o = super.clone();
    		} catch(CloneNotSupportedException cnse) {
    			// Ne devrait jamais arriver car nous implémentons 
    			// l'interface Cloneable
    			// cnse.printStackTrace(System.err);
    		}
    		// on renvoie le clone
    		return (CrmFileFieldValue)o;
    	}
    	public JSONObject toJSONObject() {
    		try{
    			JSONObject jsonObject = new JSONObject();
    			jsonObject.put("fieldType", this.fieldType.ordinal()) ;
    			jsonObject.put("valueFloat", new Float(this.valueFloat).toString()) ;
    			jsonObject.put("valueBoolean", this.valueBoolean) ;
    			if( this.valueDate != null ) {
    				jsonObject.put("valueDate", this.valueDate.getTime()) ;
    			}
    			if( this.valueString != null ) {
    				jsonObject.put("valueString", this.valueString) ;
    			}
    			if( this.displayStr != null ) {
    				jsonObject.put("displayStr", this.displayStr) ;
    			}
    			jsonObject.put("isSet", this.isSet) ;
    			return jsonObject ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null ;
			}
    	}
	}
	
	public static class CrmFileFieldDesc implements Cloneable {
    	public FieldType fieldType ;
    	public String fieldLinkBible ;
    	public String fieldCode ;
    	public String fieldName ;
    	public boolean fieldIsPivot ;
    	public boolean fieldIsReadonly ;
    	
    	public boolean fieldIsHighlight ;
    	
    	public boolean fieldAutovalueIsOn ;
    	public String fieldAutovalueSrc ;
    	public CrmFileFieldValue fieldAutovalue ;
    	
    	public boolean fieldSearchIsCondition ;
    	
    	
    	
    	public CrmFileFieldDesc( FieldType fieldType, String fieldLinkBible, String fieldCode, String fieldName, boolean fieldIsPivot, boolean fieldIsReadonly )  {
    		this.fieldType = fieldType ;
    		this.fieldLinkBible = fieldLinkBible ;
    		this.fieldCode = fieldCode ;
    		this.fieldName = fieldName ;
    		this.fieldIsPivot = fieldIsPivot ;
    		this.fieldIsReadonly = fieldIsReadonly ;
    	}
    	public CrmFileFieldDesc( FieldType fieldType, String fieldLinkBible, String fieldCode, String fieldName, boolean fieldIsHighlight )  {
    		this.fieldType = fieldType ;
    		this.fieldLinkBible = fieldLinkBible ;
    		this.fieldCode = fieldCode ;
    		this.fieldName = fieldName ;
    		this.fieldIsHighlight = fieldIsHighlight ;
    	}
    	public CrmFileFieldDesc( JSONObject jsonObject ) {
    		try {
				this.fieldType = FieldType.values()[jsonObject.getInt("fieldType")] ;
				this.fieldLinkBible = jsonObject.getString("fieldLinkBible") ;
				this.fieldCode = jsonObject.getString("fieldCode") ;
				this.fieldName = jsonObject.getString("fieldName") ;
				this.fieldIsPivot = jsonObject.optBoolean("fieldIsPivot",false) ;
				this.fieldIsReadonly = jsonObject.optBoolean("fieldIsReadonly",false) ;
				this.fieldIsHighlight = jsonObject.optBoolean("fieldIsHighlight",false) ;
				this.fieldAutovalueIsOn = jsonObject.optBoolean("fieldAutovalueIsOn",false) ;
				this.fieldAutovalueSrc = jsonObject.optString("fieldAutovalueSrc",null) ;
				if( jsonObject.has("fieldAutovalue") ) {
					this.fieldAutovalue = new CrmFileFieldValue(jsonObject.getJSONObject("fieldAutovalue")) ;
				}
				this.fieldSearchIsCondition = jsonObject.optBoolean("fieldSearchIsCondition",false) ;
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	public CrmFileFieldDesc clone() {
    		Object o = null;
    		try {
    			// On récupère l'instance à renvoyer par l'appel de la 
    			// méthode super.clone()
    			o = super.clone();
    		} catch(CloneNotSupportedException cnse) {
    			// Ne devrait jamais arriver car nous implémentons 
    			// l'interface Cloneable
    			// cnse.printStackTrace(System.err);
    		}
    		// on renvoie le clone
    		return (CrmFileFieldDesc)o;
    	}
    	
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
    			jsonObject.put("fieldType",this.fieldType.ordinal()) ;
    			jsonObject.put("fieldLinkBible",this.fieldLinkBible) ;
    			jsonObject.put("fieldCode",this.fieldCode) ;
    			jsonObject.put("fieldName",this.fieldName) ;
    			jsonObject.put("fieldIsPivot",this.fieldIsPivot) ;
    			jsonObject.put("fieldIsReadonly",this.fieldIsReadonly) ;
    			jsonObject.put("fieldIsHighlight",this.fieldIsHighlight) ;
    			if( this.fieldAutovalueIsOn ) {
    				jsonObject.put("fieldAutovalueIsOn",this.fieldAutovalueIsOn) ;
    				jsonObject.put("fieldAutovalueSrc",this.fieldAutovalueSrc) ;
    				jsonObject.put("fieldAutovalue",this.fieldAutovalue.toJSONObject()) ;
    			}
    			jsonObject.put("fieldSearchIsCondition",this.fieldSearchIsCondition) ;
    			
    			return jsonObject ;
    			
    		} catch (JSONException e) {
    			e.printStackTrace();
    			return null ;
    		}
    	}
	}
	
	public static class CrmFileRecord {
    	public int recordTmpId ;
    	public HashMap<String,CrmFileFieldValue> recordData ;
    	public CrmFilePhoto recordPhoto ;
    	public boolean recordIsDisabled ;
    	public boolean recordIsHidden ;
    	
    	public CrmFileRecord( int recordTmpId, HashMap<String,CrmFileFieldValue> recordData, boolean recordIsDisabled )  {
    		this.recordTmpId = recordTmpId ;
    		this.recordData = recordData ;
    		this.recordIsDisabled = recordIsDisabled ;
    		this.recordIsHidden = false ;
    	}
    	public CrmFileRecord( int recordTmpId, CrmFilePhoto recordPhoto, boolean recordIsDisabled )  {
    		this.recordTmpId = recordTmpId ;
    		this.recordPhoto = recordPhoto ;
    		this.recordIsDisabled = recordIsDisabled ;
    		this.recordIsHidden = false ;
    	}
    	
    	public CrmFileRecord( JSONObject jsonObject ) {
    		try {
				this.recordTmpId = jsonObject.getInt("recordTmpId") ;
				if( jsonObject.has("recordPhoto") ) {
					this.recordPhoto = new CrmFilePhoto( jsonObject.getJSONObject("recordPhoto")) ;
				}
				if( jsonObject.has("recordData") ) {
					JSONObject jsonRecordData = jsonObject.getJSONObject("recordData") ;
					this.recordData = new HashMap<String,CrmFileFieldValue>() ;
					Iterator<String> iterKeys = jsonRecordData.keys() ;
					while( iterKeys.hasNext() ) {
						String key = iterKeys.next() ;
						CrmFileFieldValue cffv = new CrmFileFieldValue(jsonRecordData.getJSONObject(key)) ;
						this.recordData.put(key, cffv) ;
					}
				}
				this.recordIsDisabled = jsonObject.getBoolean("recordIsDisabled") ;
				this.recordIsHidden = jsonObject.getBoolean("recordIsHidden") ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    	}
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
    			jsonObject.put("recordTmpId", this.recordTmpId) ;
    			if( this.recordPhoto != null ) {
    				jsonObject.put("recordPhoto", this.recordPhoto.toJSONObject()) ;
    			}
    			if( this.recordData != null ) {
    				JSONObject jsonRecordData = new JSONObject() ;
    				for( Entry<String,CrmFileFieldValue> map : this.recordData.entrySet() ) {
    					jsonRecordData.put(map.getKey(), map.getValue().toJSONObject()) ;
    					map.getValue() ;
    				}
    				jsonObject.put("recordData", jsonRecordData) ;
    			}
    			jsonObject.put("recordIsDisabled", this.recordIsDisabled) ;
    			jsonObject.put("recordIsHidden", this.recordIsHidden) ;
    			
    			
    			return jsonObject ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return null ;
			}
    	}
	}
	
	
    public static class CrmFilePhoto {
    	public String uriString ;
    	public String uriStringThumb ;
    	
    	public CrmFilePhoto( String uriString, String uriStringThumb ) {
    		this.uriString = uriString ;
    		this.uriStringThumb = uriStringThumb ;
    	}
    	public CrmFilePhoto( JSONObject jsonObject ) {
    		try {
				this.uriString = jsonObject.getString("uriString") ;
				this.uriStringThumb = jsonObject.getString("uriStringThumb") ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    	}
    	
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
    			jsonObject.put("uriString",this.uriString) ;
    			jsonObject.put("uriStringThumb",this.uriStringThumb) ;
    			return jsonObject ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return null ;
			}
    	}
    }
	
    public CrmFileTransaction( Context c , JSONObject jsonObj ) {
		this.mContext = c ;
		mDb = DatabaseManager.getInstance(c) ;
		
		try {
			this.CrmInputScenId = jsonObj.getInt("CrmInputScenId") ;
			this.CrmFileCode = jsonObj.getString("CrmFileCode") ;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		TransactionPages = new ArrayList<CrmFilePageinfo>() ;
		TransactionPageFields = new ArrayList<ArrayList<CrmFileFieldDesc>>() ;
		TransactionPageRecords = new ArrayList<ArrayList<CrmFileRecord>>() ;
		
		try {
			JSONArray JsonTransactionPages = jsonObj.getJSONArray("TransactionPages") ;
			for( int idx=0 ; idx<JsonTransactionPages.length() ; idx++ ) {
				TransactionPages.add( new CrmFilePageinfo( JsonTransactionPages.getJSONObject(idx) ) ) ;
			}
		} catch( JSONException e ) {
			e.printStackTrace() ;
		}
		
		try {
			JSONArray JsonTransactionPageFields = jsonObj.getJSONArray("TransactionPageFields") ;
			for( int idx=0 ; idx<JsonTransactionPageFields.length() ; idx++ ) {
				ArrayList<CrmFileFieldDesc> TransactionPageFields_intra = new ArrayList<CrmFileFieldDesc>() ;
				JSONArray JsonTransactionPageFields_intra = JsonTransactionPageFields.getJSONArray(idx) ;
				for( int idx2=0 ; idx2<JsonTransactionPageFields_intra.length() ; idx2++ ) {
					TransactionPageFields_intra.add( new CrmFileFieldDesc( JsonTransactionPageFields_intra.getJSONObject(idx2) ) ) ;
				}
				TransactionPageFields.add( TransactionPageFields_intra ) ;
			}
		} catch( JSONException e ) {
			e.printStackTrace() ;
		}
		
		try {
			JSONArray JsonTransactionPageRecords = jsonObj.getJSONArray("TransactionPageRecords") ;
			for( int idx=0 ; idx<JsonTransactionPageRecords.length() ; idx++ ) {
				ArrayList<CrmFileRecord> TransactionPageRecords_intra = new ArrayList<CrmFileRecord>() ;
				JSONArray JsonTransactionPageRecords_intra = JsonTransactionPageRecords.getJSONArray(idx) ;
				for( int idx2=0 ; idx2<JsonTransactionPageRecords_intra.length() ; idx2++ ) {
					TransactionPageRecords_intra.add( new CrmFileRecord( JsonTransactionPageRecords_intra.getJSONObject(idx2) ) ) ;
				}
				TransactionPageRecords.add( TransactionPageRecords_intra ) ;
			}
		} catch( JSONException e ) {
			e.printStackTrace() ;
		}
		
		
    }
	
	public CrmFileTransaction( Context c , int CrmInputScenId ) {
		this.mContext = c ;
		mDb = DatabaseManager.getInstance(c) ;
		this.CrmInputScenId = CrmInputScenId ;
		
		
		TransactionPages = new ArrayList<CrmFilePageinfo>() ;
		TransactionPageFields = new ArrayList<ArrayList<CrmFileFieldDesc>>() ;
		TransactionPageRecords = new ArrayList<ArrayList<CrmFileRecord>>() ;
		
		
		Cursor tmpCursor ;	
		Cursor tmpInnerCursor ;	
		tmpCursor = mDb.rawQuery( String.format("SELECT target_filecode FROM input_scen WHERE scen_id='%d'",CrmInputScenId) ) ;
		if( tmpCursor.getCount() < 1 ) {
			tmpCursor.close() ;
			return ;
		}
		tmpCursor.moveToNext();
		String CrmFileCode = tmpCursor.getString(0) ;
		this.CrmFileCode = CrmFileCode ;
		tmpCursor.close() ;
		
		// ******** Ajout de toutes les pages **********
		tmpCursor = mDb.rawQuery( String.format("SELECT scen_page_name, target_filecode, page_type, page_table_type, scen_page_index FROM input_scen_page WHERE scen_id='%d' ORDER BY scen_page_index",CrmInputScenId) ) ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		
    		PageType pageType = PageType.PAGETYPE_NULL;
    		String sPageType = tmpCursor.getString(2) ;
    		if( sPageType.equals("PAGETYPE_PHOTO") )
    			pageType = PageType.PAGETYPE_PHOTO ;
    		if( sPageType.equals("PAGETYPE_LIST") )
    			pageType = PageType.PAGETYPE_LIST ;
    		if( sPageType.equals("PAGETYPE_LOGLIST") )
    			pageType = PageType.PAGETYPE_LOGLIST ;
    		if( sPageType.equals("PAGETYPE_TABLE") )
    			pageType = PageType.PAGETYPE_TABLE ;
    		if( sPageType.equals("PAGETYPE_SAVE") )
    			pageType = PageType.PAGETYPE_SAVE ;
    		
    		PageTableType pagetableType = PageTableType.TABLE_NULL;
    		String sPageTableType = tmpCursor.getString(3) ;
    		if( sPageTableType.equals("TABLE_AUTOFILL_ENABLED") )
    			pagetableType = PageTableType.TABLE_AUTOFILL_ENABLED ;
    		if( sPageTableType.equals("TABLE_AUTOFILL_DISABLED") )
    			pagetableType = PageTableType.TABLE_AUTOFILL_DISABLED ;
     		
    		
    		if( pageType == PageType.PAGETYPE_SAVE ){
    			TransactionPages.add( new CrmFilePageinfo(tmpCursor.getInt(4),pageType,pagetableType,"",tmpCursor.getString(0) ,false,false) ) ;
    			continue ;
    		}
    		
    		
    		tmpInnerCursor = mDb.rawQuery( String.format("SELECT file_code, file_lib, gmap_is_on, file_parent_code FROM define_file WHERE file_code='%s'",tmpCursor.getString(1)) ) ;
    		if( tmpInnerCursor.getCount() < 1 ) {
    			tmpInnerCursor.close() ;
    			continue ;
    		}
    		tmpInnerCursor.moveToNext() ;
    		
    		CrmFilePageinfo tmpPageInfo = new CrmFilePageinfo( tmpCursor.getInt(4), pageType,pagetableType, tmpInnerCursor.getString(0) , tmpCursor.getString(0) , !(tmpInnerCursor.getString(3).equals("")), tmpInnerCursor.getString(2).equals("O") ) ;
    		if( pageType == PageType.PAGETYPE_LOGLIST ) {
    			tmpPageInfo.loadIsLoadable = true ;
    			tmpPageInfo.loadIsLoaded = false ;
    		}
    		TransactionPages.add( tmpPageInfo ) ;
    		
    		/*
    		if( tmpInnerCursor.getString(2).equals("media_img") ) {
    			TransactionPages.add( new CrmFilePageinfo( pageType,pagetableType, tmpInnerCursor.getString(0) , tmpCursor.getString(0) , !(tmpInnerCursor.getString(3).equals("")), tmpInnerCursor.getString(2).equals("O") ) ) ;
    		}
    		else {
    			TransactionPages.add( new CrmFilePageinfo( pageType,pagetableType, tmpInnerCursor.getString(0) , tmpCursor.getString(0) , !(tmpInnerCursor.getString(3).equals("")), tmpInnerCursor.getString(2).equals("O") ) ) ;
    		}
    		*/
    		tmpInnerCursor.close() ;
    	}
    	tmpCursor.close() ;
		
		/*
		tmpCursor = mDb.rawQuery( String.format("SELECT file_code, file_lib, gmap_is_on FROM define_file WHERE file_code='%s'",CrmFileCode) ) ;
		if( tmpCursor.getCount() < 1 ) {
			return ;
		}
		tmpCursor.moveToNext();
		TransactionPages.add( new CrmFilePageinfo( PageType.PAGETYPE_LIST, tmpCursor.getString(0) , tmpCursor.getString(1) , false, tmpCursor.getString(2).equals("O") ) ) ;
		tmpCursor = mDb.rawQuery( String.format("SELECT file_code, file_lib, file_type FROM define_file WHERE file_parent_code='%s' ORDER BY file_code",CrmFileCode) ) ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		if( tmpCursor.getString(2).equals("media_img") ) {
    			TransactionPages.add( new CrmFilePageinfo( PageType.PAGETYPE_PHOTO, tmpCursor.getString(0) , tmpCursor.getString(1) , true, false ) ) ;
    		}
    		else {
    			TransactionPages.add( new CrmFilePageinfo( PageType.PAGETYPE_TABLE, tmpCursor.getString(0) , tmpCursor.getString(1) , true, false ) ) ;
    		}
    		
    	}
    	*/

    	
    	Iterator<CrmFileTransaction.CrmFilePageinfo> mIter = TransactionPages.iterator() ;
    	CrmFileTransaction.CrmFilePageinfo tFileinfo ;
    	int tmpIndex = 0 ;
    	while( mIter.hasNext() ){
    		tFileinfo = mIter.next() ;
    		
    		CrmFileTransaction_initPage(tFileinfo) ;
    		
    		tmpIndex++ ;
    	}
    	
    	links_refresh() ;
    	
    	return ;
	}
	private void CrmFileTransaction_initPage( CrmFilePageinfo tFileinfo ) {
		if( tFileinfo.pageType == PageType.PAGETYPE_SAVE 
				|| tFileinfo.pageType == PageType.PAGETYPE_NULL ) {
			TransactionPageFields.add( new ArrayList<CrmFileFieldDesc>() ) ;
			TransactionPageRecords.add( new ArrayList<CrmFileRecord>() ) ;
			return ;
		}
		
		if( tFileinfo.pageType == PageType.PAGETYPE_PHOTO ) {
			TransactionPageFields.add( new ArrayList<CrmFileFieldDesc>() ) ;
			TransactionPageRecords.add( new ArrayList<CrmFileRecord>() ) ;
			return ;
		}
		
		// ******** Load de tous les FIELDS ************
		
		
		ArrayList<CrmFileFieldDesc> tFields = new ArrayList<CrmFileFieldDesc>() ;
		
		Cursor tmpCursor ;
		Cursor tmpInnerCursor ;
		
		/*
		// --- field "pivot"
		String pivotFieldCode = null ;
		String localFileCode = tFileinfo.fileCode ;
		if( tFileinfo.fileIsSubfile ){
			tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_code " +
					"FROM define_file_entry " +
					"WHERE file_code='%s' AND entry_field_type='link' AND entry_field_linkbible<>''",
					localFileCode) ) ;
			if( tmpCursor.getCount() == 1 ) {
				tmpCursor.moveToNext();
				pivotFieldCode = tmpCursor.getString(0) ;
			}
		}
		
		
		tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_type, entry_field_code, entry_field_lib, entry_field_linkbible " +
				"FROM define_file_entry " +
				"WHERE file_code='%s' ORDER BY entry_field_index",
				localFileCode) ) ;
		boolean fieldIsPivot ;
		FieldType tFieldType ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		
    		fieldIsPivot = false ;
    		tFieldType = null ;
   		
    		if( tmpCursor.getString(0).equals("number") ) {
    			tFieldType = FieldType.FIELD_NUMBER ;
    		}
    		else {
    			if( tmpCursor.getString(0).equals("date") ) {
    				tFieldType = FieldType.FIELD_DATETIME ;
    			}
    			else{
        			if( tmpCursor.getString(0).equals("link") ) {
        				tFieldType = FieldType.FIELD_BIBLE ;
        				if( pivotFieldCode != null && pivotFieldCode.equals(tmpCursor.getString(1)) ) {
        					fieldIsPivot = true ;
        				}
        			}
        			else{
        				tFieldType = FieldType.FIELD_TEXT ;
        			}
    			}
    		}
    		
    		
    		tFields.add( new CrmFileFieldDesc( tFieldType, tmpCursor.getString(3),
    				tmpCursor.getString(1) , tmpCursor.getString(2) , fieldIsPivot, false ) ) ;
    	}
    	TransactionPageFields.add( tFields ) ;
		*/
		tmpCursor = mDb.rawQuery( String.format("SELECT target_filecode, target_filefield, field_is_pivot, field_autovalue_is_on, field_autovalue_src, search_is_condition " +
				"FROM input_scen_page_field " +
				"WHERE scen_id='%d' AND scen_page_index='%d' ORDER BY scen_page_field_index",
				CrmInputScenId, tFileinfo.pageId )) ;
		
		
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		
    		tmpInnerCursor = mDb.rawQuery( String.format("SELECT entry_field_type, entry_field_code, entry_field_lib, entry_field_linkbible " +
    				"FROM define_file_entry " +
    				"WHERE file_code='%s' AND entry_field_code='%s' ORDER BY entry_field_index",
    				tmpCursor.getString(0),tmpCursor.getString(1)) ) ;
    		if( tmpInnerCursor.getCount() < 1 ) {
    			continue ;
    		}
    		tmpInnerCursor.moveToNext();
    		
    		FieldType tFieldType = null ;
   		
    		if( tmpInnerCursor.getString(0).equals("number") ) {
    			tFieldType = FieldType.FIELD_NUMBER ;
    		}
    		else {
    			if( tmpInnerCursor.getString(0).equals("date") ) {
    				tFieldType = FieldType.FIELD_DATETIME ;
    			}
    			else{
        			if( tmpInnerCursor.getString(0).equals("link") ) {
        				tFieldType = FieldType.FIELD_BIBLE ;
        			}
        			else{
        				tFieldType = FieldType.FIELD_TEXT ;
        			}
    			}
    		}
    		
    		CrmFileFieldDesc tmpField = new CrmFileFieldDesc( tFieldType, tmpInnerCursor.getString(3),
    				tmpInnerCursor.getString(1) , tmpInnerCursor.getString(2) , tmpCursor.getString(2).equals("O"), false ) ;
    		if( tmpCursor.getString(3).equals("O") ) {
    			tmpField.fieldAutovalueIsOn = true ;
    			tmpField.fieldAutovalueSrc = tmpCursor.getString(4) ;
    		}
    		if( tmpCursor.getString(5).equals("O") ) {
    			tmpField.fieldSearchIsCondition = true ;
    		}
    		tFields.add( tmpField ) ;
    	}
    	tmpCursor.close() ;
    	TransactionPageFields.add( tFields ) ;
		
    	if( tFileinfo.pageType == PageType.PAGETYPE_TABLE ) {
    		if( true ) {
    			CrmFileTransaction_initPage_preloadDataFromPivot( tFileinfo , tFields, tFileinfo.pageTableType==PageTableType.TABLE_AUTOFILL_ENABLED ) ;
    		}
    	}
    	else {
    		CrmFileTransaction_initPage_preloadDataSingle( tFileinfo , tFields ) ;
    	}
    	
    	
    	
		
		return ;
	}
	private void CrmFileTransaction_initPage_preloadDataSingle( CrmFilePageinfo tFileinfo , ArrayList<CrmFileFieldDesc> tFields ) {
		
		ArrayList<CrmFileRecord> tRecords = new ArrayList<CrmFileRecord>();
		
    	Iterator<CrmFileFieldDesc> mIter = tFields.iterator() ;
    	CrmFileFieldDesc tFieldDesc ;
    	HashMap<String,CrmFileFieldValue> recordData = new HashMap<String,CrmFileFieldValue>() ;
    	while( mIter.hasNext() ){
    		tFieldDesc = mIter.next() ;
    		switch( tFieldDesc.fieldType ) {
    		case FIELD_DATE :
    		case FIELD_DATETIME :
    			recordData.put(tFieldDesc.fieldCode,
    					new CrmFileFieldValue(tFieldDesc.fieldType,
    							new Float(0),
    							false,
    							null,
    							new Date(),
    							new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()),
    							true) ) ;
    			break ;
    			
    		case FIELD_TEXT :
       			recordData.put(tFieldDesc.fieldCode,
    					new CrmFileFieldValue(tFieldDesc.fieldType,
    							new Float(0),
    							false,
    							new String(""),
    							null,
    							new String(""),
    							tFileinfo.pageType==PageType.PAGETYPE_LOGLIST ) ) ;
    			break ;
    			
    		case FIELD_BIBLE :
       			recordData.put(tFieldDesc.fieldCode,
    					new CrmFileFieldValue(tFieldDesc.fieldType,
    							new Float(0),
    							false,
    							new String(""),
    							null,
    							new String("Bible ???") ) ) ;
    			break ;
    			
    		case FIELD_NUMBER :
       			recordData.put(tFieldDesc.fieldCode,
    					new CrmFileFieldValue(tFieldDesc.fieldType,
    							new Float(0),
    							false,
    							null,
    							null,
    							new Float(0).toString()) ) ;
    			break ;
    		
    		default :
       			recordData.put(tFieldDesc.fieldCode,
    					new CrmFileFieldValue(tFieldDesc.fieldType,
    							new Float(0),
    							false,
    							null,
    							null,
    							null) ) ;
    		}
    	}
    	tRecords.add( new CrmFileRecord(0,recordData,false) ) ;
    	TransactionPageRecords.add(tRecords) ;
	}
	private void CrmFileTransaction_initPage_preloadDataFromPivot( CrmFilePageinfo tFileinfo , ArrayList<CrmFileFieldDesc> tFields, boolean enableAll ) {
		// TransactionPageRecords.add(new ArrayList<CrmFileRecord>()) ;
		
		ArrayList<CrmFileRecord> tRecords = new ArrayList<CrmFileRecord>();
		
		Iterator<BibleHelper.BibleEntry> bibleIter ;
    	Iterator<CrmFileFieldDesc> mIter = tFields.iterator() ;
    	CrmFileFieldDesc tFieldDesc ;
    	HashMap<String,CrmFileFieldValue> recordData = new HashMap<String,CrmFileFieldValue>() ;
    	while( mIter.hasNext() ){
    		tFieldDesc = mIter.next() ;
    		
    		if(tFieldDesc.fieldIsPivot) {
    			
    			
    			
    			// appel a bible helper
    			BibleHelper bibleHelper = new BibleHelper(mContext) ;
    			bibleIter = bibleHelper.queryBible(tFieldDesc.fieldLinkBible).iterator() ;
    			BibleHelper.BibleEntry bibleEntry ;
    			int a = 0 ;
    	    	while( bibleIter.hasNext() ){
    	    		bibleEntry = bibleIter.next() ; 
    	    		
    	    		recordData = new HashMap<String,CrmFileFieldValue>() ;
    	    		
    	    		// CREATION DU FIELD
    	    		mIter = tFields.iterator() ;
    	        	while( mIter.hasNext() ){
    	        		tFieldDesc = mIter.next() ;
    	        		if( tFieldDesc.fieldIsPivot ) {
    	           			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							new String(bibleEntry.entryKey),
    	        							null,
    	        							new String(bibleEntry.displayStr),
    	        							true) ) ;
    	        			
    	        			continue ;
    	        		}
    	        		
    	        		
    	        		
    	        		
    	        		switch( tFieldDesc.fieldType ) {
    	        		case FIELD_DATE :
    	        		case FIELD_DATETIME :
    	        			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							null,
    	        							new Date(),
    	        							new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) ) ) ;
    	        			break ;
    	        			
    	        		case FIELD_TEXT :
    	           			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							new String(""),
    	        							null,
    	        							new String("") ) ) ;
    	        			break ;
    	        			
    	        		case FIELD_BIBLE :
    	           			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							new String(""),
    	        							null,
    	        							new String("") ) ) ;
    	        			break ;
    	        			
    	        		case FIELD_NUMBER :
    	           			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							null,
    	        							null,
    	        							new Integer(0).toString()) ) ;
    	        			break ;
    	        		
    	        		default :
    	           			recordData.put(tFieldDesc.fieldCode,
    	        					new CrmFileFieldValue(tFieldDesc.fieldType,
    	        							new Float(0),
    	        							false,
    	        							null,
    	        							null,
    	        							null) ) ;
    	        		}
    	        	}
    	    		tRecords.add( new CrmFileRecord(a,recordData,!enableAll) ) ;
    	    	}
    	    	
    	    	break ;
    		}
    	}
    	TransactionPageRecords.add(tRecords) ;
	}
	
	
	/*
	public String getCrmFileCode(){
		return CrmFileCode ;
	}
	*/
	public int getCrmInputScenId() {
		return CrmInputScenId ;
	}
	
	
	public ArrayList<CrmFilePageinfo> list_getAllPages() {
		return TransactionPages ;
	}
	public PageType list_getPageType( int pageId ) {
		if( TransactionPages.get(pageId) != null ) {
			return TransactionPages.get(pageId).pageType ;
		}
		return PageType.PAGETYPE_NULL ;
	}
	
	
	public ArrayList<CrmFileFieldDesc> page_getFields( int pageId ) {
		if( TransactionPageFields.get(pageId) != null ) {
			return TransactionPageFields.get(pageId) ;
		}
		return new ArrayList<CrmFileFieldDesc>()  ;
	}
	public CrmFileFieldValue page_getRecordFieldValue( int pageId , int recordId, int fieldId ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) != null ) {
			return TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		}
		return new CrmFileFieldValue(FieldType.FIELD_NULL,0,false,new String(),new Date(),new String())  ;
	}
	public ArrayList<CrmFileRecord> pageTable_getRecords( int pageId ) {
		if( TransactionPageRecords.get(pageId) != null ) {
			return TransactionPageRecords.get(pageId) ;
		}
		return new ArrayList<CrmFileRecord>()  ;
	}
	public void page_setFieldReadonly( int pageId, int fieldId, boolean isReadonly ) {
		TransactionPageFields.get(pageId).get(fieldId).fieldIsReadonly = isReadonly ;
	}
	public void page_setRecordDisabled( int pageId , int recordId, boolean isDisabled ) {
		if( list_getPageType( pageId ) != PageType.PAGETYPE_TABLE ) {
			return ;
		}
		CrmFileRecord crmFileRecord = TransactionPageRecords.get(pageId).get(recordId) ;
		crmFileRecord.recordIsDisabled = isDisabled ;
	}
	public void page_setRecordFieldValue_bible( int pageId , int recordId, int fieldId, String entryKey ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			return ;
		}
		String bibleCode = TransactionPageFields.get(pageId).get(fieldId).fieldLinkBible ;
		
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		BibleHelper.BibleEntry tmpBibleEntry = new BibleHelper(mContext).getBibleEntry(bibleCode, entryKey) ;
		record.valueString = entryKey ;
		record.displayStr = tmpBibleEntry.displayStr ;
		record.isSet = true ;
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_date( int pageId , int recordId, int fieldId, Date date ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		record.valueDate = date ;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm") ;
		record.displayStr = sdf.format(date) ;
		record.isSet = true ;
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_text( int pageId , int recordId, int fieldId, String text ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		record.valueString = text ;
		record.displayStr = text ;
		record.isSet = true ;
		
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_number( int pageId , int recordId, int fieldId, float num ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		record.valueFloat = num ;
		if( num==Math.ceil(num) ) {
			record.displayStr = String.valueOf((int)num) ;
		}
		else {
			record.displayStr = String.valueOf(num) ;
		}
		record.isSet = true ;
		
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_unset( int pageId , int recordId, int fieldId ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		record.valueFloat = 0 ;
		record.valueString = new String("") ;
		record.isSet = false ;
		
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	
	
	public void page_addRecord_photo( int pageId, String uriString, String uriStringThumb ) {
		CrmFilePhoto fPhoto = new CrmFilePhoto(uriString, uriStringThumb) ;
		
		TransactionPageRecords.get(pageId).add(0,new CrmFileRecord(0,fPhoto,false)) ;

		return ;
	}
	
	public void page_populateRecordsFromJSON( int pageId, JSONArray data ) {
		
		ArrayList<CrmFileFieldDesc> tFields = TransactionPageFields.get(pageId) ;
		
    	Iterator<CrmFileFieldDesc> mIter ;
    	CrmFileFieldDesc tFieldDesc ;
    	
    	JSONObject jsonData ;
    	int a ;
    	for(a=0 ; a<data.length() ; a++ ){
    		jsonData = data.optJSONObject(a) ;
    		
        	mIter = tFields.iterator() ;
        	HashMap<String,CrmFileFieldValue> recordData = new HashMap<String,CrmFileFieldValue>() ;
        	while( mIter.hasNext() ){
        		tFieldDesc = mIter.next() ;
        		String dbFieldCode = "field_"+tFieldDesc.fieldCode ;
        		switch( tFieldDesc.fieldType ) {
        		case FIELD_DATE :
        		case FIELD_DATETIME :
        			Date tDate = new Date() ;
        			try {
						tDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(jsonData.getString(dbFieldCode)) ;
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
        			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(0),
        							false,
        							null,
        							tDate ,
        							new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate),
        							true) ) ;
        			break ;
        			
        		case FIELD_TEXT :
           			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(0),
        							false,
        							jsonData.optString(dbFieldCode),
        							null,
        							jsonData.optString(dbFieldCode),
        							true ) ) ;
        			break ;
        			
        		case FIELD_BIBLE :
           			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(0),
        							false,
        							new String(""),
        							null,
        							new String("Bible ???"),
        							true) ) ;
        			break ;
        			
        		case FIELD_NUMBER :
           			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(Float.parseFloat(jsonData.optString(dbFieldCode))),
        							false,
        							null,
        							null,
        							new Float(Float.parseFloat(jsonData.optString(dbFieldCode))).toString(),
        							true) ) ;
        			break ;
        		
        		default :
           			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(0),
        							false,
        							null,
        							null,
        							null) ) ;
        		}
        	}
        	TransactionPageRecords.get(pageId).add(new CrmFileRecord(a,recordData,false) ) ;
    	}
	}
	
	
	
	public boolean isComplete(){
		Iterator<ArrayList<CrmFileRecord>> iter1 = TransactionPageRecords.iterator() ;
		Iterator<CrmFileRecord> iter2 ;
		Iterator<String> iter3 ;
		String fieldCode ;
		int pageId = 0 ;
		CrmFileRecord record ;
		CrmFileFieldValue recValue ;
		while( iter1.hasNext() ){
			iter2 = iter1.next().iterator() ;
			while( iter2.hasNext() ){
				record = iter2.next() ;
				if( list_getPageType( pageId )==PageType.PAGETYPE_TABLE && record.recordIsDisabled ) {
					continue ;
				}
				if( list_getPageType( pageId )==PageType.PAGETYPE_TABLE && record.recordIsHidden ) {
					continue ;
				}
				if( record.recordData != null ) {
					iter3 = record.recordData.keySet().iterator() ;
					while( iter3.hasNext() ){
						fieldCode = iter3.next() ;
						recValue = record.recordData.get(fieldCode) ;
						if( !recValue.isSet ){
							return false ;
						}
					}
				}
			}
			pageId++ ;
		}
    	return true ;
	}
	
	public void links_refresh() {
		
		// ****** Cache des valeurs de la 1ere page *****
		HashMap<String,CrmFileFieldValue> valuesCache = new HashMap<String,CrmFileFieldValue>() ;
		
		// ****** Verif de la 1ere page
		boolean isFirstComplete = true ;
		Iterator<CrmFileRecord> iter2 ;
		Iterator<String> iter3 ;
		String fieldCode ;
		int pageId = 0 ;
		CrmFileRecord record ;
		CrmFileFieldValue recValue ;
		iter2 = TransactionPageRecords.get(pageId).iterator() ;
		while( iter2.hasNext() ){
			record = iter2.next() ;
			if( list_getPageType( pageId )==PageType.PAGETYPE_TABLE && record.recordIsDisabled ) {
				continue ;
			}
			if( record.recordData != null ) {
				iter3 = record.recordData.keySet().iterator() ;
				while( iter3.hasNext() ){
					fieldCode = iter3.next() ;
					recValue = record.recordData.get(fieldCode) ;
					if( !recValue.isSet ){
						isFirstComplete = false ;
					}
					else {
						valuesCache.put(fieldCode,recValue) ;
					}
				}
			}
		}
		
		// ****** Mask/Unmask pages *****
		Iterator<CrmFilePageinfo> iter1 = TransactionPages.iterator() ;
		pageId = 0 ;
		while( iter1.hasNext() ){
			CrmFilePageinfo pageInfo = iter1.next() ;
			if(pageId==0) {
				pageInfo.pageIsHidden = false ;
			}
			else {
				pageInfo.pageIsHidden = !isFirstComplete ;
			}
				
			pageId++ ;
		}
		
		
		// ****** Each page => Autofill FIELD_AUTOVALUEs ******
		// ******     => if different, destroy Data
		Iterator<CrmFilePageinfo> pageIter ;
		pageIter = TransactionPages.iterator() ;
		pageId = -1 ;
		while(pageIter.hasNext()){
			pageId++ ;
			CrmFilePageinfo pageInfo = pageIter.next() ;
			if( pageInfo.pageType != PageType.PAGETYPE_LOGLIST ) {
				continue ;
			}
			Iterator<CrmFileFieldDesc> descIter = TransactionPageFields.get(pageId).iterator() ;
			while(descIter.hasNext()) {
				CrmFileFieldDesc fieldDesc = descIter.next() ;
				if( fieldDesc.fieldAutovalueIsOn ) {
					CrmFileFieldValue fieldValue = TransactionPageRecords.get(0).get(0).recordData.get(fieldDesc.fieldAutovalueSrc) ;
					if( fieldValue == null ) {
						continue ;
					}
					// Log.w(TAG,"Putting autovalue "+fieldDesc.fieldCode+" from "+fieldDesc.fieldAutovalueSrc) ;
					fieldDesc.fieldAutovalue = fieldValue.clone() ;
					TransactionPageRecords.get(pageId).get(0).recordData.put(fieldDesc.fieldCode,fieldDesc.fieldAutovalue) ;
				}
			}
			TransactionPageRecords.get(pageId).subList(1, TransactionPageRecords.get(pageId).size()).clear();
			pageInfo.loadIsLoaded = false ;
		}
		
		
		// ****** Each page => Gestion des tables AUTOFILL ******
		// ******     => if LINKS differents, mask/unmask table entries
		pageIter = TransactionPages.iterator() ;
		pageId = -1 ;
		while(pageIter.hasNext()){
			pageId++ ;
			CrmFilePageinfo pageInfo = pageIter.next() ;
			if( pageInfo.pageType != PageType.PAGETYPE_TABLE ) {
				continue ;
			}
			
			// @TODO
			// field pivot ?
	    	Iterator<CrmFileFieldDesc> mIter = TransactionPageFields.get(pageId).iterator() ;
	    	CrmFileFieldDesc tFieldDesc ;
	    	while( mIter.hasNext() ){
	    		tFieldDesc = mIter.next() ;
	    		
	    		if(tFieldDesc.fieldIsPivot) {
	    			ArrayList<String> activatedPivotKeys = new ArrayList<String>() ;
	    			
	    			// appel a bible helper
	    			BibleHelper bibleHelper = new BibleHelper(mContext) ;
	    			Iterator<BibleHelper.BibleEntry> bibleIter = bibleHelper.queryBible(tFieldDesc.fieldLinkBible,links_getBibleConditions()).iterator() ;
	    	    	while( bibleIter.hasNext() ){
	    	    		BibleHelper.BibleEntry bibleEntry = bibleIter.next() ;
	    	    		activatedPivotKeys.add( bibleEntry.entryKey ) ;
	    	    	}
	    	    	
	    	    	// iteration sur tous les elements du tableau => activation / desactivation
	    	    	Iterator<CrmFileRecord> recordsIter = TransactionPageRecords.get(pageId).iterator() ;
	    	    	while( recordsIter.hasNext() ){
	    	    		CrmFileRecord tableRecord = recordsIter.next() ;
	    	    		String tableRecordPivotvalue = tableRecord.recordData.get(tFieldDesc.fieldCode).valueString ;
	    	    		tableRecord.recordIsHidden = !(activatedPivotKeys.contains(tableRecordPivotvalue)) ;
	    	    	}
	    		}
	    	}
	    	
	    	
			
		}
		
		
	}
	public ArrayList<BibleHelper.BibleEntry> links_getBibleConditions() { // utilisation : dans les BiblePickers , dans les tables TABLE_AUTOFILL
		ArrayList<BibleHelper.BibleEntry> mArr = new ArrayList<BibleHelper.BibleEntry>() ;
		
		BibleHelper bh = new BibleHelper(mContext) ;
		
		Iterator<CrmFileFieldDesc> mIter = TransactionPageFields.get(0).iterator() ;
		CrmFileRecord mRecord = TransactionPageRecords.get(0).get(0) ;
		if( mRecord == null || mRecord.recordData == null ) {
			return mArr ;
		}
		while(mIter.hasNext()) {
			CrmFileFieldDesc fieldDesc = mIter.next() ;
			if( fieldDesc.fieldType != FieldType.FIELD_BIBLE ) {
				continue ;
			}
			if( mRecord.recordData.get(fieldDesc.fieldCode).isSet ) {
				// Log.w(TAG,"Adding "+fieldDesc.fieldLinkBible+" "+mRecord.recordData.get(fieldDesc.fieldCode).valueString ) ;
				mArr.add( bh.getBibleEntry(fieldDesc.fieldLinkBible , mRecord.recordData.get(fieldDesc.fieldCode).valueString) ) ;
			}
		}
		
		return mArr ;
	}
	
	
	
	

	public boolean saveAll(){
		Iterator<ArrayList<CrmFileRecord>> iter1 = TransactionPageRecords.iterator() ;
		Iterator<CrmFileRecord> iter2 ;
		Iterator<String> iter3 ;
		String fieldCode ;
		CrmFileRecord record ;
		CrmFileFieldValue recValue ;
		ContentValues cv ;
		int pageId = 0 ;
		int recId ;
		PageType pageType ;
		String fileCode ;
		long remainMainFileId = 0 ;
		long currentFileId ;
		mDb.beginTransaction() ;
    	while( iter1.hasNext() ){
    		pageType = list_getPageType( pageId ) ;
    		fileCode = TransactionPages.get(pageId).fileCode ;
    		iter2 = iter1.next().iterator() ;
    		recId = -1 ;
        	while( iter2.hasNext() ){
        		recId++ ;
        		record = iter2.next() ;
        		if( pageType==PageType.PAGETYPE_TABLE && (record.recordIsDisabled||record.recordIsHidden) ) {
        			continue ;
        		}
        		if( pageType==PageType.PAGETYPE_LOGLIST && recId > 0 ) {
        			continue ;
        		}
        		
    			// insertion de l'entete
    			currentFileId = 0 ;
    			if( !TransactionPages.get(pageId).fileIsSubfile ) {
    				if( TransactionPages.get(pageId).fileCode.equals(CrmFileCode) ) {
    					if( remainMainFileId == 0 ) {
    	        			cv = new ContentValues() ;
    	        			cv.put("file_code", fileCode);
    	        			remainMainFileId = currentFileId = mDb.insert("store_file", cv);
    					}
    					else{
    						currentFileId = remainMainFileId ;
    					}
    					
    				}
    				else {
	        			cv = new ContentValues() ;
	        			cv.put("file_code", fileCode);
	        			currentFileId = mDb.insert("store_file", cv);
    				}
    			}
    			else {
        			cv = new ContentValues() ;
        			cv.put("filerecord_parent_id", remainMainFileId);
        			cv.put("file_code", fileCode);
        			currentFileId = mDb.insert("store_file", cv);
    			}
    			
    			if( TransactionPages.get(pageId).fileHasGmap && pageId==0 ) {
    				LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    				Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    				
    				if( lastKnownLocation != null ) {
    					JSONObject mJson = new JSONObject() ;
    					try {
    						mJson.putOpt("lat", lastKnownLocation.getLatitude()) ;
    						mJson.putOpt("lng", lastKnownLocation.getLongitude()) ;
    					} catch (JSONException e) {
    						// TODO Auto-generated catch block
    					}
    					if( mJson.length() > 0 ) {
    						cv = new ContentValues() ;
    						cv.put("filerecord_id", currentFileId);
    						cv.put("filerecord_field_code", "gmap_location");
    						cv.put("filerecord_field_value_link",mJson.toString()) ;
    						mDb.insert("store_file_field", cv);
    					}
    				}
    			}
    			
    			String fieldPivot = "" ;
				Iterator<CrmFileFieldDesc> iterFields = TransactionPageFields.get(pageId).iterator() ;
				int b = -1 ;
				while( iterFields.hasNext() ) {
					CrmFileFieldDesc fieldDesc = iterFields.next();
					if( fieldDesc.fieldIsPivot ) {
						fieldPivot = fieldDesc.fieldCode ;
					}
				}
    			
    			
    			
        		switch( pageType ) {
        		case PAGETYPE_PHOTO :
        			cv = new ContentValues() ;
           			cv.put("filerecord_id", currentFileId);
        			cv.put("filerecord_field_code", "media_mimetype");
        			cv.put("filerecord_field_value_string","image/jpeg") ;
        			mDb.insert("store_file_field", cv);
        			
        			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm") ;
           			cv.put("filerecord_id", currentFileId);
        			cv.put("filerecord_field_code", "media_date");
        			cv.put("filerecord_field_value_string",sdf.format(new Date())) ;
        			mDb.insert("store_file_field", cv);
        			
         			
        			// rename definitif
        			try {
						String mPath = new URI(record.recordPhoto.uriString).getPath() ;
	        			File mFileFrom = new File(mPath);
	        			File mFileTo = new File(mPath+".save");
	        			mFileFrom.renameTo(mFileTo) ;
	           			cv.put("filerecord_id", currentFileId);
	        			cv.put("filerecord_field_code", "media_title");
	        			cv.put("filerecord_field_value_string",mFileTo.getName()) ;
	        			mDb.insert("store_file_field", cv);
					} catch (URISyntaxException e) {
					}
        			
        			try {
						String mPath = new URI(record.recordPhoto.uriStringThumb).getPath() ;
	        			File mFileDelete = new File(mPath);
	        			mFileDelete.delete() ;
					} catch (URISyntaxException e) {
					}
        			
        			
        			break ;
        			
        		case PAGETYPE_LOGLIST : // si LOGLIST + fileCode = fichier principal => on insère que la valeur PIVOT
        			if( TransactionPages.get(pageId).fileCode.equals(CrmFileCode) ) {
        				recValue = record.recordData.get( fieldPivot ) ;
                		if( recValue != null && recValue.isSet ){
                			cv = new ContentValues() ;
                			cv.put("filerecord_id", currentFileId);
                			cv.put("filerecord_field_code", fieldPivot);
                			switch(recValue.fieldType) {
                			case FIELD_BIBLE :
                				cv.put("filerecord_field_value_link", recValue.valueString);
                				break ;
                			case FIELD_DATE :
                			case FIELD_DATETIME :
                				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm") ;
                				cv.put("filerecord_field_value_date", sdf2.format(recValue.valueDate)+":00");
                				break ;
                			case FIELD_NUMBER :
                				cv.put("filerecord_field_value_number", recValue.valueFloat);
                				break ;
                			case FIELD_TEXT :
                				cv.put("filerecord_field_value_string", recValue.valueString);
                				break ;
                			default :
                				break ;
                			}
                			mDb.insert("store_file_field", cv);
                		}
       				
        				break ;
        			}
        			else {
        				// Si LOGLIST et FieldPivot nul (pas de valeur) => on break (ne sera pas inséré)
        				recValue = record.recordData.get( fieldPivot ) ;
                		if( recValue != null && recValue.isSet ){
                			if( recValue.displayStr.equals("")) {
                				mDb.execSQL(String.format("DELETE FROM store_file WHERE filerecord_id='%d'",currentFileId)) ;
                				break ;
                			}
                		}
        			}
        		
        		default :
            		iter3 = record.recordData.keySet().iterator() ;
                	while( iter3.hasNext() ){
                		fieldCode = iter3.next() ;
                		recValue = record.recordData.get(fieldCode) ;
                		if( recValue.isSet ){
                			cv = new ContentValues() ;
                			cv.put("filerecord_id", currentFileId);
                			cv.put("filerecord_field_code", fieldCode);
                			switch(recValue.fieldType) {
                			case FIELD_BIBLE :
                				cv.put("filerecord_field_value_link", recValue.valueString);
                				break ;
                			case FIELD_DATE :
                			case FIELD_DATETIME :
                				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm") ;
                				cv.put("filerecord_field_value_date", sdf2.format(recValue.valueDate)+":00");
                				break ;
                			case FIELD_NUMBER :
                				cv.put("filerecord_field_value_number", recValue.valueFloat);
                				break ;
                			case FIELD_TEXT :
                				cv.put("filerecord_field_value_string", recValue.valueString);
                				break ;
                			default :
                				break ;
                			}
                			mDb.insert("store_file_field", cv);
                		}
                	}
        			break ;
        		}
        		
        	}
        	pageId++ ;
    	}
    	mDb.endTransaction() ;
    	return true ;
	}
	
	public JSONObject toJSONObject() {
		try {
			JSONObject jsonObj = new JSONObject() ;
			jsonObj.put("CrmInputScenId",CrmInputScenId) ;
			jsonObj.put("CrmFileCode", CrmFileCode) ;
			
			JSONArray JsonTransactionPages = new JSONArray() ;
			for( CrmFilePageinfo cfp : TransactionPages ) {
				JsonTransactionPages.put(cfp.toJSONObject()) ;
			}
			jsonObj.put("TransactionPages", JsonTransactionPages) ;
			
			JSONArray JsonTransactionPageFields = new JSONArray() ;
			for( ArrayList<CrmFileFieldDesc> arrCffd : TransactionPageFields ) {
				JSONArray JsonTransactionPageFields_intra = new JSONArray() ;
				for( CrmFileFieldDesc cffd : arrCffd ) {
					JsonTransactionPageFields_intra.put(cffd.toJSONObject()) ;
				}
				JsonTransactionPageFields.put(JsonTransactionPageFields_intra) ;
			}
			jsonObj.put("TransactionPageFields", JsonTransactionPageFields) ;
			
			JSONArray JsonTransactionPageRecords = new JSONArray() ;
			for( ArrayList<CrmFileRecord> arrCfr : TransactionPageRecords ) {
				JSONArray JsonTransactionPageRecords_intra = new JSONArray() ;
				for( CrmFileRecord cfr : arrCfr ) {
					JsonTransactionPageRecords_intra.put(cfr.toJSONObject()) ;
				}
				JsonTransactionPageRecords.put(JsonTransactionPageRecords_intra) ;
			}
			jsonObj.put("TransactionPageRecords", JsonTransactionPageRecords) ;
			
			return jsonObj ;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null ;
		}
	}

}
