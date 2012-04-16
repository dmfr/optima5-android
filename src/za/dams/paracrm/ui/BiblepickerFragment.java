package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class BiblepickerFragment extends DialogFragment {
	private static final String TAG = "PARACRM/UI/BiblepickerFragment";
	
	CrmFileTransaction mTransaction ;
	
	int pageId ;
	int recordId ;
	int fieldId ;
	
	String bibleCode ;
	private ArrayList<HashMap<String,Object>> mList ;
	
    static BiblepickerFragment newInstance(int pageId , int recordId , int fieldId ) {
    	BiblepickerFragment f = new BiblepickerFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("pageId", pageId);
        args.putInt("recordId", recordId);
        args.putInt("fieldId", fieldId);
        f.setArguments(args);

        return f;
    }
    
	/*
	void searchEngine(final String[] strArr){
	      LinearLayout llay=new LinearLayout(this);
	      llay.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));        
	      llay.setOrientation(LinearLayout.VERTICAL);

	      final ListView lv=new ListView(this);        
	      lv.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_checked, strArr));      


	      final EditText ed=new EditText(this);
	      ed.setWidth(80);
	      ed.setOnEditorActionListener(new OnEditorActionListener() {

	        @Override
	        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
	            // TODO Auto-generated method stub
	             lv.setTextFilterEnabled(true);
	             String strfilter=ed.getText().toString();
	             lv.setFilterText(strfilter);

	            return false;
	        }
	    });

	      llay.addView(ed);
	      llay.addView(lv);


	    setContentView(llay);

	}*/
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageId = getArguments().getInt("pageId");
        recordId = getArguments().getInt("recordId");
        fieldId = getArguments().getInt("fieldId");
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	      
        return inflater.inflate(R.layout.biblepicker_fragment, container, false ) ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	mTransaction = mManager.getTransaction() ;
    	

    	
    	// de quelle bible ???
    	bibleCode = mTransaction.page_getFields(pageId).get(fieldId).fieldLinkBible ;
    	//Log.w(TAG,"Instanciated for bible "+bibleCode) ;
    	getDialog().setTitle("Bible Picker / "+bibleCode);
    	
        mList = new ArrayList<HashMap<String,Object>>() ;
    	String[] adaptFrom = { new String("text1") , new String("text2") } ;
    	int[] adaptTo = { R.id.biblepickerrow1 , R.id.biblepickerrow2 } ;
        ListView mlv = (ListView) getView().findViewById(R.id.biblepickerlist) ;
        mlv.setAdapter(new SimpleAdapter(getActivity().getApplicationContext(), mList, R.layout.biblepicker_row, adaptFrom, adaptTo )) ;
        syncWithData() ;
        
        
        ((EditText)getView().findViewById(R.id.biblepickertext)).addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            	BiblepickerFragment.this.syncWithData() ;
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
    public void syncWithData() {
    	mList.clear() ;
    	HashMap<String,Object> mPoint ;
    	
    	String typedText = ((EditText)getView().findViewById(R.id.biblepickertext)).getText().toString() ;
    	
		// appel a bible helper
		BibleHelper bibleHelper = BibleHelper.getInstance(getActivity().getApplicationContext()) ;
		Iterator<BibleEntry> bibleIter ;
		if( typedText.length() == 0 ) {
			// Log.w(TAG,"Size of "+mTransaction.links_getBibleConditions().size()) ;
			bibleIter = bibleHelper.queryBible(bibleCode, mTransaction.links_getBibleConditions()).iterator() ;
		}
		else{
			bibleIter = bibleHelper.queryBible(bibleCode, mTransaction.links_getBibleConditions(), typedText ).iterator() ;
		}
		BibleHelper.BibleEntry bibleEntry ;
    	while( bibleIter.hasNext() ){
    		bibleEntry = bibleIter.next() ; 
    		
    		mPoint = new HashMap<String,Object>() ;
    		mPoint.put("entry_key",bibleEntry.entryKey) ;
    		mPoint.put("text1",bibleEntry.displayStr1) ;
    		mPoint.put("text2",bibleEntry.displayStr2) ;
    		mList.add(mPoint) ;
    	}
    	((SimpleAdapter) ((ListView) getView().findViewById(R.id.biblepickerlist)).getAdapter()).notifyDataSetChanged() ;
    }
    
    public void chooseItem( int position ) {
    	String mEntryKey = mList.get(position).get("entry_key").toString() ;
    	Log.w(TAG,"Selected "+mEntryKey) ;
    	
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	mTransaction.page_setRecordFieldValue_bible(pageId,recordId,fieldId,mEntryKey ) ;
    	
    	((FiledetailListFragment)getTargetFragment()).syncWithData() ;
    	getDialog().dismiss() ;
    }
    

}
