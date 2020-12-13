/*
 * Copyright (c) 2019 CascadeBot. All rights reserved.
 * Licensed under the MIT license.
 */

package org.cascadebot.orchestra;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.event.IPlayerEventListener;
import lavalink.client.player.event.PlayerEvent;
import lavalink.client.player.event.TrackEndEvent;
import org.cascadebot.orchestra.data.enums.LoopMode;
import org.cascadebot.orchestra.players.CascadePlayer;

public class PlayerListener implements IPlayerEventListener, AudioEventListener {

    private final CascadePlayer player;

    public PlayerListener(CascadePlayer player) {
        this.player = player;
    }

    @Override
    public void onEvent(PlayerEvent playerEvent) {
        if (playerEvent instanceof TrackEndEvent) {
            onEnd(((TrackEndEvent) playerEvent).getTrack());
        }
    }

    @Override
    public void onEvent(AudioEvent audioEvent) {
        if (audioEvent instanceof com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent) {
            onEnd(((com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent) audioEvent).track);
        }
    }

    private void onEnd(AudioTrack track) {
        if (player.getLoopMode().equals(LoopMode.DISABLED) || player.getLoopMode().equals(LoopMode.PLAYLIST)) {
            player.playNextTrack(player.getLoopMode().equals(LoopMode.PLAYLIST));
        } else if (player.getLoopMode().equals(LoopMode.SONG)) {
            // Take the song that just finished and repeat it
            player.playTrack(track.makeClone());
        }

    }

}
