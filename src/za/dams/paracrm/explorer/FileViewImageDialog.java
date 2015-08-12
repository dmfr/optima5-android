package za.dams.paracrm.explorer;

import za.dams.paracrm.CrmImageLoader;
import za.dams.paracrm.MainMenuActivity;
import za.dams.paracrm.SdcardManager;
import za.dams.paracrm.CrmImageLoader.CrmUrl;
import za.dams.paracrm.R;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class FileViewImageDialog extends DialogFragment implements View.OnClickListener, CrmImageLoader.Callback {
    /** Argument name(s) */
    private static final String ARG_MODE = "mode";
    private static final String ARG_ID = "crmId";
    private static final String ARG_ISDIALOG = "isDialog";
    
    public static final int MODE_CRMMEDIAID = 1 ;
    public static final int MODE_CRMSYNCVUID = 2 ;
    
    public static final String FILEVIEWIMAGE_DIALOG_TAG = "FileViewImageDialog";
	
	private Context mContext ;
	private CrmImageLoader mCrmImageLoader ;
	
	private boolean mIsLoaded ;
	private ImageView mImageView ;
	private ProgressBar mProgressBar ;
    
    public static FileViewImageDialog newInstance(int mode, String id, boolean isDialog) {
        if (mode == 0 || id == null) {
            throw new IllegalArgumentException();
        }
        final FileViewImageDialog instance = new FileViewImageDialog();
        final Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        args.putString(ARG_ID, id);
        args.putBoolean(ARG_ISDIALOG, isDialog);
        instance.setArguments(args);
        return instance;
    }
    private String mImmutableCrmMediaId = null ;
    private String mImmutableCrmSyncVuid = null ;
    private boolean mIsDialog ;
    private void initializeArgCache() {
        if (mImmutableCrmSyncVuid != null) return;
        switch( getArguments().getInt(ARG_MODE) ) {
        case MODE_CRMMEDIAID :
        	mImmutableCrmMediaId = getArguments().getString(ARG_ID);
        	mImmutableCrmSyncVuid = null ;
        	break ;
        case MODE_CRMSYNCVUID :
        	mImmutableCrmMediaId = null ;
        	mImmutableCrmSyncVuid = getArguments().getString(ARG_ID);
        	break ;
        }
        mIsDialog = getArguments().getBoolean(ARG_ISDIALOG);
    }
    public String getCrmMediaId() {
        initializeArgCache();
        return mImmutableCrmMediaId;
    }
    public String getCrmSyncVuid() {
        initializeArgCache();
        return mImmutableCrmSyncVuid;
    }
    public boolean isDialog() {
    	initializeArgCache();
    	return mIsDialog ;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        mCrmImageLoader = new CrmImageLoader(mContext) ;
        mCrmImageLoader.setCallback(this) ;
        
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
        int dialogWidth = (int) r.getDimension(R.dimen.explorer_showimage_dialog_width);
        int dialogHeight = (int) r.getDimension(R.dimen.explorer_showimage_dialog_height);
        a.width = dialogWidth ;
        a.height = dialogHeight ;
        window.setAttributes(a);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	View v = inflater.inflate(R.layout.explorer_showimage_dialog, container) ;
    	
    	mProgressBar = (ProgressBar)v.findViewById(R.id.loading_progress) ;
    	mImageView = (ImageView)v.findViewById(R.id.image_view) ;
    	mImageView.setVisibility(View.GONE) ;
    	mImageView.setOnClickListener(this) ;
    	
    	CrmUrl crmUrl = new CrmUrl() ;
    	if( getCrmMediaId() != null ) {
    		crmUrl.mediaId = getCrmMediaId() ;
    	} else if( getCrmSyncVuid() != null ) {
    		crmUrl.syncVuid = getCrmSyncVuid() ;
    	}
    	crmUrl.thumbnail = false ;
    	mCrmImageLoader.download(crmUrl, mImageView) ;
    	
    	return v ;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void restoreInstanceState(Bundle state) {
    }
	@Override
	public void onImageLoaded(CrmUrl crmUrlRequested, ImageView imageView) {
		mProgressBar.setVisibility(View.GONE) ;
		mImageView.setVisibility(View.VISIBLE) ;
		mIsLoaded = true ;
	}
	@Override
	public void onImageLoadFailed(CrmUrl crmUrlRequested, ImageView imageView) {
		if( isDialog() ) {
			FileViewImageDialog.this.dismiss();
		}
	}
	@Override
	public void onClick(View view) {
		if( view==mImageView && mIsLoaded ) {} else return ;
		
    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	builder.setMessage("Save photo to SD card ?")
    	       .setCancelable(true)
    	       .setPositiveButton("Save to SD", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   onSaveToSd();
    	           }
    	       })
    	       .setNegativeButton("Close window", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	                FileViewImageDialog.this.dismiss();
    	           }
    	       });
    	AlertDialog alert = builder.create();
    	alert.show();
		
	}
    private void onSaveToSd() {
    	CrmUrl crmUrl = new CrmUrl() ;
    	crmUrl.syncVuid = getCrmSyncVuid() ;
    	crmUrl.thumbnail = false ;
    	
    	String timestamp = String.valueOf((int)(System.currentTimeMillis() / 1000)) ; 
    	String queryName = "CrmPhoto_"+timestamp+".jpeg" ;
    	byte[] data = mCrmImageLoader.getFileCachedBytes( crmUrl );
    	SdcardManager.saveData(mContext, queryName, data, true) ;
    }

}
