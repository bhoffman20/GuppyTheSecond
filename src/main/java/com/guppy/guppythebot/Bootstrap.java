package com.guppy.guppythebot;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

public class Bootstrap
{
	public static final String CMD_PREFIX = "/";
	
	public static void main(String[] args) throws Exception
	{
		// MzI5MDQ5MDAxNDYwNjI5NTA1.DDMyTw.dOHH8qk3yD6XTvZPqLfg2kzjtrA
		new JDABuilder(AccountType.BOT).setToken("Mzc0NTcyOTg3NTE0MzU1NzEz.DNjPzQ.RYh_vzUYqOSZY0P_ddSgWvR0Hpg").setAudioSendFactory(new NativeAudioSendFactory())
				.addEventListener(new BotApplicationManager()).buildAsync();
	}
}
