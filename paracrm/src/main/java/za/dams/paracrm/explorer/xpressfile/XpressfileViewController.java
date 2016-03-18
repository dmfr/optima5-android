package za.dams.paracrm.explorer.xpressfile;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.BibleHelper.BibleCode;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileFieldDesc;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileFieldValue;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileRecord;
import za.dams.paracrm.widget.BiblePickerDialog;
import za.dams.paracrm.widget.BiblePickerDialog.OnBibleSetListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class XpressfileViewController {
	private static final String TAG = "Xpressfile/XpressfileViewController";
	
    private Activity mActivity;
    private View mView;
    private CrmFileRecord mCrmFileRecord;
    
    ViewGroup mViewgroupTable ;
    
    View mPrimarykeyRow ; 
    TextView mPrimarykeyLabel ;
    Button mPrimarykeyButton ;
    ViewGroup mPrimarykeyFieldsGroup ;
    
    List<View> mCrmFieldViews ;
    List<CrmFileFieldDesc> mCrmFieldDescs ;
    
    XpressfileViewListener mListener ;
	public interface XpressfileViewListener {
		void onXpressfileViewChanged() ;
		void onXpressfilePrimarykeySet( String primarykeyBibleEntryKey ) ;
	}
	public void setListener( XpressfileViewListener listener ) {
		mListener = listener ;
	}
	
	private final TextWatcher mTextWatcher = new TextWatcher() {
	    @Override
	    public void onTextChanged(CharSequence s, int start, int before, int count) { }
	    @Override
	    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	    @Override
	    public void afterTextChanged(Editable s) {
	        if( mListener != null ) {
	        	mListener.onXpressfileViewChanged() ;
	        }
	    }
	};
	private final OnCheckedChangeListener mCheckWatcher = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
	        if( mListener != null ) {
	        	mListener.onXpressfileViewChanged() ;
	        }
		}
	};

    
    public XpressfileViewController(Activity activity, View view) {
    	mActivity = activity ;
    	mView = view ;
    	mCrmFileRecord = null ;
    	
        // cache all the widgets
        mViewgroupTable = (ViewGroup)view.findViewById(R.id.xpressfile_filefields);
        
        mPrimarykeyRow = view.findViewById(R.id.primarykey_row) ;
        mPrimarykeyLabel = (TextView)view.findViewById(R.id.primarykey_label) ;
        mPrimarykeyButton = (Button)view.findViewById(R.id.primarykey_button) ;
        mPrimarykeyFieldsGroup = (ViewGroup)view.findViewById(R.id.xpressfile_primarykey_biblefields);
        
        mPrimarykeyRow.setVisibility(View.GONE) ;
    }

	public void setupPrimarykey( CrmFileFieldDesc primarykeyCffd, List<BibleHelper.BibleFieldCode> bibleDesc, boolean isReadonly ) {
		mPrimarykeyRow.setVisibility(View.VISIBLE) ;
		mPrimarykeyLabel.setText(primarykeyCffd.fieldName) ;
		if( !isReadonly ) {
			mPrimarykeyButton.setOnClickListener(new BibleClickListener(new BibleCode(primarykeyCffd.fieldLinkBible),-1));
		}
		
		
		mPrimarykeyFieldsGroup.removeAllViews() ;
		LayoutInflater inflater = mActivity.getLayoutInflater() ;
		for( BibleHelper.BibleFieldCode bfc : bibleDesc ) {
			if( bfc.recordType != BibleHelper.RecordType.BIBLE_ENTRY ) {
				continue ;
			}
			if( bfc.fieldType == null ) {
				continue ;
			}
			
			View v = inflater.inflate(R.layout.xpressfile_view_row_static, null) ;
			v.setTag(bfc.fieldCode) ;
			((TextView)v.findViewById(R.id.crm_label)).setText(bfc.fieldName) ;
			mPrimarykeyFieldsGroup.addView(v) ;
		}
	}
	public void setupFileFields( List<CrmFileFieldDesc> arrCffd ) {
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
    	if( mCrmFieldDescs != null ) {
    		mCrmFieldDescs.clear() ;
    	} else {
    		mCrmFieldDescs = new ArrayList<CrmFileFieldDesc>();
    	}
    	// Fin du detach 
		
    	LayoutInflater inflater = mActivity.getLayoutInflater() ;
    	View newView ;
    	int crmFieldsIndex = -1 ;
    	for( CrmFileFieldDesc fd : arrCffd ) {
    		crmFieldsIndex++ ;
    		
    		switch( fd.fieldType ) {
    		case FIELD_BIBLE :
    	    	newView = inflater.inflate(R.layout.xpressfile_view_row_bible,null) ;
    	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
    	    	((Button)newView.findViewById(R.id.crm_button)).setOnClickListener(new BibleClickListener(new BibleCode(fd.fieldLinkBible),crmFieldsIndex));
    			break ;
    		case FIELD_TEXT :
    	    	newView = inflater.inflate(R.layout.xpressfile_view_row_text,null) ;
    	    	((EditText)newView.findViewById(R.id.crm_text)).addTextChangedListener(mTextWatcher) ;
    			break ;
    			
    		case FIELD_NUMBER :
    	    	newView = inflater.inflate(R.layout.xpressfile_view_row_number,null) ;
    	    	((EditText)newView.findViewById(R.id.crm_text)).addTextChangedListener(mTextWatcher) ;
    			break ;
    			
    		case FIELD_BOOLEAN :
    	    	newView = inflater.inflate(R.layout.xpressfile_view_row_checkbox,null) ;
    	    	((CheckBox)newView.findViewById(R.id.crm_checkbox)).setOnCheckedChangeListener(mCheckWatcher);
    			break ;
    			
    		case FIELD_NULL :
    	    	newView = inflater.inflate(R.layout.xpressfile_view_row_static,null) ;
    	    	((TextView)newView.findViewById(R.id.crm_label)).setTextColor(Color.BLACK) ;
    	    	newView.setPadding(newView.getPaddingLeft(), 24, newView.getPaddingRight(), 0) ;
    			break ;
    			
    		default:
    			newView = new View(mActivity);
    			newView.setVisibility(View.GONE) ;
    	    	mCrmFieldViews.add(newView) ;
    	    	mCrmFieldDescs.add(null) ;
    	    	mViewgroupTable.addView(newView) ;
    			continue ;
    		}
    		
	    	((TextView)newView.findViewById(R.id.crm_label)).setText(fd.fieldName) ;
	    	if( fd.fieldIsMandatory ) {
	    		((TextView)newView.findViewById(R.id.crm_label)).setTextColor(Color.RED) ;
	    	}
	    	newView.setVisibility(View.GONE) ;
	    	mCrmFieldViews.add(newView) ;
	    	mCrmFieldDescs.add(fd) ;
	    	mViewgroupTable.addView(newView) ;
    	}
    	
	}
	public void setPrimarykeyBibleEntry( BibleHelper.BibleEntry be ) {
		if( be == null ) {
			mPrimarykeyButton.setText("") ;
			for( int idx=0 ; idx<mPrimarykeyFieldsGroup.getChildCount() ; idx++ ) {
				View bibleFieldView = mPrimarykeyFieldsGroup.getChildAt(idx) ;
				((TextView)bibleFieldView.findViewById(R.id.crm_text)).setText("") ;
			}
			return ;
		}
		
		
		mPrimarykeyButton.setText(be.displayStr) ;
		if( be.optBibleFields == null ) {
			return ;
		}
		for( int idx=0 ; idx<mPrimarykeyFieldsGroup.getChildCount() ; idx++ ) {
			View bibleFieldView = mPrimarykeyFieldsGroup.getChildAt(idx) ;
			String bibleFieldCode = (String)bibleFieldView.getTag() ;
			if( bibleFieldCode == null ) {
				continue ;
			}
			String bibleFieldValue = be.optBibleFields.get(bibleFieldCode) ;
			if( bibleFieldValue == null ) {
				continue ;
			}
			((TextView)bibleFieldView.findViewById(R.id.crm_text)).setText(bibleFieldValue) ;
		}
	}
	public void setCrmFileRecord( CrmFileRecord cfr ) {
		int idx ;
		if( cfr == null ) {
			mCrmFileRecord = null ;
			
			idx = -1 ;
			for( CrmFileFieldDesc fd : mCrmFieldDescs ) {
				idx++ ;
				mCrmFieldViews.get(idx).setVisibility(View.GONE) ;
			}
			return ;
		}
		
		mCrmFileRecord = cfr ;
		
		idx = -1 ;
		for( CrmFileFieldDesc fd : mCrmFieldDescs ) {
			idx++ ;
			if( fd == null ) {
				continue ;
			}
			mCrmFieldViews.get(idx).setVisibility(View.VISIBLE) ;
			String tFieldCode = fd.fieldCode ;
    		switch( fd.fieldType ) {
    		case FIELD_BIBLE:
    			((TextView)mCrmFieldViews.get(idx).findViewById(R.id.crm_button)).setText(cfr.recordData.get(tFieldCode).displayStr) ;
    			break ;
    		case FIELD_TEXT:
    		case FIELD_NUMBER:
    			((TextView)mCrmFieldViews.get(idx).findViewById(R.id.crm_text)).setText(cfr.recordData.get(tFieldCode).displayStr) ;
    			break ;
    		case FIELD_BOOLEAN :
    			CheckBox cb = (CheckBox)mCrmFieldViews.get(idx).findViewById(R.id.crm_checkbox) ;
    			cb.setChecked(cfr.recordData.get(tFieldCode).valueBoolean) ;
    			break ;
    		}
			
		}
		return ;
	}
	
	
	public boolean prepareForSave() {
		return fillModelFromUI() ;
	}
	private boolean fillModelFromUI(){
		// Champs dynamiques CRM
		boolean isComplete = true ;
		
		boolean capturedBool ;
		String capturedStr ;
		
		int crmFieldIndex = 0 ;
		for( CrmFileFieldDesc fd : mCrmFieldDescs ) {
			CrmFileFieldValue record = mCrmFileRecord.recordData.get(fd.fieldCode) ;
			
			switch( fd.fieldType ) {
			case FIELD_BIBLE :
				// bible capturée en direct : BibleListener => rien à faire
				break ;
				
			case FIELD_BOOLEAN :
				capturedBool = ((CheckBox)mCrmFieldViews.get(crmFieldIndex).findViewById(R.id.crm_checkbox)).isChecked() ;
				record.valueBoolean = capturedBool ;
				record.displayStr = capturedBool ? "true":"false" ;
				break ;
				
			case FIELD_TEXT :
				capturedStr = ((EditText)mCrmFieldViews.get(crmFieldIndex).findViewById(R.id.crm_text)).getText().toString() ;
				if( fd.fieldIsMandatory && capturedStr.equals("") ) {
					isComplete = false ;
				}
				record.valueString = capturedStr ;
				record.displayStr = capturedStr ;
				break ;
				
			case FIELD_NUMBER :
				capturedStr = ((EditText)mCrmFieldViews.get(crmFieldIndex).findViewById(R.id.crm_text)).getText().toString() ;
				float num = Float.parseFloat(capturedStr) ;
				record.valueFloat = num ;
				if( num==Math.ceil(num) ) {
					record.displayStr = String.valueOf((int)num) ;
				}
				else {
					record.displayStr = String.valueOf(num) ;
				}
				break ;
				
			default :
				// non supporté => skip
				break ;
			}
			
			crmFieldIndex++ ;
		}
		
		return isComplete ;
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
            if( mView == mPrimarykeyButton ) {
            	if( be != null ) {
            	Log.d(TAG, "onBibleSet for button: " + be.displayStr + " for field idx "+mCrmFieldIndex);
            	((Button)mView).setText(be.displayStr) ;
            	if( mListener != null ) {
            		mListener.onXpressfilePrimarykeySet(be.entryKey);
            	}
            	}
            	return ;
            }
            CrmFileFieldDesc cfd = mCrmFieldDescs.get(mCrmFieldIndex) ;
            CrmFileFieldValue fv = mCrmFileRecord.recordData.get(cfd.fieldCode) ;
            if( be != null ) {
            	((Button)mView).setText(be.displayStr) ;
            	fv.displayStr = be.displayStr ;
            	fv.valueString = be.entryKey ;
            }
            else{
            	((Button)mView).setText("") ;
            	fv.displayStr = "" ;
            	fv.valueString = "" ;
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
        	FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
        	
            BiblePickerDialog bpd = new BiblePickerDialog(mActivity, new BibleListener(v,mCrmFieldIndex), mBc,null);
            //bpd.setTargetFragment(this, 0);
            //bpd.setCanceledOnTouchOutside(true);
            bpd.show(ft, "dialog") ;
        }
    }
	
}
