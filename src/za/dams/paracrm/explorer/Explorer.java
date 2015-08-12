package za.dams.paracrm.explorer;

public final class Explorer {
	
	static public final boolean DEBUG = false; // DO NOT SUBMIT WITH TRUE
	
	public static final String INTENT_EXIT_SETTINGS = "EXIT_SETTINGS";

	static public final int DATALIST_LOADER_ID = 1;
	static public final int BIBLELIST_LOADER_ID = 2;
	static public final int FILELIST_LOADER_ID = 3;
	static public final int QUERYLIST_LOADER_ID = 4;
	
	static public void clearContext() {
		RefreshManager.clearInstance() ;
		CrmFileManager.clearInstance() ;
		CrmExplorerConfig.clearInstance() ;
	}
}
