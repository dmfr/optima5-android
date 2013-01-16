package za.dams.paracrm.explorer;

public final class Explorer {
	
	static public final boolean DEBUG = false; // DO NOT SUBMIT WITH TRUE

	static public final int DATALIST_LOADER_ID = 1;
	static public final int FILELIST_LOADER_ID = 2;
	
	static public void clearContext() {
		RefreshManager.clearInstance() ;
		CrmFileManager.clearInstance() ;
	}
}
