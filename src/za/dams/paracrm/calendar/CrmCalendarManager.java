package za.dams.paracrm.calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.DatabaseManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;

public class CrmCalendarManager {
	private static final String TAG = "Calendar/CrmCalendarManager";
	
	private Context mContext ;
	private DatabaseManager mDb ;
	private BibleHelper mBibleHelper ;
	
	private String mCrmAgendaFileCode ;
	private CrmCalendarAccount mCrmAgendaInfos ;
	
	public enum CalendarField {
		CALFIELD_START, CALFIELD_END, CALFIELD_STATUS, CALFIELD_ACCOUNT
	}
	public static class CrmCalendarInput {
		public int mCrmInputId ;
		public String mCrmAgendaId ;
		public String mCrmAgendaLib ;
		public CrmCalendarInput(){
		}
		public CrmCalendarInput( int crmInputId, String crmAgendaId, String crmAgendaLib ) {
			mCrmInputId = crmInputId ;
			mCrmAgendaId = crmAgendaId ;
			mCrmAgendaLib = crmAgendaLib ;
		}
	}
	public enum CalendarDuration {
		DURATION_STD, DURATION_ALLDAY, DURATION_MULTIPLEDAYS
	}
	public static class CrmCalendarAccount {
		public String mCrmAgendaFilecode ;
		public String mCrmAgendaLib ;
		
		public String mEventStartFileField ;
		public String mEventEndFileField ;
		
		public ArrayList<CrmFileFieldDesc> mCrmFields ;
		public int mDurationCrmFieldIdx = -1 ;
		
		public boolean mIsDoneable ;
		public String mIsDoneFileField ;
		
		public boolean mAccountIsOn ;
		public String mAccountTargetFileField ;
		public String mAccountSrcBibleCode ;
		
