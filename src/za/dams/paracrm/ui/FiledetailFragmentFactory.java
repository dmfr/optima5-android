package za.dams.paracrm.ui;

import za.dams.paracrm.CrmFileTransaction;


public class FiledetailFragmentFactory {

    public static FiledetailFragment getFiledetailFragment(int position, CrmFileTransaction.PageType pageType) {
    	FiledetailFragment f = new FiledetailFragment();

    	switch( pageType ) {
    	case  PAGETYPE_TABLE :
            f = FiledetailTableFragment.newInstance(position);
            break;
    		
    	case  PAGETYPE_LIST :
            f = FiledetailListFragment.newInstance(position);
            break;
    	case  PAGETYPE_LOGLIST :
            f = FiledetailLoglistFragment.newInstance(position);
            break;
    		
    	case  PAGETYPE_PHOTO :
            f = FiledetailGalleryFragment.newInstance(position);
            break;
        
    	case  PAGETYPE_CONTAINER :
            f = FiledetailContainerFragment.newInstance(position);
            break;
            
    	case  PAGETYPE_SAVE :
            f = FiledetailSaveFragment.newInstance(position);
            break;
            
    		
    	default :
            f = FiledetailFragment.newInstance(position);
            break;
    	}

        return f;
    }
	
}
