package za.dams.paracrm.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.dams.paracrm.CrmFileTransaction;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldDesc;
import za.dams.paracrm.CrmFileTransaction.CrmFileFieldValue;
import za.dams.paracrm.CrmFileTransaction.CrmFilePageinfo;
import za.dams.paracrm.CrmFileTransaction.CrmFileRecord;
import za.dams.paracrm.CrmFileTransaction.FieldType;
import za.dams.paracrm.CrmFileTransaction.PageType;
import za.dams.paracrm.CrmFileTransactionManager;
import za.dams.paracrm.DatabaseManager;
import za.dams.paracrm.SyncPullRequest;
import za.dams.paracrm.SyncServiceController;
import za.dams.paracrm.explorer.CrmFileManager;
import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

public class UtilityFragment extends Fragment {

	public static final String TAG="FileCapture/UtilityFragment" ;
	
	private Context mContext ;
	private CrmFileTransaction mCrmFileTransaction ;
	private SyncServiceController mSyncServiceController ;
	private SyncControllerResult mSyncResult ;
	
    public interface Listener {
    	public void onPageInstanceChanged(int pageInstanceTag) ;
    }
    private final List<Listener> mListeners = new ArrayList<Listener>();
    public void registerListener(Listener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        if( mListeners.contains(listener) ) {
        	return ;
        }
        mListeners.add(listener);
    }
    public void unregisterListener(Listener listener) {
        if (listener == null) {
            //throw new IllegalArgumentException();
        	return ;
        }
        mListeners.remove(listener);
    }
	
	
	
	public UtilityFragment() {
		setRetainInstance(true);
	}
    public static UtilityFragment newInstance() {
        final UtilityFragment instance = new UtilityFragment();
        final Bundle args = new Bundle();
        instance.setArguments(args);
        return instance;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mContext = getActivity().getApplicationContext() ;
    	
		CrmFileTransactionManager mManager = CrmFileTransactionManager.getInstance( getActivity().getApplicationContext() ) ;
		mCrmFileTransaction = mManager.getTransaction() ;
    	
    	mSyncServiceController = SyncServiceController.getInstance(mContext);
        mSyncResult = new SyncControllerResult() ;
        mSyncServiceController.addResultCallback(mSyncResult) ;
    }
    @Override
    public void onDestroy(){
    	mSyncServiceController.removeResultCallback(mSyncResult);
    	super.onDestroy();
    }
    
    
    
    public boolean isPageHasPendingJobs( int pageInstanceTag ) {
    	// recherche dans mAutocompleteScheduled
    	for( Map.Entry<SyncPullRequest,AutocompleteJob> tMap : mAutocompleteScheduled.entrySet() ) {
    		if( tMap.getValue().targetPageInstanceTag == pageInstanceTag ) {
    			return true ;
    		}
    	}
    	// recherche dans mRunningJobs
    	for( AutocompleteJob tJob : mAutocompleteRunning ) {
    		if( tJob.targetPageInstanceTag == pageInstanceTag ) {
    			return true ;
    		}
    	}
    	
    	return false ;
    }
    
    
    private class AutocompleteJob {
    	int targetPageInstanceTag ;
    	
    	String srcFileCode ;
    	boolean srcFilter_isOn ;
    	String srcFilter_fieldCode ;
    	String srcFilter_fieldValue ;
    	
    	String targetFileCode ;
    	
    	// boolean non persistant, autorise le -remplacement- des valeurs déja en place
    	boolean weak_allowOverwrite ; 
    	
