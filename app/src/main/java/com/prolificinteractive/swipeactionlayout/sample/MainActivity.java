package com.prolificinteractive.swipeactionlayout.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.prolificinteractive.swipeactionlayout.widget.ActionItem;
import com.prolificinteractive.swipeactionlayout.widget.ActionLayout;
import com.prolificinteractive.swipeactionlayout.widget.SwipeActionLayout;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

  private SwipeActionLayout swipeActionLayout;
  private RecyclerView list;
  private DummyAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    swipeActionLayout = (SwipeActionLayout) findViewById(R.id.swipe_action_layout);
    swipeActionLayout.setOnActionSelectedListener(new SwipeActionLayout.OnActionListener() {
      @Override public void onActionSelected(int index) {
        Toast.makeText(MainActivity.this, index + "", Toast.LENGTH_SHORT).show();
      }
    });

    adapter = new DummyAdapter();
    list = (RecyclerView) findViewById(android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(adapter);

    swipeActionLayout.populateActionItems(Arrays.asList(
        new ActionItem(R.drawable.ic_close_purple_light, R.drawable.ic_close_light),
        new ActionItem(R.drawable.ic_add_purple_light, R.drawable.ic_add_light),
        new ActionItem(R.drawable.ic_check_purple_light, R.drawable.ic_check_light)
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

  class DummyAdapter extends RecyclerView.Adapter<DummyViewHolder> {

    @Override
    public DummyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new DummyViewHolder(
          LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dummy, list, false));
    }

    @Override
    public void onBindViewHolder(DummyViewHolder holder, int position) {
      holder.title.setText(String.format("Title %d", position));
      holder.subtitle.setText(String.format("Subtitle %d", position));
    }

    @Override
    public int getItemCount() {
      return 10;
    }
  }

  class DummyViewHolder extends RecyclerView.ViewHolder {

    final TextView title;
    final TextView subtitle;

    public DummyViewHolder(View view) {
      super(view);
      title = (TextView) view.findViewById(android.R.id.text1);
      subtitle = (TextView) view.findViewById(android.R.id.text2);
    }
  }
}
