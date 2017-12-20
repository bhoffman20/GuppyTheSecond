package com.guppy.guppythebot.music;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.guppy.guppythebot.BotApplicationManager;
import com.guppy.guppythebot.BotGuildContext;
import com.guppy.guppythebot.MessageDispatcher;
import com.guppy.guppythebot.controller.BotCommandHandler;
import com.guppy.guppythebot.controller.BotController;
import com.guppy.guppythebot.controller.BotControllerFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.remote.RemoteNode;
import com.sedmelluq.discord.lavaplayer.remote.message.NodeStatisticsMessage;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.DecodedTrackHolder;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.udpqueue.natives.UdpQueueManager;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.AudioManager;
import net.iharder.Base64;

public class MusicController implements BotController
{
	private static final Logger log = LoggerFactory.getLogger(MusicController.class);
	
	private final AudioPlayerManager manager;
	private final AudioPlayer player;
	private final AtomicReference<TextChannel> outputChannel;
	private final MusicScheduler scheduler;
	private final MessageDispatcher messageDispatcher;
	private final Guild guild;
	
	protected static final String QUEUE_TITLE = "__%s has added %d new track%s to the Queue:__";
	protected static final String QUEUE_DESCRIPTION = "%s **|>**  %s\n%s\n%s %s\n%s";
	protected static final String QUEUE_INFO = "Info about the Queue: (Size - %d)";
	
