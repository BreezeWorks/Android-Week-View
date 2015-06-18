package com.alamkanak.weekview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;
import android.widget.Scroller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Latest commit manually merged: 9bfd96b52a5714e2bd02c8eafce56ae69d905408
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://alamkanak.me
 */
public class WeekView extends View {

    // Breezework changes
    private static final int EMPTY_VIEW_VERTICAL_PADDING = 24;
    private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int EVENT_ORIGINAL_COLOR_WIDTH = 4;
    private static final int EVENT_BAR_WIDTH = 30;
    private static final int EVENT_WHITE_TOP_BORDER_HEIGHT = 1;
    private static final int MIN_SCROLL_DIFFERENCE = 10; //arbitrary
    private static final float MIN_EVENT_WIDTH_PERCENTAGE = 0.08f;
    private static final int MIN_EVENT_HEADER_WIDTH = 90;
    private static final String TIME_FORMAT = "0p";
    private Map<Long, Float> barredEmployeeByLeftPositionMap = new HashMap<>();
    private Set<Long> uniqueEmployeeIdsOfBarredEvents = new HashSet<>();
    private Integer finalXWhenScrollingToDate; //only useful when using goToDate

    @Deprecated
    public static final int LENGTH_SHORT = 1;
    @Deprecated
    public static final int LENGTH_LONG = 2;
    private final Context mContext;
    private Calendar mToday;
    private Calendar mStartDate;
    private Paint mTimeTextPaint;
    private float mTimeTextWidth;
    private float mTimeTextHeight;
    private Paint mHeaderTextPaint;
    private float mHeaderTextHeight;
    private float dayHeaderHeight;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private PointF mCurrentOrigin = new PointF(0f, 0f);
    private Direction mCurrentScrollDirection = Direction.NONE;
    private Paint mHeaderBackgroundPaint;
    private TextPaint mEventHeaderTextPaint;
    private float mWidthPerDay;
    private Paint mDayBackgroundPaint;
    private Paint mHourSeparatorPaint;
    private float mHeaderMarginBottom;
    private Paint mTodayBackgroundPaint;
    private Paint mTodayHeaderTextPaint;
    private TextPaint emptyViewTitleTextPaint;
    private TextPaint emptyViewSubtitleTextPaint;
    private Paint mEventBackgroundPaint;
    private float mHeaderColumnWidth;
    private List<EventRect> mEventRects;
    private TextPaint mEventTextPaint;
    private Paint mHeaderColumnBackgroundPaint;
    private Scroller mStickyScroller;
    private int mFetchedMonths[] = new int[3];
    private boolean mRefreshEvents = false;
    private float mDistanceY = 0;
    private float mDistanceX = 0;
    private Direction mCurrentFlingDirection = Direction.NONE;
    private Paint mEventHeaderBackgroundPaint;

    // Attributes and their default values.
    private int mHourHeight = 50;
    private int mColumnGap = 10;
    private int mFirstDayOfWeek = Calendar.MONDAY;
    private int mTextSize = 12;
    private int mHeaderColumnPadding = 10;
    private int mHeaderColumnTextColor;
    private int mNumberOfVisibleDays = 3;
    private int mHeaderRowPadding = 10;
    private int mHeaderRowBackgroundColor = Color.WHITE;
    private int mDayBackgroundColor = Color.rgb(245, 245, 245);
    private int mHourSeparatorColor = Color.rgb(230, 230, 230);
    private int mTodayBackgroundColor = Color.rgb(239, 247, 254);
    private int mHourSeparatorHeight = 2;
    private int mTodayHeaderTextColor = Color.rgb(39, 137, 228);
    private int mEventTextSize = 14;
    private int mEventTextColor = Color.BLACK;
    private int mEventPadding = 8;
    private int mHeaderColumnBackgroundColor = Color.WHITE;
    private int mDefaultEventColor;
    private boolean mIsFirstDraw = true;
    private boolean mAreDimensionsInvalid = true;
    @Deprecated
    private int mDayNameLength = LENGTH_LONG;
    private int mOverlappingEventGap = 0;
    private int mEventMarginVertical = 0;
    private float mXScrollingSpeed = 1f;
    private Calendar mFirstVisibleDay;
    private Calendar mLastVisibleDay;
    private Calendar mScrollToDay = null;
    private double mScrollToHour = -1;

    // Listeners.
    private EventClickListener mEventClickListener;
    private EventLongPressListener mEventLongPressListener;
    private MonthChangeListener mMonthChangeListener;
    private EmptyViewClickListener mEmptyViewClickListener;
    private EmptyViewLongPressListener mEmptyViewLongPressListener;
    private DateTimeInterpreter mDateTimeInterpreter;
    private ScrollListener mScrollListener;
    private EventListener mEventListener;

    // CurrentTime color
    private int mNowLineColor = Color.rgb(102, 102, 102);
    private int mNowLineThickness = 5;
    private boolean displayCurrentTimeLine = false;
    private Paint mCurrentTimeLinePaint;

    // Long Pressed New events
    private float longPressX;
    private float longPressY;
    private TextPaint newLongPressedEventHeaderTextPaint;
    private TextPaint newLongPressedEventTitleTextPaint;
    private Paint transparentPaint;
    private boolean isUserLongPressing = false;
    private Handler mHandler = new Handler();
    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            isUserLongPressing = true;
            ViewCompat.postInvalidateOnAnimation(WeekView.this);
        }
    };

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            mScroller.forceFinished(true);
            mStickyScroller.forceFinished(true);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mCurrentScrollDirection == Direction.NONE) {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    mCurrentScrollDirection = Direction.HORIZONTAL;
                    mCurrentFlingDirection = Direction.HORIZONTAL;
                    isUserLongPressing = false;
                } else {
                    mCurrentFlingDirection = Direction.VERTICAL;
                    mCurrentScrollDirection = Direction.VERTICAL;
                }
            }
            mDistanceX = distanceX;
            mDistanceY = distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mScroller.forceFinished(true);
