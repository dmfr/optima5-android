package za.dams.paracrm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.database.Cursor;

public class BibleMemoryHelper {
	
    public final static String TAG = "PARACRM/BibleMemoryHelper";

    private static BibleMemoryHelper instance;
    
    private DatabaseManager mDb ;
    
    private HashMap<String,ArrayList<String>> mapBible ;
    private HashMap<String,HashMap<String,String>> mapForeignLinks ;
    private HashMap<String,ArrayList<HashMap<String,String>>> memDb ;
    private HashMap<String,HashMap<String,Integer>> hashDb ;
    
    
    
    
    public static class BibleEntry {
    	public String bibleCode ;
    	public String entryKey ;
    	public String displayStr ;
    	public String displayStr1 ;
    	public String displayStr2 ;
    	
    	public BibleEntry( String bibleCode, String entryKey ) {
    		this.bibleCode = bibleCode ;
    		this.entryKey = entryKey ;
    	}
    	public BibleEntry( String bibleCode, String entryKey, String displayStr, String displayStr1, String displayStr2 ) {
    		this.bibleCode = bibleCode ;
    		this.entryKey = entryKey ;
    		this.displayStr = displayStr ;
    		this.displayStr1 = displayStr1 ;
    		this.displayStr2 = displayStr2 ;
    	}
    	public void setDisplayStr(String displayStr, String displayStr1, String displayStr2 ) {
    		this.displayStr = displayStr ;
    		this.displayStr1 = displayStr1 ;
    		this.displayStr2 = displayStr2 ;
    	}
    }
   
    
    
    

    private BibleMemoryHelper(Context c) {
    	mDb = DatabaseManager.getInstance(c) ;
    	mapBible = new HashMap<String,ArrayList<String>>() ;
    	mapForeignLinks = new HashMap<String,HashMap<String,String>>() ;
    	memDb = new HashMap<String,ArrayList<HashMap<String,String>>>() ;
    	hashDb = new HashMap<String,HashMap<String,Integer>>() ;
    }