	public MusicController(BotApplicationManager manager, BotGuildContext state, Guild guild)
	{
		this.manager = manager.getPlayerManager();
		this.guild = guild;
		
		player = manager.getPlayerManager().createPlayer();
		guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));
		
		outputChannel = new AtomicReference<>();
		
		messageDispatcher = new GlobalDispatcher();
		scheduler = new MusicScheduler(player, messageDispatcher, manager.getExecutorService(), guild);
		
		player.setVolume(50);
		player.addListener(scheduler);
	}
	
	@BotCommandHandler
	private void play(Message message, String identifier)
	{
		if (!identifier.contains("http"))
		{
			identifier = "ytsearch: " + identifier;
		}
		
		addTrack(message, identifier, false);
	}
	
	@BotCommandHandler
	private void playNow(Message message, String identifier)
	{
		if (!identifier.contains("http"))
		{
			identifier = "ytsearch: " + identifier;
		}
		
		addTrack(message, identifier, true);
	}
	
	@BotCommandHandler
	private void playNext(Message message, String identifier)
	{
		if (!identifier.contains("http"))
		{
			identifier = "ytsearch: " + identifier;
		}
		
		addTrack(message, identifier, false, true);
	}
	
	@BotCommandHandler
	private void reset(Message message)
	{
		outputChannel.set(message.getTextChannel());
		messageDispatcher.sendMessage("Clearing queue...");
		scheduler.drainQueue();
		player.destroy();
		guild.getAudioManager().closeAudioConnection();
	}
	
	@BotCommandHandler
	private void hex(Message message, int pageCount)
	{
		manager.source(YoutubeAudioSourceManager.class).setPlaylistPageCount(pageCount);
	}
	
	@BotCommandHandler
	private void serialize(Message message) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MessageOutput outputStream = new MessageOutput(baos);
		
		for (AudioTrack track : scheduler.drainQueue())
		{
			manager.encodeTrack(outputStream, track);
		}
		
		outputStream.finish();
		
		message.getChannel().sendMessage(Base64.encodeBytes(baos.toByteArray())).queue();
	}
	
	@BotCommandHandler
	private void deserialize(Message message, String content) throws IOException
	{
		outputChannel.set(message.getTextChannel());
		connectToVoiceChannel(message);
		
		byte[] bytes = Base64.decode(content);
		
		MessageInput inputStream = new MessageInput(new ByteArrayInputStream(bytes));
		DecodedTrackHolder holder;
		
		while ((holder = manager.decodeTrack(inputStream)) != null)
		{
			if (holder.decodedTrack != null)
			{
				scheduler.addToQueue(holder.decodedTrack);
			}
		}
	}
	
	@BotCommandHandler
	private void volume(Message message, int volume)
	{
		player.setVolume(volume);
	}
	
	@BotCommandHandler
	private void vol(Message message, int volume)
	{
		volume(message, volume);
	}
	
	@BotCommandHandler
	private void nodes(Message message, String addressList)
	{
		manager.useRemoteNodes(addressList.split(" "));
	}
	
	@BotCommandHandler
	private void local(Message message)
	{
		manager.useRemoteNodes();
	}
	
	@BotCommandHandler
	private void gc(Message message, int duration)
	{
		UdpQueueManager.pauseDemo(duration);
	}
	
	@BotCommandHandler
	private void skip(Message message)
	{
		scheduler.skip();
	}
	
	@BotCommandHandler
	private void forward(Message message, int duration)
	{
		forPlayingTrack(track ->
		{
			track.setPosition(track.getPosition() + duration * 1000);
		});
	}
	
	@BotCommandHandler
	private void back(Message message, int duration)
	{
		forPlayingTrack(track ->
		{
			track.setPosition(Math.max(0, track.getPosition() - duration * 1000));
		});
	}
	
	@BotCommandHandler
	private void pause(Message message)
	{
		player.setPaused(true);
	}
	
	@BotCommandHandler
	private void resume(Message message)
	{
		player.setPaused(false);
	}
	
	@BotCommandHandler
	private void duration(Message message)
	{
		forPlayingTrack(track ->
		{
			message.getChannel().sendMessage("Duration is " + track.getDuration() / 1000).queue();
		});
	}
	
	@BotCommandHandler
	private void seek(Message message, long position)
	{
		forPlayingTrack(track ->
		{
			track.setPosition(position * 1000);
		});
	}
	
	@BotCommandHandler
	private void pos(Message message)
	{
		forPlayingTrack(track ->
		{
			message.getChannel().sendMessage("Position is " + track.getPosition()).queue();
		});
	}
	
	@BotCommandHandler
	private void marker(final Message message, long position, final String text)
	{
		forPlayingTrack(track ->
		{
			track.setMarker(new TrackMarker(position, state ->
			{
				message.getChannel().sendMessage("Trigger [" + text + "] cause [" + state.name() + "]").queue();
			}));
		});
	}
	
	@BotCommandHandler
	private void unmark(Message message)
	{
		forPlayingTrack(track ->
		{
			track.setMarker(null);
		});
	}
	
	@BotCommandHandler
	private void version(Message message)
	{
		// TODO: Make this return the bot version
		message.getChannel().sendMessage(PlayerLibrary.VERSION).queue();
	}
	
	@BotCommandHandler
	private void nodeinfo(Message message)
	{
		for (RemoteNode node : manager.getRemoteNodeRegistry().getNodes())
		{
			String report = buildReportForNode(node);
			message.getChannel().sendMessage(report).queue();
		}
	}
	
	@BotCommandHandler
	private void provider(Message message)
	{
		forPlayingTrack(track ->
		{
			RemoteNode node = manager.getRemoteNodeRegistry().getNodeUsedForTrack(track);
			
			if (node != null)
			{
				message.getChannel().sendMessage("Node " + node.getAddress()).queue();
			}
			else
			{
				message.getChannel().sendMessage("Not played by a remote node.").queue();
			}
		});
	}
	
	private String buildReportForNode(RemoteNode node)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("--- ").append(node.getAddress()).append(" ---\n");
		builder.append("Connection state: ").append(node.getConnectionState()).append("\n");
		
		NodeStatisticsMessage statistics = node.getLastStatistics();
		builder.append("Node global statistics: \n").append(statistics == null ? "unavailable" : "");
		
		if (statistics != null)
		{
			builder.append("   playing tracks: ").append(statistics.playingTrackCount).append("\n");
			builder.append("   total tracks: ").append(statistics.totalTrackCount).append("\n");
			builder.append("   system CPU usage: ").append(statistics.systemCpuUsage).append("\n");
			builder.append("   process CPU usage: ").append(statistics.processCpuUsage).append("\n");
		}
		
		builder.append("Minimum tick interval: ").append(node.getTickMinimumInterval()).append("\n");
		builder.append("Tick history capacity: ").append(node.getTickHistoryCapacity()).append("\n");
		
		List<RemoteNode.Tick> ticks = node.getLastTicks(false);
		builder.append("Number of ticks in history: ").append(ticks.size()).append("\n");
		
		if (ticks.size() > 0)
		{
			int tail = Math.min(ticks.size(), 3);
			builder.append("Last ").append(tail).append(" ticks:\n");
			
			for (int i = ticks.size() - tail; i < ticks.size(); i++)
			{
				RemoteNode.Tick tick = ticks.get(i);
				
				builder.append("   [duration ").append(tick.endTime - tick.startTime).append("]\n");
				builder.append("   start time: ").append(tick.startTime).append("\n");
				builder.append("   end time: ").append(tick.endTime).append("\n");
				builder.append("   response code: ").append(tick.responseCode).append("\n");
				builder.append("   request size: ").append(tick.requestSize).append("\n");
				builder.append("   response size: ").append(tick.responseSize).append("\n");
			}
		}
		
		List<AudioTrack> tracks = node.getPlayingTracks();
		
		builder.append("Number of playing tracks: ").append(tracks.size()).append("\n");
		
		if (tracks.size() > 0)
		{
			int head = Math.min(tracks.size(), 3);
			builder.append("First ").append(head).append(" tracks:\n");
			
			for (int i = 0; i < head; i++)
			{
				AudioTrack track = tracks.get(i);
				
				builder.append("   [identifier ").append(track.getInfo().identifier).append("]\n");
				builder.append("   name: ").append(track.getInfo().author).append(" - ").append(track.getInfo().title).append("\n");
				builder.append("   progress: ").append(track.getPosition()).append(" / ").append(track.getDuration()).append("\n");
			}
		}
		
		builder.append("Balancer penalties: ").append(tracks.size()).append("\n");
		
		for (Map.Entry<String, Integer> penalty : node.getBalancerPenaltyDetails().entrySet())
		{
			builder.append("   ").append(penalty.getKey()).append(": ").append(penalty.getValue()).append("\n");
		}
		
		return builder.toString();
	}
	
	private void addTrack(final Message message, final String identifier, final boolean now)
	{
		addTrack(message, identifier, now, false);
	}
	
	private void addTrack(final Message message, final String identifier, final boolean now, final boolean next)
	{
		outputChannel.set(message.getTextChannel());
		
		manager.loadItemOrdered(this, identifier, new AudioLoadResultHandler()
		{
			@Override
			public void trackLoaded(AudioTrack track)
			{
				System.out.println("Track Loaded: " + track.getInfo().title);
				connectToVoiceChannel(message);
				
				messageDispatcher.sendMessage("Starting now: " + track.getInfo().title);
				
				if (now)
				{
					scheduler.playNow(track, true);
				}
				else if (next)
				{
					scheduler.playNext(track);
				}
				else
				{
					scheduler.addToQueue(track);
				}
			}
			
			@Override
			public void playlistLoaded(AudioPlaylist playlist)
			{
				AudioTrack selected = playlist.getSelectedTrack();
				
				List<AudioTrack> tracks = playlist.getTracks();
				if (playlist.getName().startsWith("Search results for") && selected == null)
				{
					selected = tracks.get(0);
				}
				else
				{
					messageDispatcher.sendMessage("Loaded playlist: " + playlist.getName() + " (" + tracks.size() + ")");
				}
				
				connectToVoiceChannel(message);
				
				if (selected != null)
				{
					message.getChannel().sendMessage("Adding track to queue:  " + selected.getInfo().title).queue();
				}
				else
				{
					selected = tracks.get(0);
					message.getChannel().sendMessage("Added first track from playlist: " + selected.getInfo().title).queue();
					
					for (int i = 0; i < Math.min(10, playlist.getTracks().size()); i++)
					{
						if (tracks.get(i) != selected)
						{
							scheduler.addToQueue(tracks.get(i));
						}
					}
				}
				
				if (now)
				{
					scheduler.playNow(selected, true);
				}
				else if (next)
				{
					scheduler.playNext(selected);
				}
				else
				{
					scheduler.addToQueue(selected);
				}
				
			}
			
			@Override
			public void noMatches()
			{
				message.getChannel().sendMessage("Nothing found for " + identifier).queue();
			}
			
			@Override
			public void loadFailed(FriendlyException throwable)
			{
				message.getChannel().sendMessage("Failed with message: " + throwable.getMessage() + " (" + throwable.getClass().getSimpleName() + ")").queue();
			}
		});
	}
	
	private void forPlayingTrack(TrackOperation operation)
	{
		AudioTrack track = player.getPlayingTrack();
		
		if (track != null)
		{
			operation.execute(track);
		}
	}
	
	@BotCommandHandler
	public void shuffle()
	{
		List<AudioTrack> tQueue = new ArrayList<>(scheduler.getQueuedTracks());
		AudioTrack current = tQueue.get(0);
		tQueue.remove(0);
		Collections.shuffle(tQueue);
		tQueue.add(0, current);
		scheduler.drainQueue();
		scheduler.queue.addAll(tQueue);
	}
	
	@BotCommandHandler
	private void queue(Message message)
	{
		outputChannel.set(message.getTextChannel());
		
		if (scheduler.getQueuedTracks().isEmpty())
		{
			if (null == player.getPlayingTrack())
			{
				messageDispatcher.sendMessage("Queue is empty");
			}
			else
			{
				String embedTitle = String.format(QUEUE_INFO, 1);
				messageDispatcher.sendMessage(embedTitle + "\n**>** " + buildQueueMessage(player.getPlayingTrack()), message.getTextChannel());
			}
			
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			Set<AudioTrack> queue = scheduler.getQueuedTracks();
			
			queue.forEach(t -> sb.append(buildQueueMessage(t)));
			// for (AudioTrack track : queue)
			// {
			// System.out.println(buildQueueMessage(track));
			// sb.append(buildQueueMessage(track));
			// }
			
			String embedTitle = String.format(QUEUE_INFO, queue.size() + 1);
			
			if (sb.length() <= 1960)
			{
				messageDispatcher.sendMessage(embedTitle + "\n**>** " + buildQueueMessage(player.getPlayingTrack()) + sb.toString(), message.getTextChannel());
			}
			else /* if (sb.length() <= 2000) */
			{
				try
				{
					sb.setLength(sb.length() - 1);
					HttpResponse<String> response = Unirest.post("https://hastebin.com/documents").body(sb.toString()).asString();
					messageDispatcher.sendMessage(
							embedTitle + "\n[Click here for a detailed list](https://hastebin.com/" + new JSONObject(response.getBody().toString()).getString("key") + ")",
							message.getTextChannel());
				}
				catch (UnirestException ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}
	
	@BotCommandHandler
	private void q(Message message)
	{
		queue(message);
	}
	
	protected String buildQueueMessage(AudioTrack audioTrack)
	{
		AudioTrackInfo trackInfo = audioTrack.getInfo();
		String title = trackInfo.title;
		long length = trackInfo.length;
		return "`[ " + getTimestamp(length) + " ]` " + title + "\n";
	}
	
	protected String getTimestamp(long milis)
	{
		long seconds = milis / 1000;
		long hours = Math.floorDiv(seconds, 3600);
		seconds = seconds - (hours * 3600);
		long mins = Math.floorDiv(seconds, 60);
		seconds = seconds - (mins * 60);
		return (hours == 0 ? "" : hours + ":") + String.format("%02d", mins) + ":" + String.format("%02d", seconds);
	}
	
	private void connectToVoiceChannel(Message message)
	{
		AudioManager audioManager = message.getGuild().getAudioManager();
		if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect())
		{
			if (!message.getMember().getVoiceState().inVoiceChannel())
			{
				messageDispatcher.sendMessage("User must be in a voice channel to request a song.");
			}
			else
			{
				audioManager.openAudioConnection(message.getMember().getVoiceState().getChannel());
			}
		}
	}
	
	private interface TrackOperation
	{
		void execute(AudioTrack track);
	}
	
	private class GlobalDispatcher implements MessageDispatcher
	{
		private HashMap<TextChannel, FixedDispatcher> dispatchers;
		
		
		/* Only use this if outputchannel is not null */
		@Override
		public void sendMessage(String message, Consumer<Message> success, Consumer<Throwable> failure)
		{
			TextChannel channel = outputChannel.get();
			
			if (channel != null)
			{
				channel.sendMessage(message).queue(success, failure);
			}
			else
			{
				System.out.println("WARNING: Tried to send a message to a null output channel!");
			}
		}
		
		
		/* Only use this if outputchannel is not null */
		@Override
		public void sendMessage(String message)
		{
			TextChannel channel = outputChannel.get();
			
			if (channel != null)
			{
				channel.sendMessage(message).queue();
			}
			else
			{
				System.out.println("WARNING: Tried to send a message to a null output channel!");
			}
		}
		
		public void sendMessage(String msgContent, MessageChannel tChannel)
		{
			if (tChannel == null)
			{
				System.out.println("WARNING: Tried to send a message to a null output channel!");
				return;
			}
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
	
	public static class Factory implements BotControllerFactory<MusicController>
	{
		@Override
		public Class<MusicController> getControllerClass()
		{
			return MusicController.class;
		}
		
		@Override
		public MusicController create(BotApplicationManager manager, BotGuildContext state, Guild guild)
		{
			return new MusicController(manager, state, guild);
		}
	}
	
}