		public boolean equals( Object o ) {
			if( o == null ) {
				return false ;
			}
			AutocompleteJob ec = (AutocompleteJob)o ;
			if( this.targetPageInstanceTag != ec.targetPageInstanceTag ) {
				return false ;
			}
			
			if( (this.srcFileCode == null && ec.srcFileCode != null)
					|| (this.srcFileCode != null && !this.srcFileCode.equals(ec.srcFileCode)) ) {
				return false ;
			}
			if( this.srcFilter_isOn != ec.srcFilter_isOn ) {
				return false ;
			}
			if( (this.srcFilter_fieldCode == null && ec.srcFilter_fieldCode != null)
					|| (this.srcFilter_fieldCode != null && !this.srcFilter_fieldCode.equals(ec.srcFilter_fieldCode)) ) {
				return false ;
			}
			if( (this.srcFilter_fieldValue == null && ec.srcFilter_fieldValue != null)
					|| (this.srcFilter_fieldValue != null && !this.srcFilter_fieldValue.equals(ec.srcFilter_fieldValue)) ) {
				return false ;
			}
    	
			if( (this.targetFileCode == null && ec.targetFileCode != null)
					|| (this.targetFileCode != null && !this.targetFileCode.equals(ec.targetFileCode)) ) {
				return false ;
			}

			return true ;
		}
		public int hashCode() {
			int result = 17 ;
			
			result = 31 * result + targetPageInstanceTag ;
			
			result = 31 * result + ( (srcFileCode!=null)? srcFileCode.hashCode():0 ) ;		
			result = 31 * result + ( srcFilter_isOn? 1:0 ) ;
			result = 31 * result + ( (srcFilter_fieldCode!=null)? srcFilter_fieldCode.hashCode():0 ) ;		
			result = 31 * result + ( (srcFilter_fieldValue!=null)? srcFilter_fieldValue.hashCode():0 ) ;		
		
			result = 31 * result + ( (targetFileCode!=null)? targetFileCode.hashCode():0 ) ;
			
			return result ;
		}		
    }
    
    
    
    private HashMap<SyncPullRequest,AutocompleteJob> mAutocompleteScheduled = new HashMap<SyncPullRequest,AutocompleteJob>() ;
    private List<AutocompleteJob> mAutocompleteRunning = new ArrayList<AutocompleteJob>() ;
    private List<AutocompleteJob> mAutocompleteInstalled = new ArrayList<AutocompleteJob>() ;
    public void autocompleteInit( int pageInstanceTag ) {
    	// ***** Launch AsyncTask AutocompletePrepareTask ******
    	// params( int PageInstanceTag )
    	// 
    	// doInBackground()
    	// - autocomplete ON ?
    	// - if page=TABLE, header page (idx=0) must be its parent file
    	// - if filter_on => header page must holds autocomplete_filter_src
    	// IF OK => create AutocompleteJob
    	//    if same job already installed => abort
    	// IF NOT => abort
    	// 
    	// onPostExecute()
    	// - if remote refresh <= stale time => launch AutocompleteDoTask
    	// - else
    	//  - Sync
    	//  - onSync > schedule AutocompleteDoTask
    	new AutocompleteInitTask().execute(pageInstanceTag);
    }
    private void autocompleteSchedule( SyncPullRequest syncPullRequest, AutocompleteJob autocompleteJob ) {
    	// Enregistrement du job dans la file d'attente de résultat :
    	mAutocompleteScheduled.put(syncPullRequest,autocompleteJob) ;
    	notifyPageChanged(autocompleteJob.targetPageInstanceTag);
    	
    	// Lancement du sync pull
    	mSyncServiceController.requestPull(syncPullRequest) ;
    }
    private void autocompleteDo( AutocompleteJob autocompleteJob ) {
    	// ******* Launch AsyncTask AutocompleteDoTask *********
    	// params { src_filecode, autocomplete_filter_src(field_code), autocomplete_filter_value(field_value), target_file_code(child_file), target_pageInstanceTag }
    	autocompleteRun_onJobBegin(autocompleteJob) ;
    	new AutocompleteDoTask().execute(autocompleteJob) ;
    }
	
    private class AutocompleteInitTask extends AsyncTask<Integer,Void,Void> {
    	AutocompleteJob mAutocompleteJob ;
    	SyncPullRequest mSyncPullRequest ;

