package com.prolificinteractive.swipeactionlayout.widget;

import android.support.annotation.DrawableRes;

/**
 * Create a list of actions using this class to add to the {@link SwipeActionLayout}.
 */
public class ActionItem {
  public final int drawableResId;

  public ActionItem(@DrawableRes final int drawableResId) {
    this.drawableResId = drawableResId;
  }

  public void execute() {

  }
}