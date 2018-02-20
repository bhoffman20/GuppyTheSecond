package com.guppy.guppythebot.commands;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;

import com.guppy.guppythebot.Bootstrap;
import com.guppy.guppythebot.BotApplicationManager;
import com.guppy.guppythebot.BotGuildContext;
import com.guppy.guppythebot.controller.BotCommandHandler;
import com.guppy.guppythebot.controller.BotController;
import com.guppy.guppythebot.controller.BotControllerFactory;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

public class CommandController implements BotController
{
	private final Guild guild;
	
	public CommandController(BotApplicationManager manager, BotGuildContext state, Guild guild)
	{
		this.guild = guild;
	}
	
	@BotCommandHandler
	private void ping(Message message) {
		Message m = new MessageBuilder().append("pong").build();
		message.getTextChannel().sendMessage(m).queue();
	}
	
	@BotCommandHandler
	private void ching(Message message) {
		message.getTextChannel().sendMessage("chong");
	}
	
	@BotCommandHandler
	private void say(Message message, String messageText) {
		Message me = message;
		message.delete().queue();
		
		if (Bootstrap.getDbUtil().isAdmin(me.getAuthor().getIdLong())) {
			MessageBuilder mb = new MessageBuilder();
			mb.append(messageText);
			
			if (me.getMentionedUsers() != null) {
				me.getMentionedUsers().forEach(u -> mb.replaceAll("@" + u.getName(), u.getAsMention()));
			}
			
			me.getChannel().sendMessage(mb.build()).queue();
		}
	}
	
	@BotCommandHandler
	private void timer(Message message, int timerMinutes) {
		BotTimer t = new BotTimer(message, timerMinutes * 60 * 1000L);
		Timer timer = new Timer(true);
		timer.schedule(t, 0);
	}
	
