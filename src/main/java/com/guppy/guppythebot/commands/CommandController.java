package com.guppy.guppythebot.commands;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.guppy.guppythebot.BotApplicationManager;
import com.guppy.guppythebot.BotGuildContext;
import com.guppy.guppythebot.MessageDispatcher;
import com.guppy.guppythebot.controller.BotCommandHandler;
import com.guppy.guppythebot.controller.BotController;
import com.guppy.guppythebot.controller.BotControllerFactory;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class CommandController implements BotController
{
	private final MessageDispatcher messageDispatcher;
	private final Guild guild;
	private final AtomicReference<TextChannel> outputChannel;
	
	public CommandController(BotApplicationManager manager, BotGuildContext state, Guild guild)
	{
		this.guild = guild;
		outputChannel = new AtomicReference<>();
		messageDispatcher = new GlobalDispatcher();
	}
	
	@BotCommandHandler
	private void ping(Message message)
	{
		outputChannel.set(message.getTextChannel());
		Message m = new MessageBuilder().setTTS(true).append("pong").build();
		
		message.getTextChannel().sendMessage(m).queue();
	}
	
	@BotCommandHandler
	private void ching(Message message)
	{
		outputChannel.set(message.getTextChannel());
		messageDispatcher.sendMessage("chong", message.getTextChannel());
	}
	
	@BotCommandHandler
	private void help(Message message)
	{
		outputChannel.set(message.getTextChannel());
		messageDispatcher.sendMessage("```Commands:\r\n" + "play, playNow, playNext <YouTube link | YouTube search> . Adds a song to the queue\r\n"
				+ "pause, resume . . . . . . . . . . . . . . . . . . . . . . Pause or resume the playing track\r\n"
				+ "skip . . . .  . . . . . . . . . . . . . . . . . . . . . . Skips the currently playing track\r\n"
				+ "shuffle . . . . . . . . . . . . . . . . . . . . . . . . . Shuffle the queue"
				+ "forward, back <Seconds> . . . . . . . . . . . . . . . . . Move a specified number of seconds forward or backward in the song\r\n"
				+ "seek <Seconds>. . . . . . . . . . . . . . . . . . . . . . Seek to a specified number of seconds into the song\r\n"
				+ "volume <1-100>. . . . . . . . . . . . . . . . . . . . . . Change the volume of the bot, value in percentage\r\n"
				+ "queue, q. . . . . . . . . . . . . . . . . . . . . . . . . Displays the queue\r\n" + "```");
	}
	
	private class GlobalDispatcher implements MessageDispatcher
	{
		@Override
		public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure)
		{
			TextChannel channel = outputChannel.get();
			
			if (channel != null)
			{
				channel.sendMessage(message).queue(success, failure);
			}
		}
		
		@Override
		public void sendMessage(String message)
		{
			TextChannel channel = outputChannel.get();
			
			if (channel != null)
			{
				channel.sendMessage(message).queue();
			}
		}
		
		public void sendMessage(String msgContent, MessageChannel tChannel)
		{
			if (tChannel == null) return;
			tChannel.sendMessage(msgContent).queue();
		}
		
		@Override
		public void sendEmbed(String title, String description, MessageChannel tChannel)
		{
			MessageEmbed embed = new EmbedBuilder().setTitle(title, null).setDescription(description).build();
			
			sendMessage(new MessageBuilder().setEmbed(embed).build().getContent(), tChannel);
		}
		
		public void sendPrivateMessageToUser(String content, User user)
		{
			user.openPrivateChannel().queue(c -> sendMessage(content, c));
		}
	}
	
	public static class Factory implements BotControllerFactory<CommandController>
	{
		@Override
		public Class<CommandController> getControllerClass()
		{
			return CommandController.class;
		}
		
		@Override
		public CommandController create(BotApplicationManager manager, BotGuildContext state, Guild guild)
		{
			return new CommandController(manager, state, guild);
		}
	}
	
}
