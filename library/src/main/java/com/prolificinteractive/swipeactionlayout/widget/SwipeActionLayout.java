package com.prolificinteractive.swipeactionlayout.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import java.util.List;

public class SwipeActionLayout extends ViewGroup
    implements NestedScrollingParent, NestedScrollingChild {
  private static final String LOG_TAG = SwipeActionLayout.class.getSimpleName();

  private static final int MAX_ALPHA = 255;
  private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);
  private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
  private static final int INVALID_POINTER = -1;
  private static final float DRAG_RATE = .8f;
  private static final int ALPHA_ANIMATION_DURATION = 300;
  private static final int ANIMATE_TO_START_DURATION = 300;

  private final AttributeSet mAttrs;

  private boolean mActionSelected;
  private View mAbsListView;
  private View mTarget; // the target of the gesture
  private OnActionListener mListener;
  private int mTouchSlop;
  private float mTotalDragDistance = -1;
  // If nested scrolling is enabled, the total amount that needed to be
  // consumed by this as the nested scrolling parent is used in place of the
  // overscroll determined by MOVE events in the onTouch handler
  private float mTotalUnconsumed;
  private final NestedScrollingParentHelper mNestedScrollingParentHelper;
  private final NestedScrollingChildHelper mNestedScrollingChildHelper;
  private final int[] mParentScrollConsumed = new int[2];

  private int mCurrentTargetOffsetTop;
  // Whether or not the starting offset has been determined.
  private boolean mOriginalOffsetCalculated = false;

  private float mInitialMotionY;
  private float mInitialDownY;
  private boolean mIsBeingDragged;
  private int mActivePointerId = INVALID_POINTER;

  // Target is returning to its start offset because it was cancelled or a
  // refresh was triggered.
  private boolean mReturningToStart;
  private final DecelerateInterpolator mDecelerateInterpolator;
  private static final int[] LAYOUT_ATTRS = new int[] {
      android.R.attr.enabled
  };

  private final AnimationListener mMoveToStartListener = new AnimationListener() {
    @Override public void onAnimationStart(Animation animation) {

    }

    @Override public void onAnimationEnd(Animation animation) {
      if (mActionSelected) {
        mListener.onActionSelected(mActionLayout.getSelectedIndex());
        mActionSelected = false;
      }
    }

    @Override public void onAnimationRepeat(Animation animation) {

    }
  };

  private ActionLayout mActionLayout;

  protected int mFrom;

  protected int mOriginalOffsetTop;

  private Animation mAlphaStartAnimation;

  private Animation mAlphaMaxAnimation;

  private float mSpinnerFinalOffset;

  private int mAlpha;

  /**
   * Simple constructor to use when creating a SwipeRefreshLayout from code.
   */
  public SwipeActionLayout(Context context) {
    this(context, null);
  }

  /**
   * Constructor that is called when inflating SwipeRefreshLayout from XML.
   */
  public SwipeActionLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mAttrs = attrs;

    mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    setWillNotDraw(false);
    mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

    final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
    setEnabled(a.getBoolean(0, true));
    a.recycle();

    createProgressView();
    ViewCompat.setChildrenDrawingOrderEnabled(this, true);
    // the absolute offset has to take into account that the circle starts at an offset
    mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

    mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
    setNestedScrollingEnabled(true);
  }

  private void createProgressView() {
    mActionLayout = new ActionLayout(getContext(), mAttrs);
    mActionLayout.setVisibility(View.GONE);
    addView(mActionLayout);
  }

  /**
   * Set the listener to be notified when a refresh is triggered via the swipe
   * gesture.
   */
  public void setOnActionSelectedListener(OnActionListener listener) {
    mListener = listener;
  }

  private void ensureTarget() {
    // Don't bother getting the parent height if the parent hasn't been laid
    // out yet.
    if (mTarget == null) {
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        if (!child.equals(mActionLayout)) {
          mTarget = child;
          break;
        }
      }
    }

    if (mAbsListView == null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        if (child instanceof ScrollingView) {
          mAbsListView = child;
          break;
        }
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    final int width = getMeasuredWidth();
    final int height = getMeasuredHeight();
    if (getChildCount() == 0) {
      return;
    }
    if (mTarget == null) {
      ensureTarget();
    }
    if (mTarget == null) {
      return;
    }
    final View child = mTarget;
    final int childLeft = getPaddingLeft();
    final int childTop = getPaddingTop();
    final int childWidth = width - getPaddingLeft() - getPaddingRight();
    final int childHeight = height - getPaddingTop() - getPaddingBottom();
    child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    mActionLayout.layout(0, mCurrentTargetOffsetTop,
        width, mCurrentTargetOffsetTop + mActionLayout.getMeasuredHeight());
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (mTarget == null) {
      ensureTarget();
    }
    if (mTarget == null) {
      return;
    }

    mTarget.measure(
        MeasureSpec.makeMeasureSpec(
            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
            MeasureSpec.EXACTLY
        ),
        MeasureSpec.makeMeasureSpec(
            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
            MeasureSpec.EXACTLY
        ));

    mActionLayout.measure(
        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
        mActionLayout.getMeasuredHeight()
    );

    if (!mOriginalOffsetCalculated) {
      mOriginalOffsetCalculated = true;
      mSpinnerFinalOffset = mActionLayout.getMeasuredHeight();
      mTotalDragDistance = mSpinnerFinalOffset;
      mCurrentTargetOffsetTop = mOriginalOffsetTop = -mActionLayout.getMeasuredHeight();
    }
  }

  /**
   * @return Whether it is possible for the child view of this layout to
   * scroll up. Override this if the child view is a custom view.
   */

  public boolean canChildScrollUp() {
    if (android.os.Build.VERSION.SDK_INT < 14) {
      if (mTarget instanceof AbsListView) {
        final AbsListView absListView = (AbsListView) mTarget;
        return absListView.getChildCount() > 0
            && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
            .getTop() < absListView.getPaddingTop());
      } else {
        return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
      }
    } else {
      return ViewCompat.canScrollVertically(mTarget, -1);
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    ensureTarget();

    final int action = MotionEventCompat.getActionMasked(ev);

    if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
      mReturningToStart = false;
    }

    if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
      // Fail fast if we're not in a state where a swipe is possible
      return false;
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        setTargetOffsetTopAndBottom(mOriginalOffsetTop - mActionLayout.getTop());
        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
        mIsBeingDragged = false;
        final float initialDownY = getMotionEventY(ev, mActivePointerId);
        if (initialDownY == -1) {
          return false;
        }
        mInitialDownY = initialDownY;
        break;

      case MotionEvent.ACTION_MOVE:
        if (mActivePointerId == INVALID_POINTER) {
          Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
          return false;
        }

        final float y = getMotionEventY(ev, mActivePointerId);
        if (y == -1) {
          return false;
        }
        final float yDiff = y - mInitialDownY;
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
          mInitialMotionY = mInitialDownY + mTouchSlop;
          mIsBeingDragged = true;
          mAlpha = STARTING_PROGRESS_ALPHA;
        }
        break;

      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        mIsBeingDragged = false;
        mActivePointerId = INVALID_POINTER;
        break;
    }

    return mIsBeingDragged;
  }

  private float getMotionEventY(MotionEvent ev, int activePointerId) {
    final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
    if (index < 0) {
      return -1;
    }
    return MotionEventCompat.getY(ev, index);
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean b) {
    // if this is a List < L or another view that doesn't support nested
    // scrolling, ignore this request so that the vertical scroll event
    // isn't stolen
    if ((android.os.Build.VERSION.SDK_INT >= 21 || !(mTarget instanceof AbsListView))
        && (mTarget == null || ViewCompat.isNestedScrollingEnabled(mTarget))) {
      super.requestDisallowInterceptTouchEvent(b);
    }
  }

  // NestedScrollingParent

  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    if (isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0) {
      // Dispatch up to the nested parent
      startNestedScroll(nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL);
      return true;
    }
    return false;
  }

  @Override
  public void onNestedScrollAccepted(View child, View target, int axes) {
    // Reset the counter of how much leftover scroll needs to be consumed.
    mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
    mTotalUnconsumed = 0;
  }

  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
    // before allowing the list to scroll
    if (dy > 0 && mTotalUnconsumed > 0) {
      if (dy > mTotalUnconsumed) {
        consumed[1] = dy - (int) mTotalUnconsumed;
        mTotalUnconsumed = 0;
      } else {
        mTotalUnconsumed -= dy;
        consumed[1] = dy;
      }
      moveActionLayout(mTotalUnconsumed);
    }

    // Now let our nested parent consume the leftovers
    final int[] parentConsumed = mParentScrollConsumed;
    if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
      consumed[0] += parentConsumed[0];
      consumed[1] += parentConsumed[1];
    }
  }

  @Override
  public int getNestedScrollAxes() {
    return mNestedScrollingParentHelper.getNestedScrollAxes();
  }

  @Override
  public void onStopNestedScroll(View target) {
    mNestedScrollingParentHelper.onStopNestedScroll(target);
    // Finish the spinner for nested scrolling if we ever consumed any
    // unconsumed nested scroll
    if (mTotalUnconsumed > 0) {
      finishAction(mTotalUnconsumed);
      mTotalUnconsumed = 0;
    }
    // Dispatch up our nested parent
    stopNestedScroll();
  }

  @Override
  public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
      int dyUnconsumed) {
    if (dyUnconsumed < 0) {
      dyUnconsumed = Math.abs(dyUnconsumed);
      mTotalUnconsumed += dyUnconsumed;
      moveActionLayout(mTotalUnconsumed);
    }
    // Dispatch up to the nested parent
    dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dxConsumed, null);
  }

  // NestedScrollingChild

  @Override
  public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
    return false;
  }

  @Override
  public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
    return false;
  }

  @Override
  public void setNestedScrollingEnabled(boolean enabled) {
    mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
  }

  @Override
  public boolean isNestedScrollingEnabled() {
    return mNestedScrollingChildHelper.isNestedScrollingEnabled();
  }

  @Override
  public boolean startNestedScroll(int axes) {
    return mNestedScrollingChildHelper.startNestedScroll(axes);
  }

  @Override
  public void stopNestedScroll() {
    mNestedScrollingChildHelper.stopNestedScroll();
  }

  @Override
  public boolean hasNestedScrollingParent() {
    return mNestedScrollingChildHelper.hasNestedScrollingParent();
  }

  @Override
  public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
      int dyUnconsumed, int[] offsetInWindow) {
    return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
        dxUnconsumed, dyUnconsumed, offsetInWindow);
  }

  @Override
  public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
    return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
  }

  @Override
  public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
    return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
  }

  @Override
  public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
    return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
  }

  private boolean isAnimationRunning(Animation animation) {
    return animation != null && animation.hasStarted() && !animation.hasEnded();
  }

  private void moveActionLayout(final float overscrollTop) {
    final float originalDragPercent = overscrollTop / mTotalDragDistance;
    final float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
    final int targetY = mOriginalOffsetTop + (int) (mSpinnerFinalOffset * dragPercent);

    if (mActionLayout.getVisibility() != View.VISIBLE) {
      mActionLayout.setVisibility(View.VISIBLE);
    }

    setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop);
  }

  private void finishAction(final float overscrollTop) {
    mActionSelected = overscrollTop > mTotalDragDistance;
    if (mActionSelected) {
      mActionLayout.finishAction(new AnimationListener() {
        @Override public void onAnimationStart(Animation animation) {

        }

        @Override public void onAnimationEnd(Animation animation) {
          animateOffsetToStartPosition();
        }

        @Override public void onAnimationRepeat(Animation animation) {

        }
      });
    } else {
      animateOffsetToStartPosition();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int action = MotionEventCompat.getActionMasked(ev);

    if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
      mReturningToStart = false;
    }

    if (!isEnabled() || mReturningToStart || canChildScrollUp()) {
      // Fail fast if we're not in a state where a swipe is possible
      return false;
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
        mIsBeingDragged = false;
        break;

      case MotionEvent.ACTION_MOVE: {
        final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
        if (pointerIndex < 0) {
          Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
          return false;
        }

        final float y = MotionEventCompat.getY(ev, pointerIndex);
        final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
        if (mIsBeingDragged) {
          if (overscrollTop > 0) {
            moveActionLayout(overscrollTop);
          } else {
            return false;
          }
        }
        break;
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        final int index = MotionEventCompat.getActionIndex(ev);
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);
        break;
      }

      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        if (mActivePointerId == INVALID_POINTER) {
          if (action == MotionEvent.ACTION_UP) {
            Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
          }
          return false;
        }
        final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
        final float y = MotionEventCompat.getY(ev, pointerIndex);
        final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
        mIsBeingDragged = false;
        finishAction(overscrollTop);
        mActivePointerId = INVALID_POINTER;
        return false;
      }
    }

    if (mActionLayout != null) {
      mActionLayout.onParentTouchEvent(ev);
    }

    return true;
  }

  private void animateOffsetToStartPosition() {
    mFrom = Math.round(ViewCompat.getTranslationY(mActionLayout));
    mAnimateToStartPosition.reset();
    mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
    mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
    mAnimateToStartPosition.setAnimationListener(mMoveToStartListener);
    mActionLayout.clearAnimation();
    mActionLayout.startAnimation(mAnimateToStartPosition);
  }

  private void moveToStart(float interpolatedTime) {
    setTargetOffsetTopAndBottom(Math.round((1 - interpolatedTime) * mFrom));
  }

  private final Animation mAnimateToStartPosition = new Animation() {
    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
      moveToStart(interpolatedTime);
    }
  };

  private void setTargetOffsetTopAndBottom(final int offset) {
    ViewCompat.setTranslationY(mActionLayout, offset);
    ViewCompat.setTranslationY(mAbsListView, offset);
    mCurrentTargetOffsetTop = mActionLayout.getTop();
  }

  private void onSecondaryPointerUp(MotionEvent ev) {
    final int pointerIndex = MotionEventCompat.getActionIndex(ev);
    final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
    if (pointerId == mActivePointerId) {
      // This was our active pointer going up. Choose a new
      // active pointer and adjust accordingly.
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
    }
  }

  public void populateActionItems(@Nullable final List<ActionLayout.ActionItem> items) {
    mActionLayout.populateActionItems(items);
  }

  /**
   * Classes that wish to be notified when the swipe gesture correctly
   * triggers an action should implement this interface.
   */
  public interface OnActionListener {
    void onActionSelected(int index);
  }
}