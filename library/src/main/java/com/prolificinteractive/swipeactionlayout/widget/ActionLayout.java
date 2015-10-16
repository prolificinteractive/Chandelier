package com.prolificinteractive.swipeactionlayout.widget;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.prolificinteractive.swipeactionlayout.R;
import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.CENTER;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by chahine on 10/14/15.
 */
public class ActionLayout extends FrameLayout {

  private static final int ITEM_WEIGHT = 1;
  private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR =
      new AccelerateInterpolator();
  public static final String TAG = ActionLayout.class.getSimpleName();

  private final int imageViewMargin;
  private final int selectedSize;

  private final LinearLayout container;
  private final ImageView selectedImageView;
  private final List<ActionItem> actionItems = new ArrayList<>();
  private final List<ImageView> imageViews = new ArrayList<>();

  private int measuredWidth;
  private int selectedIndex = -1;
  private boolean isAnimating = false;

  private final Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
    @Override public void onAnimationStart(Animator animation) {
      updateSelectedImageView();
    }

    @Override public void onAnimationEnd(Animator animation) {
      isAnimating = false;
      if (actionListener != null) {
        actionListener.onAnimationEnd(null);
      }
      actionListener = null;
    }

    @Override public void onAnimationCancel(Animator animation) {

    }

    @Override public void onAnimationRepeat(Animator animation) {

    }
  };

  private void updateSelectedImageView() {
    for (int i = 0; i < actionItems.size(); i++) {
      imageViews.get(i).setBackgroundResource(actionItems.get(i).unselectedResId);
    }
    imageViews.get(selectedIndex)
        .setBackgroundResource(actionItems.get(selectedIndex).selectedResId);
  }

  private Animation.AnimationListener actionListener;

  public ActionLayout(final Context context, final AttributeSet attrs) {
    super(context);

    setMinimumHeight(200);

    final TypedArray a = context.getTheme().obtainStyledAttributes(
        attrs,
        R.styleable.SwipeActionLayout,
        0, 0);

    // Action Layout

    container = new LinearLayout(context);
    container.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT, CENTER_VERTICAL));
    setBackground(a.getDrawable(R.styleable.SwipeActionLayout_al_background));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setElevation(a.getDimensionPixelSize(R.styleable.SwipeActionLayout_al_elevation, 0));
    }

    container.setOrientation(LinearLayout.HORIZONTAL);
    container.setGravity(CENTER_VERTICAL);

    // Action Item

    imageViewMargin = a.getDimensionPixelSize(R.styleable.SwipeActionLayout_ai_margin, 0);
    selectedSize = a.getDimensionPixelSize(R.styleable.SwipeActionLayout_al_selected_size, 0);

    selectedImageView = new ImageView(context);
    final LayoutParams selectedLp = new LayoutParams(selectedSize, selectedSize, CENTER_VERTICAL);
    selectedLp.setMargins(0, imageViewMargin, 0, imageViewMargin);
    selectedImageView.setLayoutParams(selectedLp);
    selectedImageView.setBackground(a.getDrawable(R.styleable.SwipeActionLayout_al_selected));

    addView(selectedImageView);
    addView(container);
  }

  public void populateActionItems(@Nullable final List<ActionItem> items) {
    container.removeAllViews();
    actionItems.clear();
    imageViews.clear();

    if (items != null) {
      actionItems.addAll(items);
      final Context context = getContext();
      for (final ActionItem item : items) {
        final FrameLayout frame = new FrameLayout(context);
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT,
                ITEM_WEIGHT)
        );

        final ImageView imageView = new ImageView(context);
        final FrameLayout.LayoutParams imageLp = new FrameLayout.LayoutParams(
            WRAP_CONTENT,
            WRAP_CONTENT,
            CENTER
        );
        imageLp.setMargins(imageViewMargin, imageViewMargin, imageViewMargin, imageViewMargin);
        imageView.setLayoutParams(imageLp);
        imageView.setBackgroundResource(item.unselectedResId);

        imageViews.add(imageView);

        frame.addView(imageView);
        container.addView(frame);
      }
      selectedIndex = actionItems.size() / 2;
      updateSelectedImageView();
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    this.measuredWidth = getMeasuredWidth();
  }

  public void onParentTouchEvent(final MotionEvent ev) {
    if (measuredWidth <= 0 || isAnimating) {
      return;
    }

    final float x = ev.getX();
    final int count = actionItems.size();

    // One quarter of the screen width
    final int q = measuredWidth / 4;
    // projected x in center of screen
    final int pX = getInRange(Math.round((x - q) * 2), 0, measuredWidth);
    // width of one item
    final int iW = Math.round(measuredWidth / count);
    final int newSelectedIndex = getInRange(Math.round(pX / iW), 0, count - 1);

    // sensitivity range
    final int xS = iW / count;

    switch (ev.getAction()) {
      case MotionEvent.ACTION_MOVE:
        if (pX < selectedIndex * iW + xS) {
          // first third

          if (selectedIndex == 0) {
            break;
          }

          // actual progress
          final float t = getInRange((selectedIndex * iW + xS - pX) / (2.0f * xS), 0f, 1f);
          // scale
          final float sX = t / 2 + 1;
          // position in the middle
          final int mX = iW * selectedIndex + (iW - selectedSize) / 2;
          // interpolated progress
          final float it = getInRange(ACCELERATE_INTERPOLATOR.getInterpolation(t), 0f, 1f);
          // amount to move
          final int dX = Math.round(it * 2 * xS);
          // target
          final int tX = mX - dX;

          selectedImageView.setPivotX(0);
          selectedImageView.setScaleX(sX);
          selectedImageView.setTranslationX(tX);

          if (pX < iW * selectedIndex - xS) {
            setSelectedIndex(newSelectedIndex);
          }
        } else if (pX > (selectedIndex + 1) * iW - xS) {
          // last third

          if (selectedIndex == count - 1) {
            break;
          }

          // actual progress
          final float t = getInRange((pX - (selectedIndex + 1) * iW + xS) / (2.0f * xS), 0f, 1f);
          // scale
          final float sX = t / 2 + 1;
          // position in the middle
          final int mX = iW * selectedIndex + (iW - selectedSize) / 2;
          // interpolated progress
          final float it = getInRange(ACCELERATE_INTERPOLATOR.getInterpolation(t), 0f, 1f);
          // amount to move
          final int dX = Math.round(it * 2 * xS);
          // target
          final int tX = mX + dX;

          selectedImageView.setPivotX(selectedSize);
          selectedImageView.setScaleX(sX);
          selectedImageView.setTranslationX(tX);

          if (pX > iW * (selectedIndex + 1) + xS) {
            setSelectedIndex(newSelectedIndex);
          }
        } else {
          // middle case
          selectedImageView.setTranslationX(selectedIndex * iW + (iW - selectedSize) / 2);
        }
        break;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        //setSelectedIndex(newSelectedIndex);
        break;
    }
  }

  private float getInRange(final float value, final float min, final float max) {
    return Math.max(min, Math.min(max, value));
  }

  private void setSelectedIndex(final int newSelectedIndex) {
    selectedIndex = newSelectedIndex;
    final int iW = measuredWidth / actionItems.size();
    final int target = iW * selectedIndex + (iW - selectedSize) / 2;
    isAnimating = true;
    selectedImageView.animate()
        .scaleX(1)
        .translationX(target)
        .setInterpolator(new AccelerateDecelerateInterpolator())
        .setListener(animatorListener);
  }

  private int getInRange(final int value, final int min, final int max) {
    return Math.max(min, Math.min(max, value));
  }

  int getSelectedIndex() {
    return selectedIndex;
  }

  public void finishAction(Animation.AnimationListener mActionListener) {
    actionListener = mActionListener;
    setSelectedIndex(selectedIndex);
  }

  public static class ActionItem {
    private final int selectedResId;
    private final int unselectedResId;

    public ActionItem(
        @DrawableRes final int selectedDrawableResId,
        @DrawableRes final int unselectedDrawableResId
    ) {
      this.selectedResId = selectedDrawableResId;
      this.unselectedResId = unselectedDrawableResId;
    }
  }
}
