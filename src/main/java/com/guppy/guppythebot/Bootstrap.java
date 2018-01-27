package com.guppy.guppythebot;

import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap
{
	public static final List<String> CMD_PREFIX = Arrays.asList(".");
	private static final Logger LOG = LogManager.getLogger(Bootstrap.class);
	public static String spotifyKey;
	
	public Bootstrap()
	{
		
	}
	
	public static void main(String[] args) throws Exception
	{
		LOG.info("Starting GuppyTheBot v2");
		
		FileReader reader = new FileReader("bot.properties");
		Properties p = new Properties();
		p.load(reader);
		
		LOG.info("Initializing Spotify API");
		
		spotifyKey = p.getProperty("spotify.secret");
		
		
		// refreshToken();
		
		// LOG.info("API Ready");
		// PlaylistRequest request = spotifyApi.getPlaylist("pohnnie",
		// "3cEBSPmP0mi4kLbOXh3fYe?si=QeIcvIuOR4KJI5hXFcH4bw").build();
		// request.get().getTracks().getItems().forEach(t ->
		// LOG.info(t.getTrack().getName() + ", " +
		// t.getTrack().getArtists().get(0).getName()));
		
		String identifier = "https://open.spotify.com/user/pohnnie/playlist/3cEBSPmP0mi4kLbOXh3fYe?si=QeIcvIuOR4KJI5hXFcH4bw";
		
		LOG.info("Loading Bot Key");
		String key = p.getProperty("key");
		if (null == key)
		{
			LOG.error("No bot API Key found!");
		}
		else
		{
			new JDABuilder(AccountType.BOT).setToken(key).setAudioSendFactory(new NativeAudioSendFactory()).addEventListener(new BotApplicationManager()).buildAsync();
		}
	}
	
	public static String getSpotifyKey()
	{
		return spotifyKey;
	}
	
}

