package com.prolificinteractive.chandelier.widget;

import android.support.annotation.DrawableRes;

/**
 * Create a list of actions using this class to add to the {@link ChandelierLayout}.
 */
public class Ornament {
  public final int drawableResId;

  public Ornament(@DrawableRes final int drawableResId) {
    this.drawableResId = drawableResId;
  }

  public void execute() {

  }
}