		protected Void doInBackground(Integer... arrPageInstanceTag) {
			if( arrPageInstanceTag.length != 1 ) {
				return null ;
			}
			int pageInstanceTag = arrPageInstanceTag[0] ;
			
			AutocompleteJob tNewJob = autocompleteRun_createJob(pageInstanceTag);
			if( tNewJob == null || mAutocompleteInstalled.contains(tNewJob) ) {
				// si pas de Job (autocomplete non activé)
				// OU même job déja installé (déja fait)
				// => abort
				
				return null ;
			}
			
			// si un job différent a déjà été installé pour la même page
			// => signifie que les paramètres ont changé (filterSrcValue)
			// ==> seul cas où on autorise le remplacement des valeurs
			for( AutocompleteJob tJob : mAutocompleteInstalled ) {
				if( tJob.targetPageInstanceTag == pageInstanceTag ) {
					tNewJob.weak_allowOverwrite = true ;
				}
			}
			
			mAutocompleteJob = tNewJob ;
			if( autocompleteRun_getSrcFilerecordId(mAutocompleteJob) > 0 ) {
				// Source trouvée dans la base locale
				// => lancement direct du job
				return null ;
			}
			
			// Sinon création d'une SyncPullRequest
			mSyncPullRequest = new SyncPullRequest() ;
			mSyncPullRequest.fileCode = tNewJob.srcFileCode ;
			mSyncPullRequest.limitResults = 1 ;
			mSyncPullRequest.supplyTimestamp = false ;
			if( tNewJob.srcFilter_isOn ) {
				SyncPullRequest.SyncPullRequestFileCondition sprfc = new SyncPullRequest.SyncPullRequestFileCondition();
				sprfc.conditionSign = "eq" ;
				sprfc.fileFieldCode = tNewJob.srcFilter_fieldCode ;
				sprfc.conditionValue = tNewJob.srcFilter_fieldValue ;
				mSyncPullRequest.fileConditions.add(sprfc);
			}
			
			return null;
		}
		protected void onPostExecute(Void arg){
			if( mAutocompleteJob == null ) {
				return ;
			}
			
			if( mSyncPullRequest != null ) {
				// schedule AutocompleteJob on PullRequest response
				autocompleteSchedule(mSyncPullRequest,mAutocompleteJob) ;			
			} else {
				// direct execution of AutocompleteJob
				autocompleteDo(mAutocompleteJob) ;
			}	
		}
		
