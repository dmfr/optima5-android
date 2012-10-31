package za.dams.paracrm;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dDBQdV90bHdhQXE3MHAxNVNwWWFWaEE6MQ") 
public class ParacrmApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
