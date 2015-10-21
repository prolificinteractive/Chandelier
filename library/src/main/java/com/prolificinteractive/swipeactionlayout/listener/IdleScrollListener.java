package com.prolificinteractive.swipeactionlayout.listener;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

public class IdleScrollListener extends RecyclerView.OnScrollListener implements AbsListView.OnScrollListener {
  Class parentType;
  int scrollState;

  public void setParent(View view) {
    parentType = view.getClass();
  }

  @Override public void onScrollStateChanged(AbsListView view, int scrollState) {
    this.scrollState = scrollState;
  }

  @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
      int totalItemCount) {

  }

  @Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
    super.onScrollStateChanged(recyclerView, newState);
    this.scrollState = newState;
  }

  @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    super.onScrolled(recyclerView, dx, dy);
  }

  public boolean isIdle() {
    if (parentType == AbsListView.class) {
      return scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE;
    }

    if (parentType == RecyclerView.class) {
      return scrollState == RecyclerView.SCROLL_STATE_IDLE;
    }

    return true;
  }
}
