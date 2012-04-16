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
	}
	
	public static class CrmFileFieldDesc {
    	public FieldType fieldType ;
    	public String fieldLinkBible ;
    	public String fieldCode ;
    	public String fieldName ;
    	public boolean fieldIsPivot ;
    	public boolean fieldIsReadonly ;
    	
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
	}
	
	public static class CrmFileRecord {
    	public int recordTmpId ;
    	public HashMap<String,CrmFileFieldValue> recordData ;
    	public CrmFilePhoto recordPhoto ;
    	public boolean recordIsDisabled ;
    	
    	public CrmFileRecord( int recordTmpId, HashMap<String,CrmFileFieldValue> recordData, boolean recordIsDisabled )  {
    		this.recordTmpId = recordTmpId ;
    		this.recordData = recordData ;
    		this.recordIsDisabled = recordIsDisabled ;
    	}
    	public CrmFileRecord( int recordTmpId, CrmFilePhoto recordPhoto, boolean recordIsDisabled )  {
    		this.recordTmpId = recordTmpId ;
    		this.recordPhoto = recordPhoto ;
    		this.recordIsDisabled = recordIsDisabled ;
    	}
	}
	
	
    public static class CrmFilePhoto {
    	public String uriString ;
    	public String uriStringThumb ;
    	
    	public CrmFilePhoto( String uriString, String uriStringThumb ) {
    		this.uriString = uriString ;
    		this.uriStringThumb = uriStringThumb ;
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
			return ;
		}
		tmpCursor.moveToNext();
		String CrmFileCode = tmpCursor.getString(0) ;
		this.CrmFileCode = CrmFileCode ;
		//Log.w(TAG,"Pouet : "+CrmFileCode) ;
		
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
    	}
		
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
    		
    		//Log.w(TAG,tmpIndex+" init done for "+tFileinfo.fileCode+" "+TransactionPageFields.get(tmpIndex).size()+" fields / "+TransactionPageRecords.get(tmpIndex).size()+" records ") ;
    		
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
		String localFileCode = tFileinfo.fileCode ;
		
		ArrayList<CrmFileFieldDesc> tFields = new ArrayList<CrmFileFieldDesc>() ;
		
		Cursor tmpCursor ;
		Cursor tmpInnerCursor ;
		
		/*
		// --- field "pivot"
		String pivotFieldCode = null ;
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
    			BibleHelper bibleHelper = BibleHelper.getInstance(mContext) ;
    			bibleIter = bibleHelper.queryBible(tFieldDesc.fieldLinkBible).iterator() ;
    			//Iterator<BibleHelper.BibleEntry> miter = bibleHelper.queryBible("STORE").iterator() ;
    			BibleHelper.BibleEntry bibleEntry ;
    			int a = 0 ;
    	    	while( bibleIter.hasNext() ){
    	    		bibleEntry = bibleIter.next() ; 
    	    		//Log.w(TAG,bibleEntry.displayStr) ;
    	    		
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
	public void page_setRecordDisabled( int pageId , int recordId, boolean isDisabled ) {
		if( list_getPageType( pageId ) != PageType.PAGETYPE_TABLE ) {
			return ;
		}
		CrmFileRecord crmFileRecord = TransactionPageRecords.get(pageId).get(recordId) ;
		crmFileRecord.recordIsDisabled = isDisabled ;
	}
	public void page_setRecordFieldValue_bible( int pageId , int recordId, int fieldId, String entryKey ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			Log.w(TAG,"Aborting...") ;
			return ;
		}
		Log.w(TAG,"Updating...") ;
		String bibleCode = TransactionPageFields.get(pageId).get(fieldId).fieldLinkBible ;
		
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		BibleHelper.BibleEntry tmpBibleEntry = BibleHelper.getInstance(mContext).prettifyEntry( new BibleHelper.BibleEntry( bibleCode, entryKey ))  ;
		record.valueString = entryKey ;
		record.displayStr = tmpBibleEntry.displayStr ;
		record.isSet = true ;
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_date( int pageId , int recordId, int fieldId, Date date ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			//Log.w(TAG,"Aborting...") ;
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		record.valueDate = date ;
		// Log.w(TAG,"okkok "+date.getYear()) ;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm") ;
		record.displayStr = sdf.format(date) ;
		record.isSet = true ;
		//TransactionPageRecords.get(pageId).get(recordId).recordData.put(TransactionPageFields.get(pageId).get(fieldId).fieldCode,record) ;

		return ;
	}
	public void page_setRecordFieldValue_text( int pageId , int recordId, int fieldId, String text ) {
		if( TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) == null ) {
			//Log.w(TAG,"Aborting...") ;
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
			//Log.w(TAG,"Aborting...") ;
			return ;
		}
		CrmFileFieldValue record = TransactionPageRecords.get(pageId).get(recordId).recordData.get(TransactionPageFields.get(pageId).get(fieldId).fieldCode) ;
		
		//Log.w(TAG,"jiejr "+num ) ;
		//Log.w(TAG,"jiejr "+String.valueOf(num) ) ;
		
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
			//Log.w(TAG,"Aborting...") ;
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
    		
    		//Log.w(TAG,jsonData.toString()) ;
    		
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
		Iterator<CrmFilePageinfo> pageIter = TransactionPages.iterator() ;
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
					Log.w(TAG,"Putting autovalue "+fieldDesc.fieldCode+" from "+fieldDesc.fieldAutovalueSrc) ;
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
				//Log.w(TAG,"Adding "+fieldDesc.fieldLinkBible+" "+mRecord.recordData.get(fieldDesc.fieldCode).valueString ) ;
				mArr.add( new BibleHelper.BibleEntry(fieldDesc.fieldLinkBible , mRecord.recordData.get(fieldDesc.fieldCode).valueString) ) ;
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
        		if( pageType==PageType.PAGETYPE_TABLE && record.recordIsDisabled ) {
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
    				//Log.w(TAG,"Lat "+lastKnownLocation.getLatitude()) ;
    				//Log.w(TAG,"Long "+lastKnownLocation.getLongitude()) ;
    				
    				
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

}
