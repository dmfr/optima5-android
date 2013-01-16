package za.dams.paracrm.explorer;

import za.dams.paracrm.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileListItemWide extends FileListItem {
	public FileListItemWide(Context context) {
		super(context);
	}
	public FileListItemWide(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public FileListItemWide(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	
	@Override
	public void buildCrmFields(CrmFileManager.CrmFileDesc crmFileDesc) {
		mCrmViewGroup.removeAllViews() ;
		mCrmFieldViews.clear() ;
		mCrmFieldDesc.clear() ;
		
		for( CrmFileManager.CrmFileFieldDesc cffd : crmFileDesc.fieldsDesc ) {
			if( !cffd.fieldIsHeader ) {
				continue ;
			}
			
			View v ;
			switch( cffd.fieldType ) {
			case FIELD_BIBLE:
				v = View.inflate(mContext, R.layout.explorer_filelist_item_row_two, null) ;
				v.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 3)) ;
				break ;
			case FIELD_NUMBER:
				v = View.inflate(mContext, R.layout.explorer_filelist_item_row_one, null) ;
				v.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1)) ;
				((TextView)v.findViewById(R.id.text)).setTextAppearance(mContext, android.R.style.TextAppearance_Medium) ;
				break ;
			case FIELD_DATE:
			case FIELD_DATETIME:
				v = View.inflate(mContext, R.layout.explorer_filelist_item_row_one, null) ;
				v.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 2)) ;
				((TextView)v.findViewById(R.id.text)).setTextAppearance(mContext, android.R.style.TextAppearance_Medium) ;
				break ;
			default :
				v = View.inflate(mContext, R.layout.explorer_filelist_item_row_one, null) ;
				v.setLayoutParams(new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 3)) ;
				((TextView)v.findViewById(R.id.text)).setTextAppearance(mContext, android.R.style.TextAppearance_Small) ;
				break ;
			}
			mCrmViewGroup.addView(v) ;
			mCrmFieldViews.add(v) ;
			mCrmFieldDesc.add(cffd) ;
			
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
				((TextView)v.findViewById(R.id.text1)).setText(cffv.displayStr1) ;
				((TextView)v.findViewById(R.id.text2)).setText(cffv.displayStr2) ;
				break ;
			default :
				((TextView)v.findViewById(R.id.text)).setText(cffv.displayStr) ;
				break ;
			}
		}
	}

}
