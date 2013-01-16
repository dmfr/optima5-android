package za.dams.paracrm.explorer;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public abstract class FileListItem extends LinearLayout {

	public FileListItem(Context context) {
		super(context);
	}
	public FileListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public FileListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public abstract void buildCrmFields( CrmFileManager.CrmFileDesc crmFileDesc ) ;
	public abstract void setCrmValues() ;

}
