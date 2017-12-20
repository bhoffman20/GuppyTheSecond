package com.guppy.guppythebot.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.guppy.guppythebot.Bootstrap;
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
		Message m = new MessageBuilder().append("pong").build();
		
		message.getTextChannel().sendMessage(m).queue();
	}
	
	@BotCommandHandler
	private void ching(Message message)
	{
		outputChannel.set(message.getTextChannel());
		messageDispatcher.sendMessage("chong", message.getTextChannel());
	}
	
	@BotCommandHandler
	private void report(Message message, String userAsMention)
	{
		outputChannel.set(message.getTextChannel());
		
		File dataFolder = new File("data");
		dataFolder.mkdirs();
		
		File propFile = new File(dataFolder, "ReportedUsers.properties");
		Properties prop = new Properties();
		
		try
		{
			propFile.createNewFile();
			FileReader reader = new FileReader(propFile.getPath());
			prop.load(reader);
		}
		catch (FileNotFoundException fnf)
		{
			System.out.println("Could not find UserReports.properties.");
		}
		catch (IOException e)
		{
			System.out.println("Could not load the UserReports.properties file.");
		}
		
		if (!message.getMentionedUsers().isEmpty())
		{
			for (User u : message.getMentionedUsers())
			{
				String originalProp = prop.getProperty(u.getId());
				int newVal = 1;
				
				if (null != originalProp && !originalProp.isEmpty())
				{
					try
					{
						int originalValue = Integer.parseInt(originalProp);
						newVal = originalValue + 1;
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
				messageDispatcher.sendMessage(u.getAsMention() + " has now been reported " + newVal + " times.");
				prop.setProperty(u.getId(), "" + newVal);
			}
		}
		else
		{
			messageDispatcher.sendMessage("You can't report no one. You have been reported.");
			
			String originalProp = prop.getProperty(message.getAuthor().getId());
			int newVal = 0;
			
			if (null != originalProp && !originalProp.isEmpty())
			{
				try
				{
					int originalValue = Integer.parseInt(originalProp);
					newVal = originalValue + 1;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			prop.setProperty(message.getAuthor().getId(), "" + newVal);
		}
		
		FileOutputStream fos = null;
		
		try
		{
			fos = new FileOutputStream(propFile);
			prop.store(fos, "");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				fos.close();
			}
			catch (IOException e)
			{
			}
		}
	}
	
	@BotCommandHandler
	private void rep(Message message)
	{
		outputChannel.set(message.getTextChannel());
		
		System.out.println("Building report map...");
		
		Properties prop = new Properties();
		FileReader reader;
		try
		{
			File f = new File("data", "ReportedUsers.properties");
			reader = new FileReader(f);
			prop.load(reader);
			
			TreeMap<String, Integer> map = new TreeMap<String, Integer>();
			
			Iterator it = prop.stringPropertyNames().iterator();
			while (it.hasNext())
			{
				String id = (String) it.next();
				String user = message.getJDA().getUserById(Long.parseLong(id)).getName();
				
				String value = prop.getProperty(id);
				Integer intVal = new Integer(value);
				
				map.put(user, intVal);
			}
			
			System.out.println(map.toString());
			
			// Sort the map
			// Comparator<String> comparator = new ValueComparator<String, Integer>(map);
			// TreeMap<String, Integer> result = new TreeMap<String, Integer>(comparator);
			// result.putAll(map);
			
			Map<String, Integer> result = sortByValue(map);
			// System.out.println(result.toString());
			
			MessageBuilder mb = new MessageBuilder();
			mb.append("```");
			
			Iterator iter = result.entrySet().iterator();
			while (iter.hasNext())
			{
				Entry<String, Integer> ent = (Entry<String, Integer>) iter.next();
				
				String mes = ent.getKey() + " has been reported " + ent.getValue() + " times!";
				mb.append(mes);
				mb.append("\r\n");
			}
			
			mb.append("```");
			message.getTextChannel().sendMessage(mb.build()).queue();
		}
		catch (FileNotFoundException e)
		{
			messageDispatcher.sendMessage("No one has been reported yet! You'll be the first!");
			report(message, "");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map)
	{
		List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>()
		{
			@Override
			public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2)
			{
				return -(e1.getValue()).compareTo(e2.getValue());
			}
		});
		Map<K, V> result = new LinkedHashMap<>();
		for (Map.Entry<K, V> entry : list)
		{
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	
	@BotCommandHandler
	private void help(Message message)
	{
		outputChannel.set(message.getTextChannel());
		messageDispatcher.sendMessage(
				"```Command Prefix: **" + Bootstrap.CMD_PREFIX + "** Commands:\r\n" + "play, playNow, playNext <YouTube link | YouTube search> . Adds a song to the queue\r\n"
						+ "pause, resume . . . . . . . . . . . . . . . . . . . . . . Pause or resume the playing track\r\n"
						+ "skip . . . .  . . . . . . . . . . . . . . . . . . . . . . Skips the currently playing track\r\n"
						+ "shuffle . . . . . . . . . . . . . . . . . . . . . . . . . Shuffle the queue"
						+ "forward, back <Seconds> . . . . . . . . . . . . . . . . . Move a specified number of seconds forward or backward in the song\r\n"
						+ "seek <Seconds>. . . . . . . . . . . . . . . . . . . . . . Seek to a specified number of seconds into the song\r\n"
						+ "volume <1-100>. . . . . . . . . . . . . . . . . . . . . . Change the volume of the bot, value in percentage\r\n"
						+ "queue, q. . . . . . . . . . . . . . . . . . . . . . . . . Displays the queue\r\n"
						+ "report <@User>. . . . . . . . . . . . . . . . . . . . . . Report a user for being a bitch\r\n" + "```");
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
