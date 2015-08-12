package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.R;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class BibleGridItem extends LinearLayout {
	
	protected Context mContext ;
	protected ViewGroup mCrmViewGroup ;
	protected ArrayList<View> mCrmFieldViews ;
	protected ArrayList<CrmBibleManager.CrmBibleFieldDesc> mCrmFieldDesc ;

	public BibleGridItem(Context context) {
		super(context);
		init(context) ;
	}
	public BibleGridItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context) ;
	}
	public BibleGridItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context) ;
	}
	
	private void init(Context c) {
		mContext = c ;
		
		mCrmFieldViews = new ArrayList<View>() ;
		mCrmFieldDesc = new ArrayList<CrmBibleManager.CrmBibleFieldDesc>() ;
	}
	
	@Override
	protected void onFinishInflate(){
		mCrmViewGroup = (ViewGroup)findViewById(R.id.crm_fields) ;
	}
	
	public void buildCrmFields( CrmBibleManager.CrmBibleDesc crmBibleDesc ) {
		mCrmViewGroup.removeAllViews() ;
		mCrmFieldViews.clear() ;
		mCrmFieldDesc.clear() ;
		
		boolean isFirst = true ;
		for( CrmBibleManager.CrmBibleFieldDesc cffd : crmBibleDesc.fieldsDesc ) {
			if( !cffd.fieldIsHeader ) {
				continue ;
			}
			
			View v ;
			switch( cffd.fieldType ) {
			default :
				v = View.inflate(mContext, R.layout.explorer_biblegrid_item_row, null) ;
				if( isFirst ) {
					((TextView)v).setTypeface(null, Typeface.BOLD);
				}
				((TextView)v).setTextAppearance(mContext, android.R.style.TextAppearance_Small) ;
				break ;
			}
			mCrmViewGroup.addView(v) ;
			mCrmFieldViews.add(v) ;
			mCrmFieldDesc.add(cffd) ;
			isFirst = false ;
		}
	}
	public void setCrmValues( CrmBibleManager.CrmBibleRecord crmBibleRecord ) {
		
		int idx = -1 ;
		for( CrmBibleManager.CrmBibleFieldDesc cffd :  mCrmFieldDesc ) {
			idx++ ;
			
			if( !crmBibleRecord.recordData.containsKey(cffd.fieldCode) ) {
				continue ;
			}
			CrmBibleManager.CrmBibleFieldValue cffv = crmBibleRecord.recordData.get(cffd.fieldCode) ;
			
			View v = mCrmFieldViews.get(idx) ;
			
			switch( cffd.fieldType ) {
			default :
				((TextView)v.findViewById(R.id.text)).setText(cffv.displayStr) ;
				break ;
			}
		}
	}

}
