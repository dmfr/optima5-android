package za.dams.paracrm.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class UiUtilities {
	public static void showAlert( Context context, String strTitle, String strText ) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(strTitle)
		.setMessage(strText)
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		})
		.create()
		.show();
	}
}
