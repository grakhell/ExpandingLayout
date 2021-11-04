# Expanding Layout
Android view layout that's can be expanded or collapsed

## Download
[![Maven Central](https://img.shields.io/maven-central/v/io.github.grakhell/ExpandingLayout.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.grakhell%22%20AND%20a:%22ExpandingLayout%22)

Grab via Gradle:

groovy
```groovy
    implementation 'io.github.grakhell:ExpandingLayout:$latest_version'
``` 
kotlin dsl
```kotlin
    implementation("io.github.grakhell:ExpandingLayout:$latest_version")
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

A layout can use a standard interpolator-based animator or a spring-based dynamic animator to animate its state change. This is selected by the xml flag app:uses_spring.
Used spring can be configured by method:
```kotlin
    expandingLayout.setSpring()
```
it takes [SpringForce](https://developer.android.com/reference/androidx/dynamicanimation/animation/SpringForce "") object as argument

And interpolator for non-dynamic animator is setting by method:
```kotlin
    expandingLayout.setInterpolator() 
```
it takes [Interpolator](https://developer.android.com/reference/android/view/animation/Interpolator "") object as argument.

Programmatically, the state can be set more precisely by the method:
```kotlin
	expandingLayout.setExpandState() // It takes a float argument ranging from 0 to 1, where 0 is completely collapsed and 1 is expanded
```

If a spring is used, duration setting has no effect. Otherwise, it sets the duration of the state change animation
```kotlin
	expandingLayout.setDuration(200l)
```

```kotlin
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