		public HashMap<String,CalendarDuration> mDurationsTable ;
		
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
	public static CrmCalendarInput queryInputFromFilecode( Context context , String fileCode ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		
		CrmCalendarInput crmCalendarInput = null ;
		
		Cursor tCursor ;
		tCursor = mDb.rawQuery("SELECT input.calendar_id, def.file_code , def.file_lib" +
				" FROM input_calendar input , define_file def " +
				" WHERE input.target_filecode=def.file_code" +
				" ORDER BY target_filecode ASC") ;
		while( tCursor.moveToNext() ) {
			if( tCursor.getString(1).equals(fileCode) ) {
				crmCalendarInput = new CrmCalendarInput( tCursor.getInt(0), tCursor.getString(1) , tCursor.getString(2) ) ;
				break ;
			}
		}
		tCursor.close() ;
		
		return crmCalendarInput ;
	}
	public static CrmCalendarInput queryInputFromEvent( Context context , int eventId ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		
		Cursor tCursor ;
		tCursor = mDb.rawQuery(String.format("SELECT file_code FROM store_file WHERE filerecord_id='%d'",eventId)) ;
		if( tCursor.getCount() != 1 ) {
			tCursor.close() ;
			return null ;
		}
		tCursor.moveToNext() ;
		String fileCode = tCursor.getString(0) ;
		tCursor.close() ;
		
		CrmCalendarInput crmCalendarInput = null ;
		
		tCursor = mDb.rawQuery("SELECT input.calendar_id, def.file_code , def.file_lib" +
				" FROM input_calendar input , define_file def " +
				" WHERE input.target_filecode=def.file_code" +
				" ORDER BY target_filecode ASC") ;
		while( tCursor.moveToNext() ) {
			if( tCursor.getString(1).equals(fileCode) ) {
				crmCalendarInput = new CrmCalendarInput( tCursor.getInt(0), tCursor.getString(1) , tCursor.getString(2) ) ;
				break ;
			}
		}
		tCursor.close() ;
		
		return crmCalendarInput ;
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
		tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_type, entry_field_code, entry_field_lib, entry_field_linkbible, entry_field_is_highlight " +
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
    				tmpCursor.getString(1) , tmpCursor.getString(2) , tmpCursor.getString(4).equals("O")?true:false ) ) ;
    	}
    	tmpCursor.close() ;
    	
    	mCrmAgendaInfos = new CrmCalendarAccount() ;
    	mCrmAgendaInfos.mCrmAgendaFilecode = localFileCode ;
    	mCrmAgendaInfos.mCrmAgendaLib = localFileLib ;
    	mCrmAgendaInfos.mCrmFields = tFields ;
    	
    	
    	// **** Examen de la config particulière de l'agenda 
    	///       + suppression des crmFields spécifiques (start,end,account,...) 
    	ArrayList<CrmFileFieldDesc> fieldsToRemove = new ArrayList<CrmFileFieldDesc>() ;
    	
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
    				mCrmAgendaInfos.mAccountTargetFileField = accountFileField ;
    			}
    			else {
    				mCrmAgendaInfos.mAccountIsOn = false ;
    			}
    			accCursor.close() ;
    			
    			
    			
    			if( mCrmAgendaInfos.mAccountIsOn && mCrmAgendaInfos.mAccountTargetFileField != null ) {
    				// ***** suppr du champ particulier des crmFields génériques ****
    				for( CrmFileFieldDesc fd : tFields ) {
    					if( fd.fieldCode.equals(mCrmAgendaInfos.mAccountTargetFileField) ) {
    						fieldsToRemove.add(fd) ;
    						break  ;
    					}
    				}
    			}
    		}
    		
    		mCrmAgendaInfos.mEventStartFileField = tmpCursor.getString(tmpCursor.getColumnIndex("eventstart_filefield"));
    		mCrmAgendaInfos.mEventEndFileField = tmpCursor.getString(tmpCursor.getColumnIndex("eventend_filefield"));
    		if( !tmpCursor.getString(tmpCursor.getColumnIndex("eventstatus_filefield")).equals("") ) {
    			mCrmAgendaInfos.mIsDoneable = true ;
    			mCrmAgendaInfos.mIsDoneFileField = tmpCursor.getString(tmpCursor.getColumnIndex("eventstatus_filefield")) ;
    		}
			for( CrmFileFieldDesc fd : tFields ) {
				if( fd.fieldCode.equals(mCrmAgendaInfos.mEventStartFileField) 
						|| fd.fieldCode.equals(mCrmAgendaInfos.mEventEndFileField)
						|| (mCrmAgendaInfos.mIsDoneable && fd.fieldCode.equals(mCrmAgendaInfos.mIsDoneFileField)) ) {
					fieldsToRemove.add(fd) ;
				}
			}
			
	    	/// Suppression des crmFields spécifiques (start,end,account,...) 
	    	int toRemove = fieldsToRemove.size() ;
	    	int removed = 0 ;
	    	for( CrmFileFieldDesc fdToRemove : fieldsToRemove ) {
	    		if( mCrmAgendaInfos.mCrmFields.remove(fdToRemove) ) {
	    			removed++ ;
	    		}
	    	}
	    	if( toRemove != removed ){
	    		Log.w(TAG,"Big problem !") ;
	    	}

			
    		if( tmpCursor.getString(tmpCursor.getColumnIndex("duration_is_fixed")).equals("O") ) {
    			String eventDurationFileField = tmpCursor.getString(tmpCursor.getColumnIndex("duration_src_filefield")) ;
    			String eventDurationSrcBibleCode = null ;
    			String eventDurationSrcBibleField = null ;
    			
    			// recherche de l'element CrmDuration
    			int tmpIdx = -1 ;
    			for( CrmFileFieldDesc fd : mCrmAgendaInfos.mCrmFields ) {
    				tmpIdx++ ;
    				if( fd.fieldCode.equals(eventDurationFileField) ) {
    					mCrmAgendaInfos.mDurationCrmFieldIdx = tmpIdx ;
    					eventDurationSrcBibleCode = fd.fieldLinkBible ;
    					eventDurationSrcBibleField = tmpCursor.getString(tmpCursor.getColumnIndex("duration_src_biblefield")) ;
    					break ;
    				}
    			}
    			
    			// load de la table "bible"
    			if( eventDurationSrcBibleCode != null && eventDurationSrcBibleField != null ) {
    				mCrmAgendaInfos.mDurationsTable = new HashMap<String,CalendarDuration>() ;
    				
    				// @DAMS TODO : requete crade
    				String bibleQuery = String.format("SELECT entry_key,entry_field_value_number FROM store_bible_entry_field WHERE bible_code='%s' AND entry_field_code='%s'",eventDurationSrcBibleCode,eventDurationSrcBibleField) ;
    				Cursor bibleCursor = mDb.rawQuery(bibleQuery) ;
    				while( bibleCursor.moveToNext() ) {
    					String entrykey = bibleCursor.getString(0) ;
    					float duration = bibleCursor.getFloat(1) ;
    					
    					if( duration > 0 ) {
    						mCrmAgendaInfos.mDurationsTable.put(entrykey, CalendarDuration.DURATION_ALLDAY) ;
    					}
    					else {
    						mCrmAgendaInfos.mDurationsTable.put(entrykey, CalendarDuration.DURATION_STD) ;
    					}
    				}
    			}
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
	
	
	
	
	
	public void populateModelEmpty( CrmEventModel crmEventModel ) {
		crmEventModel.mCrmFileCode = mCrmAgendaInfos.mCrmAgendaFilecode ;
		
		crmEventModel.mAllDay = false ;
		
		if( mCrmAgendaInfos.mAccountIsOn ) {
			crmEventModel.hasAccount = true ;
		}
		
		if( mCrmAgendaInfos.mIsDoneable ) {
			crmEventModel.isDoneable = true ;
		}
		
		crmEventModel.mCrmFields = new ArrayList<CrmFileFieldDesc>() ;
		crmEventModel.mCrmValues = new ArrayList<CrmFileFieldValue>() ;
		for( CrmFileFieldDesc fd : mCrmAgendaInfos.mCrmFields ) {
			crmEventModel.mCrmFields.add(fd.clone()) ;
			crmEventModel.mCrmValues.add(
					new CrmFileFieldValue(fd.fieldType,
							new Float(0),false,"",new Date(),
							"",
							false
							)
					) ;
		}
	}
	public void populateModelLoad( CrmEventModel crmEventModel, int filerecordId ) {
		class StoreFileFieldRecord {
			public String fieldCode ;
			public float valueNumber ;
			public String valueString ;
			public String valueDate ;
			public String valueLink ;
			
			public StoreFileFieldRecord(){
				
			}
		}
		
		Cursor tCursor ;
		
		tCursor = mDb.rawQuery(String.format("SELECT filerecord_id FROM store_file WHERE file_code='%s' AND filerecord_id='%d'", mCrmAgendaInfos.mCrmAgendaFilecode,filerecordId));
		if( tCursor.getCount() != 1 ) {
			tCursor.close() ;
			return ;
		}
		tCursor.close() ;
		
		HashMap<String,StoreFileFieldRecord> fields = new HashMap<String,StoreFileFieldRecord>() ;
		tCursor = mDb.rawQuery(String.format("SELECT * FROM store_file_field WHERE filerecord_id='%d'", filerecordId));
		while( tCursor.moveToNext() ) {
			StoreFileFieldRecord field = new StoreFileFieldRecord() ;
			field.fieldCode = tCursor.getString(tCursor.getColumnIndex("filerecord_field_code")) ;
			field.valueNumber = tCursor.getFloat(tCursor.getColumnIndex("filerecord_field_value_number")) ;
			field.valueString = tCursor.getString(tCursor.getColumnIndex("filerecord_field_value_string")) ;
			field.valueDate = tCursor.getString(tCursor.getColumnIndex("filerecord_field_value_date")) ;
			field.valueLink = tCursor.getString(tCursor.getColumnIndex("filerecord_field_value_link")) ;
			
			fields.put(field.fieldCode, field) ;
		}
		tCursor.close() ;
		
		SimpleDateFormat datetimeParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		crmEventModel.mCrmFileCode = mCrmAgendaInfos.mCrmAgendaFilecode ;
		crmEventModel.mCalendarId = filerecordId ;
		crmEventModel.mCrmFileId = filerecordId ;
		if( fields.containsKey(mCrmAgendaInfos.mEventStartFileField) ) {
			try {
				Calendar startDate = Calendar.getInstance() ;
				startDate.setTime( datetimeParser.parse(fields.get(mCrmAgendaInfos.mEventStartFileField).valueDate) ) ;
				crmEventModel.mStart = startDate.getTimeInMillis() ;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		if( fields.containsKey(mCrmAgendaInfos.mEventEndFileField) ) {
			try {
				Calendar endDate = Calendar.getInstance() ;
				endDate.setTime( datetimeParser.parse(fields.get(mCrmAgendaInfos.mEventEndFileField).valueDate) ) ;
				crmEventModel.mEnd = endDate.getTimeInMillis() ;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		if( mCrmAgendaInfos.mIsDoneable ) {
			crmEventModel.isDoneable = true ;
			if( fields.containsKey(mCrmAgendaInfos.mIsDoneFileField) ) {
				crmEventModel.isDone = (fields.get(mCrmAgendaInfos.mIsDoneFileField).valueNumber == 1)?true:false ;
			}
		}
		
		//Log.w(TAG,"Load, start is "+crmEventModel.mStart) ;
		//Log.w(TAG,"Load, end is "+crmEventModel.mEnd) ;
		
		if( this.mCrmAgendaInfos.mAccountIsOn && fields.containsKey(mCrmAgendaInfos.mAccountTargetFileField) ) {
			if( mBibleHelper==null ) {
				mBibleHelper = new BibleHelper(mContext) ;
			}
			crmEventModel.hasAccount = true ;
			crmEventModel.mAccountEntry = mBibleHelper.getBibleEntry(mCrmAgendaInfos.mAccountSrcBibleCode
					, fields.get(mCrmAgendaInfos.mAccountTargetFileField).valueLink ) ;
			
			// Log.w(TAG,"Load account "+crmEventModel.mAccountEntry.entryKey) ;
		}
		
		crmEventModel.mCrmFields = new ArrayList<CrmFileFieldDesc>() ;
		crmEventModel.mCrmValues = new ArrayList<CrmFileFieldValue>() ;
		ArrayList<String> tCrmTitle = new ArrayList<String>() ;
		for( CrmFileFieldDesc fd : mCrmAgendaInfos.mCrmFields ) {
			crmEventModel.mCrmFields.add(fd.clone()) ;
			if( !fields.containsKey(fd.fieldCode) ) {
				crmEventModel.mCrmValues.add(
						new CrmFileFieldValue(fd.fieldType,
								new Float(0),false,"",new Date(),
								"",
								false
								)
						) ;
				continue ;
			}
			
			switch( fd.fieldType ) {
			case FIELD_BIBLE :
				if( mBibleHelper==null ) {
					mBibleHelper = new BibleHelper(mContext) ;
				}
				String bibleCode = fd.fieldLinkBible ;
				String bibleEntryKey = fields.get(fd.fieldCode).valueLink ;
				BibleEntry tBe = mBibleHelper.getBibleEntry(bibleCode, bibleEntryKey) ;
				String displayStr = "" ;
				if( tBe != null ) {
					displayStr = tBe.displayStr ;
				}
				crmEventModel.mCrmValues.add(
						new CrmFileFieldValue(fd.fieldType,
								new Float(0),false,bibleEntryKey,new Date(),
								displayStr,
								true
								)
						) ;
				if( fd.fieldIsHighlight && tBe != null ){
					tCrmTitle.add( tBe.displayStr2 ) ; 
				}
				break ;
			
			case FIELD_DATE :
			case FIELD_DATETIME :
    			Date tDate = new Date() ;
    			try {
					tDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fields.get(fd.fieldCode).valueDate) ;
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
				crmEventModel.mCrmValues.add(
						new CrmFileFieldValue(fd.fieldType,
								new Float(0),false,null,tDate,
								new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate),
								true
								)
						) ;
				if( fd.fieldIsHighlight ) {
					tCrmTitle.add( new SimpleDateFormat("dd/MM/yyyy HH:mm").format(tDate) ) ;
				}
				break ;
			
			case FIELD_TEXT :
				String tText = fields.get(fd.fieldCode).valueString ;
				crmEventModel.mCrmValues.add(
						new CrmFileFieldValue(fd.fieldType,
								new Float(0),false,tText,new Date(),
								tText,
								true
								)
						) ;				
				if( fd.fieldIsHighlight ) {
					tCrmTitle.add( tText ) ;
				}
				break ;
			
			case FIELD_NUMBER :
				Float tFloat = fields.get(fd.fieldCode).valueNumber ;
				crmEventModel.mCrmValues.add(
						new CrmFileFieldValue(fd.fieldType,
								tFloat,false,null,new Date(),
								new Float(tFloat).toString(),
								true
								)
						) ;								
				if( fd.fieldIsHighlight ) {
					tCrmTitle.add( new Float(tFloat).toString() ) ;
				}
				break ;
			}
		}
		
		crmEventModel.mCrmTitle = tCrmTitle.toArray(new String[tCrmTitle.size()]) ;
		
		
		crmEventModel.mAllDay = modelIsAllDay( crmEventModel ) ;
	}
	
	public boolean modelIsAllDay( CrmEventModel crmEventModel ) {
		if( mCrmAgendaInfos.mDurationCrmFieldIdx >= 0 && crmEventModel.mCrmValues.get(mCrmAgendaInfos.mDurationCrmFieldIdx) != null ) {
			String entrykey = crmEventModel.mCrmValues.get(mCrmAgendaInfos.mDurationCrmFieldIdx).valueString ;
			if( mCrmAgendaInfos.mDurationsTable.containsKey(entrykey) ) {
				switch( mCrmAgendaInfos.mDurationsTable.get(entrykey) ) {
				case DURATION_ALLDAY :
					return true ;
				case DURATION_MULTIPLEDAYS :
					return true ;
				default :
					return false ;
				}
			}
		}
		
		return false ;
	}
	
	
	public boolean doneCheckModel( CrmEventModel crmEventModel ) {
		if( crmEventModel.mStart <= 0 || crmEventModel.mStart > crmEventModel.mEnd ) {
			return false ;
		}
		
		boolean noneSet = true ;
		for( CrmFileFieldValue fv : crmEventModel.mCrmValues ) {
			if( fv.isSet ) {
				noneSet = false ;
			}
		}
		if( noneSet ) {
			return false ;
		}
		
		return true ;
	}
	public boolean doneSaveModel( CrmEventModel crmEventModel ) {
		String localFileCode = mCrmAgendaFileCode ;
		
		Cursor tmpCursor ;
		
		
		// ***** Bricolage des champs start/end pour allDay *****
		if( modelIsAllDay(crmEventModel) ) {
	    	Time startTime = new Time() ;
	    	startTime.set(crmEventModel.mStart) ;
	    	startTime.hour = 0 ;
	    	startTime.minute = 0 ;
	    	startTime.second = 0 ;
	    	crmEventModel.mStart = startTime.toMillis(false) ;
	    	
	    	
	    	crmEventModel.mEnd = startTime.toMillis(false) ;
		}
		
		
		
		
		// ***** Catalogue des champs spéciaux *****
		HashMap<String,CalendarField> calFields = null ;
		
    	tmpCursor = mDb.rawQuery( String.format("SELECT * FROM define_file_cfg_calendar WHERE file_code='%s'",localFileCode ) ) ;
    	if( tmpCursor.getCount() == 1 ) {
    		tmpCursor.moveToNext() ;
    		calFields = new HashMap<String,CalendarField>() ;
    		
    		// startdate
    		String startField = tmpCursor.getString(tmpCursor.getColumnIndex("eventstart_filefield")) ;
    		if( !startField.equals("") ) {
    			calFields.put(startField, CalendarField.CALFIELD_START) ;
    		}
    		// enddate
    		String endField = tmpCursor.getString(tmpCursor.getColumnIndex("eventend_filefield")) ;
    		if( !endField.equals("") ) {
    			calFields.put(endField, CalendarField.CALFIELD_END) ;
    		}
    		// isdone
    		String statusField = tmpCursor.getString(tmpCursor.getColumnIndex("eventstatus_filefield")) ;
    		if( !statusField.equals("") ) {
    			calFields.put(statusField, CalendarField.CALFIELD_STATUS) ;
    		}
    		// account
    		String accountOn = tmpCursor.getString(tmpCursor.getColumnIndex("account_is_on")) ;
    		String accountField = tmpCursor.getString(tmpCursor.getColumnIndex("account_filefield")) ;
    		if( accountOn.equals("O") && !accountField.equals("") ) {
    			calFields.put(accountField, CalendarField.CALFIELD_ACCOUNT) ;
    		}
    	}
    	tmpCursor.close() ;
    	
    	if( calFields == null ) {
    		return false ;
    	}
    	if( !calFields.values().contains(CalendarField.CALFIELD_START) 
    			|| !calFields.values().contains(CalendarField.CALFIELD_END) ){
    		return false ;
    	}
    	
    	//Log.w(TAG,"Saving !!!") ;
    	ContentValues cv ;
    	
    	long currentFileId ;
    	
    	// **** Entete fichier *****
    	if( crmEventModel.mCrmFileId == -1 ) {
        	cv = new ContentValues() ;
    		cv.put("file_code", localFileCode);
    		currentFileId = mDb.insert("store_file", cv);
    	}
    	else {
    		currentFileId = crmEventModel.mCrmFileId ;
    		mDb.execSQL(String.format("UPDATE store_file SET sync_is_synced=NULL WHERE filerecord_id='%d'",currentFileId));
    	}
    	
    	
    	mDb.execSQL(String.format("DELETE FROM store_file_field WHERE filerecord_id='%d'",currentFileId));
		// ***** Revue de tous les champs à partir du define *****
		tmpCursor = mDb.rawQuery( String.format("SELECT * FROM define_file_entry WHERE file_code='%s' ORDER BY entry_field_index",localFileCode ) ) ;
		while( tmpCursor.moveToNext() ) {
			String fileFieldCode = tmpCursor.getString(tmpCursor.getColumnIndex("entry_field_code")) ;
			if( calFields.containsKey(fileFieldCode) ) {
				switch( calFields.get(fileFieldCode) ) {
				case CALFIELD_START :
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") ;
					Calendar tmpDate = Calendar.getInstance() ;
					tmpDate.setTimeInMillis(crmEventModel.mStart) ;
					cv = new ContentValues() ;
					cv.put("filerecord_id", currentFileId);
					cv.put("filerecord_field_code", fileFieldCode);
					cv.put("filerecord_field_value_date",sdf.format(tmpDate.getTime())) ;
					mDb.insert("store_file_field", cv);
					break ;
				case CALFIELD_END :
					SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") ;
					Calendar tmpDateEnd = Calendar.getInstance() ;
					tmpDateEnd.setTimeInMillis(crmEventModel.mEnd) ;
					cv = new ContentValues() ;
					cv.put("filerecord_id", currentFileId);
					cv.put("filerecord_field_code", fileFieldCode);
					cv.put("filerecord_field_value_date",sdf2.format(tmpDateEnd.getTime())) ;
					mDb.insert("store_file_field", cv);
					break ;
				case CALFIELD_STATUS :
					cv = new ContentValues() ;
					cv.put("filerecord_id", currentFileId);
					cv.put("filerecord_field_code", fileFieldCode);
					cv.put("filerecord_field_value_number",crmEventModel.isDone? 1 : 0 ) ;
					mDb.insert("store_file_field", cv);
					break ;
				case CALFIELD_ACCOUNT :
					cv = new ContentValues() ;
					cv.put("filerecord_id", currentFileId);
					cv.put("filerecord_field_code", fileFieldCode);
					cv.put("filerecord_field_value_link",crmEventModel.mAccountEntry.entryKey) ;
					mDb.insert("store_file_field", cv);
					break ;
				}
				continue ;
			}
			
			
			CrmFileFieldValue fv = null ;
			int fieldIdx = -1 ;
			for( CrmFileFieldDesc fd : crmEventModel.mCrmFields ) {
				fieldIdx++ ;
				if( fd.fieldCode.equals(fileFieldCode) ) {
					fv = crmEventModel.mCrmValues.get(fieldIdx) ;
					break ;
				}
			}
			if( fv == null ) { // rien trouvé 
				cv = new ContentValues() ;
				cv.put("filerecord_id", currentFileId);
				cv.put("filerecord_field_code", fileFieldCode);
				mDb.insert("store_file_field", cv);
				continue ;
			}
			
			
			cv = new ContentValues() ;
			cv.put("filerecord_id", currentFileId);
			cv.put("filerecord_field_code", fileFieldCode);
			switch(fv.fieldType) {
			case FIELD_BIBLE :
				cv.put("filerecord_field_value_link", fv.valueString);
				break ;
			case FIELD_DATE :
			case FIELD_DATETIME :
				SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy-MM-dd HH:mm") ;
				cv.put("filerecord_field_value_date", sdf3.format(fv.valueDate)+":00");
				break ;
			case FIELD_NUMBER :
				cv.put("filerecord_field_value_number", fv.valueFloat);
				break ;
			case FIELD_TEXT :
				cv.put("filerecord_field_value_string", fv.valueString);
				break ;
			default :
				break ;
			}
			mDb.insert("store_file_field", cv);
		}
		
		
		
		return true ;
	}
	public boolean doneDeleteModel( CrmEventModel crmEventModel ) {
		int currentFileId = crmEventModel.mCrmFileId ;
		
		/*
		mDb.execSQL(String.format("DELETE FROM store_file WHERE filerecord_id='%d'",currentFileId));
		mDb.execSQL(String.format("DELETE FROM store_file_field WHERE filerecord_id='%d'",currentFileId));
		*/
		mDb.execSQL(String.format("UPDATE store_file SET sync_is_synced=NULL , sync_is_deleted='O' WHERE filerecord_id='%d'",currentFileId));
		
		return true ;
	}
	
	
	
	public List<CrmEventModel> queryModels(int julianDayStart, int julianDayEnd) {
		if( this.mCrmAgendaInfos.mAccountIsOn ) {
			return null ;
		}
		return queryModels(julianDayStart,julianDayEnd,null) ;
	}
	public List<CrmEventModel> queryModels( int julianDayStart, int julianDayEnd, Set<String> accounts ) {
		
		
		
		ArrayList<CrmEventModel> models = new ArrayList<CrmEventModel>() ;
		
		//Log.w(TAG,"Loading for "+this.mCrmAgendaInfos.mCrmAgendaFilecode ) ;
		//Log.w(TAG,"Accounts : "+Utils.implodeArray(accounts.toArray(new String[accounts.size()]), ", ") ) ;
		
    	// ******* Construction de la requête *********
    	StringBuilder sbEnt = new StringBuilder() ;
    	sbEnt.append("SELECT ent.filerecord_id FROM store_file ent") ;
    	sbEnt.append(String.format(" JOIN store_file_field det_start ON det_start.filerecord_id=ent.filerecord_id AND det_start.filerecord_field_code='%s'",this.mCrmAgendaInfos.mEventStartFileField)) ;
    	sbEnt.append(String.format(" JOIN store_file_field det_end ON det_end.filerecord_id=ent.filerecord_id AND det_end.filerecord_field_code='%s'",this.mCrmAgendaInfos.mEventEndFileField)) ;
    	if( this.mCrmAgendaInfos.mAccountIsOn ) {
    		sbEnt.append(String.format(" JOIN store_file_field det_acct ON det_acct.filerecord_id=ent.filerecord_id AND det_acct.filerecord_field_code='%s'",this.mCrmAgendaInfos.mAccountTargetFileField)) ;
    	}
    	sbEnt.append(" WHERE 1") ;
    	sbEnt.append(String.format(" AND ent.file_code='%s' AND ( ent.sync_is_deleted IS NULL OR ent.sync_is_deleted<>'O' )",this.mCrmAgendaInfos.mCrmAgendaFilecode)) ;
		
    	Time timeStart = new Time() ;
    	timeStart.setJulianDay(julianDayStart) ;
    	String sqlTimeStart = timeStart.format("%Y-%m-%d");
    	Time timeEnd = new Time() ;
    	timeEnd.setJulianDay(julianDayEnd+1) ;
    	String sqlTimeEnd = timeEnd.format("%Y-%m-%d");

    	sbEnt.append(String.format(" AND det_start.filerecord_field_value_date < '%s'",sqlTimeEnd)) ;
    	sbEnt.append(String.format(" AND det_end.filerecord_field_value_date >= '%s'",sqlTimeStart)) ;
    	
    	if( this.mCrmAgendaInfos.mAccountIsOn && accounts.size() > 0 ) {
    		StringBuilder sbAcct = new StringBuilder() ;
    		sbAcct.append("(") ;
    		boolean isFirst = true ;
    		for( String acct : accounts ) {
    			if( !isFirst ) {
    				sbAcct.append(",") ;
    			}
    			else{
    				isFirst = false ;
    			}
    			sbAcct.append("'"+acct+"'") ;
    		}
    		sbAcct.append(")") ;
    		
    		sbEnt.append(" AND det_acct.filerecord_field_value_link IN "+sbAcct.toString()) ;
    	}
    	else if( this.mCrmAgendaInfos.mAccountIsOn ) {
    		sbEnt.append(" AND 0") ;
    	}
    	
    	sbEnt.append(" ORDER BY det_start.filerecord_field_value_date") ;
    	
    	// Log.w(TAG,"Query is "+sbEnt.toString()) ;
    	
    	Cursor tCursor = mDb.rawQuery(sbEnt.toString()) ;
    	while( tCursor.moveToNext() ) {
    		// Log.w(TAG,"Row is "+tCursor.getInt(0)) ;
    		
    		int filerecordId = tCursor.getInt(0) ;
    		
    		CrmEventModel model = new CrmEventModel() ;
    		populateModelLoad( model , filerecordId ) ;
    		if( !(model.mCrmFileId > 0) ) {
    			continue ;
    		}
    		models.add(model) ;
    	}
    	tCursor.close() ;
    	
		
		return models ;
	}
	
	
	
	public static int scenForwardGetId( Context context, int crmInputId ) {
		DatabaseManager db = DatabaseManager.getInstance(context) ;
		
		int scenId = 0 ;
		
		Cursor tCursor = db.rawQuery(String.format("SELECT linkscen_scen_id FROM input_calendar WHERE calendar_id='%d' AND linkscen_is_on='O'", crmInputId));
		if( tCursor.getCount() == 1 ) {
			tCursor.moveToNext() ;
			
			scenId = tCursor.getInt(0) ;
		}
		tCursor.close() ;
		
		return scenId ;
	}
	public static boolean scenSetdoneIsLocked( Context context, int crmInputId ) {
		DatabaseManager db = DatabaseManager.getInstance(context) ;
		
		boolean setdoneIsLocked = false ;
		
		Cursor tCursor = db.rawQuery(String.format("SELECT setdone_is_locked FROM input_calendar WHERE calendar_id='%d'", crmInputId));
		if( tCursor.getCount() == 1 ) {
			tCursor.moveToNext() ;
			
			String setdone_is_locked = tCursor.getString(0) ;
			if( setdone_is_locked != null && setdone_is_locked.equals("O") ) {
				setdoneIsLocked = true ;
			}
		}
		tCursor.close() ;
		
		return setdoneIsLocked ;
	}
	

}
