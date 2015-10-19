package com.prolificinteractive.swipeactionlayout.widget;

import android.support.annotation.DrawableRes;

/**
 * Created by chahine on 10/19/15.
 */

public class ActionItem {
  public final int selectedResId;
  public final int unselectedResId;

  public ActionItem(
      @DrawableRes final int selectedDrawableResId,
      @DrawableRes final int unselectedDrawableResId
  ) {
    this.selectedResId = selectedDrawableResId;
    this.unselectedResId = unselectedDrawableResId;
  }
}