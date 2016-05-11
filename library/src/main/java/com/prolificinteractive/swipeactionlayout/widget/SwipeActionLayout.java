package com.prolificinteractive.swipeactionlayout.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
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
import com.prolificinteractive.swipeactionlayout.R;
import java.util.List;

public class SwipeActionLayout extends ViewGroup {
  private static final String LOG_TAG = SwipeActionLayout.class.getSimpleName();

  private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
  private static final int INVALID_POINTER = -1;
  private static final float DRAG_RATE = .8f;
  private static final int ANIMATE_TO_START_DURATION = 300;
  private static final int[] LAYOUT_ATTRS = new int[] {
      android.R.attr.enabled
  };

  private final AttributeSet attrs;
  private final DecelerateInterpolator decelerateInterpolator;

  protected int from;
  protected int originalOffsetTop;

  private boolean actionSelected;
  private View absListView;
  private View target; // the target of the gesture
  private OnActionListener listener;
  private int touchSlop;
  private float totalDragDistance = -1;
  private int currentTargetOffsetTop;
  // Whether or not the starting offset has been determined.
  private boolean originalOffsetCalculated = false;
  private float initialMotionY;
  private float initialDownY;
  private boolean isBeingDragged;
  private int activePointerId = INVALID_POINTER;
  // Target is returning to its start offset because it was cancelled or a
  // refresh was triggered.
  private boolean isReturningToStart;
  private int animateToStartDuration;
  private ActionLayout actionLayout;
  private float spinnerFinalOffset;
  private IdleScrollListener scrollListener = new IdleScrollListener();

  private final AnimationListener moveToStartListener = new SimpleAnimationListener() {
    @Override public void onAnimationEnd(Animation animation) {
      if (actionSelected) {
        int selectedIndex = actionLayout.getSelectedIndex();
        listener.onActionSelected(selectedIndex, actionLayout.getActionItem(selectedIndex));
        actionSelected = false;
      }
    }
  };

  private final Animation animateToStartPosition = new Animation() {
    @Override
    public void applyTransformation(float interpolatedTime, Transformation t) {
      moveToStart(interpolatedTime);
    }
  };
  private boolean isShowingAction = false;

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
    this.attrs = attrs;

    final Resources res = getResources();
    // Defaults
    final int defaultElevation = res.getDimensionPixelSize(R.dimen.default_elevation);

    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    setWillNotDraw(false);
    decelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

