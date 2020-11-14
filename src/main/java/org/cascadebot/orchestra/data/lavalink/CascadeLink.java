package org.cascadebot.orchestra.data.lavalink;

import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.Link;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.cascadebot.orchestra.data.enums.NodeType;

public class CascadeLink extends Link {

    private CascadeLavalink lavalink;
    private String guildId;
    private LavalinkSocket node = null;
    private NodeType nodeType;

    public CascadeLink(CascadeLavalink lavalink, String guildId, NodeType nodeType) {
        super(lavalink, guildId);
        this.lavalink = lavalink;
        this.guildId = guildId;
        this.nodeType = nodeType;
    }

    public void connect(VoiceChannel channel) {
        // TODO more checks
        queueAudioConnect(channel.getIdLong());
        if (!channel.getGuild().getId().equals(guildId)) {
            return; // TODO throw errors
        }
        if (channel.getJDA().isUnavailable(channel.getGuild().getIdLong())) {
            return;
        }
        setState(State.CONNECTING);
    }

    @Override
    public LavalinkSocket getNode(boolean selectIfAbsent) {
        if (selectIfAbsent && node == null) {
            node = lavalink.getCascadeLoadBalancer().getBestSocketForType(guild, nodeType, true);
            if (getPlayer() != null) getPlayer().onNodeChange();
        }
        return node;
    }

    @Override
    protected void removeConnection() {
        // JDA handles
    }

    @Override
    protected void queueAudioDisconnect() {
        Guild guild = lavalink.getJdaFromGuild(guildId).getGuildById(guildId);
        if (guild != null) {
            lavalink.getJdaFromGuild(guildId).getDirectAudioController().disconnect(guild);
        }
    }

    @Override
    protected void queueAudioConnect(long channelId) {
        VoiceChannel voiceChannel = lavalink.getJdaFromGuild(guildId).getVoiceChannelById(channelId);
        if (voiceChannel != null) {
            lavalink.getJdaFromGuild(guildId).getDirectAudioController().connect(voiceChannel);
        }
    }
}
