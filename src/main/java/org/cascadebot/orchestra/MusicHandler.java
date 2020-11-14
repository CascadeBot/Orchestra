package org.cascadebot.orchestra;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import lavalink.client.player.IPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.cascadebot.orchestra.data.LavalinkNode;
import org.cascadebot.orchestra.data.SearchResult;
import org.cascadebot.orchestra.data.enums.NodeType;
import org.cascadebot.orchestra.data.enums.SearchResultType;
import org.cascadebot.orchestra.data.lavalink.CascadeLavalink;
import org.cascadebot.orchestra.players.CascadePlayer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicHandler {

    private static MusicHandler instance;

    private AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private Pattern typePattern = Pattern.compile("youtube#([A-z]+)");

    private Map<String, String> sourceNames = new HashMap<>();

    private Map<Long, CascadePlayer> players = new HashMap<>();

    private CascadeLavalink lavalink;

    private OkHttpClient client;
    private JsonParser musicParser = new JsonParser();

    public MusicHandler(long clientId, int numShards, Function<Integer, JDA> jdaFunction) {
        this(new ArrayList<>(), clientId, numShards, jdaFunction);
    }

    public MusicHandler(List<LavalinkNode> initialNodes, long clientId, int numShards, Function<Integer, JDA> jdaFunction) {
        AudioSourceManagers.registerLocalSource(playerManager);

        client = new OkHttpClient.Builder().build();

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

        lavalink = new CascadeLavalink(String.valueOf(clientId), numShards, jdaFunction);

        for (LavalinkNode lavalinkNode: initialNodes) {
            lavalink.addNode(lavalinkNode);
        }

        instance = this;
    }

    public CascadePlayer getPlayer(String guildId, NodeType nodeType) {
        if (players.containsKey(Long.parseLong(guildId))) {
            return players.get(Long.parseLong(guildId));
        } else {
            return createPlayer(guildId, nodeType);
        }
    }

    private CascadePlayer createPlayer(String guildId, NodeType nodeType) {
        IPlayer player = lavalink.getLink(guildId, nodeType).getPlayer();
        return new CascadePlayer(player);
    }

    public static MusicHandler getInstance() {
        return instance;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public CascadeLavalink getLavalink() {
        return lavalink;
    }

    public void searchTracks(String search, String youtubeKey, Consumer<List<SearchResult>> searchResult, Consumer<Throwable> exceptionConsumer) {
        Request request = new Request.Builder().url("https://www.googleapis.com/youtube/v3/search?part=snippet&q=" +
                URLEncoder.encode(search, StandardCharsets.UTF_8) + "&key=" + URLEncoder.encode(youtubeKey, StandardCharsets.UTF_8) +
                "&maxResults=5&type=video,playlist").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                exceptionConsumer.accept(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.body() == null) {
                    exceptionConsumer.accept(new IOException("Got no response from youtube!")); // TODO custom exceptions
                    return;
                }

                if (!response.isSuccessful()) {
                    exceptionConsumer.accept(new IOException("Request failed"));
                }

                List<SearchResult> searchResults = new ArrayList<>();
                JsonObject json = musicParser.parse(response.body().charStream()).getAsJsonObject();
                JsonArray items = json.getAsJsonArray("items");
                int i = 0;
                for (JsonElement elm : items) {
                    i++;
                    if (i > 5) {
                        break;
                    }
                    JsonObject item = elm.getAsJsonObject();
                    JsonObject idObj = item.getAsJsonObject("id");
                    String kind = idObj.get("kind").getAsString();
                    Matcher matcher = typePattern.matcher(kind);
                    if (!matcher.matches()) {
                        continue;
                    }
                    kind = matcher.group(1);
                    String url = "";
                    SearchResultType type = null;
                    switch (kind) {
                        case "playlist":
                            type = SearchResultType.PLAYLIST;
                            url = "https://www.youtube.com/playlist?list=" + URLEncoder.encode(idObj.get("playlistId").getAsString(), StandardCharsets.UTF_8);
                            break;
                        case "video":
                            type = SearchResultType.VIDEO;
                            url = "https://www.youtube.com/watch?v=" + URLEncoder.encode(idObj.get("videoId").getAsString(), StandardCharsets.UTF_8);
                            break;
                    }
                    String title = item.get("snippet").getAsJsonObject().get("title").getAsString();
                    searchResults.add(new SearchResult(type, url, title));
                }
                searchResult.accept(searchResults);
            }
        });


    }

}
