package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.Date;

import za.dams.paracrm.BibleHelper;

public class CrmQueryModel {
	
	public static enum QueryType {
	    QUERY, QMERGE, QWEB
	};
	public static enum FieldType {
	    FIELD_TEXT, FIELD_NUMBER, FIELD_DATE, FIELD_DATETIME, FIELD_BIBLE, FIELD_NULL
	};
	public static class CrmQueryCondition {
		
		public int querysrcTargetFieldSsid ;
		
		public FieldType fieldType ;
		public String fieldLinkBible ;
		
		public String fieldName ;
		
		public boolean conditionIsSet ;
		public Date conditionDateLt ;
		public Date conditionDateGt ;
		public BibleHelper.BibleEntry conditionBibleEntry ;
	}
	
	
	public int querysrcId ;
	public int querysrcIndex ;
	
	public QueryType querysrcType ;
	
	public String querysrcName ;

	public ArrayList<CrmQueryCondition> querysrcConditions = new ArrayList<CrmQueryCondition>() ;
}
