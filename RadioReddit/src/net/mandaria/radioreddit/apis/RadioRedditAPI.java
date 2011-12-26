/*
 *	radio reddit for android: mobile app to listen to radioreddit.com
 *  Copyright (C) 2011 Bryan Denny
 *  
 *  This file is part of "radio reddit for android"
 *
 *  "radio reddit for android" is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  "radio reddit for android" is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with "radio reddit for android".  If not, see <http://www.gnu.org/licenses/>.
 */

package net.mandaria.radioreddit.apis;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import net.mandaria.radioreddit.R;
import net.mandaria.radioreddit.RadioRedditApplication;
import net.mandaria.radioreddit.activities.Settings;
import net.mandaria.radioreddit.errors.CustomExceptionHandler;
import net.mandaria.radioreddit.objects.RadioEpisode;
import net.mandaria.radioreddit.objects.RadioSong;
import net.mandaria.radioreddit.objects.RadioStream;
import net.mandaria.radioreddit.objects.RadioStreams;
import net.mandaria.radioreddit.objects.RedditAccount;
import net.mandaria.radioreddit.tasks.GetCurrentEpisodeInformationTask;
import net.mandaria.radioreddit.tasks.GetCurrentSongInformationTask;
import net.mandaria.radioreddit.utils.HTTPUtil;
import android.content.Context;
import android.util.Log;

public class RadioRedditAPI
{
	public static RadioStreams GetStreams(Context context, RadioRedditApplication application)
	{
		RadioStreams radiostreams = new RadioStreams();
		radiostreams.ErrorMessage = "";

		try
		{			
			String url = context.getString(R.string.radio_reddit_streams);
			String outputStreams = "";
			boolean errorGettingStreams = false;

			try
			{
				outputStreams = HTTPUtil.get(context, url);
			}
			catch(Exception ex)
			{
				errorGettingStreams = true;
				radiostreams.ErrorMessage = context.getString(R.string.error_RadioRedditServerIsDownNotification);
			}

			if(!errorGettingStreams && outputStreams.length() > 0)
			{
				JSONTokener tokener = new JSONTokener(outputStreams);
				JSONObject json = new JSONObject(tokener);

				JSONObject streams = json.getJSONObject("streams");
				JSONArray streams_names = streams.names();
				ArrayList<RadioStream> list_radiostreams = new ArrayList<RadioStream>();

				// loop through each stream
				for(int i = 0; i < streams.length(); i++)
				{
					String name = streams_names.getString(i);
					JSONObject stream = streams.getJSONObject(name);

					RadioStream radiostream = new RadioStream();
					radiostream.Name = name;
					// if(stream.has("type"))
					radiostream.Type = stream.getString("type");
					radiostream.Description = stream.getString("description");
					radiostream.Status = stream.getString("status");

					// call status.json to get Relay
					// form url radioreddit.com + status + json
					String status_url = context.getString(R.string.radio_reddit_base_url) + radiostream.Status + context.getString(R.string.radio_reddit_status);

					String outputStatus = "";
					boolean errorGettingStatus = false;

					try
					{
						outputStatus = HTTPUtil.get(context, status_url);
					}
					catch(Exception ex)
					{
						errorGettingStatus = true;
						radiostreams.ErrorMessage = context.getString(R.string.error_RadioRedditServerIsDownNotification);
					}

					//Log.e("RadioReddit", "Length of output: "+ outputStatus.length() + "; Content of output: " + outputStatus);
					// TODO: does  outputStatus.length() > 0 need to be checked here and return a ErrorMessage back and set ErrorGettingStatus = true? 
					
					if(!errorGettingStatus && outputStatus.length() > 0)
					{
						JSONTokener status_tokener = new JSONTokener(outputStatus);
						JSONObject status_json = new JSONObject(status_tokener);

						radiostream.Online = Boolean.parseBoolean(status_json.getString("online").toLowerCase());

						if(radiostream.Online == true) // if offline, no other nodes are available
						{
							radiostream.Relay = status_json.getString("relay");

							list_radiostreams.add(radiostream);
						}
					}
				}

				// JSON parsing reverses the list for some reason, fixing it...
				if(list_radiostreams.size() > 0)
				{
					// Sorting will happen later on select station activity
					//Collections.reverse(list_radiostreams);

					radiostreams.RadioStreams = list_radiostreams;
				}
				else
				{
					radiostreams.ErrorMessage = context.getString(R.string.error_NoStreams);
				}
			}
		}
		catch(Exception ex)
		{
			// We fail to get the streams...
			CustomExceptionHandler ceh = new CustomExceptionHandler(context);
			ceh.sendEmail(ex);

			radiostreams.ErrorMessage = ex.toString();
			ex.printStackTrace();
		}

		return radiostreams;
	}

