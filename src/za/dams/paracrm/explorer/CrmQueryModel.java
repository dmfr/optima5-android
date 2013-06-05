package za.dams.paracrm.explorer;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import android.text.format.Time;

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
		public boolean fieldIsOptional ;
		
		public String fieldName ;
		
		public boolean conditionIsSet ;
		public Time conditionDateLt ;
		public Time conditionDateGt ;
		public BibleHelper.BibleEntry conditionBibleEntry ;
		
		public String getQueryConditionFootprint() {
			List<String> strArrCondition = new ArrayList<String>() ;
			strArrCondition.add(String.valueOf(querysrcTargetFieldSsid)) ;
			switch( fieldType ) {
			case FIELD_BIBLE:
				strArrCondition.add( (conditionBibleEntry!=null)? conditionBibleEntry.entryKey:"null" ) ;
				break ;
			case FIELD_DATE:
			case FIELD_DATETIME:
				strArrCondition.add( (conditionDateLt!=null)? conditionDateLt.format("%Y-%m-%d"):"0000-00-00" );
				strArrCondition.add( (conditionDateGt!=null)? conditionDateGt.format("%Y-%m-%d"):"0000-00-00" );
				break ;
			}
			return implode(",",strArrCondition) ;
		}
	}
	
	
	public int querysrcId ;
	public int querysrcIndex ;
	
	public QueryType querysrcType ;
	
	public String querysrcName ;

	public ArrayList<CrmQueryCondition> querysrcWhereConditions = new ArrayList<CrmQueryCondition>() ;
	public ArrayList<CrmQueryCondition> querysrcProgressConditions = new ArrayList<CrmQueryCondition>() ;
	
	
	public String getQueryFootprint() {
		List<String> strArrWhere = new ArrayList<String>() ;
		for( CrmQueryCondition cqc : querysrcWhereConditions ) {
			strArrWhere.add(cqc.getQueryConditionFootprint()) ;
		}
		List<String> strArrProgress = new ArrayList<String>() ;
		for( CrmQueryCondition cqc : querysrcProgressConditions ) {
			strArrProgress.add(cqc.getQueryConditionFootprint()) ;
		}
		
		List<String> strArrQuery = new ArrayList<String>() ;
		strArrQuery.add(String.valueOf(querysrcId)) ;
		strArrQuery.add(implode(";",strArrWhere)) ;
		strArrQuery.add(implode(";",strArrProgress)) ;
		
		return implode("/",strArrQuery);
	}
	private static String implode(String glue, List<String> strArray) {
	    String ret = "";
	    for(int i=0;i<strArray.size();i++)
	    {
	        ret += (i == strArray.size() - 1) ? strArray.get(i) : strArray.get(i) + glue;
	    }
	    return ret;		
	}
}
