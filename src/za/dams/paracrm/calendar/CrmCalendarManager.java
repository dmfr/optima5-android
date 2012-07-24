package za.dams.paracrm.calendar;

import java.util.ArrayList;
import java.util.HashMap;

import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class CrmCalendarManager {
	private static final String TAG = "Calendar/CrmCalendarManager";
	
	private Context mContext ;
	private DatabaseManager mDb ;
	
	private String mCrmAgendaFileCode ;
	private CrmCalendarAccount mCrmAgendaInfos ;
	
	public static class CrmCalendarInput {
		public int mCrmInputId ;
		public String mCrmAgendaId ;
		public String mCrmAgendaLib ;
		public CrmCalendarInput(){
		}
		public CrmCalendarInput( int crmInputId, String crmAgendaId, String crmAgendaLib ) {
			mCrmInputId = crmInputId ;
			mCrmAgendaId = crmAgendaId ;
			mCrmAgendaId = crmAgendaLib ;
		}
	}
	public static class CrmCalendarAccount {
		public String mCrmAgendaFilecode ;
		public String mCrmAgendaLib ;
		
		public ArrayList<CrmFileFieldDesc> mCrmFields ;
		
		public boolean mAccountIsOn ;
		public String mAccountSrcBibleCode ;
		
		public CrmCalendarAccount(){
			// empty constructor
		}
	}
	
	
	public static ArrayList<CrmCalendarInput> inputsList( Context context ) {
		ArrayList<CrmCalendarInput> mInputs = new ArrayList<CrmCalendarInput>();
		
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		Cursor tCursor = mDb.rawQuery("SELECT input.calendar_id, def.file_code , def.file_lib" +
				" FROM input_calendar input , define_file def " +
				" WHERE input.target_filecode=def.file_code" +
				" ORDER BY target_filecode ASC") ;
		while( tCursor.moveToNext() ) {
			mInputs.add( new CrmCalendarInput( tCursor.getInt(0), tCursor.getString(1) , tCursor.getString(2) ) ) ;
		}
		tCursor.close() ;
		
		return mInputs ;
	}
	
	
	
	public CrmCalendarManager( Context context, int crmInputId ) {
		mDb = DatabaseManager.getInstance(context) ;
		mContext = context ;
		
		Cursor tCursor = mDb.rawQuery(String.format("SELECT target_filecode FROM input_calendar WHERE calendar_id='%d'", crmInputId));
		if( tCursor.getCount() == 1 ) {
			tCursor.moveToNext() ;
			
			mCrmAgendaFileCode = tCursor.getString(0) ;
		}
		tCursor.close() ;
		
		CrmCalendarManagerLoad() ;
	}
	public CrmCalendarManager( Context context, String crmAgendaFileCode ) {
		mDb = DatabaseManager.getInstance(context) ;
		mContext = context ;
		
		mCrmAgendaFileCode = crmAgendaFileCode ;
		
		CrmCalendarManagerLoad() ;
	}
	public void CrmCalendarManagerLoad(){
		String localFileCode = mCrmAgendaFileCode ;
		Cursor tmpCursor ;
		
		tmpCursor = mDb.rawQuery( String.format("SELECT * FROM define_file WHERE file_code='%s'",localFileCode ) ) ;
		if( tmpCursor.getCount() != 1 ) {
			tmpCursor.close() ;
			return ;
		}
		tmpCursor.moveToNext() ;
		String localFileLib = tmpCursor.getString(tmpCursor.getColumnIndex("file_lib")) ;
		tmpCursor.close() ;
		
		
		ArrayList<CrmFileFieldDesc> tFields = new ArrayList<CrmFileFieldDesc>() ;
		tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_type, entry_field_code, entry_field_lib, entry_field_linkbible " +
				"FROM define_file_entry " +
				"WHERE file_code='%s' ORDER BY entry_field_index",
				localFileCode) ) ;
		FieldType tFieldType ;
    	while( tmpCursor.moveToNext() ){
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
        			}
        			else{
        				tFieldType = FieldType.FIELD_TEXT ;
        			}
    			}
    		}
    		
    		
    		tFields.add( new CrmFileFieldDesc( tFieldType, tmpCursor.getString(3),
    				tmpCursor.getString(1) , tmpCursor.getString(2) , false, false ) ) ;
    	}
    	tmpCursor.close() ;
    	
    	mCrmAgendaInfos = new CrmCalendarAccount() ;
    	mCrmAgendaInfos.mCrmAgendaFilecode = localFileCode ;
    	mCrmAgendaInfos.mCrmAgendaLib = localFileLib ;
    	mCrmAgendaInfos.mCrmFields = tFields ;
    	
    	
    	tmpCursor = mDb.rawQuery( String.format("SELECT * FROM define_file_cfg_calendar WHERE file_code='%s'",localFileCode ) ) ;
    	if( tmpCursor.getCount() == 1 ) {
    		tmpCursor.moveToNext() ;
    		mCrmAgendaInfos.mAccountIsOn = tmpCursor.getString(tmpCursor.getColumnIndex("account_is_on")).equals("O") ? true : false ;
    		if( mCrmAgendaInfos.mAccountIsOn ) {
    			String accountFileField = tmpCursor.getString(tmpCursor.getColumnIndex("account_filefield")) ;
    			Cursor accCursor = mDb.rawQuery( String.format("SELECT entry_field_linkbible FROM define_file_entry WHERE file_code='%s' AND entry_field_code='%s' AND entry_field_type='link'",localFileCode,accountFileField ) ) ;
    			if( accCursor.getCount() == 1 ) {
    				accCursor.moveToNext() ;
    				mCrmAgendaInfos.mAccountSrcBibleCode = accCursor.getString(0) ;
    			}
    			else {
    				mCrmAgendaInfos.mAccountIsOn = false ;
    			}
    			accCursor.close() ;
    		}
    		
    	}
    	tmpCursor.close() ;
	}
	
	public boolean isValid() {
		if( mCrmAgendaInfos != null ){
			return true ;
		}
		return false ;
	}
	public CrmCalendarAccount getCalendarInfos() {
		return mCrmAgendaInfos ;
	}
	

}
