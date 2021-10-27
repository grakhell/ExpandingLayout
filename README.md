# Expanding Layout
Android view layout that's can be expanded or collapsed

## Download
Grab via Gradle:
```kotlin

```

## How to use
```XML
<ru.grakhell.expandinglayout.ExpandingLayout
	android:id="@+id/user_exp"
	android:layout_width="match_parent"
	android:layout_height="wrap_content"
	app:duration = "200"
	app:expanded = "false"
	app:uses_spring = "false"
	app:parallax = "0.6">
		<!--Any child View here, TabLayout for example. -->
</ru.grakhell.expandinglayout.ExpandingLayout>
```

```kotlin

	expandingLayout.setDuration(200l)
	expandingLayout.setParallax(0.6)
	
	expandingLayout.toggle() //By default it's animated but you can switch state without animation by .toggle(false)
```

There are exist some performance issues when ExpandingLayout have NestedScrollView or RecyclerView as a child. To fix that lib have bundled ExpandingNestedScrollView and ExpandingRecyclerView.

## Licence
```
Copyright 2021 Dmitrii Z.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```