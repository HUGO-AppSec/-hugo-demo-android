/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.notepad.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.nononsenseapps.helpers.TimeFormatter;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.fragments.TaskDetailFragment;
import com.nononsenseapps.ui.TitleNoteTextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is the service that provides the factory to be bound to the collection service
 */
public class ListWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new ListRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	/**
	 * This is the factory that will provide data to the collection widget
	 */
	static class ListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

		final private Context mContext;
		private Cursor mCursor;
		final private int mAppWidgetId;
		private SimpleDateFormat mDateFormatter = null;
		private SimpleDateFormat weekdayFormatter;

		public ListRemoteViewsFactory(Context context, Intent intent) {
			mContext = context;
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		@Override
		public void onCreate() {}

		@Override
		public void onDestroy() {
			if (mCursor != null) {
				mCursor.close();
			}
		}

		@Override
		public int getCount() {
			if (mCursor != null)
				return mCursor.getCount();
			else
				return 0;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			// Get widget settings
			final WidgetPrefs widgetPrefs = new WidgetPrefs(mContext, mAppWidgetId);
			if (!widgetPrefs.isPresent()) {
				return null;
			}

			// load date formatter if not present
			if (mDateFormatter == null) {
				mDateFormatter = TimeFormatter.getLocalFormatterMicro(mContext);
			}

			final long listId = widgetPrefs
					.getLong(ListWidgetConfig.KEY_LIST, ListWidgetConfig.ALL_LISTS_ID);
			final int theme = widgetPrefs
					.getInt(ListWidgetConfig.KEY_THEME, ListWidgetConfig.DEFAULT_THEME);
			final int primaryTextColor = widgetPrefs
					.getInt(ListWidgetConfig.KEY_TEXTPRIMARY, ListWidgetConfig.DEFAULT_TEXTPRIMARY);
			final int rows = widgetPrefs
					.getInt(ListWidgetConfig.KEY_TITLEROWS, ListWidgetConfig.DEFAULT_ROWS);
			final boolean isCheckboxHidden = widgetPrefs
					.getBoolean(ListWidgetConfig.KEY_HIDDENCHECKBOX, false);
			boolean isDateHidden = widgetPrefs
					.getBoolean(ListWidgetConfig.KEY_HIDDENDATE, false);

			// TODO rest

			if (weekdayFormatter == null) {
				weekdayFormatter = TimeFormatter.getLocalFormatterWeekday(mContext);
			}

			RemoteViews rv = null;
			if (mCursor.moveToPosition(position)) {
				// column names of this cursor are in Task.Columns.FIELDS
				boolean isHeader = mCursor.getLong(0) < 1;
				if (isHeader) {
					rv = new RemoteViews(mContext.getPackageName(), R.layout.widgetlist_header);
					rv.setTextColor(android.R.id.text1, primaryTextColor);
					rv.setBoolean(R.id.relativeLayout_of_the_widgetlist_header,
							"setClickable", false);

					String sTemp = mCursor.getString(1);
					long dueDateMillis = mCursor.getLong(4);
					sTemp = Task.getHeaderNameForListSortedByDate(sTemp, dueDateMillis,
							weekdayFormatter, mContext);

					// Set text
					rv.setTextViewText(android.R.id.text1, sTemp);
					// TODO does not update, shows "loading..."
				} else {
					rv = new RemoteViews(mContext.getPackageName(), R.layout.widgetlist_item);

					// Complete checkbox
					final int visibleCheckBox;
					final int hiddenCheckBox;
					if (theme == ListWidgetConfig.THEME_LIGHT) {
						hiddenCheckBox = R.id.completedCheckBoxDark;
						visibleCheckBox = R.id.completedCheckBoxLight;
					} else {
						hiddenCheckBox = R.id.completedCheckBoxLight;
						visibleCheckBox = R.id.completedCheckBoxDark;
					}
					rv.setViewVisibility(hiddenCheckBox, View.GONE);
					rv.setViewVisibility(visibleCheckBox,
							isCheckboxHidden ? View.GONE : View.VISIBLE);
					// Spacer
					rv.setViewVisibility(R.id.itemSpacer,
							isCheckboxHidden ? View.GONE : View.VISIBLE);

					// Date
					if (mCursor.isNull(4)) {
						rv.setTextViewText(R.id.dueDate, "");
						isDateHidden = true;
					} else {
						rv.setTextViewText(R.id.dueDate, mDateFormatter
								.format(new Date(mCursor.getLong(4))));
					}
					rv.setViewVisibility(R.id.dueDate, isDateHidden ? View.GONE : View.VISIBLE);
					rv.setTextColor(R.id.dueDate, primaryTextColor);

					// Text
					rv.setTextColor(android.R.id.text1, primaryTextColor);
					rv.setInt(android.R.id.text1, "setMaxLines", rows);

					// Only if task it not locked
					if (mCursor.getInt(9) != 1) {
						rv.setTextViewText(android.R.id.text1, TitleNoteTextView.getStyledText(
								mCursor.getString(1), mCursor.getString(2),
								1.0f, 1, 0));
					} else {
						// Just title
						rv.setTextViewText(android.R.id.text1, TitleNoteTextView.getStyledText(
								mCursor.getString(1), 1.0f, 1, 0));
					}

					// Set the click intent
					if (widgetPrefs.getBoolean(ListWidgetConfig.KEY_LOCKSCREEN, false)) {
						final Intent clickIntent = new Intent()
								.setAction(Intent.ACTION_EDIT)
								.setData(Task.getUri(mCursor.getLong(0)))
								.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
						rv.setOnClickFillInIntent(R.id.widget_item, clickIntent);
					} else {
						final Intent fillInIntent = new Intent()
								.setAction(ListWidgetProvider.CLICK_ACTION)
								.putExtra(ListWidgetProvider.EXTRA_NOTE_ID, mCursor.getLong(0))
								.putExtra(ListWidgetProvider.EXTRA_LIST_ID, listId);
						rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
					}

					// Set complete broadcast
					// If not on lock screen, send broadcast to complete.
					// Otherwise, have to open note
					final Intent completeIntent = new Intent();
					if (widgetPrefs.getBoolean(ListWidgetConfig.KEY_LOCKSCREEN, false)) {
						completeIntent
								.setAction(Intent.ACTION_EDIT)
								.setData(Task.getUri(mCursor.getLong(0)))
								.putExtra(TaskDetailFragment.ARG_ITEM_LIST_ID, listId);
					} else {
						completeIntent
								.setAction(ListWidgetProvider.COMPLETE_ACTION)
								.putExtra(ListWidgetProvider.EXTRA_NOTE_ID,
										mCursor.getLong(0));
					}
					rv.setOnClickFillInIntent(R.id.completedCheckBoxDark, completeIntent);
					rv.setOnClickFillInIntent(R.id.completedCheckBoxLight, completeIntent);
				}
			}

			return rv;
		}

		@Override
		public RemoteViews getLoadingView() {
			// We aren't going to return a default loading view in this sample
			return null;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public void onDataSetChanged() {
			// Revert back to our process' identity so we can work with our
			// content provider
			final long identityToken = Binder.clearCallingIdentity();

			// Refresh the cursor
			if (mCursor != null) {
				mCursor.close();
			}

			// (re)load dateformatter in case preferences changed
			mDateFormatter = TimeFormatter.getLocalFormatterMicro(mContext);

			// Get widget settings
			final WidgetPrefs widgetPrefs = new WidgetPrefs(mContext, mAppWidgetId);

			final Uri targetUri;
			final long listId = widgetPrefs.getLong(ListWidgetConfig.KEY_LIST,
					ListWidgetConfig.ALL_LISTS_ID);
			final String sortSpec;
			final String sortType = widgetPrefs.getString(ListWidgetConfig.KEY_SORT_TYPE,
					mContext.getString(R.string.default_sorttype));

			if (sortType.equals(mContext
					.getString(R.string.const_possubsort)) && listId > 0) {
				targetUri = Task.URI;
				sortSpec = Task.Columns.LEFT;
			} else if (sortType.equals(mContext
					.getString(R.string.const_modified))) {
				targetUri = Task.URI;
				sortSpec = Task.Columns.UPDATED + " DESC";
			}
			// due date sorting
			else if (sortType.equals(mContext
					.getString(R.string.const_duedate))) {
				targetUri = Task.URI_SECTIONED_BY_DATE;
				sortSpec = null;
			}
			// Alphabetic
			else {
				targetUri = Task.URI;
				sortSpec = mContext.getString(R.string.const_as_alphabetic,
						Task.Columns.TITLE);
			}

			String listWhere;
			String[] listArg;
			if (listId > 0) {
				listWhere = Task.Columns.DBLIST + " IS ? AND " + Task.Columns.COMPLETED
						+ " IS NULL";
				listArg = new String[] { Long.toString(listId) };
			} else {
				listWhere = Task.Columns.COMPLETED + " IS NULL";
				listArg = null;
			}

			mCursor = mContext.getContentResolver()
					.query(targetUri, Task.Columns.FIELDS, listWhere, listArg, sortSpec);

			// Restore the identity - not sure if it's needed since we're going
			// to return right here, but it just *seems* cleaner
			Binder.restoreCallingIdentity(identityToken);
		}
	}
}