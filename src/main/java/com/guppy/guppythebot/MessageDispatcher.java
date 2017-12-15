package com.guppy.guppythebot;

import java.util.function.Consumer;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public interface MessageDispatcher
{
	void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);
	
	void sendMessage(String message);
	
	void sendMessage(String msgContent, MessageChannel tChannel);
	
	void sendEmbed(String title, String description, MessageChannel tChannel);
	
	void sendPrivateMessageToUser(String content, User user);
	
	public final class FixedDispatcher implements MessageDispatcher
	{
		private final TextChannel channel;
		
		public FixedDispatcher(TextChannel channel)
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
}
