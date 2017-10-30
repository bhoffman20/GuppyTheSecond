package com.guppy.guppythebot;

import java.util.function.Consumer;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

public interface MessageDispatcher
{
	void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure);

	void sendMessage(String message);

	void sendMessage(String msgContent, MessageChannel tChannel);

	void sendEmbed(String title, String description, MessageChannel tChannel);

	void sendPrivateMessageToUser(String content, User user);

}
