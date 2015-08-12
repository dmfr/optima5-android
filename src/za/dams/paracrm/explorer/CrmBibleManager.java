package za.dams.paracrm.explorer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.DatabaseManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class CrmBibleManager {
	
	private static CrmBibleManager sInstance;
	
	private final Context mContext;
	
    public static synchronized CrmBibleManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmBibleManager(context);
        }
        return sInstance;
    }
    public static synchronized void clearInstance() {
    	if( sInstance != null ) {
    		sInstance = null ;
    	}
    }
	private CrmBibleManager( Context context ) {
		mContext = context.getApplicationContext() ;
	}
	
	
	
    // File descriptions
	public enum FieldType {
	    FIELD_TEXT, FIELD_BOOLEAN, FIELD_NUMBER, FIELD_DATE, FIELD_DATETIME, FIELD_BIBLE, FIELD_NULL
	};
	public static class CrmBibleDesc {
		public String bibleCode ;
		public String bibleName ;
		
		public boolean bibleHasGmap ;
		public boolean bibleHasGallery ;
		public ArrayList<CrmBibleFieldDesc> fieldsDesc ;
	}
	public static class CrmBibleFieldDesc {
    	public FieldType fieldType ;
    	public String fieldLinkBible ;
    	public String fieldCode ;
    	public String fieldName ;
    	
    	public boolean fieldIsHeader ;
    	public boolean fieldIsHighlight ;
	}
	public static class CrmBibleFieldValue {
    	public FieldType fieldType ;
    	public float valueFloat ;
    	public boolean valueBoolean ;
    	public String valueString ;
    	public Date valueDate ;
    	public String displayStr ;
    	public String displayStr1 ;
    	public String displayStr2 ;
    	
    	public CrmBibleFieldValue( FieldType fieldType,
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
	public static class CrmBibleRecord {
		public String fileCode ;
		public String entryKey ;
    	public HashMap<String,CrmBibleFieldValue> recordData ;
    	public CrmBiblePhoto defaultPhoto ;
    	public ArrayList<CrmBiblePhoto> galleryPhotos ;
	}
	
	public static class CrmBiblePhoto {
		public String mediaId ;
	}
   
	private boolean isInitialized = false ;
	private List<String> mPublishedBibleCodes = new ArrayList<String>() ;
	private List<String> mBibleCodes = new ArrayList<String>() ;
	private HashMap<String,CrmBibleDesc> mBibleDescriptors = new HashMap<String,CrmBibleDesc>() ;

	public boolean isInitialized() {
		return isInitialized ;
	}
	public synchronized void bibleInitDescriptors() {
		if( isInitialized ) {
			return ;
		}
		
		DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
		Cursor c ;
		
		c = mDb.rawQuery("SELECT target_bible_code FROM input_store_src WHERE target_bible_code IS NOT NULL AND target_bible_code<>''") ;
		while( c.moveToNext() ) {
			mPublishedBibleCodes.add(c.getString(0)) ;
		}
		c.close() ;
		
		c = mDb.rawQuery("SELECT bible_code FROM define_bible ORDER BY bible_code") ;
		while( c.moveToNext() ) {
			bibleLoadDescriptor(c.getString(0)) ;
		}
		c.close() ;
		
		isInitialized = true ;
	}
	
	public CrmBibleDesc bibleGetBibleDescriptor( String bibleCode ) {
		if( !mBibleDescriptors.containsKey(bibleCode) ) {
			return null ;
    	}
    	return mBibleDescriptors.get(bibleCode) ;
    }
	
	
	public ArrayList<CrmBibleDesc> bibleGetDescriptors() {
		ArrayList<CrmBibleDesc> tDescriptors = new ArrayList<CrmBibleDesc>();
		for( String bibleCode : mBibleCodes ) {
			if( !mPublishedBibleCodes.contains(bibleCode) ) {
				continue ;
			}
			CrmBibleDesc cbd = bibleGetBibleDescriptor(bibleCode) ;
			tDescriptors.add(cbd) ;
		}
		return tDescriptors ;
	}
	
    private void bibleLoadDescriptor(String bibleCode) {
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	
    	CrmBibleDesc cbd = new CrmBibleDesc() ;
    	
    	c = mDb.rawQuery( String.format("SELECT bible_code, bible_lib, gmap_is_on, gallery_is_on FROM define_bible WHERE bible_code='%s'",bibleCode) ) ;
    	if( c.getCount() != 1 ) {
    		return ;
    	}
    	c.moveToNext() ;
    	cbd.bibleCode = c.getString(0);
    	cbd.bibleName = c.getString(1);
    	cbd.bibleHasGmap = c.getString(2).equals("O") ? true : false ;
    	cbd.bibleHasGallery = c.getString(3).equals("O") ? true : false ;
    	c.close() ;
    	
    	cbd.fieldsDesc = new ArrayList<CrmBibleFieldDesc>() ;
    	c = mDb.rawQuery( String.format("SELECT entry_field_code, entry_field_lib, entry_field_type, entry_field_linkbible, entry_field_is_header, entry_field_is_highlight FROM define_bible_entry WHERE bible_code='%s' ORDER BY entry_field_index",bibleCode) ) ;
    	while( c.moveToNext() ) {
    		CrmBibleFieldDesc cbfd = new CrmBibleFieldDesc() ;
    		cbfd.fieldCode = c.getString(0) ;
    		cbfd.fieldName = c.getString(1) ;
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
    		else if( c.getString(2).equals("string") ) {
    			tFieldType = FieldType.FIELD_TEXT ;
    		}
    		else if( c.getString(2).equals("bool") ) {
    			tFieldType = FieldType.FIELD_BOOLEAN ;
    		}
    		else{
    			tFieldType = FieldType.FIELD_NULL ;
    		}
    		cbfd.fieldType = tFieldType ;
    		cbfd.fieldLinkBible = c.getString(3) ;
    		cbfd.fieldIsHeader = c.getString(4).equals("O") ? true : false ;
    		cbfd.fieldIsHighlight = c.getString(5).equals("O") ? true : false ;

    		cbd.fieldsDesc.add(cbfd) ;
    	}
    	c.close() ;
    	
    	
    	
    	// Store...
   		mBibleCodes.add(bibleCode) ;
    	mBibleDescriptors.put(cbd.bibleCode,cbd) ;
    }
    
    
    public List<CrmBibleRecord> biblePullData( String bibleCode ) {
    	return biblePullData(bibleCode,null) ;
    }
    public List<CrmBibleRecord> biblePullData( String bibleCode, String entryKey ) {
		class StoreBibleFieldRecord {
			public String fieldCode ;
			public float valueNumber ;
			public String valueString ;
			public String valueDate ;
			public String valueLink ;
		}
		class StoreBibleGalleryRecord {
			public String mediaId ;
			public boolean mediaIsDefault ;
		}
    	
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	BibleHelper mBibleHelper = null ;
    	
    	String queryMaster ;
    	if( entryKey != null ) {
    		queryMaster = String.format("SELECT entry_key FROM store_bible_entry WHERE bible_code='%s' AND entry_key='%s'",bibleCode,entryKey) ;
    	} else {
    		queryMaster = String.format("SELECT entry_key FROM store_bible_entry WHERE bible_code='%s'",bibleCode) ;
    	}
    	
		HashMap<String,HashMap<String,StoreBibleFieldRecord>> dbRecordFields = new HashMap<String,HashMap<String,StoreBibleFieldRecord>>() ;
		c = mDb.rawQuery(String.format("SELECT * FROM store_bible_entry_field WHERE bible_code='%s' AND entry_key IN (%s)", bibleCode, queryMaster));
		while( c.moveToNext() ) {
			String cEntryKey = c.getString(c.getColumnIndex("entry_key")) ;
			
			if( !dbRecordFields.containsKey(cEntryKey) ) {
				dbRecordFields.put(cEntryKey, new HashMap<String,StoreBibleFieldRecord>()) ;
			}
			
			StoreBibleFieldRecord field = new StoreBibleFieldRecord() ;
			field.fieldCode = c.getString(c.getColumnIndex("entry_field_code")) ;
			field.valueNumber = c.getFloat(c.getColumnIndex("entry_field_value_number")) ;
			field.valueString = c.getString(c.getColumnIndex("entry_field_value_string")) ;
			field.valueDate = c.getString(c.getColumnIndex("entry_field_value_date")) ;
			field.valueLink = c.getString(c.getColumnIndex("entry_field_value_link")) ;
			
			dbRecordFields.get(cEntryKey).put(field.fieldCode, field) ;
		}
		c.close() ;
	
		HashMap<String,ArrayList<StoreBibleGalleryRecord>> dbRecordMedias = new HashMap<String,ArrayList<StoreBibleGalleryRecord>>() ;
		c = mDb.rawQuery(String.format("SELECT * FROM store_bible_entry_gallery WHERE bible_code='%s' AND entry_key IN (%s) ORDER BY entry_key, media_idx", bibleCode, queryMaster));
		while( c.moveToNext() ) {
			String cEntryKey = c.getString(c.getColumnIndex("entry_key")) ;
			
			if( !dbRecordMedias.containsKey(cEntryKey) ) {
				dbRecordMedias.put(cEntryKey, new ArrayList<StoreBibleGalleryRecord>()) ;
			}
			
			StoreBibleGalleryRecord media = new StoreBibleGalleryRecord() ;
			media.mediaId = c.getString(c.getColumnIndex("media_id")) ;
			media.mediaIsDefault = c.getString(c.getColumnIndex("media_is_default")).equals("O") ;
			dbRecordMedias.get(cEntryKey).add(media) ;
		}
		c.close() ;
		
	
		CrmBibleDesc cbd = bibleGetBibleDescriptor(bibleCode) ;
    
    	ArrayList<CrmBibleRecord> data = new ArrayList<CrmBibleRecord>();
    	c = mDb.rawQuery(String.format("SELECT entry_key FROM store_bible_entry WHERE entry_key IN (%s) ORDER BY entry_key",queryMaster)) ;
    	while( c.moveToNext() ) {
    		CrmBibleRecord cbr = new CrmBibleRecord();
    		cbr.fileCode = bibleCode ;
    		cbr.entryKey = c.getString(0) ;
    		cbr.recordData = new HashMap<String,CrmBibleFieldValue>() ;
    		
    		for( CrmBibleFieldDesc fd : cbd.fieldsDesc ) {
    			if( !dbRecordFields.containsKey(cbr.entryKey) || !dbRecordFields.get(cbr.entryKey).containsKey(fd.fieldCode) ) {
    				cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								new Float(0),false,"",new Date(),
    								"","",""
    								)
    						) ;
    				continue ;
    			}
    			
    			StoreBibleFieldRecord sbfr = dbRecordFields.get(cbr.entryKey).get(fd.fieldCode) ;
    			
    			switch( fd.fieldType ) {
    			case FIELD_DATE :
    			case FIELD_DATETIME :
        			Date tDate = new Date() ;
        			try {
    					tDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sbfr.valueDate) ;
    				} catch (ParseException e) {
    					// TODO Auto-generated catch block
    					//e.printStackTrace();
    				}
        			cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								new Float(0),false,null,tDate,
    								new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate),"",""
    								)
    						) ;
    				break ;
    			
    			case FIELD_TEXT :
    				String tText = sbfr.valueString ;
    				cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								new Float(0),false,tText,new Date(),
    								tText,"",""
    								)
    						) ;				
    				break ;
    			case FIELD_BIBLE :
    				String tLink = sbfr.valueLink ;
    				cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								new Float(0),false,tLink,new Date(),
    								tLink,"",""
    								)
    						) ;				
    				break ;

    			case FIELD_NUMBER :
    				Float tFloat = sbfr.valueNumber ;
    				float f = sbfr.valueNumber ;
    				cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								tFloat,false,null,new Date(),
    								displayFloat( f ),"",""
    								)
    						) ;								
    				break ;

    			case FIELD_BOOLEAN :
    				cbr.recordData.put(fd.fieldCode,
    						new CrmBibleFieldValue(fd.fieldType,
    								new Float(0),(sbfr.valueNumber==1),null,new Date(),
    								(sbfr.valueNumber==1)?"true":"false","",""
    								)
    						) ;								
    				break ;
    			}
    		}
    		
    		if( cbd.bibleHasGallery ) {
    			cbr.defaultPhoto = null ;
    			cbr.galleryPhotos = new ArrayList<CrmBiblePhoto>() ; 
    			if( dbRecordMedias.containsKey(cbr.entryKey) ) {
    				for( StoreBibleGalleryRecord sbgr : dbRecordMedias.get(cbr.entryKey) ) {
    					CrmBiblePhoto cbp = new CrmBiblePhoto() ;
    					cbp.mediaId = sbgr.mediaId ;
    					cbr.galleryPhotos.add(cbp) ;
    					if( sbgr.mediaIsDefault ) {
    						cbr.defaultPhoto = cbp ;
    					}
    				}
    			}
    		}
    		
    		data.add(cbr) ;
    	}
    	c.close() ;
    	return data ;
    }
    
    
    public String displayFloat( float f ) {
    	if( f==(int)(f) ) {
    		return String.valueOf((int)f) ;
    	} else {
    		return String.valueOf(f) ;
    	}
    }
}
