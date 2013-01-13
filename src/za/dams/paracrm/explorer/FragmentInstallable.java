package za.dams.paracrm.explorer;

import android.app.Fragment;

public interface FragmentInstallable {
    /**
     * Called when a {@link Fragment} wants to be installed to the host activity.
     *
     * Fragments which use this MUST call this from {@link Fragment#onActivityCreated} using
     * {@link UiUtilities#installFragment}.
     *
     * This means a host {@link Activity} can safely assume a passed {@link Fragment} is already
     * created.
     */
    public void onInstallFragment(Fragment fragment);

    /**
     * Called when a {@link Fragment} wants to be uninstalled from the host activity.
     *
     * Fragments which use this MUST call this from {@link Fragment#onDestroyView} using
     * {@link UiUtilities#uninstallFragment}.
     */
    public void onUninstallFragment(Fragment fragment);
}
