package com.nononsenseapps.notepad.espresso_tests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.filters.LargeTest;

import com.nononsenseapps.notepad.R;

import org.junit.Before;
import org.junit.Test;

@LargeTest
public class TestCreateTaskListAndDeleteIt extends BaseTestClass {

	private String taskListName;

	@Before
	public void initStrings() {
		taskListName = "a random task list";
	}

	@Test
	public void testCreateTaskListAndDeleteIt() {

		EspressoHelper.hideShowCaseViewIfShown();
		EspressoHelper.createTaskList(taskListName);

		EspressoHelper.openDrawer();

		onView(allOf(withText(taskListName), withId(android.R.id.text1))).perform(longClick());
		EspressoHelper.waitUi();

		onView(isRoot()).inRoot(isDialog()).perform(closeSoftKeyboard());
		onView(withId(R.id.deleteButton)).perform(click());

		onView(withText(this.getStringResource(android.R.string.ok))).perform(click());
		onView(withText(taskListName)).check(doesNotExist());
	}

}