	public static RadioSong GetCurrentSongInformation(Context context, RadioRedditApplication application)
	{
		RadioSong radiosong = new RadioSong();
		radiosong.ErrorMessage = "";

		try
		{
			String status_url = context.getString(R.string.radio_reddit_base_url) + application.CurrentStream.Status + context.getString(R.string.radio_reddit_status);

			String outputStatus = "";
			boolean errorGettingStatus = false;

			try
			{
				outputStatus = HTTPUtil.get(context, status_url);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				errorGettingStatus = true;
				// For now, not used. It is acceptable to error out and not alert the user
				// radiosong.ErrorMessage = context.getString(R.string.error_RadioRedditServerIsDownNotification);
			}

			if(!errorGettingStatus && outputStatus.length() > 0)
			{
				JSONTokener status_tokener = new JSONTokener(outputStatus);
				JSONObject status_json = new JSONObject(status_tokener);

				radiosong.Playlist = status_json.getString("playlist");

				JSONObject songs = status_json.getJSONObject("songs");
				JSONArray songs_array = songs.getJSONArray("song");

				// get the first song in the array
				JSONObject song = songs_array.getJSONObject(0);
				radiosong.ID = song.getInt("id");
				radiosong.Title = song.getString("title");
				radiosong.Artist = song.getString("artist");
				radiosong.Redditor = song.getString("redditor");
				radiosong.Genre = song.getString("genre");
				radiosong.Reddit_title = song.getString("reddit_title");
				radiosong.Reddit_url = song.getString("reddit_url");
				if(song.has("preview_url"))
					radiosong.Preview_url = song.getString("preview_url");
				if(song.has("download_url"))
					radiosong.Download_url = song.getString("download_url");
				if(song.has("bandcamp_link"))
					radiosong.Bandcamp_link = song.getString("bandcamp_link");
				if(song.has("bandcamp_art"))
					radiosong.Bandcamp_art = song.getString("bandcamp_art");
				if(song.has("itunes_link"))
					radiosong.Itunes_link = song.getString("itunes_link");
				if(song.has("itunes_art"))
					radiosong.Itunes_art = song.getString("itunes_art");
				if(song.has("itunes_price"))
					radiosong.Itunes_price = song.getString("itunes_price");

				// get vote score 
				String reddit_info_url = context.getString(R.string.reddit_link_by) + URLEncoder.encode(radiosong.Reddit_url);

				String outputRedditInfo = "";
				boolean errorGettingRedditInfo = false;

				try
				{
					outputRedditInfo = HTTPUtil.get(context, reddit_info_url);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					errorGettingRedditInfo = true;
					// For now, not used. It is acceptable to error out and not alert the user
					// radiosong.ErrorMessage = "Unable to connect to reddit";//context.getString(R.string.error_RadioRedditServerIsDownNotification);
				}

				if(!errorGettingRedditInfo && outputRedditInfo.length() > 0)
				{
					// Log.e("radio_reddit_test", "Length: " + outputRedditInfo.length());
					// Log.e("radio_reddit_test", "Value: " + outputRedditInfo); // TODO: sometimes the value contains "error: 404", need to check for that. (We can probably safely ignore this for now)
					JSONTokener reddit_info_tokener = new JSONTokener(outputRedditInfo);
					JSONObject reddit_info_json = new JSONObject(reddit_info_tokener);

					JSONObject data = reddit_info_json.getJSONObject("data");

					// default value of score
					String score = context.getString(R.string.vote_to_submit_song);
					String likes = "null";
					String name = "";

					JSONArray children_array = data.getJSONArray("children");

					// Song hasn't been submitted yet
					if(children_array.length() > 0)
					{
						JSONObject children = children_array.getJSONObject(0);

						JSONObject children_data = children.getJSONObject("data");
						score = children_data.getString("score");
						
						likes = children_data.getString("likes");
						name = children_data.getString("name");
					}

					radiosong.Score = score;
					radiosong.Likes = likes;
					radiosong.Name = name;
				}
				else
				{
					radiosong.Score = "?";
					radiosong.Likes = "null";
					radiosong.Name = "";
				}

				return radiosong;
			}
			return null;
		}
		catch(Exception ex)
		{
			// We fail to get the current song information...
			CustomExceptionHandler ceh = new CustomExceptionHandler(context);
			ceh.sendEmail(ex);

			ex.printStackTrace();
			radiosong.ErrorMessage = ex.toString();
			return radiosong;
		}
	}

