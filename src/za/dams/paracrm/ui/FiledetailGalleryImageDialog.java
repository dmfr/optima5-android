package za.dams.paracrm.ui;

import java.net.URI;
import java.net.URISyntaxException;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class FiledetailGalleryImageDialog extends DialogFragment implements View.OnClickListener {
	/** Argument name(s) */
	private static final String ARG_PAGEID = "pageId";
	private static final String ARG_RECORDID = "recordId";
	private static final String ARG_ISDIALOG = "isDialog";
	public static final String FILEVIEWIMAGE_DIALOG_TAG = "FiledetailGalleryImageDialog";

	private Context mContext ;

	private boolean mIsLoaded ;
	private ImageView mImageView ;
	private ProgressBar mProgressBar ;
	
	public static FiledetailGalleryImageDialog newInstance(int pageId, int recordId, boolean isDialog) {
		if( pageId < 0 || recordId < 0 ) {
			throw new IllegalArgumentException();
		}
		final FiledetailGalleryImageDialog instance = new FiledetailGalleryImageDialog();
		final Bundle args = new Bundle();
		args.putInt(ARG_PAGEID, pageId);
		args.putInt(ARG_RECORDID, recordId);
		args.putBoolean(ARG_ISDIALOG, isDialog);
		instance.setArguments(args);
		return instance;
	}
	private int mImmutablePageId = -1 ;
	private int mImmutableRecordId = -1 ;
	private boolean mIsDialog ;
	private void initializeArgCache() {
		if (mImmutablePageId >= 0 && mImmutableRecordId >= 0) return;
		mImmutablePageId = getArguments().getInt(ARG_PAGEID);
		mImmutableRecordId = getArguments().getInt(ARG_RECORDID);
		mIsDialog = getArguments().getBoolean(ARG_ISDIALOG);
	}
	public int getPageId() {
		initializeArgCache();
		return mImmutablePageId;
	}
	public int getRecordId() {
		initializeArgCache();
		return mImmutableRecordId;
	}
	public boolean isDialog() {
		initializeArgCache();
		return mIsDialog ;
	}
	
	private Runnable mSetImageRunnable = new Runnable()  {
		public void run() {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
			
			// TODO Auto-generated method stub
			int pageId = FiledetailGalleryImageDialog.this.getPageId();
			int recordId = FiledetailGalleryImageDialog.this.getRecordId();
			
	    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
	    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
	    	CrmFileTransaction.CrmFileRecord cfr = mTransaction.pageTable_getRecords(pageId).get(recordId) ;
	    	String mPath=null;
			try {
				mPath = new URI(cfr.recordPhoto.uriString).getPath();
			} catch (URISyntaxException e) {
				mImageView.post(new Runnable() {
	                public void run() {
	                	FiledetailGalleryImageDialog.this.dismiss();
	                }
	            });
			}
			final Bitmap bitmap = BitmapFactory.decodeFile(mPath);
			
			mImageView.post(new Runnable() {
                public void run() {
                	mProgressBar.setVisibility(View.GONE);
                	mImageView.setImageBitmap(bitmap);
                	mImageView.setVisibility(View.VISIBLE) ;
                	mIsLoaded = true ;
                }
            });
		}
	} ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();
		
		setStyle(DialogFragment.STYLE_NO_TITLE, 0);

		if (savedInstanceState != null) {
			restoreInstanceState(savedInstanceState);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mContext = getActivity();
		if( isDialog() ) {
			applyDialogParams() ;
		}
	}
	private void applyDialogParams() {
		Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(true);

		Window window = dialog.getWindow();

		WindowManager.LayoutParams a = window.getAttributes();
		Resources r = mContext.getResources();
		int dialogWidth = (int) r.getDimension(R.dimen.filecapture_showimage_dialog_width);
		int dialogHeight = (int) r.getDimension(R.dimen.filecapture_showimage_dialog_height);
		a.width = dialogWidth ;
		a.height = dialogHeight ;
		window.setAttributes(a);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.filecapture_showimage_dialog, container) ;

		mProgressBar = (ProgressBar)v.findViewById(R.id.loading_progress) ;
		mImageView = (ImageView)v.findViewById(R.id.image_view) ;
		mImageView.setVisibility(View.GONE) ;
		mImageView.setOnClickListener(this) ;

		new Thread(mSetImageRunnable).start();

		return v ;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	private void restoreInstanceState(Bundle state) {
	}
	
	@Override
	public void onClick(View view) {
		if( view==mImageView && mIsLoaded ) {} else return ;

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage("Delete this photo ?")
		.setCancelable(true)
		.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				onClickDelete();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

	}
	private void onClickDelete() {
    	CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
    	CrmFileTransaction mTransaction = mManager.getTransaction() ;
    	mTransaction.page_delRecord_photo( getPageId(), getRecordId() ) ;
    	
    	if( getTargetFragment() instanceof FiledetailGalleryFragment ){
    		getTargetFragment().onActivityResult(getTargetRequestCode(), FiledetailGalleryFragment.DELETEIMAGE_RESULT, null) ;
    	}
  
    	FiledetailGalleryImageDialog.this.dismiss();
	}

}
