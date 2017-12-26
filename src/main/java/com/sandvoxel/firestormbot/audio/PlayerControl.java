package com.sandvoxel.firestormbot.audio;

import com.sandvoxel.firestormbot.FireStormBot;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

public class PlayerControl extends ListenerAdapter {

    public static final int DEFAULT_VOLUME = 35; //(0 - 150, where 100 is default max volume)

    private final AudioPlayerManager playerManager;
    private final Map<String, GuildMusicManager> musicManagers;

    public PlayerControl()
    {
        java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").setLevel(Level.OFF);

        this.playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager());
        playerManager.registerSourceManager(new LocalAudioSourceManager());

        musicManagers = new HashMap<String, GuildMusicManager>();
    }
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (!event.isFromType(ChannelType.TEXT))
            return;

        try
        {
            List<String> allowedIds = Files.readAllLines(Paths.get("admins.txt"));
            if (!allowedIds.contains(event.getAuthor().getId()))
                return;
        }
        catch (IOException ignored)
        {
            //If we encounter an ioe, it is due to the file not existing.
            //In that case, we treat the music system as not having admin restrictions.
        }

        String[] command = event.getMessage().getContent().split(" ", 2);
        if (!command[0].startsWith(FireStormBot.CommandPrefix))    //message doesn't start with prefix.
            return;

        Guild guild = event.getGuild();
        GuildMusicManager mng = getMusicManager(guild);
        AudioPlayer player = mng.player;
        TrackScheduler scheduler = mng.scheduler;

        if ((FireStormBot.CommandPrefix+"join").equals(command[0]))
        {
            VoiceChannel chan = null;

            if (command.length == 1) //No channel name was provided to search for.
            {
                if(event.getMember().getVoiceState().inVoiceChannel()){
                    JoinChannel(guild,event,event.getMember().getVoiceState().getChannel(),mng.sendHandler);
                } else {
                    event.getChannel().sendMessage("No channel name was provided to search with to join.").queue();
                }
            }
            else
            {
                try
                {
                    chan = guild.getVoiceChannelById(command[1]);
                }
                catch (NumberFormatException ignored) {}

                if (chan == null)
                    chan = guild.getVoiceChannelsByName(command[1], true).stream().findFirst().orElse(null);
                if (chan == null)
                {
                    event.getChannel().sendMessage("Could not find VoiceChannel by name: " + command[1]).queue();
                }
                else
                {
                    JoinChannel(guild,event,chan,mng.sendHandler);
                }
            }
        }
        else if ((FireStormBot.CommandPrefix+"leave").equals(command[0]))
        {
            guild.getAudioManager().setSendingHandler(null);
            guild.getAudioManager().closeAudioConnection();
        }
        else if ((FireStormBot.CommandPrefix+"play").equals(command[0]))
        {
            if (command.length == 1) //It is only the command to start playback (probably after pause)
            {
                if (player.isPaused())
                {
                    player.setPaused(false);
                    event.getChannel().sendMessage("Playback as been resumed.").queue();
                }
                else if (player.getPlayingTrack() != null)
                {
                    event.getChannel().sendMessage("Player is already playing!").queue();
                }
                else if (scheduler.queue.isEmpty())
                {
                    event.getChannel().sendMessage("The current queue is empty! Add something to the queue first!").queue();
                }
            }
            else    //Commands has 2 parts, .play and url.
            {
                if(!guild.getAudioManager().isConnected() && event.getMember().getVoiceState().inVoiceChannel())
                    JoinChannel(guild,event,event.getMember().getVoiceState().getChannel(),mng.sendHandler);

                loadAndPlay(mng, event.getChannel(), command[1], false);
            }
        }
        else if ((FireStormBot.CommandPrefix+"pplay").equals(command[0]) && command.length == 2)
        {
            loadAndPlay(mng, event.getChannel(), command[1], true);
        }
        else if ((FireStormBot.CommandPrefix+"skip").equals(command[0]))
        {
            scheduler.nextTrack();
            event.getChannel().sendMessage("The current track was skipped.").queue();
        }
        else if ((FireStormBot.CommandPrefix+"pause").equals(command[0]))
        {
            if (player.getPlayingTrack() == null)
            {
                event.getChannel().sendMessage("Cannot pause or resume player because no track is loaded for playing.").queue();
                return;
            }

            player.setPaused(!player.isPaused());
            if (player.isPaused())
                event.getChannel().sendMessage("The player has been paused.").queue();
            else
                event.getChannel().sendMessage("The player has resumed playing.").queue();
        }
        else if ((FireStormBot.CommandPrefix+"stop").equals(command[0]))
        {
            scheduler.queue.clear();
            player.stopTrack();
            player.setPaused(false);
            event.getChannel().sendMessage("Playback has been completely stopped and the queue has been cleared.").queue();
        }
        else if ((FireStormBot.CommandPrefix+"volume").equals(command[0]))
        {
            if (command.length == 1)
            {
                event.getChannel().sendMessage("Current player volume: **" + player.getVolume() + "**").queue();
            }
            else
            {
                try
                {
                    int newVolume = Math.max(10, Math.min(150, Integer.parseInt(command[1])));
                    int oldVolume = player.getVolume();
                    player.setVolume(newVolume);
                    event.getChannel().sendMessage("Player volume changed from `" + oldVolume + "` to `" + newVolume + "`").queue();
                }
                catch (NumberFormatException e)
                {
                    event.getChannel().sendMessage("`" + command[1] + "` is not a valid integer. (10 - 100)").queue();
                }
            }
        }
        else if ((FireStormBot.CommandPrefix+"restart").equals(command[0]))
        {
            AudioTrack track = player.getPlayingTrack();
            if (track == null)
                track = scheduler.lastTrack;

            if (track != null)
            {
                event.getChannel().sendMessage("Restarting track: " + track.getInfo().title).queue();
                player.playTrack(track.makeClone());
            }
            else
            {
                event.getChannel().sendMessage("No track has been previously started, so the player cannot replay a track!").queue();
            }
        }
        else if ((FireStormBot.CommandPrefix+"repeat").equals(command[0]))
        {
            scheduler.setRepeating(!scheduler.isRepeating());
            event.getChannel().sendMessage("Player was set to: **" + (scheduler.isRepeating() ? "repeat" : "not repeat") + "**").queue();
        }
        else if ((FireStormBot.CommandPrefix+"reset").equals(command[0]))
        {
            synchronized (musicManagers)
            {
                scheduler.queue.clear();
                player.destroy();
                guild.getAudioManager().setSendingHandler(null);
                musicManagers.remove(guild.getId());
            }

            mng = getMusicManager(guild);
            guild.getAudioManager().setSendingHandler(mng.sendHandler);
            event.getChannel().sendMessage("The player has been completely reset!").queue();

        }
        else if ((FireStormBot.CommandPrefix+"nowplaying").equals(command[0]) || (FireStormBot.CommandPrefix+"np").equals(command[0]))
        {
            AudioTrack currentTrack = player.getPlayingTrack();
            if (currentTrack != null)
            {
                String title = currentTrack.getInfo().title;
                String position = getTimestamp(currentTrack.getPosition());
                String duration = getTimestamp(currentTrack.getDuration());

                String nowplaying = String.format("**Playing:** %s\n**Time:** [%s / %s]",
                        title, position, duration);

                event.getChannel().sendMessage(nowplaying).queue();
            }
            else
                event.getChannel().sendMessage("The player is not currently playing anything!").queue();
        }
        else if ((FireStormBot.CommandPrefix+"list").equals(command[0]))
        {
            Queue<AudioTrack> queue = scheduler.queue;
            synchronized (queue)
            {
                if (queue.isEmpty())
                {
                    event.getChannel().sendMessage("The queue is currently empty!").queue();
                }
                else
                {
                    int trackCount = 0;
                    long queueLength = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Current Queue: Entries: ").append(queue.size()).append("\n");
                    for (AudioTrack track : queue)
                    {
                        queueLength += track.getDuration();
                        if (trackCount < 10)
                        {
                            sb.append("`[").append(getTimestamp(track.getDuration())).append("]` ");
                            sb.append(track.getInfo().title).append("\n");
                            trackCount++;
                        }
                    }
                    sb.append("\n").append("Total Queue Time Length: ").append(getTimestamp(queueLength));

                    event.getChannel().sendMessage(sb.toString()).queue();
                }
            }
        }
        else if ((FireStormBot.CommandPrefix+"shuffle").equals(command[0]))
        {
            if (scheduler.queue.isEmpty())
            {
                event.getChannel().sendMessage("The queue is currently empty!").queue();
                return;
            }

            scheduler.shuffle();
            event.getChannel().sendMessage("The queue has been shuffled!").queue();
        }else if((FireStormBot.CommandPrefix+"SearchPrefix").equals(command[0]))
        {
            event.getChannel().sendMessage("YouTube Search: ytsearch:<NAME OF VID> \nSound Cloud Search: scsearch:<NAME OF SONG>").queue();
        }
    }

    private void loadAndPlay(GuildMusicManager mng, final MessageChannel channel, String url, final boolean addPlaylist)
    {
        final String trackUrl;

        //Strip <>'s that prevent discord from embedding link resources
        if (url.startsWith("<") && url.endsWith(">"))
            trackUrl = url.substring(1, url.length() - 1);
        else if(url.contains("go.twitch.tv")){
            trackUrl = url.trim().replace("go.","");
        }
        else
            trackUrl = url;

        playerManager.loadItemOrdered(mng, trackUrl, new AudioLoadResultHandler()
        {
            @Override
            public void trackLoaded(AudioTrack track)
            {
                String msg = "Adding to queue: " + track.getInfo().title;
                if (mng.player.getPlayingTrack() == null)
                    msg += "\nand the Player has started playing;";

                mng.scheduler.queue(track);
                channel.sendMessage(msg).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist)
            {
                AudioTrack firstTrack = playlist.getSelectedTrack();
                List<AudioTrack> tracks = playlist.getTracks();


                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                if (addPlaylist)
                {
                    channel.sendMessage("Adding **" + playlist.getTracks().size() +"** tracks to queue from playlist: " + playlist.getName()).queue();
                    tracks.forEach(mng.scheduler::queue);
                }
                else
                {
                    channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();
                    mng.scheduler.queue(firstTrack);
                }
            }

            @Override
            public void noMatches()
            {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception)
            {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void JoinChannel(Guild guild,MessageReceivedEvent event , VoiceChannel channel,AudioSendHandler scheduler){
        guild.getAudioManager().setSendingHandler(scheduler);
        try
        {
            guild.getAudioManager().openAudioConnection(channel);
        }
        catch (PermissionException e)
        {
            if (e.getPermission() == Permission.VOICE_CONNECT)
            {
                event.getChannel().sendMessage("Alt-Knight does not have permission to connect to: " + channel.getName()).queue();
            }
        }
    }

    private GuildMusicManager getMusicManager(Guild guild)
    {
        String guildId = guild.getId();
        GuildMusicManager mng = musicManagers.get(guildId);
        if (mng == null)
        {
            synchronized (musicManagers)
            {
                mng = musicManagers.get(guildId);
                if (mng == null)
                {
                    mng = new GuildMusicManager(playerManager);
                    mng.player.setVolume(DEFAULT_VOLUME);
                    musicManagers.put(guildId, mng);
                }
            }
        }
        return mng;
    }

    private static String getTimestamp(long milliseconds)
    {
        int seconds = (int) (milliseconds / 1000) % 60 ;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours   = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        if (hours > 0)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%02d:%02d", minutes, seconds);
    }
}
