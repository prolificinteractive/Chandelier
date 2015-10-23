package com.prolificinteractive.swipeactionlayout.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
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

  public static final String TAG = ActionLayout.class.getSimpleName();

  private static final int ITEM_WEIGHT = 1;
  private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR =
      new AccelerateInterpolator();
  private static final AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR =
      new AccelerateDecelerateInterpolator();
  private static final int DEFAULT_SCALE = 1;

  private final int imageViewMargin;
  private final int selectedSize;

  private final LinearLayout container;
  private final ImageView selectedImageView;
  private final List<ActionItem> actionItems = new ArrayList<>();
  private final List<ImageView> imageViews = new ArrayList<>();
  private final int actionItemLayoutHeight;
  private final int actionItemLayoutWidth;
  private final int shortAnimDuration =
      getResources().getInteger(android.R.integer.config_shortAnimTime);

  private boolean isScaleEnabled;
  private int measuredWidth;
  private int selectedIndex = -1;
  private boolean isAnimating = false;
  private Animation.AnimationListener actionListener;

  private final Animation.AnimationListener animationListener = new SimpleAnimationListener() {
    @Override public void onAnimationStart(Animation animation) {
      imageViews.get(selectedIndex).setSelected(true);
    }

    @Override public void onAnimationEnd(Animation animation) {
      isAnimating = false;
      if (actionListener != null) {
        actionListener.onAnimationEnd(null);
      }
      actionListener = null;
    }
  };

  public ActionLayout(final Context context, final AttributeSet attrs) {
    super(context);
    final Resources res = getResources();
    final TypedArray a = context.getTheme().obtainStyledAttributes(
        attrs,
        R.styleable.SwipeActionLayout,
        0, 0);

    // Defaults
    final int defaultElevation = res.getDimensionPixelSize(R.dimen.default_elevation);
    final int defaultSelectorMargin = res.getDimensionPixelSize(R.dimen.default_selector_margin);
    final int defaultSelectorSize = res.getDimensionPixelSize(R.dimen.default_selector_size);
    final boolean defaultScaleEnabled = res.getBoolean(R.bool.default_scale_enabled);

    isScaleEnabled = a.getBoolean(R.styleable.SwipeActionLayout_al_scale_enabled,
        defaultScaleEnabled);
    actionItemLayoutHeight =
        a.getDimensionPixelSize(R.styleable.SwipeActionLayout_ai_layout_height,
            WRAP_CONTENT);
    actionItemLayoutWidth = a.getDimensionPixelSize(R.styleable.SwipeActionLayout_ai_layout_width,
        WRAP_CONTENT);

    // Action Layout
    container = new LinearLayout(context);
    container.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT, CENTER_VERTICAL));
    setBackground(a.getDrawable(R.styleable.SwipeActionLayout_al_background));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setElevation(
          a.getDimensionPixelSize(R.styleable.SwipeActionLayout_al_elevation, defaultElevation));
    }

    container.setOrientation(LinearLayout.HORIZONTAL);
    container.setGravity(CENTER_VERTICAL);

    // Action Item
    imageViewMargin =
        a.getDimensionPixelSize(R.styleable.SwipeActionLayout_ai_margin, defaultSelectorMargin);
    selectedSize = a.getDimensionPixelSize(R.styleable.SwipeActionLayout_al_selected_size,
        defaultSelectorSize);
    selectedImageView = new ImageView(context);
    final LayoutParams selectedLp = new LayoutParams(selectedSize, selectedSize, CENTER_VERTICAL);
    selectedLp.setMargins(0, imageViewMargin, 0, imageViewMargin);
    selectedImageView.setLayoutParams(selectedLp);

    Drawable selectorBackground = a.getDrawable(R.styleable.SwipeActionLayout_al_selector);
    if (selectorBackground != null) {
      selectedImageView.setBackground(selectorBackground);
    } else {
      selectedImageView.setBackground(res.getDrawable(R.drawable.default_selector));
    }

    Drawable layoutBackground = a.getDrawable(R.styleable.SwipeActionLayout_al_background);
    if (layoutBackground != null) {
      setBackground(layoutBackground);
    } else {
      TypedValue typedValue = new TypedValue();
      context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
      setBackgroundColor(typedValue.data);
    }

    a.recycle();

    addView(selectedImageView);
    addView(container);
  }

  public void populateActionItems(int... drawablesResIds) {
    final ArrayList<ActionItem> items = new ArrayList<>();
    for (int resId : drawablesResIds) {
      items.add(new ActionItem(resId));
    }
    populateActionItems(items);
  }

  public void populateActionItems(@Nullable final List<? extends ActionItem> items) {
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
            actionItemLayoutWidth,
            actionItemLayoutHeight,
            CENTER
        );
        imageLp.setMargins(imageViewMargin, imageViewMargin, imageViewMargin, imageViewMargin);
        imageView.setLayoutParams(imageLp);
        imageView.setBackgroundResource(item.drawableResId);

        imageViews.add(imageView);

        frame.addView(imageView);
        container.addView(frame);
      }
      selectedIndex = actionItems.size() / 2;
      imageViews.get(selectedIndex).setSelected(true);
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
          // left edge

          if (selectedIndex == 0) {
            break;
          }

          // actual progress
          final float t = getInRange((selectedIndex * iW + xS - pX) / (2f * xS), 0f, 1f);
          // scale
          final float sX = isScaleEnabled ? t / 2 + 1 : 1;
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
          // right edge

          if (selectedIndex == count - 1) {
            break;
          }

          // actual progress
          final float t = getInRange((pX - (selectedIndex + 1) * iW + xS) / (2f * xS), 0f, 1f);
          // scale
          final float sX = isScaleEnabled ? t / 2 + 1 : 1;
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
          // middle
          selectedImageView.setTranslationX(selectedIndex * iW + (iW - selectedSize) / 2);
          selectedImageView.setScaleX(DEFAULT_SCALE);
        }
        break;
    }
  }

  private void setSelectedIndex(final int newSelectedIndex) {
    // Un-select previous index
    imageViews.get(selectedIndex).setSelected(false);

    selectedIndex = newSelectedIndex;
    isAnimating = true;

    final int iW = measuredWidth / actionItems.size();
    final int target = iW * selectedIndex + (iW - selectedSize) / 2;
    final float currentScale = selectedImageView.getScaleX();
    final float currentTranslation = selectedImageView.getTranslationX();

    final Animation animation = new Animation() {
      @Override protected void applyTransformation(float t, Transformation transformation) {
        ViewCompat.setScaleX(selectedImageView, (DEFAULT_SCALE - currentScale) * t + currentScale);
        ViewCompat.setTranslationX(selectedImageView,
            (target - currentTranslation) * t + currentTranslation);
      }
    };

    animation.setAnimationListener(animationListener);
    animation.setDuration(shortAnimDuration);
    animation.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
    selectedImageView.clearAnimation();
    selectedImageView.startAnimation(animation);
  }

  private int getInRange(final int value, final int min, final int max) {
    return Math.max(min, Math.min(max, value));
  }

  private float getInRange(final float value, final float min, final float max) {
    return Math.max(min, Math.min(max, value));
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public void finishAction(Animation.AnimationListener mActionListener) {
    actionListener = mActionListener;
    setSelectedIndex(selectedIndex);
  }

  public void onLayoutTranslated(final float progress) {

  }

  public ActionItem getActionItem(int index) {
    return actionItems.get(index);
  }
}