    public synchronized static BibleMemoryHelper getInstance(Context context) {
        if (instance == null) {
            instance = new BibleMemoryHelper(context.getApplicationContext());
        }
        return instance;
    }
    
    
    private void loadMemDb( String bibleCode ) {
    	Cursor tmpCursor ;
		tmpCursor = mDb.rawQuery( String.format("SELECT bible_code FROM define_bible WHERE bible_code='%s'",bibleCode) ) ;
		if( tmpCursor.getCount() < 1 ) {
			return ;
		}
		ArrayList<String> mMap = new ArrayList<String>();
		mMap.add("treenode_key") ;
		mMap.add("entry_key") ;
		HashMap<String,String> mForeign = new HashMap<String,String>();
		tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_code,entry_field_type,entry_field_linkbible FROM define_bible_entry WHERE bible_code='%s' AND entry_field_is_header='O' AND entry_field_is_key<>'O' ORDER BY entry_field_index",bibleCode) ) ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		mMap.add(tmpCursor.getString(0)) ;
    	}
    	tmpCursor = mDb.rawQuery( String.format("SELECT entry_field_code,entry_field_type,entry_field_linkbible FROM define_bible_entry WHERE bible_code='%s' ORDER BY entry_field_index",bibleCode) ) ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		if( tmpCursor.getString(1).equals("link") ){
    			mForeign.put(tmpCursor.getString(2),tmpCursor.getString(0)) ;
    		}
    	}
    	mapBible.put(bibleCode,mMap) ;
    	mapForeignLinks.put(bibleCode,mForeign) ;
    	
    	
    	
    	ArrayList<HashMap<String,String>> bibleDb = new ArrayList<HashMap<String,String>>() ;
    	HashMap<String,Integer> bibleHash = new HashMap<String,Integer>() ;
    	HashMap<String,String> bibleEntry ;
		tmpCursor = mDb.rawQuery( String.format("SELECT treenode_key, entry_key FROM store_bible_entry WHERE bible_code='%s' ORDER BY treenode_key, entry_key",bibleCode) ) ;
		int a = 0 ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		bibleEntry = new HashMap<String,String>() ;
    		bibleEntry.put("treenode_key", tmpCursor.getString(0)) ;
    		bibleEntry.put("entry_key", tmpCursor.getString(1)) ;
    		bibleDb.add(bibleEntry) ;
    		
    		bibleHash.put(tmpCursor.getString(1),new Integer(a)) ;
    		a++ ;
    	}
		tmpCursor = mDb.rawQuery( String.format("SELECT entry_key,entry_field_code,entry_field_value_string,entry_field_value_number,entry_field_value_link FROM store_bible_entry_field WHERE bible_code='%s' ORDER BY entry_key",bibleCode) ) ;
		String curEntryKey = "" ;
		String tmpValue ; 
		bibleEntry = new HashMap<String,String>() ;
    	while( !tmpCursor.isLast() ){
    		tmpCursor.moveToNext();
    		
    		if( tmpCursor.getString(0).equals("") ) {
    			continue ;
    		}
    		if( tmpCursor.getString(1).equals("") ) {
    			continue ;
    		}
    		
    		tmpValue = tmpCursor.getString(2) ;
    		if( tmpValue.equals("") && tmpCursor.getFloat(3) != 0 ) {
    			// Log.w(TAG,String.valueOf((float)tmpCursor.getFloat(3))) ;
    			if( tmpCursor.getFloat(3)==Math.ceil(tmpCursor.getFloat(3)) ) {
    				tmpValue = String.valueOf((int)tmpCursor.getFloat(3)) ;
    			}
    			else {
    				tmpValue = String.valueOf((float)tmpCursor.getFloat(3)) ;
    			}
    			
    		}
    		if( tmpValue.equals("") && !tmpCursor.getString(4).equals("")) {
    			tmpValue = tmpCursor.getString(4) ;
    		}
     		
    		if( !curEntryKey.equals(tmpCursor.getString(0)) ){
    			bibleEntry = bibleDb.get(bibleHash.get(tmpCursor.getString(0)).intValue()) ;
    			if( bibleEntry == null ) {
    				curEntryKey = "" ;
    				continue ;
    			}
    			else {
    				curEntryKey = tmpCursor.getString(0) ;
    			}
    		}
    		bibleEntry.put(tmpCursor.getString(1),tmpValue) ;
    	}
    	memDb.put(bibleCode,bibleDb) ;
    	hashDb.put(bibleCode, bibleHash) ;
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

    
    public ArrayList<BibleEntry> queryBible( String bibleCode, ArrayList<BibleEntry> mForeignEntries, String searchStr ) {
    	if( memDb.get(bibleCode) == null ){
    		loadMemDb(bibleCode) ;
    		//Log.w(TAG,"Loading "+bibleCode) ;
    	}
    	if( memDb.get(bibleCode) == null ){
    		return new ArrayList<BibleEntry>() ;
    	}
    	
    	ArrayList<BibleEntry> mResult = new ArrayList<BibleEntry>() ;
    	
    	//Log.w(TAG,"Size of "+bibleCode+" is "+memDb.get(bibleCode).size()) ;
    	Iterator<HashMap<String,String>> mIter = memDb.get(bibleCode).iterator() ; 
    	HashMap<String,String> mtmp ;
    	// String pretty ;
    	ArrayList<String> pretty1 ;
    	ArrayList<String> pretty2 ;
    	boolean goSearch = false ;
    	boolean goSearchVoid = false ;
    	if( searchStr != null && searchStr.length() > 0 ) {
    		goSearch = true ;
    		if( searchStr.length() < 3 ) {
    			goSearchVoid = true ;
    		}
    	}
    	
    	//Log.w(TAG,"Foreign size "+mForeignEntries.size()) ;
    	Iterator<BibleEntry> foreignIter ;
    	HashMap<String,String> foreignSearch = new HashMap<String,String>() ;
		if( true ) {
			foreignIter = mForeignEntries.iterator() ;
			while( foreignIter.hasNext()) {
				BibleEntry foreignBibleCondition = foreignIter.next() ;
				//Log.w(TAG,"Foreign condition "+foreignBibleCondition.bibleCode) ;
				String foreignField = mapForeignLinks.get(bibleCode).get(foreignBibleCondition.bibleCode) ;
				if( foreignField != null ) {
					//Log.w(TAG,"Putting "+foreignField+" "+foreignBibleCondition.entryKey) ;
					foreignSearch.put(foreignField,foreignBibleCondition.entryKey) ;
				}
			}
		}
		Iterator<String> foreignSearchIter ;
		//Log.w(TAG,"Foreign search "+foreignSearch.size()) ;
    	
    	
    	String[] fields =  mapBible.get(bibleCode).toArray(new String[mapBible.get(bibleCode).size()]) ;
    	
    	int fieldsSize = fields.length ;
    	int a , i ;
    	boolean isAMatch , isAMatchPartiel ;
    	String[] ttmp ;
    	while( mIter.hasNext() ){
    		mtmp = mIter.next() ;
    		
    		if( !foreignSearch.isEmpty() ) {
    			isAMatch = true ;
    			foreignSearchIter = foreignSearch.keySet().iterator() ;
    			while( foreignSearchIter.hasNext()) {
    				String fieldSearch = foreignSearchIter.next() ;
    				if( !mtmp.get(fieldSearch).contains(foreignSearch.get(fieldSearch)) ) {
    					isAMatch = false ;
    				}
    			}
    			if( !isAMatch ) {
    				continue ;
    			}
    		}
    		
    		
    		if( goSearch ) {
    			isAMatch = true ;
    			if( !goSearchVoid ) {
    				ttmp = searchStr.split(" ") ;
    				for( i=0 ; i<ttmp.length ; i++ ) {
    					isAMatchPartiel = false ;
    					for( a=1 ; a<fieldsSize ; a++ ) {
    						if( mtmp.get(fields[a]).toLowerCase().contains(ttmp[i].toLowerCase()) ) {
    							isAMatchPartiel = true ;
    							break ;
    						}
    					}
    					if( !isAMatchPartiel ){
    						isAMatch = false ;
    					}
    				}
    			}
    			else{
    				isAMatch = false ;
    			}
    			if( !isAMatch ) {
    				continue ;
    			}
    		}
    		
    		pretty1 = new ArrayList<String>() ;
    		pretty2 = new ArrayList<String>() ;
    		for( a=0 ; a<fieldsSize ; a++ ) {
    			if( a > 1 ){
    				pretty2.add(mtmp.get(fields[a])) ;
    			}
    			else{
    				if( a == 0 ) { // treenode_key
    					pretty1.add("("+mtmp.get(fields[a])+")") ;
    				}
    				else{ // entry_key
    					pretty1.add(mtmp.get(fields[a])) ;
    				}
    				
    			}
    		}
    		
    		mResult.add(new BibleEntry(bibleCode,mtmp.get("entry_key"),
    				implodeArray(pretty1.toArray(new String[pretty1.size()]), " ")+" "+implodeArray(pretty2.toArray(new String[pretty2.size()]), " "),
    				implodeArray(pretty1.toArray(new String[pretty1.size()]), " "),
    				implodeArray(pretty2.toArray(new String[pretty2.size()]), " "))) ;
    	}
    	
    	return mResult ;
    }
    public ArrayList<BibleEntry> queryBible( String bibleCode, ArrayList<BibleEntry> mForeignEntries ) {
    	return queryBible( bibleCode, mForeignEntries, null ) ;
    }
    public ArrayList<BibleEntry> queryBible( String bibleCode ) {
    	return queryBible( bibleCode, new ArrayList<BibleEntry>() ) ;
    }
    
    
    
    public BibleEntry prettifyEntry( BibleEntry bibleEntry ) {
    	if( memDb.get(bibleEntry.bibleCode) == null ){
    		loadMemDb(bibleEntry.bibleCode) ;
    		//Log.w(TAG,"Loading "+bibleCode) ;
    	}
    	if( memDb.get(bibleEntry.bibleCode) == null ){
    		return bibleEntry ;
    	}
    	
    	HashMap<String,String> mtmp ;
    	// String pretty ;
    	ArrayList<String> pretty1 ;
    	ArrayList<String> pretty2 ;
    	
		mtmp = memDb.get(bibleEntry.bibleCode).get(hashDb.get(bibleEntry.bibleCode).get(bibleEntry.entryKey)) ; 
		
		String[] fields =  mapBible.get(bibleEntry.bibleCode).toArray(new String[mapBible.get(bibleEntry.bibleCode).size()]) ;
	  	int fieldsSize = fields.length ;
    	int a ;
  	
		pretty1 = new ArrayList<String>() ;
		pretty2 = new ArrayList<String>() ;
		for( a=0 ; a<fieldsSize ; a++ ) {
			if( a > 1 ){
				pretty2.add(mtmp.get(fields[a])) ;
			}
			else{
				if( a == 0 ) { // treenode_key
					pretty1.add("("+mtmp.get(fields[a])+")") ;
				}
				else{ // entry_key
					pretty1.add(mtmp.get(fields[a])) ;
				}
				
			}
		}
		
    	return new BibleEntry(bibleEntry.bibleCode,mtmp.get("entry_key"),
				implodeArray(pretty1.toArray(new String[pretty1.size()]), " ")+" "+implodeArray(pretty2.toArray(new String[pretty2.size()]), " "),
				implodeArray(pretty1.toArray(new String[pretty1.size()]), " "),
				implodeArray(pretty2.toArray(new String[pretty2.size()]), " ")) ;
    }
    
    
}
