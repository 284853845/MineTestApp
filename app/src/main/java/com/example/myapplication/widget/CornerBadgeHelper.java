package com.example.myapplication.widget;

import android.graphics.Rect;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Adds a custom XML badge to the top-right corner of any target View.
 */
public final class CornerBadgeHelper {

    public interface BadgeBinder<T> {
        void onBind(View badgeView, @Nullable T data);
    }

    private CornerBadgeHelper() {
    }

    public static <T> BadgeHandle<T> addBadge(View target, @LayoutRes int badgeLayoutRes,
                                              @Nullable T data,
                                              @Nullable BadgeBinder<T> binder) {
        BadgeHandle<T> handle = new BadgeHandle<>(target, badgeLayoutRes, binder);
        handle.setData(data);
        handle.attach();
        return handle;
    }

    public static final class BadgeHandle<T>
            implements ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {

        private final View target;
        private final View badgeView;
        @Nullable
        private final BadgeBinder<T> binder;
        private final Rect anchorRect = new Rect();

        @Nullable
        private ViewGroup overlayHost;
        @Nullable
        private ViewTreeObserver preDrawObserver;

        private int offsetX;
        private int offsetY;
        private boolean addedToOverlay;
        private boolean removed;
        private boolean userVisible = true;

        private BadgeHandle(View target, @LayoutRes int badgeLayoutRes,
                            @Nullable BadgeBinder<T> binder) {
            if (target == null) {
                throw new IllegalArgumentException("target can not be null");
            }
            this.target = target;
            this.binder = binder;
            FrameLayout parentForParams = new FrameLayout(target.getContext());
            this.badgeView = LayoutInflater.from(target.getContext())
                    .inflate(badgeLayoutRes, parentForParams, false);
        }

        public View getBadgeView() {
            return badgeView;
        }

        public void setData(@Nullable T data) {
            if (binder != null) {
                binder.onBind(badgeView, data);
            }
            updatePosition();
        }

        public void setOffset(int xPx, int yPx) {
            offsetX = xPx;
            offsetY = yPx;
            updatePosition();
        }

        public void setOffsetDp(float xDp, float yDp) {
            setOffset(dp(xDp), dp(yDp));
        }

        public void setVisible(boolean visible) {
            userVisible = visible;
            updatePosition();
        }

        public void remove() {
            removed = true;
            target.removeOnAttachStateChangeListener(this);
            removePreDrawListener();
            removeFromOverlay();
        }

        private void attach() {
            target.addOnAttachStateChangeListener(this);
            ensureOverlay();
            updatePosition();
        }

        @Override
        public boolean onPreDraw() {
            ensureOverlay();
            updatePosition();
            return true;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            ensureOverlay();
            updatePosition();
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            removePreDrawListener();
            removeFromOverlay();
        }

        private void ensureOverlay() {
            if (removed || !target.isAttachedToWindow()) {
                return;
            }
            ViewParent parent = target.getParent();
            if (!(parent instanceof ViewGroup)) {
                return;
            }
            ViewGroup parentGroup = (ViewGroup) parent;
            if (overlayHost != parentGroup) {
                removeFromOverlay();
                overlayHost = parentGroup;
            }
            if (!addedToOverlay) {
                overlayHost.getOverlay().add(badgeView);
                addedToOverlay = true;
            }
            addPreDrawListener();
        }

        private void updatePosition() {
            if (removed || overlayHost == null || !addedToOverlay) {
                return;
            }
            boolean shouldShow = userVisible && target.isShown()
                    && target.getWidth() > 0 && target.getHeight() > 0;
            badgeView.setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
            if (!shouldShow) {
                return;
            }

            measureBadge();
            anchorRect.set(0, 0, target.getWidth(), target.getHeight());
            overlayHost.offsetDescendantRectToMyCoords(target, anchorRect);

            int badgeWidth = badgeView.getMeasuredWidth();
            int badgeHeight = badgeView.getMeasuredHeight();
            int left = anchorRect.right - badgeWidth / 2 + offsetX;
            int top = anchorRect.top - badgeHeight / 2 + offsetY;
            badgeView.layout(left, top, left + badgeWidth, top + badgeHeight);
        }

        private void measureBadge() {
            ViewGroup.LayoutParams lp = badgeView.getLayoutParams();
            int width = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.width;
            int height = lp == null ? ViewGroup.LayoutParams.WRAP_CONTENT : lp.height;
            int widthSpec = makeMeasureSpec(width, overlayHost == null ? 0 : overlayHost.getWidth());
            int heightSpec = makeMeasureSpec(height, overlayHost == null ? 0 : overlayHost.getHeight());
            badgeView.measure(widthSpec, heightSpec);
        }

        private int makeMeasureSpec(int size, int parentSize) {
            if (size > 0) {
                return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
            }
            if (size == ViewGroup.LayoutParams.MATCH_PARENT && parentSize > 0) {
                return View.MeasureSpec.makeMeasureSpec(parentSize, View.MeasureSpec.EXACTLY);
            }
            return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }

        private void addPreDrawListener() {
            if (preDrawObserver != null && preDrawObserver.isAlive()) {
                return;
            }
            preDrawObserver = target.getViewTreeObserver();
            preDrawObserver.addOnPreDrawListener(this);
        }

        private void removePreDrawListener() {
            if (preDrawObserver != null && preDrawObserver.isAlive()) {
                preDrawObserver.removeOnPreDrawListener(this);
            }
            preDrawObserver = null;
        }

        private void removeFromOverlay() {
            if (overlayHost != null && addedToOverlay) {
                overlayHost.getOverlay().remove(badgeView);
            }
            addedToOverlay = false;
        }

        private int dp(float value) {
            return Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    value,
                    target.getResources().getDisplayMetrics()));
        }
    }
}
