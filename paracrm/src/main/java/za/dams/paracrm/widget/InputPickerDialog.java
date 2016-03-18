package za.dams.paracrm.widget;

import za.dams.paracrm.BibleHelper.BibleEntry;
import za.dams.paracrm.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;

public class InputPickerDialog extends DialogFragment implements DialogInterface.OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = "Widget/BiblePickerDialog";
	
	public static final int TYPE_STRING = 1 ;
	public static final int TYPE_TEXT = 2 ;
	public static final int TYPE_INTEGER = 3 ;
	public static final int TYPE_FLOAT = 4 ;

    public interface OnInputSetListener {
        void onInputSet(String s) ;
        void onInputSet(int i) ;
        void onInputSet(float f) ;
    }
	
	private OnInputSetListener mListener ;
	private int mInputType ;
	private EditText m_editText ;
	private String mOriginalValue ;
	
	public InputPickerDialog( Context c , OnInputSetListener listener, int inputType, int value ) {
		this(c, listener, inputType, String.valueOf(value)) ;
	}
	public InputPickerDialog( Context c , OnInputSetListener listener, int inputType, float value ) {
		this(c, listener, inputType, Math.ceil(value) == value ? String.valueOf((int)value) : String.valueOf(value)) ;
	}
	public InputPickerDialog( Context c , OnInputSetListener listener, int inputType, String value ) {
		mInputType = inputType ;
		mListener = listener ;
		mOriginalValue = value ;
	}
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		m_editText = new EditText(getActivity());
		switch( mInputType ) {
		case TYPE_STRING :
			m_editText.setSingleLine(true) ;
			break ;

		case TYPE_TEXT :
			m_editText.setSingleLine(false) ;
			m_editText.setLines(5) ;
			m_editText.setMaxLines(5) ;
			m_editText.setGravity(Gravity.TOP) ;
			break ;
			
		case TYPE_INTEGER :
			m_editText.setInputType(InputType.TYPE_CLASS_NUMBER) ;
			break ;
			
		case TYPE_FLOAT :
			m_editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL) ;
			break ;
		}
		
		switch( mInputType ){
		case TYPE_STRING :
		case TYPE_INTEGER :
		case TYPE_FLOAT :
		
			m_editText.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					// If the event is a key-down event on the "enter" button
					if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
							(keyCode == KeyEvent.KEYCODE_ENTER)) {
						// Perform action on key press
						returnValue() ;
						return true;
					}
					return false;
				}	
			});
			
			break ;
		}

		m_editText.append(mOriginalValue) ;


		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()) ;
		builder.setView(m_editText) ;
    	if( getArguments() != null && getArguments().getString("title") != null ) {
    		builder.setTitle(getArguments().getString("title")) ;
    	}
    	builder.setPositiveButton("OK", this) ;
        return builder.create(); 
	}
	
	
	public void onClick(DialogInterface dialog, int id) {
		returnValue() ;
	}
	
	public void returnValue() {
		if( mListener != null ) {
			switch( mInputType ) {
			case TYPE_TEXT :
			case TYPE_STRING :
				mListener.onInputSet(m_editText.getText().toString()) ;
				break ;
				
			case TYPE_INTEGER :
				mListener.onInputSet(Integer.parseInt(m_editText.getText().toString())) ;
				break ;
			case TYPE_FLOAT :
				mListener.onInputSet(Float.parseFloat(m_editText.getText().toString())) ;
				break ;
			}
			
		}
		getDialog().dismiss() ;
	}
}
