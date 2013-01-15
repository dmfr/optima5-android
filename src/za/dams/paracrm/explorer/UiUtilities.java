package za.dams.paracrm.explorer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;

public class UiUtilities {
    private UiUtilities() {
    }
    /** Generics version of {@link Activity#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(Activity parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /** Generics version of {@link View#findViewById} */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getViewOrNull(View parent, int viewId) {
        return (T) parent.findViewById(viewId);
    }

    /**
     * Same as {@link Activity#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(Activity parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    /**
     * Same as {@link View#findViewById}, but crashes if there's no view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends View> T getView(View parent, int viewId) {
        return (T) checkView(parent.findViewById(viewId));
    }

    private static View checkView(View v) {
        if (v == null) {
            throw new IllegalArgumentException("View doesn't exist");
        }
        return v;
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View v, int visibility) {
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(Activity parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Same as {@link View#setVisibility(int)}, but doesn't crash even if {@code view} is null.
     */
    public static void setVisibilitySafe(View parent, int viewId, int visibility) {
        setVisibilitySafe(parent.findViewById(viewId), visibility);
    }

    /**
     * Used by an {@link Fragment} to install itself to the host activity.
     *
     * @see FragmentInstallable
     */
    public static void installFragment(Fragment fragment) {
        final Activity a = fragment.getActivity();
        if (a instanceof FragmentInstallable) {
            ((FragmentInstallable) a).onInstallFragment(fragment);
        }
    }

    /**
     * Used by an {@link Fragment} to uninstall itself from the host activity.
     *
     * @see FragmentInstallable
     */
    public static void uninstallFragment(Fragment fragment) {
        final Activity a = fragment.getActivity();
        if (a instanceof FragmentInstallable) {
            ((FragmentInstallable) a).onUninstallFragment(fragment);
        }
    }

    
    
    public static void listViewSmoothScrollToPosition(final Activity activity,
            final ListView listView, final int position) {
        // Workarond: delay-call smoothScrollToPosition()
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) {
                    return; // Activity being destroyed
                }
                listView.smoothScrollToPosition(position);
            }
        });
    }

}
