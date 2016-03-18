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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import za.dams.paracrm.R;
import za.dams.paracrm.calendar.CrmCalendarManager.CrmCalendarInput;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

// TODO: should Event be Parcelable so it can be passed via Intents?
public class Event implements Cloneable {

	private static final boolean DEBUG = false ;
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



    private static String mNoTitleString;
    private static int mNoColorColor;

    public long id;
    public int color;
    public boolean colorFixed ;
    public CharSequence title;
    public CharSequence location;
    public boolean allDay;
    public String organizer;
    public boolean guestsCanModify;

    public int startDay;       // start Julian day
    public int endDay;         // end Julian day
    public int startTime;      // Start and end time are in minutes since midnight
    public int endTime;
    
    public boolean isDone ;

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
        e.colorFixed = colorFixed ;
        e.location = location;
        e.allDay = allDay;
        e.startDay = startDay;
        e.endDay = endDay;
        e.startTime = startTime;
        e.endTime = endTime;
        e.startMillis = startMillis;
        e.endMillis = endMillis;
        e.isDone = isDone;
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
        dest.colorFixed = colorFixed ;
        dest.location = location;
        dest.allDay = allDay;
        dest.startDay = startDay;
        dest.endDay = endDay;
        dest.startTime = startTime;
        dest.endTime = endTime;
        dest.startMillis = startMillis;
        dest.endMillis = endMillis;
        dest.isDone = isDone;
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
        e.colorFixed = false ;
        e.location = null;
        e.allDay = false;
        e.startDay = 0;
        e.endDay = 0;
        e.startTime = 0;
        e.endTime = 0;
        e.startMillis = 0;
        e.endMillis = 0;
        e.isDone = false;
        e.hasAlarm = false;
        e.isRepeating = false;
        e.selfAttendeeStatus = 0 ;

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
    		else if( PrefsCrm.isCalendarEnabled(context, ci.mCrmAgendaId) ) {
    			models = lCalManager.queryModels(startDay,endDay) ;
    		}
    		else {
    			models = new ArrayList<CrmEventModel>() ;
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
    	e.isDone = model.isDone ;
    	
    	if( DEBUG ) {
    		e.title = "Title "+e.id ;
    	} else {
    		e.title = "" ;
    	}
    	e.location = Utils.implodeArray(model.mCrmTitle, System.getProperty("line.separator")) ;
    	
    	e.organizer = "" ;
    	e.guestsCanModify = false ;
    	e.isRepeating = false ;
    	e.hasAlarm = false ;
    	e.selfAttendeeStatus = 0 ;
    	
    	if( model.hasFixedColor ) {
    		//Log.w(TAG,"Fixed color "+model.mFixedColor);
    		e.color = model.mFixedColor ;
    		e.colorFixed = true ;
    	}
    	else if( calManager.getCalendarInfos().mAccountIsOn ) {
			e.color = accountsColor.get(model.mAccountEntry.entryKey) ;
		}
		else if( accountsColor.size() > 0 ) {
			e.color = accountsColor.get("&") ;
		}
		if( e.color == 0 ) {
			e.color = Color.GRAY ;
		}
    	
    	
    	return e ;
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
