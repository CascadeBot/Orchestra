package org.cascadebot.orchestra;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import lavalink.client.io.jda.JdaLavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.cascadebot.orchestra.data.LavalinkNode;
import org.cascadebot.orchestra.data.enums.NodeType;
import org.cascadebot.orchestra.players.CascadePlayer;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class MusicHandler {

    private static MusicHandler instance;

    private AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private Pattern typePattern = Pattern.compile("youtube#([A-z]+)");

    private Map<String, String> sourceNames = new HashMap<>();

    private Map<Long, CascadePlayer> players = new HashMap<>();

    private JdaLavalink lavalink;

    public MusicHandler(long clientId, int numShards, Function<Integer, JDA> jdaFunction) {
        this(new ArrayList<>(), clientId, numShards, jdaFunction);
    }

    public MusicHandler(List<LavalinkNode> initialNodes, long clientId, int numShards, Function<Integer, JDA> jdaFunction) {
        AudioSourceManagers.registerLocalSource(playerManager);

        YoutubeAudioSourceManager youtubeManager = new YoutubeAudioSourceManager(false);
        youtubeManager.configureRequests(requestConfig -> RequestConfig.copy(requestConfig)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES).setConnectTimeout(5000).build());
        playerManager.registerSourceManager(youtubeManager);
        sourceNames.put("youtube", youtubeManager.getSourceName());

        TwitchStreamAudioSourceManager twitchStreamAudioSourceManager = new TwitchStreamAudioSourceManager();
        sourceNames.put("twitch", twitchStreamAudioSourceManager.getSourceName());
        playerManager.registerSourceManager(twitchStreamAudioSourceManager);
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());

        lavalink = new JdaLavalink(String.valueOf(clientId), numShards, jdaFunction);

        for (LavalinkNode lavalinkNode: initialNodes) {
            lavalink.addNode(lavalinkNode.getName(), URI.create(lavalinkNode.getHost()), lavalinkNode.getPassword());
        }

        instance = this;
    }

    public CascadePlayer getPlayer(Guild guild, NodeType nodeType) {
        if (players.containsKey(guild.getIdLong())) {
            return players.get(guild.getIdLong());
        } else {
            return createPlayer(guild, nodeType);
        }
    }

    private CascadePlayer createPlayer(Guild guild, NodeType nodeType) {
        lavalink.getLink(guild).getPlayer();
        return null;
    }

    public static MusicHandler getInstance() {
        return instance;
    }

}