    final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
    setEnabled(a.getBoolean(0, true));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setElevation(
          a.getDimensionPixelSize(R.styleable.SwipeActionLayout_al_elevation, defaultElevation));
    }

    animateToStartDuration =
        a.getInteger(R.styleable.SwipeActionLayout_al_animate_to_start_duration,
            ANIMATE_TO_START_DURATION);

    a.recycle();

    createProgressView();
    ViewCompat.setChildrenDrawingOrderEnabled(this, true);
  }

  private void createProgressView() {
    actionLayout = new ActionLayout(getContext(), attrs);
    actionLayout.setVisibility(View.GONE);
    addView(actionLayout);
  }

  /**
   * Set the listener to be notified when a refresh is triggered via the swipe
   * gesture.
   */
  public void setOnActionSelectedListener(OnActionListener listener) {
    this.listener = listener;
  }

  private void ensureTarget() {
    // Don't bother getting the parent height if the parent hasn't been laid
    // out yet.
    if (target == null) {
      for (int i = 0; i < getChildCount(); i++) {
        View child = getChildAt(i);
        if (!child.equals(actionLayout)) {
          target = child;
          break;
        }
      }
    }

    if (absListView == null) {
      for (int i = 0; i < getChildCount(); i++) {
        final View child = getChildAt(i);
        if (child instanceof ScrollingView || child instanceof NestedScrollView) {
          // TODO fix validation
          absListView = child;
          scrollListener.setParent(absListView);
          if (absListView instanceof AbsListView) {
            ((AbsListView) absListView).setOnScrollListener(scrollListener);
          } else if (absListView instanceof RecyclerView) {
            ((RecyclerView) absListView).addOnScrollListener(scrollListener);
          }
          absListView.setOnTouchListener(new OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
              if (isShowingAction) {
                onTouchEvent(event);
                return true;
              }
              return false;
            }
          });
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
    if (target == null) {
      ensureTarget();
    }
    if (target == null) {
      return;
    }
    final View child = target;
    final int childLeft = getPaddingLeft();
    final int childTop = getPaddingTop();
    final int childWidth = width - getPaddingLeft() - getPaddingRight();
    final int childHeight = height - getPaddingTop() - getPaddingBottom();
    child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    actionLayout.layout(0, currentTargetOffsetTop,
        width, currentTargetOffsetTop + actionLayout.getMeasuredHeight());
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (target == null) {
      ensureTarget();
    }
    if (target == null) {
      return;
    }

    target.measure(
        MeasureSpec.makeMeasureSpec(
            getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
            MeasureSpec.EXACTLY
        ),
        MeasureSpec.makeMeasureSpec(
            getMeasuredHeight() - getPaddingTop() - getPaddingBottom(),
            MeasureSpec.EXACTLY
        ));

    actionLayout.measure(
        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
        actionLayout.getMeasuredHeight()
    );

    if (!originalOffsetCalculated) {
      originalOffsetCalculated = true;
      spinnerFinalOffset = actionLayout.getMeasuredHeight();
      totalDragDistance = spinnerFinalOffset;
      currentTargetOffsetTop = originalOffsetTop = -actionLayout.getMeasuredHeight();
    }
  }

  /**
   * @return Whether it is possible for the child view of this layout to
   * scroll up. Override this if the child view is a custom view.
   */
  public boolean canChildScrollUp() {
    if (android.os.Build.VERSION.SDK_INT < 14) {
      if (target instanceof AbsListView) {
        final AbsListView absListView = (AbsListView) target;
        return absListView.getChildCount() > 0
            && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
            .getTop() < absListView.getPaddingTop());
      } else {
        return ViewCompat.canScrollVertically(target, -1) || target.getScrollY() > 0;
      }
    } else {
      return ViewCompat.canScrollVertically(target, -1);
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    ensureTarget();

    final int action = MotionEventCompat.getActionMasked(ev);

    if (isReturningToStart && action == MotionEvent.ACTION_DOWN) {
      isReturningToStart = false;
    }

    if (!isEnabled() || isReturningToStart || canChildScrollUp()) {
      // Fail fast if we're not in a state where a swipe is possible
      return false;
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        setTargetOffsetTopAndBottom(originalOffsetTop - actionLayout.getTop());
        activePointerId = MotionEventCompat.getPointerId(ev, 0);
        isBeingDragged = false;
        final float initialDownY = getMotionEventY(ev, activePointerId);
        if (initialDownY == -1) {
          return false;
        }
        this.initialDownY = initialDownY;
        break;

      case MotionEvent.ACTION_MOVE:
        if (activePointerId == INVALID_POINTER) {
          Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
          return false;
        }

        final float y = getMotionEventY(ev, activePointerId);
        if (y == -1) {
          return false;
        }
        final float yDiff = y - this.initialDownY;
        if (yDiff > touchSlop && !isBeingDragged) {
          initialMotionY = this.initialDownY + touchSlop;
          isBeingDragged = true;
        }
        break;

      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        isBeingDragged = false;
        activePointerId = INVALID_POINTER;
        break;
    }

    return isBeingDragged;
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
    if ((android.os.Build.VERSION.SDK_INT >= 21 || !(target instanceof AbsListView))
        && (target == null || ViewCompat.isNestedScrollingEnabled(target))) {
      super.requestDisallowInterceptTouchEvent(b);
    }
  }

  private void moveActionLayout(final float overscrollTop) {
    Log.d(LOG_TAG,
        "### overscrollTop: " + overscrollTop + "; mOriginalOffsetTop: " + originalOffsetTop);
    final float originalDragPercent = overscrollTop / totalDragDistance;
    final float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
    final int targetY = originalOffsetTop + (int) (spinnerFinalOffset * dragPercent);

    if (actionLayout.getVisibility() != View.VISIBLE) {
      actionLayout.setVisibility(View.VISIBLE);
    }
    setTargetOffsetTopAndBottom(targetY - currentTargetOffsetTop);
    actionLayout.onLayoutTranslated(1 - (float) targetY / currentTargetOffsetTop);
  }

  private void finishAction(final float overscrollTop) {
    actionSelected = overscrollTop > totalDragDistance;
    if (actionSelected) {
      actionLayout.finishAction(new SimpleAnimationListener() {
        @Override public void onAnimationEnd(Animation animation) {
          animateOffsetToStartPosition();
        }
      });
    } else {
      animateOffsetToStartPosition();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    final int action = MotionEventCompat.getActionMasked(ev);

    if (isReturningToStart && action == MotionEvent.ACTION_DOWN) {
      isReturningToStart = false;
    }

    if (action == MotionEvent.ACTION_UP && isShowingAction) {
      hideActions();
      return true;
    }

    if (!isEnabled() || isReturningToStart || canChildScrollUp() || (!scrollListener.isIdle()
        && !isShowingAction)) {
      // Fail fast if we're not in a state where a swipe is possible
      if (actionLayout != null && isShowingAction) {
        actionLayout.onParentTouchEvent(ev);
      }
      return false;
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        activePointerId = MotionEventCompat.getPointerId(ev, 0);
        isBeingDragged = false;
        break;

      case MotionEvent.ACTION_MOVE: {
        final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (pointerIndex < 0) {
          Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
          return false;
        }

        final float y = MotionEventCompat.getY(ev, pointerIndex);
        final float overscrollTop = (y - initialMotionY) * DRAG_RATE;
        if (isBeingDragged) {
          if (overscrollTop > 0) {
            moveActionLayout(overscrollTop);
          } else if (!isShowingAction) {
            Log.d(LOG_TAG, "### false");
            return false;
          }
        }
        break;
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        final int index = MotionEventCompat.getActionIndex(ev);
        activePointerId = MotionEventCompat.getPointerId(ev, index);
        break;
      }

      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        if (activePointerId == INVALID_POINTER) {
          if (action == MotionEvent.ACTION_UP) {
            Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
          }
          return false;
        }
        final int pointerIndex = MotionEventCompat.findPointerIndex(ev, activePointerId);
        final float y = MotionEventCompat.getY(ev, pointerIndex);
        final float overscrollTop = (y - initialMotionY) * DRAG_RATE;
        isBeingDragged = false;
        finishAction(overscrollTop);
        activePointerId = INVALID_POINTER;
        return false;
      }
    }

    if (actionLayout != null) {
      actionLayout.onParentTouchEvent(ev);
    }

    return true;
  }

  private void animateOffsetToStartPosition() {
    from = Math.round(ViewCompat.getTranslationY(actionLayout));
    animateToStartPosition.reset();
    animateToStartPosition.setDuration(animateToStartDuration);
    animateToStartPosition.setInterpolator(decelerateInterpolator);
    animateToStartPosition.setAnimationListener(moveToStartListener);
    actionLayout.clearAnimation();
    actionLayout.startAnimation(animateToStartPosition);
  }

  private void moveToStart(float interpolatedTime) {
    setTargetOffsetTopAndBottom(Math.round((1 - interpolatedTime) * from));
  }

  private void setTargetOffsetTopAndBottom(final int offset) {
    ViewCompat.setTranslationY(actionLayout, offset);
    ViewCompat.setTranslationY(absListView, offset);
    currentTargetOffsetTop = actionLayout.getTop();
  }

  private void onSecondaryPointerUp(MotionEvent ev) {
    final int pointerIndex = MotionEventCompat.getActionIndex(ev);
    final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
    if (pointerId == activePointerId) {
      // This was our active pointer going up. Choose a new
      // active pointer and adjust accordingly.
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      activePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
    }
  }

  /**
   * Add a list of actions to the {@link SwipeActionLayout}.
   *
   * @param items list of {@link ActionItem} to display
   */
  public void populateActionItems(@Nullable final List<? extends ActionItem> items) {
    actionLayout.populateActionItems(items);
  }

  /**
   * Show the actions of the {@link SwipeActionLayout}
   */
  public void showActions() {
    isShowingAction = true;
    final Animation showAnimation = new Animation() {
      @Override protected void applyTransformation(float interpolatedTime, Transformation t) {
        moveActionLayout(-interpolatedTime * originalOffsetTop);
      }
    };
    showAnimation.reset();
    showAnimation.setDuration(200);
    startAnimation(showAnimation);
  }

  /**
   * Hide the actions of the {@link SwipeActionLayout}
   */
  public void hideActions() {
    isShowingAction = false;
    final float top = ViewCompat.getTranslationY(actionLayout);
    final Animation hideAnimation = new Animation() {
      @Override protected void applyTransformation(float interpolatedTime, Transformation t) {
        moveActionLayout((1 - interpolatedTime) * top);
      }
    };
    hideAnimation.reset();
    hideAnimation.setDuration(200);
    startAnimation(hideAnimation);
  }

  /**
   * Set the duration that the layout takes to get into its original position. Default is
   * {@link SwipeActionLayout#ANIMATE_TO_START_DURATION} = 300 millisecond.
   *
   * @param duration in millisecond
   */
  public void setAnimateToStartDuration(final int duration) {
    animateToStartDuration = duration;
  }

  /**
   * Classes that wish to be notified when the swipe gesture correctly
   * triggers an action should implement this interface.
   */
  public interface OnActionListener {
    void onActionSelected(int index, ActionItem action);
  }
}