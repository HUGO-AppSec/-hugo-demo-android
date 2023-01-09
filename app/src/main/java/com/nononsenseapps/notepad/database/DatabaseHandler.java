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

package com.nononsenseapps.notepad.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.nononsenseapps.helpers.NnnLogger;
import com.nononsenseapps.helpers.RFC3339Date;
import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.Constants;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTask;
import com.nononsenseapps.notepad.sync.googleapi.GoogleTaskList;

import java.util.Calendar;
import java.util.HashMap;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler singleton;

	public static DatabaseHandler getInstance(final Context context) {
		if (singleton == null) {
			singleton = new DatabaseHandler(context);
		}
		return singleton;
	}

	private static final int DATABASE_VERSION = 15;
	public static final String DATABASE_NAME = "nononsense_notes.db";

	private final Context context;
	private final String testPrefix;

	/**
	 * Should use the singleton for normal cases
	 */
	private DatabaseHandler(Context context) {
		this(context, "");
	}

	/**
	 * Use only for JUNIT tests
	 */
	public DatabaseHandler(Context context, String testPrefix) {
		super(context, testPrefix + DATABASE_NAME, null, DATABASE_VERSION);
		// Good idea to have the context that doesn't die with the window
		this.context = context.getApplicationContext();
		this.testPrefix = testPrefix;
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		if (!db.isReadOnly()) {
			// Enable foreign key constraints
			// This would require android16
			// db.setForeignKeyConstraintsEnabled(true);
			// This works everywhere
			db.execSQL("PRAGMA foreign_keys=ON;");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TaskList.CREATE_TABLE);
		db.execSQL(Task.CREATE_TABLE);
		db.execSQL(Task.CREATE_DELETE_TABLE);
		db.execSQL(Task.CREATE_HISTORY_TABLE);
		db.execSQL(Notification.CREATE_TABLE);
		db.execSQL(RemoteTaskList.CREATE_TABLE);
		db.execSQL(RemoteTask.CREATE_TABLE);

		db.execSQL(Notification.CREATE_JOINED_VIEW);

		db.execSQL(Task.TRIGGER_PRE_INSERT);
		db.execSQL(Task.TRIGGER_PRE_DELETE);
		db.execSQL(Task.TRIGGER_POST_DELETE);
		db.execSQL(Task.TRIGGER_MOVE_LIST);
		db.execSQL(Task.CREATE_HISTORY_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_HISTORY_UPDATE_TRIGGER);

		db.execSQL(RemoteTask.TRIGGER_LISTDELETE_CASCADE);
		// Mark as deleted when real item deleted
		db.execSQL(RemoteTask.TRIGGER_REALDELETE_MARK);
		db.execSQL(RemoteTaskList.TRIGGER_REALDELETE_MARK);
		// Create move list trigger
		db.execSQL(RemoteTask.TRIGGER_MOVE_LIST);

		// Search tables
		db.execSQL(Task.CREATE_FTS3_TABLE);
		db.execSQL(Task.CREATE_FTS3_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_UPDATE_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETE_TRIGGER);

		// Delete search tables
		db.execSQL(Task.CREATE_FTS3_DELETE_TABLE);
		db.execSQL(Task.CREATE_FTS3_DELETED_INSERT_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETED_UPDATE_TRIGGER);
		db.execSQL(Task.CREATE_FTS3_DELETED_DELETE_TRIGGER);

		initializedDB(db);
	}

	public static Cursor getLegacyLists(final SQLiteDatabase legacyDB) {
		return legacyDB.rawQuery("SELECT lists."
				+ BaseColumns._ID
				+ ",lists.title,gtasklists.googleid,gtasklists.googleaccount"
				+ " FROM " + LegacyDBHelper.NotePad.Lists.TABLE_NAME
				+ " LEFT OUTER JOIN "
				+ LegacyDBHelper.NotePad.GTaskLists.TABLE_NAME + " ON ("
				+ LegacyDBHelper.NotePad.Lists.TABLE_NAME + "."
				+ LegacyDBHelper.NotePad.Lists._ID + " = "
				+ LegacyDBHelper.NotePad.GTaskLists.TABLE_NAME + "."
				+ LegacyDBHelper.NotePad.GTaskLists.COLUMN_NAME_DB_ID + ")"
				+ " WHERE lists.deleted IS NOT 1", null);
	}

	public static Cursor getLegacyNotes(final SQLiteDatabase legacyDB) {
		return legacyDB.rawQuery("SELECT notes."
						+ BaseColumns._ID
						+ ",notes.title,notes.note,notes.duedate,notes.gtaskstatus,notes.list,notes.modified,gtasks.googleid,gtasks.googleaccount"
						+ " FROM "
						+ LegacyDBHelper.NotePad.Notes.TABLE_NAME
						+ " LEFT OUTER JOIN "
						+ LegacyDBHelper.NotePad.GTasks.TABLE_NAME
						+ " ON ("
						+ LegacyDBHelper.NotePad.Notes.TABLE_NAME
						+ "."
						+ LegacyDBHelper.NotePad.Notes._ID
						+ " = "
						+ LegacyDBHelper.NotePad.GTasks.TABLE_NAME
						+ "."
						+ LegacyDBHelper.NotePad.GTasks.COLUMN_NAME_DB_ID
						+ ")"
						+ " WHERE notes.deleted IS NOT 1 AND notes.hiddenflag IS NOT 1",
				null);
	}

	public static Cursor getLegacyNotifications(final SQLiteDatabase legacyDB) {
		return legacyDB.query(LegacyDBHelper.NotePad.Notifications.TABLE_NAME,
				new String[] { "time", "permanent", "noteid" }, null,
				null, null, null, null);
	}

	private void initializedDB(final SQLiteDatabase db) throws SQLiteException {
		// Load legacy DB if it exists
		// Open database and copy information
		// Remember to do try except

		db.beginTransaction();
		try {
			final HashMap<Long, Long> listIDMap = new HashMap<>();
			final HashMap<Long, Long> taskIDMap = new HashMap<>();
			final LegacyDBHelper legacyDBHelper = new LegacyDBHelper(context, testPrefix);
			final SQLiteDatabase legacyDB = legacyDBHelper.getReadableDatabase();

			// First copy lists
			Cursor c = getLegacyLists(legacyDB);

			while (!c.isClosed() && c.moveToNext()) {
				TaskList tl = new TaskList();
				tl.title = c.getString(1);
				tl.updated = Calendar.getInstance().getTimeInMillis();

				// insert into db
				tl.insert(context, db);
				// remember id
				listIDMap.put(c.getLong(0), tl._id);

				// handle gtask info
				GoogleTaskList rl;
				if (c.getString(2) != null
						&& !c.getString(2).isEmpty()
						&& c.getString(3) != null
						&& !c.getString(3).isEmpty()) {
					rl = new GoogleTaskList(tl._id, c.getString(2), tl.updated,
							c.getString(3));
					rl.insert(context, db);
				}
			}
			c.close();

			// Then notes
			if (!listIDMap.isEmpty()) {
				// query
				c = getLegacyNotes(legacyDB);

				// iterate over notes
				while (!c.isClosed() && c.moveToNext()) {
					Task t = new Task();
					t.title = c.getString(1);
					t.note = c.getString(2);

					if (t.note.contains("[locked]")) {
						t.locked = true;
						t.note = t.note.replace("[locked]", "");
					}

					try {
						t.due = RFC3339Date
								.parseRFC3339Date(c.getString(3))
								.getTime();
					} catch (Exception e) {
						NnnLogger.warning(DatabaseHandler.class, "date error");
					}

					// completed must be converted
					if (c.getString(4) != null
							&& "completed".equals(c.getString(4))) {
						t.setAsCompleted();
					}
					t.dblist = listIDMap.get(c.getLong(5));

					t.updated = c.getLong(6);

					// insert
					// Just make extra sure list exists
					if (t.dblist != null) {
						t.insert(context, db);

						// put in idmap
						taskIDMap.put(c.getLong(0), t._id);
					}

					// gtask
					GoogleTask gt;
					if (!c.isNull(7)
							&& !c.getString(7).isEmpty()
							&& !c.isNull(8)
							&& !c.getString(8).isEmpty()) {
						gt = new GoogleTask(t, c.getString(8));
						gt.remoteId = c.getString(7);
						gt.updated = t.updated;
						gt.insert(context, db);
					}

				}
				c.close();
			}

			// Then notifications
			if (!taskIDMap.isEmpty()) {
				c = getLegacyNotifications(legacyDB);

				while (!c.isClosed() && c.moveToNext()) {
					// Make sure id exists
					if (taskIDMap.containsValue(c.getLong(2))) {
						var n = new Notification(taskIDMap.get(c.getLong(2)));
						n.time = c.getLong(0);
						// permanent was not supported at the time
						// insert
						n.insert(context, db);
					}
				}
				c.close();
			}

			// Complete, close the legacy db
			legacyDB.close();
		} catch (SQLException e) {
			// Database must have been empty. Ignore it
		}

		// ------------
		// If no lists, insert a list and example note.
		// ------------

		Cursor c = db.query(TaskList.TABLE_NAME, TaskList.Columns.FIELDS, null,
				null, null, null, null);

		if (!c.isClosed() && c.getCount() > 0) {
			// there is already a database: don't add anything
		} else {
			// there wasn't a database: add a new list (called "tasks" in the user's language)
			final TaskList tl = new TaskList();
			tl.title = context.getString(R.string.tasks);
			tl.insert(context, db);

			// compose a note that is shown when the app is first installed
			String welcomeNoteText =
					// first the title. the \n separates it from the content
					context.getString(R.string.welcome_note_title) + "\n"
							// an hint visible also on the task list
							+ context.getString(R.string.welcome_note_row_2) + "\n\n\n"
							// when the user open the task, he is told to open the tutorial
							+ context.getString(R.string.welcome_note_row_3) + " "
							+ Constants.TUTORIAL_URL;

			final Task task = new Task();
			task.setText(welcomeNoteText);
			task.dblist = tl._id;
			try {
				task.insert(context, db);
			} catch (Exception e) {
				// well, whatever, the note will not be added. I'm sure the user will find the
				// tutorial anyway...
				NnnLogger.exception(e);
			}
		}
		c.close();
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 10) {
			// Notification locations
			// Add columns
			String preName = "ALTER TABLE " + Notification.TABLE_NAME + " ADD COLUMN ";
			String postText = " TEXT";
			String postReal = " REAL";
			db.execSQL(preName + Notification.Columns.LOCATIONNAME + postText);
			db.execSQL(preName + Notification.Columns.LATITUDE + postReal);
			db.execSQL(preName + Notification.Columns.LONGITUDE + postReal);
			db.execSQL(preName + Notification.Columns.RADIUS + postReal);
			// Drop view
			db.execSQL("DROP VIEW IF EXISTS " + Notification.WITH_TASK_VIEW_NAME);
			// Recreate view with additional tables
			db.execSQL(Notification.CREATE_JOINED_VIEW);
		}
		if (oldVersion < 11) {
			// Mark as deleted when real item deleted
			db.execSQL(RemoteTask.TRIGGER_REALDELETE_MARK);
			db.execSQL(RemoteTaskList.TRIGGER_REALDELETE_MARK);
		}
		if (oldVersion < 12) {
			// Recreate trigger
			db.execSQL("DROP TRIGGER IF EXISTS task_post_delete");
			db.execSQL(Task.TRIGGER_POST_DELETE);
		}
		if (oldVersion < 13) {
			// Create move list trigger
			db.execSQL(RemoteTask.TRIGGER_MOVE_LIST);
			// Create trigger to fix positions when moving lists
			db.execSQL(Task.TRIGGER_MOVE_LIST);
		}
		if (oldVersion < 14) {
			// Update history update trigger
			db.execSQL("DROP TRIGGER IF EXISTS " + Task.HISTORY_UPDATE_TRIGGER_NAME);
			db.execSQL(Task.CREATE_HISTORY_UPDATE_TRIGGER);
		}
		if (oldVersion < 15) {
			// Drop view, changing to temporary view instead
			db.execSQL("DROP VIEW IF EXISTS " + Notification.WITH_TASK_VIEW_NAME);
		}
		// TODO if you want to change the database, add code here to handle the upgrade!
	}

	/**
	 * Used by Espresso tests to remove the whole database
	 * when cleaning up after tests
	 */
	public static void resetDatabase(Context context) {
		context.deleteDatabase(DatabaseHandler.DATABASE_NAME);
		singleton = new DatabaseHandler(context);
		DatabaseHandler.getInstance(context).getWritableDatabase();
	}
}
