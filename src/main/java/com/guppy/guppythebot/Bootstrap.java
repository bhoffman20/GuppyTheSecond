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
	public static String SPOTIFY_SECRET;
	public static String AUTHOR_ID;
	
	public Bootstrap()
	{
		
	}
	
	public static void main(String[] args) throws Exception
	{
		LOG.info("Starting GuppyTheBot");
		
		FileReader reader = new FileReader("bot.properties");
		Properties p = new Properties();
		p.load(reader);
		
		SPOTIFY_SECRET = p.getProperty("spotify.secret");
		AUTHOR_ID = p.getProperty("authorId");
		
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
		return SPOTIFY_SECRET;
	}
	
}

