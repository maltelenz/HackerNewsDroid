package com.ml.hackernews;

import java.util.concurrent.Semaphore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Acts as the only object accessing the database directly.
 * @author Malte Lenz
 *
 */
public class DatabaseAdapter {
	/** Increment this when changing database structure. */
    private static final int DATABASE_VERSION = 3;

    /** Id of the row. */
    public static final String KEY_ROWID = "_id";
    /** Itemid from hackernews. */
    public static final String KEY_ITEMID = "itemid";
    /** Marker if the post is read. */
    public static final String KEY_POST_READ = "read";

    private static final String TAG = "DatabaseAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE_POST = "post";

    /**
     * Database creation SQL statements.
     */
    private static final String DATABASE_CREATE_POST =
        "create table " + DATABASE_TABLE_POST + " (" + KEY_ROWID + " integer primary key autoincrement, "
        + KEY_ITEMID + " integer not null, "
        + KEY_POST_READ + " integer not null);";

    private final Context mCtx;
	private static final Semaphore USE_DB = new Semaphore(1, true);

	/**
	 * Helper for database access.
	 * @author Malte Lenz
	 */
    private static class DatabaseHelper extends SQLiteOpenHelper {

    	/**
    	 * Constructor.
    	 * @param context calling context
    	 */
        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_POST);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_POST);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created.
     * @param ctx the calling context
     */
    public DatabaseAdapter(final Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database.
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     */
    public final DatabaseAdapter open() {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    /**
     * Closes the database.
     */
    public final void close() {
    	mDbHelper.close();
    }

    /**
     * Add a post to database, if it does not exist.
     * @param itemid the hackernews id
     * @return number of rows affected
     */
    public final long addOrUpdatePost(final Integer itemid) {
    	ContentValues initialValues = new ContentValues();
    	initialValues.put(KEY_ITEMID, itemid);
    	try {
			DatabaseAdapter.USE_DB.acquire();
		} catch (InterruptedException e1) {
			return 0;
		}
       	long res = mDb.update(DATABASE_TABLE_POST, initialValues, KEY_ITEMID + "=" + itemid, null);
       	if (res == 0) {
           	if (mDb.insert(DATABASE_TABLE_POST, null, initialValues) != -1) {
               	res = 1;
           	}
       	}
		DatabaseAdapter.USE_DB.release();
		return res;
    }

    /**
     * Mark a specific post as read.
     * @param itemid of the post to mark read
     * @return number of rows affected
     */
    public final long markPostRead(final Integer itemid) {
    	Log.d(TAG, "Marking post as read: " + itemid);
    	ContentValues initialValues = new ContentValues();
    	initialValues.put(KEY_ITEMID, itemid);
    	initialValues.put(KEY_POST_READ, 1);
    	try {
			DatabaseAdapter.USE_DB.acquire();
		} catch (InterruptedException e1) {
			return 0;
		}
       	long res = mDb.update(DATABASE_TABLE_POST, initialValues, KEY_ITEMID + "=" + itemid, null);
		DatabaseAdapter.USE_DB.release();
		Log.d(TAG, "markPostRead rows affected: " + res);
		return res;
    }

    /**
     * Returns if a post is read.
     * @param itemid of the post
     * @return if the post is read
     */
	public final boolean isPostRead(final Integer itemid) {
		Log.d(TAG, "isPostRead");
		Cursor c = mDb.query(DATABASE_TABLE_POST, new String[] {KEY_POST_READ}, KEY_ITEMID + "=" + itemid, null, null, null, null);
		c.moveToFirst();
		if (c.isAfterLast()) {
			Log.d(TAG, "isPostRead: no hits");
			c.close();
			return false;
		}
		Integer isread = c.getInt(c.getColumnIndex(KEY_POST_READ));
		Log.d(TAG, "isPostRead: " + isread);
		c.close();
		return isread != 0;
	}
}
