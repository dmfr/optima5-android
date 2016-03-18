package za.dams.paracrm.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleCode;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.R;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class BiblePickerDialog extends DialogFragment {
	@SuppressWarnings("unused")
	private static final String TAG = "Widget/BiblePickerDialog";
	
    public interface OnBibleSetListener {
        void onBibleSet(BibleEntry be) ;
    }
    
    private BibleCode mBibleCode ;
    private ArrayList<BibleHelper.BibleEntry> mBibleConditions ;
    
	private ArrayList<HashMap<String,Object>> mList ;
	private ArrayList<BibleEntry> mEntries ;
	
	private Thread syncWithDataThread ;
	
	private OnBibleSetListener mListener ;
    
    public BiblePickerDialog( Context c, OnBibleSetListener listener, BibleCode bc ) {
    	mBibleCode = bc ;
    	mListener = listener ;
    	mBibleConditions = null ;
    }
    public BiblePickerDialog( Context c, OnBibleSetListener listener, BibleCode bc, ArrayList<BibleHelper.BibleEntry> bibleConditions ) {
    	mBibleCode = bc ;
    	mListener = listener ;
    	mBibleConditions = bibleConditions ;
    }
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	      
        return inflater.inflate(R.layout.biblepicker_fragment, container, false ) ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
    	getDialog().setTitle("Bible Picker / "+mBibleCode.bibleCode);
    	
        mList = new ArrayList<HashMap<String,Object>>() ;
        mEntries = new ArrayList<BibleEntry>() ;
    	String[] adaptFrom = { new String("text1") , new String("text2") } ;
    	int[] adaptTo = { R.id.biblepickerrow1 , R.id.biblepickerrow2 } ;
        ListView mlv = (ListView) getView().findViewById(R.id.biblepickerlist) ;
        mlv.setAdapter(new SimpleAdapter(getActivity().getApplicationContext(), mList, R.layout.biblepicker_row, adaptFrom, adaptTo )) ;
        syncWithData() ;
        
        ((EditText)getView().findViewById(R.id.biblepickertext)).setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                	if( ((EditText)v).getText().length() == 0 ) {
                		chooseNullItem() ;
                	}
                	return true ;
                }
                return false ;
            }
        }) ;
        
        ((EditText)getView().findViewById(R.id.biblepickertext)).addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            	BiblePickerDialog.this.syncWithData(true) ;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });



        mlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
        		chooseItem( position ) ;
        	}
        }) ;
    }
    public void onPause() {
    	if( syncWithDataThread != null ) {
    		syncWithDataThread.interrupt() ;
    	}
    	super.onPause() ;
    }
    public void syncWithData(){
    	syncWithData(false) ;
    }
    public void syncWithData(final boolean doWait) {
    	final String typedText = ((EditText)getView().findViewById(R.id.biblepickertext)).getText().toString() ;
    	
    	if( syncWithDataThread != null ) {
    		syncWithDataThread.interrupt() ;
    	}
        // Async check
    	syncWithDataThread = new Thread() {
            public void run() {
            	if( doWait ) {
            		try{
            			Thread.sleep(500) ;
            		}
            		catch( InterruptedException e ) {
            			//Log.w(TAG,"Interrupting request") ;
            			return ;
            		}
            	}
            	syncWithData_queryAsync(typedText);
            };
        };
        syncWithDataThread.start();
    }
    public void syncWithData_queryAsync( String typedText ) {
    	if( !isAdded() ) {
    		//Log.w(TAG,"Fragment was gone") ;
    		return ;
    	}
    	
		// appel a bible helper
		BibleHelper bh = new BibleHelper(getActivity().getApplicationContext()) ;
		
    	final ArrayList<HashMap<String,Object>> mListNew = new ArrayList<HashMap<String,Object>>() ;
    	final ArrayList<BibleEntry> mEntriesNew = new ArrayList<BibleEntry>() ;
    	for( BibleHelper.BibleEntry bibleEntry : 
    		bh.queryBible(mBibleCode.bibleCode, mBibleConditions, typedText, 25 ) ){
    		
    		HashMap<String,Object> mPoint = new HashMap<String,Object>() ;
    		mPoint.put("entry_key",bibleEntry.entryKey) ;
    		mPoint.put("text1",bibleEntry.displayStr1) ;
    		mPoint.put("text2",bibleEntry.displayStr2) ;
    		mListNew.add(mPoint) ;
    		
    		mEntriesNew.add(bibleEntry) ;
    	}
    	if( !Thread.interrupted() && isAdded() ) {
    		getActivity().runOnUiThread(new Runnable(){
    			public void run(){
    				syncWithData_setAdapterList(mListNew,mEntriesNew) ;
    			}
    		});
    	}
    }
    public void syncWithData_setAdapterList( Collection<HashMap<String,Object>> mListNew, ArrayList<BibleEntry> mEntriesNew){
    	mList.clear() ;
    	mList.addAll(mListNew) ;
    	mEntries.clear() ;
    	mEntries.addAll(mEntriesNew) ;
    	if( isAdded() ) {
    	((SimpleAdapter) ((ListView) getView().findViewById(R.id.biblepickerlist)).getAdapter()).notifyDataSetChanged() ;
    	}
    }
    
    
    public void chooseItem( int position ) {
    	BibleEntry entry = mEntries.get(position) ;
    	// Log.w(TAG,"Selected "+entry.entryKey) ;
    	
    	if( mListener != null ){
    		mListener.onBibleSet(entry) ;
    	}

    	getDialog().dismiss() ;
    }
    public void chooseNullItem() {
    	// Log.w(TAG,"Selected nothing") ;
    	if( mListener != null ){
    		mListener.onBibleSet(null) ;
    	}
    	getDialog().dismiss() ;
    }
    

}