		public AutocompleteInitTask execute(int pageInstanceTag) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,pageInstanceTag) ;
			return this ;
		}
    }
    private class AutocompleteDoTask extends AsyncTask<AutocompleteJob,Void,AutocompleteJob> {
		@Override
		protected AutocompleteJob doInBackground(AutocompleteJob... params) {
			if( params.length != 1 ) {
				return null ;
			}
			AutocompleteJob aJob = params[0];
			
			autocompleteRun_doAutocomplete(aJob) ;
			
			return aJob;
		}
    	@Override
    	protected void onPostExecute(AutocompleteJob autocompleteJobDone) {
    		if( autocompleteJobDone==null ) {
    			return ;
    		}
    		UtilityFragment.this.autocompleteRun_onJobDone(autocompleteJobDone) ;
    	}

    	public AutocompleteDoTask execute(AutocompleteJob aJob) {
			executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,aJob) ;
			return this ;
		}
    }
    private AutocompleteJob autocompleteRun_createJob( int pageInstanceTag ) {
    	CrmFilePageinfo fpi = mCrmFileTransaction.list_getPage(mCrmFileTransaction.list_getPageIdxByTag(pageInstanceTag)) ;
    	if( !fpi.autocompleteIsOn ) {
    		return null ;
    	}	
    	
    	AutocompleteJob aJob = new AutocompleteJob() ;
    	aJob.targetPageInstanceTag = fpi.pageInstanceTag ;
    	aJob.targetFileCode = fpi.fileCode ;
    	switch( fpi.pageType ) {
    	case PAGETYPE_TABLE :
        	CrmFileManager crmFileManager = CrmFileManager.getInstance(mContext) ;
        	crmFileManager.fileInitDescriptors() ;
    		String masterFile = mCrmFileTransaction.getCrmFileCode() ;
    		String targetParent = crmFileManager.fileGetFileDescriptor(fpi.fileCode).parentFileCode ;
    		if( !masterFile.equals(targetParent) ) {
    			return null ;
    		}
    		aJob.srcFileCode = targetParent ;
    		break ;
    	default :
    		return null ;
    	}
    	if( fpi.autocompleteFilterIsOn ) {
    		int srcPosition = -1;
    		int idx=-1 ;
    		for( CrmFileFieldDesc cffd : mCrmFileTransaction.page_getFields(0) ) {
    			idx++ ;
    			if( cffd.fieldCode.equals(fpi.autocompleteFilterSrc) ) {
    				srcPosition=idx ;
    				break ;
    			}
    		}
    		if( srcPosition<0 ) {
    			return null ;
    		}
    		CrmFileFieldValue fieldValue = mCrmFileTransaction.page_getRecordFieldValue(0, 0, srcPosition) ;
    		if( !fieldValue.isSet || fieldValue.fieldType != FieldType.FIELD_BIBLE ) {
    			return null ;
    		}
    		
    		aJob.srcFilter_isOn = true ;
    		aJob.srcFilter_fieldCode = fpi.autocompleteFilterSrc ;
    		aJob.srcFilter_fieldValue = fieldValue.valueString ;
    	} else {
    		aJob.srcFilter_isOn = false ;
    	}
    	
    	return aJob ;
    }
    private int autocompleteRun_getSrcFilerecordId( AutocompleteJob aJob ) {
    	// Requete dans la base local Explorer.CrmFileManager
		DatabaseManager mDb = DatabaseManager.getInstance(mContext) ;
		Cursor c ;
		if( aJob.srcFilter_isOn ) {
			c = mDb.rawQuery(String.format("SELECT store_file.filerecord_id FROM store_file " +
					"JOIN store_file_field ON store_file_field.filerecord_id = store_file.filerecord_id AND store_file_field.filerecord_field_code='%s' " +
					"WHERE store_file.file_code='%s' AND store_file_field.filerecord_field_value_link='%s' " +
					"ORDER BY sync_timestamp DESC LIMIT 1",
					aJob.srcFilter_fieldCode,aJob.srcFileCode,aJob.srcFilter_fieldValue)) ;
		} else {
			c = mDb.rawQuery(String.format("SELECT store_file.filerecord_id FROM store_file " +
					"WHERE store_file.file_code='%s' " +
					"ORDER BY sync_timestamp DESC LIMIT 1",
					aJob.srcFileCode)) ;
		}
		int srcFilerecordId = -1 ;
		if( c.getCount() >= 1 ) {
			c.moveToNext() ;
			srcFilerecordId = c.getInt(0) ;
		}
		c.close() ;
		return srcFilerecordId ;
    }
    private void autocompleteRun_doAutocomplete( AutocompleteJob aJob ) {
    	int tInt,tInt2 ;
    	
    	int srcFilerecordId = autocompleteRun_getSrcFilerecordId(aJob);
    	if( srcFilerecordId <= 0 ) {
    		return ;
    	}
    	if( aJob.srcFileCode.equals(aJob.targetFileCode) ) {
    		return ; //TODO
    	}
    	
    	int targetPageIdx = mCrmFileTransaction.list_getPageIdxByTag(aJob.targetPageInstanceTag) ;
    	if( mCrmFileTransaction.list_getPage(targetPageIdx).pageType != PageType.PAGETYPE_TABLE ) {
    		return ;
    	}
    	
    	// Schéma de saisie du fichier target
    	List<CrmFileFieldDesc> targetFields = mCrmFileTransaction.page_getFields(targetPageIdx) ;
    	
    	// Déterminer les attributs du pivot
    	int pivotFieldId = -1 ;
    	String pivotFieldCode = null;
    	tInt=-1 ;
    	for( CrmFileFieldDesc cffd : targetFields ) {
    		tInt++ ;
    		if( cffd.fieldIsPivot ) {
    			pivotFieldId = tInt ;
    			pivotFieldCode = cffd.fieldCode ;
    			break ;
    		}
    	}
    	if( pivotFieldId < 0 ) {
    		return ;
    	}
    	
    	// load des enregistrements, indexés par fieldPivot
    	CrmFileManager crmFileManager = CrmFileManager.getInstance(mContext) ;
    	crmFileManager.fileInitDescriptors() ;
    	HashMap<String,CrmFileManager.CrmFileRecord> tRecords = new HashMap<String,CrmFileManager.CrmFileRecord>() ;
    	for( CrmFileManager.CrmFileRecord cfr : crmFileManager.filePullData(aJob.targetFileCode, 0, srcFilerecordId) ) {
    		String pivotValue = cfr.recordData.get(pivotFieldCode).valueString ;
    		tRecords.put(pivotValue, cfr) ;
    	}
    	
    	// Parcours de la table de saisie
    	for( tInt=0 ; tInt < mCrmFileTransaction.pageTable_getRecords(targetPageIdx).size() ; tInt++ ) {
    		// Valeur du pivot
    		String targetPivot = mCrmFileTransaction.page_getRecordFieldValue(targetPageIdx, tInt, pivotFieldId).valueString;
    		CrmFileManager.CrmFileRecord copySrcRecord = tRecords.get(targetPivot) ;
    		
    		boolean targetSaisie = false ;
    		tInt2=-1 ;
    		for( CrmFileFieldDesc cffd : targetFields ) {
    			tInt2++ ;
    			if( cffd.fieldIsPivot ) {
    				continue ;
    			}
    			CrmFileFieldValue destCffv = mCrmFileTransaction.page_getRecordFieldValue(targetPageIdx, tInt, tInt2) ;
    			if( destCffv.isSet ) {
    				targetSaisie = true ;
    			}
    			if( destCffv.isSet && !aJob.weak_allowOverwrite ) {
    				continue ;
    			}
    			if( copySrcRecord!=null ) {
    				CrmFileManager.CrmFileFieldValue srcCffv = copySrcRecord.recordData.get(cffd.fieldCode);
    				if( srcCffv == null ) {
    					continue ;
    				}
    				destCffv.valueDate = srcCffv.valueDate ;
    				destCffv.valueFloat = srcCffv.valueFloat ;
    				destCffv.valueBoolean = srcCffv.valueBoolean ;
    				destCffv.valueString = srcCffv.valueString ;
    				destCffv.displayStr = srcCffv.displayStr ;
    				destCffv.isSet = true ;
    			}
    		}
    		
    		// Set / unset du disable uniquement si overwrite ou zone non saisie
    		if( !targetSaisie || aJob.weak_allowOverwrite ) {
    			if( copySrcRecord != null ) {
    				mCrmFileTransaction.page_setRecordDisabled(targetPageIdx, tInt, false);
    			} else {
    				mCrmFileTransaction.page_setRecordDisabled(targetPageIdx, tInt, true);
    			}
    		}
    	}
    }

    private void autocompleteRun_onJobBegin( AutocompleteJob aJob ) {
    	mAutocompleteRunning.add(aJob);
    	
    	notifyPageChanged(aJob.targetPageInstanceTag);
    }
    private void autocompleteRun_onJobDone( AutocompleteJob aJob ) {
    	int targetPageInstanceTag = aJob.targetPageInstanceTag ;
    	for( AutocompleteJob tJob : mAutocompleteInstalled ){
    		if( tJob.targetPageInstanceTag == targetPageInstanceTag ) {
    			mAutocompleteInstalled.remove(tJob);
    		}
    	}
    	mAutocompleteInstalled.add(aJob) ;
    	
    	mAutocompleteRunning.remove(aJob) ;
    	
    	notifyPageChanged(targetPageInstanceTag);
    }
    
    private void notifyPageChanged(int pageInstanceTag) {
    	for(Listener listener : mListeners) {
    		listener.onPageInstanceChanged(pageInstanceTag);
    	}
    }
    
    private class SyncControllerResult extends SyncServiceController.Result {
    	public void onServiceEndCallback( int status, SyncPullRequest pr ) {
    		if( pr != null && mAutocompleteScheduled.containsKey(pr) ) {
        		AutocompleteJob tJobNow = mAutocompleteScheduled.remove(pr);
        		autocompleteDo(tJobNow) ;
    		}
    	}
    }
}
