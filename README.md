Swipe Action Layout
===================

A nice swipe layout that provides new actions with a material design look and feel.

<img src="/images/demo1.gif" alt="Demo Screen Capture" width="300px" />
<img src="/images/demo2.gif" alt="Demo Screen Capture" width="300px" />

Usage
-----

1. Add `compile 'com.prolificinteractive:swipe-action-layout:1.0.0'` to your dependencies.
2. Wrap your list around `SwipeActionLayout` in your layouts or view hierarchy.
3. Add a list of `ActionItem` using `MaterialCalendarView#populateActionItems()` method.
4. Set a `OnActionSelectedListener` to listen for selected action and update the view accordingly.

Or

1. Add `compile 'com.prolificinteractive:swipe-action-layout:1.0.0'` to your dependencies.
2. Wrap your list around `SwipeActionLayout` in your layouts or view hierarchy.
3. Create Objects that extend `ActionItem`
4. Override `ActionItem#Execute()` method to do the action when selected.

Example:

```xml
  <com.prolificinteractive.swipeactionlayout.widget.SwipeActionLayout
      android:id="@+id/swipe_action_layout"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      app:al_background="?attr/colorPrimary"
      >
      
      <!-- Insert your list here -->
      <include layout="@layout/example_recycler_view" />
  
  </com.prolificinteractive.swipeactionlayout.widget.SwipeActionLayout>
```

Contributing
============

Would you like to contribute? Fork us and send a pull request! Be sure to checkout our issues first.

## License

Material Calendar View is Copyright (c) 2016 Prolific Interactive. It may be redistributed under the terms specified in the [LICENSE] file.

[LICENSE]: /LICENSE

## Maintainers

![prolific](https://s3.amazonaws.com/prolificsitestaging/logos/Prolific_Logo_Full_Color.png)

Material Calendar View is maintained and funded by Prolific Interactive. The names and logos are trademarks of Prolific Interactive.
