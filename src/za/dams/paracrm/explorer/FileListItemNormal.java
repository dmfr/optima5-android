package za.dams.paracrm.explorer;

import android.content.Context;
import android.util.AttributeSet;

public class FileListItemNormal extends FileListItem {
	
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
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void setCrmValues( CrmFileManager.CrmFileRecord crmFileRecord ) {
		// TODO Auto-generated method stub

	}

}
