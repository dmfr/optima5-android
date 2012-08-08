package za.dams.paracrm.calendar;

import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class EventInfoFragment extends DialogFragment {

	CrmEventModel mModel ;
	
    private void updateEvent(View view) {
        if (mModel == null || view == null) {
            return;
        }

        /*
        String eventName = mEventCursor.getString(EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = getActivity().getString(R.string.no_title_label);
        }

        mAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        String location = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
        String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        String eventTimezone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);

        mColor = Utils.getDisplayColorFromColor(mEventCursor.getInt(EVENT_INDEX_COLOR));
        mHeadlines.setBackgroundColor(mColor);

        // What
        if (eventName != null) {
            setTextCommon(view, R.id.title, eventName);
        }

        // When
        // Set the date and repeats (if any)
        String whenDate;
        int flagsTime = DateUtils.FORMAT_SHOW_TIME;
        int flagsDate = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY |
                DateUtils.FORMAT_SHOW_YEAR;

        if (DateFormat.is24HourFormat(getActivity())) {
            flagsTime |= DateUtils.FORMAT_24HOUR;
        }

        // Put repeat after the date (if any)
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Time date = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
            if (mAllDay) {
                date.timezone = Time.TIMEZONE_UTC;
            }
            date.set(mStartMillis);
            eventRecurrence.setStartDate(date);
            repeatString = EventRecurrenceFormatter.getRepeatString(
                    getActivity().getResources(), eventRecurrence);
        }
        // If an all day event , show the date without the time
        if (mAllDay) {
            Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
            whenDate = DateUtils.formatDateRange(getActivity(), f, mStartMillis, mEndMillis,
                    flagsDate, Time.TIMEZONE_UTC).toString();
            if (repeatString != null) {
                setTextCommon(view, R.id.when_date, whenDate + " (" + repeatString + ")");
            } else {
                setTextCommon(view, R.id.when_date, whenDate);
            }
            view.findViewById(R.id.when_time).setVisibility(View.GONE);

        } else {
            // Show date for none all-day events
            whenDate = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis, flagsDate);
            String whenTime = Utils.formatDateRange(getActivity(), mStartMillis, mEndMillis,
                    flagsTime);
            if (repeatString != null) {
                setTextCommon(view, R.id.when_date, whenDate + " (" + repeatString + ")");
            } else {
                setTextCommon(view, R.id.when_date, whenDate);
            }

            // Show the event timezone if it is different from the local timezone after the time
            String localTimezone = Utils.getTimeZone(mActivity, mTZUpdater);
            if (!TextUtils.equals(localTimezone, eventTimezone)) {
                String displayName;
                // Figure out if this is in DST
                Time date = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
                if (mAllDay) {
                    date.timezone = Time.TIMEZONE_UTC;
                }
                date.set(mStartMillis);

                TimeZone tz = TimeZone.getTimeZone(localTimezone);
                if (tz == null || tz.getID().equals("GMT")) {
                    displayName = localTimezone;
                } else {
                    displayName = tz.getDisplayName(date.isDst != 0, TimeZone.LONG);
                }
                setTextCommon(view, R.id.when_time, whenTime + " (" + displayName + ")");
            }
            else {
                setTextCommon(view, R.id.when_time, whenTime);
            }
        }


        // Organizer view is setup in the updateCalendar method


        // Where
        if (location == null || location.trim().length() == 0) {
            setVisibilityCommon(view, R.id.where, View.GONE);
        } else {
            final TextView textView = mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                textView.setText(location.trim());
                linkifyTextView(textView);

                textView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            return v.onTouchEvent(event);
                        } catch (ActivityNotFoundException e) {
                            // ignore
                            return true;
                        }
                    }
                });
            }
        }

        // Description
        if (description != null && description.length() != 0) {
            mDesc.setText(description);
        }
        */
    }

}
