package za.dams.paracrm.explorer;

import java.util.List;

import za.dams.paracrm.R;
import za.dams.paracrm.explorer.QueryViewActivity.ColumnDesc;
import za.dams.paracrm.explorer.QueryViewActivity.TabGridGetter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.OverScroller;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class QueryView extends View {
	private static final String TAG="Explorer/QueryView" ;
	private static final boolean DEBUG = false ;
	
	private Context mContext ;
	private ViewSwitcher mViewSwitcher ;
    private GestureDetector mGestureDetector;
	private TabGridGetter mTabGridGetter ;
	private int mNumRows ;
	private QueryViewActivity.QueryGridTemplate mQgt ;
	
	private BuildTableTask mBuildTableTask ;
	private View mChildView ;
	private boolean mChildViewInstalled ;
	
	private int mTabIdx = -1 ;
	private int mOffset = -1 ;
	private boolean mOffsetIsFirst ;
	private boolean mOffsetIsLast ;
	public boolean stopDrawing = false ;
	
    private int mViewStartX;
    private int mViewStartY;
    private int mMaxViewStartX;
    private int mThisViewHeight;
    private int mThisViewWidth;
    private int mScrollStartX;
    private int mPreviousDirection;
    
    // pre-allocation
    private Rect mDestRect = new Rect();
	
	
	public interface Callback {
		public void onChildViewInstalled() ;
	}
	private Callback mCallback ;
	public void setCallback( Callback callback ) {
		mCallback = callback ;
	}
	
    @Override
    protected void onAttachedToWindow() {
        if (mHandler == null) {
            mHandler = getHandler();
        }
    }

	
	public QueryView(Context context, ViewSwitcher viewSwitcher, TabGridGetter tabGridGetter, int numRows) {
		super(context) ;
		mContext = context ;
		mViewSwitcher = viewSwitcher ;
		mTabGridGetter = tabGridGetter ;
		mNumRows = numRows ;
		mQgt = mTabGridGetter.getQueryGridTemplate() ;
		
        mGestureDetector = new GestureDetector(context, new QueryViewGestureListener());
        
        mScroller = new OverScroller(context);
        OVERFLING_DISTANCE = ViewConfiguration.get(context).getScaledOverflingDistance();
        
        init(context);
	}
    private void init(Context context) {
    	setWillNotDraw(false) ;
    }
	public void setTabAndOffset( int tabIdx, int offset ) {
		// When setting parameters for a child view, set view offsets to zero 
		mViewStartX = 0 ;
		mViewStartY = 0 ;
		
		if( mTabIdx==tabIdx && mOffset==offset ) {
			// The same: do nothing but fake install event
			mChildViewInstalled = true ;
			invalidate() ;
			return ;
		}
		
		mTabIdx = tabIdx ;
		mOffset = offset ;
		if( offset + mNumRows >= mTabGridGetter.getTabCount(tabIdx) ) {
			mOffsetIsLast = true ;
		} else {
			mOffsetIsLast = false ;
		}
		if( offset == 0 ) {
			mOffsetIsFirst = true ;
		} else {
			mOffsetIsFirst = false ;
		}
		
		if( mBuildTableTask != null && mBuildTableTask.getStatus() != AsyncTask.Status.FINISHED ) {
			mBuildTableTask.cancel(true) ;
		}
		mBuildTableTask = new BuildTableTask() ;
		mBuildTableTask.execute() ;
	}
	public void manualDestroy() {
		mTabIdx = -1 ;
		mOffset = -1 ;		
		mChildView = null ;
	}
	private View buildTableView() {
		LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
		
		List<QueryViewActivity.ColumnDesc> columnsDesc = mTabGridGetter.getTabColumns(mTabIdx) ;
		List<List<String>> dataGrid = mTabGridGetter.getTabRows(mTabIdx, mOffset, mNumRows) ;
	
		ViewGroup tableView = (ViewGroup)mInflater.inflate(R.layout.explorer_viewer_table, null) ;
		
		TableLayout table = (TableLayout)tableView.findViewById(R.id.table) ;
		TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
		for( ColumnDesc cd : columnsDesc ) {
			TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
			tv.setText(cd.text) ;
			if( cd.text_italic ) {
				tv.setTypeface(null, Typeface.BOLD) ;
			} else if( cd.text_bold ) {
				tv.setTypeface(null, Typeface.ITALIC) ;
			}
			tv.setGravity(Gravity.LEFT) ;
			tr.addView(tv) ;
		}
		if( mQgt.template_is_on ) {
			tr.setBackgroundColor(mQgt.colorhex_columns) ;
		}
		table.addView(tr) ;

		// iteration sur la data
		int cnt=0 ;
		for( List<String> dataRow : dataGrid ) {
			View v = new View(mContext);
			v.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
			v.setBackgroundColor(Color.BLACK) ;
			// v.setVisibility(View.VISIBLE) ;
			table.addView(v) ;

			cnt++ ;
			TableRow trData = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
			if( mQgt.template_is_on ) {
				trData.setBackgroundColor((cnt%2==0)?mQgt.colorhex_row:mQgt.colorhex_row_alt) ;
			}
			int i = -1 ;
			for( ColumnDesc cd : columnsDesc ) {
				i++ ;
				String s = dataRow.get(i) ;
				TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
				tv.setText(s) ;
				if( cd.is_bold ) {
					tv.setTypeface(null, Typeface.BOLD) ;
				}
				if( (cd.detachedColumn || cd.progressColumn) ) {
					if( mQgt.data_progress_is_bold ) {
						tv.setTypeface(null, Typeface.BOLD) ;
					}
				} else {
					if( mQgt.data_select_is_bold ) {
						tv.setTypeface(null, Typeface.BOLD) ;
					}
				}
				if( !cd.progressColumn && !cd.is_bold ) {
					tv.setGravity(Gravity.CENTER) ;
				} else {
					tv.setGravity(Gravity.LEFT) ;
				}

				if( cd.progressColumn && mQgt.template_is_on ) {
					try{
						if( Float.parseFloat(s) > 0 ) {
							tv.setTextColor(mQgt.color_green) ;
						}
						else if( Float.parseFloat(s) < 0 ) {
							tv.setTextColor(mQgt.color_red) ;
						}
						else if( Float.parseFloat(s) == 0 ) {
							tv.setText("=") ;
						}
					} catch( NumberFormatException e ) {

					}
				}

				trData.addView(tv) ;
			}
			table.addView(trData) ;
		}
		
		
		// ****** Préparation de la childView pour un View.draw() direct *********
		// - measure : dimensions de la table interne
		// - layout : positionnement des child (pas important car aucun child n'est absolu (tableRows , tableCells)
		// http://stackoverflow.com/questions/4960753/android-problems-converting-viewgroup-with-children-into-bitmap
		
		tableView.setLayoutParams(new ViewGroup.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)) ;

		tableView.measure(MeasureSpec.makeMeasureSpec(tableView.getLayoutParams().width, MeasureSpec.EXACTLY),
		        MeasureSpec.makeMeasureSpec(tableView.getLayoutParams().height, MeasureSpec.EXACTLY));
		
  		tableView.layout(0, 0, tableView.getMeasuredWidth(), tableView.getMeasuredHeight());
		
		// Log.w(TAG,"TableView created :") ;
		// Log.w(TAG,"MeasureWidth: "+tableView.getMeasuredWidth()+" MeasuredHeight:"+tableView.getMeasuredHeight()) ;
  		return tableView ;
	}
	
	
	private class BuildTableTask extends AsyncTask<Void,Void,Void> {
		View tableView = null ;

		@Override
		protected void onPreExecute() {
			mChildView = null ;
			invalidate() ;
		}
		@Override
		protected Void doInBackground(Void... arg0) {
			tableView = buildTableView() ;
			return null;
		}
		@Override
		protected void onPostExecute(Void arg0) {
			if( this.isCancelled() ) {
				return ;
			}
			mChildView = tableView ;
			
			int tWidthAvailableForDataView = mThisViewWidth ;
			// If (data)child view bigger than space avail for it
			//  => calculate max offset
			if( mChildView.getMeasuredWidth() > tWidthAvailableForDataView ) {
				mMaxViewStartX = mChildView.getMeasuredWidth() - tWidthAvailableForDataView ;
			} else {
				mMaxViewStartX = 0 ;
			}
			
			
			invalidate() ;
			mChildViewInstalled = true ;
		}
	}
	
	
	
	private void initNextView(int pageShift) {
		QueryView view = (QueryView) mViewSwitcher.getNextView();
		
		view.layout(getLeft(), getTop(), getRight(), getBottom());
		
		int offsetShift ;
		if( (mOffsetIsFirst && pageShift < 0) || (mOffsetIsLast && pageShift > 0) ) {
			offsetShift = 0 ;
		} else {
			offsetShift = pageShift * mNumRows ;
		}
		view.setTabAndOffset(mTabIdx, mOffset+offsetShift) ;
		view.mViewStartX = mViewStartX ;
	}
	
	
	
    @Override
    protected void onDraw(Canvas canvas) {
    	if( mChildView == null ) {
    		return ;
    	}
    	if( mViewStartX > mMaxViewStartX ) {
    		mViewStartX = mMaxViewStartX ;
    	}
    	
    	
    	canvas.save() ;
    	
        float xTranslate = -mViewStartX;
        // offset canvas by the current drag and header position
        canvas.translate(xTranslate, -mViewStartY);
        // clip to the data area
        Rect dest = mDestRect;
        int mDataStartX = 0 ; //@DAMS toAdd
        dest.top = 0;
        dest.bottom = mThisViewHeight ;
        dest.left = (int) (mDataStartX - xTranslate);
        dest.right = (int) (mThisViewWidth - xTranslate);
        canvas.save();
        canvas.clipRect(dest);
        
        // Draw the movable part of the view
        mChildView.draw(canvas) ;
        // restore to having no clip
    	canvas.restore() ;
    	
    	
    	// Next view (VSCROLL)
        if (mViewStartY != 0) {
            float yTranslate;
            if (mViewStartY > 0) {
                yTranslate = mThisViewHeight;
            } else {
            	yTranslate = -mThisViewHeight;
            }
            // Move the canvas around to prep it for the next view
            // specifically, shift it by a screen and undo the
            // xTranslation which will be redone in the nextView's onDraw().
            canvas.translate(-xTranslate, yTranslate);
            QueryView nextView = (QueryView) mViewSwitcher.getNextView();

            // Prevent infinite recursive calls to onDraw().
            nextView.mViewStartY = 0 ;

            nextView.onDraw(canvas);
            // Move it back for this view
            canvas.translate(0, -yTranslate);
        } else {
            // If we drew another view we already translated it back
            // If we didn't draw another view we should be at the edge of the
            // screen
            canvas.translate(-xTranslate, mViewStartY);
        }

    	if( mChildViewInstalled && mCallback != null ) {
    		mCallback.onChildViewInstalled() ;
    	}
    	mChildViewInstalled = false ;
    }
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        mThisViewWidth = width;
        mThisViewHeight = height;
    }    

    
    /**
     * The initial state of the touch mode when we enter this view.
     */
    private static final int TOUCH_MODE_INITIAL_STATE = 0;

    /**
     * Indicates we just received the touch event and we are waiting to see if
     * it is a tap or a scroll gesture.
     */
    private static final int TOUCH_MODE_DOWN = 1;

    /**
     * Indicates the touch gesture is a vertical scroll
     */
    private static final int TOUCH_MODE_VSCROLL = 0x20;

    /**
     * Indicates the touch gesture is a horizontal scroll
     */
    private static final int TOUCH_MODE_HSCROLL = 0x40;
    
    /**
     * Persistent touch status for a complete DOWN/UP sequence 
     */
    private int mTouchMode = TOUCH_MODE_INITIAL_STATE;
	
    /**
     * Flag to decide whether to handle the up event. Cases where up events
     * should be ignored are 1) right after a scale gesture and 2) finger was
     * down before app launch
     */
    private boolean mHandleActionUp = true ;
    
    private boolean mOnFlingCalled;
    private boolean mScrolling = false;
    private boolean mStartingScroll = false;
    protected boolean mPaused = true;
    private Handler mHandler;
    private OverScroller mScroller;
    
    private final int OVERFLING_DISTANCE;
    private static int mVerticalSnapBackThreshold = 256;

    private float mInitialScrollX;
    private float mInitialScrollY;
    
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (DEBUG) Log.e(TAG, "" + action + " ev.getPointerCount() = " + ev.getPointerCount());

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mStartingScroll = true;
                if (DEBUG) {
                    Log.e(TAG, "ACTION_DOWN ev.getDownTime = " + ev.getDownTime() + " Cnt="
                            + ev.getPointerCount());
                }

                mHandleActionUp = true;
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (DEBUG) Log.e(TAG, "ACTION_MOVE Cnt=" + ev.getPointerCount() + QueryView.this);
                mGestureDetector.onTouchEvent(ev);
                return true;

            case MotionEvent.ACTION_UP:
                if (DEBUG) Log.e(TAG, "ACTION_UP Cnt=" + ev.getPointerCount() + mHandleActionUp);
                // @DAMS
                //mEdgeEffectTop.onRelease();
                //mEdgeEffectBottom.onRelease();
                mStartingScroll = false;
                mGestureDetector.onTouchEvent(ev);
                if (!mHandleActionUp) {
                    mHandleActionUp = true;
                    mViewStartY = 0;
                    invalidate();
                    return true;
                }
                if (mOnFlingCalled) {
                    return true;
                }
                if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
                    mTouchMode = TOUCH_MODE_INITIAL_STATE;
                    if (Math.abs(mViewStartY) > mVerticalSnapBackThreshold) {
                        // The user has gone beyond the threshold so switch views
                        if (DEBUG) Log.d(TAG, "- vertical scroll: switch views");
                        switchViews(mViewStartY > 0, mViewStartY, mThisViewHeight, 0);
                        mViewStartY = 0;
                        return true;
                    } else {
                        // Not beyond the threshold so invalidate which will cause
                        // the view to snap back. Also call recalc() to ensure
                        // that we have the correct starting date and title.
                        if (DEBUG) Log.d(TAG, "- vertical scroll: snap back");
                        /*
                        invalidate();
                        mViewStartY = 0;
                        */
                        mScrolling = true;
                        mScroller.startScroll(mViewStartX, mViewStartY, 0, -mViewStartY,400);
                        mHandler.post(mContinueScroll);
                        return true ;
                    }
                }

                // If we were scrolling, then reset the selected hour so that it
                // is visible.
                if (mScrolling) {
                    mScrolling = false;
                    invalidate();
                }
                return true;

                // This case isn't expected to happen.
            case MotionEvent.ACTION_CANCEL:
                if (DEBUG) Log.e(TAG, "ACTION_CANCEL");
                mGestureDetector.onTouchEvent(ev);
                mScrolling = false;
                return true;

            default:
                if (DEBUG) Log.e(TAG, "Not MotionEvent " + ev.toString());
                if (mGestureDetector.onTouchEvent(ev)) {
                    return true;
                }
                return super.onTouchEvent(ev);
        }
	}
	
    // Encapsulates the code to continue the scrolling after the
    // finger is lifted. Instead of stopping the scroll immediately,
    // the scroll continues to "free spin" and gradually slows down.
    private ContinueScroll mContinueScroll = new ContinueScroll();
    private class ContinueScroll implements Runnable {
        public void run() {
            mScrolling = mScrolling && mScroller.computeScrollOffset();
            if (!mScrolling ) {
                invalidate();
                return;
            }

            mViewStartX = mScroller.getCurrX();
            mViewStartY = mScroller.getCurrY();

            // @DAMS
            /*
            if (mCallEdgeEffectOnAbsorb) {
                if (mViewStartY < 0) {
                    mEdgeEffectTop.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                } else if (mViewStartY > mMaxViewStartY) {
                    mEdgeEffectBottom.onAbsorb((int) mLastVelocity);
                    mCallEdgeEffectOnAbsorb = false;
                }
                mLastVelocity = mScroller.getCurrVelocity();
            }
            */

            if (mScrollStartX == 0 || mScrollStartX == mMaxViewStartX) {
                // Allow overscroll/springback only on a fling,
                // not a pull/fling from the end
                if (mViewStartX < 0) {
                    mViewStartX = 0;
                } else if (mViewStartX > mMaxViewStartX) {
                    mViewStartX = mMaxViewStartX;
                }
            }

            mHandler.post(this);
            invalidate();
        }
    }
    class QueryViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onSingleTapUp");
            //QueryView.this.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onLongPress");
            //QueryView.this.doLongPress(ev);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onScroll");
            QueryView.this.doScroll(e1, e2, distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onFling");
            QueryView.this.doFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onDown");
            QueryView.this.doDown(ev);
            return true;
        }
    }
    // The following routines are called from the parent activity when certain
    // touch events occur.
    private void doDown(MotionEvent ev) {
        mTouchMode = TOUCH_MODE_DOWN;
        mViewStartY = 0;
        mOnFlingCalled = false;
        mHandler.removeCallbacks(mContinueScroll);
    }
    private void doScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
        cancelAnimation();
        if (mStartingScroll) {
            mInitialScrollX = 0;
            mInitialScrollY = 0;
            mStartingScroll = false;
        }

        mInitialScrollX += deltaX;
        mInitialScrollY += deltaY;
        int distanceX = (int) mInitialScrollX;
        int distanceY = (int) mInitialScrollY;

        // If we haven't figured out the predominant scroll direction yet,
        // then do it now.
        if (mTouchMode == TOUCH_MODE_DOWN) {
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            mScrollStartX = mViewStartX;
            mPreviousDirection = 0;

            if (absDistanceY > absDistanceX) {
            	mTouchMode = TOUCH_MODE_VSCROLL;
            	int direction = (distanceY > 0) ? 1 : -1;
            	mPreviousDirection = direction ;
            	if( (direction > 0 && mOffsetIsLast) || (direction < 0 && mOffsetIsFirst) ) {
            		return ;
            	}
            	mViewStartY = distanceY;
            	initNextView( direction );
            } else {
            	mTouchMode = TOUCH_MODE_HSCROLL;
            }
        } else if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
            // We are already scrolling horizontally, so check if we
            // changed the direction of scrolling so that the other week
            // is now visible.
        	if (distanceY == 0) {
        		return ;
        	}
        	int direction = (distanceY > 0) ? 1 : -1;
        	if( (direction > 0 && mOffsetIsLast) || (direction < 0 && mOffsetIsFirst) ) {
        		return ;
        	}
            mViewStartY = distanceY;
            if (direction != mPreviousDirection) {
            	// The user has switched the direction of scrolling
            	// so re-init the next view
            	initNextView( direction );
            	mPreviousDirection = direction;
            }
        }

        if ((mTouchMode & TOUCH_MODE_HSCROLL) != 0) {
            mViewStartX = mScrollStartX + distanceX;

            // If dragging while already at the end, do a glow
            // @DAMS
            /*
            final int pulledToY = (int) (mScrollStartY + deltaY);
            if (pulledToY < 0) {
                mEdgeEffectTop.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectBottom.isFinished()) {
                    mEdgeEffectBottom.onRelease();
                }
            } else if (pulledToY > mMaxViewStartY) {
                mEdgeEffectBottom.onPull(deltaY / mViewHeight);
                if (!mEdgeEffectTop.isFinished()) {
                    mEdgeEffectTop.onRelease();
                }
            }
            */

            if (mViewStartX < 0) {
                mViewStartX = 0;
            } else if (mViewStartX > mMaxViewStartX) {
                mViewStartX = mMaxViewStartX;
            }
        }

        mScrolling = true;

        invalidate();
    }

    private void cancelAnimation() {
        Animation in = mViewSwitcher.getInAnimation();
        if (in != null) {
            // cancel() doesn't terminate cleanly.
            in.scaleCurrentDuration(0);
        }
        Animation out = mViewSwitcher.getOutAnimation();
        if (out != null) {
            // cancel() doesn't terminate cleanly.
            out.scaleCurrentDuration(0);
        }
    }

    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        cancelAnimation();

        if ((mTouchMode & TOUCH_MODE_VSCROLL) != 0) {
            // Vertical fling.
            // initNextView(deltaX);
            if (DEBUG) Log.d(TAG, "doFling: velocityY " + velocityY);
            int deltaY = (int) e2.getY() - (int) e1.getY();
            if( mViewStartY == 0 ) {
            	return ;
            }
            int directionScroll = (mViewStartY < 0)? 1:-1 ;
            int directionFling = (velocityY > 0)? 1:-1 ;
            if( directionScroll != directionFling ){
            	return ;
            }
        	if( (deltaY < 0 && mOffsetIsLast) || (deltaY > 0 && mOffsetIsFirst) ) {
        		return ;
        	}
        	mOnFlingCalled = true;
        	mTouchMode = TOUCH_MODE_INITIAL_STATE;
            switchViews(deltaY < 0, mViewStartY, mThisViewHeight, velocityY);
            mViewStartY = 0;
            return;
        }

        if ((mTouchMode & TOUCH_MODE_HSCROLL) == 0) {
            if (DEBUG) Log.d(TAG, "doFling: no fling");
            return;
        }

        // Horizontal fling > freespin scroll.
        mOnFlingCalled = true;
        mTouchMode = TOUCH_MODE_INITIAL_STATE;
        mViewStartY = 0;

        if (DEBUG) {
            Log.d(TAG, "doFling: mViewStartX" + mViewStartX + " velocityX " + velocityX);
        }

        // Continue scrolling vertically
        mScrolling = true;
        mScroller.fling(mViewStartX /* startX */, 0 /* startY */, (int) -velocityX /* velocityX */,
                0 /* velocityY */, 0 /* minX */, mMaxViewStartX /* maxX */, 0 /* minY */,
                0 /* maxY */, OVERFLING_DISTANCE, OVERFLING_DISTANCE);

        /*
        // When flinging down, show a glow when it hits the end only if it
        // wasn't started at the top
        if (velocityY > 0 && mViewStartY != 0) {
            mCallEdgeEffectOnAbsorb = true;
        }
        // When flinging up, show a glow when it hits the end only if it wasn't
        // started at the bottom
        else if (velocityY < 0 && mViewStartY != mMaxViewStartY) {
            mCallEdgeEffectOnAbsorb = true;
        }
        */
        mHandler.post(mContinueScroll);
    }

    private View switchViews(boolean forward, float yOffSet, float height, float velocity) {
        float mAnimationDistance = height - yOffSet;
        if (DEBUG) {
            Log.d(TAG, "switchViews(" + forward + ") O:" + yOffSet + " Dist:" + mAnimationDistance);
        }

        float progress = Math.abs(yOffSet) / height;
        if (progress > 1.0f) {
            progress = 1.0f;
        }

        float inFromYValue, inToYValue;
        float outFromYValue, outToYValue;
        if (forward) {
            inFromYValue = 1.0f - progress;
            inToYValue = 0.0f;
            outFromYValue = -progress;
            outToYValue = -1.0f;
        } else {
            inFromYValue = progress - 1.0f;
            inToYValue = 0.0f;
            outFromYValue = progress;
            outToYValue = 1.0f;
        }

        // We have to allocate these animation objects each time we switch views
        // because that is the only way to set the animation parameters.
        TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f,
                Animation.RELATIVE_TO_SELF, inFromYValue,
                Animation.RELATIVE_TO_SELF, inToYValue);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f,
                Animation.RELATIVE_TO_SELF, outFromYValue,
                Animation.RELATIVE_TO_SELF, outToYValue);

        int duration = (int)(400 * ( height - Math.abs(yOffSet) ) / height)  ;
        inAnimation.setDuration(duration);
        //inAnimation.setInterpolator(mHScrollInterpolator);
        //outAnimation.setInterpolator(mHScrollInterpolator);
        outAnimation.setDuration(duration);
        mViewSwitcher.setInAnimation(inAnimation);
        mViewSwitcher.setOutAnimation(outAnimation);

        QueryView view = (QueryView) mViewSwitcher.getCurrentView();
        mViewSwitcher.showNext();
        view = (QueryView) mViewSwitcher.getCurrentView();
        view.requestFocus();

        return view;
    }


	
}