	public static RadioEpisode GetCurrentEpisodeInformation(Context context, RadioRedditApplication application)
	{
		RadioEpisode radioepisode = new RadioEpisode();
		radioepisode.ErrorMessage = "";

		try
		{
			String status_url = context.getString(R.string.radio_reddit_base_url) + application.CurrentStream.Status + context.getString(R.string.radio_reddit_status);

			String outputStatus = "";
			boolean errorGettingStatus = false;

			try
			{
				outputStatus = HTTPUtil.get(context, status_url);
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
				errorGettingStatus = true;
				// For now, not used. It is acceptable to error out and not alert the user
				// radiosong.ErrorMessage = context.getString(R.string.error_RadioRedditServerIsDownNotification);
			}

			if(!errorGettingStatus && outputStatus.length() > 0)
			{
				JSONTokener status_tokener = new JSONTokener(outputStatus);
				JSONObject status_json = new JSONObject(status_tokener);

				radioepisode.Playlist = status_json.getString("playlist");

				JSONObject episodes = status_json.getJSONObject("episodes");
				JSONArray episodes_array = episodes.getJSONArray("episode");

				// get the first episode in the array
				JSONObject song = episodes_array.getJSONObject(0);
				radioepisode.ID = song.getInt("id");
				radioepisode.EpisodeTitle = song.getString("episode_title");
				radioepisode.EpisodeDescription = song.getString("episode_description");
				radioepisode.EpisodeKeywords = song.getString("episode_keywords");
				radioepisode.ShowTitle = song.getString("show_title");
				radioepisode.ShowHosts = song.getString("show_hosts").replaceAll(",", ", ");
				radioepisode.ShowRedditors = song.getString("show_redditors").replaceAll(",", ", ");
				radioepisode.ShowGenre = song.getString("show_genre");
				radioepisode.ShowFeed = song.getString("show_feed");
				radioepisode.Reddit_title = song.getString("reddit_title");
				radioepisode.Reddit_url = song.getString("reddit_url");
				if(song.has("preview_url"))
					radioepisode.Preview_url = song.getString("preview_url");
				if(song.has("download_url"))
					radioepisode.Download_url = song.getString("download_url");

				// get vote score
				String reddit_info_url = context.getString(R.string.reddit_link_by) + URLEncoder.encode(radioepisode.Reddit_url);

				String outputRedditInfo = "";
				boolean errorGettingRedditInfo = false;

				try
				{
					outputRedditInfo = HTTPUtil.get(context, reddit_info_url);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
					errorGettingRedditInfo = true;
					// For now, not used. It is acceptable to error out and not alert the user
					// radiosong.ErrorMessage = "Unable to connect to reddit";//context.getString(R.string.error_RadioRedditServerIsDownNotification);
				}

				if(!errorGettingRedditInfo && outputRedditInfo.length() > 0)
				{
					// Log.e("radio_reddit_test", "Length: " + outputRedditInfo.length());
					// Log.e("radio_reddit_test", "Value: " + outputRedditInfo); // TODO: sometimes the value contains "error: 404", need to check for that (We can probably safely ignore this for now)
					JSONTokener reddit_info_tokener = new JSONTokener(outputRedditInfo);
					JSONObject reddit_info_json = new JSONObject(reddit_info_tokener);

					JSONObject data = reddit_info_json.getJSONObject("data");

					// default value of score
					String score = context.getString(R.string.vote_to_submit_song);
					String likes = "null";
					String name = "";

					JSONArray children_array = data.getJSONArray("children");

					// Episode hasn't been submitted yet
					if(children_array.length() > 0)
					{
						JSONObject children = children_array.getJSONObject(0);

						JSONObject children_data = children.getJSONObject("data");
						score = children_data.getString("score");
						
						likes = children_data.getString("likes");
						name = children_data.getString("name");
					}

					radioepisode.Score = score;
					radioepisode.Likes = likes;
					radioepisode.Name = name;
				}
				else
				{
					radioepisode.Score = "?";
					radioepisode.Likes = "null";
					radioepisode.Name = "";
				}

				return radioepisode;
			}
			return null;
		}
		catch(Exception ex)
		{
			// We fail to get the current song information...
			CustomExceptionHandler ceh = new CustomExceptionHandler(context);
			ceh.sendEmail(ex);

			ex.printStackTrace();
			radioepisode.ErrorMessage = ex.toString();
			return radioepisode;
		}

	}
	
