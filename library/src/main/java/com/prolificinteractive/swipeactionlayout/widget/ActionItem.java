package com.prolificinteractive.swipeactionlayout.widget;

import android.support.annotation.DrawableRes;

public class ActionItem {
  public final int drawableResId;

  public ActionItem(@DrawableRes final int drawableResId) {
    this.drawableResId = drawableResId;
  }

  public void execute() {

  }
}