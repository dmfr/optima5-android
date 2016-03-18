package za.dams.paracrm.explorer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

public class ExplorerBanner {
    private static final int ANIMATION_DURATION = 100;
    private static final TimeInterpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);

    private final TextView mBannerView;
    private final int mBannerHeight;

    private boolean mShown;

    /** Hold last animator to cancel. */
    private Animator mLastAnimator;

    public ExplorerBanner(Context context, TextView bannerView, int bannerHeight) {
        mBannerView = bannerView;
        mBannerHeight = bannerHeight;

        setBannerYAnim(-mBannerHeight); // hide by default.
    }

    /**
     * @return the current y position of the banner.
     */
    private int getBannerY() {
        return ((ViewGroup.MarginLayoutParams) mBannerView.getLayoutParams()).topMargin;
    }

    private static final String PROP_SET_BANNER_Y = "bannerYAnim";

    /**
     * Set the Y position of the banner.  public, but should only be used by animators.
     */
    public void setBannerYAnim(int y) {
        ((ViewGroup.MarginLayoutParams) mBannerView.getLayoutParams()).topMargin = y;
        mBannerView.requestLayout();
    }

    /**
     * Show a banner with a message.
     *
     * @return false if a banner is already shown, in which case the message won't be updated.
     */
    public boolean show(String message) {
        if (mShown) {
            return false; // If already shown, don't change the message, to avoid flicker.
        }
        mShown = true;
        mBannerView.setText(message);
        slideBanner(0);
        return true;
    }

    /**
     * Dismiss a banner.
     */
    public void dismiss() {
        if (!mShown) {
            return; // Always hidden, or hiding.
        }
        mShown = false;
        slideBanner(-mBannerHeight); // Slide up to hide.
    }

    private void slideBanner(int toY) {
        if (mLastAnimator != null) {
            mLastAnimator.cancel();
        }

        final PropertyValuesHolder[] values = {
                PropertyValuesHolder.ofInt(PROP_SET_BANNER_Y, getBannerY(), toY) };
        final ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                this, values).setDuration(ANIMATION_DURATION);
        animator.setInterpolator(INTERPOLATOR);
        mLastAnimator = animator;
        animator.start();
    }
}
