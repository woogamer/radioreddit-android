package net.mandaria.radioreddit.data;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mandaria.radioreddit.R;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper
{
	 
    //The Android's default system path of your application database.
	private final Context myContext;
    private static String DB_PATH = "";//"/data/data/net.mandaria.radioredditfree/databases/";
    private static String DB_NAME = "radioreddit.db";
    private static int DB_VERSION = 2;
   
 
    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DatabaseHelper(Context context) 
    {
    	super(context, DB_NAME, null, DB_VERSION);
        this.myContext = context;
        this.DB_PATH = context.getString(R.string.database);
    }	
 
    @Override
	public synchronized void close() 
    {
    	super.close();
	}
 
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		String createStreamsCacheTable = 
			"CREATE TABLE [StreamsCache] (" +
			"[_id] INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT," +
			"[Name] TEXT  NOT NULL," +
			"[Type] TEXT  NOT NULL," +
			"[Description] TEXT  NOT NULL," +
			"[Status] TEXT  NOT NULL," +
			"[Relay] TEXT  NOT NULL," +
			"[Online] BOOLEAN  NOT NULL" +
			")";
		
		String createRecentlyPlayedTable = 
			"CREATE TABLE [RecentlyPlayed] (" +
			"[_id] INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT," +
			"[ListenDate] TEXT  NOT NULL," +
			"[Type] TEXT  NOT NULL," +
			"[id] TEXT  NULL," + // radio reddit id
			"[Title] TEXT  NULL," +
			"[Artist] TEXT  NULL," +
			"[Redditor] TEXT  NULL," +
			"[Genre] TEXT  NULL," +
			"[Reddit_title] TEXT  NULL," +
			"[Reddit_url] TEXT  NULL," +
			"[Preview_url] TEXT  NULL," +
			"[Download_url] TEXT  NULL," +
			"[Bandcamp_link] TEXT  NULL," +
			"[Bandcamp_art] TEXT  NULL," +
			"[Itunes_link] TEXT  NULL," +
			"[Itunes_art] TEXT  NULL," +
			"[Itunes_price] TEXT  NULL," +
			"[Name] TEXT  NULL," +
			"[EpisodeTitle] TEXT  NULL," +
			"[EpisodeDescription] TEXT  NULL," +
			"[EpisodeKeywords] TEXT  NULL," +
			"[ShowTitle] TEXT  NULL," +
			"[ShowHosts] TEXT  NULL," +
			"[ShowRedditors] TEXT  NULL," +
			"[ShowGenre] TEXT  NULL," +
			"[ShowFeed] TEXT  NULL" +
			")";
		
		try
		{
			db.execSQL(createStreamsCacheTable);
			db.execSQL(createRecentlyPlayedTable);
		}
		catch(Exception ex)
		{
			throw new Error("Error creating database", ex);
		}
	}
 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		if(oldVersion == 1) // adding RecentlyPlayed database
		{
			db.execSQL("DROP TABLE IF EXISTS StreamsCache");
			onCreate(db);
		}
		
	}
}
