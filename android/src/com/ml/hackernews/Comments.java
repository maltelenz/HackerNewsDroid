package com.ml.hackernews;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Show a post and its comments.
 * @author Malte Lenz
 *
 */
public class Comments extends Activity {
	/** Tag for logging. */
	private static final String TAG = "HN->Comments";

	/** Name of the id in the intent extras. */
	public static final String KEY_INTENT_ID = "id";

	private static final String KEY_POSTCOMMENTS = "postcomments";
	private static final String KEY_POSTNAME = "postname";
	private static final String KEY_POSTTEXT = "posttext";
	private static final String KEY_POSTPOINTS = "postpoints";

	/** How much to indent on each comment level. */
	private static final int LEVEL_INDENT = 10;

	private ProgressDialog busy;

	private String nrComments;

	private String name;

	private String text;

	private String points;

	private LinearLayout commentView;


	/**
	 * Called on creation.
	 * @param savedInstanceState from last time
	 */
    @Override
	public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comments);

        Integer id = getIntent().getExtras().getInt(KEY_INTENT_ID);
		//prepare a progress dialog
		busy = new ProgressDialog(this);
		busy.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		busy.setMessage("Fetching comments...");
		busy.setCancelable(false);

		commentView = (LinearLayout) findViewById(R.id.comments_list);
		Log.d(TAG, "commentView: " + commentView);
		//Fetch comments from server
		new GetComments().execute(id);
		busy.show();
    }

	/**
	 * Fetches the given page from hackernews via the server.
	 * @author malte
	 *
	 */
	private class GetComments extends AsyncTask<Integer, Object, String> {

		@Override
		protected String doInBackground(final Integer... itemid) {
			//prepare request

			String url = Config.baseUrl + "item/" + itemid[0];
			final ResponseHandler<String> responseHandler = new BasicResponseHandler();
			final HttpClient client = new DefaultHttpClient();
			final HttpGet get = new HttpGet(url);
			Log.d(TAG, "Fetching url: " + get.getURI().toString());
			String commentResults = null;
			//call server
			try {
				commentResults = client.execute(get, responseHandler);
			} catch (ClientProtocolException e) {
				Log.e(TAG, "ClientProtocolException while searching: " + e.toString());
			} catch (IOException e) {
				Log.e(TAG, "IOException while searching: " + e.toString());
			} catch (java.lang.IllegalArgumentException e) {
				Log.e(TAG, "IllegalArgumentException while searching: " + e.toString());
			}
			return commentResults;
		}

		@Override
		protected void onPostExecute(final String results) {
			Log.d(TAG, "Got result: " + results);
			busy.hide();
			if (results != null) {
				showResults(results);
			} else {
				Log.w(TAG, "Something went wrong in server contact.");
				//show message to the user about this
				Toast.makeText(getBaseContext(), "Could not contact server", Toast.LENGTH_LONG).show();
			}
			super.onPostExecute(results);
		}
	}


	/**
	 * Updates the listView of results the user sees.
	 * @param searchResults	a string in json format with search results
	 */
	public final void showResults(final String searchResults) {
		CommentTree resultList = new CommentTree();
		try {
			final JSONArray jsonResults = new JSONArray(searchResults);

			Log.d(TAG, "Number of items found: " + jsonResults.length());
			//check if we have any hits
			if (jsonResults.length() == 0) {
				Toast.makeText(getBaseContext(), "No items found", Toast.LENGTH_LONG).show();
			}

			for (int i = 0; i < jsonResults.length(); i = i + 1) {
				JSONObject json = jsonResults.getJSONObject(i);
				try {
					//check if this is info about the post
					text = json.getString(KEY_POSTTEXT);
					nrComments = json.getString(KEY_POSTCOMMENTS);
					name = json.getString(KEY_POSTNAME);
					points = json.getString(KEY_POSTPOINTS);
				} catch (JSONException e) {
					//this seems to be a comment
					resultList.addComment(json);
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode results: " + e.toString());
		}


		//show title
		TextView titleView = (TextView) findViewById(R.id.details_post_title);
		titleView.setText(name);

		TextView textView = (TextView) findViewById(R.id.details_post_text);
		if (!text.equals("")) {
			//post has a text, show that
			textView.setText(text);
		} else {
			//post has a link, show that
			//textView.setText(link);
		}

		final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		for (int i = 0; i < resultList.getArray().size(); i++) {
			final CommentTree.Comment c = resultList.getCommentInPosition(i);
			Log.d(TAG, "Comment fetched: " + c.toString());
			View v = vi.inflate(R.layout.comment_item, null);
			if (c != null) {
				final TextView ct = (TextView) v.findViewById(R.id.comment_text);
				final TextView cp = (TextView) v.findViewById(R.id.comment_points);
				final TextView ca = (TextView) v.findViewById(R.id.comment_author);
				final LinearLayout cl = (LinearLayout) v.findViewById(R.id.comment_level_layout);
				if (ct != null) {
					ct.setAutoLinkMask(Linkify.WEB_URLS);
					ct.setText(Html.fromHtml(c.getText()));
				}
				if (cp != null) {
					cp.setText(c.getPoints());
				}
				if (ca != null) {
					ca.setText(c.getAuthor());
				}
				if (cl != null) {
					cl.setPadding(10, 10, 0, 0);
					ImageView levelView;
					for (int j = 0; j < c.getIndent(); j++) {
						levelView = new ImageView(this);
						levelView.setImageDrawable(getResources().getDrawable(R.drawable.comment_indent));
						cl.addView(levelView, j); //.addView(levelView);
					}
				}
			}
			//v.setPadding(c.getIndent() * LEVEL_INDENT, 0, 0, 0);
			v.setBackgroundDrawable(getResources().getDrawable(R.drawable.bottom_border));
			commentView.addView(v);
		}
	}

	/**
	 * Adapter used for showing the list comments.
	 * @author Malte Lenz
	 *
	 */
	private class CommentAdapter extends ArrayAdapter<CommentTree.Comment> {

		private CommentTree commentTree;

		/**
		 * Constructor which saves the list of beers to a local field.
		 * @param context calling context
		 * @param textViewResourceId what text resource (xml file) to use for display
		 * @param arrayList list of comments
		 * @param tree a tree copy of list of comments
		 */
		public CommentAdapter(final Context context, final int textViewResourceId, final ArrayList<CommentTree.Comment> arrayList, final CommentTree tree) {
			super(context, textViewResourceId, arrayList);
			commentTree = tree;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			//Log.d(TAG, "Showing comment in position: " + position);
			View v = convertView;
			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.comment_item, null);
			}
			final CommentTree.Comment c = commentTree.getCommentInPosition(position);
			Log.d(TAG, "Comment fetched: " + c.toString());
			if (c != null) {
				final TextView ct = (TextView) v.findViewById(R.id.comment_text);
				final TextView cp = (TextView) v.findViewById(R.id.comment_points);
				final TextView ca = (TextView) v.findViewById(R.id.comment_author);
				if (ct != null) {
					ct.setText(c.getText());
				}
				if (cp != null) {
					cp.setText(c.getPoints());
				}
				if (ca != null) {
					ca.setText(c.getAuthor());
				}
			}
			v.setPadding(c.getIndent() * LEVEL_INDENT, 0, 0, 0);
			return v;
		}

	}

	@Override
	protected final void onDestroy() {
		//dismiss dialog
		busy.dismiss();
		super.onDestroy();
	}

}
