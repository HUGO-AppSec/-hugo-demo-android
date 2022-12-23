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

package com.nononsenseapps.notepad.android.helpers;

import android.content.Context;
import android.net.Uri;

import com.nononsenseapps.notepad.database.Notification;
import com.nononsenseapps.notepad.database.Task;
import com.nononsenseapps.notepad.database.TaskList;

/**
 * The purpose here is to make it easy for other classes to notify that
 * something has changed in the database. Will also call update on the widgets
 * appropriately.
 */
public class UpdateNotifier {

	/**
	 * Will update all notes and specific uri if present
	 */
	public static void notifyChangeNote(Context context) {
		notifyChange(context, Task.URI);
		updateWidgets(context);
	}

	/**
	 * Will update all notes and specific uri if present
	 *
	 * @param uri optional uri
	 */
	public static void notifyChangeNote(Context context, Uri uri) {
		notifyChange(context, uri);
		notifyChangeNote(context);
	}

	/**
	 * Will update all notes
	 *
	 */
	public static void notifyChangeList(Context context) {
		notifyChange(context, TaskList.URI);
		updateWidgets(context);
	}

	/**
	 * Will update all lists and specific uri if present
	 *
	 * @param uri optional uri
	 */
	public static void notifyChangeList(Context context, Uri uri) {
		notifyChange(context, uri);
		notifyChangeList(context);
	}

	/**
	 * Will update all lists and specific uri if present
	 * Always updates notifications
	 *
	 * @param uri optional uri
	 */
	private static void notifyChange(Context context, Uri uri) {
		if (uri != null) {
			context.getContentResolver().notifyChange(uri, null, false);
			context.getContentResolver().notifyChange(Notification.URI, null);
		}
	}

	/**
	 * Instead of doing this in a service which might be killed, simply call
	 * this whenever something is changed in here
	 *
	 * Update all widgets's views as this database has changed somehow
	 */
	public static void updateWidgets(Context context) {
		// TODO
	}
}
