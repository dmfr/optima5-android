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
	private String TAG="Explorer/QueryView" ;
	private static final boolean DEBUG = false ;
	
	private Context mContext ;
	private LayoutInflater mInflater ;
	private ViewSwitcher mViewSwitcher ;
    private GestureDetector mGestureDetector;
	private TabGridGetter mTabGridGetter ;
	private int mNumRows ;
	private QueryViewActivity.QueryGridTemplate mQgt ;
	
	private BuildTableTask mBuildTableTask ;
	private View mTableLabelsView ;
	private View mTableDataView ;
	private boolean mTableViewsInstalled ;
	private boolean mTableViewsDoNotify ;
	
	private boolean mPreloadRunning = false ;
	private PreloadAdjacentPagesTask mPreloadAdjacentPagesTask ;
	private int mPreloadTabIdx = -1 ;
	private int mPreloadAdjacentPageIdx = -1 ;
	private View mCachePreviousTableLabelsView ;
	private View mCachePreviousTableDataView ;
	private View mCacheForwardTableLabelsView ;
	private View mCacheForwardTableDataView ;
	
	
	private int mTabIdx = -1 ;
	private int mPageIdx = -1 ;
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
    
    
    protected QueryView getViewSwitcherAlternateView() {
    	if( (QueryView)mViewSwitcher.getCurrentView() == this ) {
    		return (QueryView)mViewSwitcher.getNextView() ;
    	} else {
    		return (QueryView)mViewSwitcher.getCurrentView() ;
    	}
    }

	public QueryView(Context context, ViewSwitcher viewSwitcher, TabGridGetter tabGridGetter, int numRows) {
		super(context) ;
		mContext = context ;
		mViewSwitcher = viewSwitcher ;
		mTabGridGetter = tabGridGetter ;
		mNumRows = numRows ;
		mQgt = mTabGridGetter.getQueryGridTemplate() ;
		
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
		
        mGestureDetector = new GestureDetector(context, new QueryViewGestureListener());
        
        mScroller = new OverScroller(context);
        OVERFLING_DISTANCE = ViewConfiguration.get(context).getScaledOverflingDistance();
        
        init(context);
	}
    private void init(Context context) {
    	setWillNotDraw(false) ;
    }
    public void preloadAdjacentForTabAndPage( int tabIdx, int pageIdx ) {
		// Dernier recours => chargement à la volée
		if( mPreloadAdjacentPagesTask != null && mPreloadAdjacentPagesTask.getStatus() != AsyncTask.Status.FINISHED ) {
			if( tabIdx==mPreloadTabIdx && mPreloadAdjacentPageIdx==pageIdx ) {
				//déja en cours > rien à faire
				return ;
			}
			mPreloadAdjacentPagesTask.cancel(true) ;
		}
		mPreloadTabIdx = tabIdx ;
		mPreloadAdjacentPageIdx = pageIdx ;
		mPreloadAdjacentPagesTask = new PreloadAdjacentPagesTask();
		mPreloadAdjacentPagesTask.execute();
    }
	public void setTabAndOffset( int tabIdx, int pageIdx ) {
		// When setting parameters for a child view, set view offsets to zero 
		mViewStartX = 0 ;
		mViewStartY = 0 ;
		
		if( mTabIdx==tabIdx && mPageIdx==pageIdx && mTableViewsInstalled ) {
			// The same: do nothing but fake install event
			mTableViewsDoNotify = true ;
			invalidate() ;
			return ;
		}
		
		mTabIdx = tabIdx ;
		mPageIdx = pageIdx ;
		mTableViewsInstalled = false ;
		
		int nbPages = (int) Math.ceil( (double)mTabGridGetter.getTabCount(tabIdx) / mNumRows ) ;
		if( mPageIdx >= nbPages - 1 ) {
			mOffsetIsLast = true ;
		} else {
			mOffsetIsLast = false ;
		}
		if( mPageIdx == 0 ) {
			mOffsetIsFirst = true ;
		} else {
			mOffsetIsFirst = false ;
		}
		
		// Présence dans le preload ??
		int preloadDirection = pageIdx - mPreloadAdjacentPageIdx ;
		//Log.w(TAG,"Current preload is tab="+mPreloadTabIdx+" pageMed="+mPreloadAdjacentPageIdx) ;
		if( tabIdx==mPreloadTabIdx && Math.abs(preloadDirection) == 1 ) {
			if( mPreloadRunning ) {
				// Sera installé à la fin du preload
				//Log.w(TAG,"Preload late !!") ;
			} else {
				// Installation manuelle
				if( preloadDirection == -1 ) {
					installTableViewsOnUI( mCachePreviousTableLabelsView , mCachePreviousTableDataView ) ;
				} else if( preloadDirection == 1 ) {
					installTableViewsOnUI( mCacheForwardTableLabelsView , mCacheForwardTableDataView ) ;
				}
			}
		} else {
			// Dernier recours => chargement à la volée
			if( mBuildTableTask != null && mBuildTableTask.getStatus() != AsyncTask.Status.FINISHED ) {
				mBuildTableTask.cancel(true) ;
			}
			mBuildTableTask = new BuildTableTask() ;
			mBuildTableTask.execute() ;
		}
		
		// Preload en cascade de la page suivante (l'autre vue)
		getViewSwitcherAlternateView().preloadAdjacentForTabAndPage( mTabIdx, mPageIdx ) ;
	}
	public void manualDestroy() {
		mTabIdx = -1 ;
		mPageIdx = -1 ;		
		mTableLabelsView = null ;
		mTableDataView = null ;
	}
	
	
	private void initTableView( ViewGroup tableView ) {
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
	}
	private View getTableCell( ColumnDesc cd ) {
		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
		tv.setText(cd.text) ;
		if( cd.text_italic ) {
			tv.setTypeface(null, Typeface.BOLD) ;
		} else if( cd.text_bold ) {
			tv.setTypeface(null, Typeface.ITALIC) ;
		}
		tv.setGravity(Gravity.LEFT) ;
		return tv ;
	}
	private View getTableCell( ColumnDesc cd, String dataStr ) {
		TextView tv = (TextView)mInflater.inflate(R.layout.explorer_viewer_table_cell, null) ;
		tv.setText(dataStr) ;
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
				if( Float.parseFloat(dataStr) > 0 ) {
					tv.setTextColor(mQgt.color_green) ;
				}
				else if( Float.parseFloat(dataStr) < 0 ) {
					tv.setTextColor(mQgt.color_red) ;
				}
				else if( Float.parseFloat(dataStr) == 0 ) {
					tv.setText("=") ;
				}
			} catch( NumberFormatException e ) {

			}
		}
		return tv ;
	}
	private View buildTableLabelsView( int tabIdx, int pageIdx ) {
		List<QueryViewActivity.ColumnDesc> columnsDesc = mTabGridGetter.getTabColumns(tabIdx) ;
		List<List<String>> dataGrid = mTabGridGetter.getTabRows(tabIdx, pageIdx*mNumRows, mNumRows) ;
	
		ViewGroup tableView = (ViewGroup)mInflater.inflate(R.layout.explorer_viewer_table, null) ;
		
		TableLayout table = (TableLayout)tableView.findViewById(R.id.table) ;
		TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
		for( ColumnDesc cd : columnsDesc ) {
			if( !cd.is_bold ) {
				continue ;
			}
			tr.addView(getTableCell(cd)) ;
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
				if( !cd.is_bold ) {
					continue ;
				}
				String s = dataRow.get(i) ;
				trData.addView(getTableCell(cd,s)) ;
			}
			table.addView(trData) ;
		}
		
		initTableView(tableView) ;
  		return tableView ;
	}
	private View buildTableDataView( int tabIdx, int pageIdx ) {
		List<QueryViewActivity.ColumnDesc> columnsDesc = mTabGridGetter.getTabColumns(tabIdx) ;
		List<List<String>> dataGrid = mTabGridGetter.getTabRows(tabIdx, pageIdx*mNumRows, mNumRows) ;
	
		ViewGroup tableView = (ViewGroup)mInflater.inflate(R.layout.explorer_viewer_table, null) ;
		
		TableLayout table = (TableLayout)tableView.findViewById(R.id.table) ;
		TableRow tr = (TableRow)mInflater.inflate(R.layout.explorer_viewer_table_row, null) ;
		for( ColumnDesc cd : columnsDesc ) {
			if( cd.is_bold ) {
				continue ;
			}
			
			tr.addView(getTableCell(cd)) ;
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
				if( cd.is_bold ) {
					continue ;
				}
				String s = dataRow.get(i) ;
				trData.addView(getTableCell(cd,s)) ;
			}
			table.addView(trData) ;
		}
		
		initTableView(tableView) ;
  		return tableView ;
	}
	
	
	
	private class PreloadAdjacentPagesTask extends AsyncTask<Void,Void,Void> {
		@Override
		protected void onPreExecute() {
			mPreloadRunning = true ;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// num pages ?
			int tCountPages = (int)Math.ceil( mTabGridGetter.getTabCount(mPreloadTabIdx) / mNumRows ) ;
			if( mPreloadAdjacentPageIdx < tCountPages ) {
				// FORWARD !
				int targetPageForward = mPreloadAdjacentPageIdx + 1 ;
				if( mTabIdx == mPreloadTabIdx && mPageIdx == targetPageForward && mTableViewsInstalled ) {
					// recup de la page active
					//Log.w(TAG,"Preload recup tab="+mPreloadTabIdx+" page="+targetPageForward) ;
					mCacheForwardTableLabelsView = mTableLabelsView ;
					mCacheForwardTableDataView = mTableDataView ;
				} else {
					//Log.w(TAG,"Preload build tab="+mPreloadTabIdx+" page="+targetPageForward) ;
					mCacheForwardTableLabelsView = buildTableLabelsView( mPreloadTabIdx , targetPageForward );
					mCacheForwardTableDataView = buildTableDataView( mPreloadTabIdx , targetPageForward );
				}
				
				
			}
			if( mPreloadAdjacentPageIdx > 0 ) {
				// PREVIOUS !
				int targetPagePrevious = mPreloadAdjacentPageIdx - 1 ;
				if( mTabIdx == mPreloadTabIdx && mPageIdx == targetPagePrevious && mTableViewsInstalled ) {
					// recup de la page active
					//Log.w(TAG,"Recup tab="+mPreloadTabIdx+" page="+targetPagePrevious) ;
					mCachePreviousTableLabelsView = mTableLabelsView ;
					mCachePreviousTableDataView = mTableDataView ;
				} else {
					//Log.w(TAG,"Preload build tab="+mPreloadTabIdx+" page="+targetPagePrevious) ;
					mCachePreviousTableLabelsView = buildTableLabelsView( mPreloadTabIdx , targetPagePrevious );
					mCachePreviousTableDataView = buildTableDataView( mPreloadTabIdx , targetPagePrevious );
				}
			}
			
			return null;
		}
		@Override
		protected void onPostExecute(Void arg0) {
			if( this.isCancelled() ) {
				mPreloadRunning = false ;
				return ;
			}
			
			// Rattrapage de l'affichage demandé à la fin du preload ?
			if( !mTableViewsInstalled && mTabIdx == mPreloadTabIdx && mPageIdx >= 0 ) {
				int preloadDirection = mPageIdx - mPreloadAdjacentPageIdx ;
				if( Math.abs(preloadDirection) == 1 ) {
					//Log.w(TAG,"End of preload : installing to catch up early request") ;
					if( preloadDirection == -1 ) {
						installTableViewsOnUI( mCachePreviousTableLabelsView , mCachePreviousTableDataView ) ;
					} else if( preloadDirection == 1 ) {
						installTableViewsOnUI( mCacheForwardTableLabelsView , mCacheForwardTableDataView ) ;
					}
				}
			}
			
			mPreloadRunning = false ;
		}
	}
	private class BuildTableTask extends AsyncTask<Void,Void,Void> {
		View tableLabelsView = null ;
		View tableDataView = null ;

		@Override
		protected void onPreExecute() {
			mTableLabelsView = null ;
			mTableDataView = null ;
			invalidate() ;
		}
		@Override
		protected Void doInBackground(Void... arg0) {
			tableLabelsView = buildTableLabelsView(mTabIdx,mPageIdx) ;
			tableDataView = buildTableDataView(mTabIdx,mPageIdx) ;
			return null;
		}
		@Override
		protected void onPostExecute(Void arg0) {
			if( this.isCancelled() ) {
				return ;
			}
			installTableViewsOnUI( tableLabelsView, tableDataView ) ;
		}
	}
	private void installTableViewsOnUI( View tableLabelsView, View tableDataView ) {
		if( tableLabelsView == null || tableDataView == null ) {
			return ;
		}
		
		mTableLabelsView = tableLabelsView ;
		mTableDataView = tableDataView ;
		
		int tWidthAvailableForDataView = mThisViewWidth - tableLabelsView.getMeasuredWidth() ;
		// If (data)child view bigger than space avail for it
		//  => calculate max offset
		if( mTableDataView.getMeasuredWidth() > tWidthAvailableForDataView ) {
			mMaxViewStartX = mTableDataView.getMeasuredWidth() - tWidthAvailableForDataView ;
		} else {
			mMaxViewStartX = 0 ;
		}
		
		
		mTableViewsInstalled = true ;
		mTableViewsDoNotify = true ;
		invalidate() ;
	}
	
	
	
	private void initNextView(int pageShift) {
		QueryView view = (QueryView) mViewSwitcher.getNextView();
		
		view.layout(getLeft(), getTop(), getRight(), getBottom());
		
		//int pageShift ;
		if( (mOffsetIsFirst && pageShift < 0) || (mOffsetIsLast && pageShift > 0) ) {
			pageShift = 0 ;
		}
		view.setTabAndOffset(mTabIdx, mPageIdx+pageShift) ;
		//this.preloadAdjacentForTabAndPage( mTabIdx, mPageIdx+pageShift ) ;
		view.mViewStartX = mViewStartX ;
	}
	
	
	
    @Override
    protected void onDraw(Canvas canvas) {
    	if( !mTableViewsInstalled ) {
    		return ;
    	}
    	if( mViewStartX > mMaxViewStartX ) {
    		mViewStartX = mMaxViewStartX ;
    	}
    	
    	
    	canvas.save() ;
    	
        
        // offset canvas by the current vertical drag (paging) position
        canvas.translate(0, -mViewStartY);
        // clip to the static area
        Rect dest = mDestRect;
        dest.top = 0;
        dest.bottom = mThisViewHeight ;
        dest.left = 0;
        dest.right = Math.min(mThisViewWidth, mTableLabelsView.getMeasuredWidth());
        canvas.save();
        canvas.clipRect(dest);        
        // Draw the movable part of the view
        mTableLabelsView.draw(canvas) ;
        // restore to having no clip
    	canvas.restore() ;
        
        
        
        
        
        // movable table part
        int tLabelsWidth = Math.min(mThisViewWidth, mTableLabelsView.getMeasuredWidth()) ; //@DAMS toAdd
        float xTranslate = -mViewStartX + tLabelsWidth;
        canvas.translate(xTranslate, 0);
        // clip to the data area
        Rect destMoveable = mDestRect;
        destMoveable.top = 0;
        destMoveable.bottom = mThisViewHeight ;
        destMoveable.left = (int) (tLabelsWidth - xTranslate);
        destMoveable.right = (int) (mThisViewWidth - xTranslate);
        canvas.save();
        canvas.clipRect(destMoveable);        
        // Draw the movable part of the view
        mTableDataView.draw(canvas) ;
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

    	if( mTableViewsInstalled && mTableViewsDoNotify && mCallback != null ) {
    		if( mCallback != null ) {
    			mCallback.onChildViewInstalled() ;
    		}
    		mTableViewsDoNotify = false ;
    	}   	
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

    private void doFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
