package za.dams.paracrm.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;

import android.content.Context;
import android.content.SharedPreferences;


public class CrmEventModel implements Serializable {
	private static final String TAG = "CalendarEventModel";
	
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
    
    public boolean mAccountIsOn = false ;
    public BibleHelper.BibleEntry mAccountEntry ;
    
    public ArrayList<CrmFileFieldDesc> mCrmFields ;
    public HashMap<String,CrmFileFieldValue> mCrmData ;

    
    public CrmEventModel() {
    }

    public CrmEventModel(Context context) {
        this();
    }
    
    
}
