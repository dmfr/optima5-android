package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import za.dams.paracrm.widget.DatetimePickerDialog;
import za.dams.paracrm.widget.DatetimePickerDialog.OnDatetimeSetListener;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class FiledetailTableFragment extends FiledetailFragment implements UtilityFragment.Listener {

	protected CrmFileTransaction mTransaction ;

	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/UI/FiledetailTableFragment";

	private UtilityFragment mUtilityFragment ;

	ProgressBar mProgressBar ;

	public static FiledetailTableFragment newInstance(int index) {
		FiledetailTableFragment f = new FiledetailTableFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putInt("index", index);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState) ;
		mUtilityFragment = (UtilityFragment) getFragmentManager().findFragmentByTag(UtilityFragment.TAG) ;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.filecapture_filedetail_table, container, false ) ;
		
		return view ;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		View view = getView() ;
		mProgressBar = (ProgressBar)view.findViewById(R.id.myprogress) ;
		
		super.onActivityCreated(savedInstanceState);
		CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
		mTransaction = mManager.getTransaction() ;

		mUtilityFragment.autocompleteInit(getPageInstanceTag());

		syncWithData() ;
	}
	@Override
	public void onResume(){
		super.onResume() ;
		mUtilityFragment.registerListener(this);
	}


	private void updateFooterView(){
		boolean isRefreshing = mUtilityFragment.isPageHasPendingJobs(getPageInstanceTag()) ;
		mProgressBar.setVisibility(isRefreshing ? View.VISIBLE:View.GONE) ;
	}
	public void syncWithData(){
		int pageId = getShownIndex() ;

		updateFooterView() ;

		ArrayList<CrmFileTransaction.CrmFileFieldDesc> tDesc = mTransaction.page_getFields(pageId) ;
		Iterator<CrmFileTransaction.CrmFileFieldDesc> mIter ;
		CrmFileTransaction.CrmFileFieldDesc fieldDesc ;

		TableLayout mTabLayout = (TableLayout) FiledetailTableFragment.this.getView().findViewById(R.id.mytable) ;
		mTabLayout.removeAllViews() ;
		mTabLayout.setColumnStretchable(1,true) ;
		mTabLayout.setColumnShrinkable(1,true);
		TableRow row = new TableRow(getActivity()) ;
		View view ;
		CheckBox cb ;
		ImageButton btn ;
		TextView text ;
		EditText etext ;
		text = new TextView(getActivity());
		row.addView(text);

		mIter = tDesc.iterator() ;
		while( mIter.hasNext() ){
			fieldDesc = mIter.next() ;

			text = new TextView(getActivity());
			text.setText(fieldDesc.fieldName);
			text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18) ;
			text.setTextColor(Color.rgb(255,255,255)) ;
			text.setPadding(10, 3, 10, 3) ;

			//row.addView(icon);
			row.addView(text) ;
		}
		mTabLayout.addView(row);

		view = new View(getActivity()) ;
		view.setBackgroundColor(Color.rgb(255,255,255)) ;
		//view.setMinimumHeight(3) ;
		view.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
		mTabLayout.addView(view);

		ArrayList<CrmFileTransaction.CrmFileRecord> records = mTransaction.pageTable_getRecords(pageId) ;
		Iterator<CrmFileTransaction.CrmFileRecord> mIter2 = records.iterator() ;
		CrmFileTransaction.CrmFileRecord mrecord ;
		int a = -1 ;
		int b ;
		while( mIter2.hasNext() ){
			a++ ;

			mrecord = mIter2.next() ;

			if( mrecord.recordIsHidden ) {
				continue ;
			}

			row = new TableRow(getActivity()) ;

			if( mrecord.recordIsDeletable ) {
				btn = new ImageButton(getActivity()) ;
				btn.setTag(new Integer(a)) ;
				btn.setOnClickListener(clickDeleteListener);
				btn.setImageResource(android.R.drawable.ic_delete);
				row.addView(btn);
			} else {
				cb = new CheckBox(getActivity()) ;
				cb.setTag(new Integer(a)) ;
				cb.setChecked(!(mrecord.recordIsDisabled)) ;
				cb.setOnCheckedChangeListener(checkDisableListener) ;
				row.addView(cb);
			}

			mIter = tDesc.iterator() ;
			b = 0 ;
			while( mIter.hasNext() ){
				fieldDesc = mIter.next() ;

				switch( fieldDesc.fieldType )
				{
				case FIELD_DATE:
				case FIELD_DATETIME :
					etext = new EditText(getActivity());
					if( mrecord.recordData.get(fieldDesc.fieldCode).isSet ) {
						etext.setText(mrecord.recordData.get(fieldDesc.fieldCode).displayStr);
					}
					etext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22) ;
					// text.setTextColor(Color.rgb(255,255,255)) ;
					etext.setPadding(10, 3, 10, 10) ;
					etext.setMaxLines(1) ;
					if( true ){
						etext.setInputType(InputType.TYPE_NULL) ;
					}
					etext.setId( (a*100) + b ) ;

					etext.setOnClickListener(new DateClickListener(pageId,a,b)) ;

					row.addView(etext) ;
					break ;

				case FIELD_TEXT :
				case FIELD_NUMBER :
					if( mrecord.recordIsDisabled ) {
						text = new TextView(getActivity()) ;
						row.addView(text);
					}
					else {

						etext = new EditText(getActivity());
						if( mrecord.recordData.get(fieldDesc.fieldCode).isSet ) {
							etext.setText(mrecord.recordData.get(fieldDesc.fieldCode).displayStr);
						}
						etext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18) ;
						// text.setTextColor(Color.rgb(255,255,255)) ;
						etext.setPadding(10, 3, 10, 10) ;
						etext.setMaxLines(1) ;
						if( fieldDesc.fieldType == CrmFileTransaction.FieldType.FIELD_NUMBER){
							etext.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL) ;
						}
						etext.setId( (a*100) + b ) ;

						//row.addView(icon);
						row.addView(etext) ;
					}
					break ;

				default:
					text = new TextView(getActivity());
					text.setEllipsize(TextUtils.TruncateAt.END);
					text.setMaxLines(1);
					if( mrecord.recordData.get(fieldDesc.fieldCode).isSet ) {
						text.setText(mrecord.recordData.get(fieldDesc.fieldCode).displayStr);
					}
					text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18) ;
					if( mrecord.recordIsDisabled ) {
						text.setTextColor(Color.rgb(255,0,0)) ;
						text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

					}
					text.setPadding(10, 3, 10, 10) ;

					//row.addView(icon);
					row.addView(text) ;
					break ;	
				}


				b++ ;
			}
			row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
			mTabLayout.addView(row);

			if( mIter2.hasNext() ) {
				view = new View(getActivity()) ;
				view.setBackgroundColor(Color.rgb(255,255,255)) ;
				//view.setMinimumHeight(3) ;
				view.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1));
				mTabLayout.addView(view);

			}
		}
	}

	OnCheckedChangeListener checkDisableListener = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
			int pageId = FiledetailTableFragment.this.getShownIndex() ;
			int recordId = ((Integer)buttonView.getTag()).intValue() ;
			//Log.w(TAG,"Checking button "+((Integer)buttonView.getTag()).toString() ) ;
			CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( FiledetailTableFragment.this.getActivity().getApplicationContext() ) ;
			CrmFileTransaction mTransaction = mManager.getTransaction() ;
			mTransaction.page_setRecordDisabled(pageId, recordId, !isChecked) ;
			saveValues() ;
			mTransaction.links_refresh() ;
			syncWithData() ;
			((FilelistFragment)getFragmentManager().findFragmentById(R.id.filelist)).syncWithData() ;
		}
	} ;
	OnClickListener clickDeleteListener = new OnClickListener() {
		public void onClick(View buttonView) {
			final int buttonId = ((Integer)buttonView.getTag()).intValue() ;
			
        	AlertDialog.Builder builder = new AlertDialog.Builder(FiledetailTableFragment.this.getActivity());
        	builder.setMessage("Delete record ?")
        	       .setCancelable(true)
        	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   int pageId = FiledetailTableFragment.this.getShownIndex() ;
        	        	   int recordId = buttonId ;
        	        	   CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( FiledetailTableFragment.this.getActivity().getApplicationContext() ) ;
        	        	   CrmFileTransaction mTransaction = mManager.getTransaction() ;
        	        	   saveValues() ;
        	        	   mTransaction.page_deleteRecord(pageId, recordId) ;
        	        	   mTransaction.links_refresh() ;
        	        	   syncWithData() ;
        	        	   ((FilelistFragment)getFragmentManager().findFragmentById(R.id.filelist)).syncWithData() ;
        	        	   
        	        	   dialog.dismiss();
        	           }
        	       })
        	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   dialog.dismiss();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	alert.show();
		}
	} ;


	protected void saveValues() {
		CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
		CrmFileTransaction mTransaction = mManager.getTransaction() ;
		//mTransaction.page_setRecordFieldValue_bible(pageId,recordId,fieldId,mEntryKey ) ;
		int pageId = getShownIndex() ;
		
		ArrayList<CrmFileTransaction.CrmFileFieldDesc> tDesc = mTransaction.page_getFields( pageId ) ;
		
		// Récupération de tous les EditText
		ViewGroup tableLayout = (ViewGroup)getView().findViewById(R.id.mytable) ;
		for( int trIdx=0 ; trIdx < tableLayout.getChildCount() ; trIdx++ ) {
			View tableChild = tableLayout.getChildAt(trIdx) ;
			if( !(tableChild instanceof ViewGroup) ) {
				continue ;
			}
			ViewGroup tableRow = (ViewGroup)tableChild ;
			for( int tdIdx=0 ; tdIdx < tableRow.getChildCount() ; tdIdx++ ) {
				View rowChild = tableRow.getChildAt(tdIdx) ;
				if( !(rowChild instanceof EditText) ) {
					continue ;
				}

				EditText editText = (EditText)rowChild ;
				int myId = editText.getId();
				String myTextValue = editText.getText().toString() ;
				
				//recherche du myA + myB (position dans la grille record>desc)
				int myB = myId % 100  ;
				int myA = (myId - myB) / 100 ;

				if( myTextValue.length() < 1 ){
					mTransaction.page_setRecordFieldValue_unset( pageId , myA, myB ) ;
				}
				else {
					switch( tDesc.get(myB).fieldType ) {
					case FIELD_NUMBER :
						mTransaction.page_setRecordFieldValue_number( pageId , myA, myB, Float.parseFloat(myTextValue) ) ;
						break ;
					default :
						mTransaction.page_setRecordFieldValue_text( pageId , myA, myB, myTextValue ) ;
						break ;
					}
				}
			}
		}
	}
	@Override
	public void onPause() {
		Fragment dialogFragment = getFragmentManager().findFragmentByTag("dialog") ;
		if( dialogFragment != null ) {
			((DialogFragment)dialogFragment).dismiss() ;
		}
		
		saveValues() ;
		mTransaction.links_refresh() ;

		mUtilityFragment.unregisterListener(this);

		super.onPause() ;
	}


	private class DateClickListener implements View.OnClickListener {
		int pageId ;
		int recordId ;
		int fieldId ;

		public DateClickListener(int pageId, int recordId, int fieldId) {
			this.pageId = pageId ;
			this.recordId = recordId ;
			this.fieldId = fieldId ;
		}

		public void onClick(View v) {
			CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
			CrmFileTransaction mTransaction = mManager.getTransaction() ;

			String targetTitle = mTransaction.page_getFields(pageId).get(fieldId).fieldName ;

			Date curDate = mTransaction.page_getRecordFieldValue( pageId , recordId, fieldId ).valueDate ;

			DateListener dateListener = new DateListener(pageId,recordId,fieldId) ;

			boolean hideTime = (mTransaction.page_getFields(pageId).get(fieldId).fieldType == FieldType.FIELD_DATE) ;
			long boundDateMinMillis = -1 ;
			if( mTransaction.page_getFields(pageId).get(fieldId).inputCfg_dateBoundMin ) {
				boundDateMinMillis = DatetimePickerDialog.getDayOffsetMillis(mTransaction.page_getFields(pageId).get(fieldId).inputCfg_dateBoundMin_dayOffset) ;
			}
			long boundDateMaxMillis = -1 ;
			if( mTransaction.page_getFields(pageId).get(fieldId).inputCfg_dateBoundMax ) {
				boundDateMaxMillis = DatetimePickerDialog.getDayOffsetMillis(mTransaction.page_getFields(pageId).get(fieldId).inputCfg_dateBoundMax_dayOffset) ;
			}

			DatetimePickerDialog datetimePicker = DatetimePickerDialog.newInstance(curDate.getYear() + 1900, curDate.getMonth(), curDate.getDate(), curDate.getHours(), curDate.getMinutes(), hideTime, boundDateMinMillis, boundDateMaxMillis);
			datetimePicker.setTitle(targetTitle);
			datetimePicker.setListener(dateListener) ;
			datetimePicker.show(getFragmentManager(), "dialog") ;
		}
	}
	private class DateListener implements OnDatetimeSetListener {
		int pageId ;
		int recordId ;
		int fieldId ;

		public DateListener(int pageId, int recordId, int fieldId) {
			this.pageId = pageId ;
			this.recordId = recordId ;
			this.fieldId = fieldId ;
		}

		@Override
		public void onDatetimeSet(int year, int month, int monthDay,
				int hourOfDay, int minute) {
			Date newDate = new Date(year-1900,month,monthDay,hourOfDay,minute) ;

			CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
			CrmFileTransaction mTransaction = mManager.getTransaction() ;
			mTransaction.page_setRecordFieldValue_date(pageId,recordId,fieldId, newDate ) ;
			FiledetailTableFragment.this.syncWithData() ;
		}

	}


	@Override
	public void onPageInstanceChanged(int pageInstanceTag) {
		if( pageInstanceTag==getPageInstanceTag() ) {
			syncWithData() ;
		}
	}

}
