package za.dams.paracrm.explorer.xpressfile;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.DatabaseManager;
import android.content.Context;
import android.database.Cursor;

public class CrmXpressfileManager {
	private static final String TAG = "Xpressfile/CrmXpressfileManager";
	
	public static class CrmXpressfileInput {
		public int mCrmInputId ;
		public boolean mIsHidden ;
		
		public String mTargetFilecode ;
		public String mTargetLib ;
		
		public String mTargetPrimarykeyFileField ;
	}
	
	public static List<CrmXpressfileInput> inputsList( Context context ) {
		return inputsList(context,null) ;
	}
	public static List<CrmXpressfileInput> inputsList( Context context, Integer xpressfileInputId ) {
		List<CrmXpressfileInput> mInputs = new ArrayList<CrmXpressfileInput>();
		
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		Cursor tCursor = mDb.rawQuery("SELECT input.xpressfile_id, def.file_code , def.file_lib, input.target_primarykey_fieldcode, input.xpressfile_is_hidden " +
				" FROM input_xpressfile input , define_file def " +
				" WHERE input.target_filecode=def.file_code" +
				" ORDER BY target_filecode ASC") ;
		while( tCursor.moveToNext() ) {
			if( xpressfileInputId!=null && tCursor.getInt(0) != xpressfileInputId ) {
				continue ;
			}
			
			CrmXpressfileInput cxi = new CrmXpressfileInput() ;
			cxi.mCrmInputId = tCursor.getInt(0) ;
			cxi.mIsHidden = tCursor.getString(4).equals("O") ;
			cxi.mTargetFilecode = tCursor.getString(1) ;
			cxi.mTargetLib = tCursor.getString(2) ;
			cxi.mTargetPrimarykeyFileField = tCursor.getString(3) ;
			mInputs.add( cxi ) ;
		}
		tCursor.close() ;
		
		return mInputs ;
	}

}
