package za.dams.paracrm.explorer;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class ExplorerLayout extends LinearLayout implements View.OnClickListener {
	
    public ExplorerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public ExplorerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public ExplorerLayout(Context context) {
        super(context);
        initView();
    }
    
    /** Perform basic initialization */
    private void initView() {
        setOrientation(LinearLayout.HORIZONTAL); // Always horizontal
    }

    
    
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

}