//            mStickyScroller.forceFinished(true);

            if (mCurrentFlingDirection == Direction.HORIZONTAL) {
                mScroller.fling((int) mCurrentOrigin.x, 0, (int) (velocityX * mXScrollingSpeed), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
            } else if (mCurrentFlingDirection == Direction.VERTICAL) {
                mScroller.fling(0, (int) mCurrentOrigin.y, 0, (int) velocityY, 0, 0, (int) -(mHourHeight * 24 + dayHeaderHeight - getHeight()), 0);
            }

            ViewCompat.postInvalidateOnAnimation(WeekView.this);
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // If the tap was on an event then trigger the callback.
            if (mEventRects != null && mEventClickListener != null) {
                List<EventRect> reversedEventRects = mEventRects;
                Collections.reverse(reversedEventRects);
                for (EventRect event : reversedEventRects) {
                    if (event.rectF != null && e.getX() > event.rectF.left && e.getX() < event.rectF.right && e.getY() > event.rectF.top && e.getY() < event.rectF.bottom) {
                        mEventClickListener.onEventClick(event.originalEvent, event.rectF);
                        playSoundEffect(SoundEffectConstants.CLICK);
                        return super.onSingleTapConfirmed(e);
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (mEmptyViewClickListener != null && e.getX() > mHeaderColumnWidth && e.getY() > (dayHeaderHeight + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());
                if (selectedTime != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    mEmptyViewClickListener.onEmptyViewClicked(selectedTime);
                }
            }

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (mEventLongPressListener != null && mEventRects != null) {
                List<EventRect> reversedEventRects = mEventRects;
                Collections.reverse(reversedEventRects);
                for (EventRect event : reversedEventRects) {
                    if (event.rectF != null && e.getX() > event.rectF.left && e.getX() < event.rectF.right && e.getY() > event.rectF.top && e.getY() < event.rectF.bottom) {
                        mEventLongPressListener.onEventLongPress(event.originalEvent, event.rectF);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (mEmptyViewLongPressListener != null && e.getX() > mHeaderColumnWidth && e.getY() > (dayHeaderHeight + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());
                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    mEmptyViewLongPressListener.onEmptyViewLongPress(selectedTime);
                }
            }
        }


    };

    private enum Direction {
        NONE, HORIZONTAL, VERTICAL
    }

    public WeekView(Context context) {
        this(context, null);
    }

    public WeekView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeekView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Hold references.
        mContext = context;

        // Get the attribute values (if any).
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0);
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek);
            mHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, mHourHeight);
            mTextSize = a.getDimensionPixelSize(R.styleable.WeekView_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize, context.getResources().getDisplayMetrics()));
            mHeaderColumnPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding);
            mColumnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, mColumnGap);
            mHeaderColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor);
            mNumberOfVisibleDays = a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays);
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, mHeaderRowPadding);
            mHeaderRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor);
            mDayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor);
            mHourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor);
            mTodayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor);
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight);
            mTodayHeaderTextColor = a.getColor(R.styleable.WeekView_todayHeaderTextColor, mTodayHeaderTextColor);
            mEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize, context.getResources().getDisplayMetrics()));
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor);
            mEventPadding = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mEventPadding);
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor);
            mDayNameLength = a.getInteger(R.styleable.WeekView_dayNameLength, mDayNameLength);
            mOverlappingEventGap = a.getDimensionPixelSize(R.styleable.WeekView_overlappingEventGap, mOverlappingEventGap);
            mEventMarginVertical = a.getDimensionPixelSize(R.styleable.WeekView_eventMarginVertical, mEventMarginVertical);
            mXScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, mXScrollingSpeed);
            mNowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, mNowLineColor);
            mNowLineThickness = a.getDimensionPixelSize(R.styleable.WeekView_nowLineThickness, mNowLineThickness);
            displayCurrentTimeLine = a.getBoolean(R.styleable.WeekView_displayCurrentTimeLine, false);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        // Get the date today.
        mToday = today();

        // Scrolling initialization.
        mGestureDetector = new GestureDetectorCompat(mContext, mGestureListener);
        mScroller = new OverScroller(mContext);
        mStickyScroller = new Scroller(mContext);

        // Prepare header column text color
        mHeaderColumnTextColor = getResources().getColor(R.color.breeze_gray_one);

        // Measure settings for time column.
        mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTimeTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setColor(mHeaderColumnTextColor);
        Rect rect = new Rect();
        mTimeTextPaint.getTextBounds(TIME_FORMAT, 0, TIME_FORMAT.length(), rect);
        mTimeTextWidth = mTimeTextPaint.measureText(TIME_FORMAT);
        mTimeTextHeight = rect.height();
        mHeaderMarginBottom = mTimeTextHeight / 2;

        // Measure settings for header row.
        mHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHeaderTextPaint.setColor(mHeaderColumnTextColor);
        mHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mHeaderTextPaint.setTextSize(mTextSize);
        mHeaderTextPaint.getTextBounds(TIME_FORMAT, 0, TIME_FORMAT.length(), rect);
        mHeaderTextHeight = rect.height();
        mHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Prepare header background paint.
        mHeaderBackgroundPaint = new Paint();
        mHeaderBackgroundPaint.setColor(mHeaderRowBackgroundColor);

        // Prepare day background color paint.
        mDayBackgroundPaint = new Paint();
        mDayBackgroundPaint.setColor(mDayBackgroundColor);

        // Prepare hour separator color paint.
        mHourSeparatorPaint = new Paint();
        mHourSeparatorPaint.setStyle(Paint.Style.STROKE);
        mHourSeparatorPaint.setStrokeWidth(mHourSeparatorHeight);
        mHourSeparatorPaint.setColor(mHourSeparatorColor);

        // Prepare today background color paint.
        mTodayBackgroundPaint = new Paint();
        mTodayBackgroundPaint.setColor(mTodayBackgroundColor);

        // Prepare today header text color paint.
        mTodayHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTodayHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mTodayHeaderTextPaint.setTextSize(mTextSize);
        mTodayHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTodayHeaderTextPaint.setColor(mTodayHeaderTextColor);

        // Prepare event background color.
        mEventBackgroundPaint = new Paint();
        mEventBackgroundPaint.setColor(Color.rgb(174, 208, 238));

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint = new Paint();
        mHeaderColumnBackgroundPaint.setColor(mHeaderColumnBackgroundColor);

        // Prepare event text size and color.
        mEventTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        mEventTextPaint.setStyle(Paint.Style.FILL);
        mEventTextPaint.setColor(mEventTextColor);
        mEventTextPaint.setTextSize(mEventTextSize);

        mStartDate = (Calendar) mToday.clone();

        // Prepare currentTimeLine
        // Prepare the "now" line color paint
        mCurrentTimeLinePaint = new Paint();
        mCurrentTimeLinePaint.setStrokeWidth(mNowLineThickness);
        mCurrentTimeLinePaint.setColor(mNowLineColor);

        // Set default event color.
        mDefaultEventColor = Color.parseColor("#9fc6e7");

        // Prepare new event header text size and color
        newLongPressedEventHeaderTextPaint = new TextPaint(mEventTextPaint);
        newLongPressedEventHeaderTextPaint.setColor(mEventBackgroundPaint.getColor());

        // Prepare new event title text size and color
        newLongPressedEventTitleTextPaint = new TextPaint(mEventTextPaint);
        newLongPressedEventTitleTextPaint.setColor(Color.WHITE);

        // Prepare transparent paint
        transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);

        // Prepare empty view title text
        emptyViewTitleTextPaint = new TextPaint(mEventTextPaint);
        emptyViewTitleTextPaint.setColor(getResources().getColor(R.color.breeze_red));
        emptyViewTitleTextPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.empty_view_text_size));
        emptyViewTitleTextPaint.setFakeBoldText(true);

        // Prepare empty view subtitle text
        emptyViewSubtitleTextPaint = new TextPaint(emptyViewTitleTextPaint);
        emptyViewSubtitleTextPaint.setColor(getResources().getColor(R.color.breeze_gray_one));
        emptyViewSubtitleTextPaint.setFakeBoldText(false);

        // Prepare header
        mEventHeaderBackgroundPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mEventHeaderTextPaint = new TextPaint(mEventTextPaint);
        mEventHeaderTextPaint.setColor(getResources().getColor(android.R.color.black));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the header row.
        drawHeaderRowAndEvents(canvas);

        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas);

        // Hide everything in the first cell (top left corner).
        canvas.drawRect(0, 0, mTimeTextWidth + mHeaderColumnPadding * 2, dayHeaderHeight + mHeaderMarginBottom, mHeaderBackgroundPaint);

        // Hide anything that is in the bottom margin of the header row.
        canvas.drawRect(mHeaderColumnWidth, dayHeaderHeight, getWidth(), dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - mHourSeparatorHeight / 2, mHeaderColumnBackgroundPaint);

        drawNewLongPressedEvent(canvas);
    }

    private void drawTimeColumnAndAxes(Canvas canvas) {
        // Do not let the view go above/below the limit due to scrolling. Set the max and min limit of the scroll.
        if (mCurrentScrollDirection == Direction.VERTICAL) {
            if (mCurrentOrigin.y - mDistanceY > 0) mCurrentOrigin.y = 0;
            else if (mCurrentOrigin.y - mDistanceY < -(mHourHeight * 24 + dayHeaderHeight + mHeaderMarginBottom - getHeight()))
                mCurrentOrigin.y = -(mHourHeight * 24 + dayHeaderHeight + mHeaderMarginBottom - getHeight());
            else mCurrentOrigin.y -= mDistanceY;
        }

        // Draw the background color for the header column.
        canvas.drawRect(0, dayHeaderHeight, mHeaderColumnWidth, getHeight(), mHeaderColumnBackgroundPaint);

        for (int i = 0; i < 24; i++) {
            float top = dayHeaderHeight + mCurrentOrigin.y + mHourHeight * i + mHeaderMarginBottom;

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            String time = getDateTimeInterpreter().interpretTime(i);
            if (time == null)
                throw new IllegalStateException("A DateTimeInterpreter must not return null time");
            if (top < getHeight())
                canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding * 2, top + mTimeTextHeight, mTimeTextPaint);
        }
    }

    /**
     * @param canvas
     * @return true if there are events drawn for current visible day; false otherwise
     */
    private boolean drawHeaderRowAndEvents(Canvas canvas) {
        boolean drewEvents = false;

        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 3;
        mWidthPerDay = getWidth() - mHeaderColumnWidth - mColumnGap * (mNumberOfVisibleDays - 1);
        mWidthPerDay = mWidthPerDay / mNumberOfVisibleDays;

        if (mAreDimensionsInvalid) {
            mAreDimensionsInvalid = false;
            if(mScrollToDay != null)
                goToDate(mScrollToDay);

            mAreDimensionsInvalid = false;
            if(mScrollToHour >= 0)
                goToHour(mScrollToHour);

            mScrollToDay = null;
            mScrollToHour = -1;
            mAreDimensionsInvalid = false;
        }
        if (mIsFirstDraw){
            mIsFirstDraw = false;

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if(mNumberOfVisibleDays >= 7 && mToday.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {
                int difference = (7 + (mToday.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek)) % 7;
                mCurrentOrigin.x += (mWidthPerDay + mColumnGap) * difference;
            }
        }

        // Consider scroll offset.
        if (mCurrentScrollDirection == Direction.HORIZONTAL) mCurrentOrigin.x -= mDistanceX;
        int leftDaysWithGaps = (int) -(Math.ceil(mCurrentOrigin.x / (mWidthPerDay + mColumnGap))); //  off by one so not useful to trigger scroll listener
        float startFromPixel = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
                mHeaderColumnWidth;
        float startPixel = startFromPixel;

        // Prepare to iterate for each day.
        Calendar day = (Calendar) mToday.clone();
        day.add(Calendar.HOUR, 6);

        // Prepare to iterate for each hour to draw the hour lines.
        int lineCount = (int) ((getHeight() - dayHeaderHeight  - mHeaderMarginBottom) / mHourHeight) + 1;
        lineCount = (lineCount) * (mNumberOfVisibleDays + 1);
        float[] hourLines = new float[lineCount * 4];

        // Clear the cache for event rectangles.
        if (mEventRects != null) {
            for (EventRect eventRect : mEventRects) {
                eventRect.rectF = null;
            }
        }

        // Iterate through each day.
        Calendar oldFirstVisibleDay = mFirstVisibleDay;
        mFirstVisibleDay = (Calendar) mToday.clone();
        int leftDaysWithGapsForScroll = (mCurrentOrigin.x < 0 ? 1 : -1) * (int)Math.ceil(Math.abs(mCurrentOrigin.x) / (mWidthPerDay + mColumnGap));
        mFirstVisibleDay.add(Calendar.DATE, leftDaysWithGapsForScroll);
        if(!mFirstVisibleDay.equals(oldFirstVisibleDay) && mScrollListener != null && !isGoingToDate()){
            mScrollListener.onFirstVisibleDayChanged(mFirstVisibleDay, oldFirstVisibleDay);
        }
        for (int dayNumber = leftDaysWithGaps + 1; dayNumber <= leftDaysWithGaps + mNumberOfVisibleDays + 1; dayNumber++) {

            // Check if the day is today.
            day = (Calendar) mToday.clone();
            mLastVisibleDay = (Calendar) day.clone();
            day.add(Calendar.DATE, dayNumber - 1);
            mLastVisibleDay.add(Calendar.DATE, dayNumber - 2);
            boolean sameDay = isSameDay(day, mToday);

            // Get more events if necessary. We want to store the events 3 months beforehand. Get
            // events only when it is the first iteration of the loop.
            if (mEventRects == null || mRefreshEvents || (dayNumber == leftDaysWithGaps + 1 && mFetchedMonths[1] != day.get(Calendar.MONTH) + 1 && day.get(Calendar.DAY_OF_MONTH) == 15)) {
                getMoreEvents(day);
                mRefreshEvents = false;
            }

            // Draw background color for each day.
            float start = (startPixel < mHeaderColumnWidth ? mHeaderColumnWidth : startPixel);
            if (mWidthPerDay + startPixel - start > 0)
                canvas.drawRect(start, dayHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel + mWidthPerDay, getHeight(), sameDay ? mTodayBackgroundPaint : mDayBackgroundPaint);

            drewEvents = drawEvents(day, startPixel, canvas);

            // Draw the lines for hours.
            // Prepare the separator lines for hours.
            int i = 0;
            for (int hourNumber = 0; hourNumber < 24; hourNumber++) {
                float top = dayHeaderHeight + mCurrentOrigin.y + mHourHeight * hourNumber + mTimeTextHeight / 2 + mHeaderMarginBottom;
                if (top > dayHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom - mHourSeparatorHeight && top < getHeight() && startPixel + mWidthPerDay - start > 0) {
                    hourLines[i * 4] = start;
                    hourLines[i * 4 + 1] = top;
                    hourLines[i * 4 + 2] = startPixel + mWidthPerDay;
                    hourLines[i * 4 + 3] = top;
                    i++;
                }
            }
            canvas.drawLines(hourLines, mHourSeparatorPaint);

            if (!drewEvents) {
                drawEmptyViewText(canvas, startPixel);
            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap;
        }

        // Draw the header background.
        canvas.drawRect(0, 0, getWidth(), dayHeaderHeight + mHeaderMarginBottom, mHeaderBackgroundPaint);

        // Draw the header row texts.
        startPixel = startFromPixel;
        for (int dayNumber = leftDaysWithGaps + 1; dayNumber <= leftDaysWithGaps + mNumberOfVisibleDays + 1; dayNumber++) {
            // Check if the day is today.
            day = (Calendar) mToday.clone();
            day.add(Calendar.DATE, dayNumber - 1);
            boolean sameDay = isSameDay(day, mToday);

            if (dayHeaderHeight > 0) {
                // Draw the day labels.
                String dayLabel = getDateTimeInterpreter().interpretDate(day);
                if (dayLabel == null) throw new IllegalStateException("A DateTimeInterpreter must not return null date");
                canvas.drawText(dayLabel, startPixel + mWidthPerDay / 2, dayHeaderHeight - mHeaderMarginBottom, sameDay ? mTodayHeaderTextPaint : mHeaderTextPaint);
            }

            // Draw the current time line
            float start = (startPixel < mHeaderColumnWidth ? mHeaderColumnWidth : startPixel);
            if (displayCurrentTimeLine && isSameDay(today(), day)) {
                float startY = dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 + mCurrentOrigin.y;
                Calendar now = Calendar.getInstance();
                float beforeNow = (now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60.0f) * mHourHeight;
                canvas.drawLine(start, startY + beforeNow, startPixel + mWidthPerDay, startY + beforeNow, mCurrentTimeLinePaint);
            }
            startPixel += mWidthPerDay + mColumnGap;
        }
        return drewEvents;
    }

    public void showDayHeader() {
        dayHeaderHeight = mHeaderTextHeight + mHeaderRowPadding * 2;
    }

    public void hideDayHeader() {
        dayHeaderHeight = 0f;
    }

    private boolean isGoingToDate() {
        return finalXWhenScrollingToDate != null;
    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    @Nullable
    private Calendar getTimeFromPoint(float x, float y) {
        int leftDaysWithGaps = (int) -(Math.ceil(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)));
        float startPixel = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
                mHeaderColumnWidth;
        for (int dayNumber = leftDaysWithGaps + 1; dayNumber <= leftDaysWithGaps + mNumberOfVisibleDays + 1; dayNumber++) {
            float start = (startPixel < mHeaderColumnWidth ? mHeaderColumnWidth : startPixel);
            if (mWidthPerDay + startPixel - start > 0 && x >= start && x < startPixel + mWidthPerDay) {
                Calendar day = (Calendar) mToday.clone();
                day.add(Calendar.DATE, dayNumber - 1);
                float pixelsFromZero = y - mCurrentOrigin.y - dayHeaderHeight - mTimeTextHeight / 2 - mHeaderMarginBottom;
                int hour = (int) (pixelsFromZero / mHourHeight);
                int minute = (int) (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight);
                day.add(Calendar.HOUR, hour);
                day.set(Calendar.MINUTE, minute);
                return day;
            }
            startPixel += mWidthPerDay + mColumnGap;
        }
        return null;
    }

    private void drawNewLongPressedEvent(@NonNull Canvas canvas) {
        float x = getLongPressedNewEventX();
        float y = getLongPressedNewEventY();
        Calendar date = getTimeFromPoint(x, y);
        boolean shouldDrawEvent = shouldDrawLongPressedEvent(date);
        if (date != null) {
            String time = mDateTimeInterpreter.interpretDateAsTime(date);
            canvas.drawText(time, x, y - 5, (shouldDrawEvent) ? newLongPressedEventHeaderTextPaint : transparentPaint);
        }

        // draw background
        canvas.drawRect(x, y, x + mWidthPerDay, y + mHourHeight, (shouldDrawEvent) ? newLongPressedEventHeaderTextPaint : transparentPaint);

        // draw title
        String title = "New event";
        Rect titleContainer = new Rect();
        newLongPressedEventTitleTextPaint.getTextBounds(title, 0, title.length(), titleContainer);
        canvas.drawText(title, x, y + titleContainer.height(), shouldDrawEvent ? newLongPressedEventTitleTextPaint : transparentPaint);

    }

    private boolean shouldDrawLongPressedEvent(@Nullable Calendar date) {
        return isUserLongPressing && isWithinScheduleScreen() && date != null;
    }

    private boolean isWithinScheduleScreen() {
        return longPressX >= getLongPressedNewEventX() && longPressY > (dayHeaderHeight + mHeaderRowPadding + mHeaderMarginBottom);
    }

    private float getLongPressedNewEventX() {
        return mHeaderColumnWidth;
    }

    private float getLongPressedNewEventY() {
        float bottomOfHeader = dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - mHourSeparatorHeight / 2;
        return Math.max(longPressY - bottomOfHeader, bottomOfHeader);
    }


    /**
     * Draws the empty view text when there are no jobs for a day
     */
    private void drawEmptyViewText(@NonNull Canvas canvas, float startPixel) {
        float width = mWidthPerDay-mHeaderColumnPadding*2;
        float yMiddle = getHeight() / 2;

        String emptyViewTitle = getEmptyViewClickListener().getEmptyViewTitle();
        StaticLayout titleTextLayout = new StaticLayout(emptyViewTitle, emptyViewTitleTextPaint, (int) width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        String emptyViewSubtitle = getEmptyViewClickListener().getEmptyViewSubtitle();
        StaticLayout subtitleTextLayout = new StaticLayout(emptyViewSubtitle, emptyViewSubtitleTextPaint, (int) width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        // Draw title
        canvas.save();
        float titleY = yMiddle-(titleTextLayout.getHeight()+subtitleTextLayout.getHeight())/2;
        canvas.translate(startPixel + mHeaderColumnPadding, titleY);
        titleTextLayout.draw(canvas);
        canvas.restore();

        // Draw subtitle
        canvas.save();
        canvas.translate(startPixel + mHeaderColumnPadding, titleY + titleTextLayout.getHeight());
        subtitleTextLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     * @return true if events were drawn; false if there are no events
     */
    private boolean drawEvents(Calendar date, float startFromPixel, Canvas canvas) {
        boolean hasEventsForDate = false;
        if (mEventRects != null && !mEventRects.isEmpty()) {
            for (int i = 0; i < mEventRects.size(); i++) {
                EventRect eventRect = mEventRects.get(i);
                if (isSameDay(eventRect.event.getStartTime(), date)) {
                    hasEventsForDate = true;
                    // Calculate top.
                    float top = mHourHeight * 24 * eventRect.top / 1440 + mCurrentOrigin.y + dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 + mEventMarginVertical;
                    float originalTop = top;
                    float headerHeight = dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2;
                    if (top < headerHeight) top = headerHeight;

                    // Calculate bottom.
                    float bottom = eventRect.bottom;
                    bottom = mHourHeight * 24 * bottom / 1440 + mCurrentOrigin.y + dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - mEventMarginVertical;

                    // Calculate left and right.
                    float left = startFromPixel + eventRect.left * mWidthPerDay;
                    if (left < startFromPixel) left += mOverlappingEventGap;
                    float right = left + eventRect.width * mWidthPerDay;
                    if (right < startFromPixel + mWidthPerDay || !eventRect.originalEvent.shouldExpand())
                        right -= mOverlappingEventGap; // set right margin for last bar event
                    if (left < mHeaderColumnWidth) left = mHeaderColumnWidth;

                    // Draw the event and the event name on top of it.
                    RectF eventRectF = new RectF(left, top, right, bottom);
                    if (bottom > dayHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 &&
                            left < right &&
                            eventRectF.right > mHeaderColumnWidth &&
                            eventRectF.left < getWidth() &&
                            eventRectF.bottom > dayHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom &&
                            eventRectF.top < getHeight()) {

                        mEventRects.get(i).rectF = eventRectF;

                        drawBackground(eventRect.originalEvent, eventRectF, canvas);

                        if (eventRect.originalEvent.shouldExpand()) {
                            drawEventHeader(eventRect.originalEvent, eventRectF, canvas);

                            // draw name
                            mEventTextPaint.setFakeBoldText(true); // Breezeworks change: bold event name
                            mEventTextPaint.setColor(eventRect.originalEvent.getDarkerColor());
                            float textBottom = drawText(eventRect.originalEvent.getName(), eventRectF, canvas, originalTop);

                            // draw description
                            mEventTextPaint.setFakeBoldText(false); // Breezeworks change: keep description text normal weight
                            drawDescription(eventRect.originalEvent.getDescription(), eventRectF, canvas, textBottom);
                        } else {
                            // don't show text
                        }
                    } else eventRect.rectF = null;
                }
            }
        }
        return hasEventsForDate;
    }

    private void drawEventHeader(@NonNull WeekViewEvent weekViewEvent, @NonNull RectF originalEventRect, @NonNull Canvas canvas) {
        if (mEventListener != null) {
            float left =  originalEventRect.left+EVENT_ORIGINAL_COLOR_WIDTH+mEventPadding*2;
            Bitmap headerBitmap = mEventListener.getEventHeaderImage();
            float headerImageWidth = (headerBitmap == null) ? 0 : headerBitmap.getWidth();
            float availableWidth = originalEventRect.right - left - headerImageWidth;
            if (availableWidth < MIN_EVENT_HEADER_WIDTH) {
                return;
            }

            String eventHeaderText = mEventListener.formatHeaderText(weekViewEvent);
            if (TextUtils.isEmpty(eventHeaderText)) { // don't show a header if the event has no header text
                return;
            }

            // draw background
            Bitmap headerBackgroundBitmap = mEventListener.getEventHeaderBackgroundTileImage();
            if (headerBackgroundBitmap != null) {
                BitmapShader bs = new BitmapShader(headerBackgroundBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                mEventHeaderBackgroundPaint.setColorFilter(new PorterDuffColorFilter(weekViewEvent.getLighterColor(), PorterDuff.Mode.SRC_IN));
                mEventHeaderBackgroundPaint.setShader(bs);

                Matrix m = new Matrix();
                RectF headerBackgroundRect = new RectF(originalEventRect.left, originalEventRect.top - mEventListener.getEventHeaderHeight(), originalEventRect.right, originalEventRect.top);
                m.postTranslate(headerBackgroundRect.left, headerBackgroundRect.right);
                mEventHeaderBackgroundPaint.getShader().setLocalMatrix(m);

                canvas.drawRect(headerBackgroundRect, mDayBackgroundPaint);
                canvas.drawRect(headerBackgroundRect, mEventHeaderBackgroundPaint);
            }

            // draw header contents
            float bottom = originalEventRect.top - mEventPadding*2;

            if (headerBitmap != null) {
                RectF dstRect = new RectF(left, bottom - mEventListener.getEventHeaderImageHeight(), left+headerImageWidth, bottom);
                mEventHeaderBackgroundPaint.setColorFilter(new PorterDuffColorFilter(weekViewEvent.getDarkerColor(), PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(headerBitmap, null, dstRect, mEventHeaderBackgroundPaint);
            }

            // draw header text
            left += ((headerBitmap == null) ? 0 : headerImageWidth+mEventPadding*2);
            float availableTextWidth = originalEventRect.right - left;
            mEventHeaderTextPaint.setColor(weekViewEvent.getDarkerColor());
            eventHeaderText = TextUtils.ellipsize(eventHeaderText, mEventHeaderTextPaint, availableTextWidth, TextUtils.TruncateAt.END).toString();
            canvas.drawText(eventHeaderText, left, bottom, mEventHeaderTextPaint);
        }
    }

    // Breezeworks change: draw background depending on whether event should be expanded or not
    private void drawBackground(@NonNull WeekViewEvent weekViewEvent, @NonNull RectF originalEventRect, @NonNull Canvas canvas) {
        if (weekViewEvent.shouldExpand()) {
            drawExpandedBackground(weekViewEvent, originalEventRect, canvas);
        } else {
            drawBarBackground(weekViewEvent, originalEventRect, canvas);
        }
    }

    // Breezeworks change: our background is different. we want a sliver of the original color and the rest to be the lighter color
    private void drawExpandedBackground(@NonNull WeekViewEvent weekViewEvent, @NonNull RectF originalEventRect, @NonNull Canvas canvas) {
        // draw sliver with original color
        mEventBackgroundPaint.setColor(weekViewEvent.getColor() == 0 ? mDefaultEventColor : weekViewEvent.getColor());
        RectF originallyColoredEventRect = new RectF(originalEventRect.left, originalEventRect.top, originalEventRect.left + EVENT_ORIGINAL_COLOR_WIDTH, originalEventRect.bottom);
        canvas.drawRect(originallyColoredEventRect, mEventBackgroundPaint);

        // draw rest with lighter color
        RectF lightColoredEventRect = new RectF(originallyColoredEventRect.right, originalEventRect.top, originalEventRect.right, originalEventRect.bottom);
        mEventBackgroundPaint.setColor(weekViewEvent.getLighterColor());
        canvas.drawRect(lightColoredEventRect, mEventBackgroundPaint);

        // draw white border on top
        RectF whiteTopBorder = new RectF(originalEventRect.left, originalEventRect.top, originalEventRect.right, originalEventRect.top + EVENT_WHITE_TOP_BORDER_HEIGHT);
        canvas.drawRect(whiteTopBorder, mDayBackgroundPaint);
    }

    // Breezeworks change: color background light when showing events that are bars
    private void drawBarBackground(@NonNull WeekViewEvent weekViewEvent, @NonNull RectF originalEventRect, @NonNull Canvas canvas) {
        mEventBackgroundPaint.setColor(weekViewEvent.getBarColor());
        canvas.drawRect(originalEventRect, mEventBackgroundPaint);
    }


    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param text         The text to draw.
     * @param rect         The rectangle on which the text is to be drawn.
     * @param canvas       The canvas to draw upon.
     * @param originalTop  The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @return pixel indicator for bottom of drawn text
     */
    private float drawText(String text, RectF rect, Canvas canvas, float originalTop) {
        float bottom = originalTop; // Breezeworks change: need to keep track of bottom indicator to add more text
        int height = (int) (rect.bottom - originalTop - mEventPadding * 2);
        if (rect.right - rect.left - mEventPadding * 2 - EVENT_ORIGINAL_COLOR_WIDTH - mOverlappingEventGap <= 0 || height <= 0) {
            return bottom;
        }

        float width = rect.right - rect.left - mEventPadding * 2 - EVENT_ORIGINAL_COLOR_WIDTH - mOverlappingEventGap;
        if (width < 0) {
            return bottom;
        }

        // Get text dimensions
        StaticLayout textLayout = new StaticLayout(text, mEventTextPaint, (int) width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        // Crop height
        int textHeight = textLayout.getHeight();
        int availableHeight = height / 2; // Breezeworks change: only allow title to be at most half the screen to include customer name
        if (availableHeight < textHeight && height > textHeight) {
            availableHeight = textHeight;
        }
        int lineCount = textLayout.getLineCount();
        int availableLineCount = (int) Math.floor((availableHeight * lineCount) / textHeight);
        float widthAvailable = width * availableLineCount;

        textLayout = new StaticLayout(TextUtils.ellipsize(text, mEventTextPaint, widthAvailable, TextUtils.TruncateAt.END), mEventTextPaint, (int) width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);
        bottom += textLayout.getHeight();

        // Draw text
        canvas.save();
        canvas.translate(rect.left + mEventPadding*2, originalTop + mEventPadding);
        textLayout.draw(canvas);
        canvas.restore();
        return bottom;
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param text         The text to draw.
     * @param rect         The rectangle on which the text is to be drawn.
     * @param canvas       The canvas to draw upon.
     * @param top          top position where description should be drawn
     * @return pixel indicator for bottom of drawn text
     */
    private void drawDescription(String text, RectF rect, Canvas canvas, float top) {
        int availableHeight = (int) (rect.bottom - top);
        if (rect.right - rect.left - mEventPadding * 2 - EVENT_ORIGINAL_COLOR_WIDTH - mOverlappingEventGap <= 0 || availableHeight <= 0) return;

        int width = (int) (rect.right - rect.left - mEventPadding * 3 - EVENT_ORIGINAL_COLOR_WIDTH - mOverlappingEventGap);
        if (width < 0) {
            return;
        }

        // Get text dimensions
        StaticLayout textLayout = new StaticLayout(text, mEventTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        // Crop height
        int textHeight = textLayout.getHeight();
        int lineCount = textLayout.getLineCount();
        int availableLineCount = (int) Math.floor((availableHeight * lineCount) / textHeight);
        float widthAvailable = width * availableLineCount;
        textLayout = new StaticLayout(TextUtils.ellipsize(text, mEventTextPaint, widthAvailable, TextUtils.TruncateAt.END), mEventTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);

        // Draw text
        canvas.save();
        canvas.translate(rect.left + mEventPadding * 2, top + mEventPadding);
        textLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    private class EventRect {
        public WeekViewEvent event;
        public WeekViewEvent originalEvent;
        public RectF rectF;
        public float left;
        public float width;
        public float top;
        public float bottom;

        /**
         * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
         * on the calendar for a given event. There may be more than one rectangle for a single
         * event (an event that expands more than one day). In that case two instances of the
         * EventRect will be used for a single event. The given event will be stored in
         * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
         * be stored in "event".
         *
         * @param event         Represents the event which this instance of rectangle represents.
         * @param originalEvent The original event that was passed by the user.
         * @param rectF         The rectangle.
         */
        public EventRect(WeekViewEvent event, WeekViewEvent originalEvent, RectF rectF) {
            this.event = event;
            this.rectF = rectF;
            this.originalEvent = originalEvent;
        }
    }


    /**
     * Gets more events of one/more month(s) if necessary. This method is called when the user is
     * scrolling the week view. The week view stores the events of three months: the visible month,
     * the previous month, the next month.
     *
     * @param day The day where the user is currently is.
     */
    private void getMoreEvents(Calendar day) {

        // Delete all events if its not current month +- 1.
        deleteFarMonths(day);

        // Get more events if the month is changed.
        if (mEventRects == null) mEventRects = new ArrayList<EventRect>();
        if (mMonthChangeListener == null && !isInEditMode())
            throw new IllegalStateException("You must provide a MonthChangeListener");

        // If a refresh was requested then reset some variables.
        if (mRefreshEvents) {
            mEventRects.clear();
            mFetchedMonths = new int[3];
            uniqueEmployeeIdsOfBarredEvents.clear();
            barredEmployeeByLeftPositionMap.clear();
        }

        // Get events of previous month.
        int previousMonth = (day.get(Calendar.MONTH) == 0 ? 12 : day.get(Calendar.MONTH));
        int nextMonth = (day.get(Calendar.MONTH) + 2 == 13 ? 1 : day.get(Calendar.MONTH) + 2);
        int[] lastFetchedMonth = mFetchedMonths.clone();
        if (mFetchedMonths[0] < 1 || mFetchedMonths[0] != previousMonth || mRefreshEvents) {
            if (!containsValue(lastFetchedMonth, previousMonth) && !isInEditMode()) {
                List<WeekViewEvent> events = mMonthChangeListener.onMonthChange((previousMonth == 12) ? day.get(Calendar.YEAR) - 1 : day.get(Calendar.YEAR), previousMonth);
                sortEvents(events);
                for (WeekViewEvent event : events) {
                    cacheEvent(event);
                }
            }
            mFetchedMonths[0] = previousMonth;
        }

        // Get events of this month.
        if (mFetchedMonths[1] < 1 || mFetchedMonths[1] != day.get(Calendar.MONTH) + 1 || mRefreshEvents) {
            if (!containsValue(lastFetchedMonth, day.get(Calendar.MONTH) + 1) && !isInEditMode()) {
                List<WeekViewEvent> events = mMonthChangeListener.onMonthChange(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1);
                sortEvents(events);
                for (WeekViewEvent event : events) {
                    cacheEvent(event);
                }
            }
            mFetchedMonths[1] = day.get(Calendar.MONTH) + 1;
        }

        // Get events of next month.
        if (mFetchedMonths[2] < 1 || mFetchedMonths[2] != nextMonth || mRefreshEvents) {
            if (!containsValue(lastFetchedMonth, nextMonth) && !isInEditMode()) {
                List<WeekViewEvent> events = mMonthChangeListener.onMonthChange(nextMonth == 1 ? day.get(Calendar.YEAR) + 1 : day.get(Calendar.YEAR), nextMonth);
                sortEvents(events);
                for (WeekViewEvent event : events) {
                    cacheEvent(event);
                }
            }
            mFetchedMonths[2] = nextMonth;
        }

        // Prepare to calculate positions of each events.
        ArrayList<EventRect> tempEvents = new ArrayList<EventRect>(mEventRects);
        mEventRects = new ArrayList<EventRect>();
        Calendar dayCounter = (Calendar) day.clone();
        dayCounter.add(Calendar.MONTH, -1);
        dayCounter.set(Calendar.DAY_OF_MONTH, 1);
        Calendar maxDay = (Calendar) day.clone();
        maxDay.add(Calendar.MONTH, 1);
        maxDay.set(Calendar.DAY_OF_MONTH, maxDay.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Iterate through each day to calculate the position of the events.
        while (dayCounter.getTimeInMillis() <= maxDay.getTimeInMillis()) {
            ArrayList<EventRect> eventRects = new ArrayList<EventRect>();
            for (EventRect eventRect : tempEvents) {
                if (isSameDay(eventRect.event.getStartTime(), dayCounter)) {
                    eventRects.add(eventRect);
                }
            }

            computePositionOfEvents(eventRects);
            dayCounter.add(Calendar.DATE, 1);
        }
    }

    /**
     * Cache the event for smooth scrolling functionality.
     *
     * @param event The event to cache.
     */
    private void cacheEvent(WeekViewEvent event) {
        if (!isSameDay(event.getStartTime(), event.getEndTime())) {
            Calendar endTime = (Calendar) event.getStartTime().clone();
            endTime.set(Calendar.HOUR_OF_DAY, 23);
            endTime.set(Calendar.MINUTE, 59);
            Calendar startTime = (Calendar) event.getEndTime().clone();
            startTime.set(Calendar.HOUR_OF_DAY, 0);
            startTime.set(Calendar.MINUTE, 0);
            WeekViewEvent event1 = new WeekViewEvent(event.getId(), event.getName(), event.getStartTime(), endTime);
            event1.setColor(event.getColor());
            WeekViewEvent event2 = new WeekViewEvent(event.getId(), event.getName(), startTime, event.getEndTime());
            event2.setColor(event.getColor());
            mEventRects.add(new EventRect(event1, event, null));
            mEventRects.add(new EventRect(event2, event, null));
        } else mEventRects.add(new EventRect(event, event, null));
    }

    /**
     * Sorts the events in ascending order.
     *
     * @param events The events to be sorted.
     */
    private void sortEvents(List<WeekViewEvent> events) {
        Collections.sort(events, new Comparator<WeekViewEvent>() {
            @Override
            public int compare(WeekViewEvent event1, WeekViewEvent event2) {
                long start1 = event1.getStartTime().getTimeInMillis();
                long start2 = event2.getStartTime().getTimeInMillis();
                int comparator = start1 > start2 ? 1 : (start1 < start2 ? -1 : 0);
                if (comparator == 0) {
                    long end1 = event1.getEndTime().getTimeInMillis();
                    long end2 = event2.getEndTime().getTimeInMillis();
                    comparator = end1 > end2 ? 1 : (end1 < end2 ? -1 : 0);
                }
                return comparator;
            }
        });
    }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     *
     * @param eventRects The events along with their wrapper class.
     */
    private void computePositionOfEvents(List<EventRect> eventRects) {
        uniqueEmployeeIdsOfBarredEvents.clear();
        barredEmployeeByLeftPositionMap.clear();

        // Make "collision groups" for all events that collide with others.
        List<List<EventRect>> collisionGroups = new ArrayList<List<EventRect>>();
        for (EventRect eventRect : eventRects) {
            if (!eventRect.originalEvent.shouldExpand()) {
                uniqueEmployeeIdsOfBarredEvents.add(eventRect.originalEvent.getEmployeeId());
            }
            boolean isPlaced = false;
            outerLoop:
            for (List<EventRect> collisionGroup : collisionGroups) {
                for (EventRect groupEvent : collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event)) {
                        collisionGroup.add(eventRect);
                        isPlaced = true;
                        break outerLoop;
                    }
                }
            }
            if (!isPlaced) {
                List<EventRect> newGroup = new ArrayList<EventRect>();
                newGroup.add(eventRect);
                collisionGroups.add(newGroup);
            }
        }

        for (List<EventRect> collisionGroup : collisionGroups) {
            expandEventsToMaxWidth2(collisionGroup);
        }
    }

    private class EventRow {
        public int totalNumExpandedEvents;
        public int startingHour;
        private int maxEndingHour; // considers overlapping events
        private int maxEndingMinute; // considers overlapping events
        public List<EventRect> rowEventRects = new ArrayList<>();
        public List<EventRect> overlappingEvents = new ArrayList<>();
        public List<Boolean> overlappingEventUsedFlags = new ArrayList<>(); // indicates if event is already considered

        private void updateMaxEndingTime(int eventEndingHour, int eventEndingMinute) {
            if (eventEndingHour > maxEndingHour || (eventEndingHour == maxEndingHour && eventEndingMinute > maxEndingMinute)) {
                maxEndingHour = eventEndingHour;
                maxEndingMinute = eventEndingMinute;
            }
        }

        /**
         * Adds eventRect to row and increments total number of expanded events for row if necessary
         * @param eventRect
         */
        public void addRowEvent(@NonNull EventRect eventRect) {
//            Log.e("weekview", " adding " + eventRect.originalEvent.getName() + " to " + startingHour + " numexpandedSoFar " + totalNumExpandedEvents + " curending ");
            rowEventRects.add(eventRect);
            if (eventRect.originalEvent.shouldExpand()) {
                totalNumExpandedEvents++;
            }
//            Log.e("weekview", "final numexpanded " + eventRect.originalEvent.getName() + " to " + startingHour + " numexpandedSoFar " + totalNumExpandedEvents);
        }

        /**
         * Adds overlapping eventRect to row and increments total number of expanded events for row if necessary
         * @param eventRect
         */
        public void addOverlappingEvent(@NonNull EventRect eventRect) {
//            Log.e("weekview", " adding overlapping " + eventRect.originalEvent.getName() + " to " + startingHour + " numexpandedSoFar " + totalNumExpandedEvents);
            overlappingEvents.add(eventRect);
            overlappingEventUsedFlags.add(false);
            if (eventRect.originalEvent.shouldExpand()) {
                totalNumExpandedEvents++;
            }
//            Log.e("weekview", "final numexpanded " + eventRect.originalEvent.getName() + " to " + startingHour + " numexpandedSoFar " + totalNumExpandedEvents);
        }

        public int getPosOfUnusedOverlappingEvent() {
            int indexOfFirstUnused = 0;
            for (Boolean isEventUsed : overlappingEventUsedFlags) {
                if (isEventUsed) {
                    indexOfFirstUnused++;
                } else {
                    break;
                }
            }
            return indexOfFirstUnused;
        }
    }

    @SuppressWarnings("unchecked")
    private void expandEventsToMaxWidth2(@NonNull List<EventRect> collisionGroup) {
//        Log.e("weekview", "STARTING COLLISION GROUP");
        List<EventRow> eventRows = new ArrayList<>();
        populateEventRows(eventRows, collisionGroup);

        // todo remove this after debugging
//        for (EventRow row : eventRows) {
//            Log.e("weekview", "row startingTime " + row.startingHour + " totalNumExpanded " + row.totalNumExpandedEvents);
//            for (EventRect eventRect : row.rowEventRects) {
//                Log.e("weekview", "event " + eventRect.originalEvent.getName());
//            }
//
//            for (EventRect eventRect : row.overlappingEvents) {
//                Log.e("weekview", "OVERLAPPING entry " + eventRect.originalEvent.getName());
//            }
//
//        }

        // Determine how events in rows are laid out depending on whether barred or expanded
        int totalBarredEvents = uniqueEmployeeIdsOfBarredEvents.size();
        float totalBarredWidth = (totalBarredEvents * EVENT_BAR_WIDTH) / mWidthPerDay;
        for (int rowIndex = 0; rowIndex < eventRows.size(); rowIndex++) {
            EventRow row = eventRows.get(rowIndex);
            float nextLeft = 0;
            // eventIndex includes overlapping events in addition to row events
            // curEventInRowIndex only pertains to row events
            for (int curEventInRowIndex = 0; curEventInRowIndex < row.rowEventRects.size(); curEventInRowIndex++) {
                EventRect eventRect = row.rowEventRects.get(curEventInRowIndex);
                Calendar eventStartTime = eventRect.event.getStartTime();
                int eventStartTimeHour = eventStartTime.get(Calendar.HOUR_OF_DAY);
                eventRect.top = eventStartTimeHour * 60 + eventStartTime.get(Calendar.MINUTE);

                // If end time carries over to next day, just set bottom to last ultimate position in day
                Calendar eventEndTime = eventRect.event.getEndTime();
                int eventEndTimeHour = eventEndTime.get(Calendar.HOUR_OF_DAY);
                boolean endTimeCarriesOverToNextDay = eventStartTimeHour > eventEndTimeHour;
//                Log.e("weekview", "CUR EVENT  " + eventRect.originalEvent.getName() + " for index " + curEventInRowIndex + " next left " + nextLeft);

                if (eventRect.originalEvent.shouldExpand()) {
                    // make sure left accounts for possible overlapping expanded events at beginning of cur row
                    float diff = -1;
                    int overlappingEventIndex = row.getPosOfUnusedOverlappingEvent();
                    for(; diff< MIN_EVENT_WIDTH_PERCENTAGE && overlappingEventIndex < row.overlappingEvents.size(); overlappingEventIndex++) {
                        EventRect overlappingEventRect = row.overlappingEvents.get(overlappingEventIndex);
//                        Log.e("weekview", "overlapping index " + overlappingEventIndex);
//                        Log.e("weekview", "BEFORE event " + overlappingEventRect.originalEvent.getName() + " not in row time " + row.startingHour);
                        EventRect eventRectWithDimens = getEventRect(overlappingEventRect.originalEvent.getId());
                        if (eventRectWithDimens != null) {
                            if (eventRectWithDimens.left > (nextLeft + MIN_EVENT_WIDTH_PERCENTAGE)) { // it's too far out
                                break;
                            }
                            row.overlappingEventUsedFlags.set(overlappingEventIndex, true);
                            diff = Math.abs(nextLeft - eventRectWithDimens.left);
                            nextLeft += eventRectWithDimens.width;
//                            Log.e("weekview", "BEFORE new next left " + nextLeft + " diff " + diff);
                        } else {
                            break;
                        }
                    }

                    eventRect.left = nextLeft;
//                    Log.e("weekview", "EXPANDED");

                    // Observe overlapping events after cur event to determine the width
                    float curEventRight = 0;
                    for (; overlappingEventIndex < row.overlappingEvents.size(); overlappingEventIndex++) {
//                        Log.e("weekview", "overlapping index " + overlappingEventIndex);
                        EventRect overlappingEventRect = row.overlappingEvents.get(overlappingEventIndex);
//                            Log.e("weekview", "next event " + overlappingEventRect.originalEvent.getName() + " not in row");
                            EventRect nextEventRectWithDimens = getEventRect(overlappingEventRect.originalEvent.getId());
                            // make sure that if the dimensions exist, that they belong to the right piece of event
                            if (nextEventRectWithDimens != null && nextEventRectWithDimens.event.getStartTime().get(Calendar.DAY_OF_YEAR) == eventRect.event.getStartTime().get(Calendar.DAY_OF_YEAR)) {
//                                Log.e("weekview", "getting event rect with dimens for  " + overlappingEventRect.originalEvent.getName() + " nextLeft " + nextEventRectWithDimens.left + " diff " + Math.abs(eventRect.left - nextEventRectWithDimens.left));

                                // check if there is enough space between overlapping event and cur event
                                diff = Math.abs(eventRect.left - nextEventRectWithDimens.left);
                                if (diff > MIN_EVENT_WIDTH_PERCENTAGE) {
                                    curEventRight = nextEventRectWithDimens.left;
                                } else {
                                    eventRect.left += nextEventRectWithDimens.width;
                                }
                                row.overlappingEventUsedFlags.set(overlappingEventIndex, true);
                                nextLeft += nextEventRectWithDimens.width;
//                                Log.e("weekview", "new next left " + nextLeft + " eventRect left " + eventRect.left);
                                break;
                            } else {
                                break;
                            }
                    }

//                    Log.e("weekview", "event right " + curEventRight + " event left " + eventRect.left);
                    float remainingSpaceToFill = 1-curEventRight;
                    if (curEventRight > 0 && remainingSpaceToFill > totalBarredWidth) {
                        eventRect.width = Math.abs(eventRect.left - curEventRight);
                    } else {
                        if (curEventInRowIndex == row.rowEventRects.size() - 1 && rowIndex == eventRows.size() - 1 && row.totalNumExpandedEvents > 1) {
                            // last expanded event in row that has more than one expanded event should just get the diff because left should be the most up to date
                            eventRect.width = 1-eventRect.left;
                        } else {
                            eventRect.width = (1 - totalBarredWidth) / row.totalNumExpandedEvents;
                        }
                    }
//                    Log.e("weekview", "event width " + eventRect.width);
                    nextLeft += eventRect.width;
                } else {
//                    Log.e("weekview", "BARRED");
                    long employeeId = eventRect.originalEvent.getEmployeeId();
                    eventRect.width = EVENT_BAR_WIDTH / mWidthPerDay;
                    if (barredEmployeeByLeftPositionMap.containsKey(employeeId)) {
                        eventRect.left = barredEmployeeByLeftPositionMap.get(employeeId);
//                     Log.e("weekview", "barred map contains key " + " left " + eventRect.left);
                    } else {
                        float left;
                        if (barredEmployeeByLeftPositionMap.isEmpty()) {
                            // Since there is not position mapped yet, put bar at tend of row
                            left = 1 - (EVENT_BAR_WIDTH / mWidthPerDay);
//                        Log.e("weekview", "empty map left " + left);
                        } else {
                            // Find the smallest percentage of width and increment that based on the number of barred events already accounted for
                            float smallestLeft = 1;
                            for (float barredEventLeft : barredEmployeeByLeftPositionMap.values()) {
                                smallestLeft = Math.min(smallestLeft, barredEventLeft);
                            }
                            left = smallestLeft - eventRect.width;
//                        Log.e("weekview", "not contained in barred map left " + left);
                        }
                        eventRect.left = left;
                        barredEmployeeByLeftPositionMap.put(employeeId, eventRect.left);
                    }
                }

//                Log.e("weekview", "eventstarttime + " + eventStartTime + "eventStartTimehour " + eventStartTimeHour + " endtimehour " + eventEndTimeHour);
                int endTimeHour = (endTimeCarriesOverToNextDay) ? 23 : eventEndTimeHour;
                int endTimeMinute = (endTimeCarriesOverToNextDay) ? 59 : eventEndTime.get(Calendar.MINUTE);
                eventRect.bottom = endTimeHour*60 + endTimeMinute;
//                Log.e("weekview", "adding " + eventRect.originalEvent.getName() + " to mEventRects with left " + eventRect.left + " bottom " + eventRect.bottom );
//                Log.e("weekview", "next left " + nextLeft);
                mEventRects.add(eventRect);
            }
        }
    }

    private void populateEventRows(@NonNull List<EventRow> eventRows, @NonNull List<EventRect> collisionGroup) {
        for (EventRect curEventRect : collisionGroup) {
            int eventStartingHour = curEventRect.event.getStartTime().get(Calendar.HOUR_OF_DAY);
            int eventStartingDay = curEventRect.event.getStartTime().get(Calendar.DAY_OF_MONTH);
            int eventEndingDay = curEventRect.event.getEndTime().get(Calendar.DAY_OF_MONTH);
            int eventEndingHour = curEventRect.event.getEndTime().get(Calendar.HOUR_OF_DAY);
            if (eventEndingDay > eventStartingDay || eventStartingHour > eventEndingHour) {
                eventEndingHour += 24;
            }
            int eventEndingMinute = curEventRect.event.getEndTime().get(Calendar.MINUTE);

//            Log.e("weekview", "EVENT "+ curEventRect.originalEvent.getName() +  " eventStartTime " + curEventRect.event.getStartTime().get(Calendar.DAY_OF_YEAR) + " original start time " + curEventRect.originalEvent.getStartTime().get(Calendar.DAY_OF_YEAR));

            // Add new rows if certain hours arent accounted for yet
            Integer startingHourOfNewlyAddedRows = null;
            boolean noRowsExist = eventRows.isEmpty();
            int lastRowStartingHour = (noRowsExist) ? 0 : eventRows.get(eventRows.size() - 1).startingHour;
            for (int curHour = eventStartingHour; (curHour < eventEndingHour || (eventEndingMinute > 0 && curHour <= eventEndingHour)) && curHour < 24; curHour++) {
//                Log.e("weekview", "curhour " + curHour + " eventEndingHour " + eventEndingHour + " ending minute " + eventEndingMinute);
                if (eventRows.isEmpty() || lastRowStartingHour < curHour) { // add new row if cur row isn't accounted for
                    if (startingHourOfNewlyAddedRows == null) {
                        startingHourOfNewlyAddedRows = curHour;
                    }
                    // add new row for event
                    EventRow eventRow = new EventRow();
                    eventRow.startingHour = curHour;
                    eventRow.updateMaxEndingTime(eventEndingHour, eventEndingMinute);
                    if (curHour == eventStartingHour) {
                        eventRow.addRowEvent(curEventRect);
                    } else if (curEventRect.originalEvent.shouldExpand()) {
                        eventRow.addOverlappingEvent(curEventRect);
                    }
                    eventRows.add(eventRow);
                }

            }

//            Log.e("weekview", "STARTING TO ADD OVERLAPPING EVENTS");

            // iterate through existing rows and add cur event to row or as overlapping event if necessary
            for (EventRow eventRow : eventRows) {
                if (eventRow.startingHour < eventEndingHour || (eventRow.startingHour == eventEndingHour && eventEndingMinute > 0)) {
                    if (eventRow.startingHour == eventStartingHour) {
                        if (startingHourOfNewlyAddedRows == null || eventRow.startingHour < startingHourOfNewlyAddedRows) {
                            eventRow.updateMaxEndingTime(eventEndingHour, eventEndingMinute);
                            eventRow.addRowEvent(curEventRect);
                        }
                    } else if (curEventRect.originalEvent.shouldExpand()) {
//                        Log.e("weekview",  " startinghournewly " + startingHourOfNewlyAddedRows + " eventrow starting " + eventRow.startingHour + " eventrow endinghour " + eventRow.maxEndingHour + " eventRow endingminute " + eventRow.maxEndingMinute + " curstartin " + eventStartingHour);
                        if ((startingHourOfNewlyAddedRows == null || eventRow.startingHour < startingHourOfNewlyAddedRows)
                                && (eventStartingHour < eventRow.maxEndingHour || (eventStartingHour == eventRow.maxEndingHour && eventRow.maxEndingMinute > 0))) {
                            eventRow.updateMaxEndingTime(eventEndingHour, eventEndingMinute);
                            eventRow.addOverlappingEvent(curEventRect);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private EventRect getEventRect(long eventId) {
        if (mEventRects == null) {
            mEventRects = new ArrayList<>();
        }
        for (EventRect eventRect : mEventRects) {
            if (eventRect.originalEvent.getId() == eventId) {
                return eventRect;
            }
        }
        return null;
    }

    /**
     * Checks if two events overlap to the millisecond
     *
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private boolean isEventsCollide(WeekViewEvent event1, WeekViewEvent event2) {
        long start1 = event1.getStartTime().getTimeInMillis();
        long end1 = event1.getEndTime().getTimeInMillis();
        long start2 = event2.getStartTime().getTimeInMillis();
        long end2 = event2.getEndTime().getTimeInMillis();
        return !((start1 >= end2) || (end1 <= start2));
    }


    /**
     * Checks if time1 occurs after (or at the same time) time2.
     *
     * @param time1 The time to check.
     * @param time2 The time to check against.
     * @return true if time1 and time2 are equal or if time1 is after time2. Otherwise false.
     */
    private boolean isTimeAfterOrEquals(Calendar time1, Calendar time2) {
        return !(time1 == null || time2 == null) && time1.getTimeInMillis() >= time2.getTimeInMillis();
    }

    /**
     * Deletes the events of the months that are too far away from the current month.
     *
     * @param currentDay The current day.
     */
    private void deleteFarMonths(Calendar currentDay) {

        if (mEventRects == null) return;

        Calendar nextMonth = (Calendar) currentDay.clone();
        nextMonth.add(Calendar.MONTH, 1);
        nextMonth.set(Calendar.DAY_OF_MONTH, nextMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        nextMonth.set(Calendar.HOUR_OF_DAY, 12);
        nextMonth.set(Calendar.MINUTE, 59);
        nextMonth.set(Calendar.SECOND, 59);

        Calendar prevMonth = (Calendar) currentDay.clone();
        prevMonth.add(Calendar.MONTH, -1);
        prevMonth.set(Calendar.DAY_OF_MONTH, 1);
        prevMonth.set(Calendar.HOUR_OF_DAY, 0);
        prevMonth.set(Calendar.MINUTE, 0);
        prevMonth.set(Calendar.SECOND, 0);

        List<EventRect> newEvents = new ArrayList<EventRect>();
        for (EventRect eventRect : mEventRects) {
            boolean isFarMonth = eventRect.event.getStartTime().getTimeInMillis() > nextMonth.getTimeInMillis() || eventRect.event.getEndTime().getTimeInMillis() < prevMonth.getTimeInMillis();
            if (!isFarMonth) newEvents.add(eventRect);
        }
        mEventRects.clear();
        mEventRects.addAll(newEvents);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mAreDimensionsInvalid = true;
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting and getting the properties.
    //
    /////////////////////////////////////////////////////////////////

    public void setOnEventClickListener(EventClickListener listener) {
        this.mEventClickListener = listener;
    }

    public EventClickListener getEventClickListener() {
        return mEventClickListener;
    }

    public MonthChangeListener getMonthChangeListener() {
        return mMonthChangeListener;
    }

    public void setMonthChangeListener(MonthChangeListener monthChangeListener) {
        this.mMonthChangeListener = monthChangeListener;
    }

    public EventLongPressListener getEventLongPressListener() {
        return mEventLongPressListener;
    }

    public void setEventLongPressListener(EventLongPressListener eventLongPressListener) {
        this.mEventLongPressListener = eventLongPressListener;
    }

    public void setEmptyViewClickListener(EmptyViewClickListener emptyViewClickListener) {
        this.mEmptyViewClickListener = emptyViewClickListener;
    }

    public EmptyViewClickListener getEmptyViewClickListener() {
        return mEmptyViewClickListener;
    }

    public void setEmptyViewLongPressListener(EmptyViewLongPressListener emptyViewLongPressListener) {
        this.mEmptyViewLongPressListener = emptyViewLongPressListener;
    }

    public EmptyViewLongPressListener getEmptyViewLongPressListener() {
        return mEmptyViewLongPressListener;
    }

    public void setScrollListener(ScrollListener scrolledListener){
        this.mScrollListener = scrolledListener;
    }

    public ScrollListener getScrolledListener(){
        return mScrollListener;
    }

    public EventListener getEventListener() {
        return mEventListener;
    }

    public void setEventListener(EventListener mEventListener) {
        this.mEventListener = mEventListener;
    }

    /**
     * Get the interpreter which provides the text to show in the header column and the header row.
     *
     * @return The date, time interpreter.
     */
    public DateTimeInterpreter getDateTimeInterpreter() {
        if (mDateTimeInterpreter == null) {
            mDateTimeInterpreter = new DateTimeInterpreter() {
                @Override
                public String interpretDate(Calendar date) {
                    SimpleDateFormat sdf;
                    sdf = mDayNameLength == LENGTH_SHORT ? new SimpleDateFormat("EEEEE") : new SimpleDateFormat("EEE");
                    try {
                        String dayName = sdf.format(date.getTime()).toUpperCase();
                        return String.format("%s %d/%02d", dayName, date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                    }
                }

                @Override
                public String interpretTime(int hour) {
                    String amPm;
                    if (hour >= 0 && hour < 12) amPm = "AM";
                    else amPm = "PM";
                    if (hour == 0) hour = 12;
                    if (hour > 12) hour -= 12;
                    return String.format("%02d %s", hour, amPm);
                }

                @Override
                public String interpretDateAsTime(@NonNull Calendar date) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    try {
                        return sdf.format(date.getTime()).toUpperCase();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "";
                    }
                }
            };
        }
        return mDateTimeInterpreter;
    }

    /**
     * Set the interpreter which provides the text to show in the header column and the header row.
     *
     * @param dateTimeInterpreter The date, time interpreter.
     */
    public void setDateTimeInterpreter(DateTimeInterpreter dateTimeInterpreter) {
        this.mDateTimeInterpreter = dateTimeInterpreter;
    }


    /**
     * Get the number of visible days in a week.
     *
     * @return The number of visible days in a week.
     */
    public int getNumberOfVisibleDays() {
        return mNumberOfVisibleDays;
    }

    /**
     * Set the number of visible days in a week.
     *
     * @param numberOfVisibleDays The number of visible days in a week.
     */
    public void setNumberOfVisibleDays(int numberOfVisibleDays) {
        this.mNumberOfVisibleDays = numberOfVisibleDays;
        mCurrentOrigin.x = 0;
        mCurrentOrigin.y = 0;
        invalidate();
    }

    public int getHourHeight() {
        return mHourHeight;
    }

    public void setHourHeight(int hourHeight) {
        mHourHeight = hourHeight;
        invalidate();
    }

    public int getColumnGap() {
        return mColumnGap;
    }

    public void setColumnGap(int columnGap) {
        mColumnGap = columnGap;
        invalidate();
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    /**
     * Set the first day of the week. First day of the week is used only when the week view is first
     * drawn. It does not of any effect after user starts scrolling horizontally.
     * <p>
     * <b>Note:</b> This method will only work if the week view is set to display more than 6 days at
     * once.
     * </p>
     *
     * @param firstDayOfWeek The supported values are {@link java.util.Calendar#SUNDAY},
     *                       {@link java.util.Calendar#MONDAY}, {@link java.util.Calendar#TUESDAY},
     *                       {@link java.util.Calendar#WEDNESDAY}, {@link java.util.Calendar#THURSDAY},
     *                       {@link java.util.Calendar#FRIDAY}.
     */
    public void setFirstDayOfWeek(int firstDayOfWeek) {
        mFirstDayOfWeek = firstDayOfWeek;
        invalidate();
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
        mTodayHeaderTextPaint.setTextSize(mTextSize);
        mHeaderTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setTextSize(mTextSize);
        invalidate();
    }

    public int getHeaderColumnPadding() {
        return mHeaderColumnPadding;
    }

    public void setHeaderColumnPadding(int headerColumnPadding) {
        mHeaderColumnPadding = headerColumnPadding;
        invalidate();
    }

    public int getHeaderColumnTextColor() {
        return mHeaderColumnTextColor;
    }

    public void setHeaderColumnTextColor(int headerColumnTextColor) {
        mHeaderColumnTextColor = headerColumnTextColor;
        invalidate();
    }

    public int getHeaderRowPadding() {
        return mHeaderRowPadding;
    }

    public void setHeaderRowPadding(int headerRowPadding) {
        mHeaderRowPadding = headerRowPadding;
        invalidate();
    }

    public int getHeaderRowBackgroundColor() {
        return mHeaderRowBackgroundColor;
    }

    public void setHeaderRowBackgroundColor(int headerRowBackgroundColor) {
        mHeaderRowBackgroundColor = headerRowBackgroundColor;
        invalidate();
    }

    public int getDayBackgroundColor() {
        return mDayBackgroundColor;
    }

    public void setDayBackgroundColor(int dayBackgroundColor) {
        mDayBackgroundColor = dayBackgroundColor;
        invalidate();
    }

    public int getHourSeparatorColor() {
        return mHourSeparatorColor;
    }

    public void setHourSeparatorColor(int hourSeparatorColor) {
        mHourSeparatorColor = hourSeparatorColor;
        invalidate();
    }

    public int getTodayBackgroundColor() {
        return mTodayBackgroundColor;
    }

    public void setTodayBackgroundColor(int todayBackgroundColor) {
        mTodayBackgroundColor = todayBackgroundColor;
        invalidate();
    }

    public int getHourSeparatorHeight() {
        return mHourSeparatorHeight;
    }

    public void setHourSeparatorHeight(int hourSeparatorHeight) {
        mHourSeparatorHeight = hourSeparatorHeight;
        invalidate();
    }

    public int getTodayHeaderTextColor() {
        return mTodayHeaderTextColor;
    }

    public void setTodayHeaderTextColor(int todayHeaderTextColor) {
        mTodayHeaderTextColor = todayHeaderTextColor;
        invalidate();
    }

    public int getEventTextSize() {
        return mEventTextSize;
    }

    public void setEventTextSize(int eventTextSize) {
        mEventTextSize = eventTextSize;
        mEventTextPaint.setTextSize(mEventTextSize);
        invalidate();
    }

    public int getEventTextColor() {
        return mEventTextColor;
    }

    public void setEventTextColor(int eventTextColor) {
        mEventTextColor = eventTextColor;
        invalidate();
    }

    public int getEventPadding() {
        return mEventPadding;
    }

    public void setEventPadding(int eventPadding) {
        mEventPadding = eventPadding;
        invalidate();
    }

    public int getHeaderColumnBackgroundColor() {
        return mHeaderColumnBackgroundColor;
    }

    public void setHeaderColumnBackgroundColor(int headerColumnBackgroundColor) {
        mHeaderColumnBackgroundColor = headerColumnBackgroundColor;
        invalidate();
    }

    public int getDefaultEventColor() {
        return mDefaultEventColor;
    }

    public void setDefaultEventColor(int defaultEventColor) {
        mDefaultEventColor = defaultEventColor;
        invalidate();
    }

    /**
     * <b>Note:</b> Use {@link #setDateTimeInterpreter(DateTimeInterpreter)} and
     * {@link #getDateTimeInterpreter()} instead.
     *
     * @return Either long or short day name is being used.
     */
    @Deprecated
    public int getDayNameLength() {
        return mDayNameLength;
    }

    /**
     * Set the length of the day name displayed in the header row. Example of short day names is
     * 'M' for 'Monday' and example of long day names is 'Mon' for 'Monday'.
     * <p>
     * <b>Note:</b> Use {@link #setDateTimeInterpreter(DateTimeInterpreter)} instead.
     * </p>
     *
     * @param length Supported values are {@link com.alamkanak.weekview.WeekView#LENGTH_SHORT} and
     *               {@link com.alamkanak.weekview.WeekView#LENGTH_LONG}.
     */
    @Deprecated
    public void setDayNameLength(int length) {
        if (length != LENGTH_LONG && length != LENGTH_SHORT) {
            throw new IllegalArgumentException("length parameter must be either LENGTH_LONG or LENGTH_SHORT");
        }
        this.mDayNameLength = length;
    }

    public int getOverlappingEventGap() {
        return mOverlappingEventGap;
    }

    /**
     * Set the gap between overlapping events.
     *
     * @param overlappingEventGap The gap between overlapping events.
     */
    public void setOverlappingEventGap(int overlappingEventGap) {
        this.mOverlappingEventGap = overlappingEventGap;
        invalidate();
    }

    public int getEventMarginVertical() {
        return mEventMarginVertical;
    }

    /**
     * Set the top and bottom margin of the event. The event will release this margin from the top
     * and bottom edge. This margin is useful for differentiation consecutive events.
     *
     * @param eventMarginVertical The top and bottom margin.
     */
    public void setEventMarginVertical(int eventMarginVertical) {
        this.mEventMarginVertical = eventMarginVertical;
        invalidate();
    }

    /**
     * Returns the first visible day in the week view.
     *
     * @return The first visible day in the week view.
     */
    public Calendar getFirstVisibleDay() {
        return mFirstVisibleDay;
    }

    /**
     * Returns the last visible day in the week view.
     *
     * @return The last visible day in the week view.
     */
    public Calendar getLastVisibleDay() {
        return mLastVisibleDay;
    }

    /**
     * Get the scrolling speed factor in horizontal direction.
     *
     * @return The speed factor in horizontal direction.
     */
    public float getXScrollingSpeed() {
        return mXScrollingSpeed;
    }

    /**
     * Sets the speed for horizontal scrolling.
     *
     * @param xScrollingSpeed The new horizontal scrolling speed.
     */
    public void setXScrollingSpeed(float xScrollingSpeed) {
        this.mXScrollingSpeed = xScrollingSpeed;
    }
    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finalXWhenScrollingToDate = null;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mCurrentScrollDirection == Direction.HORIZONTAL) {
                float leftDays = Math.round(mCurrentOrigin.x / (mWidthPerDay + mColumnGap));
                int nearestOrigin = (int) (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap));
                mStickyScroller.startScroll((int) mCurrentOrigin.x, 0, -nearestOrigin, 0);
                ViewCompat.postInvalidateOnAnimation(WeekView.this);
            }
            mCurrentScrollDirection = Direction.NONE;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                longPressX = event.getX();
                longPressY = event.getY();
                mHandler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                break;
            case MotionEvent.ACTION_MOVE:
                mHandler.removeCallbacks(longPressRunnable);
                longPressX = event.getX();
                longPressY = event.getY();
                ViewCompat.postInvalidateOnAnimation(WeekView.this);
                break;
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(longPressRunnable);
                if (isUserLongPressing) {
                    if (mEmptyViewClickListener != null) {
                        Calendar selectedTime = getTimeFromPoint(getLongPressedNewEventX(), getLongPressedNewEventY());
                        if (selectedTime != null) {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            mEmptyViewClickListener.onFinishDraggingNewEvent(selectedTime);
                        }
                    }
                    ViewCompat.postInvalidateOnAnimation(WeekView.this);
                }
                isUserLongPressing = false;
                break;

        }
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            if (Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) < mWidthPerDay + mColumnGap && Math.abs(mScroller.getFinalX() - mScroller.getStartX()) != 0) {
                mScroller.forceFinished(true);
                float leftDays = Math.round(mCurrentOrigin.x / (mWidthPerDay + mColumnGap));
                if (mScroller.getFinalX() < mScroller.getCurrX()) leftDays--;
                else leftDays++;
                int nearestOrigin = (int) (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap));
                mStickyScroller.startScroll((int) mCurrentOrigin.x, 0, -nearestOrigin, 0);
            } else {
                if (mCurrentFlingDirection == Direction.VERTICAL) mCurrentOrigin.y = mScroller.getCurrY();
                else mCurrentOrigin.x = mScroller.getCurrX();
            }
        }
        ViewCompat.postInvalidateOnAnimation(this);

        if (mStickyScroller.computeScrollOffset()) {
            if (finalXWhenScrollingToDate != null) {
                int difference = (int) (finalXWhenScrollingToDate - mCurrentOrigin.x);
                if (Math.abs(difference/4) > MIN_SCROLL_DIFFERENCE) { // make sure the remaining scroll distance is at least bigger than the min
                    mStickyScroller.forceFinished(true);
                    int finalX  = (int) (difference / 4 + mCurrentOrigin.x);
                    mStickyScroller.startScroll((int) mCurrentOrigin.x, 0, finalX, 0);
                    mCurrentOrigin.x = finalX;
                } else { // ATP, just make the scroller go to the expected date
                    mCurrentOrigin.x = finalXWhenScrollingToDate;
                }
            } else {
                mCurrentOrigin.x = mStickyScroller.getCurrX();
            }
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Show today on the week view with no scroll animation
     */
    public void goToToday() {
        Calendar today = Calendar.getInstance();
        goToDate(today, false);
    }

    /**
     * Show a specific day on the week view without scrolling.
     * @param date The date to show.
     */
    public void goToDate(Calendar date) {
        goToDate(date, false);
    }

    /**
     * Show a specific day on the week view.
     * @param date The date to show.
     * @param scroll true to smooth scroll; false otherwise
     */
    public void goToDate(Calendar date, boolean scroll) {
        finalXWhenScrollingToDate = null;

        mStickyScroller.forceFinished(true);
        mScroller.forceFinished(true);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        if(mAreDimensionsInvalid) {
            mScrollToDay = date;
            return;
        }

        mRefreshEvents = true;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long dateInMillis = date.getTimeInMillis() + date.getTimeZone().getOffset(date.getTimeInMillis());
        long todayInMillis = today.getTimeInMillis() + today.getTimeZone().getOffset(today.getTimeInMillis());
        int dateDifference = (int) ((dateInMillis - todayInMillis) / (1000 * 60 * 60 * 24));

        if (scroll) {
            finalXWhenScrollingToDate = (int)(-dateDifference * (mWidthPerDay + mColumnGap));
            mStickyScroller.startScroll((int) mCurrentOrigin.x, 0, (int)(finalXWhenScrollingToDate - mCurrentOrigin.x), 0);
        } else {
            mCurrentOrigin.x = -dateDifference * (mWidthPerDay + mColumnGap);
        }
        ViewCompat.postInvalidateOnAnimation(WeekView.this);
    }

    /**
     * Refreshes the view and loads the events again.
     */
    public void notifyDatasetChanged() {
        mRefreshEvents = true;
        invalidate();
    }

    /**
     * Refreshes the view
     */
    public void refresh() {
        invalidate();
    }

    public void updateWeekViewEvent(@NonNull WeekViewEvent weekViewEvent) {

        refresh();
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    public void goToHour(double hour){
        int verticalOffset = (int) (mHourHeight * hour);
        if (hour < 0)
            verticalOffset = 0;
        else if (hour > 24)
            verticalOffset = mHourHeight * 24;

        if (mAreDimensionsInvalid) {
            mScrollToHour = hour;
            return;
        } else if (verticalOffset > mHourHeight * 24 - getHeight() + dayHeaderHeight + mHeaderMarginBottom)
            verticalOffset = (int)(mHourHeight * 24 - getHeight() + dayHeaderHeight + mHeaderMarginBottom);

        mCurrentOrigin.y = -verticalOffset;
        invalidate();
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Interfaces.
    //
    /////////////////////////////////////////////////////////////////

    public interface EventClickListener {
        public void onEventClick(WeekViewEvent event, RectF eventRect);
    }

    public interface MonthChangeListener {
        public List<WeekViewEvent> onMonthChange(int newYear, int newMonth);
    }

    public interface EventLongPressListener {
        public void onEventLongPress(WeekViewEvent event, RectF eventRect);
    }

    public interface EmptyViewClickListener {
        public void onEmptyViewClicked(Calendar time);
        public void onFinishDraggingNewEvent(Calendar time);
        String getEmptyViewTitle();
        String getEmptyViewSubtitle();
    }

    public interface EmptyViewLongPressListener {
        public void onEmptyViewLongPress(Calendar time);
    }

    public interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         * (this will also be called during the first draw of the weekview)
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        public void onFirstVisibleDayChanged(Calendar newFirstVisibleDay, Calendar oldFirstVisibleDay);
    }

    public interface EventListener {
        @Nullable Bitmap getEventHeaderBackgroundTileImage();
        @Nullable Bitmap getEventHeaderImage();
        int getEventHeaderHeight();
        int getEventHeaderImageHeight();
        @Nullable String formatHeaderText(@NonNull WeekViewEvent weekViewEvent);
        void onDestroy();
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Checks if an integer array contains a particular value.
     *
     * @param list  The haystack.
     * @param value The needle.
     * @return True if the array contains the value. Otherwise returns false.
     */
    private boolean containsValue(int[] list, int value) {
        for (int i = 0; i < list.length; i++) {
            if (list[i] == value) return true;
        }
        return false;
    }

    /**
     * Checks if two times are on the same day.
     *
     * @param dayOne The first day.
     * @param dayTwo The second day.
     * @return Whether the times are on the same day.
     */
    private boolean isSameDay(Calendar dayOne, Calendar dayTwo) {
        return dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR) && dayOne.get(Calendar.DAY_OF_YEAR) == dayTwo.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Returns a calendar instance at the start of this day
     *
     * @return the calendar instance
     */
    private Calendar today() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

}
