package com.ml.hackernews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Shows a list of posts in a listview.
 * @author Malte Lenz
 *
 */
public class Posts extends Activity {
	/** Tag for logging. */
    private static final String TAG = "HN->Posts";

	private static final int MENU_REFRESH = 0;
	private static final int MENU_SWITCH_PAGE = 1;

	private static final String PAGE_NEWS = "News";
	private static final String PAGE_ASK = "Ask";
	private static final String PAGE_BEST = "Best";
	private static final String PAGE_NEWEST = "Newest";
	private static final String PAGE_ACTIVE = "Active";
	private static final String PAGE_CLASSIC = "Classic";

	private String[] pages = {PAGE_NEWS, PAGE_ASK, PAGE_BEST, PAGE_NEWEST, PAGE_ACTIVE, PAGE_CLASSIC};

	private static final int LONG_CLICK_COMMENTS = 1;
	private static final int LONG_CLICK_LINK = 2;

	private static final int DIALOG_SWITCH_PAGE = 0;

	private ProgressDialog busy;
	private ArrayList<Post> resultList;
	private ListView resultView;
	private String currentPage;

	/**
	 * Called on creation.
	 * @param savedInstanceState from last time
	 */
    @Override
	public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		//prepare a progress dialog
		busy = new ProgressDialog(this);
		busy.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		busy.setMessage("Fetching news items...");
		busy.setCancelable(false);

		resultView = (ListView) findViewById(R.id.result_list);

		currentPage = PAGE_NEWS;
		//Fetch news from server
		new GetPage().execute(currentPage.toLowerCase());
		busy.show();

