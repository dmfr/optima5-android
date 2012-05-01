package za.dams.paracrm;

import java.util.ArrayList;

import za.dams.paracrm.ui.FileCaptureActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainMenuAdapter extends BaseAdapter {
	private Context mContext;
	
	@SuppressWarnings("unused")
	private static final String TAG = "PARACRM/MainMenuAdapter";

	public static class ModuleInfo {
		public String label;
		public int id;
		public int icon;
		public Class<?> classObject;
		public int crmId ;
		
		public ModuleInfo(int aId, String aLabel, int aIcon, Class<?> aClassObject , int aCrmId) {
			id = aId;
			label = aLabel;
			icon = aIcon;
			classObject = aClassObject;
			crmId = aCrmId ;
		}
	};
	
	private static boolean initDone = false;
	
	private static ArrayList<ModuleInfo> MODULES_DICT = new ArrayList<ModuleInfo>();


    public MainMenuAdapter(Context c) {
        mContext = c;
    }
    
	private void initModules() {
		MODULES_DICT.clear();
		
		DatabaseManager mDbManager = DatabaseManager.getInstance(mContext) ;
		Cursor tmpCursor = mDbManager.rawQuery("SELECT * FROM input_scen ORDER BY scen_name") ;
		if( tmpCursor.getCount() > 0 ) {
	    	while( !tmpCursor.isLast() ){
	    		tmpCursor.moveToNext();
	    		
	    		MODULES_DICT.add(new ModuleInfo(tmpCursor.getInt(tmpCursor.getColumnIndex("scen_id")),
	    				tmpCursor.getString(tmpCursor.getColumnIndex("scen_name")), 
	    				R.drawable.mainmenu_visit, 
	    				FileCaptureActivity.class,
	    				tmpCursor.getInt(tmpCursor.getColumnIndex("scen_id"))));
	    	}
		}
		tmpCursor.close() ;
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
	public ModuleInfo getItem(int position) {
		if( !initDone ){
			initModules();
		}
		return MODULES_DICT.get(position);
	}
	
	public Class<?> getClassObj(int position) {
		if( !initDone ){
			initModules();
		}
		ModuleInfo curmod = MODULES_DICT.get(position);
		return curmod.classObject ;
	}

	@Override
	public long getItemId(int arg0) {
		if( !initDone ){
			initModules();
		}
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if( !initDone ){
			initModules();
		}
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
