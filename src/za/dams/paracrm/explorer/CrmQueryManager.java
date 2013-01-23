package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.DatabaseManager;
import android.content.Context;
import android.database.Cursor;

public class CrmQueryManager {
	
	public static List<CrmQueryModel> model_getAll( Context context ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		
		ArrayList<CrmQueryModel> crmQueryModels = new ArrayList<CrmQueryModel>() ;
		
		Cursor c = mDb.rawQuery("SELECT querysrc_id FROM input_query ORDER BY querysrc_index") ;
		while( c.moveToNext() ) {
			int querysrcId = c.getInt(0) ;
			CrmQueryModel cqm = model_get( context,querysrcId ) ;
			if( cqm != null ) {
				crmQueryModels.add(cqm) ;
			}
		}
		c.close() ;
		
		return crmQueryModels ;
	}
	public static CrmQueryModel model_get( Context context, int querysrcId ) {
		DatabaseManager mDb = DatabaseManager.getInstance(context) ;
		Cursor c ;
		c = mDb.rawQuery(String.format("SELECT querysrc_id, querysrc_index, querysrc_type, querysrc_name FROM input_query WHERE querysrc_id='%s'",querysrcId));
		if( c.getCount() != 1 ){
			c.close() ;
			return null ;
		}
		c.moveToNext() ;
		CrmQueryModel crmQueryModel = new CrmQueryModel() ;
		crmQueryModel.querysrcId = c.getInt(0) ;
		crmQueryModel.querysrcIndex = c.getInt(1) ;
		String strType = c.getString(2) ;
		if( strType.equals("query") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QUERY ;
		} else if( strType.equals("qmerge") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QMERGE ;
		} else if( strType.equals("qweb") ) {
			crmQueryModel.querysrcType = CrmQueryModel.QueryType.QWEB ;
		}
		crmQueryModel.querysrcName = c.getString(3) ;
		c.close() ;
		
		c = mDb.rawQuery(String.format("SELECT querysrc_targetfield_ssid, field_type, field_linkbible, field_lib FROM input_query_where WHERE querysrc_id='%s' ORDER BY querysrc_targetfield_ssid",querysrcId));
		while( c.moveToNext() ) {
			CrmQueryModel.CrmQueryCondition cqc = new CrmQueryModel.CrmQueryCondition() ;
			cqc.querysrcTargetFieldSsid = c.getInt(0) ;
			String fieldType = c.getString(1) ;
			if( fieldType.equals("date") ) {
				cqc.fieldType = CrmQueryModel.FieldType.FIELD_DATE ;
			} else if( fieldType.equals("link") ) {
				cqc.fieldType = CrmQueryModel.FieldType.FIELD_BIBLE ;
			} else {
				continue ;
			}
			cqc.fieldLinkBible = c.getString(2) ;
			cqc.fieldName = c.getString(3);
			cqc.conditionIsSet=false ;
			
			crmQueryModel.querysrcConditions.add(cqc) ;
		}
		c.close() ;
		return crmQueryModel ;
	}

}