	public static String VoteOnCurrentlyPlaying(Context context, RadioRedditApplication application, boolean liked)
	{
		String errorMessage = "";
		RedditAccount account = Settings.getRedditAccount(context);
		
		if(account == null)
		{
			errorMessage = context.getString(R.string.error_YouMustBeLoggedInToVote);
			return errorMessage;
		}
		
		int voteDirection = 0; // TODO: handle case to rescind vote
		if(liked == true)
			voteDirection = 1;
		else
			voteDirection = -1;
		
		RadioSong song = null;
		RadioEpisode episode = null;
		
//		1. Get most up to date song information (in case cached info is old)
		if(application.CurrentStream.Type.equals("music"))
		{
			song = RadioRedditAPI.GetCurrentSongInformation(context, application);
			
			if(song == null)
				return context.getString(R.string.error_ThereWasAProblemVotingPleaseTryAgain);
			
			if(!song.ErrorMessage.equals(""))
				return song.ErrorMessage;
		}
		else if(application.CurrentStream.Type.equals("talk"))
		{
			episode = RadioRedditAPI.GetCurrentEpisodeInformation(context, application);
			
			if(episode == null)
				return context.getString(R.string.error_ThereWasAProblemVotingPleaseTryAgain);
			
			if(!episode.ErrorMessage.equals(""))
				return episode.ErrorMessage;
		}

		
//		2a. If it exist:
//		a. Get the FULLNAME from reddit and vote on it: http://www.reddit.com/api/vote
		
//		2b. If it exists, but is archived
//		a. Submit as a new post to be voted on? Or simply say that the song has been archived and cannot be voted on?
//		b. BUG: apparently the API allows votes on archived posts. This needs to be discussed with reddit admins or similar
	
	// TODO: return to user that it must be submitted?  e.g. they must accept to submit, so pull the submit into its own function?
//	3. If it doesn't exist:
//		a. Try to submit the post http://www.reddit.com/api/submit:
//		b. If it fails, display error (or CAPTCHA) and try again
		
		//String title = "Song Title by Song Artist (redditor)";
		
		// TODO: I don't really like the if else going on here due to currentsong vs currentepisode
		if(application.CurrentStream.Type.equals("music"))
		{
			if(song.Name != "")
			{
				errorMessage = RedditAPI.Vote(context, account, voteDirection, song.Name);
			}
			else // not yet submitted
			{	
				String title = song.Title + " by " + song.Artist + " (" + song.Redditor + ")";
				String url = song.Reddit_url;
				String subreddit = "radioreddit";
				
				errorMessage = RedditAPI.SubmitLink(context, account, title, url, subreddit);
				//return context.getString(R.string.error_ThereWasAProblemPleaseTryAgain);
			}
		}
		else if(application.CurrentStream.Type.equals("talk"))
		{
			if(episode.Name != "")
			{
				errorMessage = RedditAPI.Vote(context, account, voteDirection, episode.Name);
			}
			else // not yet submitted
			{
				String title = episode.ShowTitle + ": " + episode.EpisodeTitle;
				String url = episode.Reddit_url;
				String subreddit = "talkradioreddit";
				
				errorMessage = RedditAPI.SubmitLink(context, account, title, url, subreddit);
				//return context.getString(R.string.error_ThereWasAProblemVotingPleaseTryAgain);
			}
		}
		

		return errorMessage;
	}
	
}
