package com.guppy.guppythebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap
{
	public static void main(String[] args) throws Exception
	{
		new JDABuilder(AccountType.BOT).setToken("Mzc0NTcyOTg3NTE0MzU1NzEz.DNjPzQ.RYh_vzUYqOSZY0P_ddSgWvR0Hpg")
				.setAudioSendFactory(new NativeAudioSendFactory()).addEventListener(new BotApplicationManager())
				.buildAsync();
	}
}
