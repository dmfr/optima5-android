package za.dams.paracrm.explorer;

import za.dams.paracrm.R;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class FileListItemNormal extends FileListItem {
	
	private static final int MAX_LINES = 3 ; 
	
	public FileListItemNormal(Context context) {
		super(context);
	}
	public FileListItemNormal(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public FileListItemNormal(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}	
	
	

	@Override
	public void buildCrmFields(CrmFileManager.CrmFileDesc crmFileDesc) {
		// Note : 3 lignes maximum
		
		mCrmViewGroup.removeAllViews() ;
		mCrmFieldViews.clear() ;
		mCrmFieldDesc.clear() ;
		
		int nbAdded = 0 ;
		for( CrmFileManager.CrmFileFieldDesc cffd : crmFileDesc.fieldsDesc ) {
			if( !cffd.fieldIsHeader ) {
				continue ;
			}
			
			View v = View.inflate(mContext, R.layout.explorer_filelist_item_row_singletext, null) ;
			switch( cffd.fieldType ) {
			case FIELD_DATE:
			case FIELD_DATETIME:
			case FIELD_BIBLE:
				((TextView)v).setTypeface(null, Typeface.BOLD);
				break ;
			default :
				break ;
			}
			mCrmViewGroup.addView(v) ;
			mCrmFieldViews.add(v) ;
			mCrmFieldDesc.add(cffd) ;
			nbAdded++ ;
			
			if( nbAdded >= MAX_LINES ) {
				break ;
			}
		}
	}

	@Override
	public void setCrmValues( CrmFileManager.CrmFileRecord crmFileRecord ) {
		int idx = -1 ;
		for( CrmFileManager.CrmFileFieldDesc cffd :  mCrmFieldDesc ) {
			idx++ ;
			
			if( !crmFileRecord.recordData.containsKey(cffd.fieldCode) ) {
				continue ;
			}
			CrmFileManager.CrmFileFieldValue cffv = crmFileRecord.recordData.get(cffd.fieldCode) ;
			
			View v = mCrmFieldViews.get(idx) ;
			switch( cffd.fieldType ) {
			case FIELD_BIBLE:
				((TextView)v).setText(cffv.displayStr2) ;
				break ;
			default :
				((TextView)v).setText(cffv.displayStr) ;
				break ;
			}
		}
	}

}
