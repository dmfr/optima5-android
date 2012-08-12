package za.dams.paracrm;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainMenuStaticAdapter extends BaseAdapter {
	private Context mContext;
	final Handler mHandler = new Handler();

	
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/MainMenuStaticAdapter";

	public static class StaticInfo {
		public String label;
		public String id;
		public int icon;
		public String methodName;
		
		public StaticInfo(String aId, String aLabel, int aIcon, String aMethodName) {
			id = aId;
			label = aLabel;
			icon = aIcon;
			methodName = aMethodName;
		}
	};
	
	private boolean initDone = false;
	public boolean hasAvailableUpdate = false ;
	private static ArrayList<StaticInfo> MODULES_DICT = new ArrayList<StaticInfo>();


    public MainMenuStaticAdapter(Context c) {
        mContext = c;
    }
    
	private void initModules() {
		MODULES_DICT.clear();
		
		if( SyncServiceHelper.isServiceRunning(mContext) || SyncServiceHelper.hasPendingUploads(mContext) ){
			MODULES_DICT.add(new StaticInfo("UPLOAD", "UploadTest", 
					R.drawable.ic_launcher, 
					"myUploadService"));			
		}
		else{
			if( hasAvailableUpdate ) {
				MODULES_DICT.add(new StaticInfo("UPDATE", "AutoUpdate", 
						R.drawable.mainmenu_update, 
						"myDownloadUpdate"));				
			}
			else{
				MODULES_DICT.add(new StaticInfo("QUIT", "Quit App.", 
						R.drawable.mainmenu_quit, 
						"myQuitActivity"));
			}
		}
		
		//Generic
		MODULES_DICT.add(new StaticInfo("CFG", "Settings", 
				R.drawable.mainmenu_config, 
				"myClearDbAsk"));
		MODULES_DICT.add(new StaticInfo("BIBLEDL", "Refresh DB", 
				R.drawable.mainmenu_refreshdb, 
				"myRefreshDb"));
		/*
		MODULES_DICT.add(new StaticInfo("BIBLEDL", "PrintTrees", 
				R.drawable.mainmenu_refreshdb, 
				"myPrintTrees"));
		*/
				
		initDone = true ;
	}
	@Override
	public int getCount() {
		if( !initDone ){
			initModules();
		}
		
		return MODULES_DICT.size();
	}

	@Override
	public StaticInfo getItem(int position) {
		return null;
	}
	
	public String getMyMethodName(int position) {
		StaticInfo curmod = MODULES_DICT.get(position);
		return curmod.methodName ;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		//ImageView imageView;
		TextView tv ;
		/*
        if (true) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(96, 96));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(mThumbIds[position]);
        // return imageView;
         */
        
        if (convertView == null) {

            tv = new TextView(mContext);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(8, 8, 8, 8);
            //tv.setLayoutParams(new GridView.LayoutParams(196, 196));
            //imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        } else {
            tv = (TextView) convertView;
        }
        
        StaticInfo curmod = MODULES_DICT.get(position);


        tv.setCompoundDrawablesWithIntrinsicBounds(
                0, curmod.icon, 0, 0);
        tv.setText(curmod.label);
        tv.setTextColor(Color.BLACK) ;
        tv.setTextSize( 16 ) ;
        // tv.setTag(data);

        return tv;
	}
	
	public void notifyDataSetChanged() {
		// Log.w(TAG,"Refreshing") ;
		
		initDone = false ;
		super.notifyDataSetChanged() ;
	}
    public void refreshFromAnyThread() {
        // Enqueue work on mHandler to change the data on
        // the main thread.
        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
    }


}
