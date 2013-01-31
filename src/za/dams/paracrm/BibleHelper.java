package za.dams.paracrm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class BibleHelper {
	
    public final static String TAG = "PARACRM/BibleHelper";

    private DatabaseManager mDb ;
    
    private HashMap<BibleCode,ArrayList<BibleFieldCode>> mapBible ;
    private HashMap<BibleCode,HashMap<BibleFieldCode,BibleCode>> mapForeignLinks ;
    
	public enum RecordType {
		BIBLE_TREENODE, BIBLE_ENTRY
	}
	public enum FieldType {
		FIELD_STRING, FIELD_NUMBER, FIELD_LINKBIBLE, FIELD_DATE
	}
    
    public static class BibleCode {
    	public String bibleCode ;
    	public BibleCode( String bibleCode ){
    		this.bibleCode = bibleCode ;
    	}
    	
    	public boolean equals( Object bc ) {
    		return this.bibleCode.equals( ((BibleCode)bc).bibleCode) ;
    	}
    	public int hashCode(){
    		return this.bibleCode.hashCode() ;
    	}
    }
    public static class BibleFieldCode {
    	public RecordType recordType ;
    	public String fieldCode ;
    	public FieldType fieldType ;
    	public String linkBible ;
    	public boolean isKey ;
    	public boolean isHeader ;
    	public boolean isHighlight ;
    	public BibleFieldCode( RecordType recordType, String fieldCode ){
    		this.recordType = recordType ;
    		this.fieldCode = fieldCode ;
    	}
    	public BibleFieldCode( RecordType recordType, String fieldCode,
    			FieldType fieldType, String linkBible, boolean isKey, boolean isHeader, boolean isHighlight ){
    		this.recordType = recordType ;
    		this.fieldCode = fieldCode ;
    		this.fieldType = fieldType ;
    		this.linkBible = linkBible ;
    		this.isKey = isKey ;
    		this.isHeader = isHeader ;
    		this.isHighlight = isHighlight ;
    	}
    	
    	public boolean equals( Object bfc ) {
    		if( this.recordType == ((BibleFieldCode)bfc).recordType
    				&& this.fieldCode.equals( ((BibleFieldCode)bfc).fieldCode) ){
    			return true;
    		}
    		return false ;
    	}
    	public int hashCode(){
    		int result = 17 ;
    		result = 31 * result + recordType.ordinal();
    		result = 31 * result + fieldCode.hashCode() ;
    		return result ;
    	}
    }
    
    
    
    
    public static class BibleEntry implements Cloneable, Parcelable {
    	public String bibleCode ;
    	public String treenodeKey ;
    	public String entryKey ;
    	public String displayStr ;
    	public String displayStr1 ;
    	public String displayStr2 ;
    	
    	public BibleEntry( String bibleCode, String treenodeKey, String entryKey ) {
    		this.bibleCode = bibleCode ;
    		this.treenodeKey = treenodeKey ;
    		this.entryKey = entryKey ;
    		this.displayStr = "" ;
    		this.displayStr1 = "" ;
    		this.displayStr2 = "" ;
    	}
    	public BibleEntry( String bibleCode, String treenodeKey, String entryKey, String displayStr, String displayStr1, String displayStr2 ) {
    		this.bibleCode = bibleCode ;
    		this.treenodeKey = treenodeKey ;
    		this.entryKey = entryKey ;
    		this.displayStr = displayStr ;
    		this.displayStr1 = displayStr1 ;
    		this.displayStr2 = displayStr2 ;
    	}
    	public BibleEntry( Parcel in ) {
    		this.bibleCode = in.readString() ;
    		this.treenodeKey = in.readString() ;
    		this.entryKey = in.readString() ;
    		this.displayStr = in.readString() ;
    		this.displayStr1 = in.readString() ;
    		this.displayStr2 = in.readString() ;
    	}
    	public void setDisplayStr(String displayStr, String displayStr1, String displayStr2 ) {
    		this.displayStr = displayStr ;
    		this.displayStr1 = displayStr1 ;
    		this.displayStr2 = displayStr2 ;
    	}
    	public BibleEntry clone() {
    		Object o = null;
    		try {
    			// On récupère l'instance à renvoyer par l'appel de la 
    			// méthode super.clone()
    			o = super.clone();
    		} catch(CloneNotSupportedException cnse) {
    			// Ne devrait jamais arriver car nous implémentons 
    			// l'interface Cloneable
    			// cnse.printStackTrace(System.err);
    		}
    		// on renvoie le clone
    		return (BibleEntry)o;
    	}
    	public boolean equals( Object be ) {
    		if( be == null ) {
    			return false ;
    		}
    		if( this.bibleCode.equals( ((BibleEntry)be).bibleCode )
    				&& this.entryKey.equals( ((BibleEntry)be).entryKey) ){
    			return true;
    		}
    		return false ;
    	}
    	public int hashCode(){
    		int result = 17 ;
    		result = 31 * result + bibleCode.hashCode();
    		result = 31 * result + entryKey.hashCode() ;
    		return result ;
    	}
    	
		@Override
		public int describeContents() {
			return 0;
		}
		@Override
		public void writeToParcel(Parcel out, int arg1) {
			out.writeString(this.bibleCode);
			out.writeString(this.treenodeKey);
			out.writeString(this.entryKey);
			out.writeString(this.displayStr);
			out.writeString(this.displayStr1);
			out.writeString(this.displayStr2);
		}
		
		public static Parcelable.Creator<BibleEntry> CREATOR =
				new Parcelable.Creator<BibleEntry>() {
			@Override
			public BibleEntry createFromParcel(Parcel source) {
				return new BibleEntry(source);
			}

			@Override
			public BibleEntry[] newArray(int size) {
				return new BibleEntry[size];
			}
		};
    }
    
    
    
    public static String getBibleLib( Context c, String strBibleCode ) {
		DatabaseManager mDb = DatabaseManager.getInstance(c) ;
		Cursor cursor = mDb.rawQuery(String.format("SELECT bible_lib FROM define_bible WHERE bible_code='%s'",strBibleCode)) ;
    	if( cursor.getCount() != 1 ) {
    		cursor.close() ;
    		return null ;
    	}
    	cursor.moveToNext() ;
    	String retValue = cursor.getString(0) ;
    	cursor.close();
    	return retValue ;
    }
    
    
    
    public BibleHelper(Context c) {
    	mDb = DatabaseManager.getInstance(c) ;
    	loadBiblesDefinition() ;
    }
    private void loadBiblesDefinition(){
    	mapBible = new HashMap<BibleCode,ArrayList<BibleFieldCode>>() ;
    	mapForeignLinks = new HashMap<BibleCode,HashMap<BibleFieldCode,BibleCode>>() ;
    	
    	
    	Cursor c ;
    	c = mDb.rawQuery("SELECT bible_code FROM define_bible") ;
    	BibleCode[] bibles = new BibleCode[c.getCount()] ; 
    	int a = 0 ;
    	while( c.moveToNext() ) {
    		bibles[a] = new BibleCode(c.getString(0)) ;
    		a++ ;
    	}
    	c.close() ;
    	
    	for( a=0 ; a<bibles.length ; a++ ) {
    		BibleCode bibleCode = bibles[a] ;
    		Cursor tmpCursor ;

    		ArrayList<BibleFieldCode> mMap = new ArrayList<BibleFieldCode>();
    		HashMap<BibleFieldCode,BibleCode> mForeign = new HashMap<BibleFieldCode,BibleCode>();
   		
    		
    		mMap.add(new BibleFieldCode(RecordType.BIBLE_TREENODE,"treenode_parent_key")) ;
    		mMap.add(new BibleFieldCode(RecordType.BIBLE_TREENODE,"treenode_key")) ;
    		tmpCursor = mDb.rawQuery( String.format("SELECT tree_field_code,tree_field_type,tree_field_linkbible,tree_field_is_key,tree_field_is_header,tree_field_is_highlight FROM define_bible_tree WHERE bible_code='%s' ORDER BY tree_field_index",bibleCode.bibleCode) ) ;
    		while( tmpCursor.moveToNext() ){
    			
    			FieldType tFieldType ;
        		if( tmpCursor.getString(1).equals("number") ) {
        			tFieldType = FieldType.FIELD_NUMBER ;
        		}
        		else {
        			if( tmpCursor.getString(1).equals("date") ) {
        				tFieldType = FieldType.FIELD_DATE ;
        			}
        			else{
            			if( tmpCursor.getString(1).equals("link") ) {
            				tFieldType = FieldType.FIELD_LINKBIBLE ;
            			}
            			else{
            				tFieldType = FieldType.FIELD_STRING ;
            			}
        			}
        		}
    			
        		BibleFieldCode bfc = new BibleFieldCode(
        				RecordType.BIBLE_TREENODE,tmpCursor.getString(0),
        				tFieldType,
        				tmpCursor.getString(2),
        				tmpCursor.getString(3).equals("O") ? true : false ,
                		tmpCursor.getString(4).equals("O") ? true : false ,
        				tmpCursor.getString(5).equals("O") ? true : false
        				);

    			
    			mMap.add(bfc) ;
    			
    			if( tmpCursor.getString(1).equals("link") ){
    				mForeign.put(bfc,new BibleCode(tmpCursor.getString(2))) ;
    			}
    		}
    		tmpCursor.close() ;
    		
    		
    		
    		mMap.add(new BibleFieldCode(RecordType.BIBLE_ENTRY,"treenode_key")) ;
    		mMap.add(new BibleFieldCode(RecordType.BIBLE_ENTRY,"entry_key")) ;
    		tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_code,entry_field_type,entry_field_linkbible,entry_field_is_key,entry_field_is_header,entry_field_is_highlight FROM define_bible_entry WHERE bible_code='%s' ORDER BY entry_field_index",bibleCode.bibleCode) ) ;
    		while( tmpCursor.moveToNext() ){
    			
    			FieldType tFieldType ;
        		if( tmpCursor.getString(1).equals("number") ) {
        			tFieldType = FieldType.FIELD_NUMBER ;
        		}
        		else {
        			if( tmpCursor.getString(1).equals("date") ) {
        				tFieldType = FieldType.FIELD_DATE ;
        			}
        			else{
            			if( tmpCursor.getString(1).equals("link") ) {
            				tFieldType = FieldType.FIELD_LINKBIBLE ;
            			}
            			else{
            				tFieldType = FieldType.FIELD_STRING ;
            			}
        			}
        		}
    			
        		BibleFieldCode bfc = new BibleFieldCode(
        				RecordType.BIBLE_ENTRY,tmpCursor.getString(0),
        				tFieldType,
        				tmpCursor.getString(2),
        				tmpCursor.getString(3).equals("O") ? true : false ,
                		tmpCursor.getString(4).equals("O") ? true : false ,
        				tmpCursor.getString(5).equals("O") ? true : false
        				);

    			
    			mMap.add(bfc) ;
    			
    			if( tmpCursor.getString(1).equals("link") ){
    				mForeign.put(bfc,new BibleCode(tmpCursor.getString(2))) ;
    			}
    		}
    		tmpCursor.close() ;

    		mapBible.put(bibleCode,mMap) ;
    		mapForeignLinks.put(bibleCode,mForeign) ;
    	}
    }
    
    
    
    public BibleEntry getBibleEntry( String bibleCode, String entryKey ) {
    	
    	//Log.w(TAG, String.format("SELECT treenode_key FROM store_bible_entry WHERE bible_code='%s' AND entry_key='%s'",bibleCode,entryKey));
    	Cursor c = mDb.rawQuery(String.format("SELECT treenode_key FROM store_bible_entry WHERE bible_code='%s' AND entry_key='%s'",bibleCode,entryKey));
    	if( c.getCount() == 0 ) {
    		c.close() ;
    		return null ;
    	}
    	c.moveToNext() ;
    	BibleEntry be = new BibleEntry(bibleCode,c.getString(0),entryKey) ;
    	c.close() ;
    	
       	HashMap<String,BibleFieldCode> tmpMapFields = new HashMap<String,BibleFieldCode>() ;
		for( BibleFieldCode bfc : mapBible.get(new BibleCode(be.bibleCode)) ) {
       		if( bfc.recordType == RecordType.BIBLE_ENTRY ) 
       			tmpMapFields.put(bfc.fieldCode,bfc) ;
       	}
    	
    	
  
    	c = mDb.rawQuery(String.format("SELECT entry_field_code," +
    			"entry_field_value_string,entry_field_value_number,entry_field_value_date" +
    			" FROM store_bible_entry_field " +
    			"WHERE bible_code='%s' AND entry_key='%s'",bibleCode,entryKey));
		HashMap<String,String> tmpBufSub = new HashMap<String,String>() ;
		tmpBufSub.put("treenode_key", be.treenodeKey) ;
		tmpBufSub.put("entry_key", be.entryKey) ;
    	while( c.moveToNext() ){
    		BibleFieldCode bfc = tmpMapFields.get(c.getString(0)) ;
    		if( bfc == null ) {
    			// Log.w(TAG, "Pagination for field "+c.getString(2)) ;
    			continue ;
    		}
    		switch(bfc.fieldType) {
    		case FIELD_STRING :
    			tmpBufSub.put("field_"+bfc.fieldCode,c.getString(1)) ;
    			break ;
    		case FIELD_NUMBER :
    			tmpBufSub.put("field_"+bfc.fieldCode,c.getString(2)) ;
    			break ;
    		case FIELD_DATE :
    			tmpBufSub.put("field_"+bfc.fieldCode,c.getString(3)) ;
    			break ;
    		}
		}
    	c.close() ;
    	return prettifyEntry(be,tmpBufSub) ;
    }
    
    private BibleEntry prettifyEntry( BibleEntry bibleEntry , HashMap<String,String> bibleEntryFields ) {
    	ArrayList<String> pretty1 = new ArrayList<String>() ;
    	pretty1.add("("+bibleEntryFields.get("treenode_key")+")") ;
    	pretty1.add(bibleEntryFields.get("entry_key")) ;
    	
		ArrayList<String> pretty2 = new ArrayList<String>() ;
       	for( BibleFieldCode bfc : mapBible.get(new BibleCode(bibleEntry.bibleCode)) ) {
       		if( bfc.recordType == RecordType.BIBLE_ENTRY
       				&& !bfc.isKey && bfc.isHeader )
       		{
       			pretty2.add(bibleEntryFields.get("field_"+bfc.fieldCode)) ;
       		}
       	}
       	
       	ArrayList<String> pretty = new ArrayList<String>() ;
       	for( BibleFieldCode bfc : mapBible.get(new BibleCode(bibleEntry.bibleCode)) ) {
       		if( bfc.recordType == RecordType.BIBLE_ENTRY && bfc.isHeader )
       		{
       			pretty.add(bibleEntryFields.get("field_"+bfc.fieldCode)) ;
       		}
       	}
       	
    	
       	bibleEntry.setDisplayStr(
				implodeArray(pretty.toArray(new String[pretty.size()]), " "),
				implodeArray(pretty1.toArray(new String[pretty1.size()]), " "),
				implodeArray(pretty2.toArray(new String[pretty2.size()]), " ")) ;
    	
    	return bibleEntry ;
    }
    
    
    
    
    
    
    
    private String getSqlConditionLocal( BibleCode localBible, BibleFieldCode localTargetField, ArrayList<BibleEntry> foreignEntries ) {
    	
    	ArrayList<String> foreignTreenodekeys = new ArrayList<String>() ;
    	for( BibleEntry foreignEntry : foreignEntries ) {
    		if( !localTargetField.linkBible.equals( foreignEntry.bibleCode ) ) {
    			return null ;
    		}
    		foreignTreenodekeys.add("'"+foreignEntry.treenodeKey+"'") ;
    	}
    	String sqlTreenodekeys = "("+implodeArray(foreignTreenodekeys.toArray(new String[foreignTreenodekeys.size()]), ",")+")" ;
    	
    	
    	
    	StringBuilder sb = new StringBuilder() ;
    	sb.append(" AND entry_key IN (") ;
    		sb.append(" SELECT entry_key FROM") ;
    		switch( localTargetField.recordType ) {
    		case BIBLE_TREENODE :
    			sb.append(" store_bible_tree_field_linkmembers") ;
    			break ;
    		case BIBLE_ENTRY :
    			sb.append(" store_bible_entry_field_linkmembers") ;
    			break ;
    		}
    		sb.append(String.format(" WHERE bible_code='%s'",localBible.bibleCode)) ;
    		switch( localTargetField.recordType ) {
    		case BIBLE_TREENODE :
    			sb.append(String.format(" AND tree_field_code='%s'",localTargetField.fieldCode)) ;
    			sb.append(String.format(" AND treenode_field_linkmember_treenodekey IN %s",sqlTreenodekeys)) ;
   			break ;
    		case BIBLE_ENTRY :
    			sb.append(String.format(" AND entry_field_code='%s'",localTargetField.fieldCode)) ;
    			sb.append(String.format(" AND entry_field_linkmember_treenodekey IN %s",sqlTreenodekeys)) ;
    			break ;
    		}
    	sb.append(")") ;
    	return sb.toString() ;
    }
    
    
    private String getSqlConditionForeign( BibleCode localBible, BibleFieldCode foreignTargetField, ArrayList<BibleEntry> foreignEntries ) {
    	if( !foreignTargetField.linkBible.equals( localBible.bibleCode ) ) {
    		return null ;
    	}
    	String foreignBiblecode = null ;
    	for( BibleEntry foreignEntry : foreignEntries ) {
    		foreignBiblecode = foreignEntry.bibleCode ;
    	}
    	if( foreignBiblecode==null ){
    		return null ;
    	}
    	
    	
    	StringBuilder sb = new StringBuilder() ;
    	sb.append(" AND treenode_key IN (") ;
		switch( foreignTargetField.recordType ) {
		case BIBLE_TREENODE :
	    	ArrayList<String> foreignTreenodekeys = new ArrayList<String>() ;
	    	for( BibleEntry foreignEntry : foreignEntries ) {
	    		foreignTreenodekeys.add("'"+foreignEntry.treenodeKey+"'") ;
	    	}
	    	String sqlTreenodekeys = "("+implodeArray(foreignTreenodekeys.toArray(new String[foreignTreenodekeys.size()]), ",")+")" ;
	    	
			sb.append("select treenode_field_linkmember_treenodekey FROM store_bible_tree_field_linkmembers") ;
			sb.append(String.format(
					" WHERE bible_code='%s' AND treenode_key IN %s AND treenode_field_code='%s'",
					foreignBiblecode,
					sqlTreenodekeys,
					foreignTargetField.fieldCode
					));
			break ;
		case BIBLE_ENTRY :
	    	ArrayList<String> foreignEntrykeys = new ArrayList<String>() ;
	    	for( BibleEntry foreignEntry : foreignEntries ) {
	    		foreignEntrykeys.add("'"+foreignEntry.entryKey+"'") ;
	    	}
	    	String sqlEntrykeys = "("+implodeArray(foreignEntrykeys.toArray(new String[foreignEntrykeys.size()]), ",")+")" ;

	    	sb.append("select entry_field_linkmember_treenodekey FROM store_bible_entry_field_linkmembers") ;
			sb.append(String.format(
					" WHERE bible_code='%s' AND entry_key IN %s AND entry_field_code='%s'",
					foreignBiblecode,
					sqlEntrykeys,
					foreignTargetField.fieldCode
					));
			break ;
		}
    	sb.append(")") ;
    	return sb.toString() ;
    }
    
    

    
    public ArrayList<BibleEntry> queryBible( String strBibleCode, ArrayList<BibleEntry> mForeignEntries, String searchStr, int limit ) {
    	
    	BibleCode localBible = new BibleCode(strBibleCode) ;
    	
    	
    	ArrayList<BibleEntry> localBounds = null ;
    	
    	// ******* Construction de la requête sur entête *********
    	StringBuilder sbEnt = new StringBuilder() ;
    	sbEnt.append("SELECT * FROM store_bible_entry WHERE 1") ;
    	sbEnt.append(String.format(" AND bible_code='%s'",localBible.bibleCode)) ;
    	if( mForeignEntries != null ) {
    		// ***** Process de mForeignEntries ******
    		
    		// on groupe les entries par foreignbible (UNION)
    		HashMap<String,ArrayList<BibleEntry>> tForeignEntriesByBible = new HashMap<String,ArrayList<BibleEntry>>() ;
	    	for( BibleEntry foreignEntry : mForeignEntries ) {
	    		BibleCode foreignBible = new BibleCode(foreignEntry.bibleCode) ;
	    		
	    		// on groupe les entries par foreignbible 
	    		if( !tForeignEntriesByBible.containsKey(foreignEntry.bibleCode) ) {
	    			tForeignEntriesByBible.put( foreignEntry.bibleCode, new ArrayList<BibleEntry>() ) ;
	    		}
	    		tForeignEntriesByBible.get(foreignEntry.bibleCode).add(foreignEntry) ;
	    	}
	    	
	    	for( Map.Entry<String,ArrayList<BibleEntry>> map_Biblecode_BibleEntry : tForeignEntriesByBible.entrySet() ) {	
	    		BibleCode foreignBible = new BibleCode(map_Biblecode_BibleEntry.getKey()) ;
	    		ArrayList<BibleEntry> foreignEntries = map_Biblecode_BibleEntry.getValue() ;
	    		
	    		//Log.w(TAG,"Foreign "+foreignEntry.entryKey+" tree: "+foreignEntry.treenodeKey) ;
	    		
	    		// condition locale ?  ex: req STORE (condition SALES)
	    		if( mapForeignLinks.get(localBible).containsValue(foreignBible) ) {
	    			BibleFieldCode localTargetField = getKeyByValue(mapForeignLinks.get(localBible),foreignBible) ;
	    			sbEnt.append(getSqlConditionLocal(localBible,localTargetField,foreignEntries)) ;
	    			break ;
	    		}
	    		
	    		// condition étrangère ? ex: req PROD (condition STORE)
	    		if( mapForeignLinks.get(foreignBible).containsValue(localBible) ) {
	    			BibleFieldCode foreignTargetField = getKeyByValue(mapForeignLinks.get(foreignBible),localBible) ;
	    			sbEnt.append(getSqlConditionForeign(localBible,foreignTargetField,foreignEntries)) ;
	    			break ;
	    		}
	    		
	    		if( foreignBible.equals(localBible) ) {
	    			if( localBounds==null ) {
	    				localBounds = new ArrayList<BibleEntry>() ;
	    			}
	    			localBounds.addAll(foreignEntries) ;
	    		}
	    	}
    	}
    	if( searchStr != null ) {
	    	// ***** Process du search *******
    		for( String subStr : searchStr.split(" ") ) {
    			subStr = subStr.replace("'","''"); 
    			sbEnt.append(String.format(" AND entry_key IN (" +
    					"select entry_key FROM store_bible_entry_field " +
    					"WHERE bible_code='%s' " +
    					"AND entry_field_value_string LIKE '%%%s%%')",
    					localBible.bibleCode,
    					subStr)) ;
    		}
    	}
    	sbEnt.append(" ORDER by treenode_key,entry_key") ;
    	if( limit > 0 ) {
    		sbEnt.append(String.format(" LIMIT %d",limit)) ;
    	}
    	
    	// ******* Construction de la requete complete *********
    	StringBuilder sbFull = new StringBuilder() ;
    	sbFull.append("select e.treenode_key, e.entry_key," +
    			" ef.entry_field_code, ef.entry_field_value_string, ef.entry_field_value_number, ef.entry_field_value_date " +
    			"from (") ;
    	sbFull.append(sbEnt.toString()) ;
    	sbFull.append(") e") ;
       	sbFull.append(" left outer join store_bible_entry_field ef ON ef.bible_code=e.bible_code AND ef.entry_key=e.entry_key") ;
       	sbFull.append(" ORDER BY e.treenode_key, e.entry_key") ;
       	
       	
       	//Log.w(TAG,sbFull.toString()) ;
       	
       	Cursor c = mDb.rawQuery(sbFull.toString()) ;
       	HashMap<String,BibleFieldCode> tmpMapFields = new HashMap<String,BibleFieldCode>() ;
       	for( BibleFieldCode bfc : mapBible.get(localBible) ) {
       		if( bfc.recordType == RecordType.BIBLE_ENTRY ) 
       			tmpMapFields.put(bfc.fieldCode,bfc) ;
       	}
       	ArrayList<String> tmpOrder = new ArrayList<String>() ;
       	HashMap<String,HashMap<String,String>> tmpBuffer = new HashMap<String,HashMap<String,String>>() ;
       	while( c.moveToNext() ) {
       		String treenodeKey = c.getString(0) ;
       		String entryKey = c.getString(1) ;
       		if( tmpBuffer.get(entryKey) == null ){
       			tmpOrder.add(entryKey) ;
       			
       			HashMap<String,String> tmpBufSub = new HashMap<String,String>() ;
       			tmpBufSub.put("treenode_key", treenodeKey) ;
       			tmpBufSub.put("entry_key", entryKey) ;
       			tmpBuffer.put(entryKey,tmpBufSub) ;
       		}
       		BibleFieldCode bfc = tmpMapFields.get(c.getString(2)) ;
       		if( bfc == null ) {
       			// Log.w(TAG, "Pagination for field "+c.getString(2)) ;
       			continue ;
       		}
       		switch(bfc.fieldType) {
       		case FIELD_STRING :
       			tmpBuffer.get(entryKey).put("field_"+bfc.fieldCode,c.getString(3)) ;
       			break ;
       		case FIELD_NUMBER :
       			tmpBuffer.get(entryKey).put("field_"+bfc.fieldCode,c.getString(4)) ;
       			break ;
       		case FIELD_DATE :
       			tmpBuffer.get(entryKey).put("field_"+bfc.fieldCode,c.getString(5)) ;
       			break ;
       		}
       	}
       	c.close() ;
       	
       	ArrayList<BibleEntry> retCollection = new ArrayList<BibleEntry>() ;
       	for( String entryKey : tmpOrder ) {
       		HashMap<String,String> tmpBufSub =  tmpBuffer.get(entryKey) ;
       		
       		BibleEntry be = new BibleEntry(localBible.bibleCode,tmpBufSub.get("treenode_key"),tmpBufSub.get("entry_key")) ;
       		if( localBounds != null && !localBounds.contains(be) ) {
       			continue ;
       		}
       		prettifyEntry(be,tmpBufSub) ;
       		retCollection.add(prettifyEntry(be,tmpBufSub)) ;
       	}
           	
    	
    	return retCollection ;
    }
    public ArrayList<BibleEntry> queryBible( String bibleCode, ArrayList<BibleEntry> mForeignEntries, String searchStr ) {
    	return queryBible( bibleCode, mForeignEntries, searchStr, 0 ) ;
    }
    public ArrayList<BibleEntry> queryBible( String bibleCode, ArrayList<BibleEntry> mForeignEntries ) {
    	return queryBible( bibleCode, mForeignEntries, null ) ;
    }
    public ArrayList<BibleEntry> queryBible( String bibleCode ) {
    	return queryBible( bibleCode, new ArrayList<BibleEntry>() ) ;
    }
    
    
    
    
    
    
    private class buildCacheTableCV {
    	public String tableName ;
    	public ContentValues cv ;
    	
    	public buildCacheTableCV(String tableName, ContentValues cv) {
    		this.tableName = tableName ;
    		this.cv = cv ;
    	}
    }
    
    
    public void buildCaches() {
    	buildCaches_dbClean() ;
    	
    	
    	Cursor c ;
    	c = mDb.rawQuery("SELECT bible_code FROM define_bible") ;
    	BibleCode[] bibles = new BibleCode[c.getCount()] ; 
    	int a = 0 ;
    	while( c.moveToNext() ) {
    		bibles[a] = new BibleCode(c.getString(0)) ;
    		a++;
    	}
    	c.close() ;
    	
    	
    	HashMap<BibleCode,Tree<String>> bibleTreemaps = new HashMap<BibleCode,Tree<String>>() ;
    	// ******** Pour chaque bible ***********
    	//   => création de l'arbre (treenodes)
    	for( a=0 ; a<bibles.length ; a++ ) {
    		BibleCode bibleCode = bibles[a] ;
    		
    		int nbPushed = 0 ;
    		int nbPushedThispass = 0 ;
    		
    		Tree<String> bibleTree = new Tree<String>("&");
    		
    		c = mDb.rawQuery(String.format("SELECT treenode_key, treenode_parent_key FROM store_bible_tree WHERE bible_code='%s'",bibleCode.bibleCode)) ;
    		do {
    			nbPushedThispass = 0 ;
    			for( c.moveToPosition(-1) ; !c.isLast() ; ) {
    				c.moveToNext() ;
    				
    				String treenodeParentKey = c.getString(1) ;
    				if( treenodeParentKey.equals("") ){
    					treenodeParentKey = "&" ;
    				}
    				String treenodeKey = c.getString(0) ;
    				if( treenodeKey.equals("") ){
    					continue ;
    				}
    				
    				if( bibleTree.getTree(treenodeParentKey) != null && bibleTree.getTree(treenodeKey) == null ) {
    					bibleTree.addLeaf(treenodeParentKey, treenodeKey) ;
    					nbPushedThispass++ ;
    					nbPushed++ ;
    				}
    			}
    			if( nbPushed >= c.getCount() ) {
    				break ;
    			}
    		}
    		while( nbPushedThispass > 0 ) ;
    		
    		c.close() ;
    		
    		//Log.w(TAG,"For bible "+bibleCode.bibleCode) ;
    		//Log.w(TAG,bibleTree.toString()) ;
    		//Log.w(TAG," ") ;
    		
    		
    		
    		bibleTreemaps.put(bibleCode,bibleTree) ;
    	}
    	
    	
    	// ******** Pour chaque bible ***********
    	//   => interro du define : champs concernés par des links + pour le tree, collection initiale de chaque champ (& ou rien)
    	//   => "walk" de l'arbre
    	//   => apply en série de tous les éléments (entries)
    	for( a=0 ; a<bibles.length ; a++ ) {
    		BibleCode curBibleCode = bibles[a] ;
    		
    		if( mapBible.get(curBibleCode) == null ) {
    			continue ;
    		}
    		
    		// **** A constituer : champs => bibleciblée   +    collection initiale de la bible ciblée ( & ou rien )
    		HashMap<BibleFieldCode,BibleCode> mFieldlinkBible = new HashMap<BibleFieldCode,BibleCode>()  ;
    		HashMap<BibleFieldCode,Collection<String>> mCurrentNodeLinks = new HashMap<BibleFieldCode,Collection<String>>() ;
    		
    		
    		// ***** Interro du define de la bible ********
    		for( BibleFieldCode bfc :  mapBible.get(curBibleCode) ) {
    			if( bfc.fieldType != FieldType.FIELD_LINKBIBLE ) {
    				continue ;
    			}
    			
    			if( bibleTreemaps.get(new BibleCode(bfc.linkBible)) == null ) {
    				continue ;
    			}
   			
    			mFieldlinkBible.put(bfc, new BibleCode(bfc.linkBible)) ;
    			
    			if( bfc.recordType == RecordType.BIBLE_TREENODE ) {
    				// initialement la collection racine est vide
    				//  note : on pourrait prendre '&' ou tous les éléments de la bible en question
    				mCurrentNodeLinks.put(bfc, new ArrayList<String>()) ;
    			}
    		}
    		
    		
    		buildCaches_dbInsert( buildCaches_walkBibleTree( curBibleCode,
    				bibleTreemaps,
    				mFieldlinkBible,
    				bibleTreemaps.get(curBibleCode),
    				mCurrentNodeLinks) ) ;
    		
    		buildCaches_dbInsert( buildCaches_walkBibleEntries(curBibleCode,bibleTreemaps,mFieldlinkBible) ) ;
    	}
    }
    public Collection<buildCacheTableCV> buildCaches_walkBibleTree( BibleCode bc,
    		HashMap<BibleCode,Tree<String>> bibleTreemaps,
    		HashMap<BibleFieldCode,BibleCode> mFieldlinkBible,
    		Tree<String> curTreeNode,
    		HashMap<BibleFieldCode,Collection<String>> mParentNodeLinks) {
    	
    	int b ; // utilitaire pour les itérations
    	
    	Collection<buildCacheTableCV> insertRows = new ArrayList<buildCacheTableCV>() ;
    	
    	
    	// 1 ---- apply des inherited nodes ---------
    	//   Log.w(TAG,curTreeNode.getHead()+": Applying "+curTreeNode.getAllInherited().toString() ) ;
		b = 1 ;
		for( String s : curTreeNode.getAllInherited() ) {
			ContentValues cv = new ContentValues() ;
			cv.put("bible_code",bc.bibleCode) ;
			cv.put("treenode_key",curTreeNode.getHead()) ;
			cv.put("inherited_index",b) ;
			cv.put("inherited_treenode_key",s) ;

			insertRows.add(new buildCacheTableCV("store_bible_tree_inheritednodes",cv)) ;
			b++ ;
		}
   
    	
    	Cursor c ;
    	// 2 ---- apply des memberslinknodes --------
    	//   => FOREACH mFieldlinkBible
    	//      -> si champ courant vide => on apply la collection transmise
    	//      -> sinon, chaque node de l'entrée on prend tous les "members", apply et on passe cette collection
    	HashMap<BibleFieldCode,Collection<String>> mCurrentNodeLinks = new HashMap<BibleFieldCode,Collection<String>>() ;
    	for (Map.Entry<BibleFieldCode, BibleCode> entry : mFieldlinkBible.entrySet()) {
    		if( entry.getKey().recordType != RecordType.BIBLE_TREENODE ) {
    			continue ;
    		}
    		
    		c = mDb.rawQuery(String.format("SELECT treenode_field_value_link " +
    				"FROM store_bible_tree_field " +
    				"WHERE bible_code='%s' AND treenode_key='%s' AND treenode_field_code='%s'",
    				bc.bibleCode, curTreeNode.getHead(), entry.getKey().fieldCode
    				)) ;
    		if( c.getCount() != 1 ) {
    			c.close() ;
    			mCurrentNodeLinks.put(entry.getKey(), mParentNodeLinks.get(entry.getKey())) ;
    			continue ;
    		}
    		try{
    			c.moveToNext() ;
    			JSONArray jsonArr = new JSONArray(c.getString(0)) ;
    			Collection<String> newMembers = new ArrayList<String>() ;
    			for( int a = 0 ; a<jsonArr.length() ; a++ ){
    				newMembers.addAll( bibleTreemaps.get(entry.getValue()).getAllMembers(jsonArr.getString(a)) ) ;
    			}
    			mCurrentNodeLinks.put(entry.getKey(), newMembers) ;
    		}
    		catch( Exception e ) {
    			mCurrentNodeLinks.put(entry.getKey(), mParentNodeLinks.get(entry.getKey())) ;
    		}
    		finally{
    			c.close() ;
    		}
    	}
    	for (Map.Entry<BibleFieldCode, Collection<String>> entry : mCurrentNodeLinks.entrySet()) {
    		if( entry.getKey().recordType != RecordType.BIBLE_TREENODE ) {
    			// continue ; // pas nécessaire car mCurrentNodeLinks/mInitialNodeLinks ne concerne que le tree
    		}
    		
    		// Log.w(TAG,curTreeNode.getHead()+" "+ entry.getKey().fieldCode +": Applying "+entry.getValue().toString() ) ;
    		
    		// insertRows
    		b = 1 ;
    		for( String s : entry.getValue() ) {
    			ContentValues cv = new ContentValues() ;
    			cv.put("bible_code",bc.bibleCode) ;
    			cv.put("treenode_key",curTreeNode.getHead()) ;
    			cv.put("treenode_field_code",entry.getKey().fieldCode) ;
    			cv.put("treenode_field_linkmember_index",b) ;
    			cv.put("treenode_field_linkmember_treenodekey",s) ;

    			insertRows.add(new buildCacheTableCV("store_bible_tree_field_linkmembers",cv)) ;
    			b++ ;
    		}
    	}
    	
    	// end --- récursif sur les childs
    	for( Tree<String> childTreeNode : curTreeNode.getSubTrees() ) {
    		insertRows.addAll(
    				buildCaches_walkBibleTree( bc, bibleTreemaps, mFieldlinkBible,childTreeNode, mCurrentNodeLinks )
    				);
    	}
    	return insertRows ;
    }
    
    public Collection<buildCacheTableCV> buildCaches_walkBibleEntries( BibleCode bc,
    		HashMap<BibleCode,Tree<String>> bibleTreemaps,
    		HashMap<BibleFieldCode,BibleCode> mFieldlinkBible ) {
    	
    	Collection<buildCacheTableCV> insertRows = new ArrayList<buildCacheTableCV>() ;
    	
    	Cursor c ;
    	
    	for (Map.Entry<BibleFieldCode, BibleCode> entry : mFieldlinkBible.entrySet()) {
    		if( entry.getKey().recordType != RecordType.BIBLE_ENTRY ) {
    			continue ;
    		}
    		
    		String myQuery = String.format("SELECT e.entry_key , ef.entry_field_value_link " +
    				"FROM store_bible_entry e " +
    				"LEFT OUTER JOIN store_bible_entry_field ef " +
    				"ON e.bible_code=ef.bible_code AND e.entry_key=ef.entry_key AND ef.entry_field_code='%s' " +
    				"WHERE e.bible_code='%s'",
    				entry.getKey().fieldCode, bc.bibleCode);
    		// Log.w(TAG,myQuery) ;
    		c = mDb.rawQuery(myQuery) ;
    		while( c.moveToNext() ) {
    			if( c.getString(1).equals("") ) {
    				continue ;
    			}
    			try{
        			JSONArray jsonArr = new JSONArray(c.getString(1)) ;
        			Collection<String> members = new ArrayList<String>() ;
        			for( int a = 0 ; a<jsonArr.length() ; a++ ){
        				members.addAll( bibleTreemaps.get(entry.getValue()).getAllMembers(jsonArr.getString(a)) ) ;
        			}
        			
        			// Log.w(TAG,c.getString(0)+" "+ entry.getKey().fieldCode +": Applying "+members.toString() ) ;
        			
            		// insertRows
            		int b = 1 ;
            		for( String s : members ) {
            			ContentValues cv = new ContentValues() ;
            			cv.put("bible_code",bc.bibleCode) ;
            			cv.put("entry_key",c.getString(0)) ;
            			cv.put("entry_field_code",entry.getKey().fieldCode) ;
            			cv.put("entry_field_linkmember_index",b) ;
            			cv.put("entry_field_linkmember_treenodekey",s) ;

            			insertRows.add(new buildCacheTableCV("store_bible_entry_field_linkmembers",cv)) ;
            			b++ ;
            		}
    			}
    			catch( Exception e ){
    				continue ;
    			}
    		}
    		c.close() ;
    		
    	}
    	return insertRows ;
    }
    
    private void buildCaches_dbClean() {
    	mDb.beginTransaction() ;
    	mDb.execSQL("DELETE FROM store_bible_tree_inheritednodes") ;
    	mDb.execSQL("DELETE FROM store_bible_tree_field_linkmembers") ;
    	mDb.execSQL("DELETE FROM store_bible_entry_field_linkmembers") ;   	
    	mDb.endTransaction() ;
   }
    
    private void buildCaches_dbInsert(Collection<buildCacheTableCV> tcvs) {
    	mDb.beginTransaction() ;
    	for( buildCacheTableCV tcv : tcvs ) {
    		// Log.w(TAG,"Inserting") ;
    		mDb.insert(tcv.tableName, tcv.cv) ;
    	}
    	mDb.endTransaction() ;
  }
    
    
    
    
    
    
    /**
     * Method to join array elements of type string
     * @author Hendrik Will, imwill.com
     * @param inputArray Array which contains strings
     * @param glueString String between each array element
     * @return String containing all array elements seperated by glue string
     */
    private static String implodeArray(String[] inputArray, String glueString) {

    	/** Output variable */
    	String output = "";

    	if (inputArray.length > 0) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(inputArray[0]);

    		for (int i=1; i<inputArray.length; i++) {
    			sb.append(glueString);
    			sb.append(inputArray[i]);
    		}

    		output = sb.toString();
    	}

    	return output;
    }
    private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    
}
