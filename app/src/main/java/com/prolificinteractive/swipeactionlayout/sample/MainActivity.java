package com.prolificinteractive.swipeactionlayout.sample;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private static final String CATEGORY_SAMPLE = "com.prolificinteractive.swipeactionlayout.sample.SAMPLE";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    RecyclerView list = (RecyclerView) findViewById(android.R.id.list);
    list.setLayoutManager(new LinearLayoutManager(this));
    list.setAdapter(new SampleAdapter(this, getAllSampleActivities()));
  }

  private List<ResolveInfo> getAllSampleActivities() {
    Intent filter = new Intent();
    filter.setAction(Intent.ACTION_RUN);
    filter.addCategory(CATEGORY_SAMPLE);
    return getPackageManager().queryIntentActivities(filter, 0);
  }

  private class SampleAdapter extends RecyclerView.Adapter<SampleAdapter.SampleViewHolder> {
    private final List<ResolveInfo> samples;
    private final LayoutInflater inflater;
    private final PackageManager pm;

    public SampleAdapter(Context context, List<ResolveInfo> resolveInfos) {
      this.samples = resolveInfos;
      this.inflater = LayoutInflater.from(context);
      this.pm = context.getPackageManager();
    }

    @Override public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
      return new SampleViewHolder(view);
    }

    @Override public void onBindViewHolder(SampleViewHolder holder, int position) {
      holder.activityName.setText(samples.get(position).loadLabel(pm));
    }

    @Override public int getItemCount() {
      return samples.size();
    }

    class SampleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
      TextView activityName;

      public SampleViewHolder(View view) {
        super(view);
        activityName = (TextView) view.findViewById(android.R.id.text1);
        view.setOnClickListener(this);
      }

      @Override public void onClick(View v) {
        ActivityInfo activity = samples.get(getAdapterPosition()).activityInfo;
        ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);
        startActivity(new Intent(Intent.ACTION_VIEW).setComponent(name));
      }
    }
  }

}
