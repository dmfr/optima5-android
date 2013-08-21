package za.dams.paracrm.settings;

public interface SettingsCallbacks {
	// Container Activity must implement this interface
    public void OnServerChanged();
    public void OnRequestClearDb();
}
