package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.CrmFileTransaction.FieldType;

import android.content.Context;
import android.database.Cursor;

public class CrmFileManager {
	
	private static CrmFileManager sInstance;
	
	private final Context mContext;
	
    public static synchronized CrmFileManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CrmFileManager(context);
        }
        return sInstance;
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
	public static class CrmFileRecord {
		public String fileCode ;
		public long filerecordId ;
	}
   
	private boolean isInitialized = false ;
	private ArrayList<String> mRootFileCodes = new ArrayList<String>() ;
	private HashMap<String,CrmFileDesc> mFileDescriptors = new HashMap<String,CrmFileDesc>() ;

	public boolean isInitialized() {
		return isInitialized ;
	}
	public void fileInitDescriptors() {
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
    	
    	DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
    	Cursor c ;
    	
    	int limit = getFileVisibleLimit(fileCode) ;
    	
    	ArrayList<CrmFileRecord> data = new ArrayList<CrmFileRecord>();
    	c = mDb.rawQuery(String.format("SELECT filerecord_id FROM store_file WHERE file_code='%s' ORDER BY filerecord_id DESC LIMIT %d",fileCode,limit)) ;
    	while( c.moveToNext() ) {
    		CrmFileRecord cfr = new CrmFileRecord();
    		cfr.fileCode = fileCode ;
    		cfr.filerecordId = (long)c.getInt(0) ;
    		data.add(cfr) ;
    	}
    	c.close() ;
    	return data ;
    }
}
