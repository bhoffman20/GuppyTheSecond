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
		messageDispatcher.sendMessage("pong", message.getTextChannel());
	}
	
	@BotCommandHandler
	private void ching(Message message)
	{
		messageDispatcher.sendMessage("chong", message.getTextChannel());
	}
	
	@BotCommandHandler
	private void help(Message message)
	{
		messageDispatcher.sendMessage("Commands:\n", message.getTextChannel());
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
	
	private final class FixedDispatcher implements MessageDispatcher
	{
		private final TextChannel channel;
		
		private FixedDispatcher(TextChannel channel)
		{
			this.channel = channel;
		}
		
		@Override
		public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure)
		{
			channel.sendMessage(message).queue(success, failure);
		}
		
		@Override
		public void sendMessage(String message)
		{
			channel.sendMessage(message).queue();
		}
		
		@Override
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
		
		@Override
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
