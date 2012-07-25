package za.dams.paracrm.calendar;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

public class EditEventView implements View.OnClickListener, DialogInterface.OnCancelListener,
	DialogInterface.OnClickListener, OnItemSelectedListener {
	
	
	public EditEventView(Activity activity, View view, Runnable done) {
		
		
	}
	
	
	
	public boolean prepareForSave(){
		return true ;
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

}
