package com.guppy.guppythebot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.guppy.guppythebot.util.DatabaseUtil;
import com.guppy.guppythebot.util.PropertyUtil;
import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap
{
	private static final Logger LOG = LogManager.getLogger(Bootstrap.class);
	private static JDA jda;
	public static DatabaseUtil dbUtil = new DatabaseUtil();
	
	public static void main(String[] args) throws Exception {
		LOG.info("Starting GuppyTheBot");
		
		LOG.info("Loading Bot Key");
		String key = PropertyUtil.getProperty(GuppyConstants.BOT_KEY);
		if (null == key) {
			LOG.error("No bot API Key found!");
		}
		else {
			jda = new JDABuilder(AccountType.BOT).setToken(key).setAudioSendFactory(new NativeAudioSendFactory()).addEventListener(new BotApplicationManager()).buildAsync();
		}
	}
	
	public static DatabaseUtil getDbUtil() {
		return dbUtil;
	}
	
	
}

