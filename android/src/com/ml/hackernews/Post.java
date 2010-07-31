package com.ml.hackernews;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Container of one news item.
 * @author Malte Lenz
 *
 */
public class Post {

	private static final String TAG = "HN->Post";
	private static final String KEY_TITLE = "title";
	private static final String KEY_POINTS = "points";
	private static final String KEY_COMMENTS = "comments";
	private static final String KEY_LINK = "link";
	private String title;
	private String points;
	private String commentNr;
	private String link;

	/**
	 * Constructor reading the fields from a jsonObject.
	 * @param json jsonObject representing the news item
	 */
	public Post(final JSONObject json) {
		try {
			setTitle(json.getString(KEY_TITLE));
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode title: " + e.toString());
			return;
		}
		try {
			setPoints(json.getString(KEY_POINTS));
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode points: " + e.toString());
			return;
		}
		try {
			setCommentNr(json.getString(KEY_COMMENTS));
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode comment nr: " + e.toString());
			return;
		}
		try {
			setLink(json.getString(KEY_LINK));
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode link: " + e.toString());
			return;
		}

	}

	/**
	 * sets link that news item points to.
	 * @param string full url
	 */
	private void setLink(final String string) {
		link = string;
	}

	/**
	 * sets number of comments.
	 * @param string number of comments in string format
	 */
	private void setCommentNr(final String string) {
		commentNr = string;
	}

	/**
	 * sets the number of upvotes.
	 * @param string number of upvotes in string format
	 */
	private void setPoints(final String string) {
		points = string;
	}

	/**
	 * sets the title of the news item.
	 * @param string of title.
	 */
	private void setTitle(final String string) {
		title = string;
	}

	/**
	 * fetch title.
	 * @return title
	 */
	public final CharSequence getTitle() {
		return title;
	}

	/**
	 * fetch number of upvotes.
	 * @return upvotes
	 */
	public final CharSequence getPoints() {
		return points;
	}

	/**
	 * fetch number of comments.
	 * @return nr of comments
	 */
	public final CharSequence getCommentNr() {
		return commentNr;
	}

	/**
	 * fetch the link the news item points to.
	 * @return link the news item points to
	 */
	public final String getLink() {
		return link;
	}

}
