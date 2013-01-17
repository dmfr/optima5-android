package za.dams.paracrm.explorer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.CrmFileTransaction.CrmFilePhoto;
import za.dams.paracrm.CrmFileTransaction.FieldType;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class CrmFileManager {
	
	private static CrmFileManager sInstance;
	
	private final Context mContext;
	
    public static synchronized CrmFileManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmFileManager(context);
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
	private CrmFileManager( Context context ) {
		mContext = context.getApplicationContext() ;
	}
	
	
	
    // Current fileLimits
	public static final int VISIBLE_LIMIT_DEFAULT = 25;
	public static final int VISIBLE_LIMIT_INCREMENT = 25;
	private HashMap<String,Integer> mFileVisibleLimit = new HashMap<String,Integer>() ;

    public int getFileVisibleLimit(String fileCode) {
    	if( !mFileVisibleLimit.containsKey(fileCode) ) {
    		mFileVisibleLimit.put(fileCode,VISIBLE_LIMIT_DEFAULT) ;
    		return VISIBLE_LIMIT_DEFAULT ;
    	}
    	return mFileVisibleLimit.get(fileCode);
    }
    public void increaseFileVisibleLimit(String fileCode) {
    	mFileVisibleLimit.put(fileCode,getFileVisibleLimit(fileCode)+VISIBLE_LIMIT_INCREMENT) ;
    }
    public void resetFileVisibleLimit(String fileCode) {
    	mFileVisibleLimit.put(fileCode,VISIBLE_LIMIT_DEFAULT) ;
    }
	
	
    // File descriptions
	public enum FieldType {
	    FIELD_TEXT, FIELD_NUMBER, FIELD_DATE, FIELD_DATETIME, FIELD_BIBLE, FIELD_NULL
	};
	public static class CrmFileDesc {
		public String fileCode ;
		public String fileName ;
		
		public boolean fileIsRootLevel ;
		public String parentFileCode ;
		public ArrayList<String> childFileCodes ;
		
		public boolean fileHasGmap ;
		public boolean fileIsMediaImg ;
		public ArrayList<CrmFileFieldDesc> fieldsDesc ;
	}
	public static class CrmFileFieldDesc {
    	public FieldType fieldType ;
    	public String fieldLinkBible ;
    	public String fieldCode ;
    	public String fieldName ;
    	
    	public boolean fieldIsHeader ;
    	public boolean fieldIsHighlight ;
	}
	public static class CrmFileFieldValue {
    	public FieldType fieldType ;
    	public float valueFloat ;
    	public boolean valueBoolean ;
    	public String valueString ;
    	public Date valueDate ;
    	public String displayStr ;
    	public String displayStr1 ;
    	public String displayStr2 ;
    	
    	public CrmFileFieldValue( FieldType fieldType,
    			float valueFloat ,
    			boolean valueBoolean ,
    			String valueString ,
    			Date valueDate ,
    			String displayStr ,
    			String displayStr1 ,
    			String displayStr2 )  {
    		this.fieldType = fieldType ;
    		this.valueFloat = valueFloat ;
    		this.valueBoolean = valueBoolean ;
    		this.valueString = valueString ;
    		this.valueDate = valueDate ;
    		this.displayStr = displayStr ;
    		this.displayStr1 = displayStr1 ;
    		this.displayStr2 = displayStr2 ;
    	}
	}
	public static class CrmFileRecord {
		public String fileCode ;
		public long filerecordId ;
		public String syncVuid ;
    	public HashMap<String,CrmFileFieldValue> recordData ;
    	public CrmFilePhoto recordPhoto ;
	}
   
	private boolean isInitialized = false ;
	private ArrayList<String> mRootFileCodes = new ArrayList<String>() ;
	private HashMap<String,CrmFileDesc> mFileDescriptors = new HashMap<String,CrmFileDesc>() ;

	public boolean isInitialized() {
		return isInitialized ;
	}
	public synchronized void fileInitDescriptors() {
		if( isInitialized ) {
			return ;
		}
		
		DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
		Cursor c = mDb.rawQuery("SELECT file_code FROM define_file WHERE file_parent_code IS NULL OR file_parent_code='' ORDER BY file_code") ;
		while( c.moveToNext() ) {
			fileLoadDescriptor(c.getString(0)) ;
		}
		c.close() ;
		isInitialized = true ;
	}
	public CrmFileDesc fileGetFileDescriptor( String fileCode ) {
		if( !mFileDescriptors.containsKey(fileCode) ) {
			return null ;
    	}
    	return mFileDescriptors.get(fileCode) ;
    }
	
	
	public ArrayList<CrmFileDesc> fileGetRootDescriptors() {
		ArrayList<CrmFileDesc> tChildDescriptors = new ArrayList<CrmFileDesc>();
		for( String rootFileCode : mRootFileCodes ) {
			CrmFileDesc cfd = fileGetFileDescriptor(rootFileCode) ;
			tChildDescriptors.add(cfd) ;
		}
		return tChildDescriptors ;
	}
	public ArrayList<CrmFileDesc> fileGetChildDescriptors( String fileCode ) {
		if( !mFileDescriptors.containsKey(fileCode) ) {
			return null ;
    	}
		
		ArrayList<CrmFileDesc> tChildDescriptors = new ArrayList<CrmFileDesc>();
		for( String childFileCode : fileGetFileDescriptor(fileCode).childFileCodes ) {
			tChildDescriptors.add(fileGetFileDescriptor(childFileCode)) ;
		}
		return tChildDescriptors ;
	}
    private void fileLoadDescriptor(String fileCode) {
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	
    	CrmFileDesc cfd = new CrmFileDesc() ;
    	
    	c = mDb.rawQuery( String.format("SELECT file_code, file_lib, file_type, gmap_is_on, file_parent_code FROM define_file WHERE file_code='%s'",fileCode) ) ;
    	if( c.getCount() != 1 ) {
    		return ;
    	}
    	c.moveToNext() ;
    	cfd.fileCode = c.getString(0);
    	cfd.fileName = c.getString(1);
    	cfd.fileIsRootLevel = ( c.getString(4) == null || c.getString(4).equals("") ) ;
    	cfd.parentFileCode = (c.getString(4) == null) ? "" : c.getString(4) ;
    	cfd.fileHasGmap = c.getString(3).equals("O") ? true : false ;
    	cfd.fileIsMediaImg = c.getString(2).equals("media_img") ? true : false ;
    	c.close() ;
    	
    	cfd.fieldsDesc = new ArrayList<CrmFileFieldDesc>() ;
    	c = mDb.rawQuery( String.format("SELECT entry_field_code, entry_field_lib, entry_field_type, entry_field_linkbible, entry_field_is_header, entry_field_is_highlight FROM define_file_entry WHERE file_code='%s' ORDER BY entry_field_index",fileCode) ) ;
    	while( c.moveToNext() ) {
    		CrmFileFieldDesc cffd = new CrmFileFieldDesc() ;
    		cffd.fieldCode = c.getString(0) ;
    		cffd.fieldName = c.getString(1) ;
    		FieldType tFieldType ;
    		if( c.getString(2).equals("number") ) {
				tFieldType = FieldType.FIELD_NUMBER ;
			}
    		else if( c.getString(2).equals("date") ) {
    			tFieldType = FieldType.FIELD_DATETIME ;
    		}
    		else if( c.getString(2).equals("link") ) {
    			tFieldType = FieldType.FIELD_BIBLE ;
    		}
    		else{
    			tFieldType = FieldType.FIELD_TEXT ;
    		}
    		cffd.fieldType = tFieldType ;
    		cffd.fieldLinkBible = c.getString(3) ;
    		cffd.fieldIsHeader = c.getString(4).equals("O") ? true : false ;
    		cffd.fieldIsHighlight = c.getString(5).equals("O") ? true : false ;

    		cfd.fieldsDesc.add(cffd) ;
    	}
    	c.close() ;
    	
    	
    	
    	// Chargement des subfiles
    	cfd.childFileCodes = new ArrayList<String>() ;
    	c = mDb.rawQuery( String.format("SELECT file_code FROM define_file WHERE file_parent_code='%s' ORDER BY file_code",cfd.fileCode)) ;
    	while( c.moveToNext() ) {
    		fileLoadDescriptor(c.getString(0)) ;
    		cfd.childFileCodes.add(c.getString(0)) ;
    	}
    	c.close() ;
    	
    	// Store...
    	if( cfd.fileIsRootLevel ) {
    		mRootFileCodes.add(fileCode) ;
    	}
    	mFileDescriptors.put(cfd.fileCode,cfd) ;
    }
    
    
    private BibleHelper.BibleEntry pullFilterBe ;
    public void setPullFilter( BibleHelper.BibleEntry be ) {
    	pullFilterBe = be ;
    }
    public List<CrmFileRecord> filePullData( String fileCode ) {
    	return filePullData(fileCode,0,0) ;
    }
    public List<CrmFileRecord> filePullData( String fileCode, long filerecordId, long filerecordParentId ) {
		class StoreFileFieldRecord {
			public String fieldCode ;
			public float valueNumber ;
			public String valueString ;
			public String valueDate ;
			public String valueLink ;
		}
    	
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	BibleHelper mBibleHelper = null ;
    	
    	String queryMaster ;
    	if( filerecordId > 0 ) {
    		queryMaster = String.format("SELECT filerecord_id FROM store_file WHERE file_code='%s' AND filerecord_id='%d'",fileCode,filerecordId) ;
    	} else if( filerecordParentId > 0 ) {
    		queryMaster = String.format("SELECT filerecord_id FROM store_file WHERE file_code='%s' AND filerecord_parent_id='%d'",fileCode,filerecordParentId) ;
    	} else {
        	int limit = getFileVisibleLimit(fileCode) ;
        	queryMaster = String.format("SELECT filerecord_id FROM store_file WHERE file_code='%s' ORDER BY sync_timestamp DESC LIMIT %d",fileCode,limit) ;
    	}
    	
		HashMap<Long,HashMap<String,StoreFileFieldRecord>> dbRecordFields = new HashMap<Long,HashMap<String,StoreFileFieldRecord>>() ;
		c = mDb.rawQuery(String.format("SELECT * FROM store_file_field WHERE filerecord_id IN (%s)", queryMaster));
		while( c.moveToNext() ) {
			long cfilerecordId = c.getLong(c.getColumnIndex("filerecord_id")) ;
			
			if( !dbRecordFields.containsKey(cfilerecordId) ) {
				dbRecordFields.put(cfilerecordId, new HashMap<String,StoreFileFieldRecord>()) ;
			}
			
			StoreFileFieldRecord field = new StoreFileFieldRecord() ;
			field.fieldCode = c.getString(c.getColumnIndex("filerecord_field_code")) ;
			field.valueNumber = c.getFloat(c.getColumnIndex("filerecord_field_value_number")) ;
			field.valueString = c.getString(c.getColumnIndex("filerecord_field_value_string")) ;
			field.valueDate = c.getString(c.getColumnIndex("filerecord_field_value_date")) ;
			field.valueLink = c.getString(c.getColumnIndex("filerecord_field_value_link")) ;
			
			dbRecordFields.get(cfilerecordId).put(field.fieldCode, field) ;
		}
		c.close() ;
	
    	
    	ArrayList<CrmFileRecord> data = new ArrayList<CrmFileRecord>();
    	c = mDb.rawQuery(String.format("SELECT filerecord_id, sync_vuid FROM store_file WHERE filerecord_id IN (%s)",queryMaster)) ;
    	while( c.moveToNext() ) {
    		CrmFileRecord cfr = new CrmFileRecord();
    		cfr.fileCode = fileCode ;
    		cfr.filerecordId = (long)c.getInt(0) ;
    		cfr.syncVuid = c.getString(1) ;
    		cfr.recordData = new HashMap<String,CrmFileFieldValue>() ;
    		
    		for( CrmFileFieldDesc fd : fileGetFileDescriptor(fileCode).fieldsDesc ) {
    			if( !dbRecordFields.containsKey(cfr.filerecordId) || !dbRecordFields.get(cfr.filerecordId).containsKey(fd.fieldCode) ) {
    				cfr.recordData.put(fd.fieldCode,
    						new CrmFileFieldValue(fd.fieldType,
    								new Float(0),false,"",new Date(),
    								"","",""
    								)
    						) ;
    				continue ;
    			}
    			
    			StoreFileFieldRecord sffr = dbRecordFields.get(cfr.filerecordId).get(fd.fieldCode) ;
    			
    			switch( fd.fieldType ) {
    			case FIELD_BIBLE :
    				if( mBibleHelper==null ) {
    					mBibleHelper = new BibleHelper(mContext) ;
    				}
    				String bibleCode = fd.fieldLinkBible ;
    				String bibleEntryKey = sffr.valueLink ;
    				BibleEntry tBe = mBibleHelper.getBibleEntry(bibleCode, bibleEntryKey) ;
    				String displayStr = "" ;
    				String displayStr1 = "" ;
    				String displayStr2 = "" ;
    				if( tBe != null ) {
    					displayStr = tBe.displayStr ;
    					displayStr1 = tBe.displayStr1 ;
    					displayStr2 = tBe.displayStr2 ;
    				} else {
    					//Log.w("CRMFILEMAN","Cant find "+bibleCode+" "+bibleEntryKey) ;
    				}
    				
    				cfr.recordData.put(fd.fieldCode,
    						new CrmFileFieldValue(fd.fieldType,
    								new Float(0),false,bibleEntryKey,new Date(),
    								displayStr,displayStr1,displayStr2
    								)
    						) ;
    				break ;
    			
    			case FIELD_DATE :
    			case FIELD_DATETIME :
        			Date tDate = new Date() ;
        			try {
    					tDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sffr.valueDate) ;
    				} catch (ParseException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    				}
        			cfr.recordData.put(fd.fieldCode,
    						new CrmFileFieldValue(fd.fieldType,
    								new Float(0),false,null,tDate,
    								new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate),"",""
    								)
    						) ;
    				break ;
    			
    			case FIELD_TEXT :
    				String tText = sffr.valueString ;
    				cfr.recordData.put(fd.fieldCode,
    						new CrmFileFieldValue(fd.fieldType,
    								new Float(0),false,tText,new Date(),
    								tText,"",""
    								)
    						) ;				
    				break ;
    			
    			case FIELD_NUMBER :
    				Float tFloat = sffr.valueNumber ;
    				float f = sffr.valueNumber ;
    				cfr.recordData.put(fd.fieldCode,
    						new CrmFileFieldValue(fd.fieldType,
    								tFloat,false,null,new Date(),
    								displayFloat( f ),"",""
    								)
    						) ;								
    				break ;
    			}
    		}
    		
    		
    		data.add(cfr) ;
    	}
    	c.close() ;
    	return data ;
    }
    
    
    public String lookupFilecodeForId( long filerecordId ) {
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	c = mDb.rawQuery(String.format("SELECT file_code FROM store_file WHERE filerecord_id='%d'",filerecordId)) ;
    	if( c.getCount() != 1 ) {
    		return null ;
    	}
    	c.moveToNext() ;
    	return c.getString(0) ;
    }
    
    public String displayFloat( float f ) {
    	if( f==(int)(f) ) {
    		return String.valueOf((int)f) ;
    	} else {
    		return String.valueOf(f) ;
    	}
    }
}
