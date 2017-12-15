package com.guppy.guppythebot;

import java.io.FileReader;
import java.util.Properties;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap
{
	public static final String CMD_PREFIX = "/";
	
	public static void main(String[] args) throws Exception
	{
		FileReader reader = new FileReader("bot.properties");
		Properties p = new Properties();
		p.load(reader);
		
		String key = p.getProperty("key");
		
		if (null == key)
		{
			System.out.println("No api key found");
		}
		else
		{
			new JDABuilder(AccountType.BOT).setToken(key).setAudioSendFactory(new NativeAudioSendFactory()).addEventListener(new BotApplicationManager()).buildAsync();
		}
	}
}

