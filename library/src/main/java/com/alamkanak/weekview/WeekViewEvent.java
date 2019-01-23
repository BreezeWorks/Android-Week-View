package com.alamkanak.weekview;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import java.util.Calendar;

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://april-shower.com
 */
public class WeekViewEvent {
    private long mId;
    private Calendar mStartTime;
    private Calendar mEndTime;
    private String mName;
    private SpannableString mSpannableName;
    private int mColor;

    // Breezeworks change
    private String mDescription; // for customer name
    private int mLighterColor; // since we show two-colored backgrounds
    private int mBarColor;
    private int mDarkerColor;
    private boolean mShouldExpand; // indicates whether event is a bar or expanded
    private long mEmployeeId;
    private String mNotes;
    private int mSliverColor;
    private boolean allDayEvent;
    private boolean calendarEvent;
    private boolean recurringEvent;

    private Integer driveTimeMin;

    public WeekViewEvent(){

    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startYear Year when the event starts.
     * @param startMonth Month when the event starts.
     * @param startDay Day when the event starts.
     * @param startHour Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear Year when the event ends.
     * @param endMonth Month when the event ends.
     * @param endDay Day when the event ends.
     * @param endHour Hour (in 24-hour format) when the event ends.
     * @param endMinute Minute when the event ends.
     */
    public WeekViewEvent(long id, String name, int startYear, int startMonth, int startDay, int startHour, int startMinute, int endYear, int endMonth, int endDay, int endHour, int endMinute) {
        this.mId = id;

        this.mStartTime = Calendar.getInstance();
        this.mStartTime.set(Calendar.YEAR, startYear);
        this.mStartTime.set(Calendar.MONTH, startMonth-1);
        this.mStartTime.set(Calendar.DAY_OF_MONTH, startDay);
        this.mStartTime.set(Calendar.HOUR_OF_DAY, startHour);
        this.mStartTime.set(Calendar.MINUTE, startMinute);

        this.mEndTime = Calendar.getInstance();
        this.mEndTime.set(Calendar.YEAR, endYear);
        this.mEndTime.set(Calendar.MONTH, endMonth-1);
        this.mEndTime.set(Calendar.DAY_OF_MONTH, endDay);
        this.mEndTime.set(Calendar.HOUR_OF_DAY, endHour);
        this.mEndTime.set(Calendar.MINUTE, endMinute);

        this.mName = name;
        mSliverColor = 0;
    }

    // Breezeworks change: to include customer name
    public WeekViewEvent(long id, String name, String description, Calendar startTime, Calendar endTime) {
        this(id, name, startTime, endTime);
        this.mDescription = description;
        mSliverColor = 0;
    }

    public WeekViewEvent(long id, SpannableString name, String description, Calendar startTime, Calendar endTime) {
        this(id, null, startTime, endTime);
        this.mSpannableName = name;
        this.mDescription = description;
        mSliverColor = 0;
    }

    /**
     * Initializes the event for week view.
     * @param id The id of the event.
     * @param name Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime The time when the event ends.
     */
    public WeekViewEvent(long id, String name, Calendar startTime, Calendar endTime) {
        this.mId = id;
        this.mName = name;
        this.mStartTime = startTime;
        this.mEndTime = endTime;
    }


    public Calendar getStartTime() {
        return mStartTime;
    }

    public void setStartTime(Calendar startTime) {
        this.mStartTime = startTime;
    }

    public Calendar getEndTime() {
        return mEndTime;
    }

    public void setEndTime(Calendar endTime) {
        this.mEndTime = endTime;
    }

    public SpannableString getSpannableName() {
        if (mSpannableName != null) {
            return mSpannableName;
        }
        return new SpannableString(mName);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    /********************
     * Breezeworks change
     ********************/

    public String getDescription() {
        return mDescription;
    }

    public int getLighterColor() {
        return mLighterColor;
    }

    public void setExpandedEventBackgroundColor(int mLighterColor) {
        this.mLighterColor = mLighterColor;
    }

    public int getBarColor() {
        return mBarColor;
    }

    public void setBarColor(int mBarColor) {
        this.mBarColor = mBarColor;
    }

    public int getDarkerColor() {
        return mDarkerColor;
    }

    public void setExpandedEventTextColor(int mDarkerColor) {
        this.mDarkerColor = mDarkerColor;
    }

    public boolean shouldExpand() {
        return mShouldExpand;
    }

    public void setShouldExpand(boolean mShouldExpand) {
        this.mShouldExpand = mShouldExpand;
    }

    public long getEmployeeId() {
        return mEmployeeId;
    }

    public void setEmployeeId(long mEmployeeId) {
        this.mEmployeeId = mEmployeeId;
    }

    public String getNotes() {
        return mNotes;
    }

    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    public int getSliverColor() {
        return mSliverColor;
    }

    public void setSliverColor(int mSliverColor) {
        this.mSliverColor = mSliverColor;
    }

    @Nullable
    public Integer getDriveTimeMin() {
        return driveTimeMin;
    }

    public void setDriveTimeMin(@Nullable Integer driveTimeMin) {
        this.driveTimeMin = driveTimeMin;
    }

    public boolean isAllDayEvent() {
        return allDayEvent;
    }

    public void setAllDayEvent(boolean allDayEvent) {
        this.allDayEvent = allDayEvent;
    }

    public boolean isCalendarEvent() {
        return calendarEvent;
    }

    public void setCalendarEvent(boolean calendarEvent) {
        this.calendarEvent = calendarEvent;
    }

    public boolean isRecurringEvent() {
        return recurringEvent;
    }

    public void setRecurringEvent(boolean recurringEvent) {
        this.recurringEvent = recurringEvent;
    }
}
