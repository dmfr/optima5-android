package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SimpleAdapter;

public class FiledetailGalleryFragment extends FiledetailFragment {
	private CrmFileTransaction mTransaction ;
	
	private ArrayList<HashMap<String,Object>> mList ;

	private static final String TAG = "PARACRM/UI/FiledetailGalleryFragment";
    
    public static FiledetailGalleryFragment newInstance(int index) {
    	FiledetailGalleryFragment f = new FiledetailGalleryFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);

        return f;
    }
    
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	// inflater = getActivity().getLayoutInflater() ;
    	return inflater.inflate(R.layout.filecapture_filedetail_gallery, container, false ) ;
    }
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	mTransaction = mManager.getTransaction() ;
    	
    	//Log.w(TAG,"My Fragment Id is "+mTransaction.getCrmFileCode() );
        mList = new ArrayList<HashMap<String,Object>>() ;
    	String[] adaptFrom = { new String("pictureLink") } ;
    	int[] adaptTo = { R.id.mygallerypicture } ;
        GridView mgv = (GridView) getView().findViewById(R.id.mygalleryview) ;
        mgv.setAdapter(new SimpleAdapter(getActivity().getApplicationContext(), mList, R.layout.filecapture_filedetail_galleryitem, adaptFrom, adaptTo )) ;
        syncWithData() ;
        
        mgv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
     	   public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
     		   FiledetailGalleryFragment.this.handleClickList(position) ;
     	   }
     }) ;
        mgv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
     	   public boolean onItemLongClick(AdapterView<?> parentView, View childView, int position, long id) {
     		   FiledetailGalleryFragment.this.handleLongClickList(position) ;
     		   return true ;
     	   }
     }) ;
    }	
	public void syncWithData(){
		int pageId = getShownIndex() ;
		
		mList.clear() ;
		HashMap<String,Object> mPoint ;
		
		mPoint = new HashMap<String,Object>() ;
		mPoint.put("pictureLink",R.drawable.camera_photo) ;
		mList.add(mPoint) ;
		
    	ArrayList<CrmFileTransaction.CrmFileRecord> records = mTransaction.pageTable_getRecords(pageId) ;
    	Iterator<CrmFileTransaction.CrmFileRecord> mIter2 = records.iterator() ;
    	CrmFileTransaction.CrmFileRecord mrecord ;
       	while( mIter2.hasNext() ){
       		mrecord = mIter2.next() ;
       		
       		
    		mPoint = new HashMap<String,Object>() ;
    		mPoint.put("pictureLink",mrecord.recordPhoto.uriStringThumb) ;
    		mList.add(mPoint) ;
       	}
		
        ((SimpleAdapter) ((GridView) getView().findViewById(R.id.mygalleryview)).getAdapter()).notifyDataSetChanged() ;
	}
	public void handleClickList(int position) {
		if( position != 0 ){ 
			return ;
		}
		FiledetailCameraFragment cameraFragment = FiledetailCameraFragment.newInstance(getShownIndex()); 

        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.filedetail, cameraFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null) ;
        ft.commit();
	}
	public void handleLongClickList(int position) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
    	builder.setMessage("Was long clicked !!")
    	       .setCancelable(false)
    	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });
    	AlertDialog alert = builder.create();            
    	alert.show();
	}

}
