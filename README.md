Swipe Action Layout
===================

A nice swipe layout that provides new actions with a material design look and feel.

<img src="/images/demo1.gif" alt="Demo Screen Capture" width="300px" />
<img src="/images/demo2.gif" alt="Demo Screen Capture" width="300px" />

Usage
-----

1. Add `compile 'com.prolificinteractive:swipe-action-layout:1.0.0'` to your dependencies.
2. Wrap your list around `SwipeActionLayout` in your layouts or view hierarchy.
3. Add a list of `ActionItem` using `SwipeActionLayout#populateActionItems()` method.
4. Set a `OnActionSelectedListener` to listen for selected action and update the view accordingly.

Or

1. Add `compile 'com.prolificinteractive:swipe-action-layout:1.0.0'` to your dependencies.
2. Wrap your list around `SwipeActionLayout` in your layouts or view hierarchy.
3. Create Objects that extend `ActionItem` and add them using `SwipeActionLayout#populateActionItems()` method.
4. Override `ActionItem#Execute()` method to execute the action when selected.

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

Customization
-------------

XML attributes:

- `al_background` Set background color
- `al_elevation` Set elevation 
- `al_selector` Set background selector
- `al_animate_to_start_duration` Set the duration that the layout takes to get into its original position. Default is 300 Millisecond.

- `ai_margin` Set action item margin

Mare sure to check sample for more examples.

Contributing
============

Would you like to contribute? Fork us and send a pull request! Be sure to checkout our issues first.

## License

Swipe Action Layout is Copyright (c) 2016 Prolific Interactive. It may be redistributed under the terms specified in the [LICENSE] file.

[LICENSE]: /LICENSE

## Maintainers

![prolific](https://s3.amazonaws.com/prolificsitestaging/logos/Prolific_Logo_Full_Color.png)

Swipe Action Layout is maintained and funded by Prolific Interactive. The names and logos are trademarks of Prolific Interactive.
