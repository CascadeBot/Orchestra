package org.cascadebot.orchestra.data.lavalink;

import lavalink.client.io.Link;
import lavalink.client.io.jda.JdaLink;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class CascadeVoiceInterceptor implements VoiceDispatchInterceptor {

    CascadeLavalink lavalink;
    public CascadeVoiceInterceptor(CascadeLavalink cascadeLavalink) {
        lavalink = cascadeLavalink;
    }

    @Override
    public void onVoiceServerUpdate(VoiceDispatchInterceptor.VoiceServerUpdate update) {
        JSONObject content = new JSONObject(update.toData().getObject("d").toMap());

        // Get session
        Guild guild = update.getGuild();
        if (guild == null)
            throw new IllegalArgumentException("Attempted to start audio connection with Guild that doesn't exist! JSON: " + content);

        lavalink.getLink(guild.getId()).onVoiceServerUpdate(content, guild.getSelfMember().getVoiceState().getSessionId());
    }

    @Override
    public boolean onVoiceStateUpdate(VoiceDispatchInterceptor.VoiceStateUpdate update) {
        VoiceChannel channel = update.getChannel();
        CascadeLink link = lavalink.getLink(update.getGuildId());

        if (channel == null) {
            if (link.getState() != Link.State.DESTROYED) {
                link.onDisconnected();
            }
        } else {
            link.setChannel(channel.getId());
        }

        return link.getState() == Link.State.CONNECTED;
    }

}