	@BotCommandHandler
	private void version(Message message) {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("version.txt");) {
			message.getChannel().sendMessage(IOUtils.toString(in)).queue();
		}
		catch (IOException e) {
			message.getChannel().sendMessage("Failed to read version String.").queue();
		}
	}
	
	@BotCommandHandler
	private void report(Message message, String userAsMention) {
		// message.delete().queue();
		
		if (!message.getMentionedUsers().isEmpty()) {
			for (User u : message.getMentionedUsers()) {
				int originalProp = Bootstrap.getDbUtil().getUserReputaionById(u.getIdLong());
				int newVal = originalProp - 1;
				
				Bootstrap.getDbUtil().userMinusRep(u.getIdLong());
				
				String preText = "";
				if (message.getContent().contains(" for ")) {
					String[] reason = message.getContent().split("for", 2);
					
					if (reason[1].trim() != "") {
						String reasonPhrase = reason[1].trim();
						preText = "This is a Christian Discord server, and `" + reasonPhrase + "` will not be tolerated. ";
					}
				}
				
				message.getTextChannel().sendMessage(preText + u.getAsMention() + "'s reputation is now " + newVal + "\n").queue();
			}
		}
		else {
			message.getTextChannel().sendMessage("You can't report no one. Your reputation has decresed.").queue();
			Bootstrap.getDbUtil().userMinusRep(message.getAuthor().getIdLong());
		}
	}
	
	@BotCommandHandler
	private void commend(Message message, String userAsMention) {
		message.delete().queue();
		if (!message.getMentionedUsers().isEmpty()) {
			for (User u : message.getMentionedUsers()) {
				int originalProp = Bootstrap.getDbUtil().getUserReputaionById(u.getIdLong());
				int newVal = originalProp + 1;
				Bootstrap.getDbUtil().userPlusRep(u.getIdLong());
				message.getTextChannel().sendMessage(u.getAsMention() + "'s reputation is now " + newVal + "\n").queue();
			}
		}
		else {
			message.getTextChannel().sendMessage("You can't commend no one.").queue();
		}
	}
	
	@BotCommandHandler
	private void rep(Message message) {
		Map<Integer, String> repmap = Bootstrap.getDbUtil().getAllRep();
		
		MessageBuilder mb = new MessageBuilder();
		mb.append("```");
		
		Iterator<?> iter = repmap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Integer, String> ent = (Entry<Integer, String>) iter.next();
			String sign = "";
			if (ent.getKey() > 0) {
				sign = "+";
			}
			String mes = ent.getValue() + "'s reputation is " + sign + ent.getKey().toString() + "";
			mb.append(mes);
			mb.append("\r\n");
		}
		
		mb.append("```");
		message.getTextChannel().sendMessage(mb.build()).queue();
	}
	
	@BotCommandHandler
	private void help(Message message) {
		message.getTextChannel()
				.sendMessage("```Commands:\r\n" + "play, playNow, playNext <YouTube link | YouTube search> . Adds a song to the queue\r\n"
						+ "pause, resume . . . . . . . . . . . . . . . . . . . . . . Pause or resume the playing track\r\n"
						+ "skip . . . .  . . . . . . . . . . . . . . . . . . . . . . Skips the currently playing track\r\n"
						+ "shuffle . . . . . . . . . . . . . . . . . . . . . . . . . Shuffle the queue\r\n"
						+ "forward, back <Seconds> . . . . . . . . . . . . . . . . . Move a specified number of seconds forward or backward in the song\r\n"
						+ "seek <Seconds>. . . . . . . . . . . . . . . . . . . . . . Seek to a specified number of seconds into the song\r\n"
						+ "volume <1-100>. . . . . . . . . . . . . . . . . . . . . . Change the volume of the bot, value in percentage\r\n"
						+ "queue, q. . . . . . . . . . . . . . . . . . . . . . . . . Displays the queue\r\n"
						+ "report <@User> [for] any reason . . . . . . . . . . . . . Give -rep to mentioned users\r\n"
						+ "commend <@User> . . . . . . . . . . . . . . . . . . . . . Give +rep to mentioned users\r\n"
						+ "rep . . . . . . . . . . . . . . . . . . . . . . . . . . . Displays each user's reputation\r\n"
						+ "timer <Minutes> . . . . . . . . . . . . . . . . . . . . . Set a timer for a specified number of minutes (Unstable)\r\n" + "```");
	}
	
	@BotCommandHandler
	private void populateUsers(Message message) {
		if (!Bootstrap.getDbUtil().isAdmin(message.getAuthor().getIdLong())) {
			return;
		}
		
		List<Member> members = new ArrayList<Member>();
		
		for (Member m : guild.getMembers()) {
			if (!members.contains(m)) {
				members.add(m);
			}
		}
		
		for (Member m : members) {
			if (!Bootstrap.getDbUtil().userExists(m.getUser().getIdLong())) {
				try {
					Bootstrap.getDbUtil().insertNewUser(m.getUser().getIdLong(), m.getEffectiveName(), false, null);
				}
				catch (SQLException e) {
					e.printStackTrace();
				}
			}
			Bootstrap.getDbUtil().tryUpdateDisplayName(m.getUser().getIdLong(), m.getEffectiveName());
		}
		
	}
	
	public static class Factory implements BotControllerFactory<CommandController>
	{
		@Override
		public Class<CommandController> getControllerClass() {
			return CommandController.class;
		}
		
		@Override
		public CommandController create(BotApplicationManager manager, BotGuildContext state, Guild guild) {
			return new CommandController(manager, state, guild);
		}
	}
	
	public class BotTimer extends TimerTask
	{
		public BotTimer(Message mes, Long time)
		{
			this.timeInMillis = time;
			this.message = mes;
		}
		
		private Message message;
		private final Long timeInMillis;
		
		@Override
		public void run() {
			Long currentTime = Calendar.getInstance().getTimeInMillis();
			
			SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
			Date now = new Date();
			String startDate = timeFormat.format(now);
			
			Date later = new Date();
			later.setTime(currentTime + timeInMillis);
			String endDate = timeFormat.format(later);
			
			message.getTextChannel().sendMessage("Timer started at: " + startDate + " for " + timeInMillis / 1000 / 60 + " minutes. " + "End Time: " + endDate).queue();
			
			completeTask();
			
			Date done = new Date();
			String doneDate = timeFormat.format(done);
			
			int messageCount = 5;
			for (int i = 0; i < messageCount; i++) {
				MessageBuilder m = new MessageBuilder().append(message.getAuthor()).append(" Your timer has finished! End Time: " + doneDate);
				
				message.getAuthor().openPrivateChannel().complete().sendMessage(m.build()).queue();
				try {
					Thread.sleep(1500);
				}
				catch (InterruptedException e) {
				}
			}
		}
		
		private void completeTask() {
			try {
				Thread.sleep(timeInMillis);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
