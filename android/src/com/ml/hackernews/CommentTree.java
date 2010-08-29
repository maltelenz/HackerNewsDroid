package com.ml.hackernews;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/**
 * Data structure representing a tree of comments for one post.
 * @author Malte Lenz
 *
 */
public class CommentTree {

	private static final String TAG = "HN->CommentTree";

	private static final String KEY_PARENT = "parent";
	private static final String KEY_ID = "id";
	private static final String KEY_TEXT = "text";
	private static final String KEY_POINTS = "points";
	private static final String KEY_POSTER = "poster";

	private ArrayList<Comment> comments;

	/**
	 * Add a comment to the tree.
	 * @param json structure of the comment
	 */
	public final void addComment(final JSONObject json) {
		String poster;
		String points;
		String text;
		String id;
		String parent;
		try {
			poster = json.getString(KEY_POSTER);
			points = json.getString(KEY_POINTS);
			text = json.getString(KEY_TEXT);
			id = json.getString(KEY_ID);
			parent = json.getString(KEY_PARENT);
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode comment: " + e.toString());
			return;
		}

		Integer parentComment;
		if (parent.equals("0")) {
			parentComment = -1;
		} else {
			parentComment = getComment(parent);
		}
		Log.d(TAG, "parentComment: " + parentComment);
		comments.add(new Comment(id, text, points, poster, parentComment));
	}


	/**
	 * Fetch a comment position by id from the tree.
	 * @param id of the parent
	 * @return the comment position
	 */
	private Integer getComment(final String id) {
		Iterator<Comment> commentitr = comments.iterator();
		int i = 0;
		while (commentitr.hasNext()) {
			Comment c = commentitr.next();
			if (c.id.equals(id)) {
				return i;
			}
			i++;
		}
		return null;
	}

	/**
	 * Fetch a comment on position in list.
	 * @param position in the list
	 * @return the comment
	 */
	public final Comment getCommentInPosition(final int position) {
		return comments.get(position);
	}

	/**
	 * Represents one comment and its location in the tree.
	 * @author Malte Lenz
	 *
	 */
	final class Comment {

		private String poster;
		private String points;
		private String text;
		private String id;
		private int parent;

		/**
		 * Create a new comment.
		 * @param newId given id
		 * @param newText text of the comment
		 * @param newPoints nr of points
		 * @param newPoster poster of the comment
		 * @param newParent position in the list of parent of the comment
		 */
		private Comment(final String newId, final String newText, final String newPoints, final String newPoster, final int newParent) {
			id = newId;
			text = newText;
			points = newPoints;
			poster = newPoster;
			parent = newParent;
		}

		/**
		 * Fetch text of the comment.
		 * @return text
		 */
		public String getText() {
			return text;
		}

		/**
		 * Fetch points of the comment.
		 * @return points
		 */
		public String getPoints() {
			return points;
		}

		/**
		 * Fetch author of the comment.
		 * @return author
		 */
		public String getAuthor() {
			return poster;
		}

		/**
		 * Returns the number of levels down this comment is in the tree.
		 * @return number of levels
		 */
		public Integer getIndent() {
			if (parent == -1) {
				return 0;
			}
			return getCommentInPosition(parent).getIndent() + 1;
		}
	}

	/**
	 * Empty default constructor.
	 */
	public CommentTree() {
		 comments = new ArrayList<Comment>();
	}

	/**
	 * Return a copy of our array.
	 * @return our comment array
	 */
	public final ArrayList<Comment> getArray() {
		return comments;
	}
}
