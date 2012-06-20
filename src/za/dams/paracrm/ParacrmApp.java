package za.dams.paracrm;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "dGVaT3ZqZHp4Z21BdWFZZFpuWjV5V0E6MQ") 
public class ParacrmApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