		registerForContextMenu(resultView);
		//register for clicks on news items
		resultView.setOnItemClickListener(new OnItemClickListener() {
        	@Override
			public void onItemClick(final AdapterView<?> parent, final View v, final int position, final long id) {
                showLink((int) id);
            }
        });
    }

	/**
	 * Creates the menu called by the menu button.
	 * @param menu the menu to modify
	 * @return the menu
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(Menu.NONE, MENU_REFRESH, Menu.NONE, "Refresh").setIcon(R.drawable.ic_menu_refresh);
		menu.add(Menu.NONE, MENU_SWITCH_PAGE, Menu.NONE, "Show other page").setIcon(android.R.drawable.ic_menu_help);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Called when the user has pressed a menu item.
	 * @param item the clicked item
	 * @return if the click was handled
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			//refresh the list
			new GetPage().execute(currentPage.toLowerCase());
			return true;
		case MENU_SWITCH_PAGE:
			showDialog(DIALOG_SWITCH_PAGE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Create the menu for long clicks on an item.
	 * @param menu the menu before we alter it
	 * @param v the view
	 * @param menuInfo info about the menu
	 */
    @Override
	public final void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, LONG_CLICK_COMMENTS, 0, R.string.long_click_show_comments);
		menu.add(1, LONG_CLICK_LINK, 0, R.string.long_click_show_link);
	}

    /**
     * Called when user long clicked an item.
     * @param item the item clicked
     * @return if successfully handled
     */
    @Override
	public final boolean onContextItemSelected(final MenuItem item) {

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {
			case LONG_CLICK_COMMENTS:
	    		showHnItem((int) info.id);
		        return true;
			case LONG_CLICK_LINK:
	    		showLink((int) info.id);
	    		return true;
	    	default:
	    		break;
		}
		return super.onContextItemSelected(item);
	}

    /**
     * Called when creating a dialog (not necessarily each time dialog is shown).
     * @param id what dialog to show
     * @return the Dialog object to show
     */
    protected final Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	Dialog dialog = null;
    	switch (id) {
    	case DIALOG_SWITCH_PAGE:
			builder.setTitle("Show page:");
    		builder.setItems(pages, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int item) {
					Log.d(TAG, "item chosen: " + item + " with name: " + pages[item]);
					//user has chosen, show the chosen page
					currentPage = pages[item];
					new GetPage().execute(currentPage.toLowerCase());
					busy.show();
				}
			});
    		dialog = builder.create();
    		break;
    	default:
    		break;
    	}
    	return dialog;
    }

	/**
	 * Start a browser for the given id.
	 * @param id which news item to show
	 */
	protected final void showLink(final int id) {
		String url = resultList.get(id).getLink();
		if (url.equals("")) {
			//no link, local hacker news item
			showHnItem(id);
		} else {
			resultList.get(id).setRead();
			Intent i = new Intent("android.intent.action.VIEW", Uri.parse(url));
			startActivity(i);
		}
	}

	/**
	 * shows a news item which is "local", such as a "Ask HN".
	 * @param id of the item to show
	 */
	private void showHnItem(final int id) {
		resultList.get(id).setRead();
		Intent i = new Intent(this, Comments.class);
		i.putExtra(Comments.KEY_INTENT_ID, Integer.parseInt(resultList.get(id).getId()));
		startActivity(i);
		return;
	}


	/**
	 * Fetches the given page from hackernews via the server.
	 * @author malte
	 *
	 */
	private class GetPage extends AsyncTask<String, Object, String> {

		@Override
		protected String doInBackground(final String... page) {
			//prepare request

			String url = Config.baseUrl + "home/" + page[0];
			final ResponseHandler<String> responseHandler = new BasicResponseHandler();
			final HttpClient client = new DefaultHttpClient();
			final HttpGet get = new HttpGet(url);
			Log.d(TAG, "Fetching url: " + get.getURI().toString());
			String searchResults = null;
			//call server
			try {
				searchResults = client.execute(get, responseHandler);
			} catch (ClientProtocolException e) {
				Log.e(TAG, "ClientProtocolException while searching: " + e.toString());
			} catch (IOException e) {
				Log.e(TAG, "IOException while searching: " + e.toString());
			} catch (java.lang.IllegalArgumentException e) {
				Log.e(TAG, "IllegalArgumentException while searching: " + e.toString());
			}
			return searchResults;
		}

		@Override
		protected void onPostExecute(final String results) {
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
		//set title of activity
		setTitle(getResources().getString(R.string.app_name) + " - " + currentPage);

		resultList = new ArrayList<Post>();
		try {
			final JSONArray jsonResults = new JSONArray(searchResults);

			Log.d(TAG, "Number of items found: " + jsonResults.length());
			//check if we have any hits
			if (jsonResults.length() == 0) {
				Toast.makeText(getBaseContext(), "No items found", Toast.LENGTH_LONG).show();
			}

			for (int i = 0; i < jsonResults.length(); i = i + 1) {
				resultList.add(new Post(jsonResults.getJSONObject(i), this));
			}
		} catch (JSONException e) {
			Log.e(TAG, "Could not decode results: " + e.toString());
		}

		Log.d(TAG, "Final list of search results: " + resultList.toString());

		final ResultAdapter resultAdapter = new ResultAdapter(this, R.layout.post_item, resultList);
		resultView.setAdapter(resultAdapter);
	}

	/**
	 * Adapter used for showing the list of results for a page.
	 * @author Malte Lenz
	 *
	 */
	private class ResultAdapter extends ArrayAdapter<Post> {

		private List<Post> posts;

		/**
		 * Constructor which saves the list of beers to a local field.
		 * @param context calling context
		 * @param textViewResourceId what text resource (xml file) to use for display
		 * @param items list of news items
		 */
		public ResultAdapter(final Context context, final int textViewResourceId, final List<Post> items) {
			super(context, textViewResourceId, items);
			posts = items;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			//Log.d(TAG, "Showing news item in position: " + position);
			View v = convertView;
			if (v == null) {
				final LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.post_item, null);
			}
			final Post b = posts.get(position);
			if (b != null) {
				final TextView bt = (TextView) v.findViewById(R.id.post_title);
				final TextView br = (TextView) v.findViewById(R.id.post_points);
				final TextView pc = (TextView) v.findViewById(R.id.post_comments);
				if (bt != null) {
					bt.setText(b.getTitle());
				}
				if (br != null) {
					br.setText(b.getPoints());
				}
				if (pc != null) {
					pc.setText(b.getCommentNr());
				}
				if (b.isRead()) {
					Log.d(TAG, "Setting to read: " + b.getId());
					v.setBackgroundColor(Color.DKGRAY);
				} else {
					v.setBackgroundColor(Color.BLACK);
				}
			}
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