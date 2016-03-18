package za.dams.paracrm.widget;

import za.dams.paracrm.R;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.LayoutParams;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class GridFragment extends Fragment {
	   final private Handler mHandler = new Handler();
	    final private Runnable mRequestFocus = new Runnable() {
	        public void run() {
	            mGrid.focusableViewAvailable(mGrid);
	        }
	    };
	    
	    final private AdapterView.OnItemClickListener mOnClickListener
	            = new AdapterView.OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            onGridItemClick((GridView)parent, v, position, id);
	        }
	    };
	    ListAdapter mAdapter;
	    GridView mGrid;
	    View mEmptyView;
	    TextView mStandardEmptyView;
	    View mProgressContainer;
	    View mGridContainer;
	    CharSequence mEmptyText;
	    boolean mGridShown;
	    public GridFragment() {
	    }
	    /**
	     * Provide default implementation to return a simple list view.  Subclasses
	     * can override to replace with their own layout.  If doing so, the
	     * returned view hierarchy <em>must</em> have a ListView whose id
	     * is {@link android.R.id#list android.R.id.list} and can optionally
	     * have a sibling view id {@link android.R.id#empty android.R.id.empty}
	     * that is to be shown when the list is empty.
	     * 
	     * <p>If you are overriding this method with your own custom content,
	     * consider including the standard layout {@link android.R.layout#list_content}
	     * in your layout file, so that you continue to retain all of the standard
	     * behavior of ListFragment.  In particular, this is currently the only
	     * way to have the built-in indeterminant progress state be shown.
	     */
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,
	            Bundle savedInstanceState) {
	        return inflater.inflate(R.layout.grid_content,
	                container, false);
	    }
	    /**
	     * Attach to list view once the view hierarchy has been created.
	     */
	    @Override
	    public void onViewCreated(View view, Bundle savedInstanceState) {
	        super.onViewCreated(view, savedInstanceState);
	        ensureGrid();
	    }
	    /**
	     * Detach from list view.
	     */
	    @Override
	    public void onDestroyView() {
	        mHandler.removeCallbacks(mRequestFocus);
	        mGrid = null;
	        mGridShown = false;
	        mEmptyView = mProgressContainer = mGridContainer = null;
	        mStandardEmptyView = null;
	        super.onDestroyView();
	    }
	    /**
	     * This method will be called when an item in the list is selected.
	     * Subclasses should override. Subclasses can call
	     * getListView().getItemAtPosition(position) if they need to access the
	     * data associated with the selected item.
	     *
	     * @param l The ListView where the click happened
	     * @param v The view that was clicked within the ListView
	     * @param position The position of the view in the list
	     * @param id The row id of the item that was clicked
	     */
	    public void onGridItemClick(GridView l, View v, int position, long id) {
	    }
	    /**
	     * Provide the cursor for the list view.
	     */
	    public void setListAdapter(ListAdapter adapter) {
	        boolean hadAdapter = mAdapter != null;
	        mAdapter = adapter;
	        if (mGrid != null) {
	            mGrid.setAdapter(adapter);
	            if (!mGridShown && !hadAdapter) {
	                // The list was hidden, and previously didn't have an
	                // adapter.  It is now time to show it.
	            	if( getActivity() != null && adapter.getCount() > 0 ) {
	            		// Try to measure column width
	            		View cell = adapter.getView(0, null, null) ;
	            		mGrid.setColumnWidth(measureCellWidth(getActivity(),cell));
	            	}
	            	
	                setGridShown(true, getView().getWindowToken() != null);
	            }
	        }
	    }
	    private int measureCellWidth( Context context, View cell )
	    {
	    	// http://stackoverflow.com/questions/6912922/android-how-does-gridview-auto-fit-find-the-number-of-columns
	        // We need a fake parent
	        FrameLayout buffer = new FrameLayout( context );
	        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	        buffer.addView( cell, layoutParams);

	        cell.forceLayout();
	        cell.measure(1000, 1000);

	        int width = cell.getMeasuredWidth();

	        buffer.removeAllViews();
	        
	        return width;
	    }	    
	    
	    /**
	     * Set the currently selected list item to the specified
	     * position with the adapter's data
	     *
	     * @param position
	     */
	    public void setSelection(int position) {
	    	ensureGrid();
	        mGrid.setSelection(position);
	    }
	    /**
	     * Get the position of the currently selected list item.
	     */
	    public int getSelectedItemPosition() {
	    	ensureGrid();
	        return mGrid.getSelectedItemPosition();
	    }
	    /**
	     * Get the cursor row ID of the currently selected list item.
	     */
	    public long getSelectedItemId() {
	    	ensureGrid();
	        return mGrid.getSelectedItemId();
	    }
	    /**
	     * Get the activity's list view widget.
	     */
	    public GridView getGridView() {
	        ensureGrid();
	        return mGrid;
	    }
	    /**
	     * The default content for a ListFragment has a TextView that can
	     * be shown when the list is empty.  If you would like to have it
	     * shown, call this method to supply the text it should use.
	     */
	    public void setEmptyText(CharSequence text) {
	        ensureGrid();
	        if (mStandardEmptyView == null) {
	            throw new IllegalStateException("Can't be used with a custom content view");
	        }
	        mStandardEmptyView.setText(text);
	        if (mEmptyText == null) {
	            mGrid.setEmptyView(mStandardEmptyView);
	        }
	        mEmptyText = text;
	    }
	    
	    /**
	     * Control whether the list is being displayed.  You can make it not
	     * displayed if you are waiting for the initial data to show in it.  During
	     * this time an indeterminant progress indicator will be shown instead.
	     * 
	     * <p>Applications do not normally need to use this themselves.  The default
	     * behavior of ListFragment is to start with the list not being shown, only
	     * showing it once an adapter is given with {@link #setListAdapter(ListAdapter)}.
	     * If the list at that point had not been shown, when it does get shown
	     * it will be do without the user ever seeing the hidden state.
	     * 
	     * @param shown If true, the list view is shown; if false, the progress
	     * indicator.  The initial value is true.
	     */
	    public void setGridShown(boolean shown) {
	        setGridShown(shown, true);
	    }
	    
	    /**
	     * Like {@link #setListShown(boolean)}, but no animation is used when
	     * transitioning from the previous state.
	     */
	    public void setGridShownNoAnimation(boolean shown) {
	        setGridShown(shown, false);
	    }
	    
	    /**
	     * Control whether the list is being displayed.  You can make it not
	     * displayed if you are waiting for the initial data to show in it.  During
	     * this time an indeterminant progress indicator will be shown instead.
	     * 
	     * @param shown If true, the list view is shown; if false, the progress
	     * indicator.  The initial value is true.
	     * @param animate If true, an animation will be used to transition to the
	     * new state.
	     */
	    private void setGridShown(boolean shown, boolean animate) {
	    	ensureGrid();
	        if (mProgressContainer == null) {
	            throw new IllegalStateException("Can't be used with a custom content view");
	        }
	        if (mGridShown == shown) {
	            return;
	        }
	        mGridShown = shown;
	        if (shown) {
	            if (animate) {
	                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                        getActivity(), android.R.anim.fade_out));
	                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
	                        getActivity(), android.R.anim.fade_in));
	            } else {
	                mProgressContainer.clearAnimation();
	                mGridContainer.clearAnimation();
	            }
	            mProgressContainer.setVisibility(View.GONE);
	            mGridContainer.setVisibility(View.VISIBLE);
	        } else {
	            if (animate) {
	                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                        getActivity(), android.R.anim.fade_in));
	                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
	                        getActivity(), android.R.anim.fade_out));
	            } else {
	                mProgressContainer.clearAnimation();
	                mGridContainer.clearAnimation();
	            }
	            mProgressContainer.setVisibility(View.VISIBLE);
	            mGridContainer.setVisibility(View.GONE);
	        }
	    }
	    
	    /**
	     * Get the ListAdapter associated with this activity's ListView.
	     */
	    public ListAdapter getListAdapter() {
	        return mAdapter;
	    }
	    private void ensureGrid() {
	        if (mGrid != null) {
	            return;
	        }
	        View root = getView();
	        if (root == null) {
	            throw new IllegalStateException("Content view not yet created");
	        }
	        if (root instanceof GridView) {
	            mGrid = (GridView)root;
	        } else {
	            mStandardEmptyView = (TextView)root.findViewById(R.id.internalEmpty);
                mStandardEmptyView.setVisibility(View.GONE);

	            mProgressContainer = root.findViewById(R.id.progressContainer);
	            mGridContainer = root.findViewById(R.id.gridContainer);
	            View rawGridView = root.findViewById(R.id.grid);
	            if (!(rawGridView instanceof GridView)) {
	                throw new RuntimeException(
	                        "Content has view with id attribute 'R.id.grid' "
	                        + "that is not a GridView class");
	            }
	            mGrid = (GridView)rawGridView;
	            if (mGrid == null) {
	                throw new RuntimeException(
	                        "Your content must have a GridView whose id attribute is " +
	                        "'R.id.grid'");
	            }
	            if (mEmptyView != null) {
	            	mGrid.setEmptyView(mEmptyView);
	            } else if (mEmptyText != null) {
	                mStandardEmptyView.setText(mEmptyText);
	                mGrid.setEmptyView(mStandardEmptyView);
	            }
	        }
	        mGridShown = true;
	        mGrid.setOnItemClickListener(mOnClickListener);
	        if (mAdapter != null) {
	            ListAdapter adapter = mAdapter;
	            mAdapter = null;
	            setListAdapter(adapter);
	        } else {
	            // We are starting without an adapter, so assume we won't
	            // have our data right away and start with the progress indicator.
	            if (mProgressContainer != null) {
	                setGridShown(false, false);
	            }
	        }
	        mHandler.post(mRequestFocus);
	    }
}
