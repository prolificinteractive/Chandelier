package com.prolificinteractive.chandelier.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.prolificinteractive.chandelier.widget.ChandelierLayout;
import com.prolificinteractive.chandelier.widget.Ornament;
import java.util.Arrays;

public class ScrollViewActivity extends AppCompatActivity {

  private ChandelierLayout chandelierLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_scroll_view);

    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    final TextView textView = (TextView) findViewById(R.id.text_view);
    final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
    setSupportActionBar(toolbar);

    chandelierLayout = (ChandelierLayout) findViewById(R.id.chandelier_layout);
    chandelierLayout.setOnActionSelectedListener(new ChandelierLayout.OnActionListener() {
      @Override public void onActionSelected(int index, Ornament action) {
        final String text;
        switch (index) {
          case 0:
            text = "Close";
            progressBar.setVisibility(View.GONE);
            break;
          case 1:
            text = "Add";
            progressBar.setVisibility(View.VISIBLE);
            break;
          case 2:
          default:
            text = "Check";
            progressBar.setVisibility(View.GONE);
            break;
        }
        textView.setText(text);
      }
    });

    chandelierLayout.populateActionItems(Arrays.asList(
        new Ornament(R.drawable.ic_close),
        new Ornament(R.drawable.ic_add),
        new Ornament(R.drawable.ic_check)
    ));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
