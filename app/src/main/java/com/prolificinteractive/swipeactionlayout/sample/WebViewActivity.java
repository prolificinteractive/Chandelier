package com.prolificinteractive.swipeactionlayout.sample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.prolificinteractive.swipeactionlayout.widget.ActionItem;
import com.prolificinteractive.swipeactionlayout.widget.SwipeActionLayout;
import java.util.Arrays;

public class WebViewActivity extends AppCompatActivity {

  private SwipeActionLayout swipeActionLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_web_view);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    WebView webView = (WebView) findViewById(R.id.web_view);
    webView.setWebViewClient(new WebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);
    webView.loadUrl("https://www.github.com/");

    swipeActionLayout = (SwipeActionLayout) findViewById(R.id.swipe_action_layout);
    swipeActionLayout.setOnActionSelectedListener(new SwipeActionLayout.OnActionListener() {
      @Override public void onActionSelected(int index) {
        Toast.makeText(WebViewActivity.this, index + "", Toast.LENGTH_SHORT).show();
      }
    });

    swipeActionLayout.populateActionItems(Arrays.asList(
        new ActionItem(R.drawable.ic_close_selected, R.drawable.ic_close_light),
        new ActionItem(R.drawable.ic_github_selected, R.drawable.ic_github),
        new ActionItem(R.drawable.ic_pull_request_selected, R.drawable.ic_pull_request)
    ));

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });
  }
}
