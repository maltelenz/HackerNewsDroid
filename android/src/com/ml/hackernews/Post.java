package com.ml.hackernews;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
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
	private static final String KEY_ID = "id";
	private String title;
	private String points;
	private String commentNr;
	private String link;
	private String id;
	private DatabaseAdapter dbAdapter;
	private Context ctx;

	/**
	 * Constructor reading the fields from a jsonObject.
	 * @param json jsonObject representing the news item
	 * @param context calling context
	 */
	public Post(final JSONObject json, final Context context) {
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
		try {
			setId(json.getString(KEY_ID));
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode id: " + e.toString());
			return;
		}

		ctx = context;

		dbAdapter = new DatabaseAdapter(ctx);
		dbAdapter.open();
		dbAdapter.addOrUpdatePost(Integer.parseInt(id));
		dbAdapter.close();
	}

	/**
	 * set hackernews id.
	 * @param string of hackernews id
	 */
	private void setId(final String string) {
		id = string;
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

	/**
	 * Return the hackernews id of the post.
	 * @return hackernews id
	 */
	public final String getId() {
		Log.d(TAG, "getId: " + id);
		return id;
	}

	/**
	 * Return if the post is read.
	 * @return if this post is read
	 */
	public final boolean isRead() {
		dbAdapter = new DatabaseAdapter(ctx);
		dbAdapter.open();
		boolean res = dbAdapter.isPostRead(Integer.parseInt(id));
		dbAdapter.close();
		return res;
	}

	/**
	 * Set this post as read.
	 */
	public final void setRead() {
		dbAdapter = new DatabaseAdapter(ctx);
		dbAdapter.open();
		dbAdapter.markPostRead(Integer.parseInt(id));
		dbAdapter.close();
	}
}
