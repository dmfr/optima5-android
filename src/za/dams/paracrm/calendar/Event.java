/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.dams.paracrm.calendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Debug;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import za.dams.paracrm.R;
import za.dams.paracrm.calendar.CrmCalendarManager.CrmCalendarInput;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event implements Cloneable {

    private static final String TAG = "CalEvent";
    private static final boolean PROFILE = false;

    /**
     * The sort order is:
     * 1) events with an earlier start (begin for normal events, startday for allday)
     * 2) events with a later end (end for normal events, endday for allday)
     * 3) the title (unnecessary, but nice)
     *
     * The start and end day is sorted first so that all day events are
     * sorted correctly with respect to events that are >24 hours (and
     * therefore show up in the allday area).
     */
    private static final String SORT_EVENTS_BY =
            "begin ASC, end DESC, title ASC";
    private static final String SORT_ALLDAY_BY =
            "startDay ASC, endDay DESC, title ASC";
    private static final String DISPLAY_AS_ALLDAY = "dispAllday";

    private static final String EVENTS_WHERE = DISPLAY_AS_ALLDAY + "=0";
    private static final String ALLDAY_WHERE = DISPLAY_AS_ALLDAY + "=1";

    // The projection to use when querying instances to build a list of events
    public static final String[] EVENT_PROJECTION = new String[] {
            Instances.TITLE,                 // 0
            Instances.EVENT_LOCATION,        // 1
            Instances.ALL_DAY,               // 2
            Instances.CALENDAR_COLOR,        // 3
            Instances.EVENT_TIMEZONE,        // 4
            Instances.EVENT_ID,              // 5
            Instances.BEGIN,                 // 6
            Instances.END,                   // 7
            Instances._ID,                   // 8
            Instances.START_DAY,             // 9
            Instances.END_DAY,               // 10
            Instances.START_MINUTE,          // 11
            Instances.END_MINUTE,            // 12
            Instances.HAS_ALARM,             // 13
            Instances.RRULE,                 // 14
            Instances.RDATE,                 // 15
            Instances.SELF_ATTENDEE_STATUS,  // 16
            Events.ORGANIZER,                // 17
            Events.GUESTS_CAN_MODIFY,        // 18
            Instances.ALL_DAY + "=1 OR (" + Instances.END + "-" + Instances.BEGIN + ")>="
                    + DateUtils.DAY_IN_MILLIS + " AS " + DISPLAY_AS_ALLDAY, // 19
    };

    // The indices for the projection array above.
    private static final int PROJECTION_TITLE_INDEX = 0;
    private static final int PROJECTION_LOCATION_INDEX = 1;
    private static final int PROJECTION_ALL_DAY_INDEX = 2;
    private static final int PROJECTION_COLOR_INDEX = 3;
    private static final int PROJECTION_TIMEZONE_INDEX = 4;
    private static final int PROJECTION_EVENT_ID_INDEX = 5;
    private static final int PROJECTION_BEGIN_INDEX = 6;
    private static final int PROJECTION_END_INDEX = 7;
    private static final int PROJECTION_START_DAY_INDEX = 9;
    private static final int PROJECTION_END_DAY_INDEX = 10;
    private static final int PROJECTION_START_MINUTE_INDEX = 11;
    private static final int PROJECTION_END_MINUTE_INDEX = 12;
    private static final int PROJECTION_HAS_ALARM_INDEX = 13;
    private static final int PROJECTION_RRULE_INDEX = 14;
    private static final int PROJECTION_RDATE_INDEX = 15;
    private static final int PROJECTION_SELF_ATTENDEE_STATUS_INDEX = 16;
    private static final int PROJECTION_ORGANIZER_INDEX = 17;
    private static final int PROJECTION_GUESTS_CAN_INVITE_OTHERS_INDEX = 18;
    private static final int PROJECTION_DISPLAY_AS_ALLDAY = 19;

    private static String mNoTitleString;
    private static int mNoColorColor;

    public long id;
    public int color;
    public CharSequence title;
    public CharSequence location;
    public boolean allDay;
    public String organizer;
    public boolean guestsCanModify;

    public int startDay;       // start Julian day
    public int endDay;         // end Julian day
    public int startTime;      // Start and end time are in minutes since midnight
    public int endTime;

    public long startMillis;   // UTC milliseconds since the epoch
    public long endMillis;     // UTC milliseconds since the epoch
    private int mColumn;
    private int mMaxColumns;

    public boolean hasAlarm;
    public boolean isRepeating;

    public int selfAttendeeStatus;

    // The coordinates of the event rectangle drawn on the screen.
    public float left;
    public float right;
    public float top;
    public float bottom;

    // These 4 fields are used for navigating among events within the selected
    // hour in the Day and Week view.
    public Event nextRight;
    public Event nextLeft;
    public Event nextUp;
    public Event nextDown;

    @Override
    public final Object clone() throws CloneNotSupportedException {
        super.clone();
        Event e = new Event();

        e.title = title;
        e.color = color;
        e.location = location;
        e.allDay = allDay;
        e.startDay = startDay;
        e.endDay = endDay;
        e.startTime = startTime;
        e.endTime = endTime;
        e.startMillis = startMillis;
        e.endMillis = endMillis;
        e.hasAlarm = hasAlarm;
        e.isRepeating = isRepeating;
        e.selfAttendeeStatus = selfAttendeeStatus;
        e.organizer = organizer;
        e.guestsCanModify = guestsCanModify;

        return e;
    }

    public final void copyTo(Event dest) {
        dest.id = id;
        dest.title = title;
        dest.color = color;
        dest.location = location;
        dest.allDay = allDay;
        dest.startDay = startDay;
        dest.endDay = endDay;
        dest.startTime = startTime;
        dest.endTime = endTime;
        dest.startMillis = startMillis;
        dest.endMillis = endMillis;
        dest.hasAlarm = hasAlarm;
        dest.isRepeating = isRepeating;
        dest.selfAttendeeStatus = selfAttendeeStatus;
        dest.organizer = organizer;
        dest.guestsCanModify = guestsCanModify;
    }

    public static final Event newInstance() {
        Event e = new Event();

        e.id = 0;
        e.title = null;
        e.color = 0;
        e.location = null;
        e.allDay = false;
        e.startDay = 0;
        e.endDay = 0;
        e.startTime = 0;
        e.endTime = 0;
        e.startMillis = 0;
        e.endMillis = 0;
        e.hasAlarm = false;
        e.isRepeating = false;
        e.selfAttendeeStatus = Attendees.ATTENDEE_STATUS_NONE;

        return e;
    }

    /**
     * Loads <i>days</i> days worth of instances starting at <i>startDay</i>.
     */
    public static void loadEvents(Context context, ArrayList<Event> events, int startDay, int days,
    		int requestId, AtomicInteger sequenceNumber) {

    	int endDay = startDay + days - 1;

    	// Principe de la requête : pour chaque domaine, avec les comptes configurés
    	for( CrmCalendarInput ci : CrmCalendarManager.inputsList(context) ) {
    		List<CrmEventModel> models ;
    		
    		CrmCalendarManager lCalManager = new CrmCalendarManager(context,ci.mCrmInputId) ;
    		if( lCalManager.getCalendarInfos().mAccountIsOn ) {
    			models = lCalManager.queryModels(startDay,endDay,PrefsCrm.getSubscribedAccounts(context, ci.mCrmAgendaId)) ;
    		}
    		else{
    			models = lCalManager.queryModels(startDay,endDay) ;
    		}
    		Map<String,Integer> accountsColor = PrefsCrm.getAccountsColor(context, lCalManager.getCalendarInfos().mCrmAgendaFilecode ) ;
    		
    		
    		buildEventsFromModelArray( events, lCalManager, models, accountsColor, context, startDay, endDay ) ;
    	}
        
    }
    
    public static void buildEventsFromModelArray(
            ArrayList<Event> events, CrmCalendarManager calManager, List<CrmEventModel> models, Map<String,Integer> accountsColor, Context context, int startDay, int endDay) {
        if (models == null || events == null) {
            Log.e(TAG, "buildEventsFromCursor: null cursor or null events list!");
            return;
        }

        int count = models.size();

        if (count == 0) {
            return;
        }

        Resources res = context.getResources();
        mNoTitleString = "(No title)";
        mNoColorColor = res.getColor(R.color.event_center);
        // Sort events in two passes so we ensure the allday and standard events
        // get sorted in the correct order
        for( CrmEventModel model : models ) {
            Event e = generateEventFromModel(calManager,accountsColor,model);
            if (e.startDay > endDay || e.endDay < startDay) {
                continue;
            }
            events.add(e);
        }
    }
    
    private static Event generateEventFromModel( CrmCalendarManager calManager, Map<String,Integer> accountsColor, CrmEventModel model ){
    	Event e = new Event();
    	
    	e.id = model.mCalendarId ;
    	
    	Time currentTime = new Time();
    	currentTime.setToNow() ;
    	
    	Time startTime = new Time() ;
    	startTime.set(model.mStart) ;
    	e.startMillis = model.mStart ;
    	e.startTime = startTime.hour * 60 + startTime.minute ;
    	e.startDay = Time.getJulianDay(e.startMillis, currentTime.gmtoff) ;
    	
    	
    	Time endTime = new Time() ;
    	endTime.set(model.mEnd) ;
    	e.endMillis = model.mEnd ;
    	e.endTime = endTime.hour * 60 + endTime.minute ;
    	e.endDay = Time.getJulianDay(e.endMillis, currentTime.gmtoff) ;
    	
    	
    	e.allDay = model.mAllDay ;
    	
    	e.title = "Title "+e.id ;
    	e.location = Utils.implodeArray(model.mCrmTitle, System.getProperty("line.separator")) ;
    	
    	e.organizer = "" ;
    	e.guestsCanModify = false ;
    	e.isRepeating = false ;
    	e.hasAlarm = false ;
    	e.selfAttendeeStatus = 0 ;
    	
		if( calManager.getCalendarInfos().mAccountIsOn ) {
			e.color = accountsColor.get(model.mAccountEntry.entryKey) ;
		}
		else if( accountsColor.size() > 0 ) {
			e.color = accountsColor.get("&") ;
		}
		if( e.color == 0 ) {
			e.color = Color.WHITE ;
		}
    	
    	
    	return e ;
    }
    
    

    /**
     * @param cEvents Cursor pointing at event
     * @return An event created from the cursor
     */
    private static Event generateEventFromCursor(Cursor cEvents) {
        Event e = new Event();

        e.id = cEvents.getLong(PROJECTION_EVENT_ID_INDEX);
        e.title = cEvents.getString(PROJECTION_TITLE_INDEX);
        e.location = cEvents.getString(PROJECTION_LOCATION_INDEX);
        e.allDay = cEvents.getInt(PROJECTION_ALL_DAY_INDEX) != 0;
        e.organizer = cEvents.getString(PROJECTION_ORGANIZER_INDEX);
        e.guestsCanModify = cEvents.getInt(PROJECTION_GUESTS_CAN_INVITE_OTHERS_INDEX) != 0;

        if (e.title == null || e.title.length() == 0) {
            e.title = mNoTitleString;
        }

        if (!cEvents.isNull(PROJECTION_COLOR_INDEX)) {
            // Read the color from the database
            e.color = Utils.getDisplayColorFromColor(cEvents.getInt(PROJECTION_COLOR_INDEX));
        } else {
            e.color = mNoColorColor;
        }

        long eStart = cEvents.getLong(PROJECTION_BEGIN_INDEX);
        long eEnd = cEvents.getLong(PROJECTION_END_INDEX);

        e.startMillis = eStart;
        e.startTime = cEvents.getInt(PROJECTION_START_MINUTE_INDEX);
        e.startDay = cEvents.getInt(PROJECTION_START_DAY_INDEX);

        e.endMillis = eEnd;
        e.endTime = cEvents.getInt(PROJECTION_END_MINUTE_INDEX);
        e.endDay = cEvents.getInt(PROJECTION_END_DAY_INDEX);

        e.hasAlarm = cEvents.getInt(PROJECTION_HAS_ALARM_INDEX) != 0;

        // Check if this is a repeating event
        String rrule = cEvents.getString(PROJECTION_RRULE_INDEX);
        String rdate = cEvents.getString(PROJECTION_RDATE_INDEX);
        if (!TextUtils.isEmpty(rrule) || !TextUtils.isEmpty(rdate)) {
            e.isRepeating = true;
        } else {
            e.isRepeating = false;
        }

        e.selfAttendeeStatus = cEvents.getInt(PROJECTION_SELF_ATTENDEE_STATUS_INDEX);
        return e;
    }

    /**
     * Computes a position for each event.  Each event is displayed
     * as a non-overlapping rectangle.  For normal events, these rectangles
     * are displayed in separate columns in the week view and day view.  For
     * all-day events, these rectangles are displayed in separate rows along
     * the top.  In both cases, each event is assigned two numbers: N, and
     * Max, that specify that this event is the Nth event of Max number of
     * events that are displayed in a group. The width and position of each
     * rectangle depend on the maximum number of rectangles that occur at
     * the same time.
     *
     * @param eventsList the list of events, sorted into increasing time order
     * @param minimumDurationMillis minimum duration acceptable as cell height of each event
     * rectangle in millisecond. Should be 0 when it is not determined.
     */
    /* package */ static void computePositions(ArrayList<Event> eventsList,
            long minimumDurationMillis) {
        if (eventsList == null) {
            return;
        }

        // Compute the column positions separately for the all-day events
        doComputePositions(eventsList, minimumDurationMillis, false);
        doComputePositions(eventsList, minimumDurationMillis, true);
    }

    private static void doComputePositions(ArrayList<Event> eventsList,
            long minimumDurationMillis, boolean doAlldayEvents) {
        final ArrayList<Event> activeList = new ArrayList<Event>();
        final ArrayList<Event> groupList = new ArrayList<Event>();

        if (minimumDurationMillis < 0) {
            minimumDurationMillis = 0;
        }

        long colMask = 0;
        int maxCols = 0;
        for (Event event : eventsList) {
            // Process all-day events separately
            if (event.drawAsAllday() != doAlldayEvents)
                continue;

           if (!doAlldayEvents) {
                colMask = removeNonAlldayActiveEvents(
                        event, activeList.iterator(), minimumDurationMillis, colMask);
            } else {
                colMask = removeAlldayActiveEvents(event, activeList.iterator(), colMask);
            }

            // If the active list is empty, then reset the max columns, clear
            // the column bit mask, and empty the groupList.
            if (activeList.isEmpty()) {
                for (Event ev : groupList) {
                    ev.setMaxColumns(maxCols);
                }
                maxCols = 0;
                colMask = 0;
                groupList.clear();
            }

            // Find the first empty column.  Empty columns are represented by
            // zero bits in the column mask "colMask".
            int col = findFirstZeroBit(colMask);
            if (col == 64)
                col = 63;
            colMask |= (1L << col);
            event.setColumn(col);
            activeList.add(event);
            groupList.add(event);
            int len = activeList.size();
            if (maxCols < len)
                maxCols = len;
        }
        for (Event ev : groupList) {
            ev.setMaxColumns(maxCols);
        }
    }

    private static long removeAlldayActiveEvents(Event event, Iterator<Event> iter, long colMask) {
        // Remove the inactive allday events. An event on the active list
        // becomes inactive when the end day is less than the current event's
        // start day.
        while (iter.hasNext()) {
            final Event active = iter.next();
            if (active.endDay < event.startDay) {
                colMask &= ~(1L << active.getColumn());
                iter.remove();
            }
        }
        return colMask;
    }

    private static long removeNonAlldayActiveEvents(
            Event event, Iterator<Event> iter, long minDurationMillis, long colMask) {
        long start = event.getStartMillis();
        // Remove the inactive events. An event on the active list
        // becomes inactive when its end time is less than or equal to
        // the current event's start time.
        while (iter.hasNext()) {
            final Event active = iter.next();

            final long duration = Math.max(
                    active.getEndMillis() - active.getStartMillis(), minDurationMillis);
            if ((active.getStartMillis() + duration) <= start) {
                colMask &= ~(1L << active.getColumn());
                iter.remove();
            }
        }
        return colMask;
    }

    public static int findFirstZeroBit(long val) {
        for (int ii = 0; ii < 64; ++ii) {
            if ((val & (1L << ii)) == 0)
                return ii;
        }
        return 64;
    }

    public final void dump() {
        Log.e("Cal", "+-----------------------------------------+");
        Log.e("Cal", "+        id = " + id);
        Log.e("Cal", "+     color = " + color);
        Log.e("Cal", "+     title = " + title);
        Log.e("Cal", "+  location = " + location);
        Log.e("Cal", "+    allDay = " + allDay);
        Log.e("Cal", "+  startDay = " + startDay);
        Log.e("Cal", "+    endDay = " + endDay);
        Log.e("Cal", "+ startTime = " + startTime);
        Log.e("Cal", "+   endTime = " + endTime);
        Log.e("Cal", "+ organizer = " + organizer);
        Log.e("Cal", "+  guestwrt = " + guestsCanModify);
    }

    public final boolean intersects(int julianDay, int startMinute,
            int endMinute) {
        if (endDay < julianDay) {
            return false;
        }

        if (startDay > julianDay) {
            return false;
        }

        if (endDay == julianDay) {
            if (endTime < startMinute) {
                return false;
            }
            // An event that ends at the start minute should not be considered
            // as intersecting the given time span, but don't exclude
            // zero-length (or very short) events.
            if (endTime == startMinute
                    && (startTime != endTime || startDay != endDay)) {
                return false;
            }
        }

        if (startDay == julianDay && startTime > endMinute) {
            return false;
        }

        return true;
    }

    /**
     * Returns the event title and location separated by a comma.  If the
     * location is already part of the title (at the end of the title), then
     * just the title is returned.
     *
     * @return the event title and location as a String
     */
    public String getTitleAndLocation() {
        String text = title.toString();

        // Append the location to the title, unless the title ends with the
        // location (for example, "meeting in building 42" ends with the
        // location).
        if (location != null) {
            String locationString = location.toString();
            if (!text.endsWith(locationString)) {
                text += ", " + locationString;
            }
        }
        return text;
    }

    public void setColumn(int column) {
        mColumn = column;
    }

    public int getColumn() {
    	//return 1 ; // @DAMS
        return mColumn;
    }

    public void setMaxColumns(int maxColumns) {
        mMaxColumns = maxColumns;
    }

    public int getMaxColumns() {
    	// return 1 ; // @DAMS
        return mMaxColumns; 
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public boolean drawAsAllday() {
        // Use >= so we'll pick up Exchange allday events
        return allDay || endMillis - startMillis >= DateUtils.DAY_IN_MILLIS;
    }
}
