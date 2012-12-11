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
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import za.dams.paracrm.BibleHelper.BibleEntry;
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
	
	private ArrayList<CrmFilePivot> outerPivots ;
	
	private ArrayList<CrmFilePageinfo> TransactionPages ;
	private ArrayList<ArrayList<CrmFileFieldDesc>>  TransactionPageFields ;
	private ArrayList<ArrayList<CrmFileRecord>> TransactionPageRecords ;
	
	public enum PageType {
		PAGETYPE_LIST, PAGETYPE_LOGLIST, PAGETYPE_TABLE, PAGETYPE_PHOTO, PAGETYPE_SAVE, PAGETYPE_NULL,
		PAGETYPE_CONTAINER
	}
	public enum PivotType {
		PIVOT_NULL, PIVOT_CONTAINER, PIVOT_TABLE
	}
	public enum PageTableType {
		TABLE_NULL, TABLE_MANUAL, TABLE_AUTOFILL_DISABLED, TABLE_AUTOFILL_ENABLED
	}
	public enum FieldType {
	    FIELD_TEXT, FIELD_NUMBER, FIELD_DATE, FIELD_DATETIME, FIELD_BIBLE, FIELD_NULL
	};
	
	public static class CrmFilePageinfo {
    	public int pageId ;
    	public int nbChildren ;
    	public PageType pageType = PageType.PAGETYPE_NULL ;
    	public PageTableType pageTableType = PageTableType.TABLE_NULL ;
    	public String pageCode ;
    	public String pageLib ;
    	public String fileCode ;
    	public String fileLib ;
    	public boolean fileIsSubfile ;
    	public boolean fileHasGmap ;
    	public boolean pageIsHidden ;
    	public boolean pageInnerContainer ;
    	public boolean loadIsLoadable ;
    	public boolean loadIsLoaded ;
    	public CrmFilePivot outerPivot ;
    	public CrmFilePivot innerPivot ;
    	public String pivotTag ;
    	public int pivotForeignRefIdx ;
	
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
    		pageInnerContainer = false ;
    	}
    	public CrmFilePageinfo( int aPageId , PageType aPageType , int aNbChildren ) {
    		pageId = aPageId ;
    		nbChildren = aNbChildren ;
    		pageType = aPageType ;
    		pageTableType = PageTableType.TABLE_NULL ;
    		pageCode = "" ;
    		pageLib = "" ;
    		fileCode = "" ;
    		fileLib = "" ;
    		fileIsSubfile = false ;
    		if( !fileIsSubfile ){
    			fileHasGmap = false ;
    		}
    		pageIsHidden = true ;
    		pageInnerContainer = false ;
    	}
    	public CrmFilePageinfo( JSONObject jsonObject ) {
    		try {
				pageId = jsonObject.getInt("pageId");
				nbChildren = jsonObject.getInt("nbChildren");
				pageType = PageType.values()[jsonObject.getInt("pageType")] ;
				pageTableType = PageTableType.values()[jsonObject.getInt("pageTableType")] ;
				pageCode = jsonObject.getString("pageCode");
				pageLib = jsonObject.getString("pageLib");
				fileCode = jsonObject.getString("fileCode");
				fileLib = jsonObject.getString("fileLib");
				fileIsSubfile = jsonObject.getBoolean("fileIsSubfile");
				fileHasGmap = jsonObject.getBoolean("fileHasGmap");
				pageIsHidden = jsonObject.getBoolean("pageIsHidden");
				pageInnerContainer = jsonObject.getBoolean("pageInnerContainer");
				loadIsLoadable = jsonObject.getBoolean("loadIsLoadable");
				loadIsLoaded = jsonObject.getBoolean("loadIsLoaded");
				if( jsonObject.has("outerPivot") ) {
					outerPivot = new CrmFilePivot( jsonObject.getJSONObject("outerPivot") ) ;
				}
				if( jsonObject.has("innerPivot") ) {
					innerPivot = new CrmFilePivot( jsonObject.getJSONObject("innerPivot") ) ;
				}
				pivotTag = jsonObject.getString("pivotTag");
				pivotForeignRefIdx = jsonObject.getInt("pivotForeignRefIdx");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
				jsonObject.put("pageId", pageId) ;
				jsonObject.put("nbChildren", nbChildren) ;
				jsonObject.put("pageType",pageType.ordinal()) ;
				jsonObject.put("pageTableType",pageTableType.ordinal()) ;
				jsonObject.put("pageCode", pageCode) ;
				jsonObject.put("pageLib", pageLib) ;
				jsonObject.put("fileCode", fileCode) ;
				jsonObject.put("fileLib", fileLib) ;
				jsonObject.put("fileIsSubfile", fileIsSubfile) ;
				jsonObject.put("fileHasGmap", fileHasGmap) ;
				jsonObject.put("pageIsHidden", pageIsHidden) ;
				jsonObject.put("pageInnerContainer", pageInnerContainer) ;
				jsonObject.put("loadIsLoadable", loadIsLoadable) ;
				jsonObject.put("loadIsLoaded", loadIsLoaded) ;
				if( outerPivot != null ) {
					jsonObject.put("outerPivot", outerPivot.toJSONObject()) ;
				}
				if( innerPivot != null ) {
					jsonObject.put("innerPivot", innerPivot.toJSONObject()) ;
				}
				jsonObject.put("pivotTag", pivotTag) ;
				jsonObject.put("pivotForeignRefIdx", pivotForeignRefIdx) ;
				return jsonObject ;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null ;
			}
    	}
	}
	
	public static class CrmFilePivot {
		public PivotType pivotType ;
		
		public String previousConditionsHash ;
		
		public int containerPageId ;
		public int containerSize ;
		
		public String targetBibleCode ;
		public int targetPageId ;
		public int targetFieldId ;

		public boolean conditionSrc ;
		public int conditionSrcPageId ;
		public int conditionSrcFieldId ;
		
		public CrmFilePivot() {
			
		}
    	public CrmFilePivot( JSONObject jsonObject ) {
    		try {
				pivotType = PivotType.values()[jsonObject.getInt("pivotType")] ;
				previousConditionsHash = jsonObject.getString("previousConditionsHash");
				containerPageId = jsonObject.getInt("containerPageId");
				containerSize = jsonObject.getInt("containerSize");
				targetBibleCode = jsonObject.getString("targetBibleCode");
				targetPageId = jsonObject.getInt("targetPageId");
				targetFieldId = jsonObject.getInt("targetFieldId");
				conditionSrc = jsonObject.getBoolean("conditionSrc");
				conditionSrcPageId = jsonObject.getInt("conditionSrcPageId");
				conditionSrcFieldId = jsonObject.getInt("conditionSrcFieldId");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	public JSONObject toJSONObject() {
    		try {
    			JSONObject jsonObject = new JSONObject() ;
				jsonObject.put("pivotType",pivotType.ordinal()) ;
				jsonObject.put("previousConditionsHash",previousConditionsHash) ;
				jsonObject.put("containerPageId", containerPageId) ;
				jsonObject.put("containerSize", containerSize) ;
				jsonObject.put("targetBibleCode", targetBibleCode) ;
				jsonObject.put("targetPageId", targetPageId) ;
				jsonObject.put("targetFieldId", targetFieldId) ;
				jsonObject.put("conditionSrc", conditionSrc) ;
				jsonObject.put("conditionSrcPageId", conditionSrcPageId) ;
				jsonObject.put("conditionSrcFieldId", conditionSrcFieldId) ;
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
    			Log.w(TAG,"Field code "+this.fieldCode) ;
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
    			Log.w(TAG,"Field code "+this.fieldCode) ;
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
    	
		outerPivots = new ArrayList<CrmFilePivot>() ;
		TransactionPages = new ArrayList<CrmFilePageinfo>() ;
		TransactionPageFields = new ArrayList<ArrayList<CrmFileFieldDesc>>() ;
		TransactionPageRecords = new ArrayList<ArrayList<CrmFileRecord>>() ;
		
		try {
			JSONArray JsonOuterPivots = jsonObj.getJSONArray("outerPivots") ;
			for( int idx=0 ; idx<JsonOuterPivots.length() ; idx++ ) {
				outerPivots.add( new CrmFilePivot( JsonOuterPivots.getJSONObject(idx) ) ) ;
			}
		} catch( JSONException e ) {
			e.printStackTrace() ;
		}
		
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
		
		
		outerPivots = new ArrayList<CrmFilePivot>() ;
		TransactionPages = new ArrayList<CrmFilePageinfo>() ;
		TransactionPageFields = new ArrayList<ArrayList<CrmFileFieldDesc>>() ;
		TransactionPageRecords = new ArrayList<ArrayList<CrmFileRecord>>() ;
		
		
		Cursor tmpCursor = mDb.rawQuery( String.format("SELECT target_filecode FROM input_scen WHERE scen_id='%d'",CrmInputScenId) ) ;
		if( tmpCursor.getCount() < 1 ) {
			tmpCursor.close() ;
			return ;
		}
		tmpCursor.moveToNext();
		String CrmFileCode = tmpCursor.getString(0) ;
		this.CrmFileCode = CrmFileCode ;
		tmpCursor.close() ;
		
		// ******** Ajout de toutes les pages **********
		tmpCursor = mDb.rawQuery( String.format("SELECT scen_page_index FROM input_scen_page WHERE scen_id='%d' AND scen_page_parent_index='0' ORDER BY scen_page_index",CrmInputScenId) ) ;
		whileCursor :
    	while( tmpCursor.moveToNext() ){
    		int insertOffset = TransactionPages.size() ; // on ajoute linéairement
    		List<CrmFilePageinfo> insertPages = pagetool_getCrmFilePageinfo( tmpCursor.getInt(0) ) ;
    		
    		// On extrait les pages "template" cad conditionné par un PIVOT_CONTAINER (=outerPivot)
    		for( CrmFilePageinfo fpi : insertPages ) {
    			if( fpi.outerPivot != null ) {
    				outerPivots.add(fpi.outerPivot) ;
    				continue whileCursor ;
    			}
    		}

    		pages_insert( insertOffset, insertPages ) ;
    	}
    	tmpCursor.close() ;

    	
    	links_refresh() ;
    	
    	return ;
	}

	private int pagetool_findOffset( int searchPageId, int startOffset ) {
		if( startOffset >= TransactionPages.size() || startOffset < 0 ) {
			return -1 ;
		}
		
		
		boolean fromInner = false ;
		// ******** recherche d'une page à partir d'une autre ********
		if( TransactionPages.get(startOffset).pageInnerContainer ) {
			fromInner = true ;
			while( TransactionPages.get(startOffset).pageInnerContainer ) {
				startOffset-- ;
			}
		}
		
		while( startOffset < TransactionPages.size() ) {
			// Log.w(TAG,"Search "+searchPageId+" current is "+TransactionPages.get(startOffset).pageId) ;
			if( TransactionPages.get(startOffset).pageId == searchPageId ) {
				return startOffset ;
			}
			startOffset++ ;
			if( fromInner && !TransactionPages.get(startOffset).pageInnerContainer ) {
				break ;
			}
		}
		
		if( fromInner ) {
			return pagetool_findOffset( searchPageId, 0 ) ;
		}
		return -1 ;
	}
	private List<CrmFilePageinfo> pagetool_getCrmFilePageinfo( int pageId ) {
		Cursor tmpCursor ;
		Cursor tmpInnerCursor ;
		
		// ******** Ajout d'une page + tous ses children **********
		tmpCursor = mDb.rawQuery( String.format("SELECT scen_page_name, target_filecode, page_type, page_table_type, scen_page_index, scen_page_parent_index FROM input_scen_page WHERE scen_id='%d' AND scen_page_index='%d'",CrmInputScenId,pageId) ) ;
		if( !tmpCursor.moveToNext() ) {
			tmpCursor.close() ;
			return new ArrayList<CrmFilePageinfo>() ;
		}
		
		ArrayList<CrmFilePageinfo> returnArr = new ArrayList<CrmFilePageinfo>() ;
		CrmFilePageinfo tmpPageInfo ;
    		
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
		if( sPageType.equals("PAGETYPE_CONTAINER") )
			pageType = PageType.PAGETYPE_CONTAINER ;
		if( sPageType.equals("PAGETYPE_SAVE") )
			pageType = PageType.PAGETYPE_SAVE ;

		boolean pageInnerContainer = false ;
		if( !tmpCursor.isNull(5) && tmpCursor.getInt(5) > 0 ) {
			pageInnerContainer = true ;
		}

		PageTableType pagetableType = PageTableType.TABLE_NULL;
		String sPageTableType = tmpCursor.getString(3) ;
		if( sPageTableType.equals("TABLE_AUTOFILL_ENABLED") )
			pagetableType = PageTableType.TABLE_AUTOFILL_ENABLED ;
		if( sPageTableType.equals("TABLE_AUTOFILL_DISABLED") )
			pagetableType = PageTableType.TABLE_AUTOFILL_DISABLED ;


		if( pageType == PageType.PAGETYPE_CONTAINER ) {
			int nbChildren = 0 ;
			tmpInnerCursor = mDb.rawQuery( String.format("SELECT scen_page_index FROM input_scen_page WHERE scen_id='%d' AND scen_page_parent_index='%d'",CrmInputScenId,tmpCursor.getInt(4))) ;
			while( tmpInnerCursor.moveToNext() ) {
				nbChildren++ ;
				int childPageId =  tmpInnerCursor.getInt(0) ;
				returnArr.addAll(pagetool_getCrmFilePageinfo(childPageId)) ;
			}
			tmpInnerCursor.close() ;

			tmpPageInfo = new CrmFilePageinfo(tmpCursor.getInt(4),pageType,nbChildren) ;
		}
		else if( pageType == PageType.PAGETYPE_SAVE ){
			tmpPageInfo = new CrmFilePageinfo(tmpCursor.getInt(4),pageType,pagetableType,"",tmpCursor.getString(0) ,false,false) ;
		}
		else {
			tmpInnerCursor = mDb.rawQuery( String.format("SELECT file_code, file_lib, gmap_is_on, file_parent_code FROM define_file WHERE file_code='%s'",tmpCursor.getString(1)) ) ;
			if( tmpInnerCursor.getCount() < 1 ) {
				tmpInnerCursor.close() ;
				return returnArr ;
			}
			tmpInnerCursor.moveToNext() ;

			tmpPageInfo = new CrmFilePageinfo( tmpCursor.getInt(4), pageType,pagetableType, tmpInnerCursor.getString(0) , tmpCursor.getString(0) , !(tmpInnerCursor.getString(3).equals("")), tmpInnerCursor.getString(2).equals("O") ) ;
			if( pageType == PageType.PAGETYPE_LOGLIST ) {
				tmpPageInfo.loadIsLoadable = true ;
				tmpPageInfo.loadIsLoaded = false ;
			}
			if( pageInnerContainer ) {
				tmpPageInfo.pageInnerContainer = true ;
			}
			tmpInnerCursor.close() ;
		}
		tmpCursor.close() ;
		
		// ajout de outerPivot ?
		// ajout de innerPivot ?
		tmpCursor = mDb.rawQuery( String.format("SELECT * FROM input_scen_pagepivot WHERE scen_id='%d' AND scen_page_index='%d'",CrmInputScenId,pageId) ) ;
		if( tmpCursor.moveToNext() ) {
			CrmFilePivot cfpivot = new CrmFilePivot() ;
			
			String strPivotType = tmpCursor.getString(tmpCursor.getColumnIndex("pivot_type")) ;
			if( strPivotType.equals("PIVOT_CONTAINER") ) {
				cfpivot.pivotType = PivotType.PIVOT_CONTAINER ;
			} else if( strPivotType.equals("PIVOT_TABLE") ) {
				cfpivot.pivotType = PivotType.PIVOT_TABLE ;
			} else {
				cfpivot.pivotType = PivotType.PIVOT_NULL ;
			}
			
			cfpivot.targetBibleCode = tmpCursor.getString(tmpCursor.getColumnIndex("target_bible_code")) ;
			cfpivot.targetPageId = tmpCursor.getInt(tmpCursor.getColumnIndex("target_page_index")) ;
			cfpivot.targetFieldId = tmpCursor.getInt(tmpCursor.getColumnIndex("target_page_field_index")) - 1 ;

			cfpivot.conditionSrcPageId = tmpCursor.getInt(tmpCursor.getColumnIndex("foreignsrc_page_index")) ;
			cfpivot.conditionSrcFieldId = tmpCursor.getInt(tmpCursor.getColumnIndex("foreignsrc_page_field_index")) - 1 ;
			cfpivot.conditionSrc = tmpCursor.getString(tmpCursor.getColumnIndex("foreignsrc_is_on")).equals("O") ? true : false ;
			
			switch( cfpivot.pivotType ) {
			case PIVOT_CONTAINER :
				cfpivot.containerPageId = tmpPageInfo.pageId ;
				cfpivot.containerSize = 1 + tmpPageInfo.nbChildren ;
				tmpPageInfo.outerPivot = cfpivot ;
				break ;
			case PIVOT_TABLE :
				tmpPageInfo.innerPivot = cfpivot ;
				break ;
			default : break ;
			}
		}
		tmpCursor.close() ;
		returnArr.add(0,tmpPageInfo) ;
		return returnArr ;
	}
	
	private void pages_insert( int insertOffset,  List<CrmFilePageinfo> infosPages ) {
		insertOffset-- ;
		for( CrmFilePageinfo tFileinfo : infosPages ) {
			insertOffset++ ;

			if( tFileinfo.pageType == PageType.PAGETYPE_SAVE 
					|| tFileinfo.pageType == PageType.PAGETYPE_NULL ) {
				TransactionPages.add(insertOffset,tFileinfo) ;
				TransactionPageFields.add(insertOffset, new ArrayList<CrmFileFieldDesc>() ) ;
				TransactionPageRecords.add(insertOffset, new ArrayList<CrmFileRecord>() ) ;
				continue ;
			}

			if( tFileinfo.pageType == PageType.PAGETYPE_PHOTO ) {
				TransactionPages.add(insertOffset,tFileinfo) ;
				TransactionPageFields.add(insertOffset, new ArrayList<CrmFileFieldDesc>() ) ;
				TransactionPageRecords.add(insertOffset, new ArrayList<CrmFileRecord>() ) ;
				continue ;
			}

			if( tFileinfo.pageType == PageType.PAGETYPE_CONTAINER ) {
				//Log.w(TAG,"Init empty page") ;
				TransactionPages.add(insertOffset,tFileinfo) ;
				TransactionPageFields.add(insertOffset, new ArrayList<CrmFileFieldDesc>() ) ;
				TransactionPageRecords.add(insertOffset, new ArrayList<CrmFileRecord>() ) ;
				continue ;
			}


			// ******** Load de tous les FIELDS ************

			ArrayList<CrmFileFieldDesc> tFields = new ArrayList<CrmFileFieldDesc>() ;

			Cursor tmpCursor = mDb.rawQuery( String.format("SELECT target_filecode, target_filefield, field_is_pivot, field_autovalue_is_on, field_autovalue_src, search_is_condition " +
					"FROM input_scen_page_field " +
					"WHERE scen_id='%d' AND scen_page_index='%d' ORDER BY scen_page_field_index",
					CrmInputScenId, tFileinfo.pageId )) ;

			while( tmpCursor.moveToNext() ){

				Cursor tmpInnerCursor = mDb.rawQuery( String.format("SELECT entry_field_type, entry_field_code, entry_field_lib, entry_field_linkbible " +
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
			TransactionPages.add(insertOffset,tFileinfo) ;
			TransactionPageFields.add(insertOffset, tFields ) ;
			TransactionPageRecords.add(insertOffset, new ArrayList<CrmFileRecord>() ) ;
			
			if( tFileinfo.innerPivot == null ) {
				switch( tFileinfo.pageType ) {
				case PAGETYPE_TABLE :
					page_fillTable( insertOffset, null ) ;
					break ;
				default :
					page_fillSingle(insertOffset) ;
				}
			}


    	
		}
		return ;
	}
	private void pages_delete( int deleteOffset, int deleteLength ) {
		for( int i=0 ; i<deleteLength ; i++ ) {
			TransactionPages.remove(deleteOffset) ;
			TransactionPageFields.remove(deleteOffset) ;
			TransactionPageRecords.remove(deleteOffset) ;
		}
	}
	
	
	private void page_fillSingle( int pageOffset ) {
		// *********** Création du record unique pour PAGE_LIST *******
		CrmFilePageinfo tPageinfo = TransactionPages.get(pageOffset) ;
		ArrayList<CrmFileFieldDesc> tFields = TransactionPageFields.get(pageOffset) ;
		ArrayList<CrmFileRecord> tRecords = TransactionPageRecords.get(pageOffset) ;
		
    	HashMap<String,CrmFileFieldValue> recordData = new HashMap<String,CrmFileFieldValue>() ;
    	for( CrmFileFieldDesc tFieldDesc : tFields ){
    		
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
    							tPageinfo.pageType==PageType.PAGETYPE_LOGLIST ) ) ;
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
		
	}
	private void page_fillTable( int pageOffset, ArrayList<BibleEntry> bibleConditions ) {
		// ************ Création d'une table ************
		//  - delete all records
		//  - create tagged records
		// ****************************************
		
		CrmFilePageinfo tPageinfo = TransactionPages.get(pageOffset) ;
		ArrayList<CrmFileFieldDesc> tFields = TransactionPageFields.get(pageOffset) ;
		ArrayList<CrmFileRecord> tRecords = TransactionPageRecords.get(pageOffset) ;
		
		// paramètres ?
		boolean enableAll = tPageinfo.pageTableType==PageTableType.TABLE_AUTOFILL_ENABLED ;
		
		// vidage de la table
		tRecords.clear() ;
		
		// recherche du pivot
		CrmFileFieldDesc tFieldDescPivot = null ;
    	HashMap<String,CrmFileFieldValue> recordData = new HashMap<String,CrmFileFieldValue>() ;
    	for( CrmFileFieldDesc tFieldDesc : tFields ){
    		
    		if(tFieldDesc.fieldIsPivot) {
    			tFieldDescPivot = tFieldDesc ;
    			break ;
    		}
    	}
    	if( tFieldDescPivot == null ) {
    		return ;
    	}
    	
    	
    	// appel a bible helper pour remplir la table
    	BibleHelper bibleHelper = new BibleHelper(mContext) ;
    	int a = 0 ;
    	for( BibleHelper.BibleEntry bibleEntry : bibleHelper.queryBible(tFieldDescPivot.fieldLinkBible,bibleConditions) ){

    		recordData = new HashMap<String,CrmFileFieldValue>() ;

    		// CREATION DU FIELD
    		for( CrmFileFieldDesc tFieldDesc : tFields ){

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
    	    	

    	// TransactionPageRecords.add(tRecords) ;
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
		SimpleDateFormat sdf ;
		if( TransactionPages.get(pageId).pageType == PageType.PAGETYPE_TABLE ) {
			sdf = new SimpleDateFormat("dd/MM/yyyy") ;
		}
		else{
			sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm") ;
		}
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
        			
        			String displayDate ;
        			if( TransactionPages.get(pageId).pageType == PageType.PAGETYPE_TABLE ) {
        				displayDate = new SimpleDateFormat("dd/MM/yyyy").format(tDate) ;
        			}
        			else{
        				displayDate = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate) ;
        			}
        			
        			recordData.put(tFieldDesc.fieldCode,
        					new CrmFileFieldValue(tFieldDesc.fieldType,
        							new Float(0),
        							false,
        							null,
        							tDate ,
        							displayDate,
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
			else if ( pageInfo.pageInnerContainer ) {
				pageInfo.pageIsHidden = true ;
			}
			else {
				pageInfo.pageIsHidden = !isFirstComplete ;
			}
				
			pageId++ ;
		}
		
		
		
		// ******** REFRESH des pivots *******
		pivots_refreshOuter() ;
		pivots_refreshInner() ;
		// ********************************
		
		
		// ****** Each page => Autofill FIELD_AUTOVALUEs ******
		// ******     => if different, destroy Data
		Iterator<CrmFilePageinfo> pageIter ;
		pageIter = TransactionPages.iterator() ;
		pageId = -1 ;
		while(pageIter.hasNext()){
			pageId++ ;
			CrmFilePageinfo pageInfo = pageIter.next() ;
			switch( pageInfo.pageType ) {
				case PAGETYPE_LOGLIST :
				case PAGETYPE_LIST :
					break ;
			
				default :
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
	private void pivots_refreshOuter() {
		for( CrmFilePivot fpivot : outerPivots ) {
			String targetBiblecode = fpivot.targetBibleCode ;
			
			// ****** Gestion Source *********
			String foreignBiblecode = null ;
			String foreignFieldkey = null ;
			ArrayList<BibleHelper.BibleEntry> foreignEntries = null ;
			if( !fpivot.conditionSrc ) {
				// pas de source
				
				if( fpivot.previousConditionsHash!=null && fpivot.previousConditionsHash.equals("*") ) {
					continue ;
				}
				
			} else {
			
				// recherche de la source ?
				int conditionSrcPageIdx = pagetool_findOffset( fpivot.conditionSrcPageId, 0 ) ;
				int conditionSrcFieldIdx = fpivot.conditionSrcFieldId ;
				if( conditionSrcPageIdx == -1 ) {
					// ??? mauvaise config
					Log.w(TAG,"ERROR 4") ;
					continue ;
				}


				foreignBiblecode = TransactionPageFields.get(conditionSrcPageIdx).get(conditionSrcFieldIdx).fieldLinkBible ;
				foreignFieldkey = TransactionPageFields.get(conditionSrcPageIdx).get(conditionSrcFieldIdx).fieldCode ;
				if( foreignBiblecode==null ) {
					// ??? mauvaise config
					Log.w(TAG,"ERROR 5") ;
					continue ;
				}

				// calcul du hash (besoin d'un reset ?)
				ArrayList<String> currentConditionsTab = new ArrayList<String>() ;
				for( CrmFileRecord cfr : TransactionPageRecords.get(conditionSrcPageIdx) ) {
					if( cfr.recordIsDisabled || cfr.recordIsHidden || !cfr.recordData.get(foreignFieldkey).isSet ) {
						continue ;
					}
					currentConditionsTab.add(cfr.recordData.get(foreignFieldkey).valueString) ;
				}
				String currentConditionsHash = implodeArray(currentConditionsTab.toArray(new String[currentConditionsTab.size()]),"+") ;

				// pas changé
				if( fpivot.previousConditionsHash!=null && currentConditionsHash.equals(fpivot.previousConditionsHash) ) {
					continue ;
				}
				fpivot.previousConditionsHash = currentConditionsHash ;
				// Log.w(TAG,"New hash is "+currentConditionsHash) ;

				BibleHelper bh = new BibleHelper(mContext) ;
				foreignEntries = new ArrayList<BibleHelper.BibleEntry>() ;
				for( CrmFileRecord cfr : TransactionPageRecords.get(conditionSrcPageIdx) ) {
					if( cfr.recordIsDisabled || cfr.recordIsHidden || !cfr.recordData.get(foreignFieldkey).isSet ) {
						continue ;
					}
					foreignEntries.add(bh.getBibleEntry(foreignBiblecode, cfr.recordData.get(foreignFieldkey).valueString)) ;
				}
			}
			
			// Interro bible
			BibleHelper bibleHelper = new BibleHelper(mContext) ;
			ArrayList<BibleEntry> tBibleEntries ;
			if( foreignEntries != null && foreignEntries.size() == 0 ) {
				tBibleEntries = new ArrayList<BibleEntry>() ;
			} else {
				tBibleEntries = bibleHelper.queryBible(targetBiblecode,foreignEntries) ;
			}
			
			// PAGEtags objectif
			ArrayList<String> pagetagsTarget = new ArrayList<String>() ;
			for( BibleHelper.BibleEntry bibleEntry : tBibleEntries ) {
				pagetagsTarget.add(bibleEntry.entryKey) ;
			}
			// PAGEtags current
			ArrayList<String> pagetagsCurrent = new ArrayList<String>() ;
			for( CrmFilePageinfo fpi : TransactionPages ) {
				if( fpi.pageId != fpivot.containerPageId ) {
					continue ;
				}
				pagetagsCurrent.add(fpi.pivotTag) ;
			}
			
			// DELETE DES PAGES CONCERNEES
			for( String pivotTag : pagetagsCurrent ) {
				if( pagetagsTarget.contains(pivotTag) ) {
					continue ;
				}
				int tmpIdx = -1 ;
				for( CrmFilePageinfo fpi : TransactionPages ) {
					tmpIdx++ ;
					if( fpi.pageId == fpivot.containerPageId && fpi.pivotTag == pivotTag ) {
						pages_delete( tmpIdx, fpivot.containerSize ) ;
						break ;
					}
				}
			}
			
			// INSERTION PAGE
			int insertOffset = TransactionPages.size() ;
	    	for( BibleHelper.BibleEntry bibleEntry : tBibleEntries ){
	    		
	    		String pivotTag = bibleEntry.entryKey ;
	    		
	    		
	    		// TODO : Condition inverse sur la bible originale => pour le mapping
	    		
	    		
	    		if( pagetagsCurrent.contains(pivotTag) ) {
	    			continue ;
	    		}
	    		
	    		List<CrmFilePageinfo> insertPages = pagetool_getCrmFilePageinfo( fpivot.containerPageId ) ;
	    		for( CrmFilePageinfo fpi : insertPages) {
	    			if( fpi.pageType == PageType.PAGETYPE_CONTAINER ) {
	    				fpi.pageIsHidden = false ;
	    				fpi.pageLib = bibleEntry.displayStr ;
	    				fpi.pivotTag = pivotTag ;
	    			}
	    		}
	    		
	    		pages_insert( insertOffset , insertPages ) ;
	    		
	    		int targetPageId = fpivot.targetPageId ;
	    		int targetFieldIdx = fpivot.targetFieldId ;
	    		
	    		int targetPageIdx = pagetool_findOffset(targetPageId, insertOffset) ;
	    		if( targetPageIdx != -1 ) {
	    			for( int tmpIdx2=0 ; tmpIdx2 < TransactionPageRecords.get(targetPageIdx).size() ; tmpIdx2++ ) {
	    				page_setRecordFieldValue_bible( targetPageIdx , tmpIdx2, targetFieldIdx, bibleEntry.entryKey ) ;
	    			}
	    			TransactionPageFields.get(targetPageIdx).get(targetFieldIdx).fieldIsReadonly = true ;
	    		}
	    		
	    		
	    		
	    		
	    		
	    		insertOffset += fpivot.containerSize ;
	    	}
		}
	}
	private void pivots_refreshInner() {
		int pageIdx = -1 ;
		for( CrmFilePageinfo fpi : TransactionPages ) {
			pageIdx++ ;
			if( fpi.innerPivot==null ) {
				continue ;
			}
			CrmFilePivot fpivot = fpi.innerPivot ;
			
			String targetBiblecode = fpivot.targetBibleCode ;
			if( fpi.pageId != fpivot.targetPageId ) {
				// ??? mauvaise config
				Log.w(TAG,"ERROR 1") ;
				continue ;
			}
			if( TransactionPageFields.get(pageIdx).get(fpivot.targetFieldId).fieldType != FieldType.FIELD_BIBLE ) {
				// ??? mauvaise config
				Log.w(TAG,"ERROR 2") ;
				continue ;
			}
			if( !targetBiblecode.equals(TransactionPageFields.get(pageIdx).get(fpivot.targetFieldId).fieldLinkBible) ) {
				// ??? mauvaise config
				Log.w(TAG,"ERROR 3 " + targetBiblecode + " " + TransactionPageFields.get(pageIdx).get(fpivot.targetFieldId).fieldLinkBible ) ;
				continue ;
			}
			
			// pivot sans source ?
			if( !fpivot.conditionSrc ) {
				if( fpivot.previousConditionsHash!=null && fpivot.previousConditionsHash.equals("*") ) {
					continue ;
				}
				page_fillTable( pageIdx, null ) ;
				fpivot.previousConditionsHash = "*" ;
				continue ;
			}
			
			// recherche de la source ?
			int conditionSrcPageIdx ;
			if( TransactionPages.get(pageIdx).pageInnerContainer ) {
				conditionSrcPageIdx = pagetool_findOffset( fpivot.conditionSrcPageId, pageIdx ) ;
			} else {
				conditionSrcPageIdx = pagetool_findOffset( fpivot.conditionSrcPageId, 0 ) ;
			}
			int conditionSrcFieldIdx = fpivot.conditionSrcFieldId ;
			if( conditionSrcPageIdx == -1 ) {
				// ??? mauvaise config
				Log.w(TAG,"ERROR 4") ;
				continue ;
			}
			
			
			String foreignBiblecode = TransactionPageFields.get(conditionSrcPageIdx).get(conditionSrcFieldIdx).fieldLinkBible ;
			String foreignFieldkey = TransactionPageFields.get(conditionSrcPageIdx).get(conditionSrcFieldIdx).fieldCode ;
			if( foreignBiblecode==null ) {
				// ??? mauvaise config
				Log.w(TAG,"ERROR 5") ;
				continue ;
			}
			
			// calcul du hash (besoin d'un reset ?)
			ArrayList<String> currentConditionsTab = new ArrayList<String>() ;
			for( CrmFileRecord cfr : TransactionPageRecords.get(conditionSrcPageIdx) ) {
				if( cfr.recordIsDisabled || cfr.recordIsHidden || !cfr.recordData.get(foreignFieldkey).isSet ) {
					continue ;
				}
				currentConditionsTab.add(cfr.recordData.get(foreignFieldkey).valueString) ;
			}
			String currentConditionsHash = implodeArray(currentConditionsTab.toArray(new String[currentConditionsTab.size()]),"+") ;
			
			// pas changé
			if( fpivot.previousConditionsHash!=null && currentConditionsHash.equals(fpivot.previousConditionsHash) ) {
				continue ;
			}
			fpivot.previousConditionsHash = currentConditionsHash ;
			
			BibleHelper bh = new BibleHelper(mContext) ;
			ArrayList<BibleHelper.BibleEntry> foreignEntries = new ArrayList<BibleHelper.BibleEntry>() ;
			for( CrmFileRecord cfr : TransactionPageRecords.get(conditionSrcPageIdx) ) {
				if( cfr.recordIsDisabled || cfr.recordIsHidden || !cfr.recordData.get(foreignFieldkey).isSet ) {
					continue ;
				}
				foreignEntries.add(bh.getBibleEntry(foreignBiblecode, cfr.recordData.get(foreignFieldkey).valueString)) ;
			}
			
			page_fillTable( pageIdx, foreignEntries ) ;
		}
	}
	
	
	
	

	public boolean saveAll() {
		ContentValues cv ;
		
		HashMap<String,String> mapFilecodeParentfilecode = new HashMap<String,String>() ;
		// *** Stockage filecode > parentFilecode pour les fileIsSubfile==true
		Cursor c = mDb.rawQuery("SELECT file_code, file_parent_code FROM define_file") ;
		while( c.moveToNext() ) {
			String filecode = c.getString(0) ;
			String parentFilecode = c.getString(1) ;
			if( filecode==null || filecode.equals("") || parentFilecode==null || parentFilecode.equals("") ) {
				continue ;
			}
			mapFilecodeParentfilecode.put(filecode, parentFilecode) ;
		}
		c.close() ;
		
		
		long currentFileId ;
		HashMap<String,Long> mapFilecodeFileid = new HashMap<String,Long>() ;
		
		mDb.beginTransaction() ;
		
		Iterator<ArrayList<CrmFileRecord>> iter1 = TransactionPageRecords.iterator() ;
		int pageId = -1 ;
    	while( iter1.hasNext() ){
    		pageId++ ;
    		PageType pageType = list_getPageType( pageId ) ;
    		String fileCode = TransactionPages.get(pageId).fileCode ;
    		Iterator<CrmFileRecord> iter2 = iter1.next().iterator() ;
    		int recId = -1 ;
        	while( iter2.hasNext() ){
        		recId++ ;
        		CrmFileRecord record = iter2.next() ;
        		if( pageType==PageType.PAGETYPE_TABLE && (record.recordIsDisabled||record.recordIsHidden) ) {
        			continue ;
        		}
        		if( pageType==PageType.PAGETYPE_LOGLIST && recId > 0 ) {
        			continue ;
        		}
        		
        		
    			// *************** Insertion de l'entete ************
        		//  page == 0 > (fileCode == CrmFileCode) obligatoire !  => création d'un currentFileId
        		// 
        		//  fileIsSubfile == false
        		//    - si fileCode == CrmFileCode => utilisation mapFilecodeFileid
        		//    - sinon => création currentFileId + stockage mapFilecodeFileid
        		//  
        		//  fileIsSubfile == true
        		//    - recherche du parentFileCode
        		//    - recherche mapFilecodeFileid pour le parentFileId
        		//    - création d'un currentFileId
        		// ***************************************************
        		if( pageId==0 ) {
        			if( !TransactionPages.get(pageId).fileCode.equals(CrmFileCode) ) {
        				Log.e(TAG,"Massive error !") ;
        				return false ;
        			}
        			cv = new ContentValues() ;
        			cv.put("file_code", fileCode);
        			currentFileId = mDb.insert("store_file", cv);
        			mapFilecodeFileid.put(fileCode, currentFileId) ;
        		}
        		else if( !TransactionPages.get(pageId).fileIsSubfile ) {
        			if( TransactionPages.get(pageId).fileCode.equals(CrmFileCode) ) {
        				currentFileId = mapFilecodeFileid.get(fileCode) ;
        			}
        			else {
            			cv = new ContentValues() ;
            			cv.put("file_code", fileCode);
            			currentFileId = mDb.insert("store_file", cv);
            			mapFilecodeFileid.put(fileCode, currentFileId) ;
        			}
        		}
        		else if( TransactionPages.get(pageId).fileIsSubfile ) {
        			String parentFilecode = mapFilecodeParentfilecode.get(fileCode) ;
        			if( parentFilecode==null || !mapFilecodeFileid.containsKey(parentFilecode) ) {
        				Log.e(TAG,"Massive error !") ;
        				return false ;
        			}
        			cv = new ContentValues() ;
        			cv.put("filerecord_parent_id", mapFilecodeFileid.get(parentFilecode) );
        			cv.put("file_code", fileCode);
        			currentFileId = mDb.insert("store_file", cv);
        		}
        		else {
        			Log.e(TAG,"Massive error !") ;
        			return false;
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
        				CrmFileFieldValue recValue = record.recordData.get( fieldPivot ) ;
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
        				CrmFileFieldValue recValue = record.recordData.get( fieldPivot ) ;
                		if( recValue != null && recValue.isSet ){
                			if( recValue.displayStr.equals("")) {
                				mDb.execSQL(String.format("DELETE FROM store_file WHERE filerecord_id='%d'",currentFileId)) ;
                				break ;
                			}
                		}
        			}
        		
        		default :
        			Iterator<String> iter3 = record.recordData.keySet().iterator() ;
                	while( iter3.hasNext() ){
                		String fieldCode = iter3.next() ;
                		CrmFileFieldValue recValue = record.recordData.get(fieldCode) ;
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
    	}
    	mDb.endTransaction() ;
    	return true ;
	}
	
	public JSONObject toJSONObject() {
		try {
			JSONObject jsonObj = new JSONObject() ;
			jsonObj.put("CrmInputScenId",CrmInputScenId) ;
			jsonObj.put("CrmFileCode", CrmFileCode) ;
			
			JSONArray JsonOuterPivots = new JSONArray() ;
			for( CrmFilePivot opivot : outerPivots ) {
				JsonOuterPivots.put(opivot.toJSONObject()) ;
			}
			jsonObj.put("outerPivots", JsonOuterPivots) ;
			
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

	
	
	
	
    /**
     * Method to join array elements of type string
     * @author Hendrik Will, imwill.com
     * @param inputArray Array which contains strings
     * @param glueString String between each array element
     * @return String containing all array elements seperated by glue string
     */
    private static String implodeArray(String[] inputArray, String glueString) {

    	/** Output variable */
    	String output = "";

    	if (inputArray.length > 0) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(inputArray[0]);

    		for (int i=1; i<inputArray.length; i++) {
    			sb.append(glueString);
    			sb.append(inputArray[i]);
    		}

    		output = sb.toString();
    	}

    	return output;
    }

}
