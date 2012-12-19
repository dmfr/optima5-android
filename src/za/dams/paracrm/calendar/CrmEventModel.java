package za.dams.paracrm.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.CrmFileTransaction.FieldType;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;


public class CrmEventModel implements Serializable {
	private static final String TAG = "CalendarEventModel";
	
	private Context mContext ;
	
	public String mCrmFileCode = "" ;
    public int mCrmFileId = -1;
    
    public int mCalendarId = -1;
    public String mCalendarDisplayName = ""; // Make sure this is in sync with the mCalendarId
	
    // This should be set the same as mStart when created and is used for making changes to
    // recurring events. It should not be updated after it is initially set.
    public long mOriginalStart = -1;
    public long mStart = -1;
    
    // This should be set the same as mEnd when created and is used for making changes to
    // recurring events. It should not be updated after it is initially set.
    public long mOriginalEnd = -1;
    public long mEnd = -1;
    public String mDuration = null;
    public boolean mAllDay = false;
    public boolean mHalfDay = false;
    
    public boolean isDoneable = false ;
    public boolean isDone = false ;
    
    public boolean hasAccount = false ;
    public BibleHelper.BibleEntry mAccountEntry ;
    
    public ArrayList<CrmFileFieldDesc> mCrmFields ;
    public ArrayList<CrmFileFieldValue> mCrmValues ;
    public String[] mCrmTitle ;

    
    public CrmEventModel() {
    }

    public CrmEventModel(Context context) {
        this();
    	mContext = context ;
    }
    public CrmEventModel(Context context, Intent intent) {
        this(context);
    }
    
    
    
    public List<BibleHelper.BibleEntry> getBibleEntries() {
    	List<BibleHelper.BibleEntry> bibleEntries = new ArrayList<BibleHelper.BibleEntry>() ;
    	
    	if( mAccountEntry != null ) {
    		bibleEntries.add(mAccountEntry.clone()) ;
    	}
    	if( mCrmFields != null && mCrmValues != null ) {
    		BibleHelper bh = new BibleHelper(mContext) ;
    		
    		int idx = -1 ;
    		for( CrmFileFieldDesc fd : mCrmFields ) {
    			idx++ ;
    			if( fd.fieldType != FieldType.FIELD_BIBLE ) {
    				continue ;
    			}
    			if( mCrmValues.get(idx) == null || mCrmValues.get(idx).valueString.equals("") ) {
    				continue ;
    			}
    			
    			BibleHelper.BibleEntry be = bh.getBibleEntry(fd.fieldLinkBible, mCrmValues.get(idx).valueString) ; 
    			if( be != null ) {
    				bibleEntries.add( be ) ;
    			}
    		}
    	}
    	    	
    	return bibleEntries ;
    }
    
    
}
