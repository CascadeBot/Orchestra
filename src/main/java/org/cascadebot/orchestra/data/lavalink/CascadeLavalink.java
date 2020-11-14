package org.cascadebot.orchestra.data.lavalink;

import lavalink.client.io.Lavalink;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.cascadebot.orchestra.data.LavalinkNode;
import org.cascadebot.orchestra.data.enums.NodeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CascadeLavalink extends Lavalink<CascadeLink> implements EventListener {

    private Map<String, CascadeLink> linkMap = new HashMap<>();
    private Function<Integer, JDA> jdaProvider;
    private int shards;
    private List<LavalinkNode> lavalinkNodes = new ArrayList<>();
    private String userId;
    private CascadeLoadBalancer cascadeLoadBalancer;
    private CascadeVoiceInterceptor cascadeVoiceInterceptor = new CascadeVoiceInterceptor(this);

    public CascadeLavalink(String userId, int numShards, Function<Integer, JDA> jdaProvider) {
        super(userId, numShards);
        this.jdaProvider = jdaProvider;
        this.shards = numShards;
        this.userId = userId;
    }

    @Override
    public CascadeLink getLink(String guildId) {
        return getLink(guildId, null);
    }

    public CascadeLink getLink(String guildId, NodeType nodeType) {
        if (nodeType != null) {
            return linkMap.getOrDefault(guildId, buildNewLink(guildId, nodeType));
        } else {
            return linkMap.get(guildId);
        }
    }

    protected CascadeLink buildNewLink(String guildId) {
        throw new UnsupportedOperationException("Use #buildNewLink(String, NodeType) instead");
    }

    private CascadeLink buildNewLink(String guildId, NodeType type) {
        return new CascadeLink(this, guildId, type);
    }

    public JDA getJdaFromGuild(String guildId) {
        long id = Long.parseLong(guildId);
        return jdaProvider.apply((int) ((id >> 22) % shards));
    }

    @Override
    public void addNode(URI serverUri, String password) {
        throw new UnsupportedOperationException("When using CascaodeLavalink use the #addNode(LavalinkNode) method");
    }

    @Override
    public void addNode(String name, URI serverUri, String password) {
        throw new UnsupportedOperationException("When using CascaodeLavalink use the #addNode(LavalinkNode) method");
    }

    public void addNode(LavalinkNode node) {
        super.addNode(node.getName(), node.getHost(), node.getPassword());
        node.setLavalinkSocket(getNodes().stream().filter(lavalinkSocket -> lavalinkSocket.getName().equals(node.getName())
                && lavalinkSocket.getRemoteUri().equals(node.getHost())).findFirst().orElseThrow()); // We need to do this to get the socket
        lavalinkNodes.add(node);
    }

    // TODO remove nodes

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof ReconnectedEvent) {
            for (Map.Entry<String, CascadeLink> entry : getLinksMap().entrySet()) {
                String lastChannel = entry.getValue().getLastChannel();
                if (lastChannel != null
                        && getJdaFromGuild(entry.getKey()).getGuildById(entry.getKey()) != null) {
                    VoiceChannel voiceChannel = getJdaFromGuild(entry.getKey()).getVoiceChannelById(lastChannel);
                    if (voiceChannel != null) {
                        entry.getValue().connect(voiceChannel);
                    }
                }
            }
        }
    }

    public List<LavalinkNode> getLavalinkNodes() {
        return lavalinkNodes;
    }

    public CascadeLoadBalancer getCascadeLoadBalancer() {
        return cascadeLoadBalancer;
    }

    public CascadeVoiceInterceptor getVoiceInterceptor() {
        return cascadeVoiceInterceptor;
    }
}
