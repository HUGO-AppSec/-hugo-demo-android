package com.nononsenseapps.notepad.interfaces;

import android.net.Uri;
import android.view.View;

import androidx.fragment.app.Fragment;

/**
 * This interface must be implemented by activities that contain
 * fragments to allow an interaction in this fragment to be communicated to
 * the activity and potentially other fragments contained in that activity.
 */
public interface OnFragmentInteractionListener {

	void onFragmentInteraction(final Uri uri, final long listId, final View origin);

	void addTaskInList(final String text, final long listId);

	void closeFragment(final Fragment fragment);
}
