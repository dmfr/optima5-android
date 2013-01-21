package za.dams.paracrm.explorer;

import za.dams.paracrm.CrmImageLoader;
import za.dams.paracrm.CrmImageLoader.CrmUrl;
import za.dams.paracrm.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
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

public class FileViewImageDialog extends DialogFragment implements CrmImageLoader.Callback {
    /** Argument name(s) */
    private static final String ARG_CRMSYNCVUID = "crmSyncVuid";
    private static final String ARG_ISDIALOG = "isDialog";
    public static final String FILEVIEWIMAGE_DIALOG_TAG = "FileViewImageDialog";
	
	private Context mContext ;
	private CrmImageLoader mCrmImageLoader ;
	
	private ImageView mImageView ;
	private ProgressBar mProgressBar ;
    
    public static FileViewImageDialog newInstance(String crmSyncVuid, boolean isDialog) {
        if (crmSyncVuid == null) {
            throw new IllegalArgumentException();
        }
        final FileViewImageDialog instance = new FileViewImageDialog();
        final Bundle args = new Bundle();
        args.putString(ARG_CRMSYNCVUID, crmSyncVuid);
        args.putBoolean(ARG_ISDIALOG, isDialog);
        instance.setArguments(args);
        return instance;
    }
    private String mImmutableCrmSyncVuid = null ;
    private boolean mIsDialog ;
    private void initializeArgCache() {
        if (mImmutableCrmSyncVuid != null) return;
        mImmutableCrmSyncVuid = getArguments().getString(ARG_CRMSYNCVUID);
        mIsDialog = getArguments().getBoolean(ARG_ISDIALOG);
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
        int dialogWidth = r.getInteger(R.integer.explorer_showimage_dialog_width);
        int dialogHeight = r.getInteger(R.integer.explorer_showimage_dialog_height);
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
    	
    	Log.w("FileViewImageDialog","SyncVuid is "+getCrmSyncVuid()) ;
    	
    	CrmUrl crmUrl = new CrmUrl() ;
    	crmUrl.syncVuid = getCrmSyncVuid() ;
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
	}

}
