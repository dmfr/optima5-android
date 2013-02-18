package za.dams.paracrm.explorer.xpressfile;

import java.util.ArrayList;
import java.util.List;

import za.dams.paracrm.BibleHelper;
import za.dams.paracrm.R;
import za.dams.paracrm.SyncServiceController;
import za.dams.paracrm.explorer.CrmFileManager;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileFieldValue;
import za.dams.paracrm.explorer.CrmFileManager.CrmFileRecord;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class XpressfileFragment extends Fragment 
	implements XpressfileViewController.XpressfileViewListener, CrmXpressfilePuller.Listener {
	
	private static final String TAG = "Xpressfile/XpressfileFragment";
	
    private static final int MODE_RAWFILE = 1 ;
    private static final int MODE_XPRESSFILE = 2 ;
    
    private static final String ARG_MODE = "arg_mode";
    private static final String ARG_XPRESSFILE_INPUTID = "arg_xpressfileInputId";
    private static final String ARG_XPRESSFILE_PRIMARYKEY = "arg_xpressfilePrimarykey";
    private static final String ARG_RAWFILE_FILECODE = "arg_rawfileFilecode";
    private static final String ARG_RAWFILE_FILERECORDID = "arg_rawfileFilerecordId";
    
    private static final String BUNDLE_KEY_XPRESSFILE_PRIMARYKEY = "key_xpressfilePrimarykey" ;
    
    private int mArgMode ;
    private int mArgXpressfileInputId ;
    private String mArgXpressfilePrimarykey ;
    private String mArgRawfileFilecode ;
    private int mArgRawfileFilerecordId ;
    
    private String mXpressfilePrimarykey ;
    

	private Activity mContext;
	private CrmXpressfilePuller mXpressfilePuller ;
	private BibleHelper mBibleHelper ;
	
	CrmFileRecord mRestoreModel;
	CrmFileRecord mModel;
	XpressfileViewController mView;
	private boolean mViewIsModified ;
	
	private SaveModelTask mSaveModelTask ;
	
	private ProgressDialog mProgressDialog;
	private Thread mProgressCancelThread ;
	private final Runnable mProgressCancelRunnable = new Runnable() {
		public void run(){
			try{ Thread.sleep(15000); } catch(Exception ex){
				return ;
			}
			mContext.runOnUiThread(new Runnable() {                   
				@Override
				public void run() {
			    	// unregister listener
			    	mXpressfilePuller.unregisterListener(XpressfileFragment.this) ;

		    		// Affichage du modèle retourné
		    		mModel = null ;
		    		mView.setCrmFileRecord(null) ;
		    		mContext.invalidateOptionsMenu();
		    		mViewIsModified = false ;
		    		
					if( mProgressDialog!=null && mProgressDialog.isShowing() ) {
						mProgressDialog.dismiss() ;
					}
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setTitle("Error")
					.setMessage("Cannot load data for selected entry.\nPlease try again")
					.setCancelable(false)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							return ;
						}
					});
					AlertDialog alert = builder.create();            
					alert.show();
				}
			});                         
		}
	};
	
	
	public XpressfileFragment() {
		setHasOptionsMenu(true);
	}
    public static XpressfileFragment newInstanceXpressfile(int xpressfileInputId, String primarykeyBibleEntryKey) {
        final XpressfileFragment instance = new XpressfileFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_XPRESSFILE);
        args.putInt(ARG_XPRESSFILE_INPUTID, xpressfileInputId) ;
        args.putString(ARG_XPRESSFILE_PRIMARYKEY, primarykeyBibleEntryKey);
        instance.setArguments(args);
        return instance;
    }
    public static XpressfileFragment newInstanceRawfile(String filecode, int filerecordId) {
        final XpressfileFragment instance = new XpressfileFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_MODE, MODE_RAWFILE);
        args.putString(ARG_RAWFILE_FILECODE, filecode) ;
        args.putInt(ARG_RAWFILE_FILERECORDID, filerecordId);
        instance.setArguments(args);
        return instance;
    }
    private void initializeArgCache() {
    	if (mArgMode != 0) return;
    	Bundle args = getArguments() ;
    	mArgMode = args.getInt(ARG_MODE) ;
    	switch( mArgMode ) {
    	case MODE_XPRESSFILE :
    		mArgXpressfileInputId = args.getInt(ARG_XPRESSFILE_INPUTID);
   			mArgXpressfilePrimarykey = args.getString(ARG_XPRESSFILE_PRIMARYKEY,null);
   			break ;
    	case MODE_RAWFILE :
    		mArgRawfileFilecode = args.getString(ARG_RAWFILE_FILECODE);
    		mArgRawfileFilerecordId = args.getInt(ARG_RAWFILE_FILERECORDID);
    		break ;
    	}
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.xpressfile_view, null);
        mView = new XpressfileViewController(mContext, view);
        mView.setListener( this ) ;
        
        launchInitTask() ;
        
        return view ;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeArgCache();
        
        mXpressfilePuller = CrmXpressfilePuller.getInstance(mContext) ;
        
        mXpressfilePrimarykey = mArgXpressfilePrimarykey ;
        
        if (savedInstanceState != null) {
        	if( savedInstanceState.containsKey(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ) {
        		mXpressfilePrimarykey = savedInstanceState.getString(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY) ;
        	}
        }
    }
    @Override
    public void onDestroy() {
    	mXpressfilePuller.unregisterListener(this) ;
    	super.onDestroy() ;
    }

    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.xpressfile_titlebar, menu);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(R.id.action_cancel).setVisible(mArgXpressfilePrimarykey!=null);
    	menu.findItem(R.id.action_done).setVisible(mModel!=null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return onActionBarItemSelected(item.getItemId());
    }

    /**
     * Handles menu item selections, whether they come from our custom action bar buttons or from
     * the standard menu items. Depends on the menu item ids matching the custom action bar button
     * ids.
     *
     * @param itemId the button or menu item id
     * @return whether the event was handled here
     */
    private boolean onActionBarItemSelected(int itemId) {
        switch (itemId) {
            case R.id.action_done:
            	handleSave();
                break;
            case android.R.id.home :
            case R.id.action_cancel:
            	handleBackPressed() ;
                break;
        }
        return true;
    }
    public void handleSave(){
    	// prepareForSave
    	boolean isComplete = mView.prepareForSave(); // synchro de mModel avec les champs contrôlés
    	
    	//Validation
    	if( !isComplete ) {
    		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    		builder.setTitle("Incomplete")
    		.setMessage("Please fill all required information")
    		.setCancelable(false)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    				dialog.cancel();
    				return ;
    			}
    		});
    		AlertDialog alert = builder.create();            
    		alert.show();
    		return;
    	}
    	
    	if( mSaveModelTask != null && mSaveModelTask.getStatus() != AsyncTask.Status.FINISHED ) {
    		return ;
    	}
    	mSaveModelTask = new SaveModelTask() ;
    	mSaveModelTask.execute();
    }
    public void handleBackPressed() {
    	if( mViewIsModified ) {
        	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        	builder.setMessage("Changes not saved. Quit anyway ?")
        	       .setCancelable(true)
        	       .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	               Activity a = XpressfileFragment.this.getActivity();
        	               if (a != null) {
        	                   a.finish();
        	               }
        	           }
        	       })
        	       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	alert.show();
    	} else {
            Activity a = XpressfileFragment.this.getActivity();
            if (a != null) {
                a.finish();
            }
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        mView.prepareForSave();
        outState.putString(BUNDLE_KEY_XPRESSFILE_PRIMARYKEY, mXpressfilePrimarykey) ;
    }
    
    
    
    
    private void launchInitTask() {
    	new InitTask().execute() ;
    }
    private void startLoading() {
    	if( mXpressfilePrimarykey == null ) {
    		return ;
    	}
    	
    	// Démarrage de l'asyncTask pour loading Bible
    	new LoadBibleEntryTask().execute() ;
    	
    	if( mRestoreModel != null ) {
    		mModel = mRestoreModel ;
    		mRestoreModel = null ;
    		mView.setCrmFileRecord(mModel) ;
    		mContext.invalidateOptionsMenu();
        	mViewIsModified = false ;
    		return ;
    	}
    	
    	if( mArgMode == MODE_XPRESSFILE ) {
    		// ProgressDialog : on
    		mProgressDialog = ProgressDialog.show(
    				mContext,
		    		"Fetching data",
		            "Please wait...",
    	            true);
    		if( mProgressCancelThread != null && mProgressCancelThread.isAlive() ) {
    			mProgressCancelThread.interrupt() ;
    		}
    		mProgressCancelThread = new Thread(mProgressCancelRunnable) ;
    		mProgressCancelThread.start() ;
    		
    		// PullRequest vers CrmXpressfilePuller
    		mXpressfilePuller.registerListener(this) ;
    		mXpressfilePuller.requestPull(mArgXpressfileInputId, mXpressfilePrimarykey) ;
    	}
    }
    @Override
    public void onXpressfilePullDeliver( int xpressfileInputId, String xpressfilePrimarykey, CrmFileRecord pulledCrmFileRecord ) {
    	
    	// unregister listener
    	mXpressfilePuller.unregisterListener(this) ;
    	
    	// ProgressDialog : off
    	if( mProgressDialog!=null && mProgressDialog.isShowing() ) {
    		mProgressDialog.dismiss() ;
    		if( mProgressCancelThread != null && mProgressCancelThread.isAlive() ) {
    			mProgressCancelThread.interrupt() ;
    		}
    	}
    	
    	if( pulledCrmFileRecord != null ) {
    		// Affichage du modèle retourné
    		mModel = pulledCrmFileRecord ;
    		mView.setCrmFileRecord(mModel) ;
    		mContext.invalidateOptionsMenu();
    		mViewIsModified = false ;
    		return ;
    	}
    	
    	// Création d'un modèle vide
    	new SetEmptyModelTask().execute() ;
    }
    
    
    
    
    private class InitTask extends AsyncTask<Void,Void,Void> {
    	
    	CrmFileManager.CrmFileFieldDesc tPrimarykeyFieldDesc ;
    	List<BibleHelper.BibleFieldCode> tPrimarykeyBibleFieldsDesc ;
    	boolean tPrimarykeyIsReadonly ;
    	
    	List<CrmFileManager.CrmFileFieldDesc> tFileFieldsDesc ;

		@Override
		protected Void doInBackground(Void... arg0) {
			mBibleHelper = new BibleHelper(mContext) ;
			
			// ******** Structure du fichier cible **********
			String tFileCode ;
			switch( mArgMode ) {
			case MODE_XPRESSFILE :
				tFileCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetFilecode ;
				break ;
			case MODE_RAWFILE:
				tFileCode = mArgRawfileFilecode ;
				break ;
			default :
				return null ;
			}
			CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
			cfm.fileInitDescriptors();
			CrmFileManager.CrmFileDesc cfd = cfm.fileGetFileDescriptor(tFileCode) ;
			
			tFileFieldsDesc = new ArrayList<CrmFileManager.CrmFileFieldDesc>() ;
			tFileFieldsDesc.addAll(cfd.fieldsDesc) ;
			
			// ******* Gestion de la primary key
			if( mArgMode == MODE_XPRESSFILE ) {
				
				// Appel au CrmXpressfileManager
				String tPrimarykeyFieldCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetPrimarykeyFileField ;
				tPrimarykeyFieldDesc = null ;
				
				// Reprise du tableau tfileFieldsDesc et "pop" de l'entrée correspondante
				int idx = -1 ;
				for( CrmFileManager.CrmFileFieldDesc cffd : tFileFieldsDesc ) {
					idx++ ;
					if( cffd.fieldCode.equals(tPrimarykeyFieldCode) ) {
						tPrimarykeyFieldDesc = tFileFieldsDesc.remove(idx) ;
						break ;
					}
				}
				
				// Primarykey readonly ?
				tPrimarykeyIsReadonly = false ;
				if( mArgXpressfilePrimarykey != null ) {
					// Si une primaryKey est spécifiée en argument => on ne la change pas
					tPrimarykeyIsReadonly = true ;
				}
				
				// Champs complets de la bible
				String primarykeyBibleCode = tPrimarykeyFieldDesc.fieldLinkBible ;
				tPrimarykeyBibleFieldsDesc = mBibleHelper.getBibleFieldsDesc(primarykeyBibleCode);				
			}
			
			return null;
		}
    	@Override
    	protected void onPostExecute(Void arg0) {
    		if( mArgMode == MODE_XPRESSFILE ) {
    			mView.setupPrimarykey(tPrimarykeyFieldDesc, tPrimarykeyBibleFieldsDesc, tPrimarykeyIsReadonly) ;
    		}
    		mView.setupFileFields(tFileFieldsDesc) ;
    		
    		// Chargement du formulaire
    		XpressfileFragment.this.startLoading() ;
    	}
    	
        public InitTask execute() {
    		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
        	return this ;
        }
    }
    
    private class LoadBibleEntryTask extends AsyncTask<Void,Void,Void> {
    	BibleHelper.BibleEntry loadedBe ;

		@Override
		protected Void doInBackground(Void... params) {
			String tFileCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetFilecode ;
			
			CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
			cfm.fileInitDescriptors();
			CrmFileManager.CrmFileDesc cfd = cfm.fileGetFileDescriptor(tFileCode) ;
			
			String tPrimarykeyFieldCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetPrimarykeyFileField ;
			CrmFileManager.CrmFileFieldDesc tPrimarykeyFieldDesc = null ;
			for( CrmFileManager.CrmFileFieldDesc cffd : cfd.fieldsDesc ) {
				if( cffd.fieldCode.equals(tPrimarykeyFieldCode) ) {
					tPrimarykeyFieldDesc = cffd ;
				}
			}
			
			loadedBe = mBibleHelper.getBibleEntry(tPrimarykeyFieldDesc.fieldLinkBible, mXpressfilePrimarykey) ;
			return null;
		}
    	@Override
    	protected void onPostExecute(Void arg0) {
    		if( loadedBe != null ) {
    			mView.setPrimarykeyBibleEntry(loadedBe) ;
    		}
    	}
    	
        public LoadBibleEntryTask execute() {
    		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
        	return this ;
        }
    }
    
    
    private class SetEmptyModelTask extends AsyncTask<Void,Void,Void> {
    	CrmFileRecord newCrmFileRecord ;

		@Override
		protected Void doInBackground(Void... arg0) {
			String tFileCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetFilecode ;
			
			CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
			cfm.fileInitDescriptors();
			
			CrmFileRecord tCrmFileRecord = cfm.fileGetEmptyRecord(tFileCode) ;
			
			if( mArgMode==MODE_XPRESSFILE ) {
				if( mXpressfilePrimarykey==null ) {
					return null ;
				}
				
				String tPrimarykeyFieldCode = CrmXpressfileManager.inputsList(mContext, mArgXpressfileInputId).get(0).mTargetPrimarykeyFileField ;
				
				CrmFileManager.CrmFileFieldDesc tPrimarykeyFieldDesc = null ;
				for( CrmFileManager.CrmFileFieldDesc cffd : cfm.fileGetFileDescriptor(tFileCode).fieldsDesc ) {
					if( cffd.fieldCode.equals(tPrimarykeyFieldCode) ) {
						tPrimarykeyFieldDesc = cffd ;
					}
				}
				if( tPrimarykeyFieldDesc == null ) {
					return null ;
				}
				
				CrmFileFieldValue cffv = tCrmFileRecord.recordData.get(tPrimarykeyFieldCode);
				if( cffv == null ) {
					return null ;
				}
				
				BibleHelper.BibleEntry be = mBibleHelper.getBibleEntry(tPrimarykeyFieldDesc.fieldLinkBible, mXpressfilePrimarykey) ;
				cffv.displayStr = be.displayStr;
				cffv.displayStr1 = be.displayStr1;
				cffv.displayStr2 = be.displayStr2;
				cffv.valueString = be.entryKey;
			}
			
			newCrmFileRecord = tCrmFileRecord ;
			return null;
		}
    	@Override
    	protected void onPostExecute(Void arg0) {
    		if( newCrmFileRecord != null ) {
    	    	// TEMP!!
    	    	mModel = newCrmFileRecord ;
    			mView.setCrmFileRecord(newCrmFileRecord) ;
    			mContext.invalidateOptionsMenu();
    			mViewIsModified = false ;
    		}
    	}
    	
        public SetEmptyModelTask execute() {
    		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
        	return this ;
        }
    }
    
    
    private class SaveModelTask extends AsyncTask<Void,Void,Void> {
    	boolean success ;

    	@Override
    	protected void onPreExecute() {
    		success = false ;
    		mProgressDialog = ProgressDialog.show(
    				mContext,
		    		"Saving",
		            "Please wait...",
    	            true);
    	}
		@Override
		protected Void doInBackground(Void... arg0) {
			if( mModel == null ) {
				return null ;
			}
			CrmFileManager cfm = CrmFileManager.getInstance(mContext) ;
			cfm.fileInitDescriptors();
			success = cfm.fileSaveRecord(mModel) ;
			if( success ) {
				try{
					Thread.sleep(1000) ;
				} catch(Exception e) {
					
				}
			}
			
			return null;
		}
    	@Override
    	protected void onPostExecute(Void arg0) {
        	// ProgressDialog : off
        	if( mProgressDialog!=null && mProgressDialog.isShowing() ) {
        		mProgressDialog.dismiss() ;
        	}
        	
    		if( success==false ) {
    			return ;
    		}
    		
			// @DAMS : build proper sync system
			SyncServiceController.getInstance(XpressfileFragment.this.mContext).requestPush() ;
			
			// Quit activity or switch back to empty
    		if( mArgMode == MODE_XPRESSFILE && mArgXpressfilePrimarykey == null ) {
    			// Dans le mode catalogue, on réinitialise la vue
    			mXpressfilePrimarykey = null ;
    			mModel = null ;
    			mView.setPrimarykeyBibleEntry(null) ;
    			mView.setCrmFileRecord(null) ;
    			mContext.invalidateOptionsMenu();
    			mViewIsModified = false ;
    		} else {
    			// Sinon on ferme simplement la page
    			XpressfileFragment.this.getActivity().finish() ;
    		}
    	}
    	
        public SaveModelTask execute() {
    		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) ;
        	return this ;
        }
    }
    
    
    
    // XpressfileViewController$Listener
	@Override
	public void onXpressfileViewChanged() {
		mViewIsModified = true ;
	}
	// XpressfileViewController$Listener
	@Override
	public void onXpressfilePrimarykeySet( String primarykeyBibleEntryKey ) {
		mXpressfilePrimarykey = primarykeyBibleEntryKey ;
		startLoading() ;
	}
}
