package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import za.dams.paracrm.explorer.FileViewImageDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
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
	
	public static final int SHOWIMAGE_REQUEST = 1 ;
	public static final int DELETEIMAGE_RESULT = 1 ;
    
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

    	mgv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    		public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
    			FiledetailGalleryFragment.this.handleClickList(position) ;
    		}
    	}) ;
    }	
	@Override
	public void onResume() {
		super.onResume();
		syncWithData() ;
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
			int pageId = getShownIndex() ;
			int recordId = position - 1 ;
			Log.w(TAG,"Displaying page "+pageId+" position "+recordId) ;

			FiledetailGalleryImageDialog fragment = FiledetailGalleryImageDialog.newInstance(pageId,recordId,true) ;
			fragment.setTargetFragment(this, SHOWIMAGE_REQUEST);
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			// if we have an old popup replace it
			Fragment fOld = fm.findFragmentByTag(FiledetailGalleryImageDialog.FILEVIEWIMAGE_DIALOG_TAG);
			if (fOld != null && fOld.isAdded()) {
				ft.remove(fOld);
			}
			ft.add(fragment, FiledetailGalleryImageDialog.FILEVIEWIMAGE_DIALOG_TAG);
			ft.commit();
			return ;
		}
		
		FiledetailCameraFragment cameraFragment = FiledetailCameraFragment.newInstance(getShownIndex()); 
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.filedetail, cameraFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.addToBackStack(null) ;
        ft.commit();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch( requestCode ) {
		case SHOWIMAGE_REQUEST:
			switch( resultCode ) {
			case DELETEIMAGE_RESULT:
				syncWithData();
			}
		}
	}
}
