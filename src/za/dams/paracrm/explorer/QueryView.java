package za.dams.paracrm.explorer;

import java.util.List;

import za.dams.paracrm.R;
import za.dams.paracrm.explorer.QueryViewActivity.ColumnDesc;
import za.dams.paracrm.explorer.QueryViewActivity.TabGridGetter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class QueryView extends View {
	private static final String TAG="Explorer/QueryView" ;
	private static final boolean DEBUG = true ;
	
	private Context mContext ;
	private ViewSwitcher mViewSwitcher ;
    private GestureDetector mGestureDetector;
	private TabGridGetter mTabGridGetter ;
	private int mNumRows ;
	private QueryViewActivity.QueryGridTemplate mQgt ;
	
	private ViewGroup mChildView ;
	
	private int mTabIdx ;
	private int mOffset ;
	public boolean stopDrawing = false ;
	
	public QueryView(Context context, ViewSwitcher viewSwitcher, TabGridGetter tabGridGetter, int numRows) {
		super(context) ;
		mContext = context ;
		mViewSwitcher = viewSwitcher ;
		mTabGridGetter = tabGridGetter ;
		mNumRows = numRows ;
		mQgt = mTabGridGetter.getQueryGridTemplate() ;
		
        mGestureDetector = new GestureDetector(context, new QueryViewGestureListener());
        
        init(context);
	}
    private void init(Context context) {
    	setWillNotDraw(false) ;
    }
	public void setTabAndOffset( int tabIdx, int offset ) {
		mTabIdx = tabIdx ;
		mOffset = offset ;
		
		buildTableView() ;
		invalidate() ;
	}
	public void manualDestroy() {
		mTabIdx = -1 ;
		mOffset = -1 ;		
		mChildView = null ;
	}
	private void buildTableView() {
		LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ;
		
		List<QueryViewActivity.ColumnDesc> columnsDesc = mTabGridGetter.getTabColumns(mTabIdx) ;
		List<List<String>> dataGrid = mTabGridGetter.getTabRows(mTabIdx, mOffset, mNumRows) ;
	
		View tableView = mInflater.inflate(R.layout.explorer_viewer_table, null) ;
		
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
		
		mChildView = (ViewGroup)tableView ;
		
		mChildView.setLayoutParams(new ViewGroup.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)) ;

  		mChildView.measure(MeasureSpec.makeMeasureSpec(mChildView.getLayoutParams().width, MeasureSpec.EXACTLY),
		        MeasureSpec.makeMeasureSpec(mChildView.getLayoutParams().height, MeasureSpec.EXACTLY));
		
  		mChildView.layout(0, 0, mChildView.getMeasuredWidth(), mChildView.getMeasuredHeight());
		
		// Log.w(TAG,"TableView created :") ;
		// Log.w(TAG,"MeasureWidth: "+mChildView.getMeasuredWidth()+" MeasuredHeight:"+mChildView.getMeasuredHeight()) ;
	}
	
	
	
	private void initNextView(int deltaY) {
		QueryView view = (QueryView) mViewSwitcher.getNextView();
		view.setTabAndOffset(mTabIdx, mOffset+mNumRows) ;
        view.layout(getLeft(), getTop(), getRight(), getBottom());
        invalidate() ;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
    	Log.w(TAG,"Test222!!") ;
        mGestureDetector.onTouchEvent(event);
        return true;    	
	}
	
	
    @Override
    protected void onDraw(Canvas canvas) {
    	if( mChildView == null ) {
    		return ;
    	}
    	
    	canvas.save() ;
   		mChildView.draw(canvas) ;
    	canvas.restore() ;
    	
    	/*
    	Paint mPaint = new Paint();
    	mPaint.setDither(true);
    	mPaint.setColor(0xFFFFFF00);
    	mPaint.setStyle(Paint.Style.STROKE);
    	mPaint.setStrokeJoin(Paint.Join.ROUND);
    	mPaint.setStrokeCap(Paint.Cap.ROUND);
    	mPaint.setStrokeWidth(3);    	  
    	canvas.drawCircle(200, 200, 100, mPaint) ;
    	*/

    	if( mViewSwitcher.getNextView() != this ) {
    		QueryView nextView = (QueryView) mViewSwitcher.getNextView();
    		nextView.stopDrawing = true ;
    		nextView.draw(canvas) ;
    	}
    }
    

	
    class QueryViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onSingleTapUp");
            //DayView.this.doSingleTapUp(ev);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onLongPress");
            //DayView.this.doLongPress(ev);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onScroll");
            invalidate() ;
            return false ;
            //DayView.this.doScroll(e1, e2, distanceX, distanceY);
            //return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onFling");

            //DayView.this.doFling(e1, e2, velocityX, velocityY);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            if (DEBUG) Log.e(TAG, "GestureDetector.onDown");
            QueryView.this.initNextView(3) ;
            //DayView.this.doDown(ev);
            return true;
        }
    }



	
}
