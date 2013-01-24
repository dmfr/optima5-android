package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.R;
import za.dams.paracrm.BibleHelper.BibleCode;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class QueryLaunchFragment extends Fragment {
	private static final String LOGTAG = "QueryLaunchFragment";
	
	private Context mContext;
	private LayoutInflater mInflater ;

    /** Argument name(s) */
    private static final String ARG_QUERYSRC_ID = "querysrcId";
    
	private Callback mCallback = EmptyCallback.INSTANCE;
	
	// View fields
	private View mMainView ;
	private View mLoadingProgress;
	private TableLayout mHeaderTable ;
	private ViewGroup mViewgroupTable ;
	ArrayList<View> mCrmFieldViews ;
	
	// Crm Query Model
	private CrmQueryModel mModel;
	private static final int DATE_FROM = 0 ;
	private static final int DATE_TO = 1 ;
	
	private boolean mIsQueryRunning = false ;
	private ProgressDialog mProgressDialog ;
	
    /**
     * Callback interface that owning activities must implement
     */
    public interface Callback {
    	public void onQueryLaunchStart( int querysrcId ) ;
    	public void onQueryResponseFailure( int querysrcId ) ;
    	public void onQueryResponseSuccess( int querysrcId , int cacheResultJsonId ) ;
    }
    private static class EmptyCallback implements Callback {
    	public static final Callback INSTANCE = new EmptyCallback();
    	public void onQueryLaunchStart( int querysrcId ) {}
    	public void onQueryResponseFailure( int querysrcId ) {}
    	public void onQueryResponseSuccess( int querysrcId , int cacheResultJsonId ) {}
    }
    public void setCallback(Callback callback) {
        mCallback = (callback == null) ? EmptyCallback.INSTANCE : callback;
    }
	
	
    public static QueryLaunchFragment newInstance(int querysrcId) {
        final QueryLaunchFragment instance = new QueryLaunchFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_QUERYSRC_ID, querysrcId);
        instance.setArguments(args);
        return instance;
    }
    private Integer mImmutableQuerysrcId;
    private void initializeArgCache() {
        if (mImmutableQuerysrcId != null) return;
        mImmutableQuerysrcId = getArguments().getInt(ARG_QUERYSRC_ID);
    }
    public int getQuerySrcId() {
        initializeArgCache();
        return mImmutableQuerysrcId;
    }
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.explorer_querylaunch_fragment, container, false);

        mLoadingProgress = UiUtilities.getView(view, R.id.loading_progress);
        mMainView = UiUtilities.getView(view, R.id.main_panel);
        
        mHeaderTable = (TableLayout) view.findViewById(R.id.header_table) ;

        mViewgroupTable = (ViewGroup) view.findViewById(R.id.explorer_querylaunch_wherefields) ;
        mCrmFieldViews = new ArrayList<View>() ;
     
        return view ;
    }
    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onActivityCreated");
        }
        super.onActivityCreated(savedInstanceState);
        
        showContent(false) ;
        /*
		// Not needed
        resetView();
        */
        
        mInflater = getActivity().getLayoutInflater() ;
        
        new QueryLaunchLoadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        

        UiUtilities.installFragment(this);
    }
    @Override
    public void onDestroyView() {
        if (Explorer.DEBUG) {
            Log.d(LOGTAG, this + " onDestroyView");
        }
        UiUtilities.uninstallFragment(this);
        
        /*
        // Virer l'AsyncTask ?
        cancelAllTasks();
        */

        // We should clean up the Webview here, but it can't release resources until it is
        // actually removed from the view tree.

        super.onDestroyView();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void restoreInstanceState(Bundle state) {
    }
    
    /**
     * Show/hide the content.  We hide all the content (except for the bottom buttons) when loading,
     * to avoid flicker.
     */
    private void showContent(boolean showContent) {
        mMainView.setVisibility( showContent ? View.VISIBLE : View.GONE ) ;
        mLoadingProgress.setVisibility( !showContent ? View.VISIBLE : View.GONE ) ;
    }
    
    
	private class QueryLaunchLoadTask extends AsyncTask<Void, Void, Void> {
		protected void onPreExecute() {
		}
		protected Void doInBackground(Void... arg0) {
			Context c = mContext.getApplicationContext() ;
			int querysrcId = QueryLaunchFragment.this.getQuerySrcId() ;
			CrmQueryModel crmQueryModel = CrmQueryManager.model_get(c, querysrcId) ;
			QueryLaunchFragment.this.setModel(crmQueryModel) ;
			return null ;
		}
		protected void onPostExecute(Void arg0) {
			if( mModel != null ) {
				setupFromModel() ;
			}
		}
	}
	private synchronized void setModel( CrmQueryModel crmQueryModel ) {
		mModel = crmQueryModel ;
	}
    private void setupFromModel() {
    	// ***** Header ***************
    	String text ;
    	TableRow tr ;
    	
		tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_header_row, null) ;
		switch( mModel.querysrcType ) {
		case QUERY:
			text = "Query" ;
			break ;
		case QMERGE:
			text = "Qmerge" ;
			break ;
		case QWEB :
			text = "QWeb" ;
			break;
		default: 
			text ="" ;
		}
		((TextView)tr.findViewById(R.id.crm_label)).setText("Type :") ;
		((TextView)tr.findViewById(R.id.crm_text)).setText(text) ;
		mHeaderTable.addView(tr) ;

		tr = (TableRow) mInflater.inflate(R.layout.explorer_fileview_header_row, null) ;
		((TextView)tr.findViewById(R.id.crm_label)).setText("Query :") ;
		((TextView)tr.findViewById(R.id.crm_text)).setText(mModel.querysrcName) ;
		mHeaderTable.addView(tr) ;

    	
    	// ***** Detach de toutes les fields CRM ****
    	if( mCrmFieldViews != null ){
    		for( View v : mCrmFieldViews ) {
    			if( v.getParent() != null ) {
    				((LinearLayout)v.getParent()).removeView(v);
    			}
    		}
    		mCrmFieldViews.clear() ;
    	}
    	else {
    		mCrmFieldViews = new ArrayList<View>();
    	}
    	// Fin du detach 
    	
    	// création des CrmFields
    	LayoutInflater inflater = mInflater ;
    	View newView ;
    	int crmFieldsIndex = -1 ;
    	for( CrmQueryModel.CrmQueryCondition cqc : mModel.querysrcConditions ) {
        	
    		crmFieldsIndex++ ;
    		

    		switch( cqc.fieldType ) {
    		case FIELD_BIBLE :
    			newView = inflater.inflate(R.layout.explorer_querylaunch_table_row,null) ;
    			((TextView)newView.findViewById(R.id.crm_label)).setText(cqc.fieldName) ;
    			((Button)newView.findViewById(R.id.crm_button)).setOnClickListener(new BibleClickListener(new BibleCode(cqc.fieldLinkBible),crmFieldsIndex));
    			mCrmFieldViews.add(newView) ;
    			mViewgroupTable.addView(newView) ;
    			break ;

    		case FIELD_DATE :
    			Time currentTime ;
    			
    			newView = inflater.inflate(R.layout.explorer_querylaunch_table_row,null) ;
    			((TextView)newView.findViewById(R.id.crm_label)).setText(cqc.fieldName + " / From") ;
    			currentTime = cqc.conditionDateGt ;
    			if( currentTime == null ) {
    				currentTime = new Time();
    				currentTime.setToNow() ;
    			}
    			((Button)newView.findViewById(R.id.crm_button)).setOnClickListener(new DateClickListener(currentTime,crmFieldsIndex,DATE_FROM));
    			mCrmFieldViews.add(newView) ;
    			mViewgroupTable.addView(newView) ;
    			
    			newView = inflater.inflate(R.layout.explorer_querylaunch_table_row,null) ;
    			((TextView)newView.findViewById(R.id.crm_label)).setText(cqc.fieldName + " / To") ;
    			currentTime = cqc.conditionDateLt ;
    			if( currentTime == null ) {
    				currentTime = new Time();
    				currentTime.setToNow() ;
    			}
    			((Button)newView.findViewById(R.id.crm_button)).setOnClickListener(new DateClickListener(currentTime,crmFieldsIndex,DATE_TO));
    			mCrmFieldViews.add(newView) ;
    			mViewgroupTable.addView(newView) ;
    			break ;


    		default:
    			newView = new View(mContext);
    			newView.setVisibility(View.GONE) ;
    			mCrmFieldViews.add(newView) ;
    			mViewgroupTable.addView(newView) ;
    			break ;
    		}
    		crmFieldsIndex++ ;

    		
    	}
    	
    	showContent(true) ;
    }

    
    
    private class DateListener implements OnDateSetListener {
        View mView;
        int mCrmFieldIndex ;
        int mFromOrTo ;

        public DateListener(View view, int crmFieldIndex, int fromOrTo) {
            mView = view;
            mCrmFieldIndex = crmFieldIndex ;
            mFromOrTo = fromOrTo ;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
        	Time timeToSet = null ;
        	timeToSet = new Time();
        	timeToSet.year = year ;
        	timeToSet.month = month ;
        	timeToSet.monthDay = monthDay ;
        	timeToSet.normalize(true) ;
        	
        	switch( mFromOrTo ) {
        	case DATE_FROM :
        		mModel.querysrcConditions.get(mCrmFieldIndex).conditionDateGt = timeToSet ;
        		break ;
        	case DATE_TO :
        		mModel.querysrcConditions.get(mCrmFieldIndex).conditionDateLt = timeToSet ;
        		break ;
        	default :
        		return ;
        	}
        	if( mModel.querysrcConditions.get(mCrmFieldIndex).conditionDateGt != null 
        			&& mModel.querysrcConditions.get(mCrmFieldIndex).conditionDateLt != null ) {
        		
        		mModel.querysrcConditions.get(mCrmFieldIndex).conditionIsSet = true ;
        	}
        	
        	
            int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                    | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_MONTH
                    | DateUtils.FORMAT_ABBREV_WEEKDAY;
            String dateString = DateUtils.formatDateTime(getActivity(), timeToSet.toMillis(true), flags);
            ((Button)mView).setText(dateString) ;
        }
    }
	
    private class DateClickListener implements View.OnClickListener {
        private Time mTime;
        int mCrmFieldIndex ;
        int mFromOrTo ;

        public DateClickListener(Time time, int crmFieldIndex, int fromOrTo) {
            mTime = time;
            mCrmFieldIndex = crmFieldIndex ;
            mFromOrTo = fromOrTo ;
        }

        public void onClick(View v) {
            DatePickerDialog dpd = new DatePickerDialog(
                    getActivity(), new DateListener(v,mCrmFieldIndex,mFromOrTo), mTime.year, mTime.month, mTime.monthDay);
            dpd.setCanceledOnTouchOutside(true);
            dpd.show();
        }
    }
    
    private class BibleListener implements OnBibleSetListener {
        View mView;
        int mCrmFieldIndex ;

        public BibleListener(View view, int crmFieldIndex) {
        	mCrmFieldIndex = crmFieldIndex ;
            mView = view;
        }

        @Override
        public void onBibleSet(BibleEntry be) {
            if( be != null ) {
            	((Button)mView).setText(be.displayStr) ;
            	mModel.querysrcConditions.get(mCrmFieldIndex).conditionIsSet = true ;
            	mModel.querysrcConditions.get(mCrmFieldIndex).conditionBibleEntry = be ;
            }
            else{
            	((Button)mView).setText("") ;
            	mModel.querysrcConditions.get(mCrmFieldIndex).conditionIsSet = false ;
            	mModel.querysrcConditions.get(mCrmFieldIndex).conditionBibleEntry = null ;
            }
        }
    }
	
    private class BibleClickListener implements View.OnClickListener {
    	BibleCode mBc ;
    	int mCrmFieldIndex ;
    	
        public BibleClickListener( BibleCode bc, int crmFieldIndex ) {
        	mBc = bc ;
        	mCrmFieldIndex = crmFieldIndex ;
        }

        public void onClick(View v) {
        	FragmentTransaction ft = getFragmentManager().beginTransaction();
        	
        	
            BiblePickerDialog bpd = new BiblePickerDialog(mContext, new BibleListener(v,mCrmFieldIndex), mBc,null);
            //bpd.setTargetFragment(this, 0);
            //bpd.setCanceledOnTouchOutside(true);
            bpd.show(ft, "dialog") ;
        }
    }
    
    
    public synchronized void goQuery() {
    	if( !CrmQueryManager.validateModel(mModel) ) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    		builder.setMessage("Please fill required parameters !")
    		.setCancelable(false)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    				dialog.cancel();
    			}
    		});
    		AlertDialog alert = builder.create();
    		alert.show();
    		return ;
    	}
    	
    	
    	if( mIsQueryRunning ) {
    		return ;
    	}
    	mIsQueryRunning = true ;
    	new BuildQueryResults().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
    }
    public boolean isQueryRunning() {
    	return mIsQueryRunning ;
    }
    private class BuildQueryResults extends AsyncTask<Void,Void,Integer> {

		@Override
		protected void onPreExecute() {
			QueryLaunchFragment.this.mCallback.onQueryLaunchStart(getQuerySrcId()) ;
		    mProgressDialog = ProgressDialog.show(
		    		getActivity(),
		    		"Running Query",
		            "Please wait...",
		            true);

			return ;
		}
		@Override
		protected Integer doInBackground(Void... arg0) {
			return CrmQueryManager.fetchRemoteJson(mContext, mModel) ;
		}
		@Override
		protected void onPostExecute(Integer jsonResultId) {
        	if( isAdded() ) {
    			mProgressDialog.dismiss() ;
        	}
			mIsQueryRunning = false ;
			if( jsonResultId != null && jsonResultId > 0 ) {
				QueryLaunchFragment.this.mCallback.onQueryResponseSuccess(getQuerySrcId(),jsonResultId) ;
			} else {
				QueryLaunchFragment.this.mCallback.onQueryResponseFailure(getQuerySrcId()) ;
			}
			return ;
		}
    	
    }
    
}
