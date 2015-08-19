package za.dams.paracrm.explorer;

import java.util.ArrayList;

import za.dams.paracrm.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public abstract class FileListItem extends LinearLayout {
	
	protected Context mContext ;
	protected ViewGroup mCrmViewGroup ;
	protected ImageView mIcon ;
	protected ArrayList<View> mCrmFieldViews ;
	protected ArrayList<CrmFileManager.CrmFileFieldDesc> mCrmFieldDesc ;

	public FileListItem(Context context) {
		super(context);
		init(context) ;
	}
	public FileListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context) ;
	}
	public FileListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context) ;
	}
	
	private void init(Context c) {
		mContext = c ;
		
		mCrmFieldViews = new ArrayList<View>() ;
		mCrmFieldDesc = new ArrayList<CrmFileManager.CrmFileFieldDesc>() ;
	}
	
	@Override
	protected void onFinishInflate(){
		mCrmViewGroup = (ViewGroup)findViewById(R.id.crm_fields) ;
		mIcon = (ImageView)findViewById(R.id.fileicon) ;
	}
	
	public abstract void buildCrmFields( CrmFileManager.CrmFileDesc crmFileDesc ) ;
	public abstract void setCrmValues( CrmFileManager.CrmFileRecord crmFileRecord ) ;

}
