package com.prolificinteractive.swipeactionlayout.sample;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.prolificinteractive.swipeactionlayout.widget.ActionItem;
import com.prolificinteractive.swipeactionlayout.widget.SwipeActionLayout;
import java.util.Arrays;

public class WebViewActivity extends AppCompatActivity {

  public static final String GITHUB_URL = "https://www.github.com/";
  public static final String GITHUB_NOTIFICATIONS_URL = GITHUB_URL + "notifications";
  public static final String GITHUB_PULLS_URL = GITHUB_URL + "pulls";
  public static final String GITHUB_SEARCH_URL = GITHUB_URL + "search";

  private SwipeActionLayout swipeActionLayout;
  private WebView webView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_view);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    webView = (WebView) findViewById(R.id.web_view);
    webView.setWebViewClient(new WebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);
    webView.loadUrl(GITHUB_URL);

    swipeActionLayout = (SwipeActionLayout) findViewById(R.id.swipe_action_layout);
    swipeActionLayout.setOnActionSelectedListener(new SwipeActionLayout.OnActionListener() {
      @Override public void onActionSelected(int index, ActionItem action) {
        action.execute();
      }
    });
    swipeActionLayout.populateActionItems(Arrays.asList(
        new GitHubAction.Builder()
            .setDrawableResId(R.drawable.ic_notifications)
            .setUrl(GITHUB_NOTIFICATIONS_URL)
            .setWebView(webView)
            .build(),
        new GitHubAction.Builder()
            .setDrawableResId(R.drawable.ic_github)
            .setUrl(GITHUB_URL)
            .setWebView(webView)
            .build(),
        new GitHubAction.Builder()
            .setDrawableResId(R.drawable.ic_pull_request)
            .setUrl(GITHUB_PULLS_URL)
            .setWebView(webView)
            .build()
    ));

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        webView.loadUrl(GITHUB_SEARCH_URL);
      }
    });
  }

  static class GitHubAction extends ActionItem {
    private String url;
    private WebView webView;

    public GitHubAction(@DrawableRes int drawableResId) {
      super(drawableResId);
    }

    @Override public void execute() {
      if (!TextUtils.isEmpty(url)) {
        webView.loadUrl(url);
      }
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public void setWebView(WebView webView) {
      this.webView = webView;
    }

    static class Builder {
      String url = "";
      int drawableResId;
      WebView webView;

      public Builder setDrawableResId(@DrawableRes int resId) {
        drawableResId = resId;
        return this;
      }

      public Builder setUrl(@NonNull String url) {
        this.url = url;
        return this;
      }

      public Builder setWebView(@NonNull WebView webView) {
        this.webView = webView;
        return this;
      }

      public GitHubAction build() {
        if (url == null || webView == null) {
          throw new IllegalArgumentException("URL and WebView must be set");
        }
        GitHubAction action = new GitHubAction(drawableResId);
        action.setUrl(url);
        action.setWebView(webView);
        return action;
      }
    }
  }
}
