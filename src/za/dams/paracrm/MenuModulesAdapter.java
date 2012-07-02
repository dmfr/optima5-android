package za.dams.paracrm;

import java.util.ArrayList;

import za.dams.paracrm.MainMenuAdapter.ModuleInfo;
import za.dams.paracrm.calendar.CalendarActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MenuModulesAdapter extends BaseAdapter {
	private Context mContext;
	final Handler mHandler = new Handler();

	
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/MainMenuStaticAdapter";

	public static class ModuleInfo {
		public String label;
		public String id;
		public int icon;
		public Class<?> classObject;
		
		public ModuleInfo(String aId, String aLabel, int aIcon, Class<?> aClassObject) {
			id = aId;
			label = aLabel;
			icon = aIcon;
			classObject = aClassObject;
		}
	};
	
	private boolean initDone = false;
	public boolean hasAvailableUpdate = false ;
	private static ArrayList<ModuleInfo> MODULES_DICT = new ArrayList<ModuleInfo>();


    public MenuModulesAdapter(Context c) {
        mContext = c;
    }
    
	private void initModules() {
		MODULES_DICT.clear();
		
		//Generic
		MODULES_DICT.add(new ModuleInfo("CFG", "FilesDB", 
				R.drawable.mainmenu_filemanager, 
				null));
		MODULES_DICT.add(new ModuleInfo("BIBLEDL", "Calendar", 
				R.drawable.mainmenu_calendar, 
				CalendarActivity.class));
				
		initDone = true ;
	}
	@Override
	public int getCount() {
		if( !initDone ){
			initModules();
		}
		
		return MODULES_DICT.size();
	}

	public ModuleInfo getItem(int position) {
		if( !initDone ){
			initModules();
		}
		return MODULES_DICT.get(position);
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
        
        ModuleInfo curmod = MODULES_DICT.get(position);


        tv.setCompoundDrawablesWithIntrinsicBounds(
                0, curmod.icon, 0, 0);
        tv.setText(curmod.label);
        tv.setTextColor(Color.BLACK) ;
        tv.setTextSize( 16 ) ;
        // tv.setTag(data);

        return tv;
	}
	
	public void notifyDataSetChanged() {
		initDone = false ;
		super.notifyDataSetChanged() ;
	}

}
